package com.kyx.service.hr.service.analytics;

import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.foundation.security.core.util.SecurityFrameworkUtils;
import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.service.business.api.dept.DeptApi;
import com.kyx.service.business.api.dept.dto.DeptRespDTO;
import com.kyx.service.hr.controller.admin.analytics.vo.HrAnalyticsWorkbenchRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeOverviewRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePerformancePageReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePerformanceStatsRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeRecruitmentPageReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeRecruitmentStatsRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeStatisticsRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeStatisticsTrendRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeTrainingPageReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeTrainingStatsRespVO;
import com.kyx.service.hr.controller.admin.lifecycle.vo.HrLifecycleWorkbenchRespVO;
import com.kyx.service.hr.controller.admin.payroll.vo.PayrollReportRespVO;
import com.kyx.service.hr.controller.admin.risk.vo.HrRiskWorkbenchRespVO;
import com.kyx.service.hr.controller.admin.todo.vo.HrTodoSummaryRespVO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceExceptionDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeDocumentRequestDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeMaterialDO;
import com.kyx.service.hr.dal.dataobject.payroll.PayrollBatchDO;
import com.kyx.service.hr.dal.dataobject.payroll.PayslipDO;
import com.kyx.service.hr.dal.dataobject.payroll.SocialSecurityAccountDO;
import com.kyx.service.hr.dal.dataobject.reminder.HrReminderRecordDO;
import com.kyx.service.hr.dal.mysql.attendance.AttendanceExceptionMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeDocumentRequestMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeEntryMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeMaterialMapper;
import com.kyx.service.hr.dal.mysql.payroll.PayrollBatchMapper;
import com.kyx.service.hr.dal.mysql.payroll.PayslipMapper;
import com.kyx.service.hr.dal.mysql.payroll.SocialSecurityAccountMapper;
import com.kyx.service.hr.dal.mysql.reminder.HrReminderRecordMapper;
import com.kyx.service.hr.dal.mysql.risk.HrRiskEventMapper;
import com.kyx.service.hr.dal.mysql.todo.HrTodoTaskMapper;
import com.kyx.service.hr.service.employee.EmployeePerformanceService;
import com.kyx.service.hr.service.employee.EmployeeRecruitmentService;
import com.kyx.service.hr.service.employee.EmployeeService;
import com.kyx.service.hr.service.employee.EmployeeTrainingService;
import com.kyx.service.hr.service.lifecycle.HrLifecycleService;
import com.kyx.service.hr.service.payroll.PayrollService;
import com.kyx.service.hr.service.risk.HrRiskWorkbenchService;
import com.kyx.service.hr.service.todo.HrTodoTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

@Service
@Validated
@Slf4j
public class HrAnalyticsWorkbenchServiceImpl implements HrAnalyticsWorkbenchService {

    private static final String ATTENDANCE_PENDING = "PENDING";
    private static final String DOCUMENT_PENDING = "PENDING";
    private static final String DOCUMENT_PROCESSING = "PROCESSING";
    private static final String MATERIAL_ACTIVE = "ACTIVE";
    private static final String MATERIAL_MISSING = "MISSING";
    private static final String MATERIAL_PENDING_REVIEW = "PENDING_REVIEW";
    private static final String PAYROLL_DRAFT = "DRAFT";
    private static final String PAYSLIP_ISSUE = "ISSUE";
    private static final String REMINDER_UNREAD = "UNREAD";
    private static final String TODO_OPEN = "OPEN";
    private static final Integer WORK_STATUS_PROBATION = 2;
    private static final Integer WORK_STATUS_ACTIVE = 3;
    private static final long SNAPSHOT_TTL_MILLIS = 60_000L;

    @Resource
    private EmployeeService employeeService;
    @Resource
    private EmployeeRecruitmentService employeeRecruitmentService;
    @Resource
    private EmployeePerformanceService employeePerformanceService;
    @Resource
    private EmployeeTrainingService employeeTrainingService;
    @Resource
    private HrTodoTaskService hrTodoTaskService;
    @Resource
    private HrRiskWorkbenchService hrRiskWorkbenchService;
    @Resource
    private HrLifecycleService hrLifecycleService;
    @Resource
    private PayrollService payrollService;
    @Resource
    private AttendanceExceptionMapper attendanceExceptionMapper;
    @Resource
    private EmployeeMaterialMapper employeeMaterialMapper;
    @Resource
    private EmployeeDocumentRequestMapper employeeDocumentRequestMapper;
    @Resource
    private PayrollBatchMapper payrollBatchMapper;
    @Resource
    private PayslipMapper payslipMapper;
    @Resource
    private SocialSecurityAccountMapper socialSecurityAccountMapper;
    @Resource
    private HrReminderRecordMapper hrReminderRecordMapper;
    @Resource
    private EmployeeEntryMapper employeeEntryMapper;
    @Resource
    private HrRiskEventMapper hrRiskEventMapper;
    @Resource
    private HrTodoTaskMapper hrTodoTaskMapper;
    @Resource
    private DeptApi deptApi;

    private final ConcurrentMap<String, SnapshotCacheValue> snapshotCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Object> snapshotLocks = new ConcurrentHashMap<>();

    @Override
    @Cacheable(cacheNames = "hr:analytics:workbench#60s",
            key = "'full:' + T(com.kyx.foundation.tenant.core.context.TenantContextHolder).getTenantId() + ':' + T(com.kyx.foundation.security.core.util.SecurityFrameworkUtils).getLoginUserId()",
            sync = true)
    public HrAnalyticsWorkbenchRespVO getWorkbench() {
        EmployeeStatisticsRespVO employeeStats = loadEmployeeStats();
        EmployeeStatisticsTrendRespVO employeeTrend = loadEmployeeTrend();
        EmployeeOverviewRespVO employeeOverview = loadEmployeeOverview();
        EmployeeRecruitmentStatsRespVO recruitmentStats = loadRecruitmentStats();
        EmployeePerformanceStatsRespVO performanceStats = loadPerformanceStats();
        EmployeeTrainingStatsRespVO trainingStats = loadTrainingStats();
        HrTodoSummaryRespVO todoSummary = loadTodoSummary();
        HrRiskWorkbenchRespVO riskWorkbench = loadRiskWorkbench();
        HrLifecycleWorkbenchRespVO lifecycleWorkbench = loadLifecycleWorkbench();
        PayrollReportRespVO payrollReport = loadPayrollReport();

        Integer pendingAttendanceExceptions = loadPendingAttendanceExceptions();
        Integer draftPayrollBatches = loadDraftPayrollBatches();
        Integer payrollIssueCount = loadPayrollIssueCount();
        Integer materialPendingReview = loadMaterialPendingReview();
        Integer materialExpiring = loadMaterialExpiring();
        Integer documentPending = loadDocumentPending();
        Integer reminderUnread = loadReminderUnread();
        Integer socialSecurityAccounts = loadSocialSecurityAccounts();

        HrAnalyticsWorkbenchRespVO respVO = new HrAnalyticsWorkbenchRespVO();
        respVO.setSummary(buildSummary(employeeStats, employeeOverview, recruitmentStats, performanceStats, trainingStats,
                todoSummary, riskWorkbench, lifecycleWorkbench, pendingAttendanceExceptions,
                draftPayrollBatches, payrollIssueCount, materialPendingReview, materialExpiring,
                documentPending, reminderUnread, socialSecurityAccounts));
        respVO.setTrend(buildTrend(employeeTrend));
        respVO.setExecutiveHighlights(buildExecutiveHighlights(respVO.getSummary()));
        respVO.setMetricCards(buildMetricCards(respVO.getSummary()));
        respVO.setWorkforceStructure(buildWorkforceStructure(employeeStats));
        respVO.setRecruitmentPipeline(buildRecruitmentPipeline(recruitmentStats));
        respVO.setRecruitmentEfficiency(buildRecruitmentEfficiency(recruitmentStats));
        respVO.setRecruitmentChannelDistribution(fromRecruitmentStats(recruitmentStats.getChannelStats(),
                "/hr/recruitment"));
        respVO.setRecruitmentChannelEfficiency(buildRecruitmentChannelEfficiency(recruitmentStats));
        respVO.setRecruitmentDemandHealth(fromRecruitmentStats(recruitmentStats.getDemandStatusStats(),
                "/hr/recruitment"));
        respVO.setPerformanceDistribution(buildPerformanceDistribution(performanceStats));
        respVO.setTrainingProgress(buildTrainingProgress(trainingStats));
        respVO.setTrainingQuality(buildTrainingQuality(trainingStats));
        respVO.setLifecycleEventTypes(fromLifecycleStats(lifecycleWorkbench.getEventTypeStats(),
                "event-type", "/hr/lifecycle/workbench"));
        respVO.setLifecycleEventStatus(fromLifecycleStats(lifecycleWorkbench.getEventStatusStats(),
                "event-status", "/hr/lifecycle/workbench"));
        respVO.setLifecycleTaskStatus(fromLifecycleStats(lifecycleWorkbench.getTaskStatusStats(),
                "task-status", "/hr/lifecycle/workbench"));
        respVO.setTodoHealth(buildTodoHealth(todoSummary));
        respVO.setEmployeeServiceBacklog(buildEmployeeServiceBacklog(respVO.getSummary()));
        respVO.setPayrollCostTrend(buildPayrollCostTrend(payrollReport));
        respVO.setPayrollPerCapitaTrend(buildPayrollPerCapitaTrend(payrollReport));
        respVO.setAttendanceExceptionStatus(safe("attendance exception status distribution",
                this::buildAttendanceExceptionStatus, Collections.emptyList()));
        respVO.setRiskCategories(buildRiskCategories(riskWorkbench));
        respVO.setDeptRiskTodoDistribution(safe("department risk todo distribution",
                this::buildDeptRiskTodoDistribution, Collections.emptyList()));
        respVO.setInsights(buildInsights(respVO.getSummary()));
        respVO.setQuickActions(buildQuickActions());
        return respVO;
    }

