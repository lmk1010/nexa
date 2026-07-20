package com.kyx.service.hr.service.onboarding;

import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.json.JsonUtils;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.common.util.servlet.ServletUtils;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.foundation.security.core.util.SecurityFrameworkUtils;
import com.kyx.service.bpm.api.task.BpmProcessInstanceApi;
import com.kyx.service.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.kyx.service.bpm.enums.task.BpmProcessInstanceStatusEnum;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import com.kyx.service.hr.api.onboarding.dto.OnboardingPublicReqDTO;
import com.kyx.service.hr.api.onboarding.dto.OnboardingPublicRespDTO;
import com.kyx.service.hr.controller.admin.onboarding.vo.OnboardingLinkValidateRespVO;
import com.kyx.service.hr.controller.admin.onboarding.vo.OnboardingPageReqVO;
import com.kyx.service.hr.controller.admin.onboarding.vo.OnboardingRespVO;
import com.kyx.service.hr.controller.admin.onboarding.vo.OnboardingSaveReqVO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeAttachmentDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeEducationDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeEntryDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeFamilyDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeOperationLogDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeProfileDO;
import com.kyx.service.hr.dal.dataobject.onboarding.OnboardingDO;
import com.kyx.service.hr.dal.mysql.employee.EmployeeAttachmentMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeEducationMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeEntryMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeFamilyMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeOperationLogMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeProfileMapper;
import com.kyx.service.hr.enums.OnboardingStatusEnum;
import com.kyx.service.hr.service.lifecycle.HrLifecycleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.kyx.service.hr.enums.ErrorCodeConstants.*;

/**
 * 入职管理 Service 实现类 - 基于员工档案表设计
 *
 * @author MK
 */
@Service
@Validated
@Slf4j
public class OnboardingServiceImpl implements OnboardingService {

    public static final String PROCESS_KEY = "hr_onboarding_approval";

    private static final int PROCESS_TYPE_APPROVAL = 2;
    private static final int STATUS_APPROVING = 2;
    private static final int STATUS_APPROVED = 3;
    private static final int STATUS_REJECTED = 4;
    private static final int STATUS_CANCELED = 5;
    private static final int WORK_STATUS_PENDING = 1;
    private static final int WORK_STATUS_ON_JOB = 3;

    @Resource
    private EmployeeProfileMapper employeeProfileMapper;
    
    @Resource
    private EmployeeEntryMapper employeeEntryMapper;
    
    @Resource
    private EmployeeEducationMapper employeeEducationMapper;
    
    @Resource
    private EmployeeFamilyMapper employeeFamilyMapper;
    
    @Resource
    private EmployeeAttachmentMapper employeeAttachmentMapper;

