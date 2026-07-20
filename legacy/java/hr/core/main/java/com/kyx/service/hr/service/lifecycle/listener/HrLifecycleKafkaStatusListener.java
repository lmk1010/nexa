package com.kyx.service.hr.service.lifecycle.listener;

import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.bpm.api.event.dto.BpmProcessInstanceStatusKafkaEvent;
import com.kyx.service.hr.config.KafkaTopicConfig;
import com.kyx.service.hr.service.lifecycle.HrLifecycleService;
import com.kyx.service.hr.service.lifecycle.HrLifecycleServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * HR lifecycle BPM status listener.
 */
@Slf4j
@Component
public class HrLifecycleKafkaStatusListener {

    @Resource
    private HrLifecycleService hrLifecycleService;

    @KafkaListener(
            topics = KafkaTopicConfig.BPM_PROCESS_STATUS_TOPIC,
            groupId = "hr-lifecycle-group"
    )
    public void handleBpmProcessStatusChanged(BpmProcessInstanceStatusKafkaEvent event) {
        if (!isLifecycleProcess(event.getProcessDefinitionKey())) {
            return;
        }

        Long tenantId = event.getTenantId() != null ? event.getTenantId() : TenantContextHolder.getTenantId();
        try {
            TenantUtils.execute(tenantId, () -> {
                try {
                    if (event.getBusinessKey() == null || event.getBusinessKey().trim().isEmpty()) {
                        log.warn("生命周期 BPM 回调 businessKey 为空: processInstanceId={}", event.getProcessInstanceId());
                        return;
                    }
                    Long lifecycleEventId = Long.parseLong(event.getBusinessKey());
                    hrLifecycleService.updateApprovalStatusByBpmEvent(lifecycleEventId,
                            event.getProcessInstanceId(), event.getStatus(), event.getUserId());
                } catch (NumberFormatException ex) {
                    log.error("解析生命周期业务编号失败: businessKey={}", event.getBusinessKey(), ex);
                }
            });
        } catch (Exception ex) {
            log.error("处理生命周期流程状态变更失败: event={}", event, ex);
        }
    }

    private boolean isLifecycleProcess(String processDefinitionKey) {
        return Objects.equals(HrLifecycleServiceImpl.PROCESS_KEY_REGULARIZATION, processDefinitionKey)
                || Objects.equals(HrLifecycleServiceImpl.PROCESS_KEY_TRANSFER, processDefinitionKey)
                || Objects.equals(HrLifecycleServiceImpl.PROCESS_KEY_SALARY_ADJUST, processDefinitionKey)
                || Objects.equals(HrLifecycleServiceImpl.PROCESS_KEY_RESIGNATION, processDefinitionKey);
    }

}
