package com.kyx.service.erp.enums.asset;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ERP 资产录入申请状态枚举
 *
 * @author kyx
 */
@Getter
@AllArgsConstructor
public enum ErpAssetInputStatusEnum {

    PENDING(1, "待审批"),
    APPROVING(2, "审批中"),
    APPROVED(3, "审批通过"),
    REJECTED(4, "审批拒绝");

    /**
     * 状态
     */
    private final Integer status;
    /**
     * 状态名
     */
    private final String name;

} 