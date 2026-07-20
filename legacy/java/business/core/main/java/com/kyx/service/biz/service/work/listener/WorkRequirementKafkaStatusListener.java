package com.kyx.service.biz.service.work.listener;

import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.bpm.api.event.dto.BpmProcessInstanceStatusKafkaEvent;
import com.kyx.service.biz.config.KafkaTopicConfig;
import com.kyx.service.biz.service.work.WorkRequirementService;
import com.kyx.service.biz.service.work.WorkRequirementServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 需求审批流程状态 Kafka 监听器
 */
@Slf4j
@Component
public class WorkRequirementKafkaStatusListener {

    @Resource
    private WorkRequirementService workRequirementService;

    @KafkaListener(
            topics = KafkaTopicConfig.BPM_PROCESS_STATUS_TOPIC,
            groupId = "business-work-requirement-group"
    )
    public void handleBpmProcessStatusChanged(BpmProcessInstanceStatusKafkaEvent event) {
        if (!WorkRequirementServiceImpl.PROCESS_KEY.equals(event.getProcessDefinitionKey())) {
            return;
        }
        final Long requirementId;
        try {
            requirementId = Long.parseLong(event.getBusinessKey());
        } catch (NumberFormatException ex) {
            // 非法业务编号无法重试修复，直接记录并跳过，避免阻塞消费。
            log.error("解析需求业务编号失败: businessKey={}", event.getBusinessKey(), ex);
            return;
        }
        final Long tenantId = event.getTenantId() != null ? event.getTenantId() : TenantContextHolder.getTenantId();
        TenantUtils.execute(tenantId, () -> workRequirementService.updateApprovalStatusByBpmEvent(
                requirementId, event.getProcessInstanceId(), event.getStatus(), event.getUserId()));
    }

}
