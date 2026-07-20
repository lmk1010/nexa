package com.kyx.service.finance.controller.admin.init.vo.subject;

import com.kyx.foundation.common.biz.tree.TreeNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 财务科目模板树节点 VO
 *
 * @author xyang
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class FinanceSubjectTemplateTreeNodeVO extends TreeNode<FinanceSubjectTemplateTreeNodeVO> {

    @Schema(description = "模板编号")
    private Long id;

    @Schema(description = "会计制度")
    private String accountingSystem;

    private Long customTenantId;

    @Schema(description = "科目编码")
    private String subjectCode;

    @Schema(description = "科目名称")
    private String subjectName;

    @Schema(description = "科目类型")
    private String subjectType;

    @Schema(description = "父级科目编码")
    private String parentCode;

    @Schema(description = "科目层级")
    private Integer level;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "费用性质（多选）")
    private String feeType;

    @Schema(description = "往来管理开关")
    private Boolean manageSwitch;

    @Schema(description = "经营属性：VARIABLE-变动，FIXED-固定")
    private String bizType;

    @Schema(description = "状态：0-启用，1-停用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
