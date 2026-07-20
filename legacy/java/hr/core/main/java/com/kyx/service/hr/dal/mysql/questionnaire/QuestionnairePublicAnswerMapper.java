package com.kyx.service.hr.dal.mysql.questionnaire;

import com.kyx.foundation.common.pojo.PageParam;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnairePublicAnswerDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;

/**
 * 问卷公开提交答案 Mapper
 *
 * @author MK
 */
@Mapper
public interface QuestionnairePublicAnswerMapper extends BaseMapperX<QuestionnairePublicAnswerDO> {

    default PageResult<QuestionnairePublicAnswerDO> selectPage(PageParam pageParam, Long linkId, String respondentName) {
        return selectPage(pageParam, new LambdaQueryWrapperX<QuestionnairePublicAnswerDO>()
                .eqIfPresent(QuestionnairePublicAnswerDO::getLinkId, linkId)
                .likeIfPresent(QuestionnairePublicAnswerDO::getRespondentName, respondentName)
                .orderByDesc(QuestionnairePublicAnswerDO::getSubmitTime));
    }

    default int deleteByPublishId(Long publishId) {
        return delete(new LambdaQueryWrapperX<QuestionnairePublicAnswerDO>()
                .eq(QuestionnairePublicAnswerDO::getPublishId, publishId));
    }

    default int deleteByQuestionnaireId(Long questionnaireId) {
        return delete(new LambdaQueryWrapperX<QuestionnairePublicAnswerDO>()
                .eq(QuestionnairePublicAnswerDO::getQuestionnaireId, questionnaireId));
    }

    default int deleteByLinkIds(Collection<Long> linkIds) {
        if (linkIds == null || linkIds.isEmpty()) {
            return 0;
        }
        return delete(new LambdaQueryWrapperX<QuestionnairePublicAnswerDO>()
                .in(QuestionnairePublicAnswerDO::getLinkId, linkIds));
    }

}
