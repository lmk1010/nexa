package com.kyx.service.hr.service.manager;

import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.foundation.security.core.util.SecurityFrameworkUtils;
import com.kyx.service.business.api.dept.DeptApi;
import com.kyx.service.business.api.dept.dto.DeptRespDTO;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import com.kyx.service.hr.controller.admin.manager.vo.HrManagerSelfServiceRespVO;
import com.kyx.service.hr.controller.admin.manager.vo.HrManagerTeamExportRespVO;
import com.kyx.service.hr.dal.dataobject.administrative.HrAdministrativeLeaveDO;
import com.kyx.service.hr.dal.dataobject.administrative.HrAdministrativeTripDO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceCorrectionDO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceDailyResultDO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceExceptionDO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceOvertimeDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeEntryDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeMaterialDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeePerformanceDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeProfileDO;
import com.kyx.service.hr.dal.dataobject.todo.HrTodoTaskDO;
import com.kyx.service.hr.dal.dataobject.training.TrainingAssignmentDO;
import com.kyx.service.hr.dal.mysql.administrative.HrAdministrativeLeaveMapper;
import com.kyx.service.hr.dal.mysql.administrative.HrAdministrativeTripMapper;
import com.kyx.service.hr.dal.mysql.attendance.AttendanceCorrectionMapper;
import com.kyx.service.hr.dal.mysql.attendance.AttendanceDailyResultMapper;
import com.kyx.service.hr.dal.mysql.attendance.AttendanceExceptionMapper;
import com.kyx.service.hr.dal.mysql.attendance.AttendanceOvertimeMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeEntryMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeMaterialMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeePerformanceMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeProfileMapper;
import com.kyx.service.hr.dal.mysql.todo.HrTodoTaskMapper;
import com.kyx.service.hr.dal.mysql.training.TrainingAssignmentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Validated
@Slf4j
public class HrManagerSelfServiceServiceImpl implements HrManagerSelfServiceService {

    private static final String TODO_STATUS_OPEN = "OPEN";
    private static final String STATUS_PENDING = "PENDING";
    private static final int ADMIN_STATUS_RUNNING = 1;
    private static final String MATERIAL_STATUS_ACTIVE = "ACTIVE";
    private static final String TRAINING_NOT_STARTED = "NOT_STARTED";
    private static final String TRAINING_IN_PROGRESS = "IN_PROGRESS";
    private static final String TRAINING_COMPLETED = "COMPLETED";
    private static final String RESULT_NORMAL = "NORMAL";
    private static final String RESULT_LATE = "LATE";
    private static final String RESULT_EARLY = "EARLY";
    private static final String RESULT_LATE_EARLY = "LATE_EARLY";
    private static final String RESULT_MISSING_IN = "MISSING_IN";
    private static final String RESULT_MISSING_OUT = "MISSING_OUT";
    private static final String RESULT_MISSING_BOTH = "MISSING_BOTH";
    private static final String RESULT_ABSENTEEISM = "ABSENTEEISM";
    private static final int TEAM_LIMIT = 500;
    private static final int TODO_LIMIT = 1000;
    private static final int SIGNAL_LIMIT = 20000;

    @Resource
    private EmployeeProfileMapper employeeProfileMapper;
    @Resource
    private EmployeeEntryMapper employeeEntryMapper;
    @Resource
    private EmployeePerformanceMapper employeePerformanceMapper;
    @Resource
    private HrTodoTaskMapper hrTodoTaskMapper;
    @Resource
    private AttendanceDailyResultMapper attendanceDailyResultMapper;
    @Resource
    private AttendanceExceptionMapper attendanceExceptionMapper;
    @Resource
    private AttendanceCorrectionMapper attendanceCorrectionMapper;
    @Resource
    private AttendanceOvertimeMapper attendanceOvertimeMapper;
    @Resource
    private HrAdministrativeLeaveMapper hrAdministrativeLeaveMapper;
    @Resource
    private HrAdministrativeTripMapper hrAdministrativeTripMapper;
    @Resource
    private EmployeeMaterialMapper employeeMaterialMapper;
    @Resource
    private TrainingAssignmentMapper trainingAssignmentMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;

    @Override
    public HrManagerSelfServiceRespVO getHome() {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        HrManagerSelfServiceRespVO respVO = emptyHome();
        if (loginUserId == null) {
            return respVO;
        }

        EmployeeProfileDO managerProfile = employeeProfileMapper.selectByUserId(loginUserId);
        if (managerProfile == null) {
            return respVO;
        }

        EmployeeEntryDO managerEntry = selectPrimaryEntry(managerProfile.getId(), loginUserId);
        Map<Long, EmployeeEntryDO> teamEntryMap = new LinkedHashMap<>();
        for (EmployeeEntryDO entry : selectDirectEntries(managerProfile.getId())) {
            putBetterEntry(teamEntryMap, entry);
        }
        for (EmployeeEntryDO entry : selectSubordinateEntries(loginUserId)) {
            putBetterEntry(teamEntryMap, entry);
        }

        List<EmployeeEntryDO> entries = new ArrayList<>(teamEntryMap.values());
        entries.sort(Comparator
                .comparing((EmployeeEntryDO item) -> item.getDeptId() == null ? Long.MAX_VALUE : item.getDeptId())
                .thenComparing(item -> item.getEntryDate() == null ? LocalDate.MAX : item.getEntryDate())
                .thenComparing(item -> item.getId() == null ? Long.MAX_VALUE : item.getId()));

        Set<Long> profileIds = entries.stream()
                .map(EmployeeEntryDO::getProfileId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        Set<Long> userIds = entries.stream()
                .map(EmployeeEntryDO::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));

        Map<Long, EmployeeProfileDO> profileMap = loadProfileMap(profileIds);
        Map<Long, Long> userProfileMap = profileMap.values().stream()
                .filter(item -> item.getUserId() != null)
                .collect(Collectors.toMap(EmployeeProfileDO::getUserId, EmployeeProfileDO::getId, (left, right) -> left));
        Map<Long, EmployeePerformanceDO> latestPerformanceMap = loadLatestPerformanceMap(profileIds);
        List<HrTodoTaskDO> openTodos = loadOpenTodos(profileIds, userIds);
        Map<Long, Integer> todoCountMap = buildTodoCountMap(openTodos, userProfileMap);
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
        LocalDate reminderDeadline = today.plusDays(30);

        List<AttendanceDailyResultDO> attendanceResults = loadAttendanceResults(profileIds, monthStart, today);
        List<AttendanceExceptionDO> attendanceExceptions = loadAttendanceExceptions(profileIds, monthStart, today);
        List<AttendanceCorrectionDO> pendingCorrections = loadPendingCorrections(profileIds);
        int runningLeaveCount = countRunningLeaves(userIds);
        int pendingOvertimeCount = countPendingOvertimes(profileIds, userIds);
        int runningTripCount = countRunningTrips(userIds);
        List<EmployeeMaterialDO> expiringMaterials = loadExpiringMaterials(profileIds, today, reminderDeadline);
        List<TrainingAssignmentDO> trainingAssignments = loadTrainingAssignments(profileIds);

        Map<Long, List<AttendanceDailyResultDO>> attendanceResultMap = groupByProfile(attendanceResults, AttendanceDailyResultDO::getProfileId);
        Map<Long, Integer> attendanceExceptionCountMap = countByProfile(attendanceExceptions, AttendanceExceptionDO::getProfileId);
        Map<Long, Integer> pendingCorrectionCountMap = countByProfile(pendingCorrections, AttendanceCorrectionDO::getProfileId);
        Map<Long, Integer> materialExpiringCountMap = countByProfile(expiringMaterials, EmployeeMaterialDO::getProfileId);
        Map<Long, List<TrainingAssignmentDO>> trainingAssignmentMap = groupByProfile(trainingAssignments, TrainingAssignmentDO::getProfileId);

        Set<Long> deptIds = new HashSet<>();
        addIfNotNull(deptIds, managerEntry == null ? null : managerEntry.getDeptId());
        entries.forEach(entry -> addIfNotNull(deptIds, entry.getDeptId()));
        Map<Long, DeptRespDTO> deptMap = loadDeptMap(deptIds);

        respVO.setHasProfile(true);
        respVO.setManager(buildManager(managerProfile, managerEntry, deptMap));
        List<HrManagerSelfServiceRespVO.TeamMember> members = new ArrayList<>();
        for (EmployeeEntryDO entry : entries) {
            EmployeeProfileDO profile = profileMap.get(entry.getProfileId());
            HrManagerSelfServiceRespVO.TeamMember member = buildTeamMember(entry, profile, deptMap,
                    latestPerformanceMap.get(entry.getProfileId()), todoCountMap.getOrDefault(entry.getProfileId(), 0));
            fillTeamSignals(member, entry, profile,
                    attendanceResultMap.getOrDefault(entry.getProfileId(), Collections.emptyList()),
                    attendanceExceptionCountMap.getOrDefault(entry.getProfileId(), 0),
                    pendingCorrectionCountMap.getOrDefault(entry.getProfileId(), 0),
                    materialExpiringCountMap.getOrDefault(entry.getProfileId(), 0),
                    trainingAssignmentMap.getOrDefault(entry.getProfileId(), Collections.emptyList()));
            members.add(member);
        }
        respVO.setTeamMembers(members);
        respVO.setSummary(buildSummary(entries, members, latestPerformanceMap, todoCountMap));
        respVO.setAttendanceSummary(buildAttendanceSummary(attendanceResults, attendanceExceptions, pendingCorrections));
        respVO.setReminderSummary(buildReminderSummary(entries, profileMap, expiringMaterials, today, monthStart, monthEnd, reminderDeadline));
        respVO.setLearningSummary(buildLearningSummary(trainingAssignments));
        respVO.setApprovalSummary(buildApprovalSummary(openTodos, runningLeaveCount,
                pendingCorrections.size(), pendingOvertimeCount, runningTripCount));
        respVO.setAttentionItems(buildAttentionItems(members, openTodos, userProfileMap, attendanceExceptionCountMap,
                pendingCorrectionCountMap, materialExpiringCountMap, trainingAssignmentMap, expiringMaterials));
        respVO.setQuickActions(buildQuickActions());
        respVO.setAnalysis(buildAnalysis(members));
        return respVO;
    }

