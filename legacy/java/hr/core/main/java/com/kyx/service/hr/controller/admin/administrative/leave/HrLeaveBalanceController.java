package com.kyx.service.hr.controller.admin.administrative.leave;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.administrative.leave.vo.HrLeaveBalanceAdjustReqVO;
import com.kyx.service.hr.controller.admin.administrative.leave.vo.HrLeaveBalancePageReqVO;
import com.kyx.service.hr.controller.admin.administrative.leave.vo.HrLeaveBalanceRecordPageReqVO;
import com.kyx.service.hr.controller.admin.administrative.leave.vo.HrLeaveBalanceRecordRespVO;
import com.kyx.service.hr.controller.admin.administrative.leave.vo.HrLeaveBalanceRespVO;
import com.kyx.service.hr.controller.admin.administrative.leave.vo.HrLeaveTypeRespVO;
import com.kyx.service.hr.controller.admin.administrative.leave.vo.HrLeaveTypeSaveReqVO;
import com.kyx.service.hr.service.administrative.leave.HrLeaveBalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import java.util.List;

import static com.kyx.foundation.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 假期余额")
@RestController
@RequestMapping("/hr/leave")
@Validated
public class HrLeaveBalanceController {

    @Resource
    private HrLeaveBalanceService leaveBalanceService;

    @GetMapping("/type/list")
    @Operation(summary = "获得假期类型列表")
    @PreAuthorize("@ss.hasAnyPermissions('hr:administrative-leave:query','hr:administrative-leave:query-all','hr:leave-type:manage')")
    public CommonResult<List<HrLeaveTypeRespVO>> listLeaveTypes() {
        return success(leaveBalanceService.listLeaveTypes());
    }

    @PostMapping("/type/save")
    @Operation(summary = "保存假期类型")
    @PreAuthorize("@ss.hasPermission('hr:leave-type:manage')")
    public CommonResult<Long> saveLeaveType(@Valid @RequestBody HrLeaveTypeSaveReqVO reqVO) {
        return success(leaveBalanceService.saveLeaveType(reqVO));
    }

    @GetMapping("/balance/page")
    @Operation(summary = "获得假期余额分页")
    @PreAuthorize("@ss.hasAnyPermissions('hr:leave-balance:query','hr:administrative-leave:query-all')")
    public CommonResult<PageResult<HrLeaveBalanceRespVO>> getBalancePage(@Valid HrLeaveBalancePageReqVO pageReqVO) {
        return success(leaveBalanceService.getBalancePage(pageReqVO));
    }

    @GetMapping("/balance/record/page")
    @Operation(summary = "获得假期余额流水分页")
    @PreAuthorize("@ss.hasAnyPermissions('hr:leave-balance:record','hr:leave-balance:query','hr:leave-balance:adjust','hr:administrative-leave:query-all')")
    public CommonResult<PageResult<HrLeaveBalanceRecordRespVO>> getBalanceRecordPage(
            @Valid HrLeaveBalanceRecordPageReqVO pageReqVO) {
        return success(leaveBalanceService.getBalanceRecordPage(pageReqVO));
    }

    @GetMapping("/balance/my")
    @Operation(summary = "获得我的假期余额")
    @Parameter(name = "year", description = "年份", example = "2026")
    @PreAuthorize("@ss.hasPermission('hr:administrative-leave:query')")
    public CommonResult<List<HrLeaveBalanceRespVO>> getMyBalances(
            @RequestParam(value = "year", required = false) Integer year) {
        return success(leaveBalanceService.getMyBalances(year));
    }

    @PostMapping("/balance/adjust")
    @Operation(summary = "调整假期余额")
    @PreAuthorize("@ss.hasPermission('hr:leave-balance:adjust')")
    public CommonResult<Boolean> adjustBalance(@Valid @RequestBody HrLeaveBalanceAdjustReqVO reqVO) {
        return success(leaveBalanceService.adjustBalance(reqVO));
    }

}
