package com.kyx.service.erp.controller.admin.purchase.vo.order;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 采购订单 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpPurchaseOrderRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "17386")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "采购单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "XS001")
    @ExcelProperty("采购单编号")
    private String no;

    @Schema(description = "采购状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @ExcelProperty("采购状态")
    private Integer status;

    @Schema(description = "供应商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1724")
    private Long supplierId;
    @Schema(description = "供应商名称", example = "芋道")
    @ExcelProperty("供应商名称")
    private String supplierName;

    @Schema(description = "结算账户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "311.89")
    @ExcelProperty("结算账户编号")
    private Long accountId;
    
    // ========== 新增字段 ==========
    @Schema(description = "货币代码", example = "CNY")
    @ExcelProperty("货币代码")
    private String currency;
    
    @Schema(description = "采购组织编码", example = "HQ_PURCHASE")
    @ExcelProperty("采购组织")
    private String purchaseOrganization;
    
    @Schema(description = "贸易条款", example = "FOB")
    @ExcelProperty("贸易条款")
    private String tradeTerms;
    
    @Schema(description = "付款条款", example = "NET30")
    @ExcelProperty("付款条款")
    private String paymentTerms;
    
    @Schema(description = "交货地址", example = "上海市浦东新区科技路123号")
    @ExcelProperty("交货地址")
    private String deliveryAddress;
    
    @Schema(description = "要求交货日期")
    @ExcelProperty("要求交货日期")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND, timezone = "GMT+8")
    private Date requiredDeliveryDate;
    
    @Schema(description = "项目编号/WBS", example = "PRJ001")
    @ExcelProperty("项目编号")
    private String projectCode;
    
    @Schema(description = "供应商联系人", example = "张经理 13888888888")
    @ExcelProperty("供应商联系人")
    private String supplierContact;

    @Schema(description = "采购时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("采购时间")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND, timezone = "GMT+8")
    private Date orderTime;

    @Schema(description = "合计数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "15663")
    @ExcelProperty("合计数量")
    private BigDecimal totalCount;
    @Schema(description = "最终合计价格", requiredMode = Schema.RequiredMode.REQUIRED, example = "24906")
    @ExcelProperty("最终合计价格")
    private BigDecimal totalPrice;

    @Schema(description = "合计产品价格，单位：元", requiredMode = Schema.RequiredMode.REQUIRED, example = "7127")
    private BigDecimal totalProductPrice;

    @Schema(description = "合计税额，单位：元", requiredMode = Schema.RequiredMode.REQUIRED, example = "7127")
    private BigDecimal totalTaxPrice;

    @Schema(description = "优惠率，百分比", requiredMode = Schema.RequiredMode.REQUIRED, example = "99.88")
    private BigDecimal discountPercent;

    @Schema(description = "优惠金额，单位：元", requiredMode = Schema.RequiredMode.REQUIRED, example = "7127")
    private BigDecimal discountPrice;

    @Schema(description = "定金金额，单位：元", requiredMode = Schema.RequiredMode.REQUIRED, example = "7127")
    private BigDecimal depositPrice;

    @Schema(description = "附件地址", example = "https://www.iocoder.cn")
    @ExcelProperty("附件地址")
    private String fileUrl;

    @Schema(description = "备注", example = "你猜")
    @ExcelProperty("备注")
    private String remark;
    
    // ========== 新增备注字段 ==========
    @Schema(description = "内部备注", example = "内部处理说明")
    @ExcelProperty("内部备注")
    private String internalRemark;
    
    @Schema(description = "供应商须知", example = "交货时需要提供相关资质证明")
    @ExcelProperty("供应商须知")
    private String supplierNotice;

    @Schema(description = "创建人", example = "芋道")
    private String creator;
    @Schema(description = "创建人名称", example = "芋道")
    private String creatorName;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND, timezone = "GMT+8")
    private Date createTime;

    @Schema(description = "订单项列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Item> items;

    @Schema(description = "产品信息", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("产品信息")
    private String productNames;

    // ========== 采购入库 ==========

    @Schema(description = "采购入库数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    private BigDecimal inCount;

    // ========== 采购退货（出库）） ==========

    @Schema(description = "采购退货数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    private BigDecimal returnCount;

    @Data
    @Accessors(chain = true)
    public static class Item {

        @Schema(description = "订单项编号", example = "11756")
        private Long id;
        
        // ========== 新增字段 ==========
        @Schema(description = "行号", example = "1")
        private Integer lineNumber;
        
        @Schema(description = "物料编码", example = "MAT001")
        private String materialCode;
        
        @Schema(description = "物料描述", example = "办公桌")
        private String materialName;
        
        @Schema(description = "规格型号", example = "1.2m*0.8m*0.75m")
        private String specification;

        @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3113")
        private Long productId;

        @Schema(description = "产品单位单位", requiredMode = Schema.RequiredMode.REQUIRED, example = "3113")
        private Long productUnitId;
        
        @Schema(description = "计量单位名称", example = "台")
        private String unitName;

        @Schema(description = "产品单价", example = "100.00")
        private BigDecimal productPrice;
        
        @Schema(description = "折扣百分比", example = "5.00")
        private BigDecimal discountPercent;

        @Schema(description = "产品数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
        @NotNull(message = "产品数量不能为空")
        private BigDecimal count;

        @Schema(description = "税率，百分比", example = "99.88")
        private BigDecimal taxPercent;

        @Schema(description = "税额，单位：元", example = "100.00")
        private BigDecimal taxPrice;
        
        // ========== 新增字段 ==========
        @Schema(description = "需求日期", example = "2024-12-31")
        @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND, timezone = "GMT+8")
        private Date requiredDate;
        
        @Schema(description = "交货地址", example = "北京市朝阳区建国路456号")
        private String deliveryAddress;
        
        @Schema(description = "关联采购申请行", example = "[\"PR001-1\", \"PR001-2\"]")
        private String relatedPrLines;

        @Schema(description = "备注", example = "随便")
        private String remark;

        // ========== 采购入库 ==========

        @Schema(description = "采购入库数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
        private BigDecimal inCount;

        // ========== 采购退货（入库）） ==========

        @Schema(description = "采购退货数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
        private BigDecimal returnCount;

        // ========== 关联字段 ==========

        @Schema(description = "产品名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "巧克力")
        private String productName;
        @Schema(description = "产品条码", requiredMode = Schema.RequiredMode.REQUIRED, example = "A9985")
        private String productBarCode;
        @Schema(description = "产品单位名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "盒")
        private String productUnitName;

        @Schema(description = "库存数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
        private BigDecimal stockCount; // 该字段仅仅在"详情"和"编辑"时使用

    }

}