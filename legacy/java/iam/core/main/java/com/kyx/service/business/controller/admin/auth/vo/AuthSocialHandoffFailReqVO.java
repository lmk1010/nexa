package com.kyx.service.business.controller.admin.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Schema(description = "管理后台 - 社交登录浏览器握手失败 Request VO")
@Data
public class AuthSocialHandoffFailReqVO {

    @Schema(description = "浏览器握手 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "c7d57a50f0dd4ca09fa1a08e8f8d0cab")
    @NotEmpty(message = "handoffId 不能为空")
    private String handoffId;

    @Schema(description = "失败提示", requiredMode = Schema.RequiredMode.REQUIRED, example = "钉钉授权失败，请稍后重试")
    @NotEmpty(message = "message 不能为空")
    private String message;

}
