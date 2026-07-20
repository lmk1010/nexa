package com.kyx.service.hr.service.reminder;

import com.kyx.foundation.common.exception.enums.GlobalErrorCodeConstants;
import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.foundation.security.core.service.SecurityFrameworkService;
import com.kyx.foundation.security.core.util.SecurityFrameworkUtils;
import com.kyx.service.hr.controller.admin.lifecycle.vo.HrLifecycleReminderRespVO;
import com.kyx.service.hr.controller.admin.reminder.vo.HrReminderRecordPageReqVO;
import com.kyx.service.hr.controller.admin.reminder.vo.HrReminderRecordRespVO;
import com.kyx.service.hr.dal.dataobject.reminder.HrReminderRecordDO;
import com.kyx.service.hr.dal.dataobject.reminder.HrReminderRuleDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeProfileDO;
import com.kyx.service.hr.dal.mysql.employee.EmployeeProfileMapper;
import com.kyx.service.hr.dal.dataobject.todo.HrTodoTaskDO;
import com.kyx.service.hr.dal.mysql.reminder.HrReminderRecordMapper;
import com.kyx.service.hr.dal.mysql.reminder.HrReminderRuleMapper;
import com.kyx.service.hr.dal.mysql.todo.HrTodoTaskMapper;
import com.kyx.service.hr.service.lifecycle.HrLifecycleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Validated
@Slf4j
public class HrReminderRecordServiceImpl implements HrReminderRecordService {

    private static final String STATUS_UNREAD = "UNREAD";
    private static final String STATUS_READ = "READ";
    private static final String STATUS_EXPIRED = "EXPIRED";
    private static final String SOURCE_LIFECYCLE = "LIFECYCLE";
    private static final String SOURCE_TODO = "TODO";
    private static final String STATUS_OPEN = "OPEN";
    private static final int SOURCE_SYNC_LIMIT = 500;

    @Resource
    private HrReminderRecordMapper reminderRecordMapper;
    @Resource
    private HrReminderRuleMapper reminderRuleMapper;
    @Resource
    private HrLifecycleService hrLifecycleService;
    @Resource
    private HrTodoTaskMapper hrTodoTaskMapper;
    @Resource
    private SecurityFrameworkService securityFrameworkService;
    @Resource
    private EmployeeProfileMapper employeeProfileMapper;

