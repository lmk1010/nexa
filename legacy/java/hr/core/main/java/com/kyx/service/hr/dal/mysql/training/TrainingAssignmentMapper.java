package com.kyx.service.hr.dal.mysql.training;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.training.vo.TrainingAssignmentPageReqVO;
import com.kyx.service.hr.dal.dataobject.training.TrainingAssignmentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Set;

@Mapper
public interface TrainingAssignmentMapper extends BaseMapperX<TrainingAssignmentDO> {

    default PageResult<TrainingAssignmentDO> selectPage(TrainingAssignmentPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO).orderByDesc(TrainingAssignmentDO::getId));
    }

    default List<TrainingAssignmentDO> selectListByPlanId(Long planId) {
        return selectList(TrainingAssignmentDO::getPlanId, planId);
    }

    default List<TrainingAssignmentDO> selectListByPlanIds(Set<Long> planIds) {
        return selectList(new LambdaQueryWrapperX<TrainingAssignmentDO>()
                .inIfPresent(TrainingAssignmentDO::getPlanId, planIds));
    }

    default List<TrainingAssignmentDO> selectOpenList(Integer limit) {
        LambdaQueryWrapperX<TrainingAssignmentDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.in(TrainingAssignmentDO::getStatus, java.util.Arrays.asList("NOT_STARTED", "IN_PROGRESS"));
        wrapper.orderByDesc(TrainingAssignmentDO::getId);
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        return selectList(wrapper);
    }

    default Long selectCountByPlanIdAndStatus(Long planId, String status) {
        return selectCount(new LambdaQueryWrapperX<TrainingAssignmentDO>()
                .eq(TrainingAssignmentDO::getPlanId, planId)
                .eqIfPresent(TrainingAssignmentDO::getStatus, status));
    }

    default LambdaQueryWrapperX<TrainingAssignmentDO> buildQuery(TrainingAssignmentPageReqVO reqVO) {
        return new LambdaQueryWrapperX<TrainingAssignmentDO>()
                .eqIfPresent(TrainingAssignmentDO::getPlanId, reqVO.getPlanId())
                .eqIfPresent(TrainingAssignmentDO::getCourseId, reqVO.getCourseId())
                .eqIfPresent(TrainingAssignmentDO::getProfileId, reqVO.getProfileId())
                .inIfPresent(TrainingAssignmentDO::getProfileId, reqVO.getProfileIds())
                .eqIfPresent(TrainingAssignmentDO::getExamId, reqVO.getExamId())
                .eqIfPresent(TrainingAssignmentDO::getQuestionnaireId, reqVO.getQuestionnaireId())
                .eqIfPresent(TrainingAssignmentDO::getStatus, reqVO.getStatus())
                .geIfPresent(TrainingAssignmentDO::getRetrainDate, reqVO.getRetrainDateStart())
                .leIfPresent(TrainingAssignmentDO::getRetrainDate, reqVO.getRetrainDateEnd());
    }

}
