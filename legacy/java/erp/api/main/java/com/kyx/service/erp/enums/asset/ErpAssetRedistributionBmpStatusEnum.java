package com.kyx.service.erp.enums.asset;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ERP 资产调拨BMP状态枚举
 *
 * @author kyx
 */
@AllArgsConstructor
@Getter
public enum ErpAssetRedistributionBmpStatusEnum {

    IN_PROGRESS(1, "流程中"),
    COMPLETED(2, "已完成"),
    REJECTED(3, "已拒绝"),
    CANCELLED(4, "已撤销");

    /**
     * 状态值
     */
    private final Integer status;
    /**
     * 状态名
     */
    private final String name;
} 