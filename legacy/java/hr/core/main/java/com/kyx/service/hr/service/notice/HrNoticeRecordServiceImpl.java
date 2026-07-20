package com.kyx.service.hr.service.notice;

import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.exception.enums.GlobalErrorCodeConstants;
import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.web.config.WebProperties;
import com.kyx.service.business.api.notice.NoticeApi;
import com.kyx.service.business.api.notice.dto.NoticeCreateReqDTO;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import com.kyx.service.business.enums.notice.NoticeTypeEnum;
import com.kyx.service.hr.controller.admin.notice.vo.HrNoticeRecordPageReqVO;
import com.kyx.service.hr.controller.admin.notice.vo.HrNoticeRecordRespVO;
import com.kyx.service.hr.dal.dataobject.notice.HrNoticeRecordDO;
import com.kyx.service.hr.dal.mysql.notice.HrNoticeRecordMapper;
import com.kyx.service.hr.integration.dingtalk.service.DingTalkMessageNotifyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Validated
@Slf4j
public class HrNoticeRecordServiceImpl implements HrNoticeRecordService {

    private static final int TEXT_LIMIT = 1000;
    private static final String DEFAULT_DINGTALK_DETAIL_PATH = "/sync/dingtalk";
    private static final String DINGTALK_RETRY_HEAD_TEXT = "通知重试";
    private static final String DINGTALK_RETRY_HEAD_COLOR = "FF1677FF";
    private static final String DINGTALK_RETRY_STATUS_VALUE = "重试中";
    private static final String DINGTALK_RETRY_STATUS_COLOR = "0xFF1677FF";
    private static final DateTimeFormatter SEND_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Resource
    private HrNoticeRecordMapper noticeRecordMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private NoticeApi noticeApi;
    @Resource
    private DingTalkMessageNotifyService dingTalkMessageNotifyService;
    @Resource
    private WebProperties webProperties;

    @Override
    public PageResult<HrNoticeRecordRespVO> getPage(HrNoticeRecordPageReqVO pageReqVO) {
        PageResult<HrNoticeRecordDO> pageResult = noticeRecordMapper.selectPage(pageReqVO);
        PageResult<HrNoticeRecordRespVO> result = BeanUtils.toBean(pageResult, HrNoticeRecordRespVO.class);
        List<HrNoticeRecordRespVO> records = result.getList();
        if (records == null || records.isEmpty()) {
            return result;
        }
        Map<Long, AdminUserRespDTO> userMap = loadUserMap(records);
        for (HrNoticeRecordRespVO record : records) {
            if (record.getReceiverUserId() == null) {
                record.setReceiverName(CHANNEL_IN_APP.equals(record.getChannel()) ? "全员" : "-");
                continue;
            }
            AdminUserRespDTO user = userMap.get(record.getReceiverUserId());
            String name = user == null ? null
                    : (StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
            record.setReceiverName(StringUtils.hasText(name) ? name : "用户 #" + record.getReceiverUserId());
        }
        return result;
    }

    @Override
    public String buildNoticeKey(String channel, String businessType, Long businessId, Long receiverUserId) {
        return String.join(":",
                normalizeKey(channel),
                normalizeKey(businessType),
                normalizeKey(businessId),
                normalizeKey(receiverUserId),
                UUID.randomUUID().toString().replace("-", ""));
    }

    @Override
    public void recordSuccess(String noticeKey, String channel, String businessType, Long businessId,
                              Long receiverUserId, String title, String content, String remark) {
        upsertRecord(noticeKey, channel, businessType, businessId, receiverUserId,
                title, content, STATUS_SUCCESS, null, remark);
    }

    @Override
    public void recordFailure(String noticeKey, String channel, String businessType, Long businessId,
                              Long receiverUserId, String title, String content, String errorMessage, String remark) {
        upsertRecord(noticeKey, channel, businessType, businessId, receiverUserId,
                title, content, STATUS_FAILED, errorMessage, remark);
    }

    @Override
    public void retry(Long id) {
        HrNoticeRecordDO record = noticeRecordMapper.selectById(id);
        if (record == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND, "通知记录不存在");
        }
        if (!STATUS_FAILED.equals(record.getSendStatus())) {
            return;
        }
        try {
            sendAgain(record);
            recordSuccess(record.getNoticeKey(), record.getChannel(), record.getBusinessType(), record.getBusinessId(),
                    record.getReceiverUserId(), record.getTitle(), record.getContent(), record.getRemark());
        } catch (Exception ex) {
            try {
                recordFailure(record.getNoticeKey(), record.getChannel(), record.getBusinessType(), record.getBusinessId(),
                        record.getReceiverUserId(), record.getTitle(), record.getContent(), ex.getMessage(), record.getRemark());
            } catch (Exception recordEx) {
                log.warn("Persist notice retry failure failed, id={}, reason={}", id, recordEx.getMessage());
            }
            throw ex instanceof RuntimeException ? (RuntimeException) ex
                    : new IllegalStateException("通知重试失败", ex);
        }
    }

