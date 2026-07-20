package com.kyx.service.finance.dal.dataobject.init;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.*;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 账套科目 DO
 *
 * 余额增减规则（由 subjectType 决定，无需单独存储 balance_dir）：
 * - INCOME 类：金额增加 = 收入增加（正数），冲销为负数
 * - EXPENSE/COST 类：金额增加 = 支出增加（正数），冲销为负数
 *
 * @author xyang
 */
@TableName("finance_company_subject")
@KeySequence("finance_company_subject_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class FinanceCompanySubjectDO extends TenantBaseDO {

    /** 主键ID */
    @TableId
    private Long id;

    /** 来源模板科目ID（系统导入科目有值，手动新增为 null） */
    private Long templateId;

    /** 账套ID */
    private Long companyId;

    /**
     * 关联会计制度编码（不入库，仅展示用）
     */
    @TableField(exist = false)
    private String accountingSystem;

    /** 科目编码 */
    private String subjectCode;

    /** 父级科目编码 */
    private String parentCode;

    /** 科目名称 */
    private String subjectName;

    /**
     * 科目类型：
     * ASSET(资产) / LIABILITY(负债) / EQUITY(权益) /
     * COST(成本) / INCOME(收入) / EXPENSE(费用)
     */
    private String subjectType;

    /** 科目层级：1-一级，2-二级，3-三级，4-四级 */
    private Integer level;

    /** 是否末级科目（末级才能被凭证/流水引用） */
    private Boolean isLeaf;

    /** 排序号 */
    private Integer sort;

    /**
     * 期初余额（不入库，由 finance_opening_balance 表管理）
     */
    @TableField(exist = false)
    private BigDecimal openingBalance;

    /** 科目备注 */
    private String remark;

    /**
     * 费用性质（多选，逗号分隔）
     * 可选值：SALES-销售费用，MANAGEMENT-管理费用
     */
    private String feeType;

    /**
     * 往来管理开关（0-否，1-是）
     */
    private Boolean manageSwitch;

    /**
     * 经营属性（单选）：VARIABLE-变动成本/费用，FIXED-固定成本/费用
     */
    private String bizType;

    /** 状态：0-启用，1-停用 */
    private Integer status;
}
