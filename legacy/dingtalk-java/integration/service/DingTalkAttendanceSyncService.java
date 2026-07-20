package com.kyx.service.hr.integration.dingtalk.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.kyx.foundation.common.util.json.JsonUtils;
import com.kyx.service.hr.config.DingTalkProperties;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceSyncReqVO;
import com.kyx.service.hr.integration.dingtalk.client.DingTalkOpenApiClient;
import com.kyx.service.hr.service.attendance.AttendanceDingTalkSyncResult;
import com.kyx.service.hr.service.attendance.AttendanceClockRecordService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Attendance sync from DingTalk -> OA attendance table.
 */
@Service
@Slf4j
public class DingTalkAttendanceSyncService {

    private static final int USER_BATCH_SIZE = 50;
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private DingTalkOpenApiClient dingTalkOpenApiClient;
    @Resource
    private AttendanceClockRecordService attendanceClockRecordService;
    @Resource
    private DingTalkUserBindingService dingTalkUserBindingService;
    @Resource
    private DingTalkProperties dingTalkProperties;

    public AttendanceSyncReport syncByTimeRange(LocalDateTime from, LocalDateTime to) {
        Map<String, Long> dingUserIdToLocalUserId = dingTalkUserBindingService.getCachedDingUserToOaUserMap();
        if (dingUserIdToLocalUserId == null || dingUserIdToLocalUserId.isEmpty()) {
            dingUserIdToLocalUserId = dingTalkUserBindingService.refreshDingUserToOaUserMap();
        }
        AttendanceSyncReport report = new AttendanceSyncReport();
        report.setMappedUsers(dingUserIdToLocalUserId.size());
        if (dingUserIdToLocalUserId.isEmpty()) {
            return report;
        }

        List<String> userIds = new ArrayList<>(dingUserIdToLocalUserId.keySet());
        for (int i = 0; i < userIds.size(); i += USER_BATCH_SIZE) {
            int end = Math.min(i + USER_BATCH_SIZE, userIds.size());
            List<String> batch = userIds.subList(i, end);
            List<AttendanceSyncReqVO.Record> records = fetchAttendanceRecords(batch, dingUserIdToLocalUserId, from, to, report);
            syncRecords(records, report);
            if (end < userIds.size()) {
                sleepQuietly(resolveAttendanceBatchIntervalMs());
            }
        }
        log.info("DingTalk attendance sync finished: {}", report);
        return report;
    }

    /**
     * Stream realtime path: parse event payload and write records directly.
     */
    public AttendanceSyncReport syncByStreamEventPayload(String rawPayload) {
        Map<String, Long> dingUserIdToLocalUserId = dingTalkUserBindingService.getCachedDingUserToOaUserMap();
        AttendanceSyncReport report = new AttendanceSyncReport();
        report.setMappedUsers(dingUserIdToLocalUserId.size());
        if (!StringUtils.hasText(rawPayload)) {
            return report;
        }
        JsonNode root = JsonUtils.parseTree(rawPayload);
        if (root == null || root.isMissingNode() || root.isNull()) {
            return report;
        }
        List<JsonNode> nodes = extractStreamRecordNodes(root);
        if (nodes.isEmpty()) {
            return report;
        }

        List<AttendanceSyncReqVO.Record> records = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (JsonNode node : nodes) {
            AttendanceSyncReqVO.Record record = toRecord(node, dingUserIdToLocalUserId, true);
            if (record == null) {
                report.addSkipped();
                continue;
            }
            String dedupeKey = record.getUserId() + "|" + record.getClockTime() + "|" + record.getClockType();
            if (!seen.add(dedupeKey)) {
                continue;
            }
            record.setRemark("DingTalk stream realtime");
            records.add(record);
            report.addPulled();
        }
        syncRecords(records, report);
        return report;
    }

    private void syncRecords(List<AttendanceSyncReqVO.Record> records, AttendanceSyncReport report) {
        if (records == null || records.isEmpty()) {
            return;
        }
        AttendanceSyncReqVO reqVO = new AttendanceSyncReqVO();
        reqVO.setSourceType("DINGTALK");
        reqVO.setRecords(records);
        AttendanceDingTalkSyncResult result = attendanceClockRecordService.syncDingTalkDetailed(reqVO);
        report.addSyncResult(result);
    }

