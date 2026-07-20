package com.kyx.service.hr.controller.admin.exam;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.service.hr.controller.admin.exam.vo.ExamAnswerSubmitReqVO;
import com.kyx.service.hr.controller.admin.exam.vo.ExamAttemptRespVO;
import com.kyx.service.hr.controller.admin.exam.vo.ExamPaperRespVO;
import com.kyx.service.hr.controller.admin.exam.vo.ExamReviewRespVO;
import com.kyx.service.hr.controller.admin.exam.vo.ExamStartReqVO;
import com.kyx.service.hr.service.exam.ExamAttemptService;
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
import static com.kyx.foundation.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * HR 考试作答 Controller
 */
@Tag(name = "管理后台 - HR 考试作答")
@RestController
@RequestMapping("/hr/exam-attempt")
@Validated
public class ExamAttemptController {

    @Resource
    private ExamAttemptService attemptService;

    @PostMapping("/start")
    @Operation(summary = "开始考试")
    @PreAuthorize("@ss.hasPermission('hr:exam:attempt')")
    public CommonResult<ExamAttemptRespVO> startExam(@Valid @RequestBody ExamStartReqVO reqVO) {
        return success(attemptService.startExam(reqVO.getExamId(), reqVO.getPublishId(), getLoginUserId()));
    }

    @PostMapping("/restart")
    @Operation(summary = "重新开始考试")
    @PreAuthorize("@ss.hasPermission('hr:exam:attempt')")
    public CommonResult<ExamAttemptRespVO> restartExam(@Valid @RequestBody ExamStartReqVO reqVO) {
        return success(attemptService.restartExam(reqVO.getExamId(), reqVO.getPublishId(), getLoginUserId()));
    }

    @PostMapping("/reset-in-progress")
    @Operation(summary = "重置未提交作答")
    @PreAuthorize("@ss.hasPermission('hr:exam:update')")
    public CommonResult<Integer> resetInProgress(@Valid @RequestBody ExamStartReqVO reqVO) {
        return success(attemptService.resetInProgressAttempts(
                reqVO.getExamId(), reqVO.getPublishId(), getLoginUserId()));
    }

    @GetMapping("/paper")
    @Operation(summary = "获得作答试卷")
    @Parameter(name = "attemptId", description = "作答ID", required = true)
    @PreAuthorize("@ss.hasPermission('hr:exam:attempt')")
    public CommonResult<ExamPaperRespVO> getPaper(@RequestParam("attemptId") Long attemptId) {
        return success(attemptService.getPaper(attemptId, getLoginUserId()));
    }

    @GetMapping("/review")
    @Operation(summary = "全员完成后回看答案")
    @PreAuthorize("@ss.hasPermission('hr:exam:attempt')")
    public CommonResult<ExamReviewRespVO> getReview(@RequestParam("examId") Long examId,
                                                    @RequestParam("publishId") Long publishId,
                                                    @RequestParam(value = "attemptId", required = false) Long attemptId) {
        return success(attemptService.getReview(examId, publishId, attemptId, getLoginUserId()));
    }

    @PostMapping("/submit")
    @Operation(summary = "提交考试")
    @PreAuthorize("@ss.hasPermission('hr:exam:attempt')")
    public CommonResult<Boolean> submit(@Valid @RequestBody ExamAnswerSubmitReqVO reqVO) {
        attemptService.submitExam(reqVO, getLoginUserId());
        return success(true);
    }

    @PostMapping("/draft")
    @Operation(summary = "保存考试作答草稿")
    @PreAuthorize("@ss.hasPermission('hr:exam:attempt')")
    public CommonResult<Boolean> saveDraft(@Valid @RequestBody ExamAnswerSubmitReqVO reqVO) {
        attemptService.saveDraft(reqVO, getLoginUserId());
        return success(true);
    }

    @GetMapping("/my-list")
    @Operation(summary = "我的作答记录")
    @PreAuthorize("@ss.hasPermission('hr:exam:attempt')")
    public CommonResult<List<ExamAttemptRespVO>> getMyAttempts(@RequestParam("examId") Long examId,
                                                               @RequestParam(value = "publishId", required = false) Long publishId) {
        return success(attemptService.getMyAttempts(examId, publishId, getLoginUserId()));
    }

}
