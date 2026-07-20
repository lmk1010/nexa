package com.kyx.service.business.controller.admin.scheduler.vo.task;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.kyx.foundation.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 定时任务管理分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SchedulerTaskPageReqVO extends PageParam {

    @Schema(description = "任务名称", example = "用户数据同步")
    private String taskName;

    @Schema(description = "任务状态", example = "1")
    private Integer taskStatus;

    @Schema(description = "任务类型", example = "SYNC")
    private String taskType;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}