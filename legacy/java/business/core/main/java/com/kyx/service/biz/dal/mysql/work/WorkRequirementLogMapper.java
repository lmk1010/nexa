package com.kyx.service.biz.dal.mysql.work;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.biz.dal.dataobject.work.WorkRequirementLogDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface WorkRequirementLogMapper extends BaseMapperX<WorkRequirementLogDO> {

    default List<WorkRequirementLogDO> selectListByRequirementId(Long requirementId) {
        return selectList(new LambdaQueryWrapperX<WorkRequirementLogDO>()
                .eq(WorkRequirementLogDO::getRequirementId, requirementId)
                .orderByDesc(WorkRequirementLogDO::getCreateTime)
                .orderByDesc(WorkRequirementLogDO::getId));
    }

    default void deleteByRequirementId(Long requirementId) {
        delete(new LambdaQueryWrapperX<WorkRequirementLogDO>()
                .eq(WorkRequirementLogDO::getRequirementId, requirementId));
    }

    default void deleteByRequirementIds(Collection<Long> requirementIds) {
        deleteBatch(WorkRequirementLogDO::getRequirementId, requirementIds);
    }

}
