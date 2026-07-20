package com.kyx.service.hr.enums;

import com.kyx.foundation.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 审批类型枚举
 *
 * @author MK
 */
@AllArgsConstructor
@Getter
public enum ApprovalTypeEnum implements ArrayValuable<Integer> {

    SIMPLE(1, "简易审批"),
    BPM(2, "BPM流程审批");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(ApprovalTypeEnum::getType).toArray(Integer[]::new);

    /**
     * 类型值
     */
    private final Integer type;
    /**
     * 类型名
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}