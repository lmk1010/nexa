package com.kyx.service.hr.integration.dingtalk.listener;

import com.dingtalk.open.app.api.GenericEventListener;
import com.dingtalk.open.app.api.OpenDingTalkClient;
import com.dingtalk.open.app.api.OpenDingTalkStreamClientBuilder;
import com.dingtalk.open.app.api.message.GenericOpenDingTalkEvent;
import com.dingtalk.open.app.api.security.AuthClientCredential;
import com.dingtalk.open.app.stream.protocol.event.EventAckStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.kyx.foundation.common.util.json.JsonUtils;
import com.kyx.foundation.tenant.core.service.TenantFrameworkService;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.hr.config.DingTalkProperties;
import com.kyx.service.hr.dal.dataobject.integration.DingTalkUserBindingDO;
import com.kyx.service.hr.dal.mysql.integration.DingTalkUserBindingMapper;
import com.kyx.service.hr.integration.dingtalk.service.DingTalkAttendanceSyncService;
import com.kyx.service.hr.integration.dingtalk.service.DingTalkEmployeeSyncService;
import com.kyx.service.hr.integration.dingtalk.service.DingTalkLeaveSyncService;
import com.kyx.service.hr.integration.dingtalk.service.DingTalkSyncConfigService;
import com.kyx.service.hr.integration.dingtalk.service.DingTalkSyncHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * DingTalk stream event listener.
 *
 * Stream mode keeps a long-lived websocket and writes attendance records in realtime.
 * If payload parsing fails, it falls back to small-window delta sync.
 */
@Component
@Slf4j
public class DingTalkStreamEventListener {

    private static final int EVENT_LOG_MAX_LENGTH = 1000;
    private static final String TYPE_EMPLOYEE_SYNC = "EMPLOYEE_SYNC";
    private static final String TYPE_ROSTER_SYNC = "ROSTER_SYNC";
    private static final String TYPE_ATTENDANCE_SYNC = "ATTENDANCE_SYNC";
    private static final String TYPE_LEAVE_SYNC = "LEAVE_SYNC";
    private static final String SCOPE_USER_PROFILE = "USER_PROFILE";
    private static final String SCOPE_ROSTER = "ROSTER";
    private static final String SCOPE_ATTENDANCE = "ATTENDANCE";
    private static final String SCOPE_LEAVE = "LEAVE";
    private static final String TRIGGER_STREAM = "STREAM";

    private final ExecutorService streamExecutor = newSingleThreadExecutor("dingtalk-stream-client");
    private final ExecutorService syncExecutor = newSingleThreadExecutor("dingtalk-stream-sync");
    private final AtomicLong lastAttendanceSyncEpochSeconds = new AtomicLong(0L);
    private final AtomicLong lastEmployeeSyncEpochSeconds = new AtomicLong(0L);

    @Resource
    private DingTalkProperties dingTalkProperties;
    @Resource
    private TenantFrameworkService tenantFrameworkService;
    @Resource
    private DingTalkAttendanceSyncService dingTalkAttendanceSyncService;
    @Resource
    private DingTalkEmployeeSyncService dingTalkEmployeeSyncService;
    @Resource
    private DingTalkLeaveSyncService dingTalkLeaveSyncService;
    @Resource
    private DingTalkSyncConfigService dingTalkSyncConfigService;
    @Resource
    private DingTalkSyncHistoryService dingTalkSyncHistoryService;
    @Resource
    private DingTalkUserBindingMapper dingTalkUserBindingMapper;
    @Resource
    private ConfigurableEnvironment environment;

    private volatile OpenDingTalkClient streamClient;

    @PostConstruct
    public void start() {
        logStreamSwitchDiagnose();
        Boolean enabled = dingTalkProperties.getStream() == null ? null : dingTalkProperties.getStream().getEnabled();
        if (!Boolean.TRUE.equals(enabled)) {
            log.warn("Skip DingTalk stream start: dingtalk.stream.enabled={}", enabled);
            return;
        }
        final String appKey = trim(dingTalkProperties.getApp().getAppKey());
        final String appSecret = trim(dingTalkProperties.getApp().getAppSecret());
        if (!StringUtils.hasText(appKey) || !StringUtils.hasText(appSecret)) {
            log.error("Skip DingTalk stream start: missing dingtalk.app.app-key or dingtalk.app.app-secret");
            return;
        }
        log.info("DingTalk stream bootstrap check passed, appKeyPrefix={}, consumeThreads={}",
                appKey.length() >= 6 ? appKey.substring(0, 6) : appKey,
                dingTalkProperties.getStream().getConsumeThreads());
        streamExecutor.submit(() -> startClient(appKey, appSecret));
    }

    @PreDestroy
    public void stop() {
        OpenDingTalkClient client = this.streamClient;
        if (client != null) {
            try {
                client.stop();
            } catch (Exception ex) {
                log.warn("Stop DingTalk stream client failed: {}", ex.getMessage());
            }
        }
        shutdownExecutor(syncExecutor);
        shutdownExecutor(streamExecutor);
    }

    private void startClient(String appKey, String appSecret) {
        int consumeThreads = Math.max(safeInt(dingTalkProperties.getStream().getConsumeThreads(), 4), 1);
        try {
            OpenDingTalkClient client = OpenDingTalkStreamClientBuilder.custom()
                    .credential(new AuthClientCredential(appKey, appSecret))
                    .consumeThreads(consumeThreads)
                    .registerAllEventListener(buildEventListener())
                    .build();
            this.streamClient = client;
            log.info("DingTalk stream client starting, consumeThreads={}", consumeThreads);
            client.start();
            log.warn("DingTalk stream client exited unexpectedly, waiting for app restart");
        } catch (Exception ex) {
            log.error("DingTalk stream client failed to start", ex);
        }
    }