    public AttendanceSyncPreview previewByTimeRange(LocalDateTime from, LocalDateTime to, Integer sampleLimit) {
        int resolvedSampleLimit = sampleLimit == null ? 20 : Math.max(sampleLimit, 1);
        Map<String, Long> dingUserIdToLocalUserId = dingTalkUserBindingService.refreshDingUserToOaUserMap();
        AttendanceSyncReport report = new AttendanceSyncReport();
        report.setMappedUsers(dingUserIdToLocalUserId.size());

        AttendanceSyncPreview preview = new AttendanceSyncPreview();
        preview.setFromTime(from.format(DATETIME_FORMATTER));
        preview.setToTime(to.format(DATETIME_FORMATTER));
        preview.setSampleLimit(resolvedSampleLimit);

        if (dingUserIdToLocalUserId.isEmpty()) {
            preview.setMappedUsers(0);
            return preview;
        }

        List<AttendanceRecordPreview> samples = new ArrayList<>();
        List<String> userIds = new ArrayList<>(dingUserIdToLocalUserId.keySet());
        for (int i = 0; i < userIds.size(); i += USER_BATCH_SIZE) {
            int end = Math.min(i + USER_BATCH_SIZE, userIds.size());
            List<String> batch = userIds.subList(i, end);
            List<AttendanceSyncReqVO.Record> records = fetchAttendanceRecords(batch, dingUserIdToLocalUserId, from, to, report);
            if (records.isEmpty()) {
                continue;
            }
            if (samples.size() >= resolvedSampleLimit) {
                continue;
            }
            int remain = resolvedSampleLimit - samples.size();
            int size = Math.min(remain, records.size());
            for (int j = 0; j < size; j++) {
                samples.add(toPreview(records.get(j)));
            }
        }

        preview.setMappedUsers(report.getMappedUsers());
        preview.setPulledRecords(report.getPulledRecords());
        preview.setSkippedRecords(report.getSkippedRecords());
        preview.setSampleRecords(samples);
        return preview;
    }

    private List<AttendanceSyncReqVO.Record> fetchAttendanceRecords(List<String> dingUserIds, Map<String, Long> mapping,
                                                                    LocalDateTime from, LocalDateTime to, AttendanceSyncReport report) {
        Map<String, Object> body = dingTalkOpenApiClient.body();
        body.put("checkDateFrom", from.format(DATETIME_FORMATTER));
        body.put("checkDateTo", to.format(DATETIME_FORMATTER));
        body.put("userIds", dingUserIds);

        JsonNode root = dingTalkOpenApiClient.postTopApiWithRetry("/attendance/listRecord", body);
        JsonNode list = root.path("recordresult");
        if (!list.isArray()) {
            list = root.path("result").path("recordresult");
        }
        List<AttendanceSyncReqVO.Record> records = new ArrayList<>();
        if (!list.isArray()) {
            return records;
        }

        for (JsonNode node : list) {
            AttendanceSyncReqVO.Record record = toRecord(node, mapping, false);
            if (record == null) {
                report.addSkipped();
                continue;
            }
            records.add(record);
            report.addPulled();
        }
        return records;
    }

    private List<JsonNode> extractStreamRecordNodes(JsonNode root) {
        List<JsonNode> result = new ArrayList<>();
        collectStreamRecordNodes(root, result, 0);
        return result;
    }

    private void collectStreamRecordNodes(JsonNode node, List<JsonNode> result, int depth) {
        if (node == null || node.isNull() || depth > 8) {
            return;
        }
        if (isStreamRecordNode(node)) {
            result.add(node);
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                collectStreamRecordNodes(child, result, depth + 1);
            }
            return;
        }
        if (!node.isObject()) {
            return;
        }

