package com.kyx.service.hr.service.attendance.listener;

import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.bpm.api.event.dto.BpmProcessInstanceStatusKafkaEvent;
import com.kyx.service.hr.config.KafkaTopicConfig;
import com.kyx.service.hr.service.attendance.AttendanceCorrectionService;
import com.kyx.service.hr.service.attendance.AttendanceCorrectionServiceImpl;
import com.kyx.service.hr.service.attendance.AttendanceOvertimeService;
import com.kyx.service.hr.service.attendance.AttendanceOvertimeServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * Attendance BPM status listener.
 */
@Slf4j
@Component
public class HrAttendanceKafkaStatusListener {

    @Resource
    private AttendanceCorrectionService attendanceCorrectionService;
    @Resource
    private AttendanceOvertimeService attendanceOvertimeService;

    @KafkaListener(
            topics = KafkaTopicConfig.BPM_PROCESS_STATUS_TOPIC,
            groupId = "hr-attendance-approval-group"
    )
    public void handleBpmProcessStatusChanged(BpmProcessInstanceStatusKafkaEvent event) {
        if (!isAttendanceProcess(event.getProcessDefinitionKey())) {
            return;
        }

        Long tenantId = event.getTenantId() != null ? event.getTenantId() : TenantContextHolder.getTenantId();
        try {
            TenantUtils.execute(tenantId, () -> {
                try {
                    if (event.getBusinessKey() == null || event.getBusinessKey().trim().isEmpty()) {
                        log.warn("考勤审批 BPM 回调 businessKey 为空: processInstanceId={}", event.getProcessInstanceId());
                        return;
                    }
                    Long businessId = Long.parseLong(event.getBusinessKey());
                    if (Objects.equals(AttendanceCorrectionServiceImpl.PROCESS_KEY, event.getProcessDefinitionKey())) {
                        attendanceCorrectionService.updateApprovalStatusByBpmEvent(
                                businessId, event.getProcessInstanceId(), event.getStatus(), event.getUserId());
                        return;
                    }
                    attendanceOvertimeService.updateApprovalStatusByBpmEvent(
                            businessId, event.getProcessInstanceId(), event.getStatus(), event.getUserId());
                } catch (NumberFormatException ex) {
                    log.error("解析考勤审批业务编号失败: businessKey={}", event.getBusinessKey(), ex);
                }
            });
        } catch (Exception ex) {
            log.error("处理考勤审批流程状态变更失败: event={}", event, ex);
        }
    }

    private boolean isAttendanceProcess(String processDefinitionKey) {
        return Objects.equals(AttendanceCorrectionServiceImpl.PROCESS_KEY, processDefinitionKey)
                || Objects.equals(AttendanceOvertimeServiceImpl.PROCESS_KEY, processDefinitionKey);
    }

}
