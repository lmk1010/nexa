package com.kyx.service.hr.service.todo;

import com.kyx.foundation.common.exception.enums.GlobalErrorCodeConstants;
import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.foundation.security.core.service.SecurityFrameworkService;
import com.kyx.foundation.security.core.util.SecurityFrameworkUtils;
import com.kyx.service.business.api.dept.DeptApi;
import com.kyx.service.business.api.dept.dto.DeptRespDTO;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import com.kyx.service.hr.controller.admin.todo.vo.HrTodoCompleteReqVO;
import com.kyx.service.hr.controller.admin.todo.vo.HrTodoPageReqVO;
import com.kyx.service.hr.controller.admin.todo.vo.HrTodoRespVO;
import com.kyx.service.hr.controller.admin.todo.vo.HrTodoSummaryReqVO;
import com.kyx.service.hr.controller.admin.todo.vo.HrTodoSummaryRespVO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceCorrectionDO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceExceptionDO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceMonthlyConfirmDO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceOvertimeDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeEntryDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeProfileDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeDocumentRequestDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeMaterialDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeePerformanceDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeProfileChangeRequestDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeRecruitmentDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeTrainingDO;
import com.kyx.service.hr.dal.dataobject.exam.ExamAttemptDO;
import com.kyx.service.hr.dal.dataobject.exam.ExamDO;
import com.kyx.service.hr.dal.dataobject.exam.ExamPublishDO;
import com.kyx.service.hr.dal.dataobject.lifecycle.HrLifecycleTaskDO;
import com.kyx.service.hr.dal.dataobject.payroll.PayslipDO;
import com.kyx.service.hr.dal.dataobject.payroll.SocialSecurityAccountDO;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnaireAssignmentDO;
import com.kyx.service.hr.dal.dataobject.risk.HrRiskEventDO;
import com.kyx.service.hr.dal.dataobject.todo.HrTodoTaskDO;
import com.kyx.service.hr.dal.dataobject.training.TrainingAssignmentDO;
import com.kyx.service.hr.dal.dataobject.training.TrainingCourseDO;
import com.kyx.service.hr.dal.dataobject.training.TrainingPlanDO;
import com.kyx.service.hr.dal.mysql.attendance.AttendanceCorrectionMapper;
import com.kyx.service.hr.dal.mysql.attendance.AttendanceMonthlyConfirmMapper;
import com.kyx.service.hr.dal.mysql.attendance.AttendanceExceptionMapper;
import com.kyx.service.hr.dal.mysql.attendance.AttendanceOvertimeMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeDocumentRequestMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeMaterialMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeePerformanceMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeProfileChangeRequestMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeEntryMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeProfileMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeRecruitmentMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeTrainingMapper;
import com.kyx.service.hr.dal.mysql.exam.ExamAttemptMapper;
import com.kyx.service.hr.dal.mysql.exam.ExamMapper;
import com.kyx.service.hr.dal.mysql.exam.ExamPublishMapper;
import com.kyx.service.hr.dal.mysql.lifecycle.HrLifecycleTaskMapper;
import com.kyx.service.hr.dal.mysql.payroll.PayslipMapper;
import com.kyx.service.hr.dal.mysql.payroll.SocialSecurityAccountMapper;
import com.kyx.service.hr.dal.mysql.questionnaire.QuestionnaireAssignmentMapper;
import com.kyx.service.hr.dal.mysql.risk.HrRiskEventMapper;
import com.kyx.service.hr.dal.mysql.todo.HrTodoTaskMapper;
import com.kyx.service.hr.dal.mysql.training.TrainingAssignmentMapper;
import com.kyx.service.hr.dal.mysql.training.TrainingCourseMapper;
import com.kyx.service.hr.dal.mysql.training.TrainingPlanMapper;
import com.kyx.service.hr.service.exam.ExamScopeSupport;
import com.kyx.service.hr.service.risk.HrRiskWorkbenchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Unified HR todo task Service implementation.
 */
@Service
@Validated
@Slf4j
public class HrTodoTaskServiceImpl implements HrTodoTaskService {

    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_DONE = "DONE";
    private static final String STATUS_CANCELED = "CANCELED";

    private static final String PRIORITY_HIGH = "HIGH";
    private static final String PRIORITY_MEDIUM = "MEDIUM";
    private static final String PRIORITY_LOW = "LOW";

    private static final String BUSINESS_LIFECYCLE_TASK = "LIFECYCLE_TASK";
    private static final String BUSINESS_ATTENDANCE_CORRECTION = "ATTENDANCE_CORRECTION";
    private static final String BUSINESS_ATTENDANCE_OVERTIME = "ATTENDANCE_OVERTIME";
    private static final String BUSINESS_ATTENDANCE_MONTHLY_CONFIRM = "ATTENDANCE_MONTHLY_CONFIRM";
    private static final String BUSINESS_ATTENDANCE_EXCEPTION = "ATTENDANCE_EXCEPTION";
    private static final String BUSINESS_PAYSLIP_CONFIRM = "PAYSLIP_CONFIRM";
    private static final String BUSINESS_SOCIAL_SECURITY_CHANGE = "SOCIAL_SECURITY_CHANGE";
    private static final String BUSINESS_EXAM_ASSIGNMENT = "EXAM_ASSIGNMENT";
    private static final String BUSINESS_QUESTIONNAIRE_ASSIGNMENT = "QUESTIONNAIRE_ASSIGNMENT";
    private static final String BUSINESS_DOCUMENT_REQUEST = "DOCUMENT_REQUEST";
    private static final String BUSINESS_EMPLOYEE_MATERIAL = "EMPLOYEE_MATERIAL";
    private static final String BUSINESS_PROFILE_CHANGE = "PROFILE_CHANGE";
    private static final String BUSINESS_PERFORMANCE_FOLLOW = "PERFORMANCE_FOLLOW";
    private static final String BUSINESS_PERFORMANCE_APPLICATION = "PERFORMANCE_APPLICATION";
    private static final String BUSINESS_RECRUITMENT_INTERVIEW = "RECRUITMENT_INTERVIEW";
    private static final String BUSINESS_RECRUITMENT_TOUCH = "RECRUITMENT_TOUCH";
    private static final String BUSINESS_RECRUITMENT_FOLLOW = "RECRUITMENT_FOLLOW";
    private static final String BUSINESS_TRAINING_FOLLOW = "TRAINING_FOLLOW";
    private static final String BUSINESS_TRAINING_ASSIGNMENT = "TRAINING_ASSIGNMENT";
    private static final String BUSINESS_RISK_EVENT = "RISK_EVENT";

    private static final String TASK_DEFAULT = "DEFAULT";
    private static final String TASK_APPROVE = "APPROVE";
    private static final String TASK_FILL = "FILL";
    private static final String TASK_INTERVIEW = "INTERVIEW";
    private static final String TASK_TOUCH = "TOUCH";
    private static final String TASK_FOLLOW = "FOLLOW";
    private static final String TASK_SOCIAL_ADD_PREFIX = "ADD_";
    private static final String TASK_SOCIAL_STOP_PREFIX = "STOP_";
    private static final String TASK_ATTEMPT_PREFIX = "ATTEMPT_";
    private static final int SOURCE_SYNC_LIMIT = 500;
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final String SOCIAL_PENDING_ADD = "PENDING_ADD";
    private static final String SOCIAL_ENROLLED = "ENROLLED";
    private static final String SOCIAL_PENDING_STOP = "PENDING_STOP";
    private static final String SOCIAL_SUSPENDED = "SUSPENDED";
    private static final String SOCIAL_STOPPED = "STOPPED";

    private static final String PERMISSION_TODO_QUERY = "hr:todo:query";

    @Resource
    private HrTodoTaskMapper hrTodoTaskMapper;
    @Resource
    private HrLifecycleTaskMapper lifecycleTaskMapper;
    @Resource
    private AttendanceCorrectionMapper attendanceCorrectionMapper;
    @Resource
    private AttendanceOvertimeMapper attendanceOvertimeMapper;
    @Resource
    private AttendanceMonthlyConfirmMapper attendanceMonthlyConfirmMapper;
    @Resource
    private AttendanceExceptionMapper attendanceExceptionMapper;
    @Resource
    private EmployeeDocumentRequestMapper documentRequestMapper;
    @Resource
    private EmployeeMaterialMapper employeeMaterialMapper;
    @Resource
    private EmployeePerformanceMapper employeePerformanceMapper;
    @Resource
    private EmployeeProfileChangeRequestMapper profileChangeRequestMapper;
    @Resource
    private EmployeeRecruitmentMapper employeeRecruitmentMapper;
    @Resource
    private EmployeeTrainingMapper employeeTrainingMapper;
    @Resource
    private EmployeeEntryMapper employeeEntryMapper;
    @Resource
    private TrainingAssignmentMapper trainingAssignmentMapper;
    @Resource
    private TrainingPlanMapper trainingPlanMapper;
    @Resource
    private HrRiskEventMapper hrRiskEventMapper;
    @Resource
    private HrRiskWorkbenchService hrRiskWorkbenchService;
    @Resource
    private TrainingCourseMapper trainingCourseMapper;
    @Resource
    private PayslipMapper payslipMapper;
    @Resource
    private SocialSecurityAccountMapper socialSecurityAccountMapper;
    @Resource
    private QuestionnaireAssignmentMapper questionnaireAssignmentMapper;
    @Resource
    private ExamPublishMapper examPublishMapper;
    @Resource
    private ExamAttemptMapper examAttemptMapper;
    @Resource
    private ExamMapper examMapper;
    @Resource
    private ExamScopeSupport examScopeSupport;
    @Resource
    private EmployeeProfileMapper employeeProfileMapper;
    @Resource
    private DeptApi deptApi;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private SecurityFrameworkService securityFrameworkService;

    @Override
    public PageResult<HrTodoRespVO> getPage(HrTodoPageReqVO pageReqVO) {
        applyDeptScope(pageReqVO);
        refreshGeneratedTasks();
        normalizePageReq(pageReqVO);
        boolean manage = canManage();
        PageResult<HrTodoTaskDO> pageResult = hrTodoTaskMapper.selectPage(pageReqVO, manage, SecurityFrameworkUtils.getLoginUserId());
        List<HrTodoTaskDO> rows = pageResult.getList();
        if (rows == null || rows.isEmpty()) {
            return new PageResult<>(new ArrayList<>(), pageResult.getTotal());
        }
        List<HrTodoRespVO> respList = BeanUtils.toBean(rows, HrTodoRespVO.class);
        fillPeopleInfo(rows, respList);
        return new PageResult<>(respList, pageResult.getTotal());
    }

    @Override
    @Cacheable(cacheNames = "hr:todo:summary#10s",
            key = "'mine:' + #mine + ':user:' + T(com.kyx.foundation.security.core.util.SecurityFrameworkUtils).getLoginUserId()",
            sync = true)
    public HrTodoSummaryRespVO getSummary(Boolean mine) {
        HrTodoSummaryReqVO reqVO = new HrTodoSummaryReqVO();
        reqVO.setMine(mine);
        return getSummary(reqVO);
    }

