package com.kyx.service.business.controller.admin.tenant;

import com.kyx.foundation.apilog.core.annotation.ApiAccessLog;
import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageParam;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.excel.core.util.ExcelUtils;
import com.kyx.foundation.tenant.core.aop.TenantIgnore;
import com.kyx.service.business.controller.admin.tenant.vo.tenant.BindUserToTenantReqVO;
import com.kyx.service.business.controller.admin.tenant.vo.tenant.TenantFeatureConfigRespVO;
import com.kyx.service.business.controller.admin.tenant.vo.tenant.TenantPageReqVO;
import com.kyx.service.business.controller.admin.tenant.vo.tenant.TenantRespVO;
import com.kyx.service.business.controller.admin.tenant.vo.tenant.TenantSaveReqVO;
import com.kyx.service.business.controller.admin.tenant.vo.tenant.TenantUserPageReqVO;
import com.kyx.service.business.controller.admin.tenant.vo.tenant.TenantUserRespVO;
import com.kyx.service.business.controller.admin.tenant.vo.tenant.TenantTreeRespVO;
import com.kyx.service.business.controller.admin.tenant.vo.tenant.UnbindUserFromTenantReqVO;
import com.kyx.service.business.controller.admin.tenant.vo.tenant.UpdateUserRolesInTenantReqVO;
import com.kyx.service.business.controller.admin.tenant.vo.tenant.UserTenantRespVO;
import com.kyx.service.business.controller.admin.permission.vo.role.RoleSimpleRespVO;
import com.kyx.service.business.dal.dataobject.tenant.TenantDO;
import com.kyx.service.business.dal.dataobject.permission.RoleDO;
import com.kyx.service.business.service.tenant.TenantService;
import com.kyx.service.business.service.tenant.TenantFeatureConfigService;
import com.kyx.service.business.service.permission.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

import static com.kyx.foundation.apilog.core.enums.OperateTypeEnum.EXPORT;
import static com.kyx.foundation.common.pojo.CommonResult.success;
import static com.kyx.foundation.common.util.collection.CollectionUtils.convertList;
import static com.kyx.foundation.web.core.util.WebFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 租户")
@RestController
@RequestMapping("/system/tenant")
public class TenantController {

    @Resource
    private TenantService tenantService;

    @Resource
    private RoleService roleService;

    @Resource
    private TenantFeatureConfigService tenantFeatureConfigService;

    @GetMapping("/get-id-by-name")
    @PermitAll
    @TenantIgnore
    @Operation(summary = "使用租户名，获得租户编号", description = "登录界面，根据用户的租户名，获得租户编号")
    @Parameter(name = "name", description = "租户名", required = true, example = "1024")
    public CommonResult<Long> getTenantIdByName(@RequestParam("name") String name) {
        TenantDO tenant = tenantService.getTenantByName(name);
        return success(tenant != null ? tenant.getId() : null);
    }

    @GetMapping({ "simple-list" })
    @PermitAll
    @TenantIgnore
    @Operation(summary = "获取租户精简信息列表", description = "只包含被开启的租户，用于【首页】功能的选择租户选项")
    public CommonResult<List<TenantRespVO>> getTenantSimpleList() {
        List<TenantDO> list = tenantService.getTenantListByStatus(CommonStatusEnum.ENABLE.getStatus());
        return success(convertList(list, tenantDO ->
                new TenantRespVO().setId(tenantDO.getId()).setName(tenantDO.getName())));
    }

    @GetMapping("/get-by-website")
    @PermitAll
    @TenantIgnore
    @Operation(summary = "使用域名，获得租户信息", description = "登录界面，根据用户的域名，获得租户信息")
    @Parameter(name = "website", description = "域名", required = true, example = "www.iocoder.cn")
    public CommonResult<TenantRespVO> getTenantByWebsite(@RequestParam("website") String website) {
        TenantDO tenant = tenantService.getTenantByWebsite(website);
        if (tenant == null || CommonStatusEnum.isDisable(tenant.getStatus())) {
            return success(null);
        }
        return success(new TenantRespVO().setId(tenant.getId()).setName(tenant.getName()));
    }

    @GetMapping("/get-user-tenant-list")
    @Operation(summary = "根据当前登录用户ID获取用户绑定的租户信息")
    public CommonResult<List<UserTenantRespVO>> getUserTenantList() {
        // 获取当前登录用户ID
        Long userId = getLoginUserId();
        List<UserTenantRespVO> userTenantList = tenantService.getUserTenantList(userId);
        return success(userTenantList);
    }

    @GetMapping("/get-user-tenant-list-by-username")
    @PermitAll
    @TenantIgnore
    @Operation(summary = "根据用户名（或手机号）获取用户绑定的租户信息", description = "登录界面使用")
    @Parameter(name = "username", description = "用户名或手机号", required = true, example = "admin")
    public CommonResult<List<UserTenantRespVO>> getUserTenantListByUsername(@RequestParam("username") String username) {
        return success(tenantService.getUserTenantListByUsername(username));
    }

