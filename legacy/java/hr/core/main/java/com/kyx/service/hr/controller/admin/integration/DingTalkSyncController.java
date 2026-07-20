package com.kyx.service.hr.controller.admin.integration;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.json.JsonUtils;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.bpm.api.task.BpmProcessInstanceApi;
import com.kyx.service.bpm.api.task.dto.BpmCopyNoticeResendRespDTO;
import com.kyx.service.hr.config.DingTalkProperties;
import com.kyx.service.hr.controller.admin.integration.vo.DingTalkSyncHistoryPageReqVO;
import com.kyx.service.hr.controller.admin.integration.vo.DingTalkSyncHistoryRespVO;
import com.kyx.service.hr.controller.admin.integration.vo.DingTalkSystemUpdateNoticeReqVO;
import com.kyx.service.hr.dal.dataobject.integration.DingTalkSyncHistoryDO;
import com.kyx.service.hr.integration.dingtalk.job.DingTalkSyncScheduleJob;
import com.kyx.service.hr.integration.dingtalk.service.DingTalkAttendanceSyncService;
import com.kyx.service.hr.integration.dingtalk.service.DingTalkEmployeeSyncService;
import com.kyx.service.hr.integration.dingtalk.service.DingTalkLeaveSyncService;
import com.kyx.service.hr.integration.dingtalk.service.DingTalkMessageNotifyService;
import com.kyx.service.hr.integration.dingtalk.service.DingTalkRequirementNoticeConfigService;
import com.kyx.service.hr.integration.dingtalk.service.DingTalkRequirementNoticeService;
import com.kyx.service.hr.integration.dingtalk.service.DingTalkSyncConfigService;
import com.kyx.service.hr.integration.dingtalk.service.DingTalkSyncHistoryService;
import com.kyx.service.hr.integration.dingtalk.service.DingTalkSyncSnapshotService;
import com.kyx.service.hr.integration.dingtalk.service.DingTalkSystemUpdateNoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.time.LocalDate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.kyx.foundation.common.exception.enums.GlobalErrorCodeConstants.FORBIDDEN;
import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.foundation.common.pojo.CommonResult.success;
import static com.kyx.foundation.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 钉钉同步")
@RestController
@RequestMapping("/hr/integration/dingtalk")
@Validated
@Slf4j
public class DingTalkSyncController {

    private static final String SCOPE_USER_PROFILE = "USER_PROFILE";
    private static final String SCOPE_ATTENDANCE = "ATTENDANCE";
    private static final String SCOPE_LEAVE = "LEAVE";
    private static final String SCOPE_ALL = "ALL";

    private static final String TRIGGER_MANUAL = "MANUAL";

    private static final String TYPE_EMPLOYEE_SYNC = "EMPLOYEE_SYNC";
    private static final String TYPE_EMPLOYEE_PREVIEW = "EMPLOYEE_PREVIEW";
    private static final String TYPE_EMPLOYEE_IMPORT = "EMPLOYEE_IMPORT";
    private static final String TYPE_ROSTER_SYNC = "ROSTER_SYNC";
    private static final String TYPE_ATTENDANCE_SYNC = "ATTENDANCE_SYNC";
    private static final String TYPE_ATTENDANCE_PREVIEW = "ATTENDANCE_PREVIEW";
    private static final String TYPE_LEAVE_SYNC = "LEAVE_SYNC";
    private static final String TYPE_FULL_SYNC = "FULL_SYNC";

    @Resource
    private DingTalkEmployeeSyncService dingTalkEmployeeSyncService;
    @Resource
    private DingTalkAttendanceSyncService dingTalkAttendanceSyncService;
    @Resource
    private DingTalkLeaveSyncService dingTalkLeaveSyncService;
    @Resource
    private DingTalkProperties dingTalkProperties;
    @Resource
    private DingTalkSyncConfigService dingTalkSyncConfigService;
    @Resource
    private DingTalkSyncScheduleJob dingTalkSyncScheduleJob;
    @Resource
    private DingTalkSyncHistoryService dingTalkSyncHistoryService;
    @Resource
    private DingTalkSyncSnapshotService dingTalkSyncSnapshotService;
    @Resource
    private DingTalkMessageNotifyService dingTalkMessageNotifyService;
    @Resource
    private DingTalkRequirementNoticeConfigService dingTalkRequirementNoticeConfigService;
    @Resource
    private DingTalkRequirementNoticeService dingTalkRequirementNoticeService;
    @Resource
    private DingTalkSystemUpdateNoticeService dingTalkSystemUpdateNoticeService;
    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    @PostMapping("/employees/sync")
    @Operation(summary = "同步钉钉用户到 OA（支持自动创建 OA 用户与 HR 档案）")
    @PreAuthorize("@ss.hasPermission('hr:employee:create')")
    public CommonResult<DingTalkEmployeeSyncService.EmployeeSyncReport> syncEmployees(
            @RequestParam(value = "targetTenantId", required = false) Long targetTenantId,
            @RequestParam(value = "autoCreateDeptId", required = false) Long autoCreateDeptId,
            @RequestParam(value = "autoCreateMissing", required = false) Boolean autoCreateMissing) {
        LocalDateTime startTime = LocalDateTime.now();
        Long tenantId = resolveTargetTenantId(targetTenantId);
        Long operatorUserId = getLoginUserId();
        DingTalkEmployeeSyncService.EmployeeSyncReport report = TenantUtils.execute(tenantId,
                () -> dingTalkEmployeeSyncService.syncExistingProfiles(autoCreateDeptId, autoCreateMissing));
        LocalDateTime endTime = LocalDateTime.now();
        saveHistory(buildEmployeeSyncHistoryReq(tenantId, operatorUserId, autoCreateDeptId, autoCreateMissing,
                startTime, endTime, report));
        log.info("DingTalk employee sync API done, tenantId={}, total={}, mobileMatched={}, nameMatched={}, updated={}, unchanged={}, created={}, createFailed={}, unmatched={}, skippedNoIdentity={}",
                tenantId, report.getTotalFromDingTalk(), report.getMatchedByMobile(), report.getMatchedByName(),
                report.getUpdated(), report.getUnchanged(), report.getCreated(), report.getCreateFailed(),
                report.getUnmatched(), report.getSkippedNoMobile());
        return success(report);
    }

