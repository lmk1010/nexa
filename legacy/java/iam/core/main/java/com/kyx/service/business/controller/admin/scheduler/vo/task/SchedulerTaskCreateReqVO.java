package com.kyx.service.business.controller.admin.scheduler.vo.task;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "管理后台 - 定时任务管理创建 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SchedulerTaskCreateReqVO extends SchedulerTaskBaseVO {

}