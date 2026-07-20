package com.kyx.service.finance.dal.dataobject.init;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 往来分组 DO
 */
@TableName("finance_contact_group")
@KeySequence("finance_contact_group_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class FinanceContactGroupDO extends TenantBaseDO {

    @TableId
    private Long id;

    /**
     * 分组名称
     */
    private String groupName;

    /**
     * 父级分组ID，0 表示顶级
     */
    private Long parentId;

    /**
     * 祖级分组ID链，逗号分隔，顶级为0
     */
    private String ancestors;

    /**
     * 层级，顶级为 1
     */
    private Integer level;

    /**
     * 排序号
     */
    private Integer sort;

    /**
     * 是否一级固定分组
     */
    private Boolean levelFixed;

    /**
     * 是否可编辑
     */
    private Boolean editable;

    /**
     * 状态：0 启用，1 停用
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;
}
