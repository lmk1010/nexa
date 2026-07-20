package com.kyx.service.hr.service.selfservice;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.foundation.security.core.util.SecurityFrameworkUtils;
import com.kyx.service.hr.controller.admin.exam.vo.ExamPublishPageReqVO;
import com.kyx.service.hr.controller.admin.exam.vo.ExamPublishRespVO;
import com.kyx.service.hr.controller.admin.selfservice.vo.HrSelfServiceHomeRespVO;
import com.kyx.service.hr.dal.dataobject.administrative.HrAdministrativeLeaveDO;
import com.kyx.service.hr.dal.dataobject.administrative.HrAdministrativeTripDO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceClockRecordDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeEntryDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeProfileDO;
import com.kyx.service.hr.dal.dataobject.exam.ExamAttemptDO;
import com.kyx.service.hr.dal.dataobject.lifecycle.HrLifecycleEventDO;
import com.kyx.service.hr.dal.dataobject.lifecycle.HrLifecycleTaskDO;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnaireAssignmentDO;
import com.kyx.service.hr.dal.mysql.administrative.HrAdministrativeLeaveMapper;
import com.kyx.service.hr.dal.mysql.administrative.HrAdministrativeTripMapper;
import com.kyx.service.hr.dal.mysql.attendance.AttendanceClockRecordMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeEntryMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeProfileMapper;
import com.kyx.service.hr.dal.mysql.exam.ExamAttemptMapper;
import com.kyx.service.hr.dal.mysql.lifecycle.HrLifecycleEventMapper;
import com.kyx.service.hr.dal.mysql.lifecycle.HrLifecycleTaskMapper;
import com.kyx.service.hr.dal.mysql.questionnaire.QuestionnaireAssignmentMapper;
import com.kyx.service.hr.dal.mysql.todo.HrTodoTaskMapper;
import com.kyx.service.hr.service.exam.ExamPublishService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Validated
public class HrSelfServiceHomeServiceImpl implements HrSelfServiceHomeService {

    private static final int BPM_STATUS_RUNNING = 1;
    private static final int QUESTIONNAIRE_STATUS_PENDING = 0;
    private static final int EXAM_STATUS_IN_PROGRESS = 0;

    @Resource
    private EmployeeProfileMapper employeeProfileMapper;
    @Resource
    private EmployeeEntryMapper employeeEntryMapper;
    @Resource
    private AttendanceClockRecordMapper attendanceClockRecordMapper;
    @Resource
    private HrAdministrativeLeaveMapper leaveMapper;
    @Resource
    private HrAdministrativeTripMapper tripMapper;
    @Resource
    private QuestionnaireAssignmentMapper questionnaireAssignmentMapper;
    @Resource
    private ExamAttemptMapper examAttemptMapper;
    @Resource
    private HrLifecycleEventMapper lifecycleEventMapper;
    @Resource
    private HrLifecycleTaskMapper lifecycleTaskMapper;
    @Resource
    private HrTodoTaskMapper hrTodoTaskMapper;
    @Resource
    private HrQuickActionConfigService hrQuickActionConfigService;
    @Resource
    private ExamPublishService examPublishService;

    @Override
    public HrSelfServiceHomeRespVO getHome() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        EmployeeProfileDO profile = userId == null ? null : employeeProfileMapper.selectByUserId(userId);
        EmployeeEntryDO entry = selectCurrentEntry(profile, userId);

