package com.kyx.service.erp.enums.asset;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ERP 资产录入申请BMP流程状态枚举
 *
 * @author kyx
 */
@Getter
@AllArgsConstructor
public enum ErpAssetInputBmpStatusEnum {

    IN_PROGRESS(1, "流程中"),
    COMPLETED(2, "已完成"),
    CANCELLED(3, "已取消");

    /**
     * 状态
     */
    private final Integer status;
    /**
     * 状态名
     */
    private final String name;

} 