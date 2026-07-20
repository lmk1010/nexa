package com.kyx.service.hr.enums;

import com.kyx.foundation.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 员工成长记录事件类型枚举
 *
 * @author MK
 */
@AllArgsConstructor
@Getter
public enum EmployeeEventTypeEnum implements ArrayValuable<Integer> {

    ONBOARD(1, "入职"),
    CONFIRMATION(2, "转正"),
    PROMOTION(3, "晋升"),
    DEMOTION(4, "降级"),
    TRANSFER(5, "调岗"),
    RESIGNATION(6, "离职"),
    REINSTATEMENT(7, "复职"),
    OTHER(8, "其他");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(EmployeeEventTypeEnum::getType).toArray(Integer[]::new);

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
