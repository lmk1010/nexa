package com.kyx.service.hr.service.employee;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kyx.foundation.common.exception.enums.GlobalErrorCodeConstants;
import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.common.util.json.JsonUtils;
import com.kyx.foundation.common.util.servlet.ServletUtils;
import com.kyx.foundation.security.core.util.SecurityFrameworkUtils;
import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.service.business.api.dept.DeptApi;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.UserOnboardingCreateReqDTO;
import com.kyx.service.business.api.user.dto.UserOnboardingRespDTO;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeCreateReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeCurrentRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeOverviewRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePageReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeStatisticsRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeStatisticsTrendRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeUpdateReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeWorkspaceOverviewRespVO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeAttachmentDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeEducationDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeEntryDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeFamilyDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeGrowthLogDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeOperationLogDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeePointsAccountDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeePointsDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeProfileDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeAttendanceStatDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeInventoryDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeePerformanceDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeRecruitmentDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeSalaryDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeTrainingDO;
import com.kyx.service.hr.dal.mysql.employee.EmployeeAttachmentMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeEducationMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeEntryMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeFamilyMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeGrowthLogMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeInventoryMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeOperationLogMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeePerformanceMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeePointsAccountMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeePointsMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeProfileMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeRecruitmentMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeSalaryMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeTrainingMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeAttendanceStatMapper;
import com.kyx.service.hr.enums.ErrorCodeConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 员工花名册 Service 实现类
 *
 * @author MK
 */
@Service
@Validated
@Slf4j
public class EmployeeServiceImpl implements EmployeeService {

    @Resource
    private EmployeeMapper employeeMapper;

    @Resource
    private EmployeeProfileMapper employeeProfileMapper;

    @Resource
    private EmployeeOperationLogMapper employeeOperationLogMapper;

    @Resource
    private EmployeeEntryMapper employeeEntryMapper;

    @Resource
    private EmployeeEducationMapper employeeEducationMapper;

    @Resource
    private EmployeeFamilyMapper employeeFamilyMapper;

    @Resource
    private EmployeeAttachmentMapper employeeAttachmentMapper;

    @Resource
    private EmployeeGrowthLogMapper employeeGrowthLogMapper;

    @Resource
    private EmployeePointsMapper employeePointsMapper;

    @Resource
    private EmployeePointsAccountMapper employeePointsAccountMapper;

    @Resource
    private EmployeeAttendanceStatMapper employeeAttendanceStatMapper;

    @Resource
    private EmployeeRecruitmentMapper employeeRecruitmentMapper;

    @Resource
    private EmployeeSalaryMapper employeeSalaryMapper;

    @Resource
    private EmployeePerformanceMapper employeePerformanceMapper;

    @Resource
    private EmployeeTrainingMapper employeeTrainingMapper;

    @Resource
    private EmployeeInventoryMapper employeeInventoryMapper;

    @Resource
    private EmployeeMasterDataService employeeMasterDataService;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private DeptApi deptApi;

    @Override
    public PageResult<EmployeeRespVO> getEmployeePage(EmployeePageReqVO pageReqVO) {
        // 创建MyBatis Plus分页对象
        IPage<EmployeeRespVO> page = new Page<>(pageReqVO.getPageNo(), pageReqVO.getPageSize());

        // 获取当前租户ID，如果开启全局视图则不传租户ID
        Long tenantId = TenantContextHolder.isIgnore() ? null : TenantContextHolder.getTenantId();

        // 执行分页查询
        IPage<EmployeeRespVO> result = employeeMapper.selectPage(page, pageReqVO, tenantId);

        // 转换为PageResult
        return new PageResult<>(result.getRecords(), result.getTotal());
    }

    @Override
    public EmployeeRespVO getEmployee(Long id) {
        // 如果开启全局视图则不传租户ID
        Long tenantId = TenantContextHolder.isIgnore() ? null : TenantContextHolder.getTenantId();
        return employeeMapper.selectEmployeeById(id, tenantId);
    }

    @Override
    public EmployeeCurrentRespVO getCurrentEmployee() {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            return null;
        }

        EmployeeProfileDO profile = employeeProfileMapper.selectByUserId(loginUserId);
        if (profile == null) {
            return null;
        }

