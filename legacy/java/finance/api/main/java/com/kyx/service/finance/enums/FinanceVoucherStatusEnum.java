package com.kyx.service.finance.enums;

import com.kyx.foundation.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 凭证状态枚举
 *
 * @author xyang
 */
@Getter
@AllArgsConstructor
public enum FinanceVoucherStatusEnum implements ArrayValuable<String> {

    /**
     * 草稿
     */
    DRAFT("草稿"),
    /**
     * 已审核
     */
    APPROVED("已审核"),
    /**
     * 已过账
     */
    POSTED("已过账"),
    /**
     * 已作废
     */
    VOID("已作废");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(FinanceVoucherStatusEnum::name)
            .toArray(String[]::new);

    private final String label;

    @Override
    public String[] array() {
        return ARRAYS;
    }
}
