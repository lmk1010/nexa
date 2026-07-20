package com.kyx.service.erp.enums.asset;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ERP 资产挂失BMP状态枚举
 *
 * @author kyx
 */
@AllArgsConstructor
@Getter
public enum ErpAssetLostBmpStatusEnum {

    PROCESSING(1, "流程中"),
    COMPLETED(2, "已完成"),
    CANCELLED(3, "已取消");

    /**
     * 状态值
     */
    private final Integer status;
    
    /**
     * 状态名
     */
    private final String name;
} 