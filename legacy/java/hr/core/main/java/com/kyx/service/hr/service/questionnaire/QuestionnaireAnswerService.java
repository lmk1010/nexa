package com.kyx.service.hr.service.questionnaire;

import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireAnswerRespVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireAnswerSubmitReqVO;

import javax.validation.Valid;
import java.util.List;

/**
 * HR 问卷答案 Service 接口
 *
 * @author MK
 */
public interface QuestionnaireAnswerService {

    /**
     * 提交问卷答案
     *
     * @param submitReqVO 提交信息
     */
    void submitAnswers(@Valid QuestionnaireAnswerSubmitReqVO submitReqVO);

    /**
     * 按分配ID获取答卷明细
     *
     * @param assignmentId 分配ID
     * @return 答卷明细
     */
    List<QuestionnaireAnswerRespVO> getAnswersByAssignment(Long assignmentId);

}
