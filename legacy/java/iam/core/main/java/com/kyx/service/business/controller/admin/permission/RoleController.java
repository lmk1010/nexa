package com.kyx.service.business.controller.admin.permission;

import com.kyx.foundation.apilog.core.annotation.ApiAccessLog;
import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageParam;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.excel.core.util.ExcelUtils;
import com.kyx.service.business.controller.admin.permission.vo.role.*;
import com.kyx.service.business.controller.admin.permission.vo.role.RolePageReqVO;
import com.kyx.service.business.controller.admin.permission.vo.role.RoleRespVO;
import com.kyx.service.business.controller.admin.permission.vo.role.RoleSaveReqVO;
import com.kyx.service.business.dal.dataobject.permission.RoleDO;
import com.kyx.service.business.dal.dataobject.tenant.TenantDO;
import com.kyx.service.business.service.permission.RoleService;
import com.kyx.service.business.service.tenant.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.kyx.foundation.apilog.core.enums.OperateTypeEnum.EXPORT;
import static com.kyx.foundation.common.pojo.CommonResult.success;
import static java.util.Collections.singleton;

@Tag(name = "管理后台 - 角色")
@RestController
@RequestMapping("/system/role")
@Validated
public class RoleController {

    @Resource
    private RoleService roleService;
    @Resource
    private TenantService tenantService;

    @PostMapping("/create")
    @Operation(summary = "创建角色")
    @PreAuthorize("@ss.hasPermission('system:role:create')")
    public CommonResult<Long> createRole(@Valid @RequestBody RoleSaveReqVO createReqVO) {
        return success(roleService.createRole(createReqVO, null));
    }

    @PutMapping("/update")
    @Operation(summary = "修改角色")
    @PreAuthorize("@ss.hasPermission('system:role:update')")
    public CommonResult<Boolean> updateRole(@Valid @RequestBody RoleSaveReqVO updateReqVO) {
        roleService.updateRole(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除角色")
    @Parameter(name = "id", description = "角色编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:role:delete')")
    public CommonResult<Boolean> deleteRole(@RequestParam("id") Long id) {
        roleService.deleteRole(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得角色信息")
    @PreAuthorize("@ss.hasPermission('system:role:query')")
    public CommonResult<RoleRespVO> getRole(@RequestParam("id") Long id) {
        RoleDO role = roleService.getRole(id);
        RoleRespVO respVO = BeanUtils.toBean(role, RoleRespVO.class);
        if (respVO != null) {
            fillTenantNames(java.util.Collections.singletonList(respVO));
        }
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得角色分页")
    @PreAuthorize("@ss.hasPermission('system:role:query')")
    public CommonResult<PageResult<RoleRespVO>> getRolePage(RolePageReqVO pageReqVO) {
        PageResult<RoleDO> pageResult = roleService.getRolePage(pageReqVO);
        PageResult<RoleRespVO> voResult = BeanUtils.toBean(pageResult, RoleRespVO.class);
        fillTenantNames(voResult.getList());
        return success(voResult);
    }

    @GetMapping({"/list-all-simple", "/simple-list"})
    @Operation(summary = "获取角色精简信息列表", description = "只包含被开启的角色，主要用于前端的下拉选项")
    public CommonResult<List<RoleRespVO>> getSimpleRoleList() {
        List<RoleDO> list = roleService.getRoleListByStatus(singleton(CommonStatusEnum.ENABLE.getStatus()));
        list.sort(Comparator.comparing(RoleDO::getSort));
        return success(BeanUtils.toBean(list, RoleRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出角色 Excel")
    @ApiAccessLog(operateType = EXPORT)
    @PreAuthorize("@ss.hasPermission('system:role:export')")
    public void export(HttpServletResponse response, @Validated RolePageReqVO exportReqVO) throws IOException {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<RoleDO> list = roleService.getRolePage(exportReqVO).getList();
        // 输出
        ExcelUtils.write(response, "角色数据.xls", "数据", RoleRespVO.class,
                BeanUtils.toBean(list, RoleRespVO.class));
    }

    private void fillTenantNames(List<RoleRespVO> roleList) {
        if (roleList == null || roleList.isEmpty()) {
            return;
        }
        Map<Long, String> tenantMap = roleList.stream()
                .map(RoleRespVO::getTenantId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> {
                            TenantDO tenant = tenantService.getTenant(id);
                            return tenant != null ? tenant.getName() : "";
                        }
                ));
        roleList.forEach(role -> {
            if (role.getTenantId() != null) {
                role.setTenantName(tenantMap.get(role.getTenantId()));
            }
        });
    }

}