    @PostMapping("/roster/sync")
    @Operation(summary = "同步钉钉花名册字段到已有 OA 员工档案")
    @PreAuthorize("@ss.hasPermission('hr:employee:update')")
    public CommonResult<DingTalkEmployeeSyncService.EmployeeSyncReport> syncRoster(
            @RequestParam(value = "targetTenantId", required = false) Long targetTenantId) {
        LocalDateTime startTime = LocalDateTime.now();
        Long tenantId = resolveTargetTenantId(targetTenantId);
        Long operatorUserId = getLoginUserId();
        DingTalkEmployeeSyncService.EmployeeSyncReport report = TenantUtils.execute(tenantId,
                dingTalkEmployeeSyncService::syncRosterForActiveBindings);
        LocalDateTime endTime = LocalDateTime.now();
        saveHistory(buildRosterSyncHistoryReq(tenantId, operatorUserId, startTime, endTime, report));
        log.info("DingTalk roster sync API done, tenantId={}, total={}, pulled={}, synced={}, profileUpdates={}, customFields={}, failed={}, unmatched={}",
                tenantId, report.getTotalFromDingTalk(), report.getRosterPulledUsers(),
                report.getRosterSyncedProfiles(), report.getRosterUpdatedProfiles(),
                report.getRosterCustomFields(), report.getRosterSyncFailed(), report.getUnmatched());
        return success(report);
    }

    @PostMapping("/employees/rollback")
    @Operation(summary = "Rollback DingTalk employee sync by snapshot batch id")
    @PreAuthorize("@ss.hasPermission('hr:employee:update')")
    public CommonResult<DingTalkSyncSnapshotService.RollbackReport> rollbackEmployeeSync(
            @RequestParam(value = "targetTenantId", required = false) Long targetTenantId,
            @RequestParam("snapshotBatchId") String snapshotBatchId) {
        Long tenantId = resolveTargetTenantId(targetTenantId);
        DingTalkSyncSnapshotService.RollbackReport report = TenantUtils.execute(tenantId,
                () -> dingTalkSyncSnapshotService.rollbackEmployeeBatch(snapshotBatchId));
        return success(report);
    }

    @PostMapping("/employees/preview")
    @Operation(summary = "预览钉钉用户同步结果（不落库）")
    @PreAuthorize("@ss.hasPermission('hr:employee:query')")
    public CommonResult<DingTalkEmployeeSyncService.EmployeeSyncPreview> previewEmployees(
            @RequestParam(value = "targetTenantId", required = false) Long targetTenantId,
            @RequestParam(value = "autoCreateDeptId", required = false) Long autoCreateDeptId,
            @RequestParam(value = "autoCreateMissing", required = false) Boolean autoCreateMissing) {
        LocalDateTime startTime = LocalDateTime.now();
        Long tenantId = resolveTargetTenantId(targetTenantId);
        Long operatorUserId = getLoginUserId();
        DingTalkEmployeeSyncService.EmployeeSyncPreview preview = TenantUtils.execute(tenantId,
                () -> dingTalkEmployeeSyncService.previewSync(autoCreateDeptId, autoCreateMissing));
        LocalDateTime endTime = LocalDateTime.now();
        saveHistory(buildEmployeePreviewHistoryReq(tenantId, operatorUserId, autoCreateDeptId, autoCreateMissing,
                startTime, endTime, preview));
        return success(preview);
    }

    @PostMapping("/employees/import-unmatched")
    @Operation(summary = "导入未匹配钉钉用户到 OA（初始化系统账号和员工档案）")
    @PreAuthorize("@ss.hasPermission('hr:employee:create')")
    public CommonResult<DingTalkEmployeeSyncService.EmployeeImportReport> importUnmatchedEmployees(
            @RequestParam(value = "targetTenantId", required = false) Long targetTenantId,
            @RequestParam(value = "deptId") Long deptId) {
        LocalDateTime startTime = LocalDateTime.now();
        Long tenantId = resolveTargetTenantId(targetTenantId);
        Long operatorUserId = getLoginUserId();
        DingTalkEmployeeSyncService.EmployeeImportReport report = TenantUtils.execute(tenantId,
                () -> dingTalkEmployeeSyncService.importUnmatchedProfiles(deptId));
        LocalDateTime endTime = LocalDateTime.now();
        saveHistory(buildEmployeeImportHistoryReq(tenantId, operatorUserId, deptId, startTime, endTime, report));
        log.info("DingTalk unmatched employee import API done, tenantId={}, deptId={}, total={}, imported={}, exists={}, invalid={}, failed={}",
                tenantId, deptId, report.getTotalFromDingTalk(),
                report.getImported(), report.getSkippedExists(), report.getSkippedInvalid(), report.getFailed());
        return success(report);
    }

