package com.kyx.service.business.controller.admin.auth.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 在线用户分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class OnlineUserPageReqVO extends PageParam {

    @Schema(description = "用户ID", example = "1024")
    private Long userId;

    @Schema(description = "用户名", example = "foundationyuanma")
    private String username;

    @Schema(description = "设备类型", example = "WEB")
    private String deviceType;

    @Schema(description = "设备标识", example = "device_123456")
    private String deviceId;

} 