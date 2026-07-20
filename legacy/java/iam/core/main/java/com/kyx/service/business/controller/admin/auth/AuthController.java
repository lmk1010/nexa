package com.kyx.service.business.controller.admin.auth;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.base.Suppliers;
import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.enums.UserTypeEnum;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.security.config.SecurityProperties;
import com.kyx.foundation.security.core.LoginUser;
import com.kyx.foundation.security.core.util.SecurityFrameworkUtils;
import com.kyx.foundation.tenant.core.aop.TenantIgnore;
import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.service.business.controller.admin.auth.vo.AuthAppLoginReqVO;
import com.kyx.service.business.controller.admin.auth.vo.AuthLoginReqVO;
import com.kyx.service.business.controller.admin.auth.vo.AuthLoginRespVO;
import com.kyx.service.business.controller.admin.auth.vo.AuthPreLoginReqVO;
import com.kyx.service.business.controller.admin.auth.vo.AuthPreLoginRespVO;
import com.kyx.service.business.controller.admin.auth.vo.AuthPermissionInfoRespVO;
import com.kyx.service.business.controller.admin.auth.vo.AuthRegisterReqVO;
import com.kyx.service.business.controller.admin.auth.vo.AuthResetPasswordReqVO;
import com.kyx.service.business.controller.admin.auth.vo.AuthSmsLoginReqVO;
import com.kyx.service.business.controller.admin.auth.vo.AuthSmsSendReqVO;
import com.kyx.service.business.controller.admin.auth.vo.AuthSocialHandoffCompleteReqVO;
import com.kyx.service.business.controller.admin.auth.vo.AuthSocialHandoffFailReqVO;
import com.kyx.service.business.controller.admin.auth.vo.AuthSocialHandoffStatusRespVO;
import com.kyx.service.business.controller.admin.auth.vo.AuthSocialLoginReqVO;
import com.kyx.service.business.controller.admin.auth.vo.AuthTenantLoginReqVO;
import com.kyx.service.business.convert.auth.AuthConvert;
import com.kyx.service.business.dal.dataobject.auth.AuthSocialBrowserHandoffCacheDO;
import com.kyx.service.business.dal.dataobject.permission.MenuDO;
import com.kyx.service.business.dal.dataobject.permission.RoleDO;
import com.kyx.service.business.dal.dataobject.tenant.UserTenantRelationDO;
import com.kyx.service.business.dal.dataobject.user.AdminUserDO;
import com.kyx.service.business.dal.redis.auth.AuthSocialBrowserHandoffRedisDAO;
import com.kyx.service.business.enums.logger.LoginLogTypeEnum;
import com.kyx.service.business.service.auth.AdminAuthService;
import com.kyx.service.business.service.auth.AppAuthService;
import com.kyx.service.business.service.permission.MenuService;
import com.kyx.service.business.service.permission.PermissionService;
import com.kyx.service.business.service.permission.RoleService;
import com.kyx.service.business.service.social.SocialClientService;
import com.kyx.service.business.service.tenant.TenantService;
import com.kyx.service.business.service.tenant.UserTenantRelationService;
import com.kyx.service.business.service.user.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.foundation.common.pojo.CommonResult.success;
import static com.kyx.foundation.common.util.collection.CollectionUtils.convertSet;
import static com.kyx.foundation.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static com.kyx.service.business.enums.ErrorCodeConstants.TENANT_USER_NOT_IN_TENANT;

@Tag(name = "管理后台 - 认证")
@RestController
@RequestMapping("/system/auth")
@Validated
@Slf4j
public class AuthController {

    private static final long SOCIAL_BROWSER_HANDOFF_EXPIRE_SECONDS = 300;

    @Resource
    private AdminAuthService authService;
    @Resource
    private AdminUserService userService;
    @Resource
    private RoleService roleService;
    @Resource
    private MenuService menuService;
    @Resource
    private PermissionService permissionService;
    @Resource
    private SocialClientService socialClientService;
    @Resource
    private AuthSocialBrowserHandoffRedisDAO authSocialBrowserHandoffRedisDAO;

    @Resource
    private AppAuthService appAuthService;

    @Resource
    private SecurityProperties securityProperties;

    @Resource
    private TenantService tenantService;

    @Resource
    private UserTenantRelationService userTenantRelationService;

    @PostMapping("/login")
    @PermitAll
    @Operation(summary = "使用账号密码登录")
    public CommonResult<AuthLoginRespVO> login(@RequestBody @Valid AuthLoginReqVO reqVO) {
        return success(authService.login(reqVO));
    }

