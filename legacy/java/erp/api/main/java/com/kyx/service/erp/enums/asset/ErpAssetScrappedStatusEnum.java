package com.kyx.service.erp.enums.asset;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ERP 资产报废状态枚举
 *
 * @author kyx
 */
@AllArgsConstructor
@Getter
public enum ErpAssetScrappedStatusEnum {

    PENDING(0, "申请中"),
    APPROVING(1, "审批中"),
    APPROVED(2, "审批通过"),
    REJECTED(3, "审批拒绝"),
    COMPLETED(4, "已完成");

    /**
     * 状态值
     */
    private final Integer status;

    /**
     * 状态名称
     */
    private final String name;
} 