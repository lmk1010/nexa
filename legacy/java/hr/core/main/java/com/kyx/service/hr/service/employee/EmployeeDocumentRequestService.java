package com.kyx.service.hr.service.employee;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeDocumentRequestApplyReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeDocumentRequestHandleReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeDocumentRequestPageReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeDocumentTemplatePreviewRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeDocumentTemplateRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeDocumentRequestRespVO;

import java.io.IOException;
import java.util.List;

public interface EmployeeDocumentRequestService {

    Long apply(EmployeeDocumentRequestApplyReqVO reqVO);

    PageResult<EmployeeDocumentRequestRespVO> getPage(EmployeeDocumentRequestPageReqVO pageReqVO);

    Boolean handle(EmployeeDocumentRequestHandleReqVO reqVO);

    Boolean cancel(Long id);

    List<EmployeeDocumentTemplateRespVO> listTemplates();

    EmployeeDocumentTemplatePreviewRespVO previewTemplate(Long id, String templateCode);

    byte[] exportTemplate(Long id, String templateCode) throws IOException;

}
