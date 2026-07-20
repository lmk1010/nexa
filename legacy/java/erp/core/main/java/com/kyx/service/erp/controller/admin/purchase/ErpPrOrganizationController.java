package com.kyx.service.erp.controller.admin.purchase;

import com.kyx.foundation.apilog.core.annotation.ApiAccessLog;
import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageParam;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.excel.core.util.ExcelUtils;
import com.kyx.service.erp.controller.admin.purchase.vo.prorganization.ErpPrOrganizationPageReqVO;
import com.kyx.service.erp.controller.admin.purchase.vo.prorganization.ErpPrOrganizationRespVO;
import com.kyx.service.erp.controller.admin.purchase.vo.prorganization.ErpPrOrganizationSaveReqVO;
import com.kyx.service.erp.dal.dataobject.purchase.ErpPrOrganizationDO;
import com.kyx.service.erp.service.purchase.ErpPrOrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.kyx.foundation.apilog.core.enums.OperateTypeEnum.EXPORT;
import static com.kyx.foundation.common.pojo.CommonResult.success;
import static com.kyx.foundation.common.util.collection.CollectionUtils.convertList;

@Tag(name = "管理后台 - ERP 采购组织")
@RestController
@RequestMapping("/erp/pr-organization")
@Validated
@Slf4j
public class ErpPrOrganizationController {

    @Resource
    private ErpPrOrganizationService prOrganizationService;

