package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 员工附件 Response VO")
@Data
public class EmployeeAttachmentRespVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "员工档案ID")
    private Long profileId;

    @Schema(description = "附件类型")
    private String attachmentType;

    @Schema(description = "附件名称")
    private String attachmentName;

    @Schema(description = "文件地址")
    private String fileUrl;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "文件类型")
    private String fileType;

    @Schema(description = "备注")
    private String remark;
}
