package com.kyx.service.im.dal.mysql.tencent;

import cn.hutool.core.util.StrUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.im.controller.admin.tencent.vo.TencentImUserMappingPageReqVO;
import com.kyx.service.im.dal.dataobject.tencent.TencentImUserMappingDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TencentImUserMappingMapper extends BaseMapperX<TencentImUserMappingDO> {

    default TencentImUserMappingDO selectByOaUser(Long tenantId, Long oaUserId) {
        return selectOne(new LambdaQueryWrapperX<TencentImUserMappingDO>()
                .eq(TencentImUserMappingDO::getTenantId, tenantId)
                .eq(TencentImUserMappingDO::getOaUserId, oaUserId));
    }

    default TencentImUserMappingDO selectActiveByOaUser(Long tenantId, Long oaUserId) {
        return selectOne(new LambdaQueryWrapperX<TencentImUserMappingDO>()
                .eq(TencentImUserMappingDO::getTenantId, tenantId)
                .eq(TencentImUserMappingDO::getOaUserId, oaUserId)
                .eq(TencentImUserMappingDO::getStatus, 0));
    }

    default TencentImUserMappingDO selectActiveByOaUser(Long oaUserId) {
        return selectOne(new LambdaQueryWrapperX<TencentImUserMappingDO>()
                .eq(TencentImUserMappingDO::getOaUserId, oaUserId)
                .eq(TencentImUserMappingDO::getStatus, 0)
                .last("LIMIT 1"));
    }

    default TencentImUserMappingDO selectByImUserId(String imUserId) {
        return selectOne(TencentImUserMappingDO::getImUserId, imUserId);
    }

    default PageResult<TencentImUserMappingDO> selectPage(TencentImUserMappingPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TencentImUserMappingDO>()
                .eqIfPresent(TencentImUserMappingDO::getTenantId, reqVO.getTenantId())
                .eqIfPresent(TencentImUserMappingDO::getOaUserId, reqVO.getOaUserId())
                .likeIfPresent(TencentImUserMappingDO::getOaUsername, reqVO.getOaUsername())
                .eqIfPresent(TencentImUserMappingDO::getOrdersysUserPrefix, reqVO.getOrdersysUserPrefix())
                .likeIfPresent(TencentImUserMappingDO::getOrdersysUsername, reqVO.getOrdersysUsername())
                .likeIfPresent(TencentImUserMappingDO::getImUserId, reqVO.getImUserId())
                .eqIfPresent(TencentImUserMappingDO::getStatus, reqVO.getStatus())
                .orderByDesc(TencentImUserMappingDO::getId));
    }

    default List<TencentImUserMappingDO> selectActiveContactList(
            Long tenantId, Long excludeOaUserId, String keyword, int limit) {
        LambdaQueryWrapperX<TencentImUserMappingDO> wrapper = new LambdaQueryWrapperX<TencentImUserMappingDO>()
                .eq(TencentImUserMappingDO::getStatus, 0);

        if (tenantId != null && tenantId > 0) {
            wrapper.and(query -> query
                    .eq(TencentImUserMappingDO::getTenantId, tenantId)
                    .or()
                    .eq(TencentImUserMappingDO::getTenantId, 0L));
        } else {
            wrapper.eq(TencentImUserMappingDO::getTenantId, 0L);
        }

        if (excludeOaUserId != null) {
            wrapper.ne(TencentImUserMappingDO::getOaUserId, excludeOaUserId);
        }

        if (StrUtil.isNotBlank(keyword)) {
            String trimmedKeyword = keyword.trim();
            wrapper.and(query -> query
                    .like(TencentImUserMappingDO::getOaUsername, trimmedKeyword)
                    .or()
                    .like(TencentImUserMappingDO::getOrdersysUsername, trimmedKeyword)
                    .or()
                    .like(TencentImUserMappingDO::getImUserId, trimmedKeyword)
                    .or()
                    .like(TencentImUserMappingDO::getRemark, trimmedKeyword));
        }

        return selectList(wrapper
                .orderByAsc(TencentImUserMappingDO::getOaUsername)
                .orderByAsc(TencentImUserMappingDO::getOrdersysUsername)
                .last("LIMIT " + limit));
    }
}
