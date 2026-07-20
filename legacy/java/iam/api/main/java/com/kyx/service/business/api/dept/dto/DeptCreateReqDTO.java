package com.kyx.service.business.api.dept.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Schema(description = "RPC 服务 - 部门创建 Request DTO")
@Data
public class DeptCreateReqDTO {

    @Schema(description = "部门名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "研发中心")
    @NotBlank(message = "部门名称不能为空")
    private String name;

    @Schema(description = "父部门ID", example = "1")
    private Long parentId;

    @Schema(description = "排序", example = "0")
    private Integer sort;

    @Schema(description = "状态：0开启，1关闭", example = "0")
    private Integer status;
}
