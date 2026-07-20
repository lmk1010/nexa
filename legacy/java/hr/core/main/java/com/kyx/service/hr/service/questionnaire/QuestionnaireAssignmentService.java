package com.kyx.service.hr.service.questionnaire;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireAssignmentPageReqVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireAssignmentRespVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireAssignmentSaveReqVO;

import javax.validation.Valid;

/**
 * HR 问卷分配 Service 接口
 *
 * @author MK
 */
public interface QuestionnaireAssignmentService {

    Long createAssignment(@Valid QuestionnaireAssignmentSaveReqVO createReqVO);

    void updateAssignment(@Valid QuestionnaireAssignmentSaveReqVO updateReqVO);

    void deleteAssignment(Long id);

    QuestionnaireAssignmentRespVO getAssignment(Long id);

    PageResult<QuestionnaireAssignmentRespVO> getAssignmentPage(QuestionnaireAssignmentPageReqVO pageReqVO);

    /**
     * 批量创建分配
     *
     * @param assignments 分配列表
     */
    void batchCreateAssignments(java.util.List<QuestionnaireAssignmentSaveReqVO> assignments);

}
