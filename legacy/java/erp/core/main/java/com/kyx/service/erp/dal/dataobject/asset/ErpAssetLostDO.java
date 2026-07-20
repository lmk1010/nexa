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
 * ERP 资产挂失记录 DO
 * 
 * 业务说明：
 * - 资产挂失流程：申请 -> 审批 -> 处理（找回/确认丢失） -> 完成
 * - 支持BMP工作流审批
 * - 包含找回信息记录
 *
 * @author kyx
 */
@TableName("erp_asset_lost")
@KeySequence("erp_asset_lost_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpAssetLostDO extends BaseDO {

    /**
     * 挂失记录编号
     */
    @TableId
    private Long id;
    
    /**
     * 挂失编号
     */
    private String lostNo;
    
    /**
     * 资产编号
     */
    private Long assetId;
    
    /**
     * 挂失原因
     */
    private String lostReason;
    
    /**
     * 发现挂失日期
     */
    private LocalDate lostDate;
    
    /**
     * 预计找回日期
     */
    private LocalDate foundDate;
    
    /**
     * 挂失地点
     */
    private String lostLocation;
    
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
     * 挂失说明
     */
    private String lostDescription;
    
    /**
     * 挂失状态：0-申请中，1-审批中，2-审批通过，3-审批拒绝，4-已找回，5-确认丢失
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
} 