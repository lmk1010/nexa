package com.kyx.service.hr.service.attendance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.security.core.service.SecurityFrameworkService;
import com.kyx.foundation.security.core.util.SecurityFrameworkUtils;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceClockInReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceClockRecordPageReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceClockRecordRespVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceClockRecordSummaryRespVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceMyMonthDayRespVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceMyTodayRespVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceSyncReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceWorkbenchRespVO;
import com.kyx.service.hr.dal.dataobject.administrative.HrAdministrativeLeaveDO;
import com.kyx.service.hr.dal.dataobject.administrative.HrAdministrativeTripDO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceCorrectionDO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceClockRecordDO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceExceptionDO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceMonthlyConfirmDO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceOvertimeDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeProfileDO;
import com.kyx.service.hr.dal.mysql.administrative.HrAdministrativeLeaveMapper;
import com.kyx.service.hr.dal.mysql.administrative.HrAdministrativeTripMapper;
import com.kyx.service.hr.dal.mysql.attendance.AttendanceCorrectionMapper;
import com.kyx.service.hr.dal.mysql.attendance.AttendanceClockRecordMapper;
import com.kyx.service.hr.dal.mysql.attendance.AttendanceExceptionMapper;
import com.kyx.service.hr.dal.mysql.attendance.AttendanceMonthlyConfirmMapper;
import com.kyx.service.hr.dal.mysql.attendance.AttendanceOvertimeMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeProfileMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.kyx.service.hr.enums.ErrorCodeConstants.ATTENDANCE_CLOCK_ALREADY_EXISTS;
import static com.kyx.service.hr.enums.ErrorCodeConstants.ATTENDANCE_CLOCK_TYPE_INVALID;
import static com.kyx.service.hr.enums.ErrorCodeConstants.ATTENDANCE_EMPLOYEE_NOT_BOUND;

/**
 * 员工打卡 Service 实现
 *
 * @author MK
 */
@Service
@Validated
@Slf4j
public class AttendanceClockRecordServiceImpl implements AttendanceClockRecordService {

    private static final String CLOCK_TYPE_IN = "IN";
    private static final String CLOCK_TYPE_OUT = "OUT";
    private static final String CLOCK_TYPE_UNKNOWN = "UNKNOWN";

    private static final String STATUS_NORMAL = "NORMAL";
    private static final String STATUS_LATE = "LATE";
    private static final String STATUS_EARLY = "EARLY";
    private static final String STATUS_ABSENTEEISM = "ABSENTEEISM";
    private static final String STATUS_UNKNOWN = "UNKNOWN";
    private static final String STATUS_LEAVE_COVERED = "LEAVE_COVERED";

    private static final String SOURCE_MANUAL = "MANUAL";
    private static final String SOURCE_DING_TALK = "DINGTALK";
    private static final String DINGTALK_LEAVE_PROCESS_INSTANCE_PREFIX = "dingtalk-leave-";
    private static final String PERMISSION_QUERY_ALL = "attendance:clock:query-all";
    private static final String PERMISSION_EXCEPTION_HANDLE = "attendance:exception:handle";
    private static final String PERMISSION_CORRECTION_APPROVE = "attendance:correction:approve";
    private static final String PERMISSION_OVERTIME_APPROVE = "attendance:overtime:approve";
    private static final String PERMISSION_SETTLEMENT_LOCK = "attendance:settlement:lock";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_ISSUE = "ISSUE";
    private static final int BPM_STATUS_APPROVE = 2;
    private static final int WORKDAY_MINUTES = 480;
    private static final LocalTime WORK_START_TIME = LocalTime.of(9, 0);
    private static final LocalTime WORK_END_TIME = LocalTime.of(18, 0);

    @Resource
    private AttendanceClockRecordMapper attendanceClockRecordMapper;
    @Resource
    private AttendanceCorrectionMapper attendanceCorrectionMapper;
    @Resource
    private HrAdministrativeLeaveMapper hrAdministrativeLeaveMapper;
    @Resource
    private HrAdministrativeTripMapper hrAdministrativeTripMapper;
    @Resource
    private AttendanceOvertimeMapper attendanceOvertimeMapper;
    @Resource
    private AttendanceMonthlyConfirmMapper attendanceMonthlyConfirmMapper;
    @Resource
    private AttendanceExceptionMapper attendanceExceptionMapper;
    @Resource
    private EmployeeProfileMapper employeeProfileMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private SecurityFrameworkService securityFrameworkService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long clock(AttendanceClockInReqVO reqVO) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        EmployeeProfileDO profile = employeeProfileMapper.selectByUserId(userId);
        if (profile == null) {
            throw ServiceExceptionUtil.exception(ATTENDANCE_EMPLOYEE_NOT_BOUND);
        }

        String clockType = normalizeClockType(reqVO.getClockType());
        LocalDate attendanceDate = LocalDate.now();
        AttendanceClockRecordDO existed = attendanceClockRecordMapper
                .selectByUserIdAndDateAndType(userId, attendanceDate, clockType);
        if (existed != null) {
            throw ServiceExceptionUtil.exception(ATTENDANCE_CLOCK_ALREADY_EXISTS);
        }

