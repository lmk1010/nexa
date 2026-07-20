package com.kyx.service.hr.service.employee;

import com.kyx.foundation.common.exception.enums.GlobalErrorCodeConstants;
import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.common.util.json.JsonUtils;
import com.kyx.foundation.common.util.servlet.ServletUtils;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.foundation.security.core.util.SecurityFrameworkUtils;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeEntryPageReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeEntryRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeEntrySaveReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeEntryUpdateReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeResignationReqVO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeEntryDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeGrowthLogDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeOperationLogDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeProfileDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeEducationDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeFamilyDO;
import com.kyx.service.hr.dal.mysql.employee.EmployeeEntryMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeGrowthLogMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeOperationLogMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeProfileMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeEducationMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeFamilyMapper;
import com.kyx.service.hr.service.lifecycle.HrLifecycleService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.kyx.service.hr.enums.ErrorCodeConstants.*;

/**
 * 员工入职记录 Service 实现类
 *
 * @author MK
 */
@Service
@Validated
@Slf4j
public class EmployeeEntryServiceImpl implements EmployeeEntryService {

    @Resource
    private EmployeeEntryMapper employeeEntryMapper;

    @Resource
    private EmployeeProfileMapper employeeProfileMapper;

    @Resource
    private EmployeeEducationMapper employeeEducationMapper;

    @Resource
    private EmployeeFamilyMapper employeeFamilyMapper;

    @Resource
    private EmployeeOperationLogMapper employeeOperationLogMapper;

    @Resource
    private EmployeeGrowthLogMapper employeeGrowthLogMapper;

    @Resource
    private HrLifecycleService hrLifecycleService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEmployeeEntry(EmployeeEntrySaveReqVO createReqVO) {
        // 校验员工档案存在
        EmployeeProfileDO profile = employeeProfileMapper.selectById(createReqVO.getProfileId());
        if (profile == null) {
            throw ServiceExceptionUtil.exception(EMPLOYEE_PROFILE_NOT_EXISTS);
        }

        // 生成入职编号
        if (createReqVO.getEntryNo() == null) {
            createReqVO.setEntryNo(generateEntryNo());
        }

        // 插入
        EmployeeEntryDO employeeEntry = BeanUtils.toBean(createReqVO, EmployeeEntryDO.class);
        employeeEntryMapper.insert(employeeEntry);

        recordEntryOperationLog("create", null, employeeEntry);
        createOnboardGrowthLog(employeeEntry);
        recordContractOperationLog(null, employeeEntry, true);
        recordBankOperationLog(null, employeeEntry, true);
        recordLifecycleByEntryChange(null, employeeEntry, profile);
        // 返回
        return employeeEntry.getId();
    }

    @Override
    public void updateEmployeeEntry(EmployeeEntrySaveReqVO updateReqVO) {
        // 校验存在
        validateEmployeeEntryExists(updateReqVO.getId());
        EmployeeEntryDO beforeEntry = employeeEntryMapper.selectById(updateReqVO.getId());
        validateEmployeeEntryUpdateAllowed(beforeEntry, updateReqVO);
        // 更新
        EmployeeEntryDO updateObj = BeanUtils.toBean(updateReqVO, EmployeeEntryDO.class);
        employeeEntryMapper.updateById(updateObj);

        EmployeeEntryDO afterEntry = employeeEntryMapper.selectById(updateReqVO.getId());
        recordEntryOperationLog("update", beforeEntry, afterEntry);
        recordContractOperationLog(beforeEntry, afterEntry, false);
        recordBankOperationLog(beforeEntry, afterEntry, false);
        EmployeeProfileDO profile = afterEntry.getProfileId() == null ? null : employeeProfileMapper.selectById(afterEntry.getProfileId());
        recordLifecycleByEntryChange(beforeEntry, afterEntry, profile);
    }

    @Override
    public void updateEmployeeEntryPartial(EmployeeEntryUpdateReqVO updateReqVO) {
        validateEmployeeEntryExists(updateReqVO.getId());
        EmployeeEntryDO beforeEntry = employeeEntryMapper.selectById(updateReqVO.getId());
        validateEmployeeEntryUpdateAllowed(beforeEntry, updateReqVO);
        EmployeeEntryDO updateObj = BeanUtils.toBean(updateReqVO, EmployeeEntryDO.class);
        employeeEntryMapper.updateById(updateObj);
        EmployeeEntryDO afterEntry = employeeEntryMapper.selectById(updateReqVO.getId());
        recordEntryOperationLog("update", beforeEntry, afterEntry);
        recordContractOperationLog(beforeEntry, afterEntry, false);
        recordBankOperationLog(beforeEntry, afterEntry, false);
        EmployeeProfileDO profile = afterEntry.getProfileId() == null ? null : employeeProfileMapper.selectById(afterEntry.getProfileId());
        recordLifecycleByEntryChange(beforeEntry, afterEntry, profile);
    }

