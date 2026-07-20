package com.kyx.service.hr.controller.admin.lifecycle;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.lifecycle.vo.HrLifecycleEventPageReqVO;
import com.kyx.service.hr.controller.admin.lifecycle.vo.HrLifecycleEventRespVO;
import com.kyx.service.hr.controller.admin.lifecycle.vo.HrLifecycleCalendarEventRespVO;
import com.kyx.service.hr.controller.admin.lifecycle.vo.HrLifecycleRegularizationCreateReqVO;
import com.kyx.service.hr.controller.admin.lifecycle.vo.HrLifecycleReminderRespVO;
import com.kyx.service.hr.controller.admin.lifecycle.vo.HrLifecycleResignationCreateReqVO;
import com.kyx.service.hr.controller.admin.lifecycle.vo.HrLifecycleSalaryAdjustCreateReqVO;
import com.kyx.service.hr.controller.admin.lifecycle.vo.HrLifecycleTaskCompleteReqVO;
import com.kyx.service.hr.controller.admin.lifecycle.vo.HrLifecycleTaskRespVO;
import com.kyx.service.hr.controller.admin.lifecycle.vo.HrLifecycleTransferCreateReqVO;
import com.kyx.service.hr.controller.admin.lifecycle.vo.HrLifecycleWorkbenchRespVO;
import com.kyx.service.hr.service.lifecycle.HrLifecycleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.time.LocalDate;
import java.util.List;

import static com.kyx.foundation.common.pojo.CommonResult.success;
import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Tag(name = "管理后台 - HR 入转调离生命周期")
@RestController
@RequestMapping("/hr/lifecycle")
@Validated
public class HrLifecycleController {

    @Resource
    private HrLifecycleService hrLifecycleService;

    @GetMapping("/workbench")
    @Operation(summary = "获得生命周期工作台")
    @PreAuthorize("@ss.hasPermission('hr:lifecycle:workbench')")
    public CommonResult<HrLifecycleWorkbenchRespVO> getWorkbench() {
        return success(hrLifecycleService.getWorkbench());
    }

    @GetMapping("/event/page")
    @Operation(summary = "获得生命周期事件分页")
    @PreAuthorize("@ss.hasPermission('hr:lifecycle:event:query')")
    public CommonResult<PageResult<HrLifecycleEventRespVO>> getEventPage(@Valid HrLifecycleEventPageReqVO pageReqVO) {
        return success(hrLifecycleService.getEventPage(pageReqVO));
    }

    @GetMapping("/event/get")
    @Operation(summary = "获得生命周期事件详情")
    @Parameter(name = "id", description = "事件 ID", required = true)
    @PreAuthorize("@ss.hasPermission('hr:lifecycle:event:query')")
    public CommonResult<HrLifecycleEventRespVO> getEvent(@RequestParam("id") Long id) {
        return success(hrLifecycleService.getEvent(id));
    }

    @GetMapping("/event/timeline")
    @Operation(summary = "获得员工生命周期时间线")
    @Parameter(name = "profileId", description = "员工档案 ID", required = true)
    @PreAuthorize("@ss.hasPermission('hr:lifecycle:event:query')")
    public CommonResult<List<HrLifecycleEventRespVO>> getTimeline(@RequestParam("profileId") Long profileId) {
        return success(hrLifecycleService.getTimeline(profileId));
    }

    @GetMapping("/task/list")
    @Operation(summary = "获得生命周期事件检查清单")
    @Parameter(name = "eventId", description = "事件 ID", required = true)
    @PreAuthorize("@ss.hasPermission('hr:lifecycle:event:query')")
    public CommonResult<List<HrLifecycleTaskRespVO>> getTaskList(@RequestParam("eventId") Long eventId) {
        return success(hrLifecycleService.getTaskList(eventId));
    }

    @GetMapping("/reminder/list")
    @Operation(summary = "获得生命周期提醒列表")
    @PreAuthorize("@ss.hasPermission('hr:lifecycle:workbench')")
    public CommonResult<List<HrLifecycleReminderRespVO>> getReminderList() {
        return success(hrLifecycleService.getReminderList());
    }

