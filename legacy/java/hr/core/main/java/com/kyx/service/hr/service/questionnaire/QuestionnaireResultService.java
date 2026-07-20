package com.kyx.service.hr.service.questionnaire;

import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireResultRespVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireItemStatRespVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireResultExportRespVO;

import java.util.List;

/**
 * HR 问卷结果 Service 接口
 *
 * @author MK
 */
public interface QuestionnaireResultService {

    /**
     * 查询问卷结果列表（按发布维度）
     *
     * @param publishId 发布ID
     * @param batchNo 批次号（为空查询全部批次）
     * @return 结果列表
     */
    List<QuestionnaireResultRespVO> getResultList(Long publishId, Integer batchNo);

    /**
     * 导出问卷结果明细（按分配行）
     *
     * @param publishId 发布ID
     * @param batchNo 批次号（为空导出全部批次）
     * @return 导出明细
     */
    List<QuestionnaireResultExportRespVO> getResultExportList(Long publishId, Integer batchNo);

    /**
     * 查询题目统计（按发布维度）
     *
     * @param publishId 发布ID
     * @param questionnaireId 问卷ID（用于获取题目列表）
     * @return 题目统计列表
     */
    List<QuestionnaireItemStatRespVO> getItemStats(Long publishId, Long questionnaireId);

}
