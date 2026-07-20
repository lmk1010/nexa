package com.kyx.service.hr.controller.admin.employee;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeOperationLogRespVO;
import com.kyx.service.hr.service.employee.EmployeeOperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

import static com.kyx.foundation.common.pojo.CommonResult.success;

/**
 * 员工操作日志 Controller
 *
 * @author MK
 */
@Tag(name = "管理后台 - 员工操作日志")
@RestController
@RequestMapping("/hr/employee/operation-log")
@Validated
public class EmployeeOperationLogController {

    @Resource
    private EmployeeOperationLogService employeeOperationLogService;

    @GetMapping("/list")
    @Operation(summary = "获得员工操作日志列表")
    @Parameter(name = "profileId", description = "员工档案ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:employee-operation-log:query')")
    public CommonResult<List<EmployeeOperationLogRespVO>> getEmployeeOperationLogList(@RequestParam("profileId") Long profileId) {
        return success(employeeOperationLogService.getEmployeeOperationLogList(profileId));
    }

}
