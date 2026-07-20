package com.kyx.service.im.dal.dataobject.invitecode;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 邀请码类型枚举
 *
 * @author MK
 */
@Getter
@AllArgsConstructor
public enum InviteCodeTypeEnum {

    REGISTER(1, "注册邀请"),
    GROUP_JOIN(2, "群组邀请"),
    CHANNEL_JOIN(3, "频道邀请"),
    ;

    /**
     * 类型
     */
    private final Integer type;
    /**
     * 描述
     */
    private final String description;

    public static InviteCodeTypeEnum valueOf(Integer type) {
        for (InviteCodeTypeEnum value : InviteCodeTypeEnum.values()) {
            if (value.getType().equals(type)) {
                return value;
            }
        }
        return null;
    }

} 