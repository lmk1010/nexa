package com.kyx.service.erp.enums.asset;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ERP 资产转移状态枚举
 *
 * @author kyx
 */
@AllArgsConstructor
@Getter
public enum ErpAssetTransferStatusEnum {

    PENDING(0, "申请中"),
    COMPLETED(1, "已完成"),
    REJECTED(2, "已拒绝"),
    CANCELLED(3, "已撤销");

    /**
     * 状态值
     */
    private final Integer status;
    /**
     * 状态名
     */
    private final String name;

} 