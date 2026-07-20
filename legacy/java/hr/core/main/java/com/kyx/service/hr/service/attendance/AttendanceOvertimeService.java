package com.kyx.service.hr.service.attendance;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceOvertimeApplyReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceOvertimeApproveReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceOvertimePageReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceOvertimeRespVO;

import javax.validation.Valid;

public interface AttendanceOvertimeService {

    Long apply(@Valid AttendanceOvertimeApplyReqVO reqVO);

    PageResult<AttendanceOvertimeRespVO> getPage(AttendanceOvertimePageReqVO pageReqVO);

    Boolean approve(@Valid AttendanceOvertimeApproveReqVO reqVO);

    Boolean cancel(Long id);

    void updateApprovalStatusByBpmEvent(Long id, String processInstanceId, Integer bpmStatus, Long operatorUserId);

}
