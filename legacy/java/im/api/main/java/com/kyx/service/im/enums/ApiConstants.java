package com.kyx.service.im.enums;

import com.kyx.foundation.common.enums.RpcConstants;

/**
 * IM API 相关的枚举
 *
 * @author MK
 */
public class ApiConstants {

    /**
     * 服务名
     *
     * 注意，需要保证和 spring.application.name 保持一致
     */
    public static final String NAME = "im-server";

    public static final String PREFIX = RpcConstants.RPC_API_PREFIX + "/im";

    public static final String VERSION = "1.0.0";

} 