    @PostMapping("/pre-login")
    @PermitAll
    @TenantIgnore
    @Operation(summary = "预登录（校验账号密码并返回可选租户）")
    public CommonResult<AuthPreLoginRespVO> preLogin(@RequestBody @Valid AuthPreLoginReqVO reqVO) {
        return success(authService.preLogin(reqVO));
    }

    @PostMapping("/tenant-login")
    @PermitAll
    @TenantIgnore
    @Operation(summary = "确认租户后完成登录")
    public CommonResult<AuthLoginRespVO> tenantLogin(@RequestBody @Valid AuthTenantLoginReqVO reqVO) {
        return success(authService.tenantLogin(reqVO));
    }

    @PostMapping("/app-login")
    @PermitAll
    @Operation(summary = "移动端登录", description = "支持用户名、手机号、邮箱三种登录方式")
    public CommonResult<AuthLoginRespVO> appLogin(@RequestBody @Valid AuthAppLoginReqVO reqVO) {
        return success(appAuthService.appLogin(reqVO));
    }


    @PostMapping("/logout")
    @PermitAll
    @Operation(summary = "登出系统")
    public CommonResult<Boolean> logout(HttpServletRequest request) {
        String token = SecurityFrameworkUtils.obtainAuthorization(request,
                securityProperties.getTokenHeader(), securityProperties.getTokenParameter());
        if (StrUtil.isNotBlank(token)) {
            authService.logout(token, LoginLogTypeEnum.LOGOUT_SELF.getType());
        }
        return success(true);
    }

    @PostMapping("/refresh-token")
    @PermitAll
    @Operation(summary = "刷新令牌")
    @Parameter(name = "refreshToken", description = "刷新令牌", required = true)
    public CommonResult<AuthLoginRespVO> refreshToken(@RequestParam("refreshToken") String refreshToken) {
        return success(authService.refreshToken(refreshToken));
    }

