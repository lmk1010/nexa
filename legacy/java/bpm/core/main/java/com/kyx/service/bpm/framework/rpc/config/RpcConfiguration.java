package com.kyx.service.bpm.framework.rpc.config;

import com.kyx.foundation.common.biz.infra.logger.ApiAccessLogCommonApi;
import com.kyx.foundation.common.biz.infra.logger.ApiErrorLogCommonApi;
import com.kyx.foundation.common.biz.system.tenant.TenantCommonApi;
import com.kyx.service.business.api.dept.DeptApi;
import com.kyx.service.business.api.dept.PostApi;
import com.kyx.service.business.api.dict.DictDataApi;
import com.kyx.service.business.api.notify.NotifyMessageSendApi;
import com.kyx.service.business.api.permission.PermissionApi;
import com.kyx.service.business.api.permission.RoleApi;
import com.kyx.service.business.api.sms.SmsSendApi;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.hr.api.dingtalk.DingTalkBpmNoticeApi;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration(value = "bpmRpcConfiguration", proxyBeanMethods = false)
@EnableFeignClients(clients = {RoleApi.class, DeptApi.class, PostApi.class, AdminUserApi.class, SmsSendApi.class, DictDataApi.class,
        NotifyMessageSendApi.class, PermissionApi.class, DingTalkBpmNoticeApi.class, ApiAccessLogCommonApi.class,
        ApiErrorLogCommonApi.class, TenantCommonApi.class})
public class RpcConfiguration {
}
