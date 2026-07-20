package com.kyx.service.hr.service.attendance;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.kyx.foundation.common.exception.enums.GlobalErrorCodeConstants;
import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.foundation.security.core.util.SecurityFrameworkUtils;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceCalculateReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceDailyResultPageReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceDailyResultRespVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceExceptionPageReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceMonthlyConfirmActionReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceMonthlyConfirmDetailRespVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceMonthlyConfirmIssueReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceMonthlyConfirmPageReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceMonthlyConfirmResolveReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceMonthlyConfirmRespVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceMonthlySettlementGenerateReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceMonthlySettlementPageReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceMonthlySettlementRespVO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceDailyResultDO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceExceptionDO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceMonthlyConfirmDO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceMonthlySettlementDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeProfileDO;
import com.kyx.service.hr.dal.mysql.attendance.AttendanceDailyResultMapper;
import com.kyx.service.hr.dal.mysql.attendance.AttendanceExceptionMapper;
import com.kyx.service.hr.dal.mysql.attendance.AttendanceMonthlyConfirmMapper;
import com.kyx.service.hr.dal.mysql.attendance.AttendanceMonthlySettlementMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeProfileMapper;
import com.kyx.service.hr.service.todo.HrTodoTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Monthly attendance settlement service implementation.
 */
@Service
@Validated
@Slf4j
public class AttendanceMonthlySettlementServiceImpl implements AttendanceMonthlySettlementService {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final String STATUS_GENERATED = "GENERATED";
    private static final String STATUS_LOCKED = "LOCKED";

    private static final String CONFIRM_PENDING = "PENDING";
    private static final String CONFIRM_CONFIRMED = "CONFIRMED";
    private static final String CONFIRM_ISSUE = "ISSUE";
    private static final String CONFIRM_RESOLVED = "RESOLVED";

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

    private static final String EXCEPTION_PENDING = "PENDING";
    private static final String EXCEPTION_RESOLVED = "RESOLVED";
    private static final Long DEFAULT_DEPT_ID = 0L;

    @Resource
    private AttendanceMonthlySettlementMapper attendanceMonthlySettlementMapper;
    @Resource
    private AttendanceMonthlyConfirmMapper attendanceMonthlyConfirmMapper;
    @Resource
    private AttendanceDailyResultMapper attendanceDailyResultMapper;
    @Resource
    private AttendanceExceptionMapper attendanceExceptionMapper;
    @Resource
    private AttendanceResultService attendanceResultService;
    @Resource
    private EmployeeProfileMapper employeeProfileMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private HrTodoTaskService hrTodoTaskService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttendanceMonthlySettlementRespVO generate(AttendanceMonthlySettlementGenerateReqVO reqVO) {
        LocalDate month = resolveMonth(reqVO.getYear(), reqVO.getMonth());
        String settlementMonth = month.format(MONTH_FORMATTER);
        Long deptId = normalizeDeptId(reqVO.getDeptId());

        AttendanceMonthlySettlementDO existed = attendanceMonthlySettlementMapper.selectByMonthAndDept(settlementMonth, deptId);
        if (existed != null && STATUS_LOCKED.equals(existed.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "该月份考勤已锁定，不能重新生成");
        }

        AttendanceCalculateReqVO calculateReqVO = new AttendanceCalculateReqVO();
        calculateReqVO.setYear(month.getYear());
        calculateReqVO.setMonth(month.getMonthValue());
        attendanceResultService.calculateMonth(calculateReqVO);

        SummaryCounter summary = buildSummary(month);
        LocalDateTime now = LocalDateTime.now();
        AttendanceMonthlySettlementDO settlement = existed == null ? new AttendanceMonthlySettlementDO() : existed;
        settlement.setSettlementMonth(settlementMonth);
        settlement.setDeptId(deptId);
        settlement.setStatus(STATUS_GENERATED);
        settlement.setGeneratedTime(now);
        settlement.setSummaryJson(buildSummaryJson(settlementMonth, deptId, summary));
        if (existed == null) {
            attendanceMonthlySettlementMapper.insert(settlement);
        } else {
            attendanceMonthlySettlementMapper.updateById(settlement);
        }

        upsertConfirmRows(settlement, month);
        refreshSettlementSummary(settlement);
        refreshTodoTasksQuietly();
        return BeanUtils.toBean(attendanceMonthlySettlementMapper.selectById(settlement.getId()),
                AttendanceMonthlySettlementRespVO.class);
    }

