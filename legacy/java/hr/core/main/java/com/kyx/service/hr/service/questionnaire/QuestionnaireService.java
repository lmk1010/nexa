package com.kyx.service.hr.service.questionnaire;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnairePageReqVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireRespVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireSaveReqVO;

import javax.validation.Valid;

/**
 * HR 问卷管理 Service 接口
 *
 * @author MK
 */
public interface QuestionnaireService {

    /**
     * 创建问卷
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createQuestionnaire(@Valid QuestionnaireSaveReqVO createReqVO);

    /**
     * 更新问卷
     *
     * @param updateReqVO 更新信息
     */
    void updateQuestionnaire(@Valid QuestionnaireSaveReqVO updateReqVO);

    /**
     * 删除问卷
     *
     * @param id 编号
     */
    void deleteQuestionnaire(Long id);

    /**
     * 获得问卷详情
     *
     * @param id 编号
     * @return 问卷详情
     */
    QuestionnaireRespVO getQuestionnaire(Long id);

    /**
     * 获得当前用户可访问的问卷详情
     *
     * @param id 问卷编号
     * @param assignmentId 分配编号
     * @param publishId 发布编号
     * @return 问卷详情
     */
    QuestionnaireRespVO getAccessibleQuestionnaire(Long id, Long assignmentId, Long publishId);

    /**
     * 获得问卷分页
     *
     * @param pageReqVO 分页查询
     * @return 问卷分页
     */
    PageResult<QuestionnaireRespVO> getQuestionnairePage(QuestionnairePageReqVO pageReqVO);

}
