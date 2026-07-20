package com.kyx.service.hr.service.employee;

import com.kyx.service.hr.controller.admin.employee.vo.EmployeeEducationRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeEducationSaveReqVO;

import javax.validation.Valid;
import java.util.List;

public interface EmployeeEducationService {

    List<EmployeeEducationRespVO> getEducationList(Long profileId);

    Long createEducation(@Valid EmployeeEducationSaveReqVO createReqVO);

    void updateEducation(@Valid EmployeeEducationSaveReqVO updateReqVO);

    void deleteEducation(Long id);
}
