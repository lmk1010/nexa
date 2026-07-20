package com.kyx.service.finance.dal.dataobject.init;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import lombok.*;
import lombok.experimental.Accessors;

/**
 * 科目模板 DO
 *
 * @author xyang
 */
@TableName("finance_subject_template")
@KeySequence("finance_subject_template_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class FinanceSubjectTemplateDO extends BaseDO {

    /** 模板主键ID */
    @TableId
    private Long id;

    /** 关联会计制度编码 */
    private String accountingSystem;

    /** 专属租户ID（0=系统模板） */
    private Long customTenantId;

    /** 科目编码 */
    private String subjectCode;

    /** 科目名称 */
    private String subjectName;

    /**
     * 科目类型：ASSET(资产) / LIABILITY(负债) / EQUITY(权益) /
     * COST(成本) / INCOME(收入) / EXPENSE(费用)
     */
    private String subjectType;

    /** 科目层级：1-一级，2-二级，3-三级，4-四级 */
    private Integer level;

    /** 父级科目编码 */
    private String parentCode;

    /** 科目排序号 */
    private Integer sort;

    /** 模板/科目备注 */
    private String remark;

    /**
     * 费用性质（多选，逗号分隔）
     * 可选值：SALES-销售费用，MANAGEMENT-管理费用
     * 仅 EXPENSE/COST 类科目有意义
     */
    private String feeType;

    /**
     * 往来管理开关（0-否，1-是）
     * 启用后该科目需关联往来单位
     */
    private Boolean manageSwitch;

    /**
     * 经营属性（单选）
     * 可选值：VARIABLE-变动成本/费用，FIXED-固定成本/费用
     * 用于利润表费用分类统计
     */
    private String bizType;

    /** 状态：0(启用), 1(停用) */
    private Integer status;
}
