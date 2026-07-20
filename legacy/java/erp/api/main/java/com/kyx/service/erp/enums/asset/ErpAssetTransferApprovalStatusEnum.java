package com.kyx.service.erp.enums.asset;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ERP 资产转移审批状态枚举
 *
 * @author kyx
 */
@AllArgsConstructor
@Getter
public enum ErpAssetTransferApprovalStatusEnum {

    PENDING(0, "待审批"),
    IN_PROGRESS(1, "审批中"),
    APPROVED(2, "审批通过"),
    REJECTED(3, "审批拒绝");

    /**
     * 状态值
     */
    private final Integer status;
    /**
     * 状态名
     */
    private final String name;

} 