package com.kyx.service.business.service.auth;

import com.kyx.service.business.controller.admin.auth.vo.*;
import com.kyx.service.business.dal.dataobject.user.AdminUserDO;

import javax.validation.Valid;

/**
 * 管理后台的认证 Service 接口
 *
 * 提供用户的登录、登出的能力
 *
 * @author MK
 */
public interface AdminAuthService {

    /**
     * 验证账号 + 密码。如果通过，则返回用户
     *
     * @param username 账号
     * @param password 密码
     * @return 用户
     */
    AdminUserDO authenticate(String username, String password);

    /**
     * 账号登录
     *
     * @param reqVO 登录信息
     * @return 登录结果
     */
    AuthLoginRespVO login(@Valid AuthLoginReqVO reqVO);

    /**
     * 预登录（账号密码校验，不下发正式令牌）
     *
     * @param reqVO 预登录信息
     * @return 预登录结果
     */
    AuthPreLoginRespVO preLogin(@Valid AuthPreLoginReqVO reqVO);

    /**
     * 确认租户并完成登录
     *
     * @param reqVO 租户确认信息
     * @return 登录结果
     */
    AuthLoginRespVO tenantLogin(@Valid AuthTenantLoginReqVO reqVO);

    /**
     * 基于 token 退出登录
     *
     * @param token token
     * @param logType 登出类型
     */
    void logout(String token, Integer logType);

    /**
     * 短信验证码发送
     *
     * @param reqVO 发送请求
     */
    void sendSmsCode(AuthSmsSendReqVO reqVO);

    /**
     * 短信登录
     *
     * @param reqVO 登录信息
     * @return 登录结果
     */
    AuthLoginRespVO smsLogin(AuthSmsLoginReqVO reqVO);

    /**
     * 社交快捷登录，使用 code 授权码
     *
     * @param reqVO 登录信息
     * @return 登录结果
     */
    AuthLoginRespVO socialLogin(@Valid AuthSocialLoginReqVO reqVO);

    /**
     * 社交预登录，使用 code 授权码，返回可选租户与预登录令牌
     *
     * @param reqVO 登录信息
     * @return 预登录结果
     */
    AuthPreLoginRespVO socialPreLogin(@Valid AuthSocialLoginReqVO reqVO);

    /**
     * 刷新访问令牌
     *
     * @param refreshToken 刷新令牌
     * @return 登录结果
     */
    AuthLoginRespVO refreshToken(String refreshToken);

    /**
     * 用户注册
     *
     * @param createReqVO 注册用户
     * @return 注册结果
     */
    AuthLoginRespVO register(AuthRegisterReqVO createReqVO);

    /**
     * 重置密码
     *
     * @param reqVO 验证码信息
     */
    void resetPassword(AuthResetPasswordReqVO reqVO);

}
