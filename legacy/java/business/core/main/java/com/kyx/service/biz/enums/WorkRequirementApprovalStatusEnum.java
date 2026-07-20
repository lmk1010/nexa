package com.kyx.service.biz.enums;

import com.kyx.foundation.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@RequiredArgsConstructor
@Getter
public enum WorkRequirementApprovalStatusEnum implements ArrayValuable<Integer> {

    RUNNING(1, "审批中"),
    APPROVE(2, "审批通过"),
    REJECT(3, "审批拒绝"),
    CANCEL(4, "已取消");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(WorkRequirementApprovalStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