    private GenericEventListener buildEventListener() {
        return event -> {
            try {
                if (!isCorpMatched(event)) {
                    return EventAckStatus.SUCCESS;
                }
                String eventType = resolveEventType(event);
                logEventPayload(eventType, event);
                String rawPayload = event.getData() == null ? "" : event.getData().toString();
                boolean handled = false;
                if (isRosterEvent(eventType, rawPayload)) {
                    submitRosterEventProcess(eventType, trim(event.getEventId()),
                            event.getEventBornTime(), rawPayload);
                    handled = true;
                }
                if (!handled && isEmployeeDirectoryEvent(eventType)) {
                    submitEmployeeDirectoryEventProcess(eventType, trim(event.getEventId()),
                            event.getEventBornTime(), rawPayload);
                    handled = true;
                }
                if (isAttendanceEvent(eventType)) {
                    submitAttendanceEventProcess(eventType, trim(event.getEventId()), event.getEventBornTime(),
                            rawPayload);
                    handled = true;
                }
                if (isLeaveEvent(eventType, rawPayload)) {
                    submitLeaveEventProcess(eventType, trim(event.getEventId()), event.getEventBornTime(), rawPayload);
                    handled = true;
                }
                if (!handled) {
                    log.debug("Ignore DingTalk stream event, eventType={}, eventId={}", eventType, event.getEventId());
                }
                return EventAckStatus.SUCCESS;
            } catch (Exception ex) {
                log.error("Process DingTalk stream event failed, eventId={}", event.getEventId(), ex);
                return EventAckStatus.LATER;
            }
        };
    }

    private boolean isCorpMatched(GenericOpenDingTalkEvent event) {
        String configuredCorpId = trim(dingTalkProperties.getApp().getCorpId());
        String eventCorpId = trim(event.getEventCorpId());
        if (!StringUtils.hasText(configuredCorpId) || !StringUtils.hasText(eventCorpId)) {
            return true;
        }
        if (configuredCorpId.equals(eventCorpId)) {
            return true;
        }
        log.warn("Ignore DingTalk stream event due to corp mismatch, configCorpId={}, eventCorpId={}, eventId={}",
                configuredCorpId, eventCorpId, event.getEventId());
        return false;
    }

    private void submitAttendanceEventProcess(String eventType, String eventId, Long eventBornTime, String rawPayload) {
        syncExecutor.submit(() -> processAttendanceEvent(eventType, eventId, eventBornTime, rawPayload));
    }

    private void submitEmployeeDirectoryEventProcess(String eventType, String eventId, Long eventBornTime,
                                                     String rawPayload) {
        syncExecutor.submit(() -> processEmployeeDirectoryEvent(eventType, eventId, eventBornTime, rawPayload));
    }

    private void submitRosterEventProcess(String eventType, String eventId, Long eventBornTime, String rawPayload) {
        syncExecutor.submit(() -> processRosterEvent(eventType, eventId, eventBornTime, rawPayload));
    }

    private void submitLeaveEventProcess(String eventType, String eventId, Long eventBornTime, String rawPayload) {
        syncExecutor.submit(() -> processLeaveEvent(eventType, eventId, eventBornTime, rawPayload));
    }

    private void processAttendanceEvent(String eventType, String eventId, Long eventBornTime, String rawPayload) {
        List<Long> targetTenantIds = resolveTargetTenantIds(rawPayload);
        if (targetTenantIds.isEmpty()) {
            log.warn("DingTalk stream event skipped: no tenant matched by dingUserId, eventType={}, eventId={}",
                    eventType, eventId);
            return;
        }
        DirectSyncSummary summary = syncAttendanceFromEventForTenants(eventType, eventId, eventBornTime, rawPayload, targetTenantIds);
        if (summary.totalPulledRecords <= 0) {
            log.info("DingTalk stream event processed with no records, tenantCount={}, mappedUsers={}, skippedRecords={}, will rely on scheduled sync, eventType={}, eventId={}",
                    targetTenantIds.size(), summary.totalMappedUsers, summary.totalSkippedRecords, eventType, eventId);
        }
    }

    private DirectSyncSummary syncAttendanceFromEventForTenants(String eventType, String eventId, Long eventBornTime,
                                                                String rawPayload, List<Long> tenantIds) {
        DirectSyncSummary summary = new DirectSyncSummary();
        if (tenantIds == null || tenantIds.isEmpty()) {
            log.warn("Skip DingTalk stream realtime write: no target tenant found, eventType={}, eventId={}", eventType, eventId);
            return summary;
        }
        for (Long tenantId : tenantIds) {
            if (tenantId == null) {
                continue;
            }
            LocalDateTime startTime = LocalDateTime.now();
            try {
                TenantUtils.execute(tenantId, () -> {
                    if (!dingTalkSyncConfigService.isAttendanceEnabled()) {
                        log.info("Skip DingTalk stream attendance sync by switch, tenantId={}, eventType={}, eventId={}",
                                tenantId, eventType, eventId);
                        return;
                    }
                    DingTalkAttendanceSyncService.AttendanceSyncReport report =
                            dingTalkAttendanceSyncService.syncByStreamEventPayload(rawPayload);
                    summary.totalMappedUsers += report.getMappedUsers();
                    summary.totalPulledRecords += report.getPulledRecords();
                    summary.totalSyncedRecords += report.getSyncedRecords();
                    summary.totalSkippedRecords += report.getSkippedRecords();
                    LocalDateTime endTime = LocalDateTime.now();
                    saveStreamAttendanceHistory(tenantId, startTime, endTime, eventType, eventId, eventBornTime, report);
                    if (report.getPulledRecords() > 0) {
                        log.info("DingTalk stream realtime write finished, tenantId={}, eventType={}, eventId={}, eventBornTime={}, report={}",
                                tenantId, eventType, eventId, eventBornTime, JsonUtils.toJsonString(report));
                    }
                });
            } catch (Exception ex) {
                log.error("DingTalk stream realtime write failed, tenantId={}, eventType={}, eventId={}",
                        tenantId, eventType, eventId, ex);
                saveStreamFailureHistory(TYPE_ATTENDANCE_SYNC, SCOPE_ATTENDANCE, tenantId, startTime,
                        eventType, eventId, eventBornTime, ex);
            }
        }
        return summary;
    }