    @Override
    @Cacheable(cacheNames = "hr:analytics:workbench:overview#60s",
            key = "'current:' + T(com.kyx.foundation.tenant.core.context.TenantContextHolder).getTenantId() + ':' + T(com.kyx.foundation.security.core.util.SecurityFrameworkUtils).getLoginUserId()",
            sync = true)
    public HrAnalyticsWorkbenchRespVO getWorkbenchOverview() {
        EmployeeStatisticsRespVO employeeStats = loadEmployeeStats();
        EmployeeStatisticsTrendRespVO employeeTrend = loadEmployeeTrend();
        EmployeeOverviewRespVO employeeOverview = loadEmployeeOverview();
        EmployeeRecruitmentStatsRespVO recruitmentStats = loadRecruitmentStats();
        EmployeePerformanceStatsRespVO performanceStats = loadPerformanceStats();
        EmployeeTrainingStatsRespVO trainingStats = loadTrainingStats();
        HrTodoSummaryRespVO todoSummary = loadTodoSummary();
        HrRiskWorkbenchRespVO riskWorkbench = loadRiskWorkbench();
        HrLifecycleWorkbenchRespVO lifecycleWorkbench = loadLifecycleWorkbench();

        Integer pendingAttendanceExceptions = loadPendingAttendanceExceptions();
        Integer draftPayrollBatches = loadDraftPayrollBatches();
        Integer payrollIssueCount = loadPayrollIssueCount();
        Integer materialPendingReview = loadMaterialPendingReview();
        Integer materialExpiring = loadMaterialExpiring();
        Integer documentPending = loadDocumentPending();
        Integer reminderUnread = loadReminderUnread();
        Integer socialSecurityAccounts = loadSocialSecurityAccounts();

        HrAnalyticsWorkbenchRespVO respVO = blankWorkbench();
        respVO.setSummary(buildSummary(employeeStats, employeeOverview, recruitmentStats, performanceStats, trainingStats,
                todoSummary, riskWorkbench, lifecycleWorkbench, pendingAttendanceExceptions,
                draftPayrollBatches, payrollIssueCount, materialPendingReview, materialExpiring,
                documentPending, reminderUnread, socialSecurityAccounts));
        respVO.setTrend(buildTrend(employeeTrend));
        respVO.setExecutiveHighlights(buildExecutiveHighlights(respVO.getSummary()));
        respVO.setMetricCards(buildMetricCards(respVO.getSummary()));
        respVO.setInsights(buildInsights(respVO.getSummary()));
        respVO.setQuickActions(buildQuickActions());
        return respVO;
    }

    @Override
    @Cacheable(cacheNames = "hr:analytics:workbench:signals#60s",
            key = "'current:' + T(com.kyx.foundation.tenant.core.context.TenantContextHolder).getTenantId() + ':' + T(com.kyx.foundation.security.core.util.SecurityFrameworkUtils).getLoginUserId()",
            sync = true)
    public HrAnalyticsWorkbenchRespVO getWorkbenchSignals() {
        HrTodoSummaryRespVO todoSummary = loadTodoSummary();
        HrRiskWorkbenchRespVO riskWorkbench = loadRiskWorkbench();
        HrLifecycleWorkbenchRespVO lifecycleWorkbench = loadLifecycleWorkbench();
        Integer pendingAttendanceExceptions = loadPendingAttendanceExceptions();
        Integer payrollIssueCount = loadPayrollIssueCount();
        Integer materialPendingReview = loadMaterialPendingReview();
        Integer materialExpiring = loadMaterialExpiring();
        Integer documentPending = loadDocumentPending();
        Integer reminderUnread = loadReminderUnread();

        HrAnalyticsWorkbenchRespVO respVO = blankWorkbench();
        HrAnalyticsWorkbenchRespVO.Summary signalSummary = new HrAnalyticsWorkbenchRespVO.Summary();
        signalSummary.setPendingLifecycleEvents(orZero(lifecycleWorkbench.getPendingEventCount()));
        signalSummary.setPendingAttendanceExceptions(orZero(pendingAttendanceExceptions));
        signalSummary.setPayrollIssueCount(orZero(payrollIssueCount));
        signalSummary.setMaterialPendingReview(orZero(materialPendingReview));
        signalSummary.setMaterialExpiring(orZero(materialExpiring));
        signalSummary.setDocumentPending(orZero(documentPending));
        signalSummary.setReminderUnread(orZero(reminderUnread));
        respVO.setEmployeeServiceBacklog(buildEmployeeServiceBacklog(signalSummary));
        respVO.setTodoHealth(buildTodoHealth(todoSummary));
        respVO.setLifecycleEventTypes(fromLifecycleStats(lifecycleWorkbench.getEventTypeStats(),
                "event-type", "/hr/lifecycle/workbench"));
        respVO.setLifecycleEventStatus(fromLifecycleStats(lifecycleWorkbench.getEventStatusStats(),
                "event-status", "/hr/lifecycle/workbench"));
        respVO.setLifecycleTaskStatus(fromLifecycleStats(lifecycleWorkbench.getTaskStatusStats(),
                "task-status", "/hr/lifecycle/workbench"));
        respVO.setAttendanceExceptionStatus(safe("attendance exception status distribution",
                this::buildAttendanceExceptionStatus, Collections.emptyList()));
        respVO.setRiskCategories(buildRiskCategories(riskWorkbench));
        respVO.setDeptRiskTodoDistribution(safe("department risk todo distribution",
                this::buildDeptRiskTodoDistribution, Collections.emptyList()));
        return respVO;
    }

    @Override
    @Cacheable(cacheNames = "hr:analytics:workbench:core-charts#60s",
            key = "'current:' + T(com.kyx.foundation.tenant.core.context.TenantContextHolder).getTenantId() + ':' + T(com.kyx.foundation.security.core.util.SecurityFrameworkUtils).getLoginUserId()",
            sync = true)
    public HrAnalyticsWorkbenchRespVO getWorkbenchCoreCharts() {
        EmployeeStatisticsRespVO employeeStats = loadEmployeeStats();
        EmployeeRecruitmentStatsRespVO recruitmentStats = loadRecruitmentStats();
        EmployeePerformanceStatsRespVO performanceStats = loadPerformanceStats();
        EmployeeTrainingStatsRespVO trainingStats = loadTrainingStats();

        HrAnalyticsWorkbenchRespVO respVO = blankWorkbench();
        respVO.setWorkforceStructure(buildWorkforceStructure(employeeStats));
        respVO.setRecruitmentPipeline(buildRecruitmentPipeline(recruitmentStats));
        respVO.setRecruitmentEfficiency(buildRecruitmentEfficiency(recruitmentStats));
        respVO.setPerformanceDistribution(buildPerformanceDistribution(performanceStats));
        respVO.setTrainingProgress(buildTrainingProgress(trainingStats));
        respVO.setTrainingQuality(buildTrainingQuality(trainingStats));
        return respVO;
    }

    @Override
    @Cacheable(cacheNames = "hr:analytics:workbench:extended-charts#60s",
            key = "'current:' + T(com.kyx.foundation.tenant.core.context.TenantContextHolder).getTenantId() + ':' + T(com.kyx.foundation.security.core.util.SecurityFrameworkUtils).getLoginUserId()",
            sync = true)
    public HrAnalyticsWorkbenchRespVO getWorkbenchExtendedCharts() {
        EmployeeRecruitmentStatsRespVO recruitmentStats = loadRecruitmentStats();
        PayrollReportRespVO payrollReport = loadPayrollReport();

        HrAnalyticsWorkbenchRespVO respVO = blankWorkbench();
        respVO.setRecruitmentChannelDistribution(fromRecruitmentStats(recruitmentStats.getChannelStats(),
                "/hr/recruitment"));
        respVO.setRecruitmentChannelEfficiency(buildRecruitmentChannelEfficiency(recruitmentStats));
        respVO.setRecruitmentDemandHealth(fromRecruitmentStats(recruitmentStats.getDemandStatusStats(),
                "/hr/recruitment"));
        respVO.setPayrollCostTrend(buildPayrollCostTrend(payrollReport));
        respVO.setPayrollPerCapitaTrend(buildPayrollPerCapitaTrend(payrollReport));
        return respVO;
    }

    private HrAnalyticsWorkbenchRespVO blankWorkbench() {
        HrAnalyticsWorkbenchRespVO respVO = new HrAnalyticsWorkbenchRespVO();
        respVO.setSummary(null);
        respVO.setTrend(null);
        respVO.setExecutiveHighlights(null);
        respVO.setMetricCards(null);
        respVO.setWorkforceStructure(null);
        respVO.setRecruitmentPipeline(null);
        respVO.setRecruitmentEfficiency(null);
        respVO.setRecruitmentChannelDistribution(null);
        respVO.setRecruitmentChannelEfficiency(null);
        respVO.setRecruitmentDemandHealth(null);
        respVO.setPerformanceDistribution(null);
        respVO.setTrainingProgress(null);
        respVO.setTrainingQuality(null);
        respVO.setLifecycleEventTypes(null);
        respVO.setLifecycleEventStatus(null);
        respVO.setLifecycleTaskStatus(null);
        respVO.setTodoHealth(null);
        respVO.setEmployeeServiceBacklog(null);
        respVO.setPayrollCostTrend(null);
        respVO.setPayrollPerCapitaTrend(null);
        respVO.setAttendanceExceptionStatus(null);
        respVO.setRiskCategories(null);
        respVO.setDeptRiskTodoDistribution(null);
        respVO.setInsights(null);
        respVO.setQuickActions(null);
        return respVO;
    }

