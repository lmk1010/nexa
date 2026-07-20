package com.kyx.service.hr.service.employee;

import com.kyx.service.hr.controller.admin.employee.vo.EmployeeSalaryRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeSalarySaveReqVO;

import javax.validation.Valid;
import java.util.List;

public interface EmployeeSalaryService {

    List<EmployeeSalaryRespVO> getSalaryList(Long profileId);

    Long createSalary(@Valid EmployeeSalarySaveReqVO createReqVO);

    void updateSalary(@Valid EmployeeSalarySaveReqVO updateReqVO);

    void deleteSalary(Long id);
}
