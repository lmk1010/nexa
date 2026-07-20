package com.kyx.service.im.framework.rpc.config;

import com.kyx.foundation.common.biz.infra.logger.ApiAccessLogCommonApi;
import com.kyx.foundation.common.biz.infra.logger.ApiErrorLogCommonApi;
import com.kyx.service.business.api.dept.DeptApi;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.op.api.config.ConfigApi;
import com.kyx.service.op.api.file.FileApi;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration(value = "imRpcConfiguration", proxyBeanMethods = false)
@EnableFeignClients(clients = {FileApi.class, ConfigApi.class, AdminUserApi.class, DeptApi.class, ApiAccessLogCommonApi.class, ApiErrorLogCommonApi.class})
public class RpcConfiguration {
}
