package com.kyx.service.business.service.tenant;

import com.kyx.foundation.common.util.collection.CollectionUtils;
import com.kyx.foundation.tenant.core.aop.TenantIgnore;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.business.dal.dataobject.tenant.UserTenantRelationDO;
import com.kyx.service.business.dal.mysql.tenant.UserTenantRelationMapper;
import com.kyx.service.business.service.permission.PermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 用户-租户关联 Service 实现类
 *
 * @author MK
 */
@Service
@Validated
@Slf4j
public class UserTenantRelationServiceImpl implements UserTenantRelationService {

    @Resource
    private UserTenantRelationMapper userTenantRelationMapper;

    @Resource
    private PermissionService permissionService;

    @Override
    @TenantIgnore
    public Long createUserTenantRelation(UserTenantRelationDO userTenantRelation) {
        userTenantRelationMapper.insert(userTenantRelation);
        return userTenantRelation.getId();
    }

    @Override
    @TenantIgnore
    public void updateUserTenantRelation(UserTenantRelationDO userTenantRelation) {
        userTenantRelationMapper.updateById(userTenantRelation);
    }

    @Override
    @TenantIgnore
    public void deleteUserTenantRelation(Long id) {
        userTenantRelationMapper.deleteById(id);
    }

    @Override
    @TenantIgnore
    public List<UserTenantRelationDO> getUserTenantRelationsByUserId(Long userId) {
        return userTenantRelationMapper.selectListByUserId(userId);
    }

    @Override
    @TenantIgnore
    public List<UserTenantRelationDO> getUserTenantRelationsByTenantId(Long tenantId) {
        return userTenantRelationMapper.selectListByTenantId(tenantId);
    }

    @Override
    @TenantIgnore
    public UserTenantRelationDO getUserTenantRelation(Long userId, Long tenantId) {
        return userTenantRelationMapper.selectByUserIdAndTenantId(userId, tenantId);
    }

    @Override
    @TenantIgnore
    public UserTenantRelationDO getDefaultUserTenantRelation(Long userId) {
        return userTenantRelationMapper.selectDefaultByUserId(userId);
    }

    @Override
    @TenantIgnore
    public List<UserTenantRelationDO> getValidUserTenantRelationsByUserId(Long userId) {
        return userTenantRelationMapper.selectValidListByUserId(userId);
    }

    @Override
    @TenantIgnore
    public boolean isUserInTenant(Long userId, Long tenantId) {
        return userTenantRelationMapper.existsByUserIdAndTenantId(userId, tenantId);
    }

    @Override
    @TenantIgnore
    public void setUserDefaultTenant(Long userId, Long tenantId) {
        // 1. 先将该用户的所有租户关联设为非默认
        List<UserTenantRelationDO> relations = getUserTenantRelationsByUserId(userId);
        for (UserTenantRelationDO relation : relations) {
            if (relation.getIsDefault() == 1) {
                relation.setIsDefault(0);
                updateUserTenantRelation(relation);
            }
        }

        // 2. 将指定租户设为默认
        UserTenantRelationDO targetRelation = getUserTenantRelation(userId, tenantId);
        if (targetRelation != null) {
            targetRelation.setIsDefault(1);
            updateUserTenantRelation(targetRelation);
        }
    }

    @Override
    @TenantIgnore
    public void addUserToTenant(Long userId, Long tenantId, List<Long> roleIds, Long deptId,
                               List<Long> postIds, String nickname, String avatar, Boolean isDefault) {
        // 检查是否已存在关联
        UserTenantRelationDO existingRelation = getUserTenantRelation(userId, tenantId);
        if (existingRelation != null) {
            log.warn("用户 {} 已在租户 {} 中", userId, tenantId);
            return;
        }

        // 创建新的关联
        UserTenantRelationDO relation = UserTenantRelationDO.builder()
                .userId(userId)
                .tenantId(tenantId)
                .status(1)
                .isDefault(isDefault != null && isDefault ? 1 : 0)
                .joinTime(LocalDateTime.now())
                .deptId(deptId)
                .postIds(CollectionUtils.convertSet(postIds))
                .nickname(nickname)
                .avatar(avatar)
                .build();

        createUserTenantRelation(relation);

        // 如果提供了角色ID，将角色插入到 system_user_role 表
        if (roleIds != null && !roleIds.isEmpty()) {
            TenantUtils.execute(tenantId, () -> {
                permissionService.assignUserRole(userId, CollectionUtils.convertSet(roleIds));
                return null;
            });
        }

        // 如果设为默认租户，需要处理其他租户的默认状态
        if (isDefault != null && isDefault) {
            setUserDefaultTenant(userId, tenantId);
        }
    }

    @Override
    @TenantIgnore
    public void removeUserFromTenant(Long userId, Long tenantId) {
        UserTenantRelationDO relation = getUserTenantRelation(userId, tenantId);
        if (relation != null) {
            deleteUserTenantRelation(relation.getId());
        }
    }

    @Override
    @TenantIgnore
    public List<Long> getUserAccessibleTenantIds(Long userId) {
        List<UserTenantRelationDO> relations = getValidUserTenantRelationsByUserId(userId);
        return CollectionUtils.convertList(relations, UserTenantRelationDO::getTenantId);
    }

    @Override
    @TenantIgnore
    public void updateUserRolesInTenant(Long userId, Long tenantId, List<Long> roleIds) {
        // 追加角色模式：只添加新角色，不删除已有角色
        TenantUtils.execute(tenantId, () -> {
            permissionService.appendUserRoles(userId, CollectionUtils.convertSet(roleIds));
            return null;
        });
        log.info("为用户 {} 在租户 {} 中追加角色 {}", userId, tenantId, roleIds);
    }

} 