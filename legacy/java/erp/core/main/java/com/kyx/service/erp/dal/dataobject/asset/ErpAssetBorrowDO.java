package com.kyx.service.erp.dal.dataobject.asset;

import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ERP 资产借用记录 DO
 * 
 * 业务说明：
 * - 借用（Borrow）：临时借用，有明确的归还期限，适用于短期使用场景
 * - 领用（Checkout）：长期分配，通常无明确归还期限，适用于固定资产分配
 *
 * @author kyx
 */
@TableName("erp_asset_borrow")
@KeySequence("erp_asset_borrow_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpAssetBorrowDO extends BaseDO {

    /**
     * 借用记录编号
     */
    @TableId
    private Long id;
    
    /**
     * 借用编号
     */
    private String borrowNo;
    
    /**
     * 资产编号
     */
    private Long assetId;
    
    /**
     * 借用人编号
     */
    private Long borrowUserId;
    
    /**
     * 借用部门编号
     */
    private Long borrowDeptId;
    
    /**
     * 借用日期
     */
    private LocalDate borrowDate;
    
    /**
     * 预计归还日期
     */
    private LocalDate expectedReturnDate;
    
    /**
     * 实际归还日期
     */
    private LocalDate actualReturnDate;
    
    /**
     * 借用原因
     */
    private String borrowReason;
    
    /**
     * 借用用途
     */
    private String borrowPurpose;
    
    /**
     * 借用状态：0-申请中，1-借用中，2-已归还，3-逾期未还，4-资产损坏，5-资产丢失，6-申请拒绝
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
     * BMP状态：1-流程中，2-已完成，3-已取消
     */
    private Integer bmpStatus;
    
    /**
     * BMP流程实例编号
     */
    private String bmpProcessInstanceId;
    
    /**
     * 备注
     */
    private String remark;
} 