    private EmployeeStatisticsRespVO loadEmployeeStats() {
        return cachedSnapshot("employee-statistics", employeeService::getEmployeeStatistics, new EmployeeStatisticsRespVO());
    }

    private EmployeeStatisticsTrendRespVO loadEmployeeTrend() {
        return cachedSnapshot("employee-trend-12", () -> employeeService.getEmployeeStatisticsTrend(12),
                new EmployeeStatisticsTrendRespVO());
    }

    private EmployeeOverviewRespVO loadEmployeeOverview() {
        return cachedSnapshot("employee-overview", employeeService::getEmployeeOverview, new EmployeeOverviewRespVO());
    }

    private EmployeeRecruitmentStatsRespVO loadRecruitmentStats() {
        return cachedSnapshot("recruitment-statistics",
                () -> employeeRecruitmentService.getRecruitmentStats(new EmployeeRecruitmentPageReqVO()),
                new EmployeeRecruitmentStatsRespVO());
    }

    private EmployeePerformanceStatsRespVO loadPerformanceStats() {
        return cachedSnapshot("performance-statistics",
                () -> employeePerformanceService.getPerformanceStats(new EmployeePerformancePageReqVO()),
                new EmployeePerformanceStatsRespVO());
    }

    private EmployeeTrainingStatsRespVO loadTrainingStats() {
        return cachedSnapshot("training-statistics",
                () -> employeeTrainingService.getTrainingStats(new EmployeeTrainingPageReqVO()),
                new EmployeeTrainingStatsRespVO());
    }

    private HrTodoSummaryRespVO loadTodoSummary() {
        return cachedSnapshot("todo-summary", () -> hrTodoTaskService.getSummary(false), new HrTodoSummaryRespVO());
    }

    private HrRiskWorkbenchRespVO loadRiskWorkbench() {
        return cachedSnapshot("risk-workbench", hrRiskWorkbenchService::getWorkbench, new HrRiskWorkbenchRespVO());
    }

    private HrLifecycleWorkbenchRespVO loadLifecycleWorkbench() {
        return cachedSnapshot("lifecycle-workbench", hrLifecycleService::getWorkbench, new HrLifecycleWorkbenchRespVO());
    }

    private PayrollReportRespVO loadPayrollReport() {
        int year = LocalDate.now().getYear();
        return cachedSnapshot("payroll-report-" + year, () -> payrollService.getReport(year), new PayrollReportRespVO());
    }

    private Integer loadPendingAttendanceExceptions() {
        return cachedSnapshot("attendance-exception-count", this::countPendingAttendanceExceptions, 0);
    }

    private Integer loadDraftPayrollBatches() {
        return cachedSnapshot("payroll-draft-batch-count", this::countDraftPayrollBatches, 0);
    }

    private Integer loadPayrollIssueCount() {
        return cachedSnapshot("payroll-issue-count", this::countPayrollIssues, 0);
    }

    private Integer loadMaterialPendingReview() {
        return cachedSnapshot("material-pending-review-count", this::countMaterialPendingReviews, 0);
    }

    private Integer loadMaterialExpiring() {
        return cachedSnapshot("material-expiring-count", this::countMaterialExpiring, 0);
    }

    private Integer loadDocumentPending() {
        return cachedSnapshot("document-pending-count", this::countPendingDocumentRequests, 0);
    }

    private Integer loadReminderUnread() {
        return cachedSnapshot("reminder-unread-count", this::countUnreadReminders, 0);
    }

    private Integer loadSocialSecurityAccounts() {
        return cachedSnapshot("social-security-account-count", this::countSocialSecurityAccounts, 0);
    }

