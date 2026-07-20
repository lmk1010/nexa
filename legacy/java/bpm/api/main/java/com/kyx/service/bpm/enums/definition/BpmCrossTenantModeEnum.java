package com.kyx.service.bpm.enums.definition;

import com.kyx.foundation.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * BPM 流程跨租户模式枚举
 *
 * @author MK
 */
@Getter
@AllArgsConstructor
public enum BpmCrossTenantModeEnum implements ArrayValuable<Integer> {

    /**
     * 租户隔离：仅当前租户可见（默认模式）
     */
    TENANT_ISOLATED(0, "租户隔离"),

    /**
     * 全局可见：所有租户可见
     */
    GLOBAL_VISIBLE(1, "全局可见"),

    /**
     * 指定租户可见：仅指定的租户列表可见
     */
    SPECIFIED_TENANTS(2, "指定租户可见");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(BpmCrossTenantModeEnum::getMode).toArray(Integer[]::new);

    private final Integer mode;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    public static BpmCrossTenantModeEnum valueOf(Integer mode) {
        for (BpmCrossTenantModeEnum modeEnum : values()) {
            if (modeEnum.getMode().equals(mode)) {
                return modeEnum;
            }
        }
        return null;
    }

}
