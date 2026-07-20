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
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceCalculateReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceCorrectionApplyReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceCorrectionApproveReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceCorrectionPageReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceCorrectionRespVO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceClockRecordDO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceCorrectionDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeProfileDO;
import com.kyx.service.hr.dal.mysql.attendance.AttendanceClockRecordMapper;
import com.kyx.service.hr.dal.mysql.attendance.AttendanceCorrectionMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeProfileMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Attendance correction / field clock Service implementation.
 */
@Service
@Validated
@Slf4j
public class AttendanceCorrectionServiceImpl implements AttendanceCorrectionService {

    public static final String PROCESS_KEY = "hr_attendance_correction";

    private static final String APPLY_TYPE_CORRECTION = "CORRECTION";
    private static final String APPLY_TYPE_FIELD = "FIELD";
    private static final String CLOCK_TYPE_IN = "IN";
    private static final String CLOCK_TYPE_OUT = "OUT";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_CANCELED = "CANCELED";
    private static final String CLOCK_STATUS_NORMAL = "NORMAL";
    private static final String CLOCK_STATUS_LATE = "LATE";
    private static final String CLOCK_STATUS_EARLY = "EARLY";
    private static final String SOURCE_CORRECTION = "CORRECTION";
    private static final String SOURCE_FIELD = "FIELD";
    private static final String PERMISSION_QUERY_ALL = "attendance:clock:query-all";
    private static final String PERMISSION_CORRECTION_APPROVE = "attendance:correction:approve";
    private static final LocalTime WORK_START_TIME = LocalTime.of(9, 0);
    private static final LocalTime WORK_END_TIME = LocalTime.of(18, 0);

