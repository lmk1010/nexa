package com.kyx.service.hr.enums;

import com.kyx.foundation.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 入职流程类型枚举
 *
 * @author MK
 */
@Getter
@AllArgsConstructor
public enum ProcessTypeEnum implements ArrayValuable<Integer> {

    SIMPLE(1, "简易入职"),
    APPROVAL(2, "审批入职");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(ProcessTypeEnum::getType).toArray(Integer[]::new);

    /**
     * 类型值
     */
    private final Integer type;
    /**
     * 类型名
     */
    private final String name;

    public static ProcessTypeEnum valueOf(Integer type) {
        for (ProcessTypeEnum value : ProcessTypeEnum.values()) {
            if (value.getType().equals(type)) {
                return value;
            }
        }
        return null;
    }

    @Override
    public Integer[] array() {
        return ARRAYS;
    }
} 