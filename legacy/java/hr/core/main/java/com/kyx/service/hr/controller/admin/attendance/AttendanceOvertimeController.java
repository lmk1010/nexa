package com.kyx.service.hr.controller.admin.attendance;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceOvertimeApplyReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceOvertimeApproveReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceOvertimePageReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceOvertimeRespVO;
import com.kyx.service.hr.service.attendance.AttendanceOvertimeService;
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

@Tag(name = "Admin - Attendance overtime")
@RestController
@RequestMapping("/hr/attendance/overtime")
@Validated
public class AttendanceOvertimeController {

    @Resource
    private AttendanceOvertimeService attendanceOvertimeService;

    @PostMapping("/apply")
    @Operation(summary = "Submit overtime application")
    @PreAuthorize("@ss.hasPermission('attendance:overtime:apply')")
    public CommonResult<Long> apply(@Valid @RequestBody AttendanceOvertimeApplyReqVO reqVO) {
        return success(attendanceOvertimeService.apply(reqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "Get overtime page")
    @PreAuthorize("@ss.hasPermission('attendance:overtime:query')")
    public CommonResult<PageResult<AttendanceOvertimeRespVO>> getPage(@Valid AttendanceOvertimePageReqVO pageReqVO) {
        return success(attendanceOvertimeService.getPage(pageReqVO));
    }

    @PostMapping("/approve")
    @Operation(summary = "Approve overtime application")
    @PreAuthorize("@ss.hasPermission('attendance:overtime:approve')")
    public CommonResult<Boolean> approve(@Valid @RequestBody AttendanceOvertimeApproveReqVO reqVO) {
        return success(attendanceOvertimeService.approve(reqVO));
    }

    @PostMapping("/cancel")
    @Operation(summary = "Cancel overtime application")
    @PreAuthorize("@ss.hasPermission('attendance:overtime:apply')")
    public CommonResult<Boolean> cancel(@RequestParam("id") Long id) {
        return success(attendanceOvertimeService.cancel(id));
    }

}
