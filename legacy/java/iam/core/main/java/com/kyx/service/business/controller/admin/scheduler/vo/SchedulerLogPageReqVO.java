package com.kyx.service.business.controller.admin.scheduler.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 定时任务执行日志分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SchedulerLogPageReqVO extends PageParam {

    @Schema(description = "任务名称", example = "岗位同步")
    private String jobName;

    @Schema(description = "执行状态", example = "0")
    private Integer status;

    @Schema(description = "开始时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] startTime;

}