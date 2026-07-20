package com.kyx.service.erp.dal.dataobject.asset;

import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ERP 资产领用记录 DO
 *
 * @author kyx
 */
@TableName("erp_asset_checkout")
@KeySequence("erp_asset_checkout_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpAssetCheckoutDO extends BaseDO {

    /**
     * 领用记录编号
     */
    @TableId
    private Long id;
    
    /**
     * 资产编号
     */
    private Long assetId;
    
    /**
     * 领用人编号
     */
    private Long checkoutUserId;
    
    /**
     * 领用部门编号
     */
    private Long checkoutDeptId;
    
    /**
     * 领用日期
     */
    private LocalDate checkoutDate;
    
    /**
     * 预计归还日期
     */
    private LocalDate expectedReturnDate;
    
    /**
     * 实际归还日期
     */
    private LocalDate actualReturnDate;
    
    /**
     * 领用原因
     */
    private String checkoutReason;
    
    /**
     * 领用状态：1-领用中，2-已归还，3-逾期未还，4-资产损坏，5-资产丢失
     */
    private Integer status;
    
    /**
     * 归还状态：1-完好，2-轻微损坏，3-严重损坏，4-丢失
     */
    private Integer returnCondition;
    
    /**
     * 归还备注
     */
    private String returnRemark;
    
    /**
     * 审批人编号
     */
    private Long approverUserId;
    
    /**
     * 审批时间
     */
    private LocalDateTime approvalTime;
    
    /**
     * 审批状态：1-待审批，2-审批通过，3-审批拒绝
     */
    private Integer approvalStatus;
    
    /**
     * 审批备注
     */
    private String approvalRemark;
    
    /**
     * BPM流程实例ID
     */
    private String processInstanceId;
    
    /**
     * BMP流程状态：1-流程中，2-已完成，3-已拒绝，4-已撤销
     */
    private Integer bmpStatus;
    
    /**
     * 备注
     */
    private String remark;

} 