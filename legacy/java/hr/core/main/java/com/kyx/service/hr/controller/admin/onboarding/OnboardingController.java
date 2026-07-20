package com.kyx.service.hr.controller.admin.onboarding;

import com.kyx.foundation.apilog.core.annotation.ApiAccessLog;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageParam;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.excel.core.util.ExcelUtils;
import com.kyx.service.business.api.user.dto.UserOnboardingRespDTO;
import com.kyx.service.hr.api.onboarding.dto.MobileStatusCheckRespDTO;
import com.kyx.service.hr.api.onboarding.dto.OnboardingPublicReqDTO;
import com.kyx.service.hr.api.onboarding.dto.OnboardingPublicRespDTO;
import com.kyx.service.hr.controller.admin.onboarding.vo.*;
import com.kyx.service.hr.controller.admin.employee.vo.*;
import com.kyx.service.hr.dal.dataobject.onboarding.OnboardingDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeEntryDO;
import com.kyx.service.hr.service.onboarding.OnboardingService;
import com.kyx.service.hr.service.onboarding.OnboardingUserService;
import com.kyx.service.hr.service.employee.EmployeeEntryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

import static com.kyx.foundation.apilog.core.enums.OperateTypeEnum.EXPORT;
import static com.kyx.foundation.common.pojo.CommonResult.success;

/**
 * 入职管理 Controller
 *
 * @author MK
 */
@Tag(name = "管理后台 - 入职管理")
@RestController
@RequestMapping("/hr/onboarding")
@Validated
public class OnboardingController {

    @Resource
    private OnboardingService onboardingService;

    @Resource
    private OnboardingUserService onboardingUserService;

    @Resource
    private EmployeeEntryService employeeEntryService;

    
    @PostMapping("/validate-entry")
    @Operation(summary = "验证入职记录ID")
    @Parameter(name = "entryId", description = "入职记录ID", required = true)
    public CommonResult<OnboardingLinkValidateRespVO> validateEntryId(@RequestParam("entryId") Long entryId) {
        return success(onboardingService.validateEntryId(entryId));
    }

    @PostMapping("/check-mobile-status")
    @Operation(summary = "检查手机号状态")
    @Parameter(name = "name", description = "姓名", required = true)
    @Parameter(name = "mobile", description = "手机号", required = true)
    @PreAuthorize("@ss.hasPermission('hr:onboarding:create')")
    public CommonResult<MobileStatusCheckRespDTO> checkMobileStatus(@RequestParam("name") String name, 
                                                                   @RequestParam("mobile") String mobile) {
        MobileStatusCheckRespDTO statusInfo = onboardingUserService.checkMobileStatus(name, mobile);
        return success(statusInfo);
    }

    @PostMapping("/create-user-account-by-name-mobile")
    @Operation(summary = "通过姓名和手机号创建用户账号")
    @Parameter(name = "name", description = "姓名", required = true)
    @Parameter(name = "mobile", description = "手机号", required = true)
    @PreAuthorize("@ss.hasPermission('hr:onboarding:create')")
    public CommonResult<UserOnboardingRespDTO> createUserAccountByNameAndMobile(@RequestParam("name") String name, 
                                                                                @RequestParam("mobile") String mobile) {
        UserOnboardingRespDTO userInfo = onboardingUserService.createOnboardingUserByNameAndMobile(name, mobile);
        return success(userInfo);
    }

    @PostMapping("/create-public")
    @Operation(summary = "提交入职申请（移动端表单提交）")
    public CommonResult<OnboardingPublicRespDTO> submitOnboarding(@Valid @RequestBody OnboardingPublicReqDTO createReqDTO) {
        OnboardingPublicRespDTO result = onboardingService.submitOnboarding(createReqDTO);
        return success(result);
    }

    // ========== 员工入职记录相关方法 ==========

    @PostMapping("/employee-entry/create")
    @Operation(summary = "创建员工入职记录")
    @PreAuthorize("@ss.hasPermission('hr:employee-entry:create')")
    public CommonResult<Long> createEmployeeEntry(@Valid @RequestBody EmployeeEntrySaveReqVO createReqVO) {
        return success(employeeEntryService.createEmployeeEntry(createReqVO));
    }

    @PutMapping("/employee-entry/update")
    @Operation(summary = "更新员工入职记录")
    @PreAuthorize("@ss.hasPermission('hr:employee-entry:update')")
    public CommonResult<Boolean> updateEmployeeEntry(@Valid @RequestBody EmployeeEntrySaveReqVO updateReqVO) {
        employeeEntryService.updateEmployeeEntry(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/employee-entry/delete")
    @Operation(summary = "删除员工入职记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('hr:employee-entry:delete')")
    public CommonResult<Boolean> deleteEmployeeEntry(@RequestParam("id") Long id) {
        employeeEntryService.deleteEmployeeEntry(id);
        return success(true);
    }

    @GetMapping("/employee-entry/get")
    @Operation(summary = "获得员工入职记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:employee-entry:query')")
    public CommonResult<EmployeeEntryRespVO> getEmployeeEntry(@RequestParam("id") Long id) {
        EmployeeEntryRespVO employeeEntry = employeeEntryService.getEmployeeEntryDetail(id);
        return success(employeeEntry);
    }

    @GetMapping("/employee-entry/page")
    @Operation(summary = "获得员工入职记录分页")
    @PreAuthorize("@ss.hasPermission('hr:employee-entry:query')")
    public CommonResult<PageResult<EmployeeEntryRespVO>> getEmployeeEntryPage(@Valid EmployeeEntryPageReqVO pageReqVO) {
        PageResult<EmployeeEntryRespVO> pageResult = employeeEntryService.getEmployeeEntryPage(pageReqVO);
        return success(pageResult);
    }

    @GetMapping("/employee-entry/export-excel")
    @Operation(summary = "导出员工入职记录 Excel")
    @PreAuthorize("@ss.hasPermission('hr:employee-entry:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportEmployeeEntryExcel(@Valid EmployeeEntryPageReqVO pageReqVO,
                                        HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(10000);
        List<EmployeeEntryRespVO> list = employeeEntryService.getEmployeeEntryPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "员工入职记录.xls", "数据", EmployeeEntryRespVO.class, list);
    }

    @PutMapping("/employee-entry/adjust-date")
    @Operation(summary = "调整员工入职日期")
    @PreAuthorize("@ss.hasPermission('hr:employee-entry:update')")
    public CommonResult<Boolean> adjustEmployeeEntryDate(@Valid @RequestBody EmployeeEntryDateAdjustReqVO adjustReqVO) {
        employeeEntryService.adjustEntryDate(adjustReqVO.getId(), adjustReqVO.getNewEntryDate());
        return success(true);
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消入职申请")
    @PreAuthorize("@ss.hasPermission('hr:onboarding:cancel')")
    public CommonResult<Boolean> cancelOnboarding(@Valid @RequestBody OnboardingCancelReqVO cancelReqVO) {
        onboardingService.cancelOnboarding(cancelReqVO.getId(), cancelReqVO.getCancelReason(), cancelReqVO.getRemark());
        return success(true);
    }

    @PutMapping("/restore")
    @Operation(summary = "恢复入职申请")
    @PreAuthorize("@ss.hasPermission('hr:onboarding:restore')")
    public CommonResult<Boolean> restoreOnboarding(@Valid @RequestBody OnboardingRestoreReqVO restoreReqVO) {
        onboardingService.restoreOnboarding(restoreReqVO.getId(), restoreReqVO.getRemark());
        return success(true);
    }

}