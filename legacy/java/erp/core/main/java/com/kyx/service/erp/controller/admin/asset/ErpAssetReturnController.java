package com.kyx.service.erp.controller.admin.asset;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.erp.controller.admin.asset.vo.assetreturn.ErpAssetReturnPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.assetreturn.ErpAssetReturnRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.assetreturn.ErpAssetReturnSaveReqVO;
import com.kyx.service.erp.service.asset.ErpAssetReturnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static com.kyx.foundation.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 资产归还记录")
@RestController
@RequestMapping("/erp/asset-return")
@Validated
public class ErpAssetReturnController {

    @Resource
    private ErpAssetReturnService returnService;

    @PostMapping("/create")
    @Operation(summary = "创建资产归还记录")
    @PreAuthorize("@ss.hasPermission('erp:asset-return:create')")
    public CommonResult<Long> createReturn(@Valid @RequestBody ErpAssetReturnSaveReqVO createReqVO) {
        return success(returnService.createReturn(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新资产归还记录")
    @PreAuthorize("@ss.hasPermission('erp:asset-return:update')")
    public CommonResult<Boolean> updateReturn(@Valid @RequestBody ErpAssetReturnSaveReqVO updateReqVO) {
        returnService.updateReturn(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除资产归还记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:asset-return:delete')")
    public CommonResult<Boolean> deleteReturn(@RequestParam("id") Long id) {
        returnService.deleteReturn(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得资产归还记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:asset-return:query')")
    public CommonResult<ErpAssetReturnRespVO> getReturn(@RequestParam("id") Long id) {
        // 这里需要将DO转换为VO，但为了简化，我们暂时返回基础数据
        // 实际项目中应该在Service中提供对应的VO返回方法
        return success(null); // TODO: 实现DO到VO的转换
    }

    @GetMapping("/page")
    @Operation(summary = "获得资产归还记录分页")
    @PreAuthorize("@ss.hasPermission('erp:asset-return:query')")
    public CommonResult<PageResult<ErpAssetReturnRespVO>> getReturnPage(@Valid ErpAssetReturnPageReqVO pageReqVO) {
        PageResult<ErpAssetReturnRespVO> pageResult = returnService.getReturnPage(pageReqVO);
        return success(pageResult);
    }

    @GetMapping("/list-by-asset")
    @Operation(summary = "获得指定资产的归还记录列表")
    @Parameter(name = "assetId", description = "资产编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:asset-return:query')")
    public CommonResult<List<ErpAssetReturnRespVO>> getReturnListByAssetId(@RequestParam("assetId") Long assetId) {
        List<ErpAssetReturnRespVO> list = returnService.getReturnListByAssetId(assetId);
        return success(list);
    }

    @GetMapping("/list-by-user")
    @Operation(summary = "获得指定用户的归还记录列表")
    @Parameter(name = "userId", description = "用户编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:asset-return:query')")
    public CommonResult<List<ErpAssetReturnRespVO>> getReturnListByUserId(@RequestParam("userId") Long userId) {
        List<ErpAssetReturnRespVO> list = returnService.getReturnListByUserId(userId);
        return success(list);
    }

    @PostMapping("/receive")
    @Operation(summary = "资产管理员接收确认归还")
    @PreAuthorize("@ss.hasPermission('erp:asset-return:receive')")
    public CommonResult<Boolean> receiveReturn(@RequestParam("returnId") Long returnId, 
                                             @RequestParam(value = "receiverRemark", required = false) String receiverRemark) {
        returnService.receiveReturn(returnId, receiverRemark);
        return success(true);
    }

} 