    @Override
    public List<HrManagerTeamExportRespVO> getTeamExportList() {
        HrManagerSelfServiceRespVO home = getHome();
        List<HrManagerTeamExportRespVO> rows = new ArrayList<>();
        if (home == null || home.getTeamMembers() == null) {
            return rows;
        }
        for (HrManagerSelfServiceRespVO.TeamMember member : home.getTeamMembers()) {
            rows.add(toExportRow(member));
        }
        return rows;
    }

    private HrManagerTeamExportRespVO toExportRow(HrManagerSelfServiceRespVO.TeamMember member) {
        HrManagerTeamExportRespVO row = new HrManagerTeamExportRespVO();
        row.setProfileId(member.getProfileId());
        row.setEmployeeNo(member.getEmployeeNo());
        row.setName(member.getName());
        row.setMobile(member.getMobile());
        row.setDeptName(member.getDeptName());
        row.setJobTitle(member.getJobTitle());
        row.setWorkStatusText(member.getWorkStatusText());
        row.setEmploymentTypeText(member.getEmploymentTypeText());
        row.setEntryDate(member.getEntryDate());
        row.setContractEndDate(member.getContractEndDate());
        row.setLatestPerformancePeriod(member.getLatestPerformancePeriod());
        row.setLatestPerformanceGrade(member.getLatestPerformanceGrade());
        row.setLatestPerformanceResult(member.getLatestPerformanceResult());
        row.setLatestPerformanceScore(member.getLatestPerformanceScore());
        row.setOpenTodoCount(member.getOpenTodoCount());
        row.setAttendanceExceptionCount(member.getAttendanceExceptionCount());
        row.setPendingCorrectionCount(member.getPendingCorrectionCount());
        row.setMaterialExpiringCount(member.getMaterialExpiringCount());
        row.setProbationDueDate(member.getProbationDueDate());
        row.setTrainingCompletedCount(member.getTrainingCompletedCount());
        row.setTrainingAssignmentCount(member.getTrainingAssignmentCount());
        row.setRiskLevel(member.getRiskLevel());
        row.setRiskReason(member.getRiskReason());
        return row;
    }

    private HrManagerSelfServiceRespVO emptyHome() {
        HrManagerSelfServiceRespVO respVO = new HrManagerSelfServiceRespVO();
        respVO.setHasProfile(false);
        respVO.setSummary(new HrManagerSelfServiceRespVO.Summary());
        respVO.setAttendanceSummary(new HrManagerSelfServiceRespVO.AttendanceSummary());
        respVO.setReminderSummary(new HrManagerSelfServiceRespVO.ReminderSummary());
        respVO.setLearningSummary(new HrManagerSelfServiceRespVO.LearningSummary());
        respVO.setApprovalSummary(new HrManagerSelfServiceRespVO.ApprovalSummary());
        respVO.setTeamMembers(Collections.emptyList());
        respVO.setAttentionItems(Collections.emptyList());
        respVO.setQuickActions(buildQuickActions());
        respVO.setAnalysis(emptyAnalysis());
        return respVO;
    }

    private EmployeeEntryDO selectPrimaryEntry(Long profileId, Long userId) {
        List<EmployeeEntryDO> entries = new ArrayList<>();
        if (profileId != null) {
            entries.addAll(employeeEntryMapper.selectListByProfileId(profileId));
        }
        if (userId != null) {
            entries.addAll(employeeEntryMapper.selectListByUserId(userId));
        }
        Map<Long, EmployeeEntryDO> entryMap = new LinkedHashMap<>();
        for (EmployeeEntryDO entry : entries) {
            putBetterEntry(entryMap, entry);
        }
        return entryMap.values().stream()
                .min(Comparator
                        .comparing((EmployeeEntryDO item) -> isActiveWorkStatus(item.getWorkStatus()) ? 0 : 1)
                        .thenComparing(item -> item.getEntryDate() == null ? LocalDate.MIN : item.getEntryDate(), Comparator.reverseOrder())
                        .thenComparing(item -> item.getId() == null ? Long.MIN_VALUE : item.getId(), Comparator.reverseOrder()))
                .orElse(null);
    }

    private List<EmployeeEntryDO> selectDirectEntries(Long managerProfileId) {
        if (managerProfileId == null) {
            return Collections.emptyList();
        }
        return employeeEntryMapper.selectList(new LambdaQueryWrapperX<EmployeeEntryDO>()
                .eq(EmployeeEntryDO::getDirectSupervisorId, managerProfileId)
                .orderByAsc(EmployeeEntryDO::getDeptId)
                .orderByAsc(EmployeeEntryDO::getEntryDate)
                .last("LIMIT " + TEAM_LIMIT));
    }

    private List<EmployeeEntryDO> selectSubordinateEntries(Long loginUserId) {
        Set<Long> subordinateUserIds = loadSubordinateUserIds(loginUserId);
        if (subordinateUserIds.isEmpty()) {
            return Collections.emptyList();
        }
        return employeeEntryMapper.selectList(new LambdaQueryWrapperX<EmployeeEntryDO>()
                .in(EmployeeEntryDO::getUserId, subordinateUserIds)
                .orderByAsc(EmployeeEntryDO::getDeptId)
                .orderByAsc(EmployeeEntryDO::getEntryDate)
                .last("LIMIT " + TEAM_LIMIT));
    }

    private Set<Long> loadSubordinateUserIds(Long loginUserId) {
        if (loginUserId == null) {
            return Collections.emptySet();
        }
        try {
            List<AdminUserRespDTO> users = adminUserApi.getUserListBySubordinate(loginUserId).getCheckedData();
            if (users == null || users.isEmpty()) {
                return Collections.emptySet();
            }
            return users.stream()
                    .map(AdminUserRespDTO::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(HashSet::new));
        } catch (Exception ex) {
            log.warn("Load subordinate users for manager self-service failed, userId={}, reason={}",
                    loginUserId, ex.getMessage());
            return Collections.emptySet();
        }
    }

