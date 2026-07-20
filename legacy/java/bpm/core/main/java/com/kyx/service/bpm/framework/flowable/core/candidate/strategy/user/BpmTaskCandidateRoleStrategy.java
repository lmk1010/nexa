package com.kyx.service.bpm.framework.flowable.core.candidate.strategy.user;

import com.kyx.foundation.common.util.string.StrUtils;
import com.kyx.service.bpm.framework.flowable.core.candidate.BpmTaskCandidateStrategy;
import com.kyx.service.bpm.framework.flowable.core.enums.BpmTaskCandidateStrategyEnum;
import com.kyx.service.business.api.permission.PermissionApi;
import com.kyx.service.business.api.permission.RoleApi;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Set;

/**
 * 角色 {@link BpmTaskCandidateStrategy} 实现类
 *
 * @author kyle
 */
@Component
public class BpmTaskCandidateRoleStrategy implements BpmTaskCandidateStrategy {

    @Resource
    private RoleApi roleApi;
    @Resource
    private PermissionApi permissionApi;

    @Override
    public BpmTaskCandidateStrategyEnum getStrategy() {
        return BpmTaskCandidateStrategyEnum.ROLE;
    }

    @Override
    public void validateParam(String param) {
        Set<Long> roleIds = StrUtils.splitToLongSet(param);
        roleApi.validRoleList(roleIds);
    }

    @Override
    public Set<Long> calculateUsers(String param) {
        Set<Long> roleIds = StrUtils.splitToLongSet(param);
        return permissionApi.getUserRoleIdListByRoleIds(roleIds).getCheckedData();
    }

}