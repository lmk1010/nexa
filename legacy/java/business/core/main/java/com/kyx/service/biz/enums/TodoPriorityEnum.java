package com.kyx.service.biz.enums;

import com.kyx.foundation.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@RequiredArgsConstructor
@Getter
public enum TodoPriorityEnum implements ArrayValuable<Integer> {

    LOW(1, "Low"),
    MEDIUM(2, "Medium"),
    HIGH(3, "High"),
    URGENT(4, "Urgent");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(TodoPriorityEnum::getPriority).toArray(Integer[]::new);

    private final Integer priority;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
