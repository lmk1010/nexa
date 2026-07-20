package com.kyx.service.hr.controller.admin.reminder;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.reminder.vo.HrReminderRecordPageReqVO;
import com.kyx.service.hr.controller.admin.reminder.vo.HrReminderRecordRespVO;
import com.kyx.service.hr.service.reminder.HrReminderRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

import static com.kyx.foundation.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - HR 提醒记录")
@RestController
@RequestMapping("/hr/reminder/record")
@Validated
public class HrReminderRecordController {

    @Resource
    private HrReminderRecordService reminderRecordService;

    @GetMapping("/page")
    @Operation(summary = "获得提醒记录分页")
    @PreAuthorize("@ss.hasAnyPermissions('hr:reminder:query,hr:reminder:rule')")
    public CommonResult<PageResult<HrReminderRecordRespVO>> getPage(@Valid HrReminderRecordPageReqVO pageReqVO) {
        return success(reminderRecordService.getPage(pageReqVO));
    }

    @PutMapping("/read")
    @Operation(summary = "标记提醒记录已读")
    @Parameter(name = "id", description = "记录 ID", required = true)
    @PreAuthorize("@ss.hasAnyPermissions('hr:reminder:query,hr:reminder:rule')")
    public CommonResult<Boolean> read(@RequestParam("id") Long id) {
        reminderRecordService.read(id);
        return success(true);
    }

    @PutMapping("/read-all")
    @Operation(summary = "全部标记已读")
    @PreAuthorize("@ss.hasAnyPermissions('hr:reminder:query,hr:reminder:rule')")
    public CommonResult<Integer> readAll(@RequestParam(value = "mine", required = false) Boolean mine) {
        return success(reminderRecordService.readAll(mine));
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新提醒记录")
    @PreAuthorize("@ss.hasPermission('hr:reminder:query')")
    public CommonResult<Integer> refresh() {
        return success(reminderRecordService.refreshGeneratedRecords());
    }

}
