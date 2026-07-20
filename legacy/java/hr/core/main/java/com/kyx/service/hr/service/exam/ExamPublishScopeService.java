package com.kyx.service.hr.service.exam;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyx.foundation.common.biz.system.tenant.TenantCommonApi;
import com.kyx.foundation.common.biz.system.tenant.TenantFeatureCodeConstants;
import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.enums.TenantViewScopeEnum;
import com.kyx.foundation.common.exception.enums.GlobalErrorCodeConstants;
import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.security.core.service.SecurityFrameworkService;
import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.hr.controller.admin.exam.vo.scope.ExamPublishScopeOptionsRespVO;
import com.kyx.service.hr.dal.dataobject.scope.ScopeDeptDO;
import com.kyx.service.hr.dal.dataobject.scope.ScopeRoleDO;
import com.kyx.service.hr.dal.dataobject.scope.ScopeUserDO;
import com.kyx.service.hr.dal.dataobject.tenant.TenantDO;
import com.kyx.service.hr.dal.mysql.scope.ScopeDeptMapper;
import com.kyx.service.hr.dal.mysql.scope.ScopeRoleMapper;
import com.kyx.service.hr.dal.mysql.scope.ScopeUserMapper;
import com.kyx.service.hr.dal.mysql.tenant.TenantMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ExamPublishScopeService {

    public static final String CROSS_TENANT_PERMISSION = "hr:exam:publish:cross-tenant";

    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private SecurityFrameworkService securityFrameworkService;
    @Resource
    private TenantCommonApi tenantCommonApi;
    @Resource
    private TenantMapper tenantMapper;
    @Resource
    private ScopeDeptMapper scopeDeptMapper;
    @Resource
    private ScopeRoleMapper scopeRoleMapper;
    @Resource
    private ScopeUserMapper scopeUserMapper;

    public ExamPublishScopeOptionsRespVO getScopeOptions() {
        Long currentTenantId = TenantContextHolder.getTenantId();
        List<TenantDO> tenantList = getAllowedTargetTenants();
        List<Long> tenantIds = tenantList.stream()
                .map(TenantDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, String> tenantNameMap = tenantList.stream()
                .collect(Collectors.toMap(TenantDO::getId, TenantDO::getName, (left, right) -> left, LinkedHashMap::new));
        List<ScopeDeptDO> deptList = loadDeptList(tenantIds);
        Map<Long, String> deptNameMap = deptList.stream()
                .collect(Collectors.toMap(ScopeDeptDO::getId, ScopeDeptDO::getName, (left, right) -> left, LinkedHashMap::new));

        ExamPublishScopeOptionsRespVO resp = new ExamPublishScopeOptionsRespVO();
        resp.setCurrentTenantId(currentTenantId);
        resp.setCrossTenantEnabled(tenantIds.size() > 1);
        resp.setTenantList(tenantList.stream().map(item -> {
            ExamPublishScopeOptionsRespVO.TenantOption option = new ExamPublishScopeOptionsRespVO.TenantOption();
            option.setId(item.getId());
            option.setName(item.getName());
            return option;
        }).collect(Collectors.toList()));
        resp.setOrgTree(buildOrgTree(tenantList, deptList));
        resp.setRoleList(loadRoleList(tenantIds).stream().map(item -> {
            ExamPublishScopeOptionsRespVO.RoleOption option = new ExamPublishScopeOptionsRespVO.RoleOption();
            option.setId(item.getId());
            option.setName(item.getName());
            option.setTenantId(item.getTenantId());
            option.setTenantName(tenantNameMap.get(item.getTenantId()));
            return option;
        }).collect(Collectors.toList()));
        resp.setUserList(loadUserList(tenantIds).stream().map(item -> {
            ExamPublishScopeOptionsRespVO.UserOption option = new ExamPublishScopeOptionsRespVO.UserOption();
            option.setId(item.getId());
            option.setUsername(item.getUsername());
            option.setNickname(item.getNickname());
            option.setTenantId(item.getTenantId());
            option.setTenantName(tenantNameMap.get(item.getTenantId()));
            option.setDeptId(item.getDeptId());
            option.setDeptName(deptNameMap.get(item.getDeptId()));
            return option;
        }).collect(Collectors.toList()));
        return resp;
    }

    public String normalizeScopeJson(String publishScopeJson, Long fallbackTenantId) {
        if (!StringUtils.hasText(publishScopeJson)) {
            return null;
        }
        try {
            ExamScopeSupport.PublishScope scope = objectMapper.readValue(publishScopeJson, ExamScopeSupport.PublishScope.class);
            scope.setTenantIds(resolveScopeTenantIds(scope.getTenantIds(), fallbackTenantId));
            return objectMapper.writeValueAsString(scope);
        } catch (Exception ex) {
            throw ServiceExceptionUtil.invalidParamException("发布范围格式错误");
        }
    }

    public List<Long> resolveScopeTenantIds(List<Long> requestedTenantIds, Long fallbackTenantId) {
        Long currentTenantId = fallbackTenantId != null ? fallbackTenantId : TenantContextHolder.getTenantId();
        if (currentTenantId == null) {
            return Collections.emptyList();
        }
        LinkedHashSet<Long> requested = new LinkedHashSet<>();
        if (!CollectionUtils.isEmpty(requestedTenantIds)) {
            requested.addAll(requestedTenantIds.stream().filter(Objects::nonNull).collect(Collectors.toList()));
        }
        if (requested.isEmpty()) {
            requested.add(currentTenantId);
        }
        Set<Long> allowed = new LinkedHashSet<>(getAllowedTargetTenantIds());
        if (!allowed.containsAll(requested)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.FORBIDDEN, "无权选择目标租户");
        }
        return new ArrayList<>(requested);
    }

    public List<Long> getAllowedTargetTenantIds() {
        return getAllowedTargetTenants().stream()
                .map(TenantDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<TenantDO> getAllowedTargetTenants() {
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (currentTenantId == null) {
            return Collections.emptyList();
        }
        TenantDO currentTenant = TenantUtils.executeIgnore(() -> tenantMapper.selectById(currentTenantId));
        if (currentTenant == null || CommonStatusEnum.isDisable(currentTenant.getStatus())) {
            return Collections.emptyList();
        }
        if (!isCrossTenantFeatureEnabled(currentTenantId) || !hasCrossTenantPublishPermission()) {
            return Collections.singletonList(currentTenant);
        }
        if (resolveViewScope(currentTenant) == TenantViewScopeEnum.ALL) {
            return filterEnabledTenants(TenantUtils.executeIgnore(() -> tenantMapper.selectList()));
        }
        Long rootId = currentTenant.getRootId() != null && currentTenant.getRootId() > 0
                ? currentTenant.getRootId()
                : currentTenant.getId();
        List<TenantDO> tenantList = filterEnabledTenants(TenantUtils.executeIgnore(() -> tenantMapper.selectListByRootId(rootId)));
        return CollectionUtils.isEmpty(tenantList) ? Collections.singletonList(currentTenant) : tenantList;
    }

    private List<TenantDO> filterEnabledTenants(List<TenantDO> tenantList) {
        if (CollectionUtils.isEmpty(tenantList)) {
            return Collections.emptyList();
        }
        return tenantList.stream()
                .filter(Objects::nonNull)
                .filter(item -> !CommonStatusEnum.isDisable(item.getStatus()))
                .collect(Collectors.toList());
    }

    private TenantViewScopeEnum resolveViewScope(TenantDO tenant) {
        if (tenant == null) {
            return TenantViewScopeEnum.SELF;
        }
        if (Integer.valueOf(1).equals(tenant.getGlobalView())) {
            return TenantViewScopeEnum.ALL;
        }
        return TenantViewScopeEnum.fromCode(tenant.getViewScope());
    }

    private boolean hasCrossTenantPublishPermission() {
        return securityFrameworkService.hasPermission(CROSS_TENANT_PERMISSION);
    }

    private boolean isCrossTenantFeatureEnabled(Long tenantId) {
        if (tenantId == null) {
            return false;
        }
        return tenantCommonApi.isFeatureCrossTenantEnabled(tenantId, TenantFeatureCodeConstants.HR_EXAM_PUBLISH)
                .getCheckedData();
    }

    private List<ScopeDeptDO> loadDeptList(Collection<Long> tenantIds) {
        return TenantUtils.executeIgnore(() -> scopeDeptMapper.selectListByTenantIds(tenantIds, CommonStatusEnum.ENABLE.getStatus()));
    }

    private List<ScopeRoleDO> loadRoleList(Collection<Long> tenantIds) {
        return TenantUtils.executeIgnore(() -> scopeRoleMapper.selectListByTenantIds(tenantIds, CommonStatusEnum.ENABLE.getStatus()));
    }

    private List<ScopeUserDO> loadUserList(Collection<Long> tenantIds) {
        return TenantUtils.executeIgnore(() -> scopeUserMapper.selectListByTenantIds(tenantIds, CommonStatusEnum.ENABLE.getStatus()));
    }

    private List<ExamPublishScopeOptionsRespVO.OrgTreeNode> buildOrgTree(List<TenantDO> tenantList, List<ScopeDeptDO> deptList) {
        List<ExamPublishScopeOptionsRespVO.OrgTreeNode> result = new ArrayList<>();
        Map<Long, String> tenantNameMap = tenantList.stream()
                .collect(Collectors.toMap(TenantDO::getId, TenantDO::getName, (left, right) -> left, LinkedHashMap::new));
        for (TenantDO tenant : tenantList) {
            ExamPublishScopeOptionsRespVO.OrgTreeNode node = new ExamPublishScopeOptionsRespVO.OrgTreeNode();
            node.setId("t-" + tenant.getId());
            node.setName(tenant.getName());
            if (tenant.getParentId() != null && tenant.getParentId() > 0 && tenantNameMap.containsKey(tenant.getParentId())) {
                node.setParentId("t-" + tenant.getParentId());
            }
            node.setNodeType("tenant");
            node.setTenantId(tenant.getId());
            result.add(node);
        }

        Map<Long, Long> skippedDeptToTenant = new LinkedHashMap<>();
        for (ScopeDeptDO dept : deptList) {
            boolean isRootDept = dept.getParentId() == null || Objects.equals(dept.getParentId(), ScopeDeptDO.PARENT_ID_ROOT);
            if (!isRootDept) {
                continue;
            }
            String tenantName = tenantNameMap.get(dept.getTenantId());
            if (tenantName != null && tenantName.equals(dept.getName())) {
                skippedDeptToTenant.put(dept.getId(), dept.getTenantId());
            }
        }

        for (ScopeDeptDO dept : deptList) {
            if (skippedDeptToTenant.containsKey(dept.getId())) {
                continue;
            }
            boolean isRootDept = dept.getParentId() == null || Objects.equals(dept.getParentId(), ScopeDeptDO.PARENT_ID_ROOT);
            ExamPublishScopeOptionsRespVO.OrgTreeNode node = new ExamPublishScopeOptionsRespVO.OrgTreeNode();
            node.setId("d-" + dept.getId());
            node.setName(dept.getName());
            if (isRootDept) {
                node.setParentId("t-" + dept.getTenantId());
            } else if (skippedDeptToTenant.containsKey(dept.getParentId())) {
                node.setParentId("t-" + skippedDeptToTenant.get(dept.getParentId()));
            } else {
                node.setParentId("d-" + dept.getParentId());
            }
            node.setNodeType("dept");
            node.setTenantId(dept.getTenantId());
            node.setDeptId(dept.getId());
            result.add(node);
        }
        return result;
    }
}
