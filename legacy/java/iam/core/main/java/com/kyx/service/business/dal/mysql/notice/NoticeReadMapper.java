package com.kyx.service.business.dal.mysql.notice;

import cn.hutool.core.collection.CollUtil;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.business.dal.dataobject.notice.NoticeReadDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mapper
public interface NoticeReadMapper extends BaseMapperX<NoticeReadDO> {

    default List<Long> selectReadNoticeIds(Long userId, Integer userType, Collection<Long> noticeIds) {
        if (CollUtil.isEmpty(noticeIds)) {
            return Collections.emptyList();
        }
        List<NoticeReadDO> reads = selectList(new LambdaQueryWrapperX<NoticeReadDO>()
                .select(NoticeReadDO::getNoticeId)
                .eq(NoticeReadDO::getUserId, userId)
                .eq(NoticeReadDO::getUserType, userType)
                .in(NoticeReadDO::getNoticeId, noticeIds));
        return reads.stream().map(NoticeReadDO::getNoticeId).collect(Collectors.toList());
    }

}
