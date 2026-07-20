package com.kyx.service.business.controller.admin.scheduler.vo.task;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 定时任务管理 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SchedulerTaskRespVO extends SchedulerTaskBaseVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "租户编号", example = "1")
    private Long tenantId;

    @Schema(description = "租户名称", example = "集团租户")
    private String tenantName;

}
