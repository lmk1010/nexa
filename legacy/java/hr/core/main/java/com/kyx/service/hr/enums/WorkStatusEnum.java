package com.kyx.service.hr.enums;

import com.kyx.foundation.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 工作状态枚举
 *
 * @author MK
 */
@AllArgsConstructor
@Getter
public enum WorkStatusEnum implements ArrayValuable<Integer> {

    PENDING_FILL(0, "待填写"),
    PENDING_ENTRY(1, "待入职"),
    PROBATION(2, "试用期"),
    ON_JOB(3, "在职"),
    RESIGNED(4, "离职");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(WorkStatusEnum::getStatus).toArray(Integer[]::new);

    /**
     * 状态值
     */
    private final Integer status;
    /**
     * 状态名
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }
} 