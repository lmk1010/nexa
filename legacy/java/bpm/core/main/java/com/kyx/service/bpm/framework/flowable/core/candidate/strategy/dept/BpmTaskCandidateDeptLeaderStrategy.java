package com.kyx.service.bpm.framework.flowable.core.candidate.strategy.dept;

import com.kyx.foundation.common.util.string.StrUtils;
import com.kyx.service.bpm.framework.flowable.core.candidate.BpmTaskCandidateStrategy;
import com.kyx.service.bpm.framework.flowable.core.enums.BpmTaskCandidateStrategyEnum;
import com.kyx.service.business.api.dept.DeptApi;
import com.kyx.service.business.api.dept.dto.DeptRespDTO;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;

import static com.kyx.foundation.common.util.collection.CollectionUtils.convertSet;

/**
 * 部门的负责人 {@link BpmTaskCandidateStrategy} 实现类
 *
 * @author kyle
 */
@Component
public class BpmTaskCandidateDeptLeaderStrategy implements BpmTaskCandidateStrategy {

    @Resource
    private DeptApi deptApi;

    @Override
    public BpmTaskCandidateStrategyEnum getStrategy() {
        return BpmTaskCandidateStrategyEnum.DEPT_LEADER;
    }

    @Override
    public void validateParam(String param) {
        Set<Long> deptIds = StrUtils.splitToLongSet(param);
        deptApi.validateDeptList(deptIds).checkError();
    }

    @Override
    public Set<Long> calculateUsers(String param) {
        Set<Long> deptIds = StrUtils.splitToLongSet(param);
        List<DeptRespDTO> depts = deptApi.getDeptList(deptIds).getCheckedData();
        return convertSet(depts, DeptRespDTO::getLeaderUserId);
    }

}