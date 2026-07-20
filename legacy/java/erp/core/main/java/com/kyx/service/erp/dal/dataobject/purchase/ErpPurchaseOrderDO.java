package com.kyx.service.erp.dal.dataobject.purchase;

import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.kyx.service.erp.dal.dataobject.finance.ErpAccountDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Date;

/**
 * ERP 采购订单 DO
 *
 * @author MK
 */
@TableName(value = "erp_purchase_order")
@KeySequence("erp_purchase_order_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@Accessors(chain = true)
@AllArgsConstructor
public class ErpPurchaseOrderDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 采购订单号
     */
    private String no;
    /**
     * 采购状态
     *
     * 枚举 {@link com.kyx.service.erp.enums.ErpAuditStatus}
     */
    private Integer status;
    /**
     * 供应商编号
     *
     * 关联 {@link ErpSupplierDO#getId()}
     */
    private Long supplierId;
    /**
     * 结算账户编号
     *
     * 关联 {@link ErpAccountDO#getId()}
     */
    private Long accountId;
    
    // ========== 新增字段 ==========
    /**
     * 货币代码
     */
    private String currency;
    /**
     * 采购组织编码
     */
    private String purchaseOrganization;
    /**
     * 贸易条款
     */
    private String tradeTerms;
    /**
     * 付款条款
     */
    private String paymentTerms;
    /**
     * 交货地址
     */
    private String deliveryAddress;
    /**
     * 要求交货日期
     */
    private Date requiredDeliveryDate;
    /**
     * 项目编号/WBS
     */
    private String projectCode;
    /**
     * 供应商联系人
     */
    private String supplierContact;
    
    /**
     * 下单时间
     */
    private Date orderTime;

    /**
     * 合计数量
     */
    private BigDecimal totalCount;
    /**
     * 最终合计价格，单位：元
     *
     * totalPrice = totalProductPrice + totalTaxPrice - discountPrice
     */
    private BigDecimal totalPrice;

    /**
     * 合计产品价格，单位：元
     */
    private BigDecimal totalProductPrice;
    /**
     * 合计税额，单位：元
     */
    private BigDecimal totalTaxPrice;
    /**
     * 优惠率，百分比
     */
    private BigDecimal discountPercent;
    /**
     * 优惠金额，单位：元
     *
     * discountPrice = (totalProductPrice + totalTaxPrice) * discountPercent
     */
    private BigDecimal discountPrice;
    /**
     * 定金金额，单位：元
     */
    private BigDecimal depositPrice;

    /**
     * 附件地址
     */
    private String fileUrl;
    /**
     * 备注
     */
    private String remark;
    
    // ========== 新增备注字段 ==========
    /**
     * 内部备注
     */
    private String internalRemark;
    /**
     * 供应商须知
     */
    private String supplierNotice;

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

    // ========== BPM 工作流相关字段 ==========
    /**
     * BMP流程实例ID
     */
    private String processInstanceId;
    /**
     * BMP流程状态：1-流程中，2-已完成，3-已拒绝，4-已撤销
     */
    private Integer bmpStatus;

}