package com.kyx.service.erp.controller.admin.purchase.vo.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kyx.foundation.excel.core.annotations.DictFormat;
import com.kyx.foundation.excel.core.convert.DictConvert;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 采购申请 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpPurchaseRequestRespVO {

    @Schema(description = "申请编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("申请编号")
    private Long id;

    @Schema(description = "申请单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "PR202412010001")
    @ExcelProperty("申请单号")
    private String requestNo;

    @Schema(description = "申请标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "办公用品采购申请")
    @ExcelProperty("申请标题")
    private String title;

    @Schema(description = "申请人", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @ExcelProperty("申请人")
    private String applicant;

    @Schema(description = "申请部门", requiredMode = Schema.RequiredMode.REQUIRED, example = "行政部")
    @ExcelProperty("申请部门")
    private String department;

    @Schema(description = "联系电话", example = "13812345678")
    @ExcelProperty("联系电话")
    private String contactPhone;

    @Schema(description = "预算科目", example = "办公用品采购")
    @ExcelProperty("预算科目")
    private String budgetAccount;

    @Schema(description = "申请日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("申请日期")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private Date applyDate;

    @Schema(description = "需求日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("需求日期")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private Date requiredDate;

    @Schema(description = "紧急程度：1-低，2-一般，3-高，4-紧急", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @ExcelProperty(value = "紧急程度", converter = DictConvert.class)
    @DictFormat("erp_purchase_request_urgent_level")
    private Integer urgentLevel;

    @Schema(description = "申请原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "办公室打印机墨盒用完，需要采购")
    @ExcelProperty("申请原因")
    private String reason;

    @Schema(description = "状态：0-草稿，1-待审核，2-已通过，3-已拒绝，4-已取消", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat("erp_purchase_request_status")
    private Integer status;

    @Schema(description = "总金额", example = "1000.00")
    @ExcelProperty("总金额")
    private BigDecimal totalAmount;

    @Schema(description = "备注", example = "请尽快处理")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "BPM流程实例ID", example = "12345")
    private String processInstanceId;

    @Schema(description = "BMP流程状态：1-流程中，2-已完成，3-已拒绝，4-已撤销", example = "1")
    @ExcelProperty("BMP流程状态")
    private Integer bmpStatus;

    @Schema(description = "审核意见", example = "同意申请")
    @ExcelProperty("审核意见")
    private String auditReason;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private Date createTime;

    @Schema(description = "申请明细")
    private List<Item> items;

    @Schema(description = "ERP采购申请明细 Base VO，提供给添加、修改、详细的子 VO 使用")
    @Data
    public static class Item {

        @Schema(description = "编号", example = "1")
        private Long id;

        @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        private Long productId;

        @Schema(description = "产品单位编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        private Long productUnitId;

        @Schema(description = "产品单价", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
        private BigDecimal productPrice;

        @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "10.00")
        private BigDecimal count;

        @Schema(description = "总价，单价 * 数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.00")
        private BigDecimal totalPrice;

        @Schema(description = "备注", example = "请选择优质产品")
        private String remark;

    }

} 