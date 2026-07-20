package com.kyx.service.erp.dal.dataobject.asset;

import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ERP 资产报废记录 DO
 * 
 * 业务说明：
 * - 资产报废流程：申请 -> 审批 -> 实际处理 -> 完成
 * - 支持BMP工作流审批
 * - 包含价值评估和处置收入记录
 *
 * @author kyx
 */
@TableName("erp_asset_scrapped")
@KeySequence("erp_asset_scrapped_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpAssetScrappedDO extends BaseDO {

    /**
     * 报废记录编号
     */
    @TableId
    private Long id;
    
    /**
     * 报废编号
     */
    private String scrappedNo;
    
    /**
     * 资产编号
     */
    private Long assetId;
    
    /**
     * 报废原因
     */
    private String scrappedReason;
    
    /**
     * 报废类型
     */
    private String scrappedType;
    
    /**
     * 报废日期
     */
    private LocalDate scrappedDate;
    
    /**
     * 处理人编号
     */
    private Long handleUserId;
    
    /**
     * 处理部门编号
     */
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
    private String scrappedDescription;
    
    /**
     * 报废状态：0-申请中，1-审批中，2-审批通过，3-审批拒绝，4-已完成
     */
    private Integer status;
    
    /**
     * 审批状态：1-待审批，2-审批通过，3-审批拒绝
     */
    private Integer approvalStatus;
    
    /**
     * 审批人编号
     */
    private Long approverUserId;
    
    /**
     * 审批时间
     */
    private LocalDateTime approvalTime;
    
    /**
     * 审批备注
     */
    private String approvalRemark;
    
    /**
     * BMP状态：1-流程中，2-已完成，3-已取消
     */
    private Integer bmpStatus;
    
    /**
     * BMP流程实例编号
     */
    private String bmpProcessInstanceId;
    
    /**
     * 实际处理日期
     */
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
} 