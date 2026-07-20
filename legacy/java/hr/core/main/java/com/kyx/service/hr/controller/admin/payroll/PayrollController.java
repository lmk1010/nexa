package com.kyx.service.hr.controller.admin.payroll;

import com.kyx.foundation.apilog.core.annotation.ApiAccessLog;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.excel.core.util.ExcelUtils;
import com.kyx.service.hr.controller.admin.payroll.vo.PayrollBatchActionReqVO;
import com.kyx.service.hr.controller.admin.payroll.vo.PayrollBatchGenerateReqVO;
import com.kyx.service.hr.controller.admin.payroll.vo.PayrollBatchPageReqVO;
import com.kyx.service.hr.controller.admin.payroll.vo.PayrollBatchRespVO;
import com.kyx.service.hr.controller.admin.payroll.vo.PayrollIncomeSummaryRespVO;
import com.kyx.service.hr.controller.admin.payroll.vo.PayrollReportRespVO;
import com.kyx.service.hr.controller.admin.payroll.vo.PayrollSchemePageReqVO;
import com.kyx.service.hr.controller.admin.payroll.vo.PayrollSchemeRespVO;
import com.kyx.service.hr.controller.admin.payroll.vo.PayrollSchemeSaveReqVO;
import com.kyx.service.hr.controller.admin.payroll.vo.PayrollTaxSummaryRespVO;
import com.kyx.service.hr.controller.admin.payroll.vo.PayslipActionReqVO;
import com.kyx.service.hr.controller.admin.payroll.vo.PayslipExportRespVO;
import com.kyx.service.hr.controller.admin.payroll.vo.PayslipIssueReqVO;
import com.kyx.service.hr.controller.admin.payroll.vo.PayslipPageReqVO;
import com.kyx.service.hr.controller.admin.payroll.vo.PayslipResolveReqVO;
import com.kyx.service.hr.controller.admin.payroll.vo.PayslipRespVO;
import com.kyx.service.hr.controller.admin.payroll.vo.PayslipUpdateReqVO;
import com.kyx.service.hr.controller.admin.payroll.vo.SocialSecurityAccountPageReqVO;
import com.kyx.service.hr.controller.admin.payroll.vo.SocialSecurityAccountRespVO;
import com.kyx.service.hr.controller.admin.payroll.vo.SocialSecurityAccountSaveReqVO;
import com.kyx.service.hr.service.payroll.PayrollService;
import io.swagger.v3.oas.annotations.Operation;
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
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

import static com.kyx.foundation.apilog.core.enums.OperateTypeEnum.EXPORT;
import static com.kyx.foundation.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 薪酬工资条")
@RestController
@RequestMapping("/hr/payroll")
@Validated
public class PayrollController {

    @Resource
    private PayrollService payrollService;

    @PostMapping("/batch/generate")
    @Operation(summary = "生成工资批次")
    @PreAuthorize("@ss.hasPermission('hr:payroll:manage')")
    public CommonResult<PayrollBatchRespVO> generate(@Valid @RequestBody PayrollBatchGenerateReqVO reqVO) {
        return success(payrollService.generate(reqVO));
    }

    @GetMapping("/batch/page")
    @Operation(summary = "获取工资批次分页")
    @PreAuthorize("@ss.hasPermission('hr:payroll:query')")
    public CommonResult<PageResult<PayrollBatchRespVO>> getBatchPage(@Valid PayrollBatchPageReqVO pageReqVO) {
        return success(payrollService.getBatchPage(pageReqVO));
    }

    @PostMapping("/batch/publish")
    @Operation(summary = "发布工资批次")
    @PreAuthorize("@ss.hasPermission('hr:payroll:manage')")
    public CommonResult<Boolean> publish(@Valid @RequestBody PayrollBatchActionReqVO reqVO) {
        return success(payrollService.publish(reqVO));
    }

    @PostMapping("/batch/lock")
    @Operation(summary = "锁定工资批次")
    @PreAuthorize("@ss.hasPermission('hr:payroll:manage')")
    public CommonResult<Boolean> lock(@Valid @RequestBody PayrollBatchActionReqVO reqVO) {
        return success(payrollService.lock(reqVO));
    }

    @GetMapping("/payslip/page")
    @Operation(summary = "获取工资条分页")
    @PreAuthorize("@ss.hasPermission('hr:payroll:query')")
    public CommonResult<PageResult<PayslipRespVO>> getPayslipPage(@Valid PayslipPageReqVO pageReqVO) {
        return success(payrollService.getPayslipPage(pageReqVO));
    }

    @GetMapping("/payslip/export-excel")
    @Operation(summary = "导出工资条明细 Excel")
    @PreAuthorize("@ss.hasPermission('hr:payroll:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportPayslipExcel(@Valid PayslipPageReqVO pageReqVO,
                                   HttpServletResponse response) throws IOException {
        List<PayslipExportRespVO> list = payrollService.getPayslipExportList(pageReqVO);
        ExcelUtils.write(response, "工资条明细.xls", "数据", PayslipExportRespVO.class, list);
    }

    @PutMapping("/payslip/update")
    @Operation(summary = "更新工资条")
    @PreAuthorize("@ss.hasPermission('hr:payroll:manage')")
    public CommonResult<Boolean> updatePayslip(@Valid @RequestBody PayslipUpdateReqVO reqVO) {
        return success(payrollService.updatePayslip(reqVO));
    }

