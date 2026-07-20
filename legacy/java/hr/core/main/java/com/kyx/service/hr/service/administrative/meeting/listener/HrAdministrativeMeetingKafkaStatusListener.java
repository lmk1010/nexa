package com.kyx.service.hr.service.administrative.meeting.listener;

import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.bpm.api.event.dto.BpmProcessInstanceStatusKafkaEvent;
import com.kyx.service.hr.config.KafkaTopicConfig;
import com.kyx.service.hr.service.administrative.meeting.HrAdministrativeMeetingService;
import com.kyx.service.hr.service.administrative.meeting.HrAdministrativeMeetingServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * HR 会议室预约流程状态 Kafka 监听器
 *
 * @author MK
 */
@Slf4j
@Component
public class HrAdministrativeMeetingKafkaStatusListener {

    @Resource
    private HrAdministrativeMeetingService meetingService;

    @KafkaListener(
            topics = KafkaTopicConfig.BPM_PROCESS_STATUS_TOPIC,
            groupId = "hr-administrative-meeting-group"
    )
    public void handleBpmProcessStatusChanged(BpmProcessInstanceStatusKafkaEvent event) {
        if (!HrAdministrativeMeetingServiceImpl.PROCESS_KEY.equals(event.getProcessDefinitionKey())) {
            return;
        }

        final Long tenantId = event.getTenantId() != null ? event.getTenantId() : TenantContextHolder.getTenantId();
        try {
            TenantUtils.execute(tenantId, () -> {
                try {
                    Long meetingId = Long.parseLong(event.getBusinessKey());
                    meetingService.updateMeetingStatus(meetingId, event.getStatus());
                } catch (NumberFormatException ex) {
                    log.error("解析会议预约业务编号失败: businessKey={}", event.getBusinessKey(), ex);
                }
            });
        } catch (Exception ex) {
            log.error("处理会议预约流程状态变更失败: event={}", event, ex);
        }
    }
}
