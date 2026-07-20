package com.kyx.service.finance.framework.rpc.config;

import com.kyx.foundation.common.biz.infra.logger.ApiAccessLogCommonApi;
import com.kyx.foundation.common.biz.infra.logger.ApiErrorLogCommonApi;
import com.kyx.foundation.common.biz.system.tenant.TenantCommonApi;
import com.kyx.service.business.api.dept.DeptApi;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.op.api.config.ConfigApi;
import com.kyx.service.op.api.file.FileApi;
import com.kyx.service.op.api.websocket.WebSocketSenderApi;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * 财务服务 RPC 配置类
 *
 * @author xyang
 */
@Configuration(value = "financeRpcConfiguration", proxyBeanMethods = false)
@EnableFeignClients(clients = {
        FileApi.class,
        WebSocketSenderApi.class,
        ConfigApi.class,
        AdminUserApi.class,
        DeptApi.class,
        ApiAccessLogCommonApi.class,
        ApiErrorLogCommonApi.class,
        TenantCommonApi.class
})
public class RpcConfiguration {
}
