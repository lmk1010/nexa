package com.kyx.service.hr.controller.admin.attendance;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceCalculateReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceDailyResultPageReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceDailyResultRespVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceDailyResultSummaryRespVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceExceptionBatchResolveReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceExceptionPageReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceExceptionResolveReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceExceptionRespVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceExceptionSummaryRespVO;
import com.kyx.service.hr.service.attendance.AttendanceResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

import static com.kyx.foundation.common.pojo.CommonResult.success;

/**
 * 考勤结果与异常 Controller
 */
@Tag(name = "管理后台 - 考勤结果与异常")
@RestController
@RequestMapping("/hr/attendance")
@Validated
public class AttendanceResultController {

    @Resource
    private AttendanceResultService attendanceResultService;

    @PostMapping("/calculate/day")
    @Operation(summary = "计算单日考勤结果")
    @PreAuthorize("@ss.hasPermission('attendance:exception:handle')")
    public CommonResult<Integer> calculateDay(@Valid @RequestBody AttendanceCalculateReqVO reqVO) {
        return success(attendanceResultService.calculateDay(reqVO));
    }

    @PostMapping("/calculate/month")
    @Operation(summary = "计算月度考勤结果")
    @PreAuthorize("@ss.hasPermission('attendance:exception:handle')")
    public CommonResult<Integer> calculateMonth(@Valid @RequestBody AttendanceCalculateReqVO reqVO) {
        return success(attendanceResultService.calculateMonth(reqVO));
    }

    @GetMapping("/daily-result/page")
    @Operation(summary = "获取考勤每日结果分页")
    @PreAuthorize("@ss.hasPermission('attendance:clock:query')")
    public CommonResult<PageResult<AttendanceDailyResultRespVO>> getDailyResultPage(
            @Valid AttendanceDailyResultPageReqVO pageReqVO) {
        return success(attendanceResultService.getDailyResultPage(pageReqVO));
    }

    @GetMapping("/daily-result/summary")
    @Operation(summary = "获取考勤每日结果统计")
    @PreAuthorize("@ss.hasPermission('attendance:clock:query')")
    public CommonResult<AttendanceDailyResultSummaryRespVO> getDailyResultSummary(
            @Valid AttendanceDailyResultPageReqVO pageReqVO) {
        return success(attendanceResultService.getDailyResultSummary(pageReqVO));
    }

    @GetMapping("/exception/page")
    @Operation(summary = "获取考勤异常分页")
    @PreAuthorize("@ss.hasPermission('attendance:clock:query')")
    public CommonResult<PageResult<AttendanceExceptionRespVO>> getExceptionPage(
            @Valid AttendanceExceptionPageReqVO pageReqVO) {
        return success(attendanceResultService.getExceptionPage(pageReqVO));
    }

    @GetMapping("/exception/summary")
    @Operation(summary = "获取考勤异常统计")
    @PreAuthorize("@ss.hasPermission('attendance:clock:query')")
    public CommonResult<AttendanceExceptionSummaryRespVO> getExceptionSummary(
            @Valid AttendanceExceptionPageReqVO pageReqVO) {
        return success(attendanceResultService.getExceptionSummary(pageReqVO));
    }

    @PostMapping("/exception/resolve")
    @Operation(summary = "处理考勤异常")
    @PreAuthorize("@ss.hasPermission('attendance:exception:handle')")
    public CommonResult<Boolean> resolveException(@Valid @RequestBody AttendanceExceptionResolveReqVO reqVO) {
        return success(attendanceResultService.resolveException(reqVO));
    }

    @PostMapping("/exception/batch-resolve")
    @Operation(summary = "批量处理考勤异常")
    @PreAuthorize("@ss.hasPermission('attendance:exception:handle')")
    public CommonResult<Integer> batchResolveException(@Valid @RequestBody AttendanceExceptionBatchResolveReqVO reqVO) {
        return success(attendanceResultService.batchResolveException(reqVO));
    }

}
