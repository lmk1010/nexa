package com.kyx.service.business.api.dept.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "RPC 服务 - 部门同步创建或更新 Request DTO")
@Data
public class DeptUpsertReqDTO {

    @Schema(description = "部门编号，外部同步时使用外部部门 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1084734607")
    @NotNull(message = "部门编号不能为空")
    private Long id;

    @Schema(description = "部门名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "研发中心")
    @NotBlank(message = "部门名称不能为空")
    private String name;

    @Schema(description = "父部门ID", example = "1")
    private Long parentId;

    @Schema(description = "排序", example = "0")
    private Integer sort;

    @Schema(description = "状态：0开启，1关闭", example = "0")
    private Integer status;

    @Schema(description = "部门主管用户编号；null 表示不同步主管", example = "2048")
    private Long leaderUserId;
}
