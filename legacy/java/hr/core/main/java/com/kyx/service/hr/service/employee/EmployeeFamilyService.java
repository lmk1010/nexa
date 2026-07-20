package com.kyx.service.hr.service.employee;

import com.kyx.service.hr.controller.admin.employee.vo.EmployeeFamilyRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeFamilySaveReqVO;

import javax.validation.Valid;
import java.util.List;

public interface EmployeeFamilyService {

    List<EmployeeFamilyRespVO> getFamilyList(Long profileId);

    Long createFamily(@Valid EmployeeFamilySaveReqVO createReqVO);

    void updateFamily(@Valid EmployeeFamilySaveReqVO updateReqVO);

    void deleteFamily(Long id);
}