    private Map<Long, EmployeeProfileDO> loadProfileMap(Set<Long> profileIds) {
        if (profileIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<EmployeeProfileDO> profiles = employeeProfileMapper.selectList(new LambdaQueryWrapperX<EmployeeProfileDO>()
                .in(EmployeeProfileDO::getId, profileIds));
        return profiles.stream().collect(Collectors.toMap(EmployeeProfileDO::getId, item -> item, (left, right) -> left));
    }

    private Map<Long, DeptRespDTO> loadDeptMap(Set<Long> deptIds) {
        if (deptIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            List<DeptRespDTO> depts = deptApi.getDeptList(deptIds).getCheckedData();
            if (depts == null) {
                return Collections.emptyMap();
            }
            return depts.stream()
                    .filter(item -> item.getId() != null)
                    .collect(Collectors.toMap(DeptRespDTO::getId, item -> item, (left, right) -> left));
        } catch (Exception ex) {
            log.warn("Load dept map for manager self-service failed, deptIds={}, reason={}", deptIds, ex.getMessage());
            return Collections.emptyMap();
        }
    }

    private Map<Long, EmployeePerformanceDO> loadLatestPerformanceMap(Set<Long> profileIds) {
        if (profileIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<EmployeePerformanceDO> performances = employeePerformanceMapper.selectList(new LambdaQueryWrapperX<EmployeePerformanceDO>()
                .in(EmployeePerformanceDO::getProfileId, profileIds)
                .orderByDesc(EmployeePerformanceDO::getEvaluatedDate)
                .orderByDesc(EmployeePerformanceDO::getId)
                .last("LIMIT " + Math.min(profileIds.size() * 3, 1000)));
        Map<Long, EmployeePerformanceDO> result = new HashMap<>();
        for (EmployeePerformanceDO performance : performances) {
            if (performance.getProfileId() != null) {
                result.putIfAbsent(performance.getProfileId(), performance);
            }
        }
        return result;
    }

    private List<HrTodoTaskDO> loadOpenTodos(Set<Long> profileIds, Set<Long> userIds) {
        Map<Long, HrTodoTaskDO> todoMap = new LinkedHashMap<>();
        if (!profileIds.isEmpty()) {
            List<HrTodoTaskDO> profileTodos = hrTodoTaskMapper.selectList(new LambdaQueryWrapperX<HrTodoTaskDO>()
                    .eq(HrTodoTaskDO::getStatus, TODO_STATUS_OPEN)
                    .in(HrTodoTaskDO::getProfileId, profileIds)
                    .orderByAsc(HrTodoTaskDO::getDueTime)
                    .orderByDesc(HrTodoTaskDO::getId)
                    .last("LIMIT " + TODO_LIMIT));
            profileTodos.forEach(todo -> {
                if (todo.getId() != null) {
                    todoMap.put(todo.getId(), todo);
                }
            });
        }
        if (!userIds.isEmpty()) {
            List<HrTodoTaskDO> userTodos = hrTodoTaskMapper.selectList(new LambdaQueryWrapperX<HrTodoTaskDO>()
                    .eq(HrTodoTaskDO::getStatus, TODO_STATUS_OPEN)
                    .in(HrTodoTaskDO::getAssigneeUserId, userIds)
                    .orderByAsc(HrTodoTaskDO::getDueTime)
                    .orderByDesc(HrTodoTaskDO::getId)
                    .last("LIMIT " + TODO_LIMIT));
            userTodos.forEach(todo -> {
                if (todo.getId() != null) {
                    todoMap.put(todo.getId(), todo);
                }
            });
        }
        return new ArrayList<>(todoMap.values());
    }

    private int countRunningLeaves(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return 0;
        }
        Long count = hrAdministrativeLeaveMapper.selectCount(new LambdaQueryWrapperX<HrAdministrativeLeaveDO>()
                .in(HrAdministrativeLeaveDO::getUserId, userIds)
                .eq(HrAdministrativeLeaveDO::getStatus, ADMIN_STATUS_RUNNING));
        return longToInt(count);
    }

    private int countPendingOvertimes(Set<Long> profileIds, Set<Long> userIds) {
        Map<Long, AttendanceOvertimeDO> overtimeMap = new LinkedHashMap<>();
        if (!profileIds.isEmpty()) {
            List<AttendanceOvertimeDO> profileRows = attendanceOvertimeMapper.selectList(new LambdaQueryWrapperX<AttendanceOvertimeDO>()
                    .eq(AttendanceOvertimeDO::getStatus, STATUS_PENDING)
                    .in(AttendanceOvertimeDO::getProfileId, profileIds)
                    .orderByDesc(AttendanceOvertimeDO::getOvertimeDate)
                    .orderByDesc(AttendanceOvertimeDO::getId)
                    .last("LIMIT " + SIGNAL_LIMIT));
            profileRows.forEach(row -> {
                if (row.getId() != null) {
                    overtimeMap.put(row.getId(), row);
                }
            });
        }
        if (!userIds.isEmpty()) {
            List<AttendanceOvertimeDO> userRows = attendanceOvertimeMapper.selectList(new LambdaQueryWrapperX<AttendanceOvertimeDO>()
                    .eq(AttendanceOvertimeDO::getStatus, STATUS_PENDING)
                    .in(AttendanceOvertimeDO::getUserId, userIds)
                    .orderByDesc(AttendanceOvertimeDO::getOvertimeDate)
                    .orderByDesc(AttendanceOvertimeDO::getId)
                    .last("LIMIT " + SIGNAL_LIMIT));
            userRows.forEach(row -> {
                if (row.getId() != null) {
                    overtimeMap.put(row.getId(), row);
                }
            });
        }
        return overtimeMap.size();
    }

    private int countRunningTrips(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return 0;
        }
        Long count = hrAdministrativeTripMapper.selectCount(new LambdaQueryWrapperX<HrAdministrativeTripDO>()
                .in(HrAdministrativeTripDO::getUserId, userIds)
                .eq(HrAdministrativeTripDO::getStatus, ADMIN_STATUS_RUNNING));
        return longToInt(count);
    }

    private Map<Long, Integer> buildTodoCountMap(List<HrTodoTaskDO> todos, Map<Long, Long> userProfileMap) {
        Map<Long, Integer> result = new HashMap<>();
        for (HrTodoTaskDO todo : todos) {
            Long profileId = todo.getProfileId();
            if (profileId == null && todo.getAssigneeUserId() != null) {
                profileId = userProfileMap.get(todo.getAssigneeUserId());
            }
            if (profileId != null) {
                result.merge(profileId, 1, Integer::sum);
            }
        }
        return result;
    }

    private List<AttendanceDailyResultDO> loadAttendanceResults(Set<Long> profileIds, LocalDate startDate, LocalDate endDate) {
        if (profileIds.isEmpty()) {
            return Collections.emptyList();
        }
        return attendanceDailyResultMapper.selectList(new LambdaQueryWrapperX<AttendanceDailyResultDO>()
                .in(AttendanceDailyResultDO::getProfileId, profileIds)
                .between(AttendanceDailyResultDO::getAttendanceDate, startDate, endDate)
                .orderByDesc(AttendanceDailyResultDO::getAttendanceDate)
                .orderByDesc(AttendanceDailyResultDO::getId)
                .last("LIMIT " + signalLimit(profileIds.size() * 35)));
    }

    private List<AttendanceExceptionDO> loadAttendanceExceptions(Set<Long> profileIds, LocalDate startDate, LocalDate endDate) {
        if (profileIds.isEmpty()) {
            return Collections.emptyList();
        }
        return attendanceExceptionMapper.selectList(new LambdaQueryWrapperX<AttendanceExceptionDO>()
                .in(AttendanceExceptionDO::getProfileId, profileIds)
                .between(AttendanceExceptionDO::getAttendanceDate, startDate, endDate)
                .orderByDesc(AttendanceExceptionDO::getAttendanceDate)
                .orderByDesc(AttendanceExceptionDO::getId)
                .last("LIMIT " + signalLimit(profileIds.size() * 35)));
    }

    private List<AttendanceCorrectionDO> loadPendingCorrections(Set<Long> profileIds) {
        if (profileIds.isEmpty()) {
            return Collections.emptyList();
        }
        return attendanceCorrectionMapper.selectList(new LambdaQueryWrapperX<AttendanceCorrectionDO>()
                .in(AttendanceCorrectionDO::getProfileId, profileIds)
                .eq(AttendanceCorrectionDO::getStatus, STATUS_PENDING)
                .orderByDesc(AttendanceCorrectionDO::getAttendanceDate)
                .orderByDesc(AttendanceCorrectionDO::getId)
                .last("LIMIT " + SIGNAL_LIMIT));
    }

    private List<EmployeeMaterialDO> loadExpiringMaterials(Set<Long> profileIds, LocalDate startDate, LocalDate endDate) {
        if (profileIds.isEmpty()) {
            return Collections.emptyList();
        }
        return employeeMaterialMapper.selectList(new LambdaQueryWrapperX<EmployeeMaterialDO>()
                .in(EmployeeMaterialDO::getProfileId, profileIds)
                .eq(EmployeeMaterialDO::getStatus, MATERIAL_STATUS_ACTIVE)
                .isNotNull(EmployeeMaterialDO::getExpireDate)
                .ge(EmployeeMaterialDO::getExpireDate, startDate)
                .le(EmployeeMaterialDO::getExpireDate, endDate)
                .orderByAsc(EmployeeMaterialDO::getExpireDate)
                .orderByDesc(EmployeeMaterialDO::getId)
                .last("LIMIT " + SIGNAL_LIMIT));
    }

