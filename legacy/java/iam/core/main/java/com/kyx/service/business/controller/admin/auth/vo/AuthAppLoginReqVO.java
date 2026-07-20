package com.kyx.service.business.controller.admin.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

@Schema(description = "移动端 - 登录 Request VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthAppLoginReqVO {

    @Schema(description = "登录类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "USERNAME", 
            allowableValues = {"USERNAME", "MOBILE", "EMAIL"})
    @NotEmpty(message = "登录类型不能为空")
    private String loginType;

    @Schema(description = "用户名", example = "foundationyuanma")
    @Length(min = 4, max = 16, message = "用户名长度为 4-16 位")
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "用户名格式为数字以及字母")
    private String username;

    @Schema(description = "手机号", example = "13800138000")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String mobile;

    @Schema(description = "邮箱", example = "user@example.com")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "邮箱格式不正确")
    private String email;

    @Schema(description = "密码", example = "buzhidao")
    @Length(min = 4, max = 16, message = "密码长度为 4-16 位")
    private String password;

    @Schema(description = "验证码", example = "666666")
    @Length(min = 6, max = 6, message = "验证码为6位数字")
    private String code;

    @Schema(description = "设备类型", example = "MOBILE_ANDROID")
    private String deviceType;

    @Schema(description = "设备标识", example = "device_123456")
    private String deviceId;
} 