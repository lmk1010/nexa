package com.kyx.service.hr.controller.admin.employee;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeMaterialPageReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeMaterialRenewReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeMaterialReviewReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeMaterialRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeMaterialSaveReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeMaterialSubmitReqVO;
import com.kyx.service.hr.service.employee.EmployeeMaterialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

import static com.kyx.foundation.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 员工电子材料")
@RestController
@RequestMapping("/hr/employee/material")
@Validated
public class EmployeeMaterialController {

    @Resource
    private EmployeeMaterialService employeeMaterialService;

    @GetMapping("/page")
    @Operation(summary = "获得员工电子材料分页")
    @PreAuthorize("@ss.hasAnyPermissions('hr:employee-material:query,hr:employee-material:self,hr:employee-material:manage')")
    public CommonResult<PageResult<EmployeeMaterialRespVO>> getPage(@Valid EmployeeMaterialPageReqVO pageReqVO) {
        return success(employeeMaterialService.getPage(pageReqVO));
    }

    @PostMapping("/save")
    @Operation(summary = "保存员工电子材料")
    @PreAuthorize("@ss.hasPermission('hr:employee-material:manage')")
    public CommonResult<Long> save(@Valid @RequestBody EmployeeMaterialSaveReqVO reqVO) {
        return success(employeeMaterialService.save(reqVO));
    }

    @PostMapping("/submit")
    @Operation(summary = "员工提交电子材料")
    @PreAuthorize("@ss.hasAnyPermissions('hr:employee-material:self,hr:employee-material:manage')")
    public CommonResult<Long> submit(@Valid @RequestBody EmployeeMaterialSubmitReqVO reqVO) {
        return success(employeeMaterialService.submit(reqVO));
    }

    @PostMapping("/review")
    @Operation(summary = "审核或归档电子材料")
    @PreAuthorize("@ss.hasPermission('hr:employee-material:manage')")
    public CommonResult<Boolean> review(@Valid @RequestBody EmployeeMaterialReviewReqVO reqVO) {
        return success(employeeMaterialService.review(reqVO));
    }

    @PostMapping("/renew")
    @Operation(summary = "续期员工电子材料")
    @PreAuthorize("@ss.hasPermission('hr:employee-material:manage')")
    public CommonResult<Boolean> renew(@Valid @RequestBody EmployeeMaterialRenewReqVO reqVO) {
        return success(employeeMaterialService.renew(reqVO));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除员工电子材料")
    @PreAuthorize("@ss.hasPermission('hr:employee-material:manage')")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        return success(employeeMaterialService.delete(id));
    }

}
