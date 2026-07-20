package com.kyx.service.hr.dal.mysql.attendance;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceClockRecordPageReqVO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceClockRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 员工打卡记录 Mapper
 *
 * @author MK
 */
@Mapper
public interface AttendanceClockRecordMapper extends BaseMapperX<AttendanceClockRecordDO> {

    default AttendanceClockRecordDO selectByUserIdAndDateAndType(Long userId, LocalDate attendanceDate, String clockType) {
        return selectOne(new LambdaQueryWrapperX<AttendanceClockRecordDO>()
                .eq(AttendanceClockRecordDO::getUserId, userId)
                .eq(AttendanceClockRecordDO::getAttendanceDate, attendanceDate)
                .eq(AttendanceClockRecordDO::getClockType, clockType)
                .orderByDesc(AttendanceClockRecordDO::getClockTime)
                .last("LIMIT 1"));
    }

    default List<AttendanceClockRecordDO> selectListByUserIdAndDate(Long userId, LocalDate attendanceDate) {
        return selectList(new LambdaQueryWrapperX<AttendanceClockRecordDO>()
                .eq(AttendanceClockRecordDO::getUserId, userId)
                .eq(AttendanceClockRecordDO::getAttendanceDate, attendanceDate)
                .orderByAsc(AttendanceClockRecordDO::getClockTime));
    }

    default List<AttendanceClockRecordDO> selectListByUserIdAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return selectList(new LambdaQueryWrapperX<AttendanceClockRecordDO>()
                .eq(AttendanceClockRecordDO::getUserId, userId)
                .between(AttendanceClockRecordDO::getAttendanceDate, startDate, endDate)
                .orderByAsc(AttendanceClockRecordDO::getAttendanceDate)
                .orderByAsc(AttendanceClockRecordDO::getClockTime));
    }

    default AttendanceClockRecordDO selectBySourceRecordId(String sourceRecordId) {
        return selectOne(new LambdaQueryWrapperX<AttendanceClockRecordDO>()
                .eq(AttendanceClockRecordDO::getSourceRecordId, sourceRecordId)
                .last("LIMIT 1"));
    }

    default AttendanceClockRecordDO selectByBizKey(Long userId, String sourceType, LocalDateTime clockTime, String clockType) {
        return selectOne(new LambdaQueryWrapperX<AttendanceClockRecordDO>()
                .eqIfPresent(AttendanceClockRecordDO::getUserId, userId)
                .eqIfPresent(AttendanceClockRecordDO::getSourceType, sourceType)
                .eqIfPresent(AttendanceClockRecordDO::getClockTime, clockTime)
                .eqIfPresent(AttendanceClockRecordDO::getClockType, clockType)
                .orderByDesc(AttendanceClockRecordDO::getId)
                .last("LIMIT 1"));
    }

    default AttendanceClockRecordDO selectByBizTimeKey(Long userId, String sourceType, LocalDateTime clockTime) {
        return selectOne(new LambdaQueryWrapperX<AttendanceClockRecordDO>()
                .eqIfPresent(AttendanceClockRecordDO::getUserId, userId)
                .eqIfPresent(AttendanceClockRecordDO::getSourceType, sourceType)
                .eqIfPresent(AttendanceClockRecordDO::getClockTime, clockTime)
                .orderByDesc(AttendanceClockRecordDO::getId)
                .last("LIMIT 1"));
    }

    default PageResult<AttendanceClockRecordDO> selectPage(AttendanceClockRecordPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AttendanceClockRecordDO>()
                .eqIfPresent(AttendanceClockRecordDO::getUserId, reqVO.getUserId())
                .eqIfPresent(AttendanceClockRecordDO::getProfileId, reqVO.getProfileId())
                .eqIfPresent(AttendanceClockRecordDO::getClockType, reqVO.getClockType())
                .eqIfPresent(AttendanceClockRecordDO::getSourceType, reqVO.getSourceType())
                .betweenIfPresent(AttendanceClockRecordDO::getAttendanceDate, reqVO.getAttendanceDate())
                .orderByDesc(AttendanceClockRecordDO::getClockTime));
    }

    default List<AttendanceClockRecordDO> selectListByReqVO(AttendanceClockRecordPageReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<AttendanceClockRecordDO>()
                .eqIfPresent(AttendanceClockRecordDO::getUserId, reqVO.getUserId())
                .eqIfPresent(AttendanceClockRecordDO::getProfileId, reqVO.getProfileId())
                .eqIfPresent(AttendanceClockRecordDO::getClockType, reqVO.getClockType())
                .eqIfPresent(AttendanceClockRecordDO::getSourceType, reqVO.getSourceType())
                .betweenIfPresent(AttendanceClockRecordDO::getAttendanceDate, reqVO.getAttendanceDate())
                .orderByDesc(AttendanceClockRecordDO::getClockTime));
    }

}
