package com.kyx.service.hr.service.employee;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeMaterialPageReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeMaterialRenewReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeMaterialReviewReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeMaterialRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeMaterialSaveReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeMaterialSubmitReqVO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeDocumentRequestDO;

public interface EmployeeMaterialService {

    PageResult<EmployeeMaterialRespVO> getPage(EmployeeMaterialPageReqVO pageReqVO);

    Long save(EmployeeMaterialSaveReqVO reqVO);

    Long submit(EmployeeMaterialSubmitReqVO reqVO);

    Boolean review(EmployeeMaterialReviewReqVO reqVO);

    Boolean renew(EmployeeMaterialRenewReqVO reqVO);

    Boolean delete(Long id);

    void archiveDocumentRequestResult(EmployeeDocumentRequestDO request);

}
