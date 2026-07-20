package com.kyx.service.hr.service.attendance;

import com.kyx.foundation.common.exception.enums.GlobalErrorCodeConstants;
import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.foundation.security.core.service.SecurityFrameworkService;
import com.kyx.foundation.security.core.util.SecurityFrameworkUtils;
import com.kyx.service.bpm.api.task.BpmProcessInstanceApi;
import com.kyx.service.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.kyx.service.bpm.enums.task.BpmProcessInstanceStatusEnum;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import com.kyx.service.hr.controller.admin.administrative.leave.vo.HrLeaveBalanceAdjustReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceOvertimeApplyReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceOvertimeApproveReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceOvertimePageReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceOvertimeRespVO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceOvertimeDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeProfileDO;
import com.kyx.service.hr.dal.mysql.attendance.AttendanceOvertimeMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeProfileMapper;
import com.kyx.service.hr.service.administrative.leave.HrLeaveBalanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Attendance overtime Service implementation.
 */
@Service
@Validated
@Slf4j
public class AttendanceOvertimeServiceImpl implements AttendanceOvertimeService {

    public static final String PROCESS_KEY = "hr_attendance_overtime";

    private static final String OVERTIME_WORKDAY = "WORKDAY";
    private static final String OVERTIME_WEEKEND = "WEEKEND";
    private static final String OVERTIME_HOLIDAY = "HOLIDAY";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_CANCELED = "CANCELED";
    private static final String DEFAULT_COMP_TIME_CODE = "rest";
    private static final String PERMISSION_QUERY_ALL = "attendance:clock:query-all";
    private static final String PERMISSION_OVERTIME_APPROVE = "attendance:overtime:approve";

    @Resource
    private AttendanceOvertimeMapper attendanceOvertimeMapper;
    @Resource
    private EmployeeProfileMapper employeeProfileMapper;
    @Resource
    private HrLeaveBalanceService leaveBalanceService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private SecurityFrameworkService securityFrameworkService;
    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long apply(AttendanceOvertimeApplyReqVO reqVO) {
        EmployeeProfileDO profile = resolveApplyProfile(reqVO.getProfileId(), reqVO.getUserId());
        LocalDateTime startTime = normalizeTime(reqVO.getStartTime(), "加班开始时间不能为空");
        LocalDateTime endTime = normalizeTime(reqVO.getEndTime(), "加班结束时间不能为空");
        if (!endTime.isAfter(startTime)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "加班结束时间必须晚于开始时间");
        }
        BigDecimal durationHours = calculateDurationHours(startTime, endTime);
        AttendanceOvertimeDO overtime = new AttendanceOvertimeDO();
        overtime.setProfileId(profile.getId());
        overtime.setUserId(profile.getUserId());
        overtime.setOvertimeDate(startTime.toLocalDate());
        overtime.setOvertimeType(normalizeOvertimeType(reqVO.getOvertimeType()));
        overtime.setStartTime(startTime);
        overtime.setEndTime(endTime);
        overtime.setDurationHours(durationHours);
        overtime.setConvertToLeave(!Boolean.FALSE.equals(reqVO.getConvertToLeave()));
        overtime.setLeaveTypeCode(resolveLeaveTypeCode(reqVO.getLeaveTypeCode()));
        overtime.setBalanceHours(BigDecimal.ZERO);
        overtime.setBalanceSynced(false);
        overtime.setReason(reqVO.getReason());
        overtime.setStatus(STATUS_PENDING);
        attendanceOvertimeMapper.insert(overtime);

