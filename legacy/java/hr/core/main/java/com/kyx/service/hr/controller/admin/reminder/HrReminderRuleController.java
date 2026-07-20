package com.kyx.service.hr.controller.admin.reminder;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.hr.controller.admin.reminder.vo.HrReminderRulePageReqVO;
import com.kyx.service.hr.controller.admin.reminder.vo.HrReminderRuleRespVO;
import com.kyx.service.hr.controller.admin.reminder.vo.HrReminderRuleSaveReqVO;
import com.kyx.service.hr.dal.dataobject.reminder.HrReminderRuleDO;
import com.kyx.service.hr.service.reminder.HrReminderRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

import static com.kyx.foundation.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - HR 提醒规则")
@RestController
@RequestMapping("/hr/reminder/rule")
@Validated
public class HrReminderRuleController {

    @Resource
    private HrReminderRuleService reminderRuleService;

    @PostMapping("/create")
    @Operation(summary = "创建提醒规则")
    @PreAuthorize("@ss.hasPermission('hr:reminder:rule')")
    public CommonResult<Long> createRule(@Valid @RequestBody HrReminderRuleSaveReqVO reqVO) {
        return success(reminderRuleService.createRule(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新提醒规则")
    @PreAuthorize("@ss.hasPermission('hr:reminder:rule')")
    public CommonResult<Boolean> updateRule(@Valid @RequestBody HrReminderRuleSaveReqVO reqVO) {
        reminderRuleService.updateRule(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除提醒规则")
    @Parameter(name = "id", description = "规则 ID", required = true)
    @PreAuthorize("@ss.hasPermission('hr:reminder:rule')")
    public CommonResult<Boolean> deleteRule(@RequestParam("id") Long id) {
        reminderRuleService.deleteRule(id);
        return success(true);
    }

    @PutMapping("/enable")
    @Operation(summary = "启停提醒规则")
    @PreAuthorize("@ss.hasPermission('hr:reminder:rule')")
    public CommonResult<Boolean> enableRule(@RequestParam("id") Long id,
                                            @RequestParam("enabled") Boolean enabled) {
        reminderRuleService.enableRule(id, enabled);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得提醒规则")
    @Parameter(name = "id", description = "规则 ID", required = true)
    @PreAuthorize("@ss.hasAnyPermissions('hr:reminder:rule,hr:reminder:query')")
    public CommonResult<HrReminderRuleRespVO> getRule(@RequestParam("id") Long id) {
        HrReminderRuleDO rule = reminderRuleService.getRule(id);
        return success(BeanUtils.toBean(rule, HrReminderRuleRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得提醒规则分页")
    @PreAuthorize("@ss.hasAnyPermissions('hr:reminder:rule,hr:reminder:query')")
    public CommonResult<PageResult<HrReminderRuleRespVO>> getRulePage(@Valid HrReminderRulePageReqVO pageReqVO) {
        return success(reminderRuleService.getRulePage(pageReqVO));
    }

}