    private List<TrainingAssignmentDO> loadTrainingAssignments(Set<Long> profileIds) {
        if (profileIds.isEmpty()) {
            return Collections.emptyList();
        }
        return trainingAssignmentMapper.selectList(new LambdaQueryWrapperX<TrainingAssignmentDO>()
                .in(TrainingAssignmentDO::getProfileId, profileIds)
                .orderByDesc(TrainingAssignmentDO::getId)
                .last("LIMIT " + SIGNAL_LIMIT));
    }

    private HrManagerSelfServiceRespVO.AttendanceSummary buildAttendanceSummary(List<AttendanceDailyResultDO> attendanceResults,
                                                                                List<AttendanceExceptionDO> attendanceExceptions,
                                                                                List<AttendanceCorrectionDO> pendingCorrections) {
        LocalDate today = LocalDate.now();
        HrManagerSelfServiceRespVO.AttendanceSummary summary = new HrManagerSelfServiceRespVO.AttendanceSummary();
        summary.setRangeStartDate(today.withDayOfMonth(1));
        summary.setRangeEndDate(today);
        summary.setDailyResultCount(attendanceResults.size());
        summary.setAbnormalDayCount((int) attendanceResults.stream().filter(this::isAbnormalAttendance).count());
        summary.setExceptionCount(attendanceExceptions.size());
        summary.setPendingExceptionCount((int) attendanceExceptions.stream()
                .filter(item -> STATUS_PENDING.equals(item.getExceptionStatus())).count());
        summary.setPendingCorrectionCount(pendingCorrections.size());
        summary.setLateCount((int) attendanceResults.stream().filter(this::isLate).count());
        summary.setEarlyLeaveCount((int) attendanceResults.stream().filter(this::isEarlyLeave).count());
        summary.setMissingClockCount((int) attendanceResults.stream().filter(this::isMissingClock).count());
        summary.setAbsenteeismCount((int) attendanceResults.stream().filter(this::isAbsenteeism).count());
        summary.setAbsentHours(sumBigDecimal(attendanceResults, AttendanceDailyResultDO::getAbsentHours));
        summary.setLeaveHours(sumBigDecimal(attendanceResults, AttendanceDailyResultDO::getLeaveHours));
        summary.setTripHours(sumBigDecimal(attendanceResults, AttendanceDailyResultDO::getTripHours));
        return summary;
    }

    private HrManagerSelfServiceRespVO.ReminderSummary buildReminderSummary(List<EmployeeEntryDO> entries,
                                                                            Map<Long, EmployeeProfileDO> profileMap,
                                                                            List<EmployeeMaterialDO> expiringMaterials,
                                                                            LocalDate today,
                                                                            LocalDate monthStart,
                                                                            LocalDate monthEnd,
                                                                            LocalDate reminderDeadline) {
        HrManagerSelfServiceRespVO.ReminderSummary summary = new HrManagerSelfServiceRespVO.ReminderSummary();
        summary.setProbationDueCount((int) entries.stream().filter(this::isProbationDue).count());
        summary.setContractExpiringCount((int) entries.stream().filter(this::isContractExpiring).count());
        summary.setMaterialExpiringCount(expiringMaterials.size());
        summary.setOnboardThisMonthCount((int) entries.stream()
                .filter(item -> isBetween(item.getEntryDate(), monthStart, monthEnd))
                .count());
        summary.setBirthdayIn30DaysCount((int) entries.stream()
                .map(item -> profileMap.get(item.getProfileId()))
                .filter(Objects::nonNull)
                .filter(profile -> upcomingBirthday(profile, today, reminderDeadline) != null)
                .count());
        summary.setProfileMissingCount((int) entries.stream()
                .filter(entry -> isProfileMissing(profileMap.get(entry.getProfileId()), entry))
                .count());
        return summary;
    }

