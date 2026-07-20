package com.kyx.service.erp.enums.asset;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ERP 资产领用状态枚举
 *
 * @author kyx
 */
@AllArgsConstructor
@Getter
public enum ErpAssetCheckoutStatusEnum {

    /**
     * 申请中（已提交申请，等待审批）
     */
    PENDING(0, "申请中"),

    /**
     * 领用中（审批通过后，正在使用）
     */
    IN_USE(1, "领用中"),
    
    /**
     * 已归还
     */
    RETURNED(2, "已归还"),
    
    /**
     * 逾期未还
     */
    OVERDUE(3, "逾期未还"),
    
    /**
     * 资产损坏
     */
    DAMAGED(4, "资产损坏"),
    
    /**
     * 资产丢失
     */
    LOST(5, "资产丢失"),
    
    /**
     * 申请拒绝
     */
    REJECTED(6, "申请拒绝");

    /**
     * 状态值
     */
    private final Integer status;
    
    /**
     * 状态名称
     */
    private final String name;
    
    /**
     * 判断是否为使用中状态（审批通过后）
     */
    public static boolean isInUse(Integer approvalStatus, Integer status) {
        return approvalStatus != null && approvalStatus.equals(2) && // 审批通过
               status != null && status.equals(IN_USE.getStatus()); // 领用中
    }
    
    /**
     * 判断是否为申请中状态（待审批）
     */
    public static boolean isPending(Integer approvalStatus, Integer status) {
        return approvalStatus != null && approvalStatus.equals(1) && // 待审批
               status != null && status.equals(PENDING.getStatus()); // 申请中
    }
} 