    @Override
    public HrTodoSummaryRespVO getSummary(HrTodoSummaryReqVO reqVO) {
        refreshGeneratedTasks();
        Boolean mine = reqVO == null ? null : reqVO.getMine();
        Long assigneeUserId = scopeAssigneeUserId(Boolean.TRUE.equals(mine) || !canManage());
        Set<Long> profileIds = resolveSummaryProfileIds(reqVO);
        LocalDateTime now = LocalDateTime.now();
        HrTodoSummaryRespVO respVO = new HrTodoSummaryRespVO();
        respVO.setOpenCount(count(selectSummaryScopeCount(STATUS_OPEN, assigneeUserId, null, null, null, profileIds)));
        respVO.setOverdueCount(count(selectSummaryScopeCount(STATUS_OPEN, assigneeUserId, now, null, null, profileIds)));
        respVO.setDueSoonCount(count(selectSummaryDueSoonCount(assigneeUserId, now, now.plusDays(7), profileIds)));
        respVO.setHighPriorityCount(count(selectSummaryScopeCount(STATUS_OPEN, assigneeUserId, null, PRIORITY_HIGH, null, profileIds)));
        respVO.setGeneratedOpenCount(count(selectSummaryScopeCount(STATUS_OPEN, assigneeUserId, null, null, true, profileIds)));
        respVO.setDoneCount(count(selectSummaryScopeCount(STATUS_DONE, assigneeUserId, null, null, null, profileIds)));
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean complete(HrTodoCompleteReqVO reqVO) {
        HrTodoTaskDO todo = hrTodoTaskMapper.selectById(reqVO.getId());
        if (todo == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND, "待办不存在");
        }
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (!canManage() && !Objects.equals(todo.getAssigneeUserId(), loginUserId)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.FORBIDDEN, "无权处理该待办");
        }
        if (Boolean.TRUE.equals(todo.getGeneratedFlag())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "业务来源待办请进入对应业务页面处理");
        }
        if (!STATUS_OPEN.equals(todo.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "只有待处理待办可以完成");
        }
        HrTodoTaskDO updateDO = new HrTodoTaskDO();
        updateDO.setId(todo.getId());
        updateDO.setStatus(STATUS_DONE);
        updateDO.setCompletedBy(loginUserId);
        updateDO.setCompletedTime(LocalDateTime.now());
        updateDO.setCancelReason(reqVO.getRemark());
        hrTodoTaskMapper.updateById(updateDO);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer refreshGeneratedTasks() {
        int changed = 0;
        changed += syncLifecycleTasks();
        changed += syncAttendanceCorrections();
        changed += syncAttendanceOvertimes();
        changed += syncAttendanceMonthlyConfirms();
        changed += syncAttendanceExceptions();
        changed += syncPayslipConfirms();
        changed += syncSocialSecurityChanges();
        changed += syncDocumentRequests();
        changed += syncEmployeeMaterials();
        changed += syncProfileChanges();
        changed += syncPerformanceFollows();
        changed += syncPerformanceApplications();
        changed += syncRecruitmentInterviews();
        changed += syncRecruitmentTouches();
        changed += syncRecruitmentFollows();
        changed += syncTrainingAssignments();
        changed += syncTrainingFollows();
        changed += syncExamAssignments();
        changed += syncQuestionnaireAssignments();
        changed += syncRiskEvents();
        return changed;
    }

    private int syncLifecycleTasks() {
        List<HrLifecycleTaskDO> tasks = lifecycleTaskMapper.selectList(new LambdaQueryWrapperX<HrLifecycleTaskDO>()
                .ne(HrLifecycleTaskDO::getTaskStatus, "DONE")
                .orderByAsc(HrLifecycleTaskDO::getDueDate)
                .orderByDesc(HrLifecycleTaskDO::getId)
                .last("LIMIT " + SOURCE_SYNC_LIMIT));
        Set<Long> openIds = new HashSet<>();
        int changed = 0;
        for (HrLifecycleTaskDO task : tasks) {
            if (task.getId() == null) {
                continue;
            }
            openIds.add(task.getId());
            LocalDateTime dueTime = task.getDueDate() == null ? null : LocalDateTime.of(task.getDueDate(), LocalTime.of(18, 0));
            changed += upsertGeneratedTask(BUSINESS_LIFECYCLE_TASK, task.getId(), defaultText(task.getTaskType(), TASK_DEFAULT),
                    task.getAssigneeUserId(), task.getProfileId(), defaultText(task.getTaskName(), "生命周期检查项"),
                    lifecycleContent(task), "/hr/lifecycle/workbench?eventId=" + task.getEventId(),
                    priorityByDueTime(dueTime), dueTime);
        }
        if (tasks.size() < SOURCE_SYNC_LIMIT) {
            changed += closeStaleGeneratedTasks(BUSINESS_LIFECYCLE_TASK, openIds);
        }
        return changed;
    }

    private int syncAttendanceCorrections() {
        List<AttendanceCorrectionDO> corrections = attendanceCorrectionMapper.selectList(new LambdaQueryWrapperX<AttendanceCorrectionDO>()
                .eq(AttendanceCorrectionDO::getStatus, "PENDING")
                .isNull(AttendanceCorrectionDO::getProcessInstanceId)
                .orderByDesc(AttendanceCorrectionDO::getAttendanceDate)
                .orderByDesc(AttendanceCorrectionDO::getId)
                .last("LIMIT " + SOURCE_SYNC_LIMIT));
        Set<Long> openIds = new HashSet<>();
        int changed = 0;
        for (AttendanceCorrectionDO correction : corrections) {
            if (correction.getId() == null) {
                continue;
            }
            openIds.add(correction.getId());
            LocalDateTime dueTime = correction.getAttendanceDate() == null
                    ? null : LocalDateTime.of(correction.getAttendanceDate().plusDays(2), LocalTime.of(18, 0));
            changed += upsertGeneratedTask(BUSINESS_ATTENDANCE_CORRECTION, correction.getId(), TASK_APPROVE,
                    null, correction.getProfileId(), correctionTitle(correction),
                    correctionContent(correction), "/attendance/corrections",
                    priorityByDueTime(dueTime), dueTime);
        }
        if (corrections.size() < SOURCE_SYNC_LIMIT) {
            changed += closeStaleGeneratedTasks(BUSINESS_ATTENDANCE_CORRECTION, openIds);
        }
        return changed;
    }

    private int syncAttendanceOvertimes() {
        List<AttendanceOvertimeDO> overtimes = attendanceOvertimeMapper.selectList(new LambdaQueryWrapperX<AttendanceOvertimeDO>()
                .eq(AttendanceOvertimeDO::getStatus, "PENDING")
                .isNull(AttendanceOvertimeDO::getProcessInstanceId)
                .orderByDesc(AttendanceOvertimeDO::getOvertimeDate)
                .orderByDesc(AttendanceOvertimeDO::getId)
                .last("LIMIT " + SOURCE_SYNC_LIMIT));
        Set<Long> openIds = new HashSet<>();
        int changed = 0;
        for (AttendanceOvertimeDO overtime : overtimes) {
            if (overtime.getId() == null) {
                continue;
            }
            openIds.add(overtime.getId());
            LocalDateTime dueTime = overtime.getOvertimeDate() == null
                    ? null : LocalDateTime.of(overtime.getOvertimeDate().plusDays(2), LocalTime.of(18, 0));
            changed += upsertGeneratedTask(BUSINESS_ATTENDANCE_OVERTIME, overtime.getId(), TASK_APPROVE,
                    null, overtime.getProfileId(), "加班调休审批",
                    overtimeContent(overtime), "/attendance/overtime",
                    priorityByDueTime(dueTime), dueTime);
        }
        if (overtimes.size() < SOURCE_SYNC_LIMIT) {
            changed += closeStaleGeneratedTasks(BUSINESS_ATTENDANCE_OVERTIME, openIds);
        }
        return changed;
    }

    private int syncAttendanceMonthlyConfirms() {
        List<AttendanceMonthlyConfirmDO> confirms = attendanceMonthlyConfirmMapper.selectOpenList(SOURCE_SYNC_LIMIT);
        Set<Long> openIds = new HashSet<>();
        int changed = 0;
        for (AttendanceMonthlyConfirmDO confirm : confirms) {
            if (confirm.getId() == null) {
                continue;
            }
            openIds.add(confirm.getId());
            LocalDateTime dueTime = monthlyConfirmDueTime(confirm);
            if ("ISSUE".equals(confirm.getStatus())) {
                changed += upsertGeneratedTask(BUSINESS_ATTENDANCE_MONTHLY_CONFIRM, confirm.getId(), TASK_DEFAULT,
                        null, confirm.getProfileId(), "月度考勤异议处理：" + defaultText(confirm.getSettlementMonth(), ""),
                        monthlyConfirmContent(confirm), "/attendance/monthly-settlement?confirmId=" + confirm.getId(),
                        PRIORITY_MEDIUM, dueTime);
            } else {
                changed += upsertGeneratedTask(BUSINESS_ATTENDANCE_MONTHLY_CONFIRM, confirm.getId(), TASK_DEFAULT,
                        confirm.getUserId(), confirm.getProfileId(), "月度考勤确认：" + defaultText(confirm.getSettlementMonth(), ""),
                        monthlyConfirmContent(confirm), "/attendance/my-monthly-confirm?id=" + confirm.getId(),
                        priorityByDueTime(dueTime), dueTime);
            }
        }
        if (confirms.size() < SOURCE_SYNC_LIMIT) {
            changed += closeStaleGeneratedTasks(BUSINESS_ATTENDANCE_MONTHLY_CONFIRM, openIds);
        }
        return changed;
    }

    private int syncAttendanceExceptions() {
        List<AttendanceExceptionDO> exceptions = attendanceExceptionMapper.selectList(new LambdaQueryWrapperX<AttendanceExceptionDO>()
                .eq(AttendanceExceptionDO::getExceptionStatus, "PENDING")
                .orderByDesc(AttendanceExceptionDO::getAttendanceDate)
                .orderByDesc(AttendanceExceptionDO::getId)
                .last("LIMIT " + SOURCE_SYNC_LIMIT));
        Set<Long> openIds = new HashSet<>();
        int changed = 0;
        for (AttendanceExceptionDO exception : exceptions) {
            if (exception.getId() == null) {
                continue;
            }
            openIds.add(exception.getId());
            LocalDateTime dueTime = exception.getAttendanceDate() == null
                    ? null : LocalDateTime.of(exception.getAttendanceDate().plusDays(1), LocalTime.of(18, 0));
            changed += upsertGeneratedTask(BUSINESS_ATTENDANCE_EXCEPTION, exception.getId(), TASK_DEFAULT,
                    null, exception.getProfileId(), "考勤异常待处理",
                    exceptionContent(exception), "/attendance/exceptions?exceptionId=" + exception.getId(),
                    priorityByDueTime(dueTime), dueTime);
        }
        if (exceptions.size() < SOURCE_SYNC_LIMIT) {
            changed += closeStaleGeneratedTasks(BUSINESS_ATTENDANCE_EXCEPTION, openIds);
        }
        return changed;
    }

    private int syncPayslipConfirms() {
        List<PayslipDO> slips = payslipMapper.selectOpenList(SOURCE_SYNC_LIMIT);
        Set<Long> openIds = new HashSet<>();
        int changed = 0;
        for (PayslipDO slip : slips) {
            if (slip.getId() == null) {
                continue;
            }
            openIds.add(slip.getId());
            LocalDateTime dueTime = payslipDueTime(slip);
            if ("ISSUE".equals(slip.getStatus())) {
                changed += upsertGeneratedTask(BUSINESS_PAYSLIP_CONFIRM, slip.getId(), TASK_DEFAULT,
                        null, slip.getProfileId(), "工资条异议处理：" + defaultText(slip.getPayrollMonth(), ""),
                        payslipContent(slip), "/hr/payroll?slipId=" + slip.getId(),
                        PRIORITY_HIGH, dueTime);
            } else {
                changed += upsertGeneratedTask(BUSINESS_PAYSLIP_CONFIRM, slip.getId(), TASK_DEFAULT,
                        slip.getUserId(), slip.getProfileId(), "工资条确认：" + defaultText(slip.getPayrollMonth(), ""),
                        payslipContent(slip), "/hr/my-payslip?id=" + slip.getId(),
                        priorityByDueTime(dueTime), dueTime);
            }
        }
        if (slips.size() < SOURCE_SYNC_LIMIT) {
            changed += closeStaleGeneratedTasks(BUSINESS_PAYSLIP_CONFIRM, openIds);
        }
        return changed;
    }

    private int syncSocialSecurityChanges() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = monthStart.with(TemporalAdjusters.lastDayOfMonth());
        String socialMonth = monthStart.format(MONTH_FORMATTER);
        List<EmployeeEntryDO> newHireEntries = employeeEntryMapper.selectList(new LambdaQueryWrapperX<EmployeeEntryDO>()
                .between(EmployeeEntryDO::getEntryDate, monthStart, monthEnd)
                .in(EmployeeEntryDO::getWorkStatus, 2, 3)
                .isNotNull(EmployeeEntryDO::getProfileId)
                .orderByAsc(EmployeeEntryDO::getEntryDate)
                .orderByDesc(EmployeeEntryDO::getId)
                .last("LIMIT " + SOURCE_SYNC_LIMIT));
        List<EmployeeEntryDO> leavingEntries = employeeEntryMapper.selectList(new LambdaQueryWrapperX<EmployeeEntryDO>()
                .between(EmployeeEntryDO::getLeaveDate, monthStart.minusDays(15), monthEnd)
                .eq(EmployeeEntryDO::getWorkStatus, 4)
                .isNotNull(EmployeeEntryDO::getProfileId)
                .orderByAsc(EmployeeEntryDO::getLeaveDate)
                .orderByDesc(EmployeeEntryDO::getId)
                .last("LIMIT " + SOURCE_SYNC_LIMIT));
        Set<Long> profileIds = new HashSet<>();
        collectEntryProfileIds(newHireEntries, profileIds);
        collectEntryProfileIds(leavingEntries, profileIds);
        Map<Long, EmployeeProfileDO> profileMap = loadProfileMapSafe(profileIds);
        Map<Long, SocialSecurityAccountDO> socialSecurityMap = loadSocialSecurityMap(profileIds, socialMonth);
        Set<String> openKeys = new HashSet<>();
        int changed = 0;
        for (EmployeeEntryDO entry : emptyIfNull(newHireEntries)) {
            if (entry.getProfileId() == null || isSocialActive(socialSecurityMap.get(entry.getProfileId()))) {
                continue;
            }
            String taskType = TASK_SOCIAL_ADD_PREFIX + socialMonth;
            if (!openKeys.add(todoKey(entry.getProfileId(), taskType))) {
                continue;
            }
            LocalDateTime dueTime = socialChangeDueTime(entry.getEntryDate(), 7);
            EmployeeProfileDO profile = profileMap.get(entry.getProfileId());
            changed += upsertGeneratedTask(BUSINESS_SOCIAL_SECURITY_CHANGE, entry.getProfileId(), taskType,
                    null, entry.getProfileId(), "社保增员待处理：" + profileDisplayName(profile),
                    socialAddContent(entry, profile, socialMonth),
                    socialSecurityRoute(entry.getProfileId(), socialMonth),
                    priorityByDueTime(dueTime), dueTime);
        }
        for (EmployeeEntryDO entry : emptyIfNull(leavingEntries)) {
            if (entry.getProfileId() == null || isSocialStopped(socialSecurityMap.get(entry.getProfileId()))) {
                continue;
            }
            String taskType = TASK_SOCIAL_STOP_PREFIX + socialMonth;
            if (!openKeys.add(todoKey(entry.getProfileId(), taskType))) {
                continue;
            }
            LocalDateTime dueTime = socialChangeDueTime(entry.getLeaveDate(), 3);
            EmployeeProfileDO profile = profileMap.get(entry.getProfileId());
            changed += upsertGeneratedTask(BUSINESS_SOCIAL_SECURITY_CHANGE, entry.getProfileId(), taskType,
                    null, entry.getProfileId(), "社保减员待处理：" + profileDisplayName(profile),
                    socialStopContent(entry, profile, socialMonth),
                    socialSecurityRoute(entry.getProfileId(), socialMonth),
                    priorityByDueTime(dueTime), dueTime);
        }
        if (emptyIfNull(newHireEntries).size() < SOURCE_SYNC_LIMIT && emptyIfNull(leavingEntries).size() < SOURCE_SYNC_LIMIT) {
            changed += closeStaleGeneratedTasksByKeys(BUSINESS_SOCIAL_SECURITY_CHANGE, openKeys);
        }
        return changed;
    }

    private boolean hasSocialSecurityLedger(String socialMonth) {
        if (!StringUtils.hasText(socialMonth)) {
            return false;
        }
        return socialSecurityAccountMapper.selectCount(new LambdaQueryWrapperX<SocialSecurityAccountDO>()
                .eq(SocialSecurityAccountDO::getSocialMonth, socialMonth)) > 0;
    }

    private void collectEntryProfileIds(List<EmployeeEntryDO> entries, Set<Long> profileIds) {
        for (EmployeeEntryDO entry : emptyIfNull(entries)) {
            if (entry.getProfileId() != null) {
                profileIds.add(entry.getProfileId());
            }
        }
    }

    private Map<Long, SocialSecurityAccountDO> loadSocialSecurityMap(Set<Long> profileIds, String socialMonth) {
        Map<Long, SocialSecurityAccountDO> result = new HashMap<>();
        if (profileIds == null || profileIds.isEmpty() || !StringUtils.hasText(socialMonth)) {
            return result;
        }
        List<SocialSecurityAccountDO> accounts = socialSecurityAccountMapper.selectListByMonthAndProfileIds(socialMonth, profileIds);
        for (SocialSecurityAccountDO account : emptyIfNull(accounts)) {
            if (account.getProfileId() != null && !result.containsKey(account.getProfileId())) {
                result.put(account.getProfileId(), account);
            }
        }
        return result;
    }

    private boolean isSocialActive(SocialSecurityAccountDO account) {
        String status = normalizeSocialStatus(account);
        return SOCIAL_ENROLLED.equals(status) || SOCIAL_PENDING_STOP.equals(status);
    }

    private boolean isSocialStopped(SocialSecurityAccountDO account) {
        String status = normalizeSocialStatus(account);
        return SOCIAL_STOPPED.equals(status) || SOCIAL_SUSPENDED.equals(status);
    }

    private String normalizeSocialStatus(SocialSecurityAccountDO account) {
        if (account == null || !StringUtils.hasText(account.getStatus())) {
            return SOCIAL_PENDING_ADD;
        }
        return account.getStatus().trim().toUpperCase();
    }

    private LocalDateTime socialChangeDueTime(LocalDate baseDate, int offsetDays) {
        LocalDate today = LocalDate.now();
        LocalDate dueDate = (baseDate == null ? today : baseDate).plusDays(offsetDays);
        if (dueDate.isBefore(today)) {
            dueDate = today.plusDays(1);
        }
        return LocalDateTime.of(dueDate, LocalTime.of(18, 0));
    }

    private String socialSecurityRoute(Long profileId, String socialMonth) {
        return "/hr/payroll?tab=social&socialProfileId=" + profileId + "&socialMonth=" + socialMonth;
    }

    private String profileDisplayName(EmployeeProfileDO profile) {
        return profile == null ? "未命名员工" : defaultText(profile.getName(), "未命名员工");
    }

    private String socialAddContent(EmployeeEntryDO entry, EmployeeProfileDO profile, String socialMonth) {
        List<String> pieces = new ArrayList<>();
        pieces.add("员工：" + profileDisplayName(profile));
        pieces.add("社保月份：" + socialMonth);
        if (entry.getEntryDate() != null) {
            pieces.add("入职日期：" + entry.getEntryDate());
        }
        pieces.add("请维护社保/公积金台账并确认参保状态");
        return String.join("；", pieces);
    }

    private String socialStopContent(EmployeeEntryDO entry, EmployeeProfileDO profile, String socialMonth) {
        List<String> pieces = new ArrayList<>();
        pieces.add("员工：" + profileDisplayName(profile));
        pieces.add("社保月份：" + socialMonth);
        if (entry.getLeaveDate() != null) {
            pieces.add("离职日期：" + entry.getLeaveDate());
        }
        pieces.add("当前仍为参保状态，请确认停缴/减员");
        return String.join("；", pieces);
    }

    private int syncExamAssignments() {
        LocalDateTime now = LocalDateTime.now();
        List<ExamPublishDO> publishes = examPublishMapper.selectList(new LambdaQueryWrapperX<ExamPublishDO>()
                .eq(ExamPublishDO::getStatus, 1)
                .and(query -> query
                        .and(item -> item.eq(ExamPublishDO::getPublishType, 0)
                                .isNull(ExamPublishDO::getParentPublishId))
                        .or(item -> item.isNotNull(ExamPublishDO::getParentPublishId)))
                .and(query -> query.isNull(ExamPublishDO::getStartAt)
                        .or()
                        .le(ExamPublishDO::getStartAt, now))
                .and(query -> query.isNull(ExamPublishDO::getEndAt)
                        .or()
                        .ge(ExamPublishDO::getEndAt, now))
                .orderByAsc(ExamPublishDO::getEndAt)
                .orderByDesc(ExamPublishDO::getId)
                .last("LIMIT " + SOURCE_SYNC_LIMIT));
        Set<String> openKeys = new HashSet<>();
        Map<Long, ExamDO> examCache = new HashMap<>();
        int changed = 0;
        for (ExamPublishDO publish : publishes) {
            if (publish.getId() == null) {
                continue;
            }
            Set<Long> userIds = examScopeSupport.resolveUserIds(publish.getPublishScopeJson(), publish.getTenantId());
            if (userIds.isEmpty()) {
                continue;
            }
            ExamDO exam = publish.getExamId() == null ? null : examCache.computeIfAbsent(publish.getExamId(), examMapper::selectById);
            for (Long userId : userIds) {
                if (userId == null || hasSubmittedExam(publish.getId(), userId)) {
                    continue;
                }
                String taskType = TASK_ATTEMPT_PREFIX + userId;
                openKeys.add(todoKey(publish.getId(), taskType));
                changed += upsertGeneratedTask(BUSINESS_EXAM_ASSIGNMENT, publish.getId(), taskType,
                        userId, null, examTodoTitle(exam),
                        examTodoContent(publish, exam), "/hr/learning-center?tab=exam&publishId=" + publish.getId(),
                        priorityByDueTime(publish.getEndAt()), publish.getEndAt());
            }
        }
        if (publishes.size() < SOURCE_SYNC_LIMIT) {
            changed += closeStaleGeneratedTasksByKeys(BUSINESS_EXAM_ASSIGNMENT, openKeys);
        }
        return changed;
    }

    private int syncQuestionnaireAssignments() {
        LocalDateTime now = LocalDateTime.now();
        List<QuestionnaireAssignmentDO> assignments = questionnaireAssignmentMapper.selectList(new LambdaQueryWrapperX<QuestionnaireAssignmentDO>()
                .eq(QuestionnaireAssignmentDO::getStatus, 0)
                .and(query -> query.isNull(QuestionnaireAssignmentDO::getBatchStartAt)
                        .or()
                        .le(QuestionnaireAssignmentDO::getBatchStartAt, now))
                .and(query -> query.isNull(QuestionnaireAssignmentDO::getBatchEndAt)
                        .or()
                        .ge(QuestionnaireAssignmentDO::getBatchEndAt, now))
                .orderByAsc(QuestionnaireAssignmentDO::getBatchEndAt)
                .orderByDesc(QuestionnaireAssignmentDO::getId)
                .last("LIMIT " + SOURCE_SYNC_LIMIT));
        Set<Long> openIds = new HashSet<>();
        int changed = 0;
        for (QuestionnaireAssignmentDO assignment : assignments) {
            if (assignment.getId() == null) {
                continue;
            }
            openIds.add(assignment.getId());
            changed += upsertGeneratedTask(BUSINESS_QUESTIONNAIRE_ASSIGNMENT, assignment.getId(), TASK_FILL,
                    assignment.getEvaluatorId(), null, questionnaireTitle(assignment),
                    questionnaireContent(assignment), "/hr/learning-center?tab=questionnaire",
                    priorityByDueTime(assignment.getBatchEndAt()), assignment.getBatchEndAt());
        }
        if (assignments.size() < SOURCE_SYNC_LIMIT) {
            changed += closeStaleGeneratedTasks(BUSINESS_QUESTIONNAIRE_ASSIGNMENT, openIds);
        }
        return changed;
    }

    private int syncDocumentRequests() {
        List<EmployeeDocumentRequestDO> requests = documentRequestMapper.selectOpenList(SOURCE_SYNC_LIMIT);
        Set<Long> openIds = new HashSet<>();
        int changed = 0;
        for (EmployeeDocumentRequestDO request : requests) {
            if (request.getId() == null) {
                continue;
            }
            openIds.add(request.getId());
            LocalDateTime dueTime = request.getExpectedDate() == null
                    ? null : LocalDateTime.of(request.getExpectedDate(), LocalTime.of(18, 0));
            String title = "PROCESSING".equals(request.getStatus()) ? "证明申请办理跟进" : "证明申请办理";
            changed += upsertGeneratedTask(BUSINESS_DOCUMENT_REQUEST, request.getId(), TASK_APPROVE,
                    null, request.getProfileId(), title,
                    documentRequestContent(request), "/hr/document-request?id=" + request.getId(),
                    priorityByDueTime(dueTime), dueTime);
        }
        if (requests.size() < SOURCE_SYNC_LIMIT) {
            changed += closeStaleGeneratedTasks(BUSINESS_DOCUMENT_REQUEST, openIds);
        }
        return changed;
    }

    private int syncEmployeeMaterials() {
        LocalDate today = LocalDate.now();
        List<EmployeeMaterialDO> materials = employeeMaterialMapper.selectExpiringActive(today, today.plusDays(30), SOURCE_SYNC_LIMIT);
        Set<Long> openIds = new HashSet<>();
        int changed = 0;
        for (EmployeeMaterialDO material : materials) {
            if (material.getId() == null) {
                continue;
            }
            openIds.add(material.getId());
            LocalDateTime dueTime = material.getExpireDate() == null
                    ? null : LocalDateTime.of(material.getExpireDate(), LocalTime.of(18, 0));
            changed += upsertGeneratedTask(BUSINESS_EMPLOYEE_MATERIAL, material.getId(), TASK_DEFAULT,
                    null, material.getProfileId(), employeeMaterialTitle(material),
                    employeeMaterialContent(material), "/hr/employee-material?id=" + material.getId(),
                    priorityByDueTime(dueTime), dueTime);
        }
        if (materials.size() < SOURCE_SYNC_LIMIT) {
            changed += closeStaleGeneratedTasks(BUSINESS_EMPLOYEE_MATERIAL, openIds);
        }
        return changed;
    }

    private int syncProfileChanges() {
        List<EmployeeProfileChangeRequestDO> requests = profileChangeRequestMapper.selectPendingList(SOURCE_SYNC_LIMIT);
        Set<Long> openIds = new HashSet<>();
        int changed = 0;
        for (EmployeeProfileChangeRequestDO request : requests) {
            if (request.getId() == null) {
                continue;
            }
            openIds.add(request.getId());
            LocalDateTime dueTime = dateToLocalDateTime(request.getCreateTime());
            if (dueTime != null) {
                dueTime = dueTime.plusDays(2).with(LocalTime.of(18, 0));
            }
            changed += upsertGeneratedTask(BUSINESS_PROFILE_CHANGE, request.getId(), TASK_APPROVE,
                    null, request.getProfileId(), "资料变更审批",
                    profileChangeContent(request), "/hr/profile-change",
                    priorityByDueTime(dueTime), dueTime);
        }
        if (requests.size() < SOURCE_SYNC_LIMIT) {
            changed += closeStaleGeneratedTasks(BUSINESS_PROFILE_CHANGE, openIds);
        }
        return changed;
    }

    private int syncPerformanceFollows() {
        List<EmployeePerformanceDO> performances = employeePerformanceMapper.selectList(new LambdaQueryWrapperX<EmployeePerformanceDO>()
                .orderByDesc(EmployeePerformanceDO::getEvaluatedDate)
                .orderByDesc(EmployeePerformanceDO::getId)
                .last("LIMIT " + SOURCE_SYNC_LIMIT));
        Map<Long, EmployeeProfileDO> profileMap = loadPerformanceProfileMap(performances);
        Set<Long> openIds = new HashSet<>();
        int changed = 0;
        for (EmployeePerformanceDO performance : performances) {
            if (performance.getId() == null || !isPerformanceWarning(performance)) {
                continue;
            }
            openIds.add(performance.getId());
            LocalDateTime dueTime = performanceFollowDueTime(performance);
            changed += upsertGeneratedTask(BUSINESS_PERFORMANCE_FOLLOW, performance.getId(), TASK_FOLLOW,
                    null, performance.getProfileId(), performanceTitle(performance, profileMap),
                    performanceContent(performance), "/hr/performance?id=" + performance.getId(),
                    priorityByDueTime(dueTime), dueTime);
        }
        if (performances.size() < SOURCE_SYNC_LIMIT) {
            changed += closeStaleGeneratedTasks(BUSINESS_PERFORMANCE_FOLLOW, openIds);
        }
        return changed;
    }

    private int syncPerformanceApplications() {
        List<EmployeePerformanceDO> performances = employeePerformanceMapper.selectList(new LambdaQueryWrapperX<EmployeePerformanceDO>()
                .eq(EmployeePerformanceDO::getCycleStatus, "CLOSED")
                .and(query -> query.isNull(EmployeePerformanceDO::getApplicationStatus)
                        .or()
                        .eq(EmployeePerformanceDO::getApplicationStatus, "PENDING"))
                .orderByDesc(EmployeePerformanceDO::getEvaluatedDate)
                .orderByDesc(EmployeePerformanceDO::getId)
                .last("LIMIT " + SOURCE_SYNC_LIMIT));
        Map<Long, EmployeeProfileDO> profileMap = loadPerformanceProfileMap(performances);
        Set<Long> openIds = new HashSet<>();
        int changed = 0;
        for (EmployeePerformanceDO performance : performances) {
            if (performance.getId() == null) {
                continue;
            }
            openIds.add(performance.getId());
            LocalDateTime dueTime = performanceApplicationDueTime(performance);
            changed += upsertGeneratedTask(BUSINESS_PERFORMANCE_APPLICATION, performance.getId(), TASK_FOLLOW,
                    null, performance.getProfileId(), performanceApplicationTitle(performance, profileMap),
                    performanceApplicationContent(performance), "/hr/performance?id=" + performance.getId(),
                    priorityByDueTime(dueTime), dueTime);
        }
        if (performances.size() < SOURCE_SYNC_LIMIT) {
            changed += closeStaleGeneratedTasks(BUSINESS_PERFORMANCE_APPLICATION, openIds);
        }
        return changed;
    }

    private int syncRecruitmentInterviews() {
        List<EmployeeRecruitmentDO> recruitments = employeeRecruitmentMapper.selectList(new LambdaQueryWrapperX<EmployeeRecruitmentDO>()
                .isNotNull(EmployeeRecruitmentDO::getInterviewTime)
                .orderByAsc(EmployeeRecruitmentDO::getInterviewTime)
                .orderByDesc(EmployeeRecruitmentDO::getId)
                .last("LIMIT " + SOURCE_SYNC_LIMIT));
        Map<Long, EmployeeProfileDO> profileMap = loadRecruitmentProfileMap(recruitments);
        Set<Long> openIds = new HashSet<>();
        int changed = 0;
        for (EmployeeRecruitmentDO recruitment : recruitments) {
            if (recruitment.getId() == null || isRecruitmentClosed(recruitment)
                    || hasRecruitmentInterviewEvaluation(recruitment)) {
                continue;
            }
            openIds.add(recruitment.getId());
            changed += upsertGeneratedTask(BUSINESS_RECRUITMENT_INTERVIEW, recruitment.getId(), TASK_INTERVIEW,
                    null, recruitment.getProfileId(), recruitmentTitle("招聘面试安排", recruitment, profileMap),
                    recruitmentContent(recruitment), "/hr/recruitment?id=" + recruitment.getId(),
                    priorityByDueTime(recruitment.getInterviewTime()), recruitment.getInterviewTime());
        }
        if (recruitments.size() < SOURCE_SYNC_LIMIT) {
            changed += closeStaleGeneratedTasks(BUSINESS_RECRUITMENT_INTERVIEW, openIds);
        }
        return changed;
    }

    private int syncRecruitmentTouches() {
        List<EmployeeRecruitmentDO> recruitments = employeeRecruitmentMapper.selectList(new LambdaQueryWrapperX<EmployeeRecruitmentDO>()
                .and(query -> query.isNull(EmployeeRecruitmentDO::getTouchStatus)
                        .or()
                        .eq(EmployeeRecruitmentDO::getTouchStatus, "PENDING")
                        .or()
                        .eq(EmployeeRecruitmentDO::getTouchStatus, "CONTACTED"))
                .orderByAsc(EmployeeRecruitmentDO::getCreateTime)
                .orderByDesc(EmployeeRecruitmentDO::getId)
                .last("LIMIT " + SOURCE_SYNC_LIMIT));
        Map<Long, EmployeeProfileDO> profileMap = loadRecruitmentProfileMap(recruitments);
        Set<Long> openIds = new HashSet<>();
        int changed = 0;
        for (EmployeeRecruitmentDO recruitment : recruitments) {
            if (recruitment.getId() == null || !needsRecruitmentTouch(recruitment) || isRecruitmentClosed(recruitment)) {
                continue;
            }
            openIds.add(recruitment.getId());
            LocalDateTime dueTime = recruitmentTouchDueTime(recruitment);
            changed += upsertGeneratedTask(BUSINESS_RECRUITMENT_TOUCH, recruitment.getId(), TASK_TOUCH,
                    null, recruitment.getProfileId(), recruitmentTitle("招聘触达", recruitment, profileMap),
                    recruitmentTouchContent(recruitment), "/hr/recruitment?id=" + recruitment.getId(),
                    priorityByDueTime(dueTime), dueTime);
        }
        if (recruitments.size() < SOURCE_SYNC_LIMIT) {
            changed += closeStaleGeneratedTasks(BUSINESS_RECRUITMENT_TOUCH, openIds);
        }
        return changed;
    }

    private int syncRecruitmentFollows() {
        List<EmployeeRecruitmentDO> recruitments = employeeRecruitmentMapper.selectList(new LambdaQueryWrapperX<EmployeeRecruitmentDO>()
                .isNotNull(EmployeeRecruitmentDO::getNextFollowTime)
                .orderByAsc(EmployeeRecruitmentDO::getNextFollowTime)
                .orderByDesc(EmployeeRecruitmentDO::getId)
                .last("LIMIT " + SOURCE_SYNC_LIMIT));
        Map<Long, EmployeeProfileDO> profileMap = loadRecruitmentProfileMap(recruitments);
        Set<Long> openIds = new HashSet<>();
        int changed = 0;
        for (EmployeeRecruitmentDO recruitment : recruitments) {
            if (recruitment.getId() == null || isRecruitmentClosed(recruitment)) {
                continue;
            }
            openIds.add(recruitment.getId());
            changed += upsertGeneratedTask(BUSINESS_RECRUITMENT_FOLLOW, recruitment.getId(), TASK_FOLLOW,
                    null, recruitment.getProfileId(), recruitmentTitle("招聘跟进", recruitment, profileMap),
                    recruitmentContent(recruitment), "/hr/recruitment?id=" + recruitment.getId(),
                    priorityByDueTime(recruitment.getNextFollowTime()), recruitment.getNextFollowTime());
        }
        if (recruitments.size() < SOURCE_SYNC_LIMIT) {
            changed += closeStaleGeneratedTasks(BUSINESS_RECRUITMENT_FOLLOW, openIds);
        }
        return changed;
    }

    private int syncTrainingFollows() {
        List<EmployeeTrainingDO> trainings = employeeTrainingMapper.selectList(new LambdaQueryWrapperX<EmployeeTrainingDO>()
                .isNotNull(EmployeeTrainingDO::getEndDate)
                .le(EmployeeTrainingDO::getEndDate, LocalDate.now())
                .orderByAsc(EmployeeTrainingDO::getEndDate)
                .orderByDesc(EmployeeTrainingDO::getId)
                .last("LIMIT " + SOURCE_SYNC_LIMIT));
        Map<Long, EmployeeProfileDO> profileMap = loadTrainingProfileMap(trainings);
        Set<Long> openIds = new HashSet<>();
        int changed = 0;
        for (EmployeeTrainingDO training : trainings) {
            if (training.getId() == null || isTrainingCompleted(training)) {
                continue;
            }
            openIds.add(training.getId());
            LocalDateTime dueTime = trainingFollowDueTime(training);
            changed += upsertGeneratedTask(BUSINESS_TRAINING_FOLLOW, training.getId(), TASK_FOLLOW,
                    null, training.getProfileId(), trainingTitle(training, profileMap),
                    trainingContent(training), "/hr/training?id=" + training.getId(),
                    priorityByDueTime(dueTime), dueTime);
        }
        if (trainings.size() < SOURCE_SYNC_LIMIT) {
            changed += closeStaleGeneratedTasks(BUSINESS_TRAINING_FOLLOW, openIds);
        }
        return changed;
    }

    private int syncTrainingAssignments() {
        List<TrainingAssignmentDO> assignments = trainingAssignmentMapper.selectOpenList(SOURCE_SYNC_LIMIT);
        Map<Long, TrainingPlanDO> planMap = loadTrainingAssignmentPlanMap(assignments);
        Map<Long, TrainingCourseDO> courseMap = loadTrainingAssignmentCourseMap(assignments);
        Map<Long, EmployeeProfileDO> profileMap = loadTrainingAssignmentProfileMap(assignments);
        Set<Long> openIds = new HashSet<>();
        int changed = 0;
        for (TrainingAssignmentDO assignment : assignments) {
            TrainingPlanDO plan = assignment.getPlanId() == null ? null : planMap.get(assignment.getPlanId());
            if (assignment.getId() == null || plan == null || !"PUBLISHED".equals(plan.getStatus())) {
                continue;
            }
            openIds.add(assignment.getId());
            LocalDateTime dueTime = trainingAssignmentDueTime(plan);
            TrainingCourseDO course = assignment.getCourseId() == null ? null : courseMap.get(assignment.getCourseId());
            EmployeeProfileDO profile = assignment.getProfileId() == null ? null : profileMap.get(assignment.getProfileId());
            changed += upsertGeneratedTask(BUSINESS_TRAINING_ASSIGNMENT, assignment.getId(), TASK_FILL,
                    assignment.getUserId(), assignment.getProfileId(), trainingAssignmentTitle(plan, course),
                    trainingAssignmentContent(plan, course, profile, assignment),
                    "/hr/learning-center?trainingAssignmentId=" + assignment.getId(),
                    priorityByDueTime(dueTime), dueTime);
        }
        if (assignments.size() < SOURCE_SYNC_LIMIT) {
            changed += closeStaleGeneratedTasks(BUSINESS_TRAINING_ASSIGNMENT, openIds);
        }
        return changed;
    }

    private int syncRiskEvents() {
        hrRiskWorkbenchService.refreshGeneratedEvents();
        List<HrRiskEventDO> events = hrRiskEventMapper.selectTodoSourceList(SOURCE_SYNC_LIMIT);
        Set<Long> openIds = new HashSet<>();
        int changed = 0;
        for (HrRiskEventDO event : events) {
            if (event.getId() == null) {
                continue;
            }
            openIds.add(event.getId());
            changed += upsertGeneratedTask(BUSINESS_RISK_EVENT, event.getId(), TASK_DEFAULT,
                    event.getOwnerUserId(), event.getProfileId(), riskTitle(event),
                    riskContent(event), "/hr/risk?riskId=" + event.getId(),
                    riskPriority(event), event.getDueTime());
        }
        if (events.size() < SOURCE_SYNC_LIMIT) {
            changed += closeStaleGeneratedTasks(BUSINESS_RISK_EVENT, openIds);
        }
        return changed;
    }

    private int upsertGeneratedTask(String businessType, Long businessId, String taskType,
                                     Long assigneeUserId, Long profileId, String title, String content,
                                     String routePath, String priority, LocalDateTime dueTime) {
        HrTodoTaskDO existing = hrTodoTaskMapper.selectByBusiness(businessType, businessId, taskType);
        if (existing == null) {
            HrTodoTaskDO insertDO = new HrTodoTaskDO();
            insertDO.setAssigneeUserId(assigneeUserId);
            insertDO.setProfileId(profileId);
            insertDO.setBusinessType(businessType);
            insertDO.setBusinessId(businessId);
            insertDO.setTaskType(taskType);
            insertDO.setTitle(title);
            insertDO.setContent(content);
            insertDO.setRoutePath(routePath);
            insertDO.setStatus(STATUS_OPEN);
            insertDO.setPriority(priority);
            insertDO.setDueTime(dueTime);
            insertDO.setGeneratedFlag(true);
            hrTodoTaskMapper.insert(insertDO);
            return 1;
        }
        HrTodoTaskDO updateDO = new HrTodoTaskDO();
        updateDO.setId(existing.getId());
        updateDO.setAssigneeUserId(assigneeUserId);
        updateDO.setProfileId(profileId);
        updateDO.setTitle(title);
        updateDO.setContent(content);
        updateDO.setRoutePath(routePath);
        updateDO.setStatus(STATUS_OPEN);
        updateDO.setPriority(priority);
        updateDO.setDueTime(dueTime);
        updateDO.setGeneratedFlag(true);
        updateDO.setCompletedBy(null);
        updateDO.setCompletedTime(null);
        updateDO.setCancelReason(null);
        hrTodoTaskMapper.updateById(updateDO);
        return 1;
    }

    private int closeStaleGeneratedTasks(String businessType, Set<Long> openIds) {
        List<HrTodoTaskDO> openTasks = hrTodoTaskMapper.selectOpenGeneratedByBusinessType(businessType, 1000);
        int changed = 0;
        for (HrTodoTaskDO task : openTasks) {
            if (task.getBusinessId() != null && openIds.contains(task.getBusinessId())) {
                continue;
            }
            HrTodoTaskDO updateDO = new HrTodoTaskDO();
            updateDO.setId(task.getId());
            updateDO.setStatus(STATUS_DONE);
            updateDO.setCompletedTime(LocalDateTime.now());
            updateDO.setCancelReason("来源业务已处理");
            hrTodoTaskMapper.updateById(updateDO);
            changed++;
        }
        return changed;
    }

    private int closeStaleGeneratedTasksByKeys(String businessType, Set<String> openKeys) {
        List<HrTodoTaskDO> openTasks = hrTodoTaskMapper.selectOpenGeneratedByBusinessType(businessType, 1000);
        int changed = 0;
        for (HrTodoTaskDO task : openTasks) {
            if (openKeys.contains(todoKey(task.getBusinessId(), task.getTaskType()))) {
                continue;
            }
            HrTodoTaskDO updateDO = new HrTodoTaskDO();
            updateDO.setId(task.getId());
            updateDO.setStatus(STATUS_DONE);
            updateDO.setCompletedTime(LocalDateTime.now());
            updateDO.setCancelReason("来源业务已处理");
            hrTodoTaskMapper.updateById(updateDO);
            changed++;
        }
        return changed;
    }

    private String todoKey(Long businessId, String taskType) {
        return String.valueOf(businessId) + ":" + String.valueOf(taskType);
    }

    private void normalizePageReq(HrTodoPageReqVO pageReqVO) {
        if (StringUtils.hasText(pageReqVO.getBusinessType())) {
            pageReqVO.setBusinessType(pageReqVO.getBusinessType().trim().toUpperCase());
        }
        if (StringUtils.hasText(pageReqVO.getStatus())) {
            pageReqVO.setStatus(pageReqVO.getStatus().trim().toUpperCase());
        }
        if (StringUtils.hasText(pageReqVO.getPriority())) {
            pageReqVO.setPriority(pageReqVO.getPriority().trim().toUpperCase());
        }
    }

    private void applyDeptScope(HrTodoPageReqVO pageReqVO) {
        if (pageReqVO == null || pageReqVO.getDeptId() == null || pageReqVO.getDeptId() <= 0) {
            return;
        }
        Set<Long> deptIds = resolveDeptIds(pageReqVO.getDeptId(), Boolean.TRUE.equals(pageReqVO.getIncludeChildren()));
        Set<Long> profileIds = loadDeptProfileIds(deptIds);
        pageReqVO.setProfileIds(profileIds.isEmpty() ? Collections.singletonList(-1L) : new ArrayList<>(profileIds));
    }

    private Set<Long> resolveDeptIds(Long deptId, boolean includeChildren) {
        Set<Long> deptIds = new HashSet<>();
        if (deptId == null || deptId <= 0) {
            return deptIds;
        }
        deptIds.add(deptId);
        if (!includeChildren) {
            return deptIds;
        }
        try {
            List<DeptRespDTO> children = deptApi.getChildDeptList(deptId).getCheckedData();
            if (children != null) {
                children.stream()
                        .map(DeptRespDTO::getId)
                        .filter(Objects::nonNull)
                        .forEach(deptIds::add);
            }
        } catch (Exception ex) {
            log.warn("Load child departments for todo page failed, deptId={}, reason={}", deptId, ex.getMessage());
        }
        return deptIds;
    }

    private Set<Long> loadDeptProfileIds(Set<Long> deptIds) {
        Set<Long> profileIds = new HashSet<>();
        if (deptIds == null || deptIds.isEmpty()) {
            return profileIds;
        }
        List<EmployeeEntryDO> entries = employeeEntryMapper.selectList(new LambdaQueryWrapperX<EmployeeEntryDO>()
                .in(EmployeeEntryDO::getDeptId, deptIds)
                .in(EmployeeEntryDO::getWorkStatus, Arrays.asList(1, 2, 3)));
        if (entries == null || entries.isEmpty()) {
            return profileIds;
        }
        entries.stream()
                .map(EmployeeEntryDO::getProfileId)
                .filter(Objects::nonNull)
                .forEach(profileIds::add);
        return profileIds;
    }

    private Set<Long> resolveSummaryProfileIds(HrTodoSummaryReqVO reqVO) {
        if (reqVO == null) {
            return null;
        }
        boolean hasDept = reqVO.getDeptId() != null && reqVO.getDeptId() > 0;
        boolean hasProfile = reqVO.getProfileId() != null && reqVO.getProfileId() > 0;
        if (!hasDept && !hasProfile) {
            return null;
        }
        Set<Long> profileIds = new HashSet<>();
        if (hasDept) {
            profileIds.addAll(loadDeptProfileIds(resolveDeptIds(reqVO.getDeptId(), Boolean.TRUE.equals(reqVO.getIncludeChildren()))));
        }
        if (hasProfile) {
            if (hasDept && !profileIds.contains(reqVO.getProfileId())) {
                profileIds.clear();
                profileIds.add(-1L);
                return profileIds;
            }
            profileIds.clear();
            profileIds.add(reqVO.getProfileId());
        }
        if (profileIds.isEmpty()) {
            profileIds.add(-1L);
        }
        return profileIds;
    }

    private Long selectSummaryScopeCount(String status, Long assigneeUserId, LocalDateTime dueBefore,
                                         String priority, Boolean generatedFlag, Set<Long> profileIds) {
        return hrTodoTaskMapper.selectCount(new LambdaQueryWrapperX<HrTodoTaskDO>()
                .eqIfPresent(HrTodoTaskDO::getStatus, status)
                .eqIfPresent(HrTodoTaskDO::getAssigneeUserId, assigneeUserId)
                .ltIfPresent(HrTodoTaskDO::getDueTime, dueBefore)
                .eqIfPresent(HrTodoTaskDO::getPriority, priority)
                .eqIfPresent(HrTodoTaskDO::getGeneratedFlag, generatedFlag)
                .inIfPresent(HrTodoTaskDO::getProfileId, profileIds));
    }

    private Long selectSummaryDueSoonCount(Long assigneeUserId, LocalDateTime now, LocalDateTime dueBefore,
                                           Set<Long> profileIds) {
        return hrTodoTaskMapper.selectCount(new LambdaQueryWrapperX<HrTodoTaskDO>()
                .eq(HrTodoTaskDO::getStatus, STATUS_OPEN)
                .eqIfPresent(HrTodoTaskDO::getAssigneeUserId, assigneeUserId)
                .geIfPresent(HrTodoTaskDO::getDueTime, now)
                .leIfPresent(HrTodoTaskDO::getDueTime, dueBefore)
                .inIfPresent(HrTodoTaskDO::getProfileId, profileIds));
    }

    private Long scopeAssigneeUserId(boolean mineOnly) {
        return mineOnly ? (SecurityFrameworkUtils.getLoginUserId() == null ? -1L : SecurityFrameworkUtils.getLoginUserId()) : null;
    }

    private String priorityByDueTime(LocalDateTime dueTime) {
        if (dueTime == null) {
            return PRIORITY_LOW;
        }
        LocalDateTime now = LocalDateTime.now();
        if (dueTime.isBefore(now)) {
            return PRIORITY_HIGH;
        }
        if (dueTime.isBefore(now.plusDays(3))) {
            return PRIORITY_MEDIUM;
        }
        return PRIORITY_LOW;
    }

    private String lifecycleContent(HrLifecycleTaskDO task) {
        List<String> pieces = new ArrayList<>();
        if (StringUtils.hasText(task.getEventType())) {
            pieces.add("事件：" + task.getEventType());
        }
        if (StringUtils.hasText(task.getAssigneeName())) {
            pieces.add("负责人：" + task.getAssigneeName());
        }
        if (task.getDueDate() != null) {
            pieces.add("截止：" + task.getDueDate());
        }
        return String.join("，", pieces);
    }

    private String correctionTitle(AttendanceCorrectionDO correction) {
        if ("FIELD".equals(correction.getApplyType())) {
            return "外勤打卡审批";
        }
        return "补卡申请审批";
    }

    private String correctionContent(AttendanceCorrectionDO correction) {
        List<String> pieces = new ArrayList<>();
        if (correction.getAttendanceDate() != null) {
            pieces.add("日期：" + correction.getAttendanceDate());
        }
        if (StringUtils.hasText(correction.getClockType())) {
            pieces.add("类型：" + correction.getClockType());
        }
        if (StringUtils.hasText(correction.getReason())) {
            pieces.add("原因：" + correction.getReason());
        }
        return String.join("，", pieces);
    }

    private String overtimeContent(AttendanceOvertimeDO overtime) {
        List<String> pieces = new ArrayList<>();
        if (overtime.getOvertimeDate() != null) {
            pieces.add("日期：" + overtime.getOvertimeDate());
        }
        if (overtime.getDurationHours() != null) {
            pieces.add("时长：" + overtime.getDurationHours() + "小时");
        }
        if (StringUtils.hasText(overtime.getReason())) {
            pieces.add("原因：" + overtime.getReason());
        }
        return String.join("，", pieces);
    }

    private LocalDateTime monthlyConfirmDueTime(AttendanceMonthlyConfirmDO confirm) {
        if (confirm == null || !StringUtils.hasText(confirm.getSettlementMonth())) {
            return null;
        }
        try {
            LocalDate month = LocalDate.parse(confirm.getSettlementMonth() + "-01");
            return LocalDateTime.of(month.plusMonths(1).withDayOfMonth(5), LocalTime.of(18, 0));
        } catch (Exception ex) {
            return null;
        }
    }

    private String monthlyConfirmContent(AttendanceMonthlyConfirmDO confirm) {
        List<String> pieces = new ArrayList<>();
        if (StringUtils.hasText(confirm.getSettlementMonth())) {
            pieces.add("月份：" + confirm.getSettlementMonth());
        }
        if (StringUtils.hasText(confirm.getStatus())) {
            pieces.add("状态：" + confirm.getStatus());
        }
        if (StringUtils.hasText(confirm.getIssueRemark())) {
            pieces.add("异议：" + confirm.getIssueRemark());
        }
        return String.join("；", pieces);
    }

    private String exceptionContent(AttendanceExceptionDO exception) {
        List<String> pieces = new ArrayList<>();
        if (exception.getAttendanceDate() != null) {
            pieces.add("日期：" + exception.getAttendanceDate());
        }
        if (StringUtils.hasText(exception.getExceptionType())) {
            pieces.add("类型：" + exception.getExceptionType());
        }
        if (StringUtils.hasText(exception.getReason())) {
            pieces.add("原因：" + exception.getReason());
        }
        return String.join("，", pieces);
    }

    private LocalDateTime payslipDueTime(PayslipDO slip) {
        if (slip == null || !StringUtils.hasText(slip.getPayrollMonth())) {
            return null;
        }
        try {
            LocalDate month = LocalDate.parse(slip.getPayrollMonth() + "-01");
            return LocalDateTime.of(month.plusMonths(1).withDayOfMonth(10), LocalTime.of(18, 0));
        } catch (Exception ex) {
            return null;
        }
    }

    private String payslipContent(PayslipDO slip) {
        List<String> pieces = new ArrayList<>();
        if (StringUtils.hasText(slip.getPayrollMonth())) {
            pieces.add("月份：" + slip.getPayrollMonth());
        }
        if (slip.getNetSalary() != null) {
            pieces.add("实发：" + slip.getNetSalary());
        }
        if (StringUtils.hasText(slip.getIssueRemark())) {
            pieces.add("异议：" + slip.getIssueRemark());
        }
        return String.join("；", pieces);
    }

    private boolean hasSubmittedExam(Long publishId, Long userId) {
        Long count = examAttemptMapper.selectCount(new LambdaQueryWrapperX<ExamAttemptDO>()
                .eq(ExamAttemptDO::getPublishId, publishId)
                .eq(ExamAttemptDO::getUserId, userId)
                .eq(ExamAttemptDO::getStatus, 1));
        return count != null && count > 0;
    }

    private String examTodoTitle(ExamDO exam) {
        String examName = exam == null ? null : exam.getName();
        return "考试待完成：" + defaultText(examName, "未命名考试");
    }

    private String examTodoContent(ExamPublishDO publish, ExamDO exam) {
        List<String> pieces = new ArrayList<>();
        if (StringUtils.hasText(publish.getBatchLabel())) {
            pieces.add("批次：" + publish.getBatchLabel());
        }
        if (publish.getEndAt() != null) {
            pieces.add("截止：" + publish.getEndAt());
        }
        Integer durationMin = publish.getDurationMin();
        if (durationMin == null && exam != null) {
            durationMin = exam.getDurationMin();
        }
        if (durationMin != null) {
            pieces.add("时长：" + durationMin + "分钟");
        }
        return String.join("；", pieces);
    }

    private String questionnaireTitle(QuestionnaireAssignmentDO assignment) {
        if (StringUtils.hasText(assignment.getBatchLabel())) {
            return "问卷待填写：" + assignment.getBatchLabel();
        }
        return "问卷待填写";
    }

    private String questionnaireContent(QuestionnaireAssignmentDO assignment) {
        List<String> pieces = new ArrayList<>();
        if (StringUtils.hasText(assignment.getTargetName())) {
            pieces.add("对象：" + assignment.getTargetName());
        }
        if (assignment.getBatchEndAt() != null) {
            pieces.add("截止：" + assignment.getBatchEndAt());
        }
        return String.join("，", pieces);
    }

    private String documentRequestContent(EmployeeDocumentRequestDO request) {
        List<String> pieces = new ArrayList<>();
        if (StringUtils.hasText(request.getTitle())) {
            pieces.add("事项：" + request.getTitle());
        }
        if (StringUtils.hasText(request.getPurpose())) {
            pieces.add("用途：" + request.getPurpose());
        }
        if (request.getExpectedDate() != null) {
            pieces.add("期望：" + request.getExpectedDate());
        }
        return String.join("，", pieces);
    }

    private String employeeMaterialTitle(EmployeeMaterialDO material) {
        return "员工材料到期：" + defaultText(material.getMaterialName(), "电子材料");
    }

    private String employeeMaterialContent(EmployeeMaterialDO material) {
        List<String> pieces = new ArrayList<>();
        if (StringUtils.hasText(material.getCategory())) {
            pieces.add("分类：" + material.getCategory());
        }
        if (StringUtils.hasText(material.getMaterialType())) {
            pieces.add("类型：" + material.getMaterialType());
        }
        if (material.getExpireDate() != null) {
            pieces.add("到期：" + material.getExpireDate());
        }
        return String.join("，", pieces);
    }

    private String profileChangeContent(EmployeeProfileChangeRequestDO request) {
        List<String> pieces = new ArrayList<>();
        if (StringUtils.hasText(request.getChangeSummary())) {
            pieces.add("字段：" + request.getChangeSummary());
        }
        if (StringUtils.hasText(request.getReason())) {
            pieces.add("原因：" + request.getReason());
        }
        return String.join("，", pieces);
    }

    private Map<Long, EmployeeProfileDO> loadPerformanceProfileMap(List<EmployeePerformanceDO> performances) {
        Set<Long> profileIds = new HashSet<>();
        if (performances != null) {
            for (EmployeePerformanceDO performance : performances) {
                if (performance != null && performance.getProfileId() != null) {
                    profileIds.add(performance.getProfileId());
                }
            }
        }
        return loadProfileMapSafe(profileIds);
    }

    private String performanceTitle(EmployeePerformanceDO performance, Map<Long, EmployeeProfileDO> profileMap) {
        EmployeeProfileDO profile = performance.getProfileId() == null ? null : profileMap.get(performance.getProfileId());
        String name = profile == null ? null : profile.getName();
        return "绩效改进跟进：" + defaultText(name, defaultText(performance.getPeriod(), "未命名周期"));
    }

    private String performanceContent(EmployeePerformanceDO performance) {
        List<String> pieces = new ArrayList<>();
        if (StringUtils.hasText(performance.getPeriod())) {
            pieces.add("周期：" + performance.getPeriod());
        }
        if (performance.getScore() != null) {
            pieces.add("得分：" + performance.getScore());
        }
        if (StringUtils.hasText(performance.getGrade())) {
            pieces.add("等级：" + performance.getGrade());
        }
        if (StringUtils.hasText(performance.getResult())) {
            pieces.add("结果：" + performance.getResult());
        }
        if (StringUtils.hasText(performance.getRemark())) {
            pieces.add("备注：" + performance.getRemark());
        }
        return String.join("，", pieces);
    }

    private String performanceApplicationTitle(EmployeePerformanceDO performance, Map<Long, EmployeeProfileDO> profileMap) {
        EmployeeProfileDO profile = performance.getProfileId() == null ? null : profileMap.get(performance.getProfileId());
        String name = profile == null ? null : profile.getName();
        return "绩效结果应用：" + defaultText(name, defaultText(performance.getPeriod(), "未命名周期"));
    }

    private String performanceApplicationContent(EmployeePerformanceDO performance) {
        List<String> pieces = new ArrayList<>();
        if (StringUtils.hasText(performance.getPeriod())) {
            pieces.add("周期：" + performance.getPeriod());
        }
        if (performance.getScore() != null) {
            pieces.add("得分：" + performance.getScore());
        }
        if (StringUtils.hasText(performance.getGrade())) {
            pieces.add("等级：" + performance.getGrade());
        }
        if (StringUtils.hasText(performance.getResult())) {
            pieces.add("结果：" + performance.getResult());
        }
        if (StringUtils.hasText(performance.getApplicationType())) {
            pieces.add("应用类型：" + performance.getApplicationType());
        }
        if (StringUtils.hasText(performance.getApplicationRemark())) {
            pieces.add("备注：" + performance.getApplicationRemark());
        }
        return String.join("，", pieces);
    }

    private LocalDateTime performanceApplicationDueTime(EmployeePerformanceDO performance) {
        LocalDate baseDate = performance.getEvaluatedDate() == null ? LocalDate.now() : performance.getEvaluatedDate();
        return LocalDateTime.of(baseDate.plusDays(3), LocalTime.of(18, 0));
    }

    private LocalDateTime performanceFollowDueTime(EmployeePerformanceDO performance) {
        LocalDate baseDate = performance.getEvaluatedDate() == null ? LocalDate.now() : performance.getEvaluatedDate();
        return LocalDateTime.of(baseDate.plusDays(7), LocalTime.of(18, 0));
    }

    private boolean isPerformanceWarning(EmployeePerformanceDO performance) {
        return performance.getScore() != null && performance.getScore().compareTo(BigDecimal.valueOf(60)) < 0
                || containsText(performance.getGrade(), "C")
                || containsText(performance.getGrade(), "D")
                || containsText(performance.getGrade(), "待改进")
                || containsText(performance.getGrade(), "不合格")
                || containsText(performance.getResult(), "待改进")
                || containsText(performance.getResult(), "不合格")
                || containsText(performance.getResult(), "较差");
    }

    private Map<Long, EmployeeProfileDO> loadRecruitmentProfileMap(List<EmployeeRecruitmentDO> recruitments) {
        Set<Long> profileIds = new HashSet<>();
        if (recruitments != null) {
            for (EmployeeRecruitmentDO recruitment : recruitments) {
                if (recruitment != null && recruitment.getProfileId() != null) {
                    profileIds.add(recruitment.getProfileId());
                }
            }
        }
        return loadProfileMapSafe(profileIds);
    }

    private String recruitmentTitle(String prefix, EmployeeRecruitmentDO recruitment, Map<Long, EmployeeProfileDO> profileMap) {
        String candidateName = recruitmentProfileName(recruitment, profileMap);
        if (StringUtils.hasText(candidateName)) {
            return prefix + "：" + candidateName;
        }
        if (StringUtils.hasText(recruitment.getPosition())) {
            return prefix + "：" + recruitment.getPosition();
        }
        return prefix;
    }

    private String recruitmentContent(EmployeeRecruitmentDO recruitment) {
        List<String> pieces = new ArrayList<>();
        if (StringUtils.hasText(recruitment.getPosition())) {
            pieces.add("岗位：" + recruitment.getPosition());
        }
        if (StringUtils.hasText(recruitment.getCandidateStage())) {
            pieces.add("阶段：" + recruitment.getCandidateStage());
        }
        if (StringUtils.hasText(recruitment.getRecruiter())) {
            pieces.add("负责人：" + recruitment.getRecruiter());
        }
        if (StringUtils.hasText(recruitment.getCampaignName())) {
            pieces.add("活动：" + recruitment.getCampaignName());
        }
        if (StringUtils.hasText(recruitment.getReferrerName())) {
            pieces.add("内推：" + recruitment.getReferrerName());
        }
        if (StringUtils.hasText(recruitment.getDemandDeptName())) {
            pieces.add("用人部门：" + recruitment.getDemandDeptName());
        }
        if (recruitment.getDemandHeadcount() != null) {
            pieces.add("HC：" + recruitment.getDemandHeadcount());
        }
        if (recruitment.getInterviewTime() != null) {
            pieces.add("面试：" + recruitment.getInterviewTime());
        }
        if (StringUtils.hasText(recruitment.getInterviewDecision())) {
            pieces.add("面试结论：" + recruitment.getInterviewDecision());
        }
        if (StringUtils.hasText(recruitment.getTouchStatus())) {
            pieces.add("触达：" + recruitment.getTouchStatus());
        }
        if (recruitment.getNextFollowTime() != null) {
            pieces.add("下次跟进：" + recruitment.getNextFollowTime());
        }
        if (StringUtils.hasText(recruitment.getOfferStatus())) {
            pieces.add("Offer：" + recruitment.getOfferStatus());
        }
        return String.join("，", pieces);
    }

    private String recruitmentTouchContent(EmployeeRecruitmentDO recruitment) {
        List<String> pieces = new ArrayList<>();
        String baseContent = recruitmentContent(recruitment);
        if (StringUtils.hasText(baseContent)) {
            pieces.add(baseContent);
        }
        if (recruitment.getLastContactTime() != null) {
            pieces.add("最近联系：" + recruitment.getLastContactTime());
        }
        if (recruitment.getTouchTime() != null) {
            pieces.add("最近触达：" + recruitment.getTouchTime());
        }
        if (StringUtils.hasText(recruitment.getTouchRemark())) {
            pieces.add("触达备注：" + recruitment.getTouchRemark());
        }
        if (!StringUtils.hasText(recruitment.getTouchStatus())) {
            pieces.add("系统判断：尚未触达，请优先联系候选人");
        } else if ("PENDING".equalsIgnoreCase(recruitment.getTouchStatus())) {
            pieces.add("系统判断：待触达，请完成首次联系");
        } else {
            pieces.add("系统判断：已触达但未设置下次跟进时间，请补齐跟进计划或记录回复");
        }
        return String.join("；", pieces);
    }

    private boolean needsRecruitmentTouch(EmployeeRecruitmentDO recruitment) {
        if (recruitment.getNextFollowTime() != null
                || recruitment.getInterviewTime() != null
                || recruitment.getOfferDate() != null
                || recruitment.getEntryDate() != null
                || recruitmentContains(recruitment, "面试")
                || recruitmentContains(recruitment, "Offer")
                || recruitmentContains(recruitment, "OFFER")) {
            return false;
        }
        String touchStatus = recruitment.getTouchStatus();
        return !StringUtils.hasText(touchStatus)
                || "PENDING".equalsIgnoreCase(touchStatus)
                || "CONTACTED".equalsIgnoreCase(touchStatus);
    }

    private LocalDateTime recruitmentTouchDueTime(EmployeeRecruitmentDO recruitment) {
        LocalDateTime baseTime = recruitment.getLastContactTime();
        if (baseTime == null) {
            baseTime = recruitment.getTouchTime();
        }
        if (baseTime == null) {
            baseTime = dateToLocalDateTime(recruitment.getCreateTime());
        }
        if (baseTime == null) {
            baseTime = dateToLocalDateTime(recruitment.getUpdateTime());
        }
        if (baseTime == null) {
            baseTime = LocalDateTime.now();
        }
        int days = "CONTACTED".equalsIgnoreCase(recruitment.getTouchStatus()) ? 3 : 1;
        return baseTime.plusDays(days);
    }

    private String recruitmentProfileName(EmployeeRecruitmentDO recruitment, Map<Long, EmployeeProfileDO> profileMap) {
        if (recruitment == null || recruitment.getProfileId() == null || profileMap == null) {
            return null;
        }
        EmployeeProfileDO profile = profileMap.get(recruitment.getProfileId());
        return profile == null ? null : profile.getName();
    }

    private Map<Long, EmployeeProfileDO> loadTrainingProfileMap(List<EmployeeTrainingDO> trainings) {
        Set<Long> profileIds = new HashSet<>();
        if (trainings != null) {
            for (EmployeeTrainingDO training : trainings) {
                if (training != null && training.getProfileId() != null) {
                    profileIds.add(training.getProfileId());
                }
            }
        }
        return loadProfileMapSafe(profileIds);
    }

    private String trainingTitle(EmployeeTrainingDO training, Map<Long, EmployeeProfileDO> profileMap) {
        EmployeeProfileDO profile = training.getProfileId() == null ? null : profileMap.get(training.getProfileId());
        String profileName = profile == null ? null : profile.getName();
        String name = defaultText(training.getTrainingName(), "未命名培训");
        if (StringUtils.hasText(profileName)) {
            return "培训结果跟进：" + profileName + " - " + name;
        }
        return "培训结果跟进：" + name;
    }

    private String trainingContent(EmployeeTrainingDO training) {
        List<String> pieces = new ArrayList<>();
        if (StringUtils.hasText(training.getTrainingName())) {
            pieces.add("培训：" + training.getTrainingName());
        }
        if (StringUtils.hasText(training.getProvider())) {
            pieces.add("机构：" + training.getProvider());
        }
        if (training.getEndDate() != null) {
            pieces.add("结束：" + training.getEndDate());
        }
        if (training.getHours() != null) {
            pieces.add("课时：" + training.getHours() + "小时");
        }
        if (StringUtils.hasText(training.getResult())) {
            pieces.add("结果：" + training.getResult());
        } else {
            pieces.add("结果：未填写");
        }
        return String.join("；", pieces);
    }

    private LocalDateTime trainingFollowDueTime(EmployeeTrainingDO training) {
        if (training.getEndDate() == null) {
            return null;
        }
        return LocalDateTime.of(training.getEndDate(), LocalTime.of(18, 0));
    }

    private boolean isTrainingCompleted(EmployeeTrainingDO training) {
        if (!StringUtils.hasText(training.getResult())) {
            return false;
        }
        if (isTrainingNegativeResult(training.getResult())) {
            return false;
        }
        return containsTrainingText(training.getResult(), "通过", "完成", "已完成", "合格", "PASS", "Pass", "pass");
    }

    private boolean isTrainingNegativeResult(String result) {
        return containsTrainingText(result, "未通过", "不合格", "未完成", "待完成", "待补", "FAIL", "Fail", "fail");
    }

    private boolean containsTrainingText(String value, String... keywords) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private Map<Long, TrainingPlanDO> loadTrainingAssignmentPlanMap(List<TrainingAssignmentDO> assignments) {
        Set<Long> planIds = new HashSet<>();
        if (assignments != null) {
            for (TrainingAssignmentDO assignment : assignments) {
                if (assignment != null && assignment.getPlanId() != null) {
                    planIds.add(assignment.getPlanId());
                }
            }
        }
        Map<Long, TrainingPlanDO> planMap = new HashMap<>();
        if (planIds.isEmpty()) {
            return planMap;
        }
        for (TrainingPlanDO plan : trainingPlanMapper.selectBatchIds(planIds)) {
            if (plan.getId() != null) {
                planMap.put(plan.getId(), plan);
            }
        }
        return planMap;
    }

    private Map<Long, TrainingCourseDO> loadTrainingAssignmentCourseMap(List<TrainingAssignmentDO> assignments) {
        Set<Long> courseIds = new HashSet<>();
        if (assignments != null) {
            for (TrainingAssignmentDO assignment : assignments) {
                if (assignment != null && assignment.getCourseId() != null) {
                    courseIds.add(assignment.getCourseId());
                }
            }
        }
        Map<Long, TrainingCourseDO> courseMap = new HashMap<>();
        if (courseIds.isEmpty()) {
            return courseMap;
        }
        for (TrainingCourseDO course : trainingCourseMapper.selectBatchIds(courseIds)) {
            if (course.getId() != null) {
                courseMap.put(course.getId(), course);
            }
        }
        return courseMap;
    }

    private Map<Long, EmployeeProfileDO> loadTrainingAssignmentProfileMap(List<TrainingAssignmentDO> assignments) {
        Set<Long> profileIds = new HashSet<>();
        if (assignments != null) {
            for (TrainingAssignmentDO assignment : assignments) {
                if (assignment != null && assignment.getProfileId() != null) {
                    profileIds.add(assignment.getProfileId());
                }
            }
        }
        return loadProfileMapSafe(profileIds);
    }

    private LocalDateTime trainingAssignmentDueTime(TrainingPlanDO plan) {
        if (plan == null || plan.getEndDate() == null) {
            return null;
        }
        return LocalDateTime.of(plan.getEndDate(), LocalTime.of(18, 0));
    }

    private String trainingAssignmentTitle(TrainingPlanDO plan, TrainingCourseDO course) {
        String courseName = course == null ? null : course.getCourseName();
        if (StringUtils.hasText(courseName)) {
            return "学习任务：" + courseName;
        }
        return "学习任务：" + defaultText(plan.getPlanName(), "未命名计划");
    }

    private String trainingAssignmentContent(TrainingPlanDO plan, TrainingCourseDO course,
                                             EmployeeProfileDO profile, TrainingAssignmentDO assignment) {
        List<String> pieces = new ArrayList<>();
        if (StringUtils.hasText(plan.getPlanName())) {
            pieces.add("计划：" + plan.getPlanName());
        }
        if (course != null && StringUtils.hasText(course.getCourseName())) {
            pieces.add("课程：" + course.getCourseName());
        }
        if (profile != null && StringUtils.hasText(profile.getName())) {
            pieces.add("员工：" + profile.getName());
        }
        if (plan.getEndDate() != null) {
            pieces.add("截止：" + plan.getEndDate());
        }
        if (assignment.getProgress() != null) {
            pieces.add("进度：" + assignment.getProgress() + "%");
        }
        return String.join("；", pieces);
    }

    private String riskTitle(HrRiskEventDO event) {
        return "风险处理：" + defaultText(event.getTitle(), "未命名风险");
    }

    private String riskContent(HrRiskEventDO event) {
        List<String> pieces = new ArrayList<>();
        if (StringUtils.hasText(event.getSeverity())) {
            pieces.add("等级：" + event.getSeverity());
        }
        if (StringUtils.hasText(event.getDescription())) {
            pieces.add(event.getDescription());
        }
        if (StringUtils.hasText(event.getAction())) {
            pieces.add("建议：" + event.getAction());
        }
        if (event.getDueTime() != null) {
            pieces.add("截止：" + event.getDueTime());
        }
        return String.join("；", pieces);
    }

    private String riskPriority(HrRiskEventDO event) {
        if (PRIORITY_HIGH.equals(event.getSeverity())) {
            return PRIORITY_HIGH;
        }
        if (PRIORITY_MEDIUM.equals(event.getSeverity())) {
            return PRIORITY_MEDIUM;
        }
        return PRIORITY_LOW;
    }

    private boolean isRecruitmentClosed(EmployeeRecruitmentDO recruitment) {
        if (recruitment.getEntryDate() != null) {
            return true;
        }
        return recruitmentContains(recruitment, "已入职")
                || recruitmentContains(recruitment, "入职完成")
                || recruitmentContains(recruitment, "已淘汰")
                || recruitmentContains(recruitment, "淘汰")
                || recruitmentContains(recruitment, "流失")
                || recruitmentContains(recruitment, "关闭")
                || recruitmentContains(recruitment, "拒绝")
                || recruitmentContains(recruitment, "REJECT")
                || recruitmentContains(recruitment, "CLOSED");
    }

    private boolean hasRecruitmentInterviewEvaluation(EmployeeRecruitmentDO recruitment) {
        return StringUtils.hasText(recruitment.getInterviewResult())
                || StringUtils.hasText(recruitment.getInterviewDecision())
                || StringUtils.hasText(recruitment.getInterviewFeedback())
                || recruitment.getInterviewScore() != null
                || recruitment.getInterviewEvaluationTime() != null;
    }

    private boolean recruitmentContains(EmployeeRecruitmentDO recruitment, String keyword) {
        return containsText(recruitment.getStatus(), keyword)
                || containsText(recruitment.getCandidateStage(), keyword)
                || containsText(recruitment.getDemandStatus(), keyword)
                || containsText(recruitment.getInterviewDecision(), keyword)
                || containsText(recruitment.getInterviewFeedback(), keyword)
                || containsText(recruitment.getOfferStatus(), keyword)
                || containsText(recruitment.getLossReason(), keyword);
    }

    private boolean containsText(String value, String keyword) {
        return value != null && value.contains(keyword);
    }

    private LocalDateTime dateToLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private int count(Long value) {
        return value == null ? 0 : value.intValue();
    }

    private <T> List<T> emptyIfNull(List<T> list) {
        return list == null ? new ArrayList<>() : list;
    }

    private boolean canManage() {
        try {
            return securityFrameworkService.hasPermission(PERMISSION_TODO_QUERY);
        } catch (Exception ex) {
            log.warn("check hr todo manage permission failed: {}", ex.getMessage());
            return false;
        }
    }

    private void fillPeopleInfo(List<HrTodoTaskDO> rows, List<HrTodoRespVO> respList) {
        Set<Long> userIds = new HashSet<>();
        Set<Long> profileIds = new HashSet<>();
        for (HrTodoTaskDO row : rows) {
            if (row.getAssigneeUserId() != null) {
                userIds.add(row.getAssigneeUserId());
            }
            if (row.getCompletedBy() != null) {
                userIds.add(row.getCompletedBy());
            }
            if (row.getProfileId() != null) {
                profileIds.add(row.getProfileId());
            }
        }
        Map<Long, AdminUserRespDTO> userMap = loadUserMapSafe(userIds);
        Map<Long, EmployeeProfileDO> profileMap = loadProfileMapSafe(profileIds);
        for (HrTodoRespVO item : respList) {
            AdminUserRespDTO assignee = userMap.get(item.getAssigneeUserId());
            if (assignee != null) {
                item.setAssigneeName(StringUtils.hasText(assignee.getNickname()) ? assignee.getNickname() : assignee.getUsername());
            }
            AdminUserRespDTO completedBy = userMap.get(item.getCompletedBy());
            if (completedBy != null) {
                item.setCompletedByName(StringUtils.hasText(completedBy.getNickname()) ? completedBy.getNickname() : completedBy.getUsername());
            }
            EmployeeProfileDO profile = profileMap.get(item.getProfileId());
            if (profile != null) {
                item.setProfileName(profile.getName());
            }
        }
    }

    private Map<Long, AdminUserRespDTO> loadUserMapSafe(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return adminUserApi.getUserMap(userIds);
        } catch (Exception ex) {
            log.warn("Failed to load admin users for hr todo page: {}", ex.getMessage());
            return new HashMap<>();
        }
    }

    private Map<Long, EmployeeProfileDO> loadProfileMapSafe(Set<Long> profileIds) {
        Map<Long, EmployeeProfileDO> profileMap = new HashMap<>();
        if (profileIds == null || profileIds.isEmpty()) {
            return profileMap;
        }
        List<EmployeeProfileDO> profiles = employeeProfileMapper.selectList(
                new LambdaQueryWrapperX<EmployeeProfileDO>().in(EmployeeProfileDO::getId, profileIds));
        if (profiles == null) {
            return profileMap;
        }
        for (EmployeeProfileDO profile : profiles) {
            if (profile.getId() != null) {
                profileMap.put(profile.getId(), profile);
            }
        }
        return profileMap;
    }

}
