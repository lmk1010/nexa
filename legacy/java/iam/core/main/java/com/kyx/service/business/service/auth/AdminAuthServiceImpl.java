package com.kyx.service.business.service.auth;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.enums.UserTypeEnum;
import com.kyx.foundation.common.util.monitor.TracerUtils;
import com.kyx.foundation.common.util.servlet.ServletUtils;
import com.kyx.foundation.common.util.validation.ValidationUtils;
import com.kyx.service.business.api.logger.dto.LoginLogCreateReqDTO;
import com.kyx.service.business.api.sms.SmsCodeApi;
import com.kyx.service.business.api.sms.dto.code.SmsCodeUseReqDTO;
import com.kyx.service.business.api.social.dto.SocialUserBindReqDTO;
import com.kyx.service.business.api.social.dto.SocialUserRespDTO;
import com.kyx.service.business.controller.admin.auth.vo.*;
import com.kyx.service.business.controller.admin.tenant.vo.tenant.UserTenantRespVO;
import com.kyx.service.business.convert.auth.AuthConvert;
import com.kyx.service.business.dal.dataobject.auth.AuthPreLoginCacheDO;
import com.kyx.service.business.dal.dataobject.migration.UserSyncDO;
import com.kyx.service.business.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import com.kyx.service.business.dal.dataobject.user.AdminUserDO;
import com.kyx.service.business.dal.mysql.migration.UserSyncMapper;
import com.kyx.service.business.dal.redis.auth.AuthPreLoginRedisDAO;
import com.kyx.service.business.enums.logger.LoginLogTypeEnum;
import com.kyx.service.business.enums.logger.LoginResultEnum;
import com.kyx.service.business.enums.oauth2.OAuth2ClientConstants;
import com.kyx.service.business.enums.social.SocialTypeEnum;
import com.kyx.service.business.enums.sms.SmsSceneEnum;
import com.kyx.service.business.service.logger.LoginLogService;
import com.kyx.service.business.service.member.MemberService;
import com.kyx.service.business.service.oauth2.OAuth2TokenService;
import com.kyx.service.business.service.social.SocialUserService;
import com.kyx.service.business.service.tenant.TenantService;
import com.kyx.service.business.service.tenant.UserTenantRelationService;
import com.kyx.service.business.service.user.AdminUserService;
import com.kyx.service.business.dal.dataobject.tenant.UserTenantRelationDO;
import com.kyx.foundation.tenant.core.aop.TenantIgnore;
import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
import com.google.common.annotations.VisibleForTesting;
import javax.annotation.Resource;
import javax.validation.Validator;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.foundation.common.util.servlet.ServletUtils.getClientIP;
import static com.kyx.service.business.enums.ErrorCodeConstants.*;

/**
 * Auth Service 实现类
 *
 * @author MK
 */
@Service
@Slf4j
public class AdminAuthServiceImpl implements AdminAuthService {

    private static final long PRE_LOGIN_TOKEN_EXPIRE_SECONDS = 300;

    @Resource
    private AdminUserService userService;
    @Resource
    private LoginLogService loginLogService;
    @Resource
    private OAuth2TokenService oauth2TokenService;
    @Resource
    private SocialUserService socialUserService;
    @Resource
    private MemberService memberService;
    @Resource
    private Validator validator;
    @Resource
    private CaptchaService captchaService;
    @Resource
    private SmsCodeApi smsCodeApi;
    @Resource
    private UserTenantRelationService userTenantRelationService;
    @Resource
    private TenantService tenantService;
    @Resource
    private AuthPreLoginRedisDAO authPreLoginRedisDAO;
    @Resource
    private UserSyncMapper userSyncMapper;

    /**
     * 验证码的开关，默认为 true
     */
    @Value("${foundation.captcha.enable:true}")
    @Setter // 为了单测：开启或者关闭验证码
    private Boolean captchaEnable;

