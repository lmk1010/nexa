package com.kyx.service.hr.integration.dingtalk.job;

import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.foundation.tenant.core.service.TenantFrameworkService;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.hr.config.DingTalkProperties;
import com.kyx.service.hr.integration.dingtalk.service.DingTalkAttendanceSyncService;
import com.kyx.service.hr.integration.dingtalk.service.DingTalkEmployeeSyncService;
import com.kyx.service.hr.integration.dingtalk.service.DingTalkLeaveSyncService;
import com.kyx.service.hr.integration.dingtalk.service.DingTalkSyncConfigService;
import com.kyx.service.hr.integration.dingtalk.service.DingTalkSyncHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * DingTalk sync job (employee + attendance), invoked by scheduler platform.
 */
@Component
@Slf4j
public class DingTalkSyncScheduleJob {

    private static final String TYPE_EMPLOYEE_SYNC = "EMPLOYEE_SYNC";
    private static final String TYPE_ATTENDANCE_SYNC = "ATTENDANCE_SYNC";
    private static final String TYPE_LEAVE_SYNC = "LEAVE_SYNC";
    private static final String SCOPE_USER_PROFILE = "USER_PROFILE";
    private static final String SCOPE_ATTENDANCE = "ATTENDANCE";
    private static final String SCOPE_LEAVE = "LEAVE";
    private static final String TRIGGER_SCHEDULE = "SCHEDULE";

    @Resource
    private DingTalkProperties dingTalkProperties;
    @Resource
    private TenantFrameworkService tenantFrameworkService;
    @Resource
    private DingTalkEmployeeSyncService dingTalkEmployeeSyncService;
    @Resource
    private DingTalkAttendanceSyncService dingTalkAttendanceSyncService;
    @Resource
    private DingTalkLeaveSyncService dingTalkLeaveSyncService;
    @Resource
    private DingTalkSyncConfigService dingTalkSyncConfigService;
    @Resource
    private DingTalkSyncHistoryService dingTalkSyncHistoryService;

    /**
     * DingTalk daily sync entry. OA scheduler invokes this job through HR RPC.
     * taskMethod=executeDailySync
     */
    public String executeDailySync() {
        sleepBeforeScheduledEmployeeSyncIfNeeded("daily");
        return executeSync("daily", this::syncByTenant);
    }

    /**
     * taskMethod=executeEmployeeSync
     */
    public String executeEmployeeSync() {
        sleepBeforeScheduledEmployeeSyncIfNeeded("employee");
        return executeSync("employee", this::syncEmployeeByTenant);
    }

    /**
     * taskMethod=executeAttendanceSync
     */
    public String executeAttendanceSync() {
        return executeSync("attendance", this::syncAttendanceByTenant);
    }

    /**
     * taskMethod=executeLeaveSync
     */
    public String executeLeaveSync() {
        return executeSync("leave", this::syncLeaveByTenant);
    }

    private String executeSync(String scope, Consumer<Long> syncAction) {
        if (!Boolean.TRUE.equals(dingTalkProperties.getSync().getEnabled())) {
            return "DingTalk sync disabled";
        }
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (currentTenantId != null && currentTenantId > 0) {
            syncAction.accept(currentTenantId);
            return "DingTalk " + scope + " sync done, tenantId=" + currentTenantId;
        }
        int processed = syncAllTenants(syncAction);
        return "DingTalk " + scope + " sync done, tenants=" + processed;
    }

    private int syncAllTenants(Consumer<Long> syncAction) {
        int processed = 0;
        List<Long> tenantIds = tenantFrameworkService.getTenantIds();
        if (tenantIds == null || tenantIds.isEmpty()) {
            return processed;
        }
        for (Long tenantId : tenantIds) {
            if (tenantId == null || tenantId <= 0) {
                continue;
            }
            processed++;
            TenantUtils.execute(tenantId, () -> syncAction.accept(tenantId));
        }
        return processed;
    }

