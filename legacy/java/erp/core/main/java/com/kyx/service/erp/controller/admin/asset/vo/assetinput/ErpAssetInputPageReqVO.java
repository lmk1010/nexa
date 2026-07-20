package com.kyx.service.erp.controller.admin.asset.vo.assetinput;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 资产录入申请分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpAssetInputPageReqVO extends PageParam {

    @Schema(description = "录入申请编号", example = "AI202501001")
    private String inputNo;

    @Schema(description = "资产编码", example = "A202501001")
    private String assetNo;

    @Schema(description = "资产名称", example = "笔记本电脑")
    private String name;

    @Schema(description = "资产类型", example = "IT设备")
    private String type;

    @Schema(description = "资产分类编号", example = "1")
    private Long categoryId;

    @Schema(description = "管理部门", example = "1")
    private Long deptId;

    @Schema(description = "供应商编号", example = "1")
    private Long supplierId;

    @Schema(description = "申请状态", example = "1")
    private Integer status;

    @Schema(description = "审批状态", example = "1")
    private Integer approvalStatus;

    @Schema(description = "BMP流程状态", example = "1")
    private Integer bmpStatus;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

} 