    @Resource
    private EmployeeOperationLogMapper employeeOperationLogMapper;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    @Resource
    private HrLifecycleService hrLifecycleService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveOnboarding(Long id, Boolean approved, String comment) {
        // 校验入职记录存在
        EmployeeEntryDO entry = employeeEntryMapper.selectById(id);
        if (entry == null) {
            throw ServiceExceptionUtil.exception(EMPLOYEE_ENTRY_NOT_EXISTS);
        }
        
        // 校验状态
        if (entry.getOnboardingStatus() != 2) {
            throw ServiceExceptionUtil.exception(ONBOARDING_STATUS_NOT_APPROVING);
        }
        if (StringUtils.hasText(entry.getProcessInstanceId())) {
            throw ServiceExceptionUtil.exception(ONBOARDING_STATUS_NOT_APPROVING, "该入职申请已进入 BPM 流程，请在流程中心处理审批");
        }
        
        // 更新状态
        EmployeeEntryDO updateEntry = new EmployeeEntryDO();
        updateEntry.setId(id);
        
        if (approved) {
            updateEntry.setOnboardingStatus(3); // 已通过
            updateEntry.setRemark("入职申请已通过");
            // 生成员工编号
            String employeeNo = generateEmployeeNo();
            updateEntry.setEmployeeNo(employeeNo);
            updateEntry.setWorkStatus(3); // 在职
            
            log.info("审批通过入职申请，入职记录ID: {}, 生成员工编号: {}", id, employeeNo);
        } else {
            updateEntry.setOnboardingStatus(4); // 已拒绝
            updateEntry.setRemark("入职申请已拒绝");
            updateEntry.setWorkStatus(2); // 离职
            
            log.info("拒绝入职申请，入职记录ID: {}, 拒绝原因: {}", id, comment);
        }
        
        employeeEntryMapper.updateById(updateEntry);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOnboarding(Long id, String cancelReason, String remark) {
        // 校验入职记录存在
        EmployeeEntryDO entry = employeeEntryMapper.selectById(id);
        if (entry == null) {
            throw ServiceExceptionUtil.exception(EMPLOYEE_ENTRY_NOT_EXISTS);
        }
        
        // 校验状态
        if (entry.getOnboardingStatus() == 3 || entry.getOnboardingStatus() == 4) {
            throw ServiceExceptionUtil.exception(ONBOARDING_STATUS_CANNOT_CANCEL);
        }
        if (StringUtils.hasText(entry.getProcessInstanceId()) && Objects.equals(entry.getOnboardingStatus(), STATUS_APPROVING)) {
            throw ServiceExceptionUtil.exception(ONBOARDING_STATUS_CANNOT_CANCEL, "该入职申请已进入 BPM 流程，请在流程详情中撤销");
        }
        
        // 更新状态为已取消
        EmployeeEntryDO updateEntry = new EmployeeEntryDO();
        updateEntry.setId(id);
        updateEntry.setOnboardingStatus(5); // 已取消
        updateEntry.setCancelReason(cancelReason);
        updateEntry.setRemark(remark != null ? remark : "入职申请已取消");
        updateEntry.setWorkStatus(2); // 离职
        
        employeeEntryMapper.updateById(updateEntry);
        
        log.info("取消入职申请成功，入职记录ID: {}, 取消原因: {}, 备注: {}", id, cancelReason, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreOnboarding(Long id, String remark) {
        // 校验入职记录存在
        EmployeeEntryDO entry = employeeEntryMapper.selectById(id);
        if (entry == null) {
            throw ServiceExceptionUtil.exception(EMPLOYEE_ENTRY_NOT_EXISTS);
        }
        
        // 校验状态 - 只有已取消的才能恢复
        if (entry.getOnboardingStatus() != 5) {
            throw ServiceExceptionUtil.exception(ONBOARDING_STATUS_CANNOT_RESTORE);
        }
        
        // 更新状态为待提交（重新开始流程）
        EmployeeEntryDO updateEntry = new EmployeeEntryDO();
        updateEntry.setId(id);
        updateEntry.setOnboardingStatus(1); // 待提交
        updateEntry.setCancelReason(null); // 清空取消原因
        updateEntry.setRemark(remark != null ? remark : "入职申请已恢复");
        updateEntry.setWorkStatus(1); // 待入职
        
        employeeEntryMapper.updateById(updateEntry);
        
        log.info("恢复入职申请成功，入职记录ID: {}, 备注: {}", id, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateApprovalStatusByBpmEvent(Long id, String processInstanceId, Integer bpmStatus, Long operatorUserId) {
        EmployeeEntryDO entry = employeeEntryMapper.selectById(id);
        if (entry == null) {
            log.warn("入职审批 BPM 回调记录不存在，id={}, processInstanceId={}, bpmStatus={}",
                    id, processInstanceId, bpmStatus);
            return;
        }
        if (StringUtils.hasText(entry.getProcessInstanceId())
                && StringUtils.hasText(processInstanceId)
                && !Objects.equals(entry.getProcessInstanceId(), processInstanceId)) {
            log.warn("入职审批 BPM 回调流程实例不匹配，id={}, processInstanceId={}, currentProcessInstanceId={}",
                    id, processInstanceId, entry.getProcessInstanceId());
            return;
        }
        if (Objects.equals(bpmStatus, BpmProcessInstanceStatusEnum.APPROVE.getStatus())) {
            approveByBpm(entry, processInstanceId);
            return;
        }
        if (Objects.equals(bpmStatus, BpmProcessInstanceStatusEnum.REJECT.getStatus())) {
            closeByBpm(entry, STATUS_REJECTED, processInstanceId, "BPM审批驳回");
            return;
        }
        if (Objects.equals(bpmStatus, BpmProcessInstanceStatusEnum.CANCEL.getStatus())) {
            closeByBpm(entry, STATUS_CANCELED, processInstanceId, "BPM流程撤销");
        }
    }

    @Override
    public String generateApplicationNo() {
        return generateEntryNo(); // 使用入职编号作为申请编号
    }
    
    @Override
    public OnboardingLinkValidateRespVO validateEntryId(Long entryId) {
        OnboardingLinkValidateRespVO response = new OnboardingLinkValidateRespVO();
        
        try {
            // 检查entryId是否为空
            if (entryId == null) {
                response.setValid(false);
                response.setMessage("入职记录ID不能为空");
                response.setRemainingTime(0L);
                response.setEntryId(null);
                response.setIsSubmitted(false);
                response.setOnboardingStatus(null);
                response.setOnboardingStatusDesc(null);
                return response;
            }
            
            // 验证entryId是否存在
            EmployeeEntryDO entry = employeeEntryMapper.selectById(entryId);
            if (entry != null) {
                response.setValid(true);
                response.setMessage("入职记录有效");
                response.setRemainingTime(24 * 3600L); // 24小时有效期
                response.setEntryId(entryId);
                
                // 检查入职状态
                Integer onboardingStatus = entry.getOnboardingStatus();
                response.setOnboardingStatus(onboardingStatus);
                
                // 判断是否已提交表单
                // 状态说明：
                // 1: 待提交 (未填写)
                // 2: 审批中 (已填写，等待审批)
                // 3: 已通过 (审批通过)
                // 4: 已拒绝 (审批拒绝)
                // 5: 已取消 (已取消)
                boolean isSubmitted = onboardingStatus != null && onboardingStatus >= 2;
                response.setIsSubmitted(isSubmitted);
                
                // 设置状态描述
                String statusDesc;
                switch (onboardingStatus) {
                    case 1:
                        statusDesc = "待提交";
                        break;
                    case 2:
                        statusDesc = "审批中";
                        break;
                    case 3:
                        statusDesc = "已通过";
                        break;
                    case 4:
                        statusDesc = "已拒绝";
                        break;
                    case 5:
                        statusDesc = "已取消";
                        break;
                    default:
                        statusDesc = "未知状态";
                        break;
                }
                response.setOnboardingStatusDesc(statusDesc);
                
                log.info("Entry validation successful: entryId: {}, onboardingStatus: {}, isSubmitted: {}", 
                        entryId, onboardingStatus, isSubmitted);
            } else {
                // entryId不存在
                response.setValid(false);
                response.setMessage("入职记录不存在");
                response.setRemainingTime(0L);
                response.setEntryId(null);
                response.setIsSubmitted(false);
                response.setOnboardingStatus(null);
                response.setOnboardingStatusDesc(null);
                
                log.warn("Entry validation failed: entryId: {}", entryId);
            }
            
            return response;
        } catch (Exception e) {
            log.error("Failed to validate entry: {}", entryId, e);
            response.setValid(false);
            response.setMessage("验证失败，请重试");
            response.setRemainingTime(0L);
            response.setEntryId(null);
            response.setIsSubmitted(false);
            response.setOnboardingStatus(null);
            response.setOnboardingStatusDesc(null);
            return response;
        }
    }

    /**
     * 提交入职申请（移动端表单提交）
     *
     * @param createReqDTO 创建信息
     * @return 入职申请响应
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OnboardingPublicRespDTO submitOnboarding(OnboardingPublicReqDTO createReqDTO) {
        log.info("Submitting onboarding application for employee: {}, entryId: {}", createReqDTO.getEmployeeName(), createReqDTO.getEntryId());
        
        try {
            // 1. 获取当前登录用户ID
            Long currentUserId = SecurityFrameworkUtils.getLoginUserId();
            if (currentUserId == null) {
                throw ServiceExceptionUtil.exception(ONBOARDING_CREATE_FAILED, "用户未登录");
            }
            log.info("Current user ID: {}", currentUserId);
            
            // 2. 根据 entryId 获取入职记录
            EmployeeEntryDO entry = employeeEntryMapper.selectById(createReqDTO.getEntryId());
            if (entry == null) {
                throw ServiceExceptionUtil.exception(ONBOARDING_CREATE_FAILED, "入职记录不存在");
            }
            
            // 3. 验证入职记录是否属于当前用户
            if (!currentUserId.equals(entry.getUserId())) {
                throw ServiceExceptionUtil.exception(ONBOARDING_CREATE_FAILED, "无权操作此入职记录");
            }
            
            Long profileId = entry.getProfileId();
            Long entryId = entry.getId();
            
            // 4. 根据 userId 查找员工档案
            EmployeeProfileDO existingProfile = employeeProfileMapper.selectByUserId(currentUserId);
            
            if (existingProfile != null) {
                // 已存在档案，更新档案信息
                updateEmployeeProfile(existingProfile, createReqDTO);
                profileId = existingProfile.getId();
                log.info("Updated existing profile for userId: {}, profileId: {}", currentUserId, profileId);
            } else {
                // 创建新的员工档案
                profileId = createEmployeeProfile(createReqDTO, currentUserId);
                log.info("Created new profile for userId: {}, profileId: {}", currentUserId, profileId);
            }
            
            // 5. 更新入职记录
            updateEmployeeEntry(entry, createReqDTO, profileId);
            log.info("Updated entry for entryId: {}, profileId: {}", entryId, profileId);
            
            // 6. 保存教育信息（先删除旧的，再插入新的）
            saveEducationInfo(createReqDTO, profileId);
            
            // 7. 保存家庭信息（先删除旧的，再插入新的）
            saveFamilyInfo(createReqDTO, profileId);
            
            // 8. 保存附件信息（先删除旧的，再插入新的）
            saveAttachmentInfo(createReqDTO, profileId);

            if (isApprovalProcess(createReqDTO.getProcessType())) {
                String processInstanceId = startOnboardingApprovalProcess(entry, createReqDTO, currentUserId);
                EmployeeEntryDO processUpdate = new EmployeeEntryDO();
                processUpdate.setId(entryId);
                processUpdate.setProcessInstanceId(processInstanceId);
                employeeEntryMapper.updateById(processUpdate);
                entry.setProcessInstanceId(processInstanceId);
            }
            
            // 9. 构建响应
            OnboardingPublicRespDTO response = new OnboardingPublicRespDTO();
            response.setId(entryId);
            response.setApplicationNo(generateApplicationNo());
            response.setProfileId(profileId);
            response.setProfileNo(existingProfile != null ? existingProfile.getProfileNo() : generateProfileNo());
            response.setEntryId(entryId);
            response.setEntryNo(entry.getEntryNo());
            response.setApprovalType(createReqDTO.getApprovalType());
            response.setStatus(createReqDTO.getStatus());
            response.setCreateTime(LocalDateTime.now());
            
            log.info("Successfully submitted onboarding application: {}", response);
            return response;
            
        } catch (Exception e) {
            log.error("Failed to submit onboarding application", e);
            throw ServiceExceptionUtil.exception(ONBOARDING_CREATE_FAILED);
        }
    }

    /**
     * 创建员工档案
     */
    private Long createEmployeeProfile(OnboardingPublicReqDTO createReqDTO, Long userId) {
        EmployeeProfileDO profile = new EmployeeProfileDO();
        profile.setProfileNo(generateProfileNo());
        profile.setName(createReqDTO.getEmployeeName());
        profile.setIdNumber(createReqDTO.getIdNumber());
        profile.setGender(createReqDTO.getGender());
        profile.setBirthDate(createReqDTO.getBirthDate());
        profile.setAge(createReqDTO.getAge());
        profile.setNationality(createReqDTO.getNationality());
        profile.setEthnicity(createReqDTO.getEthnicity());
        profile.setPoliticalStatus(createReqDTO.getPoliticalStatus());
        profile.setMaritalStatus(createReqDTO.getMaritalStatus());
        profile.setHometown(createReqDTO.getHometown());
        profile.setAddress(createReqDTO.getAddress());
        profile.setEmergencyContact(createReqDTO.getEmergencyContact());
        profile.setEmergencyPhone(createReqDTO.getEmergencyPhone());
        profile.setMobile(createReqDTO.getMobile());
        profile.setEmail(createReqDTO.getEmail());
        profile.setStatus(1); // 正常状态
        profile.setUserId(userId);
        
        employeeProfileMapper.insert(profile);
        return profile.getId();
    }

    /**
     * 创建员工入职记录
     */
    private Long createEmployeeEntry(OnboardingPublicReqDTO createReqDTO, Long profileId) {
        EmployeeEntryDO entry = new EmployeeEntryDO();
        entry.setEntryNo(generateEntryNo());
        entry.setProfileId(profileId);
        entry.setEntryType(1); // 首次入职
        entry.setProcessType(createReqDTO.getProcessType()); // 设置入职流程类型
        entry.setEntryDate(LocalDate.now());
        entry.setWorkStatus(1); // 在职
        entry.setOnboardingStatus(1); // 待提交
        entry.setBankName(createReqDTO.getBankName());
        entry.setBankBranch(createReqDTO.getBankBranch());
        entry.setBankAccount(createReqDTO.getBankAccount());
        entry.setAccountName(createReqDTO.getAccountName());
        entry.setRemark("入职申请待提交");
        
        employeeEntryMapper.insert(entry);
        return entry.getId();
    }

    /**
     * 保存教育信息
     */
    private void saveEducationInfo(OnboardingPublicReqDTO createReqDTO, Long profileId) {
        List<EmployeeEducationDO> beforeList = employeeEducationMapper.selectListByProfileId(profileId);
        // 先删除旧的教育信息
        employeeEducationMapper.delete(new LambdaQueryWrapperX<EmployeeEducationDO>()
                .eq(EmployeeEducationDO::getProfileId, profileId));
        
        // 插入新的教育信息
        if (createReqDTO.getEducationList() != null && !createReqDTO.getEducationList().isEmpty()) {
            createReqDTO.getEducationList().forEach(edu -> {
                EmployeeEducationDO education = new EmployeeEducationDO();
                education.setProfileId(profileId);
                education.setEducation(edu.getEducation());
                education.setSchoolName(edu.getSchoolName());
                education.setMajor(edu.getMajor());
                education.setEnrollmentDate(edu.getEnrollmentDate());
                education.setGraduationDate(edu.getGraduationDate());
                education.setIsHighest(edu.getIsHighest());
                
                employeeEducationMapper.insert(education);
            });
        }

        List<EmployeeEducationDO> afterList = employeeEducationMapper.selectListByProfileId(profileId);
        if (beforeList.isEmpty() && afterList.isEmpty()) {
            return;
        }
        recordOperationLog(profileId, "education", "员工档案内容-学历信息-更新", "更新员工学历信息",
                beforeList, afterList);
    }

    /**
     * 保存家庭信息
     */
    private void saveFamilyInfo(OnboardingPublicReqDTO createReqDTO, Long profileId) {
        List<EmployeeFamilyDO> beforeList = employeeFamilyMapper.selectListByProfileId(profileId);
        // 先删除旧的家庭信息
        employeeFamilyMapper.delete(new LambdaQueryWrapperX<EmployeeFamilyDO>()
                .eq(EmployeeFamilyDO::getProfileId, profileId));
        
        // 插入新的家庭信息
        if (createReqDTO.getFamilyList() != null && !createReqDTO.getFamilyList().isEmpty()) {
            createReqDTO.getFamilyList().forEach(family -> {
                EmployeeFamilyDO familyDO = new EmployeeFamilyDO();
                familyDO.setProfileId(profileId);
                familyDO.setRelation(family.getRelation());
                familyDO.setName(family.getName());
                familyDO.setPhone(family.getPhone());
                familyDO.setWorkplace(family.getWorkplace());
                
                employeeFamilyMapper.insert(familyDO);
            });
        }

        List<EmployeeFamilyDO> afterList = employeeFamilyMapper.selectListByProfileId(profileId);
        if (beforeList.isEmpty() && afterList.isEmpty()) {
            return;
        }
        recordOperationLog(profileId, "family", "员工档案内容-家庭信息-更新", "更新员工家庭信息",
                beforeList, afterList);
    }

    /**
     * 保存附件信息
     */
    private void saveAttachmentInfo(OnboardingPublicReqDTO createReqDTO, Long profileId) {
        List<EmployeeAttachmentDO> beforeList = employeeAttachmentMapper.selectListByProfileId(profileId);
        // 先删除旧的附件信息
        employeeAttachmentMapper.delete(new LambdaQueryWrapperX<EmployeeAttachmentDO>()
                .eq(EmployeeAttachmentDO::getProfileId, profileId));
        
        // 插入新的附件信息
        if (createReqDTO.getAttachmentList() != null && !createReqDTO.getAttachmentList().isEmpty()) {
            createReqDTO.getAttachmentList().forEach(attachment -> {
                EmployeeAttachmentDO attachmentDO = new EmployeeAttachmentDO();
                attachmentDO.setProfileId(profileId);
                attachmentDO.setAttachmentType(attachment.getAttachmentType());
                attachmentDO.setAttachmentName(attachment.getAttachmentName());
                attachmentDO.setFileUrl(attachment.getFileUrl());
                attachmentDO.setFileSize(attachment.getFileSize());
                attachmentDO.setFileType(attachment.getFileType());
                attachmentDO.setRemark(attachment.getRemark());
                
                employeeAttachmentMapper.insert(attachmentDO);
            });
        }

        List<EmployeeAttachmentDO> afterList = employeeAttachmentMapper.selectListByProfileId(profileId);
        if (beforeList.isEmpty() && afterList.isEmpty()) {
            return;
        }
        recordOperationLog(profileId, "attachment", "员工档案内容-附件信息-更新", "更新员工附件信息",
                beforeList, afterList);
    }

    /**
     * 生成档案编号
     */
    private String generateProfileNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "PROF" + dateStr;
        
        // 查询当天最大序号
        String maxNo = employeeProfileMapper.selectMaxProfileNoByDate(dateStr);
        int sequence = 1;
        if (maxNo != null && maxNo.startsWith(prefix)) {
            String sequenceStr = maxNo.substring(prefix.length());
            sequence = Integer.parseInt(sequenceStr) + 1;
        }
        
        return prefix + String.format("%04d", sequence);
    }

    private boolean isApprovalProcess(Integer processType) {
        return Objects.equals(processType, PROCESS_TYPE_APPROVAL);
    }

    private String startOnboardingApprovalProcess(EmployeeEntryDO entry, OnboardingPublicReqDTO reqDTO, Long userId) {
        if (userId == null) {
            throw ServiceExceptionUtil.exception(ONBOARDING_CREATE_FAILED, "入职审批发起人不能为空");
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("entryId", entry.getId());
        variables.put("entryNo", entry.getEntryNo());
        variables.put("profileId", entry.getProfileId());
        variables.put("userId", entry.getUserId());
        variables.put("employeeName", reqDTO.getEmployeeName());
        variables.put("mobile", reqDTO.getMobile());
        variables.put("email", reqDTO.getEmail());
        variables.put("entryDate", entry.getEntryDate());
        variables.put("deptId", entry.getDeptId());
        variables.put("jobTitle", entry.getJobTitle());
        variables.put("employmentType", entry.getEmploymentType());
        variables.put("probationMonths", entry.getProbationMonths());
        variables.put("contractType", entry.getContractType());
        return processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO()
                        .setProcessDefinitionKey(PROCESS_KEY)
                        .setBusinessKey(String.valueOf(entry.getId()))
                        .setVariables(variables))
                .getCheckedData();
    }

    private void approveByBpm(EmployeeEntryDO entry, String processInstanceId) {
        if (Objects.equals(entry.getOnboardingStatus(), STATUS_APPROVED)) {
            return;
        }
        if (!Objects.equals(entry.getOnboardingStatus(), STATUS_APPROVING)) {
            log.warn("入职审批 BPM 通过回调忽略非审批中记录，id={}, onboardingStatus={}",
                    entry.getId(), entry.getOnboardingStatus());
            return;
        }
        EmployeeEntryDO updateEntry = new EmployeeEntryDO();
        updateEntry.setId(entry.getId());
        updateEntry.setOnboardingStatus(STATUS_APPROVED);
        updateEntry.setWorkStatus(WORK_STATUS_ON_JOB);
        updateEntry.setProcessInstanceId(StringUtils.hasText(processInstanceId)
                ? processInstanceId : entry.getProcessInstanceId());
        updateEntry.setRemark("BPM审批通过，员工入职已确认");
        if (!StringUtils.hasText(entry.getEmployeeNo())) {
            updateEntry.setEmployeeNo(generateEmployeeNo());
        }
        employeeEntryMapper.updateById(updateEntry);

        EmployeeEntryDO afterEntry = employeeEntryMapper.selectById(entry.getId());
        EmployeeProfileDO profile = afterEntry.getProfileId() == null ? null
                : employeeProfileMapper.selectById(afterEntry.getProfileId());
        hrLifecycleService.recordOnboardingConfirmed(afterEntry, profile);
    }

    private void closeByBpm(EmployeeEntryDO entry, Integer onboardingStatus, String processInstanceId, String remark) {
        if (Objects.equals(entry.getOnboardingStatus(), onboardingStatus)) {
            return;
        }
        if (!Objects.equals(entry.getOnboardingStatus(), STATUS_APPROVING)) {
            log.warn("入职审批 BPM 关闭回调忽略非审批中记录，id={}, onboardingStatus={}, targetStatus={}",
                    entry.getId(), entry.getOnboardingStatus(), onboardingStatus);
            return;
        }
        EmployeeEntryDO updateEntry = new EmployeeEntryDO();
        updateEntry.setId(entry.getId());
        updateEntry.setOnboardingStatus(onboardingStatus);
        updateEntry.setWorkStatus(WORK_STATUS_PENDING);
        updateEntry.setProcessInstanceId(StringUtils.hasText(processInstanceId)
                ? processInstanceId : entry.getProcessInstanceId());
        updateEntry.setRemark(remark);
        employeeEntryMapper.updateById(updateEntry);
    }

    /**
     * 生成入职编号
     */
    private String generateEntryNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "ENTRY" + dateStr;
        
        // 查询当天最大序号
        String maxNo = employeeEntryMapper.selectMaxEntryNoByDate(dateStr);
        int sequence = 1;
        if (maxNo != null && maxNo.startsWith(prefix)) {
            String sequenceStr = maxNo.substring(prefix.length());
            sequence = Integer.parseInt(sequenceStr) + 1;
        }
        
        return prefix + String.format("%04d", sequence);
    }

    /**
     * 生成员工编号
     */
    private String generateEmployeeNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "EMP" + dateStr;
        
        // 查询当天最大序号（从入职记录表中查询已生成员工编号的记录）
        String maxNo = employeeEntryMapper.selectMaxEmployeeNoByDate(dateStr);
        int sequence = 1;
        if (maxNo != null && maxNo.startsWith(prefix)) {
            String sequenceStr = maxNo.substring(prefix.length());
            sequence = Integer.parseInt(sequenceStr) + 1;
        }
        
        return prefix + String.format("%04d", sequence);
    }

    /**
     * 计算年龄
     */
    private Integer calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return null;
        }
        LocalDate currentDate = LocalDate.now();
        if (birthDate.isAfter(currentDate)) {
            return 0;
        }
        return currentDate.getYear() - birthDate.getYear() - 
               (currentDate.getDayOfYear() < birthDate.getDayOfYear() ? 1 : 0);
    }