    private void saveStreamAttendanceHistory(Long tenantId, LocalDateTime startTime, LocalDateTime endTime,
                                             String eventType, String eventId, Long eventBornTime,
                                             DingTalkAttendanceSyncService.AttendanceSyncReport report) {
        DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq req =
                newStreamHistoryReq(TYPE_ATTENDANCE_SYNC, SCOPE_ATTENDANCE, tenantId, startTime, endTime);
        req.setTotalCount(report == null ? 0 : report.getPulledRecords());
        req.setPulledCount(report == null ? 0 : report.getPulledRecords());
        req.setSyncedCount(report == null ? 0 : report.getSyncedRecords());
        req.setCreatedCount(report == null ? 0 : report.getCreatedRecords());
        req.setUpdatedCount(report == null ? 0 : report.getUpdatedRecords());
        req.setSkippedCount(report == null ? 0 : report.getSkippedRecords());
        req.setSummary(report == null ? "stream attendance sync: empty report" : String.format(
                "stream attendance sync: mapped=%d, pulled=%d, synced=%d, created=%d, updated=%d, skipped=%d",
                report.getMappedUsers(), report.getPulledRecords(), report.getSyncedRecords(),
                report.getCreatedRecords(), report.getUpdatedRecords(), report.getSkippedRecords()));
        req.setDetailJson(toJson(detailMap(
                "eventType", eventType,
                "eventId", eventId,
                "eventBornTime", eventBornTime,
                "mappedUsers", report == null ? null : report.getMappedUsers()
        )));
        saveHistory(req);
    }

    private void processRosterEvent(String eventType, String eventId, Long eventBornTime, String rawPayload) {
        if (!Boolean.TRUE.equals(dingTalkProperties.getSync().getEnabled())
                || !Boolean.TRUE.equals(dingTalkProperties.getSync().getEmployeeEnabled())) {
            log.info("Skip DingTalk stream roster sync by employee switch, eventType={}, eventId={}", eventType, eventId);
            return;
        }
        if (!Boolean.TRUE.equals(dingTalkProperties.getSync().getRosterEnabled())) {
            log.info("Skip DingTalk stream roster sync by roster switch, eventType={}, eventId={}", eventType, eventId);
            return;
        }

        Set<String> targetDingUserIds = extractDingUserIds(rawPayload);
        if (targetDingUserIds.isEmpty()) {
            log.warn("DingTalk stream roster event has no user id, fallback to employee sync, eventType={}, eventId={}",
                    eventType, eventId);
            processEmployeeDirectoryEvent(eventType, eventId, eventBornTime, rawPayload);
            return;
        }

        Map<Long, Set<String>> userIdsByTenant = resolveRosterEventUserIdsByTenant(targetDingUserIds);
        if (userIdsByTenant.isEmpty()) {
            log.warn("DingTalk stream roster event has no bound tenant, fallback to employee sync, eventType={}, eventId={}, users={}",
                    eventType, eventId, targetDingUserIds.size());
            processEmployeeDirectoryEvent(eventType, eventId, eventBornTime, rawPayload);
            return;
        }

        syncRosterFromEventForTenants(eventType, eventId, eventBornTime, userIdsByTenant);

        Set<String> routedUserIds = new LinkedHashSet<>();
        for (Set<String> tenantUserIds : userIdsByTenant.values()) {
            if (tenantUserIds != null) {
                routedUserIds.addAll(tenantUserIds);
            }
        }
        Set<String> unroutedUserIds = new LinkedHashSet<>(targetDingUserIds);
        unroutedUserIds.removeAll(routedUserIds);
        if (!unroutedUserIds.isEmpty()) {
            log.warn("DingTalk stream roster event has unrouted users, fallback to employee sync, eventType={}, eventId={}, unrouted={}",
                    eventType, eventId, unroutedUserIds.size());
            processEmployeeDirectoryEvent(eventType, eventId, eventBornTime, rawPayload);
        }
    }

    private void syncRosterFromEventForTenants(String eventType, String eventId, Long eventBornTime,
                                               Map<Long, Set<String>> userIdsByTenant) {
        for (Map.Entry<Long, Set<String>> entry : userIdsByTenant.entrySet()) {
            Long tenantId = entry.getKey();
            Set<String> userIds = entry.getValue();
            if (tenantId == null || userIds == null || userIds.isEmpty()) {
                continue;
            }
            LocalDateTime startTime = LocalDateTime.now();
            try {
                TenantUtils.execute(tenantId, () -> {
                    DingTalkEmployeeSyncService.EmployeeSyncReport report =
                            dingTalkEmployeeSyncService.syncRosterByDingUserIds(userIds);
                    LocalDateTime endTime = LocalDateTime.now();
                    saveStreamRosterHistory(tenantId, startTime, endTime, eventType, eventId, eventBornTime, report);
                    log.info("DingTalk stream roster sync finished, tenantId={}, eventType={}, eventId={}, eventBornTime={}, "
                                    + "users={}, rosterPulled={}, rosterSynced={}, rosterProfileUpdates={}, customFields={}, failed={}",
                            tenantId, eventType, eventId, eventBornTime, report.getTotalFromDingTalk(),
                            report.getRosterPulledUsers(), report.getRosterSyncedProfiles(),
                            report.getRosterUpdatedProfiles(), report.getRosterCustomFields(),
                            report.getRosterSyncFailed());
                });
            } catch (Exception ex) {
                log.error("DingTalk stream roster sync failed, tenantId={}, eventType={}, eventId={}",
                        tenantId, eventType, eventId, ex);
                saveStreamFailureHistory(TYPE_ROSTER_SYNC, SCOPE_ROSTER, tenantId, startTime,
                        eventType, eventId, eventBornTime, ex);
            }
        }
    }

