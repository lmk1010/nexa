package com.kyx.service.hr.controller.admin.employee;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeGrowthLogRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeGrowthLogSaveReqVO;
import com.kyx.service.hr.service.employee.EmployeeGrowthLogService;
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

/**
 * 员工成长记录 Controller
 *
 * @author MK
 */
@Tag(name = "管理后台 - 员工成长记录")
@RestController
@RequestMapping("/hr/employee/growth-log")
@Validated
public class EmployeeGrowthLogController {

    @Resource
    private EmployeeGrowthLogService employeeGrowthLogService;

    @GetMapping("/list")
    @Operation(summary = "获得员工成长记录列表")
    @Parameter(name = "profileId", description = "员工档案ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:employee-growth-log:query')")
    public CommonResult<List<EmployeeGrowthLogRespVO>> getEmployeeGrowthLogList(@RequestParam("profileId") Long profileId) {
        return success(employeeGrowthLogService.getEmployeeGrowthLogList(profileId));
    }

    @PostMapping("/create")
    @Operation(summary = "创建员工成长记录")
    @PreAuthorize("@ss.hasPermission('hr:employee-growth-log:create')")
    public CommonResult<Long> createEmployeeGrowthLog(@Valid @RequestBody EmployeeGrowthLogSaveReqVO createReqVO) {
        return success(employeeGrowthLogService.createEmployeeGrowthLog(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新员工成长记录")
    @PreAuthorize("@ss.hasPermission('hr:employee-growth-log:update')")
    public CommonResult<Boolean> updateEmployeeGrowthLog(@Valid @RequestBody EmployeeGrowthLogSaveReqVO updateReqVO) {
        employeeGrowthLogService.updateEmployeeGrowthLog(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除员工成长记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:employee-growth-log:delete')")
    public CommonResult<Boolean> deleteEmployeeGrowthLog(@RequestParam("id") Long id) {
        employeeGrowthLogService.deleteEmployeeGrowthLog(id);
        return success(true);
    }

}
