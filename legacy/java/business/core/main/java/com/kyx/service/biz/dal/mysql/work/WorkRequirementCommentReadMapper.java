package com.kyx.service.biz.dal.mysql.work;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.biz.dal.dataobject.work.WorkRequirementCommentReadDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface WorkRequirementCommentReadMapper extends BaseMapperX<WorkRequirementCommentReadDO> {

    default List<WorkRequirementCommentReadDO> selectListByUserIdAndCommentIds(Long userId, Collection<Long> commentIds) {
        return selectList(new LambdaQueryWrapperX<WorkRequirementCommentReadDO>()
                .eq(WorkRequirementCommentReadDO::getUserId, userId)
                .inIfPresent(WorkRequirementCommentReadDO::getCommentId, commentIds));
    }

    default void deleteByCommentIds(Collection<Long> commentIds) {
        delete(new LambdaQueryWrapperX<WorkRequirementCommentReadDO>()
                .inIfPresent(WorkRequirementCommentReadDO::getCommentId, commentIds));
    }

}
