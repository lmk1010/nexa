package com.kyx.service.business.service.tenant;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.enums.TenantTypeEnum;
import com.kyx.foundation.common.enums.TenantViewScopeEnum;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.collection.CollectionUtils;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.datapermission.core.annotation.DataPermission;
import com.kyx.foundation.tenant.config.TenantProperties;
import com.kyx.foundation.tenant.core.aop.TenantIgnore;
import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.business.controller.admin.permission.vo.role.RoleSaveReqVO;
import com.kyx.service.business.controller.admin.tenant.vo.tenant.BindUserToTenantReqVO;
import com.kyx.service.business.controller.admin.tenant.vo.tenant.TenantPageReqVO;
import com.kyx.service.business.controller.admin.tenant.vo.tenant.TenantSaveReqVO;
import com.kyx.service.business.controller.admin.tenant.vo.tenant.TenantUserPageReqVO;
import com.kyx.service.business.controller.admin.tenant.vo.tenant.TenantUserRespVO;
import com.kyx.service.business.controller.admin.tenant.vo.tenant.UpdateUserRolesInTenantReqVO;
import com.kyx.service.business.controller.admin.tenant.vo.tenant.UserTenantRespVO;
import com.kyx.service.business.convert.tenant.TenantConvert;
import com.kyx.service.business.dal.dataobject.permission.MenuDO;
import com.kyx.service.business.dal.dataobject.permission.RoleDO;
import com.kyx.service.business.dal.dataobject.tenant.TenantDO;
import com.kyx.service.business.dal.dataobject.tenant.TenantPackageDO;
import com.kyx.service.business.dal.dataobject.tenant.UserTenantRelationDO;
import com.kyx.service.business.dal.mysql.tenant.TenantMapper;
import com.kyx.service.business.enums.permission.RoleCodeEnum;
import com.kyx.service.business.enums.permission.RoleTypeEnum;
import com.kyx.service.business.service.permission.MenuService;
import com.kyx.service.business.service.permission.PermissionService;
import com.kyx.service.business.service.permission.RoleService;
import com.kyx.service.business.service.tenant.handler.TenantInfoHandler;
import com.kyx.service.business.service.tenant.handler.TenantMenuHandler;
import com.kyx.service.business.service.user.AdminUserService;
import com.kyx.service.business.dal.dataobject.user.AdminUserDO;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.service.business.enums.ErrorCodeConstants.*;
import static java.util.Collections.singleton;

/**
 * 租户 Service 实现类
 *
 * @author MK
 */
@Service
@Validated
@Slf4j
public class TenantServiceImpl implements TenantService {

    @SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
    @Autowired(required = false) // 由于 foundation.tenant.enable 配置项，可以关闭多租户的功能，所以这里只能不强制注入
    private TenantProperties tenantProperties;

    @Resource
    private TenantMapper tenantMapper;

    @Resource
    private TenantPackageService tenantPackageService;
    @Resource
    @Lazy // 延迟，避免循环依赖报错
    private AdminUserService userService;
    @Resource
    private RoleService roleService;
    @Resource
    private MenuService menuService;
    @Resource
    private PermissionService permissionService;
    @Resource
    private UserTenantRelationService userTenantRelationService;
    @Resource
    private TenantFeatureConfigService tenantFeatureConfigService;

    @Override
    public List<Long> getTenantIdList() {
        List<TenantDO> tenants = tenantMapper.selectList();
        return CollectionUtils.convertList(tenants, TenantDO::getId);
    }

    @Override
    public void validTenant(Long id) {
        TenantDO tenant = getTenant(id);
        if (tenant == null) {
            throw exception(TENANT_NOT_EXISTS);
        }
        if (CommonStatusEnum.isDisable(tenant.getStatus())) {
            throw exception(TENANT_DISABLE, tenant.getName());
        }
        if (tenant.getExpireTime() != null && tenant.getExpireTime().isBefore(LocalDateTime.now())) {
            throw exception(TENANT_EXPIRE, tenant.getName());
        }
    }

