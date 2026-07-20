package com.kyx.service.business.enums.permission;

import com.kyx.foundation.common.util.object.ObjectUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * 角色标识枚举
 */
@Getter
@AllArgsConstructor
@Accessors(chain = true)
public enum RoleCodeEnum {

    SUPER_ADMIN("super_admin", "超级管理员"),
    TENANT_ADMIN("tenant_admin", "租户管理员"),
    CRM_ADMIN("crm_admin", "CRM 管理员"); // CRM 系统专用
    ;

    /**
     * 角色编码
     */
    private final String code;
    /**
     * 名字
     */
    private final String name;

    public static boolean isSuperAdmin(String code) {
        return ObjectUtils.equalsAny(code, SUPER_ADMIN.getCode());
    }

}
