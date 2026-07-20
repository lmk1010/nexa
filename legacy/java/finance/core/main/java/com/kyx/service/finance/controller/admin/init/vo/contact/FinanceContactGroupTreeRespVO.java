package com.kyx.service.finance.controller.admin.init.vo.contact;

import com.kyx.foundation.common.biz.tree.TreeNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 往来分组树节点
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "往来分组树节点")
public class FinanceContactGroupTreeRespVO extends TreeNode<FinanceContactGroupTreeRespVO> {

    @Schema(description = "分组ID")
    private Long id;

    @Schema(description = "父级分组ID")
    private Long parentId;

    @Schema(description = "祖级分组ID链，逗号分隔，顶级为0")
    private String ancestors;

    @Schema(description = "分组名称")
    private String groupName;

    @Schema(description = "层级")
    private Integer level;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "状态：0 启用，1 停用")
    private Integer status;

    @Schema(description = "是否一级固定分组")
    private Boolean levelFixed;

    @Schema(description = "是否可编辑")
    private Boolean editable;
}
