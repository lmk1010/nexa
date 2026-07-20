package com.kyx.service.biz.framework.rpc.config;

import com.kyx.service.business.api.dept.DeptApi;
import com.kyx.service.business.api.dept.PostApi;
import com.kyx.service.business.api.dict.DictDataApi;
import com.kyx.service.business.api.notice.NoticeApi;
import com.kyx.service.business.api.notify.NotifyMessageSendApi;
import com.kyx.service.business.api.permission.PermissionApi;
import com.kyx.service.business.api.permission.RoleApi;
import com.kyx.service.business.api.sms.SmsSendApi;
import com.kyx.service.business.api.user.AdminUserApi;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * 模块化单体的远程调用边界。
 *
 * <p>这里只注册仍然独立部署的 IAM 接口。OP、BPM、HR、AI 等已经合并的领域接口
 * 不再创建 Feign 代理，而是直接注入同一 Spring 容器中的 ApiImpl。</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableFeignClients(clients = {
        AdminUserApi.class,
        DeptApi.class,
        PostApi.class,
        DictDataApi.class,
        NoticeApi.class,
        NotifyMessageSendApi.class,
        PermissionApi.class,
        RoleApi.class,
        SmsSendApi.class
})
public class MonolithRpcConfiguration {
}
