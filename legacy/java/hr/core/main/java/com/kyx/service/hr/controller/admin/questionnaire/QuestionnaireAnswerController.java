package com.kyx.service.hr.controller.admin.questionnaire;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireAnswerRespVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireAnswerSubmitReqVO;
import com.kyx.service.hr.service.questionnaire.QuestionnaireAnswerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static com.kyx.foundation.common.pojo.CommonResult.success;

/**
 * HR 问卷答案 Controller
 *
 * @author MK
 */
@Tag(name = "管理后台 - HR 问卷答案")
@RestController
@RequestMapping("/hr/questionnaire-answer")
@Validated
public class QuestionnaireAnswerController {

    @Resource
    private QuestionnaireAnswerService answerService;

    @PostMapping("/submit")
    @Operation(summary = "提交问卷答案")
    public CommonResult<Boolean> submitAnswers(@Valid @RequestBody QuestionnaireAnswerSubmitReqVO submitReqVO) {
        answerService.submitAnswers(submitReqVO);
        return success(true);
    }

    @PostMapping("/accessible-submit")
    @Operation(summary = "当前评价人提交问卷答案")
    public CommonResult<Boolean> submitAccessibleAnswers(@Valid @RequestBody QuestionnaireAnswerSubmitReqVO submitReqVO) {
        answerService.submitAnswers(submitReqVO);
        return success(true);
    }

    @GetMapping("/list-by-assignment")
    @Operation(summary = "按分配ID获取答卷明细")
    @Parameter(name = "assignmentId", description = "分配ID", required = true)
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:assignment:query')")
    public CommonResult<List<QuestionnaireAnswerRespVO>> getAnswersByAssignment(@RequestParam("assignmentId") Long assignmentId) {
        return success(answerService.getAnswersByAssignment(assignmentId));
    }

}
