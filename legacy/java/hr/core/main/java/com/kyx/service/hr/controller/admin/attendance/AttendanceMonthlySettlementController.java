package com.kyx.service.hr.controller.admin.attendance;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceMonthlyConfirmActionReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceMonthlyConfirmDetailRespVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceMonthlyConfirmIssueReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceMonthlyConfirmPageReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceMonthlyConfirmResolveReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceMonthlyConfirmRespVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceMonthlySettlementGenerateReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceMonthlySettlementLockReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceMonthlySettlementPageReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceMonthlySettlementRespVO;
import com.kyx.service.hr.service.attendance.AttendanceMonthlySettlementService;
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

/**
 * Monthly attendance settlement controller.
 */
@Tag(name = "管理后台 - 考勤月度结算")
@RestController
@RequestMapping("/hr/attendance/settlement")
@Validated
public class AttendanceMonthlySettlementController {

    @Resource
    private AttendanceMonthlySettlementService attendanceMonthlySettlementService;

    @PostMapping("/generate")
    @Operation(summary = "生成考勤月结")
    @PreAuthorize("@ss.hasPermission('attendance:settlement:lock')")
    public CommonResult<AttendanceMonthlySettlementRespVO> generate(
            @Valid @RequestBody AttendanceMonthlySettlementGenerateReqVO reqVO) {
        return success(attendanceMonthlySettlementService.generate(reqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "获取考勤月结分页")
    @PreAuthorize("@ss.hasPermission('attendance:settlement:query')")
    public CommonResult<PageResult<AttendanceMonthlySettlementRespVO>> getPage(
            @Valid AttendanceMonthlySettlementPageReqVO pageReqVO) {
        return success(attendanceMonthlySettlementService.getPage(pageReqVO));
    }

    @PostMapping("/lock")
    @Operation(summary = "锁定考勤月结")
    @PreAuthorize("@ss.hasPermission('attendance:settlement:lock')")
    public CommonResult<Boolean> lock(@Valid @RequestBody AttendanceMonthlySettlementLockReqVO reqVO) {
        return success(attendanceMonthlySettlementService.lock(reqVO.getId()));
    }

    @PostMapping("/unlock")
    @Operation(summary = "解锁考勤月结")
    @PreAuthorize("@ss.hasPermission('attendance:settlement:lock')")
    public CommonResult<Boolean> unlock(@Valid @RequestBody AttendanceMonthlySettlementLockReqVO reqVO) {
        return success(attendanceMonthlySettlementService.unlock(reqVO.getId()));
    }

    @GetMapping("/confirm/page")
    @Operation(summary = "获取月度考勤确认分页")
    @PreAuthorize("@ss.hasPermission('attendance:settlement:query')")
    public CommonResult<PageResult<AttendanceMonthlyConfirmRespVO>> getConfirmPage(
            @Valid AttendanceMonthlyConfirmPageReqVO pageReqVO) {
        return success(attendanceMonthlySettlementService.getConfirmPage(pageReqVO));
    }

    @GetMapping("/confirm/detail")
    @Operation(summary = "获取月度考勤确认详情")
    @PreAuthorize("@ss.hasPermission('attendance:settlement:query')")
    public CommonResult<AttendanceMonthlyConfirmDetailRespVO> getConfirmDetail(@RequestParam("id") Long id) {
        return success(attendanceMonthlySettlementService.getConfirmDetail(id));
    }

    @PostMapping("/confirm/resolve")
    @Operation(summary = "处理月度考勤确认异议")
    @PreAuthorize("@ss.hasPermission('attendance:settlement:lock')")
    public CommonResult<Boolean> resolveConfirm(@Valid @RequestBody AttendanceMonthlyConfirmResolveReqVO reqVO) {
        return success(attendanceMonthlySettlementService.resolveConfirm(reqVO));
    }

    @GetMapping("/my-confirm/page")
    @Operation(summary = "获取我的月度考勤确认分页")
    @PreAuthorize("@ss.hasPermission('attendance:clock:query')")
    public CommonResult<PageResult<AttendanceMonthlyConfirmRespVO>> getMyConfirmPage(
            @Valid AttendanceMonthlyConfirmPageReqVO pageReqVO) {
        return success(attendanceMonthlySettlementService.getMyConfirmPage(pageReqVO));
    }

    @GetMapping("/my-confirm/detail")
    @Operation(summary = "获取我的月度考勤确认详情")
    @PreAuthorize("@ss.hasPermission('attendance:clock:query')")
    public CommonResult<AttendanceMonthlyConfirmDetailRespVO> getMyConfirmDetail(@RequestParam("id") Long id) {
        return success(attendanceMonthlySettlementService.getMyConfirmDetail(id));
    }

    @PostMapping("/my-confirm/confirm")
    @Operation(summary = "确认我的月度考勤")
    @PreAuthorize("@ss.hasPermission('attendance:clock:query')")
    public CommonResult<Boolean> confirmMy(@Valid @RequestBody AttendanceMonthlyConfirmActionReqVO reqVO) {
        return success(attendanceMonthlySettlementService.confirmMy(reqVO));
    }

    @PostMapping("/my-confirm/issue")
    @Operation(summary = "提交我的月度考勤异议")
    @PreAuthorize("@ss.hasPermission('attendance:clock:query')")
    public CommonResult<Boolean> issueMy(@Valid @RequestBody AttendanceMonthlyConfirmIssueReqVO reqVO) {
        return success(attendanceMonthlySettlementService.issueMy(reqVO));
    }

}
