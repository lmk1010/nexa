package com.kyx.service.erp.controller.admin.asset;

import com.kyx.foundation.apilog.core.annotation.ApiAccessLog;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.erp.controller.admin.asset.vo.attachment.ErpAssetAttachmentBatchUploadReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.attachment.ErpAssetAttachmentBatchUploadRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.attachment.ErpAssetAttachmentPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.attachment.ErpAssetAttachmentRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.attachment.ErpAssetAttachmentSaveReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.attachment.ErpAssetAttachmentUploadReqVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetAttachmentDO;
import com.kyx.service.erp.service.asset.ErpAssetAttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static com.kyx.foundation.apilog.core.enums.OperateTypeEnum.EXPORT;
import static com.kyx.foundation.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 资产附件")
@RestController
@RequestMapping("/erp/asset-attachment")
@Validated
public class ErpAssetAttachmentController {

    @Resource
    private ErpAssetAttachmentService assetAttachmentService;

    @PostMapping("/create")
    @Operation(summary = "创建资产附件")
    @PreAuthorize("@ss.hasPermission('erp:assets:create')")
    public CommonResult<Long> createAssetAttachment(@Valid @RequestBody ErpAssetAttachmentSaveReqVO createReqVO) {
        return success(assetAttachmentService.createAssetAttachment(createReqVO));
    }

    @PostMapping("/upload")
    @Operation(summary = "上传资产附件文件")
    @PreAuthorize("@ss.hasPermission('erp:assets:create')")
    public CommonResult<Long> uploadAssetAttachment(@Valid ErpAssetAttachmentUploadReqVO uploadReqVO) throws Exception {
        return success(assetAttachmentService.uploadAssetAttachment(uploadReqVO));
    }

    @PostMapping("/batch-upload")
    @Operation(summary = "批量上传资产附件文件")
    @PreAuthorize("@ss.hasPermission('erp:assets:create')")
    public CommonResult<ErpAssetAttachmentBatchUploadRespVO> batchUploadAssetAttachment(@Valid ErpAssetAttachmentBatchUploadReqVO batchUploadReqVO) {
        return success(assetAttachmentService.batchUploadAssetAttachment(batchUploadReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新资产附件")
    @PreAuthorize("@ss.hasPermission('erp:assets:update')")
    public CommonResult<Boolean> updateAssetAttachment(@Valid @RequestBody ErpAssetAttachmentSaveReqVO updateReqVO) {
        assetAttachmentService.updateAssetAttachment(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除资产附件")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:assets:delete')")
    public CommonResult<Boolean> deleteAssetAttachment(@RequestParam("id") Long id) {
        assetAttachmentService.deleteAssetAttachment(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得资产附件")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:assets:query')")
    public CommonResult<ErpAssetAttachmentRespVO> getAssetAttachment(@RequestParam("id") Long id) {
        ErpAssetAttachmentDO attachment = assetAttachmentService.getAssetAttachment(id);
        ErpAssetAttachmentRespVO respVO = BeanUtils.toBean(attachment, ErpAssetAttachmentRespVO.class);
        // 修正字段歧义：filePath 实际存储的是 fileId
        if (respVO != null) {
            respVO.setFileId(respVO.getFilePath());
        }
        return success(respVO);
    }

    @GetMapping("/list-by-asset-id")
    @Operation(summary = "根据资产ID获取附件列表")
    @Parameter(name = "assetId", description = "资产ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:assets:query')")
    public CommonResult<List<ErpAssetAttachmentRespVO>> getAssetAttachmentListByAssetId(@RequestParam("assetId") Long assetId) {
        List<ErpAssetAttachmentDO> list = assetAttachmentService.getAssetAttachmentListByAssetId(assetId);
        List<ErpAssetAttachmentRespVO> respList = BeanUtils.toBean(list, ErpAssetAttachmentRespVO.class);
        // 修正字段歧义：filePath 实际存储的是 fileId
        respList.forEach(respVO -> respVO.setFileId(respVO.getFilePath()));
        return success(respList);
    }

    @DeleteMapping("/delete-by-asset-id")
    @Operation(summary = "删除资产的所有附件")
    @Parameter(name = "assetId", description = "资产ID", required = true)
    @PreAuthorize("@ss.hasPermission('erp:assets:delete')")
    public CommonResult<Boolean> deleteAssetAttachmentsByAssetId(@RequestParam("assetId") Long assetId) {
        assetAttachmentService.deleteAssetAttachmentsByAssetId(assetId);
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获取资产附件分页")
    @PreAuthorize("@ss.hasPermission('erp:assets:query')")
    public CommonResult<PageResult<ErpAssetAttachmentRespVO>> getAssetAttachmentPage(@Valid ErpAssetAttachmentPageReqVO pageReqVO) {
        PageResult<ErpAssetAttachmentDO> pageResult = assetAttachmentService.getAssetAttachmentPage(pageReqVO);
        PageResult<ErpAssetAttachmentRespVO> respPageResult = BeanUtils.toBean(pageResult, ErpAssetAttachmentRespVO.class);
        // 修正字段歧义：filePath 实际存储的是 fileId
        if (respPageResult.getList() != null) {
            respPageResult.getList().forEach(respVO -> respVO.setFileId(respVO.getFilePath()));
        }
        return success(respPageResult);
    }

    @PostMapping("/batch-delete")
    @Operation(summary = "批量删除资产附件")
    @PreAuthorize("@ss.hasPermission('erp:assets:delete')")
    public CommonResult<Boolean> batchDeleteAssetAttachment(@RequestBody List<Long> ids) {
        for (Long id : ids) {
            assetAttachmentService.deleteAssetAttachment(id);
        }
        return success(true);
    }

} 