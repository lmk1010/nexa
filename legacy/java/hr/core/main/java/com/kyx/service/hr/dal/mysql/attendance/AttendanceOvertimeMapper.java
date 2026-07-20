package com.kyx.service.hr.dal.mysql.attendance;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceOvertimePageReqVO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceOvertimeDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * Attendance overtime Mapper.
 */
@Mapper
public interface AttendanceOvertimeMapper extends BaseMapperX<AttendanceOvertimeDO> {

    default PageResult<AttendanceOvertimeDO> selectPage(AttendanceOvertimePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AttendanceOvertimeDO>()
                .eqIfPresent(AttendanceOvertimeDO::getUserId, reqVO.getUserId())
                .eqIfPresent(AttendanceOvertimeDO::getProfileId, reqVO.getProfileId())
                .eqIfPresent(AttendanceOvertimeDO::getOvertimeType, reqVO.getOvertimeType())
                .eqIfPresent(AttendanceOvertimeDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(AttendanceOvertimeDO::getOvertimeDate, reqVO.getOvertimeDate())
                .orderByDesc(AttendanceOvertimeDO::getOvertimeDate)
                .orderByDesc(AttendanceOvertimeDO::getId));
    }

}