        HrSelfServiceHomeRespVO respVO = new HrSelfServiceHomeRespVO();
        respVO.setHasProfile(profile != null);
        respVO.setProfile(buildProfile(profile, userId));
        respVO.setEmployment(buildEmployment(entry));
        respVO.setTodayAttendance(buildTodayAttendance(userId));
        respVO.setTodoSummary(buildTodoSummary(userId, profile));
        respVO.setProfileHealth(buildProfileHealth(profile, entry));
        respVO.setLifecycleItems(buildLifecycleItems(profile));
        respVO.setQuickActions(hrQuickActionConfigService.getHomeActions());
        return respVO;
    }

    private EmployeeEntryDO selectCurrentEntry(EmployeeProfileDO profile, Long userId) {
        List<EmployeeEntryDO> entries = new ArrayList<>();
        if (profile != null && profile.getId() != null) {
            entries.addAll(employeeEntryMapper.selectListByProfileId(profile.getId()));
        }
        if (entries.isEmpty() && userId != null) {
            entries.addAll(employeeEntryMapper.selectListByUserId(userId));
        }
        if (entries.isEmpty()) {
            return null;
        }
        entries.sort((left, right) -> {
            boolean leftResigned = Objects.equals(left.getWorkStatus(), 4);
            boolean rightResigned = Objects.equals(right.getWorkStatus(), 4);
            if (leftResigned != rightResigned) {
                return leftResigned ? 1 : -1;
            }
            Long leftId = left.getId() == null ? 0L : left.getId();
            Long rightId = right.getId() == null ? 0L : right.getId();
            return rightId.compareTo(leftId);
        });
        return entries.get(0);
    }

    private HrSelfServiceHomeRespVO.EmployeeProfile buildProfile(EmployeeProfileDO profile, Long userId) {
        HrSelfServiceHomeRespVO.EmployeeProfile item = new HrSelfServiceHomeRespVO.EmployeeProfile();
        item.setUserId(userId);
        if (profile == null) {
            return item;
        }
        item.setProfileId(profile.getId());
        item.setProfileNo(profile.getProfileNo());
        item.setName(profile.getName());
        item.setMobile(profile.getMobile());
        item.setEmail(profile.getEmail());
        item.setOnboardDate(profile.getOnboardDate());
        item.setConfirmationDate(profile.getConfirmationDate());
        return item;
    }

    private HrSelfServiceHomeRespVO.Employment buildEmployment(EmployeeEntryDO entry) {
        HrSelfServiceHomeRespVO.Employment item = new HrSelfServiceHomeRespVO.Employment();
        if (entry == null) {
            return item;
        }
        item.setEntryId(entry.getId());
        item.setEmployeeNo(entry.getEmployeeNo());
        item.setDeptId(entry.getDeptId());
        item.setJobTitle(entry.getJobTitle());
        item.setWorkStatus(entry.getWorkStatus());
        item.setWorkStatusText(workStatusText(entry.getWorkStatus()));
        item.setEntryDate(entry.getEntryDate());
        item.setContractEndDate(entry.getContractEndDate());
        return item;
    }

    private HrSelfServiceHomeRespVO.TodayAttendance buildTodayAttendance(Long userId) {
        LocalDate today = LocalDate.now();
        HrSelfServiceHomeRespVO.TodayAttendance item = new HrSelfServiceHomeRespVO.TodayAttendance();
        item.setAttendanceDate(today);
        if (userId == null) {
            item.setMonthClockDays(0);
            item.setOnLeaveToday(false);
            item.setOnTripToday(false);
            return item;
        }

        List<AttendanceClockRecordDO> records = attendanceClockRecordMapper.selectListByUserIdAndDate(userId, today);
        AttendanceClockRecordDO clockIn = records.stream()
                .filter(record -> "IN".equals(record.getClockType()))
                .findFirst()
                .orElse(null);
        AttendanceClockRecordDO clockOut = records.stream()
                .filter(record -> "OUT".equals(record.getClockType()))
                .reduce((first, second) -> second)
                .orElse(null);
        if (clockIn != null) {
            item.setClockInTime(clockIn.getClockTime());
            item.setClockInStatus(clockIn.getClockStatus());
        }
        if (clockOut != null) {
            item.setClockOutTime(clockOut.getClockTime());
            item.setClockOutStatus(clockOut.getClockStatus());
        }

        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
        item.setMonthClockDays((int) attendanceClockRecordMapper
                .selectListByUserIdAndDateRange(userId, monthStart, monthEnd)
                .stream()
                .map(AttendanceClockRecordDO::getAttendanceDate)
                .filter(Objects::nonNull)
                .distinct()
                .count());
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = LocalDateTime.of(today, LocalTime.MAX);
        item.setOnLeaveToday(!leaveMapper.selectListByUserIdAndTimeRange(userId, dayStart, dayEnd).isEmpty());
        item.setOnTripToday(!tripMapper.selectListByUserIdAndTimeRange(userId, dayStart, dayEnd).isEmpty());
        return item;
    }

    private HrSelfServiceHomeRespVO.TodoSummary buildTodoSummary(Long userId, EmployeeProfileDO profile) {
        HrSelfServiceHomeRespVO.TodoSummary item = new HrSelfServiceHomeRespVO.TodoSummary();
        if (userId == null) {
            item.setPendingQuestionnaireCount(0);
            item.setAvailableExamCount(0);
            item.setInProgressExamCount(0);
            item.setPendingLifecycleTaskCount(0);
            item.setRunningLeaveCount(0);
            item.setRunningTripCount(0);
            return item;
        }
        item.setPendingQuestionnaireCount(countLong(questionnaireAssignmentMapper.selectCount(
                new LambdaQueryWrapperX<QuestionnaireAssignmentDO>()
                        .eq(QuestionnaireAssignmentDO::getEvaluatorId, userId)
                        .eq(QuestionnaireAssignmentDO::getStatus, QUESTIONNAIRE_STATUS_PENDING))));
        item.setAvailableExamCount(countAvailableExams(userId));
        item.setInProgressExamCount(countLong(examAttemptMapper.selectCount(
                new LambdaQueryWrapperX<ExamAttemptDO>()
                        .eq(ExamAttemptDO::getUserId, userId)
                        .eq(ExamAttemptDO::getStatus, EXAM_STATUS_IN_PROGRESS))));
        item.setPendingLifecycleTaskCount(lifecycleTaskMapper
                .selectPendingListByProfileOrAssignee(profile == null ? null : profile.getId(), userId, 200)
                .size());
        item.setOpenTodoCount(countOpenTodo(userId));
        item.setRunningLeaveCount(countLong(leaveMapper.selectCountByUserIdAndStatus(userId, BPM_STATUS_RUNNING)));
        item.setRunningTripCount(countLong(tripMapper.selectCountByUserIdAndStatus(userId, BPM_STATUS_RUNNING)));
        return item;
    }

    private Integer countOpenTodo(Long userId) {
        return countLong(hrTodoTaskMapper.selectScopeCount("OPEN", userId, null, null, null));
    }

    private Integer countAvailableExams(Long userId) {
        ExamPublishPageReqVO pageReqVO = new ExamPublishPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(500);
        pageReqVO.setMine(true);
        pageReqVO.setCurrentUserId(userId);
        PageResult<ExamPublishRespVO> pageResult = examPublishService.getPublishPage(pageReqVO);
        if (pageResult == null || pageResult.getList() == null) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        return (int) pageResult.getList().stream()
                .filter(item -> Objects.equals(item.getStatus(), 1))
                .filter(item -> item.getStartAt() == null || !now.isBefore(item.getStartAt()))
                .filter(item -> item.getEndAt() == null || !now.isAfter(item.getEndAt()))
                .count();
    }

    private HrSelfServiceHomeRespVO.ProfileHealth buildProfileHealth(EmployeeProfileDO profile, EmployeeEntryDO entry) {
        List<String> missingFields = new ArrayList<>();
        if (profile == null) {
            missingFields.add("员工档案");
        } else {
            addMissing(missingFields, "手机号", profile.getMobile());
            addMissing(missingFields, "邮箱", profile.getEmail());
            addMissing(missingFields, "身份证号", profile.getIdNumber());
            addMissing(missingFields, "现住址", profile.getAddress());
            addMissing(missingFields, "紧急联系人", profile.getEmergencyContact());
            addMissing(missingFields, "紧急联系电话", profile.getEmergencyPhone());
        }
        if (entry == null) {
            missingFields.add("任职记录");
        } else {
            addMissing(missingFields, "员工编号", entry.getEmployeeNo());
            addMissing(missingFields, "部门", entry.getDeptId());
            addMissing(missingFields, "职位", entry.getJobTitle());
            addMissing(missingFields, "入职日期", entry.getEntryDate());
            addMissing(missingFields, "合同结束日期", entry.getContractEndDate());
        }

        int total = 11;
        int completed = Math.max(0, total - missingFields.size());
        HrSelfServiceHomeRespVO.ProfileHealth health = new HrSelfServiceHomeRespVO.ProfileHealth();
        health.setCompleteness((int) Math.round(completed * 100.0 / total));
        health.setMissingFields(missingFields);
        return health;
    }

    private List<HrSelfServiceHomeRespVO.LifecycleItem> buildLifecycleItems(EmployeeProfileDO profile) {
        if (profile == null || profile.getId() == null) {
            return new ArrayList<>();
        }
        return lifecycleEventMapper.selectListByProfileId(profile.getId()).stream()
                .filter(event -> !"CANCELLED".equals(event.getEventStatus()))
                .limit(8)
                .map(this::buildLifecycleItem)
                .collect(Collectors.toList());
    }

    private HrSelfServiceHomeRespVO.LifecycleItem buildLifecycleItem(HrLifecycleEventDO event) {
        HrSelfServiceHomeRespVO.LifecycleItem item = new HrSelfServiceHomeRespVO.LifecycleItem();
        item.setEventId(event.getId());
        item.setEventType(event.getEventType());
        item.setEventTypeName(eventTypeName(event.getEventType()));
        item.setEventStatus(event.getEventStatus());
        item.setEventStatusText(eventStatusText(event.getEventStatus()));
        item.setEffectiveDate(event.getEffectiveDate());
        return item;
    }

    private void addMissing(List<String> missingFields, String label, Object value) {
        if (value == null || (value instanceof String && !StringUtils.hasText((String) value))) {
            missingFields.add(label);
        }
    }

    private String workStatusText(Integer workStatus) {
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

    private String eventTypeName(String eventType) {
        if ("ONBOARDING_CONFIRMED".equals(eventType)) {
            return "确认入职";
        }
        if ("ONBOARDING_CREATED".equals(eventType)) {
            return "创建入职";
        }
        if ("PROBATION_STARTED".equals(eventType)) {
            return "进入试用期";
        }
        if ("REGULARIZATION_REQUESTED".equals(eventType)) {
            return "转正办理";
        }
        if ("TRANSFER_REQUESTED".equals(eventType)) {
            return "调岗办理";
        }
        if ("SALARY_ADJUST_REQUESTED".equals(eventType)) {
            return "调薪办理";
        }
        if ("RESIGN_REQUESTED".equals(eventType)) {
            return "离职办理";
        }
        if ("RESIGN_EFFECTIVE".equals(eventType)) {
            return "离职生效";
        }
        return eventType;
    }

    private String eventStatusText(String status) {
        if ("PENDING_APPROVAL".equals(status)) {
            return "待审批";
        }
        if ("PENDING_HANDOVER".equals(status)) {
            return "待交接";
        }
        if ("PENDING_EFFECTIVE".equals(status)) {
            return "待生效";
        }
        if ("COMPLETED".equals(status)) {
            return "已完成";
        }
        if ("REJECTED".equals(status)) {
            return "已拒绝";
        }
        if ("CANCELLED".equals(status)) {
            return "已撤销";
        }
        return status;
    }

    private int countLong(Long value) {
        return value == null ? 0 : value.intValue();
    }

}
