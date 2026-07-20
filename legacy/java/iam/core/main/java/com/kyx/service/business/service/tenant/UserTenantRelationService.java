package com.kyx.service.business.service.tenant;

import com.kyx.service.business.dal.dataobject.tenant.UserTenantRelationDO;

import java.util.List;

/**
 * 用户-租户关联 Service 接口
 *
 * @author MK
 */
public interface UserTenantRelationService {

    /**
     * 创建用户-租户关联
     *
     * @param userTenantRelation 用户-租户关联信息
     * @return 关联ID
     */
    Long createUserTenantRelation(UserTenantRelationDO userTenantRelation);

    /**
     * 更新用户-租户关联
     *
     * @param userTenantRelation 用户-租户关联信息
     */
    void updateUserTenantRelation(UserTenantRelationDO userTenantRelation);

    /**
     * 删除用户-租户关联
     *
     * @param id 关联ID
     */
    void deleteUserTenantRelation(Long id);

    /**
     * 根据用户ID查询所有租户关联
     *
     * @param userId 用户ID
     * @return 租户关联列表
     */
    List<UserTenantRelationDO> getUserTenantRelationsByUserId(Long userId);

    /**
     * 根据租户ID查询所有用户关联
     *
     * @param tenantId 租户ID
     * @return 用户关联列表
     */
    List<UserTenantRelationDO> getUserTenantRelationsByTenantId(Long tenantId);

    /**
     * 根据用户ID和租户ID查询关联
     *
     * @param userId 用户ID
     * @param tenantId 租户ID
     * @return 关联信息
     */
    UserTenantRelationDO getUserTenantRelation(Long userId, Long tenantId);

    /**
     * 根据用户ID查询默认租户关联
     *
     * @param userId 用户ID
     * @return 默认租户关联
     */
    UserTenantRelationDO getDefaultUserTenantRelation(Long userId);

    /**
     * 根据用户ID查询有效的租户关联列表
     *
     * @param userId 用户ID
     * @return 有效的租户关联列表
     */
    List<UserTenantRelationDO> getValidUserTenantRelationsByUserId(Long userId);

    /**
     * 检查用户是否在指定租户中
     *
     * @param userId 用户ID
     * @param tenantId 租户ID
     * @return 是否存在
     */
    boolean isUserInTenant(Long userId, Long tenantId);

    /**
     * 设置用户默认租户
     *
     * @param userId 用户ID
     * @param tenantId 租户ID
     */
    void setUserDefaultTenant(Long userId, Long tenantId);

    /**
     * 将用户添加到租户
     *
     * @param userId 用户ID
     * @param tenantId 租户ID
     * @param roleIds 角色ID列表
     * @param deptId 部门ID
     * @param postIds 岗位ID列表
     * @param nickname 昵称
     * @param avatar 头像
     * @param isDefault 是否设为默认租户
     */
    void addUserToTenant(Long userId, Long tenantId, List<Long> roleIds, Long deptId, 
                        List<Long> postIds, String nickname, String avatar, Boolean isDefault);

    /**
     * 将用户从租户中移除
     *
     * @param userId 用户ID
     * @param tenantId 租户ID
     */
    void removeUserFromTenant(Long userId, Long tenantId);

    /**
     * 获取用户可访问的租户列表
     *
     * @param userId 用户ID
     * @return 租户ID列表
     */
    List<Long> getUserAccessibleTenantIds(Long userId);

    /**
     * 更新用户在租户内的角色
     *
     * @param userId 用户ID
     * @param tenantId 租户ID
     * @param roleIds 角色ID列表
     */
    void updateUserRolesInTenant(Long userId, Long tenantId, List<Long> roleIds);

} 