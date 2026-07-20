package com.kyx.service.business.service.notice;

import cn.hutool.core.collection.CollUtil;
import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.enums.UserTypeEnum;
import com.kyx.foundation.common.util.collection.CollectionUtils;
import com.kyx.service.business.dal.dataobject.notice.NoticeConfirmDO;
import com.kyx.service.business.dal.dataobject.notice.NoticeDO;
import com.kyx.service.business.dal.dataobject.user.AdminUserDO;
import com.kyx.service.business.dal.mysql.notice.NoticeConfirmMapper;
import com.kyx.service.business.dal.mysql.notice.NoticeMapper;
import com.kyx.service.business.service.notice.dto.NoticeConfirmStatsDTO;
import com.kyx.service.business.service.user.AdminUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Validated
public class NoticeConfirmServiceImpl implements NoticeConfirmService {

    @Resource
    private NoticeConfirmMapper noticeConfirmMapper;
    @Resource
    private NoticeMapper noticeMapper;
    @Resource
    private AdminUserService adminUserService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ensureNoticeConfirmReceipts(Long noticeId) {
        if (noticeId == null) {
            return;
        }
        NoticeDO notice = noticeMapper.selectById(noticeId);
        if (notice == null) {
            return;
        }
        List<Long> userIds = resolveTargetUserIds(notice);
        Set<Long> targetUserIdSet = new HashSet<>(userIds);
        List<NoticeConfirmDO> existingList = noticeConfirmMapper.selectListByNoticeId(noticeId);
        List<Long> staleIds = existingList.stream()
                .filter(item -> Objects.equals(item.getUserType(), UserTypeEnum.ADMIN.getValue()))
                .filter(item -> !targetUserIdSet.contains(item.getUserId()))
                .map(NoticeConfirmDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(staleIds)) {
            noticeConfirmMapper.deleteBatchIds(staleIds);
        }
        if (CollUtil.isEmpty(userIds)) {
            return;
        }
        Set<Long> existingUserIds = existingList.stream()
                .filter(item -> Objects.equals(item.getUserType(), UserTypeEnum.ADMIN.getValue()))
                .filter(item -> targetUserIdSet.contains(item.getUserId()))
                .map(NoticeConfirmDO::getUserId)
                .collect(Collectors.toSet());
        List<NoticeConfirmDO> inserts = userIds.stream()
                .filter(userId -> !existingUserIds.contains(userId))
                .map(userId -> new NoticeConfirmDO()
                        .setNoticeId(noticeId)
                        .setUserId(userId)
                        .setUserType(UserTypeEnum.ADMIN.getValue())
                        .setRemindCount(0))
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(inserts)) {
            noticeConfirmMapper.insertBatch(inserts);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNoticeConfirmReceipts(Long noticeId) {
        noticeConfirmMapper.deleteByNoticeId(noticeId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int confirmNotice(Long noticeId, Long userId, Integer userType) {
        if (noticeId == null || userId == null || userType == null) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        NoticeConfirmDO confirm = noticeConfirmMapper.selectOneByUser(noticeId, userId, userType);
        if (confirm == null) {
            noticeConfirmMapper.insert(new NoticeConfirmDO()
                    .setNoticeId(noticeId)
                    .setUserId(userId)
                    .setUserType(userType)
                    .setConfirmTime(now)
                    .setRemindCount(0));
            return 1;
        }
        if (confirm.getConfirmTime() != null) {
            return 0;
        }
        confirm.setConfirmTime(now);
        return noticeConfirmMapper.updateById(confirm);
    }

    @Override
    public Map<Long, NoticeConfirmDO> getUserConfirmMap(Collection<Long> noticeIds, Long userId, Integer userType) {
        if (CollUtil.isEmpty(noticeIds) || userId == null || userType == null) {
            return Collections.emptyMap();
        }
        return noticeConfirmMapper.selectListByUser(distinctIds(noticeIds), userId, userType).stream()
                .collect(Collectors.toMap(NoticeConfirmDO::getNoticeId, item -> item, (oldValue, newValue) -> oldValue));
    }

    @Override
    public Map<Long, NoticeConfirmStatsDTO> getStatsMap(Collection<Long> noticeIds) {
        List<Long> distinctIds = distinctIds(noticeIds);
        if (CollUtil.isEmpty(distinctIds)) {
            return Collections.emptyMap();
        }
        Map<Long, NoticeDO> noticeMap = CollectionUtils.convertMap(noticeMapper.selectBatchIds(distinctIds),
                NoticeDO::getId);
        Map<Long, NoticeConfirmStatsDTO> result = new HashMap<>();
        distinctIds.forEach(id -> result.put(id, new NoticeConfirmStatsDTO().setNoticeId(id)));
        LocalDateTime now = LocalDateTime.now();
        for (NoticeConfirmDO item : noticeConfirmMapper.selectListByNoticeIds(distinctIds)) {
            NoticeConfirmStatsDTO stats = result.computeIfAbsent(item.getNoticeId(),
                    id -> new NoticeConfirmStatsDTO().setNoticeId(id));
            stats.setConfirmTotalCount(stats.getConfirmTotalCount() + 1);
            if (item.getConfirmTime() == null) {
                stats.setUnconfirmedCount(stats.getUnconfirmedCount() + 1);
                NoticeDO notice = noticeMap.get(item.getNoticeId());
                if (notice != null && notice.getConfirmDeadline() != null && notice.getConfirmDeadline().isBefore(now)) {
                    stats.setOverdueCount(stats.getOverdueCount() + 1);
                }
            } else {
                stats.setConfirmedCount(stats.getConfirmedCount() + 1);
            }
        }
        return result;
    }

    @Override
    public List<NoticeConfirmDO> getConfirmDetailList(Long noticeId) {
        return noticeConfirmMapper.selectListByNoticeId(noticeId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<NoticeConfirmDO> remindUnconfirmed(Long noticeId) {
        List<NoticeConfirmDO> unconfirmedList = noticeConfirmMapper.selectUnconfirmedList(noticeId);
        if (CollUtil.isEmpty(unconfirmedList)) {
            return Collections.emptyList();
        }
        LocalDateTime now = LocalDateTime.now();
        for (NoticeConfirmDO item : unconfirmedList) {
            item.setRemindCount(item.getRemindCount() == null ? 1 : item.getRemindCount() + 1);
            item.setLastRemindTime(now);
            noticeConfirmMapper.updateById(item);
        }
        return unconfirmedList;
    }

    private static List<Long> distinctIds(Collection<Long> noticeIds) {
        if (CollUtil.isEmpty(noticeIds)) {
            return Collections.emptyList();
        }
        Set<Long> seen = new HashSet<>();
        return noticeIds.stream()
                .filter(Objects::nonNull)
                .filter(seen::add)
                .collect(Collectors.toList());
    }

    private List<Long> resolveTargetUserIds(NoticeDO notice) {
        if (notice == null) {
            return Collections.emptyList();
        }
        if (NoticeReceiverSupport.isUserReceiver(notice.getReceiverType())) {
            return NoticeReceiverSupport.parseUserIds(notice.getReceiverUserIds());
        }
        return adminUserService.getUserListByStatus(CommonStatusEnum.ENABLE.getStatus()).stream()
                .map(AdminUserDO::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

}
