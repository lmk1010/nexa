package com.kyx.service.business.controller.admin.dept;

import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.business.controller.admin.dept.vo.dept.DeptListReqVO;
import com.kyx.service.business.controller.admin.dept.vo.dept.DeptRespVO;
import com.kyx.service.business.controller.admin.dept.vo.dept.DeptSaveReqVO;
import com.kyx.service.business.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import com.kyx.service.business.controller.admin.dept.vo.dept.DeptTreeRespVO;
import com.kyx.service.business.dal.dataobject.dept.DeptDO;
import com.kyx.service.business.dal.dataobject.tenant.TenantDO;
import com.kyx.service.business.service.dept.DeptService;
import com.kyx.service.business.service.tenant.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.kyx.foundation.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 部门")
@RestController
@RequestMapping("/system/dept")
@Validated
public class DeptController {

    @Resource
    private DeptService deptService;
    @Resource
    private TenantService tenantService;

    @PostMapping("create")
    @Operation(summary = "创建部门")
    @PreAuthorize("@ss.hasPermission('system:dept:create')")
    public CommonResult<Long> createDept(@Valid @RequestBody DeptSaveReqVO createReqVO) {
        Long deptId = deptService.createDept(createReqVO);
        return success(deptId);
    }

    @PutMapping("update")
    @Operation(summary = "更新部门")
    @PreAuthorize("@ss.hasPermission('system:dept:update')")
    public CommonResult<Boolean> updateDept(@Valid @RequestBody DeptSaveReqVO updateReqVO) {
        deptService.updateDept(updateReqVO);
        return success(true);
    }

    @DeleteMapping("delete")
    @Operation(summary = "删除部门")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:dept:delete')")
    public CommonResult<Boolean> deleteDept(@RequestParam("id") Long id) {
        deptService.deleteDept(id);
        return success(true);
    }

    @DeleteMapping("delete-with-transfer")
    @Operation(summary = "删除部门（带员工转移）")
    @Parameter(name = "id", description = "部门编号", required = true, example = "1024")
    @Parameter(name = "transferDeptId", description = "员工转移到的部门编号", required = false, example = "1025")
    @PreAuthorize("@ss.hasPermission('system:dept:delete')")
    public CommonResult<Boolean> deleteDeptWithTransfer(@RequestParam("id") Long id, 
                                                       @RequestParam(value = "transferDeptId", required = false) Long transferDeptId) {
        deptService.deleteDeptWithUserTransfer(id, transferDeptId);
        return success(true);
    }

    @GetMapping("/list")
    @Operation(summary = "获取部门列表")
    @PreAuthorize("@ss.hasPermission('system:dept:query')")
    public CommonResult<List<DeptRespVO>> getDeptList(DeptListReqVO reqVO) {
        List<DeptDO> list = deptService.getDeptList(reqVO);
        List<DeptRespVO> respList = BeanUtils.toBean(list, DeptRespVO.class);
        
        // 统计员工数量
        fillTenantNames(respList);

        if (!list.isEmpty()) {
            List<Long> deptIds = list.stream().map(DeptDO::getId).collect(Collectors.toList());
            Map<Long, Integer> userCountMap = deptService.getUserCountByDeptIds(deptIds);
            
            // 设置员工数量
            for (DeptRespVO resp : respList) {
                resp.setUserCount(userCountMap.getOrDefault(resp.getId(), 0));
            }
        }
        
        return success(respList);
    }

    @GetMapping(value = {"/list-all-simple", "/simple-list"})
    @Operation(summary = "获取部门精简信息列表", description = "只包含被开启的部门，主要用于前端的下拉选项")
    public CommonResult<List<DeptSimpleRespVO>> getSimpleDeptList() {
        List<DeptDO> list = deptService.getDeptList(
                new DeptListReqVO().setStatus(CommonStatusEnum.ENABLE.getStatus()));
        return success(BeanUtils.toBean(list, DeptSimpleRespVO.class));
    }

    @GetMapping("/simple-list-by-tenants")
    @Operation(summary = "根据租户ID列表获取部门精简信息列表", description = "支持跨租户查询部门，用于流程审批人选择")
    @Parameter(name = "tenantIds", description = "租户ID列表，逗号分隔。为空则查询当前租户", example = "1,2,3")
    public CommonResult<List<DeptSimpleRespVO>> getSimpleDeptListByTenants(
            @RequestParam(value = "tenantIds", required = false) String tenantIds) {
        List<DeptDO> list = deptService.getDeptListByTenants(tenantIds);
        return success(BeanUtils.toBean(list, DeptSimpleRespVO.class));
    }

    @GetMapping("/tree-with-employee-count")
    @Operation(summary = "获取部门树（含员工数量）")
    @PreAuthorize("@ss.hasAnyPermissions('system:dept:query','hr:employee:query')")
    public CommonResult<List<DeptTreeRespVO>> getDeptTreeWithEmployeeCount() {
        List<DeptDO> list = deptService.getDeptList(
                new DeptListReqVO().setStatus(CommonStatusEnum.ENABLE.getStatus()));
        List<Long> deptIds = list.stream().map(DeptDO::getId).collect(Collectors.toList());
        Map<Long, Integer> countMap = deptService.getEmployeeCountByDeptIds(deptIds);
        List<DeptTreeRespVO> respList = BeanUtils.toBean(list, DeptTreeRespVO.class);
        for (DeptTreeRespVO resp : respList) {
            resp.setUserCount(countMap.getOrDefault(resp.getId(), 0));
        }
        return success(respList);
    }

    @GetMapping("/get")
    @Operation(summary = "获得部门信息")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:dept:query')")
    public CommonResult<DeptRespVO> getDept(@RequestParam("id") Long id) {
        DeptDO dept = deptService.getDept(id);
        DeptRespVO respVO = BeanUtils.toBean(dept, DeptRespVO.class);
        if (respVO != null) {
            fillTenantNames(java.util.Collections.singletonList(respVO));
        }
        return success(respVO);
    }

    private void fillTenantNames(List<DeptRespVO> deptList) {
        if (deptList == null || deptList.isEmpty()) {
            return;
        }
        Map<Long, String> tenantMap = deptList.stream()
                .map(DeptRespVO::getTenantId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> {
                            TenantDO tenant = tenantService.getTenant(id);
                            return tenant != null ? tenant.getName() : "";
                        }
                ));
        deptList.forEach(dept -> {
            if (dept.getTenantId() != null) {
                dept.setTenantName(tenantMap.get(dept.getTenantId()));
            }
        });
    }

}
