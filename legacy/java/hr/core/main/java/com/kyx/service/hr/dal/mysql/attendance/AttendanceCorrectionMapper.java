package com.kyx.service.hr.dal.mysql.attendance;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceCorrectionPageReqVO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceCorrectionDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;

/**
 * Attendance correction Mapper.
 */
@Mapper
public interface AttendanceCorrectionMapper extends BaseMapperX<AttendanceCorrectionDO> {

    default PageResult<AttendanceCorrectionDO> selectPage(AttendanceCorrectionPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AttendanceCorrectionDO>()
                .eqIfPresent(AttendanceCorrectionDO::getUserId, reqVO.getUserId())
                .eqIfPresent(AttendanceCorrectionDO::getProfileId, reqVO.getProfileId())
                .eqIfPresent(AttendanceCorrectionDO::getApplyType, reqVO.getApplyType())
                .eqIfPresent(AttendanceCorrectionDO::getClockType, reqVO.getClockType())
                .eqIfPresent(AttendanceCorrectionDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(AttendanceCorrectionDO::getAttendanceDate, reqVO.getAttendanceDate())
                .orderByDesc(AttendanceCorrectionDO::getAttendanceDate)
                .orderByDesc(AttendanceCorrectionDO::getId));
    }

    default Long selectPendingCount(Long userId, LocalDate attendanceDate, String applyType, String clockType) {
        return selectCount(new LambdaQueryWrapperX<AttendanceCorrectionDO>()
                .eq(AttendanceCorrectionDO::getUserId, userId)
                .eq(AttendanceCorrectionDO::getAttendanceDate, attendanceDate)
                .eq(AttendanceCorrectionDO::getApplyType, applyType)
                .eq(AttendanceCorrectionDO::getClockType, clockType)
                .eq(AttendanceCorrectionDO::getStatus, "PENDING"));
    }

}
