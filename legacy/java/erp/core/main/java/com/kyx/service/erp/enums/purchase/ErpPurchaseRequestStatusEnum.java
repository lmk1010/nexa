package com.kyx.service.erp.enums.purchase;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ERP 采购申请状态枚举
 *
 * @author MK
 */
@Getter
@AllArgsConstructor
public enum ErpPurchaseRequestStatusEnum {

    DRAFT(0, "草稿"),
    PENDING(1, "待审核"),
    APPROVED(2, "已通过"),
    REJECTED(3, "已拒绝"),
    CANCELLED(4, "已取消");

    /**
     * 状态
     */
    private final Integer status;
    /**
     * 名字
     */
    private final String name;

} 