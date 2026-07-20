package com.kyx.service.erp.enums.asset;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ERP 资产报废BMP状态枚举
 *
 * @author kyx
 */
@AllArgsConstructor
@Getter
public enum ErpAssetScrappedBmpStatusEnum {

    PROCESSING(1, "流程中"),
    COMPLETED(2, "已完成"),
    CANCELLED(3, "已取消");

    /**
     * 状态值
     */
    private final Integer status;

    /**
     * 状态名称
     */
    private final String name;
} 