package com.kyx.service.erp.api.asset.vo.lost;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

/**
 * ERP 资产挂失保存 Request VO
 *
 * @author kyx
 */
@Data
public class ErpAssetLostSaveReqVO {

    private Long id;

    /**
     * 资产编号
     */
    @NotNull(message = "资产编号不能为空")
    private Long assetId;

    /**
     * 挂失原因
     */
    @NotBlank(message = "挂失原因不能为空")
    private String lostReason;

    /**
     * 发现挂失日期
     */
    @NotNull(message = "发现挂失日期不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate lostDate;

    /**
     * 预计找回日期
     */
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate foundDate;

    /**
     * 挂失地点
     */
    @NotBlank(message = "挂失地点不能为空")
    private String lostLocation;

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
     * 挂失说明
     */
    @NotBlank(message = "挂失说明不能为空")
    private String lostDescription;

    /**
     * 实际找回时间
     */
    private LocalDateTime foundTime;

    /**
     * 找回地点
     */
    private String findLocation;

    /**
     * 找到人编号
     */
    private Long finderUserId;

    /**
     * 找回说明
     */
    private String findDescription;

    /**
     * 备注
     */
    private String remark;

    /**
     * 附件文件ID列表 (OP服务返回的文件ID)
     */
    private List<String> fileIds;
} 