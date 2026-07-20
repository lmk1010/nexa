package com.kyx.service.hr.service.payroll.listener;

import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.bpm.api.event.dto.BpmProcessInstanceStatusKafkaEvent;
import com.kyx.service.hr.config.KafkaTopicConfig;
import com.kyx.service.hr.service.payroll.PayrollService;
import com.kyx.service.hr.service.payroll.PayrollServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * Payroll BPM status listener.
 */
@Slf4j
@Component
public class HrPayrollKafkaStatusListener {

    @Resource
    private PayrollService payrollService;

    @KafkaListener(
            topics = KafkaTopicConfig.BPM_PROCESS_STATUS_TOPIC,
            groupId = "hr-payroll-group"
    )
    public void handleBpmProcessStatusChanged(BpmProcessInstanceStatusKafkaEvent event) {
        if (!Objects.equals(PayrollServiceImpl.PROCESS_KEY_PAYROLL_PUBLISH, event.getProcessDefinitionKey())) {
            return;
        }

        Long tenantId = event.getTenantId() != null ? event.getTenantId() : TenantContextHolder.getTenantId();
        try {
            TenantUtils.execute(tenantId, () -> {
                try {
                    if (event.getBusinessKey() == null || event.getBusinessKey().trim().isEmpty()) {
                        log.warn("发薪审批 BPM 回调 businessKey 为空: processInstanceId={}", event.getProcessInstanceId());
                        return;
                    }
                    Long batchId = Long.parseLong(event.getBusinessKey());
                    payrollService.updatePublishApprovalStatusByBpmEvent(batchId,
                            event.getProcessInstanceId(), event.getStatus(), event.getUserId());
                } catch (NumberFormatException ex) {
                    log.error("解析发薪审批业务编号失败: businessKey={}", event.getBusinessKey(), ex);
                }
            });
        } catch (Exception ex) {
            log.error("处理发薪审批流程状态变更失败: event={}", event, ex);
        }
    }

}
