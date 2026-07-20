package com.kyx.service.bpm.service.message;

import com.kyx.foundation.common.util.http.HttpUtils;
import com.kyx.foundation.web.config.WebProperties;
import com.kyx.service.bpm.convert.message.BpmMessageConvert;
import com.kyx.service.bpm.enums.message.BpmMessageEnum;
import com.kyx.service.bpm.service.message.dto.BpmMessageSendWhenProcessInstanceApproveReqDTO;
import com.kyx.service.bpm.service.message.dto.BpmMessageSendWhenProcessInstanceRejectReqDTO;
import com.kyx.service.bpm.service.message.dto.BpmMessageSendWhenTaskCreatedReqDTO;
import com.kyx.service.bpm.service.message.dto.BpmMessageSendWhenTaskTimeoutReqDTO;
import com.kyx.service.business.api.notify.NotifyMessageSendApi;
import com.kyx.service.business.api.notify.dto.NotifySendSingleToUserReqDTO;
import com.kyx.service.business.api.sms.SmsSendApi;
import com.kyx.service.hr.api.dingtalk.DingTalkBpmNoticeApi;
import com.kyx.service.hr.api.dingtalk.dto.DingTalkBpmNoticeReqDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * BPM 消息 Service 实现类
 *
 * @author MK
 */
@Service
@Validated
@Slf4j
public class BpmMessageServiceImpl implements BpmMessageService {

    @Resource
    private SmsSendApi smsSendApi;
    @Resource
    private NotifyMessageSendApi notifyMessageSendApi;
    @Resource
    private DingTalkBpmNoticeApi dingTalkBpmNoticeApi;

    @Resource
    private WebProperties webProperties;

    @Override
    public void sendMessageWhenProcessInstanceApprove(BpmMessageSendWhenProcessInstanceApproveReqDTO reqDTO) {
        Map<String, Object> templateParams = new HashMap<>();
        templateParams.put("processInstanceName", reqDTO.getProcessInstanceName());
        templateParams.put("detailUrl", getProcessInstanceDetailUrl(reqDTO.getProcessInstanceId()));
        try {
            smsSendApi.sendSingleSmsToAdmin(BpmMessageConvert.INSTANCE.convert(reqDTO.getStartUserId(),
                    BpmMessageEnum.PROCESS_INSTANCE_APPROVE.getSmsTemplateCode(), templateParams)).checkError();
        } catch (Exception e) {
            log.warn("[sendMessageWhenProcessInstanceApprove][发送短信失败，流程实例：{}]", reqDTO.getProcessInstanceName(), e);
        }
        try {
            sendNotifyToAdmin(reqDTO.getStartUserId(), BpmMessageEnum.PROCESS_INSTANCE_APPROVE.getSmsTemplateCode(),
                    templateParams);
        } catch (Exception e) {
            log.warn("[sendMessageWhenProcessInstanceApprove][发送站内信失败，流程实例：{}]", reqDTO.getProcessInstanceName(), e);
        }
    }

    @Override
    public void sendMessageWhenProcessInstanceReject(BpmMessageSendWhenProcessInstanceRejectReqDTO reqDTO) {
        Map<String, Object> templateParams = new HashMap<>();
        templateParams.put("processInstanceName", reqDTO.getProcessInstanceName());
        templateParams.put("reason", reqDTO.getReason());
        templateParams.put("detailUrl", getProcessInstanceDetailUrl(reqDTO.getProcessInstanceId()));
        try {
            smsSendApi.sendSingleSmsToAdmin(BpmMessageConvert.INSTANCE.convert(reqDTO.getStartUserId(),
                    BpmMessageEnum.PROCESS_INSTANCE_REJECT.getSmsTemplateCode(), templateParams)).checkError();
        } catch (Exception e) {
            log.warn("[sendMessageWhenProcessInstanceReject][发送短信失败，流程实例：{}]", reqDTO.getProcessInstanceName(), e);
        }
        try {
            sendNotifyToAdmin(reqDTO.getStartUserId(), BpmMessageEnum.PROCESS_INSTANCE_REJECT.getSmsTemplateCode(),
                    templateParams);
        } catch (Exception e) {
            log.warn("[sendMessageWhenProcessInstanceReject][发送站内信失败，流程实例：{}]", reqDTO.getProcessInstanceName(), e);
        }
    }