    private HrManagerSelfServiceRespVO.LearningSummary buildLearningSummary(List<TrainingAssignmentDO> assignments) {
        HrManagerSelfServiceRespVO.LearningSummary summary = new HrManagerSelfServiceRespVO.LearningSummary();
        int total = assignments.size();
        int completed = (int) assignments.stream().filter(this::isTrainingCompleted).count();
        int inProgress = (int) assignments.stream().filter(item -> TRAINING_IN_PROGRESS.equals(item.getStatus())).count();
        int notStarted = (int) assignments.stream().filter(item -> TRAINING_NOT_STARTED.equals(item.getStatus())).count();
        summary.setAssignmentCount(total);
        summary.setCompletedCount(completed);
        summary.setInProgressCount(inProgress);
        summary.setNotStartedCount(notStarted);
        summary.setOpenCount(inProgress + notStarted);
        summary.setAvgProgress(avgProgress(assignments));
        summary.setCompletionRate(total == 0 ? BigDecimal.ZERO :
                BigDecimal.valueOf(completed * 100L).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP));
        return summary;
    }

    private HrManagerSelfServiceRespVO.ApprovalSummary buildApprovalSummary(List<HrTodoTaskDO> openTodos,
                                                                            int runningLeaveCount,
                                                                            int pendingCorrectionCount,
                                                                            int pendingOvertimeCount,
                                                                            int runningTripCount) {
        HrManagerSelfServiceRespVO.ApprovalSummary summary = new HrManagerSelfServiceRespVO.ApprovalSummary();
        LocalDateTime now = LocalDateTime.now();
        int highPriorityTodoCount = (int) openTodos.stream()
                .filter(todo -> isHighPriority(todo.getPriority()))
                .count();
        int overdueTodoCount = (int) openTodos.stream()
                .filter(todo -> todo.getDueTime() != null && todo.getDueTime().isBefore(now))
                .count();
        summary.setOpenTodoCount(openTodos.size());
        summary.setHighPriorityTodoCount(highPriorityTodoCount);
        summary.setOverdueTodoCount(overdueTodoCount);
        summary.setRunningLeaveCount(runningLeaveCount);
        summary.setPendingCorrectionCount(pendingCorrectionCount);
        summary.setPendingOvertimeCount(pendingOvertimeCount);
        summary.setRunningTripCount(runningTripCount);
        summary.setWorkflowPendingCount(runningLeaveCount + pendingCorrectionCount + pendingOvertimeCount + runningTripCount);
        return summary;
    }

    private void fillTeamSignals(HrManagerSelfServiceRespVO.TeamMember member,
                                 EmployeeEntryDO entry,
                                 EmployeeProfileDO profile,
                                 List<AttendanceDailyResultDO> attendanceResults,
                                 Integer attendanceExceptionCount,
                                 Integer pendingCorrectionCount,
                                 Integer materialExpiringCount,
                                 List<TrainingAssignmentDO> trainingAssignments) {
        int completedTrainingCount = (int) trainingAssignments.stream().filter(this::isTrainingCompleted).count();
        int openTrainingTaskCount = openTrainingCount(trainingAssignments);
        member.setAttendanceExceptionCount(attendanceExceptionCount);
        member.setPendingCorrectionCount(pendingCorrectionCount);
        member.setTrainingAssignmentCount(trainingAssignments.size());
        member.setTrainingCompletedCount(completedTrainingCount);
        member.setMaterialExpiringCount(materialExpiringCount);
        member.setProbationDueDate(isProbationDue(entry) ? probationDueDate(entry) : null);
        member.setBirthdayDate(upcomingBirthday(profile, LocalDate.now(), LocalDate.now().plusDays(30)));
        upgradeRiskBySignals(member, attendanceResults, attendanceExceptionCount, pendingCorrectionCount,
                materialExpiringCount, openTrainingTaskCount);
    }

    private <T> Map<Long, List<T>> groupByProfile(List<T> rows, Function<T, Long> profileIdGetter) {
        if (rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<T>> result = new HashMap<>();
        for (T row : rows) {
            Long profileId = profileIdGetter.apply(row);
            if (profileId != null) {
                result.computeIfAbsent(profileId, key -> new ArrayList<>()).add(row);
            }
        }
        return result;
    }

    private <T> Map<Long, Integer> countByProfile(List<T> rows, Function<T, Long> profileIdGetter) {
        Map<Long, Integer> result = new HashMap<>();
        for (T row : rows) {
            Long profileId = profileIdGetter.apply(row);
            if (profileId != null) {
                result.merge(profileId, 1, Integer::sum);
            }
        }
        return result;
    }

    private int signalLimit(int expected) {
        return Math.min(Math.max(expected, 1000), SIGNAL_LIMIT);
    }

    private boolean isAbnormalAttendance(AttendanceDailyResultDO result) {
        return result != null && (isLate(result) || isEarlyLeave(result) || isMissingClock(result) || isAbsenteeism(result));
    }

    private boolean isLate(AttendanceDailyResultDO result) {
        return result != null && (RESULT_LATE.equals(result.getResultStatus())
                || RESULT_LATE_EARLY.equals(result.getResultStatus())
                || (result.getLateMinutes() != null && result.getLateMinutes() > 0));
    }

    private boolean isEarlyLeave(AttendanceDailyResultDO result) {
        return result != null && (RESULT_EARLY.equals(result.getResultStatus())
                || RESULT_LATE_EARLY.equals(result.getResultStatus())
                || (result.getEarlyLeaveMinutes() != null && result.getEarlyLeaveMinutes() > 0));
    }

    private boolean isMissingClock(AttendanceDailyResultDO result) {
        return result != null && (RESULT_MISSING_IN.equals(result.getResultStatus())
                || RESULT_MISSING_OUT.equals(result.getResultStatus())
                || RESULT_MISSING_BOTH.equals(result.getResultStatus()));
    }

    private boolean isAbsenteeism(AttendanceDailyResultDO result) {
        return result != null && (RESULT_ABSENTEEISM.equals(result.getResultStatus())
                || (result.getAbsentHours() != null && result.getAbsentHours().compareTo(BigDecimal.ZERO) > 0));
    }

    private BigDecimal sumBigDecimal(List<AttendanceDailyResultDO> rows, Function<AttendanceDailyResultDO, BigDecimal> getter) {
        BigDecimal total = BigDecimal.ZERO;
        for (AttendanceDailyResultDO row : rows) {
            BigDecimal value = row == null ? null : getter.apply(row);
            if (value != null) {
                total = total.add(value);
            }
        }
        return total;
    }

    private boolean isTrainingCompleted(TrainingAssignmentDO assignment) {
        return assignment != null && TRAINING_COMPLETED.equals(assignment.getStatus());
    }

    private int openTrainingCount(List<TrainingAssignmentDO> assignments) {
        if (assignments.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (TrainingAssignmentDO assignment : assignments) {
            if (assignment != null && (TRAINING_NOT_STARTED.equals(assignment.getStatus())
                    || TRAINING_IN_PROGRESS.equals(assignment.getStatus()))) {
                count++;
            }
        }
        return count;
    }

    private BigDecimal avgProgress(List<TrainingAssignmentDO> assignments) {
        if (assignments.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        int count = 0;
        for (TrainingAssignmentDO assignment : assignments) {
            if (assignment == null) {
                continue;
            }
            total = total.add(BigDecimal.valueOf(progressValue(assignment)));
            count++;
        }
        if (count == 0) {
            return BigDecimal.ZERO;
        }
        return total.divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP);
    }

    private int progressValue(TrainingAssignmentDO assignment) {
        if (assignment == null) {
            return 0;
        }
        Integer progress = assignment.getProgress();
        if (progress != null) {
            return Math.max(0, Math.min(progress, 100));
        }
        if (TRAINING_COMPLETED.equals(assignment.getStatus())) {
            return 100;
        }
        return 0;
    }

    private LocalDate probationDueDate(EmployeeEntryDO entry) {
        if (entry == null || entry.getEntryDate() == null || entry.getProbationMonths() == null) {
            return null;
        }
        return entry.getEntryDate().plusMonths(entry.getProbationMonths());
    }

    private LocalDate upcomingBirthday(EmployeeProfileDO profile, LocalDate startDate, LocalDate endDate) {
        if (profile == null || profile.getBirthDate() == null) {
            return null;
        }
        LocalDate birthday = profile.getBirthDate().withYear(startDate.getYear());
        if (birthday.isBefore(startDate)) {
            birthday = birthday.plusYears(1);
        }
        return birthday.isAfter(endDate) ? null : birthday;
    }

    private boolean isBetween(LocalDate value, LocalDate startDate, LocalDate endDate) {
        return value != null && !value.isBefore(startDate) && !value.isAfter(endDate);
    }

    private boolean isProfileMissing(EmployeeProfileDO profile, EmployeeEntryDO entry) {
        return profile == null
                || !StringUtils.hasText(profile.getMobile())
                || profile.getUserId() == null
                || entry == null
                || entry.getDeptId() == null;
    }

    private Map<Long, EmployeeMaterialDO> firstExpiringMaterialByProfile(List<EmployeeMaterialDO> materials) {
        Map<Long, EmployeeMaterialDO> result = new HashMap<>();
        for (EmployeeMaterialDO material : materials) {
            if (material == null || material.getProfileId() == null) {
                continue;
            }
            EmployeeMaterialDO existing = result.get(material.getProfileId());
            if (existing == null || isMaterialSooner(material, existing)) {
                result.put(material.getProfileId(), material);
            }
        }
        return result;
    }

    private boolean isMaterialSooner(EmployeeMaterialDO candidate, EmployeeMaterialDO existing) {
        LocalDate candidateDate = candidate.getExpireDate();
        LocalDate existingDate = existing.getExpireDate();
        if (candidateDate == null) {
            return false;
        }
        if (existingDate == null) {
            return true;
        }
        return candidateDate.isBefore(existingDate);
    }

    private LocalDateTime materialDueTime(EmployeeMaterialDO material) {
        if (material == null || material.getExpireDate() == null) {
            return null;
        }
        return LocalDateTime.of(material.getExpireDate(), LocalTime.of(9, 0));
    }

    private void upgradeRiskBySignals(HrManagerSelfServiceRespVO.TeamMember member,
                                      List<AttendanceDailyResultDO> attendanceResults,
                                      Integer attendanceExceptionCount,
                                      Integer pendingCorrectionCount,
                                      Integer materialExpiringCount,
                                      Integer openTrainingCount) {
        if (member == null) {
            return;
        }
        int abnormalAttendanceCount = (int) attendanceResults.stream().filter(this::isAbnormalAttendance).count();
        if (abnormalAttendanceCount >= 5 || (attendanceExceptionCount != null && attendanceExceptionCount >= 3)) {
            member.setRiskLevel("HIGH");
            member.setRiskReason("考勤异常集中");
            return;
        }
        if ("HIGH".equals(member.getRiskLevel())) {
            return;
        }
        if ((pendingCorrectionCount != null && pendingCorrectionCount > 0)
                || (materialExpiringCount != null && materialExpiringCount > 0)
                || (openTrainingCount != null && openTrainingCount >= 3)) {
            member.setRiskLevel("MEDIUM");
            if (pendingCorrectionCount != null && pendingCorrectionCount > 0) {
                member.setRiskReason("补卡待审批");
            } else if (materialExpiringCount != null && materialExpiringCount > 0) {
                member.setRiskReason("材料即将到期");
            } else {
                member.setRiskReason("培训任务待完成");
            }
        }
    }

    private String attendanceExceptionRoute(Long profileId) {
        return profileId == null ? "/attendance/exceptions" : "/attendance/exceptions?profileId=" + profileId;
    }

    private String attendanceCorrectionRoute(Long profileId) {
        return profileId == null ? "/attendance/corrections" : "/attendance/corrections?profileId=" + profileId;
    }

    private String employeeMaterialRoute(Long profileId) {
        return profileId == null ? "/hr/employee-material" : "/hr/employee-material?profileId=" + profileId;
    }

    private String employeeTrainingRoute(Long profileId) {
        return profileId == null ? "/hr/training?tab=assignments" : "/hr/training?tab=assignments&profileId=" + profileId;
    }

    private HrManagerSelfServiceRespVO.ManagerProfile buildManager(EmployeeProfileDO profile, EmployeeEntryDO entry,
                                                                   Map<Long, DeptRespDTO> deptMap) {
        HrManagerSelfServiceRespVO.ManagerProfile manager = new HrManagerSelfServiceRespVO.ManagerProfile();
        manager.setProfileId(profile.getId());
        manager.setUserId(profile.getUserId());
        manager.setName(profile.getName());
        manager.setMobile(profile.getMobile());
        manager.setEmail(profile.getEmail());
        if (entry != null) {
            manager.setDeptId(entry.getDeptId());
            manager.setDeptName(deptName(deptMap, entry.getDeptId()));
            manager.setJobTitle(entry.getJobTitle());
        }
        return manager;
    }

    private HrManagerSelfServiceRespVO.TeamMember buildTeamMember(EmployeeEntryDO entry, EmployeeProfileDO profile,
                                                                  Map<Long, DeptRespDTO> deptMap,
                                                                  EmployeePerformanceDO latestPerformance,
                                                                  Integer openTodoCount) {
        HrManagerSelfServiceRespVO.TeamMember member = new HrManagerSelfServiceRespVO.TeamMember();
        member.setProfileId(entry.getProfileId());
        member.setEntryId(entry.getId());
        member.setUserId(entry.getUserId());
        member.setEmployeeNo(entry.getEmployeeNo());
        member.setName(profile == null ? null : profile.getName());
        member.setMobile(profile == null ? null : profile.getMobile());
        member.setEmail(profile == null ? null : profile.getEmail());
        member.setDeptId(entry.getDeptId());
        member.setDeptName(deptName(deptMap, entry.getDeptId()));
        member.setJobTitle(entry.getJobTitle());
        member.setWorkStatus(entry.getWorkStatus());
        member.setWorkStatusText(workStatusText(entry.getWorkStatus()));
        member.setEmploymentType(entry.getEmploymentType());
        member.setEmploymentTypeText(employmentTypeText(entry.getEmploymentType()));
        member.setEntryDate(entry.getEntryDate());
        member.setContractEndDate(entry.getContractEndDate());
        member.setOpenTodoCount(openTodoCount);
        if (latestPerformance != null) {
            member.setLatestPerformancePeriod(latestPerformance.getPeriod());
            member.setLatestPerformanceScore(latestPerformance.getScore());
            member.setLatestPerformanceGrade(latestPerformance.getGrade());
            member.setLatestPerformanceResult(latestPerformance.getResult());
            member.setLatestPerformanceDate(latestPerformance.getEvaluatedDate());
        }
        fillRisk(member, entry, latestPerformance, openTodoCount);
        return member;
    }

    private HrManagerSelfServiceRespVO.Summary buildSummary(List<EmployeeEntryDO> entries,
                                                            List<HrManagerSelfServiceRespVO.TeamMember> members,
                                                            Map<Long, EmployeePerformanceDO> latestPerformanceMap,
                                                            Map<Long, Integer> todoCountMap) {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

        HrManagerSelfServiceRespVO.Summary summary = new HrManagerSelfServiceRespVO.Summary();
        summary.setTeamCount(entries.size());
        summary.setActiveCount((int) entries.stream().filter(item -> isActiveWorkStatus(item.getWorkStatus())).count());
        summary.setProbationCount((int) entries.stream().filter(item -> Objects.equals(item.getWorkStatus(), 2)).count());
        summary.setOnboardThisMonthCount((int) entries.stream()
                .filter(item -> item.getEntryDate() != null
                        && !item.getEntryDate().isBefore(monthStart)
                        && !item.getEntryDate().isAfter(monthEnd))
                .count());
        summary.setContractExpiringCount((int) entries.stream().filter(this::isContractExpiring).count());
        summary.setPerformanceWarningCount((int) latestPerformanceMap.values().stream()
                .filter(this::isPerformanceWarning)
                .count());
        summary.setOpenTodoCount(todoCountMap.values().stream().reduce(0, Integer::sum));
        summary.setDeptCount((int) entries.stream().map(EmployeeEntryDO::getDeptId).filter(Objects::nonNull).distinct().count());
        summary.setAvgPerformanceScore(avgScore(members));
        return summary;
    }

    private List<HrManagerSelfServiceRespVO.AttentionItem> buildAttentionItems(
            List<HrManagerSelfServiceRespVO.TeamMember> members,
            List<HrTodoTaskDO> openTodos,
            Map<Long, Long> userProfileMap,
            Map<Long, Integer> attendanceExceptionCountMap,
            Map<Long, Integer> pendingCorrectionCountMap,
            Map<Long, Integer> materialExpiringCountMap,
            Map<Long, List<TrainingAssignmentDO>> trainingAssignmentMap,
            List<EmployeeMaterialDO> expiringMaterials) {
        Map<Long, HrManagerSelfServiceRespVO.TeamMember> memberMap = members.stream()
                .filter(item -> item.getProfileId() != null)
                .collect(Collectors.toMap(HrManagerSelfServiceRespVO.TeamMember::getProfileId, item -> item, (left, right) -> left));
        List<HrManagerSelfServiceRespVO.AttentionItem> items = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        Map<Long, EmployeeMaterialDO> firstMaterialMap = firstExpiringMaterialByProfile(expiringMaterials);

        openTodos.stream()
                .filter(todo -> isHighPriority(todo.getPriority()) || (todo.getDueTime() != null && todo.getDueTime().isBefore(now)))
                .sorted(Comparator
                        .comparing((HrTodoTaskDO item) -> isHighPriority(item.getPriority()) ? 0 : 1)
                        .thenComparing(item -> item.getDueTime() == null ? LocalDateTime.MAX : item.getDueTime()))
                .limit(6)
                .forEach(todo -> {
                    Long profileId = todo.getProfileId() == null ? userProfileMap.get(todo.getAssigneeUserId()) : todo.getProfileId();
                    HrManagerSelfServiceRespVO.TeamMember member = memberMap.get(profileId);
                    if (member != null) {
                        items.add(attention("TODO", todo.getTitle(), todo.getContent(), member,
                        StringUtils.hasText(todo.getRoutePath()) ? todo.getRoutePath() : employeeRoute(profileId),
                                todo.getPriority(), todo.getDueTime()));
                    }
                });

        for (HrManagerSelfServiceRespVO.TeamMember member : members) {
            if (items.size() >= 12) {
                break;
            }
            Long profileId = member.getProfileId();
            int correctionCount = pendingCorrectionCountMap.getOrDefault(profileId, 0);
            int exceptionCount = attendanceExceptionCountMap.getOrDefault(profileId, 0);
            int materialCount = materialExpiringCountMap.getOrDefault(profileId, 0);
            int openTrainingCount = openTrainingCount(trainingAssignmentMap.getOrDefault(profileId, Collections.emptyList()));
            if (correctionCount > 0) {
                items.add(attention("ATTENDANCE_CORRECTION", "补卡/更正待审批",
                        correctionCount + " 条考勤更正需要处理", member,
                        attendanceCorrectionRoute(profileId), "HIGH", null));
            } else if (exceptionCount > 0) {
                items.add(attention("ATTENDANCE_EXCEPTION", "考勤异常待跟进",
                        "本月已有 " + exceptionCount + " 条考勤异常", member,
                        attendanceExceptionRoute(profileId), "MEDIUM", null));
            } else if (materialCount > 0) {
                EmployeeMaterialDO material = firstMaterialMap.get(profileId);
                items.add(attention("MATERIAL_EXPIRING", "材料即将到期",
                        "30 天内有 " + materialCount + " 份材料到期", member,
                        employeeMaterialRoute(profileId), "MEDIUM", materialDueTime(material)));
            } else if (openTrainingCount > 0) {
                items.add(attention("TRAINING_OPEN", "培训学习待完成",
                        openTrainingCount + " 个培训任务未完成", member,
                        employeeTrainingRoute(profileId), "LOW", null));
            }
        }

        for (HrManagerSelfServiceRespVO.TeamMember member : members) {
            if (items.size() >= 12) {
                break;
            }
            if ("HIGH".equals(member.getRiskLevel()) || "MEDIUM".equals(member.getRiskLevel())) {
                items.add(attention("TEAM_RISK", member.getRiskReason(), teamRiskContent(member), member,
                        employeeRoute(member.getProfileId()), member.getRiskLevel(), null));
            }
        }
        return items;
    }

    private HrManagerSelfServiceRespVO.AttentionItem attention(String type, String title, String content,
                                                               HrManagerSelfServiceRespVO.TeamMember member,
                                                               String routePath, String priority,
                                                               LocalDateTime dueTime) {
        HrManagerSelfServiceRespVO.AttentionItem item = new HrManagerSelfServiceRespVO.AttentionItem();
        item.setType(type);
        item.setTitle(StringUtils.hasText(title) ? title : "团队事项");
        item.setContent(StringUtils.hasText(content) ? content : "");
        item.setProfileId(member.getProfileId());
        item.setEmployeeName(member.getName());
        item.setRoutePath(routePath);
        item.setPriority(priority);
        item.setDueTime(dueTime);
        return item;
    }

    private HrManagerSelfServiceRespVO.Analysis emptyAnalysis() {
        HrManagerSelfServiceRespVO.Analysis analysis = new HrManagerSelfServiceRespVO.Analysis();
        analysis.setDeptDistribution(Collections.emptyList());
        analysis.setRiskDistribution(Collections.emptyList());
        analysis.setTodoDistribution(Collections.emptyList());
        analysis.setPerformanceDistribution(Collections.emptyList());
        analysis.setTrainingDistribution(Collections.emptyList());
        analysis.setTopAttentionMembers(Collections.emptyList());
        return analysis;
    }

    private HrManagerSelfServiceRespVO.Analysis buildAnalysis(List<HrManagerSelfServiceRespVO.TeamMember> members) {
        if (members == null || members.isEmpty()) {
            return emptyAnalysis();
        }
        HrManagerSelfServiceRespVO.Analysis analysis = new HrManagerSelfServiceRespVO.Analysis();
        analysis.setDeptDistribution(buildDeptDistribution(members));
        analysis.setRiskDistribution(buildFixedDistribution(members,
                item -> StringUtils.hasText(item.getRiskLevel()) ? item.getRiskLevel() : "LOW",
                riskNameMap(), null));
        analysis.setTodoDistribution(buildFixedDistribution(members, this::todoBucket, todoBucketNameMap(), null));
        analysis.setPerformanceDistribution(buildDynamicDistribution(members, this::performanceBucket, null));
        analysis.setTrainingDistribution(buildFixedDistribution(members, this::trainingBucket,
                trainingBucketNameMap(), trainingRouteMap()));
        analysis.setTopAttentionMembers(buildTopAttentionMembers(members));
        return analysis;
    }

    private List<HrManagerSelfServiceRespVO.DistributionItem> buildDeptDistribution(
            List<HrManagerSelfServiceRespVO.TeamMember> members) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, String> names = new HashMap<>();
        Map<String, String> routes = new HashMap<>();
        for (HrManagerSelfServiceRespVO.TeamMember member : members) {
            String key = member.getDeptId() == null ? "NO_DEPT" : String.valueOf(member.getDeptId());
            String name = StringUtils.hasText(member.getDeptName()) ? member.getDeptName() : "未分配部门";
            counts.merge(key, 1, Integer::sum);
            names.putIfAbsent(key, name);
            if (member.getDeptId() != null) {
                routes.putIfAbsent(key, "/hr/employee?deptId=" + member.getDeptId());
            }
        }
        return toDistributionItems(counts, names, routes, members.size());
    }

    private List<HrManagerSelfServiceRespVO.DistributionItem> buildFixedDistribution(
            List<HrManagerSelfServiceRespVO.TeamMember> members,
            Function<HrManagerSelfServiceRespVO.TeamMember, String> classifier,
            Map<String, String> names,
            Map<String, String> routes) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        names.keySet().forEach(key -> counts.put(key, 0));
        for (HrManagerSelfServiceRespVO.TeamMember member : members) {
            String key = classifier.apply(member);
            counts.put(key, counts.getOrDefault(key, 0) + 1);
        }
        return toDistributionItems(counts, names, routes, members.size());
    }

    private List<HrManagerSelfServiceRespVO.DistributionItem> buildDynamicDistribution(
            List<HrManagerSelfServiceRespVO.TeamMember> members,
            Function<HrManagerSelfServiceRespVO.TeamMember, String> classifier,
            Map<String, String> routes) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, String> names = new HashMap<>();
        for (HrManagerSelfServiceRespVO.TeamMember member : members) {
            String key = classifier.apply(member);
            counts.merge(key, 1, Integer::sum);
            names.putIfAbsent(key, key);
        }
        return toDistributionItems(counts, names, routes, members.size());
    }

    private List<HrManagerSelfServiceRespVO.DistributionItem> toDistributionItems(
            Map<String, Integer> counts,
            Map<String, String> names,
            Map<String, String> routes,
            int total) {
        return counts.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue() > 0)
                .map(entry -> {
                    HrManagerSelfServiceRespVO.DistributionItem item = new HrManagerSelfServiceRespVO.DistributionItem();
                    item.setKey(entry.getKey());
                    item.setName(names == null ? entry.getKey() : names.getOrDefault(entry.getKey(), entry.getKey()));
                    item.setCount(entry.getValue());
                    item.setPercent(total <= 0 ? BigDecimal.ZERO :
                            BigDecimal.valueOf(entry.getValue() * 100L)
                                    .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP));
                    item.setRoutePath(routes == null ? null : routes.get(entry.getKey()));
                    return item;
                })
                .sorted(Comparator.comparing(HrManagerSelfServiceRespVO.DistributionItem::getCount).reversed())
                .collect(Collectors.toList());
    }

    private Map<String, String> riskNameMap() {
        Map<String, String> names = new LinkedHashMap<>();
        names.put("HIGH", "高风险");
        names.put("MEDIUM", "中风险");
        names.put("LOW", "低风险");
        return names;
    }

    private Map<String, String> todoBucketNameMap() {
        Map<String, String> names = new LinkedHashMap<>();
        names.put("NONE", "无待办");
        names.put("LOW", "1-2 项");
        names.put("HIGH", "3 项及以上");
        return names;
    }

    private Map<String, String> trainingBucketNameMap() {
        Map<String, String> names = new LinkedHashMap<>();
        names.put("UNASSIGNED", "未分配");
        names.put("NOT_STARTED", "未开始");
        names.put("IN_PROGRESS", "进行中");
        names.put("COMPLETED", "已完成");
        return names;
    }

    private Map<String, String> trainingRouteMap() {
        Map<String, String> routes = new LinkedHashMap<>();
        routes.put("UNASSIGNED", "/hr/training?tab=plans");
        routes.put("NOT_STARTED", "/hr/training?tab=assignments&status=NOT_STARTED");
        routes.put("IN_PROGRESS", "/hr/training?tab=assignments&status=IN_PROGRESS");
        routes.put("COMPLETED", "/hr/training?tab=assignments&status=COMPLETED");
        return routes;
    }

    private String todoBucket(HrManagerSelfServiceRespVO.TeamMember member) {
        int count = member.getOpenTodoCount() == null ? 0 : member.getOpenTodoCount();
        if (count <= 0) {
            return "NONE";
        }
        return count >= 3 ? "HIGH" : "LOW";
    }

    private String performanceBucket(HrManagerSelfServiceRespVO.TeamMember member) {
        if (StringUtils.hasText(member.getLatestPerformanceGrade())) {
            return member.getLatestPerformanceGrade();
        }
        if (StringUtils.hasText(member.getLatestPerformanceResult())) {
            return member.getLatestPerformanceResult();
        }
        return "未记录";
    }

    private String trainingBucket(HrManagerSelfServiceRespVO.TeamMember member) {
        int total = member.getTrainingAssignmentCount() == null ? 0 : member.getTrainingAssignmentCount();
        int completed = member.getTrainingCompletedCount() == null ? 0 : member.getTrainingCompletedCount();
        if (total <= 0) {
            return "UNASSIGNED";
        }
        if (completed >= total) {
            return "COMPLETED";
        }
        return completed <= 0 ? "NOT_STARTED" : "IN_PROGRESS";
    }

    private List<HrManagerSelfServiceRespVO.AttentionMember> buildTopAttentionMembers(
            List<HrManagerSelfServiceRespVO.TeamMember> members) {
        return members.stream()
                .map(this::buildAttentionMember)
                .filter(item -> item.getAttentionScore() != null && item.getAttentionScore() > 0)
                .sorted(Comparator.comparing(HrManagerSelfServiceRespVO.AttentionMember::getAttentionScore).reversed())
                .limit(8)
                .collect(Collectors.toList());
    }

    private HrManagerSelfServiceRespVO.AttentionMember buildAttentionMember(
            HrManagerSelfServiceRespVO.TeamMember member) {
        HrManagerSelfServiceRespVO.AttentionMember item = new HrManagerSelfServiceRespVO.AttentionMember();
        int trainingOpenCount = Math.max((member.getTrainingAssignmentCount() == null ? 0 : member.getTrainingAssignmentCount())
                - (member.getTrainingCompletedCount() == null ? 0 : member.getTrainingCompletedCount()), 0);
        item.setProfileId(member.getProfileId());
        item.setName(member.getName());
        item.setDeptName(member.getDeptName());
        item.setJobTitle(member.getJobTitle());
        item.setRiskLevel(member.getRiskLevel());
        item.setRiskReason(member.getRiskReason());
        item.setOpenTodoCount(member.getOpenTodoCount());
        item.setAttendanceExceptionCount(member.getAttendanceExceptionCount());
        item.setPendingCorrectionCount(member.getPendingCorrectionCount());
        item.setMaterialExpiringCount(member.getMaterialExpiringCount());
        item.setTrainingOpenCount(trainingOpenCount);
        item.setAttentionScore(attentionScore(member, trainingOpenCount));
        item.setRoutePath(employeeRoute(member.getProfileId()));
        return item;
    }

    private int attentionScore(HrManagerSelfServiceRespVO.TeamMember member, int trainingOpenCount) {
        int score = 0;
        if ("HIGH".equals(member.getRiskLevel())) {
            score += 80;
        } else if ("MEDIUM".equals(member.getRiskLevel())) {
            score += 40;
        }
        score += safeInt(member.getOpenTodoCount()) * 6;
        score += safeInt(member.getAttendanceExceptionCount()) * 8;
        score += safeInt(member.getPendingCorrectionCount()) * 10;
        score += safeInt(member.getMaterialExpiringCount()) * 8;
        score += trainingOpenCount * 4;
        return score;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private int longToInt(Long value) {
        return value == null ? 0 : value.intValue();
    }

    private List<HrManagerSelfServiceRespVO.QuickAction> buildQuickActions() {
        List<HrManagerSelfServiceRespVO.QuickAction> actions = new ArrayList<>();
        actions.add(quickAction("团队花名册", "lucide:users-round", "/hr/employee", "people"));
        actions.add(quickAction("团队待办", "lucide:list-checks", "/hr/todo", "process"));
        actions.add(quickAction("团队考勤", "lucide:calendar-check", "/attendance/workbench", "attendance"));
        actions.add(quickAction("请假管理", "lucide:calendar-minus", "/administrative/leave", "attendance"));
        actions.add(quickAction("出差管理", "lucide:plane", "/administrative/trip", "attendance"));
        actions.add(quickAction("加班调休", "lucide:timer-reset", "/attendance/overtime", "attendance"));
        actions.add(quickAction("绩效工作台", "lucide:trophy", "/hr/performance", "performance"));
        actions.add(quickAction("培训跟进", "lucide:book-open-check", "/hr/training?tab=assignments", "learning"));
        actions.add(quickAction("电子材料", "lucide:folder-open", "/hr/employee-material", "people"));
        actions.add(quickAction("入转调离", "lucide:git-pull-request-arrow", "/hr/lifecycle/workbench", "lifecycle"));
        return actions;
    }

    private HrManagerSelfServiceRespVO.QuickAction quickAction(String title, String icon, String path, String category) {
        HrManagerSelfServiceRespVO.QuickAction action = new HrManagerSelfServiceRespVO.QuickAction();
        action.setTitle(title);
        action.setIcon(icon);
        action.setPath(path);
        action.setCategory(category);
        return action;
    }

    private void putBetterEntry(Map<Long, EmployeeEntryDO> entryMap, EmployeeEntryDO entry) {
        if (entry == null) {
            return;
        }
        Long key = entry.getProfileId();
        if (key == null) {
            key = entry.getId();
        }
        if (key == null) {
            return;
        }
        EmployeeEntryDO existing = entryMap.get(key);
        if (existing == null || isBetterEntry(entry, existing)) {
            entryMap.put(key, entry);
        }
    }

    private boolean isBetterEntry(EmployeeEntryDO candidate, EmployeeEntryDO existing) {
        boolean candidateActive = isActiveWorkStatus(candidate.getWorkStatus());
        boolean existingActive = isActiveWorkStatus(existing.getWorkStatus());
        if (candidateActive != existingActive) {
            return candidateActive;
        }
        LocalDate candidateDate = candidate.getEntryDate();
        LocalDate existingDate = existing.getEntryDate();
        if (candidateDate != null && existingDate != null && !candidateDate.equals(existingDate)) {
            return candidateDate.isAfter(existingDate);
        }
        if (candidateDate != null && existingDate == null) {
            return true;
        }
        if (candidate.getId() != null && existing.getId() != null) {
            return candidate.getId() > existing.getId();
        }
        return false;
    }

    private void fillRisk(HrManagerSelfServiceRespVO.TeamMember member, EmployeeEntryDO entry,
                          EmployeePerformanceDO performance, Integer openTodoCount) {
        if (performance != null && isPerformanceWarning(performance)) {
            member.setRiskLevel("HIGH");
            member.setRiskReason("绩效需要关注");
            return;
        }
        if (openTodoCount != null && openTodoCount >= 3) {
            member.setRiskLevel("MEDIUM");
            member.setRiskReason("待办积压");
            return;
        }
        if (isContractExpiring(entry)) {
            member.setRiskLevel("MEDIUM");
            member.setRiskReason("合同即将到期");
            return;
        }
        if (isProbationDue(entry)) {
            member.setRiskLevel("MEDIUM");
            member.setRiskReason("试用期即将到期");
            return;
        }
        member.setRiskLevel("LOW");
        member.setRiskReason("状态正常");
    }

    private boolean isActiveWorkStatus(Integer workStatus) {
        return Objects.equals(workStatus, 2) || Objects.equals(workStatus, 3);
    }

    private boolean isContractExpiring(EmployeeEntryDO entry) {
        if (entry == null || entry.getContractEndDate() == null || Objects.equals(entry.getWorkStatus(), 4)) {
            return false;
        }
        LocalDate today = LocalDate.now();
        LocalDate warningEnd = today.plusDays(60);
        return !entry.getContractEndDate().isBefore(today) && !entry.getContractEndDate().isAfter(warningEnd);
    }

    private boolean isProbationDue(EmployeeEntryDO entry) {
        if (entry == null || !Objects.equals(entry.getWorkStatus(), 2)
                || entry.getEntryDate() == null || entry.getProbationMonths() == null) {
            return false;
        }
        LocalDate dueDate = entry.getEntryDate().plusMonths(entry.getProbationMonths());
        LocalDate today = LocalDate.now();
        return !dueDate.isBefore(today) && !dueDate.isAfter(today.plusDays(30));
    }

    private boolean isPerformanceWarning(EmployeePerformanceDO performance) {
        if (performance == null) {
            return false;
        }
        if (performance.getScore() != null && performance.getScore().compareTo(BigDecimal.valueOf(60)) < 0) {
            return true;
        }
        String grade = empty(performance.getGrade()).toUpperCase();
        String result = empty(performance.getResult());
        return grade.contains("C") || grade.contains("D")
                || result.contains("待改进") || result.contains("不合格") || result.contains("较差");
    }

    private boolean isHighPriority(String priority) {
        String value = empty(priority).toUpperCase();
        return "HIGH".equals(value) || "URGENT".equals(value) || "高".equals(priority) || "紧急".equals(priority);
    }

    private BigDecimal avgScore(Collection<HrManagerSelfServiceRespVO.TeamMember> members) {
        List<BigDecimal> scores = members.stream()
                .map(HrManagerSelfServiceRespVO.TeamMember::getLatestPerformanceScore)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (scores.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(scores.size()), 1, RoundingMode.HALF_UP);
    }

    private String workStatusText(Integer status) {
        if (Objects.equals(status, 0)) {
            return "待完善";
        }
        if (Objects.equals(status, 1)) {
            return "待入职";
        }
        if (Objects.equals(status, 2)) {
            return "试用期";
        }
        if (Objects.equals(status, 3)) {
            return "在职";
        }
        if (Objects.equals(status, 4)) {
            return "离职";
        }
        return "未知";
    }

    private String employmentTypeText(Integer type) {
        if (Objects.equals(type, 1)) {
            return "全职";
        }
        if (Objects.equals(type, 2)) {
            return "兼职";
        }
        if (Objects.equals(type, 3)) {
            return "劳务";
        }
        if (Objects.equals(type, 4)) {
            return "实习";
        }
        return "未设置";
    }

    private String deptName(Map<Long, DeptRespDTO> deptMap, Long deptId) {
        DeptRespDTO dept = deptMap.get(deptId);
        return dept == null ? null : dept.getName();
    }

    private String employeeRoute(Long profileId) {
        return profileId == null ? "/hr/employee" : "/hr/employee/detail?id=" + profileId;
    }

    private String teamRiskContent(HrManagerSelfServiceRespVO.TeamMember member) {
        String name = StringUtils.hasText(member.getName()) ? member.getName() : "该成员";
        return name + "：" + member.getRiskReason();
    }

    private String empty(String value) {
        return value == null ? "" : value;
    }

    private void addIfNotNull(Set<Long> values, Long value) {
        if (value != null) {
            values.add(value);
        }
    }
}
