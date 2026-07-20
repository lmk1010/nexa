package com.kyx.service.erp.controller.admin.asset.vo.scrapped;

import com.kyx.foundation.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

/**
 * ERP 资产报废分页 Request VO
 *
 * @author kyx
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpAssetScrappedPageReqVO extends PageParam {

    /**
     * 报废编号
     */
    private String scrappedNo;

    /**
     * 资产编号
     */
    private String assetNo;

    /**
     * 资产名称
     */
    private String assetName;

    /**
     * 报废原因
     */
    private String scrappedReason;

    /**
     * 报废类型
     */
    private String scrappedType;

    /**
     * 报废状态
     */
    private Integer status;

    /**
     * 审批状态
     */
    private Integer approvalStatus;

    /**
     * 处理人编号
     */
    private Long handleUserId;

    /**
     * 处理部门编号
     */
    private Long handleDeptId;

    /**
     * 开始日期
     */
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate startDate;

    /**
     * 结束日期
     */
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate endDate;
} 