    private void saveStreamRosterHistory(Long tenantId, LocalDateTime startTime, LocalDateTime endTime,
                                         String eventType, String eventId, Long eventBornTime,
                                         DingTalkEmployeeSyncService.EmployeeSyncReport report) {
        DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq req =
                newStreamHistoryReq(TYPE_ROSTER_SYNC, SCOPE_ROSTER, tenantId, startTime, endTime);
        req.setTotalCount(report == null ? 0 : report.getTotalFromDingTalk());
        req.setPulledCount(report == null ? 0 : report.getRosterPulledUsers());
        req.setSyncedCount(report == null ? 0 : report.getRosterSyncedProfiles());
        req.setUpdatedCount(report == null ? 0 : report.getRosterUpdatedProfiles());
        req.setFailedCount(report == null ? 0 : report.getRosterSyncFailed());
        req.setSkippedCount(report == null ? 0 : report.getUnmatched());
        req.setSummary(report == null ? "stream roster sync: empty report" : String.format(
                "stream roster sync: users=%d, pulled=%d, rosterSynced=%d, profileUpdates=%d, customFields=%d, failed=%d, unmatched=%d, unchanged=%d",
                report.getTotalFromDingTalk(), report.getRosterPulledUsers(), report.getRosterSyncedProfiles(),
                report.getRosterUpdatedProfiles(), report.getRosterCustomFields(), report.getRosterSyncFailed(),
                report.getUnmatched(), report.getUnchanged()));
        req.setDetailJson(toJson(detailMap(
                "eventType", eventType,
                "eventId", eventId,
                "eventBornTime", eventBornTime,
                "snapshotBatchId", report == null ? null : report.getSnapshotBatchId(),
                "rosterEnabled", report == null ? null : report.isRosterEnabled(),
                "rosterPulledUsers", report == null ? null : report.getRosterPulledUsers(),
                "rosterSyncedProfiles", report == null ? null : report.getRosterSyncedProfiles(),
                "rosterUpdatedProfiles", report == null ? null : report.getRosterUpdatedProfiles(),
                "rosterCustomFields", report == null ? null : report.getRosterCustomFields(),
                "rosterSyncFailed", report == null ? null : report.getRosterSyncFailed(),
                "unmatched", report == null ? null : report.getUnmatched(),
                "unchanged", report == null ? null : report.getUnchanged(),
                "syncDetails", report == null ? null : report.getSyncDetails()
        )));
        saveHistory(req);
    }

    private void processEmployeeDirectoryEvent(String eventType, String eventId, Long eventBornTime,
                                               String rawPayload) {
        if (!Boolean.TRUE.equals(dingTalkProperties.getSync().getEnabled())
                || !Boolean.TRUE.equals(dingTalkProperties.getSync().getEmployeeEnabled())) {
            log.info("Skip DingTalk stream employee sync by switch, eventType={}, eventId={}", eventType, eventId);
            return;
        }
        long nowEpochSeconds = Instant.now().getEpochSecond();
        long minIntervalSeconds = Math.max(safeLong(
                dingTalkProperties.getStream().getEmployeeSyncMinIntervalSeconds(), 60L), 1L);
        if (!tryAcquireEmployeeSyncPermit(nowEpochSeconds, minIntervalSeconds)) {
            log.info("Coalesce DingTalk stream employee sync, eventType={}, eventId={}, minIntervalSeconds={}",
                    eventType, eventId, minIntervalSeconds);
            return;
        }

        List<Long> targetTenantIds = resolveEmployeeEventTargetTenantIds(eventType, rawPayload);
        if (targetTenantIds.isEmpty()) {
            log.warn("DingTalk stream employee event skipped: no tenant found, eventType={}, eventId={}",
                    eventType, eventId);
            return;
        }
        try {
            syncEmployeesFromEventForTenants(eventType, eventId, eventBornTime, targetTenantIds);
        } finally {
            lastEmployeeSyncEpochSeconds.set(Instant.now().getEpochSecond());
        }
    }

    private void syncEmployeesFromEventForTenants(String eventType, String eventId, Long eventBornTime,
                                                  List<Long> tenantIds) {
        for (Long tenantId : tenantIds) {
            if (tenantId == null) {
                continue;
            }
            LocalDateTime startTime = LocalDateTime.now();
            try {
                TenantUtils.execute(tenantId, () -> {
                    DingTalkEmployeeSyncService.EmployeeSyncReport report =
                            dingTalkEmployeeSyncService.syncExistingProfiles();
                    LocalDateTime endTime = LocalDateTime.now();
                    saveStreamEmployeeHistory(tenantId, startTime, endTime, eventType, eventId, eventBornTime, report);
                    log.info("DingTalk stream employee sync finished, tenantId={}, eventType={}, eventId={}, eventBornTime={}, "
                                    + "total={}, created={}, updated={}, failed={}, bindings={}, snapshots={}",
                            tenantId, eventType, eventId, eventBornTime, report.getTotalFromDingTalk(),
                            report.getCreated(), report.getUpdated(), report.getCreateFailed(),
                            report.getSyncedBindings(), report.getSnapshotRows());
                });
            } catch (Exception ex) {
                log.error("DingTalk stream employee sync failed, tenantId={}, eventType={}, eventId={}",
                        tenantId, eventType, eventId, ex);
                try {
                    TenantUtils.execute(tenantId, () ->
                            saveStreamEmployeeFailureHistory(tenantId, startTime, eventType, eventId, eventBornTime, ex));
                } catch (Exception historyEx) {
                    log.warn("Save DingTalk stream employee failure history failed, tenantId={}, eventType={}, eventId={}, reason={}",
                            tenantId, eventType, eventId, historyEx.getMessage());
                }
            }
        }
    }