    @Override
    public PageResult<AttendanceMonthlySettlementRespVO> getPage(AttendanceMonthlySettlementPageReqVO pageReqVO) {
        PageResult<AttendanceMonthlySettlementDO> pageResult = attendanceMonthlySettlementMapper.selectPage(pageReqVO);
        List<AttendanceMonthlySettlementRespVO> records = BeanUtils.toBean(pageResult.getList(),
                AttendanceMonthlySettlementRespVO.class);
        return new PageResult<>(emptyIfNull(records), pageResult.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean lock(Long id) {
        AttendanceMonthlySettlementDO settlement = getSettlement(id);
        if (STATUS_LOCKED.equals(settlement.getStatus())) {
            return true;
        }
        LocalDate month = parseSettlementMonth(settlement.getSettlementMonth());
        upsertConfirmRows(settlement, month);

        SummaryCounter summary = buildSummary(month);
        applyConfirmSummary(summary, id);
        if (summary.pendingExceptionCount > 0) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST,
                    "还有待处理考勤异常，处理后才能锁定月结");
        }
        if (summary.confirmPendingCount > 0 || summary.confirmIssueCount > 0) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST,
                    "还有员工未确认或存在异议，处理后才能锁定月结");
        }
        AttendanceMonthlySettlementDO updateDO = new AttendanceMonthlySettlementDO();
        updateDO.setId(id);
        updateDO.setStatus(STATUS_LOCKED);
        updateDO.setLockedTime(LocalDateTime.now());
        updateDO.setLockedBy(SecurityFrameworkUtils.getLoginUserId());
        updateDO.setSummaryJson(buildSummaryJson(settlement.getSettlementMonth(), normalizeDeptId(settlement.getDeptId()), summary));
        attendanceMonthlySettlementMapper.updateById(updateDO);
        refreshTodoTasksQuietly();
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean unlock(Long id) {
        AttendanceMonthlySettlementDO settlement = getSettlement(id);
        if (!STATUS_LOCKED.equals(settlement.getStatus())) {
            return true;
        }
        attendanceMonthlySettlementMapper.update(null, new LambdaUpdateWrapper<AttendanceMonthlySettlementDO>()
                .eq(AttendanceMonthlySettlementDO::getId, id)
                .set(AttendanceMonthlySettlementDO::getStatus, STATUS_GENERATED)
                .set(AttendanceMonthlySettlementDO::getLockedTime, null)
                .set(AttendanceMonthlySettlementDO::getLockedBy, null));
        refreshSettlementSummary(settlement);
        refreshTodoTasksQuietly();
        return true;
    }

    @Override
    public PageResult<AttendanceMonthlyConfirmRespVO> getConfirmPage(AttendanceMonthlyConfirmPageReqVO pageReqVO) {
        normalizeConfirmPageReq(pageReqVO);
        PageResult<AttendanceMonthlyConfirmDO> pageResult = attendanceMonthlyConfirmMapper.selectPage(pageReqVO);
        List<AttendanceMonthlyConfirmRespVO> records = BeanUtils.toBean(pageResult.getList(),
                AttendanceMonthlyConfirmRespVO.class);
        fillConfirmPeopleInfo(pageResult.getList(), records);
        return new PageResult<>(emptyIfNull(records), pageResult.getTotal());
    }

    @Override
    public PageResult<AttendanceMonthlyConfirmRespVO> getMyConfirmPage(AttendanceMonthlyConfirmPageReqVO pageReqVO) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            return new PageResult<>(new ArrayList<>(), 0L);
        }
        normalizeConfirmPageReq(pageReqVO);
        pageReqVO.setUserId(loginUserId);
        PageResult<AttendanceMonthlyConfirmDO> pageResult = attendanceMonthlyConfirmMapper.selectPage(pageReqVO);
        List<AttendanceMonthlyConfirmRespVO> records = BeanUtils.toBean(pageResult.getList(),
                AttendanceMonthlyConfirmRespVO.class);
        fillConfirmPeopleInfo(pageResult.getList(), records);
        return new PageResult<>(emptyIfNull(records), pageResult.getTotal());
    }

    @Override
    public AttendanceMonthlyConfirmDetailRespVO getConfirmDetail(Long id) {
        return buildConfirmDetail(getConfirm(id), false);
    }

    @Override
    public AttendanceMonthlyConfirmDetailRespVO getMyConfirmDetail(Long id) {
        return buildConfirmDetail(getConfirmOwnedByCurrentUser(id), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean confirmMy(AttendanceMonthlyConfirmActionReqVO reqVO) {
        AttendanceMonthlyConfirmDO confirm = getConfirmOwnedByCurrentUser(reqVO.getId());
        ensureSettlementEditable(confirm.getSettlementId());
        if (CONFIRM_CONFIRMED.equals(confirm.getStatus())) {
            return true;
        }
        if (CONFIRM_ISSUE.equals(confirm.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "异议处理中，需HR处理后再确认");
        }
        AttendanceMonthlyConfirmDO updateDO = new AttendanceMonthlyConfirmDO();
        updateDO.setId(confirm.getId());
        updateDO.setStatus(CONFIRM_CONFIRMED);
        updateDO.setConfirmedTime(LocalDateTime.now());
        attendanceMonthlyConfirmMapper.updateById(updateDO);
        refreshSettlementSummary(getSettlement(confirm.getSettlementId()));
        refreshTodoTasksQuietly();
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean issueMy(AttendanceMonthlyConfirmIssueReqVO reqVO) {
        AttendanceMonthlyConfirmDO confirm = getConfirmOwnedByCurrentUser(reqVO.getId());
        ensureSettlementEditable(confirm.getSettlementId());
        attendanceMonthlyConfirmMapper.update(null, new LambdaUpdateWrapper<AttendanceMonthlyConfirmDO>()
                .eq(AttendanceMonthlyConfirmDO::getId, confirm.getId())
                .set(AttendanceMonthlyConfirmDO::getStatus, CONFIRM_ISSUE)
                .set(AttendanceMonthlyConfirmDO::getConfirmedTime, null)
                .set(AttendanceMonthlyConfirmDO::getIssueTime, LocalDateTime.now())
                .set(AttendanceMonthlyConfirmDO::getIssueRemark, reqVO.getIssueRemark().trim())
                .set(AttendanceMonthlyConfirmDO::getResolvedTime, null)
                .set(AttendanceMonthlyConfirmDO::getResolvedBy, null)
                .set(AttendanceMonthlyConfirmDO::getResolveRemark, null));
        refreshSettlementSummary(getSettlement(confirm.getSettlementId()));
        refreshTodoTasksQuietly();
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean resolveConfirm(AttendanceMonthlyConfirmResolveReqVO reqVO) {
        AttendanceMonthlyConfirmDO confirm = getConfirm(reqVO.getId());
        ensureSettlementEditable(confirm.getSettlementId());
        if (!CONFIRM_ISSUE.equals(confirm.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "只有异议状态可以处理");
        }
        AttendanceMonthlyConfirmDO updateDO = new AttendanceMonthlyConfirmDO();
        updateDO.setId(confirm.getId());
        updateDO.setStatus(CONFIRM_RESOLVED);
        updateDO.setResolvedTime(LocalDateTime.now());
        updateDO.setResolvedBy(SecurityFrameworkUtils.getLoginUserId());
        updateDO.setResolveRemark(StringUtils.hasText(reqVO.getResolveRemark()) ? reqVO.getResolveRemark().trim() : null);
        attendanceMonthlyConfirmMapper.updateById(updateDO);
        refreshSettlementSummary(getSettlement(confirm.getSettlementId()));
        refreshTodoTasksQuietly();
        return true;
    }

    private AttendanceMonthlySettlementDO getSettlement(Long id) {
        AttendanceMonthlySettlementDO settlement = attendanceMonthlySettlementMapper.selectById(id);
        if (settlement == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND, "考勤月结不存在");
        }
        return settlement;
    }

    private AttendanceMonthlyConfirmDO getConfirm(Long id) {
        AttendanceMonthlyConfirmDO confirm = attendanceMonthlyConfirmMapper.selectById(id);
        if (confirm == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND, "月度考勤确认单不存在");
        }
        return confirm;
    }

    private AttendanceMonthlyConfirmDO getConfirmOwnedByCurrentUser(Long id) {
        AttendanceMonthlyConfirmDO confirm = getConfirm(id);
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null || !Objects.equals(loginUserId, confirm.getUserId())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.FORBIDDEN, "无权处理该月度考勤确认单");
        }
        return confirm;
    }

    private void ensureSettlementEditable(Long settlementId) {
        AttendanceMonthlySettlementDO settlement = getSettlement(settlementId);
        if (STATUS_LOCKED.equals(settlement.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "月结已锁定，不能继续处理");
        }
    }

    private AttendanceMonthlyConfirmDetailRespVO buildConfirmDetail(AttendanceMonthlyConfirmDO confirm, boolean mineOnly) {
        if (mineOnly) {
            Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
            if (loginUserId == null || !Objects.equals(loginUserId, confirm.getUserId())) {
                throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.FORBIDDEN, "无权查看该月度考勤确认单");
            }
        }
        AttendanceMonthlyConfirmDetailRespVO detail = new AttendanceMonthlyConfirmDetailRespVO();
        AttendanceMonthlySettlementDO settlement = getSettlement(confirm.getSettlementId());
        detail.setSettlement(BeanUtils.toBean(settlement, AttendanceMonthlySettlementRespVO.class));

        List<AttendanceMonthlyConfirmDO> confirmRows = new ArrayList<>();
        confirmRows.add(confirm);
        List<AttendanceMonthlyConfirmRespVO> confirmResp = BeanUtils.toBean(confirmRows, AttendanceMonthlyConfirmRespVO.class);
        fillConfirmPeopleInfo(confirmRows, confirmResp);
        detail.setConfirm(confirmResp.isEmpty() ? null : confirmResp.get(0));
        detail.setDailyResults(loadDailyResults(confirm));
        return detail;
    }

    private List<AttendanceDailyResultRespVO> loadDailyResults(AttendanceMonthlyConfirmDO confirm) {
        LocalDate month = parseSettlementMonth(confirm.getSettlementMonth());
        AttendanceDailyResultPageReqVO reqVO = new AttendanceDailyResultPageReqVO();
        reqVO.setUserId(confirm.getUserId());
        reqVO.setAttendanceDate(new LocalDate[]{month.withDayOfMonth(1), month.with(TemporalAdjusters.lastDayOfMonth())});
        List<AttendanceDailyResultDO> rows = attendanceDailyResultMapper.selectListByReqVO(reqVO);
        rows = emptyIfNull(rows);
        rows.sort(Comparator.comparing(AttendanceDailyResultDO::getAttendanceDate,
                Comparator.nullsLast(Comparator.naturalOrder())));
        List<AttendanceDailyResultRespVO> respList = BeanUtils.toBean(rows, AttendanceDailyResultRespVO.class);
        String profileName = null;
        if (confirm.getProfileId() != null) {
            EmployeeProfileDO profile = employeeProfileMapper.selectById(confirm.getProfileId());
            profileName = profile == null ? null : profile.getName();
        }
        String userNickname = loadUserName(confirm.getUserId());
        for (AttendanceDailyResultRespVO row : emptyIfNull(respList)) {
            row.setProfileName(profileName);
            row.setUserNickname(userNickname);
        }
        return emptyIfNull(respList);
    }

    private void upsertConfirmRows(AttendanceMonthlySettlementDO settlement, LocalDate month) {
        List<AttendanceDailyResultDO> dailyResults = loadDailyResultsForMonth(month);
        Map<Long, ConfirmSeed> userRows = new LinkedHashMap<>();
        for (AttendanceDailyResultDO row : emptyIfNull(dailyResults)) {
            if (row.getUserId() == null) {
                continue;
            }
            ConfirmSeed existed = userRows.get(row.getUserId());
            if (existed == null || (existed.profileId == null && row.getProfileId() != null)) {
                userRows.put(row.getUserId(), new ConfirmSeed(row.getProfileId(), row.getUserId()));
            }
        }
        for (EmployeeProfileDO profile : loadActiveProfilesWithUser()) {
            if (profile.getUserId() != null && !userRows.containsKey(profile.getUserId())) {
                userRows.put(profile.getUserId(), new ConfirmSeed(profile.getId(), profile.getUserId()));
            }
        }
        for (ConfirmSeed row : userRows.values()) {
            AttendanceMonthlyConfirmDO existing = attendanceMonthlyConfirmMapper
                    .selectBySettlementIdAndUserId(settlement.getId(), row.userId);
            if (existing == null) {
                AttendanceMonthlyConfirmDO insertDO = new AttendanceMonthlyConfirmDO();
                insertDO.setSettlementId(settlement.getId());
                insertDO.setSettlementMonth(settlement.getSettlementMonth());
                insertDO.setDeptId(normalizeDeptId(settlement.getDeptId()));
                insertDO.setProfileId(row.profileId);
                insertDO.setUserId(row.userId);
                insertDO.setStatus(CONFIRM_PENDING);
                attendanceMonthlyConfirmMapper.insert(insertDO);
                continue;
            }
            AttendanceMonthlyConfirmDO updateDO = new AttendanceMonthlyConfirmDO();
            updateDO.setId(existing.getId());
            updateDO.setSettlementMonth(settlement.getSettlementMonth());
            updateDO.setDeptId(normalizeDeptId(settlement.getDeptId()));
            updateDO.setProfileId(row.profileId);
            if (!StringUtils.hasText(existing.getStatus())) {
                updateDO.setStatus(CONFIRM_PENDING);
            }
            attendanceMonthlyConfirmMapper.updateById(updateDO);
        }
    }

    private List<EmployeeProfileDO> loadActiveProfilesWithUser() {
        return emptyIfNull(employeeProfileMapper.selectList(new LambdaQueryWrapperX<EmployeeProfileDO>()
                .eq(EmployeeProfileDO::getStatus, 1)
                .isNotNull(EmployeeProfileDO::getUserId)
                .orderByAsc(EmployeeProfileDO::getId)));
    }

    private List<AttendanceDailyResultDO> loadDailyResultsForMonth(LocalDate month) {
        AttendanceDailyResultPageReqVO dailyReqVO = new AttendanceDailyResultPageReqVO();
        dailyReqVO.setAttendanceDate(new LocalDate[]{month.withDayOfMonth(1), month.with(TemporalAdjusters.lastDayOfMonth())});
        return attendanceDailyResultMapper.selectListByReqVO(dailyReqVO);
    }

    private SummaryCounter buildSummary(LocalDate month) {
        LocalDate startDate = month.withDayOfMonth(1);
        LocalDate endDate = month.with(TemporalAdjusters.lastDayOfMonth());

        List<AttendanceDailyResultDO> dailyResults = loadDailyResultsForMonth(month);

        AttendanceExceptionPageReqVO exceptionReqVO = new AttendanceExceptionPageReqVO();
        exceptionReqVO.setAttendanceDate(new LocalDate[]{startDate, endDate});
        List<AttendanceExceptionDO> exceptions = attendanceExceptionMapper.selectListByReqVO(exceptionReqVO);

        SummaryCounter summary = new SummaryCounter();
        Set<Long> userIds = new HashSet<>();
        Set<LocalDate> days = new HashSet<>();
        for (AttendanceDailyResultDO row : emptyIfNull(dailyResults)) {
            if (row.getUserId() != null) {
                userIds.add(row.getUserId());
            }
            if (row.getAttendanceDate() != null) {
                days.add(row.getAttendanceDate());
            }
            summary.recordCount++;
            summary.leaveHours = add(summary.leaveHours, row.getLeaveHours());
            summary.tripHours = add(summary.tripHours, row.getTripHours());
            summary.absentHours = add(summary.absentHours, row.getAbsentHours());
            countResultStatus(summary, row.getResultStatus());
        }
        for (AttendanceExceptionDO exception : emptyIfNull(exceptions)) {
            summary.exceptionCount++;
            if (EXCEPTION_PENDING.equals(exception.getExceptionStatus())) {
                summary.pendingExceptionCount++;
            } else if (EXCEPTION_RESOLVED.equals(exception.getExceptionStatus())) {
                summary.resolvedExceptionCount++;
            }
        }
        summary.employeeCount = userIds.size();
        summary.dayCount = days.size();
        return summary;
    }

    private void refreshSettlementSummary(AttendanceMonthlySettlementDO settlement) {
        if (settlement == null || settlement.getId() == null || !StringUtils.hasText(settlement.getSettlementMonth())) {
            return;
        }
        LocalDate month = parseSettlementMonth(settlement.getSettlementMonth());
        SummaryCounter summary = buildSummary(month);
        applyConfirmSummary(summary, settlement.getId());
        AttendanceMonthlySettlementDO updateDO = new AttendanceMonthlySettlementDO();
        updateDO.setId(settlement.getId());
        updateDO.setSummaryJson(buildSummaryJson(settlement.getSettlementMonth(), normalizeDeptId(settlement.getDeptId()), summary));
        attendanceMonthlySettlementMapper.updateById(updateDO);
    }

    private void applyConfirmSummary(SummaryCounter summary, Long settlementId) {
        List<AttendanceMonthlyConfirmDO> confirms = attendanceMonthlyConfirmMapper.selectListBySettlementId(settlementId);
        for (AttendanceMonthlyConfirmDO confirm : emptyIfNull(confirms)) {
            summary.confirmTotalCount++;
            if (CONFIRM_PENDING.equals(confirm.getStatus())) {
                summary.confirmPendingCount++;
            } else if (CONFIRM_CONFIRMED.equals(confirm.getStatus())) {
                summary.confirmConfirmedCount++;
            } else if (CONFIRM_ISSUE.equals(confirm.getStatus())) {
                summary.confirmIssueCount++;
            } else if (CONFIRM_RESOLVED.equals(confirm.getStatus())) {
                summary.confirmResolvedCount++;
            }
        }
        summary.employeeCount = Math.max(summary.employeeCount, (int) summary.confirmTotalCount);
    }

    private void countResultStatus(SummaryCounter summary, String status) {
        if (!StringUtils.hasText(status) || RESULT_NORMAL.equals(status)) {
            summary.normalCount++;
            return;
        }
        if (RESULT_LEAVE.equals(status)) {
            summary.leaveCount++;
        } else if (RESULT_TRIP.equals(status)) {
            summary.tripCount++;
        } else if (RESULT_LATE.equals(status)) {
            summary.lateCount++;
        } else if (RESULT_EARLY.equals(status)) {
            summary.earlyCount++;
        } else if (RESULT_LATE_EARLY.equals(status)) {
            summary.lateCount++;
            summary.earlyCount++;
            summary.lateEarlyCount++;
        } else if (RESULT_MISSING_IN.equals(status)) {
            summary.missingInCount++;
        } else if (RESULT_MISSING_OUT.equals(status)) {
            summary.missingOutCount++;
        } else if (RESULT_MISSING_BOTH.equals(status)) {
            summary.missingBothCount++;
        } else if (RESULT_ABSENTEEISM.equals(status)) {
            summary.absenteeismCount++;
        } else {
            summary.unknownCount++;
        }
    }

    private String buildSummaryJson(String settlementMonth, Long deptId, SummaryCounter summary) {
        StringBuilder builder = new StringBuilder("{");
        appendJsonText(builder, "settlementMonth", settlementMonth);
        appendJsonNumber(builder, "deptId", deptId);
        appendJsonNumber(builder, "employeeCount", summary.employeeCount);
        appendJsonNumber(builder, "dayCount", summary.dayCount);
        appendJsonNumber(builder, "recordCount", summary.recordCount);
        appendJsonNumber(builder, "normalCount", summary.normalCount);
        appendJsonNumber(builder, "leaveCount", summary.leaveCount);
        appendJsonNumber(builder, "tripCount", summary.tripCount);
        appendJsonNumber(builder, "lateCount", summary.lateCount);
        appendJsonNumber(builder, "earlyCount", summary.earlyCount);
        appendJsonNumber(builder, "lateEarlyCount", summary.lateEarlyCount);
        appendJsonNumber(builder, "missingInCount", summary.missingInCount);
        appendJsonNumber(builder, "missingOutCount", summary.missingOutCount);
        appendJsonNumber(builder, "missingBothCount", summary.missingBothCount);
        appendJsonNumber(builder, "absenteeismCount", summary.absenteeismCount);
        appendJsonNumber(builder, "unknownCount", summary.unknownCount);
        appendJsonNumber(builder, "exceptionCount", summary.exceptionCount);
        appendJsonNumber(builder, "pendingExceptionCount", summary.pendingExceptionCount);
        appendJsonNumber(builder, "resolvedExceptionCount", summary.resolvedExceptionCount);
        appendJsonNumber(builder, "confirmTotalCount", summary.confirmTotalCount);
        appendJsonNumber(builder, "confirmPendingCount", summary.confirmPendingCount);
        appendJsonNumber(builder, "confirmConfirmedCount", summary.confirmConfirmedCount);
        appendJsonNumber(builder, "confirmIssueCount", summary.confirmIssueCount);
        appendJsonNumber(builder, "confirmResolvedCount", summary.confirmResolvedCount);
        appendJsonNumber(builder, "leaveHours", summary.leaveHours);
        appendJsonNumber(builder, "tripHours", summary.tripHours);
        appendJsonNumber(builder, "absentHours", summary.absentHours);
        builder.append('}');
        return builder.toString();
    }

    private void appendJsonText(StringBuilder builder, String key, String value) {
        appendCommaIfNeeded(builder);
        builder.append('"').append(key).append("\":\"").append(escapeJson(value)).append('"');
    }

    private void appendJsonNumber(StringBuilder builder, String key, Number value) {
        appendCommaIfNeeded(builder);
        builder.append('"').append(key).append("\":").append(value == null ? 0 : value);
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void appendCommaIfNeeded(StringBuilder builder) {
        if (builder.length() > 1) {
            builder.append(',');
        }
    }

    private void normalizeConfirmPageReq(AttendanceMonthlyConfirmPageReqVO pageReqVO) {
        if (StringUtils.hasText(pageReqVO.getStatus())) {
            pageReqVO.setStatus(pageReqVO.getStatus().trim().toUpperCase());
        }
        if (StringUtils.hasText(pageReqVO.getSettlementMonth())) {
            pageReqVO.setSettlementMonth(pageReqVO.getSettlementMonth().trim());
        }
    }

    private void fillConfirmPeopleInfo(List<AttendanceMonthlyConfirmDO> rows,
                                       List<AttendanceMonthlyConfirmRespVO> respList) {
        if (rows == null || rows.isEmpty() || respList == null || respList.isEmpty()) {
            return;
        }
        Set<Long> userIds = new HashSet<>();
        Set<Long> profileIds = new HashSet<>();
        for (AttendanceMonthlyConfirmDO row : rows) {
            if (row.getUserId() != null) {
                userIds.add(row.getUserId());
            }
            if (row.getResolvedBy() != null) {
                userIds.add(row.getResolvedBy());
            }
            if (row.getProfileId() != null) {
                profileIds.add(row.getProfileId());
            }
        }
        Map<Long, AdminUserRespDTO> userMap = loadUserMapSafe(userIds);
        Map<Long, EmployeeProfileDO> profileMap = loadProfileMapSafe(profileIds);
        for (AttendanceMonthlyConfirmRespVO item : respList) {
            AdminUserRespDTO user = userMap.get(item.getUserId());
            if (user != null) {
                item.setUserNickname(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
            }
            AdminUserRespDTO resolvedBy = userMap.get(item.getResolvedBy());
            if (resolvedBy != null) {
                item.setResolvedByName(StringUtils.hasText(resolvedBy.getNickname())
                        ? resolvedBy.getNickname() : resolvedBy.getUsername());
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
            log.warn("Failed to load admin users for monthly settlement: {}", ex.getMessage());
            return new HashMap<>();
        }
    }

    private String loadUserName(Long userId) {
        if (userId == null) {
            return null;
        }
        Set<Long> ids = new HashSet<>();
        ids.add(userId);
        AdminUserRespDTO user = loadUserMapSafe(ids).get(userId);
        if (user == null) {
            return null;
        }
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
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

    private void refreshTodoTasksQuietly() {
        try {
            hrTodoTaskService.refreshGeneratedTasks();
        } catch (Exception ex) {
            log.warn("Refresh HR todo tasks after monthly settlement failed: {}", ex.getMessage());
        }
    }

    private BigDecimal add(BigDecimal left, BigDecimal right) {
        return (left == null ? BigDecimal.ZERO : left).add(right == null ? BigDecimal.ZERO : right);
    }

    private LocalDate resolveMonth(Integer year, Integer month) {
        LocalDate now = LocalDate.now();
        int resolvedYear = year == null ? now.getYear() : year;
        int resolvedMonth = month == null ? now.getMonthValue() : month;
        if (resolvedMonth < 1 || resolvedMonth > 12) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "月份不合法");
        }
        return LocalDate.of(resolvedYear, resolvedMonth, 1);
    }

    private LocalDate parseSettlementMonth(String settlementMonth) {
        try {
            return LocalDate.parse(settlementMonth + "-01");
        } catch (Exception ex) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "月结月份格式不正确");
        }
    }

    private Long normalizeDeptId(Long deptId) {
        return deptId == null ? DEFAULT_DEPT_ID : deptId;
    }

    private <T> List<T> emptyIfNull(List<T> rows) {
        return rows == null ? new ArrayList<>() : rows;
    }

    private static class SummaryCounter {
        private int employeeCount;
        private int dayCount;
        private long recordCount;
        private long normalCount;
        private long leaveCount;
        private long tripCount;
        private long lateCount;
        private long earlyCount;
        private long lateEarlyCount;
        private long missingInCount;
        private long missingOutCount;
        private long missingBothCount;
        private long absenteeismCount;
        private long unknownCount;
        private long exceptionCount;
        private long pendingExceptionCount;
        private long resolvedExceptionCount;
        private long confirmTotalCount;
        private long confirmPendingCount;
        private long confirmConfirmedCount;
        private long confirmIssueCount;
        private long confirmResolvedCount;
        private BigDecimal leaveHours = BigDecimal.ZERO;
        private BigDecimal tripHours = BigDecimal.ZERO;
        private BigDecimal absentHours = BigDecimal.ZERO;
    }

    private static class ConfirmSeed {
        private final Long profileId;
        private final Long userId;

        private ConfirmSeed(Long profileId, Long userId) {
            this.profileId = profileId;
            this.userId = userId;
        }
    }

}
