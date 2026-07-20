package com.kyx.service.hr.service.exam;

import com.kyx.service.hr.controller.admin.exam.vo.ExamResultRespVO;

import java.util.List;

/**
 * HR 考试结果 Service
 *
 * @author MK
 */
public interface ExamResultService {

    List<ExamResultRespVO> getResultList(Long examId);

    List<ExamResultRespVO> getResultList(Long examId, Long publishId);

    void retryAiGrade(Long attemptId);

    void retryAiGradeBatch(List<Long> attemptIds);

    void pauseAiGrade(Long attemptId);

    void pauseAiGradeBatch(List<Long> attemptIds);

    void updateManualScore(Long answerId, Integer score);

}