    @Override
    @DSTransactional // 多数据源，使用 @DSTransactional 保证本地事务，以及数据源的切换
    @DataPermission(enable = false) // 参见 https://gitee.com/zhijiantianya/ruoyi-vue-pro/pulls/1154 说明
    public Long createTenant(TenantSaveReqVO createReqVO) {
        // 校验租户名称是否重复
        validTenantNameDuplicate(createReqVO.getName(), null);
        // 校验租户域名是否重复
        validTenantWebsiteDuplicate(createReqVO.getWebsite(), null);
        // 校验套餐被禁用
        TenantPackageDO tenantPackage = tenantPackageService.validTenantPackage(createReqVO.getPackageId());

        // 创建租户
        TenantDO tenant = BeanUtils.toBean(createReqVO, TenantDO.class);
        applyTenantDefaults(tenant);
        tenantMapper.insert(tenant);
        tenantFeatureConfigService.saveTenantFeatureConfigs(tenant.getId(), createReqVO.getFeatureConfigs());
        // 补齐层级关系（依赖自增 ID）
        updateTenantHierarchyOnCreate(tenant);
        // 创建租户的管理员
        TenantUtils.execute(tenant.getId(), () -> {
            // 创建角色
            Long roleId = createRole(tenantPackage);
            // 创建用户，并分配角色
            Long userId = createUser(roleId, createReqVO);
            // 修改租户的管理员
            tenantMapper.updateById(new TenantDO().setId(tenant.getId()).setContactUserId(userId));
        });
        return tenant.getId();
    }

    private Long createUser(Long roleId, TenantSaveReqVO createReqVO) {
        // 创建用户
        Long userId = userService.createUser(TenantConvert.INSTANCE.convert02(createReqVO));
        // 分配角色
        permissionService.assignUserRole(userId, singleton(roleId));
        return userId;
    }

    private Long createRole(TenantPackageDO tenantPackage) {
        // 创建角色
        RoleSaveReqVO reqVO = new RoleSaveReqVO();
        reqVO.setName(RoleCodeEnum.TENANT_ADMIN.getName()).setCode(RoleCodeEnum.TENANT_ADMIN.getCode())
                .setSort(0).setRemark("系统自动生成");
        Long roleId = roleService.createRole(reqVO, RoleTypeEnum.SYSTEM.getType());
        // 分配权限
        permissionService.assignRoleMenu(roleId, tenantPackage.getMenuIds());
        return roleId;
    }

    @Override
    @DSTransactional // 多数据源，使用 @DSTransactional 保证本地事务，以及数据源的切换
    public void updateTenant(TenantSaveReqVO updateReqVO) {
        // 校验存在
        TenantDO tenant = validateUpdateTenant(updateReqVO.getId());
        // 校验租户名称是否重复
        validTenantNameDuplicate(updateReqVO.getName(), updateReqVO.getId());
        // 校验租户域名是否重复
        validTenantWebsiteDuplicate(updateReqVO.getWebsite(), updateReqVO.getId());
        // 校验套餐被禁用
        TenantPackageDO tenantPackage = tenantPackageService.validTenantPackage(updateReqVO.getPackageId());

        // 更新租户
        TenantDO updateObj = BeanUtils.toBean(updateReqVO, TenantDO.class);
        if (updateObj.getParentId() == null) {
            updateObj.setParentId(tenant.getParentId());
        }
        if (StrUtil.isBlank(updateObj.getTenantType())) {
            updateObj.setTenantType(tenant.getTenantType());
        }
        if (StrUtil.isBlank(updateObj.getViewScope())) {
            updateObj.setViewScope(tenant.getViewScope());
        }
        applyTenantDefaults(updateObj);
        tenantMapper.updateById(updateObj);
        // 如父级发生变化，更新层级关系
        if (updateReqVO.getParentId() != null
                && !Objects.equals(updateReqVO.getParentId(), tenant.getParentId())) {
            updateTenantHierarchy(updateReqVO.getId(), updateReqVO.getParentId());
        } else {
            refreshTenantHierarchyIfMissing(updateReqVO.getId());
        }
        tenantFeatureConfigService.saveTenantFeatureConfigs(updateReqVO.getId(), updateReqVO.getFeatureConfigs());
        // 如果套餐发生变化，则修改其角色的权限
        if (ObjectUtil.notEqual(tenant.getPackageId(), updateReqVO.getPackageId())) {
            updateTenantRoleMenu(tenant.getId(), tenantPackage.getMenuIds());
        }
    }

