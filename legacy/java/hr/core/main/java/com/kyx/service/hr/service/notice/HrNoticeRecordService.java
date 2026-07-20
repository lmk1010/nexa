package com.kyx.service.hr.service.notice;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.notice.vo.HrNoticeRecordPageReqVO;
import com.kyx.service.hr.controller.admin.notice.vo.HrNoticeRecordRespVO;

public interface HrNoticeRecordService {

    String CHANNEL_IN_APP = "IN_APP";
    String CHANNEL_DINGTALK = "DINGTALK";

    String STATUS_PENDING = "PENDING";
    String STATUS_SUCCESS = "SUCCESS";
    String STATUS_FAILED = "FAILED";
    String STATUS_SKIPPED = "SKIPPED";

    PageResult<HrNoticeRecordRespVO> getPage(HrNoticeRecordPageReqVO pageReqVO);

    String buildNoticeKey(String channel, String businessType, Long businessId, Long receiverUserId);

    void recordSuccess(String noticeKey, String channel, String businessType, Long businessId,
                       Long receiverUserId, String title, String content, String remark);

    void recordFailure(String noticeKey, String channel, String businessType, Long businessId,
                       Long receiverUserId, String title, String content, String errorMessage, String remark);

    void retry(Long id);

}