    @Override
    public void deleteEmployeeEntry(Long id) {
        // 校验存在
        EmployeeEntryDO employeeEntry = employeeEntryMapper.selectById(id);
        if (employeeEntry == null) {
            throw ServiceExceptionUtil.exception(EMPLOYEE_ENTRY_NOT_EXISTS);
        }
        if (isManagedEmploymentStatus(employeeEntry.getWorkStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "已入职员工任职记录不能直接删除");
        }
        // 删除
        employeeEntryMapper.deleteById(id);
    }

    private void validateEmployeeEntryExists(Long id) {
        if (employeeEntryMapper.selectById(id) == null) {
            throw ServiceExceptionUtil.exception(EMPLOYEE_ENTRY_NOT_EXISTS);
        }
    }

    private void recordEntryOperationLog(String operationType, EmployeeEntryDO beforeEntry, EmployeeEntryDO afterEntry) {
        if (afterEntry == null) {
            return;
        }
        EmployeeOperationLogDO log = new EmployeeOperationLogDO();
        log.setProfileId(afterEntry.getProfileId());
        log.setOperationType(operationType);
        log.setOperationModule("work_info");
        boolean isCreate = "create".equalsIgnoreCase(operationType);
        log.setOperationTitle(isCreate ? "员工档案内容-工作信息-新增" : "员工档案内容-工作信息-修改");
        log.setOperationContent(isCreate ? "新增员工工作信息" : "更新员工工作信息");
        if (beforeEntry != null) {
            log.setBeforeData(JsonUtils.toJsonString(beforeEntry));
        }
        log.setAfterData(JsonUtils.toJsonString(afterEntry));
        log.setOperationTime(LocalDateTime.now());
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        String operatorName = SecurityFrameworkUtils.getLoginUserNickname();
        log.setOperatorId(operatorId != null ? operatorId : 0L);
        log.setOperatorName(operatorName != null ? operatorName : "system");
        log.setOperationIp(ServletUtils.getClientIP());
        log.setOperationSource("web");
        employeeOperationLogMapper.insert(log);
    }

    private void recordContractOperationLog(EmployeeEntryDO beforeEntry, EmployeeEntryDO afterEntry, boolean isCreate) {
        if (afterEntry == null || afterEntry.getProfileId() == null) {
            return;
        }
        boolean hasChange = isCreate ? hasContractInfo(afterEntry) : hasContractChanged(beforeEntry, afterEntry);
        if (!hasChange) {
            return;
        }
        EmployeeOperationLogDO log = new EmployeeOperationLogDO();
        log.setProfileId(afterEntry.getProfileId());
        log.setOperationType(isCreate ? "create" : "update");
        log.setOperationModule("contract");
        log.setOperationTitle(isCreate ? "员工档案内容-合同信息-新增" : "员工档案内容-合同信息-修改");
        log.setOperationContent(isCreate ? "新增员工合同信息" : "更新员工合同信息");
        if (beforeEntry != null) {
            log.setBeforeData(JsonUtils.toJsonString(beforeEntry));
        }
        log.setAfterData(JsonUtils.toJsonString(afterEntry));
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        String operatorName = SecurityFrameworkUtils.getLoginUserNickname();
        log.setOperatorId(operatorId != null ? operatorId : 0L);
        log.setOperatorName(operatorName != null ? operatorName : "system");
        log.setOperationTime(LocalDateTime.now());
        log.setOperationIp(ServletUtils.getClientIP());
        log.setOperationSource("web");
        employeeOperationLogMapper.insert(log);
    }

    private boolean hasContractChanged(EmployeeEntryDO beforeEntry, EmployeeEntryDO afterEntry) {
        if (beforeEntry == null || afterEntry == null) {
            return false;
        }
        return !Objects.equals(beforeEntry.getContractType(), afterEntry.getContractType())
                || !Objects.equals(beforeEntry.getContractStartDate(), afterEntry.getContractStartDate())
                || !Objects.equals(beforeEntry.getContractEndDate(), afterEntry.getContractEndDate());
    }