    @Override
    @TenantIgnore // 登录时忽略租户过滤，需要跨租户查询用户
    public AdminUserDO authenticate(String username, String password) {
        // 判断账号类型：手机号格式则按手机号查询，否则按用户名查询
        boolean isMobile = username.matches("^1[3-9]\\d{9}$");
        final LoginLogTypeEnum logTypeEnum = isMobile ? LoginLogTypeEnum.LOGIN_MOBILE : LoginLogTypeEnum.LOGIN_USERNAME;

        // 校验账号是否存在（忽略租户过滤）
        AdminUserDO user = isMobile ? userService.getUserByMobile(username) : userService.getUserByUsername(username);
        if (user == null) {
            createLoginLog(null, username, logTypeEnum, LoginResultEnum.BAD_CREDENTIALS);
            throw exception(AUTH_LOGIN_BAD_CREDENTIALS);
        }

        // 校验密码
        if (!userService.isPasswordMatch(password, user.getPassword())) {
            createLoginLog(user.getId(), username, logTypeEnum, LoginResultEnum.BAD_CREDENTIALS);
            throw exception(AUTH_LOGIN_BAD_CREDENTIALS);
        }

        // 校验用户是否在当前租户中
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (currentTenantId != null) {
            UserTenantRelationDO userTenantRelation = userTenantRelationService.getUserTenantRelation(user.getId(), currentTenantId);
            if (userTenantRelation == null || userTenantRelation.getStatus() != 1) {
                createLoginLog(user.getId(), username, logTypeEnum, LoginResultEnum.BAD_CREDENTIALS);
                throw exception(TENANT_USER_NOT_IN_TENANT);
            }
        }

        // 校验是否禁用
        if (CommonStatusEnum.isDisable(user.getStatus())) {
            createLoginLog(user.getId(), username, logTypeEnum, LoginResultEnum.USER_DISABLED);
            throw exception(AUTH_LOGIN_USER_DISABLED);
        }
        return user;
    }

    @Override
    public AuthLoginRespVO login(AuthLoginReqVO reqVO) {
        // 校验验证码
        validateCaptcha(reqVO);

        // 使用账号密码，进行登录
        AdminUserDO user = authenticate(reqVO.getUsername(), reqVO.getPassword());

        // 如果 socialType 非空，说明需要绑定社交用户
        if (reqVO.getSocialType() != null) {
            socialUserService.bindSocialUser(new SocialUserBindReqDTO(user.getId(), getUserType().getValue(),
                    reqVO.getSocialType(), reqVO.getSocialCode(), reqVO.getSocialState()));
        }
        // 创建 Token 令牌，记录登录日志
        return createTokenAfterLoginSuccess(user.getId(), reqVO.getUsername(), LoginLogTypeEnum.LOGIN_USERNAME, reqVO.getDeviceType(), reqVO.getDeviceId());
    }

    @Override
    public AuthPreLoginRespVO preLogin(AuthPreLoginReqVO reqVO) {
        validateCaptcha(reqVO);

        AdminUserDO user = authenticate(reqVO.getUsername(), reqVO.getPassword());
        List<UserTenantRespVO> tenantList = tenantService.getUserTenantList(user.getId());
        if (CollUtil.isEmpty(tenantList)) {
            throw exception(AUTH_PRE_LOGIN_TENANT_INVALID);
        }

        String preAuthToken = IdUtil.fastSimpleUUID();
        AuthPreLoginCacheDO cacheDO = new AuthPreLoginCacheDO();
        cacheDO.setUserId(user.getId());
        cacheDO.setUsername(reqVO.getUsername());
        cacheDO.setDeviceType(reqVO.getDeviceType());
        cacheDO.setDeviceId(reqVO.getDeviceId());
        cacheDO.setTenantIds(tenantList.stream().map(UserTenantRespVO::getTenantId).filter(Objects::nonNull).collect(java.util.stream.Collectors.toList()));
        authPreLoginRedisDAO.set(preAuthToken, cacheDO, PRE_LOGIN_TOKEN_EXPIRE_SECONDS);

        return AuthPreLoginRespVO.builder()
                .preAuthToken(preAuthToken)
                .tenantList(tenantList)
                .build();
    }

