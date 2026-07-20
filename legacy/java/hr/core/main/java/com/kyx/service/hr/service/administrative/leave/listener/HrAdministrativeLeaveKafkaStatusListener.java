package com.kyx.service.hr.service.administrative.leave.listener;

import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.bpm.api.event.dto.BpmProcessInstanceStatusKafkaEvent;
import com.kyx.service.hr.config.KafkaTopicConfig;
import com.kyx.service.hr.service.administrative.leave.HrAdministrativeLeaveService;
import com.kyx.service.hr.service.administrative.leave.HrAdministrativeLeaveServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * HR 请假流程状态 Kafka 监听器
 *
 * @author MK
 */
@Slf4j
@Component
public class HrAdministrativeLeaveKafkaStatusListener {

    @Resource
    private HrAdministrativeLeaveService leaveService;

    @KafkaListener(
            topics = KafkaTopicConfig.BPM_PROCESS_STATUS_TOPIC,
            groupId = "hr-administrative-leave-group"
    )
    public void handleBpmProcessStatusChanged(BpmProcessInstanceStatusKafkaEvent event) {
        if (!HrAdministrativeLeaveServiceImpl.PROCESS_KEY.equals(event.getProcessDefinitionKey())) {
            return;
        }

        final Long tenantId = event.getTenantId() != null ? event.getTenantId() : TenantContextHolder.getTenantId();
        try {
            TenantUtils.execute(tenantId, () -> {
                try {
                    Long leaveId = Long.parseLong(event.getBusinessKey());
                    leaveService.updateLeaveStatus(leaveId, event.getStatus());
                } catch (NumberFormatException ex) {
                    log.error("解析请假业务编号失败: businessKey={}", event.getBusinessKey(), ex);
                }
            });
        } catch (Exception ex) {
            log.error("处理请假流程状态变更失败: event={}", event, ex);
        }
    }
}