    @PostMapping("/attendance/sync")
    @Operation(summary = "同步钉钉考勤到 OA")
    @PreAuthorize("@ss.hasPermission('attendance:clock:sync')")
    public CommonResult<DingTalkAttendanceSyncService.AttendanceSyncReport> syncAttendance(
            @RequestParam(value = "targetTenantId", required = false) Long targetTenantId,
            @RequestParam(value = "lookbackMinutes", required = false, defaultValue = "30") Long lookbackMinutes) {
        LocalDateTime startTime = LocalDateTime.now();
        long minutes = lookbackMinutes == null ? 30 : Math.max(lookbackMinutes, 1L);
        LocalDateTime to = LocalDateTime.now();
        LocalDateTime from = to.minusMinutes(minutes);
        Long tenantId = resolveTargetTenantId(targetTenantId);
        Long operatorUserId = getLoginUserId();
        DingTalkAttendanceSyncService.AttendanceSyncReport report = TenantUtils.execute(tenantId,
                () -> dingTalkAttendanceSyncService.syncByTimeRange(from, to));
        LocalDateTime endTime = LocalDateTime.now();
        saveHistory(buildAttendanceSyncHistoryReq(tenantId, operatorUserId, minutes, startTime, endTime, report));
        return success(report);
    }

    @PostMapping("/leave/sync")
    @Operation(summary = "同步钉钉请假到 OA")
    @PreAuthorize("@ss.hasPermission('attendance:clock:sync')")
    public CommonResult<DingTalkLeaveSyncService.LeaveSyncReport> syncLeave(
            @RequestParam(value = "targetTenantId", required = false) Long targetTenantId,
            @RequestParam(value = "lookbackDays", required = false) Integer lookbackDays,
            @RequestParam(value = "forwardDays", required = false) Integer forwardDays) {
        LocalDateTime startTime = LocalDateTime.now();
        int lookback = lookbackDays == null ? resolveDefaultLeaveLookbackDays() : Math.max(lookbackDays, 1);
        int forward = forwardDays == null ? resolveDefaultLeaveForwardDays() : Math.max(forwardDays, 0);
        LocalDate baseDate = LocalDate.now().minusDays(1);
        Long tenantId = resolveTargetTenantId(targetTenantId);
        Long operatorUserId = getLoginUserId();
        LocalDate fromDate = baseDate.minusDays(Math.max(lookback - 1, 0));
        LocalDate toDate = baseDate.plusDays(forward);
        DingTalkLeaveSyncService.LeaveSyncReport report = TenantUtils.execute(tenantId,
                () -> dingTalkLeaveSyncService.syncByDateRange(fromDate, toDate));
        LocalDateTime endTime = LocalDateTime.now();
        saveHistory(buildLeaveSyncHistoryReq(tenantId, operatorUserId, lookback, forward, startTime, endTime, report));
        return success(report);
    }

    private int resolveDefaultLeaveLookbackDays() {
        Integer value = dingTalkProperties.getSync() == null ? null : dingTalkProperties.getSync().getLeaveLookbackDays();
        return value == null ? 1 : Math.max(value, 1);
    }

    private int resolveDefaultLeaveForwardDays() {
        Integer value = dingTalkProperties.getSync() == null ? null : dingTalkProperties.getSync().getLeaveForwardDays();
        return value == null ? 0 : Math.max(value, 0);
    }

    @PostMapping("/attendance/preview")
    @Operation(summary = "预览钉钉考勤同步数据（不入库）")
    @PreAuthorize("@ss.hasPermission('attendance:clock:sync')")
    public CommonResult<DingTalkAttendanceSyncService.AttendanceSyncPreview> previewAttendance(
            @RequestParam(value = "targetTenantId", required = false) Long targetTenantId,
            @RequestParam(value = "lookbackMinutes", required = false, defaultValue = "30") Long lookbackMinutes,
            @RequestParam(value = "sampleLimit", required = false, defaultValue = "20") Integer sampleLimit) {
        LocalDateTime startTime = LocalDateTime.now();
        long minutes = lookbackMinutes == null ? 30 : Math.max(lookbackMinutes, 1L);
        LocalDateTime to = LocalDateTime.now();
        LocalDateTime from = to.minusMinutes(minutes);
        Long tenantId = resolveTargetTenantId(targetTenantId);
        Long operatorUserId = getLoginUserId();
        DingTalkAttendanceSyncService.AttendanceSyncPreview preview = TenantUtils.execute(tenantId,
                () -> dingTalkAttendanceSyncService.previewByTimeRange(from, to, sampleLimit));
        LocalDateTime endTime = LocalDateTime.now();
        saveHistory(buildAttendancePreviewHistoryReq(tenantId, operatorUserId, minutes, sampleLimit, startTime, endTime, preview));
        return success(preview);
    }

