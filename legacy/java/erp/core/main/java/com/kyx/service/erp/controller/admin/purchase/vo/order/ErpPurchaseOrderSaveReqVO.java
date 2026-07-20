package com.kyx.service.erp.controller.admin.purchase.vo.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 采购订单新增/修改 Request VO")
@Data
public class ErpPurchaseOrderSaveReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "17386")
    private Long id;

    @Schema(description = "供应商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1724")
    @NotNull(message = "供应商编号不能为空")
    private Long supplierId;

    @Schema(description = "结算账户编号", example = "31189")
    private Long accountId;
    
    // ========== 新增字段 ==========
    @Schema(description = "货币代码", example = "CNY")
    private String currency;
    
    @Schema(description = "采购组织编码", example = "HQ_PURCHASE")
    private String purchaseOrganization;
    
    @Schema(description = "贸易条款", example = "FOB")
    private String tradeTerms;
    
    @Schema(description = "付款条款", example = "NET30")
    private String paymentTerms;
    
    @Schema(description = "交货地址", example = "上海市浦东新区科技路123号")
    private String deliveryAddress;
    
    @Schema(description = "要求交货日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND, timezone = "GMT+8")
    private Date requiredDeliveryDate;
    
    @Schema(description = "项目编号/WBS", example = "PRJ001")
    private String projectCode;
    
    @Schema(description = "供应商联系人", example = "张经理 13888888888")
    private String supplierContact;

    @Schema(description = "采购时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "采购时间不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND, timezone = "GMT+8")
    private Date orderTime;

    @Schema(description = "优惠率，百分比", requiredMode = Schema.RequiredMode.REQUIRED, example = "99.88")
    private BigDecimal discountPercent;

    @Schema(description = "定金金额，单位：元", example = "7127")
    private BigDecimal depositPrice;

    @Schema(description = "附件地址", example = "https://www.iocoder.cn")
    private String fileUrl;

    @Schema(description = "备注", example = "你猜")
    private String remark;
    
    // ========== 新增备注字段 ==========
    @Schema(description = "内部备注", example = "内部处理说明")
    private String internalRemark;
    
    @Schema(description = "供应商须知", example = "交货时需要提供相关资质证明")
    private String supplierNotice;

    @Schema(description = "订单清单列表")
    private List<Item> items;

    @Schema(description = "BPM - 发起人自选审批人")
    private Map<String, List<Long>> startUserSelectAssignees;

    @Data
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
        @NotNull(message = "产品编号不能为空")
        private Long productId;

        @Schema(description = "产品单位编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3113")
        @NotNull(message = "产品单位编号不能为空")
        private Long productUnitId;
        
        @Schema(description = "计量单位名称", example = "台")
        private String unitName;

        @Schema(description = "产品单价", example = "100.00")
        @NotNull(message = "产品单价不能为空")
        private BigDecimal productPrice;
        
        @Schema(description = "折扣百分比", example = "5.00")
        private BigDecimal discountPercent;

        @Schema(description = "产品数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
        @NotNull(message = "产品数量不能为空")
        private BigDecimal count;

        @Schema(description = "税率，百分比", example = "99.88")
        private BigDecimal taxPercent;
        
        // ========== 新增字段 ==========
        @Schema(description = "需求日期", example = "2024-12-31")
        @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
        @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND, timezone = "GMT+8")
        private Date requiredDate;
        
        @Schema(description = "交货地址", example = "北京市朝阳区建国路456号")
        private String deliveryAddress;
        
        @Schema(description = "关联采购申请行", example = "[\"PR001-1\", \"PR001-2\"]")
        private String relatedPrLines;

        @Schema(description = "备注", example = "随便")
        private String remark;

    }

}