package com.kyx.service.business.service.auth;

import cn.hutool.core.util.StrUtil;
import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.util.monitor.TracerUtils;
import com.kyx.foundation.common.util.servlet.ServletUtils;
import com.kyx.service.business.api.logger.dto.LoginLogCreateReqDTO;

import com.kyx.service.business.api.sms.dto.code.SmsCodeUseReqDTO;
import com.kyx.service.business.api.sms.SmsCodeApi;
import com.kyx.service.business.controller.admin.auth.vo.AuthAppLoginReqVO;
import com.kyx.service.business.controller.admin.auth.vo.AuthLoginRespVO;
import com.kyx.service.business.controller.admin.auth.vo.AuthRegisterReqVO;
import com.kyx.service.business.controller.admin.auth.vo.AuthResetPasswordReqVO;
import com.kyx.service.business.controller.admin.auth.vo.AuthSmsSendReqVO;
import com.kyx.service.business.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import com.kyx.service.business.dal.dataobject.user.AdminUserDO;
import com.kyx.service.business.dal.dataobject.tenant.TenantDO;
import com.kyx.service.business.enums.logger.LoginLogTypeEnum;
import com.kyx.service.business.enums.logger.LoginResultEnum;
import com.kyx.service.business.enums.oauth2.OAuth2ClientConstants;
import com.kyx.service.business.enums.sms.SmsSceneEnum;
import com.kyx.service.business.service.logger.LoginLogService;
import com.kyx.service.business.service.oauth2.OAuth2TokenService;
import com.kyx.service.business.service.user.AdminUserService;
import com.kyx.service.business.service.tenant.TenantService;
import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Objects;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.foundation.common.util.servlet.ServletUtils.getClientIP;
import static com.kyx.service.business.enums.ErrorCodeConstants.*;

/**
 * 移动端认证 Service 实现类
 *
 * @author MK
 */
@Service
@Slf4j
public class AppAuthServiceImpl implements AppAuthService {

    @Resource
    private AdminUserService userService;
    @Resource
    private LoginLogService loginLogService;
    @Resource
    private OAuth2TokenService oauth2TokenService;
    @Resource
    private TenantService tenantService;
    @Resource
    private SmsCodeApi smsCodeApi;
    
    @Resource
    private UserRegistrationService userRegistrationService;

