package com.kyx.service.hr.controller.admin.employee;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeEntryUpdateReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeResignationReqVO;
import com.kyx.service.hr.service.employee.EmployeeEntryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

import static com.kyx.foundation.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 员工入职记录（花名册）")
@RestController
@RequestMapping("/hr/employee/entry")
@Validated
public class EmployeeEntryController {

    @Resource
    private EmployeeEntryService employeeEntryService;

    @PutMapping("/update")
    @Operation(summary = "局部更新员工入职记录")
    @PreAuthorize("@ss.hasPermission('hr:employee:update')")
    public CommonResult<Boolean> updateEmployeeEntry(@Valid @RequestBody EmployeeEntryUpdateReqVO updateReqVO) {
        employeeEntryService.updateEmployeeEntryPartial(updateReqVO);
        return success(true);
    }

    @PostMapping("/resignation")
    @Operation(summary = "办理员工离职")
    @PreAuthorize("@ss.hasPermission('hr:employee:update')")
    public CommonResult<Boolean> processResignation(@Valid @RequestBody EmployeeResignationReqVO reqVO) {
        employeeEntryService.processResignation(reqVO);
        return success(true);
    }
}
