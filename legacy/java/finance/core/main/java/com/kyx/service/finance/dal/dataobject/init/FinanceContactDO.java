package com.kyx.service.finance.dal.dataobject.init;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
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
 * 往来信息 DO
 * @author xyang
 */
@TableName("finance_contact")
@KeySequence("finance_contact_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class FinanceContactDO extends TenantBaseDO {

    /**
     * 主键 ID
     */
    @TableId
    private Long id;

    /**
     * 往来分组ID
     */
    private Long groupId;

    /**
     * 往来名称
     */
    private String contactName;

    /**
     * 地址
     */
    private String address;

    /**
     * 账户类型（支付宝/银行卡等）
     */
    private String accountType;

    /**
     * 账户名称
     */
    private String accountName;

    /**
     * 账号
     */
    private String accountNo;

    /**
     * 姓名
     */
    private String ownerName;

    /**
     * 联系方式
     */
    private String phone;

    /**
     * 状态：0 启用，1 停用
     */
    private Integer status;

    /**
     * 分组名称（非持久化）
     */
    @TableField(exist = false)
    private String groupName;

    private String remark;
}
