package com.kyx.service.hr.integration.dingtalk.service;

import com.kyx.service.bpm.api.event.dto.BpmProcessInstanceStatusKafkaEvent;
import com.kyx.service.hr.config.DingTalkProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

/**
 * Sends OA approval status updates to DingTalk users.
 */
@Service
@Slf4j
public class DingTalkApprovalNotifyService {

    @Resource
    private DingTalkProperties dingTalkProperties;
    @Resource
    private DingTalkUserBindingService dingTalkUserBindingService;
    @Resource
    private DingTalkMessageNotifyService dingTalkMessageNotifyService;

    public void notifyProcessStatus(BpmProcessInstanceStatusKafkaEvent event) {
        if (!Boolean.TRUE.equals(dingTalkProperties.getNotify().getEnabled())) {
            return;
        }
        if (event == null || event.getUserId() == null) {
            return;
        }
        String dingUserId = resolveDingUserId(event.getUserId());
        if (!StringUtils.hasText(dingUserId)) {
            log.debug("Skip DingTalk notify, mapping not found for local userId={}", event.getUserId());
            return;
        }
        String agentId = trim(dingTalkProperties.getApp().getAgentId());
        if (!StringUtils.hasText(agentId)) {
            log.warn("Skip DingTalk notify, missing dingtalk.app.agent-id");
            return;
        }

        Long parsedAgentId = parseAgentId(agentId);
        if (parsedAgentId == null) {
            log.warn("Skip DingTalk notify, invalid dingtalk.app.agent-id={}", agentId);
            return;
        }
        dingTalkMessageNotifyService.sendTextToDingUserId(dingUserId, buildContent(event));
    }

    private String resolveDingUserId(Long localUserId) {
        return dingTalkUserBindingService.findDingUserIdByOaUserId(localUserId);
    }

    private String buildContent(BpmProcessInstanceStatusKafkaEvent event) {
        return String.format("OA审批状态变更：流程=%s，业务ID=%s，状态=%s",
                empty(event.getProcessDefinitionKey()),
                empty(event.getBusinessKey()),
                event.getStatus());
    }

    private String empty(String text) {
        return text == null ? "-" : text;
    }

    private String trim(String text) {
        return text == null ? null : text.trim();
    }

    private Long parseAgentId(String text) {
        try {
            return Long.parseLong(text);
        } catch (Exception ignored) {
            return null;
        }
    }

}
