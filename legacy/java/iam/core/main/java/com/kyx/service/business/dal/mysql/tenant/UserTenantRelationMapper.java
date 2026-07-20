package com.kyx.service.business.dal.mysql.tenant;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.foundation.tenant.core.aop.TenantIgnore;
import com.kyx.service.business.dal.dataobject.tenant.UserTenantRelationDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户-租户关联 Mapper
 *
 * @author MK
 */
@Mapper
@TenantIgnore
public interface UserTenantRelationMapper extends BaseMapperX<UserTenantRelationDO> {

    /**
     * 根据用户ID查询所有租户关联
     *
     * @param userId 用户ID
     * @return 租户关联列表
     */
    default List<UserTenantRelationDO> selectListByUserId(Long userId) {
        return selectList(UserTenantRelationDO::getUserId, userId);
    }

    /**
     * 根据租户ID查询所有用户关联
     *
     * @param tenantId 租户ID
     * @return 用户关联列表
     */
    default List<UserTenantRelationDO> selectListByTenantId(Long tenantId) {
        return selectList(UserTenantRelationDO::getTenantId, tenantId);
    }

    /**
     * 根据用户ID和租户ID查询关联
     *
     * @param userId 用户ID
     * @param tenantId 租户ID
     * @return 关联信息
     */
    default UserTenantRelationDO selectByUserIdAndTenantId(Long userId, Long tenantId) {
        return selectOne(UserTenantRelationDO::getUserId, userId,
                UserTenantRelationDO::getTenantId, tenantId);
    }

    /**
     * 根据用户ID查询默认租户关联
     *
     * @param userId 用户ID
     * @return 默认租户关联
     */
    default UserTenantRelationDO selectDefaultByUserId(Long userId) {
        return selectOne(UserTenantRelationDO::getUserId, userId,
                UserTenantRelationDO::getIsDefault, 1);
    }

    /**
     * 根据用户ID查询有效的租户关联列表
     *
     * @param userId 用户ID
     * @return 有效的租户关联列表
     */
    default List<UserTenantRelationDO> selectValidListByUserId(Long userId) {
        return selectList(UserTenantRelationDO::getUserId, userId,
                UserTenantRelationDO::getStatus, 1);
    }

    /**
     * 根据租户ID查询有效的用户关联列表
     *
     * @param tenantId 租户ID
     * @return 有效的用户关联列表
     */
    default List<UserTenantRelationDO> selectValidListByTenantId(Long tenantId) {
        return selectList(UserTenantRelationDO::getTenantId, tenantId,
                UserTenantRelationDO::getStatus, 1);
    }

    /**
     * 检查用户是否在指定租户中
     *
     * @param userId 用户ID
     * @param tenantId 租户ID
     * @return 是否存在
     */
    default boolean existsByUserIdAndTenantId(Long userId, Long tenantId) {
        return selectCount(new LambdaQueryWrapperX<UserTenantRelationDO>()
                .eq(UserTenantRelationDO::getUserId, userId)
                .eq(UserTenantRelationDO::getTenantId, tenantId)
                .eq(UserTenantRelationDO::getStatus, 1)) > 0;
    }

} 