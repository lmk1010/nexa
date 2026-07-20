package com.kyx.service.hr.controller.admin.questionnaire.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 问卷公开链接 Save VO
 */
@Schema(description = "管理后台 - 问卷公开链接 Save VO")
@Data
public class QuestionnairePublicLinkSaveReqVO {

    @Schema(description = "ID，更新时必填")
    private Long id;

    @Schema(description = "发布ID")
    private Long publishId;

    @Schema(description = "问卷ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "问卷ID不能为空")
    private Long questionnaireId;

    @Schema(description = "链接标题")
    private String title;

    @Schema(description = "是否启用")
    private Integer enabled;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "最大提交数")
    private Integer maxSubmit;

    @Schema(description = "是否收集填写人信息")
    private Integer collectInfo;

    @Schema(description = "访问密码")
    private String password;

}
