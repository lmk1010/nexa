package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.time.LocalDate;

@Schema(description = "Admin - Employee document request apply Request VO")
@Data
public class EmployeeDocumentRequestApplyReqVO {

    @Schema(description = "Profile id, optional for self apply")
    private Long profileId;

    @Schema(description = "User id, optional for self apply")
    private Long userId;

    @Schema(description = "Request type")
    @NotBlank(message = "证明类型不能为空")
    private String requestType;

    @Schema(description = "Title")
    private String title;

    @Schema(description = "Purpose")
    private String purpose;

    @Schema(description = "Expected date")
    private LocalDate expectedDate;

    @Schema(description = "Delivery mode")
    private String deliveryMode;

    @Schema(description = "Contact info")
    private String contactInfo;

    @Schema(description = "Attachment json")
    private String attachmentJson;

}
