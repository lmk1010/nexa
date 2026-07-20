package com.kyx.service.business.service.notice;

import java.util.Collection;
import java.util.Set;

/**
 * 通知公告阅读 Service 接口
 *
 * @author MK
 */
public interface NoticeReadService {

    /**
     * 获取用户已读公告编号
     *
     * @param userId 用户编号
     * @param userType 用户类型
     * @param noticeIds 公告编号集合
     * @return 已读公告编号集合
     */
    Set<Long> getReadNoticeIds(Long userId, Integer userType, Collection<Long> noticeIds);

    /**
     * 标记公告为已读
     *
     * @param noticeIds 公告编号集合
     * @param userId 用户编号
     * @param userType 用户类型
     * @return 标记数量
     */
    int markRead(Collection<Long> noticeIds, Long userId, Integer userType);

    /**
     * 标记全部公告为已读
     *
     * @param userId 用户编号
     * @param userType 用户类型
     * @return 标记数量
     */
    int markAllRead(Long userId, Integer userType);
}
