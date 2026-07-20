package com.kyx.service.hr.dal.mysql.questionnaire;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnaireTimelineDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 问卷时间线 Mapper
 */
@Mapper
public interface QuestionnaireTimelineMapper extends BaseMapperX<QuestionnaireTimelineDO> {

    default boolean existsByBizTypeAndBizIdAndDate(String bizType, Long bizId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return selectCount(new LambdaQueryWrapperX<QuestionnaireTimelineDO>()
                .eq(QuestionnaireTimelineDO::getBizType, bizType)
                .eq(QuestionnaireTimelineDO::getBizId, bizId)
                .ge(QuestionnaireTimelineDO::getOccurredAt, start)
                .lt(QuestionnaireTimelineDO::getOccurredAt, end)) > 0;
    }

}
