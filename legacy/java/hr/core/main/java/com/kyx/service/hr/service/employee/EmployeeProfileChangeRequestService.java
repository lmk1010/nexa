package com.kyx.service.hr.service.employee;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeProfileChangeApplyReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeProfileChangeApproveReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeProfileChangePageReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeProfileChangeRespVO;

public interface EmployeeProfileChangeRequestService {

    Long apply(EmployeeProfileChangeApplyReqVO reqVO);

    PageResult<EmployeeProfileChangeRespVO> getPage(EmployeeProfileChangePageReqVO pageReqVO);

    Boolean approve(EmployeeProfileChangeApproveReqVO reqVO);

    Boolean cancel(Long id);

    void updateApprovalStatusByBpmEvent(Long id, String processInstanceId, Integer bpmStatus, Long operatorUserId);

}