        EmployeeCurrentRespVO respVO = new EmployeeCurrentRespVO();
        respVO.setProfileId(profile.getId());
        respVO.setUserId(loginUserId);
        respVO.setName(profile.getName());
        respVO.setMobile(profile.getMobile());
        respVO.setEmail(profile.getEmail());
        return respVO;
    }

    @Override
    public EmployeeStatisticsRespVO getEmployeeStatistics() {
        // 如果开启全局视图则不传租户ID
        Long tenantId = TenantContextHolder.isIgnore() ? null : TenantContextHolder.getTenantId();
        return employeeMapper.selectEmployeeStatistics(tenantId);
    }

    @Override
    public EmployeeStatisticsTrendRespVO getEmployeeStatisticsTrend(Integer months) {
        int monthCount = normalizeTrendMonths(months);
        Long tenantId = TenantContextHolder.isIgnore() ? null : TenantContextHolder.getTenantId();
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("M月");

        List<String> monthLabels = new ArrayList<>(monthCount);
        List<Integer> totalTrend = new ArrayList<>(monthCount);
        List<Integer> activeTrend = new ArrayList<>(monthCount);
        List<Integer> probationTrend = new ArrayList<>(monthCount);
        List<Integer> fullTimeTrend = new ArrayList<>(monthCount);
        List<Integer> partTimeTrend = new ArrayList<>(monthCount);
        List<Integer> formalTrend = new ArrayList<>(monthCount);
        List<Integer> leavingTrend = new ArrayList<>(monthCount);
        List<Integer> onboardingTrend = new ArrayList<>(monthCount);
        List<Double> avgAgeTrend = new ArrayList<>(monthCount);
        List<Integer> stabilityTrend = new ArrayList<>(monthCount);

        for (int i = monthCount - 1; i >= 0; i--) {
            LocalDate monthEnd = monthStart.minusMonths(i).with(TemporalAdjusters.lastDayOfMonth());
            EmployeeStatisticsRespVO snapshot = employeeMapper.selectEmployeeStatisticsByDate(tenantId, monthEnd);
            EmployeeOverviewRespVO.Age ageSnapshot = employeeMapper.selectEmployeeOverviewAgeByDate(tenantId, monthEnd);
            if (snapshot == null) {
                snapshot = new EmployeeStatisticsRespVO();
            }
            if (ageSnapshot == null) {
                ageSnapshot = new EmployeeOverviewRespVO.Age();
            }

            monthLabels.add(monthEnd.format(labelFormatter));
            totalTrend.add(orZero(snapshot.getTotal()));
            activeTrend.add(orZero(snapshot.getActive()));
            probationTrend.add(orZero(snapshot.getProbation()));
            fullTimeTrend.add(orZero(snapshot.getFullTime()));
            partTimeTrend.add(orZero(snapshot.getPartTime()));
            formalTrend.add(orZero(snapshot.getFormal()));
            leavingTrend.add(orZero(snapshot.getLeaving()));
            onboardingTrend.add(orZero(snapshot.getOnboarding()));
            avgAgeTrend.add(orZeroDecimal(ageSnapshot.getAvgAge()).doubleValue());
            stabilityTrend.add(orZero(ageSnapshot.getStability()));
        }

        EmployeeStatisticsTrendRespVO respVO = new EmployeeStatisticsTrendRespVO();
        respVO.setMonths(monthLabels);
        respVO.setTotalTrend(totalTrend);
        respVO.setActiveTrend(activeTrend);
        respVO.setProbationTrend(probationTrend);
        respVO.setFullTimeTrend(fullTimeTrend);
        respVO.setPartTimeTrend(partTimeTrend);
        respVO.setFormalTrend(formalTrend);
        respVO.setLeavingTrend(leavingTrend);
        respVO.setOnboardingTrend(onboardingTrend);
        respVO.setAvgAgeTrend(avgAgeTrend);
        respVO.setStabilityTrend(stabilityTrend);
        return respVO;
    }

    @Override
    public EmployeeOverviewRespVO getEmployeeOverview() {
        Long tenantId = TenantContextHolder.isIgnore() ? null : TenantContextHolder.getTenantId();
        EmployeeOverviewRespVO.Scale scale = employeeMapper.selectEmployeeOverviewScale(tenantId);
        EmployeeOverviewRespVO.Age age = employeeMapper.selectEmployeeOverviewAge(tenantId);
        EmployeeOverviewRespVO.Education education = employeeMapper.selectEmployeeOverviewEducation(tenantId);
        java.util.List<EmployeeOverviewRespVO.PostStat> postStats = employeeMapper.selectEmployeeOverviewPostStats(tenantId);

        if (scale == null) {
            scale = new EmployeeOverviewRespVO.Scale();
        }
        if (age == null) {
            age = new EmployeeOverviewRespVO.Age();
        }
        if (education == null) {
            education = new EmployeeOverviewRespVO.Education();
        }
        if (postStats == null) {
            postStats = java.util.Collections.emptyList();
        }

        scale.setTotal(orZero(scale.getTotal()));
        scale.setAdmin(orZero(scale.getAdmin()));
        scale.setAdminFront(orZero(scale.getAdminFront()));
        scale.setAdminLogistics(orZero(scale.getAdminLogistics()));
        scale.setHr(orZero(scale.getHr()));
        scale.setHrRecruit(orZero(scale.getHrRecruit()));
        scale.setHrSalary(orZero(scale.getHrSalary()));

        age.setUnder30(orZero(age.getUnder30()));
        age.setOver3Years(orZero(age.getOver3Years()));
        age.setStability(orZero(age.getStability()));

        education.setMaster(orZero(education.getMaster()));
        education.setBachelor(orZero(education.getBachelor()));
        education.setRelated(orZero(education.getRelated()));

        if (scale.getTotal() > 0) {
            education.setMasterPercent((int) Math.round(education.getMaster() * 100.0 / scale.getTotal()));
        } else {
            education.setMasterPercent(0);
        }

        EmployeeOverviewRespVO overview = new EmployeeOverviewRespVO();
        overview.setScale(scale);
        overview.setAge(age);
        overview.setEducation(education);
        overview.setPostStats(postStats);
        return overview;
    }

    @Override
    @Cacheable(cacheNames = "hr:employee:workspace-overview#60s",
            key = "'tenant:' + T(com.kyx.foundation.tenant.core.context.TenantContextHolder).getTenantId() + ':months:' + #months",
            sync = true)
    public EmployeeWorkspaceOverviewRespVO getEmployeeWorkspaceOverview(Integer months) {
        int monthCount = normalizeTrendMonths(months);
        EmployeeWorkspaceOverviewRespVO respVO = new EmployeeWorkspaceOverviewRespVO();
        respVO.setStatistics(getEmployeeStatistics());
        respVO.setOverview(getEmployeeOverview());
        respVO.setTrend(getEmployeeStatisticsTrend(monthCount));
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEmployee(EmployeeUpdateReqVO updateReqVO) {
        EmployeeProfileDO profile = employeeProfileMapper.selectById(updateReqVO.getId());
        if (profile == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_PROFILE_NOT_EXISTS);
        }
        validateLifecycleManagedProfileUpdate(profile, updateReqVO);

        EmployeeProfileDO updateObj = BeanUtils.toBean(updateReqVO, EmployeeProfileDO.class);
        updateObj.setHometown(updateReqVO.getNativePlace());
        employeeProfileMapper.updateById(updateObj);

        EmployeeProfileDO updatedProfile = employeeProfileMapper.selectById(updateReqVO.getId());
        recordProfileOperationLog(profile, updatedProfile);
        employeeMasterDataService.recordProfileChanges(profile, updatedProfile, "PROFILE_UPDATE", updateReqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEmployee(EmployeeCreateReqVO createReqVO) {
        if (createReqVO.getMobile() != null && !createReqVO.getMobile().trim().isEmpty()) {
            if (!employeeProfileMapper.selectListByMobile(createReqVO.getMobile()).isEmpty()) {
                throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_MOBILE_EXISTS);
            }
        }
        if (createReqVO.getEmail() != null && !createReqVO.getEmail().trim().isEmpty()) {
            if (!employeeProfileMapper.selectListByEmail(createReqVO.getEmail()).isEmpty()) {
                throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_EMAIL_EXISTS);
            }
        }

        CommonResult<Boolean> deptValid = deptApi.validateDeptList(Collections.singleton(createReqVO.getDeptId()));
        if (!Boolean.TRUE.equals(deptValid.getCheckedData())) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_DEPT_NOT_EXISTS);
        }

        String profileNo = generateProfileNo();
        EmployeeProfileDO profile = new EmployeeProfileDO();
        profile.setProfileNo(profileNo);
        profile.setName(createReqVO.getName());
        profile.setMobile(createReqVO.getMobile());
        profile.setEmail(createReqVO.getEmail());
        profile.setGender(createReqVO.getGender());
        profile.setOnboardDate(createReqVO.getOnboardDate());
        profile.setStatus(createReqVO.getStatus() != null ? createReqVO.getStatus() : 1);
        employeeProfileMapper.insert(profile);

        EmployeeEntryDO entry = new EmployeeEntryDO();
        entry.setEntryNo(generateEntryNo());
        entry.setEmployeeNo(generateEmployeeNo());
        entry.setProfileId(profile.getId());
        entry.setEntryType(1);
        entry.setProcessType(1);
        entry.setEntryDate(createReqVO.getOnboardDate() != null ? createReqVO.getOnboardDate() : LocalDate.now());
        entry.setDeptId(createReqVO.getDeptId());
        entry.setJobTitle(createReqVO.getJobTitle());
        entry.setWorkStatus(createReqVO.getWorkStatus() != null ? createReqVO.getWorkStatus() : 3);
        entry.setEmploymentType(createReqVO.getEmploymentType() != null ? createReqVO.getEmploymentType() : 1);
        employeeEntryMapper.insert(entry);

        UserOnboardingCreateReqDTO userReqDTO = new UserOnboardingCreateReqDTO();
        userReqDTO.setEmployeeName(createReqVO.getName());
        userReqDTO.setMobile(createReqVO.getMobile());
        userReqDTO.setEmail(createReqVO.getEmail());
        userReqDTO.setDeptId(createReqVO.getDeptId());
        userReqDTO.setPosition(createReqVO.getJobTitle());
        userReqDTO.setSex(createReqVO.getGender());
        userReqDTO.setOnboardingNo(entry.getEntryNo());

        CommonResult<UserOnboardingRespDTO> userResult = adminUserApi.createOnboardingUser(userReqDTO);
        UserOnboardingRespDTO userInfo = userResult.getCheckedData();
        profile.setUserId(userInfo.getUserId());
        employeeProfileMapper.updateById(profile);
        entry.setUserId(userInfo.getUserId());
        employeeEntryMapper.updateById(entry);

        return profile.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEmployee(Long id) {
        EmployeeProfileDO profile = employeeProfileMapper.selectById(id);
        if (profile == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_PROFILE_NOT_EXISTS);
        }
        employeeEntryMapper.delete(new LambdaQueryWrapperX<EmployeeEntryDO>()
                .eq(EmployeeEntryDO::getProfileId, id));
        employeeEducationMapper.delete(new LambdaQueryWrapperX<EmployeeEducationDO>()
                .eq(EmployeeEducationDO::getProfileId, id));
        employeeFamilyMapper.delete(new LambdaQueryWrapperX<EmployeeFamilyDO>()
                .eq(EmployeeFamilyDO::getProfileId, id));
        employeeAttachmentMapper.delete(new LambdaQueryWrapperX<EmployeeAttachmentDO>()
                .eq(EmployeeAttachmentDO::getProfileId, id));
        employeeGrowthLogMapper.delete(new LambdaQueryWrapperX<EmployeeGrowthLogDO>()
                .eq(EmployeeGrowthLogDO::getProfileId, id));
        employeePointsMapper.delete(new LambdaQueryWrapperX<EmployeePointsDO>()
                .eq(EmployeePointsDO::getProfileId, id));
        employeePointsAccountMapper.delete(new LambdaQueryWrapperX<EmployeePointsAccountDO>()
                .eq(EmployeePointsAccountDO::getProfileId, id));
        employeeAttendanceStatMapper.delete(new LambdaQueryWrapperX<EmployeeAttendanceStatDO>()
                .eq(EmployeeAttendanceStatDO::getProfileId, id));
        employeeRecruitmentMapper.delete(new LambdaQueryWrapperX<EmployeeRecruitmentDO>()
                .eq(EmployeeRecruitmentDO::getProfileId, id));
        employeeSalaryMapper.delete(new LambdaQueryWrapperX<EmployeeSalaryDO>()
                .eq(EmployeeSalaryDO::getProfileId, id));
        employeePerformanceMapper.delete(new LambdaQueryWrapperX<EmployeePerformanceDO>()
                .eq(EmployeePerformanceDO::getProfileId, id));
        employeeTrainingMapper.delete(new LambdaQueryWrapperX<EmployeeTrainingDO>()
                .eq(EmployeeTrainingDO::getProfileId, id));
        employeeInventoryMapper.delete(new LambdaQueryWrapperX<EmployeeInventoryDO>()
                .eq(EmployeeInventoryDO::getProfileId, id));
        employeeProfileMapper.deleteById(id);
    }

    private void recordProfileOperationLog(EmployeeProfileDO beforeProfile, EmployeeProfileDO afterProfile) {
        if (beforeProfile == null || afterProfile == null) {
            return;
        }
        EmployeeOperationLogDO log = new EmployeeOperationLogDO();
        log.setProfileId(beforeProfile.getId());
        log.setOperationType("update");
        log.setOperationModule("basic_info");
        log.setOperationTitle("员工档案内容-基础信息-修改");
        log.setOperationContent("更新员工基础信息");
        log.setBeforeData(JsonUtils.toJsonString(beforeProfile));
        log.setAfterData(JsonUtils.toJsonString(afterProfile));
        log.setOperationTime(LocalDateTime.now());

        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        String operatorName = SecurityFrameworkUtils.getLoginUserNickname();
        log.setOperatorId(operatorId != null ? operatorId : 0L);
        log.setOperatorName(operatorName != null ? operatorName : "system");
        log.setOperationIp(ServletUtils.getClientIP());
        log.setOperationSource("web");
        employeeOperationLogMapper.insert(log);
    }

    private String generateProfileNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "PROF" + dateStr;
        String maxNo = employeeProfileMapper.selectMaxProfileNoByDate(dateStr);
        int sequence = 1;
        if (maxNo != null && maxNo.startsWith(prefix)) {
            String sequenceStr = maxNo.substring(prefix.length());
            try {
                sequence = Integer.parseInt(sequenceStr) + 1;
            } catch (NumberFormatException ignored) {
                sequence = 1;
            }
        }
        return prefix + String.format("%04d", sequence);
    }

    private void validateLifecycleManagedProfileUpdate(EmployeeProfileDO beforeProfile, EmployeeUpdateReqVO reqVO) {
        if (beforeProfile == null || reqVO == null || reqVO.getConfirmationDate() == null
                || Objects.equals(reqVO.getConfirmationDate(), beforeProfile.getConfirmationDate())) {
            return;
        }
        boolean hasActiveEmployment = employeeEntryMapper.selectListByProfileId(beforeProfile.getId()).stream()
                .map(EmployeeEntryDO::getWorkStatus)
                .anyMatch(status -> Objects.equals(status, 2) || Objects.equals(status, 3));
        if (hasActiveEmployment) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST,
                    "转正日期请通过生命周期转正审批维护");
        }
    }

    private String generateEntryNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "ENTRY" + dateStr;
        String maxNo = employeeEntryMapper.selectMaxEntryNoByDate(dateStr);
        int sequence = 1;
        if (maxNo != null && maxNo.startsWith(prefix)) {
            String sequenceStr = maxNo.substring(prefix.length());
            try {
                sequence = Integer.parseInt(sequenceStr) + 1;
            } catch (NumberFormatException ignored) {
                sequence = 1;
            }
        }
        return prefix + String.format("%04d", sequence);
    }

    private String generateEmployeeNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "EMP" + dateStr;
        String maxNo = employeeEntryMapper.selectMaxEmployeeNoByDate(dateStr);
        int sequence = 1;
        if (maxNo != null && maxNo.startsWith(prefix)) {
            String sequenceStr = maxNo.substring(prefix.length());
            try {
                sequence = Integer.parseInt(sequenceStr) + 1;
            } catch (NumberFormatException ignored) {
                sequence = 1;
            }
        }
        return prefix + String.format("%04d", sequence);
    }

    private int normalizeTrendMonths(Integer months) {
        if (months == null) {
            return 6;
        }
        if (months < 3) {
            return 3;
        }
        return Math.min(months, 12);
    }

    private BigDecimal orZeroDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Integer orZero(Integer value) {
        return value == null ? 0 : value;
    }
}
