package com.kyx.service.hr.service.selfservice;

import com.kyx.foundation.common.exception.enums.GlobalErrorCodeConstants;
import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.foundation.security.core.util.SecurityFrameworkUtils;
import com.kyx.service.hr.controller.admin.selfservice.vo.HrSelfServiceApplicationPageReqVO;
import com.kyx.service.hr.controller.admin.selfservice.vo.HrSelfServiceApplicationRespVO;
import com.kyx.service.hr.dal.dataobject.administrative.HrAdministrativeLeaveDO;
import com.kyx.service.hr.dal.dataobject.administrative.HrAdministrativeTripDO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceCorrectionDO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceOvertimeDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeDocumentRequestDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeProfileChangeRequestDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeProfileDO;
import com.kyx.service.hr.dal.mysql.administrative.HrAdministrativeLeaveMapper;
import com.kyx.service.hr.dal.mysql.administrative.HrAdministrativeTripMapper;
import com.kyx.service.hr.dal.mysql.attendance.AttendanceCorrectionMapper;
import com.kyx.service.hr.dal.mysql.attendance.AttendanceOvertimeMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeDocumentRequestMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeProfileChangeRequestMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeProfileMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Service
@Validated
public class HrSelfServiceApplicationServiceImpl implements HrSelfServiceApplicationService {

    private static final int SOURCE_LIMIT = 200;

    private static final String BUSINESS_LEAVE = "LEAVE";
    private static final String BUSINESS_TRIP = "TRIP";
    private static final String BUSINESS_ATTENDANCE_CORRECTION = "ATTENDANCE_CORRECTION";
    private static final String BUSINESS_ATTENDANCE_OVERTIME = "ATTENDANCE_OVERTIME";
    private static final String BUSINESS_DOCUMENT_REQUEST = "DOCUMENT_REQUEST";
    private static final String BUSINESS_PROFILE_CHANGE = "PROFILE_CHANGE";

    @Resource
    private HrAdministrativeLeaveMapper leaveMapper;
    @Resource
    private HrAdministrativeTripMapper tripMapper;
    @Resource
    private AttendanceCorrectionMapper correctionMapper;
    @Resource
    private AttendanceOvertimeMapper overtimeMapper;
    @Resource
    private EmployeeDocumentRequestMapper documentRequestMapper;
    @Resource
    private EmployeeProfileChangeRequestMapper profileChangeRequestMapper;
    @Resource
    private EmployeeProfileMapper employeeProfileMapper;

