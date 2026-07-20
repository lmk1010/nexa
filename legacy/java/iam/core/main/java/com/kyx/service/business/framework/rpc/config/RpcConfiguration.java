package com.kyx.service.business.framework.rpc.config;

import com.kyx.foundation.common.biz.infra.logger.ApiAccessLogCommonApi;
import com.kyx.foundation.common.biz.infra.logger.ApiErrorLogCommonApi;
import com.kyx.foundation.common.biz.system.permission.PermissionCommonApi;
import com.kyx.foundation.common.biz.system.tenant.TenantCommonApi;
import com.kyx.service.hr.api.dingtalk.DingTalkSyncApi;
import com.kyx.service.im.api.invitecode.InviteCodeApi;
import com.kyx.service.op.api.config.ConfigApi;
import com.kyx.service.op.api.file.FileApi;
import com.kyx.service.op.api.websocket.WebSocketSenderApi;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration(value = "systemRpcConfiguration", proxyBeanMethods = false)
@EnableFeignClients(clients = {
    FileApi.class,
    WebSocketSenderApi.class,
    ConfigApi.class,
    InviteCodeApi.class,
    ApiAccessLogCommonApi.class,
    ApiErrorLogCommonApi.class,
    TenantCommonApi.class,
    PermissionCommonApi.class,
    DingTalkSyncApi.class
})
public class RpcConfiguration {
}