    @Override
    public PageResult<HrReminderRecordRespVO> getPage(HrReminderRecordPageReqVO pageReqVO) {
        refreshGeneratedRecords();
        boolean manage = canManage();
        PageResult<HrReminderRecordDO> pageResult = reminderRecordMapper.selectPage(
                pageReqVO, manage, SecurityFrameworkUtils.getLoginUserId());
        PageResult<HrReminderRecordRespVO> result = BeanUtils.toBean(pageResult, HrReminderRecordRespVO.class);
        List<HrReminderRecordRespVO> records = result.getList();
        if (records != null && !records.isEmpty()) {
            Set<Long> profileIds = new HashSet<>();
            Set<Long> userIds = new HashSet<>();
            for (HrReminderRecordRespVO item : records) {
                if (item.getProfileId() != null) {
                    profileIds.add(item.getProfileId());
                }
                if (item.getReceiverUserId() != null) {
                    userIds.add(item.getReceiverUserId());
                }
            }
            Map<Long, String> profileNameMap = loadProfileNameMapByIds(profileIds);
            Map<Long, String> userNameMap = loadProfileNameMapByUserIds(userIds);
            for (HrReminderRecordRespVO item : records) {
                String employeeName = item.getProfileId() == null
                        ? userNameMap.get(item.getReceiverUserId())
                        : profileNameMap.get(item.getProfileId());
                if (!StringUtils.hasText(employeeName) && item.getReceiverUserId() != null) {
                    employeeName = userNameMap.get(item.getReceiverUserId());
                }
                item.setEmployeeName(employeeName);
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void read(Long id) {
        HrReminderRecordDO record = reminderRecordMapper.selectById(id);
        if (record == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND, "提醒记录不存在");
        }
        if (!canManage() && !isMine(record)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.FORBIDDEN, "无权处理该提醒记录");
        }
        if (STATUS_READ.equals(record.getStatus())) {
            return;
        }
        HrReminderRecordDO updateDO = new HrReminderRecordDO();
        updateDO.setId(record.getId());
        updateDO.setStatus(STATUS_READ);
        updateDO.setReadTime(LocalDateTime.now());
        reminderRecordMapper.updateById(updateDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer readAll(Boolean mineOnly) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            return 0;
        }
        boolean manage = canManage();
        LambdaQueryWrapperX<HrReminderRecordDO> query = new LambdaQueryWrapperX<HrReminderRecordDO>()
                .eq(HrReminderRecordDO::getStatus, STATUS_UNREAD)
                .orderByDesc(HrReminderRecordDO::getTriggerTime)
                .last("LIMIT 1000");
        if (Boolean.TRUE.equals(mineOnly) || !manage) {
            query.eq(HrReminderRecordDO::getReceiverUserId, loginUserId);
        }
        List<HrReminderRecordDO> records = reminderRecordMapper.selectList(query);
        int changed = 0;
        for (HrReminderRecordDO record : records) {
            if (Boolean.FALSE.equals(mineOnly) && !manage) {
                continue;
            }
            HrReminderRecordDO updateDO = new HrReminderRecordDO();
            updateDO.setId(record.getId());
            updateDO.setStatus(STATUS_READ);
            updateDO.setReadTime(LocalDateTime.now());
            reminderRecordMapper.updateById(updateDO);
            changed++;
        }
        return changed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer refreshGeneratedRecords() {
        Map<String, HrReminderRuleDO> ruleMap = loadEnabledRuleMap();
        Set<String> activeKeys = new HashSet<>();
        int changed = 0;
        changed += syncLifecycleRecords(ruleMap, activeKeys);
        changed += syncTodoRecords(ruleMap, activeKeys);
        changed += expireStaleRecords(activeKeys);
        return changed;
    }

    private int syncLifecycleRecords(Map<String, HrReminderRuleDO> ruleMap, Set<String> activeKeys) {
        List<HrLifecycleReminderRespVO> reminders = hrLifecycleService.getReminderList();
        if (reminders == null || reminders.isEmpty()) {
            return 0;
        }
        int changed = 0;
        for (HrLifecycleReminderRespVO reminder : reminders) {
            if (reminder == null || !shouldInclude(ruleMap, reminder.getReminderType())) {
                continue;
            }
            String recordKey = buildLifecycleRecordKey(reminder);
            activeKeys.add(recordKey);
            changed += upsertRecord(recordKey, ruleMap.get(reminder.getReminderType()), SOURCE_LIFECYCLE,
                    reminder.getReminderType(), resolveLifecycleBusinessId(reminder),
                    reminder.getProfileId(), resolveReceiverUserId(reminder.getProfileId()),
                    reminder.getTitle(), reminder.getDescription(), reminder.getSeverity(),
                    lifecycleRoute(reminder), resolveLifecycleSourceId(reminder), null);
        }
        return changed;
    }

    private int syncTodoRecords(Map<String, HrReminderRuleDO> ruleMap, Set<String> activeKeys) {
        List<HrTodoTaskDO> todoList = hrTodoTaskMapper.selectList(new LambdaQueryWrapperX<HrTodoTaskDO>()
                .eq(HrTodoTaskDO::getStatus, STATUS_OPEN)
                .orderByAsc(HrTodoTaskDO::getDueTime)
                .orderByDesc(HrTodoTaskDO::getId)
                .last("LIMIT " + SOURCE_SYNC_LIMIT));
        if (todoList == null || todoList.isEmpty()) {
            return 0;
        }
        int changed = 0;
        for (HrTodoTaskDO todo : todoList) {
            if (todo == null || !shouldInclude(ruleMap, todo.getBusinessType())) {
                continue;
            }
            String recordKey = buildTodoRecordKey(todo);
            activeKeys.add(recordKey);
            changed += upsertRecord(recordKey, ruleMap.get(todo.getBusinessType()), SOURCE_TODO,
                    todo.getBusinessType(), todo.getBusinessId(), todo.getProfileId(),
                    todo.getAssigneeUserId(), todo.getTitle(), todo.getContent(), todo.getPriority(),
                    todo.getRoutePath(), todo.getBusinessId(), todo.getId());
        }
        return changed;
    }

    private int expireStaleRecords(Set<String> activeKeys) {
        List<HrReminderRecordDO> openRecords = reminderRecordMapper.selectList(new LambdaQueryWrapperX<HrReminderRecordDO>()
                .eq(HrReminderRecordDO::getStatus, STATUS_UNREAD)
                .in(HrReminderRecordDO::getSourceType, SOURCE_LIFECYCLE, SOURCE_TODO)
                .orderByDesc(HrReminderRecordDO::getTriggerTime)
                .last("LIMIT 1000"));
        int changed = 0;
        for (HrReminderRecordDO record : openRecords) {
            if (record.getRecordKey() == null || activeKeys.contains(record.getRecordKey())) {
                continue;
            }
            HrReminderRecordDO updateDO = new HrReminderRecordDO();
            updateDO.setId(record.getId());
            updateDO.setStatus(STATUS_EXPIRED);
            reminderRecordMapper.updateById(updateDO);
            changed++;
        }
        return changed;
    }

    private int upsertRecord(String recordKey, HrReminderRuleDO rule, String sourceType,
                             String ruleCode, Long businessId, Long profileId, Long receiverUserId,
                             String title, String content, String severity, String routePath,
                             Long sourceId, Long relatedId) {
        HrReminderRecordDO existing = reminderRecordMapper.selectByRecordKey(recordKey);
        if (existing == null) {
            HrReminderRecordDO insertDO = new HrReminderRecordDO();
            insertDO.setRecordKey(recordKey);
            insertDO.setRuleId(rule == null ? null : rule.getId());
            insertDO.setRuleCode(ruleCode);
            insertDO.setRuleName(rule == null ? null : rule.getRuleName());
            insertDO.setBusinessType(rule == null ? ruleCode : rule.getBusinessType());
            insertDO.setBusinessId(businessId);
            insertDO.setReceiverUserId(receiverUserId);
            insertDO.setProfileId(profileId);
            insertDO.setTitle(title);
            insertDO.setContent(content);
            insertDO.setSeverity(normalizeSeverity(severity));
            insertDO.setStatus(STATUS_UNREAD);
            insertDO.setRoutePath(routePath);
            insertDO.setSourceType(sourceType);
            insertDO.setSourceId(sourceId);
            insertDO.setTriggerTime(LocalDateTime.now());
            reminderRecordMapper.insert(insertDO);
            return 1;
        }
        HrReminderRecordDO updateDO = new HrReminderRecordDO();
        updateDO.setId(existing.getId());
        updateDO.setRuleId(rule == null ? existing.getRuleId() : rule.getId());
        updateDO.setRuleCode(ruleCode);
        updateDO.setRuleName(rule == null ? existing.getRuleName() : rule.getRuleName());
        updateDO.setBusinessType(rule == null ? existing.getBusinessType() : rule.getBusinessType());
        updateDO.setBusinessId(businessId);
        updateDO.setReceiverUserId(receiverUserId);
        updateDO.setProfileId(profileId);
        updateDO.setTitle(title);
        updateDO.setContent(content);
        updateDO.setSeverity(normalizeSeverity(severity));
        updateDO.setRoutePath(routePath);
        updateDO.setSourceType(sourceType);
        updateDO.setSourceId(sourceId);
        if (!STATUS_READ.equals(existing.getStatus())) {
            updateDO.setStatus(existing.getStatus());
        }
        if (existing.getReadTime() != null) {
            updateDO.setReadTime(existing.getReadTime());
        }
        updateDO.setTriggerTime(existing.getTriggerTime() == null ? LocalDateTime.now() : existing.getTriggerTime());
        reminderRecordMapper.updateById(updateDO);
        return 1;
    }

    private Map<String, HrReminderRuleDO> loadEnabledRuleMap() {
        Map<String, HrReminderRuleDO> ruleMap = new HashMap<>();
        List<HrReminderRuleDO> rules = reminderRuleMapper.selectList(new LambdaQueryWrapperX<HrReminderRuleDO>());
        if (rules == null) {
            return ruleMap;
        }
        for (HrReminderRuleDO rule : rules) {
            if (rule != null && StringUtils.hasText(rule.getRuleCode())) {
                ruleMap.put(rule.getRuleCode(), rule);
            }
        }
        return ruleMap;
    }

    private boolean shouldInclude(Map<String, HrReminderRuleDO> ruleMap, String ruleCode) {
        if (!StringUtils.hasText(ruleCode)) {
            return false;
        }
        HrReminderRuleDO rule = ruleMap.get(ruleCode);
        return rule == null || Boolean.TRUE.equals(rule.getEnabled());
    }

    private String buildLifecycleRecordKey(HrLifecycleReminderRespVO reminder) {
        return String.join(":",
                SOURCE_LIFECYCLE,
                normalizeKey(reminder.getReminderType()),
                normalizeKey(reminder.getProfileId()),
                normalizeKey(reminder.getEventId()),
                normalizeKey(reminder.getTaskId()),
                normalizeKey(reminder.getDueDate()));
    }

    private String buildTodoRecordKey(HrTodoTaskDO todo) {
        return String.join(":",
                SOURCE_TODO,
                normalizeKey(todo.getBusinessType()),
                normalizeKey(todo.getBusinessId()),
                normalizeKey(todo.getAssigneeUserId()),
                normalizeKey(todo.getDueTime()));
    }

    private String lifecycleRoute(HrLifecycleReminderRespVO reminder) {
        if (reminder.getEventId() != null) {
            return "/hr/lifecycle/workbench?eventId=" + reminder.getEventId();
        }
        if (reminder.getProfileId() != null) {
            return "/hr/employee/detail?id=" + reminder.getProfileId();
        }
        return "/hr/lifecycle/workbench";
    }

    private Long resolveLifecycleBusinessId(HrLifecycleReminderRespVO reminder) {
        if (reminder.getEventId() != null) {
            return reminder.getEventId();
        }
        if (reminder.getTaskId() != null) {
            return reminder.getTaskId();
        }
        if (reminder.getEntryId() != null) {
            return reminder.getEntryId();
        }
        return reminder.getProfileId();
    }

    private Long resolveLifecycleSourceId(HrLifecycleReminderRespVO reminder) {
        if (reminder.getTaskId() != null) {
            return reminder.getTaskId();
        }
        return resolveLifecycleBusinessId(reminder);
    }

    private Long resolveReceiverUserId(Long profileId) {
        if (profileId == null) {
            return null;
        }
        EmployeeProfileDO profile = employeeProfileMapper.selectById(profileId);
        return profile == null ? null : profile.getUserId();
    }

    private Map<Long, String> loadProfileNameMapByIds(Set<Long> profileIds) {
        Map<Long, String> profileNameMap = new HashMap<>();
        if (profileIds == null || profileIds.isEmpty()) {
            return profileNameMap;
        }
        List<EmployeeProfileDO> profiles = employeeProfileMapper.selectList(new LambdaQueryWrapperX<EmployeeProfileDO>()
                .inIfPresent(EmployeeProfileDO::getId, profileIds));
        for (EmployeeProfileDO profile : profiles) {
            if (profile != null && profile.getId() != null && StringUtils.hasText(profile.getName())) {
                profileNameMap.put(profile.getId(), profile.getName());
            }
        }
        return profileNameMap;
    }

    private Map<Long, String> loadProfileNameMapByUserIds(Set<Long> userIds) {
        Map<Long, String> profileNameMap = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return profileNameMap;
        }
        List<EmployeeProfileDO> profiles = employeeProfileMapper.selectList(new LambdaQueryWrapperX<EmployeeProfileDO>()
                .inIfPresent(EmployeeProfileDO::getUserId, userIds));
        for (EmployeeProfileDO profile : profiles) {
            if (profile != null && profile.getUserId() != null && StringUtils.hasText(profile.getName())) {
                profileNameMap.put(profile.getUserId(), profile.getName());
            }
        }
        return profileNameMap;
    }

    private String normalizeSeverity(String severity) {
        if (!StringUtils.hasText(severity)) {
            return "LOW";
        }
        String value = severity.trim().toUpperCase();
        if ("HIGH".equals(value) || "MEDIUM".equals(value) || "LOW".equals(value)) {
            return value;
        }
        return "LOW";
    }

    private String normalizeKey(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private boolean canManage() {
        try {
            return securityFrameworkService.hasPermission("hr:reminder:query")
                    || securityFrameworkService.hasPermission("hr:reminder:rule");
        } catch (Exception ex) {
            log.warn("check hr reminder permission failed: {}", ex.getMessage());
            return false;
        }
    }

    private boolean isMine(HrReminderRecordDO record) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        return loginUserId != null && loginUserId.equals(record.getReceiverUserId());
    }

}
