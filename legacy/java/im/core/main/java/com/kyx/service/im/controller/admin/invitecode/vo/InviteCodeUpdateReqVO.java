package com.kyx.service.im.controller.admin.invitecode.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 邀请码更新 Request VO")
@Data
public class InviteCodeUpdateReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "编号不能为空")
    private Long id;

    @Schema(description = "邀请码", example = "INV123456")
    private String code;

    @Schema(description = "邀请码类型", example = "1")
    private Integer type;

    @Schema(description = "使用次数限制", example = "100")
    private Integer usageLimit;

    @Schema(description = "有效期开始时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime validStartTime;

    @Schema(description = "有效期结束时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime validEndTime;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "备注", example = "这是一个邀请码")
    private String remark;

} 