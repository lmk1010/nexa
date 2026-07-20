package com.kyx.service.biz.service.work;

import cn.hutool.core.collection.CollUtil;
import com.kyx.foundation.common.biz.system.tenant.TenantCommonApi;
import com.kyx.foundation.security.core.service.SecurityFrameworkService;
import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.service.business.enums.permission.RoleCodeEnum;
import com.kyx.service.biz.controller.admin.work.vo.WorkRequirementScopeOptionsRespVO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.service.biz.enums.ErrorCodeConstants.WORK_REQUIREMENT_FORBIDDEN;

@Service
public class WorkRequirementTenantScopeService {

    private static final String FEATURE_CODE = "work.requirement";
    public static final String PERMISSION_QUERY_ALL = "work:requirement:query-all";
    public static final String PERMISSION_CROSS_TENANT = "work:requirement:cross-tenant";
    private static final String ROLE_SYSTEM_ADMIN = "system_admin";
    private static final String ROLE_BIZ_BOSS = "biz_boss";

    @Resource
    private TenantCommonApi tenantCommonApi;
    @Resource
    private SecurityFrameworkService securityFrameworkService;

    public WorkRequirementScopeOptionsRespVO getScopeOptions() {
        Long currentTenantId = TenantContextHolder.getRequiredTenantId();
        WorkRequirementScopeOptionsRespVO respVO = new WorkRequirementScopeOptionsRespVO();
        respVO.setCurrentTenantId(currentTenantId);
        respVO.setCrossTenantEnabled(isCrossTenantSelectionEnabled(currentTenantId));
        respVO.setQueryAllEnabled(canQueryAllRequirements());
        respVO.setSelectableTenantIds(getSelectableTenantIds());
        return respVO;
    }

    public List<Long> getSelectableTenantIds() {
        Long currentTenantId = TenantContextHolder.getRequiredTenantId();
        if (!isCrossTenantSelectionEnabled(currentTenantId)) {
            return Collections.singletonList(currentTenantId);
        }
        return normalizeTenantIds(tenantCommonApi.getCollaborationTenantIds(currentTenantId).getCheckedData(), currentTenantId);
    }

    public List<Long> getParticipantSearchTenantIds() {
        Long currentTenantId = TenantContextHolder.getRequiredTenantId();
        return normalizeTenantIds(tenantCommonApi.getCollaborationTenantIds(currentTenantId).getCheckedData(), currentTenantId);
    }

    public List<Long> getQueryAllTenantIds() {
        Long currentTenantId = TenantContextHolder.getRequiredTenantId();
        if (!canQueryAllRequirements()) {
            return Collections.singletonList(currentTenantId);
        }
        if (!isCrossTenantSelectionEnabled(currentTenantId)) {
            return Collections.singletonList(currentTenantId);
        }
        return normalizeTenantIds(tenantCommonApi.getCollaborationTenantIds(currentTenantId).getCheckedData(), currentTenantId);
    }

    public Long resolveAssigneeTenantId(Long assigneeUserId, Long requestedTenantId) {
        if (assigneeUserId == null) {
            return null;
        }
        Long currentTenantId = TenantContextHolder.getRequiredTenantId();
        List<Long> selectableTenantIds = getSelectableTenantIds();
        if (requestedTenantId != null
                && selectableTenantIds.contains(requestedTenantId)
                && hasUserTenantAccess(assigneeUserId, requestedTenantId)) {
            return requestedTenantId;
        }
        if (selectableTenantIds.contains(currentTenantId) && hasUserTenantAccess(assigneeUserId, currentTenantId)) {
            return currentTenantId;
        }
        for (Long tenantId : selectableTenantIds) {
            if (hasUserTenantAccess(assigneeUserId, tenantId)) {
                return tenantId;
            }
        }
        throw exception(WORK_REQUIREMENT_FORBIDDEN);
    }

    public boolean canQueryAllRequirements() {
        return securityFrameworkService.hasPermission(PERMISSION_QUERY_ALL)
                || securityFrameworkService.hasAnyRoles(
                RoleCodeEnum.SUPER_ADMIN.getCode(),
                RoleCodeEnum.TENANT_ADMIN.getCode(),
                ROLE_SYSTEM_ADMIN,
                ROLE_BIZ_BOSS);
    }

    public boolean isCrossTenantSelectionEnabled() {
        Long currentTenantId = TenantContextHolder.getRequiredTenantId();
        return isCrossTenantSelectionEnabled(currentTenantId);
    }

    private boolean isCrossTenantSelectionEnabled(Long currentTenantId) {
        return currentTenantId != null
                && securityFrameworkService.hasPermission(PERMISSION_CROSS_TENANT)
                && Boolean.TRUE.equals(tenantCommonApi.isFeatureCrossTenantEnabled(
                currentTenantId, FEATURE_CODE).getCheckedData());
    }

    private boolean hasUserTenantAccess(Long userId, Long tenantId) {
        return userId != null
                && tenantId != null
                && Boolean.TRUE.equals(tenantCommonApi.checkUserTenantAccess(userId, tenantId).getCheckedData());
    }

    private List<Long> normalizeTenantIds(List<Long> tenantIds, Long fallbackTenantId) {
        LinkedHashSet<Long> normalized = new LinkedHashSet<>();
        if (CollUtil.isNotEmpty(tenantIds)) {
            for (Long tenantId : tenantIds) {
                if (tenantId != null) {
                    normalized.add(tenantId);
                }
            }
        }
        if (normalized.isEmpty() && fallbackTenantId != null) {
            normalized.add(fallbackTenantId);
        }
        List<Long> result = new ArrayList<>(normalized);
        result.removeIf(Objects::isNull);
        return result;
    }

}
