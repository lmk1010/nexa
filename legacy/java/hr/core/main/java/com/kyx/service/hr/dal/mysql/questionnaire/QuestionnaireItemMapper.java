package com.kyx.service.hr.dal.mysql.questionnaire;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnaireItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HR 问卷题目 Mapper
 *
 * @author MK
 */
@Mapper
public interface QuestionnaireItemMapper extends BaseMapperX<QuestionnaireItemDO> {

    default List<QuestionnaireItemDO> selectListByQuestionnaireId(Long questionnaireId) {
        return selectList(new LambdaQueryWrapperX<QuestionnaireItemDO>()
                .eq(QuestionnaireItemDO::getQuestionnaireId, questionnaireId)
                .orderByAsc(QuestionnaireItemDO::getSortNo));
    }

    default List<QuestionnaireItemDO> selectListByQuestionnaireIds(List<Long> questionnaireIds) {
        return selectList(new LambdaQueryWrapperX<QuestionnaireItemDO>()
                .in(QuestionnaireItemDO::getQuestionnaireId, questionnaireIds)
                .orderByAsc(QuestionnaireItemDO::getSortNo));
    }

    default void deleteByQuestionnaireId(Long questionnaireId) {
        delete(new LambdaQueryWrapperX<QuestionnaireItemDO>()
                .eq(QuestionnaireItemDO::getQuestionnaireId, questionnaireId));
    }

}
