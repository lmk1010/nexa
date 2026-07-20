package com.kyx.service.hr.service.attendance;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceCorrectionApplyReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceCorrectionApproveReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceCorrectionPageReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceCorrectionRespVO;

import javax.validation.Valid;

public interface AttendanceCorrectionService {

    Long apply(@Valid AttendanceCorrectionApplyReqVO reqVO);

    PageResult<AttendanceCorrectionRespVO> getPage(AttendanceCorrectionPageReqVO pageReqVO);

    Boolean approve(@Valid AttendanceCorrectionApproveReqVO reqVO);

    Boolean cancel(Long id);

    void updateApprovalStatusByBpmEvent(Long id, String processInstanceId, Integer bpmStatus, Long operatorUserId);

}
