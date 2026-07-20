package com.kyx.service.biz.dal.mysql.work;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.biz.dal.dataobject.work.WorkCalendarMeetingDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface WorkCalendarMeetingMapper extends BaseMapperX<WorkCalendarMeetingDO> {

    default List<WorkCalendarMeetingDO> selectCalendarList(Long userId, LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        return selectList(new LambdaQueryWrapperX<WorkCalendarMeetingDO>()
                .eq(WorkCalendarMeetingDO::getUserId, userId)
                .le(WorkCalendarMeetingDO::getStartTime, rangeEnd)
                .ge(WorkCalendarMeetingDO::getEndTime, rangeStart)
                .orderByAsc(WorkCalendarMeetingDO::getStartTime));
    }
}
