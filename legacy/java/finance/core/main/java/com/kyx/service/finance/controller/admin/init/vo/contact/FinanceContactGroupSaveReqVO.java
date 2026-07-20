package com.kyx.service.finance.controller.admin.init.vo.contact;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 往来分组保存请求
 */
@Data
@Schema(description = "往来分组保存请求")
public class FinanceContactGroupSaveReqVO {

    @Schema(description = "分组ID（更新时必填）")
    private Long id;

    @Schema(description = "父级分组ID（新增时必填）")
    private Long parentId;

    @Schema(description = "分组名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "分组名称不能为空")
    private String groupName;

    @Schema(description = "排序号")
    private Integer sort;
}
