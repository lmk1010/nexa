package com.kyx.service.hr.service.questionnaire;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnairePublishAddAssigneesReqVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnairePublishAddAssigneesRespVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnairePublishBatchRespVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnairePublishPageReqVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnairePublishRespVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnairePublishSaveReqVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnairePublishScopePreviewRespVO;

import javax.validation.Valid;

/**
 * HR 问卷发布 Service 接口
 *
 * @author MK
 */
public interface QuestionnairePublishService {

    /**
     * 创建发布
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createPublish(@Valid QuestionnairePublishSaveReqVO createReqVO);

    /**
     * 更新发布
     *
     * @param updateReqVO 更新信息
     */
    void updatePublish(@Valid QuestionnairePublishSaveReqVO updateReqVO);

    /**
     * 追加当前发布批次的填写人/评价关系
     *
     * @param reqVO 追加信息
     * @return 追加结果
     */
    QuestionnairePublishAddAssigneesRespVO addAssignees(@Valid QuestionnairePublishAddAssigneesReqVO reqVO);

    /**
     * 删除发布
     *
     * @param id 编号
     */
    void deletePublish(Long id);

    /**
     * 获得发布详情
     *
     * @param id 编号
     * @return 发布详情
     */
    QuestionnairePublishRespVO getPublish(Long id);

    /**
     * 获得发布分页
     *
     * @param pageReqVO 分页查询
     * @return 发布分页
     */
    PageResult<QuestionnairePublishRespVO> getPublishPage(QuestionnairePublishPageReqVO pageReqVO);

    /**
     * 发布到期的定时问卷
     */
    void publishScheduled();

    /**
     * 处理过期的问卷发布
     */
    void processDueDeadlines();

    /**
     * 发送到期提醒
     */
    void sendDueReminders();

    /**
     * 发布列表
     *
     * @param status 状态
     * @return 列表
     */
    java.util.List<com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnairePublishRespVO> getPublishList(Integer status);

    /**
     * 当前用户可见发布列表
     *
     * @param userId 用户ID
     * @param status 分配状态
     * @return 列表
     */
    java.util.List<com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnairePublishRespVO> getMyPublishList(Long userId, Integer status);

    /**
     * 查询发布批次列表
     *
     * @param publishId 发布ID
     * @return 批次列表
     */
    java.util.List<QuestionnairePublishBatchRespVO> getPublishBatchList(Long publishId);

    /**
     * 手动结束指定批次
     *
     * @param publishId 发布ID
     * @param batchNo 批次号（为空则结束当前批次）
     */
    void finishBatch(Long publishId, Integer batchNo);

    /**
     * 手动启用指定批次
     *
     * @param publishId 发布ID
     * @param batchNo 批次号（为空则启用当前批次）
     */
    void enableBatch(Long publishId, Integer batchNo);

    /**
     * 预览发布范围覆盖的用户数量
     *
     * @param publishScopeJson 发布范围 JSON
     * @return 去重后的用户数量
     */
    int previewScopeUserCount(String publishScopeJson);

    /**
     * 预览发布范围评价人、被评人明细
     *
     * @param publishScopeJson 发布范围 JSON
     * @return 预览明细
     */
    QuestionnairePublishScopePreviewRespVO previewScopeUsers(String publishScopeJson);

}
