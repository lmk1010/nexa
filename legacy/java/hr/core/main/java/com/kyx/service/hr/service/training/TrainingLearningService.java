package com.kyx.service.hr.service.training;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.training.vo.TrainingAssignmentPageReqVO;
import com.kyx.service.hr.controller.admin.training.vo.TrainingAssignmentRespVO;
import com.kyx.service.hr.controller.admin.training.vo.TrainingAssignmentUpdateReqVO;
import com.kyx.service.hr.controller.admin.training.vo.TrainingCoursePageReqVO;
import com.kyx.service.hr.controller.admin.training.vo.TrainingCourseRespVO;
import com.kyx.service.hr.controller.admin.training.vo.TrainingCourseSaveReqVO;
import com.kyx.service.hr.controller.admin.training.vo.TrainingPlanPageReqVO;
import com.kyx.service.hr.controller.admin.training.vo.TrainingPlanRespVO;
import com.kyx.service.hr.controller.admin.training.vo.TrainingPlanSaveReqVO;

import javax.validation.Valid;

public interface TrainingLearningService {

    PageResult<TrainingCourseRespVO> getCoursePage(TrainingCoursePageReqVO pageReqVO);

    TrainingCourseRespVO getCourse(Long id);

    Long createCourse(@Valid TrainingCourseSaveReqVO createReqVO);

    void updateCourse(@Valid TrainingCourseSaveReqVO updateReqVO);

    void deleteCourse(Long id);

    PageResult<TrainingPlanRespVO> getPlanPage(TrainingPlanPageReqVO pageReqVO);

    TrainingPlanRespVO getPlan(Long id);

    Long createPlan(@Valid TrainingPlanSaveReqVO createReqVO);

    void updatePlan(@Valid TrainingPlanSaveReqVO updateReqVO);

    void publishPlan(Long id);

    void closePlan(Long id);

    void deletePlan(Long id);

    Long enrollPlan(Long planId);

    PageResult<TrainingAssignmentRespVO> getAssignmentPage(TrainingAssignmentPageReqVO pageReqVO);

    void updateAssignment(@Valid TrainingAssignmentUpdateReqVO updateReqVO);

    void completeMyAssignment(@Valid TrainingAssignmentUpdateReqVO updateReqVO);

}