    @GetMapping("/calendar/list")
    @Operation(summary = "获得生命周期日历事件")
    @PreAuthorize("@ss.hasPermission('hr:lifecycle:workbench')")
    public CommonResult<List<HrLifecycleCalendarEventRespVO>> getCalendarEvents(
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY) LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY) LocalDate endDate) {
        return success(hrLifecycleService.getCalendarEvents(startDate, endDate));
    }

    @PostMapping("/resignation/create")
    @Operation(summary = "发起离职办理")
    @PreAuthorize("@ss.hasPermission('hr:lifecycle:resignation')")
    public CommonResult<Long> createResignation(@Valid @RequestBody HrLifecycleResignationCreateReqVO reqVO) {
        return success(hrLifecycleService.createResignation(reqVO));
    }

    @PostMapping("/regularization/create")
    @Operation(summary = "发起转正办理")
    @PreAuthorize("@ss.hasPermission('hr:lifecycle:regularization')")
    public CommonResult<Long> createRegularization(@Valid @RequestBody HrLifecycleRegularizationCreateReqVO reqVO) {
        return success(hrLifecycleService.createRegularization(reqVO));
    }

    @PostMapping("/transfer/create")
    @Operation(summary = "发起调岗办理")
    @PreAuthorize("@ss.hasPermission('hr:lifecycle:transfer')")
    public CommonResult<Long> createTransfer(@Valid @RequestBody HrLifecycleTransferCreateReqVO reqVO) {
        return success(hrLifecycleService.createTransfer(reqVO));
    }

    @PostMapping("/salary-adjust/create")
    @Operation(summary = "发起调薪办理")
    @PreAuthorize("@ss.hasPermission('hr:lifecycle:salary-adjust')")
    public CommonResult<Long> createSalaryAdjust(@Valid @RequestBody HrLifecycleSalaryAdjustCreateReqVO reqVO) {
        return success(hrLifecycleService.createSalaryAdjust(reqVO));
    }

    @PutMapping("/task/complete")
    @Operation(summary = "完成生命周期检查项")
    @PreAuthorize("@ss.hasPermission('hr:lifecycle:task:update')")
    public CommonResult<Boolean> completeTask(@Valid @RequestBody HrLifecycleTaskCompleteReqVO reqVO) {
        hrLifecycleService.completeTask(reqVO);
        return success(true);
    }

    @PostMapping("/event/effective")
    @Operation(summary = "生命周期事件生效")
    @Parameter(name = "id", description = "事件 ID", required = true)
    @PreAuthorize("@ss.hasPermission('hr:lifecycle:event:effective')")
    public CommonResult<Boolean> effectiveEvent(@RequestParam("id") Long id) {
        hrLifecycleService.effectiveEvent(id);
        return success(true);
    }

    @PostMapping("/event/effective-due")
    @Operation(summary = "批量生效到期生命周期事件")
    @PreAuthorize("@ss.hasPermission('hr:lifecycle:event:effective')")
    public CommonResult<Integer> effectiveDueEvents() {
        return success(hrLifecycleService.effectiveDueEvents());
    }

    @PostMapping("/event/cancel")
    @Operation(summary = "撤销生命周期事件")
    @PreAuthorize("@ss.hasPermission('hr:lifecycle:event:cancel')")
    public CommonResult<Boolean> cancelEvent(@RequestParam("id") Long id,
                                             @RequestParam(value = "reason", required = false) String reason) {
        hrLifecycleService.cancelEvent(id, reason);
        return success(true);
    }

    @PostMapping("/event/backfill")
    @Operation(summary = "回填历史员工生命周期基线事件")
    @PreAuthorize("@ss.hasPermission('hr:lifecycle:event:backfill')")
    public CommonResult<Integer> backfillBaselineEvents() {
        return success(hrLifecycleService.backfillBaselineEvents());
    }

}
