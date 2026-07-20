package com.kyx.service.hr.integration.dingtalk.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.kyx.service.hr.config.DingTalkProperties;
import com.kyx.service.hr.dal.dataobject.administrative.HrAdministrativeLeaveDO;
import com.kyx.service.hr.dal.mysql.administrative.HrAdministrativeLeaveMapper;
import com.kyx.service.hr.integration.dingtalk.client.DingTalkOpenApiClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sync approved DingTalk leave duration into local HR leave table.
 */
@Service
@Slf4j
public class DingTalkLeaveSyncService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String PROCESS_INSTANCE_PREFIX = "dingtalk-leave-";
    private static final int MAX_BATCH_USER_SIZE = 100;
    private static final int LEAVE_STATUS_PAGE_SIZE = 20;
    private static final int MAX_DURATION_FALLBACK_CALLS = 300;
    private static final int WORKDAY_MINUTES = 480;
    private static final int STATUS_APPROVE = 2;

    @Resource
    private DingTalkOpenApiClient dingTalkOpenApiClient;
    @Resource
    private DingTalkUserBindingService dingTalkUserBindingService;
    @Resource
    private HrAdministrativeLeaveMapper hrAdministrativeLeaveMapper;
    @Resource
    private DingTalkProperties dingTalkProperties;

    public LeaveSyncReport syncByDateRange(LocalDate from, LocalDate to) {
        return syncByDateRange(from, to, null);
    }

    public LeaveSyncReport syncByDateRange(LocalDate from, LocalDate to, Collection<String> targetDingUserIds) {
        LocalDate startDate = from == null ? LocalDate.now() : from;
        LocalDate endDate = to == null ? startDate : to;
        if (endDate.isBefore(startDate)) {
            LocalDate temp = startDate;
            startDate = endDate;
            endDate = temp;
        }

        Map<String, Long> mapping = dingTalkUserBindingService.getCachedDingUserToOaUserMap();
        if (mapping == null || mapping.isEmpty()) {
            mapping = dingTalkUserBindingService.refreshDingUserToOaUserMap();
        }

        LeaveSyncReport report = new LeaveSyncReport();
        report.setFromDate(startDate.toString());
        report.setToDate(endDate.toString());
        report.setMappedUsers(mapping == null ? 0 : mapping.size());
        if (mapping == null || mapping.isEmpty()) {
            return report;
        }

        Set<String> targetSet = normalizeTargetDingUserIds(targetDingUserIds);
        Map<String, Long> targetMapping = filterTargetMapping(mapping, targetSet, report);
        if (targetMapping.isEmpty()) {
            return report;
        }

        try {
            report.setSyncMode("BATCH_STATUS");
            syncByBatchLeaveStatus(targetMapping, startDate, endDate, report);
        } catch (Exception ex) {
            int estimatedFallbackCalls = estimateFallbackCalls(targetMapping, startDate, endDate);
            if (estimatedFallbackCalls > MAX_DURATION_FALLBACK_CALLS) {
                log.warn("Batch DingTalk leave status sync failed and fallback skipped, estimatedCalls={}, reason={}",
                        estimatedFallbackCalls, ex.getMessage());
                throw new IllegalStateException("Batch DingTalk leave status sync failed, fallback skipped to avoid "
                        + estimatedFallbackCalls + " DingTalk API calls", ex);
            }
            log.warn("Batch DingTalk leave status sync failed, fallback to duration API, reason={}", ex.getMessage());
            report.setSyncMode("DURATION_FALLBACK");
            syncByDurationFallback(targetMapping, startDate, endDate, report);
        }
        log.info("DingTalk leave sync finished: {}", report);
        return report;
    }

    public LeaveSyncReport syncAroundEvent(Long eventBornTime, Collection<String> targetDingUserIds) {
        LocalDate baseDate = eventBornTime == null
                ? LocalDate.now()
                : LocalDateTime.ofInstant(Instant.ofEpochMilli(normalizeEpochMillis(eventBornTime)), ZoneId.systemDefault())
                .toLocalDate();
        int lookbackDays = Math.max(safeInteger(dingTalkProperties.getStream().getLeaveSyncLookbackDays(), 3), 0);
        int forwardDays = Math.max(safeInteger(dingTalkProperties.getStream().getLeaveSyncForwardDays(), 0), 0);
        LocalDate latestAllowedDate = LocalDate.now();
        LocalDate startDate = baseDate.minusDays(lookbackDays);
        LocalDate endDate = minDate(baseDate.plusDays(forwardDays), latestAllowedDate);
        if (endDate.isBefore(startDate)) {
            startDate = endDate;
        }
        return syncByDateRange(startDate, endDate, targetDingUserIds);
    }

    private Map<String, Long> filterTargetMapping(Map<String, Long> mapping, Set<String> targetSet, LeaveSyncReport report) {
        Map<String, Long> targetMapping = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : mapping.entrySet()) {
            String dingUserId = entry.getKey();
            Long userId = entry.getValue();
            if (!StringUtils.hasText(dingUserId) || userId == null) {
                report.addSkippedUser();
                continue;
            }
            String normalizedDingUserId = dingUserId.trim();
            if (!targetSet.isEmpty() && !targetSet.contains(normalizedDingUserId)) {
                continue;
            }
            targetMapping.put(normalizedDingUserId, userId);
        }
        return targetMapping;
    }

    private void syncByBatchLeaveStatus(Map<String, Long> targetMapping, LocalDate startDate, LocalDate endDate,
                                        LeaveSyncReport report) {
        List<String> dingUserIds = new ArrayList<>(targetMapping.keySet());
        long days = Math.max(ChronoUnit.DAYS.between(startDate, endDate) + 1, 0L);
        report.addCheckedDays(safeLongToInt(days * dingUserIds.size()));

        Set<String> leaveDayKeys = new HashSet<>();
        for (int index = 0; index < dingUserIds.size(); index += MAX_BATCH_USER_SIZE) {
            List<String> batch = dingUserIds.subList(index, Math.min(index + MAX_BATCH_USER_SIZE, dingUserIds.size()));
            Map<String, LeaveDayAggregate> aggregates = fetchLeaveStatusAggregates(batch, targetMapping, startDate, endDate, report);
            for (LeaveDayAggregate aggregate : aggregates.values()) {
                if (aggregate == null || aggregate.minutes <= 0 || aggregate.userId == null || aggregate.date == null) {
                    continue;
                }
                Integer approvedMinutes = fetchLeaveMinutesWithReport(aggregate.dingUserId, aggregate.date, report,
                        "Verify DingTalk leave status");
                sleepQuietly(resolveLeaveBatchIntervalMs());
                if (approvedMinutes == null) {
                    continue;
                }
                if (approvedMinutes <= 0) {
                    if (removeAutoSyncedLeaveDayIfExists(aggregate.dingUserId, aggregate.date)) {
                        report.addCancelledLeave();
                    }
                    continue;
                }
                aggregate.minutes = resolveSyncedLeaveMinutes(aggregate, approvedMinutes);
                leaveDayKeys.add(buildLeaveDayKey(aggregate.dingUserId, aggregate.date));
                if (upsertLeaveDay(aggregate)) {
                    report.addCreatedLeave();
                } else {
                    report.addUpdatedLeave();
                }
                report.addPulledLeaveDay();
            }
        }

        cancelMissingLeaveDays(targetMapping.keySet(), startDate, endDate, leaveDayKeys, report);
    }

    private Map<String, LeaveDayAggregate> fetchLeaveStatusAggregates(List<String> dingUserIds,
                                                                      Map<String, Long> targetMapping,
                                                                      LocalDate startDate,
                                                                      LocalDate endDate,
                                                                      LeaveSyncReport report) {
        Map<String, LeaveDayAggregate> aggregates = new LinkedHashMap<>();
        int offset = 0;
        while (true) {
            Map<String, Object> body = dingTalkOpenApiClient.body();
            body.put("userid_list", String.join(",", dingUserIds));
            body.put("start_time", toEpochMillis(startDate.atStartOfDay()));
            body.put("end_time", toEpochMillis(endDate.atTime(23, 59, 59)));
            body.put("offset", offset);
            body.put("size", LEAVE_STATUS_PAGE_SIZE);
            report.addApiCall();
            JsonNode root = dingTalkOpenApiClient.postTopApiWithRetry("/topapi/attendance/getleavestatus", body);

            JsonNode result = root.path("result");
            JsonNode leaveStatusList = result.path("leave_status");
            if (leaveStatusList.isArray()) {
                for (JsonNode node : leaveStatusList) {
                    mergeLeaveStatus(aggregates, targetMapping, startDate, endDate, node);
                }
            }

            boolean hasMore = result.path("has_more").asBoolean(false);
            if (!hasMore) {
                break;
            }
            offset += LEAVE_STATUS_PAGE_SIZE;
            sleepQuietly(resolveLeaveBatchIntervalMs());
        }
        return aggregates;
    }

    private void mergeLeaveStatus(Map<String, LeaveDayAggregate> aggregates, Map<String, Long> targetMapping,
                                  LocalDate rangeStart, LocalDate rangeEnd, JsonNode node) {
        String dingUserId = readText(node, "userid");
        if (!StringUtils.hasText(dingUserId)) {
            return;
        }
        dingUserId = dingUserId.trim();
        Long userId = targetMapping.get(dingUserId);
        if (userId == null) {
            return;
        }

        LocalDateTime startTime = toLocalDateTime(readLong(node, "start_time"));
        LocalDateTime endTime = toLocalDateTime(readLong(node, "end_time"));
        LocalDate date = startTime == null ? null : startTime.toLocalDate();
        if (date == null) {
            return;
        }
        if ((rangeStart != null && date.isBefore(rangeStart)) || (rangeEnd != null && date.isAfter(rangeEnd))) {
            log.debug("Skip DingTalk leave node outside sync range, dingUserId={}, date={}, rangeStart={}, rangeEnd={}",
                    dingUserId, date, rangeStart, rangeEnd);
            return;
        }
        int minutes = resolveLeaveStatusMinutes(node, startTime, endTime);
        if (minutes <= 0) {
            return;
        }

        final String finalDingUserId = dingUserId;
        final Long finalUserId = userId;
        final LocalDate finalDate = date;
        String key = buildLeaveDayKey(finalDingUserId, finalDate);
        LeaveDayAggregate aggregate = aggregates.computeIfAbsent(key, ignored -> {
            LeaveDayAggregate value = new LeaveDayAggregate();
            value.dingUserId = finalDingUserId;
            value.userId = finalUserId;
            value.date = finalDate;
            return value;
        });
        aggregate.minutes += minutes;
        if (aggregate.startTime == null || (startTime != null && startTime.isBefore(aggregate.startTime))) {
            aggregate.startTime = startTime;
        }
        if (aggregate.endTime == null || (endTime != null && endTime.isAfter(aggregate.endTime))) {
            aggregate.endTime = endTime;
        }
        String leaveCode = readText(node, "leave_code");
        if (StringUtils.hasText(leaveCode)) {
            aggregate.leaveCode = leaveCode;
        }
    }

    private int estimateFallbackCalls(Map<String, Long> targetMapping, LocalDate startDate, LocalDate endDate) {
        long userCount = targetMapping == null ? 0L : targetMapping.size();
        long days = Math.max(ChronoUnit.DAYS.between(startDate, endDate) + 1, 0L);
        return safeLongToInt(userCount * days);
    }

    private void cancelMissingLeaveDays(Collection<String> dingUserIds, LocalDate startDate, LocalDate endDate,
                                        Set<String> leaveDayKeys, LeaveSyncReport report) {
        for (String dingUserId : dingUserIds) {
            if (!StringUtils.hasText(dingUserId)) {
                continue;
            }
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                if (!leaveDayKeys.contains(buildLeaveDayKey(dingUserId, date))
                        && removeMissingLeaveDayIfNoApprovedDuration(dingUserId, date, report)) {
                    report.addCancelledLeave();
                }
            }
        }
    }

    private void syncByDurationFallback(Map<String, Long> targetMapping, LocalDate startDate, LocalDate endDate,
                                        LeaveSyncReport report) {
        for (Map.Entry<String, Long> entry : targetMapping.entrySet()) {
            syncUserLeaveDays(entry.getKey(), entry.getValue(), startDate, endDate, report);
        }
    }

    private void syncUserLeaveDays(String dingUserId, Long userId, LocalDate startDate, LocalDate endDate,
                                   LeaveSyncReport report) {
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            try {
                report.addCheckedDay();
                Integer minutes = fetchLeaveMinutesWithReport(dingUserId, date, report, "Sync DingTalk leave day");
                if (minutes == null) {
                    continue;
                }
                if (minutes > 0) {
                    report.addPulledLeaveDay();
                    if (upsertLeaveDay(dingUserId, userId, date, minutes)) {
                        report.addCreatedLeave();
                    } else {
                        report.addUpdatedLeave();
                    }
                } else if (removeAutoSyncedLeaveDayIfExists(dingUserId, date)) {
                    report.addCancelledLeave();
                }
                sleepQuietly(resolveLeaveBatchIntervalMs());
            } catch (Exception ex) {
                report.addFailedCall();
                log.warn("Sync DingTalk leave day failed, dingUserId={}, userId={}, date={}, reason={}",
                        dingUserId, userId, date, ex.getMessage());
            }
        }
    }

    private int fetchLeaveMinutes(String dingUserId, LocalDate date) {
        Map<String, Object> body = dingTalkOpenApiClient.body();
        body.put("userid", dingUserId);
        body.put("from_date", date.atStartOfDay().format(DATETIME_FORMATTER));
        body.put("to_date", date.atTime(23, 59, 59).format(DATETIME_FORMATTER));
        JsonNode root = dingTalkOpenApiClient.postTopApiWithRetry("/topapi/attendance/getleaveapproveduration", body);
        JsonNode value = root.path("result").path("duration_in_minutes");
        if (value.isNumber()) {
            return Math.max(value.asInt(), 0);
        }
        if (value.isTextual()) {
            try {
                return Math.max(Integer.parseInt(value.asText()), 0);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private Integer fetchLeaveMinutesWithReport(String dingUserId, LocalDate date, LeaveSyncReport report, String action) {
        try {
            report.addApiCall();
            return fetchLeaveMinutes(dingUserId, date);
        } catch (Exception ex) {
            report.addFailedCall();
            log.warn("{}, dingUserId={}, date={}, reason={}", action, dingUserId, date, ex.getMessage());
            return null;
        }
    }

    private boolean upsertLeaveDay(LeaveDayAggregate aggregate) {
        String processInstanceId = buildProcessInstanceId(aggregate.dingUserId, aggregate.date);
        HrAdministrativeLeaveDO existed = hrAdministrativeLeaveMapper.selectByProcessInstanceId(processInstanceId);
        HrAdministrativeLeaveDO leave = buildLeaveDO(aggregate.dingUserId, aggregate.userId, aggregate.date,
                aggregate.minutes, processInstanceId, aggregate.startTime, aggregate.endTime, aggregate.leaveCode);
        if (existed == null) {
            hrAdministrativeLeaveMapper.insert(leave);
            return true;
        }
        leave.setId(existed.getId());
        hrAdministrativeLeaveMapper.updateById(leave);
        return false;
    }

    private boolean upsertLeaveDay(String dingUserId, Long userId, LocalDate date, int minutes) {
        String processInstanceId = buildProcessInstanceId(dingUserId, date);
        HrAdministrativeLeaveDO existed = hrAdministrativeLeaveMapper.selectByProcessInstanceId(processInstanceId);
        HrAdministrativeLeaveDO leave = buildLeaveDO(dingUserId, userId, date, minutes, processInstanceId, null, null, null);
        if (existed == null) {
            hrAdministrativeLeaveMapper.insert(leave);
            return true;
        }
        leave.setId(existed.getId());
        hrAdministrativeLeaveMapper.updateById(leave);
        return false;
    }

    private boolean removeMissingLeaveDayIfNoApprovedDuration(String dingUserId, LocalDate date, LeaveSyncReport report) {
        HrAdministrativeLeaveDO existed = hrAdministrativeLeaveMapper.selectByProcessInstanceId(
                buildProcessInstanceId(dingUserId, date));
        if (existed == null) {
            return false;
        }
        Integer approvedMinutes = fetchLeaveMinutesWithReport(dingUserId, date, report,
                "Verify missing DingTalk leave day");
        sleepQuietly(resolveLeaveBatchIntervalMs());
        if (approvedMinutes == null || approvedMinutes > 0) {
            return false;
        }
        return removeAutoSyncedLeaveDayIfExists(dingUserId, date);
    }

    private boolean removeAutoSyncedLeaveDayIfExists(String dingUserId, LocalDate date) {
        HrAdministrativeLeaveDO existed = hrAdministrativeLeaveMapper.selectByProcessInstanceId(
                buildProcessInstanceId(dingUserId, date));
        if (existed == null || existed.getId() == null) {
            return false;
        }
        return hrAdministrativeLeaveMapper.deleteById(existed.getId()) > 0;
    }

    private HrAdministrativeLeaveDO buildLeaveDO(String dingUserId, Long userId, LocalDate date, int minutes,
                                                 String processInstanceId, LocalDateTime sourceStartTime,
                                                 LocalDateTime sourceEndTime, String leaveCode) {
        int normalizedMinutes = normalizeLeaveMinutes(minutes, sourceStartTime, sourceEndTime, dingUserId, date);
        LocalDateTime startTime = sourceStartTime == null ? date.atTime(LocalTime.of(9, 0)) : sourceStartTime;
        LocalDateTime endTime = sourceEndTime == null ? startTime.plusMinutes(normalizedMinutes) : sourceEndTime;
        LocalDateTime latestEndTime = date.atTime(23, 59, 59);
        if (endTime.isAfter(latestEndTime)) {
            endTime = latestEndTime;
        }

        HrAdministrativeLeaveDO leave = new HrAdministrativeLeaveDO();
        leave.setUserId(userId);
        leave.setLeaveCategory("leave");
        leave.setLeaveType("dingtalk");
        leave.setStartTime(startTime);
        leave.setEndTime(endTime);
        leave.setDuration(BigDecimal.valueOf(normalizedMinutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP));
        leave.setEmergencyPhone("-");
        leave.setWorkHandover("-");
        leave.setRemark("DingTalk approved leave auto sync, dingUserId=" + dingUserId
                + ", minutes=" + normalizedMinutes
                + (StringUtils.hasText(leaveCode) ? ", leaveCode=" + leaveCode : ""));
        leave.setStatus(STATUS_APPROVE);
        leave.setProcessInstanceId(processInstanceId);
        return leave;
    }

    private int normalizeLeaveMinutes(int minutes, LocalDateTime sourceStartTime, LocalDateTime sourceEndTime,
                                      String dingUserId, LocalDate date) {
        int normalizedMinutes = Math.max(minutes, 0);
        if (sourceStartTime == null || sourceEndTime == null || !sourceEndTime.isAfter(sourceStartTime)) {
            return normalizedMinutes;
        }

        int intervalMinutes = safeLongToInt(ChronoUnit.MINUTES.between(sourceStartTime, sourceEndTime));
        if (intervalMinutes > 0 && normalizedMinutes > intervalMinutes) {
            log.warn("DingTalk leave duration exceeds source interval, dingUserId={}, date={}, durationMinutes={}, "
                            + "intervalMinutes={}, startTime={}, endTime={}",
                    dingUserId, date, normalizedMinutes, intervalMinutes, sourceStartTime, sourceEndTime);
            return intervalMinutes;
        }
        return normalizedMinutes;
    }

    private String buildProcessInstanceId(String dingUserId, LocalDate date) {
        String seed = dingUserId + ":" + date;
        String hash = DigestUtils.md5DigestAsHex(seed.getBytes(StandardCharsets.UTF_8)).substring(0, 16);
        return PROCESS_INSTANCE_PREFIX + hash + "-" + date;
    }

    private String buildLeaveDayKey(String dingUserId, LocalDate date) {
        return dingUserId + "|" + date;
    }

    private int resolveLeaveStatusMinutes(JsonNode node, LocalDateTime startTime, LocalDateTime endTime) {
        Integer durationPercent = readInt(node, "duration_percent");
        String durationUnit = readText(node, "duration_unit");
        if (durationPercent != null && durationPercent > 0) {
            if ("percent_hour".equalsIgnoreCase(durationUnit)) {
                return Math.max(BigDecimal.valueOf(durationPercent)
                        .multiply(BigDecimal.valueOf(60))
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                        .intValue(), 0);
            }
            if ("percent_day".equalsIgnoreCase(durationUnit)) {
                return Math.max(BigDecimal.valueOf(durationPercent)
                        .multiply(BigDecimal.valueOf(WORKDAY_MINUTES))
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                        .intValue(), 0);
            }
        }
        if (startTime != null && endTime != null && endTime.isAfter(startTime)) {
            return safeLongToInt(ChronoUnit.MINUTES.between(startTime, endTime));
        }
        return 0;
    }

    private int resolveSyncedLeaveMinutes(LeaveDayAggregate aggregate, int approvedMinutes) {
        int normalizedMinutes = Math.max(approvedMinutes, 0);
        if (aggregate == null) {
            return normalizedMinutes;
        }

        int statusMinutes = Math.max(aggregate.minutes, 0);
        int intervalMinutes = 0;
        if (aggregate.startTime != null && aggregate.endTime != null
                && aggregate.endTime.isAfter(aggregate.startTime)) {
            intervalMinutes = safeLongToInt(ChronoUnit.MINUTES.between(aggregate.startTime, aggregate.endTime));
        }

        int cappedMinutes = normalizedMinutes;
        if (statusMinutes > 0 && cappedMinutes > statusMinutes) {
            cappedMinutes = statusMinutes;
        }
        if (intervalMinutes > 0 && cappedMinutes > intervalMinutes) {
            cappedMinutes = intervalMinutes;
        }

        if (cappedMinutes != normalizedMinutes) {
            log.warn("DingTalk approved duration exceeds leave status, dingUserId={}, date={}, approvedMinutes={}, "
                            + "statusMinutes={}, intervalMinutes={}, startTime={}, endTime={}",
                    aggregate.dingUserId, aggregate.date, normalizedMinutes, statusMinutes, intervalMinutes,
                    aggregate.startTime, aggregate.endTime);
        }
        return cappedMinutes;
    }

    private long toEpochMillis(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private LocalDateTime toLocalDateTime(Long epochMillis) {
        if (epochMillis == null || epochMillis <= 0) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(normalizeEpochMillis(epochMillis)), ZoneId.systemDefault());
    }

    private int safeLongToInt(long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) value;
    }

    private Integer readInt(JsonNode node, String name) {
        JsonNode target = node == null ? null : node.path(name);
        if (target == null || target.isMissingNode() || target.isNull()) {
            return null;
        }
        if (target.isNumber()) {
            return target.intValue();
        }
        if (target.isTextual()) {
            try {
                return Integer.parseInt(target.textValue());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Long readLong(JsonNode node, String name) {
        JsonNode target = node == null ? null : node.path(name);
        if (target == null || target.isMissingNode() || target.isNull()) {
            return null;
        }
        if (target.isNumber()) {
            return target.longValue();
        }
        if (target.isTextual()) {
            try {
                return Long.parseLong(target.textValue());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String readText(JsonNode node, String name) {
        JsonNode target = node == null ? null : node.path(name);
        return target == null || target.isMissingNode() || target.isNull() ? "" : target.asText("");
    }

    private Set<String> normalizeTargetDingUserIds(Collection<String> targetDingUserIds) {
        Set<String> result = new HashSet<>();
        if (targetDingUserIds == null || targetDingUserIds.isEmpty()) {
            return result;
        }
        for (String dingUserId : targetDingUserIds) {
            if (StringUtils.hasText(dingUserId)) {
                result.add(dingUserId.trim());
            }
        }
        return result;
    }

    private long resolveLeaveBatchIntervalMs() {
        Long value = dingTalkProperties.getSync() == null ? null : dingTalkProperties.getSync().getLeaveBatchIntervalMs();
        return value == null ? 120L : Math.max(value, 0L);
    }

    private int safeInteger(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private long normalizeEpochMillis(long value) {
        return value < 100_000_000_000L ? value * 1000L : value;
    }

    private LocalDate minDate(LocalDate left, LocalDate right) {
        return left.isBefore(right) ? left : right;
    }

    private void sleepQuietly(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    @Data
    public static class LeaveSyncReport {
        private String fromDate;
        private String toDate;
        private Integer mappedUsers = 0;
        private Integer checkedDays = 0;
        private Integer pulledLeaveDays = 0;
        private Integer createdLeaves = 0;
        private Integer updatedLeaves = 0;
        private Integer cancelledLeaves = 0;
        private Integer skippedUsers = 0;
        private Integer failedCalls = 0;
        private Integer apiCalls = 0;
        private String syncMode;

        public Integer getSyncedLeaves() {
            return createdLeaves + updatedLeaves;
        }

        private void addCheckedDay() {
            checkedDays++;
        }

        private void addCheckedDays(int count) {
            checkedDays += Math.max(count, 0);
        }

        private void addPulledLeaveDay() {
            pulledLeaveDays++;
        }

        private void addCreatedLeave() {
            createdLeaves++;
        }

        private void addUpdatedLeave() {
            updatedLeaves++;
        }

        private void addCancelledLeave() {
            cancelledLeaves++;
        }

        private void addSkippedUser() {
            skippedUsers++;
        }

        private void addFailedCall() {
            failedCalls++;
        }

        private void addApiCall() {
            apiCalls++;
        }
    }

    private static class LeaveDayAggregate {
        private String dingUserId;
        private Long userId;
        private LocalDate date;
        private int minutes;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String leaveCode;
    }

}