    @PostMapping("/create")
    @Operation(summary = "创建采购组织")
    @PreAuthorize("@ss.hasPermission('erp:pr-organization:create')")
    public CommonResult<Long> createPrOrganization(@Valid @RequestBody ErpPrOrganizationSaveReqVO createReqVO) {
        return success(prOrganizationService.createPrOrganization(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新采购组织")
    @PreAuthorize("@ss.hasPermission('erp:pr-organization:update')")
    public CommonResult<Boolean> updatePrOrganization(@Valid @RequestBody ErpPrOrganizationSaveReqVO updateReqVO) {
        prOrganizationService.updatePrOrganization(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除采购组织")
    @Parameter(name = "ids", description = "编号数组", required = true)
    @PreAuthorize("@ss.hasPermission('erp:pr-organization:delete')")
    public CommonResult<Boolean> deletePrOrganization(@RequestParam("ids") List<Long> ids) {
        prOrganizationService.deletePrOrganizationBatch(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得采购组织")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:pr-organization:query')")
    public CommonResult<ErpPrOrganizationRespVO> getPrOrganization(@RequestParam("id") Long id) {
        ErpPrOrganizationDO prOrganization = prOrganizationService.getPrOrganization(id);
        ErpPrOrganizationRespVO respVO = BeanUtils.toBean(prOrganization, ErpPrOrganizationRespVO.class);
        
        // 填充上级组织名称
        if (prOrganization != null && prOrganization.getParentId() != null && prOrganization.getParentId() > 0) {
            ErpPrOrganizationDO parentOrganization = prOrganizationService.getPrOrganization(prOrganization.getParentId());
            if (parentOrganization != null) {
                respVO.setParentName(parentOrganization.getName());
            }
        }
        
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得采购组织分页")
    @PreAuthorize("@ss.hasPermission('erp:pr-organization:query')")
    public CommonResult<PageResult<ErpPrOrganizationRespVO>> getPrOrganizationPage(@Valid ErpPrOrganizationPageReqVO pageReqVO) {
        PageResult<ErpPrOrganizationDO> pageResult = prOrganizationService.getPrOrganizationPage(pageReqVO);
        PageResult<ErpPrOrganizationRespVO> respPageResult = BeanUtils.toBean(pageResult, ErpPrOrganizationRespVO.class);
        
        // 批量获取上级组织信息
        List<Long> parentIds = respPageResult.getList().stream()
                .map(ErpPrOrganizationRespVO::getParentId)
                .filter(parentId -> parentId != null && parentId > 0)
                .distinct()
                .collect(Collectors.toList());
        
        if (!parentIds.isEmpty()) {
            Map<Long, ErpPrOrganizationDO> parentMap = prOrganizationService.getPrOrganizationMap(parentIds);
            respPageResult.getList().forEach(respVO -> {
                if (respVO.getParentId() != null && respVO.getParentId() > 0) {
                    ErpPrOrganizationDO parent = parentMap.get(respVO.getParentId());
                    if (parent != null) {
                        respVO.setParentName(parent.getName());
                    }
                }
            });
        }
        
        return success(respPageResult);
    }

    @GetMapping("/tree")
    @Operation(summary = "获得采购组织树形列表")
    @PreAuthorize("@ss.hasPermission('erp:pr-organization:query')")
    public CommonResult<List<ErpPrOrganizationRespVO>> getPrOrganizationTree() {
        List<ErpPrOrganizationDO> list = prOrganizationService.getPrOrganizationTree();
        List<ErpPrOrganizationRespVO> respList = BeanUtils.toBean(list, ErpPrOrganizationRespVO.class);
        
        // 构建树形结构
        List<ErpPrOrganizationRespVO> treeList = buildTreeStructure(respList);
        
        return success(treeList);
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得采购组织精简列表", description = "只包含被启用的采购组织，主要用于前端的下拉选项")
    @PreAuthorize("@ss.hasPermission('erp:pr-organization:query')")
    public CommonResult<List<ErpPrOrganizationRespVO>> getPrOrganizationSimpleList() {
        List<ErpPrOrganizationDO> list = prOrganizationService.getPrOrganizationListByStatus(CommonStatusEnum.ENABLE.getStatus());
        return success(convertList(list, organization -> {
            ErpPrOrganizationRespVO respVO = new ErpPrOrganizationRespVO();
            respVO.setId(organization.getId());
            respVO.setName(organization.getName());
            respVO.setCode(organization.getCode());
            return respVO;
        }));
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新采购组织状态")
    @PreAuthorize("@ss.hasPermission('erp:pr-organization:update')")
    public CommonResult<Boolean> updatePrOrganizationStatus(
            @RequestParam("id") Long id,
            @RequestParam("status") Integer status) {
        prOrganizationService.updatePrOrganizationStatus(id, status);
        return success(true);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出采购组织 Excel")
    @PreAuthorize("@ss.hasPermission('erp:pr-organization:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportPrOrganizationExcel(@Valid ErpPrOrganizationPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ErpPrOrganizationDO> list = prOrganizationService.getPrOrganizationPage(pageReqVO).getList();
        
        // 转换为响应VO并填充上级组织名称
        List<ErpPrOrganizationRespVO> respList = BeanUtils.toBean(list, ErpPrOrganizationRespVO.class);
        
        // 批量获取上级组织信息
        List<Long> parentIds = respList.stream()
                .map(ErpPrOrganizationRespVO::getParentId)
                .filter(parentId -> parentId != null && parentId > 0)
                .distinct()
                .collect(Collectors.toList());
        
        if (!parentIds.isEmpty()) {
            Map<Long, ErpPrOrganizationDO> parentMap = prOrganizationService.getPrOrganizationMap(parentIds);
            respList.forEach(respVO -> {
                if (respVO.getParentId() != null && respVO.getParentId() > 0) {
                    ErpPrOrganizationDO parent = parentMap.get(respVO.getParentId());
                    if (parent != null) {
                        respVO.setParentName(parent.getName());
                    }
                }
            });
        }
        
        // 导出 Excel
        ExcelUtils.write(response, "采购组织.xls", "数据", ErpPrOrganizationRespVO.class, respList);
    }

    /**
     * 构建树形结构
     *
     * @param list 扁平列表
     * @return 树形列表
     */
    private List<ErpPrOrganizationRespVO> buildTreeStructure(List<ErpPrOrganizationRespVO> list) {
        Map<Long, List<ErpPrOrganizationRespVO>> parentGroupMap = list.stream()
                .collect(Collectors.groupingBy(org -> org.getParentId() == null ? 0L : org.getParentId()));

        return buildTreeRecursive(parentGroupMap, 0L);
    }

    /**
     * 递归构建树形结构
     *
     * @param parentGroupMap 按父ID分组的Map
     * @param parentId 父ID
     * @return 子树列表
     */
    private List<ErpPrOrganizationRespVO> buildTreeRecursive(Map<Long, List<ErpPrOrganizationRespVO>> parentGroupMap, Long parentId) {
        List<ErpPrOrganizationRespVO> children = parentGroupMap.get(parentId);
        if (children == null || children.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        return children.stream()
                .sorted((a, b) -> {
                    int sortResult = Integer.compare(a.getSort() != null ? a.getSort() : 0, 
                                                   b.getSort() != null ? b.getSort() : 0);
                    return sortResult != 0 ? sortResult : Long.compare(a.getId(), b.getId());
                })
                .peek(child -> {
                    // 递归获取子节点
                    List<ErpPrOrganizationRespVO> subChildren = buildTreeRecursive(parentGroupMap, child.getId());
                    child.setChildren(subChildren);
                })
                .collect(Collectors.toList());
    }

} 