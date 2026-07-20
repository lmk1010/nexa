package com.kyx.service.business.service.notice;

import com.kyx.service.business.dal.dataobject.notice.NoticeConfirmDO;
import com.kyx.service.business.service.notice.dto.NoticeConfirmStatsDTO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface NoticeConfirmService {

    void ensureNoticeConfirmReceipts(Long noticeId);

    void deleteNoticeConfirmReceipts(Long noticeId);

    int confirmNotice(Long noticeId, Long userId, Integer userType);

    Map<Long, NoticeConfirmDO> getUserConfirmMap(Collection<Long> noticeIds, Long userId, Integer userType);

    Map<Long, NoticeConfirmStatsDTO> getStatsMap(Collection<Long> noticeIds);

    List<NoticeConfirmDO> getConfirmDetailList(Long noticeId);

    List<NoticeConfirmDO> remindUnconfirmed(Long noticeId);

}
