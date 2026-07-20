package com.kyx.service.hr.service.employee.listener;

import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.bpm.api.event.dto.BpmProcessInstanceStatusKafkaEvent;
import com.kyx.service.hr.config.KafkaTopicConfig;
import com.kyx.service.hr.service.employee.EmployeePerformanceService;
import com.kyx.service.hr.service.employee.EmployeePerformanceServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * Performance BPM status listener.
 */
@Slf4j
@Component
public class HrPerformanceKafkaStatusListener {

    @Resource
    private EmployeePerformanceService employeePerformanceService;

    @KafkaListener(
            topics = KafkaTopicConfig.BPM_PROCESS_STATUS_TOPIC,
            groupId = "hr-performance-approval-group"
    )
    public void handleBpmProcessStatusChanged(BpmProcessInstanceStatusKafkaEvent event) {
        if (!Objects.equals(EmployeePerformanceServiceImpl.PROCESS_KEY, event.getProcessDefinitionKey())) {
            return;
        }

        Long tenantId = event.getTenantId() != null ? event.getTenantId() : TenantContextHolder.getTenantId();
        try {
            TenantUtils.execute(tenantId, () -> {
                try {
                    if (event.getBusinessKey() == null || event.getBusinessKey().trim().isEmpty()) {
                        log.warn("绩效审批 BPM 回调 businessKey 为空: processInstanceId={}", event.getProcessInstanceId());
                        return;
                    }
                    Long performanceId = Long.parseLong(event.getBusinessKey());
                    employeePerformanceService.updateApprovalStatusByBpmEvent(
                            performanceId, event.getProcessInstanceId(), event.getStatus(), event.getUserId());
                } catch (NumberFormatException ex) {
                    log.error("解析绩效审批业务编号失败: businessKey={}", event.getBusinessKey(), ex);
                }
            });
        } catch (Exception ex) {
            log.error("处理绩效审批流程状态变更失败: event={}", event, ex);
        }
    }

}
