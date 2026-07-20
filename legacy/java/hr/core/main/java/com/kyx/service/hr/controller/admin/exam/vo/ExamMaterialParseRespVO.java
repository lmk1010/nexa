package com.kyx.service.hr.controller.admin.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 考试培训资料解析 Response VO")
@Data
public class ExamMaterialParseRespVO {

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "解析后的文本内容")
    private String content;

}