    @Override
    public void sendMessageWhenTaskAssigned(BpmMessageSendWhenTaskCreatedReqDTO reqDTO) {
        Map<String, Object> templateParams = new HashMap<>();
        templateParams.put("processInstanceName", reqDTO.getProcessInstanceName());
        templateParams.put("taskName", reqDTO.getTaskName());
        templateParams.put("startUserNickname", reqDTO.getStartUserNickname());
        templateParams.put("detailUrl", getProcessInstanceDetailUrl(reqDTO.getProcessInstanceId(), reqDTO.getTaskId()));
        try {
            smsSendApi.sendSingleSmsToAdmin(BpmMessageConvert.INSTANCE.convert(reqDTO.getAssigneeUserId(),
                    BpmMessageEnum.TASK_ASSIGNED.getSmsTemplateCode(), templateParams)).checkError();
        } catch (Exception e) {
            log.warn("[sendMessageWhenTaskAssigned][发送短信失败，任务：{}]", reqDTO.getTaskName(), e);
        }
        try {
            sendNotifyToAdmin(reqDTO.getAssigneeUserId(), BpmMessageEnum.TASK_ASSIGNED.getSmsTemplateCode(),
                    templateParams);
        } catch (Exception e) {
            log.warn("[sendMessageWhenTaskAssigned][发送站内信失败，任务：{}]", reqDTO.getTaskName(), e);
        }
        sendDingTalkWhenTaskAssigned(reqDTO);
    }

    @Override
    public void sendDingTalkWhenTaskAssigned(BpmMessageSendWhenTaskCreatedReqDTO reqDTO) {
        try {
            dingTalkBpmNoticeApi.sendTaskTodo(new DingTalkBpmNoticeReqDTO()
                    .setReceiverUserIds(Collections.singletonList(reqDTO.getAssigneeUserId()))
                    .setProcessInstanceId(reqDTO.getProcessInstanceId())
                    .setProcessInstanceName(reqDTO.getProcessInstanceName())
                    .setStartUserId(reqDTO.getStartUserId())
                    .setStartUserNickname(reqDTO.getStartUserNickname())
                    .setTaskId(reqDTO.getTaskId())
                    .setTaskName(reqDTO.getTaskName())
                    .setDetailUrl(getProcessInstanceDetailUrl(reqDTO.getProcessInstanceId(), reqDTO.getTaskId()))
                    .setDedupBizId("bpm-task:" + reqDTO.getTaskId())).checkError();
        } catch (Exception e) {
            log.warn("[sendDingTalkWhenTaskAssigned][发送钉钉待办失败，任务：{}]", reqDTO.getTaskName(), e);
        }
    }

    @Override
    public void sendMessageWhenTaskTimeout(BpmMessageSendWhenTaskTimeoutReqDTO reqDTO) {
        Map<String, Object> templateParams = new HashMap<>();
        templateParams.put("processInstanceName", reqDTO.getProcessInstanceName());
        templateParams.put("taskName", reqDTO.getTaskName());
        templateParams.put("detailUrl", getProcessInstanceDetailUrl(reqDTO.getProcessInstanceId()));
        try {
            smsSendApi.sendSingleSmsToAdmin(BpmMessageConvert.INSTANCE.convert(reqDTO.getAssigneeUserId(),
                    BpmMessageEnum.TASK_TIMEOUT.getSmsTemplateCode(), templateParams)).checkError();
        } catch (Exception e) {
            log.warn("[sendMessageWhenTaskTimeout][发送短信失败，任务：{}]", reqDTO.getTaskName(), e);
        }
        try {
            sendNotifyToAdmin(reqDTO.getAssigneeUserId(), BpmMessageEnum.TASK_TIMEOUT.getSmsTemplateCode(),
                    templateParams);
        } catch (Exception e) {
            log.warn("[sendMessageWhenTaskTimeout][发送站内信失败，任务：{}]", reqDTO.getTaskName(), e);
        }
    }

    private String getProcessInstanceDetailUrl(String processInstanceId) {
        return getProcessInstanceDetailUrl(processInstanceId, null);
    }

    private String getProcessInstanceDetailUrl(String processInstanceId, String taskId) {
        StringBuilder url = new StringBuilder(webProperties.getAdminUi().getUrl())
                .append("/bpm/process-instance/detail?id=")
                .append(encodeQueryValue(processInstanceId));
        if (taskId != null && !taskId.trim().isEmpty()) {
            url.append("&taskId=").append(encodeQueryValue(taskId));
        }
        return url.toString();
    }

    private String encodeQueryValue(String value) {
        return HttpUtils.encodeUtf8(value == null ? "" : value);
    }

    private void sendNotifyToAdmin(Long userId, String templateCode, Map<String, Object> templateParams) {
        if (userId == null) {
            return;
        }
        NotifySendSingleToUserReqDTO reqDTO = new NotifySendSingleToUserReqDTO();
        reqDTO.setUserId(userId);
        reqDTO.setTemplateCode(templateCode);
        reqDTO.setTemplateParams(templateParams);
        notifyMessageSendApi.sendSingleMessageToAdmin(reqDTO).checkError();
    }

}