    @PostMapping("/payslip/resolve")
    @Operation(summary = "处理工资条异议")
    @PreAuthorize("@ss.hasPermission('hr:payroll:manage')")
    public CommonResult<Boolean> resolvePayslip(@Valid @RequestBody PayslipResolveReqVO reqVO) {
        return success(payrollService.resolvePayslip(reqVO));
    }

    @GetMapping("/my-payslip/page")
    @Operation(summary = "获取我的工资条分页")
    @PreAuthorize("@ss.hasPermission('hr:payslip:self')")
    public CommonResult<PageResult<PayslipRespVO>> getMyPayslipPage(@Valid PayslipPageReqVO pageReqVO) {
        return success(payrollService.getMyPayslipPage(pageReqVO));
    }

    @GetMapping("/my-income-summary")
    @Operation(summary = "获取我的年度收入汇总")
    @PreAuthorize("@ss.hasPermission('hr:payslip:self')")
    public CommonResult<PayrollIncomeSummaryRespVO> getMyIncomeSummary(
            @RequestParam(value = "year", required = false) Integer year) {
        return success(payrollService.getMyIncomeSummary(year));
    }

    @GetMapping("/my-tax-summary")
    @Operation(summary = "获取我的个税扣缴汇总")
    @PreAuthorize("@ss.hasPermission('hr:payslip:self')")
    public CommonResult<PayrollTaxSummaryRespVO> getMyTaxSummary(
            @RequestParam(value = "year", required = false) Integer year) {
        return success(payrollService.getMyTaxSummary(year));
    }

    @GetMapping("/report/summary")
    @Operation(summary = "获取薪酬成本报表")
    @PreAuthorize("@ss.hasPermission('hr:payroll:query')")
    public CommonResult<PayrollReportRespVO> getReport(
            @RequestParam(value = "year", required = false) Integer year) {
        return success(payrollService.getReport(year));
    }

    @GetMapping("/my-social-security/page")
    @Operation(summary = "获取我的社保公积金明细分页")
    @PreAuthorize("@ss.hasPermission('hr:payslip:self')")
    public CommonResult<PageResult<SocialSecurityAccountRespVO>> getMySocialSecurityPage(
            @Valid SocialSecurityAccountPageReqVO pageReqVO) {
        return success(payrollService.getMySocialSecurityPage(pageReqVO));
    }

    @PostMapping("/my-payslip/confirm")
    @Operation(summary = "确认我的工资条")
    @PreAuthorize("@ss.hasPermission('hr:payslip:self')")
    public CommonResult<Boolean> confirmMy(@Valid @RequestBody PayslipActionReqVO reqVO) {
        return success(payrollService.confirmMy(reqVO));
    }

    @PostMapping("/my-payslip/issue")
    @Operation(summary = "提交我的工资条异议")
    @PreAuthorize("@ss.hasPermission('hr:payslip:self')")
    public CommonResult<Boolean> issueMy(@Valid @RequestBody PayslipIssueReqVO reqVO) {
        return success(payrollService.issueMy(reqVO));
    }

    @GetMapping("/social-security/page")
    @Operation(summary = "获取社保公积金台账分页")
    @PreAuthorize("@ss.hasPermission('hr:payroll:query')")
    public CommonResult<PageResult<SocialSecurityAccountRespVO>> getSocialSecurityPage(
            @Valid SocialSecurityAccountPageReqVO pageReqVO) {
        return success(payrollService.getSocialSecurityPage(pageReqVO));
    }

    @PostMapping("/social-security/save")
    @Operation(summary = "保存社保公积金台账")
    @PreAuthorize("@ss.hasPermission('hr:payroll:manage')")
    public CommonResult<Long> saveSocialSecurity(@Valid @RequestBody SocialSecurityAccountSaveReqVO reqVO) {
        return success(payrollService.saveSocialSecurity(reqVO));
    }

    @DeleteMapping("/social-security/delete")
    @Operation(summary = "删除社保公积金台账")
    @PreAuthorize("@ss.hasPermission('hr:payroll:manage')")
    public CommonResult<Boolean> deleteSocialSecurity(@RequestParam("id") Long id) {
        payrollService.deleteSocialSecurity(id);
        return success(true);
    }

    @GetMapping("/scheme/page")
    @Operation(summary = "获取薪资方案分页")
    @PreAuthorize("@ss.hasPermission('hr:payroll:query')")
    public CommonResult<PageResult<PayrollSchemeRespVO>> getSchemePage(@Valid PayrollSchemePageReqVO pageReqVO) {
        return success(payrollService.getSchemePage(pageReqVO));
    }

    @PostMapping("/scheme/save")
    @Operation(summary = "保存薪资方案")
    @PreAuthorize("@ss.hasPermission('hr:payroll:manage')")
    public CommonResult<Long> saveScheme(@Valid @RequestBody PayrollSchemeSaveReqVO reqVO) {
        return success(payrollService.saveScheme(reqVO));
    }

    @PostMapping("/scheme/enable")
    @Operation(summary = "启用薪资方案")
    @PreAuthorize("@ss.hasPermission('hr:payroll:manage')")
    public CommonResult<Boolean> enableScheme(@RequestParam("id") Long id) {
        return success(payrollService.enableScheme(id));
    }

    @DeleteMapping("/scheme/delete")
    @Operation(summary = "删除薪资方案")
    @PreAuthorize("@ss.hasPermission('hr:payroll:manage')")
    public CommonResult<Boolean> deleteScheme(@RequestParam("id") Long id) {
        payrollService.deleteScheme(id);
        return success(true);
    }

}
