package com.kyx.service.hr.controller.admin.employee;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeProfileChangeApplyReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeProfileChangeApproveReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeProfileChangePageReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeProfileChangeRespVO;
import com.kyx.service.hr.service.employee.EmployeeProfileChangeRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

import static com.kyx.foundation.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 员工资料变更")
@RestController
@RequestMapping("/hr/employee/profile-change")
@Validated
public class EmployeeProfileChangeRequestController {

    @Resource
    private EmployeeProfileChangeRequestService employeeProfileChangeRequestService;

    @PostMapping("/apply")
    @Operation(summary = "提交员工资料变更申请")
    @PreAuthorize("@ss.hasPermission('hr:profile-change:apply')")
    public CommonResult<Long> apply(@Valid @RequestBody EmployeeProfileChangeApplyReqVO reqVO) {
        return success(employeeProfileChangeRequestService.apply(reqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "获得员工资料变更分页")
    @PreAuthorize("@ss.hasAnyPermissions('hr:profile-change:query,hr:profile-change:apply')")
    public CommonResult<PageResult<EmployeeProfileChangeRespVO>> getPage(@Valid EmployeeProfileChangePageReqVO pageReqVO) {
        return success(employeeProfileChangeRequestService.getPage(pageReqVO));
    }

    @PostMapping("/approve")
    @Operation(summary = "审批员工资料变更")
    @PreAuthorize("@ss.hasPermission('hr:profile-change:approve')")
    public CommonResult<Boolean> approve(@Valid @RequestBody EmployeeProfileChangeApproveReqVO reqVO) {
        return success(employeeProfileChangeRequestService.approve(reqVO));
    }

    @PostMapping("/cancel")
    @Operation(summary = "撤销员工资料变更申请")
    @PreAuthorize("@ss.hasPermission('hr:profile-change:apply')")
    public CommonResult<Boolean> cancel(@RequestParam("id") Long id) {
        return success(employeeProfileChangeRequestService.cancel(id));
    }

}
