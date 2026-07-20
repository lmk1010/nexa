package com.kyx.service.erp.dal.dataobject.purchase;

import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 采购申请明细 DO
 *
 * @author MK
 */
@TableName("erp_purchase_request_item")
@KeySequence("erp_purchase_request_item_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpPurchaseRequestItemDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 采购申请ID
     */
    private Long requestId;
    /**
     * 产品编号
     */
    private Long productId;
    /**
     * 产品单位编号
     */
    private Long productUnitId;
    /**
     * 产品单价
     */
    private BigDecimal productPrice;
    /**
     * 数量
     */
    private BigDecimal count;
    /**
     * 总价，单价 * 数量
     */
    private BigDecimal totalPrice;
    /**
     * 备注
     */
    private String remark;

} 