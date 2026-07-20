package com.kyx.service.hr.service.questionnaire;

import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnairePublicLinkRespVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnairePublicLinkSaveReqVO;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnairePublicLinkDO;

import java.util.List;

/**
 * 问卷公开链接 Service
 */
public interface QuestionnairePublicLinkService {

    /**
     * 创建公开链接
     */
    Long createPublicLink(QuestionnairePublicLinkSaveReqVO createReqVO);

    /**
     * 更新公开链接
     */
    void updatePublicLink(QuestionnairePublicLinkSaveReqVO updateReqVO);

    /**
     * 删除公开链接
     */
    void deletePublicLink(Long id);

    /**
     * 获取公开链接
     */
    QuestionnairePublicLinkRespVO getPublicLink(Long id);

    /**
     * 根据问卷ID获取公开链接列表
     */
    List<QuestionnairePublicLinkRespVO> getPublicLinksByQuestionnaireId(Long questionnaireId);

    /**
     * 根据发布ID获取公开链接列表
     */
    List<QuestionnairePublicLinkRespVO> getPublicLinksByPublishId(Long publishId);

    /**
     * 根据token获取公开链接（用于公开填写）
     */
    QuestionnairePublicLinkDO getPublicLinkByToken(String token);

    /**
     * 验证链接密码
     */
    boolean verifyPassword(String token, String password);

    /**
     * 增加提交计数
     */
    void incrementSubmitCount(Long linkId);

}
