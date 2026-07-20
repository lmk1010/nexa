package com.kyx.service.finance.controller.admin.init.vo.company;

import com.kyx.foundation.common.biz.tree.TreeNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账套科目响应 VO
 *
 * @author xyang
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "账套科目")
public class FinanceCompanySubjectRespVO extends TreeNode<FinanceCompanySubjectRespVO> {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "来源模板科目ID")
    private Long templateId;

    @Schema(description = "账套ID")
    private Long companyId;

    @Schema(description = "会计制度编码")
    private String accountingSystem;

    @Schema(description = "科目编码")
    private String subjectCode;

    @Schema(description = "父级科目编码")
    private String parentCode;

    @Schema(description = "科目名称")
    private String subjectName;

    @Schema(description = "科目类型")
    private String subjectType;

    @Schema(description = "科目层级")
    private Integer level;

    @Schema(description = "是否末级")
    private Boolean isLeaf;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "期初余额")
    private BigDecimal openingBalance;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "费用性质（多选）")
    private String feeType;

    @Schema(description = "往来管理开关")
    private Boolean manageSwitch;

    @Schema(description = "经营属性")
    private String bizType;

    @Schema(description = "状态：0-启用，1-停用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
