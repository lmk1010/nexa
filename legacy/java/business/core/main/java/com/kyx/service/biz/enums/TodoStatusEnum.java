package com.kyx.service.biz.enums;

import com.kyx.foundation.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@RequiredArgsConstructor
@Getter
public enum TodoStatusEnum implements ArrayValuable<Integer> {

    PROCESS(0, "In Progress"),
    DONE(1, "Done");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(TodoStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