    @Override
    public AuthLoginRespVO tenantLogin(AuthTenantLoginReqVO reqVO) {
        AuthPreLoginCacheDO cacheDO = authPreLoginRedisDAO.get(reqVO.getPreAuthToken());
        if (cacheDO == null || cacheDO.getUserId() == null) {
            throw exception(AUTH_PRE_LOGIN_TOKEN_INVALID);
        }
        if (reqVO.getTenantId() == null || CollUtil.isEmpty(cacheDO.getTenantIds()) || !cacheDO.getTenantIds().contains(reqVO.getTenantId())) {
            throw exception(AUTH_PRE_LOGIN_TENANT_INVALID);
        }

        UserTenantRelationDO relation = userTenantRelationService.getUserTenantRelation(cacheDO.getUserId(), reqVO.getTenantId());
        if (relation == null || relation.getStatus() != 1) {
            throw exception(AUTH_PRE_LOGIN_TENANT_INVALID);
        }

        String deviceType = reqVO.getDeviceType() != null ? reqVO.getDeviceType() : cacheDO.getDeviceType();
        String deviceId = reqVO.getDeviceId() != null ? reqVO.getDeviceId() : cacheDO.getDeviceId();
        authPreLoginRedisDAO.delete(reqVO.getPreAuthToken());
        return TenantUtils.execute(reqVO.getTenantId(), () ->
                createTokenAfterLoginSuccess(
                        cacheDO.getUserId(),
                        cacheDO.getUsername(),
                        LoginLogTypeEnum.LOGIN_USERNAME,
                        deviceType,
                        deviceId
                )
        );
    }

    @Override
    public void sendSmsCode(AuthSmsSendReqVO reqVO) {
        // 如果是重置密码场景，需要校验图形验证码是否正确
        if (Objects.equals(SmsSceneEnum.ADMIN_MEMBER_RESET_PASSWORD.getScene(), reqVO.getScene())) {
            ResponseModel response = doValidateCaptcha(reqVO);
            if (!response.isSuccess()) {
                throw exception(AUTH_REGISTER_CAPTCHA_CODE_ERROR, response.getRepMsg());
            }
        }

        // 登录场景，验证是否存在
        if (userService.getUserByMobile(reqVO.getMobile()) == null) {
            throw exception(AUTH_MOBILE_NOT_EXISTS);
        }
        // 发送验证码
        smsCodeApi.sendSmsCode(AuthConvert.INSTANCE.convert(reqVO).setCreateIp(getClientIP()));
    }

    @Override
    public AuthLoginRespVO smsLogin(AuthSmsLoginReqVO reqVO) {
        // 校验验证码
        smsCodeApi.useSmsCode(AuthConvert.INSTANCE.convert(reqVO, SmsSceneEnum.ADMIN_MEMBER_LOGIN.getScene(), getClientIP())).checkError();

        // 获得用户信息
        AdminUserDO user = userService.getUserByMobile(reqVO.getMobile());
        if (user == null) {
            throw exception(USER_NOT_EXISTS);
        }

        // 创建 Token 令牌，记录登录日志
        return createTokenAfterLoginSuccess(user.getId(), reqVO.getMobile(), LoginLogTypeEnum.LOGIN_MOBILE);
    }

    private void createLoginLog(Long userId, String username,
                                LoginLogTypeEnum logTypeEnum, LoginResultEnum loginResult) {
        createLoginLog(userId, username, logTypeEnum, loginResult, null, null);
    }

