package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 员工操作日志 Response VO")
@Data
public class EmployeeOperationLogRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "员工档案ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long profileId;

    @Schema(description = "操作类型：create/update/delete/import/export")
    private String operationType;

    @Schema(description = "操作模块：basic_info/work_info/education/contract等")
    private String operationModule;

    @Schema(description = "操作标题")
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

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
