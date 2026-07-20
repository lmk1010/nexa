package com.kyx.service.im.dal.dataobject.invitecode;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 邀请码状态枚举
 *
 * @author MK
 */
@Getter
@AllArgsConstructor
public enum InviteCodeStatusEnum {

    ENABLE(0, "启用"),
    DISABLE(1, "禁用"),
    EXPIRED(2, "已过期"),
    USED_UP(3, "已用完"),
    ;

    /**
     * 状态
     */
    private final Integer status;
    /**
     * 描述
     */
    private final String description;

    public static InviteCodeStatusEnum valueOf(Integer status) {
        for (InviteCodeStatusEnum value : InviteCodeStatusEnum.values()) {
            if (value.getStatus().equals(status)) {
                return value;
            }
        }
        return null;
    }

} 