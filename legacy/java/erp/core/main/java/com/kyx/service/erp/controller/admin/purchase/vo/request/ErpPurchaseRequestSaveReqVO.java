package com.kyx.service.erp.controller.admin.purchase.vo.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 采购申请新增/修改 Request VO")
@Data
public class ErpPurchaseRequestSaveReqVO {

    @Schema(description = "申请编号", example = "1")
    private Long id;

    @Schema(description = "申请标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "办公用品采购申请")
    @NotBlank(message = "申请标题不能为空")
    private String title;

    @Schema(description = "申请人", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotBlank(message = "申请人不能为空")
    private String applicant;

    @Schema(description = "申请部门", requiredMode = Schema.RequiredMode.REQUIRED, example = "行政部")
    @NotBlank(message = "申请部门不能为空")
    private String department;

    @Schema(description = "联系电话", example = "13812345678")
    private String contactPhone;

    @Schema(description = "预算科目", example = "办公用品采购")
    private String budgetAccount;

    @Schema(description = "申请日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "申请日期不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private Date applyDate;

    @Schema(description = "需求日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "需求日期不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private Date requiredDate;

    @Schema(description = "紧急程度：1-低，2-一般，3-高，4-紧急", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "紧急程度不能为空")
    private Integer urgentLevel;

    @Schema(description = "申请原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "办公室打印机墨盒用完，需要采购")
    @NotBlank(message = "申请原因不能为空")
    private String reason;

    @Schema(description = "状态：0-草稿，1-待审核，2-已通过，3-已拒绝，4-已取消", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "总金额", example = "1000.00")
    private BigDecimal totalAmount;

    @Schema(description = "备注", example = "请尽快处理")
    private String remark;

    @Schema(description = "BMP流程状态：1-流程中，2-已完成，3-已拒绝，4-已撤销", example = "1")
    private Integer bmpStatus;

    @Schema(description = "申请明细")
    private List<Item> items;

    @Schema(description = "BPM - 发起人自选审批人")
    private Map<String, List<Long>> startUserSelectAssignees;

    @Schema(description = "ERP采购申请明细 Base VO，提供给添加、修改、详细的子 VO 使用")
    @Data
    public static class Item {

        @Schema(description = "编号", example = "1")
        private Long id;

        @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        @NotNull(message = "产品编号不能为空")
        private Long productId;

        @Schema(description = "产品单位编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        @NotNull(message = "产品单位编号不能为空")
        private Long productUnitId;

        @Schema(description = "产品单价", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
        @NotNull(message = "产品单价不能为空")
        private BigDecimal productPrice;

        @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "10.00")
        @NotNull(message = "数量不能为空")
        private BigDecimal count;

        @Schema(description = "总价，单价 * 数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.00")
        @NotNull(message = "总价不能为空")
        private BigDecimal totalPrice;

        @Schema(description = "备注", example = "请选择优质产品")
        private String remark;

    }

} 