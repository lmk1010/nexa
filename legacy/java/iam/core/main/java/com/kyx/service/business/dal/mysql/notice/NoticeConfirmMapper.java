package com.kyx.service.business.dal.mysql.notice;

import cn.hutool.core.collection.CollUtil;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.business.dal.dataobject.notice.NoticeConfirmDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface NoticeConfirmMapper extends BaseMapperX<NoticeConfirmDO> {

    default List<NoticeConfirmDO> selectListByNoticeId(Long noticeId) {
        if (noticeId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<NoticeConfirmDO>()
                .eq(NoticeConfirmDO::getNoticeId, noticeId)
                .orderByDesc(NoticeConfirmDO::getConfirmTime)
                .orderByDesc(NoticeConfirmDO::getLastRemindTime)
                .orderByAsc(NoticeConfirmDO::getUserId));
    }

    default List<NoticeConfirmDO> selectListByNoticeIds(Collection<Long> noticeIds) {
        if (CollUtil.isEmpty(noticeIds)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<NoticeConfirmDO>()
                .in(NoticeConfirmDO::getNoticeId, noticeIds));
    }

    default NoticeConfirmDO selectOneByUser(Long noticeId, Long userId, Integer userType) {
        return selectOne(new LambdaQueryWrapperX<NoticeConfirmDO>()
                .eq(NoticeConfirmDO::getNoticeId, noticeId)
                .eq(NoticeConfirmDO::getUserId, userId)
                .eq(NoticeConfirmDO::getUserType, userType));
    }

    default List<NoticeConfirmDO> selectListByUser(Collection<Long> noticeIds, Long userId, Integer userType) {
        if (CollUtil.isEmpty(noticeIds)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<NoticeConfirmDO>()
                .in(NoticeConfirmDO::getNoticeId, noticeIds)
                .eq(NoticeConfirmDO::getUserId, userId)
                .eq(NoticeConfirmDO::getUserType, userType));
    }

    default List<NoticeConfirmDO> selectUnconfirmedList(Long noticeId) {
        if (noticeId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<NoticeConfirmDO>()
                .eq(NoticeConfirmDO::getNoticeId, noticeId)
                .isNull(NoticeConfirmDO::getConfirmTime)
                .orderByAsc(NoticeConfirmDO::getUserId));
    }

    default int deleteByNoticeId(Long noticeId) {
        if (noticeId == null) {
            return 0;
        }
        return delete(new LambdaQueryWrapperX<NoticeConfirmDO>()
                .eq(NoticeConfirmDO::getNoticeId, noticeId));
    }

}
