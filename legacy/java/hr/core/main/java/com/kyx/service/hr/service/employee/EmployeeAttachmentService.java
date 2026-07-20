package com.kyx.service.hr.service.employee;

import com.kyx.service.hr.controller.admin.employee.vo.EmployeeAttachmentRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeAttachmentSaveReqVO;

import javax.validation.Valid;
import java.util.List;

public interface EmployeeAttachmentService {

    List<EmployeeAttachmentRespVO> getAttachmentList(Long profileId);

    Long createAttachment(@Valid EmployeeAttachmentSaveReqVO createReqVO);

    void updateAttachment(@Valid EmployeeAttachmentSaveReqVO updateReqVO);

    void deleteAttachment(Long id);
}