    @PostMapping("/full/sync")
    @Operation(summary = "执行每日全量逻辑（用户+考勤）")
    @PreAuthorize("@ss.hasPermission('hr:employee:create')")
    public CommonResult<FullSyncReport> fullSync(
            @RequestParam(value = "targetTenantId", required = false) Long targetTenantId) {
        LocalDateTime startTime = LocalDateTime.now();
        Long tenantId = resolveTargetTenantId(targetTenantId);
        Long operatorUserId = getLoginUserId();
        FullSyncReport report = new FullSyncReport();
        report.setResult(TenantUtils.execute(tenantId, dingTalkSyncScheduleJob::executeDailySync));
        LocalDateTime endTime = LocalDateTime.now();
        saveHistory(buildFullSyncHistoryReq(tenantId, operatorUserId, startTime, endTime, report.getResult()));
        return success(report);
    }

    @PostMapping("/message/test")
    @Operation(summary = "发送钉钉测试消息")
    @PreAuthorize("@ss.hasPermission('hr:employee:query')")
    public CommonResult<DingTalkMessageNotifyService.TextSendResult> sendTestMessage(
            @RequestParam(value = "targetTenantId", required = false) Long targetTenantId,
            @RequestParam(value = "name") String name,
            @RequestParam(value = "content", required = false) String content) {
        Long tenantId = resolveTargetTenantId(targetTenantId);
        DingTalkMessageNotifyService.TextSendResult result = TenantUtils.execute(tenantId,
                () -> dingTalkMessageNotifyService.sendTextToDingUserName(name, content));
        return success(result);
    }

    @PostMapping("/system-update-notice/preview")
    @Operation(summary = "预览系统更新钉钉卡片通知接收人")
    @PreAuthorize("@ss.hasPermission('hr:employee:query')")
    public CommonResult<DingTalkSystemUpdateNoticeService.SystemUpdateNoticePreview> previewSystemUpdateNotice(
            @RequestParam(value = "targetTenantId", required = false) Long targetTenantId,
            @Valid @RequestBody(required = false) DingTalkSystemUpdateNoticeReqVO reqVO) {
        Long tenantId = resolveTargetTenantId(targetTenantId);
        return success(TenantUtils.execute(tenantId,
                () -> dingTalkSystemUpdateNoticeService.previewRecipients(reqVO)));
    }

    @PostMapping("/system-update-notice/send")
    @Operation(summary = "发送系统更新钉钉卡片通知")
    @PreAuthorize("@ss.hasPermission('hr:employee:create')")
    public CommonResult<DingTalkSystemUpdateNoticeService.SystemUpdateNoticeSendReport> sendSystemUpdateNotice(
            @RequestParam(value = "targetTenantId", required = false) Long targetTenantId,
            @Valid @RequestBody(required = false) DingTalkSystemUpdateNoticeReqVO reqVO) {
        Long tenantId = resolveTargetTenantId(targetTenantId);
        return success(TenantUtils.execute(tenantId,
                () -> dingTalkSystemUpdateNoticeService.send(reqVO)));
    }

    @GetMapping("/system-update-notice/available-users")
    @Operation(summary = "获取可接收系统更新钉钉卡片通知的人员")
    @PreAuthorize("@ss.hasPermission('hr:employee:query')")
    public CommonResult<List<DingTalkSystemUpdateNoticeService.Recipient>> getSystemUpdateNoticeAvailableUsers(
            @RequestParam(value = "targetTenantId", required = false) Long targetTenantId) {
        Long tenantId = resolveTargetTenantId(targetTenantId);
        return success(TenantUtils.execute(tenantId,
                dingTalkSystemUpdateNoticeService::listAvailableRecipients));
    }

    @GetMapping("/requirement-notice/config")
    @Operation(summary = "获取需求钉钉通知配置")
    @PreAuthorize("@ss.hasPermission('hr:employee:query')")
    public CommonResult<RequirementNoticeConfigResp> getRequirementNoticeConfig() {
        RequirementNoticeConfigResp resp = new RequirementNoticeConfigResp();
        resp.setEnabled(dingTalkRequirementNoticeConfigService.isEnabled());
        resp.setScenes(dingTalkRequirementNoticeConfigService.getSceneConfigs());
        resp.setStats(dingTalkRequirementNoticeService.getStats());
        return success(resp);
    }

    @GetMapping("/sync/config")
    @Operation(summary = "获取钉钉同步配置")
    @PreAuthorize("@ss.hasPermission('hr:employee:query')")
    public CommonResult<DingTalkSyncConfigService.SyncConfig> getSyncConfig() {
        return success(dingTalkSyncConfigService.getConfig());
    }

    @PostMapping("/sync/config/attendance")
    @Operation(summary = "更新钉钉考勤同步开关")
    @PreAuthorize("@ss.hasPermission('hr:employee:create')")
    public CommonResult<DingTalkSyncConfigService.SyncConfig> updateAttendanceSyncConfig(
            @RequestParam("enabled") Boolean enabled) {
        dingTalkSyncConfigService.setAttendanceEnabled(Boolean.TRUE.equals(enabled));
        return success(dingTalkSyncConfigService.getConfig());
    }

    @PostMapping("/sync/config/leave")
    @Operation(summary = "更新钉钉请假同步开关")
    @PreAuthorize("@ss.hasPermission('hr:employee:create')")
    public CommonResult<DingTalkSyncConfigService.SyncConfig> updateLeaveSyncConfig(
            @RequestParam("enabled") Boolean enabled) {
        dingTalkSyncConfigService.setLeaveEnabled(Boolean.TRUE.equals(enabled));
        return success(dingTalkSyncConfigService.getConfig());
    }