        String processInstanceId = startOvertimeApprovalProcess(overtime, profile);
        AttendanceOvertimeDO processUpdate = new AttendanceOvertimeDO();
        processUpdate.setId(overtime.getId());
        processUpdate.setProcessInstanceId(processInstanceId);
        attendanceOvertimeMapper.updateById(processUpdate);
        return overtime.getId();
    }

    @Override
    public PageResult<AttendanceOvertimeRespVO> getPage(AttendanceOvertimePageReqVO pageReqVO) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            return new PageResult<>(new ArrayList<>(), 0L);
        }
        if (!canManage()) {
            pageReqVO.setUserId(loginUserId);
            pageReqVO.setProfileId(null);
        }
        normalizePageReq(pageReqVO);
        PageResult<AttendanceOvertimeDO> pageResult = attendanceOvertimeMapper.selectPage(pageReqVO);
        List<AttendanceOvertimeDO> rows = pageResult.getList();
        if (rows == null || rows.isEmpty()) {
            return new PageResult<>(new ArrayList<>(), pageResult.getTotal());
        }
        List<AttendanceOvertimeRespVO> respList = BeanUtils.toBean(rows, AttendanceOvertimeRespVO.class);
        fillPeopleInfo(rows, respList);
        return new PageResult<>(respList, pageResult.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean approve(AttendanceOvertimeApproveReqVO reqVO) {
        AttendanceOvertimeDO overtime = attendanceOvertimeMapper.selectById(reqVO.getId());
        if (overtime == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND, "加班申请不存在");
        }
        if (!STATUS_PENDING.equals(overtime.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "只有待审批加班申请可以处理");
        }
        if (StringUtils.hasText(overtime.getProcessInstanceId())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "该申请已进入 BPM 流程，请在流程中心处理审批");
        }

        BigDecimal balanceHours = BigDecimal.ZERO;
        boolean balanceSynced = false;
        if (Boolean.TRUE.equals(reqVO.getApproved()) && Boolean.TRUE.equals(overtime.getConvertToLeave())) {
            balanceHours = defaultZero(overtime.getDurationHours());
            syncCompTimeBalance(overtime, balanceHours);
            balanceSynced = true;
        }

        AttendanceOvertimeDO updateDO = new AttendanceOvertimeDO();
        updateDO.setId(overtime.getId());
        updateDO.setStatus(Boolean.TRUE.equals(reqVO.getApproved()) ? STATUS_APPROVED : STATUS_REJECTED);
        updateDO.setApproverId(SecurityFrameworkUtils.getLoginUserId());
        updateDO.setApprovedTime(LocalDateTime.now());
        updateDO.setApproveRemark(reqVO.getApproveRemark());
        updateDO.setBalanceHours(balanceHours);
        updateDO.setBalanceSynced(balanceSynced);
        attendanceOvertimeMapper.updateById(updateDO);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean cancel(Long id) {
        AttendanceOvertimeDO overtime = attendanceOvertimeMapper.selectById(id);
        if (overtime == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND, "加班申请不存在");
        }
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (!canManage() && (loginUserId == null || !loginUserId.equals(overtime.getUserId()))) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.FORBIDDEN, "无权撤销该加班申请");
        }
        if (!STATUS_PENDING.equals(overtime.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "只有待审批加班申请可以撤销");
        }
        if (StringUtils.hasText(overtime.getProcessInstanceId())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "该申请已进入 BPM 流程，请在流程详情中撤销");
        }
        AttendanceOvertimeDO updateDO = new AttendanceOvertimeDO();
        updateDO.setId(overtime.getId());
        updateDO.setStatus(STATUS_CANCELED);
        attendanceOvertimeMapper.updateById(updateDO);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateApprovalStatusByBpmEvent(Long id, String processInstanceId, Integer bpmStatus, Long operatorUserId) {
        AttendanceOvertimeDO overtime = attendanceOvertimeMapper.selectById(id);
        if (overtime == null) {
            log.warn("加班调休 BPM 回调申请不存在，id={}, processInstanceId={}, bpmStatus={}",
                    id, processInstanceId, bpmStatus);
            return;
        }
        if (StringUtils.hasText(overtime.getProcessInstanceId())
                && StringUtils.hasText(processInstanceId)
                && !Objects.equals(overtime.getProcessInstanceId(), processInstanceId)) {
            log.warn("加班调休 BPM 回调流程实例不匹配，id={}, processInstanceId={}, currentProcessInstanceId={}",
                    id, processInstanceId, overtime.getProcessInstanceId());
            return;
        }
        if (Objects.equals(bpmStatus, BpmProcessInstanceStatusEnum.APPROVE.getStatus())) {
            approveByBpm(overtime, processInstanceId, operatorUserId);
            return;
        }
        if (Objects.equals(bpmStatus, BpmProcessInstanceStatusEnum.REJECT.getStatus())) {
            closeByBpm(overtime, STATUS_REJECTED, processInstanceId, operatorUserId, "BPM审批驳回");
            return;
        }
        if (Objects.equals(bpmStatus, BpmProcessInstanceStatusEnum.CANCEL.getStatus())) {
            closeByBpm(overtime, STATUS_CANCELED, processInstanceId, operatorUserId, "BPM流程撤销");
        }
    }

    private String startOvertimeApprovalProcess(AttendanceOvertimeDO overtime, EmployeeProfileDO profile) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.UNAUTHORIZED, "加班调休发起人不能为空");
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("overtimeId", overtime.getId());
        variables.put("profileId", overtime.getProfileId());
        variables.put("userId", overtime.getUserId());
        variables.put("employeeName", profile.getName());
        variables.put("overtimeDate", overtime.getOvertimeDate());
        variables.put("overtimeType", overtime.getOvertimeType());
        variables.put("startTime", overtime.getStartTime());
        variables.put("endTime", overtime.getEndTime());
        variables.put("durationHours", overtime.getDurationHours());
        variables.put("convertToLeave", overtime.getConvertToLeave());
        variables.put("leaveTypeCode", overtime.getLeaveTypeCode());
        variables.put("reason", overtime.getReason());
        return processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO()
                        .setProcessDefinitionKey(PROCESS_KEY)
                        .setBusinessKey(String.valueOf(overtime.getId()))
                        .setVariables(variables))
                .getCheckedData();
    }

    private void approveByBpm(AttendanceOvertimeDO overtime, String processInstanceId, Long operatorUserId) {
        if (STATUS_APPROVED.equals(overtime.getStatus())) {
            return;
        }
        if (!STATUS_PENDING.equals(overtime.getStatus())) {
            log.warn("加班调休 BPM 通过回调忽略非待审批记录，id={}, status={}", overtime.getId(), overtime.getStatus());
            return;
        }
        BigDecimal balanceHours = BigDecimal.ZERO;
        boolean balanceSynced = false;
        if (Boolean.TRUE.equals(overtime.getConvertToLeave())) {
            balanceHours = defaultZero(overtime.getDurationHours());
            syncCompTimeBalance(overtime, balanceHours);
            balanceSynced = balanceHours.compareTo(BigDecimal.ZERO) > 0;
        }
        AttendanceOvertimeDO updateDO = new AttendanceOvertimeDO();
        updateDO.setId(overtime.getId());
        updateDO.setStatus(STATUS_APPROVED);
        updateDO.setProcessInstanceId(StringUtils.hasText(processInstanceId)
                ? processInstanceId : overtime.getProcessInstanceId());
        updateDO.setApproverId(operatorUserId);
        updateDO.setApprovedTime(LocalDateTime.now());
        updateDO.setApproveRemark("BPM审批通过");
        updateDO.setBalanceHours(balanceHours);
        updateDO.setBalanceSynced(balanceSynced);
        attendanceOvertimeMapper.updateById(updateDO);
    }

    private void closeByBpm(AttendanceOvertimeDO overtime, String status,
                            String processInstanceId, Long operatorUserId, String remark) {
        if (status.equals(overtime.getStatus())) {
            return;
        }
        if (!STATUS_PENDING.equals(overtime.getStatus())) {
            log.warn("加班调休 BPM 关闭回调忽略非待审批记录，id={}, status={}, targetStatus={}",
                    overtime.getId(), overtime.getStatus(), status);
            return;
        }
        AttendanceOvertimeDO updateDO = new AttendanceOvertimeDO();
        updateDO.setId(overtime.getId());
        updateDO.setStatus(status);
        updateDO.setProcessInstanceId(StringUtils.hasText(processInstanceId)
                ? processInstanceId : overtime.getProcessInstanceId());
        updateDO.setApproverId(operatorUserId);
        updateDO.setApprovedTime(LocalDateTime.now());
        updateDO.setApproveRemark(remark);
        updateDO.setBalanceHours(BigDecimal.ZERO);
        updateDO.setBalanceSynced(false);
        attendanceOvertimeMapper.updateById(updateDO);
    }

    private void syncCompTimeBalance(AttendanceOvertimeDO overtime, BigDecimal balanceHours) {
        if (balanceHours.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        HrLeaveBalanceAdjustReqVO adjustReqVO = new HrLeaveBalanceAdjustReqVO();
        adjustReqVO.setProfileId(overtime.getProfileId());
        adjustReqVO.setUserId(overtime.getUserId());
        adjustReqVO.setLeaveTypeCode(resolveLeaveTypeCode(overtime.getLeaveTypeCode()));
        adjustReqVO.setYear(overtime.getStartTime().getYear());
        adjustReqVO.setAmount(balanceHours);
        adjustReqVO.setRemark("加班转调休：" + overtime.getId());
        leaveBalanceService.adjustBalance(adjustReqVO);
    }

    private EmployeeProfileDO resolveApplyProfile(Long profileId, Long userId) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.UNAUTHORIZED);
        }
        boolean manage = canManage();
        if ((profileId != null || userId != null) && !manage) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.FORBIDDEN, "无权替他人提交加班申请");
        }
        EmployeeProfileDO profile = profileId == null ? null : employeeProfileMapper.selectById(profileId);
        if (profile == null) {
            profile = employeeProfileMapper.selectByUserId(userId == null ? loginUserId : userId);
        }
        if (profile == null || profile.getUserId() == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "员工档案不存在或未绑定登录用户");
        }
        if (userId != null && !userId.equals(profile.getUserId())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "员工档案和用户不匹配");
        }
        return profile;
    }

    private void normalizePageReq(AttendanceOvertimePageReqVO pageReqVO) {
        if (StringUtils.hasText(pageReqVO.getOvertimeType())) {
            pageReqVO.setOvertimeType(normalizeOvertimeType(pageReqVO.getOvertimeType()));
        }
        if (StringUtils.hasText(pageReqVO.getStatus())) {
            pageReqVO.setStatus(pageReqVO.getStatus().trim().toUpperCase());
        }
    }

    private String normalizeOvertimeType(String overtimeType) {
        if (!StringUtils.hasText(overtimeType)) {
            return OVERTIME_WORKDAY;
        }
        String value = overtimeType.trim().toUpperCase();
        if (OVERTIME_WORKDAY.equals(value) || OVERTIME_WEEKEND.equals(value) || OVERTIME_HOLIDAY.equals(value)) {
            return value;
        }
        throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "加班类型不合法");
    }

    private String resolveLeaveTypeCode(String leaveTypeCode) {
        return StringUtils.hasText(leaveTypeCode) ? leaveTypeCode.trim().toLowerCase() : DEFAULT_COMP_TIME_CODE;
    }

    private LocalDateTime normalizeTime(LocalDateTime time, String message) {
        if (time == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, message);
        }
        return time.withNano(0);
    }

    private BigDecimal calculateDurationHours(LocalDateTime startTime, LocalDateTime endTime) {
        long minutes = Duration.between(startTime, endTime).toMinutes();
        if (minutes <= 0) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "加班时长必须大于0");
        }
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void fillPeopleInfo(List<AttendanceOvertimeDO> rows, List<AttendanceOvertimeRespVO> respList) {
        Set<Long> userIds = new HashSet<>();
        Set<Long> profileIds = new HashSet<>();
        for (AttendanceOvertimeDO row : rows) {
            if (row.getUserId() != null) {
                userIds.add(row.getUserId());
            }
            if (row.getApproverId() != null) {
                userIds.add(row.getApproverId());
            }
            if (row.getProfileId() != null) {
                profileIds.add(row.getProfileId());
            }
        }
        Map<Long, AdminUserRespDTO> userMap = loadUserMapSafe(userIds);
        Map<Long, EmployeeProfileDO> profileMap = loadProfileMapSafe(profileIds);
        for (AttendanceOvertimeRespVO item : respList) {
            AdminUserRespDTO user = userMap.get(item.getUserId());
            if (user != null) {
                item.setUserNickname(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
            }
            AdminUserRespDTO approver = userMap.get(item.getApproverId());
            if (approver != null) {
                item.setApproverNickname(StringUtils.hasText(approver.getNickname()) ? approver.getNickname() : approver.getUsername());
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
            log.warn("Failed to load admin users for attendance overtime page: {}", ex.getMessage());
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

    private boolean canManage() {
        try {
            return securityFrameworkService.hasPermission(PERMISSION_OVERTIME_APPROVE)
                    || securityFrameworkService.hasPermission(PERMISSION_QUERY_ALL);
        } catch (Exception ex) {
            log.warn("check attendance overtime manage permission failed: {}", ex.getMessage());
            return false;
        }
    }

}
