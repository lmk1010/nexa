package com.kyx.service.finance.enums;

import com.kyx.foundation.common.enums.RpcConstants;

/**
 * API 常量类
 *
 * @author xyang
 */
public class ApiConstants {

    /**
     * 服务名
     *
     * 注意，需要保证和 spring.application.name 保持一致
     */
    public static final String NAME = "finance-server";

    public static final String PREFIX = RpcConstants.RPC_API_PREFIX + "/finance";

    public static final String VERSION = "1.0.0";
}
