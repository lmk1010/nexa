package com.kyx.service.hr.service.payroll;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.kyx.foundation.common.exception.enums.GlobalErrorCodeConstants;
import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.json.JsonUtils;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.foundation.security.core.util.SecurityFrameworkUtils;
import com.kyx.service.bpm.api.task.BpmProcessInstanceApi;
import com.kyx.service.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.kyx.service.bpm.enums.task.BpmProcessInstanceStatusEnum;
import com.kyx.service.business.api.dept.DeptApi;
import com.kyx.service.business.api.dept.dto.DeptRespDTO;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
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
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceDailyResultDO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceMonthlyConfirmDO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceMonthlySettlementDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeEntryDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeOperationLogDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeProfileDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeSalaryDO;
import com.kyx.service.hr.dal.dataobject.payroll.PayrollBatchDO;
import com.kyx.service.hr.dal.dataobject.payroll.PayrollSchemeDO;
import com.kyx.service.hr.dal.dataobject.payroll.PayslipDO;
import com.kyx.service.hr.dal.dataobject.payroll.SocialSecurityAccountDO;
import com.kyx.service.hr.dal.mysql.attendance.AttendanceDailyResultMapper;
import com.kyx.service.hr.dal.mysql.attendance.AttendanceMonthlyConfirmMapper;
import com.kyx.service.hr.dal.mysql.attendance.AttendanceMonthlySettlementMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeEntryMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeOperationLogMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeProfileMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeSalaryMapper;
import com.kyx.service.hr.dal.mysql.payroll.PayrollBatchMapper;
import com.kyx.service.hr.dal.mysql.payroll.PayrollSchemeMapper;
import com.kyx.service.hr.dal.mysql.payroll.PayslipMapper;
import com.kyx.service.hr.dal.mysql.payroll.SocialSecurityAccountMapper;
import com.kyx.service.hr.service.todo.HrTodoTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

@Service
@Validated
@Slf4j
public class PayrollServiceImpl implements PayrollService {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    public static final String PROCESS_KEY_PAYROLL_PUBLISH = "hr_payroll_publish";
    private static final String BATCH_DRAFT = "DRAFT";
    private static final String BATCH_PENDING_APPROVAL = "PENDING_APPROVAL";
    private static final String BATCH_PUBLISHED = "PUBLISHED";
    private static final String BATCH_LOCKED = "LOCKED";
    private static final String BATCH_REJECTED = "REJECTED";
    private static final String BATCH_CANCELLED = "CANCELLED";

    private static final String SLIP_DRAFT = "DRAFT";
    private static final String SLIP_PUBLISHED = "PUBLISHED";
    private static final String SLIP_CONFIRMED = "CONFIRMED";
    private static final String SLIP_ISSUE = "ISSUE";
    private static final String SLIP_RESOLVED = "RESOLVED";
    private static final String ATTENDANCE_SETTLEMENT_LOCKED = "LOCKED";
    private static final String ATTENDANCE_CONFIRM_CONFIRMED = "CONFIRMED";
    private static final String ATTENDANCE_CONFIRM_RESOLVED = "RESOLVED";
    private static final String SCHEME_ACTIVE = "ACTIVE";
    private static final String SCHEME_DISABLED = "DISABLED";
    private static final String SOCIAL_PENDING_ADD = "PENDING_ADD";
    private static final String SOCIAL_ENROLLED = "ENROLLED";
    private static final String SOCIAL_PENDING_STOP = "PENDING_STOP";
    private static final String SOCIAL_SUSPENDED = "SUSPENDED";
    private static final String SOCIAL_STOPPED = "STOPPED";

    @Resource
    private PayrollBatchMapper payrollBatchMapper;
    @Resource
    private PayrollSchemeMapper payrollSchemeMapper;
    @Resource
    private PayslipMapper payslipMapper;
    @Resource
    private SocialSecurityAccountMapper socialSecurityAccountMapper;
    @Resource
    private EmployeeProfileMapper employeeProfileMapper;
    @Resource
    private EmployeeEntryMapper employeeEntryMapper;
    @Resource
    private EmployeeSalaryMapper employeeSalaryMapper;
    @Resource
    private EmployeeOperationLogMapper employeeOperationLogMapper;
    @Resource
    private AttendanceDailyResultMapper attendanceDailyResultMapper;
    @Resource
    private AttendanceMonthlySettlementMapper attendanceMonthlySettlementMapper;
    @Resource
    private AttendanceMonthlyConfirmMapper attendanceMonthlyConfirmMapper;
    @Resource
    private BpmProcessInstanceApi processInstanceApi;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private HrTodoTaskService hrTodoTaskService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayrollBatchRespVO generate(PayrollBatchGenerateReqVO reqVO) {
        LocalDate month = resolveMonth(reqVO.getYear(), reqVO.getMonth());
        String payrollMonth = month.format(MONTH_FORMATTER);
        PayrollBatchDO existed = payrollBatchMapper.selectByMonth(payrollMonth);
        if (existed != null && BATCH_LOCKED.equals(existed.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "该月份工资批次已锁定，不能重新生成");
        }
        if (existed != null && BATCH_PENDING_APPROVAL.equals(existed.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "该月份工资批次正在发薪审批中，不能重新生成");
        }

        PayrollBatchDO batch = existed == null ? new PayrollBatchDO() : existed;
        batch.setPayrollMonth(payrollMonth);
        batch.setBatchName(StringUtils.hasText(reqVO.getBatchName()) ? reqVO.getBatchName().trim() : payrollMonth + " 工资条");
        batch.setStatus(BATCH_DRAFT);
        batch.setProcessInstanceId(null);
        batch.setApprovalStatus(null);
        batch.setGeneratedTime(LocalDateTime.now());
        batch.setPublishedTime(null);
        batch.setPublishedBy(null);
        batch.setSummaryJson("{}");
        if (existed == null) {
            payrollBatchMapper.insert(batch);
        } else {
            payrollBatchMapper.updateById(batch);
        }

        generatePayslips(batch, month);
        refreshBatchSummary(batch);
        return BeanUtils.toBean(payrollBatchMapper.selectById(batch.getId()), PayrollBatchRespVO.class);
    }

