package com.kyx.service.hr.service.payroll;

import com.kyx.foundation.common.pojo.PageResult;
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

import javax.validation.Valid;
import java.util.List;

public interface PayrollService {

    PayrollBatchRespVO generate(@Valid PayrollBatchGenerateReqVO reqVO);

    PageResult<PayrollBatchRespVO> getBatchPage(PayrollBatchPageReqVO pageReqVO);

    Boolean publish(@Valid PayrollBatchActionReqVO reqVO);

    Boolean lock(@Valid PayrollBatchActionReqVO reqVO);

    void updatePublishApprovalStatusByBpmEvent(Long batchId, String processInstanceId, Integer bpmStatus, Long operatorUserId);

    PageResult<PayslipRespVO> getPayslipPage(PayslipPageReqVO pageReqVO);

    List<PayslipExportRespVO> getPayslipExportList(PayslipPageReqVO pageReqVO);

    Boolean updatePayslip(@Valid PayslipUpdateReqVO reqVO);

    Boolean resolvePayslip(@Valid PayslipResolveReqVO reqVO);

    PageResult<PayslipRespVO> getMyPayslipPage(PayslipPageReqVO pageReqVO);

    PayrollIncomeSummaryRespVO getMyIncomeSummary(Integer year);

    PayrollTaxSummaryRespVO getMyTaxSummary(Integer year);

    PayrollReportRespVO getReport(Integer year);

    PageResult<SocialSecurityAccountRespVO> getMySocialSecurityPage(SocialSecurityAccountPageReqVO pageReqVO);

    Boolean confirmMy(@Valid PayslipActionReqVO reqVO);

    Boolean issueMy(@Valid PayslipIssueReqVO reqVO);

    PageResult<SocialSecurityAccountRespVO> getSocialSecurityPage(SocialSecurityAccountPageReqVO pageReqVO);

    Long saveSocialSecurity(@Valid SocialSecurityAccountSaveReqVO reqVO);

    void deleteSocialSecurity(Long id);

    PageResult<PayrollSchemeRespVO> getSchemePage(PayrollSchemePageReqVO pageReqVO);

    Long saveScheme(@Valid PayrollSchemeSaveReqVO reqVO);

    Boolean enableScheme(Long id);

    void deleteScheme(Long id);

}
