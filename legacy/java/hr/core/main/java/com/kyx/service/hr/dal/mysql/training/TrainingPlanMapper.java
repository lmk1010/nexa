package com.kyx.service.hr.dal.mysql.training;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.training.vo.TrainingPlanPageReqVO;
import com.kyx.service.hr.dal.dataobject.training.TrainingPlanDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TrainingPlanMapper extends BaseMapperX<TrainingPlanDO> {

    default PageResult<TrainingPlanDO> selectPage(TrainingPlanPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO)
                .orderByDesc(TrainingPlanDO::getStartDate)
                .orderByDesc(TrainingPlanDO::getId));
    }

    default LambdaQueryWrapperX<TrainingPlanDO> buildQuery(TrainingPlanPageReqVO reqVO) {
        return new LambdaQueryWrapperX<TrainingPlanDO>()
                .eqIfPresent(TrainingPlanDO::getCourseId, reqVO.getCourseId())
                .inIfPresent(TrainingPlanDO::getCourseId, reqVO.getCourseIds())
                .eqIfPresent(TrainingPlanDO::getExamId, reqVO.getExamId())
                .eqIfPresent(TrainingPlanDO::getQuestionnaireId, reqVO.getQuestionnaireId())
                .likeIfPresent(TrainingPlanDO::getPlanName, reqVO.getPlanName())
                .eqIfPresent(TrainingPlanDO::getStatus, reqVO.getStatus())
                .geIfPresent(TrainingPlanDO::getStartDate, reqVO.getStartDateStart())
                .leIfPresent(TrainingPlanDO::getStartDate, reqVO.getStartDateEnd())
                .geIfPresent(TrainingPlanDO::getEndDate, reqVO.getEndDateStart())
                .leIfPresent(TrainingPlanDO::getEndDate, reqVO.getEndDateEnd());
    }

    default List<TrainingPlanDO> selectPublishedList(Integer limit) {
        LambdaQueryWrapperX<TrainingPlanDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(TrainingPlanDO::getStatus, "PUBLISHED");
        wrapper.orderByAsc(TrainingPlanDO::getEndDate);
        wrapper.orderByDesc(TrainingPlanDO::getId);
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        return selectList(wrapper);
    }

}