    private void upsertRecord(String noticeKey, String channel, String businessType, Long businessId,
                              Long receiverUserId, String title, String content, String sendStatus,
                              String errorMessage, String remark) {
        if (!StringUtils.hasText(noticeKey)) {
            noticeKey = buildNoticeKey(channel, businessType, businessId, receiverUserId);
        }
        HrNoticeRecordDO existing = noticeRecordMapper.selectByNoticeKey(noticeKey);
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            HrNoticeRecordDO insertDO = new HrNoticeRecordDO();
            insertDO.setNoticeKey(noticeKey);
            insertDO.setChannel(channel);
            insertDO.setBusinessType(businessType);
            insertDO.setBusinessId(businessId);
            insertDO.setReceiverUserId(receiverUserId);
            insertDO.setTitle(limit(title, 200));
            insertDO.setContent(limit(content, TEXT_LIMIT));
            insertDO.setSendStatus(sendStatus);
            insertDO.setSendTime(now);
            insertDO.setErrorMessage(limit(errorMessage, TEXT_LIMIT));
            insertDO.setRetryCount(0);
            insertDO.setRemark(limit(remark, 500));
            noticeRecordMapper.insert(insertDO);
            return;
        }

        HrNoticeRecordDO updateDO = new HrNoticeRecordDO();
        updateDO.setId(existing.getId());
        updateDO.setNoticeKey(existing.getNoticeKey());
        updateDO.setChannel(StringUtils.hasText(channel) ? channel : existing.getChannel());
        updateDO.setBusinessType(StringUtils.hasText(businessType) ? businessType : existing.getBusinessType());
        updateDO.setBusinessId(businessId == null ? existing.getBusinessId() : businessId);
        updateDO.setReceiverUserId(receiverUserId == null ? existing.getReceiverUserId() : receiverUserId);
        updateDO.setTitle(limit(StringUtils.hasText(title) ? title : existing.getTitle(), 200));
        updateDO.setContent(limit(StringUtils.hasText(content) ? content : existing.getContent(), TEXT_LIMIT));
        updateDO.setSendStatus(sendStatus);
        updateDO.setSendTime(now);
        updateDO.setErrorMessage(limit(errorMessage, TEXT_LIMIT));
        updateDO.setRetryCount((existing.getRetryCount() == null ? 0 : existing.getRetryCount()) + 1);
        updateDO.setRemark(limit(StringUtils.hasText(remark) ? remark : existing.getRemark(), 500));
        noticeRecordMapper.updateById(updateDO);
    }

    private void sendAgain(HrNoticeRecordDO record) {
        if (CHANNEL_IN_APP.equals(record.getChannel())) {
            retryInApp(record);
            return;
        }
        if (CHANNEL_DINGTALK.equals(record.getChannel())) {
            retryDingTalk(record);
            return;
        }
        throw new IllegalStateException("不支持的通知渠道：" + record.getChannel());
    }

    private void retryInApp(HrNoticeRecordDO record) {
        NoticeCreateReqDTO reqDTO = new NoticeCreateReqDTO();
        reqDTO.setTitle(record.getTitle());
        reqDTO.setType(NoticeTypeEnum.ANNOUNCEMENT.getType());
        reqDTO.setContent(record.getContent());
        reqDTO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        noticeApi.createNotice(reqDTO);
    }

    private void retryDingTalk(HrNoticeRecordDO record) {
        if (record.getReceiverUserId() == null) {
            throw new IllegalStateException("钉钉通知缺少接收人，无法重试");
        }
        String detailUrl = resolveDetailUrl(record.getRemark());
        String messageUrl = dingTalkMessageNotifyService.buildDingTalkOpenAppUrl(detailUrl);
        List<DingTalkMessageNotifyService.OaFormItem> form = new ArrayList<>();
        form.add(new DingTalkMessageNotifyService.OaFormItem("业务类型", empty(record.getBusinessType())));
        form.add(new DingTalkMessageNotifyService.OaFormItem("接收人ID", String.valueOf(record.getReceiverUserId())));
        form.add(new DingTalkMessageNotifyService.OaFormItem("重试时间", LocalDateTime.now().format(SEND_TIME_FORMATTER)));
        form.add(new DingTalkMessageNotifyService.OaFormItem("重试次数",
                String.valueOf((record.getRetryCount() == null ? 0 : record.getRetryCount()) + 1)));
        DingTalkMessageNotifyService.OaStatusBar statusBar =
                new DingTalkMessageNotifyService.OaStatusBar(DINGTALK_RETRY_STATUS_VALUE, DINGTALK_RETRY_STATUS_COLOR);
        dingTalkMessageNotifyService.sendOaCardToOaUserId(record.getReceiverUserId(), DINGTALK_RETRY_HEAD_TEXT,
                DINGTALK_RETRY_HEAD_COLOR, record.getTitle(), record.getContent(), form, messageUrl, detailUrl,
                statusBar, null);
    }

    private Map<Long, AdminUserRespDTO> loadUserMap(List<HrNoticeRecordRespVO> records) {
        Set<Long> userIds = new LinkedHashSet<>();
        for (HrNoticeRecordRespVO record : records) {
            if (record.getReceiverUserId() != null) {
                userIds.add(record.getReceiverUserId());
            }
        }
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        try {
            return adminUserApi.getUserMap(userIds);
        } catch (Exception ex) {
            log.warn("Load notice receiver users failed, userIds={}, reason={}", userIds, ex.getMessage());
            return new HashMap<>();
        }
    }

    private String resolveDetailUrl(String remark) {
        if (StringUtils.hasText(remark) && remark.trim().startsWith("http")) {
            return remark.trim();
        }
        String adminUiUrl = webProperties == null || webProperties.getAdminUi() == null
                ? null : webProperties.getAdminUi().getUrl();
        if (!StringUtils.hasText(adminUiUrl)) {
            return "";
        }
        return trimTrailingSlash(adminUiUrl.trim()) + DEFAULT_DINGTALK_DETAIL_PATH;
    }

    private String trimTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String normalizeKey(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private String limit(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String text = value.trim();
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private String empty(String value) {
        return value == null ? "-" : value;
    }

}
