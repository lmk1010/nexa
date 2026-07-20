package com.kyx.service.hr.controller.admin.employee;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePointsAccountRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePointsAddReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePointsDeductReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePointsRespVO;
import com.kyx.service.hr.service.employee.EmployeePointsService;
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
import static com.kyx.foundation.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static com.kyx.foundation.security.core.util.SecurityFrameworkUtils.getLoginUserNickname;

/**
 * 员工积分 Controller
 *
 * @author MK
 */
@Tag(name = "管理后台 - 员工积分")
@RestController
@RequestMapping("/hr/employee/points")
@Validated
public class EmployeePointsController {

    @Resource
    private EmployeePointsService employeePointsService;

    @GetMapping("/list")
    @Operation(summary = "获得员工积分记录列表")
    @Parameter(name = "profileId", description = "员工档案ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:employee-points:query')")
    public CommonResult<List<EmployeePointsRespVO>> getEmployeePointsList(@RequestParam("profileId") Long profileId) {
        return success(employeePointsService.getEmployeePointsList(profileId));
    }

    @GetMapping("/account")
    @Operation(summary = "获得员工积分账户")
    @Parameter(name = "profileId", description = "员工档案ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:employee-points:query')")
    public CommonResult<EmployeePointsAccountRespVO> getEmployeePointsAccount(@RequestParam("profileId") Long profileId) {
        return success(employeePointsService.getEmployeePointsAccount(profileId));
    }

    @PostMapping("/add")
    @Operation(summary = "增加员工积分")
    @PreAuthorize("@ss.hasPermission('hr:employee-points:add')")
    public CommonResult<Long> addEmployeePoints(@Valid @RequestBody EmployeePointsAddReqVO reqVO) {
        return success(employeePointsService.addEmployeePoints(getLoginUserId(), getLoginUserNickname(), reqVO));
    }

    @PostMapping("/deduct")
    @Operation(summary = "扣减员工积分")
    @PreAuthorize("@ss.hasPermission('hr:employee-points:deduct')")
    public CommonResult<Long> deductEmployeePoints(@Valid @RequestBody EmployeePointsDeductReqVO reqVO) {
        return success(employeePointsService.deductEmployeePoints(getLoginUserId(), getLoginUserNickname(), reqVO));
    }

}
