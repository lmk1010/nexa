package com.kyx.service.im.controller.admin.invitecode.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Schema(description = "管理后台 - 邀请码 Response VO")
@Data
@Accessors(chain = true)
public class InviteCodeRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "邀请码", requiredMode = Schema.RequiredMode.REQUIRED, example = "INV123456")
    private String code;

    @Schema(description = "租户ID", example = "1")
    private Long tenantId;

    @Schema(description = "租户名称", example = "示例公司")
    private String tenantName;

    @Schema(description = "租户状态", example = "1")
    private Integer tenantStatus;

    @Schema(description = "邀请码类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer type;

    @Schema(description = "使用次数限制", example = "100")
    private Integer usageLimit;

    @Schema(description = "已使用次数", example = "10")
    private Integer usedCount;

    @Schema(description = "有效期开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date validStartTime;

    @Schema(description = "有效期结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date validEndTime;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "备注", example = "这是一个邀请码")
    private String remark;

    @Schema(description = "创建者用户ID", example = "1024")
    private Long creatorId;

    @Schema(description = "创建者用户名", example = "admin")
    private String creatorName;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

} 