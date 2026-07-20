package com.kyx.service.bpm.framework.flowable.core.candidate.strategy.user;

import com.kyx.foundation.common.util.string.StrUtils;
import com.kyx.service.bpm.dal.dataobject.definition.BpmUserGroupDO;
import com.kyx.service.bpm.framework.flowable.core.candidate.BpmTaskCandidateStrategy;
import com.kyx.service.bpm.framework.flowable.core.enums.BpmTaskCandidateStrategyEnum;
import com.kyx.service.bpm.service.definition.BpmUserGroupService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.kyx.foundation.common.util.collection.CollectionUtils.convertSetByFlatMap;

/**
 * 用户组 {@link BpmTaskCandidateStrategy} 实现类
 *
 * @author kyle
 */
@Component
public class BpmTaskCandidateGroupStrategy implements BpmTaskCandidateStrategy {

    @Resource
    private BpmUserGroupService userGroupService;

    @Override
    public BpmTaskCandidateStrategyEnum getStrategy() {
        return BpmTaskCandidateStrategyEnum.USER_GROUP;
    }

    @Override
    public void validateParam(String param) {
        Set<Long> groupIds = StrUtils.splitToLongSet(param);
        userGroupService.validUserGroups(groupIds);
    }

    @Override
    public Set<Long> calculateUsers(String param) {
        Set<Long> groupIds = StrUtils.splitToLongSet(param);
        List<BpmUserGroupDO> groups = userGroupService.getUserGroupList(groupIds);
        return convertSetByFlatMap(groups, BpmUserGroupDO::getUserIds, Collection::stream);
    }

}