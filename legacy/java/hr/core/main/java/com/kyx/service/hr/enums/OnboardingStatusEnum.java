package com.kyx.service.hr.enums;

import com.kyx.foundation.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 入职状态枚举
 *
 * @author MK
 */
@Getter
@AllArgsConstructor
public enum OnboardingStatusEnum implements ArrayValuable<Integer> {

    DRAFT(1, "待提交"),
    APPROVING(2, "审批中"),
    APPROVED(3, "已通过"),
    REJECTED(4, "已拒绝"),
    CANCELLED(5, "已取消");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(OnboardingStatusEnum::getStatus).toArray(Integer[]::new);

    /**
     * 状态值
     */
    private final Integer status;
    /**
     * 状态名
     */
    private final String name;

    public static OnboardingStatusEnum valueOf(Integer status) {
        for (OnboardingStatusEnum value : OnboardingStatusEnum.values()) {
            if (value.getStatus().equals(status)) {
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