    private boolean hasContractInfo(EmployeeEntryDO entry) {
        if (entry == null) {
            return false;
        }
        return entry.getContractType() != null
                || entry.getContractStartDate() != null
                || entry.getContractEndDate() != null;
    }

    private void recordBankOperationLog(EmployeeEntryDO beforeEntry, EmployeeEntryDO afterEntry, boolean isCreate) {
        if (afterEntry == null || afterEntry.getProfileId() == null) {
            return;
        }
        boolean hasChange = isCreate ? hasBankInfo(afterEntry) : hasBankChanged(beforeEntry, afterEntry);
        if (!hasChange) {
            return;
        }
        EmployeeOperationLogDO log = new EmployeeOperationLogDO();
        log.setProfileId(afterEntry.getProfileId());
        log.setOperationType(isCreate ? "create" : "update");
        log.setOperationModule("bank");
        log.setOperationTitle(isCreate ? "员工档案内容-银行卡信息-新增" : "员工档案内容-银行卡信息-修改");
        log.setOperationContent(isCreate ? "新增员工银行卡信息" : "更新员工银行卡信息");
        if (beforeEntry != null) {
            log.setBeforeData(JsonUtils.toJsonString(beforeEntry));
        }
        log.setAfterData(JsonUtils.toJsonString(afterEntry));
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        String operatorName = SecurityFrameworkUtils.getLoginUserNickname();
        log.setOperatorId(operatorId != null ? operatorId : 0L);
        log.setOperatorName(operatorName != null ? operatorName : "system");
        log.setOperationTime(LocalDateTime.now());
        log.setOperationIp(ServletUtils.getClientIP());
        log.setOperationSource("web");
        employeeOperationLogMapper.insert(log);
    }

    private boolean hasBankChanged(EmployeeEntryDO beforeEntry, EmployeeEntryDO afterEntry) {
        if (beforeEntry == null || afterEntry == null) {
            return false;
        }
        return !Objects.equals(beforeEntry.getBankName(), afterEntry.getBankName())
                || !Objects.equals(beforeEntry.getBankBranch(), afterEntry.getBankBranch())
                || !Objects.equals(beforeEntry.getBankAccount(), afterEntry.getBankAccount())
                || !Objects.equals(beforeEntry.getAccountName(), afterEntry.getAccountName());
    }

    private boolean hasBankInfo(EmployeeEntryDO entry) {
        if (entry == null) {
            return false;
        }
        return entry.getBankName() != null
                || entry.getBankBranch() != null
                || entry.getBankAccount() != null
                || entry.getAccountName() != null;
    }

    private void createOnboardGrowthLog(EmployeeEntryDO employeeEntry) {
        if (employeeEntry == null || employeeEntry.getProfileId() == null) {
            return;
        }
        Long profileId = employeeEntry.getProfileId();
        boolean exists = employeeGrowthLogMapper.selectCount(new LambdaQueryWrapperX<EmployeeGrowthLogDO>()
                .eq(EmployeeGrowthLogDO::getProfileId, profileId)
                .eq(EmployeeGrowthLogDO::getEventType, 1)
                .eq(EmployeeGrowthLogDO::getEventDate, employeeEntry.getEntryDate())) > 0;
        if (exists) {
            return;
        }
        EmployeeGrowthLogDO growthLog = new EmployeeGrowthLogDO();
        growthLog.setProfileId(profileId);
        growthLog.setEventType(1);
        growthLog.setEventDate(employeeEntry.getEntryDate());
        growthLog.setTitle("入职");
        growthLog.setContent("员工入职");
        growthLog.setAfterDeptId(employeeEntry.getDeptId());
        growthLog.setAfterJobTitle(employeeEntry.getJobTitle());
        growthLog.setAfterJobLevelId(employeeEntry.getJobLevelId());
        employeeGrowthLogMapper.insert(growthLog);
    }

