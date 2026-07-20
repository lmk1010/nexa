package com.kyx.service.hr.service.employee.listener;

import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.bpm.api.event.dto.BpmProcessInstanceStatusKafkaEvent;
import com.kyx.service.hr.config.KafkaTopicConfig;
import com.kyx.service.hr.service.employee.EmployeeProfileChangeRequestService;
import com.kyx.service.hr.service.employee.EmployeeProfileChangeRequestServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * Profile change BPM status listener.
 */
@Slf4j
@Component
public class HrProfileChangeKafkaStatusListener {

    @Resource
    private EmployeeProfileChangeRequestService profileChangeRequestService;

    @KafkaListener(
            topics = KafkaTopicConfig.BPM_PROCESS_STATUS_TOPIC,
            groupId = "hr-profile-change-group"
    )
    public void handleBpmProcessStatusChanged(BpmProcessInstanceStatusKafkaEvent event) {
        if (!Objects.equals(EmployeeProfileChangeRequestServiceImpl.PROCESS_KEY, event.getProcessDefinitionKey())) {
            return;
        }

        Long tenantId = event.getTenantId() != null ? event.getTenantId() : TenantContextHolder.getTenantId();
        try {
            TenantUtils.execute(tenantId, () -> {
                try {
                    if (event.getBusinessKey() == null || event.getBusinessKey().trim().isEmpty()) {
                        log.warn("资料变更 BPM 回调 businessKey 为空: processInstanceId={}", event.getProcessInstanceId());
                        return;
                    }
                    Long requestId = Long.parseLong(event.getBusinessKey());
                    profileChangeRequestService.updateApprovalStatusByBpmEvent(
                            requestId, event.getProcessInstanceId(), event.getStatus(), event.getUserId());
                } catch (NumberFormatException ex) {
                    log.error("解析资料变更业务编号失败: businessKey={}", event.getBusinessKey(), ex);
                }
            });
        } catch (Exception ex) {
            log.error("处理资料变更流程状态变更失败: event={}", event, ex);
        }
    }

}
