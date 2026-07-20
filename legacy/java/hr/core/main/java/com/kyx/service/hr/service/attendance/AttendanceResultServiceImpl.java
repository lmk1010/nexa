package com.kyx.service.hr.service.attendance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.kyx.foundation.common.exception.enums.GlobalErrorCodeConstants;
import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.foundation.security.core.service.SecurityFrameworkService;
import com.kyx.foundation.security.core.util.SecurityFrameworkUtils;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceCalculateReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceDailyResultPageReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceDailyResultRespVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceDailyResultSummaryRespVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceExceptionBatchResolveReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceExceptionPageReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceExceptionResolveReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceExceptionRespVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceExceptionSummaryRespVO;
import com.kyx.service.hr.dal.dataobject.administrative.HrAdministrativeLeaveDO;
import com.kyx.service.hr.dal.dataobject.administrative.HrAdministrativeTripDO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceClockRecordDO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceDailyResultDO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceExceptionDO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceGroupDO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceShiftRuleDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeProfileDO;
import com.kyx.service.hr.dal.mysql.administrative.HrAdministrativeLeaveMapper;
import com.kyx.service.hr.dal.mysql.administrative.HrAdministrativeTripMapper;
import com.kyx.service.hr.dal.mysql.attendance.AttendanceClockRecordMapper;
import com.kyx.service.hr.dal.mysql.attendance.AttendanceDailyResultMapper;
import com.kyx.service.hr.dal.mysql.attendance.AttendanceExceptionMapper;
import com.kyx.service.hr.dal.mysql.attendance.AttendanceGroupMapper;
import com.kyx.service.hr.dal.mysql.attendance.AttendanceShiftRuleMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeProfileMapper;
import com.kyx.service.hr.service.todo.HrTodoTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

/**
 * 考勤结果与异常 Service 实现
 */
@Service
@Validated
@Slf4j
public class AttendanceResultServiceImpl implements AttendanceResultService {

    private static final String CLOCK_TYPE_IN = "IN";
    private static final String CLOCK_TYPE_OUT = "OUT";
    private static final String RESULT_NORMAL = "NORMAL";
    private static final String RESULT_LEAVE = "LEAVE";
    private static final String RESULT_TRIP = "TRIP";
    private static final String RESULT_LATE = "LATE";
    private static final String RESULT_EARLY = "EARLY";
    private static final String RESULT_LATE_EARLY = "LATE_EARLY";
    private static final String RESULT_MISSING_IN = "MISSING_IN";
    private static final String RESULT_MISSING_OUT = "MISSING_OUT";
    private static final String RESULT_MISSING_BOTH = "MISSING_BOTH";
    private static final String RESULT_ABSENTEEISM = "ABSENTEEISM";
    private static final String EXCEPTION_LATE = "LATE";
    private static final String EXCEPTION_EARLY = "EARLY";
    private static final String EXCEPTION_MISSING_IN = "MISSING_IN";
    private static final String EXCEPTION_MISSING_OUT = "MISSING_OUT";
    private static final String EXCEPTION_ABSENTEEISM = "ABSENTEEISM";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_RESOLVED = "RESOLVED";
    private static final String STATUS_IGNORED = "IGNORED";
    private static final String PERMISSION_QUERY_ALL = "attendance:clock:query-all";
    private static final String PERMISSION_EXCEPTION_HANDLE = "attendance:exception:handle";
    private static final String PERMISSION_SETTLEMENT_LOCK = "attendance:settlement:lock";
    private static final String SCOPE_ALL = "ALL";
    private static final String SCOPE_USER = "USER";
    private static final String SCOPE_PROFILE = "PROFILE";
    private static final int BPM_STATUS_APPROVE = 2;
    private static final int WORKDAY_MINUTES = 480;
    private static final LocalTime WORK_START_TIME = LocalTime.of(9, 0);
    private static final LocalTime WORK_END_TIME = LocalTime.of(18, 0);