    private void recordLifecycleByEntryChange(EmployeeEntryDO beforeEntry, EmployeeEntryDO afterEntry,
                                              EmployeeProfileDO profile) {
        if (afterEntry == null || afterEntry.getProfileId() == null) {
            return;
        }
        hrLifecycleService.recordOnboardingConfirmed(afterEntry, profile);
        boolean changedToResigned = Objects.equals(afterEntry.getWorkStatus(), 4)
                && (beforeEntry == null || !Objects.equals(beforeEntry.getWorkStatus(), 4));
        if (changedToResigned) {
            hrLifecycleService.recordResignationEffective(beforeEntry, afterEntry, profile, afterEntry.getLeaveReason());
        } else if (hasTransferInfoChanged(beforeEntry, afterEntry)) {
            hrLifecycleService.recordTransferEffective(beforeEntry, afterEntry, profile, "HR 直接维护工作信息");
        }
    }

    private void validateEmployeeEntryUpdateAllowed(EmployeeEntryDO beforeEntry, EmployeeEntrySaveReqVO reqVO) {
        validateOnboardingProcessUpdateAllowed(beforeEntry, reqVO == null ? null : reqVO.getOnboardingStatus(),
                reqVO == null ? null : reqVO.getWorkStatus());
        if (beforeEntry == null || reqVO == null || !isManagedEmploymentStatus(beforeEntry.getWorkStatus())) {
            return;
        }
        validateLifecycleManagedStatusChange(beforeEntry, reqVO.getWorkStatus(), reqVO.getLeaveDate(), reqVO.getLeaveReason());
        validateLifecycleManagedTransferChange(beforeEntry, reqVO.getDeptId(), reqVO.getJobTitle(),
                reqVO.getJobLevelId(), reqVO.getJobSequenceId(), reqVO.getWorkLocationId(),
                reqVO.getDirectSupervisorId());
    }

    private void validateEmployeeEntryUpdateAllowed(EmployeeEntryDO beforeEntry, EmployeeEntryUpdateReqVO reqVO) {
        validateOnboardingProcessUpdateAllowed(beforeEntry, null, reqVO == null ? null : reqVO.getWorkStatus());
        if (beforeEntry == null || reqVO == null || !isManagedEmploymentStatus(beforeEntry.getWorkStatus())) {
            return;
        }
        validateLifecycleManagedStatusChange(beforeEntry, reqVO.getWorkStatus(), reqVO.getLeaveDate(), reqVO.getLeaveReason());
        validateLifecycleManagedTransferChange(beforeEntry, reqVO.getDeptId(), reqVO.getJobTitle(),
                reqVO.getJobLevelId(), reqVO.getJobSequenceId(), reqVO.getWorkLocationId(),
                reqVO.getDirectSupervisorId());
    }

