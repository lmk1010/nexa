package com.kyx.service.hr.controller.admin.employee;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeAttendanceStatRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeAttendanceStatSaveReqVO;
import com.kyx.service.hr.service.employee.EmployeeAttendanceStatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

import static com.kyx.foundation.common.pojo.CommonResult.success;

/**
 * 员工考勤统计 Controller
 *
 * @author MK
 */
@Tag(name = "管理后台 - 员工考勤统计")
@RestController
@RequestMapping("/hr/employee/attendance-stat")
@Validated
public class EmployeeAttendanceStatController {

    @Resource
    private EmployeeAttendanceStatService employeeAttendanceStatService;

    @GetMapping
    @Operation(summary = "获得员工月度考勤统计")
    @Parameter(name = "profileId", description = "员工档案ID", required = true, example = "1024")
    @Parameter(name = "year", description = "统计年份", required = true, example = "2025")
    @Parameter(name = "month", description = "统计月份", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('hr:employee-attendance-stat:query')")
    public CommonResult<EmployeeAttendanceStatRespVO> getEmployeeAttendanceStat(@RequestParam("profileId") Long profileId,
                                                                                @RequestParam("year") Integer year,
                                                                                @RequestParam("month") Integer month) {
        return success(employeeAttendanceStatService.getEmployeeAttendanceStat(profileId, year, month));
    }

    @PostMapping("/create")
    @Operation(summary = "创建员工月度考勤统计")
    @PreAuthorize("@ss.hasPermission('hr:employee:update')")
    public CommonResult<Long> createEmployeeAttendanceStat(@Valid @RequestBody EmployeeAttendanceStatSaveReqVO createReqVO) {
        return success(employeeAttendanceStatService.createEmployeeAttendanceStat(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新员工月度考勤统计")
    @PreAuthorize("@ss.hasPermission('hr:employee:update')")
    public CommonResult<Boolean> updateEmployeeAttendanceStat(@Valid @RequestBody EmployeeAttendanceStatSaveReqVO updateReqVO) {
        employeeAttendanceStatService.updateEmployeeAttendanceStat(updateReqVO);
        return success(true);
    }
}