    private void createLoginLog(Long userId, String username,
                                LoginLogTypeEnum logTypeEnum, LoginResultEnum loginResult, String deviceType, String deviceId) {
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

    @Override
    public AuthLoginRespVO socialLogin(AuthSocialLoginReqVO reqVO) {
        AdminUserDO user = resolveSocialLoginUser(reqVO);
        // 创建 Token 令牌，记录登录日志
        return createTokenAfterLoginSuccess(user.getId(), user.getUsername(), LoginLogTypeEnum.LOGIN_SOCIAL);
    }

    @Override
    public AuthPreLoginRespVO socialPreLogin(AuthSocialLoginReqVO reqVO) {
        AdminUserDO user = resolveSocialLoginUser(reqVO);
        List<UserTenantRespVO> tenantList = tenantService.getUserTenantList(user.getId()).stream()
                .filter(item -> Boolean.TRUE.equals(item.getHasRole()))
                .collect(java.util.stream.Collectors.toList());
        if (CollUtil.isEmpty(tenantList)) {
            throw exception(AUTH_PRE_LOGIN_TENANT_INVALID);
        }

        String preAuthToken = IdUtil.fastSimpleUUID();
        AuthPreLoginCacheDO cacheDO = new AuthPreLoginCacheDO();
        cacheDO.setUserId(user.getId());
        cacheDO.setUsername(user.getUsername());
        cacheDO.setDeviceType("WEB");
        cacheDO.setTenantIds(tenantList.stream()
                .map(UserTenantRespVO::getTenantId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toList()));
        authPreLoginRedisDAO.set(preAuthToken, cacheDO, PRE_LOGIN_TOKEN_EXPIRE_SECONDS);

        return AuthPreLoginRespVO.builder()
                .preAuthToken(preAuthToken)
                .tenantList(tenantList)
                .build();
    }

    private AdminUserDO resolveSocialLoginUser(AuthSocialLoginReqVO reqVO) {
        String resolvedAuthCode = reqVO.resolveAuthCode();
        SocialUserRespDTO socialUser = socialUserService.getSocialUserByCode(
                UserTypeEnum.ADMIN.getValue(), reqVO.getType(), resolvedAuthCode, reqVO.getState());
        Long resolvedUserId = socialUser != null ? socialUser.getUserId() : null;
        if (resolvedUserId == null) {
            resolvedUserId = tryAutoBindByDingTalkMapping(reqVO, socialUser, resolvedAuthCode);
        }
        if (resolvedUserId == null) {
            throw exception(AUTH_THIRD_LOGIN_NOT_BIND);
        }

        AdminUserDO user = userService.getUserIgnoreTenant(resolvedUserId);
        if (user == null) {
            throw exception(USER_NOT_EXISTS);
        }
        if (CommonStatusEnum.isDisable(user.getStatus())) {
            throw exception(AUTH_LOGIN_USER_DISABLED);
        }
        return user;
    }

    private Long tryAutoBindByDingTalkMapping(AuthSocialLoginReqVO reqVO, SocialUserRespDTO socialUser,
                                              String resolvedAuthCode) {
        if (!Objects.equals(reqVO.getType(), SocialTypeEnum.DINGTALK.getType()) || socialUser == null) {
            return null;
        }
        String dingTalkUserId = StrUtil.trimToNull(socialUser.getOpenid());
        if (dingTalkUserId == null) {
            return null;
        }
        AdminUserDO matchedUser = resolveUserBySyncMapping(userSyncMapper.selectByExternalUserId(dingTalkUserId));
        if (matchedUser == null) {
            matchedUser = resolveUserByMobile(socialUser.getMobile());
        }
        if (matchedUser == null) {
            // 昵称精确匹配兜底：用于用户同步数据未写入映射表时自动绑定
            matchedUser = resolveUserByDisplayName(socialUser.getNickname());
        }
        if (matchedUser == null) {
            return null;
        }
        if (CommonStatusEnum.isDisable(matchedUser.getStatus())) {
            log.warn("Skip DingTalk auto bind because user disabled, userId={}, dingTalkUserId={}",
                    matchedUser.getId(), dingTalkUserId);
            return null;
        }

        socialUserService.bindSocialUser(new SocialUserBindReqDTO(
                matchedUser.getId(),
                UserTypeEnum.ADMIN.getValue(),
                reqVO.getType(),
                resolvedAuthCode,
                reqVO.getState()
        ));
        log.info("Auto bind DingTalk social user success, dingTalkUserId={}, userId={}",
                dingTalkUserId, matchedUser.getId());
        return matchedUser.getId();
    }

    private AdminUserDO resolveUserBySyncMapping(UserSyncDO mapping) {
        if (mapping == null) {
            return null;
        }
        AdminUserDO matchedUser = null;
        if (StrUtil.isNotBlank(mapping.getMobile())) {
            matchedUser = userService.getUserByMobile(mapping.getMobile());
        }
        if (matchedUser == null && StrUtil.isNotBlank(mapping.getUsername())) {
            matchedUser = userService.getUserByUsername(mapping.getUsername());
        }
        if (matchedUser == null && StrUtil.isNotBlank(mapping.getNickname())) {
            matchedUser = resolveUserByDisplayName(mapping.getNickname());
        }
        if (matchedUser == null && StrUtil.isNotBlank(mapping.getLinkName())) {
            matchedUser = resolveUserByDisplayName(mapping.getLinkName());
        }
        return matchedUser;
    }

    private AdminUserDO resolveUserByMobile(String mobile) {
        String normalizedMobile = StrUtil.trimToNull(mobile);
        if (normalizedMobile == null) {
            return null;
        }
        return userService.getUserByMobile(normalizedMobile);
    }

    private AdminUserDO resolveUserByDisplayName(String displayName) {
        String normalizedName = StrUtil.trimToNull(displayName);
        if (normalizedName == null) {
            return null;
        }
        List<AdminUserDO> candidates = userService.getUserListByNickname(normalizedName);
        if (CollUtil.isEmpty(candidates)) {
            return null;
        }
        List<AdminUserDO> exactMatches = candidates.stream()
                .filter(user -> StrUtil.equals(normalizedName, StrUtil.trimToEmpty(user.getNickname())))
                .collect(java.util.stream.Collectors.toList());
        if (exactMatches.size() != 1) {
            log.warn("Skip DingTalk auto bind by name because match is ambiguous, name={}, matchCount={}",
                    normalizedName, exactMatches.size());
            return null;
        }
        return exactMatches.get(0);
    }

    @VisibleForTesting
    void validateCaptcha(AuthLoginReqVO reqVO) {
        ResponseModel response = doValidateCaptcha(reqVO);
        // 校验验证码
        if (!response.isSuccess()) {
            // 创建登录失败日志（验证码不正确)
            createLoginLog(null, reqVO.getUsername(), LoginLogTypeEnum.LOGIN_USERNAME, LoginResultEnum.CAPTCHA_CODE_ERROR);
            throw exception(AUTH_LOGIN_CAPTCHA_CODE_ERROR, response.getRepMsg());
        }
    }

    @VisibleForTesting
    void validateCaptcha(AuthPreLoginReqVO reqVO) {
        if (isMobileClient(reqVO.getDeviceType())) {
            return;
        }
        ResponseModel response = doValidateCaptcha(reqVO);
        if (!response.isSuccess()) {
            createLoginLog(null, reqVO.getUsername(), LoginLogTypeEnum.LOGIN_USERNAME, LoginResultEnum.CAPTCHA_CODE_ERROR);
            throw exception(AUTH_LOGIN_CAPTCHA_CODE_ERROR, response.getRepMsg());
        }
    }

    private boolean isMobileClient(String deviceType) {
        if (deviceType == null) {
            return false;
        }
        String normalized = deviceType.trim().toUpperCase();
        return normalized.startsWith("MOBILE")
                || normalized.equals("ANDROID")
                || normalized.equals("IOS")
                || normalized.equals("APP");
    }

    private ResponseModel doValidateCaptcha(CaptchaVerificationReqVO reqVO) {
        // 如果验证码关闭，则不进行校验
        if (!captchaEnable) {
            return ResponseModel.success();
        }
        ValidationUtils.validate(validator, reqVO, CaptchaVerificationReqVO.CodeEnableGroup.class);
        CaptchaVO captchaVO = new CaptchaVO();
        captchaVO.setCaptchaVerification(reqVO.getCaptchaVerification());
        return captchaService.verification(captchaVO);
    }

    private AuthLoginRespVO createTokenAfterLoginSuccess(Long userId, String username, LoginLogTypeEnum logType) {
        return createTokenAfterLoginSuccess(userId, username, logType, null, null);
    }

    private AuthLoginRespVO createTokenAfterLoginSuccess(Long userId, String username, LoginLogTypeEnum logType, String deviceType, String deviceId) {
        // 插入登陆日志
        createLoginLog(userId, username, logType, LoginResultEnum.SUCCESS, deviceType, deviceId);
        // 创建访问令牌
        OAuth2AccessTokenDO accessTokenDO = oauth2TokenService.createAccessToken(userId, getUserType().getValue(),
                OAuth2ClientConstants.CLIENT_ID_DEFAULT, null, deviceType, deviceId);
        // 构建返回结果
        return AuthConvert.INSTANCE.convert(accessTokenDO);
    }

    @Override
    public AuthLoginRespVO refreshToken(String refreshToken) {
        OAuth2AccessTokenDO accessTokenDO = oauth2TokenService.refreshAccessToken(refreshToken, OAuth2ClientConstants.CLIENT_ID_DEFAULT);
        return AuthConvert.INSTANCE.convert(accessTokenDO);
    }

    @Override
    public void logout(String token, Integer logType) {
        // 删除访问令牌
        OAuth2AccessTokenDO accessTokenDO = oauth2TokenService.removeAccessToken(token);
        if (accessTokenDO == null) {
            return;
        }
        // 删除成功，则记录登出日志
        createLogoutLog(accessTokenDO.getUserId(), accessTokenDO.getUserType(), logType);
    }

    private void createLogoutLog(Long userId, Integer userType, Integer logType) {
        LoginLogCreateReqDTO reqDTO = new LoginLogCreateReqDTO();
        reqDTO.setLogType(logType);
        reqDTO.setTraceId(TracerUtils.getTraceId());
        reqDTO.setUserId(userId);
        reqDTO.setUserType(userType);
        if (ObjectUtil.equal(getUserType().getValue(), userType)) {
            reqDTO.setUsername(getUsername(userId));
        } else {
            reqDTO.setUsername(memberService.getMemberUserMobile(userId));
        }
        reqDTO.setUserAgent(ServletUtils.getUserAgent());
        reqDTO.setUserIp(ServletUtils.getClientIP());
        reqDTO.setResult(LoginResultEnum.SUCCESS.getResult());
        loginLogService.createLoginLog(reqDTO);
    }

    private String getUsername(Long userId) {
        if (userId == null) {
            return null;
        }
        AdminUserDO user = userService.getUser(userId);
        return user != null ? user.getUsername() : null;
    }

    private UserTypeEnum getUserType() {
        return UserTypeEnum.ADMIN;
    }

    @Override
    public AuthLoginRespVO register(AuthRegisterReqVO registerReqVO) {
        // 1. 校验验证码
        validateCaptcha(registerReqVO);

        // 2. 校验用户名是否已存在
        Long userId = userService.registerUser(registerReqVO);

        // 3. 创建 Token 令牌，记录登录日志
        return createTokenAfterLoginSuccess(userId, registerReqVO.getUsername(), LoginLogTypeEnum.LOGIN_USERNAME);
    }

    @VisibleForTesting
    void validateCaptcha(AuthRegisterReqVO reqVO) {
        ResponseModel response = doValidateCaptcha(reqVO);
        // 验证不通过
        if (!response.isSuccess()) {
            throw exception(AUTH_REGISTER_CAPTCHA_CODE_ERROR, response.getRepMsg());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(AuthResetPasswordReqVO reqVO) {
        AdminUserDO userByMobile = userService.getUserByMobile(reqVO.getMobile());
        if (userByMobile == null) {
            throw exception(USER_MOBILE_NOT_EXISTS);
        }

        smsCodeApi.useSmsCode(new SmsCodeUseReqDTO()
                .setCode(reqVO.getCode())
                .setMobile(reqVO.getMobile())
                .setScene(SmsSceneEnum.ADMIN_MEMBER_RESET_PASSWORD.getScene())
                .setUsedIp(getClientIP())
        ).checkError();

        userService.updateUserPassword(userByMobile.getId(), reqVO.getPassword());
    }
}
