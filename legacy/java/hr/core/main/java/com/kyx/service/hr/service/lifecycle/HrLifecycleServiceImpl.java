package com.kyx.service.hr.service.lifecycle;

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
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.hr.controller.admin.lifecycle.vo.HrLifecycleCalendarEventRespVO;
import com.kyx.service.hr.controller.admin.lifecycle.vo.HrLifecycleEventPageReqVO;
import com.kyx.service.hr.controller.admin.lifecycle.vo.HrLifecycleEventRespVO;
import com.kyx.service.hr.controller.admin.lifecycle.vo.HrLifecycleRegularizationCreateReqVO;
import com.kyx.service.hr.controller.admin.lifecycle.vo.HrLifecycleReminderRespVO;
import com.kyx.service.hr.controller.admin.lifecycle.vo.HrLifecycleResignationCreateReqVO;
import com.kyx.service.hr.controller.admin.lifecycle.vo.HrLifecycleSalaryAdjustCreateReqVO;
import com.kyx.service.hr.controller.admin.lifecycle.vo.HrLifecycleTaskCompleteReqVO;
import com.kyx.service.hr.controller.admin.lifecycle.vo.HrLifecycleTaskRespVO;
import com.kyx.service.hr.controller.admin.lifecycle.vo.HrLifecycleTransferCreateReqVO;
import com.kyx.service.hr.controller.admin.lifecycle.vo.HrLifecycleWorkbenchRespVO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeEntryDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeGrowthLogDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeOperationLogDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeProfileDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeSalaryDO;
import com.kyx.service.hr.dal.dataobject.lifecycle.HrLifecycleEventDO;
import com.kyx.service.hr.dal.dataobject.lifecycle.HrLifecycleTaskDO;
import com.kyx.service.hr.dal.mysql.employee.EmployeeEntryMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeGrowthLogMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeOperationLogMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeProfileMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeSalaryMapper;
import com.kyx.service.hr.dal.mysql.lifecycle.HrLifecycleEventMapper;
import com.kyx.service.hr.dal.mysql.lifecycle.HrLifecycleTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.kyx.service.hr.enums.ErrorCodeConstants.*;

@Service
@Validated
@Slf4j
public class HrLifecycleServiceImpl implements HrLifecycleService {

    private static final String EVENT_ONBOARDING_CONFIRMED = "ONBOARDING_CONFIRMED";
    private static final String EVENT_ONBOARDING_CREATED = "ONBOARDING_CREATED";
    private static final String EVENT_REHIRE_CREATED = "REHIRE_CREATED";
    private static final String EVENT_PROBATION_STARTED = "PROBATION_STARTED";
    private static final String EVENT_REGULARIZATION_REQUESTED = "REGULARIZATION_REQUESTED";
    private static final String EVENT_TRANSFER_REQUESTED = "TRANSFER_REQUESTED";
    private static final String EVENT_SALARY_ADJUST_REQUESTED = "SALARY_ADJUST_REQUESTED";
    private static final String EVENT_RESIGN_REQUESTED = "RESIGN_REQUESTED";
    private static final String EVENT_RESIGN_EFFECTIVE = "RESIGN_EFFECTIVE";

    public static final String PROCESS_KEY_REGULARIZATION = "hr_lifecycle_regularization";
    public static final String PROCESS_KEY_TRANSFER = "hr_lifecycle_transfer";
    public static final String PROCESS_KEY_SALARY_ADJUST = "hr_lifecycle_salary_adjust";
    public static final String PROCESS_KEY_RESIGNATION = "hr_lifecycle_resignation";

    private static final String STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";
    private static final String STATUS_PENDING_HANDOVER = "PENDING_HANDOVER";
    private static final String STATUS_PENDING_EFFECTIVE = "PENDING_EFFECTIVE";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_REJECTED = "REJECTED";

    private static final String TASK_PENDING = "PENDING";
    private static final String TASK_DONE = "DONE";

    private static final String SOURCE_EMPLOYEE_ENTRY = "EMPLOYEE_ENTRY";
    private static final Integer ADMIN_USER_STATUS_DISABLED = 1;
    private static final int MIN_EMPLOYEE_AGE_FOR_BIRTHDAY = 16;
    private static final List<String> OPEN_EVENT_STATUSES =
            Arrays.asList(STATUS_PENDING_APPROVAL, STATUS_PENDING_HANDOVER, STATUS_PENDING_EFFECTIVE);
    private static final List<String> APPROVAL_EVENT_TYPES = Arrays.asList(
            EVENT_REGULARIZATION_REQUESTED,
            EVENT_TRANSFER_REQUESTED,
            EVENT_SALARY_ADJUST_REQUESTED,
            EVENT_RESIGN_REQUESTED);

    @Resource
    private HrLifecycleEventMapper hrLifecycleEventMapper;

    @Resource
    private HrLifecycleTaskMapper hrLifecycleTaskMapper;

    @Resource
    private EmployeeEntryMapper employeeEntryMapper;

    @Resource
    private EmployeeProfileMapper employeeProfileMapper;

    @Resource
    private EmployeeGrowthLogMapper employeeGrowthLogMapper;

    @Resource
    private EmployeeOperationLogMapper employeeOperationLogMapper;

    @Resource
    private EmployeeSalaryMapper employeeSalaryMapper;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    @Override
    @Cacheable(cacheNames = "hr:lifecycle:workbench#15s", key = "'current'", sync = true)
    public HrLifecycleWorkbenchRespVO getWorkbench() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate nextMonthStart = monthStart.plusMonths(1);
        LocalDate probationDeadline = today.plusDays(30);

        List<EmployeeEntryDO> entries = employeeEntryMapper.selectList();
        List<EmployeeProfileDO> profiles = employeeProfileMapper.selectList();
        List<HrLifecycleEventDO> lifecycleEvents = hrLifecycleEventMapper.selectList();
        List<HrLifecycleTaskDO> lifecycleTasks = hrLifecycleTaskMapper.selectList();

