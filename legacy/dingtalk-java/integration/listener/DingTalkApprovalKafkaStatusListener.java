package com.kyx.service.hr.integration.dingtalk.listener;

import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.bpm.api.event.dto.BpmProcessInstanceStatusKafkaEvent;
import com.kyx.service.hr.config.KafkaTopicConfig;
import com.kyx.service.hr.integration.dingtalk.service.DingTalkApprovalNotifyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Listens BPM status events and bridges approval notifications to DingTalk.
 */
@Component
@Slf4j
public class DingTalkApprovalKafkaStatusListener {

    @Resource
    private DingTalkApprovalNotifyService dingTalkApprovalNotifyService;

    @KafkaListener(
            topics = KafkaTopicConfig.BPM_PROCESS_STATUS_TOPIC,
            groupId = "hr-dingtalk-approval-notify-group"
    )
    public void handleBpmProcessStatusChanged(BpmProcessInstanceStatusKafkaEvent event) {
        final Long tenantId = event.getTenantId() != null ? event.getTenantId() : TenantContextHolder.getTenantId();
        try {
            TenantUtils.execute(tenantId, () -> dingTalkApprovalNotifyService.notifyProcessStatus(event));
        } catch (Exception ex) {
            log.error("DingTalk approval notify failed, event={}", event, ex);
        }
    }

}

