package com.kyx.service.hr.controller.admin.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 考试保存 VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 考试保存 VO")
@Data
public class ExamSaveReqVO {

    @Schema(description = "考试ID")
    private Long id;

    @Schema(description = "考试编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "考试编码不能为空")
    @Size(max = 64, message = "考试编码长度不能超过64个字符")
    private String code;

    @Schema(description = "考试名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "考试名称不能为空")
    @Size(max = 200, message = "考试名称长度不能超过200个字符")
    private String name;

    @Schema(description = "考试类型(0一次性 1定期考核)")
    private Integer examType;

    @Schema(description = "状态(0草稿 1已发布 2已关闭)")
    private Integer status;

    @Schema(description = "考试模式(fixed/bank)")
    private String examMode;

    @Schema(description = "考试时长(分钟)")
    private Integer durationMin;

    @Schema(description = "及格分")
    private Integer passScore;

    @Schema(description = "最大次数")
    private Integer maxAttempts;

    @Schema(description = "开始时间")
    private LocalDateTime startAt;

    @Schema(description = "结束时间")
    private LocalDateTime endAt;

    @Schema(description = "发布范围JSON（deptIds/roleIds/userIds）")
    private String publishScopeJson;

    @Schema(description = "试卷列表")
    @Valid
    private List<ExamPaperSaveReqVO> papers;

}
