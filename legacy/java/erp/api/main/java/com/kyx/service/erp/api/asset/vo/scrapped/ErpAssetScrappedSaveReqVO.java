package com.kyx.service.erp.api.asset.vo.scrapped;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

/**
 * ERP 资产报废保存 Request VO
 *
 * @author kyx
 */
@Data
public class ErpAssetScrappedSaveReqVO {

    private Long id;

    /**
     * 资产编号
     */
    @NotNull(message = "资产编号不能为空")
    private Long assetId;

    /**
     * 报废原因
     */
    @NotBlank(message = "报废原因不能为空")
    private String scrappedReason;

    /**
     * 报废类型
     */
    @NotBlank(message = "报废类型不能为空")
    private String scrappedType;

    /**
     * 报废日期
     */
    @NotNull(message = "报废日期不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate scrappedDate;

    /**
     * 处理人编号
     */
    @NotNull(message = "处理人不能为空")
    private Long handleUserId;

    /**
     * 处理部门编号
     */
    @NotNull(message = "处理部门不能为空")
    private Long handleDeptId;

    /**
     * 预估价值（元）
     */
    private BigDecimal estimatedValue;

    /**
     * 实际价值（元）
     */
    private BigDecimal actualValue;

    /**
     * 报废说明
     */
    @NotBlank(message = "报废说明不能为空")
    private String scrappedDescription;

    /**
     * 实际处理日期
     */
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate processingDate;

    /**
     * 处理方式
     */
    private String processingMethod;

    /**
     * 处置收入（元）
     */
    private BigDecimal disposalRevenue;

    /**
     * 备注
     */
    private String remark;

    /**
     * 附件文件ID列表 (OP服务返回的文件ID)
     */
    private List<String> fileIds;
} 