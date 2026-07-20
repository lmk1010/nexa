package com.kyx.service.hr.integration.dingtalk.service;

import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.service.hr.api.dingtalk.dto.DingTalkBpmNoticeReqDTO;
import com.kyx.service.hr.api.dingtalk.dto.DingTalkRequirementNoticeReqDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DingTalkRequirementNoticeService {

    private static final String LIMIT_KEY_PREFIX = "hr:dingtalk:requirement-notice:limit:";
    private static final String DEDUP_KEY_PREFIX = "hr:dingtalk:requirement-notice:dedup:";
    private static final String STATS_TOTAL_KEY_PREFIX = "hr:dingtalk:requirement-notice:stats:total:";
    private static final String STATS_DAILY_KEY_PREFIX = "hr:dingtalk:requirement-notice:stats:daily:";
    private static final String STATS_MINUTE_KEY_PREFIX = "hr:dingtalk:requirement-notice:stats:minute:";
    private static final String DEFAULT_TENANT_KEY = "default";
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final String APPROVAL_HEAD_COLOR = "FFFF6B00";
    private static final String DEV_HEAD_COLOR = "FF1677FF";
    private static final String COMMENT_HEAD_COLOR = "FF13A8A8";
    private static final String BPM_TASK_HEAD_COLOR = "FF6B46C1";
    private static final String BPM_COPY_HEAD_COLOR = "FF52C41A";
    private static final String APPROVAL_STATUS_COLOR = "0xFFFF6B00";
    private static final String DEV_STATUS_COLOR = "0xFF1677FF";
    private static final String COMMENT_STATUS_COLOR = "0xFF13A8A8";
    private static final String BPM_TASK_STATUS_COLOR = "0xFF6B46C1";
    private static final String BPM_COPY_STATUS_COLOR = "0xFF52C41A";
    private static final int DEFAULT_PER_USER_LIMIT_PER_MINUTE = 8;
    private static final int DEFAULT_TENANT_LIMIT_PER_MINUTE = 60;
    private static final int DEFAULT_DEDUP_TTL_MINUTES = 10;

    @Resource
    private DingTalkRequirementNoticeConfigService configService;
    @Resource
    private DingTalkMessageNotifyService messageNotifyService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Value("${dingtalk.requirement-notice.per-user-limit-per-minute:8}")
    private int perUserLimitPerMinute;
    @Value("${dingtalk.requirement-notice.tenant-limit-per-minute:60}")
    private int tenantLimitPerMinute;
    @Value("${dingtalk.requirement-notice.dedup-ttl-minutes:10}")
    private int dedupTtlMinutes;

    public void sendApprovalTodo(DingTalkRequirementNoticeReqDTO reqDTO) {
        if (!configService.isSceneEnabled(DingTalkRequirementNoticeConfigService.SCENE_APPROVAL_TODO)) {
            return;
        }
        send(reqDTO, DingTalkRequirementNoticeConfigService.SCENE_APPROVAL_TODO, "快易修OA · 需求审批", APPROVAL_HEAD_COLOR,
                requirementTitle(reqDTO), "请审批该需求，点击卡片查看详情。",
                buildApprovalForm(reqDTO), "审批待处理", APPROVAL_STATUS_COLOR);
    }

    public void sendAssignedDev(DingTalkRequirementNoticeReqDTO reqDTO) {
        if (!configService.isSceneEnabled(DingTalkRequirementNoticeConfigService.SCENE_ASSIGNED_DEV)) {
            return;
        }
        send(reqDTO, DingTalkRequirementNoticeConfigService.SCENE_ASSIGNED_DEV, "快易修OA · 开发任务", DEV_HEAD_COLOR,
                requirementTitle(reqDTO), "需求已分配到你，点击卡片查看详情。",
                buildAssignedDevForm(reqDTO), "开发待处理", DEV_STATUS_COLOR);
    }

    public void sendCommentRemind(DingTalkRequirementNoticeReqDTO reqDTO) {
        if (!configService.isSceneEnabled(DingTalkRequirementNoticeConfigService.SCENE_COMMENT_REMIND)) {
            return;
        }
        send(reqDTO, DingTalkRequirementNoticeConfigService.SCENE_COMMENT_REMIND, "快易修OA · 沟通提醒", COMMENT_HEAD_COLOR,
                requirementTitle(reqDTO), "有人在需求沟通记录中提醒你，点击卡片查看详情。",
                buildCommentRemindForm(reqDTO), "沟通待查看", COMMENT_STATUS_COLOR);
    }

    public void sendBpmTaskTodo(DingTalkBpmNoticeReqDTO reqDTO) {
        if (!configService.isSceneEnabled(DingTalkRequirementNoticeConfigService.SCENE_APPROVAL_TODO)) {
            return;
        }
        sendBpm(reqDTO, DingTalkRequirementNoticeConfigService.SCENE_APPROVAL_TODO, "快易修OA · BPM待办",
                BPM_TASK_HEAD_COLOR, bpmTitle(reqDTO), "流程已流转到你处理，点击卡片查看详情。",
                buildBpmTaskForm(reqDTO), "待确认处理", BPM_TASK_STATUS_COLOR);
    }

    public void sendBpmCopy(DingTalkBpmNoticeReqDTO reqDTO) {
        if (!configService.isSceneEnabled(DingTalkRequirementNoticeConfigService.SCENE_BPM_COPY)) {
            return;
        }
        sendBpm(reqDTO, DingTalkRequirementNoticeConfigService.SCENE_BPM_COPY, "快易修OA · BPM知会",
                BPM_COPY_HEAD_COLOR, bpmTitle(reqDTO), "有流程节点知会你，点击卡片查看详情。",
                buildBpmCopyForm(reqDTO), "知会待查看", BPM_COPY_STATUS_COLOR);
    }

    private void send(DingTalkRequirementNoticeReqDTO reqDTO, String scene, String headText, String headColor,
                      String title, String content, List<DingTalkMessageNotifyService.OaFormItem> form,
                      String statusValue, String statusColor) {
        if (reqDTO == null || reqDTO.getReceiverUserIds() == null || reqDTO.getReceiverUserIds().isEmpty()) {
            return;
        }
        String messageUrl = messageNotifyService.buildDingTalkOpenAppUrl(reqDTO.getDetailUrl());
        DingTalkMessageNotifyService.OaStatusBar statusBar =
                new DingTalkMessageNotifyService.OaStatusBar(statusValue, statusColor);
        for (Long receiverUserId : reqDTO.getReceiverUserIds()) {
            try {
                if (!acquireNoticePermit(reqDTO, scene, receiverUserId)) {
                    continue;
                }
                List<DingTalkMessageNotifyService.TextSendResult> results =
                        messageNotifyService.sendOaCardToOaUserIds(Collections.singleton(receiverUserId),
                                headText, headColor, title, content, form, messageUrl, reqDTO.getDetailUrl(), statusBar, null);
                recordSent(scene, results.size());
            } catch (Exception ex) {
                log.warn("Send DingTalk requirement notice failed, scene={}, requirementId={}, receiverUserId={}, reason={}",
                        scene, reqDTO.getRequirementId(), receiverUserId, ex.getMessage());
            }
        }
    }

    private void sendBpm(DingTalkBpmNoticeReqDTO reqDTO, String scene, String headText, String headColor,
                         String title, String content, List<DingTalkMessageNotifyService.OaFormItem> form,
                         String statusValue, String statusColor) {
        if (reqDTO == null || reqDTO.getReceiverUserIds() == null || reqDTO.getReceiverUserIds().isEmpty()) {
            return;
        }
        String messageUrl = messageNotifyService.buildDingTalkOpenAppUrl(reqDTO.getDetailUrl());
        DingTalkMessageNotifyService.OaStatusBar statusBar =
                new DingTalkMessageNotifyService.OaStatusBar(statusValue, statusColor);
        for (Long receiverUserId : reqDTO.getReceiverUserIds()) {
            try {
                if (!acquireBpmNoticePermit(reqDTO, scene, receiverUserId)) {
                    continue;
                }
                List<DingTalkMessageNotifyService.TextSendResult> results =
                        messageNotifyService.sendOaCardToOaUserIds(Collections.singleton(receiverUserId),
                                headText, headColor, title, content, form, messageUrl, reqDTO.getDetailUrl(), statusBar, null);
                recordSent(scene, results.size());
            } catch (Exception ex) {
                log.warn("Send DingTalk BPM notice failed, scene={}, processInstanceId={}, taskId={}, receiverUserId={}, reason={}",
                        scene, reqDTO.getProcessInstanceId(), reqDTO.getTaskId(), receiverUserId, ex.getMessage());
            }
        }
    }

    private boolean acquireNoticePermit(DingTalkRequirementNoticeReqDTO reqDTO, String scene, Long receiverUserId) {
        if (receiverUserId == null) {
            return false;
        }
        String tenantKey = currentTenantKey();
        String dedupBizId = StringUtils.hasText(reqDTO.getDedupBizId())
                ? reqDTO.getDedupBizId().trim() : String.valueOf(reqDTO.getRequirementId());
        String dedupKey = DEDUP_KEY_PREFIX + tenantKey + ":" + scene + ":" + dedupBizId + ":" + receiverUserId;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(dedupKey))) {
            log.info("Skip duplicated DingTalk requirement notice, scene={}, requirementId={}, receiverUserId={}",
                    scene, reqDTO.getRequirementId(), receiverUserId);
            return false;
        }
        if (!acquireMinutePermit(LIMIT_KEY_PREFIX + tenantKey + ":tenant",
                positiveOrDefault(tenantLimitPerMinute, DEFAULT_TENANT_LIMIT_PER_MINUTE))) {
            log.warn("Skip DingTalk requirement notice by tenant minute limit, scene={}, requirementId={}, receiverUserId={}",
                    scene, reqDTO.getRequirementId(), receiverUserId);
            return false;
        }
        if (!acquireMinutePermit(LIMIT_KEY_PREFIX + tenantKey + ":user:" + receiverUserId,
                positiveOrDefault(perUserLimitPerMinute, DEFAULT_PER_USER_LIMIT_PER_MINUTE))) {
            log.warn("Skip DingTalk requirement notice by user minute limit, scene={}, requirementId={}, receiverUserId={}",
                    scene, reqDTO.getRequirementId(), receiverUserId);
            return false;
        }
        Boolean first = stringRedisTemplate.opsForValue().setIfAbsent(dedupKey, "1",
                positiveOrDefault(dedupTtlMinutes, DEFAULT_DEDUP_TTL_MINUTES), TimeUnit.MINUTES);
        return Boolean.TRUE.equals(first);
    }

    private boolean acquireBpmNoticePermit(DingTalkBpmNoticeReqDTO reqDTO, String scene, Long receiverUserId) {
        if (receiverUserId == null) {
            return false;
        }
        String tenantKey = currentTenantKey();
        String dedupBizId = StringUtils.hasText(reqDTO.getDedupBizId())
                ? reqDTO.getDedupBizId().trim() : reqDTO.getProcessInstanceId() + ":" + empty(reqDTO.getTaskId());
        String dedupKey = DEDUP_KEY_PREFIX + tenantKey + ":" + scene + ":" + dedupBizId + ":" + receiverUserId;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(dedupKey))) {
            log.info("Skip duplicated DingTalk BPM notice, scene={}, processInstanceId={}, taskId={}, receiverUserId={}",
                    scene, reqDTO.getProcessInstanceId(), reqDTO.getTaskId(), receiverUserId);
            return false;
        }
        if (!acquireMinutePermit(LIMIT_KEY_PREFIX + tenantKey + ":tenant",
                positiveOrDefault(tenantLimitPerMinute, DEFAULT_TENANT_LIMIT_PER_MINUTE))) {
            log.warn("Skip DingTalk BPM notice by tenant minute limit, scene={}, processInstanceId={}, receiverUserId={}",
                    scene, reqDTO.getProcessInstanceId(), receiverUserId);
            return false;
        }
        if (!acquireMinutePermit(LIMIT_KEY_PREFIX + tenantKey + ":user:" + receiverUserId,
                positiveOrDefault(perUserLimitPerMinute, DEFAULT_PER_USER_LIMIT_PER_MINUTE))) {
            log.warn("Skip DingTalk BPM notice by user minute limit, scene={}, processInstanceId={}, receiverUserId={}",
                    scene, reqDTO.getProcessInstanceId(), receiverUserId);
            return false;
        }
        Boolean first = stringRedisTemplate.opsForValue().setIfAbsent(dedupKey, "1",
                positiveOrDefault(dedupTtlMinutes, DEFAULT_DEDUP_TTL_MINUTES), TimeUnit.MINUTES);
        return Boolean.TRUE.equals(first);
    }

    private boolean acquireMinutePermit(String key, int limit) {
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, 1, TimeUnit.MINUTES);
        }
        return count == null || count <= limit;
    }

    public NoticeStats getStats() {
        try {
            String tenantKey = currentTenantKey();
            String today = LocalDate.now().format(DAY_FORMATTER);
            String minute = LocalDateTime.now().format(MINUTE_FORMATTER);
            List<SceneStats> sceneStats = configService.getSceneConfigs().stream()
                    .map(scene -> buildSceneStats(tenantKey, scene.getScene(), today, minute))
                    .collect(Collectors.toList());
            long total = sceneStats.stream().mapToLong(SceneStats::getTotalSentCount).sum();
            long todayTotal = sceneStats.stream().mapToLong(SceneStats::getTodaySentCount).sum();
            long minuteTotal = sceneStats.stream().mapToLong(SceneStats::getCurrentMinuteSentCount).sum();
            return new NoticeStats(total, todayTotal, minuteTotal,
                    positiveOrDefault(perUserLimitPerMinute, DEFAULT_PER_USER_LIMIT_PER_MINUTE),
                    positiveOrDefault(tenantLimitPerMinute, DEFAULT_TENANT_LIMIT_PER_MINUTE),
                    positiveOrDefault(dedupTtlMinutes, DEFAULT_DEDUP_TTL_MINUTES),
                    sceneStats);
        } catch (Exception ex) {
            log.warn("Read DingTalk requirement notice stats failed, reason={}", ex.getMessage());
        }
        return new NoticeStats(0L, 0L, 0L,
                positiveOrDefault(perUserLimitPerMinute, DEFAULT_PER_USER_LIMIT_PER_MINUTE),
                positiveOrDefault(tenantLimitPerMinute, DEFAULT_TENANT_LIMIT_PER_MINUTE),
                positiveOrDefault(dedupTtlMinutes, DEFAULT_DEDUP_TTL_MINUTES),
                Arrays.asList(
                        new SceneStats(DingTalkRequirementNoticeConfigService.SCENE_APPROVAL_TODO, 0L, 0L, 0L),
                        new SceneStats(DingTalkRequirementNoticeConfigService.SCENE_ASSIGNED_DEV, 0L, 0L, 0L),
                        new SceneStats(DingTalkRequirementNoticeConfigService.SCENE_COMMENT_REMIND, 0L, 0L, 0L),
                        new SceneStats(DingTalkRequirementNoticeConfigService.SCENE_BPM_COPY, 0L, 0L, 0L)
                ));
    }

    private SceneStats buildSceneStats(String tenantKey, String scene, String today, String minute) {
        return new SceneStats(scene,
                readLong(STATS_TOTAL_KEY_PREFIX + tenantKey + ":" + scene),
                readLong(STATS_DAILY_KEY_PREFIX + tenantKey + ":" + today + ":" + scene),
                readLong(STATS_MINUTE_KEY_PREFIX + tenantKey + ":" + minute + ":" + scene));
    }

    private void recordSent(String scene, int count) {
        if (count <= 0) {
            return;
        }
        try {
            String tenantKey = currentTenantKey();
            String today = LocalDate.now().format(DAY_FORMATTER);
            String minute = LocalDateTime.now().format(MINUTE_FORMATTER);
            stringRedisTemplate.opsForValue().increment(STATS_TOTAL_KEY_PREFIX + tenantKey + ":" + scene, count);
            String dailyKey = STATS_DAILY_KEY_PREFIX + tenantKey + ":" + today + ":" + scene;
            stringRedisTemplate.opsForValue().increment(dailyKey, count);
            stringRedisTemplate.expire(dailyKey, 35, TimeUnit.DAYS);
            String minuteKey = STATS_MINUTE_KEY_PREFIX + tenantKey + ":" + minute + ":" + scene;
            stringRedisTemplate.opsForValue().increment(minuteKey, count);
            stringRedisTemplate.expire(minuteKey, 2, TimeUnit.MINUTES);
        } catch (Exception ex) {
            log.warn("Record DingTalk requirement notice stats failed, scene={}, count={}, reason={}",
                    scene, count, ex.getMessage());
        }
    }

    private long readLong(String key) {
        String value = stringRedisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(value)) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private int positiveOrDefault(int value, int defaultValue) {
        return value > 0 ? value : defaultValue;
    }

    private String currentTenantKey() {
        Long tenantId = TenantContextHolder.getTenantId();
        return tenantId == null ? DEFAULT_TENANT_KEY : tenantId.toString();
    }

    private List<DingTalkMessageNotifyService.OaFormItem> buildApprovalForm(DingTalkRequirementNoticeReqDTO reqDTO) {
        List<DingTalkMessageNotifyService.OaFormItem> form = new ArrayList<>();
        form.add(new DingTalkMessageNotifyService.OaFormItem("处理状态：", "待审批"));
        form.add(new DingTalkMessageNotifyService.OaFormItem("需求发起：", empty(reqDTO.getProposerName())));
        form.add(new DingTalkMessageNotifyService.OaFormItem("提出部门：", empty(reqDTO.getProposerDept())));
        form.add(new DingTalkMessageNotifyService.OaFormItem("期望完成：", emptyDate(reqDTO.getExpectedFinishDate())));
        return form;
    }

    private List<DingTalkMessageNotifyService.OaFormItem> buildAssignedDevForm(DingTalkRequirementNoticeReqDTO reqDTO) {
        List<DingTalkMessageNotifyService.OaFormItem> form = new ArrayList<>();
        form.add(new DingTalkMessageNotifyService.OaFormItem("开发状态：", "待处理"));
        form.add(new DingTalkMessageNotifyService.OaFormItem("需求发起：", empty(reqDTO.getProposerName())));
        form.add(new DingTalkMessageNotifyService.OaFormItem("开发负责：", empty(reqDTO.getAssigneeName())));
        form.add(new DingTalkMessageNotifyService.OaFormItem("期望完成：", emptyDate(reqDTO.getExpectedFinishDate())));
        return form;
    }

    private List<DingTalkMessageNotifyService.OaFormItem> buildCommentRemindForm(DingTalkRequirementNoticeReqDTO reqDTO) {
        List<DingTalkMessageNotifyService.OaFormItem> form = new ArrayList<>();
        form.add(new DingTalkMessageNotifyService.OaFormItem("沟通类型：", empty(reqDTO.getCommentTypeLabel())));
        form.add(new DingTalkMessageNotifyService.OaFormItem("提醒对象：", empty(reqDTO.getTargetUserName())));
        form.add(new DingTalkMessageNotifyService.OaFormItem("发起人：", empty(reqDTO.getOperatorName())));
        form.add(new DingTalkMessageNotifyService.OaFormItem("内容摘要：", truncate(empty(reqDTO.getCommentContent()), 80)));
        return form;
    }

    private List<DingTalkMessageNotifyService.OaFormItem> buildBpmTaskForm(DingTalkBpmNoticeReqDTO reqDTO) {
        List<DingTalkMessageNotifyService.OaFormItem> form = new ArrayList<>();
        form.add(new DingTalkMessageNotifyService.OaFormItem("节点名称：", empty(reqDTO.getTaskName())));
        form.add(new DingTalkMessageNotifyService.OaFormItem("发起人：", empty(reqDTO.getStartUserNickname())));
        form.add(new DingTalkMessageNotifyService.OaFormItem("流程编号：", empty(reqDTO.getProcessInstanceId())));
        return form;
    }

    private List<DingTalkMessageNotifyService.OaFormItem> buildBpmCopyForm(DingTalkBpmNoticeReqDTO reqDTO) {
        List<DingTalkMessageNotifyService.OaFormItem> form = new ArrayList<>();
        form.add(new DingTalkMessageNotifyService.OaFormItem("知会节点：", empty(firstText(reqDTO.getActivityName(), reqDTO.getTaskName()))));
        form.add(new DingTalkMessageNotifyService.OaFormItem("发起人：", empty(reqDTO.getStartUserNickname())));
        form.add(new DingTalkMessageNotifyService.OaFormItem("知会说明：", empty(reqDTO.getReason())));
        return form;
    }

    private String bpmTitle(DingTalkBpmNoticeReqDTO reqDTO) {
        String name = firstText(reqDTO.getProcessInstanceName(), reqDTO.getTaskName());
        return StringUtils.hasText(name) ? name.trim() : "BPM流程";
    }

    private String requirementTitle(DingTalkRequirementNoticeReqDTO reqDTO) {
        String title = StringUtils.hasText(reqDTO.getTitle()) ? reqDTO.getTitle().trim() : "未命名需求";
        return "#" + reqDTO.getRequirementId() + " " + title;
    }

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private String empty(String value) {
        return StringUtils.hasText(value) ? value.trim() : "-";
    }

    private String emptyDate(String value) {
        if (!StringUtils.hasText(value) || "-".equals(value.trim())) {
            return "未设置";
        }
        String date = value.trim();
        return date.endsWith(" 00:00:00") ? date.substring(0, date.length() - 9) : date;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength)) + "...";
    }

    public static class NoticeStats {
        private final Long totalSentCount;
        private final Long todaySentCount;
        private final Long currentMinuteSentCount;
        private final Integer perUserLimitPerMinute;
        private final Integer tenantLimitPerMinute;
        private final Integer dedupTtlMinutes;
        private final List<SceneStats> sceneStats;

        public NoticeStats(Long totalSentCount, Long todaySentCount, Long currentMinuteSentCount,
                           Integer perUserLimitPerMinute, Integer tenantLimitPerMinute, Integer dedupTtlMinutes,
                           List<SceneStats> sceneStats) {
            this.totalSentCount = totalSentCount;
            this.todaySentCount = todaySentCount;
            this.currentMinuteSentCount = currentMinuteSentCount;
            this.perUserLimitPerMinute = perUserLimitPerMinute;
            this.tenantLimitPerMinute = tenantLimitPerMinute;
            this.dedupTtlMinutes = dedupTtlMinutes;
            this.sceneStats = sceneStats;
        }

        public Long getTotalSentCount() {
            return totalSentCount;
        }

        public Long getTodaySentCount() {
            return todaySentCount;
        }

        public Long getCurrentMinuteSentCount() {
            return currentMinuteSentCount;
        }

        public Integer getPerUserLimitPerMinute() {
            return perUserLimitPerMinute;
        }

        public Integer getTenantLimitPerMinute() {
            return tenantLimitPerMinute;
        }

        public Integer getDedupTtlMinutes() {
            return dedupTtlMinutes;
        }

        public List<SceneStats> getSceneStats() {
            return sceneStats;
        }
    }

    public static class SceneStats {
        private final String scene;
        private final Long totalSentCount;
        private final Long todaySentCount;
        private final Long currentMinuteSentCount;

        public SceneStats(String scene, Long totalSentCount, Long todaySentCount, Long currentMinuteSentCount) {
            this.scene = scene;
            this.totalSentCount = totalSentCount;
            this.todaySentCount = todaySentCount;
            this.currentMinuteSentCount = currentMinuteSentCount;
        }

        public String getScene() {
            return scene;
        }

        public Long getTotalSentCount() {
            return totalSentCount;
        }

        public Long getTodaySentCount() {
            return todaySentCount;
        }

        public Long getCurrentMinuteSentCount() {
            return currentMinuteSentCount;
        }
    }
}
