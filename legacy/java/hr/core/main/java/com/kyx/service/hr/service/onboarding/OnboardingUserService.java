package com.kyx.service.hr.service.onboarding;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import com.kyx.service.business.api.user.dto.UserOnboardingCreateReqDTO;
import com.kyx.service.business.api.user.dto.UserOnboardingRespDTO;
import com.kyx.service.hr.api.onboarding.dto.MobileStatusCheckRespDTO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeEntryDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeProfileDO;
import com.kyx.service.hr.dal.mysql.employee.EmployeeEntryMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeProfileMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 入职用户服务
 *
 * @author MK
 */
@Service
@Slf4j
public class OnboardingUserService {

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private EmployeeEntryMapper employeeEntryMapper;

    @Resource
    private EmployeeProfileMapper employeeProfileMapper;

    /**
     * 检查手机号状态
     *
     * @param name 姓名
     * @param mobile 手机号
     * @return 手机号状态信息
     */
    public MobileStatusCheckRespDTO checkMobileStatus(String name, String mobile) {
        // 验证参数
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("姓名不能为空");
        }
        if (mobile == null || !mobile.matches("^1[3-9]\\d{9}$")) {
            throw new RuntimeException("手机号格式不正确");
        }

        MobileStatusCheckRespDTO result = new MobileStatusCheckRespDTO();
        result.setMobile(mobile);
        result.setName(name.trim());

        // 1. 检查手机号是否已存在员工档案
        List<EmployeeProfileDO> existingProfiles = employeeProfileMapper.selectListByMobile(mobile);
        if (!existingProfiles.isEmpty()) {
            // 找到匹配的档案
            EmployeeProfileDO matchedProfile = null;
            for (EmployeeProfileDO profile : existingProfiles) {
                if (name.trim().equals(profile.getName())) {
                    matchedProfile = profile;
                    break;
                }
            }

            if (matchedProfile != null) {
                // 姓名和手机号都匹配，检查入职状态
                List<EmployeeEntryDO> entries = employeeEntryMapper.selectListByProfileId(matchedProfile.getId());
                
                if (!entries.isEmpty()) {
                    // 找到最新的入职记录
                    EmployeeEntryDO latestEntry = entries.stream()
                            .sorted((e1, e2) -> e2.getCreateTime().compareTo(e1.getCreateTime()))
                            .findFirst()
                            .orElse(null);

                    if (latestEntry != null) {
                        result.setProfileId(matchedProfile.getId());
                        result.setEntryId(latestEntry.getId());
                        result.setEntryNo(latestEntry.getEntryNo());

                        // 根据工作状态判断
                        if (latestEntry.getWorkStatus() == 0) {
                            // 待填写状态 - 办理中
                            result.setStatusType(1);
                            result.setStatusDesc("该手机号正在办理入职中");
                            result.setCanShowQRCode(true);
                            result.setCanCreateAccount(false);
                            
                            // 查找账号信息
                            findAndSetUserInfo(result, latestEntry, mobile);
                        } else if (latestEntry.getWorkStatus() == 1) {
                            // 待入职状态 - 已初始化
                            result.setStatusType(2);
                            result.setStatusDesc("该手机号已初始化，可以直接显示二维码");
                            result.setCanShowQRCode(true);
                            result.setCanCreateAccount(false);
                            
                            // 查找账号信息
                            findAndSetUserInfo(result, latestEntry, mobile);
                        } else if (latestEntry.getWorkStatus() >= 3) {
                            // 在职或更高状态 - 已入职
                            result.setStatusType(3);
                            result.setStatusDesc("该手机号已入职，不允许重复创建");
                            result.setCanShowQRCode(false);
                            result.setCanCreateAccount(false);
                            
                            // 查找账号信息
                            findAndSetUserInfo(result, latestEntry, mobile);
                        } else {
                            // 其他状态
                            result.setStatusType(0);
                            result.setStatusDesc("该手机号状态异常");
                            result.setCanShowQRCode(false);
                            result.setCanCreateAccount(false);
                            
                            // 查找账号信息
                            findAndSetUserInfo(result, latestEntry, mobile);
                        }
                    } else {
                        result.setStatusType(0);
                        result.setStatusDesc("该手机号状态异常");
                        result.setCanShowQRCode(false);
                        result.setCanCreateAccount(false);
                    }
                } else {
                    // 有档案但没有入职记录
                    result.setStatusType(0);
                    result.setStatusDesc("该手机号有档案但无入职记录");
                    result.setCanShowQRCode(false);
                    result.setCanCreateAccount(false);
                    
                    // 尝试通过手机号查找用户信息
                    try {
                        CommonResult<AdminUserRespDTO> userByMobileResult = adminUserApi.getUserByMobile(mobile);
                        if (userByMobileResult.isSuccess() && userByMobileResult.getData() != null) {
                            AdminUserRespDTO user = userByMobileResult.getData();
                            result.setUsername(user.getUsername());
                            result.setDefaultPassword("kyx123456"); // 默认密码
                        }
                    } catch (Exception e) {
                        log.warn("通过手机号获取用户信息失败，mobile: {}, error: {}", mobile, e.getMessage());
                    }
                }
            } else {
                // 手机号存在但姓名不匹配
                result.setStatusType(0);
                result.setStatusDesc("该手机号已被其他员工使用");
                result.setCanShowQRCode(false);
                result.setCanCreateAccount(false);
                
                // 尝试通过手机号查找用户信息
                try {
                    CommonResult<AdminUserRespDTO> userByMobileResult = adminUserApi.getUserByMobile(mobile);
                    if (userByMobileResult.isSuccess() && userByMobileResult.getData() != null) {
                        AdminUserRespDTO user = userByMobileResult.getData();
                        result.setUsername(user.getUsername());
                        result.setDefaultPassword("kyx123456"); // 默认密码
                    }
                } catch (Exception e) {
                    log.warn("通过手机号获取用户信息失败，mobile: {}, error: {}", mobile, e.getMessage());
                }
            }
        } else {
            // 手机号不存在，可以创建新账号
            result.setStatusType(0);
            result.setStatusDesc("该手机号未使用，可以创建新账号");
            result.setCanShowQRCode(false);
            result.setCanCreateAccount(true);
        }

