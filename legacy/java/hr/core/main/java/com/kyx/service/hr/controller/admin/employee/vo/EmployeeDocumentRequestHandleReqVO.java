package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "Admin - Employee document request handle Request VO")
@Data
public class EmployeeDocumentRequestHandleReqVO {

    @Schema(description = "Request id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "申请ID不能为空")
    private Long id;

    @Schema(description = "Action: PROCESS/COMPLETE/REJECT", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "处理动作不能为空")
    private String action;

    @Schema(description = "Handle remark")
    private String handleRemark;

    @Schema(description = "Result file url")
    private String resultFileUrl;

    @Schema(description = "Result file name")
    private String resultFileName;

}
