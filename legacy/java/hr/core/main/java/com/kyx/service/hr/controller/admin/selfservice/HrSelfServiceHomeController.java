package com.kyx.service.hr.controller.admin.selfservice;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.selfservice.vo.HrSelfServiceApplicationPageReqVO;
import com.kyx.service.hr.controller.admin.selfservice.vo.HrSelfServiceApplicationRespVO;
import com.kyx.service.hr.controller.admin.selfservice.vo.HrQuickActionConfigRespVO;
import com.kyx.service.hr.controller.admin.selfservice.vo.HrQuickActionConfigSaveReqVO;
import com.kyx.service.hr.controller.admin.selfservice.vo.HrSelfServiceHomeRespVO;
import com.kyx.service.hr.service.selfservice.HrSelfServiceApplicationService;
import com.kyx.service.hr.service.selfservice.HrSelfServiceHomeService;
import com.kyx.service.hr.service.selfservice.HrQuickActionConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static com.kyx.foundation.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 员工自助")
@RestController
@RequestMapping("/hr/self-service")
@Validated
public class HrSelfServiceHomeController {

    @Resource
    private HrSelfServiceHomeService hrSelfServiceHomeService;
    @Resource
    private HrSelfServiceApplicationService hrSelfServiceApplicationService;
    @Resource
    private HrQuickActionConfigService hrQuickActionConfigService;

    @GetMapping("/home")
    @Operation(summary = "获得员工自助首页")
    @PreAuthorize("@ss.hasPermission('hr:self-service:home')")
    public CommonResult<HrSelfServiceHomeRespVO> getHome() {
        return success(hrSelfServiceHomeService.getHome());
    }

    @GetMapping("/application/page")
    @Operation(summary = "获得我的申请分页")
    @PreAuthorize("@ss.hasPermission('hr:self-service:application')")
    public CommonResult<PageResult<HrSelfServiceApplicationRespVO>> getApplicationPage(HrSelfServiceApplicationPageReqVO reqVO) {
        return success(hrSelfServiceApplicationService.getApplicationPage(reqVO));
    }

    @GetMapping("/quick-action/list")
    @Operation(summary = "获得员工自助快捷入口配置")
    @PreAuthorize("@ss.hasPermission('hr:self-service:action-query')")
    public CommonResult<List<HrQuickActionConfigRespVO>> getQuickActionList(@RequestParam(value = "enabledOnly", required = false) Boolean enabledOnly) {
        return success(hrQuickActionConfigService.getList(enabledOnly));
    }

    @PostMapping("/quick-action/save")
    @Operation(summary = "保存员工自助快捷入口配置")
    @PreAuthorize("@ss.hasPermission('hr:self-service:action-manage')")
    public CommonResult<Long> saveQuickAction(@Valid @RequestBody HrQuickActionConfigSaveReqVO reqVO) {
        return success(hrQuickActionConfigService.save(reqVO));
    }

    @DeleteMapping("/quick-action/delete")
    @Operation(summary = "删除员工自助快捷入口配置")
    @PreAuthorize("@ss.hasPermission('hr:self-service:action-manage')")
    public CommonResult<Boolean> deleteQuickAction(@RequestParam("id") Long id) {
        return success(hrQuickActionConfigService.delete(id));
    }

    @PostMapping("/quick-action/reset-default")
    @Operation(summary = "重置员工自助默认快捷入口")
    @PreAuthorize("@ss.hasPermission('hr:self-service:action-manage')")
    public CommonResult<Integer> resetQuickActionDefault() {
        return success(hrQuickActionConfigService.resetDefault());
    }

}
