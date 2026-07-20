package com.kyx.service.biz.dal.mysql.todo;

import cn.hutool.core.util.StrUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.biz.controller.admin.todo.vo.TodoPageReqVO;
import com.kyx.service.biz.dal.dataobject.todo.TodoDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TodoMapper extends BaseMapperX<TodoDO> {

    default PageResult<TodoDO> selectPage(TodoPageReqVO reqVO, Long userId) {
        LambdaQueryWrapperX<TodoDO> wrapper = new LambdaQueryWrapperX<TodoDO>()
                .eq(TodoDO::getUserId, userId)
                .eqIfPresent(TodoDO::getStatus, reqVO.getStatus())
                .eqIfPresent(TodoDO::getPriority, reqVO.getPriority())
                .orderByDesc(TodoDO::getCreateTime);
        if (StrUtil.isNotBlank(reqVO.getKeyword())) {
            wrapper.and(query -> query.like(TodoDO::getTitle, reqVO.getKeyword())
                    .or().like(TodoDO::getDescription, reqVO.getKeyword())
                    .or().like(TodoDO::getTags, reqVO.getKeyword()));
        }
        return selectPage(reqVO, wrapper);
    }

    default List<TodoDO> selectCalendarList(Long userId, java.util.Date startDate, java.util.Date endDate, Integer status) {
        return selectList(new LambdaQueryWrapperX<TodoDO>()
                .eq(TodoDO::getUserId, userId)
                .eqIfPresent(TodoDO::getStatus, status)
                .betweenIfPresent(TodoDO::getDueDate, startDate, endDate)
                .orderByAsc(TodoDO::getDueDate));
    }

}