    @GetMapping("/get-tenant-users")
    @Operation(summary = "获取租户下的所有用户")
    @Parameter(name = "tenantId", description = "租户ID", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('system:tenant:query')")
    public CommonResult<List<TenantUserRespVO>> getTenantUsers(@RequestParam("tenantId") Long tenantId) {
        List<TenantUserRespVO> userList = tenantService.getTenantUsers(tenantId);
        return success(userList);
    }

    @GetMapping("/get-tenant-users-page")
    @Operation(summary = "分页查询租户下的用户")
    @PreAuthorize("@ss.hasPermission('system:tenant:query')")
    public CommonResult<PageResult<TenantUserRespVO>> getTenantUsersPage(@Valid TenantUserPageReqVO pageReqVO) {
        PageResult<TenantUserRespVO> pageResult = tenantService.getTenantUsersPage(pageReqVO);
        return success(pageResult);
    }

    @PostMapping("/bind-user")
    @Operation(summary = "绑定用户到租户")
    @PreAuthorize("@ss.hasPermission('system:tenant:update')")
    public CommonResult<Boolean> bindUserToTenant(@Valid @RequestBody BindUserToTenantReqVO reqVO) {
        tenantService.bindUserToTenant(reqVO);
        return success(true);
    }

    @PostMapping("/unbind-user")
    @Operation(summary = "解绑用户从租户")
    @PreAuthorize("@ss.hasPermission('system:tenant:update')")
    public CommonResult<Boolean> unbindUserFromTenant(@Valid @RequestBody UnbindUserFromTenantReqVO reqVO) {
        tenantService.unbindUserFromTenant(reqVO.getUserId(), reqVO.getTenantId());
        return success(true);
    }

    @GetMapping("/get-tenant-roles")
    @Operation(summary = "获取租户下可用的角色列表")
    @Parameter(name = "tenantId", description = "租户ID", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('system:tenant:query')")
    public CommonResult<List<RoleSimpleRespVO>> getTenantRoles(@RequestParam("tenantId") Long tenantId) {
        List<RoleDO> roles = tenantService.getTenantRoles(tenantId);
        return success(convertList(roles, role -> {
            RoleSimpleRespVO vo = new RoleSimpleRespVO();
            vo.setId(role.getId());
            vo.setName(role.getName());
            return vo;
        }));
    }

    @PutMapping("/update-user-roles")
    @Operation(summary = "更新用户在租户内的角色")
    @PreAuthorize("@ss.hasPermission('system:tenant:update')")
    public CommonResult<Boolean> updateUserRolesInTenant(@Valid @RequestBody UpdateUserRolesInTenantReqVO reqVO) {
        tenantService.updateUserRolesInTenant(reqVO);
        return success(true);
    }

    @PostMapping("/create")
    @Operation(summary = "创建租户")
    @PreAuthorize("@ss.hasPermission('system:tenant:create')")
    public CommonResult<Long> createTenant(@Valid @RequestBody TenantSaveReqVO createReqVO) {
        return success(tenantService.createTenant(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新租户")
    @PreAuthorize("@ss.hasPermission('system:tenant:update')")
    public CommonResult<Boolean> updateTenant(@Valid @RequestBody TenantSaveReqVO updateReqVO) {
        tenantService.updateTenant(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除租户")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:tenant:delete')")
    public CommonResult<Boolean> deleteTenant(@RequestParam("id") Long id) {
        tenantService.deleteTenant(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得租户")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:tenant:query')")
    public CommonResult<TenantRespVO> getTenant(@RequestParam("id") Long id) {
        TenantDO tenant = tenantService.getTenant(id);
        TenantRespVO respVO = BeanUtils.toBean(tenant, TenantRespVO.class);
        respVO.setFeatureConfigs(convertList(tenantFeatureConfigService.getTenantFeatureConfigs(id), item -> {
            TenantFeatureConfigRespVO vo = new TenantFeatureConfigRespVO();
            vo.setFeatureCode(item.getFeatureCode());
            vo.setCrossTenantEnabled(Integer.valueOf(1).equals(item.getCrossTenantEnabled()));
            return vo;
        }));
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得租户分页")
    @PreAuthorize("@ss.hasPermission('system:tenant:query')")
    public CommonResult<PageResult<TenantRespVO>> getTenantPage(@Valid TenantPageReqVO pageVO) {
        PageResult<TenantDO> pageResult = tenantService.getTenantPage(pageVO);
        return success(BeanUtils.toBean(pageResult, TenantRespVO.class));
    }

    @GetMapping("/tree")
    @Operation(summary = "获得租户树")
    @PreAuthorize("@ss.hasAnyPermissions('system:tenant:query','hr:employee:query')")
    public CommonResult<List<TenantTreeRespVO>> getTenantTree() {
        Long tenantId = com.kyx.foundation.tenant.core.context.TenantContextHolder.getRequiredTenantId();
        List<Long> allowedIds = tenantService.getAllowedTenantIds(tenantId);
        List<TenantDO> list = tenantService.getTenantListByIds(allowedIds);
        return success(BeanUtils.toBean(list, TenantTreeRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出租户 Excel")
    @PreAuthorize("@ss.hasPermission('system:tenant:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportTenantExcel(@Valid TenantPageReqVO exportReqVO, HttpServletResponse response) throws IOException {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<TenantDO> list = tenantService.getTenantPage(exportReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "租户.xls", "数据", TenantRespVO.class,
                BeanUtils.toBean(list, TenantRespVO.class));
    }

}