    @SuppressWarnings("unchecked")
    private <T> T cachedSnapshot(String name, Supplier<T> loader, T fallback) {
        String key = buildSnapshotKey(name);
        long now = System.currentTimeMillis();
        SnapshotCacheValue current = snapshotCache.get(key);
        if (current != null && current.expiresAt > now) {
            return (T) current.value;
        }
        Object lock = snapshotLocks.computeIfAbsent(key, ignored -> new Object());
        synchronized (lock) {
            current = snapshotCache.get(key);
            now = System.currentTimeMillis();
            if (current != null && current.expiresAt > now) {
                return (T) current.value;
            }
            T value = safe(name, loader, fallback);
            snapshotCache.put(key, new SnapshotCacheValue(value, now + SNAPSHOT_TTL_MILLIS));
            if (snapshotCache.size() > 200) {
                long cleanupNow = now;
                snapshotCache.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().expiresAt <= cleanupNow);
            }
            return value;
        }
    }

    private String buildSnapshotKey(String name) {
        Long tenantId = TenantContextHolder.getTenantId();
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        return (tenantId == null ? 0L : tenantId) + ":" + (userId == null ? 0L : userId) + ":" + name;
    }

    private HrAnalyticsWorkbenchRespVO.Summary buildSummary(EmployeeStatisticsRespVO employeeStats,
                                                            EmployeeOverviewRespVO employeeOverview,
                                                            EmployeeRecruitmentStatsRespVO recruitmentStats,
                                                            EmployeePerformanceStatsRespVO performanceStats,
                                                            EmployeeTrainingStatsRespVO trainingStats,
                                                            HrTodoSummaryRespVO todoSummary,
                                                            HrRiskWorkbenchRespVO riskWorkbench,
                                                            HrLifecycleWorkbenchRespVO lifecycleWorkbench,
                                                            Integer pendingAttendanceExceptions,
                                                            Integer draftPayrollBatches,
                                                            Integer payrollIssueCount,
                                                            Integer materialPendingReview,
                                                            Integer materialExpiring,
                                                            Integer documentPending,
                                                            Integer reminderUnread,
                                                            Integer socialSecurityAccounts) {
        HrRiskWorkbenchRespVO.Summary riskSummary = riskWorkbench.getSummary() == null
                ? new HrRiskWorkbenchRespVO.Summary() : riskWorkbench.getSummary();
        EmployeeOverviewRespVO.Age age = employeeOverview.getAge();
        EmployeeOverviewRespVO.Education education = employeeOverview.getEducation();

        HrAnalyticsWorkbenchRespVO.Summary summary = new HrAnalyticsWorkbenchRespVO.Summary();
        summary.setTotalEmployees(orZero(employeeStats.getTotal()));
        summary.setActiveEmployees(orZero(employeeStats.getActive()));
        summary.setProbationEmployees(orZero(employeeStats.getProbation()));
        summary.setMasterOrAboveEmployees(education == null ? 0 : orZero(education.getMaster()));
        summary.setMasterOrAbovePercent(education == null ? 0 : orZero(education.getMasterPercent()));
        summary.setEducationFilledEmployees(education == null ? 0 : orZero(education.getRelated()));
        summary.setOnboardingEmployees(orZero(employeeStats.getOnboarding()));
        summary.setLeavingEmployees(orZero(employeeStats.getLeaving()));
        summary.setNewThisMonth(orZero(lifecycleWorkbench.getMonthOnboardCount()));
        summary.setLeaveThisMonth(orZero(lifecycleWorkbench.getMonthResignCount()));
        summary.setStabilityIndex(age == null ? 0 : orZero(age.getStability()));
        summary.setAvgAge(age == null || age.getAvgAge() == null ? BigDecimal.ZERO : age.getAvgAge());

        summary.setRecruitmentTotal(orZero(recruitmentStats.getTotalCount()));
        summary.setRecruitmentOffer(orZero(recruitmentStats.getOfferCount()));
        summary.setRecruitmentEntry(orZero(recruitmentStats.getEntryCount()));
        summary.setRecruitmentOverdue(orZero(recruitmentStats.getOverdueFollowCount()));
        summary.setRecruitmentDemandApproved(orZero(recruitmentStats.getDemandApprovedCount()));
        summary.setRecruitmentInterviewEvaluated(orZero(recruitmentStats.getInterviewEvaluatedCount()));
        summary.setRecruitmentReferral(orZero(recruitmentStats.getReferralCount()));
        summary.setRecruitmentTouched(orZero(recruitmentStats.getTouchedCount()));
        summary.setRecruitmentChannelCost(recruitmentStats.getChannelCostTotal() == null
                ? BigDecimal.ZERO : recruitmentStats.getChannelCostTotal());
        summary.setRecruitmentAvgInterviewScore(recruitmentStats.getAvgInterviewScore() == null
                ? BigDecimal.ZERO : recruitmentStats.getAvgInterviewScore());

        summary.setPerformanceTotal(orZero(performanceStats.getTotalCount()));
        summary.setAvgPerformanceScore(performanceStats.getAvgScore() == null
                ? BigDecimal.ZERO : performanceStats.getAvgScore());
        summary.setPerformanceExcellent(orZero(performanceStats.getExcellentCount()));
        summary.setPerformanceWarning(orZero(performanceStats.getWarningCount()));

        summary.setOpenTodoCount(orZero(todoSummary.getOpenCount()));
        summary.setOverdueTodoCount(orZero(todoSummary.getOverdueCount()));
        summary.setHighPriorityTodoCount(orZero(todoSummary.getHighPriorityCount()));
        summary.setHighRiskCount(orZero(riskSummary.getHighRiskCount()));
        summary.setMediumRiskCount(orZero(riskSummary.getMediumRiskCount()));
        summary.setDataQualityScore(orZero(riskSummary.getDataQualityScore()));
        summary.setPendingLifecycleEvents(orZero(lifecycleWorkbench.getPendingEventCount()));
        summary.setPendingAttendanceExceptions(orZero(pendingAttendanceExceptions));
        summary.setDraftPayrollBatches(orZero(draftPayrollBatches));
        summary.setPayrollIssueCount(orZero(payrollIssueCount));
        summary.setTrainingTotal(orZero(trainingStats.getTotalCount()));
        summary.setTrainingCompleted(orZero(trainingStats.getCompletedCount()));
        summary.setTrainingOverdue(orZero(trainingStats.getOverdueCount()));
        summary.setTrainingSatisfactionRate(trainingStats.getSatisfactionRate() == null
                ? BigDecimal.ZERO : trainingStats.getSatisfactionRate());
        summary.setMaterialPendingReview(orZero(materialPendingReview));
        summary.setMaterialExpiring(orZero(materialExpiring));
        summary.setDocumentPending(orZero(documentPending));
        summary.setReminderUnread(orZero(reminderUnread));
        summary.setSocialSecurityAccounts(orZero(socialSecurityAccounts));
        return summary;
    }

    private HrAnalyticsWorkbenchRespVO.Trend buildTrend(EmployeeStatisticsTrendRespVO source) {
        HrAnalyticsWorkbenchRespVO.Trend trend = new HrAnalyticsWorkbenchRespVO.Trend();
        trend.setMonths(list(source.getMonths()));
        trend.setTotalTrend(list(source.getTotalTrend()));
        trend.setActiveTrend(list(source.getActiveTrend()));
        trend.setOnboardingTrend(list(source.getOnboardingTrend()));
        trend.setProbationTrend(list(source.getProbationTrend()));
        trend.setLeavingTrend(list(source.getLeavingTrend()));
        trend.setStabilityTrend(list(source.getStabilityTrend()));
        return trend;
    }

    private List<HrAnalyticsWorkbenchRespVO.ExecutiveHighlight> buildExecutiveHighlights(
            HrAnalyticsWorkbenchRespVO.Summary summary) {
        List<HrAnalyticsWorkbenchRespVO.ExecutiveHighlight> items = new ArrayList<>();
        int netChange = orZero(summary.getNewThisMonth()) - orZero(summary.getLeaveThisMonth());
        items.add(executive("organization", "组织稳定", summary.getStabilityIndex() + "%",
                "在职 " + summary.getActiveEmployees() + " 人，本月净变化 " + netChange,
                "lucide:activity", stabilityTone(summary.getStabilityIndex()), "/hr/lifecycle/workbench"));
        int pressure = orZero(summary.getHighRiskCount())
                + orZero(summary.getMediumRiskCount())
                + orZero(summary.getOverdueTodoCount());
        items.add(executive("risk-pressure", "风险压力", String.valueOf(pressure),
                "高风险 " + summary.getHighRiskCount() + " / 逾期待办 " + summary.getOverdueTodoCount(),
                "lucide:shield-alert", pressureTone(pressure), "/hr/risk"));
        items.add(executive("talent-supply", "人才供给", String.valueOf(summary.getRecruitmentTotal()),
                "需求 " + summary.getRecruitmentDemandApproved() + " / Offer " + summary.getRecruitmentOffer()
                        + " / 入职 " + summary.getRecruitmentEntry(),
                "lucide:user-plus", summary.getRecruitmentOverdue() > 0 ? "orange" : "cyan", "/hr/recruitment"));
        int serviceBacklog = orZero(summary.getDocumentPending())
                + orZero(summary.getMaterialPendingReview())
                + orZero(summary.getReminderUnread())
                + orZero(summary.getPayrollIssueCount());
        items.add(executive("service-backlog", "服务积压", String.valueOf(serviceBacklog),
                "证明 " + summary.getDocumentPending() + " / 材料 " + summary.getMaterialPendingReview()
                        + " / 薪资异议 " + summary.getPayrollIssueCount(),
                "lucide:folder-clock", serviceBacklog > 0 ? "amber" : "green", "/hr/todo"));
        return items;
    }

    private List<HrAnalyticsWorkbenchRespVO.MetricCard> buildMetricCards(HrAnalyticsWorkbenchRespVO.Summary summary) {
        List<HrAnalyticsWorkbenchRespVO.MetricCard> cards = new ArrayList<>();
        cards.add(metric("employees", "在册员工", summary.getTotalEmployees(), "人",
                "在职 " + summary.getActiveEmployees() + " 人", "lucide:users-round", "blue", "/hr/employee"));
        cards.add(metric("probation", "试用期员工", summary.getProbationEmployees(), "人",
                "按入职日期与试用期识别", "lucide:hourglass", "orange", "/hr/employee"));
        cards.add(metric("stability", "组织稳定度", summary.getStabilityIndex(), "%",
                "本月入职 " + summary.getNewThisMonth() + " / 离职 " + summary.getLeaveThisMonth(),
                "lucide:activity", "green", "/hr/lifecycle/workbench"));
        cards.add(metric("master-or-above", "硕士以上", summary.getMasterOrAboveEmployees(), "人",
                "占比 " + summary.getMasterOrAbovePercent() + "% / 已填 " + summary.getEducationFilledEmployees(),
                "lucide:graduation-cap", "cyan", "/hr/employee"));
        cards.add(metric("recruitment", "招聘漏斗", summary.getRecruitmentTotal(), "人",
                "Offer " + summary.getRecruitmentOffer() + " / 入职 " + summary.getRecruitmentEntry(),
                "lucide:user-plus", "cyan", "/hr/recruitment"));
        cards.add(metric("performance", "绩效均分", formatDecimal(summary.getAvgPerformanceScore()), "分",
                "预警 " + summary.getPerformanceWarning() + " 人", "lucide:trophy", "amber", "/hr/performance"));
        cards.add(metric("training", "培训完成", summary.getTrainingCompleted(), "项",
                "逾期 " + summary.getTrainingOverdue() + " / 满意 " + formatDecimal(summary.getTrainingSatisfactionRate()) + "%",
                "lucide:graduation-cap", "green", "/hr/training"));
        cards.add(metric("todo", "开放待办", summary.getOpenTodoCount(), "项",
                "逾期 " + summary.getOverdueTodoCount() + " / 高优 " + summary.getHighPriorityTodoCount(),
                "lucide:list-checks", "orange", "/hr/todo"));
        cards.add(metric("risk", "高风险", summary.getHighRiskCount(), "项",
                "数据质量 " + summary.getDataQualityScore() + " 分", "lucide:shield-alert", "red", "/hr/risk"));
        return cards;
    }

    private List<HrAnalyticsWorkbenchRespVO.ChartItem> buildWorkforceStructure(EmployeeStatisticsRespVO stats) {
        int total = Math.max(orZero(stats.getTotal()), 1);
        List<HrAnalyticsWorkbenchRespVO.ChartItem> items = new ArrayList<>();
        items.add(chart("fullTime", "全职", orZero(stats.getFullTime()), percent(stats.getFullTime(), total), "blue", "/hr/employee"));
        items.add(chart("partTime", "兼职", orZero(stats.getPartTime()), percent(stats.getPartTime(), total), "cyan", "/hr/employee"));
        items.add(chart("intern", "实习", orZero(stats.getIntern()), percent(stats.getIntern(), total), "green", "/hr/employee"));
        items.add(chart("labor", "劳务", orZero(stats.getLabor()), percent(stats.getLabor(), total), "amber", "/hr/employee"));
        items.add(chart("probation", "试用", orZero(stats.getProbation()), percent(stats.getProbation(), total), "orange", "/hr/employee"));
        items.add(chart("leaving", "待离职", orZero(stats.getLeaving()), percent(stats.getLeaving(), total), "red", "/hr/lifecycle/workbench"));
        return items;
    }

    private List<HrAnalyticsWorkbenchRespVO.ChartItem> buildTrainingProgress(EmployeeTrainingStatsRespVO stats) {
        int total = Math.max(orZero(stats.getTotalCount()), 1);
        List<HrAnalyticsWorkbenchRespVO.ChartItem> items = new ArrayList<>();
        items.add(chart("completed", "已完成", orZero(stats.getCompletedCount()), percent(stats.getCompletedCount(), total), "green", "/hr/training"));
        items.add(chart("inProgress", "进行中", orZero(stats.getInProgressCount()), percent(stats.getInProgressCount(), total), "blue", "/hr/training"));
        items.add(chart("upcoming", "待开始", orZero(stats.getUpcomingCount()), percent(stats.getUpcomingCount(), total), "cyan", "/hr/training"));
        items.add(chart("overdue", "需跟进", orZero(stats.getOverdueCount()), percent(stats.getOverdueCount(), total), "red", "/hr/training"));
        items.add(chart("retrainDue", "复训提醒", orZero(stats.getRetrainDueCount()), percent(stats.getRetrainDueCount(), total), "amber", "/hr/training"));
        items.add(chart("certificateExpiring", "证书到期", orZero(stats.getCertificateExpiringCount()), percent(stats.getCertificateExpiringCount(), total), "orange", "/hr/training"));
        return items;
    }

    private List<HrAnalyticsWorkbenchRespVO.ChartItem> buildEmployeeServiceBacklog(
            HrAnalyticsWorkbenchRespVO.Summary summary) {
        List<HrAnalyticsWorkbenchRespVO.ChartItem> items = new ArrayList<>();
        int total = summary.getDocumentPending()
                + summary.getMaterialPendingReview()
                + summary.getMaterialExpiring()
                + summary.getPendingAttendanceExceptions()
                + summary.getPayrollIssueCount()
                + summary.getReminderUnread()
                + summary.getPendingLifecycleEvents();
        int denominator = Math.max(total, 1);
        items.add(chart("documentPending", "证明待办", summary.getDocumentPending(), percent(summary.getDocumentPending(), denominator), "blue", "/hr/document-request"));
        items.add(chart("materialReview", "材料待审", summary.getMaterialPendingReview(), percent(summary.getMaterialPendingReview(), denominator), "cyan", "/hr/employee-material"));
        items.add(chart("materialExpiring", "材料到期", summary.getMaterialExpiring(), percent(summary.getMaterialExpiring(), denominator), "amber", "/hr/employee-material"));
        items.add(chart("attendanceException", "考勤异常", summary.getPendingAttendanceExceptions(), percent(summary.getPendingAttendanceExceptions(), denominator), "orange", "/attendance/exceptions"));
        items.add(chart("payrollIssue", "工资异议", summary.getPayrollIssueCount(), percent(summary.getPayrollIssueCount(), denominator), "red", "/hr/payroll"));
        items.add(chart("reminderUnread", "未读提醒", summary.getReminderUnread(), percent(summary.getReminderUnread(), denominator), "green", "/hr/reminder"));
        items.add(chart("lifecyclePending", "生命周期", summary.getPendingLifecycleEvents(), percent(summary.getPendingLifecycleEvents(), denominator), "blue", "/hr/lifecycle/workbench"));
        return items;
    }

    private List<HrAnalyticsWorkbenchRespVO.ChartItem> buildPayrollCostTrend(PayrollReportRespVO report) {
        if (report.getMonths() == null || report.getMonths().isEmpty()) {
            return Collections.emptyList();
        }
        int max = report.getMonths().stream()
                .map(PayrollReportRespVO.MonthSummary::getLaborCostTotal)
                .map(this::amountToTenThousand)
                .max(Integer::compareTo)
                .orElse(1);
        int denominator = Math.max(max, 1);
        List<HrAnalyticsWorkbenchRespVO.ChartItem> items = new ArrayList<>();
        for (PayrollReportRespVO.MonthSummary month : report.getMonths()) {
            int value = amountToTenThousand(month.getLaborCostTotal());
            items.add(chart(month.getPayrollMonth(), month.getPayrollMonth(), value,
                    percent(value, denominator), "green", "/hr/payroll"));
        }
        return items;
    }

    private List<HrAnalyticsWorkbenchRespVO.ChartItem> buildPayrollPerCapitaTrend(PayrollReportRespVO report) {
        if (report.getMonths() == null || report.getMonths().isEmpty()) {
            return Collections.emptyList();
        }
        int max = report.getMonths().stream()
                .map(PayrollReportRespVO.MonthSummary::getLaborCostPerCapita)
                .map(this::amountToThousand)
                .max(Integer::compareTo)
                .orElse(1);
        int denominator = Math.max(max, 1);
        List<HrAnalyticsWorkbenchRespVO.ChartItem> items = new ArrayList<>();
        for (PayrollReportRespVO.MonthSummary month : report.getMonths()) {
            int value = amountToThousand(month.getLaborCostPerCapita());
            items.add(chart(month.getPayrollMonth(), month.getPayrollMonth(), value,
                    percent(value, denominator), "cyan", "/hr/payroll"));
        }
        return items;
    }

    private List<HrAnalyticsWorkbenchRespVO.ChartItem> buildRecruitmentChannelEfficiency(
            EmployeeRecruitmentStatsRespVO stats) {
        if (stats.getChannelEffectStats() == null || stats.getChannelEffectStats().isEmpty()) {
            return Collections.emptyList();
        }
        List<EmployeeRecruitmentStatsRespVO.ChannelEffectItem> source = new ArrayList<>(stats.getChannelEffectStats());
        source.sort(Comparator
                .comparing((EmployeeRecruitmentStatsRespVO.ChannelEffectItem item) -> rateValue(item.getEntryRate()))
                .reversed()
                .thenComparing(Comparator.comparing(EmployeeRecruitmentStatsRespVO.ChannelEffectItem::getEntryCount,
                        Comparator.nullsLast(Integer::compareTo)).reversed()));
        List<HrAnalyticsWorkbenchRespVO.ChartItem> items = new ArrayList<>();
        int index = 0;
        for (EmployeeRecruitmentStatsRespVO.ChannelEffectItem item : source) {
            if (items.size() >= 8) {
                break;
            }
            items.add(rateChart("channel-efficiency-" + index, defaultText(item.getName(), "未填写"),
                    item.getEntryRate(), toneByIndex(index), "/hr/recruitment"));
            index++;
        }
        return items;
    }

    private List<HrAnalyticsWorkbenchRespVO.ChartItem> buildRecruitmentPipeline(EmployeeRecruitmentStatsRespVO stats) {
        if (stats.getStageStats() != null && !stats.getStageStats().isEmpty()) {
            return fromRecruitmentStats(stats.getStageStats(), "/hr/recruitment");
        }
        int total = Math.max(orZero(stats.getTotalCount()), 1);
        List<HrAnalyticsWorkbenchRespVO.ChartItem> items = new ArrayList<>();
        items.add(chart("interview", "面试中", orZero(stats.getInterviewCount()), percent(stats.getInterviewCount(), total), "blue", "/hr/recruitment"));
        items.add(chart("offer", "已发 Offer", orZero(stats.getOfferCount()), percent(stats.getOfferCount(), total), "cyan", "/hr/recruitment"));
        items.add(chart("accepted", "已接 Offer", orZero(stats.getOfferAcceptedCount()), percent(stats.getOfferAcceptedCount(), total), "green", "/hr/recruitment"));
        items.add(chart("pendingEntry", "待入职", orZero(stats.getPendingEntryCount()), percent(stats.getPendingEntryCount(), total), "amber", "/hr/recruitment"));
        items.add(chart("entry", "已入职", orZero(stats.getEntryCount()), percent(stats.getEntryCount(), total), "green", "/hr/recruitment"));
        items.add(chart("overdue", "逾期跟进", orZero(stats.getOverdueFollowCount()), percent(stats.getOverdueFollowCount(), total), "red", "/hr/recruitment"));
        return items;
    }

    private List<HrAnalyticsWorkbenchRespVO.ChartItem> buildRecruitmentEfficiency(EmployeeRecruitmentStatsRespVO stats) {
        List<HrAnalyticsWorkbenchRespVO.ChartItem> items = new ArrayList<>();
        items.add(rateChart("responseRate", "回复率", stats.getResponseRate(), "cyan"));
        items.add(rateChart("interviewRate", "面试率", stats.getInterviewRate(), "blue"));
        items.add(rateChart("offerRate", "Offer率", stats.getOfferRate(), "amber"));
        items.add(rateChart("entryRate", "入职率", stats.getEntryRate(), "green"));
        return items;
    }

    private List<HrAnalyticsWorkbenchRespVO.ChartItem> buildTrainingQuality(EmployeeTrainingStatsRespVO stats) {
        List<HrAnalyticsWorkbenchRespVO.ChartItem> items = new ArrayList<>();
        items.add(rateChart("completionRate", "完成率", stats.getCompletionRate(), "green", "/hr/training"));
        items.add(rateChart("overdueRate", "逾期率", stats.getOverdueRate(), "red", "/hr/training"));
        items.add(rateChart("certificateCoverageRate", "证书覆盖", stats.getCertificateCoverageRate(), "blue",
                "/hr/training"));
        items.add(rateChart("examPassRate", "考试通过", stats.getExamPassRate(), "cyan", "/hr/training"));
        items.add(rateChart("satisfactionRate", "满意度", stats.getSatisfactionRate(), "amber", "/hr/training"));
        return items;
    }

    private List<HrAnalyticsWorkbenchRespVO.ChartItem> buildTodoHealth(HrTodoSummaryRespVO summary) {
        int total = Math.max(orZero(summary.getOpenCount()) + orZero(summary.getDoneCount()), 1);
        List<HrAnalyticsWorkbenchRespVO.ChartItem> items = new ArrayList<>();
        items.add(chart("open", "开放待办", orZero(summary.getOpenCount()), percent(summary.getOpenCount(), total),
                "blue", "/hr/todo"));
        items.add(chart("overdue", "逾期待办", orZero(summary.getOverdueCount()),
                percent(summary.getOverdueCount(), total), "red", "/hr/todo"));
        items.add(chart("dueSoon", "即将到期", orZero(summary.getDueSoonCount()),
                percent(summary.getDueSoonCount(), total), "amber", "/hr/todo"));
        items.add(chart("highPriority", "高优先级", orZero(summary.getHighPriorityCount()),
                percent(summary.getHighPriorityCount(), total), "orange", "/hr/todo"));
        items.add(chart("generatedOpen", "系统生成", orZero(summary.getGeneratedOpenCount()),
                percent(summary.getGeneratedOpenCount(), total), "cyan", "/hr/todo"));
        items.add(chart("done", "已完成", orZero(summary.getDoneCount()), percent(summary.getDoneCount(), total),
                "green", "/hr/todo"));
        return items;
    }

    private List<HrAnalyticsWorkbenchRespVO.ChartItem> buildPerformanceDistribution(EmployeePerformanceStatsRespVO stats) {
        if (stats.getGradeStats() != null && !stats.getGradeStats().isEmpty()) {
            return fromPerformanceStats(stats.getGradeStats(), "/hr/performance");
        }
        int total = Math.max(orZero(stats.getTotalCount()), 1);
        List<HrAnalyticsWorkbenchRespVO.ChartItem> items = new ArrayList<>();
        items.add(chart("excellent", "优秀", orZero(stats.getExcellentCount()), percent(stats.getExcellentCount(), total), "green", "/hr/performance"));
        items.add(chart("warning", "预警", orZero(stats.getWarningCount()), percent(stats.getWarningCount(), total), "red", "/hr/performance"));
        items.add(chart("recent", "近期评估", orZero(stats.getRecentCount()), percent(stats.getRecentCount(), total), "blue", "/hr/performance"));
        return items;
    }

    private List<HrAnalyticsWorkbenchRespVO.ChartItem> buildRiskCategories(HrRiskWorkbenchRespVO riskWorkbench) {
        if (riskWorkbench.getCategories() == null || riskWorkbench.getCategories().isEmpty()) {
            return Collections.emptyList();
        }
        int total = riskWorkbench.getCategories().stream()
                .map(HrRiskWorkbenchRespVO.RiskCategory::getCount)
                .mapToInt(this::orZero)
                .sum();
        int denominator = Math.max(total, 1);
        List<HrAnalyticsWorkbenchRespVO.ChartItem> items = new ArrayList<>();
        for (HrRiskWorkbenchRespVO.RiskCategory category : riskWorkbench.getCategories()) {
            items.add(chart(category.getCode(), category.getName(), orZero(category.getCount()),
                    percent(category.getCount(), denominator), toneBySeverity(category.getSeverity()),
                    category.getRoutePath()));
        }
        return items;
    }

    private List<HrAnalyticsWorkbenchRespVO.ChartItem> fromLifecycleStats(
            List<HrLifecycleWorkbenchRespVO.StatItem> stats, String group, String path) {
        if (stats == null || stats.isEmpty()) {
            return Collections.emptyList();
        }
        int total = stats.stream().map(HrLifecycleWorkbenchRespVO.StatItem::getCount)
                .mapToInt(this::orZero).sum();
        int denominator = Math.max(total, 1);
        List<HrAnalyticsWorkbenchRespVO.ChartItem> items = new ArrayList<>();
        int index = 0;
        for (HrLifecycleWorkbenchRespVO.StatItem item : stats) {
            Integer chartPercent = item.getPercent() == null ? percent(item.getCount(), denominator)
                    : rateValue(item.getPercent());
            items.add(chart(group + "-" + index, lifecycleStatName(group, item.getName()), orZero(item.getCount()),
                    chartPercent, toneByIndex(index), path));
            index++;
        }
        return items;
    }

    private List<HrAnalyticsWorkbenchRespVO.ChartItem> buildAttendanceExceptionStatus() {
        List<AttendanceExceptionMapper.StatusCount> statusCounts = attendanceExceptionMapper.selectStatusCounts();
        if (statusCounts == null || statusCounts.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Integer> countMap = new HashMap<>();
        for (AttendanceExceptionMapper.StatusCount statusCount : statusCounts) {
            String status = defaultText(statusCount.getStatus(), "未填写");
            countMap.put(status, countToInt(statusCount.getCount()));
        }
        int total = countMap.values().stream().mapToInt(Integer::intValue).sum();
        int denominator = Math.max(total, 1);
        List<HrAnalyticsWorkbenchRespVO.ChartItem> items = new ArrayList<>();
        Set<String> used = new HashSet<>();
        addAttendanceStatusItem(items, countMap, used, "PENDING", "待处理", "orange", denominator);
        addAttendanceStatusItem(items, countMap, used, "RESOLVED", "已处理", "green", denominator);
        addAttendanceStatusItem(items, countMap, used, "IGNORED", "已忽略", "blue", denominator);
        addAttendanceStatusItem(items, countMap, used, "REOPENED", "已重开", "red", denominator);
        for (Map.Entry<String, Integer> entry : countMap.entrySet()) {
            if (used.contains(entry.getKey())) {
                continue;
            }
            items.add(chart("attendance-status-" + items.size(), attendanceStatusName(entry.getKey()),
                    entry.getValue(), percent(entry.getValue(), denominator), toneByIndex(items.size()),
                    "/attendance/exceptions"));
        }
        return items;
    }

    private List<HrAnalyticsWorkbenchRespVO.DeptOperationalItem> buildDeptRiskTodoDistribution() {
        Map<Long, DeptAggregate> aggregateMap = new HashMap<>();
        mergeDeptHeadcounts(aggregateMap, employeeEntryMapper.selectDeptHeadcounts(
                WORK_STATUS_PROBATION, WORK_STATUS_ACTIVE));
        mergeDeptRiskCounts(aggregateMap, hrRiskEventMapper.selectActiveDeptRiskCounts(
                WORK_STATUS_PROBATION, WORK_STATUS_ACTIVE));
        mergeDeptTodoCounts(aggregateMap, hrTodoTaskMapper.selectDeptTodoCounts(TODO_OPEN,
                WORK_STATUS_PROBATION, WORK_STATUS_ACTIVE, LocalDateTime.now()));

        Map<Long, String> deptNames = loadDeptNames(aggregateMap.keySet());
        List<DeptAggregate> aggregates = new ArrayList<>(aggregateMap.values());
        for (DeptAggregate aggregate : aggregates) {
            aggregate.score = aggregate.highRiskCount * 5
                    + aggregate.mediumRiskCount * 3
                    + aggregate.overdueTodoCount * 3
                    + aggregate.openTodoCount
                    + aggregate.openRiskCount
                    + aggregate.headcount;
        }
        aggregates.sort(Comparator.comparingInt((DeptAggregate item) -> item.score).reversed()
                .thenComparing(Comparator.comparingInt((DeptAggregate item) -> item.headcount).reversed()));

        List<HrAnalyticsWorkbenchRespVO.DeptOperationalItem> result = new ArrayList<>();
        for (DeptAggregate aggregate : aggregates) {
            if (result.size() >= 8) {
                break;
            }
            result.add(toDeptOperationalItem(aggregate, deptNames));
        }
        return result;
    }

    private void mergeDeptHeadcounts(Map<Long, DeptAggregate> aggregateMap,
                                     List<EmployeeEntryMapper.DeptHeadcount> headcounts) {
        if (headcounts == null || headcounts.isEmpty()) {
            return;
        }
        for (EmployeeEntryMapper.DeptHeadcount headcount : headcounts) {
            DeptAggregate aggregate = aggregateMap.computeIfAbsent(normalizeDeptId(headcount.getDeptId()),
                    DeptAggregate::new);
            aggregate.headcount += countToInt(headcount.getHeadcount());
        }
    }

    private void mergeDeptRiskCounts(Map<Long, DeptAggregate> aggregateMap,
                                     List<HrRiskEventMapper.DeptRiskCount> riskCounts) {
        if (riskCounts == null || riskCounts.isEmpty()) {
            return;
        }
        for (HrRiskEventMapper.DeptRiskCount riskCount : riskCounts) {
            DeptAggregate aggregate = aggregateMap.computeIfAbsent(normalizeDeptId(riskCount.getDeptId()),
                    DeptAggregate::new);
            aggregate.openRiskCount += countToInt(riskCount.getOpenRiskCount());
            aggregate.highRiskCount += countToInt(riskCount.getHighRiskCount());
            aggregate.mediumRiskCount += countToInt(riskCount.getMediumRiskCount());
        }
    }

    private void mergeDeptTodoCounts(Map<Long, DeptAggregate> aggregateMap,
                                     List<HrTodoTaskMapper.DeptTodoCount> todoCounts) {
        if (todoCounts == null || todoCounts.isEmpty()) {
            return;
        }
        for (HrTodoTaskMapper.DeptTodoCount todoCount : todoCounts) {
            DeptAggregate aggregate = aggregateMap.computeIfAbsent(normalizeDeptId(todoCount.getDeptId()),
                    DeptAggregate::new);
            aggregate.openTodoCount += countToInt(todoCount.getOpenTodoCount());
            aggregate.overdueTodoCount += countToInt(todoCount.getOverdueTodoCount());
        }
    }

    private Long normalizeDeptId(Long deptId) {
        return deptId == null ? 0L : deptId;
    }

    private Map<Long, String> loadDeptNames(Set<Long> deptIds) {
        Set<Long> validDeptIds = new HashSet<>();
        for (Long deptId : deptIds) {
            if (deptId != null && deptId > 0) {
                validDeptIds.add(deptId);
            }
        }
        if (validDeptIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(validDeptIds);
            Map<Long, String> result = new HashMap<>();
            for (Map.Entry<Long, DeptRespDTO> entry : deptMap.entrySet()) {
                if (entry.getValue() != null) {
                    result.put(entry.getKey(), entry.getValue().getName());
                }
            }
            return result;
        } catch (Exception ex) {
            log.warn("Load HR analytics department names failed: {}", ex.getMessage());
            return Collections.emptyMap();
        }
    }

    private HrAnalyticsWorkbenchRespVO.DeptOperationalItem toDeptOperationalItem(DeptAggregate aggregate,
                                                                                 Map<Long, String> deptNames) {
        HrAnalyticsWorkbenchRespVO.DeptOperationalItem item = new HrAnalyticsWorkbenchRespVO.DeptOperationalItem();
        item.setDeptId(aggregate.deptId);
        item.setDeptName(resolveDeptName(aggregate.deptId, deptNames));
        item.setHeadcount(aggregate.headcount);
        item.setOpenTodoCount(aggregate.openTodoCount);
        item.setOverdueTodoCount(aggregate.overdueTodoCount);
        item.setHighRiskCount(aggregate.highRiskCount);
        item.setMediumRiskCount(aggregate.mediumRiskCount);
        item.setOpenRiskCount(aggregate.openRiskCount);
        item.setScore(aggregate.score);
        item.setTone(deptTone(aggregate));
        item.setPath(aggregate.deptId != null && aggregate.deptId > 0
                ? "/hr/risk?deptId=" + aggregate.deptId + "&includeChildren=true" : "/hr/risk");
        return item;
    }

    private String resolveDeptName(Long deptId, Map<Long, String> deptNames) {
        if (deptId == null || deptId <= 0) {
            return "未关联部门";
        }
        String deptName = deptNames.get(deptId);
        return deptName == null ? "未命名部门" : deptName;
    }

    private String deptTone(DeptAggregate aggregate) {
        if (aggregate.highRiskCount > 0 || aggregate.overdueTodoCount > 0) {
            return "red";
        }
        if (aggregate.mediumRiskCount > 0 || aggregate.openTodoCount > 0) {
            return "orange";
        }
        return "blue";
    }

    private List<HrAnalyticsWorkbenchRespVO.Insight> buildInsights(HrAnalyticsWorkbenchRespVO.Summary summary) {
        List<HrAnalyticsWorkbenchRespVO.Insight> insights = new ArrayList<>();
        if (summary.getHighRiskCount() > 0) {
            insights.add(insight("HIGH", "高风险事项需要处理",
                    "当前有 " + summary.getHighRiskCount() + " 项高风险，优先进入合规风险页闭环。",
                    "查看风险", "/hr/risk", "lucide:shield-alert"));
        }
        if (summary.getOverdueTodoCount() > 0) {
            insights.add(insight("HIGH", "HR 待办已逾期",
                    "统一待办中有 " + summary.getOverdueTodoCount() + " 项逾期，建议先清理负责人和截止时间。",
                    "处理待办", "/hr/todo", "lucide:clock-alert"));
        }
        if (summary.getRecruitmentOverdue() > 0) {
            insights.add(insight("MEDIUM", "候选人跟进逾期",
                    "招聘池中有 " + summary.getRecruitmentOverdue() + " 位候选人需要跟进。",
                    "打开招聘", "/hr/recruitment", "lucide:user-round-search"));
        }
        if (summary.getRecruitmentTouched() < summary.getRecruitmentTotal()) {
            insights.add(insight("LOW", "招聘触达未完全覆盖",
                    "当前候选记录 " + summary.getRecruitmentTotal() + " 条，已触达 " + summary.getRecruitmentTouched() + " 条。",
                    "补充触达", "/hr/recruitment", "lucide:send"));
        }
        if (summary.getPerformanceWarning() > 0) {
            insights.add(insight("MEDIUM", "绩效预警人群",
                    "最近绩效中有 " + summary.getPerformanceWarning() + " 人进入预警，应补齐辅导和改进记录。",
                    "查看绩效", "/hr/performance", "lucide:trophy"));
        }
        if (summary.getPendingAttendanceExceptions() > 0) {
            insights.add(insight("MEDIUM", "考勤异常待处理",
                    "考勤模块还有 " + summary.getPendingAttendanceExceptions() + " 条异常未处理。",
                    "查看考勤", "/hr/attendance", "lucide:calendar-clock"));
        }
        if (summary.getTrainingOverdue() > 0) {
            insights.add(insight("MEDIUM", "培训学习需跟进",
                    "当前有 " + summary.getTrainingOverdue() + " 条培训记录逾期或未完成，应补齐学习结果和复训计划。",
                    "查看培训", "/hr/training", "lucide:graduation-cap"));
        }
        if (summary.getMaterialPendingReview() > 0 || summary.getDocumentPending() > 0) {
            insights.add(insight("MEDIUM", "员工服务未闭环",
                    "材料待审 " + summary.getMaterialPendingReview() + " 条，证明待办 " + summary.getDocumentPending() + " 条。",
                    "处理员工服务", "/hr/document-request", "lucide:folder-check"));
        }
        if (summary.getMaterialExpiring() > 0) {
            insights.add(insight("LOW", "电子材料即将到期",
                    "未来 30 天有 " + summary.getMaterialExpiring() + " 份材料到期，建议提前提醒员工补充。",
                    "查看材料", "/hr/employee-material", "lucide:file-warning"));
        }
        if (summary.getPayrollIssueCount() > 0 || summary.getDraftPayrollBatches() > 0) {
            insights.add(insight("MEDIUM", "薪资批次未闭环",
                    "草稿批次 " + summary.getDraftPayrollBatches() + " 个，工资条异议 " + summary.getPayrollIssueCount() + " 条。",
                    "查看薪酬", "/hr/payroll", "lucide:wallet-cards"));
        }
        if (insights.isEmpty()) {
            insights.add(insight("LOW", "当前 HR 运营平稳",
                    "核心指标暂未发现明显阻塞，可以继续推进主数据质量和自动化待办。",
                    "查看主数据", "/hr/employee/master-data", "lucide:badge-check"));
        }
        return insights;
    }

    private List<HrAnalyticsWorkbenchRespVO.QuickAction> buildQuickActions() {
        List<HrAnalyticsWorkbenchRespVO.QuickAction> actions = new ArrayList<>();
        actions.add(action("花名册", "lucide:users-round", "/hr/employee", "员工主数据与组织画像"));
        actions.add(action("经理自助", "lucide:users-2", "/hr/manager-self-service", "团队、待办、风险与绩效"));
        actions.add(action("招聘", "lucide:user-plus", "/hr/recruitment", "候选人、面试、Offer、入职"));
        actions.add(action("培训", "lucide:graduation-cap", "/hr/training", "课程、计划、任务和评价"));
        actions.add(action("证明材料", "lucide:folder-check", "/hr/employee-material", "材料审核、证明开具和归档"));
        actions.add(action("提醒日历", "lucide:calendar-days", "/hr/calendar", "人事提醒和业务日历"));
        actions.add(action("薪酬社保", "lucide:wallet-cards", "/hr/payroll", "工资条、社保和个税"));
        actions.add(action("入转调离", "lucide:workflow", "/hr/lifecycle/workbench", "生命周期事件与任务"));
        actions.add(action("绩效", "lucide:trophy", "/hr/performance", "绩效记录与预警"));
        actions.add(action("待办", "lucide:list-checks", "/hr/todo", "统一待办闭环"));
        actions.add(action("风险", "lucide:shield-alert", "/hr/risk", "合规和数据质量风险"));
        return actions;
    }

    private List<HrAnalyticsWorkbenchRespVO.ChartItem> fromRecruitmentStats(
            List<EmployeeRecruitmentStatsRespVO.StatItem> stats, String path) {
        if (stats == null || stats.isEmpty()) {
            return Collections.emptyList();
        }
        int total = stats.stream().map(EmployeeRecruitmentStatsRespVO.StatItem::getCount)
                .mapToInt(this::orZero).sum();
        int denominator = Math.max(total, 1);
        List<HrAnalyticsWorkbenchRespVO.ChartItem> items = new ArrayList<>();
        int index = 0;
        for (EmployeeRecruitmentStatsRespVO.StatItem item : stats) {
            items.add(chart("recruitment-" + index, item.getName(), orZero(item.getCount()),
                    percent(item.getCount(), denominator), toneByIndex(index), path));
            index++;
        }
        return items;
    }

    private List<HrAnalyticsWorkbenchRespVO.ChartItem> fromPerformanceStats(
            List<EmployeePerformanceStatsRespVO.StatItem> stats, String path) {
        int total = stats.stream().map(EmployeePerformanceStatsRespVO.StatItem::getCount)
                .mapToInt(this::orZero).sum();
        int denominator = Math.max(total, 1);
        List<HrAnalyticsWorkbenchRespVO.ChartItem> items = new ArrayList<>();
        int index = 0;
        for (EmployeePerformanceStatsRespVO.StatItem item : stats) {
            items.add(chart("performance-" + index, item.getName(), orZero(item.getCount()),
                    percent(item.getCount(), denominator), toneByIndex(index), path));
            index++;
        }
        return items;
    }

    private Integer countPendingAttendanceExceptions() {
        return countToInt(attendanceExceptionMapper.selectCount(new LambdaQueryWrapperX<AttendanceExceptionDO>()
                .eq(AttendanceExceptionDO::getExceptionStatus, ATTENDANCE_PENDING)));
    }

    private Integer countDraftPayrollBatches() {
        return countToInt(payrollBatchMapper.selectCount(new LambdaQueryWrapperX<PayrollBatchDO>()
                .eq(PayrollBatchDO::getStatus, PAYROLL_DRAFT)));
    }

    private Integer countPayrollIssues() {
        return countToInt(payslipMapper.selectCount(new LambdaQueryWrapperX<PayslipDO>()
                .eq(PayslipDO::getStatus, PAYSLIP_ISSUE)));
    }

    private Integer countMaterialPendingReviews() {
        return countToInt(employeeMaterialMapper.selectCount(new LambdaQueryWrapperX<EmployeeMaterialDO>()
                .eq(EmployeeMaterialDO::getStatus, MATERIAL_PENDING_REVIEW)));
    }

    private Integer countMaterialExpiring() {
        LocalDate today = LocalDate.now();
        return countToInt(employeeMaterialMapper.selectCount(new LambdaQueryWrapperX<EmployeeMaterialDO>()
                .in(EmployeeMaterialDO::getStatus, MATERIAL_ACTIVE, MATERIAL_MISSING)
                .isNotNull(EmployeeMaterialDO::getExpireDate)
                .ge(EmployeeMaterialDO::getExpireDate, today)
                .le(EmployeeMaterialDO::getExpireDate, today.plusDays(30))));
    }

    private Integer countPendingDocumentRequests() {
        return countToInt(employeeDocumentRequestMapper.selectCount(new LambdaQueryWrapperX<EmployeeDocumentRequestDO>()
                .in(EmployeeDocumentRequestDO::getStatus, DOCUMENT_PENDING, DOCUMENT_PROCESSING)));
    }

    private Integer countUnreadReminders() {
        return countToInt(hrReminderRecordMapper.selectCount(new LambdaQueryWrapperX<HrReminderRecordDO>()
                .eq(HrReminderRecordDO::getStatus, REMINDER_UNREAD)));
    }

    private Integer countSocialSecurityAccounts() {
        return countToInt(socialSecurityAccountMapper.selectCount(new LambdaQueryWrapperX<SocialSecurityAccountDO>()));
    }

    private static class DeptAggregate {

        private final Long deptId;
        private int headcount;
        private int openTodoCount;
        private int overdueTodoCount;
        private int highRiskCount;
        private int mediumRiskCount;
        private int openRiskCount;
        private int score;

        private DeptAggregate(Long deptId) {
            this.deptId = deptId;
        }
    }

    private static class SnapshotCacheValue {

        private final Object value;
        private final long expiresAt;

        private SnapshotCacheValue(Object value, long expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }
    }

    private HrAnalyticsWorkbenchRespVO.MetricCard metric(String code, String title, Object value, String unit,
                                                         String subtitle, String icon, String tone, String path) {
        HrAnalyticsWorkbenchRespVO.MetricCard card = new HrAnalyticsWorkbenchRespVO.MetricCard();
        card.setCode(code);
        card.setTitle(title);
        card.setValue(String.valueOf(value));
        card.setUnit(unit);
        card.setSubtitle(subtitle);
        card.setIcon(icon);
        card.setTone(tone);
        card.setPath(path);
        return card;
    }

    private HrAnalyticsWorkbenchRespVO.ExecutiveHighlight executive(String code, String title, String value,
                                                                    String subtitle, String icon, String tone,
                                                                    String path) {
        HrAnalyticsWorkbenchRespVO.ExecutiveHighlight item = new HrAnalyticsWorkbenchRespVO.ExecutiveHighlight();
        item.setCode(code);
        item.setTitle(title);
        item.setValue(value);
        item.setSubtitle(subtitle);
        item.setIcon(icon);
        item.setTone(tone);
        item.setPath(path);
        return item;
    }

    private HrAnalyticsWorkbenchRespVO.ChartItem chart(String code, String name, Integer value, Integer percent,
                                                       String tone, String path) {
        HrAnalyticsWorkbenchRespVO.ChartItem item = new HrAnalyticsWorkbenchRespVO.ChartItem();
        item.setCode(code);
        item.setName(name);
        item.setValue(orZero(value));
        item.setPercent(orZero(percent));
        item.setTone(tone);
        item.setPath(path);
        return item;
    }

    private HrAnalyticsWorkbenchRespVO.ChartItem rateChart(String code, String name, BigDecimal rate, String tone) {
        return rateChart(code, name, rate, tone, "/hr/recruitment");
    }

    private HrAnalyticsWorkbenchRespVO.ChartItem rateChart(String code, String name, BigDecimal rate, String tone,
                                                           String path) {
        int value = rateValue(rate);
        return chart(code, name, value, value, tone, path);
    }

    private int amountToTenThousand(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        int value = amount.divide(BigDecimal.valueOf(10000), 0, RoundingMode.HALF_UP).intValue();
        return Math.max(value, 1);
    }

    private int amountToThousand(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        int value = amount.divide(BigDecimal.valueOf(1000), 0, RoundingMode.HALF_UP).intValue();
        return Math.max(value, 1);
    }

    private int rateValue(BigDecimal rate) {
        return rate == null ? 0 : rate.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private HrAnalyticsWorkbenchRespVO.Insight insight(String severity, String title, String description,
                                                       String action, String path, String icon) {
        HrAnalyticsWorkbenchRespVO.Insight item = new HrAnalyticsWorkbenchRespVO.Insight();
        item.setSeverity(severity);
        item.setTitle(title);
        item.setDescription(description);
        item.setAction(action);
        item.setPath(path);
        item.setIcon(icon);
        return item;
    }

    private HrAnalyticsWorkbenchRespVO.QuickAction action(String title, String icon, String path, String description) {
        HrAnalyticsWorkbenchRespVO.QuickAction action = new HrAnalyticsWorkbenchRespVO.QuickAction();
        action.setTitle(title);
        action.setIcon(icon);
        action.setPath(path);
        action.setDescription(description);
        return action;
    }

    private <T> T safe(String name, Supplier<T> supplier, T fallback) {
        try {
            T value = supplier.get();
            return value == null ? fallback : value;
        } catch (Exception ex) {
            log.warn("Load HR analytics {} failed: {}", name, ex.getMessage());
            return fallback;
        }
    }

    private <T> List<T> list(List<T> source) {
        return source == null ? new ArrayList<>() : new ArrayList<>(source);
    }

    private Integer percent(Integer value, Integer total) {
        if (value == null || total == null || total <= 0) {
            return 0;
        }
        return BigDecimal.valueOf(value)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private Integer countToInt(Long value) {
        if (value == null) {
            return 0;
        }
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : value.intValue();
    }

    private Integer orZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String formatDecimal(BigDecimal value) {
        if (value == null) {
            return "0.0";
        }
        return value.setScale(1, RoundingMode.HALF_UP).toPlainString();
    }

    private void addAttendanceStatusItem(List<HrAnalyticsWorkbenchRespVO.ChartItem> items, Map<String, Integer> countMap,
                                         Set<String> used, String status, String name, String tone, int denominator) {
        Integer count = countMap.get(status);
        if (count == null) {
            return;
        }
        used.add(status);
        items.add(chart("attendance-status-" + status.toLowerCase(), name, count, percent(count, denominator), tone,
                "/attendance/exceptions"));
    }

    private String lifecycleStatName(String group, String name) {
        String value = defaultText(name, "未填写");
        if ("event-type".equals(group)) {
            if ("ONBOARDING_CONFIRMED".equals(value)) {
                return "确认入职";
            }
            if ("ONBOARDING_CREATED".equals(value)) {
                return "创建入职";
            }
            if ("PROBATION_STARTED".equals(value)) {
                return "进入试用";
            }
            if ("REGULARIZATION_REQUESTED".equals(value)) {
                return "发起转正";
            }
            if ("REGULARIZATION_APPROVED".equals(value)) {
                return "转正通过";
            }
            if ("TRANSFER_REQUESTED".equals(value)) {
                return "发起调岗";
            }
            if ("TRANSFER_EFFECTIVE".equals(value)) {
                return "调岗生效";
            }
            if ("SALARY_ADJUST_REQUESTED".equals(value)) {
                return "发起调薪";
            }
            if ("SALARY_ADJUST_EFFECTIVE".equals(value)) {
                return "调薪生效";
            }
            if ("RESIGN_REQUESTED".equals(value)) {
                return "发起离职";
            }
            if ("RESIGN_EFFECTIVE".equals(value)) {
                return "离职生效";
            }
            if ("REHIRE_CREATED".equals(value)) {
                return "发起返聘";
            }
        }
        if ("event-status".equals(group)) {
            if ("PENDING_APPROVAL".equals(value)) {
                return "待审批";
            }
            if ("PENDING_HANDOVER".equals(value)) {
                return "待交接";
            }
            if ("PENDING_EFFECTIVE".equals(value)) {
                return "待生效";
            }
            if ("COMPLETED".equals(value)) {
                return "已完成";
            }
            if ("CANCELLED".equals(value)) {
                return "已撤销";
            }
            if ("REJECTED".equals(value)) {
                return "已拒绝";
            }
        }
        if ("task-status".equals(group)) {
            if ("DONE".equals(value)) {
                return "已完成";
            }
            if ("PENDING".equals(value)) {
                return "待处理";
            }
        }
        return value;
    }

    private String attendanceStatusName(String status) {
        String value = defaultText(status, "未填写");
        if ("PENDING".equals(value)) {
            return "待处理";
        }
        if ("RESOLVED".equals(value)) {
            return "已处理";
        }
        if ("IGNORED".equals(value)) {
            return "已忽略";
        }
        if ("REOPENED".equals(value)) {
            return "已重开";
        }
        return value;
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private String toneBySeverity(String severity) {
        if ("HIGH".equals(severity)) {
            return "red";
        }
        if ("MEDIUM".equals(severity)) {
            return "orange";
        }
        if ("LOW".equals(severity)) {
            return "green";
        }
        return "blue";
    }

    private String stabilityTone(Integer stabilityIndex) {
        int value = orZero(stabilityIndex);
        if (value >= 90) {
            return "green";
        }
        if (value >= 80) {
            return "amber";
        }
        return "red";
    }

    private String pressureTone(Integer pressure) {
        int value = orZero(pressure);
        if (value > 0) {
            return value >= 10 ? "red" : "orange";
        }
        return "green";
    }

    private String toneByIndex(int index) {
        String[] tones = {"blue", "cyan", "green", "amber", "orange", "red"};
        return tones[index % tones.length];
    }
}
