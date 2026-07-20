package com.kyx.service.hr.dal.mysql.attendance;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceExceptionPageReqVO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceExceptionDO;
import lombok.Data;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 考勤异常 Mapper
 */
@Mapper
public interface AttendanceExceptionMapper extends BaseMapperX<AttendanceExceptionDO> {

    @Select("SELECT COALESCE(exception_status, '') AS status, COUNT(1) AS count " +
            "FROM hr_attendance_exception " +
            "WHERE deleted = 0 " +
            "GROUP BY COALESCE(exception_status, '')")
    List<StatusCount> selectStatusCounts();

    default List<AttendanceExceptionDO> selectListByDailyResultId(Long dailyResultId) {
        return selectList(new LambdaQueryWrapperX<AttendanceExceptionDO>()
                .eq(AttendanceExceptionDO::getDailyResultId, dailyResultId)
                .orderByAsc(AttendanceExceptionDO::getId));
    }

    default PageResult<AttendanceExceptionDO> selectPage(AttendanceExceptionPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO)
                .orderByDesc(AttendanceExceptionDO::getAttendanceDate)
                .orderByDesc(AttendanceExceptionDO::getId));
    }

    default List<AttendanceExceptionDO> selectListByReqVO(AttendanceExceptionPageReqVO reqVO) {
        return selectList(buildQuery(reqVO)
                .orderByDesc(AttendanceExceptionDO::getAttendanceDate)
                .orderByDesc(AttendanceExceptionDO::getId));
    }

    default LambdaQueryWrapperX<AttendanceExceptionDO> buildQuery(AttendanceExceptionPageReqVO reqVO) {
        LambdaQueryWrapperX<AttendanceExceptionDO> wrapper = new LambdaQueryWrapperX<AttendanceExceptionDO>()
                .eqIfPresent(AttendanceExceptionDO::getId, reqVO.getId())
                .eqIfPresent(AttendanceExceptionDO::getUserId, reqVO.getUserId())
                .eqIfPresent(AttendanceExceptionDO::getProfileId, reqVO.getProfileId())
                .eqIfPresent(AttendanceExceptionDO::getExceptionType, reqVO.getExceptionType())
                .eqIfPresent(AttendanceExceptionDO::getExceptionStatus, reqVO.getExceptionStatus())
                .eqIfPresent(AttendanceExceptionDO::getHandlerId, reqVO.getHandlerId())
                .betweenIfPresent(AttendanceExceptionDO::getAttendanceDate, reqVO.getAttendanceDate())
                .betweenIfPresent(AttendanceExceptionDO::getHandledTime, reqVO.getHandledTime());
        if (StringUtils.hasText(reqVO.getKeyword())) {
            String keyword = reqVO.getKeyword().trim();
            wrapper.and(query -> query.like(AttendanceExceptionDO::getReason, keyword)
                    .or()
                    .like(AttendanceExceptionDO::getHandleRemark, keyword));
        }
        return wrapper;
    }

    @Data
    class StatusCount {

        private String status;

        private Long count;
    }

}