        JsonNode preferred = firstPresentNode(node,
                "recordresult", "recordResult", "records", "recordList", "checkRecordList", "list", "result", "data");
        if (preferred != null) {
            collectStreamRecordNodes(preferred, result, depth + 1);
            return;
        }
        for (JsonNode child : node) {
            collectStreamRecordNodes(child, result, depth + 1);
        }
    }

    private boolean isStreamRecordNode(JsonNode node) {
        if (node == null || !node.isObject()) {
            return false;
        }
        String dingUserId = text(node, "userId", "userid", "staffId", "staff_id", "operatorUserId", "operatorUserid");
        return StringUtils.hasText(dingUserId) && parseClockTime(node) != null;
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

    private AttendanceSyncReqVO.Record toRecord(JsonNode node, Map<String, Long> mapping, boolean enrichByListRecord) {
        String dingUserId = text(node, "userId", "userid", "staffId", "staff_id", "operatorUserId", "operatorUserid");
        if (!StringUtils.hasText(dingUserId)) {
            return null;
        }
        String normalizedDingUserId = dingUserId.trim();
        Long localUserId = mapping.get(normalizedDingUserId);
        if (localUserId == null) {
            log.debug("Skip attendance record because no OA mapping for dingUserId={}", normalizedDingUserId);
            return null;
        }
        LocalDateTime clockTime = parseClockTime(node);
        if (clockTime == null) {
            return null;
        }
        LocalDateTime normalizedClockTime = normalizeClockTime(clockTime);

        AttendanceSyncReqVO.Record record = new AttendanceSyncReqVO.Record();
        record.setUserId(localUserId);
        record.setAttendanceDate(normalizedClockTime.toLocalDate());
        record.setClockTime(normalizedClockTime);

        String resolvedClockType = resolveClockType(node);
        String rawClockStatus = text(node, "timeResult", "time_result", "status", "clockStatus", "clock_status");
        if (enrichByListRecord && (!StringUtils.hasText(resolvedClockType) || !StringUtils.hasText(rawClockStatus))) {
            EnrichedClockFields enrichedClockFields = enrichClockFieldsFromListRecord(node, normalizedDingUserId, normalizedClockTime);
            if (!StringUtils.hasText(resolvedClockType) && StringUtils.hasText(enrichedClockFields.getClockType())) {
                resolvedClockType = enrichedClockFields.getClockType();
            }
            if (!StringUtils.hasText(rawClockStatus) && StringUtils.hasText(enrichedClockFields.getClockStatus())) {
                rawClockStatus = enrichedClockFields.getClockStatus();
            }
        }
        if (!StringUtils.hasText(resolvedClockType)) {
            resolvedClockType = "UNKNOWN";
        }
        record.setClockType(resolvedClockType);
        if (!StringUtils.hasText(rawClockStatus) && "UNKNOWN".equals(resolvedClockType)) {
            rawClockStatus = "UNKNOWN";
        }
        record.setClockStatus(normalizeStatus(rawClockStatus));
        record.setSourceRecordId(resolveRecordId(node, normalizedDingUserId, normalizedClockTime));
        record.setLocationName(text(node,
                "userAddress", "user_address",
                "address",
                "locationName", "location_name",
                "baseAddress", "base_address",
                "deviceName"));
        record.setLocationAddress(text(node,
                "userAddress", "user_address",
                "locationAddress", "location_address",
                "address",
                "baseAddress", "base_address"));
        record.setDeviceInfo(text(node, "deviceName", "deviceSN", "deviceId", "device_id", "deviceInfo", "device_info", "deviceType", "device_type"));
        record.setRemark("DingTalk auto sync");
        record.setRawPayload(node.toString());
        return record;
    }

    private EnrichedClockFields enrichClockFieldsFromListRecord(JsonNode streamNode, String dingUserId, LocalDateTime clockTime) {
        LocalDateTime from = clockTime.minusMinutes(2);
        LocalDateTime to = clockTime.plusMinutes(2);
        try {
            Map<String, Object> body = dingTalkOpenApiClient.body();
            body.put("checkDateFrom", from.format(DATETIME_FORMATTER));
            body.put("checkDateTo", to.format(DATETIME_FORMATTER));
            body.put("userIds", java.util.Collections.singletonList(dingUserId));

            JsonNode root = dingTalkOpenApiClient.postTopApiWithRetry("/attendance/listRecord", body);
            JsonNode list = root.path("recordresult");
            if (!list.isArray()) {
                list = root.path("result").path("recordresult");
            }
            if (!list.isArray()) {
                return EnrichedClockFields.empty();
            }

            String eventBizId = text(streamNode, "bizId", "biz_id", "recordId", "record_id", "id");
            JsonNode bestNode = pickBestRecordNode(list, dingUserId, clockTime, eventBizId);
            if (bestNode == null) {
                return EnrichedClockFields.empty();
            }

            return new EnrichedClockFields(
                    resolveClockType(bestNode),
                    text(bestNode, "timeResult", "time_result", "status", "clockStatus", "clock_status")
            );
        } catch (Exception ex) {
            log.debug("Stream one-shot enrich failed, dingUserId={}, checkTime={}, message={}",
                    dingUserId, clockTime, ex.getMessage());
            return EnrichedClockFields.empty();
        }
    }

    private JsonNode pickBestRecordNode(JsonNode list, String dingUserId, LocalDateTime clockTime, String eventBizId) {
        JsonNode nearestNode = null;
        long targetEpochMillis = clockTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long nearestDiff = Long.MAX_VALUE;
        for (JsonNode candidate : list) {
            String candidateUserId = text(candidate, "userId", "userid", "staffId", "staff_id", "operatorUserId", "operatorUserid");
            if (!StringUtils.hasText(candidateUserId) || !dingUserId.equals(candidateUserId.trim())) {
                continue;
            }
            if (StringUtils.hasText(eventBizId)) {
                String candidateBizId = text(candidate, "bizId", "biz_id", "recordId", "record_id", "id");
                if (StringUtils.hasText(candidateBizId) && eventBizId.equals(candidateBizId.trim())) {
                    return candidate;
                }
            }
            LocalDateTime candidateClockTime = parseClockTime(candidate);
            if (candidateClockTime == null) {
                continue;
            }
            long candidateEpochMillis = candidateClockTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long diff = Math.abs(candidateEpochMillis - targetEpochMillis);
            if (diff < nearestDiff) {
                nearestDiff = diff;
                nearestNode = candidate;
            }
        }
        if (nearestNode == null) {
            return null;
        }
        return nearestDiff <= TimeUnit.MINUTES.toMillis(10) ? nearestNode : null;
    }

    private AttendanceRecordPreview toPreview(AttendanceSyncReqVO.Record record) {
        AttendanceRecordPreview preview = new AttendanceRecordPreview();
        preview.setUserId(record.getUserId());
        preview.setAttendanceDate(record.getAttendanceDate());
        preview.setClockTime(record.getClockTime());
        preview.setClockType(record.getClockType());
        preview.setClockStatus(record.getClockStatus());
        preview.setSourceRecordId(record.getSourceRecordId());
        preview.setLocationName(record.getLocationName());
        preview.setLocationAddress(record.getLocationAddress());
        preview.setDeviceInfo(record.getDeviceInfo());
        return preview;
    }

    private String resolveRecordId(JsonNode node, String dingUserId, LocalDateTime clockTime) {
        String id = text(node, "recordId", "record_id", "id", "bizId", "biz_id");
        if (StringUtils.hasText(id)) {
            return "dingtalk-" + dingUserId + "-" + id.trim();
        }
        return "dingtalk-" + dingUserId + "-" + clockTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private LocalDateTime normalizeClockTime(LocalDateTime clockTime) {
        if (clockTime == null) {
            return null;
        }
        return clockTime.withNano(0);
    }

    private LocalDateTime parseClockTime(JsonNode node) {
        String[] names = {"userCheckTime", "checkTime", "check_time", "workTime", "work_time", "timestamp", "gmtCreate", "gmt_create"};
        for (String name : names) {
            LocalDateTime parsed = parseClockTimeNode(node.path(name));
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private LocalDateTime parseClockTimeNode(JsonNode valueNode) {
        if (valueNode == null || valueNode.isMissingNode() || valueNode.isNull()) {
            return null;
        }
        if (valueNode.isNumber()) {
            long epochMillis = normalizeEpochMillis(valueNode.longValue());
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
        }
        String value = valueNode.asText("");
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.matches("^\\d+$")) {
            long epochMillis = normalizeEpochMillis(Long.parseLong(trimmed));
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
        }
        return parseClockTimeText(trimmed);
    }

    private long normalizeEpochMillis(long value) {
        return value < 100_000_000_000L ? value * 1000L : value;
    }

    private LocalDateTime parseClockTimeText(String value) {
        try {
            return LocalDateTime.parse(value, DATETIME_FORMATTER);
        } catch (Exception ignored) {
            // ignore
        }
        try {
            return LocalDateTime.parse(value);
        } catch (Exception ignored) {
            // ignore
        }
        try {
            return LocalDateTime.ofInstant(Instant.parse(value), ZoneId.systemDefault());
        } catch (Exception ignored) {
            // ignore
        }
        if (value.contains("T")) {
            try {
                return LocalDateTime.parse(value.replace("T", " "), DATETIME_FORMATTER);
            } catch (Exception ignored) {
                // ignore
            }
        }
        return null;
    }

    private String resolveClockType(JsonNode node) {
        String directType = text(node, "checkType", "check_type", "type", "clockType", "clock_type",
                "checkTypeName", "check_type_name");
        return mapClockType(directType);
    }

    private String mapClockType(String checkType) {
        if (!StringUtils.hasText(checkType)) {
            return null;
        }
        String value = checkType.trim().toUpperCase();
        if (value.contains("下班") || value.contains("签退")) {
            return "OUT";
        }
        if (value.contains("上班") || value.contains("签到")) {
            return "IN";
        }
        if (value.contains("OFF") || value.contains("OUT")) {
            return "OUT";
        }
        if (value.contains("ON") || "IN".equals(value) || "1".equals(value)) {
            return "IN";
        }
        return null;
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "NORMAL";
        }
        return status.trim().toUpperCase();
    }

    private String text(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode v = node.path(name);
            if (!v.isMissingNode() && !v.isNull()) {
                String text = v.asText("");
                if (StringUtils.hasText(text)) {
                    return text.trim();
                }
            }
        }
        return null;
    }

    private long resolveAttendanceBatchIntervalMs() {
        Long value = dingTalkProperties.getSync() == null ? null : dingTalkProperties.getSync().getAttendanceBatchIntervalMs();
        return value == null ? 0L : Math.max(value, 0L);
    }

    private void sleepQuietly(long sleepMs) {
        if (sleepMs <= 0) {
            return;
        }
        try {
            TimeUnit.MILLISECONDS.sleep(sleepMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    @Data
    public static class AttendanceSyncReport {
        private int mappedUsers;
        private int pulledRecords;
        private int syncedRecords;
        private int createdRecords;
        private int updatedRecords;
        private int skippedRecords;

        void addPulled() {
            this.pulledRecords++;
        }

        void addSynced(int count) {
            this.syncedRecords += Math.max(count, 0);
        }

        void addSyncResult(AttendanceDingTalkSyncResult result) {
            if (result == null) {
                return;
            }
            this.syncedRecords += Math.max(result.getProcessedCount(), 0);
            this.createdRecords += Math.max(result.getCreatedCount(), 0);
            this.updatedRecords += Math.max(result.getUpdatedCount(), 0);
            this.skippedRecords += Math.max(result.getSkippedCount(), 0);
        }

        void addSkipped() {
            this.skippedRecords++;
        }
    }

    @Data
    public static class AttendanceSyncPreview {
        private String fromTime;
        private String toTime;
        private int sampleLimit;
        private int mappedUsers;
        private int pulledRecords;
        private int skippedRecords;
        private List<AttendanceRecordPreview> sampleRecords = new ArrayList<>();
    }

    @Data
    public static class AttendanceRecordPreview {
        private Long userId;
        private java.time.LocalDate attendanceDate;
        private LocalDateTime clockTime;
        private String clockType;
        private String clockStatus;
        private String sourceRecordId;
        private String locationName;
        private String locationAddress;
        private String deviceInfo;
    }

    @Data
    private static class EnrichedClockFields {
        private final String clockType;
        private final String clockStatus;

        static EnrichedClockFields empty() {
            return new EnrichedClockFields(null, null);
        }
    }

}
