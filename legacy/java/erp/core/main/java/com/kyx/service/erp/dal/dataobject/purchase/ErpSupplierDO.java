package com.kyx.service.erp.dal.dataobject.purchase;

import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 供应商 DO
 *
 * @author MK
 */
@TableName("erp_supplier")
@KeySequence("erp_supplier_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpSupplierDO extends BaseDO {

    /**
     * 供应商编号
     */
    @TableId
    private Long id;
    /**
     * 供应商名称
     */
    private String name;
    /**
     * 联系人
     */
    private String contact;
    /**
     * 手机号码
     */
    private String mobile;
    /**
     * 联系电话
     */
    private String telephone;
    /**
     * 电子邮箱
     */
    private String email;
    /**
     * 备注
     */
    private String remark;
    /**
     * 开启状态
     *
     * 枚举 {@link com.kyx.foundation.common.enums.CommonStatusEnum}
     */
    private Integer status;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 纳税人识别号
     */
    private String taxNo;
    /**
     * 税率
     */
    private BigDecimal taxPercent;
    /**
     * 付款方式配置（JSON格式）
     */
    private String paymentMethods;
    /**
     * 常用收货地点配置（JSON格式）
     */
    private String deliveryAddresses;

    // ========== 忽略数据库中已废弃的字段，避免MyBatis-Plus查询错误 ==========
    
    /**
     * 传真字段（已废弃，仅用于向下兼容）
     */
    @TableField(exist = false)
    private String fax;
    
    /**
     * 开户行字段（已废弃，仅用于向下兼容）
     */
    @TableField(exist = false)
    private String bankName;
    
    /**
     * 开户账号字段（已废弃，仅用于向下兼容）
     */
    @TableField(exist = false)
    private String bankAccount;
    
    /**
     * 开户地址字段（已废弃，仅用于向下兼容）
     */
    @TableField(exist = false)
    private String bankAddress;

}