    private void validateOnboardingProcessUpdateAllowed(EmployeeEntryDO beforeEntry, Integer targetOnboardingStatus,
                                                        Integer targetWorkStatus) {
        if (beforeEntry == null || !StringUtils.hasText(beforeEntry.getProcessInstanceId())
                || !Objects.equals(beforeEntry.getOnboardingStatus(), 2)) {
            return;
        }
        boolean onboardingStatusChanged = targetOnboardingStatus != null
                && !Objects.equals(targetOnboardingStatus, beforeEntry.getOnboardingStatus());
        boolean confirmsOnJob = targetWorkStatus != null
                && !Objects.equals(targetWorkStatus, beforeEntry.getWorkStatus())
                && Objects.equals(targetWorkStatus, 3);
        if (onboardingStatusChanged || confirmsOnJob) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST,
                    "该入职记录已进入 BPM 流程，请在流程中心确认入职审批");
        }
    }

    private void validateLifecycleManagedStatusChange(EmployeeEntryDO beforeEntry, Integer targetWorkStatus,
                                                      LocalDate targetLeaveDate, String targetLeaveReason) {
        if (targetLeaveDate != null && !Objects.equals(targetLeaveDate, beforeEntry.getLeaveDate())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "离职请通过生命周期工作台发起审批");
        }
        if (targetLeaveReason != null && !Objects.equals(trimText(targetLeaveReason), trimText(beforeEntry.getLeaveReason()))) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "离职请通过生命周期工作台发起审批");
        }
        if (targetWorkStatus == null || Objects.equals(targetWorkStatus, beforeEntry.getWorkStatus())) {
            return;
        }
        if (Objects.equals(targetWorkStatus, 4)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "离职请通过生命周期工作台发起审批");
        }
        if (Objects.equals(beforeEntry.getWorkStatus(), 2) && Objects.equals(targetWorkStatus, 3)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "转正请通过生命周期工作台发起审批");
        }
        throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "已入职员工工作状态不能直接修改，请通过生命周期流程处理");
    }

    private void validateLifecycleManagedTransferChange(EmployeeEntryDO beforeEntry, Long targetDeptId,
                                                        String targetJobTitle, Long targetJobLevelId,
                                                        Long targetJobSequenceId, Long targetWorkLocationId,
                                                        Long targetDirectSupervisorId) {
        boolean changed = hasRequestedLongChange(targetDeptId, beforeEntry.getDeptId())
                || hasRequestedStringChange(targetJobTitle, beforeEntry.getJobTitle())
                || hasRequestedLongChange(targetJobLevelId, beforeEntry.getJobLevelId())
                || hasRequestedLongChange(targetJobSequenceId, beforeEntry.getJobSequenceId())
                || hasRequestedLongChange(targetWorkLocationId, beforeEntry.getWorkLocationId())
                || hasRequestedLongChange(targetDirectSupervisorId, beforeEntry.getDirectSupervisorId());
        if (changed) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST,
                    "部门、岗位、上级、职级等任职变更请通过生命周期调岗审批");
        }
    }

    private boolean hasTransferInfoChanged(EmployeeEntryDO beforeEntry, EmployeeEntryDO afterEntry) {
        if (beforeEntry == null || afterEntry == null) {
            return false;
        }
        return !Objects.equals(beforeEntry.getDeptId(), afterEntry.getDeptId())
                || !Objects.equals(trimText(beforeEntry.getJobTitle()), trimText(afterEntry.getJobTitle()))
                || !Objects.equals(beforeEntry.getJobLevelId(), afterEntry.getJobLevelId())
                || !Objects.equals(beforeEntry.getJobSequenceId(), afterEntry.getJobSequenceId())
                || !Objects.equals(beforeEntry.getWorkLocationId(), afterEntry.getWorkLocationId())
                || !Objects.equals(beforeEntry.getDirectSupervisorId(), afterEntry.getDirectSupervisorId());
    }

    private boolean hasRequestedLongChange(Long targetValue, Long currentValue) {
        return targetValue != null && !Objects.equals(targetValue, currentValue);
    }

    private boolean hasRequestedStringChange(String targetValue, String currentValue) {
        return targetValue != null && !Objects.equals(trimText(targetValue), trimText(currentValue));
    }

    private boolean isManagedEmploymentStatus(Integer workStatus) {
        return Objects.equals(workStatus, 2) || Objects.equals(workStatus, 3) || Objects.equals(workStatus, 4);
    }

    private String trimText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void rejectDirectResignation() {
        throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "离职请通过生命周期工作台发起审批，审批通过后再生效");
    }

    @Override
    public EmployeeEntryDO getEmployeeEntry(Long id) {
        return employeeEntryMapper.selectById(id);
    }

    @Override
    public EmployeeEntryRespVO getEmployeeEntryDetail(Long id) {
        // 查询员工入职记录基本信息
        EmployeeEntryRespVO employeeEntry = employeeEntryMapper.selectDetailById(id);
        if (employeeEntry == null) {
            throw ServiceExceptionUtil.exception(EMPLOYEE_ENTRY_NOT_EXISTS);
        }

        // 查询教育信息
        List<EmployeeEducationDO> educationList = employeeEducationMapper.selectListByProfileId(employeeEntry.getProfileId());
        if (educationList != null && !educationList.isEmpty()) {
            List<EmployeeEntryRespVO.EducationInfo> educationInfoList = educationList.stream()
                    .map(edu -> {
                        EmployeeEntryRespVO.EducationInfo info = new EmployeeEntryRespVO.EducationInfo();
                        info.setId(edu.getId());
                        info.setEducation(edu.getEducation());
                        info.setSchoolName(edu.getSchoolName());
                        info.setMajor(edu.getMajor());
                        info.setEnrollmentDate(edu.getEnrollmentDate());
                        info.setGraduationDate(edu.getGraduationDate());
                        info.setIsHighest(edu.getIsHighest());
                        return info;
                    })
                    .collect(Collectors.toList());
            employeeEntry.setEducationList(educationInfoList);
        }

        // 查询家庭信息
        List<EmployeeFamilyDO> familyList = employeeFamilyMapper.selectListByProfileId(employeeEntry.getProfileId());
        if (familyList != null && !familyList.isEmpty()) {
            List<EmployeeEntryRespVO.FamilyInfo> familyInfoList = familyList.stream()
                    .map(family -> {
                        EmployeeEntryRespVO.FamilyInfo info = new EmployeeEntryRespVO.FamilyInfo();
                        info.setId(family.getId());
                        info.setRelation(family.getRelation());
                        info.setName(family.getName());
                        info.setGender(family.getGender());
                        info.setBirthDate(family.getBirthDate());
                        info.setPhone(family.getPhone());
                        info.setWorkplace(family.getWorkplace());
                        return info;
                    })
                    .collect(Collectors.toList());
            employeeEntry.setFamilyList(familyInfoList);
        }

        return employeeEntry;
    }

    @Override
    public PageResult<EmployeeEntryRespVO> getEmployeeEntryPage(EmployeeEntryPageReqVO pageReqVO) {
        // 使用 MyBatis Plus 分页机制
        IPage<EmployeeEntryRespVO> page = new Page<>(pageReqVO.getPageNo(), pageReqVO.getPageSize());
        IPage<EmployeeEntryRespVO> result = employeeEntryMapper.selectPageWithProfile(page, pageReqVO);
        return new PageResult<>(result.getRecords(), result.getTotal());
    }

    @Override
    public List<EmployeeEntryDO> getEmployeeEntryList(List<Long> ids) {
        return employeeEntryMapper.selectBatchIds(ids);
    }

    @Override
    public List<EmployeeEntryDO> getEmployeeEntryListByProfileId(Long profileId) {
        return employeeEntryMapper.selectListByProfileId(profileId);
    }

    @Override
    public EmployeeEntryDO getEmployeeEntryByEmployeeNo(String employeeNo) {
        return employeeEntryMapper.selectByEmployeeNo(employeeNo);
    }

    @Override
    public EmployeeEntryDO getEmployeeEntryByEntryNo(String entryNo) {
        return employeeEntryMapper.selectByEntryNo(entryNo);
    }

    @Override
    public String generateEntryNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String maxEntryNo = employeeEntryMapper.selectMaxEntryNoByDate(dateStr);
        
        int sequence = 1;
        if (maxEntryNo != null && maxEntryNo.length() >= 12) {
            try {
                sequence = Integer.parseInt(maxEntryNo.substring(8)) + 1;
            } catch (NumberFormatException e) {
                log.warn("解析入职编号序列失败: {}", maxEntryNo);
            }
        }
        
        return String.format("ENTRY%s%03d", dateStr, sequence);
    }

    @Override
    public String generateEmployeeNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String maxEmployeeNo = employeeEntryMapper.selectMaxEmployeeNoByDate(dateStr);
        
        int sequence = 1;
        if (maxEmployeeNo != null && maxEmployeeNo.length() >= 11) {
            try {
                sequence = Integer.parseInt(maxEmployeeNo.substring(7)) + 1;
            } catch (NumberFormatException e) {
                log.warn("解析员工编号序列失败: {}", maxEmployeeNo);
            }
        }
        
        return String.format("EMP%s%03d", dateStr, sequence);
    }

    @Override
    public void adjustEntryDate(Long id, String newEntryDate) {
        // 校验入职记录存在
        EmployeeEntryDO employeeEntry = employeeEntryMapper.selectById(id);
        if (employeeEntry == null) {
            throw ServiceExceptionUtil.exception(EMPLOYEE_ENTRY_NOT_EXISTS);
        }
        if (isManagedEmploymentStatus(employeeEntry.getWorkStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "已入职员工入职日期不能直接调整");
        }

        // 转换日期格式
        LocalDate entryDate = LocalDate.parse(newEntryDate);

        // 更新入职日期
        EmployeeEntryDO updateObj = new EmployeeEntryDO();
        updateObj.setId(id);
        updateObj.setEntryDate(entryDate);
        employeeEntryMapper.updateById(updateObj);
        
        log.info("调整员工入职日期成功，ID: {}, 新入职日期: {}", id, newEntryDate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processResignation(EmployeeResignationReqVO reqVO) {
        EmployeeEntryDO employeeEntry = employeeEntryMapper.selectById(reqVO.getEntryId());
        if (employeeEntry == null) {
            throw ServiceExceptionUtil.exception(EMPLOYEE_ENTRY_NOT_EXISTS);
        }

        if (Objects.equals(employeeEntry.getWorkStatus(), 4)) {
            throw ServiceExceptionUtil.exception(EMPLOYEE_ALREADY_RESIGNED);
        }
        rejectDirectResignation();
    }
} 
