package com.kyx.service.erp.controller.admin.asset.vo.lost;

import com.kyx.foundation.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

/**
 * ERP 资产挂失分页 Request VO
 *
 * @author kyx
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpAssetLostPageReqVO extends PageParam {

    /**
     * 挂失编号
     */
    private String lostNo;

    /**
     * 资产编号
     */
    private String assetNo;

    /**
     * 资产名称
     */
    private String assetName;

    /**
     * 挂失原因
     */
    private String lostReason;

    /**
     * 挂失地点
     */
    private String lostLocation;

    /**
     * 挂失状态
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