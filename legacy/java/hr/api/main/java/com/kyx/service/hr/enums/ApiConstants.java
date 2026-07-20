package com.kyx.service.hr.enums;

import com.kyx.foundation.common.enums.RpcConstants;

/**
 * HR API 相关的枚举
 *
 * @author MK
 */
public class ApiConstants {

    /**
     * 服务名
     *
     * 注意，需要保证和 spring.application.name 保持一致
     */
    public static final String NAME = "hr-server";

    public static final String PREFIX = RpcConstants.RPC_API_PREFIX + "/hr";

    public static final String VERSION = "1.0.0";

} 