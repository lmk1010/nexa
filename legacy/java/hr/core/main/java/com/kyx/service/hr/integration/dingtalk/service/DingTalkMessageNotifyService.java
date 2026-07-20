package com.kyx.service.hr.integration.dingtalk.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.kyx.service.hr.config.DingTalkProperties;
import com.kyx.service.hr.dal.dataobject.integration.DingTalkUserBindingDO;
import com.kyx.service.hr.dal.mysql.integration.DingTalkUserBindingMapper;
import com.kyx.service.hr.integration.dingtalk.client.DingTalkOpenApiClient;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DingTalkMessageNotifyService {

    private static final String SEND_MESSAGE_API = "/topapi/message/corpconversation/asyncsend_v2";
    private static final String DEFAULT_TEST_CONTENT = "这是一条钉钉连通性校验消息";

    @Resource
    private DingTalkProperties dingTalkProperties;
    @Resource
    private DingTalkOpenApiClient dingTalkOpenApiClient;
    @Resource
    private DingTalkUserBindingMapper dingTalkUserBindingMapper;
    @Resource
    private DingTalkUserBindingService dingTalkUserBindingService;

    public TextSendResult sendTextToDingUserName(String dingUserName, String content) {
        DingTalkUserBindingDO binding = dingTalkUserBindingMapper.selectByDingUserName(dingUserName);
        if (binding == null || !StringUtils.hasText(binding.getDingUserId())) {
            throw new IllegalArgumentException("未找到钉钉用户绑定：" + dingUserName);
        }
        return sendTextToDingUserId(binding.getDingUserId(), content,
                binding.getDingUserName(), binding.getOaUserId());
    }

    public TextSendResult sendTextToDingUserId(String dingUserId, String content) {
        return sendTextToDingUserId(dingUserId, content, null, null);
    }

    public TextSendResult sendActionCardToOaUserId(Long oaUserId, String title, String markdown,
                                                    String singleTitle, String singleUrl) {
        String dingUserId = dingTalkUserBindingService.findDingUserIdByOaUserId(oaUserId);
        if (!StringUtils.hasText(dingUserId)) {
            throw new IllegalArgumentException("未找到OA用户的钉钉绑定：" + oaUserId);
        }
        return sendMessageToDingUserId(dingUserId, actionCardMsg(title, markdown, singleTitle, singleUrl), title, null, oaUserId);
    }

    public List<TextSendResult> sendActionCardToOaUserIds(Collection<Long> oaUserIds, String title, String markdown,
                                                          String singleTitle, String singleUrl) {
        List<TextSendResult> results = new ArrayList<>();
        if (oaUserIds == null || oaUserIds.isEmpty()) {
            return results;
        }
        for (Long oaUserId : oaUserIds) {
            results.add(sendActionCardToOaUserId(oaUserId, title, markdown, singleTitle, singleUrl));
        }
        return results;
    }

    public TextSendResult sendOaCardToOaUserId(Long oaUserId, String headText, String headColor, String title,
                                               String content, List<OaFormItem> form, String messageUrl,
                                               String pcMessageUrl) {
        return sendOaCardToOaUserId(oaUserId, headText, headColor, title, content, form, messageUrl, pcMessageUrl,
                null, null);
    }

    public TextSendResult sendOaCardToOaUserId(Long oaUserId, String headText, String headColor, String title,
                                               String content, List<OaFormItem> form, String messageUrl,
                                               String pcMessageUrl, OaStatusBar statusBar, OaRich rich) {
        String dingUserId = dingTalkUserBindingService.findDingUserIdByOaUserId(oaUserId);
        if (!StringUtils.hasText(dingUserId)) {
            throw new IllegalArgumentException("未找到OA用户的钉钉绑定：" + oaUserId);
        }
        return sendMessageToDingUserId(dingUserId,
                oaMsg(headText, headColor, title, content, form, messageUrl, pcMessageUrl, statusBar, rich),
                title, null, oaUserId);
    }

    public List<TextSendResult> sendOaCardToOaUserIds(Collection<Long> oaUserIds, String headText, String headColor,
                                                      String title, String content, List<OaFormItem> form,
                                                      String messageUrl, String pcMessageUrl) {
        return sendOaCardToOaUserIds(oaUserIds, headText, headColor, title, content, form, messageUrl, pcMessageUrl,
                null, null);
    }

    public List<TextSendResult> sendOaCardToOaUserIds(Collection<Long> oaUserIds, String headText, String headColor,
                                                      String title, String content, List<OaFormItem> form,
                                                      String messageUrl, String pcMessageUrl,
                                                      OaStatusBar statusBar, OaRich rich) {
        List<TextSendResult> results = new ArrayList<>();
        if (oaUserIds == null || oaUserIds.isEmpty()) {
            return results;
        }
        for (Long oaUserId : oaUserIds) {
            results.add(sendOaCardToOaUserId(oaUserId, headText, headColor, title, content, form, messageUrl, pcMessageUrl,
                    statusBar, rich));
        }
        return results;
    }

    public String buildDingTalkOpenAppUrl(String redirectUrl) {
        DingTalkProperties.App app = dingTalkProperties.getApp();
        if (!StringUtils.hasText(redirectUrl) || app == null
                || !StringUtils.hasText(app.getCorpId())
                || (!StringUtils.hasText(app.getAppId()) && !StringUtils.hasText(app.getAgentId()))) {
            return redirectUrl;
        }
        String appId = resolveOpenAppId(app);
        try {
            return "dingtalk://dingtalkclient/action/openapp"
                    + "?corpid=" + URLEncoder.encode(app.getCorpId().trim(), "UTF-8")
                    + "&container_type=work_platform"
                    + "&app_id=" + URLEncoder.encode(appId, "UTF-8")
                    + "&redirect_type=jump"
                    + "&redirect_url=" + URLEncoder.encode(redirectUrl.trim(), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException("URL encode DingTalk open app url failed", ex);
        }
    }

    private String resolveOpenAppId(DingTalkProperties.App app) {
        if (StringUtils.hasText(app.getAgentId())) {
            String agentId = app.getAgentId().trim();
            return agentId.startsWith("0_") ? agentId : "0_" + agentId;
        }
        return app.getAppId().trim();
    }

    private TextSendResult sendTextToDingUserId(String dingUserId, String content, String dingUserName, Long oaUserId) {
        if (!StringUtils.hasText(dingUserId)) {
            throw new IllegalArgumentException("钉钉用户ID不能为空");
        }
        String messageContent = StringUtils.hasText(content) ? content.trim() : DEFAULT_TEST_CONTENT;
        return sendMessageToDingUserId(dingUserId, textMsg(messageContent), messageContent, dingUserName, oaUserId);
    }

    private TextSendResult sendMessageToDingUserId(String dingUserId, Map<String, Object> msg,
                                                   String content, String dingUserName, Long oaUserId) {
        Map<String, Object> body = new HashMap<>();
        body.put("agent_id", resolveAgentId());
        body.put("userid_list", dingUserId.trim());
        body.put("msg", msg);
        JsonNode response = dingTalkOpenApiClient.postTopApiWithRetry(SEND_MESSAGE_API, body);
        TextSendResult result = new TextSendResult();
        result.setDingUserId(dingUserId.trim());
        result.setDingUserName(dingUserName);
        result.setOaUserId(oaUserId);
        result.setContent(content);
        result.setTaskId(readLong(response, "task_id"));
        result.setRequestId(readText(response, "request_id"));
        return result;
    }

    private Long resolveAgentId() {
        String agentId = dingTalkProperties.getApp() == null ? null : dingTalkProperties.getApp().getAgentId();
        if (!StringUtils.hasText(agentId)) {
            throw new IllegalStateException("缺少 dingtalk.app.agent-id 配置");
        }
        try {
            return Long.parseLong(agentId.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("非法 dingtalk.app.agent-id 配置：" + agentId, ex);
        }
    }

    private Map<String, Object> textMsg(String content) {
        Map<String, Object> msg = new HashMap<>();
        Map<String, Object> text = new HashMap<>();
        text.put("content", content);
        msg.put("msgtype", "text");
        msg.put("text", text);
        return msg;
    }

    private Map<String, Object> actionCardMsg(String title, String markdown, String singleTitle, String singleUrl) {
        Map<String, Object> msg = new HashMap<>();
        Map<String, Object> actionCard = new HashMap<>();
        actionCard.put("title", StringUtils.hasText(title) ? title.trim() : "需求通知");
        actionCard.put("markdown", StringUtils.hasText(markdown) ? markdown.trim() : "你有需求要处理");
        actionCard.put("single_title", StringUtils.hasText(singleTitle) ? singleTitle.trim() : "查看详情");
        actionCard.put("single_url", StringUtils.hasText(singleUrl) ? singleUrl.trim() : "");
        msg.put("msgtype", "action_card");
        msg.put("action_card", actionCard);
        return msg;
    }

    private Map<String, Object> oaMsg(String headText, String headColor, String title, String content,
                                      List<OaFormItem> form, String messageUrl, String pcMessageUrl,
                                      OaStatusBar statusBar, OaRich rich) {
        Map<String, Object> msg = new HashMap<>();
        Map<String, Object> oa = new HashMap<>();
        oa.put("message_url", StringUtils.hasText(messageUrl) ? messageUrl.trim() : "");
        oa.put("pc_message_url", StringUtils.hasText(pcMessageUrl) ? pcMessageUrl.trim() : "");

        Map<String, Object> head = new HashMap<>();
        head.put("bgcolor", StringUtils.hasText(headColor) ? headColor.trim() : "FF1677FF");
        head.put("text", StringUtils.hasText(headText) ? headText.trim() : "需求通知");
        oa.put("head", head);

        if (statusBar != null && StringUtils.hasText(statusBar.getStatusValue())) {
            Map<String, Object> status = new HashMap<>();
            status.put("status_value", statusBar.getStatusValue().trim());
            status.put("status_bg", StringUtils.hasText(statusBar.getStatusBg()) ? statusBar.getStatusBg().trim() : "0xFF1677FF");
            oa.put("status_bar", status);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("title", StringUtils.hasText(title) ? title.trim() : "需求通知");
        if (StringUtils.hasText(content)) {
            body.put("content", content.trim());
        }
        if (rich != null && (StringUtils.hasText(rich.getNum()) || StringUtils.hasText(rich.getUnit()))) {
            Map<String, Object> richItem = new HashMap<>();
            richItem.put("num", StringUtils.hasText(rich.getNum()) ? rich.getNum().trim() : "");
            richItem.put("unit", StringUtils.hasText(rich.getUnit()) ? rich.getUnit().trim() : "");
            body.put("rich", richItem);
        }
        if (form != null && !form.isEmpty()) {
            List<Map<String, Object>> formItems = new ArrayList<>();
            for (OaFormItem item : form) {
                if (item == null) {
                    continue;
                }
                Map<String, Object> formItem = new HashMap<>();
                formItem.put("key", StringUtils.hasText(item.getKey()) ? item.getKey().trim() : "");
                formItem.put("value", StringUtils.hasText(item.getValue()) ? item.getValue().trim() : "-");
                formItems.add(formItem);
            }
            body.put("form", formItems);
        }
        oa.put("body", body);

        msg.put("msgtype", "oa");
        msg.put("oa", oa);
        return msg;
    }

    private Long readLong(JsonNode node, String name) {
        JsonNode target = node == null ? null : node.path(name);
        if (target == null || target.isMissingNode() || target.isNull()) {
            return null;
        }
        if (target.isIntegralNumber()) {
            return target.longValue();
        }
        if (target.isTextual()) {
            try {
                return Long.parseLong(target.textValue());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String readText(JsonNode node, String name) {
        JsonNode target = node == null ? null : node.path(name);
        return target == null || target.isMissingNode() || target.isNull() ? null : target.asText();
    }

    @Data
    public static class TextSendResult {
        private String dingUserId;
        private String dingUserName;
        private Long oaUserId;
        private String content;
        private Long taskId;
        private String requestId;
    }

    @Data
    public static class OaFormItem {
        private final String key;
        private final String value;
    }

    @Data
    public static class OaStatusBar {
        private final String statusValue;
        private final String statusBg;
    }

    @Data
    public static class OaRich {
        private final String num;
        private final String unit;
    }
}
