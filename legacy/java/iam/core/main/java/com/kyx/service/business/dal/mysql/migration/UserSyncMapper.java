package com.kyx.service.business.dal.mysql.migration;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.business.dal.dataobject.migration.UserSyncDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 用户同步迁移 Mapper
 * 
 * @author MK
 */
@Mapper
public interface UserSyncMapper extends BaseMapperX<UserSyncDO> {

    /**
     * 根据外部用户ID查询
     */
    default UserSyncDO selectByExternalUserId(String externalUserId) {
        return selectOne(UserSyncDO::getExternalUserId, externalUserId);
    }

    /**
     * 根据用户名查询
     */
    default UserSyncDO selectByUsername(String username) {
        return selectOne(UserSyncDO::getUsername, username);
    }

    /**
     * 根据同步状态查询列表
     */
    default List<UserSyncDO> selectListBySyncStatus(Integer syncStatus) {
        return selectList(UserSyncDO::getSyncStatus, syncStatus);
    }

    /**
     * 查询待同步的用户列表
     */
    default List<UserSyncDO> selectPendingSyncList() {
        return selectList(new LambdaQueryWrapperX<UserSyncDO>()
                .eq(UserSyncDO::getSyncStatus, UserSyncDO.SyncStatus.PENDING.getCode())
                .orderByAsc(UserSyncDO::getCreateTime));
    }

    /**
     * 根据ID列表查询用户同步数据
     */
    default List<UserSyncDO> selectListByIds(List<Long> ids) {
        return selectList(new LambdaQueryWrapperX<UserSyncDO>()
                .in(UserSyncDO::getId, ids));
    }

    default List<UserSyncDO> selectListByExternalUserIds(Collection<String> externalUserIds) {
        if (externalUserIds == null || externalUserIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<UserSyncDO>()
                .in(UserSyncDO::getExternalUserId, externalUserIds));
    }
}
