package com.kyx.service.biz.enums;

import com.kyx.foundation.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@RequiredArgsConstructor
@Getter
public enum WorkRequirementStatusEnum implements ArrayValuable<Integer> {

    PENDING_ASSIGN(0, "待分派"),
    PENDING_DEVELOP(1, "待开发"),
    DEVELOPING(2, "开发中"),
    TESTING(3, "测试中"),
    PENDING_ACCEPT(4, "待验收"),
    DONE(5, "已完成"),
    CANCELED(6, "已取消"),
    SUSPENDED(7, "已挂起");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(WorkRequirementStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