    @GetMapping("/get-permission-info")
    @Operation(summary = "获取登录用户的权限信息")
    public CommonResult<AuthPermissionInfoRespVO> getPermissionInfo() {
        Long userId = getLoginUserId();
        Long requestTenantId = TenantContextHolder.getTenantId();
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        Long permissionTenantId = requestTenantId;
        if (TenantContextHolder.isIgnore() && loginUser != null && loginUser.getTenantId() != null) {
            permissionTenantId = loginUser.getTenantId();
        }

        log.info("[getPermissionInfo] 开始获取权限信息，userId={}, requestTenantId={}, permissionTenantId={}, tenantIgnore={}",
                userId, requestTenantId, permissionTenantId, TenantContextHolder.isIgnore());

        Long oldTenantId = TenantContextHolder.getTenantId();
        Boolean oldIgnore = TenantContextHolder.isIgnore();
        try {
            if (permissionTenantId != null) {
                TenantContextHolder.setTenantId(permissionTenantId);
            }
            // 权限始终按指定租户计算，避免跨租户权限叠加
            TenantContextHolder.setIgnore(false);

            // 1.1 检查用户是否有权限访问当前租户
            UserTenantRelationDO userTenantRelation = userTenantRelationService.getUserTenantRelation(userId, permissionTenantId);
            if (userTenantRelation == null || userTenantRelation.getStatus() != 1) {
                log.warn("[getPermissionInfo] 用户无权访问该租户，userId={}, tenantId={}", userId, permissionTenantId);
                throw exception(TENANT_USER_NOT_IN_TENANT);
            }

            log.info("[getPermissionInfo] 用户租户关系查询成功，deptId={}, postIds={}",
                userTenantRelation.getDeptId(), userTenantRelation.getPostIds());

            // 1.2 获得用户基本信息
            // 注意：这里不需要忽略租户过滤，因为我们已经通过 userTenantRelation 验证了用户在当前租户中
            // 如果用户的主租户不是当前租户，我们使用 userTenantRelation 中的信息来构建用户对象
            AdminUserDO user = userService.getUser(userId);
            if (user == null) {
                // 如果在当前租户查不到用户（用户的主租户不是当前租户），则使用忽略租户过滤的方法获取用户基本信息
                user = userService.getUserIgnoreTenant(userId);
                if (user != null) {
                    // 使用租户关联表中的信息覆盖用户信息
                    user.setNickname(userTenantRelation.getNickname() != null ? userTenantRelation.getNickname() : user.getNickname());
                    user.setAvatar(userTenantRelation.getAvatar() != null ? userTenantRelation.getAvatar() : user.getAvatar());
                    user.setStatus(userTenantRelation.getStatus());
                }
            }

            if (user == null) {
                return success(null);
            }

            // 1.3 获得用户在当前租户下的角色列表
            // 直接从用户角色表中获取，不再使用租户关联表的 role_ids 字段
            Set<Long> roleIds = permissionService.getUserRoleIdListByUserId(userId);
            log.info("[getPermissionInfo] 从用户角色表获取到的角色ID: {}", roleIds);

            if (CollUtil.isEmpty(roleIds)) {
                log.warn("[getPermissionInfo] 用户在当前租户下没有任何角色，userId={}, tenantId={}", userId, permissionTenantId);
                // 返回用户信息，但标记为无角色权限
                AuthPermissionInfoRespVO respVO = AuthConvert.INSTANCE.convert(user, Collections.emptyList(), Collections.emptyList());
                respVO.setHasRole(false); // 标记用户没有角色
                return success(respVO);
            }
            List<RoleDO> roles = roleService.getRoleList(roleIds);
            roles.removeIf(role -> !CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus())); // 移除禁用的角色

            // 1.4 获得菜单列表
            Set<Long> effectiveRoleIds = convertSet(roles, RoleDO::getId);
            Supplier<List<MenuDO>> allMenusSupplier = Suppliers.memoize(menuService::getMenuList);
            Supplier<Set<Long>> allMenuIdsSupplier = Suppliers.memoize(() -> convertSet(allMenusSupplier.get(), MenuDO::getId));
            boolean hasSuperAdmin = roleService.hasAnySuperAdmin(effectiveRoleIds);
            Set<Long> menuIds = permissionService.getRoleMenuListByRoleId(effectiveRoleIds, allMenuIdsSupplier);
            List<MenuDO> menuList = hasSuperAdmin ? new ArrayList<>(allMenusSupplier.get()) : menuService.getMenuList(menuIds);
            List<MenuDO> packageFilteredMenuList = new ArrayList<>(menuList);

            // 1.4 按租户套餐过滤菜单（重要：确保只显示租户套餐内的菜单）
            tenantService.handleTenantMenu(tenantMenuIds ->
                    packageFilteredMenuList.removeIf(menu -> !CollUtil.contains(tenantMenuIds, menu.getId())),
                allMenuIdsSupplier);

            // 菜单展示仍然需要过滤父级禁用/缺失的节点；按钮权限则直接基于套餐过滤后的完整菜单集合返回
            List<MenuDO> filteredMenuList = menuService.filterDisableMenus(new ArrayList<>(packageFilteredMenuList));

            // 2. 拼接结果返回
            AuthPermissionInfoRespVO respVO = AuthConvert.INSTANCE.convert(user, roles, filteredMenuList);
            Set<String> permissions = convertSet(packageFilteredMenuList, MenuDO::getPermission);
            permissions.removeIf(StrUtil::isBlank);
            respVO.setPermissions(permissions);
            respVO.setHasRole(true); // 标记用户有角色
            log.info("[getPermissionInfo] 最终返回结果: hasRole={}, roles={}, permissions={}, menus={}",
                respVO.getHasRole(), respVO.getRoles(), respVO.getPermissions().size(), respVO.getMenus().size());
            return success(respVO);
        } finally {
            TenantContextHolder.setTenantId(oldTenantId);
            TenantContextHolder.setIgnore(oldIgnore);
        }
    }

    @PostMapping("/register")
    @PermitAll
    @Operation(summary = "注册用户")
    public CommonResult<AuthLoginRespVO> register(@RequestBody @Valid AuthRegisterReqVO registerReqVO) {
        return success(authService.register(registerReqVO));
    }

    // ========== 短信登录相关 ==========

    @PostMapping("/sms-login")
    @PermitAll
    @Operation(summary = "使用短信验证码登录")
    public CommonResult<AuthLoginRespVO> smsLogin(@RequestBody @Valid AuthSmsLoginReqVO reqVO) {
        return success(authService.smsLogin(reqVO));
    }

    @PostMapping("/send-sms-code")
    @PermitAll
    @Operation(summary = "发送手机验证码")
    public CommonResult<Boolean> sendLoginSmsCode(@RequestBody @Valid AuthSmsSendReqVO reqVO) {
        authService.sendSmsCode(reqVO);
        return success(true);
    }

    @PostMapping("/reset-password")
    @PermitAll
    @Operation(summary = "重置密码")
    public CommonResult<Boolean> resetPassword(@RequestBody @Valid AuthResetPasswordReqVO reqVO) {
        authService.resetPassword(reqVO);
        return success(true);
    }

    // ========== 社交登录相关 ==========

    @GetMapping("/social-auth-redirect")
    @PermitAll
    @TenantIgnore
    @Operation(summary = "社交授权的跳转")
    @Parameters({
            @Parameter(name = "type", description = "社交类型", required = true),
            @Parameter(name = "redirectUri", description = "回调路径")
    })
    public CommonResult<String> socialLogin(@RequestParam("type") Integer type,
                                            @RequestParam("redirectUri") String redirectUri) {
        return success(socialClientService.getAuthorizeUrl(
                type, UserTypeEnum.ADMIN.getValue(), redirectUri));
    }

    @PostMapping("/social-login")
    @PermitAll
    @TenantIgnore
    @Operation(summary = "社交快捷登录，使用 code 授权码", description = "适合未登录的用户，但是社交账号已绑定用户")
    public CommonResult<AuthLoginRespVO> socialQuickLogin(@RequestBody @Valid AuthSocialLoginReqVO reqVO) {
        return success(authService.socialLogin(reqVO));
    }

    @PostMapping("/social-pre-login")
    @PermitAll
    @TenantIgnore
    @Operation(summary = "社交预登录，使用 code 授权码", description = "返回可选租户与预登录令牌，需再调用 tenant-login 完成登录")
    public CommonResult<AuthPreLoginRespVO> socialPreLogin(@RequestBody @Valid AuthSocialLoginReqVO reqVO) {
        return success(authService.socialPreLogin(reqVO));
    }

    @PostMapping("/social-browser-handoff/complete")
    @PermitAll
    @TenantIgnore
    @Operation(summary = "完成社交登录浏览器握手", description = "钉钉内完成授权后写回浏览器待消费的预登录结果")
    public CommonResult<AuthSocialHandoffStatusRespVO> completeSocialBrowserHandoff(
            @RequestBody @Valid AuthSocialHandoffCompleteReqVO reqVO) {
        AuthPreLoginRespVO respVO = authService.socialPreLogin(reqVO);
        AuthSocialBrowserHandoffCacheDO cacheDO = new AuthSocialBrowserHandoffCacheDO();
        cacheDO.setStatus(AuthSocialHandoffStatusRespVO.STATUS_SUCCESS);
        cacheDO.setMessage("AUTHORIZED");
        cacheDO.setPreAuthToken(respVO.getPreAuthToken());
        cacheDO.setTenantList(respVO.getTenantList());
        authSocialBrowserHandoffRedisDAO.set(reqVO.getHandoffId(), cacheDO, SOCIAL_BROWSER_HANDOFF_EXPIRE_SECONDS);
        return success(buildSocialHandoffStatus(cacheDO));
    }

    @PostMapping("/social-browser-handoff/fail")
    @PermitAll
    @TenantIgnore
    @Operation(summary = "标记社交登录浏览器握手失败")
    public CommonResult<Boolean> failSocialBrowserHandoff(@RequestBody @Valid AuthSocialHandoffFailReqVO reqVO) {
        AuthSocialBrowserHandoffCacheDO cacheDO = new AuthSocialBrowserHandoffCacheDO();
        cacheDO.setStatus(AuthSocialHandoffStatusRespVO.STATUS_FAILED);
        cacheDO.setMessage(reqVO.getMessage());
        authSocialBrowserHandoffRedisDAO.set(reqVO.getHandoffId(), cacheDO, SOCIAL_BROWSER_HANDOFF_EXPIRE_SECONDS);
        return success(true);
    }

    @GetMapping("/social-browser-handoff/status")
    @PermitAll
    @TenantIgnore
    @Operation(summary = "获取社交登录浏览器握手状态")
    public CommonResult<AuthSocialHandoffStatusRespVO> getSocialBrowserHandoffStatus(
            @RequestParam("handoffId") String handoffId) {
        AuthSocialBrowserHandoffCacheDO cacheDO = authSocialBrowserHandoffRedisDAO.get(handoffId);
        if (cacheDO == null) {
            return success(AuthSocialHandoffStatusRespVO.pending());
        }
        return success(buildSocialHandoffStatus(cacheDO));
    }

    private static AuthSocialHandoffStatusRespVO buildSocialHandoffStatus(AuthSocialBrowserHandoffCacheDO cacheDO) {
        return AuthSocialHandoffStatusRespVO.builder()
                .status(cacheDO.getStatus())
                .message(cacheDO.getMessage())
                .preAuthToken(cacheDO.getPreAuthToken())
                .tenantList(cacheDO.getTenantList())
                .build();
    }

}