    private void saveStreamEmployeeHistory(Long tenantId, LocalDateTime startTime, LocalDateTime endTime,
                                           String eventType, String eventId, Long eventBornTime,
                                           DingTalkEmployeeSyncService.EmployeeSyncReport report) {
        DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq req =
                new DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq();
        req.setSyncType(TYPE_EMPLOYEE_SYNC);
        req.setSyncScope(SCOPE_USER_PROFILE);
        req.setTriggerMode(TRIGGER_STREAM);
        req.setTargetTenantId(tenantId);
        req.setTotalCount(report == null ? 0 : report.getTotalFromDingTalk());
        req.setPulledCount(report == null ? 0 : report.getTotalFromDingTalk());
        req.setSyncedCount(report == null ? 0 : report.getCreated() + report.getUpdated());
        req.setCreatedCount(report == null ? 0 : report.getCreated());
        req.setUpdatedCount(report == null ? 0 : report.getUpdated());
        req.setFailedCount(report == null ? 0 : report.getCreateFailed());
        req.setSkippedCount(report == null ? 0 : report.getSkippedCreateInvalid()
                + report.getSkippedDuplicateMapping() + report.getSkippedNoMobile() + report.getUnmatched());
        req.setSyncStartTime(startTime);
        req.setSyncEndTime(endTime);
        req.setDurationMs(calculateDurationMs(startTime, endTime));
        req.setSummary(report == null ? "stream employee sync: empty report" : String.format(
                "stream employee sync: created=%d, updated=%d, failed=%d, deptUpdates=%d, jobTitleUpdates=%d, postSyncs=%d, postCreates=%d, roleCreates=%d, roleAssigns=%d, roleRemoves=%d, functionSyncFailed=%d, rosterPulled=%d, rosterSynced=%d, rosterProfileUpdates=%d, rosterCustomFields=%d, rosterFailed=%d, snapshots=%d, cleanedProfiles=%d, cleanedBindings=%d, disabledUsers=%d",
                report.getCreated(), report.getUpdated(), report.getCreateFailed(), report.getSyncedDeptIds(),
                report.getSyncedJobTitles(), report.getSyncedPosts(), report.getCreatedPosts(),
                report.getCreatedRoles(), report.getAssignedRoles(), report.getRemovedRoles(),
                report.getFunctionSyncFailed(), report.getRosterPulledUsers(), report.getRosterSyncedProfiles(),
                report.getRosterUpdatedProfiles(), report.getRosterCustomFields(), report.getRosterSyncFailed(),
                report.getSnapshotRows(), report.getCleanedProfiles(), report.getCleanedBindings(),
                report.getDisabledAdminUsers()));
        req.setDetailJson(toJson(detailMap(
                "eventType", eventType,
                "eventId", eventId,
                "eventBornTime", eventBornTime,
                "snapshotBatchId", report == null ? null : report.getSnapshotBatchId(),
                "snapshotRows", report == null ? null : report.getSnapshotRows(),
                "rosterEnabled", report == null ? null : report.isRosterEnabled(),
                "rosterPulledUsers", report == null ? null : report.getRosterPulledUsers(),
                "rosterSyncedProfiles", report == null ? null : report.getRosterSyncedProfiles(),
                "rosterUpdatedProfiles", report == null ? null : report.getRosterUpdatedProfiles(),
                "rosterCustomFields", report == null ? null : report.getRosterCustomFields(),
                "rosterSyncFailed", report == null ? null : report.getRosterSyncFailed(),
                "syncedBindings", report == null ? null : report.getSyncedBindings(),
                "syncedEntryStatuses", report == null ? null : report.getSyncedEntryStatuses(),
                "syncedDeptIds", report == null ? null : report.getSyncedDeptIds(),
                "syncedJobTitles", report == null ? null : report.getSyncedJobTitles(),
                "syncedPosts", report == null ? null : report.getSyncedPosts(),
                "createdPosts", report == null ? null : report.getCreatedPosts(),
                "createdRoles", report == null ? null : report.getCreatedRoles(),
                "assignedRoles", report == null ? null : report.getAssignedRoles(),
                "removedRoles", report == null ? null : report.getRemovedRoles(),
                "functionSyncFailed", report == null ? null : report.getFunctionSyncFailed(),
                "enabledAdminUsers", report == null ? null : report.getEnabledAdminUsers(),
                "disabledAdminUsers", report == null ? null : report.getDisabledAdminUsers(),
                "cleanedProfiles", report == null ? null : report.getCleanedProfiles(),
                "cleanedBindings", report == null ? null : report.getCleanedBindings(),
                "cleanedUserSyncRows", report == null ? null : report.getCleanedUserSyncRows()
        )));
        saveHistory(req);
    }

    private void saveStreamEmployeeFailureHistory(Long tenantId, LocalDateTime startTime, String eventType,
                                                  String eventId, Long eventBornTime, Exception cause) {
        LocalDateTime endTime = LocalDateTime.now();
        DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq req =
                new DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq();
        req.setSyncType(TYPE_EMPLOYEE_SYNC);
        req.setSyncScope(SCOPE_USER_PROFILE);
        req.setTriggerMode(TRIGGER_STREAM);
        req.setTargetTenantId(tenantId);
        req.setFailedCount(1);
        req.setSyncStartTime(startTime);
        req.setSyncEndTime(endTime);
        req.setDurationMs(calculateDurationMs(startTime, endTime));
        req.setSummary("stream employee sync failed: " + resolveErrorMessage(cause));
        req.setDetailJson(toJson(detailMap(
                "eventType", eventType,
                "eventId", eventId,
                "eventBornTime", eventBornTime,
                "error", resolveErrorMessage(cause)
        )));
        saveHistory(req);
    }

    private void processLeaveEvent(String eventType, String eventId, Long eventBornTime, String rawPayload) {
        Set<String> targetDingUserIds = extractDingUserIds(rawPayload);
        if (targetDingUserIds.isEmpty()) {
            log.warn("DingTalk leave stream event skipped: no user resolved, eventType={}, eventId={}",
                    eventType, eventId);
            return;
        }
        List<Long> targetTenantIds = resolveTargetTenantIds(rawPayload);
        if (targetTenantIds.isEmpty()) {
            log.warn("DingTalk leave stream event skipped: no tenant matched, eventType={}, eventId={}",
                    eventType, eventId);
            return;
        }
        syncLeaveFromEventForTenants(eventType, eventId, eventBornTime, targetDingUserIds, targetTenantIds);
    }

    private void syncLeaveFromEventForTenants(String eventType, String eventId, Long eventBornTime,
                                              Set<String> targetDingUserIds, List<Long> tenantIds) {
        for (Long tenantId : tenantIds) {
            if (tenantId == null) {
                continue;
            }
            LocalDateTime startTime = LocalDateTime.now();
            try {
                TenantUtils.execute(tenantId, () -> {
                    if (!dingTalkSyncConfigService.isLeaveEnabled()) {
                        log.info("Skip DingTalk stream leave sync by switch, tenantId={}, eventType={}, eventId={}",
                                tenantId, eventType, eventId);
                        return;
                    }
                    DingTalkLeaveSyncService.LeaveSyncReport report =
                            dingTalkLeaveSyncService.syncAroundEvent(eventBornTime, targetDingUserIds);
                    LocalDateTime endTime = LocalDateTime.now();
                    saveStreamLeaveHistory(tenantId, startTime, endTime, eventType, eventId, eventBornTime, report);
                    log.info("DingTalk stream leave sync finished, tenantId={}, eventType={}, eventId={}, eventBornTime={}, report={}",
                            tenantId, eventType, eventId, eventBornTime, JsonUtils.toJsonString(report));
                });
            } catch (Exception ex) {
                log.error("DingTalk stream leave sync failed, tenantId={}, eventType={}, eventId={}",
                        tenantId, eventType, eventId, ex);
                saveStreamFailureHistory(TYPE_LEAVE_SYNC, SCOPE_LEAVE, tenantId, startTime,
                        eventType, eventId, eventBornTime, ex);
            }
        }
    }

