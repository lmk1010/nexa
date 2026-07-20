package com.kyx.service.business.api.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Schema(description = "RPC - 通知公告创建 Request DTO")
@Data
public class NoticeCreateReqDTO {

    @Schema(description = "公告标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "HR 问卷通知")
    @NotBlank(message = "公告标题不能为空")
    @Size(max = 50, message = "公告标题不能超过50个字符")
    private String title;

    @Schema(description = "公告类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "公告类型不能为空")
    private Integer type;

    @Schema(description = "公告内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "请尽快完成问卷")
    private String content;

    @Schema(description = "状态，参见 CommonStatusEnum", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer status;

    @Schema(description = "接收范围：ALL 全员，USER 指定人员", example = "USER")
    private String receiverType;

    @Schema(description = "接收用户编号列表")
    private List<Long> receiverUserIds;

}