    /**
     * 更新员工档案
     */
    private void updateEmployeeProfile(EmployeeProfileDO profile, OnboardingPublicReqDTO createReqDTO) {
        profile.setName(createReqDTO.getEmployeeName());
        profile.setIdNumber(createReqDTO.getIdNumber());
        profile.setGender(createReqDTO.getGender());
        profile.setBirthDate(createReqDTO.getBirthDate());
        profile.setAge(createReqDTO.getAge());
        profile.setNationality(createReqDTO.getNationality());
        profile.setEthnicity(createReqDTO.getEthnicity());
        profile.setPoliticalStatus(createReqDTO.getPoliticalStatus());
        profile.setMaritalStatus(createReqDTO.getMaritalStatus());
        profile.setHometown(createReqDTO.getHometown());
        profile.setAddress(createReqDTO.getAddress());
        profile.setEmergencyContact(createReqDTO.getEmergencyContact());
        profile.setEmergencyPhone(createReqDTO.getEmergencyPhone());
        profile.setMobile(createReqDTO.getMobile());
        profile.setEmail(createReqDTO.getEmail());
        
        employeeProfileMapper.updateById(profile);
    }

    /**
     * 更新员工入职记录
     */
    private void updateEmployeeEntry(EmployeeEntryDO entry, OnboardingPublicReqDTO createReqDTO, Long profileId) {
        EmployeeEntryDO beforeBank = new EmployeeEntryDO();
        beforeBank.setBankName(entry.getBankName());
        beforeBank.setBankBranch(entry.getBankBranch());
        beforeBank.setBankAccount(entry.getBankAccount());
        beforeBank.setAccountName(entry.getAccountName());
        entry.setProfileId(profileId);
        entry.setProcessType(createReqDTO.getProcessType()); // 设置入职流程类型
        entry.setOnboardingStatus(2); // 审批中（等待HR审核）
        entry.setWorkStatus(1); // 待入职
        entry.setBankName(createReqDTO.getBankName());
        entry.setBankBranch(createReqDTO.getBankBranch());
        entry.setBankAccount(createReqDTO.getBankAccount());
        entry.setAccountName(createReqDTO.getAccountName());
        entry.setRemark("入职申请已提交，等待HR审核");
        
        employeeEntryMapper.updateById(entry);

        EmployeeEntryDO afterBank = new EmployeeEntryDO();
        afterBank.setBankName(entry.getBankName());
        afterBank.setBankBranch(entry.getBankBranch());
        afterBank.setBankAccount(entry.getBankAccount());
        afterBank.setAccountName(entry.getAccountName());
        if (hasBankChanged(beforeBank, afterBank)) {
            recordOperationLog(profileId, "bank", "员工档案内容-银行卡信息-更新", "更新员工银行卡信息",
                    beforeBank, afterBank);
        }
    }

    private void recordOperationLog(Long profileId, String module, String title, String content,
                                    Object beforeData, Object afterData) {
        if (profileId == null) {
            return;
        }
        EmployeeOperationLogDO log = new EmployeeOperationLogDO();
        log.setProfileId(profileId);
        log.setOperationType("update");
        log.setOperationModule(module);
        log.setOperationTitle(title);
        log.setOperationContent(content);
        if (beforeData != null) {
            log.setBeforeData(JsonUtils.toJsonString(beforeData));
        }
        if (afterData != null) {
            log.setAfterData(JsonUtils.toJsonString(afterData));
        }
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        String operatorName = SecurityFrameworkUtils.getLoginUserNickname();
        log.setOperatorId(operatorId != null ? operatorId : 0L);
        log.setOperatorName(operatorName != null ? operatorName : "system");
        log.setOperationTime(LocalDateTime.now());
        log.setOperationIp(ServletUtils.getClientIP());
        log.setOperationSource("app");
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
}
