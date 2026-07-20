package com.kyx.service.hr.dal.mysql.questionnaire;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnairePublicLinkDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 问卷公开链接 Mapper
 *
 * @author MK
 */
@Mapper
public interface QuestionnairePublicLinkMapper extends BaseMapperX<QuestionnairePublicLinkDO> {

    default QuestionnairePublicLinkDO selectByToken(String token) {
        return selectOne(QuestionnairePublicLinkDO::getToken, token);
    }

    default List<QuestionnairePublicLinkDO> selectListByQuestionnaireId(Long questionnaireId) {
        return selectList(new LambdaQueryWrapperX<QuestionnairePublicLinkDO>()
                .eq(QuestionnairePublicLinkDO::getQuestionnaireId, questionnaireId));
    }

    default List<QuestionnairePublicLinkDO> selectListByPublishId(Long publishId) {
        return selectList(new LambdaQueryWrapperX<QuestionnairePublicLinkDO>()
                .eq(QuestionnairePublicLinkDO::getPublishId, publishId));
    }

    default int deleteByQuestionnaireId(Long questionnaireId) {
        return delete(new LambdaQueryWrapperX<QuestionnairePublicLinkDO>()
                .eq(QuestionnairePublicLinkDO::getQuestionnaireId, questionnaireId));
    }

    default int deleteByPublishId(Long publishId) {
        return delete(new LambdaQueryWrapperX<QuestionnairePublicLinkDO>()
                .eq(QuestionnairePublicLinkDO::getPublishId, publishId));
    }

}
