package com.kyx.service.hr.dal.mysql.questionnaire;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnaireOptionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * HR 问卷选项 Mapper
 *
 * @author MK
 */
@Mapper
public interface QuestionnaireOptionMapper extends BaseMapperX<QuestionnaireOptionDO> {

    default List<QuestionnaireOptionDO> selectListByItemIds(Collection<Long> itemIds) {
        return selectList(new LambdaQueryWrapperX<QuestionnaireOptionDO>()
                .in(QuestionnaireOptionDO::getItemId, itemIds)
                .orderByAsc(QuestionnaireOptionDO::getSortNo));
    }

    default void deleteByItemIds(Collection<Long> itemIds) {
        delete(new LambdaQueryWrapperX<QuestionnaireOptionDO>()
                .in(QuestionnaireOptionDO::getItemId, itemIds));
    }

}