    @Resource
    private AttendanceDailyResultMapper attendanceDailyResultMapper;
    @Resource
    private AttendanceExceptionMapper attendanceExceptionMapper;
    @Resource
    private AttendanceClockRecordMapper attendanceClockRecordMapper;
    @Resource
    private AttendanceShiftRuleMapper attendanceShiftRuleMapper;
    @Resource
    private AttendanceGroupMapper attendanceGroupMapper;
    @Resource
    private HrAdministrativeLeaveMapper hrAdministrativeLeaveMapper;
    @Resource
    private HrAdministrativeTripMapper hrAdministrativeTripMapper;
    @Resource
    private EmployeeProfileMapper employeeProfileMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private SecurityFrameworkService securityFrameworkService;
    @Resource
    private HrTodoTaskService hrTodoTaskService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer calculateDay(AttendanceCalculateReqVO reqVO) {
        LocalDate attendanceDate = reqVO.getAttendanceDate() == null ? LocalDate.now() : reqVO.getAttendanceDate();
        Integer count = calculateRange(attendanceDate, attendanceDate, reqVO);
        refreshTodoTasksQuietly();
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer calculateMonth(AttendanceCalculateReqVO reqVO) {
        LocalDate month = resolveMonth(reqVO.getYear(), reqVO.getMonth());
        Integer count = calculateRange(month.withDayOfMonth(1), month.with(TemporalAdjusters.lastDayOfMonth()), reqVO);
        refreshTodoTasksQuietly();
        return count;
    }

    @Override
    public PageResult<AttendanceDailyResultRespVO> getDailyResultPage(AttendanceDailyResultPageReqVO pageReqVO) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            return new PageResult<>(new ArrayList<>(), 0L);
        }
        if (!canQueryAll()) {
            pageReqVO.setUserId(loginUserId);
            pageReqVO.setProfileId(null);
        }
        PageResult<AttendanceDailyResultDO> pageResult = attendanceDailyResultMapper.selectPage(pageReqVO);
        List<AttendanceDailyResultDO> records = pageResult.getList();
        if (records == null || records.isEmpty()) {
            return new PageResult<>(new ArrayList<>(), pageResult.getTotal());
        }
        List<AttendanceDailyResultRespVO> respList = BeanUtils.toBean(records, AttendanceDailyResultRespVO.class);
        fillPeopleInfo(records, respList, null);
        return new PageResult<>(respList, pageResult.getTotal());
    }

    @Override
    public AttendanceDailyResultSummaryRespVO getDailyResultSummary(AttendanceDailyResultPageReqVO pageReqVO) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            return new AttendanceDailyResultSummaryRespVO();
        }
        if (!canQueryAll()) {
            pageReqVO.setUserId(loginUserId);
            pageReqVO.setProfileId(null);
        }
        List<AttendanceDailyResultDO> records = attendanceDailyResultMapper.selectListByReqVO(pageReqVO);
        return buildDailyResultSummary(records);
    }

    @Override
    public PageResult<AttendanceExceptionRespVO> getExceptionPage(AttendanceExceptionPageReqVO pageReqVO) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            return new PageResult<>(new ArrayList<>(), 0L);
        }
        if (!canQueryAll()) {
            pageReqVO.setUserId(loginUserId);
            pageReqVO.setProfileId(null);
        }
        PageResult<AttendanceExceptionDO> pageResult = attendanceExceptionMapper.selectPage(pageReqVO);
        List<AttendanceExceptionDO> records = pageResult.getList();
        if (records == null || records.isEmpty()) {
            return new PageResult<>(new ArrayList<>(), pageResult.getTotal());
        }
        List<AttendanceExceptionRespVO> respList = BeanUtils.toBean(records, AttendanceExceptionRespVO.class);
        fillPeopleInfo(null, null, respList);
        return new PageResult<>(respList, pageResult.getTotal());
    }

    @Override
    public AttendanceExceptionSummaryRespVO getExceptionSummary(AttendanceExceptionPageReqVO pageReqVO) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            return new AttendanceExceptionSummaryRespVO();
        }
        if (!canQueryAll()) {
            pageReqVO.setUserId(loginUserId);
            pageReqVO.setProfileId(null);
        }
        List<AttendanceExceptionDO> records = attendanceExceptionMapper.selectListByReqVO(pageReqVO);
        return buildExceptionSummary(records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean resolveException(AttendanceExceptionResolveReqVO reqVO) {
        AttendanceExceptionDO exception = attendanceExceptionMapper.selectById(reqVO.getId());
        if (exception == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND);
        }
        if (!canQueryAll()) {
            Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
            if (loginUserId == null || !loginUserId.equals(exception.getUserId())) {
                throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.FORBIDDEN, "无权处理该考勤异常");
            }
        }
        updateExceptionStatus(exception, normalizeExceptionStatus(reqVO.getExceptionStatus()), reqVO.getHandleRemark());
        refreshTodoTasksQuietly();
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer batchResolveException(AttendanceExceptionBatchResolveReqVO reqVO) {
        if (reqVO.getIds() == null || reqVO.getIds().isEmpty()) {
            return 0;
        }
        String targetStatus = normalizeExceptionStatus(reqVO.getExceptionStatus());
        int changed = 0;
        for (Long id : reqVO.getIds()) {
            if (id == null) {
                continue;
            }
            AttendanceExceptionDO exception = attendanceExceptionMapper.selectById(id);
            if (exception == null) {
                continue;
            }
            if (!canQueryAll()) {
                Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
                if (loginUserId == null || !loginUserId.equals(exception.getUserId())) {
                    continue;
                }
            }
            updateExceptionStatus(exception, targetStatus, reqVO.getHandleRemark());
            changed++;
        }
        if (changed > 0) {
            refreshTodoTasksQuietly();
        }
        return changed;
    }

    private Integer calculateRange(LocalDate startDate, LocalDate endDate, AttendanceCalculateReqVO reqVO) {
        boolean queryAll = canQueryAll();
        List<EmployeeProfileDO> profiles = loadCalculateProfiles(queryAll, reqVO);
        if (profiles.isEmpty()) {
            return 0;
        }
        Set<Long> userIds = new HashSet<>();
        for (EmployeeProfileDO profile : profiles) {
            if (profile.getUserId() != null) {
                userIds.add(profile.getUserId());
            }
        }
        if (userIds.isEmpty()) {
            return 0;
        }
        Map<String, List<AttendanceClockRecordDO>> recordsMap = groupClockRecords(userIds, startDate, endDate);
        Map<String, List<HrAdministrativeLeaveDO>> leavesMap = groupLeaves(userIds, startDate, endDate);
        Map<String, List<HrAdministrativeTripDO>> tripsMap = groupTrips(userIds, startDate, endDate);
        AttendanceRuleContext ruleContext = loadAttendanceRuleContext();

        int count = 0;
        for (EmployeeProfileDO profile : profiles) {
            if (profile.getUserId() == null) {
                continue;
            }
            WorkShift workShift = resolveWorkShift(ruleContext, profile);
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                calculateOne(profile, date, workShift,
                        recordsMap.get(buildKey(profile.getUserId(), date)),
                        leavesMap.get(buildKey(profile.getUserId(), date)),
                        tripsMap.get(buildKey(profile.getUserId(), date)));
                count++;
            }
        }
        return count;
    }

    private List<EmployeeProfileDO> loadCalculateProfiles(boolean queryAll, AttendanceCalculateReqVO reqVO) {
        if (!queryAll) {
            Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
            if (loginUserId == null) {
                return new ArrayList<>();
            }
            if (reqVO.getUserId() != null && !loginUserId.equals(reqVO.getUserId())) {
                return new ArrayList<>();
            }
            EmployeeProfileDO profile = employeeProfileMapper.selectByUserId(loginUserId);
            if (profile == null || (reqVO.getProfileId() != null && !reqVO.getProfileId().equals(profile.getId()))) {
                return new ArrayList<>();
            }
            List<EmployeeProfileDO> result = new ArrayList<>();
            result.add(profile);
            return result;
        }
        return employeeProfileMapper.selectList(new LambdaQueryWrapperX<EmployeeProfileDO>()
                .eq(EmployeeProfileDO::getStatus, 1)
                .eqIfPresent(EmployeeProfileDO::getUserId, reqVO.getUserId())
                .eqIfPresent(EmployeeProfileDO::getId, reqVO.getProfileId())
                .orderByAsc(EmployeeProfileDO::getId));
    }

    private Map<String, List<AttendanceClockRecordDO>> groupClockRecords(Set<Long> userIds,
                                                                         LocalDate startDate,
                                                                         LocalDate endDate) {
        Map<String, List<AttendanceClockRecordDO>> result = new HashMap<>();
        List<AttendanceClockRecordDO> records = attendanceClockRecordMapper.selectList(
                new LambdaQueryWrapper<AttendanceClockRecordDO>()
                        .in(AttendanceClockRecordDO::getUserId, userIds)
                        .between(AttendanceClockRecordDO::getAttendanceDate, startDate, endDate)
                        .orderByAsc(AttendanceClockRecordDO::getClockTime));
        if (records == null) {
            return result;
        }
        for (AttendanceClockRecordDO record : records) {
            if (record == null || record.getUserId() == null || record.getAttendanceDate() == null) {
                continue;
            }
            result.computeIfAbsent(buildKey(record.getUserId(), record.getAttendanceDate()), ignored -> new ArrayList<>())
                    .add(record);
        }
        return result;
    }

    private Map<String, List<HrAdministrativeLeaveDO>> groupLeaves(Set<Long> userIds,
                                                                   LocalDate startDate,
                                                                   LocalDate endDate) {
        Map<String, List<HrAdministrativeLeaveDO>> result = new HashMap<>();
        List<HrAdministrativeLeaveDO> leaves = hrAdministrativeLeaveMapper.selectList(
                new LambdaQueryWrapper<HrAdministrativeLeaveDO>()
                        .in(HrAdministrativeLeaveDO::getUserId, userIds)
                        .eq(HrAdministrativeLeaveDO::getStatus, BPM_STATUS_APPROVE)
                        .le(HrAdministrativeLeaveDO::getStartTime, endDate.plusDays(1).atStartOfDay())
                        .ge(HrAdministrativeLeaveDO::getEndTime, startDate.atStartOfDay())
                        .orderByAsc(HrAdministrativeLeaveDO::getStartTime));
        if (leaves == null) {
            return result;
        }
        for (HrAdministrativeLeaveDO leave : leaves) {
            mergeRangeItem(result, leave.getUserId(), leave.getStartTime(), leave.getEndTime(), startDate, endDate, leave);
        }
        return result;
    }

    private Map<String, List<HrAdministrativeTripDO>> groupTrips(Set<Long> userIds,
                                                                 LocalDate startDate,
                                                                 LocalDate endDate) {
        Map<String, List<HrAdministrativeTripDO>> result = new HashMap<>();
        List<HrAdministrativeTripDO> trips = hrAdministrativeTripMapper.selectList(
                new LambdaQueryWrapper<HrAdministrativeTripDO>()
                        .in(HrAdministrativeTripDO::getUserId, userIds)
                        .eq(HrAdministrativeTripDO::getStatus, BPM_STATUS_APPROVE)
                        .le(HrAdministrativeTripDO::getStartTime, endDate.plusDays(1).atStartOfDay())
                        .ge(HrAdministrativeTripDO::getEndTime, startDate.atStartOfDay())
                        .orderByAsc(HrAdministrativeTripDO::getStartTime));
        if (trips == null) {
            return result;
        }
        for (HrAdministrativeTripDO trip : trips) {
            mergeRangeItem(result, trip.getUserId(), trip.getStartTime(), trip.getEndTime(), startDate, endDate, trip);
        }
        return result;
    }

    private <T> void mergeRangeItem(Map<String, List<T>> target, Long userId, LocalDateTime itemStart,
                                    LocalDateTime itemEnd, LocalDate startDate, LocalDate endDate, T item) {
        if (userId == null || itemStart == null || itemEnd == null || !itemEnd.isAfter(itemStart)) {
            return;
        }
        LocalDate start = maxDate(startDate, itemStart.toLocalDate());
        LocalDate end = minDate(endDate, itemEnd.toLocalDate());
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            target.computeIfAbsent(buildKey(userId, date), ignored -> new ArrayList<>()).add(item);
        }
    }

    private void calculateOne(EmployeeProfileDO profile, LocalDate date, WorkShift workShift,
                              List<AttendanceClockRecordDO> records,
                              List<HrAdministrativeLeaveDO> leaves, List<HrAdministrativeTripDO> trips) {
        List<AttendanceClockRecordDO> dayRecords = records == null ? new ArrayList<>() : records;
        List<HrAdministrativeLeaveDO> dayLeaves = leaves == null ? new ArrayList<>() : leaves;
        List<HrAdministrativeTripDO> dayTrips = trips == null ? new ArrayList<>() : trips;
        LocalDateTime workStart = date.atTime(workShift.startTime);
        LocalDateTime workEnd = date.atTime(workShift.endTime);
        List<TimeInterval> workIntervals = buildWorkIntervals(date, workShift);
        AttendanceClockRecordDO firstIn = dayRecords.stream()
                .filter(record -> CLOCK_TYPE_IN.equals(record.getClockType()))
                .min(Comparator.comparing(AttendanceClockRecordDO::getClockTime))
                .orElse(null);
        AttendanceClockRecordDO lastOut = dayRecords.stream()
                .filter(record -> CLOCK_TYPE_OUT.equals(record.getClockType()))
                .max(Comparator.comparing(AttendanceClockRecordDO::getClockTime))
                .orElse(null);

        List<TimeInterval> coverIntervals = new ArrayList<>();
        int leaveMinutes = collectLeaveMinutes(dayLeaves, workIntervals, coverIntervals, workShift.workdayMinutes);
        int tripMinutes = collectTripMinutes(dayTrips, workIntervals, coverIntervals, workShift.workdayMinutes);
        int coveredMinutes = Math.min(workShift.workdayMinutes, calculateCoveredMinutes(coverIntervals, workIntervals));

        DailyCalculation calculation = new DailyCalculation();
        calculation.lateMinutes = firstIn == null ? 0
                : calculateLateMinutes(firstIn.getClockTime(), workStart, workEnd, coverIntervals,
                workIntervals, workShift.lateGraceMinutes);
        calculation.earlyLeaveMinutes = lastOut == null ? 0
                : calculateEarlyLeaveMinutes(lastOut.getClockTime(), workStart, workEnd, coverIntervals,
                workIntervals, workShift.earlyLeaveGraceMinutes);
        calculation.absentMinutes = 0;
        calculation.resultStatus = resolveResultStatus(firstIn, lastOut, coveredMinutes, leaveMinutes, tripMinutes,
                workShift.workdayMinutes, calculation);

        AttendanceDailyResultDO resultDO = buildDailyResult(profile, date, workStart, workEnd, firstIn, lastOut,
                leaveMinutes, tripMinutes, calculation, dayRecords, dayLeaves, dayTrips);
        AttendanceDailyResultDO existed = attendanceDailyResultMapper.selectByUserIdAndDate(profile.getUserId(), date);
        if (existed == null) {
            attendanceDailyResultMapper.insert(resultDO);
        } else {
            resultDO.setId(existed.getId());
            attendanceDailyResultMapper.updateById(resultDO);
        }
        reconcileExceptions(resultDO, calculation.exceptionReasons);
    }

    private int collectLeaveMinutes(List<HrAdministrativeLeaveDO> leaves, List<TimeInterval> workIntervals,
                                    List<TimeInterval> coverIntervals, int workdayMinutes) {
        int minutes = 0;
        for (HrAdministrativeLeaveDO leave : leaves) {
            for (TimeInterval workInterval : workIntervals) {
                TimeInterval overlap = overlapInterval(leave.getStartTime(), leave.getEndTime(), workInterval);
                if (overlap == null) {
                    continue;
                }
                minutes += overlap.minutes();
                coverIntervals.add(overlap);
            }
        }
        return Math.min(workdayMinutes, minutes);
    }

    private int collectTripMinutes(List<HrAdministrativeTripDO> trips, List<TimeInterval> workIntervals,
                                   List<TimeInterval> coverIntervals, int workdayMinutes) {
        int minutes = 0;
        for (HrAdministrativeTripDO trip : trips) {
            for (TimeInterval workInterval : workIntervals) {
                TimeInterval overlap = overlapInterval(trip.getStartTime(), trip.getEndTime(), workInterval);
                if (overlap == null) {
                    continue;
                }
                minutes += overlap.minutes();
                coverIntervals.add(overlap);
            }
        }
        return Math.min(workdayMinutes, minutes);
    }

    private String resolveResultStatus(AttendanceClockRecordDO firstIn, AttendanceClockRecordDO lastOut,
                                       int coveredMinutes, int leaveMinutes, int tripMinutes,
                                       int workdayMinutes,
                                       DailyCalculation calculation) {
        if (coveredMinutes >= workdayMinutes) {
            return leaveMinutes >= tripMinutes ? RESULT_LEAVE : RESULT_TRIP;
        }
        boolean hasIn = firstIn != null;
        boolean hasOut = lastOut != null;
        if (!hasIn && !hasOut) {
            calculation.absentMinutes = workdayMinutes - coveredMinutes;
            calculation.exceptionReasons.put(EXCEPTION_ABSENTEEISM,
                    "全天缺卡，扣除请假/出差覆盖后旷工 " + minutesToHours(calculation.absentMinutes) + " 小时");
            return RESULT_ABSENTEEISM;
        }
        if (!hasIn) {
            calculation.exceptionReasons.put(EXCEPTION_MISSING_IN, "缺上班卡");
        }
        if (!hasOut) {
            calculation.exceptionReasons.put(EXCEPTION_MISSING_OUT, "缺下班卡");
        }
        if (calculation.lateMinutes > 0) {
            calculation.exceptionReasons.put(EXCEPTION_LATE, "上班迟到 " + calculation.lateMinutes + " 分钟");
        }
        if (calculation.earlyLeaveMinutes > 0) {
            calculation.exceptionReasons.put(EXCEPTION_EARLY, "下班早退 " + calculation.earlyLeaveMinutes + " 分钟");
        }
        if (calculation.exceptionReasons.isEmpty()) {
            return RESULT_NORMAL;
        }
        if (!hasIn && !hasOut) {
            return RESULT_MISSING_BOTH;
        }
        if (!hasIn) {
            return RESULT_MISSING_IN;
        }
        if (!hasOut) {
            return RESULT_MISSING_OUT;
        }
        if (calculation.lateMinutes > 0 && calculation.earlyLeaveMinutes > 0) {
            return RESULT_LATE_EARLY;
        }
        if (calculation.lateMinutes > 0) {
            return RESULT_LATE;
        }
        return RESULT_EARLY;
    }

    private AttendanceDailyResultDO buildDailyResult(EmployeeProfileDO profile, LocalDate date,
                                                     LocalDateTime workStart, LocalDateTime workEnd,
                                                     AttendanceClockRecordDO firstIn, AttendanceClockRecordDO lastOut,
                                                     int leaveMinutes, int tripMinutes,
                                                     DailyCalculation calculation,
                                                     List<AttendanceClockRecordDO> records,
                                                     List<HrAdministrativeLeaveDO> leaves,
                                                     List<HrAdministrativeTripDO> trips) {
        AttendanceDailyResultDO resultDO = new AttendanceDailyResultDO();
        resultDO.setProfileId(profile.getId());
        resultDO.setUserId(profile.getUserId());
        resultDO.setAttendanceDate(date);
        resultDO.setExpectedStartTime(workStart);
        resultDO.setExpectedEndTime(workEnd);
        resultDO.setActualStartTime(firstIn == null ? null : firstIn.getClockTime());
        resultDO.setActualEndTime(lastOut == null ? null : lastOut.getClockTime());
        resultDO.setResultStatus(calculation.resultStatus);
        resultDO.setLateMinutes(calculation.lateMinutes);
        resultDO.setEarlyLeaveMinutes(calculation.earlyLeaveMinutes);
        resultDO.setAbsentHours(minutesToHours(calculation.absentMinutes));
        resultDO.setLeaveHours(minutesToHours(leaveMinutes));
        resultDO.setTripHours(minutesToHours(tripMinutes));
        resultDO.setSourceJson(buildSourceJson(records, leaves, trips));
        resultDO.setCalculatedTime(LocalDateTime.now());
        return resultDO;
    }

    private void reconcileExceptions(AttendanceDailyResultDO resultDO, Map<String, String> exceptionReasons) {
        List<AttendanceExceptionDO> existedList = resultDO.getId() == null ? new ArrayList<>()
                : attendanceExceptionMapper.selectListByDailyResultId(resultDO.getId());
        Map<String, AttendanceExceptionDO> existedMap = new HashMap<>();
        for (AttendanceExceptionDO exception : existedList) {
            existedMap.put(exception.getExceptionType(), exception);
        }
        for (Map.Entry<String, String> entry : exceptionReasons.entrySet()) {
            AttendanceExceptionDO existed = existedMap.get(entry.getKey());
            if (existed == null) {
                AttendanceExceptionDO createDO = new AttendanceExceptionDO();
                createDO.setDailyResultId(resultDO.getId());
                createDO.setProfileId(resultDO.getProfileId());
                createDO.setUserId(resultDO.getUserId());
                createDO.setAttendanceDate(resultDO.getAttendanceDate());
                createDO.setExceptionType(entry.getKey());
                createDO.setExceptionStatus(STATUS_PENDING);
                createDO.setReason(entry.getValue());
                attendanceExceptionMapper.insert(createDO);
                continue;
            }
            if (!STATUS_RESOLVED.equals(existed.getExceptionStatus())) {
                AttendanceExceptionDO updateDO = new AttendanceExceptionDO();
                updateDO.setId(existed.getId());
                updateDO.setProfileId(resultDO.getProfileId());
                updateDO.setUserId(resultDO.getUserId());
                updateDO.setAttendanceDate(resultDO.getAttendanceDate());
                updateDO.setExceptionStatus(STATUS_PENDING);
                updateDO.setReason(entry.getValue());
                attendanceExceptionMapper.updateById(updateDO);
            }
        }
        for (AttendanceExceptionDO existed : existedList) {
            if (!exceptionReasons.containsKey(existed.getExceptionType())
                    && STATUS_PENDING.equals(existed.getExceptionStatus())) {
                AttendanceExceptionDO updateDO = new AttendanceExceptionDO();
                updateDO.setId(existed.getId());
                updateDO.setExceptionStatus(STATUS_RESOLVED);
                updateDO.setHandledTime(LocalDateTime.now());
                updateDO.setHandleRemark("重新计算后自动关闭");
                attendanceExceptionMapper.updateById(updateDO);
            }
        }
    }

    private int calculateLateMinutes(LocalDateTime actualStart, LocalDateTime workStart,
                                     LocalDateTime workEnd, List<TimeInterval> coverIntervals,
                                     List<TimeInterval> workIntervals, int lateGraceMinutes) {
        LocalDateTime latestNormalStart = workStart.plusMinutes(lateGraceMinutes);
        if (actualStart == null || !actualStart.isAfter(latestNormalStart)) {
            return 0;
        }
        return calculateUncoveredMinutes(coverIntervals, workIntervals, workStart, minDateTime(actualStart, workEnd));
    }

    private int calculateEarlyLeaveMinutes(LocalDateTime actualEnd, LocalDateTime workStart,
                                           LocalDateTime workEnd, List<TimeInterval> coverIntervals,
                                           List<TimeInterval> workIntervals, int earlyLeaveGraceMinutes) {
        LocalDateTime earliestNormalEnd = workEnd.minusMinutes(earlyLeaveGraceMinutes);
        if (actualEnd == null || !actualEnd.isBefore(earliestNormalEnd)) {
            return 0;
        }
        return calculateUncoveredMinutes(coverIntervals, workIntervals, maxDateTime(actualEnd, workStart), workEnd);
    }

    private int calculateUncoveredMinutes(List<TimeInterval> coverIntervals, List<TimeInterval> workIntervals,
                                          LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            return 0;
        }
        int total = calculateWorkMinutes(workIntervals, startTime, endTime);
        int covered = calculateCoveredMinutes(coverIntervals, workIntervals, startTime, endTime);
        return Math.max(total - covered, 0);
    }

    private int calculateCoveredMinutes(List<TimeInterval> coverIntervals, List<TimeInterval> workIntervals) {
        if (workIntervals == null || workIntervals.isEmpty()) {
            return 0;
        }
        LocalDateTime startTime = workIntervals.get(0).startTime;
        LocalDateTime endTime = workIntervals.get(workIntervals.size() - 1).endTime;
        return calculateCoveredMinutes(coverIntervals, workIntervals, startTime, endTime);
    }

    private int calculateCoveredMinutes(List<TimeInterval> coverIntervals, List<TimeInterval> workIntervals,
                                        LocalDateTime startTime, LocalDateTime endTime) {
        if (coverIntervals == null || coverIntervals.isEmpty() || workIntervals == null || workIntervals.isEmpty()
                || startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            return 0;
        }
        List<TimeInterval> overlaps = new ArrayList<>();
        for (TimeInterval coverInterval : coverIntervals) {
            for (TimeInterval workInterval : workIntervals) {
                TimeInterval rangeOverlap = overlapInterval(startTime, endTime, workInterval);
                if (rangeOverlap == null) {
                    continue;
                }
                TimeInterval overlap = overlapInterval(coverInterval.startTime, coverInterval.endTime, rangeOverlap);
                if (overlap != null) {
                    overlaps.add(overlap);
                }
            }
        }
        if (overlaps.isEmpty()) {
            return 0;
        }
        overlaps.sort(Comparator.comparing(interval -> interval.startTime));
        int minutes = 0;
        LocalDateTime mergedStart = null;
        LocalDateTime mergedEnd = null;
        for (TimeInterval interval : overlaps) {
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

    private int calculateWorkMinutes(List<TimeInterval> workIntervals, LocalDateTime startTime, LocalDateTime endTime) {
        int minutes = 0;
        for (TimeInterval workInterval : workIntervals) {
            TimeInterval overlap = overlapInterval(startTime, endTime, workInterval);
            if (overlap != null) {
                minutes += overlap.minutes();
            }
        }
        return minutes;
    }

    private TimeInterval overlapInterval(LocalDateTime itemStart, LocalDateTime itemEnd, TimeInterval range) {
        if (itemStart == null || itemEnd == null || range == null || !itemEnd.isAfter(itemStart)) {
            return null;
        }
        LocalDateTime start = maxDateTime(itemStart, range.startTime);
        LocalDateTime end = minDateTime(itemEnd, range.endTime);
        return end.isAfter(start) ? new TimeInterval(start, end) : null;
    }

    private int overlapMinutes(LocalDateTime itemStart, LocalDateTime itemEnd,
                               LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        if (itemStart == null || itemEnd == null || !itemEnd.isAfter(itemStart)) {
            return 0;
        }
        LocalDateTime start = maxDateTime(itemStart, rangeStart);
        LocalDateTime end = minDateTime(itemEnd, rangeEnd);
        return end.isAfter(start) ? (int) Duration.between(start, end).toMinutes() : 0;
    }

    private BigDecimal minutesToHours(int minutes) {
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private String buildSourceJson(List<AttendanceClockRecordDO> records,
                                   List<HrAdministrativeLeaveDO> leaves,
                                   List<HrAdministrativeTripDO> trips) {
        StringBuilder builder = new StringBuilder("{");
        builder.append("\"recordIds\":");
        appendIds(builder, records);
        builder.append(",\"leaveIds\":");
        appendIds(builder, leaves);
        builder.append(",\"tripIds\":");
        appendIds(builder, trips);
        builder.append('}');
        return builder.toString();
    }

    private void appendIds(StringBuilder builder, List<?> rows) {
        builder.append('[');
        boolean first = true;
        if (rows != null) {
            for (Object row : rows) {
                Long id = extractId(row);
                if (id == null) {
                    continue;
                }
                if (!first) {
                    builder.append(',');
                }
                builder.append(id);
                first = false;
            }
        }
        builder.append(']');
    }

    private Long extractId(Object row) {
        if (row instanceof AttendanceClockRecordDO) {
            return ((AttendanceClockRecordDO) row).getId();
        }
        if (row instanceof HrAdministrativeLeaveDO) {
            return ((HrAdministrativeLeaveDO) row).getId();
        }
        if (row instanceof HrAdministrativeTripDO) {
            return ((HrAdministrativeTripDO) row).getId();
        }
        return null;
    }

    private AttendanceRuleContext loadAttendanceRuleContext() {
        List<AttendanceShiftRuleDO> shifts = attendanceShiftRuleMapper.selectActiveList();
        Map<Long, AttendanceShiftRuleDO> shiftMap = new HashMap<>();
        AttendanceShiftRuleDO defaultShift = null;
        for (AttendanceShiftRuleDO shift : shifts) {
            if (shift == null || shift.getId() == null) {
                continue;
            }
            shiftMap.put(shift.getId(), shift);
            if (defaultShift == null || Boolean.TRUE.equals(shift.getDefaultFlag())) {
                defaultShift = shift;
            }
        }
        AttendanceRuleContext context = new AttendanceRuleContext();
        context.shiftMap = shiftMap;
        context.defaultShift = defaultShift;
        context.groups = attendanceGroupMapper.selectActiveList();
        return context;
    }

    private WorkShift resolveWorkShift(AttendanceRuleContext context, EmployeeProfileDO profile) {
        if (context != null && context.groups != null) {
            for (AttendanceGroupDO group : context.groups) {
                if (!matchesAttendanceGroup(group, profile)) {
                    continue;
                }
                AttendanceShiftRuleDO shiftRule = context.shiftMap.get(group.getShiftRuleId());
                if (shiftRule != null) {
                    return WorkShift.from(shiftRule);
                }
            }
        }
        if (context != null && context.defaultShift != null) {
            return WorkShift.from(context.defaultShift);
        }
        return WorkShift.defaultShift();
    }

    private boolean matchesAttendanceGroup(AttendanceGroupDO group, EmployeeProfileDO profile) {
        if (group == null || profile == null) {
            return false;
        }
        String scopeType = StringUtils.hasText(group.getScopeType()) ? group.getScopeType().trim().toUpperCase() : SCOPE_ALL;
        if (SCOPE_ALL.equals(scopeType)) {
            return true;
        }
        if (SCOPE_USER.equals(scopeType)) {
            return containsScopeId(group.getScopeJson(), profile.getUserId());
        }
        if (SCOPE_PROFILE.equals(scopeType)) {
            return containsScopeId(group.getScopeJson(), profile.getId());
        }
        return false;
    }

    private boolean containsScopeId(String scopeJson, Long id) {
        if (id == null || !StringUtils.hasText(scopeJson)) {
            return false;
        }
        String target = String.valueOf(id);
        String[] tokens = scopeJson.replaceAll("[^0-9]+", ",").split(",");
        for (String token : tokens) {
            if (target.equals(token)) {
                return true;
            }
        }
        return false;
    }

    private List<TimeInterval> buildWorkIntervals(LocalDate date, WorkShift workShift) {
        List<TimeInterval> intervals = new ArrayList<>();
        LocalDateTime workStart = date.atTime(workShift.startTime);
        LocalDateTime workEnd = date.atTime(workShift.endTime);
        if (workShift.restStartTime == null || workShift.restEndTime == null
                || !workShift.restStartTime.isAfter(workShift.startTime)
                || !workShift.restEndTime.isBefore(workShift.endTime)
                || !workShift.restEndTime.isAfter(workShift.restStartTime)) {
            intervals.add(new TimeInterval(workStart, workEnd));
            return intervals;
        }
        intervals.add(new TimeInterval(workStart, date.atTime(workShift.restStartTime)));
        intervals.add(new TimeInterval(date.atTime(workShift.restEndTime), workEnd));
        return intervals;
    }

    private void fillPeopleInfo(List<AttendanceDailyResultDO> resultRows,
                                List<AttendanceDailyResultRespVO> resultRespList,
                                List<AttendanceExceptionRespVO> exceptionRespList) {
        Set<Long> userIds = new HashSet<>();
        Set<Long> profileIds = new HashSet<>();
        if (resultRows != null) {
            for (AttendanceDailyResultDO row : resultRows) {
                if (row.getUserId() != null) {
                    userIds.add(row.getUserId());
                }
                if (row.getProfileId() != null) {
                    profileIds.add(row.getProfileId());
                }
            }
        }
        if (exceptionRespList != null) {
            for (AttendanceExceptionRespVO row : exceptionRespList) {
                if (row.getUserId() != null) {
                    userIds.add(row.getUserId());
                }
                if (row.getHandlerId() != null) {
                    userIds.add(row.getHandlerId());
                }
                if (row.getProfileId() != null) {
                    profileIds.add(row.getProfileId());
                }
            }
        }
        Map<Long, AdminUserRespDTO> userMap = loadUserMapSafe(userIds);
        Map<Long, EmployeeProfileDO> profileMap = loadProfileMapSafe(profileIds);
        if (resultRespList != null) {
            for (AttendanceDailyResultRespVO row : resultRespList) {
                fillPeopleInfo(row, userMap, profileMap);
            }
        }
        if (exceptionRespList != null) {
            for (AttendanceExceptionRespVO row : exceptionRespList) {
                fillPeopleInfo(row, userMap, profileMap);
            }
        }
    }

    private void fillPeopleInfo(AttendanceDailyResultRespVO row, Map<Long, AdminUserRespDTO> userMap,
                                Map<Long, EmployeeProfileDO> profileMap) {
        AdminUserRespDTO user = userMap.get(row.getUserId());
        if (user != null) {
            row.setUserNickname(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
        }
        EmployeeProfileDO profile = profileMap.get(row.getProfileId());
        if (profile != null) {
            row.setProfileName(profile.getName());
        }
    }

    private void updateExceptionStatus(AttendanceExceptionDO exception, String targetStatus, String handleRemark) {
        if (STATUS_PENDING.equals(targetStatus)) {
            attendanceExceptionMapper.update(null, new LambdaUpdateWrapper<AttendanceExceptionDO>()
                    .eq(AttendanceExceptionDO::getId, exception.getId())
                    .set(AttendanceExceptionDO::getExceptionStatus, targetStatus)
                    .set(AttendanceExceptionDO::getHandleRemark, handleRemark)
                    .set(AttendanceExceptionDO::getHandlerId, null)
                    .set(AttendanceExceptionDO::getHandledTime, null));
        } else {
            AttendanceExceptionDO updateDO = new AttendanceExceptionDO();
            updateDO.setId(exception.getId());
            updateDO.setExceptionStatus(targetStatus);
            updateDO.setHandleRemark(handleRemark);
            updateDO.setHandlerId(SecurityFrameworkUtils.getLoginUserId());
            updateDO.setHandledTime(LocalDateTime.now());
            attendanceExceptionMapper.updateById(updateDO);
        }
    }

    private AttendanceExceptionSummaryRespVO buildExceptionSummary(List<AttendanceExceptionDO> records) {
        AttendanceExceptionSummaryRespVO summary = new AttendanceExceptionSummaryRespVO();
        if (records == null || records.isEmpty()) {
            return summary;
        }
        Map<String, Integer> typeStats = new LinkedHashMap<>();
        Map<String, Integer> statusStats = new LinkedHashMap<>();
        int pending = 0;
        int resolved = 0;
        int ignored = 0;
        for (AttendanceExceptionDO record : records) {
            if (record == null) {
                continue;
            }
            String type = StringUtils.hasText(record.getExceptionType()) ? record.getExceptionType() : "UNKNOWN";
            String status = StringUtils.hasText(record.getExceptionStatus()) ? record.getExceptionStatus() : STATUS_PENDING;
            typeStats.put(type, typeStats.getOrDefault(type, 0) + 1);
            statusStats.put(status, statusStats.getOrDefault(status, 0) + 1);
            if (STATUS_PENDING.equals(status)) {
                pending++;
            } else if (STATUS_RESOLVED.equals(status)) {
                resolved++;
            } else if (STATUS_IGNORED.equals(status)) {
                ignored++;
            }
        }
        int total = records.size();
        summary.setTotalCount(total);
        summary.setPendingCount(pending);
        summary.setResolvedCount(resolved);
        summary.setIgnoredCount(ignored);
        summary.setPendingRate(percent(pending, total));
        summary.setTypeStats(toStatItems(typeStats));
        summary.setStatusStats(toStatItems(statusStats));
        return summary;
    }

    private AttendanceDailyResultSummaryRespVO buildDailyResultSummary(List<AttendanceDailyResultDO> records) {
        AttendanceDailyResultSummaryRespVO summary = new AttendanceDailyResultSummaryRespVO();
        if (records == null || records.isEmpty()) {
            return summary;
        }
        int normal = 0;
        int leave = 0;
        int trip = 0;
        int abnormal = 0;
        int missing = 0;
        int absenteeism = 0;
        int late = 0;
        int early = 0;
        BigDecimal leaveHours = BigDecimal.ZERO;
        BigDecimal tripHours = BigDecimal.ZERO;
        BigDecimal absentHours = BigDecimal.ZERO;
        for (AttendanceDailyResultDO record : records) {
            if (record == null) {
                continue;
            }
            String status = record.getResultStatus();
            if (RESULT_NORMAL.equals(status)) {
                normal++;
            } else if (RESULT_LEAVE.equals(status)) {
                leave++;
            } else if (RESULT_TRIP.equals(status)) {
                trip++;
            } else {
                abnormal++;
            }
            if (RESULT_LATE.equals(status) || RESULT_LATE_EARLY.equals(status)) {
                late++;
            }
            if (RESULT_EARLY.equals(status) || RESULT_LATE_EARLY.equals(status)) {
                early++;
            }
            if (RESULT_MISSING_IN.equals(status) || RESULT_MISSING_OUT.equals(status)
                    || RESULT_MISSING_BOTH.equals(status)) {
                missing++;
            }
            if (RESULT_ABSENTEEISM.equals(status)) {
                absenteeism++;
            }
            leaveHours = leaveHours.add(record.getLeaveHours() == null ? BigDecimal.ZERO : record.getLeaveHours());
            tripHours = tripHours.add(record.getTripHours() == null ? BigDecimal.ZERO : record.getTripHours());
            absentHours = absentHours.add(record.getAbsentHours() == null ? BigDecimal.ZERO : record.getAbsentHours());
        }
        int total = records.size();
        summary.setTotalCount(total);
        summary.setNormalCount(normal);
        summary.setLeaveCount(leave);
        summary.setTripCount(trip);
        summary.setAbnormalCount(abnormal);
        summary.setMissingCount(missing);
        summary.setAbsenteeismCount(absenteeism);
        summary.setLateCount(late);
        summary.setEarlyCount(early);
        summary.setLeaveHours(leaveHours);
        summary.setTripHours(tripHours);
        summary.setAbsentHours(absentHours);
        summary.setAbnormalRate(percent(abnormal, total));
        return summary;
    }

    private List<AttendanceExceptionSummaryRespVO.StatItem> toStatItems(Map<String, Integer> source) {
        List<AttendanceExceptionSummaryRespVO.StatItem> items = new ArrayList<>();
        if (source == null || source.isEmpty()) {
            return items;
        }
        source.forEach((name, count) -> items.add(new AttendanceExceptionSummaryRespVO.StatItem(name, count)));
        return items;
    }

    private BigDecimal percent(int numerator, int denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private String normalizeExceptionStatus(String exceptionStatus) {
        if (!StringUtils.hasText(exceptionStatus)) {
            return STATUS_RESOLVED;
        }
        String value = exceptionStatus.trim().toUpperCase();
        if (STATUS_PENDING.equals(value) || STATUS_RESOLVED.equals(value) || STATUS_IGNORED.equals(value)) {
            return value;
        }
        throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "考勤异常处理状态不合法");
    }

    private void fillPeopleInfo(AttendanceExceptionRespVO row, Map<Long, AdminUserRespDTO> userMap,
                                Map<Long, EmployeeProfileDO> profileMap) {
        AdminUserRespDTO user = userMap.get(row.getUserId());
        if (user != null) {
            row.setUserNickname(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
        }
        AdminUserRespDTO handler = userMap.get(row.getHandlerId());
        if (handler != null) {
            row.setHandlerName(StringUtils.hasText(handler.getNickname()) ? handler.getNickname() : handler.getUsername());
        }
        EmployeeProfileDO profile = profileMap.get(row.getProfileId());
        if (profile != null) {
            row.setProfileName(profile.getName());
        }
    }

    private Map<Long, AdminUserRespDTO> loadUserMapSafe(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return adminUserApi.getUserMap(userIds);
        } catch (Exception ex) {
            log.warn("Failed to load admin users for attendance result: {}", ex.getMessage());
            return new HashMap<>();
        }
    }

    private Map<Long, EmployeeProfileDO> loadProfileMapSafe(Set<Long> profileIds) {
        Map<Long, EmployeeProfileDO> profileMap = new HashMap<>();
        if (profileIds == null || profileIds.isEmpty()) {
            return profileMap;
        }
        List<EmployeeProfileDO> profiles = employeeProfileMapper.selectBatchIds(profileIds);
        if (profiles == null) {
            return profileMap;
        }
        for (EmployeeProfileDO profile : profiles) {
            if (profile != null && profile.getId() != null) {
                profileMap.put(profile.getId(), profile);
            }
        }
        return profileMap;
    }

    private void refreshTodoTasksQuietly() {
        try {
            hrTodoTaskService.refreshGeneratedTasks();
        } catch (Exception ex) {
            log.warn("Refresh HR todo tasks failed after attendance update: {}", ex.getMessage());
        }
    }

    private boolean canQueryAll() {
        try {
            return securityFrameworkService.hasPermission(PERMISSION_QUERY_ALL)
                    || securityFrameworkService.hasPermission(PERMISSION_EXCEPTION_HANDLE)
                    || securityFrameworkService.hasPermission(PERMISSION_SETTLEMENT_LOCK);
        } catch (Exception ex) {
            log.warn("check attendance query-all permission failed: {}", ex.getMessage());
            return false;
        }
    }

    private LocalDate resolveMonth(Integer year, Integer month) {
        LocalDate now = LocalDate.now();
        int resolvedYear = year == null ? now.getYear() : year;
        int resolvedMonth = month == null ? now.getMonthValue() : month;
        if (resolvedMonth < 1 || resolvedMonth > 12) {
            resolvedMonth = now.getMonthValue();
        }
        return LocalDate.of(resolvedYear, resolvedMonth, 1);
    }

    private String buildKey(Long userId, LocalDate date) {
        return userId + "|" + date;
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

    private static class TimeInterval {
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;

        private TimeInterval(LocalDateTime startTime, LocalDateTime endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        private int minutes() {
            return (int) Duration.between(startTime, endTime).toMinutes();
        }
    }

    private static class DailyCalculation {
        private String resultStatus;
        private int lateMinutes;
        private int earlyLeaveMinutes;
        private int absentMinutes;
        private final Map<String, String> exceptionReasons = new LinkedHashMap<>();
    }

    private static class AttendanceRuleContext {
        private Map<Long, AttendanceShiftRuleDO> shiftMap = new HashMap<>();
        private AttendanceShiftRuleDO defaultShift;
        private List<AttendanceGroupDO> groups = new ArrayList<>();
    }

    private static class WorkShift {
        private final LocalTime startTime;
        private final LocalTime endTime;
        private final LocalTime restStartTime;
        private final LocalTime restEndTime;
        private final int lateGraceMinutes;
        private final int earlyLeaveGraceMinutes;
        private final int workdayMinutes;

        private WorkShift(LocalTime startTime, LocalTime endTime, LocalTime restStartTime, LocalTime restEndTime,
                          int lateGraceMinutes, int earlyLeaveGraceMinutes, int workdayMinutes) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.restStartTime = restStartTime;
            this.restEndTime = restEndTime;
            this.lateGraceMinutes = lateGraceMinutes;
            this.earlyLeaveGraceMinutes = earlyLeaveGraceMinutes;
            this.workdayMinutes = workdayMinutes;
        }

        private static WorkShift defaultShift() {
            return new WorkShift(WORK_START_TIME, WORK_END_TIME, null, null, 0, 0, WORKDAY_MINUTES);
        }

        private static WorkShift from(AttendanceShiftRuleDO shiftRule) {
            int minutes = WORKDAY_MINUTES;
            if (shiftRule.getWorkHours() != null && shiftRule.getWorkHours().compareTo(BigDecimal.ZERO) > 0) {
                minutes = shiftRule.getWorkHours().multiply(BigDecimal.valueOf(60))
                        .setScale(0, RoundingMode.HALF_UP).intValue();
            } else if (shiftRule.getStartTime() != null && shiftRule.getEndTime() != null
                    && shiftRule.getEndTime().isAfter(shiftRule.getStartTime())) {
                minutes = (int) Duration.between(shiftRule.getStartTime(), shiftRule.getEndTime()).toMinutes();
                if (shiftRule.getRestStartTime() != null && shiftRule.getRestEndTime() != null
                        && shiftRule.getRestEndTime().isAfter(shiftRule.getRestStartTime())) {
                    minutes -= (int) Duration.between(shiftRule.getRestStartTime(), shiftRule.getRestEndTime()).toMinutes();
                }
            }
            return new WorkShift(
                    shiftRule.getStartTime() == null ? WORK_START_TIME : shiftRule.getStartTime(),
                    shiftRule.getEndTime() == null ? WORK_END_TIME : shiftRule.getEndTime(),
                    shiftRule.getRestStartTime(),
                    shiftRule.getRestEndTime(),
                    shiftRule.getLateGraceMinutes() == null ? 0 : Math.max(shiftRule.getLateGraceMinutes(), 0),
                    shiftRule.getEarlyLeaveGraceMinutes() == null ? 0 : Math.max(shiftRule.getEarlyLeaveGraceMinutes(), 0),
                    Math.max(minutes, 0));
        }
    }

}