    private void sleepBeforeScheduledEmployeeSyncIfNeeded(String scope) {
        Long delayMs = dingTalkProperties.getSync() == null ? null
                : dingTalkProperties.getSync().getEmployeeScheduleStartDelayMs();
        long waitMs = delayMs == null ? 0L : Math.max(delayMs, 0L);
        if (waitMs <= 0L) {
            return;
        }
        log.info("Delay DingTalk scheduled {} sync before employee stage, waitMs={}", scope, waitMs);
        try {
            Thread.sleep(waitMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while delaying DingTalk scheduled {} sync", scope);
        }
    }

    private void syncByTenant(Long tenantId) {
        syncEmployeeByTenant(tenantId);
        syncAttendanceByTenant(tenantId);
        syncLeaveByTenant(tenantId);
    }

    private void syncEmployeeByTenant(Long tenantId) {
        if (!Boolean.TRUE.equals(dingTalkProperties.getSync().getEmployeeEnabled())) {
            return;
        }
        LocalDateTime startTime = LocalDateTime.now();
        try {
            DingTalkEmployeeSyncService.EmployeeSyncReport report = dingTalkEmployeeSyncService.syncExistingProfiles();
            LocalDateTime endTime = LocalDateTime.now();
            DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq req = baseHistoryReq(
                    TYPE_EMPLOYEE_SYNC, SCOPE_USER_PROFILE, tenantId, startTime, endTime);
            req.setTotalCount(report.getTotalFromDingTalk());
            req.setPulledCount(report.getTotalFromDingTalk());
            req.setSyncedCount(report.getCreated() + report.getUpdated() + report.getRosterSyncedProfiles());
            req.setCreatedCount(report.getCreated());
            req.setUpdatedCount(report.getUpdated() + report.getRosterSyncedProfiles());
            req.setFailedCount(report.getCreateFailed() + report.getRosterSyncFailed());
            req.setSkippedCount(report.getSkippedCreateInvalid() + report.getSkippedDuplicateMapping()
                    + report.getSkippedNoMobile() + report.getUnmatched());
            req.setSummary(String.format("schedule employee sync: created=%d, updated=%d, failed=%d, rosterPulled=%d, rosterSynced=%d, rosterCustomFields=%d, rosterFailed=%d, deptTreeSynced=%d, deptTreeCreated=%d, deptTreeUpdated=%d, deptTreeFailed=%d, deptUpdates=%d, jobTitleUpdates=%d, postSyncs=%d, postCreates=%d, roleCreates=%d, roleAssigns=%d, roleRemoves=%d, functionSyncFailed=%d, snapshots=%d, cleanedProfiles=%d, cleanedBindings=%d, disabledUsers=%d",
                    report.getCreated(), report.getUpdated(), report.getCreateFailed(),
                    report.getRosterPulledUsers(), report.getRosterSyncedProfiles(),
                    report.getRosterCustomFields(), report.getRosterSyncFailed(),
                    report.getDeptTreeSynced(), report.getDeptTreeCreated(), report.getDeptTreeUpdated(),
                    report.getDeptTreeFailed(), report.getSyncedDeptIds(), report.getSyncedJobTitles(),
                    report.getSyncedPosts(), report.getCreatedPosts(), report.getCreatedRoles(),
                    report.getAssignedRoles(), report.getRemovedRoles(), report.getFunctionSyncFailed(),
                    report.getSnapshotRows(), report.getCleanedProfiles(), report.getCleanedBindings(),
                    report.getDisabledAdminUsers()));
            req.setDetailJson(toJson(detailMap(
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
                    "syncedBindings", report.getSyncedBindings(),
                    "syncedEntryStatuses", report.getSyncedEntryStatuses(),
                    "syncedDeptIds", report.getSyncedDeptIds(),
                    "syncedJobTitles", report.getSyncedJobTitles(),
                    "syncedPosts", report.getSyncedPosts(),
                    "createdPosts", report.getCreatedPosts(),
                    "createdRoles", report.getCreatedRoles(),
                    "assignedRoles", report.getAssignedRoles(),
                    "removedRoles", report.getRemovedRoles(),
                    "functionSyncFailed", report.getFunctionSyncFailed(),
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
            )));
            saveHistory(req);
        } catch (Exception ex) {
            log.error("DingTalk employee sync failed, tenantId={}", tenantId, ex);
            saveFailureHistory(TYPE_EMPLOYEE_SYNC, SCOPE_USER_PROFILE, tenantId, startTime, ex);
        }
    }

    private void syncAttendanceByTenant(Long tenantId) {
        LocalDateTime startTime = LocalDateTime.now();
        try {
            DingTalkSyncConfigService.SyncConfig syncConfig = dingTalkSyncConfigService.getConfig();
            if (!Boolean.TRUE.equals(dingTalkProperties.getSync().getAttendanceEnabled())
                    || !Boolean.TRUE.equals(syncConfig.getAttendanceEnabled())) {
                return;
            }
            Long lookbackMinutes = dingTalkProperties.getSync().getAttendanceLookbackMinutes();
            long lookback = lookbackMinutes == null ? 1_440L : Math.max(lookbackMinutes, 1L);
            LocalDateTime to = startTime;
            LocalDateTime from = to.minusMinutes(lookback);
            DingTalkAttendanceSyncService.AttendanceSyncReport report = dingTalkAttendanceSyncService.syncByTimeRange(from, to);
            LocalDateTime endTime = LocalDateTime.now();
            DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq req = baseHistoryReq(
                    TYPE_ATTENDANCE_SYNC, SCOPE_ATTENDANCE, tenantId, startTime, endTime);
            req.setLookbackMinutes(lookback);
            req.setTotalCount(report.getPulledRecords());
            req.setPulledCount(report.getPulledRecords());
            req.setSyncedCount(report.getSyncedRecords());
            req.setCreatedCount(report.getCreatedRecords());
            req.setUpdatedCount(report.getUpdatedRecords());
            req.setFailedCount(0);
            req.setSkippedCount(report.getSkippedRecords());
            req.setSummary(String.format("schedule attendance sync: mapped=%d, pulled=%d, synced=%d, created=%d, updated=%d, skipped=%d",
                    report.getMappedUsers(), report.getPulledRecords(), report.getSyncedRecords(),
                    report.getCreatedRecords(), report.getUpdatedRecords(), report.getSkippedRecords()));
            req.setDetailJson(toJson(detailMap(
                    "mappedUsers", report.getMappedUsers(),
                    "fromTime", from,
                    "toTime", to
            )));
            saveHistory(req);
        } catch (Exception ex) {
            log.error("DingTalk attendance sync failed, tenantId={}", tenantId, ex);
            saveFailureHistory(TYPE_ATTENDANCE_SYNC, SCOPE_ATTENDANCE, tenantId, startTime, ex);
        }
    }

    private void syncLeaveByTenant(Long tenantId) {
        LocalDateTime startTime = LocalDateTime.now();
        try {
            DingTalkSyncConfigService.SyncConfig syncConfig = dingTalkSyncConfigService.getConfig();
            if (!Boolean.TRUE.equals(dingTalkProperties.getSync().getLeaveEnabled())
                    || !Boolean.TRUE.equals(syncConfig.getLeaveEnabled())) {
                return;
            }
            Integer lookbackDays = dingTalkProperties.getSync().getLeaveLookbackDays();
            Integer forwardDays = dingTalkProperties.getSync().getLeaveForwardDays();
            int lookback = lookbackDays == null ? 1 : Math.max(lookbackDays, 1);
            int forward = forwardDays == null ? 0 : Math.max(forwardDays, 0);
            LocalDate baseDate = startTime.toLocalDate().minusDays(1);
            DingTalkLeaveSyncService.LeaveSyncReport report = dingTalkLeaveSyncService.syncByDateRange(
                    baseDate.minusDays(Math.max(lookback - 1, 0)), baseDate.plusDays(forward));
            LocalDateTime endTime = LocalDateTime.now();
            DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq req = baseHistoryReq(
                    TYPE_LEAVE_SYNC, SCOPE_LEAVE, tenantId, startTime, endTime);
            req.setLookbackMinutes((long) lookback * 24 * 60);
            req.setTotalCount(report.getCheckedDays());
            req.setPulledCount(report.getPulledLeaveDays());
            req.setSyncedCount(report.getSyncedLeaves());
            req.setCreatedCount(report.getCreatedLeaves());
            req.setUpdatedCount(report.getUpdatedLeaves());
            req.setFailedCount(report.getFailedCalls());
            req.setSkippedCount(report.getSkippedUsers());
            req.setSummary(String.format("schedule leave sync: mapped=%d, checkedDays=%d, leaveDays=%d, created=%d, updated=%d, cancelled=%d, failed=%d",
                    report.getMappedUsers(), report.getCheckedDays(), report.getPulledLeaveDays(),
                    report.getCreatedLeaves(), report.getUpdatedLeaves(), report.getCancelledLeaves(),
                    report.getFailedCalls()));
            req.setDetailJson(toJson(detailMap(
                    "fromDate", report.getFromDate(),
                    "toDate", report.getToDate(),
                    "forwardDays", forward,
                    "cancelledLeaves", report.getCancelledLeaves()
            )));
            saveHistory(req);
        } catch (Exception ex) {
            log.error("DingTalk leave sync failed, tenantId={}", tenantId, ex);
            saveFailureHistory(TYPE_LEAVE_SYNC, SCOPE_LEAVE, tenantId, startTime, ex);
        }
    }

    private DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq baseHistoryReq(String syncType,
                                                                                  String syncScope,
                                                                                  Long tenantId,
                                                                                  LocalDateTime startTime,
                                                                                  LocalDateTime endTime) {
        DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq req = new DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq();
        req.setSyncType(syncType);
        req.setSyncScope(syncScope);
        req.setTriggerMode(TRIGGER_SCHEDULE);
        req.setTargetTenantId(tenantId);
        req.setSyncStartTime(startTime);
        req.setSyncEndTime(endTime);
        req.setDurationMs(calculateDurationMs(startTime, endTime));
        return req;
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
            log.warn("Save scheduled DingTalk history failed, syncType={}, tenantId={}, reason={}",
                    req == null ? null : req.getSyncType(),
                    req == null ? null : req.getTargetTenantId(),
                    ex.getMessage());
        }
    }

