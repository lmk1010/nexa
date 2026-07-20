package com.kyx.service.hr.dal.mysql.questionnaire;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnairePageReqVO;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnaireDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * HR 问卷模板 Mapper
 *
 * @author MK
 */
@Mapper
public interface QuestionnaireMapper extends BaseMapperX<QuestionnaireDO> {

    default PageResult<QuestionnaireDO> selectPage(QuestionnairePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<QuestionnaireDO>()
                .likeIfPresent(QuestionnaireDO::getName, reqVO.getName())
                .eqIfPresent(QuestionnaireDO::getType, reqVO.getType())
                .eqIfPresent(QuestionnaireDO::getStatus, reqVO.getStatus())
                .orderByDesc(QuestionnaireDO::getId));
    }

    default QuestionnaireDO selectByCode(String code) {
        return selectOne(QuestionnaireDO::getCode, code);
    }

}
