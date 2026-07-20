package com.kyx.service.biz.enums;

import com.kyx.foundation.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@RequiredArgsConstructor
@Getter
public enum WorkRequirementPriorityEnum implements ArrayValuable<Integer> {

    LOW(1, "低"),
    MEDIUM(2, "中"),
    HIGH(3, "高"),
    URGENT(4, "紧急");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(WorkRequirementPriorityEnum::getPriority).toArray(Integer[]::new);

    private final Integer priority;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
