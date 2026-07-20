package com.kyx.service.erp.service.asset;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.erp.controller.admin.asset.vo.attachment.ErpAssetAttachmentBatchUploadReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.attachment.ErpAssetAttachmentBatchUploadRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.attachment.ErpAssetAttachmentPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.attachment.ErpAssetAttachmentSaveReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.attachment.ErpAssetAttachmentUploadReqVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetAttachmentDO;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetAttachmentMapper;
import com.kyx.service.op.api.file.FileApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.service.erp.enums.ErrorCodeConstants.*;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;

/**
 * ERP 资产附件 Service 实现类
 *
 * @author kyx
 */
@Service
@Validated
@Slf4j
public class ErpAssetAttachmentServiceImpl implements ErpAssetAttachmentService {

    /**
     * 批量上传最大文件数量限制
     */
    private static final int MAX_BATCH_UPLOAD_COUNT = 20;

    @Resource
    private ErpAssetAttachmentMapper assetAttachmentMapper;

    @Resource
    private ErpAssetService assetService;

    @Resource
    private FileApi fileApi;

    @Override
    @Transactional
    public Long createAssetAttachment(ErpAssetAttachmentSaveReqVO createReqVO) {
        // 校验资产是否存在
        assetService.getAsset(createReqVO.getAssetId());
        
        // 插入附件记录
        ErpAssetAttachmentDO attachment = BeanUtils.toBean(createReqVO, ErpAssetAttachmentDO.class);
        if (attachment.getSort() == null) {
            attachment.setSort(0);
        }
        assetAttachmentMapper.insert(attachment);
        return attachment.getId();
    }

    @Override
    @Transactional
    public Long uploadAssetAttachment(ErpAssetAttachmentUploadReqVO uploadReqVO) throws Exception {
        // 校验资产是否存在
        assetService.getAsset(uploadReqVO.getAssetId());
        
        MultipartFile file = uploadReqVO.getFile();
        
        // 校验文件
        if (file == null || file.isEmpty()) {
            throw exception(ASSET_ATTACHMENT_FILE_EMPTY);
        }
        
        // 文件大小校验（16MB限制）
        if (file.getSize() > 16 * 1024 * 1024) {
            throw exception(ASSET_ATTACHMENT_FILE_SIZE_EXCEED);
        }

        try {
            // 调用文件上传服务
            String fileName = file.getOriginalFilename();
            String fileType = file.getContentType();
            Long fileSize = file.getSize();
            
            // 读取文件内容并上传到MinIO
            byte[] content = IoUtil.readBytes(file.getInputStream());
            String fileUrl = fileApi.createFile(content, fileName, "asset", fileType);
            
            // 从URL中提取文件路径
            String filePath = fileUrl;
            if (fileUrl.contains("/kyx-files/")) {
                filePath = fileUrl.substring(fileUrl.indexOf("/kyx-files/") + 11);
            }
            
            // 创建附件记录
            ErpAssetAttachmentDO attachment = ErpAssetAttachmentDO.builder()
                    .assetId(uploadReqVO.getAssetId())
                    .fileName(fileName)
                    .filePath(filePath)
                    .fileUrl(fileUrl)
                    .fileSize(fileSize)
                    .fileType(fileType)
                    .sort(uploadReqVO.getSort() != null ? uploadReqVO.getSort() : 0)
                    .remark(uploadReqVO.getRemark())
                    .build();
            
            assetAttachmentMapper.insert(attachment);
            return attachment.getId();
            
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw exception(ASSET_ATTACHMENT_UPLOAD_FAIL);
        }
    }

