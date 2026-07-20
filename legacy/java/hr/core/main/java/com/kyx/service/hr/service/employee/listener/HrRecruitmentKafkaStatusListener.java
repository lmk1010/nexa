package com.kyx.service.hr.service.employee.listener;

import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.bpm.api.event.dto.BpmProcessInstanceStatusKafkaEvent;
import com.kyx.service.hr.config.KafkaTopicConfig;
import com.kyx.service.hr.service.employee.EmployeeRecruitmentService;
import com.kyx.service.hr.service.employee.EmployeeRecruitmentServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;

@Slf4j
@Component
public class HrRecruitmentKafkaStatusListener {

    @Resource
    private EmployeeRecruitmentService employeeRecruitmentService;

    @KafkaListener(
            topics = KafkaTopicConfig.BPM_PROCESS_STATUS_TOPIC,
            groupId = "hr-recruitment-group"
    )
    public void handleBpmProcessStatusChanged(BpmProcessInstanceStatusKafkaEvent event) {
        if (!isRecruitmentProcess(event.getProcessDefinitionKey())) {
            return;
        }

        Long tenantId = event.getTenantId() != null ? event.getTenantId() : TenantContextHolder.getTenantId();
        try {
            TenantUtils.execute(tenantId, () -> {
                try {
                    if (event.getBusinessKey() == null || event.getBusinessKey().trim().isEmpty()) {
                        log.warn("招聘审批 BPM 回调 businessKey 为空: processInstanceId={}", event.getProcessInstanceId());
                        return;
                    }
                    Long recruitmentId = Long.parseLong(event.getBusinessKey());
                    employeeRecruitmentService.updateApprovalStatusByBpmEvent(recruitmentId,
                            event.getProcessDefinitionKey(), event.getProcessInstanceId(),
                            event.getStatus(), event.getUserId());
                } catch (NumberFormatException ex) {
                    log.error("解析招聘审批业务编号失败: businessKey={}", event.getBusinessKey(), ex);
                }
            });
        } catch (Exception ex) {
            log.error("处理招聘审批流程状态变更失败: event={}", event, ex);
        }
    }

    private boolean isRecruitmentProcess(String processDefinitionKey) {
        return Objects.equals(EmployeeRecruitmentServiceImpl.PROCESS_KEY_RECRUITMENT_DEMAND, processDefinitionKey)
                || Objects.equals(EmployeeRecruitmentServiceImpl.PROCESS_KEY_RECRUITMENT_OFFER, processDefinitionKey);
    }
}