    @Override
    public PageResult<HrSelfServiceApplicationRespVO> getApplicationPage(HrSelfServiceApplicationPageReqVO reqVO) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.UNAUTHORIZED);
        }
        String businessType = normalize(reqVO.getBusinessType());
        String status = normalize(reqVO.getStatus());
        EmployeeProfileDO profile = employeeProfileMapper.selectByUserId(userId);
        List<HrSelfServiceApplicationRespVO> applications = new ArrayList<>();
        if (matchesBusiness(businessType, BUSINESS_LEAVE)) {
            addLeaveApplications(applications, userId, profile);
        }
        if (matchesBusiness(businessType, BUSINESS_TRIP)) {
            addTripApplications(applications, userId, profile);
        }
        if (matchesBusiness(businessType, BUSINESS_ATTENDANCE_CORRECTION)) {
            addCorrectionApplications(applications, userId, profile);
        }
        if (matchesBusiness(businessType, BUSINESS_ATTENDANCE_OVERTIME)) {
            addOvertimeApplications(applications, userId, profile);
        }
        if (matchesBusiness(businessType, BUSINESS_DOCUMENT_REQUEST)) {
            addDocumentApplications(applications, userId, profile);
        }
        if (matchesBusiness(businessType, BUSINESS_PROFILE_CHANGE)) {
            addProfileChangeApplications(applications, userId, profile);
        }
        if (StringUtils.hasText(status)) {
            applications.removeIf(item -> !status.equals(item.getStatus()));
        }
        applications.sort(this::compareApplication);
        return page(applications, reqVO);
    }

    private void addLeaveApplications(List<HrSelfServiceApplicationRespVO> applications, Long userId, EmployeeProfileDO profile) {
        List<HrAdministrativeLeaveDO> rows = leaveMapper.selectList(new LambdaQueryWrapperX<HrAdministrativeLeaveDO>()
                .eq(HrAdministrativeLeaveDO::getUserId, userId)
                .orderByDesc(HrAdministrativeLeaveDO::getId)
                .last("LIMIT " + SOURCE_LIMIT));
        for (HrAdministrativeLeaveDO row : rows) {
            HrSelfServiceApplicationRespVO item = baseItem(BUSINESS_LEAVE, row.getId(), "请假申请",
                    bpmStatus(row.getStatus()), dateToLocalDateTime(row.getCreateTime()), profile);
            item.setTitle("请假申请");
            item.setSummary(join("类型：" + defaultText(row.getLeaveType(), row.getLeaveCategory()),
                    row.getDuration() == null ? null : "时长：" + strip(row.getDuration()),
                    row.getRemark()));
            item.setStartTime(row.getStartTime());
            item.setEndTime(row.getEndTime());
            item.setRoutePath("/administrative/leave/detail?id=" + row.getId());
            applications.add(item);
        }
    }

    private void addTripApplications(List<HrSelfServiceApplicationRespVO> applications, Long userId, EmployeeProfileDO profile) {
        List<HrAdministrativeTripDO> rows = tripMapper.selectList(new LambdaQueryWrapperX<HrAdministrativeTripDO>()
                .eq(HrAdministrativeTripDO::getUserId, userId)
                .orderByDesc(HrAdministrativeTripDO::getId)
                .last("LIMIT " + SOURCE_LIMIT));
        for (HrAdministrativeTripDO row : rows) {
            HrSelfServiceApplicationRespVO item = baseItem(BUSINESS_TRIP, row.getId(), "出差申请",
                    bpmStatus(row.getStatus()), dateToLocalDateTime(row.getCreateTime()), profile);
            item.setSummary(join(defaultText(row.getDestinationCity(), row.getDestinationAddress()),
                    row.getDuration() == null ? null : "时长：" + strip(row.getDuration()) + "天",
                    row.getPurpose()));
            item.setStartTime(row.getStartTime());
            item.setEndTime(row.getEndTime());
            item.setRoutePath("/administrative/trip/detail?id=" + row.getId());
            applications.add(item);
        }
    }

    private void addCorrectionApplications(List<HrSelfServiceApplicationRespVO> applications, Long userId, EmployeeProfileDO profile) {
        List<AttendanceCorrectionDO> rows = correctionMapper.selectList(new LambdaQueryWrapperX<AttendanceCorrectionDO>()
                .eq(AttendanceCorrectionDO::getUserId, userId)
                .orderByDesc(AttendanceCorrectionDO::getAttendanceDate)
                .orderByDesc(AttendanceCorrectionDO::getId)
                .last("LIMIT " + SOURCE_LIMIT));
        for (AttendanceCorrectionDO row : rows) {
            String title = "FIELD".equals(row.getApplyType()) ? "外勤打卡申请" : "补卡申请";
            HrSelfServiceApplicationRespVO item = baseItem(BUSINESS_ATTENDANCE_CORRECTION, row.getId(), title,
                    normalize(row.getStatus()), dateToLocalDateTime(row.getCreateTime()), profile);
            item.setSummary(join(row.getAttendanceDate() == null ? null : "日期：" + row.getAttendanceDate(),
                    row.getClockType() == null ? null : "类型：" + row.getClockType(),
                    row.getReason()));
            item.setStartTime(row.getClockTime());
            item.setEndTime(row.getClockTime());
            item.setRoutePath("/attendance/corrections");
            applications.add(item);
        }
    }

    private void addOvertimeApplications(List<HrSelfServiceApplicationRespVO> applications, Long userId, EmployeeProfileDO profile) {
        List<AttendanceOvertimeDO> rows = overtimeMapper.selectList(new LambdaQueryWrapperX<AttendanceOvertimeDO>()
                .eq(AttendanceOvertimeDO::getUserId, userId)
                .orderByDesc(AttendanceOvertimeDO::getOvertimeDate)
                .orderByDesc(AttendanceOvertimeDO::getId)
                .last("LIMIT " + SOURCE_LIMIT));
        for (AttendanceOvertimeDO row : rows) {
            HrSelfServiceApplicationRespVO item = baseItem(BUSINESS_ATTENDANCE_OVERTIME, row.getId(), "加班调休申请",
                    normalize(row.getStatus()), dateToLocalDateTime(row.getCreateTime()), profile);
            item.setSummary(join(row.getOvertimeDate() == null ? null : "日期：" + row.getOvertimeDate(),
                    row.getDurationHours() == null ? null : "时长：" + strip(row.getDurationHours()) + "小时",
                    Boolean.TRUE.equals(row.getConvertToLeave()) ? "转调休" : null,
                    row.getReason()));
            item.setStartTime(row.getStartTime());
            item.setEndTime(row.getEndTime());
            item.setRoutePath("/attendance/overtime");
            applications.add(item);
        }
    }

    private void addDocumentApplications(List<HrSelfServiceApplicationRespVO> applications, Long userId, EmployeeProfileDO profile) {
        List<EmployeeDocumentRequestDO> rows = documentRequestMapper.selectList(new LambdaQueryWrapperX<EmployeeDocumentRequestDO>()
                .eq(EmployeeDocumentRequestDO::getUserId, userId)
                .orderByDesc(EmployeeDocumentRequestDO::getId)
                .last("LIMIT " + SOURCE_LIMIT));
        for (EmployeeDocumentRequestDO row : rows) {
            HrSelfServiceApplicationRespVO item = baseItem(BUSINESS_DOCUMENT_REQUEST, row.getId(),
                    defaultText(row.getTitle(), "证明申请"), normalize(row.getStatus()),
                    dateToLocalDateTime(row.getCreateTime()), profile);
            item.setSummary(join(row.getRequestType(), row.getPurpose(),
                    row.getExpectedDate() == null ? null : "期望：" + row.getExpectedDate()));
            item.setStartTime(localDateToStart(row.getExpectedDate()));
            item.setEndTime(localDateToStart(row.getExpectedDate()));
            item.setRoutePath("/hr/document-request?scope=mine");
            applications.add(item);
        }
    }

    private void addProfileChangeApplications(List<HrSelfServiceApplicationRespVO> applications, Long userId, EmployeeProfileDO profile) {
        List<EmployeeProfileChangeRequestDO> rows = profileChangeRequestMapper.selectList(new LambdaQueryWrapperX<EmployeeProfileChangeRequestDO>()
                .eq(EmployeeProfileChangeRequestDO::getUserId, userId)
                .orderByDesc(EmployeeProfileChangeRequestDO::getId)
                .last("LIMIT " + SOURCE_LIMIT));
        for (EmployeeProfileChangeRequestDO row : rows) {
            HrSelfServiceApplicationRespVO item = baseItem(BUSINESS_PROFILE_CHANGE, row.getId(), "资料变更申请",
                    normalize(row.getStatus()), dateToLocalDateTime(row.getCreateTime()), profile);
            item.setSummary(join(row.getChangeSummary(), row.getReason()));
            item.setStartTime(dateToLocalDateTime(row.getCreateTime()));
            item.setEndTime(row.getApprovedTime());
            item.setRoutePath("/hr/profile-change?scope=mine");
            applications.add(item);
        }
    }

    private HrSelfServiceApplicationRespVO baseItem(String businessType, Long businessId, String title,
                                                   String status, LocalDateTime applyTime, EmployeeProfileDO profile) {
        HrSelfServiceApplicationRespVO item = new HrSelfServiceApplicationRespVO();
        item.setBusinessType(businessType);
        item.setBusinessId(businessId);
        item.setTitle(title);
        item.setStatus(status);
        item.setStatusText(statusText(status));
        item.setApplyTime(applyTime);
        if (profile != null) {
            item.setProfileId(profile.getId());
            item.setProfileName(profile.getName());
        }
        return item;
    }

    private PageResult<HrSelfServiceApplicationRespVO> page(List<HrSelfServiceApplicationRespVO> applications,
                                                            HrSelfServiceApplicationPageReqVO reqVO) {
        int pageNo = reqVO.getPageNo() == null || reqVO.getPageNo() < 1 ? 1 : reqVO.getPageNo();
        int pageSize = reqVO.getPageSize() == null || reqVO.getPageSize() < 1 ? 20 : reqVO.getPageSize();
        int fromIndex = Math.min((pageNo - 1) * pageSize, applications.size());
        int toIndex = Math.min(fromIndex + pageSize, applications.size());
        return new PageResult<>(new ArrayList<>(applications.subList(fromIndex, toIndex)), (long) applications.size());
    }

    private int compareApplication(HrSelfServiceApplicationRespVO left, HrSelfServiceApplicationRespVO right) {
        LocalDateTime leftTime = left.getApplyTime();
        LocalDateTime rightTime = right.getApplyTime();
        if (leftTime == null && rightTime == null) {
            return Long.compare(defaultLong(right.getBusinessId()), defaultLong(left.getBusinessId()));
        }
        if (leftTime == null) {
            return 1;
        }
        if (rightTime == null) {
            return -1;
        }
        int timeCompare = rightTime.compareTo(leftTime);
        if (timeCompare != 0) {
            return timeCompare;
        }
        return Long.compare(defaultLong(right.getBusinessId()), defaultLong(left.getBusinessId()));
    }

    private boolean matchesBusiness(String queryBusinessType, String businessType) {
        return !StringUtils.hasText(queryBusinessType) || businessType.equals(queryBusinessType);
    }

    private String bpmStatus(Integer status) {
        if (status == null) {
            return "UNKNOWN";
        }
        if (status == 1) {
            return "RUNNING";
        }
        if (status == 2) {
            return "APPROVED";
        }
        if (status == 3) {
            return "REJECTED";
        }
        if (status == 4) {
            return "CANCELED";
        }
        return "UNKNOWN";
    }

    private String statusText(String status) {
        if ("PENDING".equals(status) || "RUNNING".equals(status)) {
            return "处理中";
        }
        if ("PROCESSING".equals(status)) {
            return "办理中";
        }
        if ("APPROVED".equals(status) || "COMPLETED".equals(status)) {
            return "已完成";
        }
        if ("REJECTED".equals(status)) {
            return "已驳回";
        }
        if ("CANCELED".equals(status)) {
            return "已撤销";
        }
        return "未知";
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String join(String... pieces) {
        List<String> values = new ArrayList<>();
        for (String piece : pieces) {
            if (StringUtils.hasText(piece)) {
                values.add(piece);
            }
        }
        return String.join("，", values);
    }

    private String strip(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private LocalDateTime dateToLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    private LocalDateTime localDateToStart(LocalDate date) {
        return date == null ? null : LocalDateTime.of(date, LocalTime.MIN);
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

}
