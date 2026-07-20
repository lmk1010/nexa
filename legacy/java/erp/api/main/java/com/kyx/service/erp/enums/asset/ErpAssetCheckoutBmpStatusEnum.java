package com.kyx.service.erp.enums.asset;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ERP 资产领用 BMP 流程状态枚举
 *
 * @author kyx
 */
@AllArgsConstructor
@Getter
public enum ErpAssetCheckoutBmpStatusEnum {

    /**
     * 流程中
     */
    IN_PROGRESS(1, "流程中"),
    
    /**
     * 已完成
     */
    COMPLETED(2, "已完成"),
    
    /**
     * 已拒绝
     */
    REJECTED(3, "已拒绝"),
    
    /**
     * 已撤销
     */
    CANCELLED(4, "已撤销");

    /**
     * 状态
     */
    private final Integer status;
    
    /**
     * 状态名
     */
    private final String name;

} 