    @Override
    @Transactional
    public ErpAssetAttachmentBatchUploadRespVO batchUploadAssetAttachment(ErpAssetAttachmentBatchUploadReqVO batchUploadReqVO) {
        // 校验资产是否存在
        assetService.getAsset(batchUploadReqVO.getAssetId());
        
        MultipartFile[] files = batchUploadReqVO.getFiles();
        
        // 校验文件列表
        if (files == null || files.length == 0) {
            throw exception(ASSET_ATTACHMENT_BATCH_FILES_EMPTY);
        }
        
        // 校验文件数量限制
        if (files.length > MAX_BATCH_UPLOAD_COUNT) {
            throw exception(ASSET_ATTACHMENT_BATCH_FILES_COUNT_EXCEED, MAX_BATCH_UPLOAD_COUNT);
        }

        List<Long> successIds = new ArrayList<>();
        List<ErpAssetAttachmentBatchUploadRespVO.UploadFailDetail> failDetails = new ArrayList<>();
        
        // 获取起始排序值
        Integer currentSort = batchUploadReqVO.getSort() != null ? batchUploadReqVO.getSort() : 0;
        
        // 逐个处理文件上传
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            String fileName = file.getOriginalFilename();
            
            try {
                // 校验单个文件
                if (file.isEmpty()) {
                    failDetails.add(ErpAssetAttachmentBatchUploadRespVO.UploadFailDetail.builder()
                            .fileName(fileName)
                            .fileSize(file.getSize())
                            .reason("文件为空")
                            .build());
                    continue;
                }
                
                // 文件大小校验（16MB限制）
                if (file.getSize() > 16 * 1024 * 1024) {
                    failDetails.add(ErpAssetAttachmentBatchUploadRespVO.UploadFailDetail.builder()
                            .fileName(fileName)
                            .fileSize(file.getSize())
                            .reason("文件大小超过16MB限制")
                            .build());
                    continue;
                }

                // 调用文件上传服务
                String fileType = file.getContentType();
                Long fileSize = file.getSize();
                
                // 读取文件内容并上传到MinIO
                byte[] content = IoUtil.readBytes(file.getInputStream());
                String fileUrl = fileApi.createFile(content, fileName, "asset", fileType);
                
                // 从URL中提取文件路径
                String filePath = fileUrl;
                if (fileUrl.contains("/kyx-files/")) {
                    filePath = fileUrl.substring(fileUrl.indexOf("/kyx-files/") + 11);
                }
                
                // 创建附件记录
                ErpAssetAttachmentDO attachment = ErpAssetAttachmentDO.builder()
                        .assetId(batchUploadReqVO.getAssetId())
                        .fileName(fileName)
                        .filePath(filePath)
                        .fileUrl(fileUrl)
                        .fileSize(fileSize)
                        .fileType(fileType)
                        .sort(currentSort + i)
                        .remark(batchUploadReqVO.getRemark())
                        .build();
                
                assetAttachmentMapper.insert(attachment);
                successIds.add(attachment.getId());
                
                log.info("资产附件上传成功: assetId={}, fileName={}, attachmentId={}", 
                        batchUploadReqVO.getAssetId(), fileName, attachment.getId());
                
            } catch (Exception e) {
                log.error("文件上传失败: fileName={}, error={}", fileName, e.getMessage(), e);
                failDetails.add(ErpAssetAttachmentBatchUploadRespVO.UploadFailDetail.builder()
                        .fileName(fileName)
                        .fileSize(file.getSize())
                        .reason("上传失败: " + e.getMessage())
                        .build());
            }
        }
        
        // 检查是否所有文件都失败了
        if (successIds.isEmpty() && !failDetails.isEmpty()) {
            throw exception(ASSET_ATTACHMENT_BATCH_UPLOAD_ALL_FAIL);
        }
        
        return ErpAssetAttachmentBatchUploadRespVO.builder()
                .successIds(successIds)
                .successCount(successIds.size())
                .failCount(failDetails.size())
                .failDetails(failDetails)
                .build();
    }

    @Override
    public void updateAssetAttachment(ErpAssetAttachmentSaveReqVO updateReqVO) {
        // 校验存在
        validateAssetAttachmentExists(updateReqVO.getId());
        
        // 校验资产是否存在
        assetService.getAsset(updateReqVO.getAssetId());
        
        // 更新
        ErpAssetAttachmentDO updateObj = BeanUtils.toBean(updateReqVO, ErpAssetAttachmentDO.class);
        assetAttachmentMapper.updateById(updateObj);
    }

    @Override
    public void deleteAssetAttachment(Long id) {
        // 校验存在
        validateAssetAttachmentExists(id);
        
        // 删除
        assetAttachmentMapper.deleteById(id);
    }

    @Override
    public ErpAssetAttachmentDO getAssetAttachment(Long id) {
        return assetAttachmentMapper.selectById(id);
    }

    @Override
    public List<ErpAssetAttachmentDO> getAssetAttachmentListByAssetId(Long assetId) {
        return assetAttachmentMapper.selectListByAssetId(assetId);
    }

    @Override
    public PageResult<ErpAssetAttachmentDO> getAssetAttachmentPage(ErpAssetAttachmentPageReqVO pageReqVO) {
        // 使用分页查询
        return assetAttachmentMapper.selectPage(pageReqVO, new LambdaQueryWrapperX<ErpAssetAttachmentDO>()
                .eqIfPresent(ErpAssetAttachmentDO::getAssetId, pageReqVO.getAssetId())
                .likeIfPresent(ErpAssetAttachmentDO::getFileName, pageReqVO.getFileName())
                .eqIfPresent(ErpAssetAttachmentDO::getFileType, pageReqVO.getFileType())
                .betweenIfPresent(ErpAssetAttachmentDO::getCreateTime, pageReqVO.getCreateTime())
                .orderByDesc(ErpAssetAttachmentDO::getCreateTime));
    }

    @Override
    @Transactional
    public void deleteAssetAttachmentsByAssetId(Long assetId) {
        assetAttachmentMapper.deleteByAssetId(assetId);
    }

    @Override
    public ErpAssetAttachmentDO validateAssetAttachmentExists(Long id) {
        ErpAssetAttachmentDO attachment = assetAttachmentMapper.selectById(id);
        if (attachment == null) {
            throw exception(ASSET_ATTACHMENT_NOT_EXISTS);
        }
        return attachment;
    }

} 