package com.kyx.service.biz.framework.rpc.config;

import com.kyx.foundation.common.biz.infra.logger.ApiAccessLogCommonApi;
import com.kyx.foundation.common.biz.infra.logger.ApiErrorLogCommonApi;
import com.kyx.service.business.api.dept.DeptApi;
import com.kyx.service.business.api.notify.NotifyMessageSendApi;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.bpm.api.task.BpmProcessInstanceApi;
import com.kyx.service.hr.api.dingtalk.DingTalkRequirementNoticeApi;
import com.kyx.service.op.api.config.ConfigApi;
import com.kyx.service.op.api.file.FileApi;
import com.kyx.service.op.api.websocket.WebSocketSenderApi;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration(value = "businessRpcConfiguration", proxyBeanMethods = false)
@EnableFeignClients(clients = {FileApi.class, WebSocketSenderApi.class, ConfigApi.class, AdminUserApi.class, DeptApi.class, NotifyMessageSendApi.class, ApiAccessLogCommonApi.class, ApiErrorLogCommonApi.class, BpmProcessInstanceApi.class, DingTalkRequirementNoticeApi.class})
public class RpcConfiguration {
}
