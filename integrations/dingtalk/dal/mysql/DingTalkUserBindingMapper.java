package com.kyx.service.hr.dal.mysql.integration;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.dal.dataobject.integration.DingTalkUserBindingDO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface DingTalkUserBindingMapper extends BaseMapperX<DingTalkUserBindingDO> {

    default DingTalkUserBindingDO selectByDingUserId(String dingUserId) {
        if (!StringUtils.hasText(dingUserId)) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<DingTalkUserBindingDO>()
                .eq(DingTalkUserBindingDO::getDingUserId, dingUserId.trim())
                .orderByDesc(DingTalkUserBindingDO::getId)
                .last("LIMIT 1"));
    }

    default DingTalkUserBindingDO selectByOaUserId(Long oaUserId) {
        if (oaUserId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<DingTalkUserBindingDO>()
                .eq(DingTalkUserBindingDO::getOaUserId, oaUserId)
                .orderByDesc(DingTalkUserBindingDO::getId)
                .last("LIMIT 1"));
    }

    default List<DingTalkUserBindingDO> selectListByOaUserIds(Collection<Long> oaUserIds) {
        if (oaUserIds == null || oaUserIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<DingTalkUserBindingDO>()
                .in(DingTalkUserBindingDO::getOaUserId, oaUserIds)
                .orderByDesc(DingTalkUserBindingDO::getDingActive)
                .orderByDesc(DingTalkUserBindingDO::getSyncTime)
                .orderByDesc(DingTalkUserBindingDO::getId));
    }

    default List<DingTalkUserBindingDO> selectListActive() {
        return selectList(new LambdaQueryWrapperX<DingTalkUserBindingDO>()
                .isNotNull(DingTalkUserBindingDO::getOaUserId)
                .isNotNull(DingTalkUserBindingDO::getDingUserId)
                .eq(DingTalkUserBindingDO::getDingActive, true)
                .orderByDesc(DingTalkUserBindingDO::getSyncTime)
                .orderByDesc(DingTalkUserBindingDO::getId));
    }

    default DingTalkUserBindingDO selectByDingUserName(String dingUserName) {
        if (!StringUtils.hasText(dingUserName)) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<DingTalkUserBindingDO>()
                .eq(DingTalkUserBindingDO::getDingUserName, dingUserName.trim())
                .orderByDesc(DingTalkUserBindingDO::getDingActive)
                .orderByDesc(DingTalkUserBindingDO::getSyncTime)
                .orderByDesc(DingTalkUserBindingDO::getId)
                .last("LIMIT 1"));
    }

    default List<DingTalkUserBindingDO> selectListByDingUserIds(Collection<String> dingUserIds) {
        if (dingUserIds == null || dingUserIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<DingTalkUserBindingDO>()
                .in(DingTalkUserBindingDO::getDingUserId, dingUserIds));
    }

    default List<DingTalkUserBindingDO> selectListAll() {
        return selectList(new LambdaQueryWrapperX<DingTalkUserBindingDO>()
                .isNotNull(DingTalkUserBindingDO::getDingUserId)
                .isNotNull(DingTalkUserBindingDO::getOaUserId));
    }

}