    private void saveStreamLeaveHistory(Long tenantId, LocalDateTime startTime, LocalDateTime endTime,
                                        String eventType, String eventId, Long eventBornTime,
                                        DingTalkLeaveSyncService.LeaveSyncReport report) {
        DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq req =
                newStreamHistoryReq(TYPE_LEAVE_SYNC, SCOPE_LEAVE, tenantId, startTime, endTime);
        req.setTotalCount(report == null ? 0 : report.getCheckedDays());
        req.setPulledCount(report == null ? 0 : report.getPulledLeaveDays());
        req.setSyncedCount(report == null ? 0 : report.getSyncedLeaves());
        req.setCreatedCount(report == null ? 0 : report.getCreatedLeaves());
        req.setUpdatedCount(report == null ? 0 : report.getUpdatedLeaves());
        req.setFailedCount(report == null ? 0 : report.getFailedCalls());
        req.setSkippedCount(report == null ? 0 : report.getSkippedUsers());
        req.setSummary(report == null ? "stream leave sync: empty report" : String.format(
                "stream leave sync: mapped=%d, checkedDays=%d, leaveDays=%d, created=%d, updated=%d, cancelled=%d, failed=%d",
                report.getMappedUsers(), report.getCheckedDays(), report.getPulledLeaveDays(),
                report.getCreatedLeaves(), report.getUpdatedLeaves(), report.getCancelledLeaves(),
                report.getFailedCalls()));
        req.setDetailJson(toJson(detailMap(
                "eventType", eventType,
                "eventId", eventId,
                "eventBornTime", eventBornTime,
                "fromDate", report == null ? null : report.getFromDate(),
                "toDate", report == null ? null : report.getToDate(),
                "syncMode", report == null ? null : report.getSyncMode(),
                "apiCalls", report == null ? null : report.getApiCalls()
        )));
        saveHistory(req);
    }

    private DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq newStreamHistoryReq(String syncType,
                                                                                      String syncScope,
                                                                                      Long tenantId,
                                                                                      LocalDateTime startTime,
                                                                                      LocalDateTime endTime) {
        DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq req =
                new DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq();
        req.setSyncType(syncType);
        req.setSyncScope(syncScope);
        req.setTriggerMode(TRIGGER_STREAM);
        req.setTargetTenantId(tenantId);
        req.setSyncStartTime(startTime);
        req.setSyncEndTime(endTime);
        req.setDurationMs(calculateDurationMs(startTime, endTime));
        return req;
    }

    private void saveStreamFailureHistory(String syncType, String syncScope, Long tenantId,
                                          LocalDateTime startTime, String eventType, String eventId,
                                          Long eventBornTime, Exception cause) {
        LocalDateTime endTime = LocalDateTime.now();
        DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq req =
                newStreamHistoryReq(syncType, syncScope, tenantId, startTime, endTime);
        req.setFailedCount(1);
        req.setSummary("stream " + syncScope + " sync failed: " + resolveErrorMessage(cause));
        req.setDetailJson(toJson(detailMap(
                "eventType", eventType,
                "eventId", eventId,
                "eventBornTime", eventBornTime,
                "error", resolveErrorMessage(cause)
        )));
        saveHistory(req);
    }

    private List<Long> resolveEmployeeEventTargetTenantIds(String eventType, String rawPayload) {
        if (isDepartmentDirectoryEvent(eventType)) {
            return resolveAllTenantIds();
        }
        List<Long> tenantIds = resolveTargetTenantIds(rawPayload);
        return tenantIds.isEmpty() ? resolveAllTenantIds() : tenantIds;
    }