    private void validTenantNameDuplicate(String name, Long id) {
        TenantDO tenant = tenantMapper.selectByName(name);
        if (tenant == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同名字的租户
        if (id == null) {
            throw exception(TENANT_NAME_DUPLICATE, name);
        }
        if (!tenant.getId().equals(id)) {
            throw exception(TENANT_NAME_DUPLICATE, name);
        }
    }

    private void validTenantWebsiteDuplicate(String website, Long id) {
        if (StrUtil.isEmpty(website)) {
            return;
        }
        TenantDO tenant = tenantMapper.selectByWebsite(website);
        if (tenant == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同名字的租户
        if (id == null) {
            throw exception(TENANT_WEBSITE_DUPLICATE, website);
        }
        if (!tenant.getId().equals(id)) {
            throw exception(TENANT_WEBSITE_DUPLICATE, website);
        }
    }

    @Override
    @DSTransactional
    public void updateTenantRoleMenu(Long tenantId, Set<Long> menuIds) {
        TenantUtils.execute(tenantId, () -> {
            // 获得所有角色
            List<RoleDO> roles = roleService.getRoleList();
            roles.forEach(role -> Assert.isTrue(tenantId.equals(role.getTenantId()), "角色({}/{}) 租户不匹配",
                    role.getId(), role.getTenantId(), tenantId)); // 兜底校验
            // 重新分配每个角色的权限
            roles.forEach(role -> {
                // 如果是租户管理员或超级管理员，直接分配套餐的完整权限
                if (Objects.equals(role.getCode(), RoleCodeEnum.TENANT_ADMIN.getCode()) ||
                    Objects.equals(role.getCode(), RoleCodeEnum.SUPER_ADMIN.getCode())) {
                    permissionService.assignRoleMenu(role.getId(), menuIds);
                    log.info("[updateTenantRoleMenu][{}({}/{}) 的权限修改为套餐完整权限({})]",
                            role.getName(), role.getId(), role.getTenantId(), menuIds);
                    return;
                }
                // 如果是普通角色，使用并集（自动获得新权限，同时保留原有权限）
                Set<Long> roleMenuIds = permissionService.getRoleMenuListByRoleId(role.getId());
                // 取并集：原有权限 + 新套餐权限，但限制在套餐范围内
                roleMenuIds = CollUtil.unionDistinct(roleMenuIds, menuIds);
                roleMenuIds = CollUtil.intersectionDistinct(roleMenuIds, menuIds);
                permissionService.assignRoleMenu(role.getId(), roleMenuIds);
                log.info("[updateTenantRoleMenu][{}({}/{}) 的权限修改为({})]",
                        role.getName(), role.getId(), role.getTenantId(), roleMenuIds);
            });
        });
    }

    @Override
    public void deleteTenant(Long id) {
        // 校验存在
        validateUpdateTenant(id);
        // 删除
        tenantMapper.deleteById(id);
    }

    private TenantDO validateUpdateTenant(Long id) {
        TenantDO tenant = tenantMapper.selectById(id);
        if (tenant == null) {
            throw exception(TENANT_NOT_EXISTS);
        }
        // 内置租户，不允许删除
        if (isSystemTenant(tenant)) {
            throw exception(TENANT_CAN_NOT_UPDATE_SYSTEM);
        }
        return tenant;
    }

    @Override
    public TenantDO getTenant(Long id) {
        return tenantMapper.selectById(id);
    }

    @Override
    public PageResult<TenantDO> getTenantPage(TenantPageReqVO pageReqVO) {
        return tenantMapper.selectPage(pageReqVO);
    }

    @Override
    public TenantDO getTenantByName(String name) {
        return tenantMapper.selectByName(name);
    }

    @Override
    public TenantDO getTenantByWebsite(String website) {
        return tenantMapper.selectByWebsite(website);
    }

    @Override
    public Long getTenantCountByPackageId(Long packageId) {
        return tenantMapper.selectCountByPackageId(packageId);
    }

    @Override
    public List<TenantDO> getTenantListByPackageId(Long packageId) {
        return tenantMapper.selectListByPackageId(packageId);
    }

    @Override
    public List<TenantDO> getTenantListByStatus(Integer status) {
        return tenantMapper.selectListByStatus(status);
    }

    @Override
    public void handleTenantInfo(TenantInfoHandler handler) {
        // 如果禁用，则不执行逻辑
        if (isTenantDisable()) {
            return;
        }
        // 获得租户
        TenantDO tenant = getTenant(TenantContextHolder.getRequiredTenantId());
        // 执行处理器
        handler.handle(tenant);
    }

    @Override
    public void handleTenantMenu(TenantMenuHandler handler) {
        handleTenantMenu(handler, () -> CollectionUtils.convertSet(menuService.getMenuList(), MenuDO::getId));
    }

    @Override
    public void handleTenantMenu(TenantMenuHandler handler, Supplier<Set<Long>> allMenuIdsSupplier) {
        // 如果禁用，则不执行逻辑
        if (isTenantDisable()) {
            return;
        }
        // 获得租户，然后获得菜单
        TenantDO tenant = getTenant(TenantContextHolder.getRequiredTenantId());
        Set<Long> menuIds;
        if (isSystemTenant(tenant)) { // 系统租户，菜单是全量的
            menuIds = allMenuIdsSupplier.get();
        } else {
            menuIds = tenantPackageService.getTenantPackage(tenant.getPackageId()).getMenuIds();
        }
        // 执行处理器
        handler.handle(menuIds);
    }

    private static boolean isSystemTenant(TenantDO tenant) {
        return Objects.equals(tenant.getPackageId(), TenantDO.PACKAGE_ID_SYSTEM);
    }

    private boolean isTenantDisable() {
        return tenantProperties == null || Boolean.FALSE.equals(tenantProperties.getEnable());
    }

    @Override
    public List<UserTenantRespVO> getUserTenantList(Long userId) {
        // 1. 从 system_user_tenant_relation 表查询用户的所有有效租户关联
        List<UserTenantRelationDO> relationList = userTenantRelationService.getValidUserTenantRelationsByUserId(userId);

        // 2. 如果用户没有租户关联，直接返回空列表（不再从 user 表的 tenantId 字段兼容）
        if (CollUtil.isEmpty(relationList)) {
            return new ArrayList<>();
        }

        // 3. 构建返回结果列表
        List<UserTenantRespVO> result = new ArrayList<>();
        for (UserTenantRelationDO relation : relationList) {
            // 获取租户信息
            TenantDO tenant = getTenant(relation.getTenantId());
            if (tenant == null || CommonStatusEnum.isDisable(tenant.getStatus())) {
                continue; // 跳过不存在或已禁用的租户
            }

            // 构建响应对象
            UserTenantRespVO vo = new UserTenantRespVO();
            vo.setTenantId(tenant.getId());
            vo.setTenantName(tenant.getName());
            vo.setStatus(tenant.getStatus());
            vo.setExpired(tenant.getExpireTime() != null && tenant.getExpireTime().isBefore(LocalDateTime.now()));
            vo.setIsDefault(relation.getIsDefault() == 1);
            vo.setGlobalView(tenant.getGlobalView());
            vo.setViewScope(getViewScope(tenant.getId()).getCode());

            // 检查用户在该租户下是否有角色权限（从 system_user_role 表查询）
            Set<Long> roleIds = TenantUtils.execute(tenant.getId(), () ->
                permissionService.getUserRoleIdListByUserId(userId)
            );
            vo.setHasRole(CollUtil.isNotEmpty(roleIds));

            result.add(vo);
        }

        return result;
    }

    @Override
    @TenantIgnore
    public List<UserTenantRespVO> getUserTenantListByUsername(String username) {
        if (StrUtil.isBlank(username)) {
            return new ArrayList<>();
        }

        String account = username.trim();
        boolean isMobile = account.matches("^1[3-9]\\d{9}$");
        AdminUserDO user = isMobile ? userService.getUserByMobile(account) : userService.getUserByUsername(account);
        if (user == null) {
            return new ArrayList<>();
        }
        return getUserTenantList(user.getId());
    }

    @Override
    public boolean isGlobalView(Long id) {
        if (id == null) {
            return false;
        }
        TenantDO tenant = getTenant(id);
        if (tenant == null) {
            return false;
        }
        return TenantViewScopeEnum.ALL.getCode().equalsIgnoreCase(normalizeViewScope(tenant.getViewScope(), tenant.getGlobalView()));
    }

    @Override
    public TenantViewScopeEnum getViewScope(Long id) {
        if (id == null) {
            return TenantViewScopeEnum.SELF;
        }
        TenantDO tenant = getTenant(id);
        if (tenant == null) {
            return TenantViewScopeEnum.SELF;
        }
        return TenantViewScopeEnum.fromCode(normalizeViewScope(tenant.getViewScope(), tenant.getGlobalView()));
    }

    @Override
    public List<Long> getAllowedTenantIds(Long id) {
        if (id == null) {
            return Collections.emptyList();
        }
        TenantViewScopeEnum scope = getViewScope(id);
        if (scope == TenantViewScopeEnum.ALL) {
            return getTenantIdList();
        }
        if (scope == TenantViewScopeEnum.GROUP) {
            TenantDO tenant = getTenant(id);
            if (tenant == null) {
                return Collections.emptyList();
            }
            Long rootId = tenant.getRootId() != null && tenant.getRootId() > 0 ? tenant.getRootId() : tenant.getId();
            return CollectionUtils.convertList(tenantMapper.selectListByRootId(rootId), TenantDO::getId);
        }
        return Collections.singletonList(id);
    }

    @Override
    public List<Long> getCollaborationTenantIds(Long id) {
        if (id == null) {
            return Collections.emptyList();
        }
        TenantDO tenant = getTenant(id);
        if (tenant == null || CommonStatusEnum.isDisable(tenant.getStatus())) {
            return Collections.emptyList();
        }
        if (getViewScope(id) == TenantViewScopeEnum.ALL) {
            return tenantMapper.selectList().stream()
                    .filter(Objects::nonNull)
                    .filter(item -> !CommonStatusEnum.isDisable(item.getStatus()))
                    .map(TenantDO::getId)
                    .collect(Collectors.toList());
        }
        Long rootId = tenant.getRootId() != null && tenant.getRootId() > 0 ? tenant.getRootId() : tenant.getId();
        return tenantMapper.selectListByRootId(rootId).stream()
                .filter(Objects::nonNull)
                .filter(item -> !CommonStatusEnum.isDisable(item.getStatus()))
                .map(TenantDO::getId)
                .collect(Collectors.toList());
    }

    @Override
    public List<TenantDO> getTenantListByRootId(Long rootId) {
        if (rootId == null) {
            return Collections.emptyList();
        }
        return tenantMapper.selectListByRootId(rootId);
    }

    @Override
    public List<TenantDO> getTenantListByIds(java.util.Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return tenantMapper.selectListByIds(new ArrayList<>(ids));
    }

    @Override
    @TenantIgnore
    public boolean checkUserTenantAccess(Long userId, Long tenantId) {
        if (userId == null || tenantId == null) {
            return false;
        }
        // 检查用户是否在该租户下有关联记录
        UserTenantRelationDO relation = userTenantRelationService.getUserTenantRelation(userId, tenantId);
        // 关联记录存在且状态为正常（1）则有权访问
        return relation != null && Integer.valueOf(1).equals(relation.getStatus());
    }

    @Override
    @DataPermission(enable = false)
    @TenantIgnore
    public List<TenantUserRespVO> getTenantUsers(Long tenantId) {
        // 1. 查询租户下的所有用户关联
        List<UserTenantRelationDO> relationList = userTenantRelationService.getUserTenantRelationsByTenantId(tenantId);
        if (CollUtil.isEmpty(relationList)) {
            return new ArrayList<>();
        }

        // 2. 构建返回结果
        List<TenantUserRespVO> result = new ArrayList<>();
        for (UserTenantRelationDO relation : relationList) {
            // 忽略租户过滤，直接查询用户信息
            AdminUserDO user = userService.getUser(relation.getUserId());
            if (user == null) {
                continue;
            }

            TenantUserRespVO vo = new TenantUserRespVO();
            vo.setUserId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setNickname(relation.getNickname() != null ? relation.getNickname() : user.getNickname());
            vo.setAvatar(relation.getAvatar() != null ? relation.getAvatar() : user.getAvatar());
            vo.setMobile(user.getMobile());
            vo.setEmail(user.getEmail());
            vo.setDeptId(relation.getDeptId());
            vo.setPostIds(relation.getPostIds());
            // 从 system_user_role 表查询角色（需要在租户上下文中查询）
            vo.setRoleIds(TenantUtils.execute(tenantId, () ->
                permissionService.getUserRoleIdListByUserId(user.getId())));
            vo.setStatus(relation.getStatus());
            vo.setIsDefault(relation.getIsDefault() == 1);
            vo.setJoinTime(relation.getJoinTime());
            result.add(vo);
        }

        // 3. 按加入时间降序排序（最新的在前面）
        result.sort((a, b) -> {
            if (a.getJoinTime() == null && b.getJoinTime() == null) return 0;
            if (a.getJoinTime() == null) return 1;
            if (b.getJoinTime() == null) return -1;
            return b.getJoinTime().compareTo(a.getJoinTime());
        });

        return result;
    }

    @Override
    @DataPermission(enable = false)
    @TenantIgnore
    public PageResult<TenantUserRespVO> getTenantUsersPage(TenantUserPageReqVO pageReqVO) {
        // 1. 查询租户下的所有用户关联
        List<UserTenantRelationDO> relationList = userTenantRelationService.getUserTenantRelationsByTenantId(pageReqVO.getTenantId());
        if (CollUtil.isEmpty(relationList)) {
            return PageResult.empty();
        }

        // 2. 构建完整的用户列表
        List<TenantUserRespVO> allUsers = new ArrayList<>();
        for (UserTenantRelationDO relation : relationList) {
            // 忽略租户过滤，直接查询用户信息
            AdminUserDO user = userService.getUser(relation.getUserId());
            if (user == null) {
                continue;
            }

            TenantUserRespVO vo = new TenantUserRespVO();
            vo.setUserId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setNickname(relation.getNickname() != null ? relation.getNickname() : user.getNickname());
            vo.setAvatar(relation.getAvatar() != null ? relation.getAvatar() : user.getAvatar());
            vo.setMobile(user.getMobile());
            vo.setEmail(user.getEmail());
            vo.setDeptId(relation.getDeptId());
            vo.setPostIds(relation.getPostIds());
            // 从 system_user_role 表查询角色（需要在租户上下文中查询）
            vo.setRoleIds(TenantUtils.execute(pageReqVO.getTenantId(), () ->
                permissionService.getUserRoleIdListByUserId(user.getId())));
            vo.setStatus(relation.getStatus());
            vo.setIsDefault(relation.getIsDefault() == 1);
            vo.setJoinTime(relation.getJoinTime());
            allUsers.add(vo);
        }

        // 3. 应用搜索过滤
        List<TenantUserRespVO> filteredUsers = allUsers.stream()
                .filter(user -> {
                    if (StrUtil.isNotBlank(pageReqVO.getUsername()) &&
                            !user.getUsername().contains(pageReqVO.getUsername())) {
                        return false;
                    }
                    if (StrUtil.isNotBlank(pageReqVO.getNickname()) &&
                            !user.getNickname().contains(pageReqVO.getNickname())) {
                        return false;
                    }
                    if (StrUtil.isNotBlank(pageReqVO.getMobile()) &&
                            (user.getMobile() == null || !user.getMobile().contains(pageReqVO.getMobile()))) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        // 4. 按加入时间降序排序（最新的在前面）
        filteredUsers.sort((a, b) -> {
            if (a.getJoinTime() == null && b.getJoinTime() == null) return 0;
            if (a.getJoinTime() == null) return 1;
            if (b.getJoinTime() == null) return -1;
            return b.getJoinTime().compareTo(a.getJoinTime());
        });

        // 5. 手动分页
        int total = filteredUsers.size();
        int pageNo = pageReqVO.getPageNo();
        int pageSize = pageReqVO.getPageSize();
        int fromIndex = (pageNo - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);

        if (fromIndex >= total) {
            return new PageResult<>(new ArrayList<>(), 0L);
        }

        List<TenantUserRespVO> pageData = filteredUsers.subList(fromIndex, toIndex);
        return new PageResult<>(pageData, (long) total);
    }

    @Override
    public void bindUserToTenant(BindUserToTenantReqVO reqVO) {
        // 检查用户是否已经绑定到该租户
        UserTenantRelationDO existRelation = userTenantRelationService.getUserTenantRelation(
                reqVO.getUserId(), reqVO.getTenantId());
        if (existRelation != null) {
            throw exception(TENANT_USER_ALREADY_BOUND);
        }

        // 调用 UserTenantRelationService 的 addUserToTenant 方法
        userTenantRelationService.addUserToTenant(
                reqVO.getUserId(),
                reqVO.getTenantId(),
                reqVO.getRoleIds(),
                reqVO.getDeptId(),
                reqVO.getPostIds(),
                null, // nickname
                null, // avatar
                reqVO.getIsDefault()
        );
    }

    @Override
    public void unbindUserFromTenant(Long userId, Long tenantId) {
        userTenantRelationService.removeUserFromTenant(userId, tenantId);
    }

    @Override
    @TenantIgnore
    public List<RoleDO> getTenantRoles(Long tenantId) {
        // 在指定租户上下文中获取角色列表
        return TenantUtils.execute(tenantId, () -> {
            return roleService.getRoleListByStatus(singleton(CommonStatusEnum.ENABLE.getStatus()));
        });
    }

    @Override
    @TenantIgnore
    public void updateUserRolesInTenant(UpdateUserRolesInTenantReqVO reqVO) {
        // 检查用户是否已绑定到该租户
        UserTenantRelationDO relation = userTenantRelationService.getUserTenantRelation(
                reqVO.getUserId(), reqVO.getTenantId());
        if (relation == null) {
            throw exception(TENANT_USER_NOT_BOUND);
        }

        // 更新用户在租户内的角色
        userTenantRelationService.updateUserRolesInTenant(
                reqVO.getUserId(), reqVO.getTenantId(), reqVO.getRoleIds());
    }

    private void applyTenantDefaults(TenantDO tenant) {
        if (tenant.getParentId() == null) {
            tenant.setParentId(0L);
        }
        if (StrUtil.isBlank(tenant.getTenantType())) {
            tenant.setTenantType(TenantTypeEnum.COMPANY.getCode());
        } else {
            tenant.setTenantType(TenantTypeEnum.fromCode(tenant.getTenantType()).getCode());
        }
        String resolvedScope = normalizeViewScope(tenant.getViewScope(), tenant.getGlobalView());
        tenant.setViewScope(resolvedScope);
        if (TenantViewScopeEnum.ALL.getCode().equalsIgnoreCase(resolvedScope)) {
            tenant.setGlobalView(1);
        } else if (tenant.getGlobalView() == null) {
            tenant.setGlobalView(0);
        }
    }

    private String normalizeViewScope(String viewScope, Integer globalView) {
        if (Integer.valueOf(1).equals(globalView)) {
            return TenantViewScopeEnum.ALL.getCode();
        }
        if (StrUtil.isBlank(viewScope)) {
            return TenantViewScopeEnum.SELF.getCode();
        }
        return TenantViewScopeEnum.fromCode(viewScope).getCode();
    }

    private void updateTenantHierarchyOnCreate(TenantDO tenant) {
        Long parentId = tenant.getParentId();
        Long tenantId = tenant.getId();
        TenantDO updateObj = new TenantDO();
        updateObj.setId(tenantId);
        if (parentId == null || parentId == 0L) {
            updateObj.setRootId(tenantId);
            updateObj.setPath("/" + tenantId + "/");
        } else {
            TenantDO parent = tenantMapper.selectById(parentId);
            if (parent == null) {
                throw exception(TENANT_NOT_EXISTS);
            }
            Long rootId = parent.getRootId() != null && parent.getRootId() > 0 ? parent.getRootId() : parent.getId();
            updateObj.setRootId(rootId);
            String parentPath = StrUtil.isBlank(parent.getPath()) ? ("/" + parent.getId() + "/") : parent.getPath();
            updateObj.setPath(parentPath + tenantId + "/");
        }
        tenantMapper.updateById(updateObj);
    }

    private void updateTenantHierarchy(Long tenantId, Long newParentId) {
        TenantDO tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            return;
        }
        String oldPath = StrUtil.isBlank(tenant.getPath()) ? ("/" + tenantId + "/") : tenant.getPath();
        TenantDO parent = (newParentId == null || newParentId == 0L) ? null : tenantMapper.selectById(newParentId);
        Long newRootId = parent == null
                ? tenantId
                : (parent.getRootId() != null && parent.getRootId() > 0 ? parent.getRootId() : parent.getId());
        String parentPath = parent == null
                ? "/"
                : (StrUtil.isBlank(parent.getPath()) ? ("/" + parent.getId() + "/") : parent.getPath());
        String newPath = parentPath + tenantId + "/";
        tenantMapper.updateById(new TenantDO()
                .setId(tenantId)
                .setParentId(newParentId == null ? 0L : newParentId)
                .setRootId(newRootId)
                .setPath(newPath));
        tenantMapper.updatePathByPrefix(oldPath, newPath, newRootId);
    }

    private void refreshTenantHierarchyIfMissing(Long tenantId) {
        TenantDO tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            return;
        }
        if (tenant.getRootId() != null && tenant.getRootId() > 0 && StrUtil.isNotBlank(tenant.getPath())) {
            return;
        }
        updateTenantHierarchy(tenantId, tenant.getParentId());
    }

}
