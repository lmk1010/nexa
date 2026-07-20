package com.kyx.service.hr.controller.admin.questionnaire;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireAssignmentBatchCreateReqVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireAssignmentPageReqVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireAssignmentRespVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireAssignmentSaveReqVO;
import com.kyx.service.hr.service.questionnaire.QuestionnaireAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static com.kyx.foundation.common.pojo.CommonResult.success;
import static com.kyx.foundation.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * HR 问卷分配 Controller
 *
 * @author MK
 */
@Tag(name = "管理后台 - HR 问卷分配")
@RestController
@RequestMapping("/hr/questionnaire-assignment")
@Validated
public class QuestionnaireAssignmentController {

    @Resource
    private QuestionnaireAssignmentService assignmentService;

    @PostMapping("/create")
    @Operation(summary = "创建分配")
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:assignment:create')")
    public CommonResult<Long> createAssignment(@Valid @RequestBody QuestionnaireAssignmentSaveReqVO createReqVO) {
        return success(assignmentService.createAssignment(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新分配")
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:assignment:update')")
    public CommonResult<Boolean> updateAssignment(@Valid @RequestBody QuestionnaireAssignmentSaveReqVO updateReqVO) {
        assignmentService.updateAssignment(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除分配")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:assignment:delete')")
    public CommonResult<Boolean> deleteAssignment(@RequestParam("id") Long id) {
        assignmentService.deleteAssignment(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得分配详情")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:assignment:query')")
    public CommonResult<QuestionnaireAssignmentRespVO> getAssignment(@RequestParam("id") Long id) {
        return success(assignmentService.getAssignment(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得分配分页")
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:assignment:query')")
    public CommonResult<PageResult<QuestionnaireAssignmentRespVO>> getAssignmentPage(
            @Valid QuestionnaireAssignmentPageReqVO pageVO) {
        return success(assignmentService.getAssignmentPage(pageVO));
    }

    @GetMapping("/my-page")
    @Operation(summary = "获得我的分配分页")
    @PreAuthorize("@ss.hasAnyPermissions('hr:questionnaire:self,hr:learning:self,hr:questionnaire:assignment:query')")
    public CommonResult<PageResult<QuestionnaireAssignmentRespVO>> getMyAssignmentPage(
            @Valid QuestionnaireAssignmentPageReqVO pageVO) {
        pageVO.setEvaluatorId(getLoginUserId());
        pageVO.setVisibleOnly(true);
        return success(assignmentService.getAssignmentPage(pageVO));
    }

    @PostMapping("/batch-create")
    @Operation(summary = "批量创建分配")
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:assignment:create')")
    public CommonResult<Boolean> batchCreate(@Valid @RequestBody QuestionnaireAssignmentBatchCreateReqVO reqVO) {
        assignmentService.batchCreateAssignments(reqVO.getAssignments());
        return success(true);
    }

}
