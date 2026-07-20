package com.kyx.service.business.controller.admin.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;

@Schema(description = "管理后台 - 预登录 Request VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthPreLoginReqVO extends CaptchaVerificationReqVO {

    @Schema(description = "账号（支持用户名、手机号）", requiredMode = Schema.RequiredMode.REQUIRED, example = "admin")
    @NotEmpty(message = "登录账号不能为空")
    @Length(min = 2, max = 32, message = "账号长度为 2-32 位")
    private String username;

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "admin123")
    @NotEmpty(message = "密码不能为空")
    @Length(min = 4, max = 16, message = "密码长度为 4-16 位")
    private String password;

    @Schema(description = "设备类型", example = "WEB")
    private String deviceType;

    @Schema(description = "设备标识", example = "device_123456")
    private String deviceId;

}
