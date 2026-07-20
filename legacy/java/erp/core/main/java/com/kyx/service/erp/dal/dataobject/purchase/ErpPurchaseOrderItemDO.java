package com.kyx.service.erp.dal.dataobject.purchase;

import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.kyx.service.erp.dal.dataobject.product.ErpProductDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Date;

/**
 * ERP 采购订单项 DO
 *
 * @author MK
 */
@TableName("erp_purchase_order_items")
@KeySequence("erp_purchase_order_items_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@Accessors(chain = true)
@AllArgsConstructor
public class ErpPurchaseOrderItemDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 采购订单编号
     *
     * 关联 {@link ErpPurchaseOrderDO#getId()}
     */
    private Long orderId;
    
    // ========== 新增字段 ==========
    /**
     * 行号
     */
    private Integer lineNumber;
    /**
     * 物料编码
     */
    private String materialCode;
    /**
     * 物料描述
     */
    private String materialName;
    /**
     * 规格型号
     */
    private String specification;
    
    /**
     * 产品编号
     *
     * 关联 {@link ErpProductDO#getId()}
     */
    private Long productId;
    /**
     * 产品单位单位
     *
     * 冗余 {@link ErpProductDO#getUnitId()}
     */
    private Long productUnitId;
    /**
     * 计量单位名称（冗余存储，便于显示）
     */
    private String unitName;

    /**
     * 产品单位单价，单位：元
     */
    private BigDecimal productPrice;
    /**
     * 折扣百分比
     */
    private BigDecimal discountPercent;
    /**
     * 数量
     */
    private BigDecimal count;
    /**
     * 总价，单位：元
     *
     * totalPrice = productPrice * count
     */
    private BigDecimal totalPrice;
    /**
     * 税率，百分比
     */
    private BigDecimal taxPercent;
    /**
     * 税额，单位：元
     *
     * taxPrice = totalPrice * taxPercent
     */
    private BigDecimal taxPrice;
    
    // ========== 新增字段 ==========
    /**
     * 需求日期
     */
    private Date requiredDate;
    /**
     * 交货地址
     */
    private String deliveryAddress;
    /**
     * 关联采购申请行(JSON格式)
     */
    private String relatedPrLines;

    /**
     * 备注
     */
    private String remark;

    // ========== 采购入库 ==========
    /**
     * 采购入库数量
     */
    private BigDecimal inCount;

    // ========== 采购退货（出库）） ==========
    /**
     * 采购退货数量
     */
    private BigDecimal returnCount;

}