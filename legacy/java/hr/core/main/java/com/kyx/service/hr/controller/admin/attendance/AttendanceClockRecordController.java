package com.kyx.service.hr.controller.admin.attendance;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceClockInReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceClockRecordPageReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceClockRecordRespVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceClockRecordSummaryRespVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceMyMonthDayRespVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceMyTodayRespVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceSyncReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceWorkbenchRespVO;
import com.kyx.service.hr.service.attendance.AttendanceClockRecordService;
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

/**
 * 员工打卡 Controller
 *
 * @author MK
 */
@Tag(name = "管理后台 - 员工打卡")
@RestController
@RequestMapping("/hr/attendance")
@Validated
public class AttendanceClockRecordController {

    @Resource
    private AttendanceClockRecordService attendanceClockRecordService;

    @PostMapping("/clock-in")
    @Operation(summary = "员工打卡")
    @PreAuthorize("@ss.hasPermission('attendance:clock:clock')")
    public CommonResult<Long> clock(@Valid @RequestBody AttendanceClockInReqVO reqVO) {
        return success(attendanceClockRecordService.clock(reqVO));
    }

    @GetMapping("/my-today")
    @Operation(summary = "获取我的今日打卡")
    @PreAuthorize("@ss.hasPermission('attendance:clock:query')")
    public CommonResult<AttendanceMyTodayRespVO> getMyToday() {
        return success(attendanceClockRecordService.getMyToday());
    }

    @GetMapping("/my-month")
    @Operation(summary = "获取我的月度打卡日历")
    @Parameter(name = "year", description = "年份", example = "2026")
    @Parameter(name = "month", description = "月份", example = "3")
    @PreAuthorize("@ss.hasPermission('attendance:clock:query')")
    public CommonResult<List<AttendanceMyMonthDayRespVO>> getMyMonth(@RequestParam(value = "year", required = false) Integer year,
                                                                      @RequestParam(value = "month", required = false) Integer month) {
        return success(attendanceClockRecordService.getMyMonth(year, month));
    }

    @GetMapping("/page")
    @Operation(summary = "获取打卡记录分页")
    @PreAuthorize("@ss.hasPermission('attendance:clock:query')")
    public CommonResult<PageResult<AttendanceClockRecordRespVO>> getClockRecordPage(@Valid AttendanceClockRecordPageReqVO pageReqVO) {
        return success(attendanceClockRecordService.getClockRecordPage(pageReqVO));
    }

    @GetMapping("/my-page")
    @Operation(summary = "获取我的考勤分页")
    @PreAuthorize("@ss.hasPermission('attendance:clock:query')")
    public CommonResult<PageResult<AttendanceClockRecordRespVO>> getMyClockRecordPage(@Valid AttendanceClockRecordPageReqVO pageReqVO) {
        return success(attendanceClockRecordService.getMyClockRecordPage(pageReqVO));
    }

    @GetMapping("/summary")
    @Operation(summary = "获取考勤汇总")
    @PreAuthorize("@ss.hasPermission('attendance:clock:query')")
    public CommonResult<AttendanceClockRecordSummaryRespVO> getClockRecordSummary(@Valid AttendanceClockRecordPageReqVO pageReqVO) {
        return success(attendanceClockRecordService.getClockRecordSummary(pageReqVO));
    }

    @GetMapping("/workbench")
    @Operation(summary = "获取考勤工作台")
    @PreAuthorize("@ss.hasPermission('attendance:clock:query')")
    public CommonResult<AttendanceWorkbenchRespVO> getWorkbench() {
        return success(attendanceClockRecordService.getWorkbench());
    }

    @PostMapping("/sync/dingtalk")
    @Operation(summary = "同步钉钉打卡记录")
    @PreAuthorize("@ss.hasPermission('attendance:clock:sync')")
    public CommonResult<Integer> syncDingTalk(@Valid @RequestBody AttendanceSyncReqVO reqVO) {
        return success(attendanceClockRecordService.syncDingTalk(reqVO));
    }

}