        return result;
    }

    /**
     * 通过姓名和手机号创建用户账号
     *
     * @param name 姓名
     * @param mobile 手机号
     * @return 用户信息
     */
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public UserOnboardingRespDTO createOnboardingUserByNameAndMobile(String name, String mobile) {
        // 验证参数
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("姓名不能为空");
        }
        if (mobile == null || !mobile.matches("^1[3-9]\\d{9}$")) {
            throw new RuntimeException("手机号格式不正确");
        }
        
        // 检查手机号状态
        MobileStatusCheckRespDTO statusCheck = checkMobileStatus(name, mobile);
        
        // 如果不允许创建账号，抛出异常
        if (!statusCheck.getCanCreateAccount()) {
            throw new RuntimeException(statusCheck.getStatusDesc());
        }
        
        // 检查手机号是否已存在员工档案
        List<EmployeeProfileDO> existingProfiles = employeeProfileMapper.selectListByMobile(mobile);
        if (!existingProfiles.isEmpty()) {
            throw new RuntimeException("该手机号已存在员工档案");
        }
        
        // 1. 创建员工档案
        EmployeeProfileDO profile = new EmployeeProfileDO();
        profile.setProfileNo(generateProfileNo());
        profile.setName(name.trim());
        profile.setMobile(mobile);
        profile.setEmail("");
        profile.setGender(1); // 默认男性
        profile.setStatus(1); // 正常状态
        
        employeeProfileMapper.insert(profile);
        
        // 2. 创建入职记录
        EmployeeEntryDO entry = new EmployeeEntryDO();
        entry.setEntryNo(generateEntryNo());
        entry.setProfileId(profile.getId()); // 关联员工档案ID
        entry.setEntryType(1); // 首次入职
        entry.setEntryDate(java.time.LocalDate.now());
        entry.setDeptId(null); // 部门为空，后续可分配
        entry.setJobTitle(""); // 职位为空，后续可设置
        entry.setEmploymentType(1); // 全职
        entry.setWorkStatus(0); // 待填写状态
        entry.setRemark("通过姓名和手机号创建的入职记录");
        
        employeeEntryMapper.insert(entry);
        
        // 3. 构建创建用户请求
        UserOnboardingCreateReqDTO createReqDTO = new UserOnboardingCreateReqDTO();
        createReqDTO.setEmployeeName(name.trim());
        createReqDTO.setMobile(mobile);
        createReqDTO.setEmail("");
        createReqDTO.setDeptId(entry.getDeptId());
        createReqDTO.setPosition(entry.getJobTitle());
        createReqDTO.setSex(1);
        createReqDTO.setOnboardingNo(entry.getEntryNo());

        // 4. 调用business服务创建用户
        CommonResult<UserOnboardingRespDTO> result = adminUserApi.createOnboardingUser(createReqDTO);
        if (!result.isSuccess()) {
            log.error("创建入职用户账号失败: {}", result.getMsg());
            throw new RuntimeException("创建入职用户账号失败: " + result.getMsg());
        }

        UserOnboardingRespDTO userInfo = result.getData();
        
        // 5. 更新员工档案的userId
        profile.setUserId(userInfo.getUserId());
        employeeProfileMapper.updateById(profile);
        
        // 6. 更新入职记录的userId
        entry.setUserId(userInfo.getUserId());
        employeeEntryMapper.updateById(entry);
        
        // 7. 设置entryId到返回结果
        userInfo.setEntryId(entry.getId());
        
        log.info("成功创建入职用户账号: userId={}, username={}, name={}, mobile={}, entryId={}, profileId={}", 
                userInfo.getUserId(), userInfo.getUsername(), name, userInfo.getMobile(), entry.getId(), profile.getId());

        return userInfo;
    }
    
    /**
     * 生成档案编号
     */
    private String generateProfileNo() {
        String dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "PROF" + dateStr;
        
        // 查询当天最大序号
        String maxNo = employeeProfileMapper.selectMaxProfileNoByDate(dateStr);
        int sequence = 1;
        if (maxNo != null && maxNo.startsWith(prefix)) {
            String sequenceStr = maxNo.substring(prefix.length());
            try {
                sequence = Integer.parseInt(sequenceStr) + 1;
            } catch (NumberFormatException e) {
                sequence = 1;
            }
        }
        
        return prefix + String.format("%04d", sequence);
    }
    
    /**
     * 生成入职编号
     */
    private String generateEntryNo() {
        String dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "ENTRY" + dateStr;
        
        // 查询当天最大序号
        String maxNo = employeeEntryMapper.selectMaxEntryNoByDate(dateStr);
        int sequence = 1;
        if (maxNo != null && maxNo.startsWith(prefix)) {
            String sequenceStr = maxNo.substring(prefix.length());
            try {
                sequence = Integer.parseInt(sequenceStr) + 1;
            } catch (NumberFormatException e) {
                sequence = 1;
            }
        }
        
        return prefix + String.format("%04d", sequence);
    }
    
    /**
     * 查找并设置用户信息
     *
     * @param result 状态检查结果
     * @param entry 入职记录
     * @param mobile 手机号
     */
    private void findAndSetUserInfo(MobileStatusCheckRespDTO result, EmployeeEntryDO entry, String mobile) {
        // 优先通过userId查找，如果没有则通过手机号查找
        if (entry.getUserId() != null && entry.getUserId() > 0) {
            try {
                CommonResult<AdminUserRespDTO> userResult = adminUserApi.getUser(entry.getUserId());
                if (userResult.isSuccess() && userResult.getData() != null) {
                    AdminUserRespDTO user = userResult.getData();
                    log.info("通过userId获取用户信息成功: userId={}, username={}, nickname={}", 
                            entry.getUserId(), user.getUsername(), user.getNickname());
                    result.setUsername(user.getUsername());
                    result.setDefaultPassword("kyx123456"); // 默认密码
                    return;
                } else {
                    log.warn("通过userId获取用户信息失败: userId={}, success={}, data={}", 
                            entry.getUserId(), userResult.isSuccess(), userResult.getData());
                }
            } catch (Exception e) {
                log.warn("通过userId获取用户信息失败，userId: {}, error: {}", entry.getUserId(), e.getMessage());
            }
        }
        
        // 如果通过userId获取失败或没有userId，尝试通过手机号查找
        try {
            CommonResult<AdminUserRespDTO> userByMobileResult = adminUserApi.getUserByMobile(mobile);
            if (userByMobileResult.isSuccess() && userByMobileResult.getData() != null) {
                AdminUserRespDTO user = userByMobileResult.getData();
                log.info("通过手机号获取用户信息成功: mobile={}, username={}, nickname={}", 
                        mobile, user.getUsername(), user.getNickname());
                result.setUsername(user.getUsername());
                result.setDefaultPassword("kyx123456"); // 默认密码
            } else {
                log.warn("通过手机号获取用户信息失败: mobile={}, success={}, data={}", 
                        mobile, userByMobileResult.isSuccess(), userByMobileResult.getData());
            }
        } catch (Exception e) {
            log.warn("通过手机号获取用户信息失败，mobile: {}, error: {}", mobile, e.getMessage());
        }
    }
} 