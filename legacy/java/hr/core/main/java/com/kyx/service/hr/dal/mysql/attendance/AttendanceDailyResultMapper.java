package com.kyx.service.hr.dal.mysql.attendance;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceDailyResultPageReqVO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceDailyResultDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.time.LocalDate;
import java.util.List;

/**
 * 考勤每日结果 Mapper
 */
@Mapper
public interface AttendanceDailyResultMapper extends BaseMapperX<AttendanceDailyResultDO> {

    default AttendanceDailyResultDO selectByUserIdAndDate(Long userId, LocalDate attendanceDate) {
        return selectOne(new LambdaQueryWrapperX<AttendanceDailyResultDO>()
                .eq(AttendanceDailyResultDO::getUserId, userId)
                .eq(AttendanceDailyResultDO::getAttendanceDate, attendanceDate)
                .last("LIMIT 1"));
    }

    default PageResult<AttendanceDailyResultDO> selectPage(AttendanceDailyResultPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO).orderByDesc(AttendanceDailyResultDO::getAttendanceDate)
                .orderByDesc(AttendanceDailyResultDO::getId));
    }

    default List<AttendanceDailyResultDO> selectListByReqVO(AttendanceDailyResultPageReqVO reqVO) {
        return selectList(buildQuery(reqVO).orderByDesc(AttendanceDailyResultDO::getAttendanceDate)
                .orderByDesc(AttendanceDailyResultDO::getId));
    }

    default List<AttendanceDailyResultDO> selectListByMonthAndUserIds(LocalDate startDate, LocalDate endDate,
                                                                      Collection<Long> userIds) {
        return selectList(new LambdaQueryWrapperX<AttendanceDailyResultDO>()
                .betweenIfPresent(AttendanceDailyResultDO::getAttendanceDate, startDate, endDate)
                .in(AttendanceDailyResultDO::getUserId, userIds)
                .orderByAsc(AttendanceDailyResultDO::getUserId)
                .orderByAsc(AttendanceDailyResultDO::getAttendanceDate)
                .orderByDesc(AttendanceDailyResultDO::getId));
    }

    default LambdaQueryWrapperX<AttendanceDailyResultDO> buildQuery(AttendanceDailyResultPageReqVO reqVO) {
        return new LambdaQueryWrapperX<AttendanceDailyResultDO>()
                .eqIfPresent(AttendanceDailyResultDO::getUserId, reqVO.getUserId())
                .eqIfPresent(AttendanceDailyResultDO::getProfileId, reqVO.getProfileId())
                .eqIfPresent(AttendanceDailyResultDO::getResultStatus, reqVO.getResultStatus())
                .betweenIfPresent(AttendanceDailyResultDO::getAttendanceDate, reqVO.getAttendanceDate());
    }

}
