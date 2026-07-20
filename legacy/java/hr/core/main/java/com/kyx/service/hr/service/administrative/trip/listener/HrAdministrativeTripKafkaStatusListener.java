package com.kyx.service.hr.service.administrative.trip.listener;

import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.bpm.api.event.dto.BpmProcessInstanceStatusKafkaEvent;
import com.kyx.service.hr.config.KafkaTopicConfig;
import com.kyx.service.hr.service.administrative.trip.HrAdministrativeTripService;
import com.kyx.service.hr.service.administrative.trip.HrAdministrativeTripServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * HR 出差流程状态 Kafka 监听器
 *
 * @author MK
 */
@Slf4j
@Component
public class HrAdministrativeTripKafkaStatusListener {

    @Resource
    private HrAdministrativeTripService tripService;

    @KafkaListener(
            topics = KafkaTopicConfig.BPM_PROCESS_STATUS_TOPIC,
            groupId = "hr-administrative-trip-group"
    )
    public void handleBpmProcessStatusChanged(BpmProcessInstanceStatusKafkaEvent event) {
        if (!HrAdministrativeTripServiceImpl.PROCESS_KEY.equals(event.getProcessDefinitionKey())) {
            return;
        }

        final Long tenantId = event.getTenantId() != null ? event.getTenantId() : TenantContextHolder.getTenantId();
        try {
            TenantUtils.execute(tenantId, () -> {
                try {
                    Long tripId = Long.parseLong(event.getBusinessKey());
                    tripService.updateTripStatus(tripId, event.getStatus());
                } catch (NumberFormatException ex) {
                    log.error("解析出差业务编号失败: businessKey={}", event.getBusinessKey(), ex);
                }
            });
        } catch (Exception ex) {
            log.error("处理出差流程状态变更失败: event={}", event, ex);
        }
    }
}