    private List<Long> resolveAllTenantIds() {
        List<Long> tenantIds = tenantFrameworkService.getTenantIds();
        if (tenantIds == null || tenantIds.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<Long> result = new LinkedHashSet<>();
        for (Long tenantId : tenantIds) {
            if (tenantId != null && tenantId > 0) {
                result.add(tenantId);
            }
        }
        return new ArrayList<>(result);
    }

    private List<Long> resolveTargetTenantIds(String rawPayload) {
        Set<String> dingUserIds = extractDingUserIds(rawPayload);
        if (dingUserIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<DingTalkUserBindingDO> bindings = TenantUtils.executeIgnore(
                () -> dingTalkUserBindingMapper.selectListByDingUserIds(dingUserIds)
        );
        if (bindings == null || bindings.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<Long> tenantIds = new LinkedHashSet<>();
        for (DingTalkUserBindingDO binding : bindings) {
            if (binding == null || binding.getTenantId() == null || binding.getTenantId() <= 0) {
                continue;
            }
            tenantIds.add(binding.getTenantId());
        }
        return new ArrayList<>(tenantIds);
    }

    private Map<Long, Set<String>> resolveRosterEventUserIdsByTenant(Set<String> dingUserIds) {
        if (dingUserIds == null || dingUserIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<DingTalkUserBindingDO> bindings = TenantUtils.executeIgnore(
                () -> dingTalkUserBindingMapper.selectListByDingUserIds(dingUserIds)
        );
        if (bindings == null || bindings.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Set<String>> result = new LinkedHashMap<>();
        for (DingTalkUserBindingDO binding : bindings) {
            String dingUserId = trim(binding == null ? null : binding.getDingUserId());
            Long tenantId = binding == null ? null : binding.getTenantId();
            if (!StringUtils.hasText(dingUserId) || tenantId == null || tenantId <= 0) {
                continue;
            }
            if (!dingUserIds.contains(dingUserId)) {
                continue;
            }
            result.computeIfAbsent(tenantId, key -> new LinkedHashSet<>()).add(dingUserId);
        }
        return result;
    }

    private Set<String> extractDingUserIds(String rawPayload) {
        if (!StringUtils.hasText(rawPayload)) {
            return Collections.emptySet();
        }
        JsonNode root = JsonUtils.parseTree(rawPayload);
        if (root == null || root.isNull() || root.isMissingNode()) {
            return Collections.emptySet();
        }
        Set<String> result = new LinkedHashSet<>();
        collectDingUserIds(root, result, 0);
        return result;
    }

    private void collectDingUserIds(JsonNode node, Set<String> result, int depth) {
        if (node == null || node.isNull() || depth > 8) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                collectDingUserIds(child, result, depth + 1);
            }
            return;
        }
        if (!node.isObject()) {
            return;
        }

        String userId = text(node, "userId", "userid", "UserId", "UserID", "staffId", "staff_id",
                "employeeId", "employeeUserId", "changedUserId", "targetUserId", "personnelUserId",
                "operatorUserId", "operatorUserid", "originatorUserId", "originatorUserid",
                "approverUserId", "approverUserid");
        if (StringUtils.hasText(userId)) {
            result.add(userId.trim());
        }
        collectDingUserIdsFromField(node, result, depth,
                "userIds", "userids", "userIdList", "useridList", "user_id_list",
                "staffIds", "staffIdList", "staff_id_list", "employeeIds", "employeeIdList",
                "employeeUserIds", "employeeUserIdList", "changedUserIds", "changedUserIdList",
                "targetUserIds", "targetUserIdList");

        JsonNode preferred = firstPresentNode(node, "dataList", "recordresult", "recordResult", "records", "recordList",
                "checkRecordList", "list", "result", "data");
        if (preferred != null) {
            collectDingUserIds(preferred, result, depth + 1);
            return;
        }
        for (JsonNode child : node) {
            collectDingUserIds(child, result, depth + 1);
        }
    }

    private void collectDingUserIdsFromField(JsonNode node, Set<String> result, int depth, String... fieldNames) {
        if (node == null || fieldNames == null || fieldNames.length == 0) {
            return;
        }
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (value.isMissingNode() || value.isNull()) {
                continue;
            }
            collectDingUserIdFieldValue(value, result, depth + 1);
        }
    }

    private void collectDingUserIdFieldValue(JsonNode value, Set<String> result, int depth) {
        if (value == null || value.isNull() || depth > 8) {
            return;
        }
        if (value.isArray()) {
            for (JsonNode child : value) {
                collectDingUserIdFieldValue(child, result, depth + 1);
            }
            return;
        }
        if (value.isValueNode()) {
            String text = trim(value.asText(""));
            if (StringUtils.hasText(text)) {
                result.add(text);
            }
            return;
        }
        collectDingUserIds(value, result, depth + 1);
    }

    private JsonNode firstPresentNode(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.path(name);
            if (!value.isMissingNode() && !value.isNull()) {
                return value;
            }
        }
        return null;
    }

    private String text(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.path(name);
            if (!value.isMissingNode() && !value.isNull()) {
                String text = value.asText("");
                if (StringUtils.hasText(text)) {
                    return text.trim();
                }
            }
        }
        return null;
    }

    private boolean tryAcquireSyncPermit(long nowEpochSeconds, long minIntervalSeconds) {
        while (true) {
            long last = lastAttendanceSyncEpochSeconds.get();
            if (nowEpochSeconds - last < minIntervalSeconds) {
                return false;
            }
            if (lastAttendanceSyncEpochSeconds.compareAndSet(last, nowEpochSeconds)) {
                return true;
            }
        }
    }

    private boolean tryAcquireEmployeeSyncPermit(long nowEpochSeconds, long minIntervalSeconds) {
        while (true) {
            long last = lastEmployeeSyncEpochSeconds.get();
            if (nowEpochSeconds - last < minIntervalSeconds) {
                return false;
            }
            if (lastEmployeeSyncEpochSeconds.compareAndSet(last, nowEpochSeconds)) {
                return true;
            }
        }
    }

    private void syncAttendanceForAllTenants(String eventType, String eventId, Long eventBornTime) {
        long lookbackMinutes = Math.max(safeLong(dingTalkProperties.getStream().getAttendanceSyncLookbackMinutes(), 15L), 1L);
        LocalDateTime to = LocalDateTime.now();
        LocalDateTime from = to.minusMinutes(lookbackMinutes);

        List<Long> tenantIds = tenantFrameworkService.getTenantIds();
        if (tenantIds == null || tenantIds.isEmpty()) {
            log.warn("Skip DingTalk stream attendance sync: no tenant found, eventType={}, eventId={}", eventType, eventId);
            return;
        }

        for (Long tenantId : tenantIds) {
            if (tenantId == null) {
                continue;
            }
            try {
                TenantUtils.execute(tenantId, () -> {
                    DingTalkAttendanceSyncService.AttendanceSyncReport report = dingTalkAttendanceSyncService.syncByTimeRange(from, to);
                    log.info("DingTalk stream attendance sync finished, tenantId={}, eventType={}, eventId={}, eventBornTime={}, from={}, to={}, report={}",
                            tenantId, eventType, eventId, eventBornTime, from, to, JsonUtils.toJsonString(report));
                });
            } catch (Exception ex) {
                log.error("DingTalk stream attendance sync failed, tenantId={}, eventType={}, eventId={}",
                        tenantId, eventType, eventId, ex);
            }
        }
    }

    private boolean isRosterEvent(String eventType, String rawPayload) {
        return containsRosterSignal(eventType) || containsRosterSignal(rawPayload);
    }

