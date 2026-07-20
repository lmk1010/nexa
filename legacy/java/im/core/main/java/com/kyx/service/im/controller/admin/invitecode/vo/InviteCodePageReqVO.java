package com.kyx.service.im.controller.admin.invitecode.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 邀请码分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class InviteCodePageReqVO extends PageParam {

    @Schema(description = "邀请码", example = "INV123456")
    private String code;

    @Schema(description = "邀请码类型", example = "1")
    private Integer type;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "创建人姓名", example = "张三")
    private String creatorName;

    @Schema(description = "租户ID", example = "1")
    private Long tenantId;

    @Schema(description = "租户名称", example = "示例公司")
    private String tenantName;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

    @Schema(description = "有效期开始时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime validStartTime;

    @Schema(description = "有效期结束时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime validEndTime;

    @Schema(description = "使用次数限制", example = "100")
    private Integer usageLimit;

} 