    private void saveFailureHistory(String syncType, String syncScope, Long tenantId,
                                    LocalDateTime startTime, Exception cause) {
        LocalDateTime endTime = LocalDateTime.now();
        DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq req = baseHistoryReq(
                syncType, syncScope, tenantId, startTime, endTime);
        req.setFailedCount(1);
        req.setSummary("schedule " + syncScope + " sync failed: " + resolveErrorMessage(cause));
        req.setDetailJson(toJson(detailMap("error", resolveErrorMessage(cause))));
        saveHistory(req);
    }

    private String resolveErrorMessage(Exception ex) {
        if (ex == null) {
            return null;
        }
        Throwable cause = ex;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        if (message == null || message.trim().isEmpty()) {
            message = ex.getMessage();
        }
        return message == null || message.trim().isEmpty() ? ex.getClass().getSimpleName() : message.trim();
    }

    private Map<String, Object> detailMap(Object... keyValues) {
        Map<String, Object> detail = new LinkedHashMap<>();
        if (keyValues == null || keyValues.length == 0) {
            return detail;
        }
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            Object key = keyValues[i];
            if (key == null) {
                continue;
            }
            detail.put(String.valueOf(key), keyValues[i + 1]);
        }
        return detail;
    }

    private String toJson(Object obj) {
        try {
            return com.kyx.foundation.common.util.json.JsonUtils.toJsonString(obj);
        } catch (Exception ex) {
            return null;
        }
    }

}
