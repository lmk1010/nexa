package com.kyx.service.business.dal.mysql.dept;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.service.business.dal.dataobject.dept.PostSyncDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 岗位同步 Mapper
 *
 * @author MK
 */
@Mapper
public interface PostSyncMapper extends BaseMapperX<PostSyncDO> {

    /**
     * 根据外部岗位ID查询同步记录
     */
    default PostSyncDO selectByExternalPostId(Long externalPostId) {
        return selectOne(PostSyncDO::getExternalPostId, externalPostId);
    }

    /**
     * 查询待同步的记录
     */
    default List<PostSyncDO> selectPendingSync() {
        return selectList(PostSyncDO::getSyncStatus, PostSyncDO.SyncStatus.PENDING.getValue());
    }

    /**
     * 查询所有同步记录
     */
    default List<PostSyncDO> selectAllSync() {
        return selectList();
    }

}