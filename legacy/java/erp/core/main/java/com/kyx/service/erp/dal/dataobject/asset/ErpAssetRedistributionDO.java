package com.kyx.service.erp.dal.dataobject.asset;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ERP 资产调拨记录 DO
 *
 * @author kyx
 */
@TableName("erp_asset_redistribution")
@KeySequence("erp_asset_redistribution_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpAssetRedistributionDO extends BaseDO {

    /**
     * 调拨记录ID
     */
    @TableId
    private Long id;
    
    /**
     * 调拨编号
     */
    private String redistributionNo;
    
    /**
     * 调拨前部门ID
     */
    private Long fromDeptId;
    
    /**
     * 调拨到部门ID
     */
    private Long toDeptId;
    
    /**
     * 调拨前位置
     */
    private String fromLocation;
    
    /**
     * 调拨到位置
     */
    private String toLocation;
    
    /**
     * 调拨日期
     */
    private LocalDateTime allocationDate;
    
    /**
     * 调拨原因
     */
    private String allocationReason;
    
    /**
     * 调拨备注
     */
    private String remark;
    
    /**
     * 调拨状态
     * 0-申请中，1-已完成，2-已拒绝，3-已撤销
     */
    private Integer status;
    
    /**
     * 审批人用户ID
     */
    private Long approverUserId;
    
    /**
     * 审批时间
     */
    private LocalDateTime approvalTime;
    
    /**
     * 审批状态
     * 1-待审批，2-审批通过，3-审批拒绝
     */
    private Integer approvalStatus;
    
    /**
     * 审批备注
     */
    private String approvalRemark;
    
    /**
     * BPM流程状态
     * 1-流程中，2-已完成，3-已拒绝，4-已撤销
     */
    private Integer bmpStatus;
    
    /**
     * BPM流程实例ID
     */
    private String processInstanceId;
    
    /**
     * 确认接收时间
     */
    private LocalDateTime confirmTime;
    
    /**
     * 确认接收备注
     */
    private String confirmRemark;
} 