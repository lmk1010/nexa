package com.kyx.service.hr.service.attendance;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceCalculateReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceDailyResultPageReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceDailyResultRespVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceDailyResultSummaryRespVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceExceptionBatchResolveReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceExceptionPageReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceExceptionResolveReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceExceptionRespVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceExceptionSummaryRespVO;

import javax.validation.Valid;

/**
 * 考勤结果与异常 Service
 */
public interface AttendanceResultService {

    Integer calculateDay(@Valid AttendanceCalculateReqVO reqVO);

    Integer calculateMonth(@Valid AttendanceCalculateReqVO reqVO);

    PageResult<AttendanceDailyResultRespVO> getDailyResultPage(AttendanceDailyResultPageReqVO pageReqVO);

    AttendanceDailyResultSummaryRespVO getDailyResultSummary(AttendanceDailyResultPageReqVO pageReqVO);

    PageResult<AttendanceExceptionRespVO> getExceptionPage(AttendanceExceptionPageReqVO pageReqVO);

    AttendanceExceptionSummaryRespVO getExceptionSummary(AttendanceExceptionPageReqVO pageReqVO);

    Boolean resolveException(@Valid AttendanceExceptionResolveReqVO reqVO);

    Integer batchResolveException(@Valid AttendanceExceptionBatchResolveReqVO reqVO);

}
