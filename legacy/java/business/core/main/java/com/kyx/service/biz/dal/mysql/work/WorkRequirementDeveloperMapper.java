package com.kyx.service.biz.dal.mysql.work;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.biz.dal.dataobject.work.WorkRequirementDeveloperDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mapper
public interface WorkRequirementDeveloperMapper extends BaseMapperX<WorkRequirementDeveloperDO> {

    default List<WorkRequirementDeveloperDO> selectListByRequirementId(Long requirementId) {
        return selectList(new LambdaQueryWrapperX<WorkRequirementDeveloperDO>()
                .eq(WorkRequirementDeveloperDO::getRequirementId, requirementId)
                .orderByAsc(WorkRequirementDeveloperDO::getId));
    }

    default List<WorkRequirementDeveloperDO> selectListByRequirementIds(Collection<Long> requirementIds) {
        return selectList(new LambdaQueryWrapperX<WorkRequirementDeveloperDO>()
                .inIfPresent(WorkRequirementDeveloperDO::getRequirementId, requirementIds)
                .orderByAsc(WorkRequirementDeveloperDO::getId));
    }

    default List<Long> selectRequirementIdsByUserId(Long userId, Collection<Long> tenantIds) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<WorkRequirementDeveloperDO>()
                .inIfPresent(WorkRequirementDeveloperDO::getTenantId, tenantIds)
                .eq(WorkRequirementDeveloperDO::getUserId, userId))
                .stream()
                .map(WorkRequirementDeveloperDO::getRequirementId)
                .distinct()
                .collect(Collectors.toList());
    }

    default void deleteByRequirementId(Long requirementId) {
        delete(new LambdaQueryWrapperX<WorkRequirementDeveloperDO>()
                .eq(WorkRequirementDeveloperDO::getRequirementId, requirementId));
    }

    default void deleteByRequirementIds(Collection<Long> requirementIds) {
        deleteBatch(WorkRequirementDeveloperDO::getRequirementId, requirementIds);
    }

    @Delete("DELETE FROM business_work_requirement_developer WHERE requirement_id = #{requirementId}")
    void deletePhysicallyByRequirementId(@Param("requirementId") Long requirementId);

}
