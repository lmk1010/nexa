package com.kyx.service.hr.dal.mysql.questionnaire;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnairePublishPageReqVO;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnairePublishDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * HR 问卷发布配置 Mapper
 *
 * @author MK
 */
@Mapper
public interface QuestionnairePublishMapper extends BaseMapperX<QuestionnairePublishDO> {

    default PageResult<QuestionnairePublishDO> selectPage(QuestionnairePublishPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<QuestionnairePublishDO>()
                .eqIfPresent(QuestionnairePublishDO::getQuestionnaireId, reqVO.getQuestionnaireId())
                .eqIfPresent(QuestionnairePublishDO::getStatus, reqVO.getStatus())
                .orderByDesc(QuestionnairePublishDO::getId));
    }

    default List<QuestionnairePublishDO> selectListByScheduleDue(LocalDateTime now) {
        return selectList(new LambdaQueryWrapperX<QuestionnairePublishDO>()
                .eq(QuestionnairePublishDO::getSendType, 1)
                .eq(QuestionnairePublishDO::getStatus, 0)
                .le(QuestionnairePublishDO::getSendAt, now));
    }

    /**
     * 查询到期的周期性发布（nextPublishTime <= now 且 status=1 且 scheduleType>0）
     */
    default List<QuestionnairePublishDO> selectRecurringDue(LocalDateTime now) {
        return selectList(new LambdaQueryWrapperX<QuestionnairePublishDO>()
                .eq(QuestionnairePublishDO::getStatus, 1)
                .gt(QuestionnairePublishDO::getScheduleType, 0)
                .le(QuestionnairePublishDO::getNextPublishTime, now));
    }

    default List<QuestionnairePublishDO> selectListByStatus(Integer status) {
        return selectList(new LambdaQueryWrapperX<QuestionnairePublishDO>()
                .eqIfPresent(QuestionnairePublishDO::getStatus, status)
                .orderByDesc(QuestionnairePublishDO::getId));
    }

    default List<QuestionnairePublishDO> selectListByQuestionnaireId(Long questionnaireId) {
        return selectList(new LambdaQueryWrapperX<QuestionnairePublishDO>()
                .eq(QuestionnairePublishDO::getQuestionnaireId, questionnaireId));
    }

    default int deleteByQuestionnaireId(Long questionnaireId) {
        return delete(new LambdaQueryWrapperX<QuestionnairePublishDO>()
                .eq(QuestionnairePublishDO::getQuestionnaireId, questionnaireId));
    }

    default List<QuestionnairePublishDO> selectListByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return selectBatchIds(ids);
    }

    default List<QuestionnairePublishDO> selectListByNeedClose(LocalDateTime now) {
        return selectList(new LambdaQueryWrapperX<QuestionnairePublishDO>()
                .eq(QuestionnairePublishDO::getStatus, 1)
                .eq(QuestionnairePublishDO::getScheduleType, 0)
                .isNotNull(QuestionnairePublishDO::getDeadlineAt)
                .le(QuestionnairePublishDO::getDeadlineAt, now));
    }

    default List<QuestionnairePublishDO> selectListByNeedRemind() {
        return selectList(new LambdaQueryWrapperX<QuestionnairePublishDO>()
                .eq(QuestionnairePublishDO::getStatus, 1)
                .isNotNull(QuestionnairePublishDO::getRemindRuleJson)
                .ne(QuestionnairePublishDO::getRemindRuleJson, ""));
    }

}