    @Override
    public PageResult<PayrollBatchRespVO> getBatchPage(PayrollBatchPageReqVO pageReqVO) {
        normalizeBatchPageReq(pageReqVO);
        PageResult<PayrollBatchDO> pageResult = payrollBatchMapper.selectPage(pageReqVO);
        List<PayrollBatchRespVO> records = BeanUtils.toBean(pageResult.getList(), PayrollBatchRespVO.class);
        return new PageResult<>(emptyIfNull(records), pageResult.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean publish(PayrollBatchActionReqVO reqVO) {
        PayrollBatchDO batch = getBatch(reqVO.getId());
        if (BATCH_LOCKED.equals(batch.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "工资批次已锁定，不能发布");
        }
        if (BATCH_PUBLISHED.equals(batch.getStatus())) {
            return true;
        }
        if (BATCH_PENDING_APPROVAL.equals(batch.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "工资批次正在发薪审批中");
        }
        List<PayslipDO> slips = payslipMapper.selectListByBatchId(batch.getId());
        if (slips == null || slips.isEmpty()) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "工资批次没有工资条，不能提交发薪审批");
        }
        startPayrollPublishApproval(batch, slips);
        refreshBatchSummary(getBatch(batch.getId()));
        refreshTodoTasksQuietly();
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePublishApprovalStatusByBpmEvent(Long batchId, String processInstanceId, Integer bpmStatus, Long operatorUserId) {
        PayrollBatchDO batch = payrollBatchMapper.selectById(batchId);
        if (batch == null) {
            log.warn("发薪审批 BPM 回调批次不存在，batchId={}, processInstanceId={}, bpmStatus={}",
                    batchId, processInstanceId, bpmStatus);
            return;
        }
        if (StringUtils.hasText(batch.getProcessInstanceId())
                && StringUtils.hasText(processInstanceId)
                && !Objects.equals(batch.getProcessInstanceId(), processInstanceId)) {
            log.warn("发薪审批 BPM 回调流程实例不匹配，batchId={}, processInstanceId={}, currentProcessInstanceId={}",
                    batchId, processInstanceId, batch.getProcessInstanceId());
            return;
        }
        if (Objects.equals(bpmStatus, BpmProcessInstanceStatusEnum.APPROVE.getStatus())) {
            publishBatchAfterApproval(batch, processInstanceId, operatorUserId);
            return;
        }
        if (Objects.equals(bpmStatus, BpmProcessInstanceStatusEnum.REJECT.getStatus())) {
            closePayrollApproval(batch, BATCH_REJECTED, processInstanceId, bpmStatus);
            return;
        }
        if (Objects.equals(bpmStatus, BpmProcessInstanceStatusEnum.CANCEL.getStatus())) {
            closePayrollApproval(batch, BATCH_CANCELLED, processInstanceId, bpmStatus);
        }
    }

    private void publishBatchAfterApproval(PayrollBatchDO batch, String processInstanceId, Long operatorUserId) {
        payslipMapper.update(null, new LambdaUpdateWrapper<PayslipDO>()
                .eq(PayslipDO::getBatchId, batch.getId())
                .eq(PayslipDO::getStatus, SLIP_DRAFT)
                .set(PayslipDO::getStatus, SLIP_PUBLISHED));
        PayrollBatchDO updateDO = new PayrollBatchDO();
        updateDO.setId(batch.getId());
        updateDO.setStatus(BATCH_PUBLISHED);
        updateDO.setApprovalStatus(BpmProcessInstanceStatusEnum.APPROVE.getStatus());
        updateDO.setProcessInstanceId(StringUtils.hasText(processInstanceId) ? processInstanceId : batch.getProcessInstanceId());
        updateDO.setPublishedTime(LocalDateTime.now());
        updateDO.setPublishedBy(operatorUserId != null ? operatorUserId : SecurityFrameworkUtils.getLoginUserId());
        payrollBatchMapper.updateById(updateDO);
        recordPayrollBatchLog(batch, "payroll_publish", "发布工资条",
                batch.getPayrollMonth() + " 发薪审批通过，工资条已发布");
        refreshBatchSummary(getBatch(batch.getId()));
        refreshTodoTasksQuietly();
    }

    private void startPayrollPublishApproval(PayrollBatchDO batch, List<PayslipDO> slips) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.UNAUTHORIZED, "发薪审批发起人不能为空");
        }
        ensureAttendanceReadyForPayroll(batch, slips);
        PayrollSummary summary = buildSummary(batch.getId());
        if (summary.totalCount == 0) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "工资批次没有工资条，不能提交发薪审批");
        }
        if (summary.issueCount > 0) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "工资批次存在异议工资条，处理后才能提交发薪审批");
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("batchId", batch.getId());
        variables.put("payrollMonth", batch.getPayrollMonth());
        variables.put("batchName", batch.getBatchName());
        variables.put("totalCount", summary.totalCount);
        variables.put("baseSalaryTotal", summary.baseSalaryTotal);
        variables.put("attendanceDeductionTotal", summary.attendanceDeductionTotal);
        variables.put("netSalaryTotal", summary.netSalaryTotal);
        variables.put("draftCount", summary.draftCount);
        variables.put("confirmedCount", summary.confirmedCount);
        variables.put("issueCount", summary.issueCount);

        String processInstanceId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO()
                        .setProcessDefinitionKey(PROCESS_KEY_PAYROLL_PUBLISH)
                        .setBusinessKey(String.valueOf(batch.getId()))
                        .setVariables(variables))
                .getCheckedData();

        PayrollBatchDO updateDO = new PayrollBatchDO();
        updateDO.setId(batch.getId());
        updateDO.setStatus(BATCH_PENDING_APPROVAL);
        updateDO.setApprovalStatus(BpmProcessInstanceStatusEnum.RUNNING.getStatus());
        updateDO.setProcessInstanceId(processInstanceId);
        payrollBatchMapper.updateById(updateDO);
    }

    private void ensureAttendanceReadyForPayroll(PayrollBatchDO batch, List<PayslipDO> slips) {
        if (batch == null || !StringUtils.hasText(batch.getPayrollMonth())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "发薪月份不能为空");
        }
        Set<Long> userIds = new HashSet<>();
        for (PayslipDO slip : emptyIfNull(slips)) {
            if (slip.getUserId() != null) {
                userIds.add(slip.getUserId());
            }
        }
        if (userIds.isEmpty()) {
            return;
        }
        List<AttendanceMonthlySettlementDO> lockedSettlements =
                attendanceMonthlySettlementMapper.selectLockedListByMonth(batch.getPayrollMonth());
        if (lockedSettlements == null || lockedSettlements.isEmpty()) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST,
                    batch.getPayrollMonth() + " 考勤月结还未锁定，不能提交发薪审批");
        }
        Set<Long> settlementIds = new HashSet<>();
        for (AttendanceMonthlySettlementDO settlement : lockedSettlements) {
            if (settlement.getId() != null) {
                settlementIds.add(settlement.getId());
            }
        }
        if (settlementIds.isEmpty()) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST,
                    batch.getPayrollMonth() + " 考勤月结还未锁定，不能提交发薪审批");
        }
        Map<Long, AttendanceMonthlyConfirmDO> confirmMap = new HashMap<>();
        for (AttendanceMonthlyConfirmDO confirm : emptyIfNull(
                attendanceMonthlyConfirmMapper.selectListBySettlementIdsAndUserIds(settlementIds, userIds))) {
            if (confirm.getUserId() != null && !confirmMap.containsKey(confirm.getUserId())) {
                confirmMap.put(confirm.getUserId(), confirm);
            }
        }
        long missingCount = 0;
        long openCount = 0;
        for (Long userId : userIds) {
            AttendanceMonthlyConfirmDO confirm = confirmMap.get(userId);
            if (confirm == null) {
                missingCount++;
                continue;
            }
            if (!ATTENDANCE_CONFIRM_CONFIRMED.equals(confirm.getStatus())
                    && !ATTENDANCE_CONFIRM_RESOLVED.equals(confirm.getStatus())) {
                openCount++;
            }
        }
        if (missingCount > 0 || openCount > 0) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST,
                    "考勤月结未闭环：还有 " + missingCount + " 人缺少确认单，"
                            + openCount + " 人未确认或存在异议，不能提交发薪审批");
        }
    }

    private void closePayrollApproval(PayrollBatchDO batch, String status, String processInstanceId, Integer bpmStatus) {
        PayrollBatchDO updateDO = new PayrollBatchDO();
        updateDO.setId(batch.getId());
        updateDO.setStatus(status);
        updateDO.setApprovalStatus(bpmStatus);
        updateDO.setProcessInstanceId(StringUtils.hasText(processInstanceId) ? processInstanceId : batch.getProcessInstanceId());
        payrollBatchMapper.updateById(updateDO);
        recordPayrollBatchLog(batch, "payroll_approval_close", "发薪审批结束",
                batch.getPayrollMonth() + " 发薪审批状态：" + status);
        refreshBatchSummary(getBatch(batch.getId()));
        refreshTodoTasksQuietly();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean lock(PayrollBatchActionReqVO reqVO) {
        PayrollBatchDO batch = getBatch(reqVO.getId());
        if (BATCH_LOCKED.equals(batch.getStatus())) {
            return true;
        }
        if (!BATCH_PUBLISHED.equals(batch.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "工资批次审批通过并发布后才能锁定");
        }
        PayrollSummary summary = buildSummary(batch.getId());
        if (summary.draftCount > 0 || summary.pendingCount > 0 || summary.issueCount > 0) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST,
                    "还有未发布、未确认或存在异议的工资条，处理后才能锁定");
        }
        if (summary.totalCount == 0) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "工资批次没有工资条，不能锁定");
        }
        PayrollBatchDO updateDO = new PayrollBatchDO();
        updateDO.setId(batch.getId());
        updateDO.setStatus(BATCH_LOCKED);
        updateDO.setLockedTime(LocalDateTime.now());
        updateDO.setLockedBy(SecurityFrameworkUtils.getLoginUserId());
        updateDO.setSummaryJson(buildSummaryJson(summary));
        payrollBatchMapper.updateById(updateDO);
        recordPayrollBatchLog(batch, "payroll_lock", "锁定工资批次",
                batch.getPayrollMonth() + " 工资批次已锁定");
        refreshTodoTasksQuietly();
        return true;
    }

    @Override
    public PageResult<PayslipRespVO> getPayslipPage(PayslipPageReqVO pageReqVO) {
        normalizePayslipPageReq(pageReqVO);
        PageResult<PayslipDO> pageResult = payslipMapper.selectPage(pageReqVO);
        List<PayslipRespVO> records = BeanUtils.toBean(pageResult.getList(), PayslipRespVO.class);
        fillPeopleInfo(pageResult.getList(), records);
        return new PageResult<>(emptyIfNull(records), pageResult.getTotal());
    }

    @Override
    public List<PayslipExportRespVO> getPayslipExportList(PayslipPageReqVO pageReqVO) {
        normalizePayslipPageReq(pageReqVO);
        List<PayslipDO> rows = emptyIfNull(payslipMapper.selectExportList(pageReqVO, 10000));
        List<PayslipRespVO> records = BeanUtils.toBean(rows, PayslipRespVO.class);
        fillPeopleInfo(rows, records);
        List<PayslipExportRespVO> exportRows = BeanUtils.toBean(records, PayslipExportRespVO.class);
        for (int i = 0; i < rows.size() && i < exportRows.size(); i++) {
            exportRows.get(i).setStatusName(payslipStatusLabel(rows.get(i).getStatus()));
            recordPayslipLog(rows.get(i), "payslip_export", "导出工资条",
                    "导出 " + rows.get(i).getPayrollMonth() + " 工资条");
        }
        return exportRows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updatePayslip(PayslipUpdateReqVO reqVO) {
        PayslipDO slip = getSlip(reqVO.getId());
        PayrollBatchDO batch = getBatch(slip.getBatchId());
        ensureBatchEditable(batch);
        BigDecimal netSalary = calculateNet(reqVO);
        LambdaUpdateWrapper<PayslipDO> update = new LambdaUpdateWrapper<PayslipDO>()
                .eq(PayslipDO::getId, slip.getId())
                .set(PayslipDO::getCurrency, StringUtils.hasText(reqVO.getCurrency()) ? reqVO.getCurrency().trim() : "CNY")
                .set(PayslipDO::getBaseSalary, nvl(reqVO.getBaseSalary()))
                .set(PayslipDO::getAttendanceDeduction, nvl(reqVO.getAttendanceDeduction()))
                .set(PayslipDO::getOvertimePay, nvl(reqVO.getOvertimePay()))
                .set(PayslipDO::getBonus, nvl(reqVO.getBonus()))
                .set(PayslipDO::getAllowance, nvl(reqVO.getAllowance()))
                .set(PayslipDO::getDeduction, nvl(reqVO.getDeduction()))
                .set(PayslipDO::getSocialInsurance, nvl(reqVO.getSocialInsurance()))
                .set(PayslipDO::getHousingFund, nvl(reqVO.getHousingFund()))
                .set(PayslipDO::getTax, nvl(reqVO.getTax()))
                .set(PayslipDO::getNetSalary, netSalary)
                .set(PayslipDO::getLineItemJson, reqVO.getLineItemJson());
        if (BATCH_PUBLISHED.equals(batch.getStatus())) {
            update.set(PayslipDO::getStatus, SLIP_PUBLISHED)
                    .set(PayslipDO::getConfirmedTime, null)
                    .set(PayslipDO::getResolvedTime, null)
                    .set(PayslipDO::getResolvedBy, null)
                    .set(PayslipDO::getResolveRemark, null);
        }
        payslipMapper.update(null, update);
        recordPayslipLog(slip, "payslip_update", "修改工资条",
                "修改 " + slip.getPayrollMonth() + " 工资条，实发金额调整为 " + netSalary);
        refreshBatchSummary(batch);
        refreshTodoTasksQuietly();
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean resolvePayslip(PayslipResolveReqVO reqVO) {
        PayslipDO slip = getSlip(reqVO.getId());
        ensureBatchEditable(getBatch(slip.getBatchId()));
        if (!SLIP_ISSUE.equals(slip.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "只有异议状态可以处理");
        }
        PayslipDO updateDO = new PayslipDO();
        updateDO.setId(slip.getId());
        updateDO.setStatus(SLIP_RESOLVED);
        updateDO.setResolvedTime(LocalDateTime.now());
        updateDO.setResolvedBy(SecurityFrameworkUtils.getLoginUserId());
        updateDO.setResolveRemark(StringUtils.hasText(reqVO.getResolveRemark()) ? reqVO.getResolveRemark().trim() : null);
        payslipMapper.updateById(updateDO);
        recordPayslipLog(slip, "payslip_issue_resolve", "处理工资条异议",
                "处理 " + slip.getPayrollMonth() + " 工资条异议：" + defaultText(updateDO.getResolveRemark(), "-"));
        refreshBatchSummary(getBatch(slip.getBatchId()));
        refreshTodoTasksQuietly();
        return true;
    }

    @Override
    public PageResult<PayslipRespVO> getMyPayslipPage(PayslipPageReqVO pageReqVO) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            return new PageResult<>(new ArrayList<>(), 0L);
        }
        normalizePayslipPageReq(pageReqVO);
        pageReqVO.setUserId(loginUserId);
        PageResult<PayslipDO> pageResult = payslipMapper.selectEmployeePage(pageReqVO);
        List<PayslipRespVO> records = BeanUtils.toBean(pageResult.getList(), PayslipRespVO.class);
        fillPeopleInfo(pageResult.getList(), records);
        return new PageResult<>(emptyIfNull(records), pageResult.getTotal());
    }

    @Override
    public PayrollIncomeSummaryRespVO getMyIncomeSummary(Integer year) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        PayrollIncomeSummaryRespVO summary = new PayrollIncomeSummaryRespVO();
        if (loginUserId == null) {
            return summary;
        }

        LocalDate now = LocalDate.now();
        int resolvedYear = year == null ? now.getYear() : year;
        if (resolvedYear < 1970 || resolvedYear > 2100) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "年份不合法");
        }
        LocalDate start = LocalDate.of(resolvedYear, 1, 1);
        LocalDate end = LocalDate.of(resolvedYear, 12, 31);
        List<PayslipDO> slips = payslipMapper.selectEmployeeYearList(
                loginUserId, start.format(MONTH_FORMATTER), end.format(MONTH_FORMATTER));

        summary.setYear(resolvedYear);
        slips = emptyIfNull(slips);
        if (slips.isEmpty()) {
            return summary;
        }

        Map<String, PayrollIncomeSummaryRespVO.MonthSummary> monthMap = new TreeMap<>();
        for (PayslipDO slip : slips) {
            if (slip == null) {
                continue;
            }
            summary.setPayslipCount(summary.getPayslipCount() + 1);
            if (StringUtils.hasText(slip.getCurrency())) {
                summary.setCurrency(slip.getCurrency().trim());
            }
            summary.setGrossIncomeTotal(summary.getGrossIncomeTotal().add(calculateGrossIncome(slip)));
            summary.setPayableSalaryTotal(summary.getPayableSalaryTotal().add(calculatePayableSalary(slip)));
            summary.setBaseSalaryTotal(summary.getBaseSalaryTotal().add(nvl(slip.getBaseSalary())));
            summary.setAttendanceDeductionTotal(summary.getAttendanceDeductionTotal().add(nvl(slip.getAttendanceDeduction())));
            summary.setOvertimePayTotal(summary.getOvertimePayTotal().add(nvl(slip.getOvertimePay())));
            summary.setBonusTotal(summary.getBonusTotal().add(nvl(slip.getBonus())));
            summary.setAllowanceTotal(summary.getAllowanceTotal().add(nvl(slip.getAllowance())));
            summary.setDeductionTotal(summary.getDeductionTotal().add(nvl(slip.getDeduction())));
            summary.setSocialInsuranceTotal(summary.getSocialInsuranceTotal().add(nvl(slip.getSocialInsurance())));
            summary.setHousingFundTotal(summary.getHousingFundTotal().add(nvl(slip.getHousingFund())));
            summary.setTaxTotal(summary.getTaxTotal().add(nvl(slip.getTax())));
            summary.setNetSalaryTotal(summary.getNetSalaryTotal().add(nvl(slip.getNetSalary())));
            addStatusCount(summary, slip.getStatus());

            String payrollMonth = slip.getPayrollMonth();
            if (!StringUtils.hasText(payrollMonth)) {
                continue;
            }
            PayrollIncomeSummaryRespVO.MonthSummary monthSummary = monthMap.computeIfAbsent(payrollMonth, key -> {
                PayrollIncomeSummaryRespVO.MonthSummary item = new PayrollIncomeSummaryRespVO.MonthSummary();
                item.setPayrollMonth(key);
                return item;
            });
            monthSummary.setPayslipCount(monthSummary.getPayslipCount() + 1);
            monthSummary.setGrossIncome(monthSummary.getGrossIncome().add(calculateGrossIncome(slip)));
            monthSummary.setPayableSalary(monthSummary.getPayableSalary().add(calculatePayableSalary(slip)));
            monthSummary.setBaseSalary(monthSummary.getBaseSalary().add(nvl(slip.getBaseSalary())));
            monthSummary.setAttendanceDeduction(monthSummary.getAttendanceDeduction().add(nvl(slip.getAttendanceDeduction())));
            monthSummary.setOvertimePay(monthSummary.getOvertimePay().add(nvl(slip.getOvertimePay())));
            monthSummary.setBonus(monthSummary.getBonus().add(nvl(slip.getBonus())));
            monthSummary.setAllowance(monthSummary.getAllowance().add(nvl(slip.getAllowance())));
            monthSummary.setDeduction(monthSummary.getDeduction().add(nvl(slip.getDeduction())));
            monthSummary.setSocialInsurance(monthSummary.getSocialInsurance().add(nvl(slip.getSocialInsurance())));
            monthSummary.setHousingFund(monthSummary.getHousingFund().add(nvl(slip.getHousingFund())));
            monthSummary.setTax(monthSummary.getTax().add(nvl(slip.getTax())));
            monthSummary.setNetSalary(monthSummary.getNetSalary().add(nvl(slip.getNetSalary())));
            addStatusCount(monthSummary, slip.getStatus());
        }

        summary.setMonthCount(monthMap.size());
        summary.setMonths(new ArrayList<>(monthMap.values()));
        return summary;
    }

    @Override
    public PayrollTaxSummaryRespVO getMyTaxSummary(Integer year) {
        PayrollIncomeSummaryRespVO incomeSummary = getMyIncomeSummary(year);
        PayrollTaxSummaryRespVO taxSummary = new PayrollTaxSummaryRespVO();
        taxSummary.setYear(incomeSummary.getYear());
        taxSummary.setCurrency(incomeSummary.getCurrency());
        taxSummary.setMonthCount(incomeSummary.getMonthCount());
        taxSummary.setPayslipCount(incomeSummary.getPayslipCount());
        taxSummary.setPendingCount(incomeSummary.getPendingCount());
        taxSummary.setConfirmedCount(incomeSummary.getConfirmedCount());
        taxSummary.setIssueCount(incomeSummary.getIssueCount());
        taxSummary.setResolvedCount(incomeSummary.getResolvedCount());
        taxSummary.setPayableSalaryTotal(incomeSummary.getPayableSalaryTotal());
        taxSummary.setSocialInsuranceTotal(incomeSummary.getSocialInsuranceTotal());
        taxSummary.setHousingFundTotal(incomeSummary.getHousingFundTotal());
        taxSummary.setTaxTotal(incomeSummary.getTaxTotal());
        taxSummary.setNetSalaryTotal(incomeSummary.getNetSalaryTotal());

        List<PayrollTaxSummaryRespVO.MonthTaxSummary> monthTaxes = new ArrayList<>();
        BigDecimal withholdingBaseTotal = BigDecimal.ZERO;
        for (PayrollIncomeSummaryRespVO.MonthSummary month : emptyIfNull(incomeSummary.getMonths())) {
            PayrollTaxSummaryRespVO.MonthTaxSummary monthTax = new PayrollTaxSummaryRespVO.MonthTaxSummary();
            monthTax.setPayrollMonth(month.getPayrollMonth());
            monthTax.setPayslipCount(month.getPayslipCount());
            monthTax.setPendingCount(month.getPendingCount());
            monthTax.setConfirmedCount(month.getConfirmedCount());
            monthTax.setIssueCount(month.getIssueCount());
            monthTax.setResolvedCount(month.getResolvedCount());
            monthTax.setPayableSalary(month.getPayableSalary());
            monthTax.setSocialInsurance(month.getSocialInsurance());
            monthTax.setHousingFund(month.getHousingFund());
            monthTax.setWithholdingBase(calculateWithholdingBase(
                    month.getPayableSalary(), month.getSocialInsurance(), month.getHousingFund()));
            monthTax.setTax(month.getTax());
            monthTax.setNetSalary(month.getNetSalary());
            withholdingBaseTotal = withholdingBaseTotal.add(monthTax.getWithholdingBase());
            monthTaxes.add(monthTax);
        }
        taxSummary.setWithholdingBaseTotal(withholdingBaseTotal);
        taxSummary.setMonths(monthTaxes);
        return taxSummary;
    }

    @Override
    public PayrollReportRespVO getReport(Integer year) {
        LocalDate now = LocalDate.now();
        int resolvedYear = year == null ? now.getYear() : year;
        if (resolvedYear < 1970 || resolvedYear > 2100) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "年份不合法");
        }

        String startMonth = LocalDate.of(resolvedYear, 1, 1).format(MONTH_FORMATTER);
        String endMonth = LocalDate.of(resolvedYear, 12, 1).format(MONTH_FORMATTER);
        List<PayrollBatchDO> batches = emptyIfNull(payrollBatchMapper.selectList(new LambdaQueryWrapperX<PayrollBatchDO>()
                .between(PayrollBatchDO::getPayrollMonth, startMonth, endMonth)
                .orderByAsc(PayrollBatchDO::getPayrollMonth)
                .orderByAsc(PayrollBatchDO::getId)));
        List<PayslipDO> slips = emptyIfNull(payslipMapper.selectList(new LambdaQueryWrapperX<PayslipDO>()
                .between(PayslipDO::getPayrollMonth, startMonth, endMonth)
                .orderByAsc(PayslipDO::getPayrollMonth)
                .orderByAsc(PayslipDO::getProfileId)
                .orderByAsc(PayslipDO::getId)));

        PayrollReportRespVO report = new PayrollReportRespVO();
        report.setYear(resolvedYear);
        report.setBatchCount(batches.size());

        Map<String, Integer> batchCountByMonth = buildBatchCountByMonth(batches);
        Map<String, PayrollReportRespVO.MonthSummary> monthMap = new TreeMap<>();
        Map<String, Set<Long>> monthProfileMap = new HashMap<>();
        for (Map.Entry<String, Integer> entry : batchCountByMonth.entrySet()) {
            PayrollReportRespVO.MonthSummary month = new PayrollReportRespVO.MonthSummary();
            month.setPayrollMonth(entry.getKey());
            month.setBatchCount(entry.getValue());
            monthMap.put(entry.getKey(), month);
        }

        Set<Long> reportProfileIds = new HashSet<>();
        for (PayslipDO slip : slips) {
            addIfNotNull(reportProfileIds, slip.getProfileId());
        }
        Map<Long, List<EmployeeEntryDO>> entryMap = loadEntryMap(reportProfileIds);
        Map<String, SocialSecurityAccountDO> socialSecurityMap = loadSocialSecurityByProfileAndMonth(
                reportProfileIds, startMonth, endMonth);

        Map<Long, PayrollReportRespVO.DepartmentSummary> departmentMap = new HashMap<>();
        Map<Long, Set<Long>> departmentProfileMap = new HashMap<>();
        Set<Long> deptIds = new HashSet<>();

        for (PayslipDO slip : slips) {
            report.setPayslipCount(report.getPayslipCount() + 1);
            if (StringUtils.hasText(slip.getCurrency())) {
                report.setCurrency(slip.getCurrency().trim());
            }
            SocialSecurityAccountDO socialSecurity = socialSecurityMap.get(reportSocialKey(
                    slip.getProfileId(), slip.getPayrollMonth()));
            PayrollReportAmounts amounts = buildReportAmounts(slip, socialSecurity);
            addReportTotals(report, amounts);
            addReportStatusCount(report, slip.getStatus());

            if (StringUtils.hasText(slip.getPayrollMonth())) {
                PayrollReportRespVO.MonthSummary monthSummary = monthMap.computeIfAbsent(slip.getPayrollMonth(), key -> {
                    PayrollReportRespVO.MonthSummary item = new PayrollReportRespVO.MonthSummary();
                    item.setPayrollMonth(key);
                    item.setBatchCount(batchCountByMonth.getOrDefault(key, 0));
                    return item;
                });
                monthSummary.setPayslipCount(monthSummary.getPayslipCount() + 1);
                addReportTotals(monthSummary, amounts);
                addReportStatusCount(monthSummary, slip.getStatus());
                if (slip.getProfileId() != null) {
                    monthProfileMap.computeIfAbsent(slip.getPayrollMonth(), key -> new HashSet<>())
                            .add(slip.getProfileId());
                }
            }

            EmployeeEntryDO employeeEntry = resolveEntryForMonth(entryMap.get(slip.getProfileId()), slip.getPayrollMonth());
            Long deptId = employeeEntry == null ? null : employeeEntry.getDeptId();
            addIfNotNull(deptIds, deptId);
            Long deptKey = deptId == null ? -1L : deptId;
            PayrollReportRespVO.DepartmentSummary deptSummary = departmentMap.computeIfAbsent(deptKey, key -> {
                PayrollReportRespVO.DepartmentSummary item = new PayrollReportRespVO.DepartmentSummary();
                item.setDeptId(deptId);
                return item;
            });
            deptSummary.setPayslipCount(deptSummary.getPayslipCount() + 1);
            addReportTotals(deptSummary, amounts);
            if (slip.getProfileId() != null) {
                departmentProfileMap.computeIfAbsent(deptKey, key -> new HashSet<>()).add(slip.getProfileId());
            }
        }

        Map<Long, DeptRespDTO> deptMap = loadDeptMap(deptIds);
        List<PayrollReportRespVO.DepartmentSummary> departments = new ArrayList<>(departmentMap.values());
        for (PayrollReportRespVO.DepartmentSummary department : departments) {
            Long deptKey = department.getDeptId() == null ? -1L : department.getDeptId();
            Set<Long> profileIds = departmentProfileMap.get(deptKey);
            department.setProfileCount(profileIds == null ? 0 : profileIds.size());
            department.setDeptName(deptName(deptMap, department.getDeptId()));
            fillPerCapita(department);
        }
        departments.sort(Comparator.comparing(PayrollReportRespVO.DepartmentSummary::getLaborCostTotal).reversed());

        report.setProfileCount(reportProfileIds.size());
        report.setDeptCount(departments.size());
        fillPerCapita(report);
        for (PayrollReportRespVO.MonthSummary month : monthMap.values()) {
            Set<Long> monthProfileIds = monthProfileMap.get(month.getPayrollMonth());
            month.setProfileCount(monthProfileIds == null ? 0 : monthProfileIds.size());
            fillPerCapita(month);
        }
        report.setDepartments(departments);
        report.setMonths(new ArrayList<>(monthMap.values()));
        report.setSalaryStructures(buildSalaryStructures(report));
        return report;
    }

    @Override
    public PageResult<SocialSecurityAccountRespVO> getMySocialSecurityPage(SocialSecurityAccountPageReqVO pageReqVO) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            return new PageResult<>(new ArrayList<>(), 0L);
        }
        EmployeeProfileDO profile = employeeProfileMapper.selectByUserId(loginUserId);
        if (profile == null || profile.getId() == null) {
            return new PageResult<>(new ArrayList<>(), 0L);
        }
        normalizeSocialPageReq(pageReqVO);
        pageReqVO.setProfileId(profile.getId());
        pageReqVO.setProfileIds(null);
        pageReqVO.setProfileName(null);
        pageReqVO.setProfileMobile(null);
        PageResult<SocialSecurityAccountDO> pageResult = socialSecurityAccountMapper.selectPage(pageReqVO);
        List<SocialSecurityAccountRespVO> records = BeanUtils.toBean(pageResult.getList(), SocialSecurityAccountRespVO.class);
        fillSocialSecurityInfo(pageResult.getList(), records);
        return new PageResult<>(emptyIfNull(records), pageResult.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean confirmMy(PayslipActionReqVO reqVO) {
        PayslipDO slip = getMySlip(reqVO.getId());
        ensureBatchEditable(getBatch(slip.getBatchId()));
        if (SLIP_CONFIRMED.equals(slip.getStatus())) {
            return true;
        }
        if (SLIP_ISSUE.equals(slip.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "工资条异议处理中，需HR处理后再确认");
        }
        if (SLIP_DRAFT.equals(slip.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "工资条尚未发布");
        }
        PayslipDO updateDO = new PayslipDO();
        updateDO.setId(slip.getId());
        updateDO.setStatus(SLIP_CONFIRMED);
        updateDO.setConfirmedTime(LocalDateTime.now());
        payslipMapper.updateById(updateDO);
        recordPayslipLog(slip, "payslip_confirm", "确认工资条",
                "员工确认 " + slip.getPayrollMonth() + " 工资条");
        refreshBatchSummary(getBatch(slip.getBatchId()));
        refreshTodoTasksQuietly();
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean issueMy(PayslipIssueReqVO reqVO) {
        PayslipDO slip = getMySlip(reqVO.getId());
        ensureBatchEditable(getBatch(slip.getBatchId()));
        if (SLIP_DRAFT.equals(slip.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "工资条尚未发布");
        }
        payslipMapper.update(null, new LambdaUpdateWrapper<PayslipDO>()
                .eq(PayslipDO::getId, slip.getId())
                .set(PayslipDO::getStatus, SLIP_ISSUE)
                .set(PayslipDO::getConfirmedTime, null)
                .set(PayslipDO::getIssueTime, LocalDateTime.now())
                .set(PayslipDO::getIssueRemark, reqVO.getIssueRemark().trim())
                .set(PayslipDO::getResolvedTime, null)
                .set(PayslipDO::getResolvedBy, null)
                .set(PayslipDO::getResolveRemark, null));
        recordPayslipLog(slip, "payslip_issue", "提交工资条异议",
                "员工对 " + slip.getPayrollMonth() + " 工资条提交异议：" + reqVO.getIssueRemark().trim());
        refreshBatchSummary(getBatch(slip.getBatchId()));
        refreshTodoTasksQuietly();
        return true;
    }

    @Override
    public PageResult<SocialSecurityAccountRespVO> getSocialSecurityPage(SocialSecurityAccountPageReqVO pageReqVO) {
        normalizeSocialPageReq(pageReqVO);
        if (!prepareSocialProfileFilter(pageReqVO)) {
            return new PageResult<>(new ArrayList<>(), 0L);
        }
        PageResult<SocialSecurityAccountDO> pageResult = socialSecurityAccountMapper.selectPage(pageReqVO);
        List<SocialSecurityAccountRespVO> records = BeanUtils.toBean(pageResult.getList(), SocialSecurityAccountRespVO.class);
        fillSocialSecurityInfo(pageResult.getList(), records);
        return new PageResult<>(emptyIfNull(records), pageResult.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveSocialSecurity(SocialSecurityAccountSaveReqVO reqVO) {
        validateProfileExists(reqVO.getProfileId());
        SocialSecurityAccountDO account = BeanUtils.toBean(reqVO, SocialSecurityAccountDO.class);
        normalizeSocialAccount(account);
        ensureSocialSecurityEditable(account.getSocialMonth());
        if (account.getId() == null) {
            socialSecurityAccountMapper.insert(account);
            recordPayrollOperationLog(account.getProfileId(), "social_security_create", "新增社保台账",
                    account.getSocialMonth() + " 社保公积金台账已新增");
        } else {
            SocialSecurityAccountDO existed = socialSecurityAccountMapper.selectById(account.getId());
            if (existed == null) {
                throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND, "社保台账不存在");
            }
            ensureSocialSecurityEditable(existed.getSocialMonth());
            socialSecurityAccountMapper.updateById(account);
            recordPayrollOperationLog(account.getProfileId(), "social_security_update", "修改社保台账",
                    account.getSocialMonth() + " 社保公积金台账已修改");
        }
        refreshTodoTasksQuietly();
        return account.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSocialSecurity(Long id) {
        SocialSecurityAccountDO account = socialSecurityAccountMapper.selectById(id);
        if (account == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND, "社保台账不存在");
        }
        ensureSocialSecurityEditable(account.getSocialMonth());
        recordPayrollOperationLog(account.getProfileId(), "social_security_delete", "删除社保台账",
                account.getSocialMonth() + " 社保公积金台账已删除");
        socialSecurityAccountMapper.deleteById(id);
        refreshTodoTasksQuietly();
    }

    @Override
    public PageResult<PayrollSchemeRespVO> getSchemePage(PayrollSchemePageReqVO pageReqVO) {
        normalizeSchemePageReq(pageReqVO);
        PageResult<PayrollSchemeDO> pageResult = payrollSchemeMapper.selectPage(pageReqVO);
        List<PayrollSchemeRespVO> records = BeanUtils.toBean(pageResult.getList(), PayrollSchemeRespVO.class);
        return new PageResult<>(emptyIfNull(records), pageResult.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveScheme(PayrollSchemeSaveReqVO reqVO) {
        PayrollSchemeDO scheme = BeanUtils.toBean(reqVO, PayrollSchemeDO.class);
        normalizeScheme(scheme);
        if (Boolean.TRUE.equals(scheme.getDefaultFlag()) && SCHEME_ACTIVE.equals(scheme.getStatus())) {
            clearOtherDefaultSchemes(scheme.getId());
        }
        if (scheme.getId() == null) {
            payrollSchemeMapper.insert(scheme);
        } else {
            if (payrollSchemeMapper.selectById(scheme.getId()) == null) {
                throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND, "薪资方案不存在");
            }
            payrollSchemeMapper.updateById(scheme);
        }
        return scheme.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean enableScheme(Long id) {
        PayrollSchemeDO scheme = getScheme(id);
        PayrollSchemeDO updateDO = new PayrollSchemeDO();
        updateDO.setId(scheme.getId());
        updateDO.setStatus(SCHEME_ACTIVE);
        updateDO.setDefaultFlag(true);
        clearOtherDefaultSchemes(scheme.getId());
        payrollSchemeMapper.updateById(updateDO);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteScheme(Long id) {
        PayrollSchemeDO scheme = getScheme(id);
        if (SCHEME_ACTIVE.equals(scheme.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "启用中的薪资方案不能删除，请先停用");
        }
        payrollSchemeMapper.deleteById(id);
    }

    private void generatePayslips(PayrollBatchDO batch, LocalDate month) {
        List<EmployeeProfileDO> profiles = employeeProfileMapper.selectList(new LambdaQueryWrapperX<EmployeeProfileDO>()
                .eq(EmployeeProfileDO::getStatus, 1)
                .isNotNull(EmployeeProfileDO::getUserId)
                .orderByAsc(EmployeeProfileDO::getId));
        if (profiles == null || profiles.isEmpty()) {
            return;
        }
        Set<Long> profileIds = new HashSet<>();
        for (EmployeeProfileDO profile : profiles) {
            if (profile.getId() != null) {
                profileIds.add(profile.getId());
            }
        }
        Map<Long, EmployeeSalaryDO> salaryMap = loadLatestSalaryMap(profileIds, month.with(TemporalAdjusters.lastDayOfMonth()));
        Map<Long, SocialSecurityAccountDO> socialSecurityMap = loadSocialSecurityMap(profileIds, batch.getPayrollMonth());
        PayrollSchemeDO defaultScheme = payrollSchemeMapper.selectDefaultActive(month.with(TemporalAdjusters.lastDayOfMonth()));
        Map<Long, AttendancePayrollSnapshot> attendanceMap = loadAttendanceSnapshotMap(profiles, batch.getPayrollMonth());
        for (EmployeeProfileDO profile : profiles) {
            if (profile.getUserId() == null) {
                continue;
            }
            EmployeeSalaryDO salary = salaryMap.get(profile.getId());
            SocialSecurityAccountDO socialSecurity = socialSecurityMap.get(profile.getId());
            BigDecimal baseSalary = resolveBaseSalary(salary, defaultScheme);
            String currency = resolveCurrency(salary, defaultScheme);
            AttendancePayrollSnapshot attendance = attendanceMap.getOrDefault(profile.getUserId(),
                    AttendancePayrollSnapshot.missing(batch.getPayrollMonth(), profile.getUserId()));
            BigDecimal attendanceDeduction = calculateAttendanceDeduction(attendance, defaultScheme);
            PayslipDO existing = payslipMapper.selectByBatchIdAndUserId(batch.getId(), profile.getUserId());
            if (existing == null) {
                PayslipDO insertDO = buildGeneratedSlip(batch, profile, currency, baseSalary, defaultScheme, socialSecurity,
                        attendanceDeduction, attendance);
                payslipMapper.insert(insertDO);
            } else if (SLIP_DRAFT.equals(existing.getStatus())) {
                PayslipDO updateDO = buildGeneratedSlip(batch, profile, currency, baseSalary, defaultScheme, socialSecurity,
                        attendanceDeduction, attendance);
                updateDO.setId(existing.getId());
                payslipMapper.updateById(updateDO);
            }
        }
    }

    private PayslipDO buildGeneratedSlip(PayrollBatchDO batch, EmployeeProfileDO profile, String currency,
                                         BigDecimal baseSalary, PayrollSchemeDO defaultScheme, SocialSecurityAccountDO socialSecurity,
                                         BigDecimal attendanceDeduction, AttendancePayrollSnapshot attendance) {
        BigDecimal socialInsurance = socialSecurityPersonalTotal(socialSecurity);
        BigDecimal housingFund = isSocialPayableStatus(socialSecurity) ? nvl(socialSecurity.getFundPersonal()) : BigDecimal.ZERO;
        BigDecimal allowance = defaultScheme == null ? BigDecimal.ZERO : nvl(defaultScheme.getAllowance());
        BigDecimal deduction = defaultScheme == null ? BigDecimal.ZERO : nvl(defaultScheme.getDeduction());
        BigDecimal payableSalary = baseSalary.add(allowance).subtract(nvl(attendanceDeduction)).subtract(deduction);
        BigDecimal tax = calculateMonthlyTax(defaultScheme, payableSalary, socialInsurance, housingFund);
        PayslipDO slip = new PayslipDO();
        slip.setBatchId(batch.getId());
        slip.setPayrollMonth(batch.getPayrollMonth());
        slip.setProfileId(profile.getId());
        slip.setUserId(profile.getUserId());
        slip.setCurrency(currency);
        slip.setBaseSalary(baseSalary);
        slip.setAttendanceDeduction(nvl(attendanceDeduction));
        slip.setOvertimePay(BigDecimal.ZERO);
        slip.setBonus(BigDecimal.ZERO);
        slip.setAllowance(allowance);
        slip.setDeduction(deduction);
        slip.setSocialInsurance(socialInsurance);
        slip.setHousingFund(housingFund);
        slip.setTax(tax);
        slip.setNetSalary(payableSalary.subtract(socialInsurance).subtract(housingFund).subtract(tax));
        slip.setLineItemJson(buildLineItemJson(baseSalary, allowance, deduction, socialInsurance, housingFund,
                attendanceDeduction, tax, defaultScheme, attendance, socialSecurity));
        slip.setStatus(SLIP_DRAFT);
        return slip;
    }

    private Map<Long, AttendancePayrollSnapshot> loadAttendanceSnapshotMap(List<EmployeeProfileDO> profiles,
                                                                            String payrollMonth) {
        Map<Long, AttendancePayrollSnapshot> result = new HashMap<>();
        if (profiles == null || profiles.isEmpty() || !StringUtils.hasText(payrollMonth)) {
            return result;
        }
        Set<Long> userIds = new HashSet<>();
        for (EmployeeProfileDO profile : profiles) {
            if (profile.getUserId() != null) {
                userIds.add(profile.getUserId());
                result.put(profile.getUserId(), AttendancePayrollSnapshot.missing(payrollMonth, profile.getUserId()));
            }
        }
        if (userIds.isEmpty()) {
            return result;
        }

        Map<Long, AttendanceMonthlySettlementDO> settlementMap = new HashMap<>();
        Set<Long> settlementIds = new HashSet<>();
        for (AttendanceMonthlySettlementDO settlement : emptyIfNull(attendanceMonthlySettlementMapper.selectListByMonth(payrollMonth))) {
            if (settlement.getId() == null) {
                continue;
            }
            settlementIds.add(settlement.getId());
            settlementMap.put(settlement.getId(), settlement);
        }
        if (!settlementIds.isEmpty()) {
            for (AttendanceMonthlyConfirmDO confirm : emptyIfNull(
                    attendanceMonthlyConfirmMapper.selectListBySettlementIdsAndUserIds(settlementIds, userIds))) {
                if (confirm.getUserId() == null) {
                    continue;
                }
                AttendancePayrollSnapshot current = result.get(confirm.getUserId());
                AttendanceMonthlySettlementDO settlement = settlementMap.get(confirm.getSettlementId());
                if (shouldUseConfirmSnapshot(current, confirm, settlement)) {
                    AttendancePayrollSnapshot snapshot = current == null
                            ? AttendancePayrollSnapshot.missing(payrollMonth, confirm.getUserId()) : current;
                    snapshot.settlementId = confirm.getSettlementId();
                    snapshot.setSettlementStatus(settlement == null ? null : settlement.getStatus());
                    snapshot.setConfirmId(confirm.getId());
                    snapshot.setConfirmStatus(confirm.getStatus());
                    result.put(confirm.getUserId(), snapshot);
                }
            }
        }

        LocalDate month = LocalDate.parse(payrollMonth + "-01");
        List<AttendanceDailyResultDO> dailyResults = attendanceDailyResultMapper.selectListByMonthAndUserIds(
                month.withDayOfMonth(1), month.with(TemporalAdjusters.lastDayOfMonth()), userIds);
        Set<String> seenDailyRows = new HashSet<>();
        for (AttendanceDailyResultDO row : emptyIfNull(dailyResults)) {
            if (row.getUserId() == null || row.getAttendanceDate() == null) {
                continue;
            }
            String dailyKey = row.getUserId() + "#" + row.getAttendanceDate();
            if (!seenDailyRows.add(dailyKey)) {
                continue;
            }
            result.computeIfAbsent(row.getUserId(),
                    key -> AttendancePayrollSnapshot.missing(payrollMonth, key)).record(row);
        }
        return result;
    }

    private boolean shouldUseConfirmSnapshot(AttendancePayrollSnapshot current, AttendanceMonthlyConfirmDO confirm,
                                             AttendanceMonthlySettlementDO settlement) {
        if (confirm == null) {
            return false;
        }
        if (current == null || current.confirmId == null) {
            return true;
        }
        boolean nextLocked = settlement != null && ATTENDANCE_SETTLEMENT_LOCKED.equals(settlement.getStatus());
        boolean currentLocked = ATTENDANCE_SETTLEMENT_LOCKED.equals(current.settlementStatus);
        if (nextLocked != currentLocked) {
            return nextLocked;
        }
        return confirm.getId() != null && confirm.getId() > current.confirmId;
    }

    private BigDecimal calculateAttendanceDeduction(AttendancePayrollSnapshot attendance, PayrollSchemeDO defaultScheme) {
        if (attendance == null) {
            return BigDecimal.ZERO;
        }
        Map<String, Object> rule = parseAttendanceRule(defaultScheme);
        BigDecimal total = BigDecimal.ZERO;
        total = total.add(nvl(attendance.absentHours).multiply(ruleAmount(rule, "absenceHourlyDeduction")));
        total = total.add(BigDecimal.valueOf(attendance.lateMinutes).multiply(ruleAmount(rule, "lateMinuteDeduction")));
        total = total.add(BigDecimal.valueOf(attendance.earlyLeaveMinutes).multiply(ruleAmount(rule, "earlyMinuteDeduction")));
        total = total.add(BigDecimal.valueOf(attendance.lateCount).multiply(ruleAmount(rule, "lateDeduction")));
        total = total.add(BigDecimal.valueOf(attendance.earlyLeaveCount).multiply(ruleAmount(rule, "earlyDeduction")));
        return total.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, Object> parseAttendanceRule(PayrollSchemeDO defaultScheme) {
        if (defaultScheme == null || !StringUtils.hasText(defaultScheme.getAttendanceRuleJson())) {
            return new HashMap<>();
        }
        Map<String, Object> rule = JsonUtils.parseObjectQuietly(defaultScheme.getAttendanceRuleJson(),
                new TypeReference<Map<String, Object>>() {});
        return rule == null ? new HashMap<>() : rule;
    }

    private BigDecimal ruleAmount(Map<String, Object> rule, String key) {
        if (rule == null || !rule.containsKey(key)) {
            return BigDecimal.ZERO;
        }
        Object value = rule.get(key);
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        if (value instanceof String && StringUtils.hasText((String) value)) {
            try {
                return new BigDecimal(((String) value).trim());
            } catch (NumberFormatException ignored) {
                return BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }

    private Map<Long, EmployeeSalaryDO> loadLatestSalaryMap(Set<Long> profileIds, LocalDate monthEnd) {
        Map<Long, EmployeeSalaryDO> result = new HashMap<>();
        if (profileIds == null || profileIds.isEmpty()) {
            return result;
        }
        LambdaQueryWrapperX<EmployeeSalaryDO> salaryQuery = new LambdaQueryWrapperX<>();
        salaryQuery.in(EmployeeSalaryDO::getProfileId, profileIds);
        salaryQuery.leIfPresent(EmployeeSalaryDO::getEffectiveDate, monthEnd)
                .orderByAsc(EmployeeSalaryDO::getProfileId)
                .orderByDesc(EmployeeSalaryDO::getEffectiveDate)
                .orderByDesc(EmployeeSalaryDO::getId);
        List<EmployeeSalaryDO> salaries = employeeSalaryMapper.selectList(salaryQuery);
        for (EmployeeSalaryDO salary : emptyIfNull(salaries)) {
            if (salary.getProfileId() != null && !result.containsKey(salary.getProfileId())) {
                result.put(salary.getProfileId(), salary);
            }
        }
        return result;
    }

    private Map<Long, SocialSecurityAccountDO> loadSocialSecurityMap(Set<Long> profileIds, String socialMonth) {
        Map<Long, SocialSecurityAccountDO> result = new HashMap<>();
        if (profileIds == null || profileIds.isEmpty() || !StringUtils.hasText(socialMonth)) {
            return result;
        }
        List<SocialSecurityAccountDO> accounts = socialSecurityAccountMapper.selectListByMonthAndProfileIds(socialMonth, profileIds);
        for (SocialSecurityAccountDO account : emptyIfNull(accounts)) {
            if (account.getProfileId() != null && !result.containsKey(account.getProfileId())) {
                result.put(account.getProfileId(), account);
            }
        }
        return result;
    }

    private boolean isSocialPayableStatus(SocialSecurityAccountDO account) {
        if (account == null) {
            return false;
        }
        String status = StringUtils.hasText(account.getStatus()) ? account.getStatus().trim().toUpperCase() : SOCIAL_ENROLLED;
        return SOCIAL_ENROLLED.equals(status) || SOCIAL_PENDING_STOP.equals(status);
    }

    private Map<String, SocialSecurityAccountDO> loadSocialSecurityByProfileAndMonth(Set<Long> profileIds,
                                                                                      String startMonth,
                                                                                      String endMonth) {
        Map<String, SocialSecurityAccountDO> result = new HashMap<>();
        if (profileIds == null || profileIds.isEmpty()) {
            return result;
        }
        List<SocialSecurityAccountDO> accounts = socialSecurityAccountMapper.selectList(new LambdaQueryWrapperX<SocialSecurityAccountDO>()
                .in(SocialSecurityAccountDO::getProfileId, profileIds)
                .between(SocialSecurityAccountDO::getSocialMonth, startMonth, endMonth)
                .orderByDesc(SocialSecurityAccountDO::getId));
        for (SocialSecurityAccountDO account : emptyIfNull(accounts)) {
            String key = reportSocialKey(account.getProfileId(), account.getSocialMonth());
            if (key != null && !result.containsKey(key)) {
                result.put(key, account);
            }
        }
        return result;
    }

    private String reportSocialKey(Long profileId, String socialMonth) {
        if (profileId == null || !StringUtils.hasText(socialMonth)) {
            return null;
        }
        return profileId + "#" + socialMonth;
    }

    private Map<Long, List<EmployeeEntryDO>> loadEntryMap(Set<Long> profileIds) {
        if (profileIds == null || profileIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<EmployeeEntryDO> entries = employeeEntryMapper.selectList(new LambdaQueryWrapperX<EmployeeEntryDO>()
                .in(EmployeeEntryDO::getProfileId, profileIds)
                .orderByAsc(EmployeeEntryDO::getProfileId)
                .orderByAsc(EmployeeEntryDO::getEntryDate)
                .orderByAsc(EmployeeEntryDO::getId));
        Map<Long, List<EmployeeEntryDO>> result = new HashMap<>();
        for (EmployeeEntryDO entry : emptyIfNull(entries)) {
            if (entry.getProfileId() == null) {
                continue;
            }
            result.computeIfAbsent(entry.getProfileId(), key -> new ArrayList<>()).add(entry);
        }
        return result;
    }

    private EmployeeEntryDO resolveEntryForMonth(List<EmployeeEntryDO> entries, String payrollMonth) {
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        LocalDate monthEnd = parsePayrollMonthEnd(payrollMonth);
        EmployeeEntryDO selected = null;
        for (EmployeeEntryDO entry : entries) {
            if (entry == null) {
                continue;
            }
            if (monthEnd == null || entry.getEntryDate() == null || !entry.getEntryDate().isAfter(monthEnd)) {
                selected = entry;
            } else if (selected == null) {
                selected = entry;
            }
        }
        return selected;
    }

    private LocalDate parsePayrollMonthEnd(String payrollMonth) {
        if (!StringUtils.hasText(payrollMonth)) {
            return null;
        }
        try {
            return LocalDate.parse(payrollMonth + "-01").with(TemporalAdjusters.lastDayOfMonth());
        } catch (Exception ex) {
            return null;
        }
    }

    private Map<String, Integer> buildBatchCountByMonth(List<PayrollBatchDO> batches) {
        Map<String, Integer> result = new TreeMap<>();
        for (PayrollBatchDO batch : emptyIfNull(batches)) {
            if (!StringUtils.hasText(batch.getPayrollMonth())) {
                continue;
            }
            result.put(batch.getPayrollMonth(), result.getOrDefault(batch.getPayrollMonth(), 0) + 1);
        }
        return result;
    }

    private Map<Long, DeptRespDTO> loadDeptMap(Set<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            List<DeptRespDTO> depts = deptApi.getDeptList(deptIds).getCheckedData();
            Map<Long, DeptRespDTO> result = new HashMap<>();
            for (DeptRespDTO dept : emptyIfNull(depts)) {
                if (dept.getId() != null) {
                    result.put(dept.getId(), dept);
                }
            }
            return result;
        } catch (Exception ex) {
            log.warn("Load dept map for payroll report failed, deptIds={}, reason={}", deptIds, ex.getMessage());
            return Collections.emptyMap();
        }
    }

    private String deptName(Map<Long, DeptRespDTO> deptMap, Long deptId) {
        if (deptId == null) {
            return "未分配";
        }
        DeptRespDTO dept = deptMap.get(deptId);
        return dept == null || !StringUtils.hasText(dept.getName()) ? "部门 " + deptId : dept.getName();
    }

    private void addIfNotNull(Set<Long> values, Long value) {
        if (value != null) {
            values.add(value);
        }
    }

    private BigDecimal resolveBaseSalary(EmployeeSalaryDO salary, PayrollSchemeDO defaultScheme) {
        if (salary != null && salary.getAmount() != null) {
            return nvl(salary.getAmount());
        }
        return defaultScheme == null ? BigDecimal.ZERO : nvl(defaultScheme.getBaseSalary());
    }

    private String resolveCurrency(EmployeeSalaryDO salary, PayrollSchemeDO defaultScheme) {
        if (salary != null && StringUtils.hasText(salary.getCurrency())) {
            return salary.getCurrency().trim();
        }
        if (defaultScheme != null && StringUtils.hasText(defaultScheme.getCurrency())) {
            return defaultScheme.getCurrency().trim();
        }
        return "CNY";
    }

    private void refreshBatchSummary(PayrollBatchDO batch) {
        if (batch == null || batch.getId() == null) {
            return;
        }
        PayrollSummary summary = buildSummary(batch.getId());
        PayrollBatchDO updateDO = new PayrollBatchDO();
        updateDO.setId(batch.getId());
        updateDO.setSummaryJson(buildSummaryJson(summary));
        payrollBatchMapper.updateById(updateDO);
    }

    private PayrollSummary buildSummary(Long batchId) {
        PayrollSummary summary = new PayrollSummary();
        for (PayslipDO slip : emptyIfNull(payslipMapper.selectListByBatchId(batchId))) {
            summary.totalCount++;
            summary.baseSalaryTotal = summary.baseSalaryTotal.add(nvl(slip.getBaseSalary()));
            summary.attendanceDeductionTotal = summary.attendanceDeductionTotal.add(nvl(slip.getAttendanceDeduction()));
            summary.netSalaryTotal = summary.netSalaryTotal.add(nvl(slip.getNetSalary()));
            if (SLIP_DRAFT.equals(slip.getStatus())) {
                summary.draftCount++;
            } else if (SLIP_PUBLISHED.equals(slip.getStatus())) {
                summary.pendingCount++;
            } else if (SLIP_CONFIRMED.equals(slip.getStatus())) {
                summary.confirmedCount++;
            } else if (SLIP_ISSUE.equals(slip.getStatus())) {
                summary.issueCount++;
            } else if (SLIP_RESOLVED.equals(slip.getStatus())) {
                summary.resolvedCount++;
            }
        }
        return summary;
    }

    private String buildSummaryJson(PayrollSummary summary) {
        StringBuilder builder = new StringBuilder("{");
        appendJsonNumber(builder, "totalCount", summary.totalCount);
        appendJsonNumber(builder, "draftCount", summary.draftCount);
        appendJsonNumber(builder, "pendingCount", summary.pendingCount);
        appendJsonNumber(builder, "confirmedCount", summary.confirmedCount);
        appendJsonNumber(builder, "issueCount", summary.issueCount);
        appendJsonNumber(builder, "resolvedCount", summary.resolvedCount);
        appendJsonNumber(builder, "baseSalaryTotal", summary.baseSalaryTotal);
        appendJsonNumber(builder, "attendanceDeductionTotal", summary.attendanceDeductionTotal);
        appendJsonNumber(builder, "netSalaryTotal", summary.netSalaryTotal);
        builder.append('}');
        return builder.toString();
    }

    private String buildLineItemJson(BigDecimal baseSalary, BigDecimal allowance, BigDecimal deduction,
                                     BigDecimal socialInsurance, BigDecimal housingFund,
                                     BigDecimal attendanceDeduction, BigDecimal tax, PayrollSchemeDO defaultScheme,
                                     AttendancePayrollSnapshot attendance,
                                     SocialSecurityAccountDO socialSecurity) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("baseSalary", nvl(baseSalary));
        payload.put("allowance", nvl(allowance));
        payload.put("deduction", nvl(deduction));
        payload.put("attendanceDeduction", nvl(attendanceDeduction));
        payload.put("socialInsurance", nvl(socialInsurance));
        payload.put("housingFund", nvl(housingFund));
        payload.put("tax", nvl(tax));
        if (defaultScheme != null) {
            Map<String, Object> schemePayload = new LinkedHashMap<>();
            schemePayload.put("id", defaultScheme.getId());
            schemePayload.put("schemeCode", defaultScheme.getSchemeCode());
            schemePayload.put("schemeName", defaultScheme.getSchemeName());
            schemePayload.put("taxRuleJson", defaultScheme.getTaxRuleJson());
            schemePayload.put("attendanceRuleJson", defaultScheme.getAttendanceRuleJson());
            payload.put("payrollScheme", schemePayload);
        }
        payload.put("attendance", attendance == null ? null : attendance.toMap());
        if (socialSecurity != null) {
            Map<String, Object> socialPayload = new LinkedHashMap<>();
            socialPayload.put("id", socialSecurity.getId());
            socialPayload.put("socialMonth", socialSecurity.getSocialMonth());
            socialPayload.put("city", socialSecurity.getCity());
            socialPayload.put("status", socialSecurity.getStatus());
            socialPayload.put("payable", isSocialPayableStatus(socialSecurity));
            payload.put("socialSecurity", socialPayload);
        }
        return JsonUtils.toJsonString(payload);
    }

    private void appendJsonNumber(StringBuilder builder, String key, Number value) {
        if (builder.length() > 1) {
            builder.append(',');
        }
        builder.append('"').append(key).append("\":").append(value == null ? 0 : value);
    }

    private BigDecimal calculateNet(PayslipUpdateReqVO reqVO) {
        return nvl(reqVO.getBaseSalary())
                .add(nvl(reqVO.getOvertimePay()))
                .add(nvl(reqVO.getBonus()))
                .add(nvl(reqVO.getAllowance()))
                .subtract(nvl(reqVO.getAttendanceDeduction()))
                .subtract(nvl(reqVO.getDeduction()))
                .subtract(nvl(reqVO.getSocialInsurance()))
                .subtract(nvl(reqVO.getHousingFund()))
                .subtract(nvl(reqVO.getTax()));
    }

    private BigDecimal calculateGrossIncome(PayslipDO slip) {
        return nvl(slip.getBaseSalary())
                .add(nvl(slip.getOvertimePay()))
                .add(nvl(slip.getBonus()))
                .add(nvl(slip.getAllowance()));
    }

    private BigDecimal calculatePayableSalary(PayslipDO slip) {
        return calculateGrossIncome(slip)
                .subtract(nvl(slip.getAttendanceDeduction()))
                .subtract(nvl(slip.getDeduction()));
    }

    private BigDecimal calculateWithholdingBase(BigDecimal payableSalary, BigDecimal socialInsurance, BigDecimal housingFund) {
        return nvl(payableSalary)
                .subtract(nvl(socialInsurance))
                .subtract(nvl(housingFund))
                .max(BigDecimal.ZERO);
    }

    private BigDecimal calculateMonthlyTax(PayrollSchemeDO defaultScheme, BigDecimal payableSalary,
                                           BigDecimal socialInsurance, BigDecimal housingFund) {
        Map<String, Object> rule = parseTaxRule(defaultScheme);
        BigDecimal threshold = ruleAmount(rule, "threshold");
        if (threshold.compareTo(BigDecimal.ZERO) <= 0) {
            threshold = BigDecimal.valueOf(5000);
        }
        BigDecimal taxable = calculateWithholdingBase(payableSalary, socialInsurance, housingFund)
                .subtract(threshold)
                .max(BigDecimal.ZERO);
        if (taxable.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal configuredRate = ruleAmount(rule, "rate");
        if (configuredRate.compareTo(BigDecimal.ZERO) > 0) {
            return taxable.multiply(configuredRate)
                    .subtract(ruleAmount(rule, "quickDeduction"))
                    .max(BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return calculateChinaMonthlyTax(taxable);
    }

    private Map<String, Object> parseTaxRule(PayrollSchemeDO defaultScheme) {
        if (defaultScheme == null || !StringUtils.hasText(defaultScheme.getTaxRuleJson())) {
            return new HashMap<>();
        }
        Map<String, Object> rule = JsonUtils.parseObjectQuietly(defaultScheme.getTaxRuleJson(),
                new TypeReference<Map<String, Object>>() {});
        return rule == null ? new HashMap<>() : rule;
    }

    private BigDecimal calculateChinaMonthlyTax(BigDecimal taxable) {
        BigDecimal rate;
        BigDecimal quickDeduction;
        if (taxable.compareTo(BigDecimal.valueOf(3000)) <= 0) {
            rate = BigDecimal.valueOf(0.03);
            quickDeduction = BigDecimal.ZERO;
        } else if (taxable.compareTo(BigDecimal.valueOf(12000)) <= 0) {
            rate = BigDecimal.valueOf(0.10);
            quickDeduction = BigDecimal.valueOf(210);
        } else if (taxable.compareTo(BigDecimal.valueOf(25000)) <= 0) {
            rate = BigDecimal.valueOf(0.20);
            quickDeduction = BigDecimal.valueOf(1410);
        } else if (taxable.compareTo(BigDecimal.valueOf(35000)) <= 0) {
            rate = BigDecimal.valueOf(0.25);
            quickDeduction = BigDecimal.valueOf(2660);
        } else if (taxable.compareTo(BigDecimal.valueOf(55000)) <= 0) {
            rate = BigDecimal.valueOf(0.30);
            quickDeduction = BigDecimal.valueOf(4410);
        } else if (taxable.compareTo(BigDecimal.valueOf(80000)) <= 0) {
            rate = BigDecimal.valueOf(0.35);
            quickDeduction = BigDecimal.valueOf(7160);
        } else {
            rate = BigDecimal.valueOf(0.45);
            quickDeduction = BigDecimal.valueOf(15160);
        }
        return taxable.multiply(rate)
                .subtract(quickDeduction)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void addStatusCount(PayrollIncomeSummaryRespVO summary, String status) {
        if (SLIP_CONFIRMED.equals(status)) {
            summary.setConfirmedCount(summary.getConfirmedCount() + 1);
        } else if (SLIP_ISSUE.equals(status)) {
            summary.setIssueCount(summary.getIssueCount() + 1);
        } else if (SLIP_RESOLVED.equals(status)) {
            summary.setResolvedCount(summary.getResolvedCount() + 1);
        } else {
            summary.setPendingCount(summary.getPendingCount() + 1);
        }
    }

    private void addStatusCount(PayrollIncomeSummaryRespVO.MonthSummary summary, String status) {
        if (SLIP_CONFIRMED.equals(status)) {
            summary.setConfirmedCount(summary.getConfirmedCount() + 1);
        } else if (SLIP_ISSUE.equals(status)) {
            summary.setIssueCount(summary.getIssueCount() + 1);
        } else if (SLIP_RESOLVED.equals(status)) {
            summary.setResolvedCount(summary.getResolvedCount() + 1);
        } else {
            summary.setPendingCount(summary.getPendingCount() + 1);
        }
    }

    private PayrollReportAmounts buildReportAmounts(PayslipDO slip, SocialSecurityAccountDO socialSecurity) {
        PayrollReportAmounts amounts = new PayrollReportAmounts();
        amounts.baseSalary = nvl(slip.getBaseSalary());
        amounts.payableSalary = calculatePayableSalary(slip);
        amounts.attendanceDeduction = nvl(slip.getAttendanceDeduction());
        amounts.overtimePay = nvl(slip.getOvertimePay());
        amounts.bonus = nvl(slip.getBonus());
        amounts.allowance = nvl(slip.getAllowance());
        amounts.deduction = nvl(slip.getDeduction());
        amounts.socialInsurance = nvl(slip.getSocialInsurance());
        amounts.housingFund = nvl(slip.getHousingFund());
        amounts.companySocialSecurity = socialSecurityCompanyTotal(socialSecurity);
        amounts.companyHousingFund = isSocialPayableStatus(socialSecurity) ? nvl(socialSecurity.getFundCompany()) : BigDecimal.ZERO;
        amounts.tax = nvl(slip.getTax());
        amounts.netSalary = nvl(slip.getNetSalary());
        amounts.laborCost = amounts.payableSalary
                .add(amounts.companySocialSecurity)
                .add(amounts.companyHousingFund);
        return amounts;
    }

    private void addReportTotals(PayrollReportRespVO summary, PayrollReportAmounts amounts) {
        summary.setBaseSalaryTotal(summary.getBaseSalaryTotal().add(amounts.baseSalary));
        summary.setPayableSalaryTotal(summary.getPayableSalaryTotal().add(amounts.payableSalary));
        summary.setAttendanceDeductionTotal(summary.getAttendanceDeductionTotal().add(amounts.attendanceDeduction));
        summary.setOvertimePayTotal(summary.getOvertimePayTotal().add(amounts.overtimePay));
        summary.setBonusTotal(summary.getBonusTotal().add(amounts.bonus));
        summary.setAllowanceTotal(summary.getAllowanceTotal().add(amounts.allowance));
        summary.setDeductionTotal(summary.getDeductionTotal().add(amounts.deduction));
        summary.setSocialInsuranceTotal(summary.getSocialInsuranceTotal().add(amounts.socialInsurance));
        summary.setHousingFundTotal(summary.getHousingFundTotal().add(amounts.housingFund));
        summary.setCompanySocialSecurityTotal(summary.getCompanySocialSecurityTotal().add(amounts.companySocialSecurity));
        summary.setCompanyHousingFundTotal(summary.getCompanyHousingFundTotal().add(amounts.companyHousingFund));
        summary.setTaxTotal(summary.getTaxTotal().add(amounts.tax));
        summary.setNetSalaryTotal(summary.getNetSalaryTotal().add(amounts.netSalary));
        summary.setLaborCostTotal(summary.getLaborCostTotal().add(amounts.laborCost));
    }

    private void addReportTotals(PayrollReportRespVO.MonthSummary summary, PayrollReportAmounts amounts) {
        summary.setBaseSalaryTotal(summary.getBaseSalaryTotal().add(amounts.baseSalary));
        summary.setPayableSalaryTotal(summary.getPayableSalaryTotal().add(amounts.payableSalary));
        summary.setAttendanceDeductionTotal(summary.getAttendanceDeductionTotal().add(amounts.attendanceDeduction));
        summary.setOvertimePayTotal(summary.getOvertimePayTotal().add(amounts.overtimePay));
        summary.setBonusTotal(summary.getBonusTotal().add(amounts.bonus));
        summary.setAllowanceTotal(summary.getAllowanceTotal().add(amounts.allowance));
        summary.setDeductionTotal(summary.getDeductionTotal().add(amounts.deduction));
        summary.setSocialInsuranceTotal(summary.getSocialInsuranceTotal().add(amounts.socialInsurance));
        summary.setHousingFundTotal(summary.getHousingFundTotal().add(amounts.housingFund));
        summary.setCompanySocialSecurityTotal(summary.getCompanySocialSecurityTotal().add(amounts.companySocialSecurity));
        summary.setCompanyHousingFundTotal(summary.getCompanyHousingFundTotal().add(amounts.companyHousingFund));
        summary.setTaxTotal(summary.getTaxTotal().add(amounts.tax));
        summary.setNetSalaryTotal(summary.getNetSalaryTotal().add(amounts.netSalary));
        summary.setLaborCostTotal(summary.getLaborCostTotal().add(amounts.laborCost));
    }

    private void addReportTotals(PayrollReportRespVO.DepartmentSummary summary, PayrollReportAmounts amounts) {
        summary.setBaseSalaryTotal(summary.getBaseSalaryTotal().add(amounts.baseSalary));
        summary.setPayableSalaryTotal(summary.getPayableSalaryTotal().add(amounts.payableSalary));
        summary.setAttendanceDeductionTotal(summary.getAttendanceDeductionTotal().add(amounts.attendanceDeduction));
        summary.setSocialInsuranceTotal(summary.getSocialInsuranceTotal().add(amounts.socialInsurance));
        summary.setHousingFundTotal(summary.getHousingFundTotal().add(amounts.housingFund));
        summary.setCompanySocialSecurityTotal(summary.getCompanySocialSecurityTotal().add(amounts.companySocialSecurity));
        summary.setCompanyHousingFundTotal(summary.getCompanyHousingFundTotal().add(amounts.companyHousingFund));
        summary.setTaxTotal(summary.getTaxTotal().add(amounts.tax));
        summary.setNetSalaryTotal(summary.getNetSalaryTotal().add(amounts.netSalary));
        summary.setLaborCostTotal(summary.getLaborCostTotal().add(amounts.laborCost));
    }

    private void fillPerCapita(PayrollReportRespVO summary) {
        int count = summary.getProfileCount() == null ? 0 : summary.getProfileCount();
        summary.setPayableSalaryPerCapita(reportAverage(summary.getPayableSalaryTotal(), count));
        summary.setNetSalaryPerCapita(reportAverage(summary.getNetSalaryTotal(), count));
        summary.setLaborCostPerCapita(reportAverage(summary.getLaborCostTotal(), count));
    }

    private void fillPerCapita(PayrollReportRespVO.MonthSummary summary) {
        int count = summary.getProfileCount() == null ? 0 : summary.getProfileCount();
        summary.setPayableSalaryPerCapita(reportAverage(summary.getPayableSalaryTotal(), count));
        summary.setNetSalaryPerCapita(reportAverage(summary.getNetSalaryTotal(), count));
        summary.setLaborCostPerCapita(reportAverage(summary.getLaborCostTotal(), count));
    }

    private void fillPerCapita(PayrollReportRespVO.DepartmentSummary summary) {
        int count = summary.getProfileCount() == null ? 0 : summary.getProfileCount();
        summary.setPayableSalaryPerCapita(reportAverage(summary.getPayableSalaryTotal(), count));
        summary.setNetSalaryPerCapita(reportAverage(summary.getNetSalaryTotal(), count));
        summary.setLaborCostPerCapita(reportAverage(summary.getLaborCostTotal(), count));
    }

    private List<PayrollReportRespVO.SalaryStructure> buildSalaryStructures(PayrollReportRespVO report) {
        BigDecimal incomeBasis = nvl(report.getBaseSalaryTotal())
                .add(nvl(report.getOvertimePayTotal()))
                .add(nvl(report.getBonusTotal()))
                .add(nvl(report.getAllowanceTotal()));
        BigDecimal deductionBasis = nvl(report.getPayableSalaryTotal());
        BigDecimal costBasis = nvl(report.getLaborCostTotal());
        List<PayrollReportRespVO.SalaryStructure> structures = new ArrayList<>();
        addSalaryStructure(structures, "BASE_SALARY", "基本工资", "收入项", "收入合计",
                report.getBaseSalaryTotal(), incomeBasis);
        addSalaryStructure(structures, "OVERTIME_PAY", "加班工资", "收入项", "收入合计",
                report.getOvertimePayTotal(), incomeBasis);
        addSalaryStructure(structures, "BONUS", "奖金", "收入项", "收入合计",
                report.getBonusTotal(), incomeBasis);
        addSalaryStructure(structures, "ALLOWANCE", "津贴补贴", "收入项", "收入合计",
                report.getAllowanceTotal(), incomeBasis);
        addSalaryStructure(structures, "ATTENDANCE_DEDUCTION", "考勤扣减", "扣减项", "应发工资",
                report.getAttendanceDeductionTotal(), deductionBasis);
        addSalaryStructure(structures, "DEDUCTION", "其他扣减", "扣减项", "应发工资",
                report.getDeductionTotal(), deductionBasis);
        addSalaryStructure(structures, "SOCIAL_INSURANCE", "个人社保", "扣减项", "应发工资",
                report.getSocialInsuranceTotal(), deductionBasis);
        addSalaryStructure(structures, "HOUSING_FUND", "个人公积金", "扣减项", "应发工资",
                report.getHousingFundTotal(), deductionBasis);
        addSalaryStructure(structures, "TAX", "个税", "扣减项", "应发工资",
                report.getTaxTotal(), deductionBasis);
        addSalaryStructure(structures, "PAYABLE_SALARY", "应发工资", "人工成本", "人工成本",
                report.getPayableSalaryTotal(), costBasis);
        addSalaryStructure(structures, "COMPANY_SOCIAL_SECURITY", "公司社保", "人工成本", "人工成本",
                report.getCompanySocialSecurityTotal(), costBasis);
        addSalaryStructure(structures, "COMPANY_HOUSING_FUND", "公司公积金", "人工成本", "人工成本",
                report.getCompanyHousingFundTotal(), costBasis);
        return structures;
    }

    private void addSalaryStructure(List<PayrollReportRespVO.SalaryStructure> structures, String code,
                                    String name, String category, String basisName,
                                    BigDecimal amount, BigDecimal basis) {
        PayrollReportRespVO.SalaryStructure item = new PayrollReportRespVO.SalaryStructure();
        item.setCode(code);
        item.setName(name);
        item.setCategory(category);
        item.setBasisName(basisName);
        item.setAmountTotal(nvl(amount));
        item.setRatio(reportRatio(amount, basis));
        structures.add(item);
    }

    private BigDecimal reportRatio(BigDecimal amount, BigDecimal basis) {
        BigDecimal denominator = nvl(basis);
        if (denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return nvl(amount).divide(denominator, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal reportAverage(BigDecimal amount, int count) {
        if (count <= 0) {
            return BigDecimal.ZERO;
        }
        return nvl(amount).divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private void addReportStatusCount(PayrollReportRespVO summary, String status) {
        if (SLIP_CONFIRMED.equals(status)) {
            summary.setConfirmedCount(summary.getConfirmedCount() + 1);
        } else if (SLIP_ISSUE.equals(status)) {
            summary.setIssueCount(summary.getIssueCount() + 1);
        } else if (SLIP_RESOLVED.equals(status)) {
            summary.setResolvedCount(summary.getResolvedCount() + 1);
        } else {
            summary.setPendingCount(summary.getPendingCount() + 1);
        }
    }

    private void addReportStatusCount(PayrollReportRespVO.MonthSummary summary, String status) {
        if (SLIP_CONFIRMED.equals(status)) {
            summary.setConfirmedCount(summary.getConfirmedCount() + 1);
        } else if (SLIP_ISSUE.equals(status)) {
            summary.setIssueCount(summary.getIssueCount() + 1);
        } else if (SLIP_RESOLVED.equals(status)) {
            summary.setResolvedCount(summary.getResolvedCount() + 1);
        } else {
            summary.setPendingCount(summary.getPendingCount() + 1);
        }
    }

    private PayrollBatchDO getBatch(Long id) {
        PayrollBatchDO batch = payrollBatchMapper.selectById(id);
        if (batch == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND, "工资批次不存在");
        }
        return batch;
    }

    private PayslipDO getSlip(Long id) {
        PayslipDO slip = payslipMapper.selectById(id);
        if (slip == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND, "工资条不存在");
        }
        return slip;
    }

    private PayrollSchemeDO getScheme(Long id) {
        PayrollSchemeDO scheme = payrollSchemeMapper.selectById(id);
        if (scheme == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND, "薪资方案不存在");
        }
        return scheme;
    }

    private PayslipDO getMySlip(Long id) {
        PayslipDO slip = getSlip(id);
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null || !Objects.equals(loginUserId, slip.getUserId())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.FORBIDDEN, "无权处理该工资条");
        }
        return slip;
    }

    private void recordPayslipLog(PayslipDO slip, String operationType, String title, String content) {
        if (slip == null) {
            return;
        }
        recordPayrollOperationLog(slip.getProfileId(), operationType, title, content);
    }

    private void recordPayrollBatchLog(PayrollBatchDO batch, String operationType, String title, String content) {
        if (batch == null || batch.getId() == null) {
            return;
        }
        for (PayslipDO slip : emptyIfNull(payslipMapper.selectListByBatchId(batch.getId()))) {
            recordPayslipLog(slip, operationType, title, content);
        }
    }

    private void recordPayrollOperationLog(Long profileId, String operationType, String title, String content) {
        if (profileId == null) {
            return;
        }
        try {
            EmployeeOperationLogDO operationLog = new EmployeeOperationLogDO();
            operationLog.setProfileId(profileId);
            operationLog.setOperationType(operationType);
            operationLog.setOperationModule("payroll");
            operationLog.setOperationTitle(title);
            operationLog.setOperationContent(content);
            operationLog.setOperatorId(SecurityFrameworkUtils.getLoginUserId());
            operationLog.setOperatorName(defaultOperatorName());
            operationLog.setOperationTime(LocalDateTime.now());
            operationLog.setOperationSource("web");
            employeeOperationLogMapper.insert(operationLog);
        } catch (Exception ex) {
            log.warn("Record payroll operation log failed, profileId={}, operationType={}",
                    profileId, operationType, ex);
        }
    }

    private String defaultOperatorName() {
        String nickname = SecurityFrameworkUtils.getLoginUserNickname();
        return StringUtils.hasText(nickname) ? nickname : "system";
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private void ensureBatchEditable(PayrollBatchDO batch) {
        if (BATCH_LOCKED.equals(batch.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "工资批次已锁定，不能继续处理");
        }
        if (BATCH_PENDING_APPROVAL.equals(batch.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "工资批次正在发薪审批中，不能继续编辑");
        }
    }

    private void normalizeBatchPageReq(PayrollBatchPageReqVO reqVO) {
        if (StringUtils.hasText(reqVO.getStatus())) {
            reqVO.setStatus(reqVO.getStatus().trim().toUpperCase());
        }
        if (StringUtils.hasText(reqVO.getPayrollMonth())) {
            reqVO.setPayrollMonth(reqVO.getPayrollMonth().trim());
        }
    }

    private void normalizePayslipPageReq(PayslipPageReqVO reqVO) {
        if (StringUtils.hasText(reqVO.getStatus())) {
            reqVO.setStatus(reqVO.getStatus().trim().toUpperCase());
        }
        if (StringUtils.hasText(reqVO.getPayrollMonth())) {
            reqVO.setPayrollMonth(reqVO.getPayrollMonth().trim());
        }
    }

    private String payslipStatusLabel(String status) {
        if (!StringUtils.hasText(status)) {
            return "-";
        }
        switch (status.trim().toUpperCase()) {
            case SLIP_DRAFT:
                return "草稿";
            case SLIP_PUBLISHED:
                return "待确认";
            case SLIP_CONFIRMED:
                return "已确认";
            case SLIP_ISSUE:
                return "有异议";
            case SLIP_RESOLVED:
                return "已处理";
            default:
                return status;
        }
    }

    private void normalizeSocialPageReq(SocialSecurityAccountPageReqVO reqVO) {
        if (StringUtils.hasText(reqVO.getSocialMonth())) {
            reqVO.setSocialMonth(reqVO.getSocialMonth().trim());
        }
        if (StringUtils.hasText(reqVO.getStatus())) {
            reqVO.setStatus(reqVO.getStatus().trim().toUpperCase());
        }
        if (StringUtils.hasText(reqVO.getCity())) {
            reqVO.setCity(reqVO.getCity().trim());
        }
    }

    private void normalizeSchemePageReq(PayrollSchemePageReqVO reqVO) {
        if (StringUtils.hasText(reqVO.getSchemeCode())) {
            reqVO.setSchemeCode(reqVO.getSchemeCode().trim());
        }
        if (StringUtils.hasText(reqVO.getSchemeName())) {
            reqVO.setSchemeName(reqVO.getSchemeName().trim());
        }
        if (StringUtils.hasText(reqVO.getStatus())) {
            reqVO.setStatus(reqVO.getStatus().trim().toUpperCase());
        }
    }

    private void normalizeScheme(PayrollSchemeDO scheme) {
        scheme.setSchemeName(scheme.getSchemeName().trim());
        if (!StringUtils.hasText(scheme.getSchemeCode())) {
            scheme.setSchemeCode("SCHEME-" + System.currentTimeMillis());
        } else {
            scheme.setSchemeCode(scheme.getSchemeCode().trim());
        }
        scheme.setStatus(StringUtils.hasText(scheme.getStatus())
                ? scheme.getStatus().trim().toUpperCase() : SCHEME_ACTIVE);
        if (!SCHEME_ACTIVE.equals(scheme.getStatus()) && !SCHEME_DISABLED.equals(scheme.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "薪资方案状态不合法");
        }
        scheme.setDefaultFlag(Boolean.TRUE.equals(scheme.getDefaultFlag()));
        scheme.setSalaryType(StringUtils.hasText(scheme.getSalaryType()) ? scheme.getSalaryType().trim() : "MONTHLY");
        scheme.setCurrency(StringUtils.hasText(scheme.getCurrency()) ? scheme.getCurrency().trim() : "CNY");
        scheme.setBaseSalary(nvl(scheme.getBaseSalary()));
        scheme.setAllowance(nvl(scheme.getAllowance()));
        scheme.setDeduction(nvl(scheme.getDeduction()));
        validateSchemeJson(scheme.getTaxRuleJson(), "个税规则");
        validateSchemeJson(scheme.getAttendanceRuleJson(), "考勤扣减规则");
        if (scheme.getEffectiveDate() == null) {
            scheme.setEffectiveDate(LocalDate.now());
        }
        if (scheme.getExpireDate() != null && scheme.getExpireDate().isBefore(scheme.getEffectiveDate())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "失效日期不能早于生效日期");
        }
    }

    private void validateSchemeJson(String value, String label) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        Map<String, Object> parsed = JsonUtils.parseObjectQuietly(value, new TypeReference<Map<String, Object>>() {});
        if (parsed == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, label + "必须是 JSON 对象");
        }
    }

    private void clearOtherDefaultSchemes(Long currentId) {
        LambdaUpdateWrapper<PayrollSchemeDO> update = new LambdaUpdateWrapper<PayrollSchemeDO>()
                .eq(PayrollSchemeDO::getDefaultFlag, true)
                .set(PayrollSchemeDO::getDefaultFlag, false);
        if (currentId != null) {
            update.ne(PayrollSchemeDO::getId, currentId);
        }
        payrollSchemeMapper.update(null, update);
    }

    private void normalizeSocialAccount(SocialSecurityAccountDO account) {
        account.setSocialMonth(account.getSocialMonth().trim());
        if (!account.getSocialMonth().matches("\\d{4}-\\d{2}")) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "社保月份格式应为 yyyy-MM");
        }
        account.setCity(StringUtils.hasText(account.getCity()) ? account.getCity().trim() : null);
        account.setStatus(StringUtils.hasText(account.getStatus()) ? account.getStatus().trim().toUpperCase() : SOCIAL_ENROLLED);
        if (!SOCIAL_PENDING_ADD.equals(account.getStatus())
                && !SOCIAL_ENROLLED.equals(account.getStatus())
                && !SOCIAL_PENDING_STOP.equals(account.getStatus())
                && !SOCIAL_SUSPENDED.equals(account.getStatus())
                && !SOCIAL_STOPPED.equals(account.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "社保增减员状态不合法");
        }
        account.setInsuranceBase(nvl(account.getInsuranceBase()));
        account.setFundBase(nvl(account.getFundBase()));
        account.setPensionPersonal(nvl(account.getPensionPersonal()));
        account.setMedicalPersonal(nvl(account.getMedicalPersonal()));
        account.setUnemploymentPersonal(nvl(account.getUnemploymentPersonal()));
        account.setPensionCompany(nvl(account.getPensionCompany()));
        account.setMedicalCompany(nvl(account.getMedicalCompany()));
        account.setUnemploymentCompany(nvl(account.getUnemploymentCompany()));
        account.setWorkInjuryCompany(nvl(account.getWorkInjuryCompany()));
        account.setMaternityCompany(nvl(account.getMaternityCompany()));
        account.setFundPersonal(nvl(account.getFundPersonal()));
        account.setFundCompany(nvl(account.getFundCompany()));
    }

    private void ensureSocialSecurityEditable(String socialMonth) {
        if (!StringUtils.hasText(socialMonth)) {
            return;
        }
        PayrollBatchDO batch = payrollBatchMapper.selectByMonth(socialMonth);
        if (batch == null) {
            return;
        }
        if (BATCH_PENDING_APPROVAL.equals(batch.getStatus())
                || BATCH_PUBLISHED.equals(batch.getStatus())
                || BATCH_LOCKED.equals(batch.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST,
                    "该月份工资批次已进入审批、发布或锁定，不能修改社保公积金台账");
        }
    }

    private boolean prepareSocialProfileFilter(SocialSecurityAccountPageReqVO reqVO) {
        if (!StringUtils.hasText(reqVO.getProfileName()) && !StringUtils.hasText(reqVO.getProfileMobile())) {
            return true;
        }
        List<EmployeeProfileDO> profiles = employeeProfileMapper.selectList(new LambdaQueryWrapperX<EmployeeProfileDO>()
                .likeIfPresent(EmployeeProfileDO::getName, reqVO.getProfileName())
                .likeIfPresent(EmployeeProfileDO::getMobile, reqVO.getProfileMobile())
                .last("LIMIT 1000"));
        Set<Long> profileIds = new HashSet<>();
        for (EmployeeProfileDO profile : emptyIfNull(profiles)) {
            if (profile.getId() != null) {
                profileIds.add(profile.getId());
            }
        }
        if (profileIds.isEmpty()) {
            return false;
        }
        reqVO.setProfileIds(profileIds);
        return true;
    }

    private EmployeeProfileDO validateProfileExists(Long profileId) {
        EmployeeProfileDO profile = employeeProfileMapper.selectById(profileId);
        if (profile == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND, "员工档案不存在");
        }
        return profile;
    }

    private void fillPeopleInfo(List<PayslipDO> rows, List<PayslipRespVO> respList) {
        if (rows == null || rows.isEmpty() || respList == null || respList.isEmpty()) {
            return;
        }
        Set<Long> userIds = new HashSet<>();
        Set<Long> profileIds = new HashSet<>();
        for (PayslipDO row : rows) {
            if (row.getUserId() != null) {
                userIds.add(row.getUserId());
            }
            if (row.getResolvedBy() != null) {
                userIds.add(row.getResolvedBy());
            }
            if (row.getProfileId() != null) {
                profileIds.add(row.getProfileId());
            }
        }
        Map<Long, AdminUserRespDTO> userMap = loadUserMapSafe(userIds);
        Map<Long, EmployeeProfileDO> profileMap = loadProfileMapSafe(profileIds);
        for (PayslipRespVO item : respList) {
            AdminUserRespDTO user = userMap.get(item.getUserId());
            if (user != null) {
                item.setUserNickname(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
            }
            AdminUserRespDTO resolvedBy = userMap.get(item.getResolvedBy());
            if (resolvedBy != null) {
                item.setResolvedByName(StringUtils.hasText(resolvedBy.getNickname())
                        ? resolvedBy.getNickname() : resolvedBy.getUsername());
            }
            EmployeeProfileDO profile = profileMap.get(item.getProfileId());
            if (profile != null) {
                item.setProfileName(profile.getName());
            }
        }
    }

    private void fillSocialSecurityInfo(List<SocialSecurityAccountDO> rows, List<SocialSecurityAccountRespVO> respList) {
        if (rows == null || rows.isEmpty() || respList == null || respList.isEmpty()) {
            return;
        }
        Set<Long> profileIds = new HashSet<>();
        Map<Long, SocialSecurityAccountDO> rowMap = new HashMap<>();
        for (SocialSecurityAccountDO row : rows) {
            if (row.getProfileId() != null) {
                profileIds.add(row.getProfileId());
            }
            if (row.getId() != null) {
                rowMap.put(row.getId(), row);
            }
        }
        Map<Long, EmployeeProfileDO> profileMap = loadProfileMapSafe(profileIds);
        for (SocialSecurityAccountRespVO item : respList) {
            EmployeeProfileDO profile = profileMap.get(item.getProfileId());
            if (profile != null) {
                item.setProfileName(profile.getName());
                item.setProfileMobile(profile.getMobile());
            }
            SocialSecurityAccountDO account = rowMap.get(item.getId());
            item.setPersonalTotal(socialSecurityPersonalTotal(account)
                    .add(isSocialPayableStatus(account) ? nvl(account.getFundPersonal()) : BigDecimal.ZERO));
            item.setCompanyTotal(socialSecurityCompanyTotal(account)
                    .add(isSocialPayableStatus(account) ? nvl(account.getFundCompany()) : BigDecimal.ZERO));
        }
    }

    private BigDecimal socialSecurityPersonalTotal(SocialSecurityAccountDO account) {
        if (!isSocialPayableStatus(account)) {
            return BigDecimal.ZERO;
        }
        return nvl(account.getPensionPersonal())
                .add(nvl(account.getMedicalPersonal()))
                .add(nvl(account.getUnemploymentPersonal()));
    }

    private BigDecimal socialSecurityCompanyTotal(SocialSecurityAccountDO account) {
        if (!isSocialPayableStatus(account)) {
            return BigDecimal.ZERO;
        }
        return nvl(account.getPensionCompany())
                .add(nvl(account.getMedicalCompany()))
                .add(nvl(account.getUnemploymentCompany()))
                .add(nvl(account.getWorkInjuryCompany()))
                .add(nvl(account.getMaternityCompany()));
    }

    private Map<Long, AdminUserRespDTO> loadUserMapSafe(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return adminUserApi.getUserMap(userIds);
        } catch (Exception ex) {
            log.warn("Failed to load admin users for payroll: {}", ex.getMessage());
            return new HashMap<>();
        }
    }

    private Map<Long, EmployeeProfileDO> loadProfileMapSafe(Set<Long> profileIds) {
        Map<Long, EmployeeProfileDO> profileMap = new HashMap<>();
        if (profileIds == null || profileIds.isEmpty()) {
            return profileMap;
        }
        List<EmployeeProfileDO> profiles = employeeProfileMapper.selectList(
                new LambdaQueryWrapperX<EmployeeProfileDO>().in(EmployeeProfileDO::getId, profileIds));
        for (EmployeeProfileDO profile : emptyIfNull(profiles)) {
            if (profile.getId() != null) {
                profileMap.put(profile.getId(), profile);
            }
        }
        return profileMap;
    }

    private void refreshTodoTasksQuietly() {
        try {
            hrTodoTaskService.refreshGeneratedTasks();
        } catch (Exception ex) {
            log.warn("Refresh HR todo tasks after payroll failed: {}", ex.getMessage());
        }
    }

    private LocalDate resolveMonth(Integer year, Integer month) {
        LocalDate now = LocalDate.now();
        int resolvedYear = year == null ? now.getYear() : year;
        int resolvedMonth = month == null ? now.getMonthValue() : month;
        if (resolvedMonth < 1 || resolvedMonth > 12) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "月份不合法");
        }
        return LocalDate.of(resolvedYear, resolvedMonth, 1);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private <T> List<T> emptyIfNull(List<T> rows) {
        return rows == null ? new ArrayList<>() : rows;
    }

    private static class PayrollReportAmounts {
        private BigDecimal baseSalary = BigDecimal.ZERO;
        private BigDecimal payableSalary = BigDecimal.ZERO;
        private BigDecimal attendanceDeduction = BigDecimal.ZERO;
        private BigDecimal overtimePay = BigDecimal.ZERO;
        private BigDecimal bonus = BigDecimal.ZERO;
        private BigDecimal allowance = BigDecimal.ZERO;
        private BigDecimal deduction = BigDecimal.ZERO;
        private BigDecimal socialInsurance = BigDecimal.ZERO;
        private BigDecimal housingFund = BigDecimal.ZERO;
        private BigDecimal companySocialSecurity = BigDecimal.ZERO;
        private BigDecimal companyHousingFund = BigDecimal.ZERO;
        private BigDecimal tax = BigDecimal.ZERO;
        private BigDecimal netSalary = BigDecimal.ZERO;
        private BigDecimal laborCost = BigDecimal.ZERO;
    }

    private static class PayrollSummary {
        private long totalCount;
        private long draftCount;
        private long pendingCount;
        private long confirmedCount;
        private long issueCount;
        private long resolvedCount;
        private BigDecimal baseSalaryTotal = BigDecimal.ZERO;
        private BigDecimal attendanceDeductionTotal = BigDecimal.ZERO;
        private BigDecimal netSalaryTotal = BigDecimal.ZERO;
    }

    private static class AttendancePayrollSnapshot {
        private String payrollMonth;
        private Long userId;
        private Long settlementId;
        private String settlementStatus;
        private Long confirmId;
        private String confirmStatus;
        private int recordCount;
        private int lateCount;
        private int earlyLeaveCount;
        private int missingCount;
        private int absenteeismCount;
        private int abnormalCount;
        private int lateMinutes;
        private int earlyLeaveMinutes;
        private BigDecimal absentHours = BigDecimal.ZERO;
        private BigDecimal leaveHours = BigDecimal.ZERO;
        private BigDecimal tripHours = BigDecimal.ZERO;

        static AttendancePayrollSnapshot missing(String payrollMonth, Long userId) {
            AttendancePayrollSnapshot snapshot = new AttendancePayrollSnapshot();
            snapshot.payrollMonth = payrollMonth;
            snapshot.userId = userId;
            return snapshot;
        }

        void setSettlementStatus(String settlementStatus) {
            this.settlementStatus = settlementStatus;
        }

        void setConfirmId(Long confirmId) {
            this.confirmId = confirmId;
        }

        void setConfirmStatus(String confirmStatus) {
            this.confirmStatus = confirmStatus;
        }

        void record(AttendanceDailyResultDO row) {
            recordCount++;
            lateMinutes += row.getLateMinutes() == null ? 0 : row.getLateMinutes();
            earlyLeaveMinutes += row.getEarlyLeaveMinutes() == null ? 0 : row.getEarlyLeaveMinutes();
            absentHours = absentHours.add(row.getAbsentHours() == null ? BigDecimal.ZERO : row.getAbsentHours());
            leaveHours = leaveHours.add(row.getLeaveHours() == null ? BigDecimal.ZERO : row.getLeaveHours());
            tripHours = tripHours.add(row.getTripHours() == null ? BigDecimal.ZERO : row.getTripHours());
            String status = row.getResultStatus();
            if ("LATE".equals(status)) {
                lateCount++;
                abnormalCount++;
            } else if ("EARLY".equals(status)) {
                earlyLeaveCount++;
                abnormalCount++;
            } else if ("LATE_EARLY".equals(status)) {
                lateCount++;
                earlyLeaveCount++;
                abnormalCount++;
            } else if ("MISSING_IN".equals(status) || "MISSING_OUT".equals(status) || "MISSING_BOTH".equals(status)) {
                missingCount++;
                abnormalCount++;
            } else if ("ABSENTEEISM".equals(status)) {
                absenteeismCount++;
                abnormalCount++;
            }
        }

        Map<String, Object> toMap() {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("payrollMonth", payrollMonth);
            payload.put("userId", userId);
            payload.put("settlementId", settlementId);
            payload.put("settlementStatus", settlementStatus);
            payload.put("confirmId", confirmId);
            payload.put("confirmStatus", confirmStatus);
            payload.put("ready", ATTENDANCE_SETTLEMENT_LOCKED.equals(settlementStatus)
                    && (ATTENDANCE_CONFIRM_CONFIRMED.equals(confirmStatus)
                    || ATTENDANCE_CONFIRM_RESOLVED.equals(confirmStatus)));
            payload.put("recordCount", recordCount);
            payload.put("lateCount", lateCount);
            payload.put("earlyLeaveCount", earlyLeaveCount);
            payload.put("missingCount", missingCount);
            payload.put("absenteeismCount", absenteeismCount);
            payload.put("abnormalCount", abnormalCount);
            payload.put("lateMinutes", lateMinutes);
            payload.put("earlyLeaveMinutes", earlyLeaveMinutes);
            payload.put("absentHours", absentHours);
            payload.put("leaveHours", leaveHours);
            payload.put("tripHours", tripHours);
            return payload;
        }
    }

}
