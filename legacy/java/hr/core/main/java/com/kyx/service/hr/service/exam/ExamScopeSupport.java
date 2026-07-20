package com.kyx.service.hr.service.exam;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.hr.dal.dataobject.scope.ScopeDeptDO;
import com.kyx.service.hr.dal.dataobject.scope.ScopeRoleDO;
import com.kyx.service.hr.dal.dataobject.scope.ScopeUserDO;
import com.kyx.service.hr.dal.dataobject.scope.ScopeUserRoleDO;
import com.kyx.service.hr.dal.mysql.scope.ScopeDeptMapper;
import com.kyx.service.hr.dal.mysql.scope.ScopeRoleMapper;
import com.kyx.service.hr.dal.mysql.scope.ScopeUserMapper;
import com.kyx.service.hr.dal.mysql.scope.ScopeUserRoleMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ExamScopeSupport {

    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private ScopeDeptMapper scopeDeptMapper;
    @Resource
    private ScopeRoleMapper scopeRoleMapper;
    @Resource
    private ScopeUserMapper scopeUserMapper;
    @Resource
    private ScopeUserRoleMapper scopeUserRoleMapper;

    public boolean isUserInScope(String publishScopeJson, Long userId) {
        return isUserInScope(publishScopeJson, userId, null);
    }

    public boolean isUserInScope(String publishScopeJson, Long userId, Long tenantId) {
        if (userId == null) {
            return false;
        }
        return resolveUserIds(publishScopeJson, tenantId).contains(userId);
    }

    public Set<Long> resolveUserIds(String publishScopeJson) {
        return resolveUserIds(publishScopeJson, null);
    }

    public Set<Long> resolveUserIds(String publishScopeJson, Long tenantId) {
        if (publishScopeJson == null || publishScopeJson.trim().isEmpty()) {
            return Collections.emptySet();
        }
        try {
            PublishScope scope = objectMapper.readValue(publishScopeJson, PublishScope.class);
            return resolveUserIds(scope, tenantId);
        } catch (Exception ex) {
            log.warn("resolve exam scope users failed, publishScopeJson={}", publishScopeJson, ex);
            return Collections.emptySet();
        }
    }

    private Set<Long> resolveUserIds(PublishScope scope, Long fallbackTenantId) {
        if (scope == null) {
            return Collections.emptySet();
        }
        List<Long> effectiveTenantIds = resolveEffectiveTenantIds(scope.getTenantIds(), fallbackTenantId);
        if (CollectionUtils.isEmpty(effectiveTenantIds)) {
            return Collections.emptySet();
        }

        List<ScopeDeptDO> deptList = TenantUtils.executeIgnore(
                () -> scopeDeptMapper.selectListByTenantIds(effectiveTenantIds, CommonStatusEnum.ENABLE.getStatus()));
        List<ScopeRoleDO> roleList = TenantUtils.executeIgnore(
                () -> scopeRoleMapper.selectListByTenantIds(effectiveTenantIds, CommonStatusEnum.ENABLE.getStatus()));
        List<ScopeUserDO> userList = TenantUtils.executeIgnore(
                () -> scopeUserMapper.selectListByTenantIds(effectiveTenantIds, CommonStatusEnum.ENABLE.getStatus()));

        Map<Long, ScopeUserDO> userMap = userList.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(ScopeUserDO::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        Set<Long> result = new LinkedHashSet<>();

        Set<Long> deptIds = expandDeptIds(scope.getDeptIds(), deptList);
        if (!deptIds.isEmpty()) {
            userList.stream()
                    .filter(item -> item.getDeptId() != null && deptIds.contains(item.getDeptId()))
                    .map(ScopeUserDO::getId)
                    .filter(Objects::nonNull)
                    .forEach(result::add);
        }

        if (!CollectionUtils.isEmpty(scope.getRoleIds())) {
            Set<Long> enabledRoleIds = roleList.stream()
                    .map(ScopeRoleDO::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            Set<Long> selectedRoleIds = scope.getRoleIds().stream()
                    .filter(Objects::nonNull)
                    .filter(enabledRoleIds::contains)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (!selectedRoleIds.isEmpty()) {
                List<ScopeUserRoleDO> userRoleList = TenantUtils.executeIgnore(
                        () -> scopeUserRoleMapper.selectListByRoleIds(selectedRoleIds));
                userRoleList.stream()
                        .map(ScopeUserRoleDO::getUserId)
                        .filter(Objects::nonNull)
                        .filter(userMap::containsKey)
                        .forEach(result::add);
            }
        }

        if (!CollectionUtils.isEmpty(scope.getUserIds())) {
            scope.getUserIds().stream()
                    .filter(Objects::nonNull)
                    .filter(userMap::containsKey)
                    .forEach(result::add);
        }
        return result;
    }

    private List<Long> resolveEffectiveTenantIds(List<Long> tenantIds, Long fallbackTenantId) {
        LinkedHashSet<Long> result = new LinkedHashSet<>();
        if (!CollectionUtils.isEmpty(tenantIds)) {
            tenantIds.stream().filter(Objects::nonNull).forEach(result::add);
        }
        if (result.isEmpty()) {
            Long tenantId = fallbackTenantId != null ? fallbackTenantId : TenantContextHolder.getTenantId();
            if (tenantId != null) {
                result.add(tenantId);
            }
        }
        return new ArrayList<>(result);
    }

    private Set<Long> expandDeptIds(List<Long> selectedDeptIds, List<ScopeDeptDO> deptList) {
        if (CollectionUtils.isEmpty(selectedDeptIds) || CollectionUtils.isEmpty(deptList)) {
            return Collections.emptySet();
        }
        Set<Long> validDeptIds = deptList.stream()
                .map(ScopeDeptDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        Map<Long, List<Long>> childrenMap = new HashMap<>();
        for (ScopeDeptDO dept : deptList) {
            if (dept.getId() == null || dept.getParentId() == null || dept.getParentId() <= 0) {
                continue;
            }
            childrenMap.computeIfAbsent(dept.getParentId(), key -> new ArrayList<>()).add(dept.getId());
        }

        Deque<Long> queue = new ArrayDeque<>();
        selectedDeptIds.stream()
                .filter(Objects::nonNull)
                .filter(validDeptIds::contains)
                .forEach(queue::offer);

        Set<Long> result = new LinkedHashSet<>();
        while (!queue.isEmpty()) {
            Long deptId = queue.poll();
            if (deptId == null || !result.add(deptId)) {
                continue;
            }
            List<Long> children = childrenMap.getOrDefault(deptId, Collections.emptyList());
            for (Long childId : children) {
                if (childId != null) {
                    queue.offer(childId);
                }
            }
        }
        return result;
    }

    @Data
    public static class PublishScope {
        private List<Long> tenantIds;
        private List<Long> deptIds;
        private List<Long> roleIds;
        private List<Long> userIds;
    }
}