        LocalDateTime now = LocalDateTime.now();
        AttendanceClockRecordDO record = new AttendanceClockRecordDO();
        record.setUserId(userId);
        record.setProfileId(profile.getId());
        record.setAttendanceDate(attendanceDate);
        record.setClockType(clockType);
        record.setClockTime(now);
        record.setClockStatus(resolveClockStatus(clockType, now.toLocalTime()));
        record.setSourceType(SOURCE_MANUAL);
        record.setLocationName(reqVO.getLocationName());
        record.setLocationAddress(reqVO.getLocationAddress());
        record.setDeviceInfo(reqVO.getDeviceInfo());
        record.setRemark(reqVO.getRemark());
        attendanceClockRecordMapper.insert(record);
        return record.getId();
    }

    @Override
    public AttendanceMyTodayRespVO getMyToday() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        LocalDate today = LocalDate.now();

        AttendanceMyTodayRespVO respVO = new AttendanceMyTodayRespVO();
        respVO.setAttendanceDate(today);
        if (userId == null) {
            respVO.setHasClockIn(false);
            respVO.setHasClockOut(false);
            respVO.setRecords(new ArrayList<>());
            return respVO;
        }

        List<AttendanceClockRecordDO> records = attendanceClockRecordMapper.selectListByUserIdAndDate(userId, today);
        fillTodayResp(respVO, records);
        return respVO;
    }

    @Override
    public List<AttendanceMyMonthDayRespVO> getMyMonth(Integer year, Integer month) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            return new ArrayList<>();
        }

        LocalDate monthDate = resolveMonth(year, month);
        LocalDate startDate = monthDate.withDayOfMonth(1);
        LocalDate endDate = monthDate.with(TemporalAdjusters.lastDayOfMonth());

        List<AttendanceClockRecordDO> records = attendanceClockRecordMapper
                .selectListByUserIdAndDateRange(userId, startDate, endDate);
        Map<LocalDate, List<AttendanceClockRecordDO>> grouped = new LinkedHashMap<>();
        for (AttendanceClockRecordDO record : records) {
            grouped.computeIfAbsent(record.getAttendanceDate(), key -> new ArrayList<>()).add(record);
        }
        Map<LocalDate, Integer> leaveMinutesByDate = loadLeaveMinutesByDate(userId, startDate, endDate);

        List<AttendanceMyMonthDayRespVO> result = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            result.add(buildMonthDayResp(date, grouped.getOrDefault(date, new ArrayList<>()),
                    leaveMinutesByDate.getOrDefault(date, 0)));
        }
        return result;
    }

    @Override
    public PageResult<AttendanceClockRecordRespVO> getClockRecordPage(AttendanceClockRecordPageReqVO pageReqVO) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            return new PageResult<>(new ArrayList<>(), 0L);
        }
        if (!canQueryAllClockRecords()) {
            // 非管理员强制仅查看本人，避免通过请求参数越权查询他人
            pageReqVO.setUserId(loginUserId);
            pageReqVO.setProfileId(null);
        }

        PageResult<AttendanceClockRecordDO> pageResult = attendanceClockRecordMapper.selectPage(pageReqVO);
        List<AttendanceClockRecordDO> records = pageResult.getList();
        if (records == null || records.isEmpty()) {
            return new PageResult<>(new ArrayList<>(), pageResult.getTotal());
        }

        List<AttendanceClockRecordRespVO> respList = BeanUtils.toBean(records, AttendanceClockRecordRespVO.class);

        Set<Long> userIds = new HashSet<>();
        Set<Long> profileIds = new HashSet<>();
        for (AttendanceClockRecordDO record : records) {
            if (record.getUserId() != null) {
                userIds.add(record.getUserId());
            }
            if (record.getProfileId() != null) {
                profileIds.add(record.getProfileId());
            }
        }

        Map<Long, AdminUserRespDTO> userMap = loadUserMapSafe(userIds);
        Map<Long, EmployeeProfileDO> profileMap = loadProfileMapSafe(profileIds);
        Map<String, LeaveCoverage> leaveCoverageMap = loadLeaveCoverageMap(records);
        for (int i = 0; i < respList.size(); i++) {
            AttendanceClockRecordRespVO item = respList.get(i);
            AttendanceClockRecordDO record = records.get(i);
            Long userId = item.getUserId();
            if (userId != null) {
                AdminUserRespDTO user = userMap.get(userId);
                if (user != null) {
                    item.setUserNickname(
                            StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
                }
            }

            Long profileId = item.getProfileId();
            if (profileId != null) {
                EmployeeProfileDO profile = profileMap.get(profileId);
                if (profile != null) {
                    item.setProfileName(profile.getName());
                }
            }

            if (!StringUtils.hasText(item.getDingUserId())) {
                item.setDingUserId(extractDingUserIdFromSourceRecordId(item.getSourceRecordId()));
            }
            applyLeaveCoverage(item, record, leaveCoverageMap);
        }
        return new PageResult<>(respList, pageResult.getTotal());
    }

    @Override
    public PageResult<AttendanceClockRecordRespVO> getMyClockRecordPage(AttendanceClockRecordPageReqVO pageReqVO) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            return new PageResult<>(new ArrayList<>(), 0L);
        }
        pageReqVO.setUserId(loginUserId);
        pageReqVO.setProfileId(null);
        return getClockRecordPage(pageReqVO);
    }

    @Override
    public AttendanceClockRecordSummaryRespVO getClockRecordSummary(AttendanceClockRecordPageReqVO pageReqVO) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            return buildEmptySummary();
        }
        if (!canQueryAllClockRecords()) {
            pageReqVO.setUserId(loginUserId);
            pageReqVO.setProfileId(null);
        }
        List<AttendanceClockRecordDO> records = attendanceClockRecordMapper.selectListByReqVO(pageReqVO);
        Map<String, LeaveCoverage> leaveCoverageMap = loadLeaveCoverageMap(records);
        AttendanceClockRecordSummaryRespVO summary = buildEmptySummary();
        Set<Long> userIds = new HashSet<>();
        Set<LocalDate> days = new HashSet<>();
        long normalCount = 0L;
        long lateCount = 0L;
        long earlyCount = 0L;
        long absenteeismCount = 0L;
        long unknownCount = 0L;
        long leaveCoveredCount = 0L;
        long clockInCount = 0L;
        long clockOutCount = 0L;
        long manualCount = 0L;
        long dingTalkCount = 0L;
        for (AttendanceClockRecordDO record : records) {
            if (record.getUserId() != null) {
                userIds.add(record.getUserId());
            }
            if (record.getAttendanceDate() != null) {
                days.add(record.getAttendanceDate());
            }
            String status = record.getClockStatus();
            LeaveCoverage leaveCoverage = leaveCoverageMap.get(buildAttendanceDayKey(
                    record.getUserId(), record.getAttendanceDate()));
            if (isClockStatusLeaveCovered(record, leaveCoverage)) {
                leaveCoveredCount++;
            } else if (!StringUtils.hasText(status) || STATUS_NORMAL.equals(status)) {
                normalCount++;
            } else if (STATUS_LATE.equals(status)) {
                lateCount++;
            } else if (STATUS_EARLY.equals(status)) {
                earlyCount++;
            } else if (STATUS_ABSENTEEISM.equals(status)) {
                absenteeismCount++;
            } else {
                unknownCount++;
            }
            if (CLOCK_TYPE_IN.equals(record.getClockType())) {
                clockInCount++;
            } else if (CLOCK_TYPE_OUT.equals(record.getClockType())) {
                clockOutCount++;
            }
            if (SOURCE_MANUAL.equals(record.getSourceType())) {
                manualCount++;
            } else if (SOURCE_DING_TALK.equals(record.getSourceType())) {
                dingTalkCount++;
            }
        }
        summary.setRecordCount((long) records.size());
        summary.setUserCount((long) userIds.size());
        summary.setDayCount((long) days.size());
        summary.setNormalCount(normalCount);
        summary.setLateCount(lateCount);
        summary.setEarlyCount(earlyCount);
        summary.setAbsenteeismCount(absenteeismCount);
        summary.setUnknownCount(unknownCount);
        summary.setLeaveCoveredCount(leaveCoveredCount);
        summary.setClockInCount(clockInCount);
        summary.setClockOutCount(clockOutCount);
        summary.setManualCount(manualCount);
        summary.setDingTalkCount(dingTalkCount);
        return summary;
    }

    @Override
    public AttendanceWorkbenchRespVO getWorkbench() {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        boolean queryAll = canQueryAllClockRecords();
        boolean settlementManage = canManageMonthlySettlement();
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.with(TemporalAdjusters.lastDayOfMonth());

        AttendanceClockRecordPageReqVO todayReqVO = new AttendanceClockRecordPageReqVO();
        todayReqVO.setAttendanceDate(new LocalDate[]{today, today});
        if (!queryAll) {
            todayReqVO.setUserId(loginUserId);
        }
        List<AttendanceClockRecordDO> todayRecords = loginUserId == null
                ? new ArrayList<>() : attendanceClockRecordMapper.selectListByReqVO(todayReqVO);

        AttendanceClockRecordPageReqVO monthReqVO = new AttendanceClockRecordPageReqVO();
        monthReqVO.setAttendanceDate(new LocalDate[]{monthStart, monthEnd});
        AttendanceClockRecordSummaryRespVO monthSummary = getClockRecordSummary(monthReqVO);

        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = LocalDateTime.of(today, LocalTime.MAX);
        List<HrAdministrativeLeaveDO> todayLeaves = selectTodayLeaves(queryAll, loginUserId, dayStart, dayEnd);
        List<HrAdministrativeTripDO> todayTrips = selectTodayTrips(queryAll, loginUserId, dayStart, dayEnd);
        int activeEmployeeCount = queryAll ? countActiveEmployees() : (loginUserId == null ? 0 : 1);
        int todayClockUserCount = countDistinctClockUsers(todayRecords);
        int runningLeaveCount = countRunningLeaves(queryAll, loginUserId);
        int runningTripCount = countRunningTrips(queryAll, loginUserId);
        int pendingCorrectionCount = countPendingCorrections(queryAll, loginUserId);
        int pendingOvertimeCount = countPendingOvertimes(queryAll, loginUserId);
        int pendingMonthlyConfirmCount = countPendingMonthlyConfirms(settlementManage, loginUserId);
        int pendingMonthlyIssueCount = countPendingMonthlyIssues(settlementManage, loginUserId);
        int pendingExceptionCount = countPendingExceptions(queryAll, loginUserId);
        int pendingTodoCount = pendingCorrectionCount + pendingOvertimeCount + pendingMonthlyConfirmCount
                + pendingMonthlyIssueCount + pendingExceptionCount;

        AttendanceWorkbenchRespVO respVO = new AttendanceWorkbenchRespVO();
        respVO.setToday(today);
        respVO.setActiveEmployeeCount(activeEmployeeCount);
        respVO.setTodayClockUserCount(todayClockUserCount);
        respVO.setTodayClockInCount(countClockType(todayRecords, CLOCK_TYPE_IN));
        respVO.setTodayClockOutCount(countClockType(todayRecords, CLOCK_TYPE_OUT));
        respVO.setTodayLeaveUserCount(countDistinctLeaveUsers(todayLeaves));
        respVO.setTodayTripUserCount(countDistinctTripUsers(todayTrips));
        respVO.setRunningLeaveCount(runningLeaveCount);
        respVO.setRunningTripCount(runningTripCount);
        respVO.setPendingCorrectionCount(pendingCorrectionCount);
        respVO.setPendingOvertimeCount(pendingOvertimeCount);
        respVO.setPendingMonthlyConfirmCount(pendingMonthlyConfirmCount);
        respVO.setPendingMonthlyIssueCount(pendingMonthlyIssueCount);
        respVO.setPendingExceptionCount(pendingExceptionCount);
        respVO.setPendingTodoCount(pendingTodoCount);
        respVO.setMonthSummary(monthSummary);
        respVO.setAlerts(buildWorkbenchAlerts(activeEmployeeCount, todayClockUserCount,
                respVO.getTodayLeaveUserCount(), respVO.getTodayTripUserCount(), monthSummary,
                runningLeaveCount, runningTripCount, pendingCorrectionCount, pendingOvertimeCount,
                pendingMonthlyConfirmCount, pendingMonthlyIssueCount, pendingExceptionCount, settlementManage));
        respVO.setQuickActions(buildWorkbenchQuickActions());
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer syncDingTalk(AttendanceSyncReqVO reqVO) {
        return syncDingTalkDetailed(reqVO).getProcessedCount();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttendanceDingTalkSyncResult syncDingTalkDetailed(AttendanceSyncReqVO reqVO) {
        String sourceType = StringUtils.hasText(reqVO.getSourceType())
                ? reqVO.getSourceType().trim().toUpperCase()
                : SOURCE_DING_TALK;

        AttendanceDingTalkSyncResult result = new AttendanceDingTalkSyncResult();
        if (reqVO.getRecords() == null || reqVO.getRecords().isEmpty()) {
            return result;
        }
        for (AttendanceSyncReqVO.Record item : reqVO.getRecords()) {
            if (item == null || item.getUserId() == null || item.getClockTime() == null) {
                result.addSkipped();
                continue;
            }
            String clockType = normalizeSyncClockType(item.getClockType());
            LocalDateTime normalizedClockTime = normalizeClockTime(item.getClockTime());

            Long profileId = item.getProfileId();
            if (profileId == null) {
                EmployeeProfileDO profile = employeeProfileMapper.selectByUserId(item.getUserId());
                if (profile == null) {
                    log.warn("Skip DingTalk attendance record because OA user has no employee profile, userId={}, sourceRecordId={}",
                            item.getUserId(), item.getSourceRecordId());
                    result.addSkipped();
                    continue;
                }
                profileId = profile.getId();
            }

            LocalDate attendanceDate = item.getAttendanceDate() != null
                    ? item.getAttendanceDate()
                    : normalizedClockTime.toLocalDate();
            String clockStatus = StringUtils.hasText(item.getClockStatus())
                    ? item.getClockStatus().trim().toUpperCase()
                    : (CLOCK_TYPE_UNKNOWN.equals(clockType)
                    ? CLOCK_TYPE_UNKNOWN
                    : resolveClockStatus(clockType, normalizedClockTime.toLocalTime()));

            AttendanceClockRecordDO existed = attendanceClockRecordMapper.selectBySourceRecordId(item.getSourceRecordId());
            if (existed == null) {
                existed = attendanceClockRecordMapper.selectByBizKey(
                        item.getUserId(),
                        sourceType,
                        normalizedClockTime,
                        clockType
                );
            }
            if (existed == null && !CLOCK_TYPE_UNKNOWN.equals(clockType)) {
                existed = attendanceClockRecordMapper.selectByBizKey(
                        item.getUserId(),
                        sourceType,
                        normalizedClockTime,
                        CLOCK_TYPE_UNKNOWN
                );
            }
            if (existed == null && isAutoSyncRemark(item.getRemark())) {
                existed = attendanceClockRecordMapper.selectByBizTimeKey(
                        item.getUserId(),
                        sourceType,
                        normalizedClockTime
                );
            }
            if (existed == null) {
                AttendanceClockRecordDO createDO = new AttendanceClockRecordDO();
                createDO.setUserId(item.getUserId());
                createDO.setProfileId(profileId);
                createDO.setAttendanceDate(attendanceDate);
                createDO.setClockType(clockType);
                createDO.setClockTime(normalizedClockTime);
                createDO.setClockStatus(clockStatus);
                createDO.setSourceType(sourceType);
                createDO.setSourceRecordId(item.getSourceRecordId());
                createDO.setLocationName(item.getLocationName());
                createDO.setLocationAddress(item.getLocationAddress());
                createDO.setDeviceInfo(item.getDeviceInfo());
                createDO.setRemark(item.getRemark());
                createDO.setRawPayload(item.getRawPayload());
                createDO.setSyncTime(LocalDateTime.now());
                try {
                    attendanceClockRecordMapper.insert(createDO);
                    result.addCreated();
                } catch (DuplicateKeyException duplicateKeyException) {
                    existed = attendanceClockRecordMapper.selectByBizKey(
                            item.getUserId(),
                            sourceType,
                            normalizedClockTime,
                            clockType
                    );
                    if (existed == null) {
                        throw duplicateKeyException;
                    }
                    AttendanceClockRecordDO updateDO = new AttendanceClockRecordDO();
                    updateDO.setId(existed.getId());
                    updateDO.setUserId(item.getUserId());
                    updateDO.setProfileId(profileId);
                    updateDO.setAttendanceDate(attendanceDate);
                    updateDO.setClockType(clockType);
                    updateDO.setClockTime(normalizedClockTime);
                    updateDO.setClockStatus(clockStatus);
                    updateDO.setSourceType(sourceType);
                    updateDO.setSourceRecordId(preferIncoming(item.getSourceRecordId(), existed.getSourceRecordId()));
                    updateDO.setLocationName(preferIncoming(item.getLocationName(), existed.getLocationName()));
                    updateDO.setLocationAddress(preferIncoming(item.getLocationAddress(), existed.getLocationAddress()));
                    updateDO.setDeviceInfo(preferIncoming(item.getDeviceInfo(), existed.getDeviceInfo()));
                    updateDO.setRemark(preferIncoming(item.getRemark(), existed.getRemark()));
                    updateDO.setRawPayload(preferIncoming(item.getRawPayload(), existed.getRawPayload()));
                    updateDO.setSyncTime(LocalDateTime.now());
                    attendanceClockRecordMapper.updateById(updateDO);
                    result.addUpdated();
                }
            } else {
                AttendanceClockRecordDO updateDO = new AttendanceClockRecordDO();
                updateDO.setId(existed.getId());
                updateDO.setUserId(item.getUserId());
                updateDO.setProfileId(profileId);
                updateDO.setAttendanceDate(attendanceDate);
                updateDO.setClockType(clockType);
                updateDO.setClockTime(normalizedClockTime);
                updateDO.setClockStatus(clockStatus);
                updateDO.setSourceType(sourceType);
                updateDO.setSourceRecordId(preferIncoming(item.getSourceRecordId(), existed.getSourceRecordId()));
                updateDO.setLocationName(preferIncoming(item.getLocationName(), existed.getLocationName()));
                updateDO.setLocationAddress(preferIncoming(item.getLocationAddress(), existed.getLocationAddress()));
                updateDO.setDeviceInfo(preferIncoming(item.getDeviceInfo(), existed.getDeviceInfo()));
                updateDO.setRemark(preferIncoming(item.getRemark(), existed.getRemark()));
                updateDO.setRawPayload(preferIncoming(item.getRawPayload(), existed.getRawPayload()));
                updateDO.setSyncTime(LocalDateTime.now());
                attendanceClockRecordMapper.updateById(updateDO);
                result.addUpdated();
            }
        }
        return result;
    }

    private AttendanceClockRecordSummaryRespVO buildEmptySummary() {
        AttendanceClockRecordSummaryRespVO summary = new AttendanceClockRecordSummaryRespVO();
        summary.setRecordCount(0L);
        summary.setUserCount(0L);
        summary.setDayCount(0L);
        summary.setNormalCount(0L);
        summary.setLateCount(0L);
        summary.setEarlyCount(0L);
        summary.setAbsenteeismCount(0L);
        summary.setUnknownCount(0L);
        summary.setLeaveCoveredCount(0L);
        summary.setClockInCount(0L);
        summary.setClockOutCount(0L);
        summary.setManualCount(0L);
        summary.setDingTalkCount(0L);
        return summary;
    }

    private List<HrAdministrativeLeaveDO> selectTodayLeaves(boolean queryAll, Long loginUserId,
                                                            LocalDateTime dayStart, LocalDateTime dayEnd) {
        if (!queryAll && loginUserId == null) {
            return new ArrayList<>();
        }
        if (!queryAll) {
            return hrAdministrativeLeaveMapper.selectListByUserIdAndTimeRange(loginUserId, dayStart, dayEnd);
        }
        List<HrAdministrativeLeaveDO> leaves = hrAdministrativeLeaveMapper.selectList(
                new LambdaQueryWrapper<HrAdministrativeLeaveDO>()
                        .in(HrAdministrativeLeaveDO::getStatus, 1, BPM_STATUS_APPROVE)
                        .le(HrAdministrativeLeaveDO::getStartTime, dayEnd)
                        .ge(HrAdministrativeLeaveDO::getEndTime, dayStart)
                        .orderByDesc(HrAdministrativeLeaveDO::getId));
        return leaves == null ? new ArrayList<>() : leaves;
    }

    private List<HrAdministrativeTripDO> selectTodayTrips(boolean queryAll, Long loginUserId,
                                                          LocalDateTime dayStart, LocalDateTime dayEnd) {
        if (!queryAll && loginUserId == null) {
            return new ArrayList<>();
        }
        if (!queryAll) {
            return hrAdministrativeTripMapper.selectListByUserIdAndTimeRange(loginUserId, dayStart, dayEnd);
        }
        List<HrAdministrativeTripDO> trips = hrAdministrativeTripMapper.selectList(
                new LambdaQueryWrapper<HrAdministrativeTripDO>()
                        .in(HrAdministrativeTripDO::getStatus, 1, BPM_STATUS_APPROVE)
                        .le(HrAdministrativeTripDO::getStartTime, dayEnd)
                        .ge(HrAdministrativeTripDO::getEndTime, dayStart)
                        .orderByDesc(HrAdministrativeTripDO::getId));
        return trips == null ? new ArrayList<>() : trips;
    }

    private int countActiveEmployees() {
        Long count = employeeProfileMapper.selectCount(new LambdaQueryWrapper<EmployeeProfileDO>()
                .eq(EmployeeProfileDO::getStatus, 1));
        return count == null ? 0 : count.intValue();
    }

    private int countDistinctClockUsers(List<AttendanceClockRecordDO> records) {
        Set<Long> userIds = new HashSet<>();
        if (records != null) {
            for (AttendanceClockRecordDO record : records) {
                if (record != null && record.getUserId() != null) {
                    userIds.add(record.getUserId());
                }
            }
        }
        return userIds.size();
    }

    private int countRunningLeaves(boolean queryAll, Long loginUserId) {
        Long count;
        if (!queryAll) {
            count = loginUserId == null ? 0L : hrAdministrativeLeaveMapper.selectCountByUserIdAndStatus(loginUserId, 1);
        } else {
            count = hrAdministrativeLeaveMapper.selectCount(new LambdaQueryWrapper<HrAdministrativeLeaveDO>()
                    .eq(HrAdministrativeLeaveDO::getStatus, 1));
        }
        return count == null ? 0 : count.intValue();
    }

    private int countRunningTrips(boolean queryAll, Long loginUserId) {
        Long count;
        if (!queryAll) {
            count = loginUserId == null ? 0L : hrAdministrativeTripMapper.selectCountByUserIdAndStatus(loginUserId, 1);
        } else {
            count = hrAdministrativeTripMapper.selectCount(new LambdaQueryWrapper<HrAdministrativeTripDO>()
                    .eq(HrAdministrativeTripDO::getStatus, 1));
        }
        return count == null ? 0 : count.intValue();
    }

    private int countClockType(List<AttendanceClockRecordDO> records, String clockType) {
        int count = 0;
        if (records == null || !StringUtils.hasText(clockType)) {
            return count;
        }
        for (AttendanceClockRecordDO record : records) {
            if (record != null && clockType.equals(record.getClockType())) {
                count++;
            }
        }
        return count;
    }

    private int countDistinctLeaveUsers(List<HrAdministrativeLeaveDO> leaves) {
        Set<Long> userIds = new HashSet<>();
        if (leaves != null) {
            for (HrAdministrativeLeaveDO leave : leaves) {
                if (leave != null && leave.getUserId() != null) {
                    userIds.add(leave.getUserId());
                }
            }
        }
        return userIds.size();
    }

    private int countDistinctTripUsers(List<HrAdministrativeTripDO> trips) {
        Set<Long> userIds = new HashSet<>();
        if (trips != null) {
            for (HrAdministrativeTripDO trip : trips) {
                if (trip != null && trip.getUserId() != null) {
                    userIds.add(trip.getUserId());
                }
            }
        }
        return userIds.size();
    }

    private int countPendingCorrections(boolean queryAll, Long loginUserId) {
        if (!queryAll && loginUserId == null) {
            return 0;
        }
        LambdaQueryWrapper<AttendanceCorrectionDO> wrapper = new LambdaQueryWrapper<AttendanceCorrectionDO>()
                .eq(AttendanceCorrectionDO::getStatus, STATUS_PENDING);
        if (!queryAll) {
            wrapper.eq(AttendanceCorrectionDO::getUserId, loginUserId);
        }
        return toInt(attendanceCorrectionMapper.selectCount(wrapper));
    }

    private int countPendingOvertimes(boolean queryAll, Long loginUserId) {
        if (!queryAll && loginUserId == null) {
            return 0;
        }
        LambdaQueryWrapper<AttendanceOvertimeDO> wrapper = new LambdaQueryWrapper<AttendanceOvertimeDO>()
                .eq(AttendanceOvertimeDO::getStatus, STATUS_PENDING);
        if (!queryAll) {
            wrapper.eq(AttendanceOvertimeDO::getUserId, loginUserId);
        }
        return toInt(attendanceOvertimeMapper.selectCount(wrapper));
    }

    private int countPendingMonthlyConfirms(boolean settlementManage, Long loginUserId) {
        if (!settlementManage && loginUserId == null) {
            return 0;
        }
        LambdaQueryWrapper<AttendanceMonthlyConfirmDO> wrapper = new LambdaQueryWrapper<AttendanceMonthlyConfirmDO>()
                .eq(AttendanceMonthlyConfirmDO::getStatus, STATUS_PENDING);
        if (!settlementManage) {
            wrapper.eq(AttendanceMonthlyConfirmDO::getUserId, loginUserId);
        }
        return toInt(attendanceMonthlyConfirmMapper.selectCount(wrapper));
    }

    private int countPendingMonthlyIssues(boolean settlementManage, Long loginUserId) {
        if (!settlementManage && loginUserId == null) {
            return 0;
        }
        LambdaQueryWrapper<AttendanceMonthlyConfirmDO> wrapper = new LambdaQueryWrapper<AttendanceMonthlyConfirmDO>()
                .eq(AttendanceMonthlyConfirmDO::getStatus, STATUS_ISSUE);
        if (!settlementManage) {
            wrapper.eq(AttendanceMonthlyConfirmDO::getUserId, loginUserId);
        }
        return toInt(attendanceMonthlyConfirmMapper.selectCount(wrapper));
    }

    private int countPendingExceptions(boolean queryAll, Long loginUserId) {
        if (!queryAll && loginUserId == null) {
            return 0;
        }
        LambdaQueryWrapper<AttendanceExceptionDO> wrapper = new LambdaQueryWrapper<AttendanceExceptionDO>()
                .eq(AttendanceExceptionDO::getExceptionStatus, STATUS_PENDING);
        if (!queryAll) {
            wrapper.eq(AttendanceExceptionDO::getUserId, loginUserId);
        }
        return toInt(attendanceExceptionMapper.selectCount(wrapper));
    }

    private List<AttendanceWorkbenchRespVO.Alert> buildWorkbenchAlerts(int activeEmployeeCount,
                                                                       int todayClockUserCount,
                                                                       int todayLeaveUserCount,
                                                                       int todayTripUserCount,
                                                                       AttendanceClockRecordSummaryRespVO monthSummary,
                                                                       int runningLeaveCount,
                                                                       int runningTripCount,
                                                                       int pendingCorrectionCount,
                                                                       int pendingOvertimeCount,
                                                                       int pendingMonthlyConfirmCount,
                                                                       int pendingMonthlyIssueCount,
                                                                       int pendingExceptionCount,
                                                                       boolean settlementManage) {
        List<AttendanceWorkbenchRespVO.Alert> alerts = new ArrayList<>();
        int missingClockUserCount = Math.max(activeEmployeeCount - todayClockUserCount
                - todayLeaveUserCount - todayTripUserCount, 0);
        if (missingClockUserCount > 0) {
            alerts.add(buildWorkbenchAlert("MISSING_CLOCK", "今日未打卡人数", missingClockUserCount,
                    "warning", "/attendance/exceptions"));
        }
        int abnormalCount = countMonthAbnormal(monthSummary);
        if (abnormalCount > 0) {
            alerts.add(buildWorkbenchAlert("MONTH_ABNORMAL", "本月考勤异常", abnormalCount,
                    "danger", "/attendance/exceptions"));
        }
        if (runningLeaveCount > 0) {
            alerts.add(buildWorkbenchAlert("RUNNING_LEAVE", "审批中请假", runningLeaveCount,
                    "info", "/administrative/leave"));
        }
        if (runningTripCount > 0) {
            alerts.add(buildWorkbenchAlert("RUNNING_TRIP", "审批中出差", runningTripCount,
                    "info", "/administrative/trip"));
        }
        if (pendingCorrectionCount > 0) {
            alerts.add(buildWorkbenchAlert("PENDING_CORRECTION", "补卡待处理", pendingCorrectionCount,
                    "warning", "/attendance/corrections"));
        }
        if (pendingOvertimeCount > 0) {
            alerts.add(buildWorkbenchAlert("PENDING_OVERTIME", "加班待处理", pendingOvertimeCount,
                    "warning", "/attendance/overtime"));
        }
        if (pendingMonthlyConfirmCount > 0) {
            alerts.add(buildWorkbenchAlert("PENDING_MONTHLY_CONFIRM", "月度考勤待确认", pendingMonthlyConfirmCount,
                    "warning", settlementManage ? "/attendance/monthly-settlement" : "/attendance/my-monthly-confirm"));
        }
        if (pendingMonthlyIssueCount > 0) {
            alerts.add(buildWorkbenchAlert("PENDING_MONTHLY_ISSUE", "月度考勤异议", pendingMonthlyIssueCount,
                    "danger", settlementManage ? "/attendance/monthly-settlement" : "/attendance/my-monthly-confirm"));
        }
        if (pendingExceptionCount > 0) {
            alerts.add(buildWorkbenchAlert("PENDING_EXCEPTION", "考勤异常待处理", pendingExceptionCount,
                    "danger", "/attendance/exceptions"));
        }
        return alerts;
    }

    private int countMonthAbnormal(AttendanceClockRecordSummaryRespVO monthSummary) {
        if (monthSummary == null) {
            return 0;
        }
        return toInt(monthSummary.getLateCount())
                + toInt(monthSummary.getEarlyCount())
                + toInt(monthSummary.getAbsenteeismCount())
                + toInt(monthSummary.getUnknownCount());
    }

    private int toInt(Long value) {
        return value == null ? 0 : value.intValue();
    }

    private AttendanceWorkbenchRespVO.Alert buildWorkbenchAlert(String type, String title, Integer count,
                                                                String severity, String path) {
        AttendanceWorkbenchRespVO.Alert alert = new AttendanceWorkbenchRespVO.Alert();
        alert.setType(type);
        alert.setTitle(title);
        alert.setCount(count);
        alert.setSeverity(severity);
        alert.setPath(path);
        return alert;
    }

    private List<AttendanceWorkbenchRespVO.QuickAction> buildWorkbenchQuickActions() {
        List<AttendanceWorkbenchRespVO.QuickAction> actions = new ArrayList<>();
        actions.add(buildWorkbenchQuickAction("打卡中心", "lucide:clock-3", "/attendance/clock-in"));
        actions.add(buildWorkbenchQuickAction("考勤列表", "lucide:clipboard-list", "/attendance/records"));
        actions.add(buildWorkbenchQuickAction("考勤结果", "lucide:calendar-check", "/attendance/daily-result"));
        actions.add(buildWorkbenchQuickAction("异常处理", "lucide:circle-alert", "/attendance/exceptions"));
        actions.add(buildWorkbenchQuickAction("假期余额", "lucide:hourglass", "/attendance/leave-balance"));
        actions.add(buildWorkbenchQuickAction("月度结算", "lucide:calendar-range", "/attendance/monthly-settlement"));
        actions.add(buildWorkbenchQuickAction("月度确认", "lucide:calendar-check", "/attendance/my-monthly-confirm"));
        actions.add(buildWorkbenchQuickAction("考勤规则", "lucide:settings-2", "/attendance/rules"));
        actions.add(buildWorkbenchQuickAction("补卡外勤", "lucide:file-check-2", "/attendance/corrections"));
        actions.add(buildWorkbenchQuickAction("加班调休", "lucide:timer-reset", "/attendance/overtime"));
        actions.add(buildWorkbenchQuickAction("请假管理", "lucide:calendar-minus", "/administrative/leave"));
        actions.add(buildWorkbenchQuickAction("出差管理", "lucide:briefcase-business", "/administrative/trip"));
        actions.add(buildWorkbenchQuickAction("人事待办", "lucide:list-checks", "/hr/todo"));
        return actions;
    }

    private AttendanceWorkbenchRespVO.QuickAction buildWorkbenchQuickAction(String title, String icon, String path) {
        AttendanceWorkbenchRespVO.QuickAction action = new AttendanceWorkbenchRespVO.QuickAction();
        action.setTitle(title);
        action.setIcon(icon);
        action.setPath(path);
        return action;
    }

    private boolean canQueryAllClockRecords() {
        try {
            return securityFrameworkService.hasPermission(PERMISSION_QUERY_ALL)
                    || securityFrameworkService.hasPermission(PERMISSION_EXCEPTION_HANDLE)
                    || securityFrameworkService.hasPermission(PERMISSION_CORRECTION_APPROVE)
                    || securityFrameworkService.hasPermission(PERMISSION_OVERTIME_APPROVE)
                    || securityFrameworkService.hasPermission(PERMISSION_SETTLEMENT_LOCK);
        } catch (Exception ex) {
            log.warn("check attendance query-all permission failed: {}", ex.getMessage());
            return false;
        }
    }

    private boolean canManageMonthlySettlement() {
        try {
            return securityFrameworkService.hasPermission(PERMISSION_SETTLEMENT_LOCK);
        } catch (Exception ex) {
            log.warn("check attendance settlement permission failed: {}", ex.getMessage());
            return false;
        }
    }

    private LocalDateTime normalizeClockTime(LocalDateTime clockTime) {
        if (clockTime == null) {
            return null;
        }
        return clockTime.withNano(0);
    }

    private String preferIncoming(String incoming, String existing) {
        return StringUtils.hasText(incoming) ? incoming : existing;
    }

    private boolean isAutoSyncRemark(String remark) {
        return StringUtils.hasText(remark) && remark.toLowerCase().contains("auto sync");
    }

    private Map<Long, AdminUserRespDTO> loadUserMapSafe(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return adminUserApi.getUserMap(userIds);
        } catch (Exception ex) {
            log.warn("Failed to load admin users for attendance page: {}", ex.getMessage());
            return new HashMap<>();
        }
    }

    private Map<Long, EmployeeProfileDO> loadProfileMapSafe(Set<Long> profileIds) {
        Map<Long, EmployeeProfileDO> profileMap = new HashMap<>();
        if (profileIds == null || profileIds.isEmpty()) {
            return profileMap;
        }
        try {
            List<EmployeeProfileDO> profiles = employeeProfileMapper.selectBatchIds(profileIds);
            if (profiles == null) {
                return profileMap;
            }
            for (EmployeeProfileDO profile : profiles) {
                if (profile != null && profile.getId() != null) {
                    profileMap.put(profile.getId(), profile);
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to load employee profiles for attendance page: {}", ex.getMessage());
        }
        return profileMap;
    }

    private String extractDingUserIdFromSourceRecordId(String sourceRecordId) {
        if (!StringUtils.hasText(sourceRecordId)) {
            return null;
        }
        String value = sourceRecordId.trim();
        String prefix = "dingtalk-";
        if (!value.startsWith(prefix)) {
            return null;
        }
        String tail = value.substring(prefix.length());
        int lastDash = tail.lastIndexOf('-');
        if (lastDash <= 0) {
            return tail;
        }
        return tail.substring(0, lastDash);
    }

    private void applyLeaveCoverage(AttendanceClockRecordRespVO item, AttendanceClockRecordDO record,
                                    Map<String, LeaveCoverage> leaveCoverageMap) {
        item.setLeaveCovered(false);
        item.setLeaveMinutes(0);
        if (record == null || leaveCoverageMap == null || leaveCoverageMap.isEmpty()) {
            return;
        }
        LeaveCoverage leaveCoverage = leaveCoverageMap.get(buildAttendanceDayKey(
                record.getUserId(), record.getAttendanceDate()));
        if (leaveCoverage == null || leaveCoverage.minutes <= 0) {
            return;
        }
        item.setLeaveMinutes(leaveCoverage.minutes);
        if (isClockStatusLeaveCovered(record, leaveCoverage)) {
            item.setLeaveCovered(true);
            item.setClockStatus(STATUS_LEAVE_COVERED);
        }
    }

    private Map<String, LeaveCoverage> loadLeaveCoverageMap(List<AttendanceClockRecordDO> records) {
        Map<String, LeaveCoverage> result = new HashMap<>();
        if (records == null || records.isEmpty()) {
            return result;
        }
        Set<Long> userIds = new HashSet<>();
        LocalDate startDate = null;
        LocalDate endDate = null;
        for (AttendanceClockRecordDO record : records) {
            if (record == null || record.getUserId() == null || record.getAttendanceDate() == null) {
                continue;
            }
            userIds.add(record.getUserId());
            startDate = startDate == null ? record.getAttendanceDate() : minDate(startDate, record.getAttendanceDate());
            endDate = endDate == null ? record.getAttendanceDate() : maxDate(endDate, record.getAttendanceDate());
        }
        if (userIds.isEmpty() || startDate == null || endDate == null) {
            return result;
        }

        List<HrAdministrativeLeaveDO> leaves = hrAdministrativeLeaveMapper.selectList(
                new LambdaQueryWrapper<HrAdministrativeLeaveDO>()
                        .in(HrAdministrativeLeaveDO::getUserId, userIds)
                        .eq(HrAdministrativeLeaveDO::getStatus, BPM_STATUS_APPROVE)
                        .le(HrAdministrativeLeaveDO::getStartTime, endDate.plusDays(1).atStartOfDay())
                        .ge(HrAdministrativeLeaveDO::getEndTime, startDate.atStartOfDay()));
        if (leaves == null || leaves.isEmpty()) {
            return result;
        }
        for (HrAdministrativeLeaveDO leave : leaves) {
            mergeLeaveCoverage(result, startDate, endDate, leave);
        }
        return result;
    }

    private void mergeLeaveCoverage(Map<String, LeaveCoverage> target, LocalDate rangeStart, LocalDate rangeEnd,
                                    HrAdministrativeLeaveDO leave) {
        if (target == null || leave == null || leave.getUserId() == null
                || leave.getStartTime() == null || leave.getEndTime() == null
                || !leave.getEndTime().isAfter(leave.getStartTime())) {
            return;
        }
        LocalDate effectiveRangeEnd = rangeEnd;
        if (isDingTalkAutoLeave(leave)) {
            effectiveRangeEnd = minDate(effectiveRangeEnd, LocalDate.now().minusDays(1));
        }
        if (effectiveRangeEnd.isBefore(rangeStart)) {
            return;
        }

        LocalDate startDate = maxDate(rangeStart, leave.getStartTime().toLocalDate());
        LocalDate endDate = minDate(effectiveRangeEnd, leave.getEndTime().toLocalDate());
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
            LocalDateTime overlapStart = maxDateTime(leave.getStartTime(), dayStart);
            LocalDateTime overlapEnd = minDateTime(leave.getEndTime(), dayEnd);
            if (!overlapEnd.isAfter(overlapStart)) {
                continue;
            }
            String key = buildAttendanceDayKey(leave.getUserId(), date);
            LeaveCoverage coverage = target.computeIfAbsent(key, ignored -> new LeaveCoverage());
            coverage.minutes += (int) Duration.between(overlapStart, overlapEnd).toMinutes();
            coverage.intervals.add(new LeaveInterval(overlapStart, overlapEnd));
        }
    }

    private boolean isClockStatusLeaveCovered(AttendanceClockRecordDO record, LeaveCoverage leaveCoverage) {
        if (record == null || leaveCoverage == null || leaveCoverage.minutes <= 0
                || record.getAttendanceDate() == null) {
            return false;
        }
        String status = record.getClockStatus();
        if (!StringUtils.hasText(status) || STATUS_NORMAL.equals(status)) {
            return false;
        }

        LocalDateTime workStart = record.getAttendanceDate().atTime(WORK_START_TIME);
        LocalDateTime workEnd = record.getAttendanceDate().atTime(WORK_END_TIME);
        if (STATUS_LATE.equals(status) && CLOCK_TYPE_IN.equals(record.getClockType())) {
            LocalDateTime clockTime = record.getClockTime();
            if (clockTime == null || !clockTime.isAfter(workStart)) {
                return false;
            }
            return isIntervalCovered(leaveCoverage, workStart, minDateTime(clockTime, workEnd));
        }
        if (STATUS_EARLY.equals(status) && CLOCK_TYPE_OUT.equals(record.getClockType())) {
            LocalDateTime clockTime = record.getClockTime();
            if (clockTime == null || !clockTime.isBefore(workEnd)) {
                return false;
            }
            return isIntervalCovered(leaveCoverage, maxDateTime(clockTime, workStart), workEnd);
        }
        if (STATUS_ABSENTEEISM.equals(status) || STATUS_UNKNOWN.equals(status)
                || CLOCK_TYPE_UNKNOWN.equals(record.getClockType())) {
            return calculateCoveredMinutes(leaveCoverage, workStart, workEnd) >= WORKDAY_MINUTES;
        }
        return false;
    }

    private boolean isIntervalCovered(LeaveCoverage leaveCoverage, LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            return false;
        }
        long requiredMinutes = Duration.between(startTime, endTime).toMinutes();
        return calculateCoveredMinutes(leaveCoverage, startTime, endTime) >= requiredMinutes;
    }

    private int calculateCoveredMinutes(LeaveCoverage leaveCoverage, LocalDateTime startTime, LocalDateTime endTime) {
        if (leaveCoverage == null || leaveCoverage.intervals.isEmpty()
                || startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            return 0;
        }
        List<LeaveInterval> overlaps = new ArrayList<>();
        for (LeaveInterval interval : leaveCoverage.intervals) {
            LocalDateTime overlapStart = maxDateTime(interval.startTime, startTime);
            LocalDateTime overlapEnd = minDateTime(interval.endTime, endTime);
            if (overlapEnd.isAfter(overlapStart)) {
                overlaps.add(new LeaveInterval(overlapStart, overlapEnd));
            }
        }
        if (overlaps.isEmpty()) {
            return 0;
        }
        overlaps.sort(Comparator.comparing(interval -> interval.startTime));
        int minutes = 0;
        LocalDateTime mergedStart = null;
        LocalDateTime mergedEnd = null;
        for (LeaveInterval interval : overlaps) {
            if (mergedStart == null) {
                mergedStart = interval.startTime;
                mergedEnd = interval.endTime;
                continue;
            }
            if (!interval.startTime.isAfter(mergedEnd)) {
                mergedEnd = maxDateTime(mergedEnd, interval.endTime);
                continue;
            }
            minutes += (int) Duration.between(mergedStart, mergedEnd).toMinutes();
            mergedStart = interval.startTime;
            mergedEnd = interval.endTime;
        }
        if (mergedStart != null && mergedEnd != null) {
            minutes += (int) Duration.between(mergedStart, mergedEnd).toMinutes();
        }
        return minutes;
    }

    private String buildAttendanceDayKey(Long userId, LocalDate date) {
        if (userId == null || date == null) {
            return "";
        }
        return userId + "|" + date;
    }

    private boolean isDingTalkAutoLeave(HrAdministrativeLeaveDO leave) {
        return leave != null
                && StringUtils.hasText(leave.getProcessInstanceId())
                && leave.getProcessInstanceId().startsWith(DINGTALK_LEAVE_PROCESS_INSTANCE_PREFIX);
    }

    private void fillTodayResp(AttendanceMyTodayRespVO respVO, List<AttendanceClockRecordDO> records) {
        AttendanceClockRecordDO firstIn = records.stream()
                .filter(record -> CLOCK_TYPE_IN.equals(record.getClockType()))
                .min(Comparator.comparing(AttendanceClockRecordDO::getClockTime))
                .orElse(null);
        AttendanceClockRecordDO lastOut = records.stream()
                .filter(record -> CLOCK_TYPE_OUT.equals(record.getClockType()))
                .max(Comparator.comparing(AttendanceClockRecordDO::getClockTime))
                .orElse(null);

        respVO.setHasClockIn(firstIn != null);
        respVO.setHasClockOut(lastOut != null);
        respVO.setFirstClockInTime(firstIn != null ? firstIn.getClockTime() : null);
        respVO.setLastClockOutTime(lastOut != null ? lastOut.getClockTime() : null);

        List<AttendanceMyTodayRespVO.RecordItem> items = new ArrayList<>();
        for (AttendanceClockRecordDO record : records) {
            AttendanceMyTodayRespVO.RecordItem item = new AttendanceMyTodayRespVO.RecordItem();
            item.setId(record.getId());
            item.setClockType(record.getClockType());
            item.setClockTime(record.getClockTime());
            item.setClockStatus(record.getClockStatus());
            item.setSourceType(record.getSourceType());
            item.setLocationName(record.getLocationName());
            item.setRemark(record.getRemark());
            items.add(item);
        }
        respVO.setRecords(items);
    }

    private AttendanceMyMonthDayRespVO buildMonthDayResp(LocalDate date, List<AttendanceClockRecordDO> records,
                                                         Integer leaveMinutes) {
        AttendanceClockRecordDO firstIn = records.stream()
                .filter(record -> CLOCK_TYPE_IN.equals(record.getClockType()))
                .min(Comparator.comparing(AttendanceClockRecordDO::getClockTime))
                .orElse(null);
        AttendanceClockRecordDO lastOut = records.stream()
                .filter(record -> CLOCK_TYPE_OUT.equals(record.getClockType()))
                .max(Comparator.comparing(AttendanceClockRecordDO::getClockTime))
                .orElse(null);

        boolean hasClockIn = firstIn != null;
        boolean hasClockOut = lastOut != null;

        AttendanceMyMonthDayRespVO respVO = new AttendanceMyMonthDayRespVO();
        respVO.setAttendanceDate(date);
        respVO.setRecordCount(records.size());
        respVO.setHasClockIn(hasClockIn);
        respVO.setHasClockOut(hasClockOut);
        respVO.setFirstClockInTime(hasClockIn ? firstIn.getClockTime() : null);
        respVO.setLastClockOutTime(hasClockOut ? lastOut.getClockTime() : null);
        respVO.setLeaveMinutes(leaveMinutes == null ? 0 : leaveMinutes);
        respVO.setDayStatus(resolveDayStatus(date, hasClockIn, hasClockOut, respVO.getLeaveMinutes()));
        return respVO;
    }

    private Map<LocalDate, Integer> loadLeaveMinutesByDate(Long userId, LocalDate startDate, LocalDate endDate) {
        return loadLocalApprovedLeaveMinutes(userId, startDate, endDate);
    }

    private Map<LocalDate, Integer> loadLocalApprovedLeaveMinutes(Long userId, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Integer> result = new HashMap<>();
        List<HrAdministrativeLeaveDO> leaves = hrAdministrativeLeaveMapper.selectList(
                new LambdaQueryWrapper<HrAdministrativeLeaveDO>()
                        .eq(HrAdministrativeLeaveDO::getUserId, userId)
                        .eq(HrAdministrativeLeaveDO::getStatus, BPM_STATUS_APPROVE)
                        .le(HrAdministrativeLeaveDO::getStartTime, endDate.plusDays(1).atStartOfDay())
                        .ge(HrAdministrativeLeaveDO::getEndTime, startDate.atStartOfDay()));
        if (leaves == null || leaves.isEmpty()) {
            return result;
        }
        for (HrAdministrativeLeaveDO leave : leaves) {
            LocalDate effectiveEndDate = endDate;
            if (isDingTalkAutoLeave(leave)) {
                effectiveEndDate = minDate(effectiveEndDate, LocalDate.now().minusDays(1));
            }
            if (!effectiveEndDate.isBefore(startDate)) {
                mergeLeaveMinutes(result, startDate, effectiveEndDate, leave.getStartTime(), leave.getEndTime());
            }
        }
        return result;
    }

    private void mergeLeaveMinutes(Map<LocalDate, Integer> target, LocalDate rangeStart, LocalDate rangeEnd,
                                   LocalDateTime leaveStart, LocalDateTime leaveEnd) {
        if (leaveStart == null || leaveEnd == null || !leaveEnd.isAfter(leaveStart)) {
            return;
        }
        LocalDate startDate = maxDate(rangeStart, leaveStart.toLocalDate());
        LocalDate endDate = minDate(rangeEnd, leaveEnd.toLocalDate());
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
            LocalDateTime overlapStart = maxDateTime(leaveStart, dayStart);
            LocalDateTime overlapEnd = minDateTime(leaveEnd, dayEnd);
            if (overlapEnd.isAfter(overlapStart)) {
                int minutes = (int) Duration.between(overlapStart, overlapEnd).toMinutes();
                target.merge(date, minutes, Integer::sum);
            }
        }
    }

    private LocalDate resolveMonth(Integer year, Integer month) {
        LocalDate now = LocalDate.now();
        int resolvedYear = year == null ? now.getYear() : year;
        int resolvedMonth = month == null ? now.getMonthValue() : month;
        if (resolvedMonth < 1 || resolvedMonth > 12) {
            return LocalDate.of(resolvedYear, now.getMonthValue(), 1);
        }
        return LocalDate.of(resolvedYear, resolvedMonth, 1);
    }

    private String normalizeClockType(String clockType) {
        String normalized = clockType == null ? "" : clockType.trim().toUpperCase();
        if (!CLOCK_TYPE_IN.equals(normalized) && !CLOCK_TYPE_OUT.equals(normalized)) {
            throw ServiceExceptionUtil.exception(ATTENDANCE_CLOCK_TYPE_INVALID);
        }
        return normalized;
    }

    private String normalizeSyncClockType(String clockType) {
        if (!StringUtils.hasText(clockType)) {
            return CLOCK_TYPE_UNKNOWN;
        }
        String normalized = clockType.trim().toUpperCase();
        if (CLOCK_TYPE_IN.equals(normalized) || CLOCK_TYPE_OUT.equals(normalized) || CLOCK_TYPE_UNKNOWN.equals(normalized)) {
            return normalized;
        }
        return CLOCK_TYPE_UNKNOWN;
    }

    private String resolveClockStatus(String clockType, LocalTime clockTime) {
        if (CLOCK_TYPE_IN.equals(clockType) && clockTime.isAfter(WORK_START_TIME)) {
            return STATUS_LATE;
        }
        if (CLOCK_TYPE_OUT.equals(clockType) && clockTime.isBefore(WORK_END_TIME)) {
            return STATUS_EARLY;
        }
        return STATUS_NORMAL;
    }

    private String resolveDayStatus(LocalDate date, boolean hasClockIn, boolean hasClockOut, Integer leaveMinutes) {
        if (leaveMinutes != null && leaveMinutes > 0) {
            return "LEAVE";
        }
        if (hasClockIn && hasClockOut) {
            return "COMPLETE";
        }
        if (hasClockIn && date != null && date.equals(LocalDate.now()) && LocalTime.now().isBefore(WORK_END_TIME)) {
            return "NONE";
        }
        if (hasClockIn) {
            return "MISSING_OUT";
        }
        if (hasClockOut) {
            return "MISSING_IN";
        }
        return "NONE";
    }

    private LocalDate maxDate(LocalDate left, LocalDate right) {
        return left.isAfter(right) ? left : right;
    }

    private LocalDate minDate(LocalDate left, LocalDate right) {
        return left.isBefore(right) ? left : right;
    }

    private LocalDateTime maxDateTime(LocalDateTime left, LocalDateTime right) {
        return left.isAfter(right) ? left : right;
    }

    private LocalDateTime minDateTime(LocalDateTime left, LocalDateTime right) {
        return left.isBefore(right) ? left : right;
    }

    private static class LeaveCoverage {
        private int minutes;
        private final List<LeaveInterval> intervals = new ArrayList<>();
    }

    private static class LeaveInterval {
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;

        private LeaveInterval(LocalDateTime startTime, LocalDateTime endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

}
