package com.kyx.service.business.controller.app.auth;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.security.config.SecurityProperties;
import com.kyx.foundation.security.core.util.SecurityFrameworkUtils;
import com.kyx.service.business.controller.admin.auth.vo.AuthAppLoginReqVO;
import com.kyx.service.business.controller.admin.auth.vo.AuthLoginRespVO;
import com.kyx.service.business.controller.admin.auth.vo.AuthRegisterReqVO;
import com.kyx.service.business.controller.admin.auth.vo.AuthResetPasswordReqVO;
import com.kyx.service.business.controller.admin.auth.vo.AuthSmsSendReqVO;
import com.kyx.service.business.service.auth.AppAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import static com.kyx.foundation.common.pojo.CommonResult.success;

/**
 * 移动端认证控制器
 * 
 * 提供移动端应用的用户认证相关API接口
 * 包括登录、注册、验证码发送、密码重置等功能
 * 
 * 主要功能：
 * - 用户登录认证（支持多种登录方式）
 * - 用户注册
 * - 短信验证码发送
 * - 密码重置
 * 
 * 安全特性：
 * - 所有接口都标记为@PermitAll，允许未认证访问
 * - 使用@Validated进行参数验证
 * - 支持多种登录方式的安全认证
 * 
 * @author MK
 * @version 1.0.0
 * @since 2024-01-01
 */
@Tag(name = "移动端 - 认证")
@RestController
@RequestMapping("/app/auth")
@Validated
@Slf4j
public class AppAuthController {

    /**
     * 移动端认证服务
     */
    @Resource
    private AppAuthService appAuthService;

    /**
     * 安全配置属性
     */
    @Resource
    private SecurityProperties securityProperties;

    /**
     * 移动端用户登录
     * 
     * 支持多种登录方式：
     * - 用户名登录
     * - 手机号登录
     * - 邮箱登录
     * 
     * 支持密码登录和验证码登录两种认证方式
     * 
     * @param reqVO 登录请求参数
     * @return 登录响应结果，包含用户信息和访问令牌
     */
    @PostMapping("/login")
    @PermitAll
    @Operation(summary = "移动端登录", description = "支持用户名、手机号、邮箱三种登录方式")
    public CommonResult<AuthLoginRespVO> appLogin(@RequestBody @Valid AuthAppLoginReqVO reqVO) {
        return success(appAuthService.appLogin(reqVO));
    }

    /**
     * 移动端用户注册
     * 
     * 支持多种注册方式：
     * - 用户名注册
     * - 手机号注册
     * - 邮箱注册
     * 
     * 注册成功后自动登录并返回用户信息
     * 
     * @param reqVO 注册请求参数
     * @return 注册响应结果，包含用户信息和访问令牌
     */
    @PostMapping("/register")
    @PermitAll
    @Operation(summary = "移动端注册", description = "支持用户名、手机号、邮箱三种注册方式")
    public CommonResult<AuthLoginRespVO> appRegister(@RequestBody @Valid AuthRegisterReqVO reqVO) {
        return success(appAuthService.appRegister(reqVO));
    }

    /**
     * 发送短信验证码
     * 
     * 用于登录、注册、密码重置等场景的短信验证码发送
     * 支持手机号验证码发送功能
     * 
     * @param reqVO 短信发送请求参数
     * @return 发送结果
     */
    @PostMapping("/send-sms-code")
    @PermitAll
    @Operation(summary = "发送手机验证码")
    public CommonResult<Boolean> sendSmsCode(@RequestBody @Valid AuthSmsSendReqVO reqVO) {
        appAuthService.sendSmsCode(reqVO);
        return success(true);
    }

    /**
     * 重置用户密码
     * 
     * 通过验证码验证后重置用户密码
     * 支持手机号验证码重置密码
     * 
     * @param reqVO 密码重置请求参数
     * @return 重置结果
     */
    @PostMapping("/reset-password")
    @PermitAll
    @Operation(summary = "重置密码")
    public CommonResult<Boolean> resetPassword(@RequestBody @Valid AuthResetPasswordReqVO reqVO) {
        appAuthService.resetPassword(reqVO);
        return success(true);
    }

} 