    @PostMapping("/requirement-notice/config")
    @Operation(summary = "更新需求钉钉通知配置")
    @PreAuthorize("@ss.hasPermission('hr:employee:create')")
    public CommonResult<RequirementNoticeConfigResp> updateRequirementNoticeConfig(
            @RequestParam("enabled") Boolean enabled) {
        RequirementNoticeConfigResp resp = new RequirementNoticeConfigResp();
        resp.setEnabled(dingTalkRequirementNoticeConfigService.setEnabled(Boolean.TRUE.equals(enabled)));
        resp.setScenes(dingTalkRequirementNoticeConfigService.getSceneConfigs());
        resp.setStats(dingTalkRequirementNoticeService.getStats());
        return success(resp);
    }

    @PostMapping("/requirement-notice/scene-config")
    @Operation(summary = "更新需求钉钉通知场景配置")
    @PreAuthorize("@ss.hasPermission('hr:employee:create')")
    public CommonResult<RequirementNoticeConfigResp> updateRequirementNoticeSceneConfig(
            @RequestParam("scene") String scene,
            @RequestParam("enabled") Boolean enabled) {
        dingTalkRequirementNoticeConfigService.setSceneEnabled(scene, Boolean.TRUE.equals(enabled));
        RequirementNoticeConfigResp resp = new RequirementNoticeConfigResp();
        resp.setEnabled(dingTalkRequirementNoticeConfigService.isEnabled());
        resp.setScenes(dingTalkRequirementNoticeConfigService.getSceneConfigs());
        resp.setStats(dingTalkRequirementNoticeService.getStats());
        return success(resp);
    }

    @PostMapping("/requirement-notice/bpm-copy/resend-running")
    @Operation(summary = "补发运行中 BPM 知会钉钉通知")
    @PreAuthorize("@ss.hasPermission('hr:employee:create')")
    public CommonResult<BpmCopyNoticeResendRespDTO> resendRunningBpmCopyNotice(
            @RequestParam(value = "limit", required = false, defaultValue = "200") Integer limit,
            @RequestParam(value = "dryRun", required = false, defaultValue = "false") Boolean dryRun) {
        return success(processInstanceApi.resendRunningCopyNotices(limit, dryRun).getCheckedData());
    }

