package com.kyx.service.erp.enums.asset;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ERP 资产借用 BMP 状态枚举
 *
 * @author kyx
 */
@AllArgsConstructor
@Getter
public enum ErpAssetBorrowBmpStatusEnum {

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
     * 已取消
     */
    CANCELLED(4, "已取消");

    /**
     * 状态
     */
    private final Integer status;

    /**
     * 状态名
     */
    private final String name;

} 