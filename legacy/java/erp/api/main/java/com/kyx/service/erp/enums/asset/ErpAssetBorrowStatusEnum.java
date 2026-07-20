package com.kyx.service.erp.enums.asset;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ERP 资产借用状态枚举
 *
 * @author kyx
 */
@Getter
@AllArgsConstructor
public enum ErpAssetBorrowStatusEnum {

    PENDING(0, "申请中"),
    BORROWED(1, "借用中"),
    RETURNED(2, "已归还"),
    OVERDUE(3, "逾期未还"),
    DAMAGED(4, "资产损坏"),
    LOST(5, "资产丢失"),
    REJECTED(6, "申请拒绝");

    /**
     * 状态
     */
    private final Integer status;
    /**
     * 状态名
     */
    private final String name;

    public static ErpAssetBorrowStatusEnum valueOf(Integer status) {
        for (ErpAssetBorrowStatusEnum statusEnum : ErpAssetBorrowStatusEnum.values()) {
            if (statusEnum.getStatus().equals(status)) {
                return statusEnum;
            }
        }
        throw new IllegalArgumentException("未知的借用状态：" + status);
    }
} 