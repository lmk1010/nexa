package com.kyx.service.hr.controller.admin.questionnaire.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问卷公开链接 Response VO
 */
@Schema(description = "管理后台 - 问卷公开链接 Response VO")
@Data
public class QuestionnairePublicLinkRespVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "发布ID")
    private Long publishId;

    @Schema(description = "问卷ID")
    private Long questionnaireId;

    @Schema(description = "问卷名称")
    private String questionnaireName;

    @Schema(description = "访问令牌")
    private String token;

    @Schema(description = "链接标题")
    private String title;

    @Schema(description = "是否启用")
    private Integer enabled;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "最大提交数")
    private Integer maxSubmit;

    @Schema(description = "已提交数")
    private Integer submitCount;

    @Schema(description = "是否收集填写人信息")
    private Integer collectInfo;

    @Schema(description = "是否有密码")
    private Boolean hasPassword;

    @Schema(description = "完整链接")
    private String fullUrl;

}
