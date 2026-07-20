package com.kyx.service.hr.controller.admin.training;

import com.kyx.foundation.common.pojo.CommonResult;
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
import com.kyx.service.hr.service.training.TrainingLearningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

import static com.kyx.foundation.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 培训学习")
@RestController
@RequestMapping("/hr/training")
@Validated
public class TrainingLearningController {

    @Resource
    private TrainingLearningService trainingLearningService;

    @GetMapping("/course/page")
    @Operation(summary = "课程分页")
    @PreAuthorize("@ss.hasPermission('hr:training:query')")
    public CommonResult<PageResult<TrainingCourseRespVO>> getCoursePage(@Valid TrainingCoursePageReqVO pageReqVO) {
        return success(trainingLearningService.getCoursePage(pageReqVO));
    }

    @GetMapping("/course/get")
    @Operation(summary = "获得课程详情")
    @Parameter(name = "id", description = "课程ID", required = true)
    @PreAuthorize("@ss.hasPermission('hr:training:query')")
    public CommonResult<TrainingCourseRespVO> getCourse(@RequestParam("id") Long id) {
        return success(trainingLearningService.getCourse(id));
    }

    @PostMapping("/course/create")
    @Operation(summary = "创建课程")
    @PreAuthorize("@ss.hasPermission('hr:training:manage')")
    public CommonResult<Long> createCourse(@Valid @RequestBody TrainingCourseSaveReqVO createReqVO) {
        return success(trainingLearningService.createCourse(createReqVO));
    }

    @PutMapping("/course/update")
    @Operation(summary = "更新课程")
    @PreAuthorize("@ss.hasPermission('hr:training:manage')")
    public CommonResult<Boolean> updateCourse(@Valid @RequestBody TrainingCourseSaveReqVO updateReqVO) {
        trainingLearningService.updateCourse(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/course/delete")
    @Operation(summary = "删除课程")
    @Parameter(name = "id", description = "课程ID", required = true)
    @PreAuthorize("@ss.hasPermission('hr:training:manage')")
    public CommonResult<Boolean> deleteCourse(@RequestParam("id") Long id) {
        trainingLearningService.deleteCourse(id);
        return success(true);
    }

    @GetMapping("/plan/page")
    @Operation(summary = "学习计划分页")
    @PreAuthorize("@ss.hasPermission('hr:training:query')")
    public CommonResult<PageResult<TrainingPlanRespVO>> getPlanPage(@Valid TrainingPlanPageReqVO pageReqVO) {
        return success(trainingLearningService.getPlanPage(pageReqVO));
    }

    @GetMapping("/plan/get")
    @Operation(summary = "获得学习计划详情")
    @Parameter(name = "id", description = "计划ID", required = true)
    @PreAuthorize("@ss.hasPermission('hr:training:query')")
    public CommonResult<TrainingPlanRespVO> getPlan(@RequestParam("id") Long id) {
        return success(trainingLearningService.getPlan(id));
    }

    @PostMapping("/plan/create")
    @Operation(summary = "创建学习计划")
    @PreAuthorize("@ss.hasPermission('hr:training:manage')")
    public CommonResult<Long> createPlan(@Valid @RequestBody TrainingPlanSaveReqVO createReqVO) {
        return success(trainingLearningService.createPlan(createReqVO));
    }

    @PutMapping("/plan/update")
    @Operation(summary = "更新学习计划")
    @PreAuthorize("@ss.hasPermission('hr:training:manage')")
    public CommonResult<Boolean> updatePlan(@Valid @RequestBody TrainingPlanSaveReqVO updateReqVO) {
        trainingLearningService.updatePlan(updateReqVO);
        return success(true);
    }

    @PutMapping("/plan/publish")
    @Operation(summary = "发布学习计划")
    @PreAuthorize("@ss.hasPermission('hr:training:manage')")
    public CommonResult<Boolean> publishPlan(@RequestParam("id") Long id) {
        trainingLearningService.publishPlan(id);
        return success(true);
    }

    @PutMapping("/plan/close")
    @Operation(summary = "关闭学习计划")
    @PreAuthorize("@ss.hasPermission('hr:training:manage')")
    public CommonResult<Boolean> closePlan(@RequestParam("id") Long id) {
        trainingLearningService.closePlan(id);
        return success(true);
    }

    @DeleteMapping("/plan/delete")
    @Operation(summary = "删除学习计划")
    @Parameter(name = "id", description = "计划ID", required = true)
    @PreAuthorize("@ss.hasPermission('hr:training:manage')")
    public CommonResult<Boolean> deletePlan(@RequestParam("id") Long id) {
        trainingLearningService.deletePlan(id);
        return success(true);
    }

    @PostMapping("/plan/enroll")
    @Operation(summary = "报名学习计划")
    @PreAuthorize("@ss.hasAnyPermissions('hr:learning:self,hr:training:query')")
    public CommonResult<Long> enrollPlan(@RequestParam("id") Long id) {
        return success(trainingLearningService.enrollPlan(id));
    }

    @GetMapping("/assignment/page")
    @Operation(summary = "学习任务分页")
    @PreAuthorize("@ss.hasPermission('hr:training:query')")
    public CommonResult<PageResult<TrainingAssignmentRespVO>> getAssignmentPage(@Valid TrainingAssignmentPageReqVO pageReqVO) {
        return success(trainingLearningService.getAssignmentPage(pageReqVO));
    }

    @GetMapping("/assignment/my-page")
    @Operation(summary = "我的学习任务分页")
    @PreAuthorize("@ss.hasAnyPermissions('hr:learning:self,hr:training:query')")
    public CommonResult<PageResult<TrainingAssignmentRespVO>> getMyAssignmentPage(@Valid TrainingAssignmentPageReqVO pageReqVO) {
        pageReqVO.setMine(true);
        return success(trainingLearningService.getAssignmentPage(pageReqVO));
    }

    @PutMapping("/assignment/update")
    @Operation(summary = "更新学习任务")
    @PreAuthorize("@ss.hasPermission('hr:training:manage')")
    public CommonResult<Boolean> updateAssignment(@Valid @RequestBody TrainingAssignmentUpdateReqVO updateReqVO) {
        trainingLearningService.updateAssignment(updateReqVO);
        return success(true);
    }

    @PutMapping("/assignment/complete-my")
    @Operation(summary = "员工更新自己的学习任务")
    @PreAuthorize("@ss.hasAnyPermissions('hr:learning:self,hr:training:query')")
    public CommonResult<Boolean> completeMyAssignment(@Valid @RequestBody TrainingAssignmentUpdateReqVO updateReqVO) {
        trainingLearningService.completeMyAssignment(updateReqVO);
        return success(true);
    }

}
