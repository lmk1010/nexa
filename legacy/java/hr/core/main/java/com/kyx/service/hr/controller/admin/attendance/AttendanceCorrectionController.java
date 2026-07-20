package com.kyx.service.hr.controller.admin.attendance;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceCorrectionApplyReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceCorrectionApproveReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceCorrectionPageReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceCorrectionRespVO;
import com.kyx.service.hr.service.attendance.AttendanceCorrectionService;
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

@Tag(name = "Admin - Attendance correction")
@RestController
@RequestMapping("/hr/attendance/correction")
@Validated
public class AttendanceCorrectionController {

    @Resource
    private AttendanceCorrectionService attendanceCorrectionService;

    @PostMapping("/apply")
    @Operation(summary = "Submit attendance correction / field clock")
    @PreAuthorize("@ss.hasPermission('attendance:correction:apply')")
    public CommonResult<Long> apply(@Valid @RequestBody AttendanceCorrectionApplyReqVO reqVO) {
        return success(attendanceCorrectionService.apply(reqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "Get attendance correction page")
    @PreAuthorize("@ss.hasPermission('attendance:correction:query')")
    public CommonResult<PageResult<AttendanceCorrectionRespVO>> getPage(@Valid AttendanceCorrectionPageReqVO pageReqVO) {
        return success(attendanceCorrectionService.getPage(pageReqVO));
    }

    @PostMapping("/approve")
    @Operation(summary = "Approve attendance correction / field clock")
    @PreAuthorize("@ss.hasPermission('attendance:correction:approve')")
    public CommonResult<Boolean> approve(@Valid @RequestBody AttendanceCorrectionApproveReqVO reqVO) {
        return success(attendanceCorrectionService.approve(reqVO));
    }

    @PostMapping("/cancel")
    @Operation(summary = "Cancel attendance correction / field clock")
    @PreAuthorize("@ss.hasPermission('attendance:correction:apply')")
    public CommonResult<Boolean> cancel(@RequestParam("id") Long id) {
        return success(attendanceCorrectionService.cancel(id));
    }

}
