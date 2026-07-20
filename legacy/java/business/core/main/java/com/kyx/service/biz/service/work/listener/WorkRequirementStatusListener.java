package com.kyx.service.biz.service.work.listener;

import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.biz.service.work.WorkRequirementService;
import com.kyx.service.biz.service.work.WorkRequirementServiceImpl;
import com.kyx.service.bpm.api.event.BpmProcessInstanceStatusEvent;
import com.kyx.service.bpm.api.event.BpmProcessInstanceStatusEventListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 需求审批流程状态本地监听器。
 *
 * <p>模块化单体下 BPM 与 Business 同 JVM，优先走 Spring 事件，避免仅依赖 Kafka
 * 时因 broker 不可达导致审批结果无法回写业务状态。</p>
 */
@Component
public class WorkRequirementStatusListener extends BpmProcessInstanceStatusEventListener {

    @Resource
    private WorkRequirementService workRequirementService;

    @Override
    protected String getProcessDefinitionKey() {
        return WorkRequirementServiceImpl.PROCESS_KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        final Long requirementId;
        try {
            requirementId = Long.parseLong(event.getBusinessKey());
        } catch (Exception ex) {
            return;
        }
        Long tenantId = TenantContextHolder.getTenantId();
        Runnable action = () -> workRequirementService.updateApprovalStatusByBpmEvent(
                requirementId, event.getId(), event.getStatus(), null);
        if (tenantId != null) {
            TenantUtils.execute(tenantId, action);
        } else {
            action.run();
        }
    }

}