    @Override
    public AuthLoginRespVO appLogin(AuthAppLoginReqVO reqVO) {
        // 根据登录类型查找用户
        AdminUserDO user = findUserByLoginType(reqVO);
        
        // 如果是验证码登录且用户不存在，返回用户未注册错误
        if (user == null && StrUtil.isNotBlank(reqVO.getCode())) {
            createLoginLog(null, getLoginIdentifier(reqVO), getLoginLogType(reqVO.getLoginType()), LoginResultEnum.BAD_CREDENTIALS);
            throw exception(AUTH_USER_NOT_REGISTERED);
        }
        
        if (user == null) {
            createLoginLog(null, getLoginIdentifier(reqVO), getLoginLogType(reqVO.getLoginType()), LoginResultEnum.BAD_CREDENTIALS);
            throw exception(AUTH_LOGIN_BAD_CREDENTIALS);
        }

        // 根据登录类型进行不同的验证
        if (StrUtil.isNotBlank(reqVO.getCode())) {
            // 验证码登录
            validateVerificationCode(reqVO);
        } else if (StrUtil.isNotBlank(reqVO.getPassword())) {
            // 密码登录
            if (!userService.isPasswordMatch(reqVO.getPassword(), user.getPassword())) {
                createLoginLog(user.getId(), getLoginIdentifier(reqVO), getLoginLogType(reqVO.getLoginType()), LoginResultEnum.BAD_CREDENTIALS);
                throw exception(AUTH_LOGIN_BAD_CREDENTIALS);
            }
        } else {
            // 既没有验证码也没有密码
            throw exception(AUTH_LOGIN_BAD_CREDENTIALS);
        }

        // 校验是否禁用
        if (CommonStatusEnum.isDisable(user.getStatus())) {
            createLoginLog(user.getId(), getLoginIdentifier(reqVO), getLoginLogType(reqVO.getLoginType()), LoginResultEnum.USER_DISABLED);
            throw exception(AUTH_LOGIN_USER_DISABLED);
        }

        // 创建 Token 令牌，记录登录日志
        return createTokenAfterLoginSuccess(user.getId(), getLoginIdentifier(reqVO), getLoginLogType(reqVO.getLoginType()), reqVO.getDeviceType(), reqVO.getDeviceId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthLoginRespVO appRegister(AuthRegisterReqVO reqVO) {
        try {
            // 1. 验证验证码（如果需要）
            validateVerificationCodeForRegister(reqVO);
            
            // 2. 使用专门的注册服务执行注册
            Long userId = userRegistrationService.executeRegistration(reqVO);
            
            // 3. 创建 Token 令牌，记录登录日志
            return createTokenAfterLoginSuccess(userId, reqVO.getUsername(), LoginLogTypeEnum.LOGIN_USERNAME, reqVO.getDeviceType(), reqVO.getDeviceId());
        } catch (Exception e) {
            // 记录错误日志
            log.error("用户注册失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void sendSmsCode(AuthSmsSendReqVO reqVO) {
        // TODO: 实现短信验证码发送
        // 暂时返回成功，实际需要集成短信服务
        log.info("发送短信验证码到手机号: {}", reqVO.getMobile());
        
        // 根据场景决定是否检查用户是否存在
        // 对于登录场景(scene=3)，允许发送验证码，因为用户可能要通过验证码注册
        // 对于其他场景，需要检查用户是否存在
        if (reqVO.getScene() != null && reqVO.getScene() != 3) {
            // 非登录场景，检查用户是否存在
            AdminUserDO user = userService.getUserByMobile(reqVO.getMobile());
            if (user == null) {
                throw exception(AUTH_MOBILE_NOT_EXISTS);
            }
        }
        
        // 发送验证码
        // TODO: 调用实际的短信服务
        log.info("验证码发送成功，手机号: {}, 场景: {}", reqVO.getMobile(), reqVO.getScene());
    }

    @Override
    public void resetPassword(AuthResetPasswordReqVO reqVO) {
        // 1. 校验用户是否存在
        AdminUserDO user = userService.getUserByMobile(reqVO.getMobile());
        if (user == null) {
            throw exception(USER_MOBILE_NOT_EXISTS);
        }

        // 2. 校验验证码
        smsCodeApi.useSmsCode(new SmsCodeUseReqDTO()
                .setCode(reqVO.getCode())
                .setMobile(reqVO.getMobile())
                .setScene(SmsSceneEnum.ADMIN_MEMBER_RESET_PASSWORD.getScene())
                .setUsedIp(getClientIP())
        ).checkError();

        // 3. 更新密码
        userService.updateUserPassword(user.getId(), reqVO.getPassword());
    }



    /**
     * 验证验证码
     */
    private void validateVerificationCode(AuthAppLoginReqVO reqVO) {
        // TODO: 实现验证码验证逻辑
        // 暂时使用固定验证码666666进行测试
        if (!"666666".equals(reqVO.getCode())) {
            throw exception(AUTH_LOGIN_BAD_CREDENTIALS);
        }
        log.info("验证码登录验证通过，手机号: {}", reqVO.getMobile());
    }
    
    /**
     * 验证注册验证码
     */
    private void validateVerificationCodeForRegister(AuthRegisterReqVO reqVO) {
        // TODO: 实现验证码验证逻辑
        // 暂时使用固定验证码666666进行测试
        if (StrUtil.isNotBlank(reqVO.getCode()) && !"666666".equals(reqVO.getCode())) {
            throw exception(AUTH_LOGIN_BAD_CREDENTIALS);
        }
        log.info("注册验证码验证通过，手机号: {}, 邮箱: {}", reqVO.getMobile(), reqVO.getEmail());
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
     * 根据登录类型查找用户
     */
    private AdminUserDO findUserByLoginType(AuthAppLoginReqVO reqVO) {
        switch (reqVO.getLoginType().toUpperCase()) {
            case "USERNAME":
                if (StrUtil.isBlank(reqVO.getUsername())) {
                    throw exception(AUTH_LOGIN_BAD_CREDENTIALS);
                }
                return userService.getUserByUsername(reqVO.getUsername());
            case "MOBILE":
                if (StrUtil.isBlank(reqVO.getMobile())) {
                    throw exception(AUTH_LOGIN_BAD_CREDENTIALS);
                }
                return userService.getUserByMobile(reqVO.getMobile());
            case "EMAIL":
                if (StrUtil.isBlank(reqVO.getEmail())) {
                    throw exception(AUTH_LOGIN_BAD_CREDENTIALS);
                }
                return userService.getUserByEmail(reqVO.getEmail());
            default:
                throw exception(AUTH_LOGIN_BAD_CREDENTIALS);
        }
    }

    /**
     * 获取登录标识符
     */
    private String getLoginIdentifier(AuthAppLoginReqVO reqVO) {
        switch (reqVO.getLoginType().toUpperCase()) {
            case "USERNAME":
                return reqVO.getUsername();
            case "MOBILE":
                return reqVO.getMobile();
            case "EMAIL":
                return reqVO.getEmail();
            default:
                return "";
        }
    }

    /**
     * 获取登录日志类型
     */
    private LoginLogTypeEnum getLoginLogType(String loginType) {
        switch (loginType.toUpperCase()) {
            case "USERNAME":
                return LoginLogTypeEnum.LOGIN_USERNAME;
            case "MOBILE":
                return LoginLogTypeEnum.LOGIN_MOBILE;
            case "EMAIL":
                return LoginLogTypeEnum.LOGIN_USERNAME; // 邮箱登录使用用户名登录类型
            default:
                return LoginLogTypeEnum.LOGIN_USERNAME;
        }
    }

    /**
     * 创建登录成功后的 Token
     */
    private AuthLoginRespVO createTokenAfterLoginSuccess(Long userId, String username, LoginLogTypeEnum logType) {
        return createTokenAfterLoginSuccess(userId, username, logType, null, null);
    }

    /**
     * 创建登录成功后的 Token（带设备信息）
     */
    private AuthLoginRespVO createTokenAfterLoginSuccess(Long userId, String username, LoginLogTypeEnum logType, String deviceType, String deviceId) {
        // 创建刷新令牌
        OAuth2AccessTokenDO accessTokenDO = oauth2TokenService.createAccessToken(userId, getUserType().getValue(),
                OAuth2ClientConstants.CLIENT_ID_DEFAULT, null, deviceType, deviceId);
        
        // 从当前上下文获取租户ID（不再从 user 表获取）
        Long tenantId = TenantContextHolder.getTenantId();

        // 构建返回结果
        AuthLoginRespVO respVO = AuthLoginRespVO.builder()
                .userId(userId)
                .accessToken(accessTokenDO.getAccessToken())
                .refreshToken(accessTokenDO.getRefreshToken())
                .expiresTime(accessTokenDO.getExpiresTime())
                .tenantId(tenantId)
                .build();

        // 如果有租户ID，获取租户名称
        if (tenantId != null) {
            try {
                // 获取租户信息
                TenantDO tenant = tenantService.getTenant(tenantId);
                if (tenant != null) {
                    respVO.setTenantName(tenant.getName());
                } else {
                    respVO.setTenantName("未知租户");
                }
            } catch (Exception e) {
                log.warn("获取租户信息失败: {}", e.getMessage());
                respVO.setTenantName("未知租户");
            }
        }
        
        // 记录登录日志
        createLoginLog(userId, username, logType, LoginResultEnum.SUCCESS, deviceType, deviceId);
        return respVO;
    }

    /**
     * 创建登录日志
     */
    private void createLoginLog(Long userId, String username, LoginLogTypeEnum logTypeEnum, LoginResultEnum loginResult) {
        createLoginLog(userId, username, logTypeEnum, loginResult, null, null);
    }

    /**
     * 创建登录日志（带设备信息）
     */
    private void createLoginLog(Long userId, String username, LoginLogTypeEnum logTypeEnum, LoginResultEnum loginResult, String deviceType, String deviceId) {
        // 插入登录日志
        LoginLogCreateReqDTO reqDTO = new LoginLogCreateReqDTO();
        reqDTO.setLogType(logTypeEnum.getType());
        reqDTO.setTraceId(TracerUtils.getTraceId());
        reqDTO.setUserId(userId);
        reqDTO.setUserType(getUserType().getValue());
        reqDTO.setUsername(username);
        reqDTO.setUserAgent(ServletUtils.getUserAgent());
        reqDTO.setUserIp(ServletUtils.getClientIP());
        reqDTO.setResult(loginResult.getResult());
        reqDTO.setDeviceType(deviceType);
        reqDTO.setDeviceId(deviceId);
        loginLogService.createLoginLog(reqDTO);
        // 更新最后登录时间
        if (userId != null && Objects.equals(LoginResultEnum.SUCCESS.getResult(), loginResult.getResult())) {
            userService.updateUserLogin(userId, ServletUtils.getClientIP());
        }
    }



    /**
     * 获取用户类型
     */
    private com.kyx.foundation.common.enums.UserTypeEnum getUserType() {
        return com.kyx.foundation.common.enums.UserTypeEnum.ADMIN;
    }
} 