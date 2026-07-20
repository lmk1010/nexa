package com.kyx.service.business.controller.admin.dept.vo.dept;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 部门树 Response VO（含员工数量）")
@Data
public class DeptTreeRespVO {

    @Schema(description = "部门编号", example = "1024")
    private Long id;

    @Schema(description = "部门名称", example = "研发部")
    private String name;

    @Schema(description = "父部门编号", example = "0")
    private Long parentId;

    @Schema(description = "显示顺序", example = "1")
    private Integer sort;

    @Schema(description = "负责人的用户编号", example = "2048")
    private Long leaderUserId;

    @Schema(description = "状态,见 CommonStatusEnum 枚举", example = "0")
    private Integer status;

    @Schema(description = "租户编号", example = "1")
    private Long tenantId;

    @Schema(description = "员工数量（含子部门）", example = "10")
    private Integer userCount;
}
