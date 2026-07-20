package com.kyx.service.hr.service.exam;

import com.kyx.service.hr.controller.admin.exam.vo.ExamAnswerSubmitReqVO;
import com.kyx.service.hr.controller.admin.exam.vo.ExamAttemptRespVO;
import com.kyx.service.hr.controller.admin.exam.vo.ExamPaperRespVO;
import com.kyx.service.hr.controller.admin.exam.vo.ExamReviewRespVO;

import java.util.List;

/**
 * HR 考试作答 Service
 */
public interface ExamAttemptService {

    ExamAttemptRespVO startExam(Long examId, Long publishId, Long userId);

    ExamAttemptRespVO restartExam(Long examId, Long publishId, Long userId);

    Integer resetInProgressAttempts(Long examId, Long publishId, Long operatorUserId);

    ExamPaperRespVO getPaper(Long attemptId, Long userId);

    ExamReviewRespVO getReview(Long examId, Long publishId, Long attemptId, Long userId);

    void saveDraft(ExamAnswerSubmitReqVO submitReqVO, Long userId);

    void submitExam(ExamAnswerSubmitReqVO submitReqVO, Long userId);

    List<ExamAttemptRespVO> getMyAttempts(Long examId, Long publishId, Long userId);

}
