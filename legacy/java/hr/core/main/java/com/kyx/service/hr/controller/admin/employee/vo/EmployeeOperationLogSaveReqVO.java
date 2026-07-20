package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 员工操作日志新增 Request VO")
@Data
public class EmployeeOperationLogSaveReqVO {

    @Schema(description = "员工档案ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "员工档案ID不能为空")
    private Long profileId;

    @Schema(description = "操作类型：create/update/delete/import/export", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "操作类型不能为空")
    private String operationType;

    @Schema(description = "操作模块：basic_info/work_info/education/contract等", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "操作模块不能为空")
    private String operationModule;

    @Schema(description = "操作标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "操作标题不能为空")
    private String operationTitle;

    @Schema(description = "操作内容描述")
    private String operationContent;

    @Schema(description = "变更前数据(JSON)")
    private String beforeData;

    @Schema(description = "变更后数据(JSON)")
    private String afterData;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "操作人姓名")
    private String operatorName;

    @Schema(description = "操作时间")
    private LocalDateTime operationTime;

    @Schema(description = "操作IP")
    private String operationIp;

    @Schema(description = "操作来源：web/app/api/import")
    private String operationSource;

}
