package com.kyx.service.business.service.tenant;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.service.business.controller.admin.tenant.vo.tenant.BindUserToTenantReqVO;
import com.kyx.service.business.controller.admin.tenant.vo.tenant.TenantPageReqVO;
import com.kyx.service.business.controller.admin.tenant.vo.tenant.TenantSaveReqVO;
import com.kyx.service.business.controller.admin.tenant.vo.tenant.TenantUserPageReqVO;
import com.kyx.service.business.controller.admin.tenant.vo.tenant.TenantUserRespVO;
import com.kyx.service.business.controller.admin.tenant.vo.tenant.UpdateUserRolesInTenantReqVO;
import com.kyx.service.business.controller.admin.tenant.vo.tenant.UserTenantRespVO;
import com.kyx.service.business.dal.dataobject.tenant.TenantDO;
import com.kyx.service.business.dal.dataobject.permission.RoleDO;
import com.kyx.service.business.service.tenant.handler.TenantInfoHandler;
import com.kyx.service.business.service.tenant.handler.TenantMenuHandler;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import com.kyx.foundation.common.enums.TenantViewScopeEnum;

/**
 * 租户 Service 接口
 *
 * @author MK
 */
public interface TenantService {

    /**
     * 创建租户
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createTenant(@Valid TenantSaveReqVO createReqVO);

    /**
     * 更新租户
     *
     * @param updateReqVO 更新信息
     */
    void updateTenant(@Valid TenantSaveReqVO updateReqVO);

    /**
     * 更新租户的角色菜单
     *
     * @param tenantId 租户编号
     * @param menuIds  菜单编号数组
     */
    void updateTenantRoleMenu(Long tenantId, Set<Long> menuIds);

    /**
     * 删除租户
     *
     * @param id 编号
     */
    void deleteTenant(Long id);

    /**
     * 获得租户
     *
     * @param id 编号
     * @return 租户
     */
    TenantDO getTenant(Long id);

    /**
     * 获得租户分页
     *
     * @param pageReqVO 分页查询
     * @return 租户分页
     */
    PageResult<TenantDO> getTenantPage(TenantPageReqVO pageReqVO);

    /**
     * 获得名字对应的租户
     *
     * @param name 租户名
     * @return 租户
     */
    TenantDO getTenantByName(String name);

    /**
     * 获得域名对应的租户
     *
     * @param website 域名
     * @return 租户
     */
    TenantDO getTenantByWebsite(String website);

    /**
     * 获得使用指定套餐的租户数量
     *
     * @param packageId 租户套餐编号
     * @return 租户数量
     */
    Long getTenantCountByPackageId(Long packageId);

    /**
     * 获得使用指定套餐的租户数组
     *
     * @param packageId 租户套餐编号
     * @return 租户数组
     */
    List<TenantDO> getTenantListByPackageId(Long packageId);

    /**
     * 获得指定状态的租户列表
     *
     * @param status 状态
     * @return 租户列表
     */
    List<TenantDO> getTenantListByStatus(Integer status);

    /**
     * 进行租户的信息处理逻辑
     * 其中，租户编号从 {@link TenantContextHolder} 上下文中获取
     *
     * @param handler 处理器
     */
    void handleTenantInfo(TenantInfoHandler handler);

    /**
     * 进行租户的菜单处理逻辑
     * 其中，租户编号从 {@link TenantContextHolder} 上下文中获取
     *
     * @param handler 处理器
     */
    void handleTenantMenu(TenantMenuHandler handler);

    /**
     * 进行租户的菜单处理逻辑
     * 其中，租户编号从 {@link TenantContextHolder} 上下文中获取
     * 当为系统租户时，优先复用外部已加载的全量菜单编号，避免重复查询菜单表
     *
     * @param handler 处理器
     * @param allMenuIdsSupplier 全量菜单编号提供器
     */
    default void handleTenantMenu(TenantMenuHandler handler, Supplier<Set<Long>> allMenuIdsSupplier) {
        handleTenantMenu(handler);
    }

    /**
     * 获得所有租户
     *
     * @return 租户编号数组
     */
    List<Long> getTenantIdList();

    /**
     * 校验租户是否合法
     *
     * @param id 租户编号
     */
    void validTenant(Long id);

    /**
     * 根据用户ID获取用户绑定的租户信息
     *
     * @param userId 用户ID
     * @return 用户绑定的租户信息列表
     */
    List<UserTenantRespVO> getUserTenantList(Long userId);

    /**
     * 根据用户名（或手机号）获取用户绑定的租户信息
     *
     * @param username 用户名或手机号
     * @return 用户绑定的租户信息列表
     */
    List<UserTenantRespVO> getUserTenantListByUsername(String username);

    /**
     * 判断租户是否开启全局视图
     *
     * @param id 租户编号
     * @return 是否开启全局视图
     */
    boolean isGlobalView(Long id);

    /**
     * 获取租户数据视角
     *
     * @param id 租户编号
     * @return 数据视角
     */
    TenantViewScopeEnum getViewScope(Long id);

    /**
     * 获取租户可访问的租户编号集合
     *
     * @param id 租户编号
     * @return 租户编号集合
     */
    List<Long> getAllowedTenantIds(Long id);

    /**
     * 获取租户可用于跨租户协作的租户编号集合
     *
     * 规则：ALL=全平台，其它=同根租户集团
     *
     * @param id 租户编号
     * @return 租户编号集合
     */
    List<Long> getCollaborationTenantIds(Long id);

    /**
     * 根据根租户编号获取租户列表
     *
     * @param rootId 根租户编号
     * @return 租户列表
     */
    List<TenantDO> getTenantListByRootId(Long rootId);

    /**
     * 根据编号集合获取租户列表
     *
     * @param ids 编号集合
     * @return 租户列表
     */
    List<TenantDO> getTenantListByIds(Collection<Long> ids);

    /**
     * 检查用户是否有权访问指定租户
     *
     * @param userId 用户ID
     * @param tenantId 租户ID
     * @return 是否有权访问
     */
    boolean checkUserTenantAccess(Long userId, Long tenantId);

    /**
     * 获取租户下的所有用户
     *
     * @param tenantId 租户ID
     * @return 租户用户列表
     */
    List<TenantUserRespVO> getTenantUsers(Long tenantId);

    /**
     * 分页查询租户下的用户
     *
     * @param pageReqVO 分页查询参数
     * @return 租户用户分页结果
     */
    PageResult<TenantUserRespVO> getTenantUsersPage(TenantUserPageReqVO pageReqVO);

    /**
     * 绑定用户到租户
     *
     * @param reqVO 绑定请求参数
     */
    void bindUserToTenant(BindUserToTenantReqVO reqVO);

    /**
     * 解绑用户从租户
     *
     * @param userId 用户ID
     * @param tenantId 租户ID
     */
    void unbindUserFromTenant(Long userId, Long tenantId);

    /**
     * 获取租户下可用的角色列表
     *
     * @param tenantId 租户ID
     * @return 角色列表
     */
    List<RoleDO> getTenantRoles(Long tenantId);

    /**
     * 更新用户在租户内的角色
     *
     * @param reqVO 更新请求参数
     */
    void updateUserRolesInTenant(UpdateUserRolesInTenantReqVO reqVO);

}
