package com.kyx.service.erp.framework.rpc.config;

import com.kyx.foundation.common.biz.infra.logger.ApiAccessLogCommonApi;
import com.kyx.foundation.common.biz.infra.logger.ApiErrorLogCommonApi;
import com.kyx.foundation.common.biz.system.tenant.TenantCommonApi;
import com.kyx.service.bpm.api.task.BpmProcessInstanceApi;
import com.kyx.service.business.api.dept.DeptApi;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.op.api.config.ConfigApi;
import com.kyx.service.op.api.file.FileApi;
import com.kyx.service.op.api.websocket.WebSocketSenderApi;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration(value = "erpRpcConfiguration", proxyBeanMethods = false)
@EnableFeignClients(clients = {FileApi.class, WebSocketSenderApi.class, ConfigApi.class, AdminUserApi.class, DeptApi.class, ApiAccessLogCommonApi.class, ApiErrorLogCommonApi.class, BpmProcessInstanceApi.class, TenantCommonApi.class})
public class RpcConfiguration {
}
