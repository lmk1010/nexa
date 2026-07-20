package com.kyx.service.hr.integration.dingtalk.service;

import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.foundation.web.config.WebProperties;
import com.kyx.service.hr.controller.admin.integration.vo.DingTalkSystemUpdateNoticeReqVO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeProfileDO;
import com.kyx.service.hr.dal.dataobject.integration.DingTalkUserBindingDO;
import com.kyx.service.hr.dal.dataobject.scope.ScopeDeptDO;
import com.kyx.service.hr.dal.dataobject.scope.ScopeRoleDO;
import com.kyx.service.hr.dal.dataobject.scope.ScopeUserDO;
import com.kyx.service.hr.dal.dataobject.scope.ScopeUserRoleDO;
import com.kyx.service.hr.dal.mysql.employee.EmployeeProfileMapper;
import com.kyx.service.hr.dal.mysql.integration.DingTalkUserBindingMapper;
import com.kyx.service.hr.dal.mysql.scope.ScopeDeptMapper;
import com.kyx.service.hr.dal.mysql.scope.ScopeRoleMapper;
import com.kyx.service.hr.dal.mysql.scope.ScopeUserMapper;
import com.kyx.service.hr.dal.mysql.scope.ScopeUserRoleMapper;
import com.kyx.service.hr.service.notice.HrNoticeRecordService;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DingTalkSystemUpdateNoticeService {

    private static final String ROLE_LEADER = "队长";
    private static final String DEPT_TECH = "技术部";
    private static final int USER_STATUS_ENABLED = 0;
    private static final int ROLE_STATUS_ENABLED = 0;
    private static final int DEPT_STATUS_ENABLED = 0;
    private static final int PROFILE_STATUS_ENABLED = 1;
    private static final String SOURCE_LEADER = "队长";
    private static final String SOURCE_TECH = "技术部";
    private static final String SOURCE_ROLE_PREFIX = "角色：";
    private static final String SOURCE_DEPT_PREFIX = "部门：";
    private static final String SOURCE_MANUAL = "手动选择";
    private static final String DEFAULT_TITLE = "连途系统更新通知";
    private static final String DEFAULT_CONTENT = "连途系统正在更新中。";
    private static final String HEAD_TEXT = "系统更新";
    private static final String HEAD_COLOR = "FF1677FF";
    private static final String STATUS_VALUE = "系统更新中";
    private static final String STATUS_COLOR = "0xFF1677FF";
    private static final String DEFAULT_DETAIL_PATH = "/sync/dingtalk";
    private static final String BUSINESS_TYPE_SYSTEM_UPDATE = "SYSTEM_UPDATE";
    private static final DateTimeFormatter SEND_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Resource
    private ScopeRoleMapper scopeRoleMapper;
    @Resource
    private ScopeUserRoleMapper scopeUserRoleMapper;
    @Resource
    private ScopeUserMapper scopeUserMapper;
    @Resource
    private ScopeDeptMapper scopeDeptMapper;
    @Resource
    private EmployeeProfileMapper employeeProfileMapper;
    @Resource
    private DingTalkUserBindingMapper dingTalkUserBindingMapper;
    @Resource
    private DingTalkMessageNotifyService dingTalkMessageNotifyService;
    @Resource
    private WebProperties webProperties;
    @Resource
    private HrNoticeRecordService noticeRecordService;

    public SystemUpdateNoticePreview previewRecipients(DingTalkSystemUpdateNoticeReqVO reqVO) {
        List<Recipient> recipients = resolveRecipients(reqVO);
        SystemUpdateNoticePreview preview = new SystemUpdateNoticePreview();
        applyRecipientCounts(preview, recipients);
        preview.setRecipients(recipients);
        return preview;
    }

    public List<Recipient> listAvailableRecipients() {
        List<DingTalkUserBindingDO> bindings = dingTalkUserBindingMapper.selectListActive();
        if (CollectionUtils.isEmpty(bindings)) {
            return Collections.emptyList();
        }
        Map<Long, Candidate> candidates = new LinkedHashMap<>();
        for (DingTalkUserBindingDO binding : bindings) {
            if (binding.getOaUserId() != null) {
                addCandidate(candidates, binding.getOaUserId(), SOURCE_MANUAL);
            }
        }
        List<Recipient> recipients = buildRecipients(candidates);
        for (Recipient recipient : recipients) {
            recipient.setSources(Collections.emptyList());
        }
        return recipients;
    }

    public SystemUpdateNoticeSendReport send(DingTalkSystemUpdateNoticeReqVO reqVO) {
        List<Recipient> recipients = resolveRecipients(reqVO);
        SystemUpdateNoticeSendReport report = new SystemUpdateNoticeSendReport();
        applyRecipientCounts(report, recipients);
        report.setTitle(resolveTitle(reqVO));
        report.setContent(resolveContent(reqVO));
        report.setRecipients(recipients);
        report.setTaskIds(new ArrayList<>());
        report.setFailedRecipients(new ArrayList<>());

        if (recipients.isEmpty()) {
            return report;
        }

        String sendTime = LocalDateTime.now().format(SEND_TIME_FORMATTER);
        List<DingTalkMessageNotifyService.OaFormItem> form = buildForm(report, sendTime);
        DingTalkMessageNotifyService.OaStatusBar statusBar =
                new DingTalkMessageNotifyService.OaStatusBar(STATUS_VALUE, STATUS_COLOR);
        String detailUrl = resolveDetailUrl(reqVO);
        String messageUrl = dingTalkMessageNotifyService.buildDingTalkOpenAppUrl(detailUrl);
        Long noticeBatchId = System.currentTimeMillis();

        for (Recipient recipient : recipients) {
            String noticeKey = noticeRecordService.buildNoticeKey(HrNoticeRecordService.CHANNEL_DINGTALK,
                    BUSINESS_TYPE_SYSTEM_UPDATE, noticeBatchId, recipient.getUserId());
            try {
                List<DingTalkMessageNotifyService.TextSendResult> results =
                        dingTalkMessageNotifyService.sendOaCardToOaUserIds(Collections.singleton(recipient.getUserId()),
                                HEAD_TEXT, HEAD_COLOR, report.getTitle(), report.getContent(),
                                form, messageUrl, detailUrl, statusBar, null);
                report.setSentCount(report.getSentCount() + results.size());
                for (DingTalkMessageNotifyService.TextSendResult result : results) {
                    if (result.getTaskId() != null) {
                        report.getTaskIds().add(result.getTaskId());
                    }
                }
                recordNoticeSuccess(noticeKey, noticeBatchId, recipient.getUserId(),
                        report.getTitle(), report.getContent(), detailUrl);
            } catch (Exception ex) {
                report.setFailedCount(report.getFailedCount() + 1);
                FailedRecipient failed = new FailedRecipient();
                failed.setUserId(recipient.getUserId());
                failed.setNickname(recipient.getNickname());
                failed.setDeptName(recipient.getDeptName());
                failed.setReason(ex.getMessage());
                report.getFailedRecipients().add(failed);
                recordNoticeFailure(noticeKey, noticeBatchId, recipient.getUserId(),
                        report.getTitle(), report.getContent(), ex.getMessage(), detailUrl);
                log.warn("Send DingTalk system update notice failed, userId={}, nickname={}, reason={}",
                        recipient.getUserId(), recipient.getNickname(), ex.getMessage());
            }
        }
        return report;
    }

    private void recordNoticeSuccess(String noticeKey, Long businessId, Long receiverUserId,
                                     String title, String content, String remark) {
        try {
            noticeRecordService.recordSuccess(noticeKey, HrNoticeRecordService.CHANNEL_DINGTALK,
                    BUSINESS_TYPE_SYSTEM_UPDATE, businessId, receiverUserId, title, content, remark);
        } catch (Exception ex) {
            log.warn("Persist DingTalk system update notice success failed, receiverUserId={}, reason={}",
                    receiverUserId, ex.getMessage());
        }
    }

    private void recordNoticeFailure(String noticeKey, Long businessId, Long receiverUserId,
                                     String title, String content, String errorMessage, String remark) {
        try {
            noticeRecordService.recordFailure(noticeKey, HrNoticeRecordService.CHANNEL_DINGTALK,
                    BUSINESS_TYPE_SYSTEM_UPDATE, businessId, receiverUserId, title, content, errorMessage, remark);
        } catch (Exception ex) {
            log.warn("Persist DingTalk system update notice failure failed, receiverUserId={}, reason={}",
                    receiverUserId, ex.getMessage());
        }
    }

    private List<DingTalkMessageNotifyService.OaFormItem> buildForm(SystemUpdateNoticeSendReport report,
                                                                    String sendTime) {
        List<DingTalkMessageNotifyService.OaFormItem> form = new ArrayList<>();
        form.add(new DingTalkMessageNotifyService.OaFormItem("通知类型：", "系统更新"));
        form.add(new DingTalkMessageNotifyService.OaFormItem("接收范围：", buildSourceSummary(report)));
        form.add(new DingTalkMessageNotifyService.OaFormItem("接收人数：", report.getTotalCount() + " 人"));
        form.add(new DingTalkMessageNotifyService.OaFormItem("发送时间：", sendTime));
        return form;
    }

    private String buildSourceSummary(SystemUpdateNoticePreview preview) {
        List<String> parts = new ArrayList<>();
        if (preview.getRoleCount() > 0) {
            parts.add("角色匹配 " + preview.getRoleCount() + " 人");
        }
        if (preview.getDeptCount() > 0) {
            parts.add("部门匹配 " + preview.getDeptCount() + " 人");
        }
        if (preview.getLeaderCount() > 0) {
            parts.add("队长 " + preview.getLeaderCount() + " 人");
        }
        if (preview.getTechDeptCount() > 0) {
            parts.add("技术部 " + preview.getTechDeptCount() + " 人");
        }
        if (preview.getManualCount() > 0) {
            parts.add("手动选择 " + preview.getManualCount() + " 人");
        }
        return parts.isEmpty() ? "-" : String.join("，", parts);
    }

    private List<Recipient> resolveRecipients(DingTalkSystemUpdateNoticeReqVO reqVO) {
        Set<Long> roleIds = toOrderedIds(reqVO == null ? null : reqVO.getRoleIds());
        Set<Long> deptIds = toOrderedIds(reqVO == null ? null : reqVO.getDeptIds());
        Set<Long> manualUserIds = toOrderedIds(reqVO == null ? null : reqVO.getReceiverUserIds());
        Set<Long> excludeUserIds = toOrderedIds(reqVO == null ? null : reqVO.getExcludeUserIds());
        boolean legacyDefault = reqVO == null || (reqVO.getIncludeLeaders() == null
                && reqVO.getIncludeTechDept() == null
                && roleIds.isEmpty() && deptIds.isEmpty() && manualUserIds.isEmpty());
        boolean includeLeaders = legacyDefault || Boolean.TRUE.equals(reqVO.getIncludeLeaders());
        boolean includeTechDept = legacyDefault || Boolean.TRUE.equals(reqVO.getIncludeTechDept());
        if (!includeLeaders && !includeTechDept && roleIds.isEmpty() && deptIds.isEmpty() && manualUserIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Candidate> candidates = new LinkedHashMap<>();
        if (includeLeaders) {
            addLeaderCandidates(candidates);
        }
        if (includeTechDept) {
            addTechDeptCandidates(candidates);
        }
        addRoleCandidates(candidates, roleIds);
        addDeptCandidates(candidates, deptIds);
        for (Long userId : manualUserIds) {
            addCandidate(candidates, userId, SOURCE_MANUAL);
        }
        for (Long userId : excludeUserIds) {
            candidates.remove(userId);
        }
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        return buildRecipients(candidates);
    }

    private List<Recipient> buildRecipients(Map<Long, Candidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> candidateUserIds = candidates.keySet();
        Map<Long, ScopeUserDO> userMap = selectEnabledUsers(candidateUserIds);
        Map<Long, EmployeeProfileDO> activeProfileMap = selectActiveProfiles(userMap.keySet());
        Map<Long, DingTalkUserBindingDO> activeBindingMap = selectActiveBindings(userMap.keySet());
        Map<Long, String> deptNameMap = selectDeptNameMap(userMap.values());

        Map<String, Recipient> recipientsByDingUserId = new LinkedHashMap<>();
        for (Map.Entry<Long, Candidate> entry : candidates.entrySet()) {
            Long userId = entry.getKey();
            ScopeUserDO user = userMap.get(userId);
            EmployeeProfileDO profile = activeProfileMap.get(userId);
            DingTalkUserBindingDO binding = activeBindingMap.get(userId);
            if (user == null || profile == null || binding == null || !StringUtils.hasText(binding.getDingUserId())) {
                continue;
            }
            Recipient recipient = recipientsByDingUserId.get(binding.getDingUserId());
            if (recipient == null) {
                recipient = new Recipient();
                recipient.setUserId(userId);
                recipient.setNickname(StringUtils.hasText(user.getNickname()) ? user.getNickname() : profile.getName());
                recipient.setUsername(user.getUsername());
                recipient.setDeptId(user.getDeptId());
                recipient.setDeptName(deptNameMap.getOrDefault(user.getDeptId(), ""));
                recipient.setDingUserId(binding.getDingUserId());
                recipient.setSources(new ArrayList<>(entry.getValue().getSources()));
                recipientsByDingUserId.put(binding.getDingUserId(), recipient);
            } else {
                mergeSources(recipient.getSources(), entry.getValue().getSources());
            }
        }

        List<Recipient> recipients = new ArrayList<>(recipientsByDingUserId.values());
        recipients.sort(Comparator
                .comparing((Recipient item) -> sourceSort(item.getSources()))
                .thenComparing(item -> empty(item.getDeptName()))
                .thenComparing(item -> empty(item.getNickname()))
                .thenComparing(item -> item.getUserId() == null ? 0L : item.getUserId()));
        return recipients;
    }

    private void addLeaderCandidates(Map<Long, Candidate> candidates) {
        List<ScopeRoleDO> roles = scopeRoleMapper.selectList(new LambdaQueryWrapperX<ScopeRoleDO>()
                .eq(ScopeRoleDO::getName, ROLE_LEADER)
                .eq(ScopeRoleDO::getStatus, ROLE_STATUS_ENABLED));
        if (CollectionUtils.isEmpty(roles)) {
            return;
        }
        Set<Long> roleIds = roles.stream().map(ScopeRoleDO::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        List<ScopeUserRoleDO> userRoles = scopeUserRoleMapper.selectListByRoleIds(roleIds);
        for (ScopeUserRoleDO userRole : userRoles) {
            if (userRole.getUserId() != null) {
                addCandidate(candidates, userRole.getUserId(), SOURCE_LEADER);
            }
        }
    }

    private void addTechDeptCandidates(Map<Long, Candidate> candidates) {
        List<ScopeDeptDO> depts = scopeDeptMapper.selectList(new LambdaQueryWrapperX<ScopeDeptDO>()
                .eq(ScopeDeptDO::getName, DEPT_TECH)
                .eq(ScopeDeptDO::getStatus, DEPT_STATUS_ENABLED));
        if (CollectionUtils.isEmpty(depts)) {
            return;
        }
        Set<Long> deptIds = depts.stream().map(ScopeDeptDO::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        List<ScopeUserDO> users = scopeUserMapper.selectListByDeptIds(deptIds, USER_STATUS_ENABLED);
        for (ScopeUserDO user : users) {
            if (user.getId() != null) {
                addCandidate(candidates, user.getId(), SOURCE_TECH);
            }
        }
    }

    private void addRoleCandidates(Map<Long, Candidate> candidates, Collection<Long> selectedRoleIds) {
        if (CollectionUtils.isEmpty(selectedRoleIds)) {
            return;
        }
        List<ScopeRoleDO> roles = scopeRoleMapper.selectList(new LambdaQueryWrapperX<ScopeRoleDO>()
                .in(ScopeRoleDO::getId, selectedRoleIds)
                .eq(ScopeRoleDO::getStatus, ROLE_STATUS_ENABLED)
                .orderByAsc(ScopeRoleDO::getSort, ScopeRoleDO::getId));
        if (CollectionUtils.isEmpty(roles)) {
            return;
        }
        Map<Long, String> roleSourceMap = roles.stream()
                .filter(role -> role.getId() != null)
                .collect(Collectors.toMap(ScopeRoleDO::getId,
                        role -> SOURCE_ROLE_PREFIX + (StringUtils.hasText(role.getName())
                                ? role.getName() : String.valueOf(role.getId())),
                        (left, right) -> left, LinkedHashMap::new));
        List<ScopeUserRoleDO> userRoles = scopeUserRoleMapper.selectListByRoleIds(roleSourceMap.keySet());
        for (ScopeUserRoleDO userRole : userRoles) {
            String source = roleSourceMap.get(userRole.getRoleId());
            if (userRole.getUserId() != null && StringUtils.hasText(source)) {
                addCandidate(candidates, userRole.getUserId(), source);
            }
        }
    }

    private void addDeptCandidates(Map<Long, Candidate> candidates, Collection<Long> selectedDeptIds) {
        if (CollectionUtils.isEmpty(selectedDeptIds)) {
            return;
        }
        List<ScopeDeptDO> depts = scopeDeptMapper.selectList(new LambdaQueryWrapperX<ScopeDeptDO>()
                .in(ScopeDeptDO::getId, selectedDeptIds)
                .eq(ScopeDeptDO::getStatus, DEPT_STATUS_ENABLED)
                .orderByAsc(ScopeDeptDO::getParentId, ScopeDeptDO::getSort, ScopeDeptDO::getId));
        if (CollectionUtils.isEmpty(depts)) {
            return;
        }
        Map<Long, String> deptSourceMap = depts.stream()
                .filter(dept -> dept.getId() != null)
                .collect(Collectors.toMap(ScopeDeptDO::getId,
                        dept -> SOURCE_DEPT_PREFIX + (StringUtils.hasText(dept.getName())
                                ? dept.getName() : String.valueOf(dept.getId())),
                        (left, right) -> left, LinkedHashMap::new));
        List<ScopeUserDO> users = scopeUserMapper.selectListByDeptIds(deptSourceMap.keySet(), USER_STATUS_ENABLED);
        for (ScopeUserDO user : users) {
            String source = deptSourceMap.get(user.getDeptId());
            if (user.getId() != null && StringUtils.hasText(source)) {
                addCandidate(candidates, user.getId(), source);
            }
        }
    }

    private void addCandidate(Map<Long, Candidate> candidates, Long userId, String source) {
        Candidate candidate = candidates.computeIfAbsent(userId, key -> new Candidate());
        candidate.getSources().add(source);
    }

    private Set<Long> toOrderedIds(Collection<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptySet();
        }
        return ids.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Map<Long, ScopeUserDO> selectEnabledUsers(Collection<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        List<ScopeUserDO> users = scopeUserMapper.selectList(new LambdaQueryWrapperX<ScopeUserDO>()
                .in(ScopeUserDO::getId, userIds)
                .eq(ScopeUserDO::getStatus, USER_STATUS_ENABLED));
        return users.stream().collect(Collectors.toMap(ScopeUserDO::getId, item -> item, (left, right) -> left,
                LinkedHashMap::new));
    }

    private Map<Long, EmployeeProfileDO> selectActiveProfiles(Collection<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        List<EmployeeProfileDO> profiles = employeeProfileMapper.selectList(new LambdaQueryWrapperX<EmployeeProfileDO>()
                .in(EmployeeProfileDO::getUserId, userIds)
                .eq(EmployeeProfileDO::getStatus, PROFILE_STATUS_ENABLED));
        return profiles.stream()
                .filter(item -> item.getUserId() != null)
                .collect(Collectors.toMap(EmployeeProfileDO::getUserId, item -> item, (left, right) -> left,
                        LinkedHashMap::new));
    }

    private Map<Long, DingTalkUserBindingDO> selectActiveBindings(Collection<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        List<DingTalkUserBindingDO> bindings = dingTalkUserBindingMapper.selectListByOaUserIds(userIds);
        Map<Long, DingTalkUserBindingDO> result = new LinkedHashMap<>();
        for (DingTalkUserBindingDO binding : bindings) {
            if (binding.getOaUserId() == null || !Boolean.TRUE.equals(binding.getDingActive())
                    || !StringUtils.hasText(binding.getDingUserId())) {
                continue;
            }
            result.putIfAbsent(binding.getOaUserId(), binding);
        }
        return result;
    }

    private Map<Long, String> selectDeptNameMap(Collection<ScopeUserDO> users) {
        if (CollectionUtils.isEmpty(users)) {
            return Collections.emptyMap();
        }
        Set<Long> deptIds = users.stream()
                .map(ScopeUserDO::getDeptId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (deptIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ScopeDeptDO> depts = scopeDeptMapper.selectBatchIds(deptIds);
        Map<Long, String> result = new HashMap<>();
        for (ScopeDeptDO dept : depts) {
            result.put(dept.getId(), dept.getName());
        }
        return result;
    }

    private void applyRecipientCounts(SystemUpdateNoticePreview preview, List<Recipient> recipients) {
        preview.setTotalCount(recipients.size());
        preview.setLeaderCount(countBySource(recipients, SOURCE_LEADER));
        preview.setTechDeptCount(countBySource(recipients, SOURCE_TECH));
        preview.setRoleCount(countBySourcePrefix(recipients, SOURCE_ROLE_PREFIX));
        preview.setDeptCount(countBySourcePrefix(recipients, SOURCE_DEPT_PREFIX));
        preview.setManualCount(countBySource(recipients, SOURCE_MANUAL));
    }

    private int countBySource(List<Recipient> recipients, String source) {
        if (CollectionUtils.isEmpty(recipients)) {
            return 0;
        }
        int count = 0;
        for (Recipient recipient : recipients) {
            if (recipient.getSources() != null && recipient.getSources().contains(source)) {
                count++;
            }
        }
        return count;
    }

    private int countBySourcePrefix(List<Recipient> recipients, String sourcePrefix) {
        if (CollectionUtils.isEmpty(recipients)) {
            return 0;
        }
        int count = 0;
        for (Recipient recipient : recipients) {
            if (hasSourcePrefix(recipient.getSources(), sourcePrefix)) {
                count++;
            }
        }
        return count;
    }

    private boolean hasSourcePrefix(List<String> sources, String sourcePrefix) {
        if (sources == null || !StringUtils.hasText(sourcePrefix)) {
            return false;
        }
        for (String source : sources) {
            if (StringUtils.hasText(source) && source.startsWith(sourcePrefix)) {
                return true;
            }
        }
        return false;
    }

    private void mergeSources(List<String> target, Collection<String> sources) {
        if (target == null || sources == null) {
            return;
        }
        for (String source : sources) {
            if (!target.contains(source)) {
                target.add(source);
            }
        }
    }

    private int sourceSort(List<String> sources) {
        if (sources != null && sources.contains(SOURCE_MANUAL)) {
            return 0;
        }
        if (sources != null && (hasSourcePrefix(sources, SOURCE_ROLE_PREFIX) || sources.contains(SOURCE_LEADER))) {
            return 1;
        }
        if (sources != null && (hasSourcePrefix(sources, SOURCE_DEPT_PREFIX) || sources.contains(SOURCE_TECH))) {
            return 2;
        }
        return 3;
    }

    private String resolveTitle(DingTalkSystemUpdateNoticeReqVO reqVO) {
        String title = trim(reqVO == null ? null : reqVO.getTitle());
        return StringUtils.hasText(title) ? title : DEFAULT_TITLE;
    }

    private String resolveContent(DingTalkSystemUpdateNoticeReqVO reqVO) {
        String content = trim(reqVO == null ? null : reqVO.getContent());
        return StringUtils.hasText(content) ? content : DEFAULT_CONTENT;
    }

    private String resolveDetailUrl(DingTalkSystemUpdateNoticeReqVO reqVO) {
        String detailUrl = trim(reqVO == null ? null : reqVO.getDetailUrl());
        if (StringUtils.hasText(detailUrl)) {
            return detailUrl;
        }
        String adminUiUrl = webProperties == null || webProperties.getAdminUi() == null
                ? null : trim(webProperties.getAdminUi().getUrl());
        if (!StringUtils.hasText(adminUiUrl)) {
            return "";
        }
        return trimTrailingSlash(adminUiUrl) + DEFAULT_DETAIL_PATH;
    }

    private String trimTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String empty(String value) {
        return value == null ? "" : value;
    }

    @Data
    private static class Candidate {
        private Set<String> sources = new LinkedHashSet<>();
    }

    @Data
    public static class Recipient {
        private Long userId;
        private String nickname;
        private String username;
        private Long deptId;
        private String deptName;
        private String dingUserId;
        private List<String> sources = new ArrayList<>();
    }

    @Data
    public static class FailedRecipient {
        private Long userId;
        private String nickname;
        private String deptName;
        private String reason;
    }

    @Data
    public static class SystemUpdateNoticePreview {
        private Integer totalCount = 0;
        private Integer leaderCount = 0;
        private Integer techDeptCount = 0;
        private Integer roleCount = 0;
        private Integer deptCount = 0;
        private Integer manualCount = 0;
        private List<Recipient> recipients = Collections.emptyList();
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class SystemUpdateNoticeSendReport extends SystemUpdateNoticePreview {
        private String title;
        private String content;
        private Integer sentCount = 0;
        private Integer failedCount = 0;
        private List<Long> taskIds = Collections.emptyList();
        private List<FailedRecipient> failedRecipients = Collections.emptyList();
    }
}
