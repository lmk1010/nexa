package com.kyx.service.business.service.notice;

import cn.hutool.core.collection.CollUtil;
import com.kyx.service.business.dal.dataobject.notice.NoticeReadDO;
import com.kyx.service.business.dal.mysql.notice.NoticeMapper;
import com.kyx.service.business.dal.mysql.notice.NoticeReadMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 通知公告阅读 Service 实现类
 *
 * @author MK
 */
@Service
@Validated
public class NoticeReadServiceImpl implements NoticeReadService {

    @Resource
    private NoticeReadMapper noticeReadMapper;
    @Resource
    private NoticeMapper noticeMapper;

    @Override
    public Set<Long> getReadNoticeIds(Long userId, Integer userType, Collection<Long> noticeIds) {
        if (CollUtil.isEmpty(noticeIds)) {
            return new HashSet<>();
        }
        List<Long> readIds = noticeReadMapper.selectReadNoticeIds(userId, userType, noticeIds);
        return new HashSet<>(readIds);
    }

    @Override
    public int markRead(Collection<Long> noticeIds, Long userId, Integer userType) {
        if (CollUtil.isEmpty(noticeIds)) {
            return 0;
        }
        List<Long> distinctIds = noticeIds.stream().distinct().collect(Collectors.toList());
        List<Long> visibleIds = noticeMapper.selectVisibleNoticeIds(userId, userType, distinctIds);
        if (CollUtil.isEmpty(visibleIds)) {
            return 0;
        }
        Set<Long> readIds = getReadNoticeIds(userId, userType, visibleIds);
        LocalDateTime now = LocalDateTime.now();
        List<NoticeReadDO> inserts = visibleIds.stream()
                .filter(id -> !readIds.contains(id))
                .map(id -> new NoticeReadDO().setNoticeId(id)
                        .setUserId(userId)
                        .setUserType(userType)
                        .setReadTime(now))
                .collect(Collectors.toList());
        if (inserts.isEmpty()) {
            return 0;
        }
        noticeReadMapper.insertBatch(inserts);
        return inserts.size();
    }

    @Override
    public int markAllRead(Long userId, Integer userType) {
        List<Long> unreadIds = noticeMapper.selectUnreadNoticeIds(userId, userType);
        return markRead(unreadIds, userId, userType);
    }
}
