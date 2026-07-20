package com.kyx.service.erp.enums.purchase;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ERP 采购申请紧急程度枚举
 *
 * @author MK
 */
@Getter
@AllArgsConstructor
public enum ErpPurchaseRequestUrgentLevelEnum {

    LOW(1, "低"),
    NORMAL(2, "一般"),
    HIGH(3, "高"),
    URGENT(4, "紧急");

    /**
     * 紧急程度
     */
    private final Integer level;
    /**
     * 名字
     */
    private final String name;

} 