    @Resource
    private AttendanceCorrectionMapper attendanceCorrectionMapper;
    @Resource
    private AttendanceClockRecordMapper attendanceClockRecordMapper;
    @Resource
    private EmployeeProfileMapper employeeProfileMapper;
    @Resource
    private AttendanceResultService attendanceResultService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private SecurityFrameworkService securityFrameworkService;
    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long apply(AttendanceCorrectionApplyReqVO reqVO) {
        EmployeeProfileDO profile = resolveApplyProfile(reqVO);
        String applyType = normalizeApplyType(reqVO.getApplyType());
        String clockType = normalizeClockType(reqVO.getClockType());
        LocalDateTime clockTime = normalizeClockTime(reqVO.getClockTime());
        LocalDate attendanceDate = clockTime.toLocalDate();

        Long pendingCount = attendanceCorrectionMapper.selectPendingCount(
                profile.getUserId(), attendanceDate, applyType, clockType);
        if (pendingCount != null && pendingCount > 0) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "同一天同类型补卡/外勤申请仍在审批中");
        }

        AttendanceCorrectionDO correction = new AttendanceCorrectionDO();
        correction.setProfileId(profile.getId());
        correction.setUserId(profile.getUserId());
        correction.setAttendanceDate(attendanceDate);
        correction.setApplyType(applyType);
        correction.setClockType(clockType);
        correction.setClockTime(clockTime);
        correction.setReason(reqVO.getReason());
        correction.setLocationName(reqVO.getLocationName());
        correction.setLocationAddress(reqVO.getLocationAddress());
        correction.setAttachmentJson(reqVO.getAttachmentJson());
        correction.setExceptionId(reqVO.getExceptionId());
        correction.setStatus(STATUS_PENDING);
        attendanceCorrectionMapper.insert(correction);

        String processInstanceId = startCorrectionApprovalProcess(correction, profile);
        AttendanceCorrectionDO processUpdate = new AttendanceCorrectionDO();
        processUpdate.setId(correction.getId());
        processUpdate.setProcessInstanceId(processInstanceId);
        attendanceCorrectionMapper.updateById(processUpdate);
        return correction.getId();
    }

    @Override
    public PageResult<AttendanceCorrectionRespVO> getPage(AttendanceCorrectionPageReqVO pageReqVO) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            return new PageResult<>(new ArrayList<>(), 0L);
        }
        if (!canManage()) {
            pageReqVO.setUserId(loginUserId);
            pageReqVO.setProfileId(null);
        }
        normalizePageReq(pageReqVO);
        PageResult<AttendanceCorrectionDO> pageResult = attendanceCorrectionMapper.selectPage(pageReqVO);
        List<AttendanceCorrectionDO> rows = pageResult.getList();
        if (rows == null || rows.isEmpty()) {
            return new PageResult<>(new ArrayList<>(), pageResult.getTotal());
        }
        List<AttendanceCorrectionRespVO> respList = BeanUtils.toBean(rows, AttendanceCorrectionRespVO.class);
        fillPeopleInfo(rows, respList);
        return new PageResult<>(respList, pageResult.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean approve(AttendanceCorrectionApproveReqVO reqVO) {
        AttendanceCorrectionDO correction = attendanceCorrectionMapper.selectById(reqVO.getId());
        if (correction == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND, "补卡/外勤申请不存在");
        }
        if (!STATUS_PENDING.equals(correction.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "只有待审批申请可以处理");
        }
        if (StringUtils.hasText(correction.getProcessInstanceId())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "该申请已进入 BPM 流程，请在流程中心处理审批");
        }

        AttendanceCorrectionDO updateDO = new AttendanceCorrectionDO();
        updateDO.setId(correction.getId());
        updateDO.setStatus(Boolean.TRUE.equals(reqVO.getApproved()) ? STATUS_APPROVED : STATUS_REJECTED);
        updateDO.setApproverId(SecurityFrameworkUtils.getLoginUserId());
        updateDO.setApprovedTime(LocalDateTime.now());
        updateDO.setApproveRemark(reqVO.getApproveRemark());

        if (Boolean.TRUE.equals(reqVO.getApproved())) {
            Long clockRecordId = upsertClockRecord(correction);
            updateDO.setClockRecordId(clockRecordId);
        }
        attendanceCorrectionMapper.updateById(updateDO);

        if (Boolean.TRUE.equals(reqVO.getApproved())) {
            recalculateDay(correction);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean cancel(Long id) {
        AttendanceCorrectionDO correction = attendanceCorrectionMapper.selectById(id);
        if (correction == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND, "补卡/外勤申请不存在");
        }
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (!canManage() && (loginUserId == null || !loginUserId.equals(correction.getUserId()))) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.FORBIDDEN, "无权撤销该补卡/外勤申请");
        }
        if (!STATUS_PENDING.equals(correction.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "只有待审批申请可以撤销");
        }
        if (StringUtils.hasText(correction.getProcessInstanceId())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "该申请已进入 BPM 流程，请在流程详情中撤销");
        }
        AttendanceCorrectionDO updateDO = new AttendanceCorrectionDO();
        updateDO.setId(correction.getId());
        updateDO.setStatus(STATUS_CANCELED);
        attendanceCorrectionMapper.updateById(updateDO);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateApprovalStatusByBpmEvent(Long id, String processInstanceId, Integer bpmStatus, Long operatorUserId) {
        AttendanceCorrectionDO correction = attendanceCorrectionMapper.selectById(id);
        if (correction == null) {
            log.warn("补卡外勤 BPM 回调申请不存在，id={}, processInstanceId={}, bpmStatus={}",
                    id, processInstanceId, bpmStatus);
            return;
        }
        if (StringUtils.hasText(correction.getProcessInstanceId())
                && StringUtils.hasText(processInstanceId)
                && !Objects.equals(correction.getProcessInstanceId(), processInstanceId)) {
            log.warn("补卡外勤 BPM 回调流程实例不匹配，id={}, processInstanceId={}, currentProcessInstanceId={}",
                    id, processInstanceId, correction.getProcessInstanceId());
            return;
        }
        if (Objects.equals(bpmStatus, BpmProcessInstanceStatusEnum.APPROVE.getStatus())) {
            approveByBpm(correction, processInstanceId, operatorUserId);
            return;
        }
        if (Objects.equals(bpmStatus, BpmProcessInstanceStatusEnum.REJECT.getStatus())) {
            closeByBpm(correction, STATUS_REJECTED, processInstanceId, operatorUserId, "BPM审批驳回");
            return;
        }
        if (Objects.equals(bpmStatus, BpmProcessInstanceStatusEnum.CANCEL.getStatus())) {
            closeByBpm(correction, STATUS_CANCELED, processInstanceId, operatorUserId, "BPM流程撤销");
        }
    }

    private String startCorrectionApprovalProcess(AttendanceCorrectionDO correction, EmployeeProfileDO profile) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.UNAUTHORIZED, "补卡外勤发起人不能为空");
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("correctionId", correction.getId());
        variables.put("profileId", correction.getProfileId());
        variables.put("userId", correction.getUserId());
        variables.put("employeeName", profile.getName());
        variables.put("applyType", correction.getApplyType());
        variables.put("clockType", correction.getClockType());
        variables.put("attendanceDate", correction.getAttendanceDate());
        variables.put("clockTime", correction.getClockTime());
        variables.put("locationName", correction.getLocationName());
        variables.put("locationAddress", correction.getLocationAddress());
        variables.put("reason", correction.getReason());
        return processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO()
                        .setProcessDefinitionKey(PROCESS_KEY)
                        .setBusinessKey(String.valueOf(correction.getId()))
                        .setVariables(variables))
                .getCheckedData();
    }

    private void approveByBpm(AttendanceCorrectionDO correction, String processInstanceId, Long operatorUserId) {
        if (STATUS_APPROVED.equals(correction.getStatus())) {
            return;
        }
        if (!STATUS_PENDING.equals(correction.getStatus())) {
            log.warn("补卡外勤 BPM 通过回调忽略非待审批记录，id={}, status={}", correction.getId(), correction.getStatus());
            return;
        }
        Long clockRecordId = upsertClockRecord(correction);
        AttendanceCorrectionDO updateDO = new AttendanceCorrectionDO();
        updateDO.setId(correction.getId());
        updateDO.setStatus(STATUS_APPROVED);
        updateDO.setProcessInstanceId(StringUtils.hasText(processInstanceId)
                ? processInstanceId : correction.getProcessInstanceId());
        updateDO.setApproverId(operatorUserId);
        updateDO.setApprovedTime(LocalDateTime.now());
        updateDO.setApproveRemark("BPM审批通过");
        updateDO.setClockRecordId(clockRecordId);
        attendanceCorrectionMapper.updateById(updateDO);
        recalculateDay(correction);
    }

    private void closeByBpm(AttendanceCorrectionDO correction, String status,
                            String processInstanceId, Long operatorUserId, String remark) {
        if (status.equals(correction.getStatus())) {
            return;
        }
        if (!STATUS_PENDING.equals(correction.getStatus())) {
            log.warn("补卡外勤 BPM 关闭回调忽略非待审批记录，id={}, status={}, targetStatus={}",
                    correction.getId(), correction.getStatus(), status);
            return;
        }
        AttendanceCorrectionDO updateDO = new AttendanceCorrectionDO();
        updateDO.setId(correction.getId());
        updateDO.setStatus(status);
        updateDO.setProcessInstanceId(StringUtils.hasText(processInstanceId)
                ? processInstanceId : correction.getProcessInstanceId());
        updateDO.setApproverId(operatorUserId);
        updateDO.setApprovedTime(LocalDateTime.now());
        updateDO.setApproveRemark(remark);
        attendanceCorrectionMapper.updateById(updateDO);
    }

    private EmployeeProfileDO resolveApplyProfile(AttendanceCorrectionApplyReqVO reqVO) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.UNAUTHORIZED);
        }
        boolean manage = canManage();
        Long targetUserId = reqVO.getUserId();
        Long targetProfileId = reqVO.getProfileId();
        if ((targetUserId != null || targetProfileId != null) && !manage) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.FORBIDDEN, "无权替他人提交补卡/外勤申请");
        }
        EmployeeProfileDO profile;
        if (targetProfileId != null) {
            profile = employeeProfileMapper.selectById(targetProfileId);
        } else {
            profile = employeeProfileMapper.selectByUserId(targetUserId == null ? loginUserId : targetUserId);
        }
        if (profile == null || profile.getUserId() == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "员工档案不存在或未绑定登录用户");
        }
        if (targetUserId != null && !targetUserId.equals(profile.getUserId())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "员工档案和用户不匹配");
        }
        return profile;
    }

    private Long upsertClockRecord(AttendanceCorrectionDO correction) {
        String sourceType = APPLY_TYPE_FIELD.equals(correction.getApplyType()) ? SOURCE_FIELD : SOURCE_CORRECTION;
        String sourceRecordId = buildSourceRecordId(correction.getId());
        AttendanceClockRecordDO existed = attendanceClockRecordMapper.selectBySourceRecordId(sourceRecordId);
        AttendanceClockRecordDO record = new AttendanceClockRecordDO();
        record.setUserId(correction.getUserId());
        record.setProfileId(correction.getProfileId());
        record.setAttendanceDate(correction.getAttendanceDate());
        record.setClockType(correction.getClockType());
        record.setClockTime(correction.getClockTime());
        record.setClockStatus(resolveClockStatus(correction.getClockType(), correction.getClockTime().toLocalTime()));
        record.setSourceType(sourceType);
        record.setSourceRecordId(sourceRecordId);
        record.setLocationName(correction.getLocationName());
        record.setLocationAddress(correction.getLocationAddress());
        record.setDeviceInfo("attendance-correction");
        record.setRemark(buildClockRemark(correction));
        record.setRawPayload(correction.getAttachmentJson());
        record.setSyncTime(LocalDateTime.now());
        if (existed == null) {
            attendanceClockRecordMapper.insert(record);
            return record.getId();
        }
        record.setId(existed.getId());
        attendanceClockRecordMapper.updateById(record);
        return existed.getId();
    }

    private void recalculateDay(AttendanceCorrectionDO correction) {
        AttendanceCalculateReqVO calculateReqVO = new AttendanceCalculateReqVO();
        calculateReqVO.setAttendanceDate(correction.getAttendanceDate());
        calculateReqVO.setUserId(correction.getUserId());
        calculateReqVO.setProfileId(correction.getProfileId());
        attendanceResultService.calculateDay(calculateReqVO);
    }

    private void normalizePageReq(AttendanceCorrectionPageReqVO pageReqVO) {
        if (StringUtils.hasText(pageReqVO.getApplyType())) {
            pageReqVO.setApplyType(normalizeApplyType(pageReqVO.getApplyType()));
        }
        if (StringUtils.hasText(pageReqVO.getClockType())) {
            pageReqVO.setClockType(normalizeClockType(pageReqVO.getClockType()));
        }
        if (StringUtils.hasText(pageReqVO.getStatus())) {
            pageReqVO.setStatus(pageReqVO.getStatus().trim().toUpperCase());
        }
    }

    private String normalizeApplyType(String applyType) {
        String value = applyType == null ? "" : applyType.trim().toUpperCase();
        if (APPLY_TYPE_CORRECTION.equals(value) || APPLY_TYPE_FIELD.equals(value)) {
            return value;
        }
        throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "申请类型不合法");
    }

    private String normalizeClockType(String clockType) {
        String value = clockType == null ? "" : clockType.trim().toUpperCase();
        if (CLOCK_TYPE_IN.equals(value) || CLOCK_TYPE_OUT.equals(value)) {
            return value;
        }
        throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "打卡类型不合法");
    }

    private LocalDateTime normalizeClockTime(LocalDateTime clockTime) {
        if (clockTime == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "补卡时间不能为空");
        }
        return clockTime.withNano(0);
    }

    private String resolveClockStatus(String clockType, LocalTime clockTime) {
        if (CLOCK_TYPE_IN.equals(clockType) && clockTime.isAfter(WORK_START_TIME)) {
            return CLOCK_STATUS_LATE;
        }
        if (CLOCK_TYPE_OUT.equals(clockType) && clockTime.isBefore(WORK_END_TIME)) {
            return CLOCK_STATUS_EARLY;
        }
        return CLOCK_STATUS_NORMAL;
    }

    private String buildSourceRecordId(Long id) {
        return "attendance-correction-" + id;
    }

    private String buildClockRemark(AttendanceCorrectionDO correction) {
        String prefix = APPLY_TYPE_FIELD.equals(correction.getApplyType()) ? "外勤打卡" : "补卡";
        if (!StringUtils.hasText(correction.getReason())) {
            return prefix;
        }
        return prefix + ": " + correction.getReason().trim();
    }

    private void fillPeopleInfo(List<AttendanceCorrectionDO> rows, List<AttendanceCorrectionRespVO> respList) {
        Set<Long> userIds = new HashSet<>();
        Set<Long> profileIds = new HashSet<>();
        for (AttendanceCorrectionDO row : rows) {
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
        for (AttendanceCorrectionRespVO item : respList) {
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
            log.warn("Failed to load admin users for attendance correction page: {}", ex.getMessage());
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
            return securityFrameworkService.hasPermission(PERMISSION_CORRECTION_APPROVE)
                    || securityFrameworkService.hasPermission(PERMISSION_QUERY_ALL);
        } catch (Exception ex) {
            log.warn("check attendance correction manage permission failed: {}", ex.getMessage());
            return false;
        }
    }

}
