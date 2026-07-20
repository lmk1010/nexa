package com.kyx.service.erp.enums.purchase;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ERP 采购申请BMP流程状态枚举
 *
 * @author MK
 */
@AllArgsConstructor
@Getter
public enum ErpPurchaseRequestBmpStatusEnum {

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
     * 状态值
     */
    private final Integer status;
    /**
     * 状态名称
     */
    private final String name;

} 