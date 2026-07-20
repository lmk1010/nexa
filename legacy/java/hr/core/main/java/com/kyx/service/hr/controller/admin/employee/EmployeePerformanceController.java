package com.kyx.service.hr.controller.admin.employee;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePerformanceAdvanceReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePerformanceApprovalReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePerformancePageReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePerformanceRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePerformanceSchemePageReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePerformanceSchemeRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePerformanceSchemeSaveReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePerformanceSaveReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePerformanceStatsRespVO;
import com.kyx.service.hr.service.employee.EmployeePerformanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static com.kyx.foundation.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 员工业绩信息")
@RestController
@RequestMapping("/hr/employee/performance")
@Validated
public class EmployeePerformanceController {

    @Resource
    private EmployeePerformanceService employeePerformanceService;

    @GetMapping("/list")
    @Operation(summary = "获得员工业绩信息列表")
    @Parameter(name = "profileId", description = "员工档案ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:employee:query')")
    public CommonResult<List<EmployeePerformanceRespVO>> getPerformanceList(@RequestParam("profileId") Long profileId) {
        return success(employeePerformanceService.getPerformanceList(profileId));
    }

    @GetMapping("/page")
    @Operation(summary = "获得绩效工作台分页")
    @PreAuthorize("@ss.hasAnyPermissions('hr:performance:query,hr:employee:query')")
    public CommonResult<PageResult<EmployeePerformanceRespVO>> getPerformancePage(
            @Valid EmployeePerformancePageReqVO pageReqVO) {
        return success(employeePerformanceService.getPerformancePage(pageReqVO));
    }

    @GetMapping("/stats")
    @Operation(summary = "获得绩效工作台统计")
    @PreAuthorize("@ss.hasAnyPermissions('hr:performance:query,hr:employee:query')")
    public CommonResult<EmployeePerformanceStatsRespVO> getPerformanceStats(
            @Valid EmployeePerformancePageReqVO pageReqVO) {
        return success(employeePerformanceService.getPerformanceStats(pageReqVO));
    }

    @PostMapping("/create")
    @Operation(summary = "创建员工业绩信息")
    @PreAuthorize("@ss.hasAnyPermissions('hr:performance:manage,hr:employee:update')")
    public CommonResult<Long> createPerformance(@Valid @RequestBody EmployeePerformanceSaveReqVO createReqVO) {
        return success(employeePerformanceService.createPerformance(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新员工业绩信息")
    @PreAuthorize("@ss.hasAnyPermissions('hr:performance:manage,hr:employee:update')")
    public CommonResult<Boolean> updatePerformance(@Valid @RequestBody EmployeePerformanceSaveReqVO updateReqVO) {
        employeePerformanceService.updatePerformance(updateReqVO);
        return success(true);
    }

    @PutMapping("/advance")
    @Operation(summary = "快速推进绩效周期状态")
    @PreAuthorize("@ss.hasAnyPermissions('hr:performance:manage,hr:employee:update')")
    public CommonResult<Boolean> advancePerformance(@Valid @RequestBody EmployeePerformanceAdvanceReqVO advanceReqVO) {
        employeePerformanceService.advancePerformance(advanceReqVO);
        return success(true);
    }

    @PutMapping("/submit")
    @Operation(summary = "提交绩效审批")
    @PreAuthorize("@ss.hasAnyPermissions('hr:performance:manage,hr:employee:update')")
    public CommonResult<Boolean> submitPerformance(@Valid @RequestBody EmployeePerformanceApprovalReqVO approvalReqVO) {
        employeePerformanceService.submitPerformance(approvalReqVO);
        return success(true);
    }

    @PutMapping("/approve")
    @Operation(summary = "审批通过绩效记录")
    @PreAuthorize("@ss.hasPermission('hr:performance:manage')")
    public CommonResult<Boolean> approvePerformance(@Valid @RequestBody EmployeePerformanceApprovalReqVO approvalReqVO) {
        employeePerformanceService.approvePerformance(approvalReqVO);
        return success(true);
    }

    @PutMapping("/reject")
    @Operation(summary = "驳回绩效记录")
    @PreAuthorize("@ss.hasPermission('hr:performance:manage')")
    public CommonResult<Boolean> rejectPerformance(@Valid @RequestBody EmployeePerformanceApprovalReqVO approvalReqVO) {
        employeePerformanceService.rejectPerformance(approvalReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除员工业绩信息")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasAnyPermissions('hr:performance:manage,hr:employee:update')")
    public CommonResult<Boolean> deletePerformance(@RequestParam("id") Long id) {
        employeePerformanceService.deletePerformance(id);
        return success(true);
    }

    @GetMapping("/scheme/page")
    @Operation(summary = "获得绩效方案分页")
    @PreAuthorize("@ss.hasPermission('hr:performance:query')")
    public CommonResult<PageResult<EmployeePerformanceSchemeRespVO>> getSchemePage(
            @Valid EmployeePerformanceSchemePageReqVO pageReqVO) {
        return success(employeePerformanceService.getSchemePage(pageReqVO));
    }

    @GetMapping("/scheme/list")
    @Operation(summary = "获得可用绩效方案列表")
    @PreAuthorize("@ss.hasPermission('hr:performance:query')")
    public CommonResult<List<EmployeePerformanceSchemeRespVO>> getActiveSchemeList() {
        return success(employeePerformanceService.getActiveSchemeList());
    }

    @PostMapping("/scheme/save")
    @Operation(summary = "保存绩效方案")
    @PreAuthorize("@ss.hasPermission('hr:performance:manage')")
    public CommonResult<Long> saveScheme(@Valid @RequestBody EmployeePerformanceSchemeSaveReqVO reqVO) {
        return success(employeePerformanceService.saveScheme(reqVO));
    }

    @PostMapping("/scheme/enable")
    @Operation(summary = "启用绩效方案")
    @PreAuthorize("@ss.hasPermission('hr:performance:manage')")
    public CommonResult<Boolean> enableScheme(@RequestParam("id") Long id) {
        return success(employeePerformanceService.enableScheme(id));
    }

    @DeleteMapping("/scheme/delete")
    @Operation(summary = "删除绩效方案")
    @PreAuthorize("@ss.hasPermission('hr:performance:manage')")
    public CommonResult<Boolean> deleteScheme(@RequestParam("id") Long id) {
        employeePerformanceService.deleteScheme(id);
        return success(true);
    }
}
