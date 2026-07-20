package com.kyx.service.hr.controller.admin.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 考试开始 Request VO")
@Data
public class ExamStartReqVO {

    @Schema(description = "考试ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "考试ID不能为空")
    private Long examId;

    @Schema(description = "发布批次ID")
    private Long publishId;

}
