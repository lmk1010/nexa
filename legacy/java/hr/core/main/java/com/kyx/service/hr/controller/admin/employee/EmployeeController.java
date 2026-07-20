package com.kyx.service.hr.controller.admin.employee;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeCreateReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeCurrentRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeOverviewRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePageReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeStatisticsRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeStatisticsTrendRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeUpdateReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeWorkspaceOverviewRespVO;
import com.kyx.service.hr.service.employee.EmployeeService;
import com.kyx.foundation.apilog.core.annotation.ApiAccessLog;
import com.kyx.foundation.excel.core.util.ExcelUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

import static com.kyx.foundation.common.pojo.CommonResult.success;
import static com.kyx.foundation.apilog.core.enums.OperateTypeEnum.EXPORT;

/**
 * 员工花名册 Controller
 *
 * @author MK
 */
@Tag(name = "管理后台 - 员工花名册")
@RestController
@RequestMapping("/hr/employee")
@Validated
public class EmployeeController {

    private static final String EMPLOYEE_ANALYTICS_ACCESS_CHECK =
            "@ss.hasAnyRoles('super_admin', 'tenant_admin', 'system_admin', 'admin', 'administrator', " +
                    "'HROwner', 'hrowner', 'hr_admin', 'hr_manager', 'hr_owner', 'human_resources', 'biz_boss')";

    @Resource
    private EmployeeService employeeService;

    @GetMapping("/get")
    @Operation(summary = "获得员工详情")
    @Parameter(name = "id", description = "员工ID", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('hr:employee:query')")
    public CommonResult<EmployeeRespVO> getEmployee(@RequestParam("id") Long id) {
        EmployeeRespVO employee = employeeService.getEmployee(id);
        return success(employee);
    }

    @GetMapping("/current")
    @Operation(summary = "获得当前登录用户对应员工档案")
    @PreAuthorize("@ss.hasPermission('hr:employee:query')")
    public CommonResult<EmployeeCurrentRespVO> getCurrentEmployee() {
        return success(employeeService.getCurrentEmployee());
    }

    @GetMapping("/page")
    @Operation(summary = "获得员工花名册分页")
    @PreAuthorize("@ss.hasPermission('hr:employee:query')")
    public CommonResult<PageResult<EmployeeRespVO>> getEmployeePage(@Valid EmployeePageReqVO pageReqVO) {
        PageResult<EmployeeRespVO> pageResult = employeeService.getEmployeePage(pageReqVO);
        return success(pageResult);
    }

    @GetMapping("/statistics")
    @Operation(summary = "获得员工统计数据")
    @PreAuthorize(EMPLOYEE_ANALYTICS_ACCESS_CHECK)
    public CommonResult<EmployeeStatisticsRespVO> getEmployeeStatistics() {
        EmployeeStatisticsRespVO statistics = employeeService.getEmployeeStatistics();
        return success(statistics);
    }

    @GetMapping("/statistics-trend")
    @Operation(summary = "获得员工统计趋势")
    @Parameter(name = "months", description = "趋势月份数，默认6，范围3-12", example = "6")
    @PreAuthorize(EMPLOYEE_ANALYTICS_ACCESS_CHECK)
    public CommonResult<EmployeeStatisticsTrendRespVO> getEmployeeStatisticsTrend(
            @RequestParam(value = "months", required = false, defaultValue = "6") Integer months) {
        return success(employeeService.getEmployeeStatisticsTrend(months));
    }

    @GetMapping("/overview")
    @Operation(summary = "获得员工总览数据")
    @PreAuthorize(EMPLOYEE_ANALYTICS_ACCESS_CHECK)
    public CommonResult<EmployeeOverviewRespVO> getEmployeeOverview() {
        EmployeeOverviewRespVO overview = employeeService.getEmployeeOverview();
        return success(overview);
    }

    @GetMapping("/workspace-overview")
    @Operation(summary = "获得工作台员工聚合概览")
    @Parameter(name = "months", description = "趋势月份数，默认6，范围3-12", example = "6")
    @PreAuthorize(EMPLOYEE_ANALYTICS_ACCESS_CHECK)
    public CommonResult<EmployeeWorkspaceOverviewRespVO> getEmployeeWorkspaceOverview(
            @RequestParam(value = "months", required = false, defaultValue = "6") Integer months) {
        return success(employeeService.getEmployeeWorkspaceOverview(months));
    }

    @PostMapping("/create")
    @Operation(summary = "创建员工信息")
    @PreAuthorize("@ss.hasPermission('hr:employee:create')")
    public CommonResult<Long> createEmployee(@Valid @RequestBody EmployeeCreateReqVO createReqVO) {
        return success(employeeService.createEmployee(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新员工信息")
    @PreAuthorize("@ss.hasPermission('hr:employee:update')")
    public CommonResult<Boolean> updateEmployee(@Valid @RequestBody EmployeeUpdateReqVO updateReqVO) {
        employeeService.updateEmployee(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除员工信息")
    @Parameter(name = "id", description = "员工ID", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('hr:employee:delete')")
    public CommonResult<Boolean> deleteEmployee(@RequestParam("id") Long id) {
        employeeService.deleteEmployee(id);
        return success(true);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出员工花名册 Excel")
    @PreAuthorize("@ss.hasPermission('hr:employee:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportEmployeeExcel(@Valid EmployeePageReqVO pageReqVO, HttpServletResponse response)
            throws IOException {
        pageReqVO.setPageSize(10000);
        List<EmployeeRespVO> list = employeeService.getEmployeePage(pageReqVO).getList();
        ExcelUtils.write(response, "员工花名册.xls", "数据", EmployeeRespVO.class, list);
    }
}
