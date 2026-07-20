package com.kyx.service.erp.enums.asset;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ERP 资产挂失状态枚举
 *
 * @author kyx
 */
@AllArgsConstructor
@Getter
public enum ErpAssetLostStatusEnum {

    PENDING(0, "申请中"),
    APPROVING(1, "审批中"),
    APPROVED(2, "审批通过"),
    REJECTED(3, "审批拒绝"),
    FOUND(4, "已找回"),
    CONFIRMED_LOST(5, "确认丢失");

    /**
     * 状态值
     */
    private final Integer status;
    
    /**
     * 状态名
     */
    private final String name;
} 