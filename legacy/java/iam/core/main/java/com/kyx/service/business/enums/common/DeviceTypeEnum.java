package com.kyx.service.business.enums.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 设备类型枚举
 *
 * @author MK
 */
@Getter
@AllArgsConstructor
public enum DeviceTypeEnum {

    WEB("WEB", "Web浏览器"),
    MOBILE_ANDROID("MOBILE_ANDROID", "Android移动端"),
    MOBILE_IOS("MOBILE_IOS", "iOS移动端"),
    MOBILE_H5("MOBILE_H5", "移动端H5"),
    DESKTOP("DESKTOP", "桌面应用");

    /**
     * 类型
     */
    private final String type;
    /**
     * 描述
     */
    private final String description;

    public static DeviceTypeEnum getByType(String type) {
        for (DeviceTypeEnum value : values()) {
            if (value.getType().equals(type)) {
                return value;
            }
        }
        return WEB; // 默认返回WEB
    }
} 