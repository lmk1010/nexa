package com.kyx.service.business.service.auth;

import cn.hutool.core.util.StrUtil;
import com.kyx.foundation.common.util.monitor.TracerUtils;
import com.kyx.foundation.common.util.servlet.ServletUtils;
import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.business.api.logger.dto.LoginLogCreateReqDTO;
import com.kyx.service.business.controller.admin.auth.vo.AuthRegisterReqVO;
import com.kyx.service.business.dal.dataobject.user.AdminUserDO;
import com.kyx.service.business.enums.logger.LoginLogTypeEnum;
import com.kyx.service.business.enums.logger.LoginResultEnum;
import com.kyx.service.business.service.logger.LoginLogService;
import com.kyx.service.business.service.user.AdminUserService;
import com.kyx.service.im.api.invitecode.InviteCodeApi;
import com.kyx.service.im.api.invitecode.dto.InviteCodeRespDTO;
import com.kyx.service.im.api.invitecode.dto.InviteCodeValidateRespDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.service.business.enums.ErrorCodeConstants.*;

/**
 * 用户注册服务
 * 专门处理用户注册过程中的事务管理和数据一致性
 *
 * @author MK
 */
@Service
@Slf4j
public class UserRegistrationService {

    @Resource
    private AdminUserService userService;

    @Resource
    private LoginLogService loginLogService;

    @Resource
    private InviteCodeApi inviteCodeApi;