    private boolean containsRosterSignal(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        return normalized.contains("hrm_user_record_change")
                || normalized.contains("hrm_mdm_user_info_change")
                || normalized.contains("hrm.user.record.change")
                || normalized.contains("hrm.mdm.user.info.change")
                || normalized.contains("mdm_user_info_change")
                || normalized.contains("hrm")
                || normalized.contains("roster")
                || normalized.contains("personnel")
                || normalized.contains("employee_file")
                || normalized.contains("employee-file")
                || normalized.contains("employee file")
                || normalized.contains("staff_file")
                || normalized.contains("staff-file")
                || normalized.contains("staff file")
                || normalized.contains("employee_profile")
                || normalized.contains("employee-profile")
                || normalized.contains("employee profile")
                || normalized.contains("user_record")
                || normalized.contains("user-record")
                || normalized.contains("user record")
                || text.contains("智能人事")
                || text.contains("人事平台")
                || text.contains("员工档案")
                || text.contains("花名册")
                || text.contains("员工信息变更");
    }

    private boolean isAttendanceEvent(String eventType) {
        if (!StringUtils.hasText(eventType)) {
            return false;
        }
        String normalized = eventType.toLowerCase(Locale.ROOT);
        return normalized.contains("attendance") || normalized.contains("check_record") || normalized.contains("checkrecord");
    }

    private boolean isEmployeeDirectoryEvent(String eventType) {
        return isUserDirectoryEvent(eventType) || isDepartmentDirectoryEvent(eventType);
    }

    private boolean isUserDirectoryEvent(String eventType) {
        if (!StringUtils.hasText(eventType)) {
            return false;
        }
        String normalized = eventType.toLowerCase(Locale.ROOT);
        return (normalized.contains("user") && normalized.contains("org"))
                || normalized.contains("org_user")
                || normalized.contains("address_book_user")
                || normalized.contains("contact_user");
    }

    private boolean isDepartmentDirectoryEvent(String eventType) {
        if (!StringUtils.hasText(eventType)) {
            return false;
        }
        String normalized = eventType.toLowerCase(Locale.ROOT);
        return ((normalized.contains("dept") || normalized.contains("department"))
                && (normalized.contains("org") || normalized.contains("address_book")
                || normalized.contains("contact")));
    }

    private boolean isLeaveEvent(String eventType, String rawPayload) {
        if (isEmployeeDirectoryEvent(eventType)) {
            return false;
        }
        String normalized = (eventType == null ? "" : eventType).toLowerCase(Locale.ROOT);
        if (normalized.contains("leave") || normalized.contains("vacation") || normalized.contains("bpms")) {
            return true;
        }
        return containsLeaveSignal(rawPayload);
    }

    private boolean containsLeaveSignal(String rawPayload) {
        if (!StringUtils.hasText(rawPayload)) {
            return false;
        }
        String normalized = rawPayload.toLowerCase(Locale.ROOT);
        return normalized.contains("leave")
                || normalized.contains("vacation")
                || rawPayload.contains("请假")
                || rawPayload.contains("调休")
                || rawPayload.contains("假勤");
    }

    private String resolveEventType(GenericOpenDingTalkEvent event) {
        String eventType = trim(event.getEventType());
        if (StringUtils.hasText(eventType)) {
            return eventType;
        }
        if (event.getData() == null) {
            return "";
        }
        eventType = trim(event.getData().getString("EventType"));
        if (StringUtils.hasText(eventType)) {
            return eventType;
        }
        eventType = trim(event.getData().getString("eventType"));
        return eventType == null ? "" : eventType;
    }

    private ExecutorService newSingleThreadExecutor(String threadName) {
        return Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName(threadName);
            thread.setDaemon(true);
            return thread;
        });
    }

    private void shutdownExecutor(ExecutorService executorService) {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(3, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private int safeInt(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private long safeLong(Long value, long defaultValue) {
        return value == null ? defaultValue : value;
    }

    private long calculateDurationMs(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return 0L;
        }
        return Math.max(Duration.between(startTime, endTime).toMillis(), 0L);
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
            return JsonUtils.toJsonString(obj);
        } catch (Exception ex) {
            return null;
        }
    }

    private void saveHistory(DingTalkSyncHistoryService.DingTalkSyncHistorySaveReq req) {
        try {
            dingTalkSyncHistoryService.save(req);
        } catch (Exception ex) {
            log.warn("Save DingTalk stream history failed, syncType={}, tenantId={}, reason={}",
                    req == null ? null : req.getSyncType(),
                    req == null ? null : req.getTargetTenantId(),
                    ex.getMessage());
        }
    }

    private String resolveErrorMessage(Exception ex) {
        if (ex == null) {
            return null;
        }
        Throwable cause = ex;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = trim(cause.getMessage());
        if (!StringUtils.hasText(message)) {
            message = trim(ex.getMessage());
        }
        return StringUtils.hasText(message) ? message : ex.getClass().getSimpleName();
    }

    private void logStreamSwitchDiagnose() {
        String resolved = environment.getProperty("dingtalk.stream.enabled");
        String systemProperty = System.getProperty("dingtalk.stream.enabled");
        String envVariable = System.getenv("DINGTALK_STREAM_ENABLED");
        StringBuilder sourceDetail = new StringBuilder();
        for (PropertySource<?> source : environment.getPropertySources()) {
            Object value = source.getProperty("dingtalk.stream.enabled");
            if (value != null) {
                if (sourceDetail.length() > 0) {
                    sourceDetail.append("; ");
                }
                sourceDetail.append(source.getName()).append("=").append(value);
            }
        }
        log.warn("DingTalk stream switch diagnose: activeProfiles={}, resolved={}, sysProp={}, envVar={}, sources={}",
                Arrays.toString(environment.getActiveProfiles()), resolved, systemProperty, envVariable, sourceDetail.toString());
    }

    private void logEventPayload(String eventType, GenericOpenDingTalkEvent event) {
        String rawData = event.getData() == null ? "{}" : event.getData().toString();
        String payload = abbreviate(rawData, EVENT_LOG_MAX_LENGTH);
        if (!log.isDebugEnabled()) {
            return;
        }
        log.debug("DingTalk stream event payload, eventType={}, eventId={}, corpId={}, bornTime={}, data={}",
                eventType, trim(event.getEventId()), trim(event.getEventCorpId()), event.getEventBornTime(), payload);
    }

    private String abbreviate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...(truncated,total=" + text.length() + ")";
    }

    private static class DirectSyncSummary {
        private int totalMappedUsers;
        private int totalPulledRecords;
        private int totalSyncedRecords;
        private int totalSkippedRecords;
    }

}