        HrLifecycleWorkbenchRespVO respVO = new HrLifecycleWorkbenchRespVO();
        respVO.setPendingOnboardingCount((int) entries.stream().filter(this::isPendingOnboarding).count());
        respVO.setProbationDueCount((int) entries.stream()
                .filter(entry -> isProbationDue(entry, today, probationDeadline)).count());
        respVO.setResignPendingCount(countLong(hrLifecycleEventMapper.selectCount(
                new LambdaQueryWrapperX<HrLifecycleEventDO>()
                        .eq(HrLifecycleEventDO::getEventType, EVENT_RESIGN_REQUESTED)
                        .in(HrLifecycleEventDO::getEventStatus, OPEN_EVENT_STATUSES))));
        respVO.setRegularizationPendingCount(countOpenEvent(EVENT_REGULARIZATION_REQUESTED));
        respVO.setTransferPendingCount(countOpenEvent(EVENT_TRANSFER_REQUESTED));
        respVO.setSalaryAdjustPendingCount(countOpenEvent(EVENT_SALARY_ADJUST_REQUESTED));
        respVO.setDueEffectiveEventCount(countLong(hrLifecycleEventMapper.selectCount(
                new LambdaQueryWrapperX<HrLifecycleEventDO>()
                        .eq(HrLifecycleEventDO::getEventStatus, STATUS_PENDING_EFFECTIVE)
                        .le(HrLifecycleEventDO::getEffectiveDate, today))));
        respVO.setActiveEmployeeCount((int) profiles.stream().filter(this::isActiveProfile).count());
        respVO.setResignedEmployeeCount((int) entries.stream()
                .filter(entry -> Objects.equals(entry.getWorkStatus(), 4)).count());
        respVO.setMonthOnboardCount((int) entries.stream()
                .filter(entry -> inMonth(entry.getEntryDate(), monthStart, nextMonthStart)).count());
        respVO.setMonthResignCount((int) entries.stream()
                .filter(entry -> inMonth(entry.getLeaveDate(), monthStart, nextMonthStart)).count());
        respVO.setPendingEventCount(countLong(hrLifecycleEventMapper.selectCount(
                new LambdaQueryWrapperX<HrLifecycleEventDO>()
                        .in(HrLifecycleEventDO::getEventStatus, OPEN_EVENT_STATUSES))));
        respVO.setPendingTaskCount(countLong(hrLifecycleTaskMapper.selectPendingCount()));
        respVO.setOverdueTaskCount(countLong(hrLifecycleTaskMapper.selectOverdueCount(today)));
        respVO.setCompletedTaskCount((int) lifecycleTasks.stream()
                .filter(task -> TASK_DONE.equals(task.getTaskStatus())).count());
        respVO.setTaskCompletionRate(percent(respVO.getCompletedTaskCount(), lifecycleTasks.size()));
        respVO.setEventTypeStats(buildStats(lifecycleEvents.stream()
                .map(HrLifecycleEventDO::getEventType).collect(Collectors.toList())));
        respVO.setEventStatusStats(buildStats(lifecycleEvents.stream()
                .map(HrLifecycleEventDO::getEventStatus).collect(Collectors.toList())));
        respVO.setTaskStatusStats(buildStats(lifecycleTasks.stream()
                .map(HrLifecycleTaskDO::getTaskStatus).collect(Collectors.toList())));
        respVO.setRecentEvents(hrLifecycleEventMapper.selectRecentList(8).stream()
                .map(this::buildEventRespVO)
                .collect(Collectors.toList()));
        respVO.setOverdueTasks(BeanUtils.toBean(hrLifecycleTaskMapper.selectOverdueList(today, 8),
                HrLifecycleTaskRespVO.class));
        return respVO;
    }

    @Override
    public PageResult<HrLifecycleEventRespVO> getEventPage(HrLifecycleEventPageReqVO pageReqVO) {
        PageResult<HrLifecycleEventDO> pageResult = hrLifecycleEventMapper.selectPage(pageReqVO);
        List<HrLifecycleEventRespVO> list = pageResult.getList().stream()
                .map(this::buildEventRespVO)
                .collect(Collectors.toList());
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public HrLifecycleEventRespVO getEvent(Long id) {
        return buildEventRespVO(validateEventExists(id));
    }

    @Override
    public List<HrLifecycleEventRespVO> getTimeline(Long profileId) {
        validateProfileExists(profileId);
        return hrLifecycleEventMapper.selectListByProfileId(profileId).stream()
                .map(this::buildEventRespVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<HrLifecycleTaskRespVO> getTaskList(Long eventId) {
        validateEventExists(eventId);
        return BeanUtils.toBean(hrLifecycleTaskMapper.selectListByEventId(eventId), HrLifecycleTaskRespVO.class);
    }

    @Override
    public List<HrLifecycleReminderRespVO> getReminderList() {
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(30);
        List<HrLifecycleReminderRespVO> reminders = new ArrayList<>();
        List<EmployeeEntryDO> entries = employeeEntryMapper.selectList();
        List<EmployeeProfileDO> profiles = employeeProfileMapper.selectList();
        Map<Long, EmployeeProfileDO> profileMap = toProfileMap(profiles);
        Map<Long, EmployeeEntryDO> entryByProfileId = toEntryByProfileId(entries);

        for (EmployeeEntryDO entry : entries) {
            if (entry.getProfileId() == null || Objects.equals(entry.getWorkStatus(), 4)) {
                continue;
            }
            EmployeeProfileDO profile = profileMap.get(entry.getProfileId());
            if (profile == null) {
                continue;
            }
            if (isProbationDue(entry, today, deadline)) {
                LocalDate dueDate = entry.getEntryDate().plusMonths(entry.getProbationMonths());
                reminders.add(buildReminder("PROBATION_DUE", "待转正", "LOW", profile, entry,
                        null, null, dueDate, "员工 30 天内需要转正确认。", "发起转正办理"));
            }
            if (entry.getContractEndDate() != null
                    && !entry.getContractEndDate().isBefore(today)
                    && !entry.getContractEndDate().isAfter(deadline)) {
                reminders.add(buildReminder("CONTRACT_EXPIRING", "合同即将到期", "MEDIUM", profile, entry,
                        null, null, entry.getContractEndDate(), "员工合同将在 30 天内到期。", "续签或终止合同"));
            }
        }

        for (EmployeeProfileDO profile : profiles) {
            EmployeeEntryDO entry = entryByProfileId.get(profile.getId());
            if (!shouldIncludeProfileInReminder(profile, entry)) {
                continue;
            }
            List<String> missingFields = collectMissingProfileFields(profile, entry);
            if (!missingFields.isEmpty()) {
                String missingText = missingFields.stream().limit(4).collect(Collectors.joining("、"));
                reminders.add(buildReminder("PROFILE_MISSING", "档案资料待补齐", "MEDIUM", profile, entry,
                        null, null, today.plusDays(7),
                        "缺少" + missingText + (missingFields.size() > 4 ? "等资料。" : "。"),
                        "补齐员工档案"));
            }
            addBirthdayReminder(reminders, "EMPLOYEE_BIRTHDAY", "员工生日", profile, entry,
                    employeeBirthMonthDay(profile.getBirthDate()), today, deadline, "员工生日在 30 天内。", "准备生日关怀");
            addBirthdayReminder(reminders, "FATHER_BIRTHDAY", "父亲生日", profile, entry,
                    parseMonthDay(profile.getFatherBirthday()), today, deadline, "员工父亲生日在 30 天内。", "准备家庭关怀");
            addBirthdayReminder(reminders, "MOTHER_BIRTHDAY", "母亲生日", profile, entry,
                    parseMonthDay(profile.getMotherBirthday()), today, deadline, "员工母亲生日在 30 天内。", "准备家庭关怀");
        }

        for (HrLifecycleTaskDO task : hrLifecycleTaskMapper.selectOverdueList(today, 50)) {
            EmployeeProfileDO profile = task.getProfileId() == null ? null : profileMap.get(task.getProfileId());
            reminders.add(buildReminder("TASK_OVERDUE", "生命周期任务逾期", "HIGH", profile, null,
                    task.getEventId(), task.getId(), task.getDueDate(), task.getTaskName(), "完成检查项"));
        }

        for (HrLifecycleEventDO event : hrLifecycleEventMapper.selectDueEffectiveList(today, 50)) {
            EmployeeProfileDO profile = event.getProfileId() == null ? null : profileMap.get(event.getProfileId());
            reminders.add(buildReminder("EVENT_DUE_EFFECTIVE", "生命周期事件待生效", "HIGH", profile, null,
                    event.getId(), null, event.getEffectiveDate(), eventTypeName(event.getEventType()), "执行生效"));
        }

        reminders.sort(Comparator
                .comparingInt((HrLifecycleReminderRespVO reminder) -> severityRank(reminder.getSeverity()))
                .thenComparing(HrLifecycleReminderRespVO::getDueDate, Comparator.nullsLast(LocalDate::compareTo)));
        return reminders.stream().limit(100).collect(Collectors.toList());
    }

    @Override
    public List<HrLifecycleCalendarEventRespVO> getCalendarEvents(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate == null ? LocalDate.now().withDayOfMonth(1) : startDate;
        LocalDate end = endDate == null ? start.plusMonths(1).minusDays(1) : endDate;
        List<HrLifecycleCalendarEventRespVO> events = new ArrayList<>();
        List<EmployeeEntryDO> entries = employeeEntryMapper.selectList();
        List<EmployeeProfileDO> profiles = employeeProfileMapper.selectList();
        Map<Long, EmployeeProfileDO> profileMap = toProfileMap(profiles);
        Map<Long, EmployeeEntryDO> entryByProfileId = toEntryByProfileId(entries);

        for (EmployeeEntryDO entry : entries) {
            EmployeeProfileDO profile = entry.getProfileId() == null ? null : profileMap.get(entry.getProfileId());
            if (isBetween(entry.getEntryDate(), start, end)) {
                events.add(buildCalendarEvent("ONBOARDING", "入职", entry.getEntryDate(), profile,
                        SOURCE_EMPLOYEE_ENTRY, entry.getId(), statusText(entry.getWorkStatus()), "green"));
            }
            if (entry.getProbationMonths() != null && entry.getEntryDate() != null && Objects.equals(entry.getWorkStatus(), 2)) {
                LocalDate dueDate = entry.getEntryDate().plusMonths(entry.getProbationMonths());
                if (isBetween(dueDate, start, end)) {
                    events.add(buildCalendarEvent("REGULARIZATION_DUE", "转正", dueDate, profile,
                            SOURCE_EMPLOYEE_ENTRY, entry.getId(), "待转正", "blue"));
                }
            }
            if (isBetween(entry.getContractEndDate(), start, end)) {
                events.add(buildCalendarEvent("CONTRACT_END", "合同到期", entry.getContractEndDate(), profile,
                        SOURCE_EMPLOYEE_ENTRY, entry.getId(), "待处理", "orange"));
            }
            if (isBetween(entry.getLeaveDate(), start, end)) {
                events.add(buildCalendarEvent("RESIGNATION", "离职", entry.getLeaveDate(), profile,
                        SOURCE_EMPLOYEE_ENTRY, entry.getId(), statusText(entry.getWorkStatus()), "red"));
            }
        }

        for (EmployeeProfileDO profile : profiles) {
            EmployeeEntryDO entry = entryByProfileId.get(profile.getId());
            if (!shouldIncludeProfileInReminder(profile, entry)) {
                continue;
            }
            addAnniversaryCalendarEvents(events, "EMPLOYEE_BIRTHDAY", "生日", profile,
                    employeeBirthMonthDay(profile.getBirthDate()), start, end, "待关怀", "cyan");
            addAnniversaryCalendarEvents(events, "FATHER_BIRTHDAY", "父亲生日", profile,
                    parseMonthDay(profile.getFatherBirthday()), start, end, "待关怀", "cyan");
            addAnniversaryCalendarEvents(events, "MOTHER_BIRTHDAY", "母亲生日", profile,
                    parseMonthDay(profile.getMotherBirthday()), start, end, "待关怀", "cyan");
        }

        for (HrLifecycleEventDO event : hrLifecycleEventMapper.selectListByEffectiveDateRange(start, end)) {
            EmployeeProfileDO profile = event.getProfileId() == null ? null : profileMap.get(event.getProfileId());
            events.add(buildCalendarEvent("LIFECYCLE_EVENT", eventTypeName(event.getEventType()), event.getEffectiveDate(),
                    profile, "HR_LIFECYCLE_EVENT", event.getId(), event.getEventStatus(), "purple"));
        }

        events.sort(Comparator
                .comparing(HrLifecycleCalendarEventRespVO::getEventDate, Comparator.nullsLast(LocalDate::compareTo))
                .thenComparing(HrLifecycleCalendarEventRespVO::getEventType));
        return events;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createResignation(HrLifecycleResignationCreateReqVO reqVO) {
        EmployeeEntryDO entry = validateEntryExists(reqVO.getEntryId());
        validateActiveEmploymentEntry(entry);
        validateNoActiveLifecycleApproval(entry.getId(), EVENT_RESIGN_REQUESTED);
        validateDateNotBeforeToday(reqVO.getLeaveDate(), "离职日期不能早于今天");
        requireText(reqVO.getLeaveReason(), "离职原因不能为空");
        if (reqVO.getHandoverUserId() == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "离职交接人不能为空");
        }
        EmployeeProfileDO profile = validateProfileExists(entry.getProfileId());

        Map<String, Object> payload = new HashMap<>();
        payload.put("leaveDate", reqVO.getLeaveDate());
        payload.put("leaveReason", reqVO.getLeaveReason());
        payload.put("handoverUserId", reqVO.getHandoverUserId());
        payload.put("handoverUserName", reqVO.getHandoverUserName());
        payload.put("remark", reqVO.getRemark());

        HrLifecycleEventDO event = new HrLifecycleEventDO();
        event.setProfileId(entry.getProfileId());
        event.setEntryId(entry.getId());
        event.setUserId(resolveUserId(profile, entry));
        event.setEmployeeName(profile.getName());
        event.setEventType(EVENT_RESIGN_REQUESTED);
        event.setEventStatus(STATUS_PENDING_APPROVAL);
        event.setSourceType(SOURCE_EMPLOYEE_ENTRY);
        event.setSourceId(entry.getId());
        event.setApplyUserId(SecurityFrameworkUtils.getLoginUserId());
        event.setApplyUserName(SecurityFrameworkUtils.getLoginUserNickname());
        event.setApplyTime(LocalDateTime.now());
        event.setEffectiveDate(reqVO.getLeaveDate());
        event.setBeforeJson(JsonUtils.toJsonString(entry));
        event.setAfterJson(JsonUtils.toJsonString(payload));
        event.setReason(reqVO.getLeaveReason());
        event.setRemark(reqVO.getRemark());
        hrLifecycleEventMapper.insert(event);

        createResignationTasks(event, reqVO, false);
        startLifecycleApprovalProcess(event, payload);
        recordOperationLog(entry.getProfileId(), "resignation_request", "发起离职办理",
                "发起离职办理，计划离职日期：" + reqVO.getLeaveDate());
        return event.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRegularization(HrLifecycleRegularizationCreateReqVO reqVO) {
        EmployeeEntryDO entry = validateOpenEntry(reqVO.getEntryId());
        validateProbationEntry(entry);
        validateNoActiveLifecycleApproval(entry.getId(), EVENT_REGULARIZATION_REQUESTED);
        validateDateNotBeforeEntry(reqVO.getConfirmationDate(), entry, "转正日期不能早于入职日期");
        requireText(reqVO.getManagerEvaluation(), "负责人评价不能为空");
        requireText(reqVO.getHrReview(), "HR 复核意见不能为空");
        EmployeeProfileDO profile = validateProfileExists(entry.getProfileId());

        Map<String, Object> payload = new HashMap<>();
        payload.put("confirmationDate", reqVO.getConfirmationDate());
        payload.put("managerEvaluation", reqVO.getManagerEvaluation());
        payload.put("hrReview", reqVO.getHrReview());
        payload.put("remark", reqVO.getRemark());

        HrLifecycleEventDO event = createPendingEvent(entry, profile, EVENT_REGULARIZATION_REQUESTED,
                reqVO.getConfirmationDate(), "发起转正", reqVO.getRemark(), payload);
        insertTask(event, "MANAGER_EVALUATION", "负责人转正评价", entry.getDirectSupervisorId(), null,
                reqVO.getConfirmationDate(), true, 10, false);
        insertTask(event, "HR_REVIEW", "HR 转正复核", null, null,
                reqVO.getConfirmationDate(), true, 20, false);
        insertTask(event, "POSITION_SALARY_CONFIRM", "岗位职级薪酬确认", null, null,
                reqVO.getConfirmationDate(), false, 30, false);
        startLifecycleApprovalProcess(event, payload);
        recordOperationLog(entry.getProfileId(), "regularization_request", "发起转正办理",
                "发起转正办理，计划转正日期：" + reqVO.getConfirmationDate());
        return event.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTransfer(HrLifecycleTransferCreateReqVO reqVO) {
        EmployeeEntryDO entry = validateOpenEntry(reqVO.getEntryId());
        validateActiveEmploymentEntry(entry);
        validateNoActiveLifecycleApproval(entry.getId(), EVENT_TRANSFER_REQUESTED);
        validateDateNotBeforeToday(reqVO.getEffectiveDate(), "调岗生效日期不能早于今天");
        requireText(reqVO.getReason(), "调岗原因不能为空");
        validateTransferHasRealChange(entry, reqVO);
        EmployeeProfileDO profile = validateProfileExists(entry.getProfileId());

        Map<String, Object> payload = new HashMap<>();
        payload.put("targetDeptId", reqVO.getTargetDeptId());
        payload.put("targetJobTitle", reqVO.getTargetJobTitle());
        payload.put("targetJobLevelId", reqVO.getTargetJobLevelId());
        payload.put("targetJobSequenceId", reqVO.getTargetJobSequenceId());
        payload.put("targetWorkLocationId", reqVO.getTargetWorkLocationId());
        payload.put("targetDirectSupervisorId", reqVO.getTargetDirectSupervisorId());
        payload.put("reason", reqVO.getReason());
        payload.put("remark", reqVO.getRemark());

        HrLifecycleEventDO event = createPendingEvent(entry, profile, EVENT_TRANSFER_REQUESTED,
                reqVO.getEffectiveDate(), reqVO.getReason(), reqVO.getRemark(), payload);
        insertTask(event, "TRANSFER_OUT_CONFIRM", "调出部门确认", entry.getDirectSupervisorId(), null,
                reqVO.getEffectiveDate(), true, 10, false);
        insertTask(event, "TRANSFER_IN_CONFIRM", "调入部门确认", reqVO.getTargetDirectSupervisorId(), null,
                reqVO.getEffectiveDate(), true, 20, false);
        insertTask(event, "HR_TRANSFER_REVIEW", "HR 调岗复核", null, null,
                reqVO.getEffectiveDate(), true, 30, false);
        insertTask(event, "ORG_PERMISSION_NOTIFY", "组织权限调整通知", null, null,
                reqVO.getEffectiveDate(), false, 40, false);
        startLifecycleApprovalProcess(event, payload);
        recordOperationLog(entry.getProfileId(), "transfer_request", "发起调岗办理",
                "发起调岗办理，计划生效日期：" + reqVO.getEffectiveDate());
        return event.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSalaryAdjust(HrLifecycleSalaryAdjustCreateReqVO reqVO) {
        EmployeeEntryDO entry = validateOpenEntry(reqVO.getEntryId());
        validateActiveEmploymentEntry(entry);
        validateNoActiveLifecycleApproval(entry.getId(), EVENT_SALARY_ADJUST_REQUESTED);
        validateDateNotBeforeToday(reqVO.getEffectiveDate(), "调薪生效日期不能早于今天");
        validateSalaryAdjustRequest(entry, reqVO);
        EmployeeProfileDO profile = validateProfileExists(entry.getProfileId());
        EmployeeSalaryDO currentSalary = resolveCurrentSalary(entry.getProfileId(), reqVO.getSalaryType(),
                reqVO.getCurrency(), reqVO.getEffectiveDate());

        Map<String, Object> payload = new HashMap<>();
        payload.put("salaryType", StringUtils.hasText(reqVO.getSalaryType()) ? reqVO.getSalaryType() : "月薪");
        payload.put("beforeSalaryAmount", currentSalary == null ? null : currentSalary.getAmount());
        payload.put("beforeSalaryType", currentSalary == null ? null : currentSalary.getSalaryType());
        payload.put("beforeSalaryCurrency", currentSalary == null ? null : currentSalary.getCurrency());
        payload.put("amount", reqVO.getAmount());
        payload.put("currency", StringUtils.hasText(reqVO.getCurrency()) ? reqVO.getCurrency() : "CNY");
        payload.put("reason", reqVO.getReason());
        payload.put("remark", reqVO.getRemark());

        HrLifecycleEventDO event = createPendingEvent(entry, profile, EVENT_SALARY_ADJUST_REQUESTED,
                reqVO.getEffectiveDate(), reqVO.getReason(), reqVO.getRemark(), payload);
        insertTask(event, "MANAGER_SALARY_CONFIRM", "负责人调薪确认", entry.getDirectSupervisorId(), null,
                reqVO.getEffectiveDate(), true, 10, false);
        insertTask(event, "HR_SALARY_REVIEW", "HR 薪酬复核", null, null,
                reqVO.getEffectiveDate(), true, 20, false);
        insertTask(event, "PAYROLL_ARCHIVE", "薪酬档案归档", null, null,
                reqVO.getEffectiveDate(), true, 30, false);
        startLifecycleApprovalProcess(event, payload);
        recordOperationLog(entry.getProfileId(), "salary_adjust_request", "发起调薪办理",
                "发起调薪办理，计划生效日期：" + reqVO.getEffectiveDate());
        return event.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(HrLifecycleTaskCompleteReqVO reqVO) {
        HrLifecycleTaskDO task = validateTaskExists(reqVO.getId());
        HrLifecycleEventDO event = validateEventExists(task.getEventId());
        if (STATUS_PENDING_APPROVAL.equals(event.getEventStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "生命周期审批通过后才能处理检查清单");
        }
        if (STATUS_COMPLETED.equals(event.getEventStatus())
                || STATUS_REJECTED.equals(event.getEventStatus())
                || STATUS_CANCELLED.equals(event.getEventStatus())) {
            throw ServiceExceptionUtil.exception(HR_LIFECYCLE_EVENT_TYPE_UNSUPPORTED);
        }
        if (!TASK_DONE.equals(task.getTaskStatus())) {
            task.setTaskStatus(TASK_DONE);
            task.setCompletedTime(LocalDateTime.now());
            task.setCompletedBy(SecurityFrameworkUtils.getLoginUserId());
        }
        task.setRemark(reqVO.getRemark());
        hrLifecycleTaskMapper.updateById(task);

        if (STATUS_PENDING_HANDOVER.equals(event.getEventStatus())
                && hrLifecycleTaskMapper.selectOpenRequiredCountByEventId(event.getId()) <= 0) {
            HrLifecycleEventDO update = new HrLifecycleEventDO();
            update.setId(event.getId());
            update.setEventStatus(STATUS_PENDING_EFFECTIVE);
            hrLifecycleEventMapper.updateById(update);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void effectiveEvent(Long id) {
        HrLifecycleEventDO event = validateEventExists(id);
        if (STATUS_COMPLETED.equals(event.getEventStatus())) {
            return;
        }
        if (STATUS_REJECTED.equals(event.getEventStatus()) || STATUS_CANCELLED.equals(event.getEventStatus())) {
            throw ServiceExceptionUtil.exception(HR_LIFECYCLE_EVENT_TYPE_UNSUPPORTED);
        }
        if (isApprovalLifecycleEvent(event) && !STATUS_PENDING_EFFECTIVE.equals(event.getEventStatus())) {
            throw ServiceExceptionUtil.exception(HR_LIFECYCLE_APPROVAL_NOT_PASSED);
        }
        if (hrLifecycleTaskMapper.selectOpenRequiredCountByEventId(id) > 0) {
            throw ServiceExceptionUtil.exception(HR_LIFECYCLE_REQUIRED_TASK_OPEN);
        }
        if (EVENT_RESIGN_REQUESTED.equals(event.getEventType())) {
            completeResignationEvent(event);
            return;
        }
        if (EVENT_REGULARIZATION_REQUESTED.equals(event.getEventType())) {
            completeRegularizationEvent(event);
            return;
        }
        if (EVENT_TRANSFER_REQUESTED.equals(event.getEventType())) {
            completeTransferEvent(event);
            return;
        }
        if (EVENT_SALARY_ADJUST_REQUESTED.equals(event.getEventType())) {
            completeSalaryAdjustEvent(event);
            return;
        }
        throw ServiceExceptionUtil.exception(HR_LIFECYCLE_EVENT_TYPE_UNSUPPORTED);
    }

    @Override
    public Integer effectiveDueEvents() {
        int successCount = 0;
        List<HrLifecycleEventDO> events = hrLifecycleEventMapper.selectDueEffectiveList(LocalDate.now(), 200);
        for (HrLifecycleEventDO event : events) {
            try {
                effectiveEvent(event.getId());
                successCount++;
            } catch (Exception ex) {
                log.warn("生命周期到期事件生效失败，eventId={}, eventType={}", event.getId(), event.getEventType(), ex);
            }
        }
        return successCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelEvent(Long id, String reason) {
        HrLifecycleEventDO event = validateEventExists(id);
        if (STATUS_COMPLETED.equals(event.getEventStatus())) {
            throw ServiceExceptionUtil.exception(HR_LIFECYCLE_EVENT_TYPE_UNSUPPORTED);
        }
        if (STATUS_PENDING_APPROVAL.equals(event.getEventStatus())
                && StringUtils.hasText(event.getProcessInstanceId())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "审批中的生命周期事件请在 BPM 流程中撤销");
        }
        HrLifecycleEventDO update = new HrLifecycleEventDO();
        update.setId(event.getId());
        update.setEventStatus(STATUS_CANCELLED);
        update.setCompletedTime(LocalDateTime.now());
        update.setRemark(StringUtils.hasText(reason) ? reason : event.getRemark());
        hrLifecycleEventMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateApprovalStatusByBpmEvent(Long eventId, String processInstanceId, Integer bpmStatus, Long operatorUserId) {
        if (eventId == null) {
            return;
        }
        HrLifecycleEventDO event = hrLifecycleEventMapper.selectById(eventId);
        if (event == null) {
            log.warn("生命周期 BPM 回调事件不存在，eventId={}, processInstanceId={}, bpmStatus={}",
                    eventId, processInstanceId, bpmStatus);
            return;
        }
        if (StringUtils.hasText(event.getProcessInstanceId())
                && StringUtils.hasText(processInstanceId)
                && !Objects.equals(event.getProcessInstanceId(), processInstanceId)) {
            log.warn("生命周期 BPM 回调流程实例不匹配，eventId={}, processInstanceId={}, currentProcessInstanceId={}",
                    eventId, processInstanceId, event.getProcessInstanceId());
            return;
        }

        if (Objects.equals(bpmStatus, BpmProcessInstanceStatusEnum.APPROVE.getStatus())) {
            if (EVENT_RESIGN_REQUESTED.equals(event.getEventType())) {
                HrLifecycleEventDO update = new HrLifecycleEventDO();
                update.setId(eventId);
                update.setEventStatus(STATUS_PENDING_HANDOVER);
                update.setProcessInstanceId(StringUtils.hasText(processInstanceId) ? processInstanceId : event.getProcessInstanceId());
                hrLifecycleEventMapper.updateById(update);
                recordOperationLog(event.getProfileId(), "lifecycle_bpm_approve", "离职审批通过",
                        "离职审批通过，进入交接清单");
                return;
            }

            markEventTasksDone(eventId, "BPM 审批通过，系统自动归档生命周期必办项");
            HrLifecycleEventDO update = new HrLifecycleEventDO();
            update.setId(eventId);
            update.setEventStatus(STATUS_PENDING_EFFECTIVE);
            update.setProcessInstanceId(StringUtils.hasText(processInstanceId) ? processInstanceId : event.getProcessInstanceId());
            hrLifecycleEventMapper.updateById(update);
            recordOperationLog(event.getProfileId(), "lifecycle_bpm_approve", "生命周期审批通过",
                    eventTypeName(event.getEventType()) + "审批通过，等待生效");
            if (event.getEffectiveDate() == null || !event.getEffectiveDate().isAfter(LocalDate.now())) {
                effectiveEvent(eventId);
            }
            return;
        }

        if (Objects.equals(bpmStatus, BpmProcessInstanceStatusEnum.REJECT.getStatus())) {
            updateEventClosedByBpm(event, STATUS_REJECTED, processInstanceId);
            recordOperationLog(event.getProfileId(), "lifecycle_bpm_reject", "生命周期审批拒绝",
                    eventTypeName(event.getEventType()) + "审批拒绝，未执行业务生效");
            return;
        }

        if (Objects.equals(bpmStatus, BpmProcessInstanceStatusEnum.CANCEL.getStatus())) {
            updateEventClosedByBpm(event, STATUS_CANCELLED, processInstanceId);
            recordOperationLog(event.getProfileId(), "lifecycle_bpm_cancel", "生命周期审批撤销",
                    eventTypeName(event.getEventType()) + "审批撤销，未执行业务生效");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer backfillBaselineEvents() {
        int count = 0;
        List<EmployeeEntryDO> entries = employeeEntryMapper.selectList();
        for (EmployeeEntryDO entry : entries) {
            if (entry.getProfileId() == null) {
                continue;
            }
            EmployeeProfileDO profile = employeeProfileMapper.selectById(entry.getProfileId());
            if (profile == null) {
                continue;
            }
            recordOnboardingConfirmed(entry, profile);
            count++;
            if (Objects.equals(entry.getWorkStatus(), 4)) {
                recordResignationEffective(entry, entry, profile, entry.getLeaveReason());
            }
        }
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordOnboardingConfirmed(EmployeeEntryDO employeeEntry, EmployeeProfileDO profile) {
        if (employeeEntry == null || employeeEntry.getId() == null || employeeEntry.getProfileId() == null) {
            return;
        }
        EmployeeProfileDO resolvedProfile = profile != null ? profile : employeeProfileMapper.selectById(employeeEntry.getProfileId());
        if (resolvedProfile == null) {
            return;
        }
        String onboardingEventType = isRehireEntry(employeeEntry)
                ? EVENT_REHIRE_CREATED
                : (isConfirmedEntry(employeeEntry) ? EVENT_ONBOARDING_CONFIRMED : EVENT_ONBOARDING_CREATED);
        if (hrLifecycleEventMapper.selectFirstBySource(onboardingEventType, SOURCE_EMPLOYEE_ENTRY, employeeEntry.getId()) == null) {
            String eventReason = EVENT_REHIRE_CREATED.equals(onboardingEventType)
                    ? "返聘入职"
                    : (EVENT_ONBOARDING_CONFIRMED.equals(onboardingEventType) ? "确认入职" : "创建入职记录");
            HrLifecycleEventDO event = buildCompletedEvent(employeeEntry, resolvedProfile, onboardingEventType,
                    employeeEntry.getEntryDate(), eventReason,
                    JsonUtils.toJsonString(employeeEntry));
            hrLifecycleEventMapper.insert(event);
        }

        if (isConfirmedEntry(employeeEntry)
                && Objects.equals(employeeEntry.getWorkStatus(), 2)
                && hrLifecycleEventMapper.selectFirstBySource(EVENT_PROBATION_STARTED,
                SOURCE_EMPLOYEE_ENTRY, employeeEntry.getId()) == null) {
            HrLifecycleEventDO probationEvent = buildCompletedEvent(employeeEntry, resolvedProfile, EVENT_PROBATION_STARTED,
                    employeeEntry.getEntryDate(), "进入试用期", JsonUtils.toJsonString(employeeEntry));
            hrLifecycleEventMapper.insert(probationEvent);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordResignationEffective(EmployeeEntryDO beforeEntry, EmployeeEntryDO afterEntry,
                                           EmployeeProfileDO profile, String leaveReason) {
        if (afterEntry == null || afterEntry.getId() == null || afterEntry.getProfileId() == null) {
            return;
        }
        EmployeeProfileDO resolvedProfile = profile != null ? profile : employeeProfileMapper.selectById(afterEntry.getProfileId());
        if (resolvedProfile == null) {
            return;
        }

        HrLifecycleEventDO pendingEvent = hrLifecycleEventMapper.selectActiveResignationByEntryId(afterEntry.getId());
        if (pendingEvent != null) {
            markEventTasksDone(pendingEvent.getId(), "原离职接口已直接生效");
            updateResignationEventCompleted(pendingEvent, beforeEntry, afterEntry, leaveReason);
            return;
        }

        if (hrLifecycleEventMapper.selectFirstBySource(EVENT_RESIGN_EFFECTIVE, SOURCE_EMPLOYEE_ENTRY, afterEntry.getId()) != null) {
            return;
        }
        HrLifecycleEventDO event = buildCompletedEvent(afterEntry, resolvedProfile, EVENT_RESIGN_EFFECTIVE,
                afterEntry.getLeaveDate(), leaveReason, JsonUtils.toJsonString(afterEntry));
        event.setBeforeJson(beforeEntry == null ? null : JsonUtils.toJsonString(beforeEntry));
        event.setAfterJson(JsonUtils.toJsonString(afterEntry));
        hrLifecycleEventMapper.insert(event);
        createResignationTasks(event, null, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordTransferEffective(EmployeeEntryDO beforeEntry, EmployeeEntryDO afterEntry,
                                        EmployeeProfileDO profile, String reason) {
        if (beforeEntry == null || afterEntry == null || afterEntry.getId() == null
                || afterEntry.getProfileId() == null || !hasTransferSnapshotChanged(beforeEntry, afterEntry)) {
            return;
        }
        if (hrLifecycleEventMapper.selectActiveByEntryIdAndType(afterEntry.getId(), EVENT_TRANSFER_REQUESTED) != null) {
            return;
        }
        EmployeeProfileDO resolvedProfile = profile != null ? profile : employeeProfileMapper.selectById(afterEntry.getProfileId());
        if (resolvedProfile == null) {
            return;
        }
        String resolvedReason = StringUtils.hasText(reason) ? reason : "HR 直接维护组织岗位信息";
        HrLifecycleEventDO event = buildCompletedEvent(afterEntry, resolvedProfile, "TRANSFER_EFFECTIVE",
                LocalDate.now(), resolvedReason, JsonUtils.toJsonString(afterEntry));
        event.setBeforeJson(JsonUtils.toJsonString(beforeEntry));
        event.setAfterJson(JsonUtils.toJsonString(afterEntry));
        hrLifecycleEventMapper.insert(event);
        createTransferGrowthLog(beforeEntry, afterEntry, LocalDate.now(), resolvedReason);
        syncAdminUserDept(afterEntry, afterEntry.getDeptId());
        recordOperationLog(afterEntry.getProfileId(), "transfer_direct_effective", "组织岗位变更已生效",
                "组织岗位变更已生效，来源：员工详情直接维护");
    }

    private void completeRegularizationEvent(HrLifecycleEventDO event) {
        EmployeeEntryDO beforeEntry = validateEntryExists(event.getEntryId());
        Map<String, Object> payload = parsePayload(event);
        LocalDate confirmationDate = getLocalDate(payload, "confirmationDate", event.getEffectiveDate());

        EmployeeProfileDO profileUpdate = new EmployeeProfileDO();
        profileUpdate.setId(beforeEntry.getProfileId());
        profileUpdate.setConfirmationDate(confirmationDate);
        profileUpdate.setStatus(1);
        employeeProfileMapper.updateById(profileUpdate);

        EmployeeEntryDO entryUpdate = new EmployeeEntryDO();
        entryUpdate.setId(beforeEntry.getId());
        entryUpdate.setWorkStatus(3);
        employeeEntryMapper.updateById(entryUpdate);

        EmployeeEntryDO afterEntry = employeeEntryMapper.selectById(beforeEntry.getId());
        completeEvent(event, beforeEntry, afterEntry, payload);
        createRegularizationGrowthLog(afterEntry, confirmationDate, getString(payload, "managerEvaluation"));
        recordOperationLog(afterEntry.getProfileId(), "regularization_effective", "转正生效",
                "转正生效，转正日期：" + confirmationDate);
    }

    private void completeTransferEvent(HrLifecycleEventDO event) {
        EmployeeEntryDO beforeEntry = validateEntryExists(event.getEntryId());
        Map<String, Object> payload = parsePayload(event);

        EmployeeEntryDO entryUpdate = new EmployeeEntryDO();
        entryUpdate.setId(beforeEntry.getId());
        entryUpdate.setDeptId(getLong(payload, "targetDeptId", beforeEntry.getDeptId()));
        entryUpdate.setJobTitle(getString(payload, "targetJobTitle", beforeEntry.getJobTitle()));
        entryUpdate.setJobLevelId(getLong(payload, "targetJobLevelId", beforeEntry.getJobLevelId()));
        entryUpdate.setJobSequenceId(getLong(payload, "targetJobSequenceId", beforeEntry.getJobSequenceId()));
        entryUpdate.setWorkLocationId(getLong(payload, "targetWorkLocationId", beforeEntry.getWorkLocationId()));
        entryUpdate.setDirectSupervisorId(getLong(payload, "targetDirectSupervisorId", beforeEntry.getDirectSupervisorId()));
        employeeEntryMapper.updateById(entryUpdate);

        EmployeeEntryDO afterEntry = employeeEntryMapper.selectById(beforeEntry.getId());
        completeEvent(event, beforeEntry, afterEntry, payload);
        createTransferGrowthLog(beforeEntry, afterEntry, event.getEffectiveDate(), getString(payload, "reason"));
        syncAdminUserDept(afterEntry, getLong(payload, "targetDeptId", afterEntry.getDeptId()));
        recordOperationLog(afterEntry.getProfileId(), "transfer_effective", "调岗生效",
                "调岗生效，生效日期：" + event.getEffectiveDate());
    }

    private boolean hasTransferSnapshotChanged(EmployeeEntryDO beforeEntry, EmployeeEntryDO afterEntry) {
        if (beforeEntry == null || afterEntry == null) {
            return false;
        }
        return !Objects.equals(beforeEntry.getDeptId(), afterEntry.getDeptId())
                || !Objects.equals(beforeEntry.getJobTitle(), afterEntry.getJobTitle())
                || !Objects.equals(beforeEntry.getJobLevelId(), afterEntry.getJobLevelId())
                || !Objects.equals(beforeEntry.getJobSequenceId(), afterEntry.getJobSequenceId())
                || !Objects.equals(beforeEntry.getWorkLocationId(), afterEntry.getWorkLocationId())
                || !Objects.equals(beforeEntry.getDirectSupervisorId(), afterEntry.getDirectSupervisorId());
    }

    private void completeSalaryAdjustEvent(HrLifecycleEventDO event) {
        EmployeeEntryDO entry = validateEntryExists(event.getEntryId());
        Map<String, Object> payload = parsePayload(event);

        EmployeeSalaryDO salary = new EmployeeSalaryDO();
        salary.setProfileId(entry.getProfileId());
        salary.setSalaryType(getString(payload, "salaryType", "月薪"));
        salary.setAmount(getBigDecimal(payload, "amount"));
        salary.setCurrency(getString(payload, "currency", "CNY"));
        salary.setEffectiveDate(event.getEffectiveDate());
        salary.setRemark(buildSalaryRemark(payload));
        employeeSalaryMapper.insert(salary);

        completeEvent(event, entry, entry, payload);
        createSalaryAdjustGrowthLog(entry, salary, getString(payload, "reason"));
        recordOperationLog(entry.getProfileId(), "salary_adjust_effective", "调薪生效",
                "调薪生效，生效日期：" + event.getEffectiveDate());
    }

    private void completeResignationEvent(HrLifecycleEventDO event) {
        EmployeeEntryDO beforeEntry = validateEntryExists(event.getEntryId());
        if (!Objects.equals(beforeEntry.getWorkStatus(), 4)) {
            EmployeeEntryDO updateEntry = new EmployeeEntryDO();
            updateEntry.setId(beforeEntry.getId());
            updateEntry.setLeaveDate(event.getEffectiveDate());
            updateEntry.setLeaveReason(event.getReason());
            updateEntry.setWorkStatus(4);
            employeeEntryMapper.updateById(updateEntry);
        }
        if (beforeEntry.getProfileId() != null) {
            EmployeeProfileDO profileUpdate = new EmployeeProfileDO();
            profileUpdate.setId(beforeEntry.getProfileId());
            profileUpdate.setStatus(0);
            employeeProfileMapper.updateById(profileUpdate);
        }
        EmployeeEntryDO afterEntry = employeeEntryMapper.selectById(beforeEntry.getId());
        disableAdminUser(afterEntry);
        updateResignationEventCompleted(event, beforeEntry, afterEntry, event.getReason());
        createResignationGrowthLog(afterEntry);
        recordOperationLog(afterEntry.getProfileId(), "resignation_effective", "离职生效",
                "离职生效，离职日期：" + afterEntry.getLeaveDate());
    }

    private void completeEvent(HrLifecycleEventDO event, EmployeeEntryDO beforeEntry, EmployeeEntryDO afterEntry,
                               Map<String, Object> payload) {
        HrLifecycleEventDO update = new HrLifecycleEventDO();
        update.setId(event.getId());
        update.setEventStatus(STATUS_COMPLETED);
        update.setCompletedTime(LocalDateTime.now());
        update.setBeforeJson(beforeEntry == null ? event.getBeforeJson() : JsonUtils.toJsonString(beforeEntry));
        update.setAfterJson(JsonUtils.toJsonString(buildAfterSnapshot(afterEntry, payload)));
        hrLifecycleEventMapper.updateById(update);
    }

    private void updateResignationEventCompleted(HrLifecycleEventDO event, EmployeeEntryDO beforeEntry,
                                                EmployeeEntryDO afterEntry, String leaveReason) {
        HrLifecycleEventDO update = new HrLifecycleEventDO();
        update.setId(event.getId());
        update.setEventStatus(STATUS_COMPLETED);
        update.setCompletedTime(LocalDateTime.now());
        update.setEffectiveDate(afterEntry.getLeaveDate());
        update.setBeforeJson(beforeEntry == null ? event.getBeforeJson() : JsonUtils.toJsonString(beforeEntry));
        update.setAfterJson(JsonUtils.toJsonString(afterEntry));
        update.setReason(StringUtils.hasText(leaveReason) ? leaveReason : event.getReason());
        hrLifecycleEventMapper.updateById(update);
        createResignationGrowthLog(afterEntry);
    }

    private HrLifecycleEventDO buildCompletedEvent(EmployeeEntryDO entry, EmployeeProfileDO profile, String eventType,
                                                  LocalDate effectiveDate, String reason, String afterJson) {
        HrLifecycleEventDO event = new HrLifecycleEventDO();
        event.setProfileId(entry.getProfileId());
        event.setEntryId(entry.getId());
        event.setUserId(resolveUserId(profile, entry));
        event.setEmployeeName(profile.getName());
        event.setEventType(eventType);
        event.setEventStatus(STATUS_COMPLETED);
        event.setSourceType(SOURCE_EMPLOYEE_ENTRY);
        event.setSourceId(entry.getId());
        event.setApplyUserId(SecurityFrameworkUtils.getLoginUserId());
        event.setApplyUserName(defaultOperatorName());
        event.setApplyTime(LocalDateTime.now());
        event.setEffectiveDate(effectiveDate);
        event.setCompletedTime(LocalDateTime.now());
        event.setAfterJson(afterJson);
        event.setReason(reason);
        return event;
    }

    private HrLifecycleEventDO createPendingEvent(EmployeeEntryDO entry, EmployeeProfileDO profile, String eventType,
                                                  LocalDate effectiveDate, String reason, String remark,
                                                  Map<String, Object> payload) {
        HrLifecycleEventDO event = new HrLifecycleEventDO();
        event.setProfileId(entry.getProfileId());
        event.setEntryId(entry.getId());
        event.setUserId(resolveUserId(profile, entry));
        event.setEmployeeName(profile.getName());
        event.setEventType(eventType);
        event.setEventStatus(STATUS_PENDING_APPROVAL);
        event.setSourceType(SOURCE_EMPLOYEE_ENTRY);
        event.setSourceId(entry.getId());
        event.setApplyUserId(SecurityFrameworkUtils.getLoginUserId());
        event.setApplyUserName(defaultOperatorName());
        event.setApplyTime(LocalDateTime.now());
        event.setEffectiveDate(effectiveDate);
        event.setBeforeJson(JsonUtils.toJsonString(entry));
        event.setAfterJson(JsonUtils.toJsonString(payload));
        event.setReason(reason);
        event.setRemark(remark);
        hrLifecycleEventMapper.insert(event);
        return event;
    }

    private void validateNoActiveLifecycleApproval(Long entryId, String currentEventType) {
        Long count = hrLifecycleEventMapper.selectCount(new LambdaQueryWrapperX<HrLifecycleEventDO>()
                .eq(HrLifecycleEventDO::getEntryId, entryId)
                .in(HrLifecycleEventDO::getEventType, APPROVAL_EVENT_TYPES)
                .in(HrLifecycleEventDO::getEventStatus, OPEN_EVENT_STATUSES));
        if (count != null && count > 0) {
            if (EVENT_RESIGN_REQUESTED.equals(currentEventType)) {
                throw ServiceExceptionUtil.exception(HR_LIFECYCLE_RESIGNATION_PENDING_EXISTS);
            }
            throw ServiceExceptionUtil.exception(HR_LIFECYCLE_EVENT_PENDING_EXISTS);
        }
    }

    private void validateProbationEntry(EmployeeEntryDO entry) {
        if (!Objects.equals(entry.getWorkStatus(), 2)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "只有试用期员工可以发起转正办理");
        }
    }

    private void validateActiveEmploymentEntry(EmployeeEntryDO entry) {
        if (Objects.equals(entry.getWorkStatus(), 4)) {
            throw ServiceExceptionUtil.exception(EMPLOYEE_ALREADY_RESIGNED);
        }
        if (!Objects.equals(entry.getWorkStatus(), 2) && !Objects.equals(entry.getWorkStatus(), 3)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "只有已入职员工可以发起该生命周期办理");
        }
    }

    private void validateDateNotBeforeToday(LocalDate date, String message) {
        if (date != null && date.isBefore(LocalDate.now())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, message);
        }
    }

    private void validateDateNotBeforeEntry(LocalDate date, EmployeeEntryDO entry, String message) {
        if (date != null && entry != null && entry.getEntryDate() != null && date.isBefore(entry.getEntryDate())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, message);
        }
    }

    private void validateTransferHasRealChange(EmployeeEntryDO entry, HrLifecycleTransferCreateReqVO reqVO) {
        boolean changed = isChanged(reqVO.getTargetDeptId(), entry.getDeptId())
                || isChanged(reqVO.getTargetJobTitle(), entry.getJobTitle())
                || isChanged(reqVO.getTargetJobLevelId(), entry.getJobLevelId())
                || isChanged(reqVO.getTargetJobSequenceId(), entry.getJobSequenceId())
                || isChanged(reqVO.getTargetWorkLocationId(), entry.getWorkLocationId())
                || isChanged(reqVO.getTargetDirectSupervisorId(), entry.getDirectSupervisorId());
        if (!changed) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "请至少填写一项和当前任职信息不同的调岗内容");
        }
    }

    private boolean isChanged(Long targetValue, Long currentValue) {
        return targetValue != null && !Objects.equals(targetValue, currentValue);
    }

    private boolean isChanged(String targetValue, String currentValue) {
        return StringUtils.hasText(targetValue) && !Objects.equals(targetValue.trim(), trimText(currentValue));
    }

    private void validateSalaryAdjustRequest(EmployeeEntryDO entry, HrLifecycleSalaryAdjustCreateReqVO reqVO) {
        requireText(reqVO.getReason(), "调薪原因不能为空");
        if (reqVO.getAmount() == null || reqVO.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "调整后金额必须大于 0");
        }
        EmployeeSalaryDO currentSalary = resolveCurrentSalary(entry.getProfileId(), reqVO.getSalaryType(),
                reqVO.getCurrency(), reqVO.getEffectiveDate());
        if (currentSalary == null) {
            return;
        }
        String salaryType = StringUtils.hasText(reqVO.getSalaryType()) ? reqVO.getSalaryType().trim() : "月薪";
        String currency = StringUtils.hasText(reqVO.getCurrency()) ? reqVO.getCurrency().trim() : "CNY";
        if (currentSalary.getAmount() != null
                && reqVO.getAmount().compareTo(currentSalary.getAmount()) == 0
                && Objects.equals(salaryType, trimText(currentSalary.getSalaryType()))
                && Objects.equals(currency, trimText(currentSalary.getCurrency()))) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "调整后薪资和当前薪资一致，无需发起调薪");
        }
    }

    private EmployeeSalaryDO resolveCurrentSalary(Long profileId, String salaryType, String currency, LocalDate effectiveDate) {
        if (profileId == null) {
            return null;
        }
        LocalDate benchmark = effectiveDate == null ? LocalDate.now() : effectiveDate;
        String resolvedSalaryType = StringUtils.hasText(salaryType) ? salaryType.trim() : "月薪";
        String resolvedCurrency = StringUtils.hasText(currency) ? currency.trim() : "CNY";
        return employeeSalaryMapper.selectListByProfileId(profileId).stream()
                .filter(item -> item.getEffectiveDate() != null && !item.getEffectiveDate().isAfter(benchmark))
                .filter(item -> Objects.equals(trimText(item.getSalaryType()), resolvedSalaryType))
                .filter(item -> Objects.equals(trimText(item.getCurrency()), resolvedCurrency))
                .max(Comparator.comparing(EmployeeSalaryDO::getEffectiveDate)
                        .thenComparing(EmployeeSalaryDO::getId, Comparator.nullsLast(Long::compareTo)))
                .orElse(null);
    }

    private boolean isApprovalLifecycleEvent(HrLifecycleEventDO event) {
        return event != null && APPROVAL_EVENT_TYPES.contains(event.getEventType());
    }

    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, message);
        }
    }

    private String trimText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void startLifecycleApprovalProcess(HrLifecycleEventDO event, Map<String, Object> payload) {
        Long userId = event.getApplyUserId() != null ? event.getApplyUserId() : SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            throw new IllegalStateException("生命周期审批发起人不能为空");
        }
        Map<String, Object> variables = new HashMap<>();
        if (payload != null) {
            variables.putAll(payload);
        }
        variables.put("eventId", event.getId());
        variables.put("entryId", event.getEntryId());
        variables.put("profileId", event.getProfileId());
        variables.put("employeeUserId", event.getUserId());
        variables.put("employeeName", event.getEmployeeName());
        variables.put("eventType", event.getEventType());
        variables.put("effectiveDate", event.getEffectiveDate());
        variables.put("reason", event.getReason());

        String processInstanceId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO()
                        .setProcessDefinitionKey(resolveLifecycleProcessKey(event.getEventType()))
                        .setBusinessKey(String.valueOf(event.getId()))
                        .setVariables(variables))
                .getCheckedData();

        HrLifecycleEventDO update = new HrLifecycleEventDO();
        update.setId(event.getId());
        update.setProcessInstanceId(processInstanceId);
        hrLifecycleEventMapper.updateById(update);
        event.setProcessInstanceId(processInstanceId);
    }

    private String resolveLifecycleProcessKey(String eventType) {
        if (EVENT_REGULARIZATION_REQUESTED.equals(eventType)) {
            return PROCESS_KEY_REGULARIZATION;
        }
        if (EVENT_TRANSFER_REQUESTED.equals(eventType)) {
            return PROCESS_KEY_TRANSFER;
        }
        if (EVENT_SALARY_ADJUST_REQUESTED.equals(eventType)) {
            return PROCESS_KEY_SALARY_ADJUST;
        }
        if (EVENT_RESIGN_REQUESTED.equals(eventType)) {
            return PROCESS_KEY_RESIGNATION;
        }
        throw ServiceExceptionUtil.exception(HR_LIFECYCLE_EVENT_TYPE_UNSUPPORTED);
    }

    private void updateEventClosedByBpm(HrLifecycleEventDO event, String eventStatus, String processInstanceId) {
        HrLifecycleEventDO update = new HrLifecycleEventDO();
        update.setId(event.getId());
        update.setEventStatus(eventStatus);
        update.setCompletedTime(LocalDateTime.now());
        update.setProcessInstanceId(StringUtils.hasText(processInstanceId) ? processInstanceId : event.getProcessInstanceId());
        hrLifecycleEventMapper.updateById(update);
    }

    private void createResignationTasks(HrLifecycleEventDO event, HrLifecycleResignationCreateReqVO reqVO,
                                        boolean completed) {
        if (event == null || event.getId() == null || !hrLifecycleTaskMapper.selectListByEventId(event.getId()).isEmpty()) {
            return;
        }
        Long handoverUserId = reqVO == null ? null : reqVO.getHandoverUserId();
        String handoverUserName = reqVO == null ? null : reqVO.getHandoverUserName();
        LocalDate dueDate = event.getEffectiveDate();
        insertTask(event, "WORK_HANDOVER", "工作交接", handoverUserId, handoverUserName, dueDate, true, 10, completed);
        insertTask(event, "ASSET_RETURN", "资产归还", null, null, dueDate, true, 20, completed);
        insertTask(event, "ACCOUNT_DISABLE", "账号停用", null, null, dueDate, true, 30, completed);
        insertTask(event, "DINGTALK_ARCHIVE", "钉钉处理", null, null, dueDate, true, 40, completed);
        insertTask(event, "SALARY_SETTLEMENT", "工资结算", null, null, dueDate, true, 50, completed);
        insertTask(event, "SOCIAL_SECURITY", "社保公积金处理", null, null, dueDate, true, 60, completed);
        insertTask(event, "EXIT_INTERVIEW", "离职面谈", null, null, dueDate, false, 70, completed);
        insertTask(event, "RESIGN_CERTIFICATE", "离职证明", null, null, dueDate, false, 80, completed);
        insertTask(event, "HR_ARCHIVE", "人事档案归档", null, null, dueDate, true, 90, completed);
    }

    private void insertTask(HrLifecycleEventDO event, String taskType, String taskName, Long assigneeUserId,
                            String assigneeName, LocalDate dueDate, boolean required, Integer sortOrder,
                            boolean completed) {
        HrLifecycleTaskDO task = new HrLifecycleTaskDO();
        task.setEventId(event.getId());
        task.setProfileId(event.getProfileId());
        task.setEventType(event.getEventType());
        task.setTaskType(taskType);
        task.setTaskName(taskName);
        task.setAssigneeUserId(assigneeUserId);
        task.setAssigneeName(assigneeName);
        task.setDueDate(dueDate);
        task.setTaskStatus(completed ? TASK_DONE : TASK_PENDING);
        task.setRequiredFlag(required);
        task.setSortOrder(sortOrder);
        if (completed) {
            task.setCompletedTime(LocalDateTime.now());
            task.setCompletedBy(SecurityFrameworkUtils.getLoginUserId());
            task.setRemark("历史或直接生效流程自动归档");
        }
        hrLifecycleTaskMapper.insert(task);
    }

    private void markEventTasksDone(Long eventId, String remark) {
        for (HrLifecycleTaskDO task : hrLifecycleTaskMapper.selectListByEventId(eventId)) {
            if (TASK_DONE.equals(task.getTaskStatus())) {
                continue;
            }
            task.setTaskStatus(TASK_DONE);
            task.setCompletedTime(LocalDateTime.now());
            task.setCompletedBy(SecurityFrameworkUtils.getLoginUserId());
            task.setRemark(remark);
            hrLifecycleTaskMapper.updateById(task);
        }
    }

    private HrLifecycleEventRespVO buildEventRespVO(HrLifecycleEventDO event) {
        HrLifecycleEventRespVO respVO = BeanUtils.toBean(event, HrLifecycleEventRespVO.class);
        List<HrLifecycleTaskDO> tasks = hrLifecycleTaskMapper.selectListByEventId(event.getId());
        respVO.setTotalTaskCount(tasks.size());
        respVO.setCompletedTaskCount((int) tasks.stream()
                .filter(task -> TASK_DONE.equals(task.getTaskStatus())).count());
        respVO.setOpenRequiredTaskCount((int) tasks.stream()
                .filter(task -> Boolean.TRUE.equals(task.getRequiredFlag()))
                .filter(task -> !TASK_DONE.equals(task.getTaskStatus()))
                .count());
        return respVO;
    }

    private void createResignationGrowthLog(EmployeeEntryDO entry) {
        if (entry == null || entry.getProfileId() == null || entry.getLeaveDate() == null) {
            return;
        }
        boolean exists = employeeGrowthLogMapper.selectCount(new LambdaQueryWrapperX<EmployeeGrowthLogDO>()
                .eq(EmployeeGrowthLogDO::getProfileId, entry.getProfileId())
                .eq(EmployeeGrowthLogDO::getEventType, 6)
                .eq(EmployeeGrowthLogDO::getEventDate, entry.getLeaveDate())) > 0;
        if (exists) {
            return;
        }
        EmployeeGrowthLogDO growthLog = new EmployeeGrowthLogDO();
        growthLog.setProfileId(entry.getProfileId());
        growthLog.setEventType(6);
        growthLog.setEventDate(entry.getLeaveDate());
        growthLog.setTitle("离职");
        growthLog.setContent(StringUtils.hasText(entry.getLeaveReason())
                ? "员工离职：" + entry.getLeaveReason() : "员工离职");
        growthLog.setBeforeDeptId(entry.getDeptId());
        growthLog.setBeforeJobTitle(entry.getJobTitle());
        growthLog.setBeforeJobLevelId(entry.getJobLevelId());
        employeeGrowthLogMapper.insert(growthLog);
    }

    private void createRegularizationGrowthLog(EmployeeEntryDO entry, LocalDate confirmationDate, String evaluation) {
        if (entry == null || entry.getProfileId() == null || confirmationDate == null) {
            return;
        }
        boolean exists = employeeGrowthLogMapper.selectCount(new LambdaQueryWrapperX<EmployeeGrowthLogDO>()
                .eq(EmployeeGrowthLogDO::getProfileId, entry.getProfileId())
                .eq(EmployeeGrowthLogDO::getEventType, 2)
                .eq(EmployeeGrowthLogDO::getEventDate, confirmationDate)) > 0;
        if (exists) {
            return;
        }
        EmployeeGrowthLogDO growthLog = new EmployeeGrowthLogDO();
        growthLog.setProfileId(entry.getProfileId());
        growthLog.setEventType(2);
        growthLog.setEventDate(confirmationDate);
        growthLog.setTitle("转正");
        growthLog.setContent(StringUtils.hasText(evaluation) ? "员工转正：" + evaluation : "员工转正");
        growthLog.setAfterDeptId(entry.getDeptId());
        growthLog.setAfterJobTitle(entry.getJobTitle());
        growthLog.setAfterJobLevelId(entry.getJobLevelId());
        employeeGrowthLogMapper.insert(growthLog);
    }

    private void createTransferGrowthLog(EmployeeEntryDO beforeEntry, EmployeeEntryDO afterEntry,
                                         LocalDate eventDate, String reason) {
        if (afterEntry == null || afterEntry.getProfileId() == null || afterEntry.getId() == null) {
            return;
        }
        LocalDate resolvedEventDate = eventDate == null ? LocalDate.now() : eventDate;
        boolean exists = employeeGrowthLogMapper.selectCount(new LambdaQueryWrapperX<EmployeeGrowthLogDO>()
                .eq(EmployeeGrowthLogDO::getProfileId, afterEntry.getProfileId())
                .eq(EmployeeGrowthLogDO::getEventType, 5)
                .eq(EmployeeGrowthLogDO::getEventDate, resolvedEventDate)
                .eq(EmployeeGrowthLogDO::getAfterDeptId, afterEntry.getDeptId())
                .eq(EmployeeGrowthLogDO::getAfterJobTitle, afterEntry.getJobTitle())) > 0;
        if (exists) {
            return;
        }
        EmployeeGrowthLogDO growthLog = new EmployeeGrowthLogDO();
        growthLog.setProfileId(afterEntry.getProfileId());
        growthLog.setEventType(5);
        growthLog.setEventDate(resolvedEventDate);
        growthLog.setTitle("调岗");
        growthLog.setContent(StringUtils.hasText(reason) ? "员工调岗：" + reason : "员工调岗");
        if (beforeEntry != null) {
            growthLog.setBeforeDeptId(beforeEntry.getDeptId());
            growthLog.setBeforeJobTitle(beforeEntry.getJobTitle());
            growthLog.setBeforeJobLevelId(beforeEntry.getJobLevelId());
        }
        growthLog.setAfterDeptId(afterEntry.getDeptId());
        growthLog.setAfterJobTitle(afterEntry.getJobTitle());
        growthLog.setAfterJobLevelId(afterEntry.getJobLevelId());
        employeeGrowthLogMapper.insert(growthLog);
    }

    private void createSalaryAdjustGrowthLog(EmployeeEntryDO entry, EmployeeSalaryDO salary, String reason) {
        if (entry == null || salary == null || entry.getProfileId() == null || salary.getEffectiveDate() == null) {
            return;
        }
        EmployeeGrowthLogDO growthLog = new EmployeeGrowthLogDO();
        growthLog.setProfileId(entry.getProfileId());
        growthLog.setEventType(8);
        growthLog.setEventDate(salary.getEffectiveDate());
        growthLog.setTitle("调薪");
        growthLog.setContent(StringUtils.hasText(reason) ? "员工调薪：" + reason : "员工调薪");
        growthLog.setAfterDeptId(entry.getDeptId());
        growthLog.setAfterJobTitle(entry.getJobTitle());
        growthLog.setAfterJobLevelId(entry.getJobLevelId());
        employeeGrowthLogMapper.insert(growthLog);
    }

    private void syncAdminUserDept(EmployeeEntryDO entry, Long targetDeptId) {
        Long userId = resolveEntryUserId(entry);
        if (userId == null || targetDeptId == null) {
            return;
        }
        try {
            adminUserApi.updateUserDept(userId, targetDeptId).getCheckedData();
        } catch (Exception ex) {
            log.warn("生命周期调岗联动用户部门失败，entryId={}, userId={}, targetDeptId={}",
                    entry == null ? null : entry.getId(), userId, targetDeptId, ex);
        }
    }

    private void disableAdminUser(EmployeeEntryDO entry) {
        Long userId = resolveEntryUserId(entry);
        if (userId == null) {
            return;
        }
        try {
            adminUserApi.updateUserStatus(userId, ADMIN_USER_STATUS_DISABLED).getCheckedData();
        } catch (Exception ex) {
            log.warn("生命周期离职联动账号停用失败，entryId={}, userId={}",
                    entry == null ? null : entry.getId(), userId, ex);
        }
    }

    private Long resolveEntryUserId(EmployeeEntryDO entry) {
        if (entry == null) {
            return null;
        }
        if (entry.getUserId() != null) {
            return entry.getUserId();
        }
        if (entry.getProfileId() == null) {
            return null;
        }
        EmployeeProfileDO profile = employeeProfileMapper.selectById(entry.getProfileId());
        return profile == null ? null : profile.getUserId();
    }

    private Map<String, Object> parsePayload(HrLifecycleEventDO event) {
        Map<String, Object> payload = JsonUtils.parseObjectQuietly(event.getAfterJson(),
                new TypeReference<Map<String, Object>>() {});
        return payload == null ? new HashMap<>() : payload;
    }

    private Map<String, Object> buildAfterSnapshot(EmployeeEntryDO entry, Map<String, Object> payload) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("entry", entry);
        snapshot.put("payload", payload);
        return snapshot;
    }

    private String buildSalaryRemark(Map<String, Object> payload) {
        String reason = getString(payload, "reason");
        String remark = getString(payload, "remark");
        if (StringUtils.hasText(reason) && StringUtils.hasText(remark)) {
            return reason + "；" + remark;
        }
        return StringUtils.hasText(reason) ? reason : remark;
    }

    private Map<Long, EmployeeProfileDO> toProfileMap(List<EmployeeProfileDO> profiles) {
        return profiles.stream()
                .filter(profile -> profile.getId() != null)
                .collect(Collectors.toMap(EmployeeProfileDO::getId, profile -> profile, (a, b) -> a));
    }

    private Map<Long, EmployeeEntryDO> toEntryByProfileId(List<EmployeeEntryDO> entries) {
        return entries.stream()
                .filter(entry -> entry.getProfileId() != null)
                .sorted(Comparator.comparing(EmployeeEntryDO::getId,
                        Comparator.nullsLast(Long::compareTo)).reversed())
                .collect(Collectors.toMap(EmployeeEntryDO::getProfileId, entry -> entry, (a, b) -> a));
    }

    private boolean shouldIncludeProfileInReminder(EmployeeProfileDO profile, EmployeeEntryDO entry) {
        return profile != null && isActiveProfile(profile)
                && (entry == null || !Objects.equals(entry.getWorkStatus(), 4));
    }

    private List<String> collectMissingProfileFields(EmployeeProfileDO profile, EmployeeEntryDO entry) {
        List<String> missingFields = new ArrayList<>();
        addMissing(missingFields, "手机号", profile.getMobile());
        addMissing(missingFields, "邮箱", profile.getEmail());
        addMissing(missingFields, "身份证号", profile.getIdNumber());
        addMissing(missingFields, "现住址", profile.getAddress());
        addMissing(missingFields, "紧急联系人", profile.getEmergencyContact());
        addMissing(missingFields, "紧急联系电话", profile.getEmergencyPhone());
        addMissing(missingFields, "入职日期",
                profile.getOnboardDate() == null && entry != null ? entry.getEntryDate() : profile.getOnboardDate());
        if (entry == null) {
            missingFields.add("任职记录");
            return missingFields;
        }
        addMissing(missingFields, "员工编号", entry.getEmployeeNo());
        addMissing(missingFields, "部门", entry.getDeptId());
        addMissing(missingFields, "职位", entry.getJobTitle());
        addMissing(missingFields, "合同到期", entry.getContractEndDate());
        return missingFields;
    }

    private void addMissing(List<String> missingFields, String label, Object value) {
        if (!hasValue(value)) {
            missingFields.add(label);
        }
    }

    private boolean hasValue(Object value) {
        if (value == null) {
            return false;
        }
        return !(value instanceof String) || StringUtils.hasText((String) value);
    }

    private void addBirthdayReminder(List<HrLifecycleReminderRespVO> reminders, String reminderType,
                                     String title, EmployeeProfileDO profile, EmployeeEntryDO entry,
                                     MonthDay monthDay, LocalDate today, LocalDate deadline,
                                     String description, String action) {
        if (monthDay == null) {
            return;
        }
        LocalDate dueDate = nextOccurrence(monthDay, today);
        if (isBetween(dueDate, today, deadline)) {
            reminders.add(buildReminder(reminderType, title, "LOW", profile, entry,
                    null, null, dueDate, description, action));
        }
    }

    private void addAnniversaryCalendarEvents(List<HrLifecycleCalendarEventRespVO> events,
                                              String eventType, String title, EmployeeProfileDO profile,
                                              MonthDay monthDay, LocalDate start, LocalDate end,
                                              String status, String color) {
        if (monthDay == null || end.isBefore(start)) {
            return;
        }
        for (int year = start.getYear(); year <= end.getYear(); year++) {
            LocalDate occurrence = monthDay.atYear(year);
            if (isBetween(occurrence, start, end)) {
                events.add(buildCalendarEvent(eventType, title, occurrence, profile,
                        "HR_EMPLOYEE_PROFILE", profile.getId(), status, color));
            }
        }
    }

    private LocalDate nextOccurrence(MonthDay monthDay, LocalDate today) {
        LocalDate occurrence = monthDay.atYear(today.getYear());
        if (occurrence.isBefore(today)) {
            return monthDay.atYear(today.getYear() + 1);
        }
        return occurrence;
    }

    private MonthDay monthDay(LocalDate date) {
        return date == null ? null : MonthDay.from(date);
    }

    private MonthDay employeeBirthMonthDay(LocalDate birthDate) {
        return isReliableEmployeeBirthDate(birthDate) ? monthDay(birthDate) : null;
    }

    private boolean isReliableEmployeeBirthDate(LocalDate birthDate) {
        if (birthDate == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return !birthDate.isBefore(LocalDate.of(1900, 1, 1))
                && !birthDate.isAfter(today.minusYears(MIN_EMPLOYEE_AGE_FOR_BIRTHDAY));
    }

    private MonthDay parseMonthDay(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim()
                .replace('/', '-')
                .replace('.', '-')
                .replace("年", "-")
                .replace("月", "-")
                .replace("日", "");
        try {
            return MonthDay.from(LocalDate.parse(normalized));
        } catch (Exception ignored) {
            // Support simple month-day values like 05-20 or 5-20.
        }
        String[] parts = normalized.split("-");
        if (parts.length < 2) {
            return null;
        }
        try {
            int monthIndex = parts.length == 2 ? 0 : parts.length - 2;
            int dayIndex = parts.length == 2 ? 1 : parts.length - 1;
            return MonthDay.of(Integer.parseInt(parts[monthIndex].trim()),
                    Integer.parseInt(parts[dayIndex].trim()));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private Long getLong(Map<String, Object> payload, String key, Long defaultValue) {
        Object value = payload.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private String getString(Map<String, Object> payload, String key) {
        return getString(payload, key, null);
    }

    private String getString(Map<String, Object> payload, String key, String defaultValue) {
        Object value = payload.get(key);
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return StringUtils.hasText(text) ? text : defaultValue;
    }

    private LocalDate getLocalDate(Map<String, Object> payload, String key, LocalDate defaultValue) {
        Object value = payload.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }
        return LocalDate.parse(String.valueOf(value));
    }

    private BigDecimal getBigDecimal(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return new BigDecimal(String.valueOf(value));
        }
        return new BigDecimal(String.valueOf(value));
    }

    private void recordOperationLog(Long profileId, String operationType, String title, String content) {
        if (profileId == null) {
            return;
        }
        EmployeeOperationLogDO log = new EmployeeOperationLogDO();
        log.setProfileId(profileId);
        log.setOperationType(operationType);
        log.setOperationModule("lifecycle");
        log.setOperationTitle(title);
        log.setOperationContent(content);
        log.setOperatorId(SecurityFrameworkUtils.getLoginUserId());
        log.setOperatorName(defaultOperatorName());
        log.setOperationTime(LocalDateTime.now());
        log.setOperationSource("web");
        employeeOperationLogMapper.insert(log);
    }

    private HrLifecycleReminderRespVO buildReminder(String reminderType, String title, String severity,
                                                    EmployeeProfileDO profile, EmployeeEntryDO entry,
                                                    Long eventId, Long taskId, LocalDate dueDate,
                                                    String description, String action) {
        HrLifecycleReminderRespVO reminder = new HrLifecycleReminderRespVO();
        reminder.setReminderType(reminderType);
        reminder.setTitle(title);
        reminder.setSeverity(severity);
        reminder.setProfileId(profile == null ? null : profile.getId());
        reminder.setEmployeeName(profile == null ? null : profile.getName());
        reminder.setEntryId(entry == null ? null : entry.getId());
        reminder.setEventId(eventId);
        reminder.setTaskId(taskId);
        reminder.setDueDate(dueDate);
        reminder.setDescription(description);
        reminder.setAction(action);
        return reminder;
    }

    private HrLifecycleCalendarEventRespVO buildCalendarEvent(String eventType, String title, LocalDate eventDate,
                                                              EmployeeProfileDO profile, String sourceType,
                                                              Long sourceId, String status, String color) {
        HrLifecycleCalendarEventRespVO event = new HrLifecycleCalendarEventRespVO();
        event.setEventType(eventType);
        event.setTitle((profile == null || !StringUtils.hasText(profile.getName())) ? title : profile.getName() + " " + title);
        event.setEventDate(eventDate);
        event.setProfileId(profile == null ? null : profile.getId());
        event.setEmployeeName(profile == null ? null : profile.getName());
        event.setSourceType(sourceType);
        event.setSourceId(sourceId);
        event.setStatus(status);
        event.setColor(color);
        return event;
    }

    private int severityRank(String severity) {
        if ("HIGH".equals(severity)) {
            return 0;
        }
        if ("MEDIUM".equals(severity)) {
            return 1;
        }
        return 2;
    }

    private boolean isPendingOnboarding(EmployeeEntryDO entry) {
        return Objects.equals(entry.getWorkStatus(), 0) || Objects.equals(entry.getWorkStatus(), 1);
    }

    private boolean isConfirmedEntry(EmployeeEntryDO entry) {
        return Objects.equals(entry.getWorkStatus(), 2)
                || Objects.equals(entry.getWorkStatus(), 3)
                || Objects.equals(entry.getWorkStatus(), 4);
    }

    private boolean isRehireEntry(EmployeeEntryDO entry) {
        return entry != null && Objects.equals(entry.getEntryType(), 2);
    }

    private boolean isActiveProfile(EmployeeProfileDO profile) {
        return profile.getStatus() == null || Objects.equals(profile.getStatus(), 1);
    }

    private boolean isProbationDue(EmployeeEntryDO entry, LocalDate today, LocalDate deadline) {
        if (!Objects.equals(entry.getWorkStatus(), 2) || entry.getEntryDate() == null || entry.getProbationMonths() == null) {
            return false;
        }
        LocalDate estimatedDate = entry.getEntryDate().plusMonths(entry.getProbationMonths());
        return !estimatedDate.isBefore(today) && !estimatedDate.isAfter(deadline);
    }

    private boolean inMonth(LocalDate date, LocalDate monthStart, LocalDate nextMonthStart) {
        return date != null && !date.isBefore(monthStart) && date.isBefore(nextMonthStart);
    }

    private boolean isBetween(LocalDate date, LocalDate startDate, LocalDate endDate) {
        return date != null && !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    private String eventTypeName(String eventType) {
        if (EVENT_ONBOARDING_CREATED.equals(eventType)) {
            return "创建入职";
        }
        if (EVENT_ONBOARDING_CONFIRMED.equals(eventType)) {
            return "确认入职";
        }
        if (EVENT_REHIRE_CREATED.equals(eventType)) {
            return "返聘入职";
        }
        if (EVENT_PROBATION_STARTED.equals(eventType)) {
            return "进入试用期";
        }
        if (EVENT_REGULARIZATION_REQUESTED.equals(eventType)) {
            return "转正办理";
        }
        if (EVENT_TRANSFER_REQUESTED.equals(eventType)) {
            return "调岗办理";
        }
        if (EVENT_SALARY_ADJUST_REQUESTED.equals(eventType)) {
            return "调薪办理";
        }
        if (EVENT_RESIGN_REQUESTED.equals(eventType)) {
            return "离职办理";
        }
        if (EVENT_RESIGN_EFFECTIVE.equals(eventType)) {
            return "离职生效";
        }
        return eventType;
    }

    private String statusText(Integer workStatus) {
        if (Objects.equals(workStatus, 0)) {
            return "待填写";
        }
        if (Objects.equals(workStatus, 1)) {
            return "待入职";
        }
        if (Objects.equals(workStatus, 2)) {
            return "试用期";
        }
        if (Objects.equals(workStatus, 3)) {
            return "在职";
        }
        if (Objects.equals(workStatus, 4)) {
            return "离职";
        }
        return "未知";
    }

    private int countOpenEvent(String eventType) {
        return countLong(hrLifecycleEventMapper.selectCount(new LambdaQueryWrapperX<HrLifecycleEventDO>()
                .eq(HrLifecycleEventDO::getEventType, eventType)
                .in(HrLifecycleEventDO::getEventStatus, OPEN_EVENT_STATUSES)));
    }

    private int countLong(Long value) {
        return value == null ? 0 : value.intValue();
    }

    private List<HrLifecycleWorkbenchRespVO.StatItem> buildStats(List<String> values) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, Long> countMap = values.stream()
                .filter(StringUtils::hasText)
                .collect(Collectors.groupingBy(value -> value, LinkedHashMap::new, Collectors.counting()));
        int total = countMap.values().stream().mapToInt(Long::intValue).sum();
        return countMap.entrySet().stream()
                .map(entry -> new HrLifecycleWorkbenchRespVO.StatItem(
                        entry.getKey(), entry.getValue().intValue(), percent(entry.getValue().intValue(), total)))
                .collect(Collectors.toList());
    }

    private BigDecimal percent(int numerator, int denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private Long resolveUserId(EmployeeProfileDO profile, EmployeeEntryDO entry) {
        if (profile != null && profile.getUserId() != null) {
            return profile.getUserId();
        }
        return entry == null ? null : entry.getUserId();
    }

    private String defaultOperatorName() {
        String nickname = SecurityFrameworkUtils.getLoginUserNickname();
        return StringUtils.hasText(nickname) ? nickname : "system";
    }

    private EmployeeEntryDO validateEntryExists(Long id) {
        EmployeeEntryDO entry = employeeEntryMapper.selectById(id);
        if (entry == null) {
            throw ServiceExceptionUtil.exception(EMPLOYEE_ENTRY_NOT_EXISTS);
        }
        return entry;
    }

    private EmployeeEntryDO validateOpenEntry(Long id) {
        EmployeeEntryDO entry = validateEntryExists(id);
        if (Objects.equals(entry.getWorkStatus(), 4)) {
            throw ServiceExceptionUtil.exception(EMPLOYEE_ALREADY_RESIGNED);
        }
        return entry;
    }

    private EmployeeProfileDO validateProfileExists(Long id) {
        EmployeeProfileDO profile = employeeProfileMapper.selectById(id);
        if (profile == null) {
            throw ServiceExceptionUtil.exception(EMPLOYEE_PROFILE_NOT_EXISTS);
        }
        return profile;
    }

    private HrLifecycleEventDO validateEventExists(Long id) {
        HrLifecycleEventDO event = hrLifecycleEventMapper.selectById(id);
        if (event == null) {
            throw ServiceExceptionUtil.exception(HR_LIFECYCLE_EVENT_NOT_EXISTS);
        }
        return event;
    }

    private HrLifecycleTaskDO validateTaskExists(Long id) {
        HrLifecycleTaskDO task = hrLifecycleTaskMapper.selectById(id);
        if (task == null) {
            throw ServiceExceptionUtil.exception(HR_LIFECYCLE_TASK_NOT_EXISTS);
        }
        return task;
    }

}
