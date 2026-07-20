package com.kyx.service.biz.dal.mysql.work;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.biz.dal.dataobject.work.WorkCalendarTripDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface WorkCalendarTripMapper extends BaseMapperX<WorkCalendarTripDO> {

    default List<WorkCalendarTripDO> selectCalendarList(Long userId, LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        return selectList(new LambdaQueryWrapperX<WorkCalendarTripDO>()
                .eq(WorkCalendarTripDO::getUserId, userId)
                .le(WorkCalendarTripDO::getStartTime, rangeEnd)
                .ge(WorkCalendarTripDO::getEndTime, rangeStart)
                .orderByAsc(WorkCalendarTripDO::getStartTime));
    }
}