    /**
     * 执行用户注册
     * 包含完整的事务管理和错误处理
     *
     * @param reqVO 注册请求
     * @return 用户ID
     */
    public Long executeRegistration(AuthRegisterReqVO reqVO) {
        log.info("开始用户注册流程，用户名: {}, 手机号: {}, 邮箱: {}, 邀请码: {}", 
                reqVO.getUsername(), reqVO.getMobile(), reqVO.getEmail(), reqVO.getInvitationCode());

        try {
            // 1. 参数校验
            validateRegistrationParams(reqVO);

            // 2. 检查用户是否已存在
            validateUserNotExists(reqVO);

            // 3. 处理邀请码并获取租户ID
            Long tenantId = processInvitationCode(reqVO);

            // 4. 生成用户名（如果需要）
            String username = generateUsernameIfNeeded(reqVO);
            reqVO.setUsername(username);

            log.info("设置租户上下文，租户ID: {}", TenantContextHolder.getTenantId());

            // 6. 执行用户注册
            Long userId = userService.registerUser(reqVO);

            // 7. 记录注册成功日志
            createRegistrationLog(userId, username, LoginResultEnum.SUCCESS);

            log.info("用户注册成功，用户ID: {}, 用户名: {}, 租户ID: {}", userId, username, tenantId);
            return userId;

        } catch (Exception e) {
            // 记录注册失败日志
            createRegistrationLog(null, reqVO.getUsername(), LoginResultEnum.BAD_CREDENTIALS);
            
            log.error("用户注册失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 验证注册参数
     */
    private void validateRegistrationParams(AuthRegisterReqVO reqVO) {
        // 检查是否提供了至少一种注册方式
        if (StrUtil.isBlank(reqVO.getUsername()) && 
            StrUtil.isBlank(reqVO.getMobile()) && 
            StrUtil.isBlank(reqVO.getEmail())) {
            throw exception(REGISTER_IDENTIFIER_REQUIRED);
        }
        
        // 检查密码是否为空
        if (StrUtil.isBlank(reqVO.getPassword())) {
            throw exception(REGISTER_PASSWORD_REQUIRED);
        }
        
        // 检查昵称是否为空
        if (StrUtil.isBlank(reqVO.getNickname())) {
            throw exception(REGISTER_NICKNAME_REQUIRED);
        }
    }

    /**
     * 验证用户不存在
     */
    private void validateUserNotExists(AuthRegisterReqVO reqVO) {
        String username = reqVO.getUsername();
        String mobile = reqVO.getMobile();
        String email = reqVO.getEmail();

        // 检查用户名是否已存在
        if (StrUtil.isNotBlank(username)) {
            AdminUserDO existingUser = userService.getUserByUsername(username);
            if (existingUser != null) {
                throw exception(USER_USERNAME_EXISTS);
            }
        }

        // 检查手机号是否已存在
        if (StrUtil.isNotBlank(mobile)) {
            AdminUserDO existingUser = userService.getUserByMobile(mobile);
            if (existingUser != null) {
                throw exception(USER_MOBILE_EXISTS);
            }
        }

        // 检查邮箱是否已存在
        if (StrUtil.isNotBlank(email)) {
            AdminUserDO existingUser = userService.getUserByEmail(email);
            if (existingUser != null) {
                throw exception(USER_EMAIL_EXISTS);
            }
        }
    }

    /**
     * 处理邀请码并返回租户ID
     */
    private Long processInvitationCode(AuthRegisterReqVO reqVO) {
        String invitationCode = reqVO.getInvitationCode();
        
        // 如果没有提供邀请码，直接拦截
        if (StrUtil.isBlank(invitationCode)) {
            log.info("未提供邀请码");
            throw exception(REGISTER_INVITATION_CODE_REQUIRED);
        }

        try {
            // 先获取邀请码详细信息，确定租户ID
            InviteCodeRespDTO inviteCodeInfo = inviteCodeApi.getInviteCodeByCode(invitationCode);
            if (inviteCodeInfo == null) {
                throw exception(REGISTER_INVITATION_CODE_NOT_EXISTS);
            }
            
            Long tenantId = inviteCodeInfo.getTenantId();
            if (tenantId == null){
                throw exception(TENANT_NOT_EXISTS);
            }

            //  设置租户上下文并验证
            TenantContextHolder.setTenantId(tenantId);

            // 校验邀请码生效
            InviteCodeValidateRespDTO validateResult = inviteCodeApi.validateInviteCode(invitationCode);
            if (validateResult == null || validateResult.getValid() == null || !validateResult.getValid()) {
                throw exception(REGISTER_INVITATION_CODE_INVALID);
            }

            // 使用邀请码
            inviteCodeApi.useInviteCode(invitationCode);
            log.info("邀请码验证成功，租户ID: {}, 租户名称: {}", tenantId, inviteCodeInfo.getTenantName());
            
            return tenantId;
            
        } catch (Exception e) {
            log.error("邀请码处理失败: {}", e.getMessage(), e);
            throw exception(REGISTER_INVITATION_CODE_VALIDATION_FAILED, e.getMessage());
        }
    }

    /**
     * 生成用户名（如果需要）
     */
    private String generateUsernameIfNeeded(AuthRegisterReqVO reqVO) {
        String username = reqVO.getUsername();
        String mobile = reqVO.getMobile();
        String email = reqVO.getEmail();

        if (StrUtil.isNotBlank(username)) {
            return username;
        }

        if (StrUtil.isNotBlank(mobile)) {
            return generateUsernameFromMobile(mobile);
        }

        if (StrUtil.isNotBlank(email)) {
            return generateUsernameFromEmail(email);
        }

        // 如果都没有提供，生成一个默认用户名
        return "user_" + System.currentTimeMillis();
    }

    /**
     * 根据手机号生成用户名
     */
    private String generateUsernameFromMobile(String mobile) {
        // 生成规则：user_手机号后4位_时间戳后6位
        String suffix = mobile.substring(mobile.length() - 4);
        String timestamp = String.valueOf(System.currentTimeMillis());
        String timeSuffix = timestamp.substring(timestamp.length() - 6);
        return "user_" + suffix + "_" + timeSuffix;
    }

    /**
     * 根据邮箱生成用户名
     */
    private String generateUsernameFromEmail(String email) {
        // 生成规则：user_邮箱前缀_时间戳后6位
        String prefix = email.substring(0, email.indexOf("@"));
        String timestamp = String.valueOf(System.currentTimeMillis());
        String timeSuffix = timestamp.substring(timestamp.length() - 6);
        return "user_" + prefix + "_" + timeSuffix;
    }

    /**
     * 创建注册日志
     */
    private void createRegistrationLog(Long userId, String username, LoginResultEnum result) {
        try {
            LoginLogCreateReqDTO reqDTO = new LoginLogCreateReqDTO();
            reqDTO.setLogType(LoginLogTypeEnum.LOGIN_USERNAME.getType());
            reqDTO.setTraceId(TracerUtils.getTraceId());
            reqDTO.setUserId(userId);
            reqDTO.setUserType(com.kyx.foundation.common.enums.UserTypeEnum.ADMIN.getValue());
            reqDTO.setUsername(username);
            reqDTO.setUserAgent(ServletUtils.getUserAgent());
            reqDTO.setUserIp(ServletUtils.getClientIP());
            reqDTO.setResult(result.getResult());
            reqDTO.setDeviceType("WEB"); // 默认设备类型
            reqDTO.setDeviceId("registration_service");
            
            loginLogService.createLoginLog(reqDTO);
        } catch (Exception e) {
            // 日志记录失败不应该影响主流程
            log.warn("注册日志记录失败: {}", e.getMessage());
        }
    }
} 