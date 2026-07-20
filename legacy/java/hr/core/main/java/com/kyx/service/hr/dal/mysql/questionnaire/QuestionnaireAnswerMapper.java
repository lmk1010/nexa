package com.kyx.service.hr.dal.mysql.questionnaire;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnaireAnswerDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HR 问卷答案 Mapper
 *
 * @author MK
 */
@Mapper
public interface QuestionnaireAnswerMapper extends BaseMapperX<QuestionnaireAnswerDO> {

    default List<QuestionnaireAnswerDO> selectListByAssignmentId(Long assignmentId) {
        return selectList(new LambdaQueryWrapperX<QuestionnaireAnswerDO>()
                .eq(QuestionnaireAnswerDO::getAssignmentId, assignmentId));
    }

    default List<QuestionnaireAnswerDO> selectListByAssignmentIds(List<Long> assignmentIds) {
        if (assignmentIds == null || assignmentIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<QuestionnaireAnswerDO>()
                .in(QuestionnaireAnswerDO::getAssignmentId, assignmentIds));
    }

    default List<QuestionnaireAnswerDO> selectListByQuestionnaireId(Long questionnaireId) {
        return selectList(new LambdaQueryWrapperX<QuestionnaireAnswerDO>()
                .eq(QuestionnaireAnswerDO::getQuestionnaireId, questionnaireId));
    }

    default List<QuestionnaireAnswerDO> selectListByPublishId(Long publishId) {
        return selectList(new LambdaQueryWrapperX<QuestionnaireAnswerDO>()
                .eq(QuestionnaireAnswerDO::getPublishId, publishId));
    }

    default void deleteByAssignmentId(Long assignmentId) {
        delete(new LambdaQueryWrapperX<QuestionnaireAnswerDO>()
                .eq(QuestionnaireAnswerDO::getAssignmentId, assignmentId));
    }

    default int deleteByAssignmentIds(List<Long> assignmentIds) {
        if (assignmentIds == null || assignmentIds.isEmpty()) {
            return 0;
        }
        return delete(new LambdaQueryWrapperX<QuestionnaireAnswerDO>()
                .in(QuestionnaireAnswerDO::getAssignmentId, assignmentIds));
    }

    default int deleteByPublishId(Long publishId) {
        return delete(new LambdaQueryWrapperX<QuestionnaireAnswerDO>()
                .eq(QuestionnaireAnswerDO::getPublishId, publishId));
    }

    default int deleteByQuestionnaireId(Long questionnaireId) {
        return delete(new LambdaQueryWrapperX<QuestionnaireAnswerDO>()
                .eq(QuestionnaireAnswerDO::getQuestionnaireId, questionnaireId));
    }

}
