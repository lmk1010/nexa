package com.kyx.service.business.service.auth;

import com.kyx.service.business.controller.admin.auth.vo.AuthAppLoginReqVO;
import com.kyx.service.business.controller.admin.auth.vo.AuthLoginRespVO;
import com.kyx.service.business.controller.admin.auth.vo.AuthRegisterReqVO;
import com.kyx.service.business.controller.admin.auth.vo.AuthResetPasswordReqVO;
import com.kyx.service.business.controller.admin.auth.vo.AuthSmsSendReqVO;


import javax.validation.Valid;

/**
 * 移动端认证服务接口
 * 
 * 提供移动端应用的用户认证相关业务功能
 * 包括用户登录、注册、验证码发送、密码重置等核心功能
 * 
 * 主要功能：
 * - 多方式用户登录认证
 * - 用户注册和信息管理
 * - 短信验证码发送
 * - 密码重置和安全管理
 * 
 * 安全特性：
 * - 支持多种登录方式（用户名、手机号、邮箱）
 * - 支持密码登录和验证码登录
 * - 密码加密存储和验证
 * - 访问令牌生成和管理
 * - 用户会话管理
 * 
 * @author MK
 * @version 1.0.0
 * @since 2024-01-01
 */
public interface AppAuthService {

    /**
     * 移动端用户登录
     * 
     * 支持多种登录方式：
     * - 用户名登录：使用用户名和密码进行认证
     * - 手机号登录：支持密码登录和验证码登录
     * - 邮箱登录：支持密码登录和验证码登录
     * 
     * 登录成功后返回：
     * - 用户基本信息
     * - 访问令牌（Access Token）
     * - 刷新令牌（Refresh Token）
     * - 令牌过期时间
     * - 用户权限信息
     * 
     * @param reqVO 登录请求参数，包含登录类型、用户标识、密码或验证码等
     * @return 登录响应结果，包含用户信息和访问令牌
     * @throws com.kyx.foundation.common.exception.ServiceException 当登录失败时抛出异常
     */
    AuthLoginRespVO appLogin(@Valid AuthAppLoginReqVO reqVO);

    /**
     * 移动端用户注册
     * 
     * 支持多种注册方式：
     * - 用户名注册：使用用户名、密码和验证码
     * - 手机号注册：使用手机号、验证码和密码
     * - 邮箱注册：使用邮箱、验证码和密码
     * 
     * 注册流程：
     * 1. 验证注册参数的有效性
     * 2. 检查用户是否已存在
     * 3. 验证验证码的正确性
     * 4. 创建用户账户
     * 5. 自动登录并返回用户信息
     * 
     * @param reqVO 注册请求参数，包含注册类型、用户信息、验证码等
     * @return 注册响应结果，包含用户信息和访问令牌
     * @throws com.kyx.foundation.common.exception.ServiceException 当注册失败时抛出异常
     */
    AuthLoginRespVO appRegister(@Valid AuthRegisterReqVO reqVO);

    /**
     * 发送短信验证码
     * 
     * 用于以下场景的短信验证码发送：
     * - 用户注册时的手机号验证
     * - 用户登录时的验证码登录
     * - 密码重置时的身份验证
     * - 其他需要手机号验证的业务场景
     * 
     * 发送流程：
     * 1. 验证手机号格式
     * 2. 检查发送频率限制
     * 3. 生成随机验证码
     * 4. 调用短信服务发送验证码
     * 5. 将验证码存储到缓存中（设置过期时间）
     * 
     * @param reqVO 短信发送请求参数，包含手机号、验证码类型等
     * @throws com.kyx.foundation.common.exception.ServiceException 当发送失败时抛出异常
     */
    void sendSmsCode(@Valid AuthSmsSendReqVO reqVO);

    /**
     * 重置用户密码
     * 
     * 通过手机号验证码验证后重置用户密码
     * 
     * 重置流程：
     * 1. 验证手机号格式
     * 2. 验证短信验证码的正确性
     * 3. 检查用户是否存在
     * 4. 加密新密码
     * 5. 更新用户密码
     * 6. 清除相关缓存
     * 
     * @param reqVO 密码重置请求参数，包含手机号、验证码、新密码等
     * @throws com.kyx.foundation.common.exception.ServiceException 当重置失败时抛出异常
     */
    void resetPassword(@Valid AuthResetPasswordReqVO reqVO);

} 