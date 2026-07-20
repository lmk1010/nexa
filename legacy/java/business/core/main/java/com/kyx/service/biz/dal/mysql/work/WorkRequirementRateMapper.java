package com.kyx.service.biz.dal.mysql.work;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.biz.dal.dataobject.work.WorkRequirementRateDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface WorkRequirementRateMapper extends BaseMapperX<WorkRequirementRateDO> {

    default List<WorkRequirementRateDO> selectListByRequirementId(Long requirementId) {
        return selectList(new LambdaQueryWrapperX<WorkRequirementRateDO>()
                .eq(WorkRequirementRateDO::getRequirementId, requirementId)
                .orderByDesc(WorkRequirementRateDO::getCreateTime)
                .orderByDesc(WorkRequirementRateDO::getId));
    }

    default WorkRequirementRateDO selectOneByUnique(Long requirementId, Long raterUserId, Long targetUserId) {
        return selectOne(new LambdaQueryWrapperX<WorkRequirementRateDO>()
                .eq(WorkRequirementRateDO::getRequirementId, requirementId)
                .eq(WorkRequirementRateDO::getRaterUserId, raterUserId)
                .eq(WorkRequirementRateDO::getTargetUserId, targetUserId));
    }

    default void deleteByRequirementId(Long requirementId) {
        delete(new LambdaQueryWrapperX<WorkRequirementRateDO>()
                .eq(WorkRequirementRateDO::getRequirementId, requirementId));
    }

    default void deleteByRequirementIds(Collection<Long> requirementIds) {
        deleteBatch(WorkRequirementRateDO::getRequirementId, requirementIds);
    }

}