    @GetMapping("/history/page")
    @Operation(summary = "获取钉钉同步历史分页")
    @PreAuthorize("@ss.hasPermission('hr:employee:query')")
    public CommonResult<PageResult<DingTalkSyncHistoryRespVO>> getSyncHistoryPage(@Valid DingTalkSyncHistoryPageReqVO pageReqVO) {
        PageResult<DingTalkSyncHistoryDO> pageResult = dingTalkSyncHistoryService.getPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, DingTalkSyncHistoryRespVO.class));
    }

    private DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq buildEmployeeSyncHistoryReq(Long tenantId,
                                                                                               Long operatorUserId,
                                                                                               Long autoCreateDeptId,
                                                                                               Boolean autoCreateMissing,
                                                                                               LocalDateTime startTime,
                                                                                               LocalDateTime endTime,
                                                                                               DingTalkEmployeeSyncService.EmployeeSyncReport report) {
        DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq req = baseHistoryReq(
                TYPE_EMPLOYEE_SYNC, SCOPE_USER_PROFILE, TRIGGER_MANUAL, tenantId, operatorUserId, startTime, endTime);
        req.setAutoCreateEnabled(Boolean.TRUE.equals(autoCreateMissing));
        req.setAutoCreateDeptId(autoCreateDeptId);
        req.setTotalCount(report.getTotalFromDingTalk());
        req.setPulledCount(report.getTotalFromDingTalk());
        req.setSyncedCount(report.getCreated() + report.getUpdated() + report.getRosterSyncedProfiles());
        req.setCreatedCount(report.getCreated());
        req.setUpdatedCount(report.getUpdated() + report.getRosterSyncedProfiles());
        req.setFailedCount(report.getCreateFailed() + report.getRosterSyncFailed());
        req.setSkippedCount(report.getSkippedCreateInvalid() + report.getSkippedDuplicateMapping()
                + report.getSkippedNoMobile() + report.getUnmatched());
        req.setSummary(String.format("created=%d, updated=%d, unmatched=%d, rosterPulled=%d, rosterSynced=%d, rosterProfileUpdates=%d, rosterCustomFields=%d, rosterFailed=%d, bindings=%d, deptTreeSynced=%d, deptTreeCreated=%d, deptTreeUpdated=%d, deptTreeFailed=%d, deptUpdates=%d, snapshots=%d, cleanedBindings=%d, disabledUsers=%d",
                report.getCreated(), report.getUpdated(), report.getUnmatched(),
                report.getRosterPulledUsers(), report.getRosterSyncedProfiles(), report.getRosterUpdatedProfiles(),
                report.getRosterCustomFields(), report.getRosterSyncFailed(), report.getSyncedBindings(),
                report.getDeptTreeSynced(), report.getDeptTreeCreated(), report.getDeptTreeUpdated(),
                report.getDeptTreeFailed(), report.getSyncedDeptIds(), report.getSnapshotRows(),
                report.getCleanedBindings(), report.getDisabledAdminUsers()));
        req.setDetailJson(buildDetailJson(
                "snapshotBatchId", report.getSnapshotBatchId(),
                "snapshotRows", report.getSnapshotRows(),
                "matchedByMobile", report.getMatchedByMobile(),
                "matchedByName", report.getMatchedByName(),
                "unchanged", report.getUnchanged(),
                "deptTreePulled", report.getDeptTreePulled(),
                "deptTreeSynced", report.getDeptTreeSynced(),
                "deptTreeCreated", report.getDeptTreeCreated(),
                "deptTreeUpdated", report.getDeptTreeUpdated(),
                "deptTreeFailed", report.getDeptTreeFailed(),
                "stagedToUserSync", report.getStagedToUserSync(),
                "skippedNoExternalUserId", report.getSkippedNoExternalUserId(),
                "syncedUserSyncRows", report.getSyncedUserSyncRows(),
                "skippedNoMobile", report.getSkippedNoMobile(),
                "skippedCreateInvalid", report.getSkippedCreateInvalid(),
                "skippedDuplicateMapping", report.getSkippedDuplicateMapping(),
                "syncedBindings", report.getSyncedBindings(),
                "syncedEntryStatuses", report.getSyncedEntryStatuses(),
                "syncedDeptIds", report.getSyncedDeptIds(),
                "rosterEnabled", report.isRosterEnabled(),
                "rosterPulledUsers", report.getRosterPulledUsers(),
                "rosterSyncedProfiles", report.getRosterSyncedProfiles(),
                "rosterUpdatedProfiles", report.getRosterUpdatedProfiles(),
                "rosterCustomFields", report.getRosterCustomFields(),
                "rosterSyncFailed", report.getRosterSyncFailed(),
                "enabledAdminUsers", report.getEnabledAdminUsers(),
                "disabledAdminUsers", report.getDisabledAdminUsers(),
                "cleanedProfiles", report.getCleanedProfiles(),
                "cleanedBindings", report.getCleanedBindings(),
                "cleanedUserSyncRows", report.getCleanedUserSyncRows()
        ));
        return req;
    }

    private DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq buildEmployeePreviewHistoryReq(Long tenantId,
                                                                                                  Long operatorUserId,
                                                                                                  Long autoCreateDeptId,
                                                                                                  Boolean autoCreateMissing,
                                                                                                  LocalDateTime startTime,
                                                                                                  LocalDateTime endTime,
                                                                                                  DingTalkEmployeeSyncService.EmployeeSyncPreview preview) {
        DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq req = baseHistoryReq(
                TYPE_EMPLOYEE_PREVIEW, SCOPE_USER_PROFILE, TRIGGER_MANUAL, tenantId, operatorUserId, startTime, endTime);
        req.setAutoCreateEnabled(Boolean.TRUE.equals(autoCreateMissing));
        req.setAutoCreateDeptId(autoCreateDeptId);
        req.setTotalCount(preview.getTotalFromDingTalk());
        req.setPulledCount(preview.getTotalFromDingTalk());
        req.setSyncedCount(preview.getUpdated());
        req.setCreatedCount(preview.getToCreate());
        req.setUpdatedCount(preview.getUpdated());
        req.setFailedCount(0);
        req.setSkippedCount(preview.getSkippedCreateInvalid() + preview.getSkippedDuplicateMapping()
                + preview.getSkippedNoMobile() + preview.getUnmatched());
        req.setSummary(String.format("toCreate=%d, updated=%d, unmatched=%d, rosterEnabled=%s, rosterPulled=%d, toDisableProfiles=%d, toResignEntries=%d",
                preview.getToCreate(), preview.getUpdated(), preview.getUnmatched(),
                preview.isRosterEnabled(), preview.getRosterPulledUsers(),
                preview.getToDisableProfiles(), preview.getToMarkEntriesResigned()));
        req.setDetailJson(buildDetailJson(
                "matchedByMobile", preview.getMatchedByMobile(),
                "matchedByName", preview.getMatchedByName(),
                "unchanged", preview.getUnchanged(),
                "skippedNoMobile", preview.getSkippedNoMobile(),
                "skippedCreateInvalid", preview.getSkippedCreateInvalid(),
                "skippedDuplicateMapping", preview.getSkippedDuplicateMapping(),
                "toMarkBindingsInactive", preview.getToMarkBindingsInactive(),
                "rosterEnabled", preview.isRosterEnabled(),
                "rosterPulledUsers", preview.getRosterPulledUsers(),
                "toDisableProfiles", preview.getToDisableProfiles(),
                "toMarkEntriesResigned", preview.getToMarkEntriesResigned(),
                "toDisableAdminUsers", preview.getToDisableAdminUsers()
        ));
        return req;
    }

    private DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq buildRosterSyncHistoryReq(Long tenantId,
                                                                                           Long operatorUserId,
                                                                                           LocalDateTime startTime,
                                                                                           LocalDateTime endTime,
                                                                                           DingTalkEmployeeSyncService.EmployeeSyncReport report) {
        DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq req = baseHistoryReq(
                TYPE_ROSTER_SYNC, SCOPE_USER_PROFILE, TRIGGER_MANUAL, tenantId, operatorUserId, startTime, endTime);
        req.setTotalCount(report.getTotalFromDingTalk());
        req.setPulledCount(report.getRosterPulledUsers());
        req.setSyncedCount(report.getRosterSyncedProfiles());
        req.setCreatedCount(0);
        req.setUpdatedCount(report.getRosterSyncedProfiles());
        req.setFailedCount(report.getRosterSyncFailed());
        req.setSkippedCount(report.getUnmatched() + report.getUnchanged());
        req.setSummary(String.format("rosterPulled=%d, rosterSynced=%d, profileUpdates=%d, customFields=%d, unchanged=%d, unmatched=%d, failed=%d",
                report.getRosterPulledUsers(), report.getRosterSyncedProfiles(), report.getRosterUpdatedProfiles(),
                report.getRosterCustomFields(), report.getUnchanged(), report.getUnmatched(), report.getRosterSyncFailed()));
        req.setDetailJson(buildDetailJson(
                "rosterEnabled", report.isRosterEnabled(),
                "rosterPulledUsers", report.getRosterPulledUsers(),
                "rosterSyncedProfiles", report.getRosterSyncedProfiles(),
                "rosterUpdatedProfiles", report.getRosterUpdatedProfiles(),
                "rosterCustomFields", report.getRosterCustomFields(),
                "rosterSyncFailed", report.getRosterSyncFailed(),
                "unmatched", report.getUnmatched(),
                "unchanged", report.getUnchanged(),
                "snapshotBatchId", report.getSnapshotBatchId(),
                "snapshotRows", report.getSnapshotRows()
        ));
        return req;
    }

    private DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq buildEmployeeImportHistoryReq(Long tenantId,
                                                                                                 Long operatorUserId,
                                                                                                 Long deptId,
                                                                                                 LocalDateTime startTime,
                                                                                                 LocalDateTime endTime,
                                                                                                 DingTalkEmployeeSyncService.EmployeeImportReport report) {
        DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq req = baseHistoryReq(
                TYPE_EMPLOYEE_IMPORT, SCOPE_USER_PROFILE, TRIGGER_MANUAL, tenantId, operatorUserId, startTime, endTime);
        req.setAutoCreateEnabled(Boolean.TRUE);
        req.setAutoCreateDeptId(deptId);
        req.setTotalCount(report.getTotalFromDingTalk());
        req.setPulledCount(report.getTotalFromDingTalk());
        req.setSyncedCount(report.getImported());
        req.setCreatedCount(report.getImported());
        req.setUpdatedCount(0);
        req.setFailedCount(report.getFailed());
        req.setSkippedCount(report.getSkippedExists() + report.getSkippedInvalid());
        req.setSummary(String.format("imported=%d, skippedExists=%d, skippedInvalid=%d, failed=%d",
                report.getImported(), report.getSkippedExists(), report.getSkippedInvalid(), report.getFailed()));
        req.setDetailJson(buildDetailJson(
                "targetDeptId", report.getTargetDeptId(),
                "imported", report.getImported(),
                "failed", report.getFailed(),
                "skippedExists", report.getSkippedExists(),
                "skippedInvalid", report.getSkippedInvalid()
        ));
        return req;
    }

    private DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq buildAttendanceSyncHistoryReq(Long tenantId,
                                                                                                 Long operatorUserId,
                                                                                                 Long lookbackMinutes,
                                                                                                 LocalDateTime startTime,
                                                                                                 LocalDateTime endTime,
                                                                                                 DingTalkAttendanceSyncService.AttendanceSyncReport report) {
        DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq req = baseHistoryReq(
                TYPE_ATTENDANCE_SYNC, SCOPE_ATTENDANCE, TRIGGER_MANUAL, tenantId, operatorUserId, startTime, endTime);
        req.setLookbackMinutes(lookbackMinutes);
        req.setTotalCount(report.getPulledRecords());
        req.setPulledCount(report.getPulledRecords());
        req.setSyncedCount(report.getSyncedRecords());
        req.setCreatedCount(report.getCreatedRecords());
        req.setUpdatedCount(report.getUpdatedRecords());
        req.setFailedCount(0);
        req.setSkippedCount(report.getSkippedRecords());
        req.setSummary(String.format("mappedUsers=%d, pulled=%d, synced=%d, created=%d, updated=%d, skipped=%d",
                report.getMappedUsers(), report.getPulledRecords(), report.getSyncedRecords(),
                report.getCreatedRecords(), report.getUpdatedRecords(), report.getSkippedRecords()));
        req.setDetailJson(buildDetailJson(
                "mappedUsers", report.getMappedUsers(),
                "createdRecords", report.getCreatedRecords(),
                "updatedRecords", report.getUpdatedRecords()
        ));
        return req;
    }

    private DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq buildLeaveSyncHistoryReq(Long tenantId,
                                                                                            Long operatorUserId,
                                                                                            Integer lookbackDays,
                                                                                            Integer forwardDays,
                                                                                            LocalDateTime startTime,
                                                                                            LocalDateTime endTime,
                                                                                            DingTalkLeaveSyncService.LeaveSyncReport report) {
        DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq req = baseHistoryReq(
                TYPE_LEAVE_SYNC, SCOPE_LEAVE, TRIGGER_MANUAL, tenantId, operatorUserId, startTime, endTime);
        req.setLookbackMinutes((long) Math.max(lookbackDays == null ? 0 : lookbackDays, 0) * 24 * 60);
        req.setTotalCount(report.getCheckedDays());
        req.setPulledCount(report.getPulledLeaveDays());
        req.setSyncedCount(report.getSyncedLeaves());
        req.setCreatedCount(report.getCreatedLeaves());
        req.setUpdatedCount(report.getUpdatedLeaves());
        req.setFailedCount(report.getFailedCalls());
        req.setSkippedCount(report.getSkippedUsers());
        req.setSummary(String.format("mappedUsers=%d, checkedDays=%d, leaveDays=%d, created=%d, updated=%d, cancelled=%d, failed=%d",
                report.getMappedUsers(), report.getCheckedDays(), report.getPulledLeaveDays(),
                report.getCreatedLeaves(), report.getUpdatedLeaves(), report.getCancelledLeaves(), report.getFailedCalls()));
        req.setDetailJson(buildDetailJson(
                "fromDate", report.getFromDate(),
                "toDate", report.getToDate(),
                "lookbackDays", lookbackDays,
                "forwardDays", forwardDays,
                "syncMode", report.getSyncMode(),
                "apiCalls", report.getApiCalls(),
                "cancelledLeaves", report.getCancelledLeaves()
        ));
        return req;
    }

    private DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq buildAttendancePreviewHistoryReq(Long tenantId,
                                                                                                    Long operatorUserId,
                                                                                                    Long lookbackMinutes,
                                                                                                    Integer sampleLimit,
                                                                                                    LocalDateTime startTime,
                                                                                                    LocalDateTime endTime,
                                                                                                    DingTalkAttendanceSyncService.AttendanceSyncPreview preview) {
        DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq req = baseHistoryReq(
                TYPE_ATTENDANCE_PREVIEW, SCOPE_ATTENDANCE, TRIGGER_MANUAL, tenantId, operatorUserId, startTime, endTime);
        req.setLookbackMinutes(lookbackMinutes);
        req.setTotalCount(preview.getPulledRecords());
        req.setPulledCount(preview.getPulledRecords());
        req.setSyncedCount(0);
        req.setCreatedCount(0);
        req.setUpdatedCount(0);
        req.setFailedCount(0);
        req.setSkippedCount(preview.getSkippedRecords());
        req.setSummary(String.format("mappedUsers=%d, pulled=%d, skipped=%d, sample=%d",
                preview.getMappedUsers(), preview.getPulledRecords(), preview.getSkippedRecords(),
                preview.getSampleRecords() == null ? 0 : preview.getSampleRecords().size()));
        req.setDetailJson(buildDetailJson(
                "sampleLimit", sampleLimit,
                "sampleSize", preview.getSampleRecords() == null ? 0 : preview.getSampleRecords().size(),
                "fromTime", preview.getFromTime(),
                "toTime", preview.getToTime(),
                "mappedUsers", preview.getMappedUsers()
        ));
        return req;
    }

    private DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq buildFullSyncHistoryReq(Long tenantId,
                                                                                           Long operatorUserId,
                                                                                           LocalDateTime startTime,
                                                                                           LocalDateTime endTime,
                                                                                           String result) {
        DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq req = baseHistoryReq(
                TYPE_FULL_SYNC, SCOPE_ALL, TRIGGER_MANUAL, tenantId, operatorUserId, startTime, endTime);
        req.setTotalCount(0);
        req.setPulledCount(0);
        req.setSyncedCount(0);
        req.setCreatedCount(0);
        req.setUpdatedCount(0);
        req.setFailedCount(0);
        req.setSkippedCount(0);
        req.setSummary(result);
        req.setDetailJson(buildDetailJson("result", result));
        return req;
    }

    private DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq baseHistoryReq(String syncType,
                                                                                  String syncScope,
                                                                                  String triggerMode,
                                                                                  Long tenantId,
                                                                                  Long operatorUserId,
                                                                                  LocalDateTime startTime,
                                                                                  LocalDateTime endTime) {
        DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq req = new DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq();
        req.setSyncType(syncType);
        req.setSyncScope(syncScope);
        req.setTriggerMode(triggerMode);
        req.setTargetTenantId(tenantId);
        req.setOperatorUserId(operatorUserId);
        req.setSyncStartTime(startTime);
        req.setSyncEndTime(endTime);
        req.setDurationMs(calculateDurationMs(startTime, endTime));
        return req;
    }

    private String buildDetailJson(Object... keyValues) {
        Map<String, Object> detail = new LinkedHashMap<>();
        if (keyValues == null || keyValues.length == 0) {
            return JsonUtils.toJsonString(detail);
        }
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            Object key = keyValues[i];
            if (key == null) {
                continue;
            }
            detail.put(String.valueOf(key), keyValues[i + 1]);
        }
        return JsonUtils.toJsonString(detail);
    }

    private long calculateDurationMs(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return 0L;
        }
        return Math.max(Duration.between(startTime, endTime).toMillis(), 0L);
    }

    private void saveHistory(DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq req) {
        try {
            dingTalkSyncHistoryService.save(req);
        } catch (Exception ex) {
            log.warn("Save DingTalk sync history failed, syncType={}, tenantId={}, reason={}",
                    req == null ? null : req.getSyncType(),
                    req == null ? null : req.getTargetTenantId(),
                    ex.getMessage());
        }
    }

    private Long resolveTargetTenantId(Long targetTenantId) {
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (targetTenantId == null || targetTenantId <= 0 || Objects.equals(targetTenantId, currentTenantId)) {
            return currentTenantId;
        }
        Set<Long> tenantIdList = TenantContextHolder.getTenantIdList();
        if (tenantIdList != null && tenantIdList.contains(targetTenantId)) {
            return targetTenantId;
        }
        throw exception(FORBIDDEN, "无权限访问目标租户");
    }

    @Data
    public static class FullSyncReport {
        private String result;
    }

    @Data
    public static class RequirementNoticeConfigResp {
        private Boolean enabled;
        private List<DingTalkRequirementNoticeConfigService.SceneConfig> scenes;
        private DingTalkRequirementNoticeService.NoticeStats stats;
    }
}
