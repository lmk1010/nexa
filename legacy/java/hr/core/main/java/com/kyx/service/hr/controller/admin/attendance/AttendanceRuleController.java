package com.kyx.service.hr.controller.admin.attendance;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceGroupPageReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceGroupRespVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceGroupSaveReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceShiftRuleRespVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceShiftRuleSaveReqVO;
import com.kyx.service.hr.service.attendance.AttendanceRuleService;
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
import java.util.List;

import static com.kyx.foundation.common.pojo.CommonResult.success;

/**
 * 考勤规则 Controller
 */
@Tag(name = "管理后台 - 考勤规则")
@RestController
@RequestMapping("/hr/attendance/rule")
@Validated
public class AttendanceRuleController {

    @Resource
    private AttendanceRuleService attendanceRuleService;

    @PostMapping("/shift/save")
    @Operation(summary = "保存班次规则")
    @PreAuthorize("@ss.hasPermission('attendance:rule:manage')")
    public CommonResult<Long> saveShift(@Valid @RequestBody AttendanceShiftRuleSaveReqVO reqVO) {
        return success(attendanceRuleService.saveShift(reqVO));
    }

    @GetMapping("/shift/list")
    @Operation(summary = "获取班次规则列表")
    @PreAuthorize("@ss.hasPermission('attendance:rule:manage')")
    public CommonResult<List<AttendanceShiftRuleRespVO>> getShiftList() {
        return success(attendanceRuleService.getShiftList());
    }

    @PostMapping("/group/save")
    @Operation(summary = "保存考勤组")
    @PreAuthorize("@ss.hasPermission('attendance:rule:manage')")
    public CommonResult<Long> saveGroup(@Valid @RequestBody AttendanceGroupSaveReqVO reqVO) {
        return success(attendanceRuleService.saveGroup(reqVO));
    }

    @GetMapping("/group/page")
    @Operation(summary = "获取考勤组分页")
    @PreAuthorize("@ss.hasPermission('attendance:rule:manage')")
    public CommonResult<PageResult<AttendanceGroupRespVO>> getGroupPage(@Valid AttendanceGroupPageReqVO pageReqVO) {
        return success(attendanceRuleService.getGroupPage(pageReqVO));
    }

}
