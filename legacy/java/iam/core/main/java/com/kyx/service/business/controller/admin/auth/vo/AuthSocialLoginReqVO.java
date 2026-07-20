package com.kyx.service.business.controller.admin.auth.vo;

import cn.hutool.core.util.StrUtil;
import com.kyx.foundation.common.validation.InEnum;
import com.kyx.service.business.enums.social.SocialTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 社交绑定登录 Request VO，使用 code 或 authCode 授权码")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthSocialLoginReqVO {

    @Schema(description = "社交平台的类型，参见 SocialTypeEnum 枚举值", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @InEnum(SocialTypeEnum.class)
    @NotNull(message = "社交平台的类型不能为空")
    private Integer type;

    @Schema(description = "授权码(code)", example = "1024")
    private String code;

    @Schema(description = "授权码(authCode)", example = "1024")
    private String authCode;

    @Schema(description = "state", requiredMode = Schema.RequiredMode.REQUIRED, example = "9b2ffbc1-7425-4155-9894-9d5c08541d62")
    @NotEmpty(message = "state 不能为空")
    private String state;

    @AssertTrue(message = "授权码不能为空")
    public boolean isAuthCodePresent() {
        return StrUtil.isNotBlank(code) || StrUtil.isNotBlank(authCode);
    }

    public String resolveAuthCode() {
        String normalizedCode = StrUtil.trimToNull(code);
        if (normalizedCode != null) {
            return normalizedCode;
        }
        return StrUtil.trimToNull(authCode);
    }

}
