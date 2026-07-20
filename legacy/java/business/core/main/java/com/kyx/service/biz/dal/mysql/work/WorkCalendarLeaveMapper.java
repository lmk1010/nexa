package com.kyx.service.biz.dal.mysql.work;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.biz.dal.dataobject.work.WorkCalendarLeaveDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface WorkCalendarLeaveMapper extends BaseMapperX<WorkCalendarLeaveDO> {

    default List<WorkCalendarLeaveDO> selectCalendarList(Long userId, LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        return selectList(new LambdaQueryWrapperX<WorkCalendarLeaveDO>()
                .eq(WorkCalendarLeaveDO::getUserId, userId)
                .le(WorkCalendarLeaveDO::getStartTime, rangeEnd)
                .ge(WorkCalendarLeaveDO::getEndTime, rangeStart)
                .orderByAsc(WorkCalendarLeaveDO::getStartTime));
    }
}
