package com.kyx.service.erp.service.asset;

import cn.hutool.core.collection.CollUtil;
import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.common.util.collection.MapUtils;
import com.kyx.service.erp.controller.admin.asset.vo.asset.ErpAssetPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.asset.ErpAssetRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.asset.ErpAssetSaveReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.asset.ErpAssetBatchSaveReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.asset.ErpAssetBatchSaveRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.asset.ErpAssetLogRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.attachment.ErpAssetAttachmentRespVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetDO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetCheckoutDO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetReturnDO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetOwnershipDO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetCategoryDO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetAttachmentDO;
import com.kyx.service.erp.dal.dataobject.purchase.ErpSupplierDO;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetMapper;
import com.kyx.service.erp.service.purchase.ErpSupplierService;
import com.kyx.service.business.api.dept.DeptApi;
import com.kyx.service.business.api.dept.dto.DeptRespDTO;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import com.kyx.service.op.api.file.FileApi;
import com.kyx.service.op.api.file.dto.FileCreateReqDTO;
import com.kyx.service.bpm.api.task.BpmProcessInstanceApi;
import com.kyx.service.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.foundation.common.util.collection.CollectionUtils.convertMap;
import static com.kyx.foundation.common.util.collection.CollectionUtils.convertSet;
import static com.kyx.service.erp.enums.ErrorCodeConstants.ASSET_NOT_EXISTS;
import static com.kyx.service.erp.enums.ErrorCodeConstants.ASSET_NO_DUPLICATE;

/**
 * ERP 资产 Service 实现类
 *
 * @author kyx
 */
@Service
@Validated
@Slf4j
public class ErpAssetServiceImpl implements ErpAssetService {

    @Resource
    private ErpAssetMapper assetMapper;
    
    @Resource  
    private com.kyx.service.erp.dal.mysql.asset.ErpAssetCheckoutMapper assetCheckoutMapper;
    
    @Resource
    private com.kyx.service.erp.dal.mysql.asset.ErpAssetReturnMapper assetReturnMapper;
    
    @Resource
    private com.kyx.service.erp.dal.mysql.asset.ErpAssetOwnershipMapper assetOwnershipMapper;

    @Resource
    private ErpAssetCategoryService assetCategoryService;
    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    @Lazy // 解决循环依赖问题
    private ErpAssetAttachmentService assetAttachmentService;
    @Resource
    private FileApi fileApi;
    @Resource
    private BpmProcessInstanceApi bpmProcessInstanceApi;

    @Override
    @Transactional
    public Long createAsset(ErpAssetSaveReqVO createReqVO) {
        // 校验资产编码是否重复
        validateAssetNoUnique(null, createReqVO.getAssetNo());
        // 校验分类
        if (createReqVO.getCategoryId() != null) {
            assetCategoryService.validateAssetCategory(createReqVO.getCategoryId());
        }
        // 插入
        ErpAssetDO asset = BeanUtils.toBean(createReqVO, ErpAssetDO.class);
        assetMapper.insert(asset);
        
        // 处理附件文件ID，创建附件记录
        if (CollUtil.isNotEmpty(createReqVO.getFileIds())) {
            createAssetAttachmentsByFileIds(asset.getId(), createReqVO.getFileIds());
        }
        
        // 返回
        return asset.getId();
    }

    @Override
    @Transactional
    public ErpAssetBatchSaveRespVO batchCreateAssets(ErpAssetBatchSaveReqVO batchSaveReqVO) {
        List<ErpAssetSaveReqVO> assets = batchSaveReqVO.getAssets();
        int total = assets.size();
        List<Long> successIds = new ArrayList<>();
        List<ErpAssetBatchSaveRespVO.FailureInfo> failures = new ArrayList<>();

        log.info("开始批量创建资产，总数量: {}", total);

        for (int i = 0; i < assets.size(); i++) {
            ErpAssetSaveReqVO assetReqVO = assets.get(i);
            try {
                // 调用单个创建方法
                Long assetId = createAsset(assetReqVO);
                successIds.add(assetId);
                log.debug("第 {} 个资产创建成功，ID: {}, 编码: {}", i + 1, assetId, assetReqVO.getAssetNo());
            } catch (Exception e) {
                log.warn("第 {} 个资产创建失败，编码: {}, 名称: {}, 错误: {}", 
                    i + 1, assetReqVO.getAssetNo(), assetReqVO.getName(), e.getMessage());
                
                ErpAssetBatchSaveRespVO.FailureInfo failureInfo = ErpAssetBatchSaveRespVO.FailureInfo.builder()
                    .index(i)
                    .assetNo(assetReqVO.getAssetNo())
                    .assetName(assetReqVO.getName())
                    .errorMessage(e.getMessage())
                    .build();
                failures.add(failureInfo);
            }
        }

        int successCount = successIds.size();
        int failureCount = failures.size();
        
        log.info("批量创建资产完成，总数: {}, 成功: {}, 失败: {}", total, successCount, failureCount);

        return ErpAssetBatchSaveRespVO.builder()
            .total(total)
            .successCount(successCount)
            .failureCount(failureCount)
            .successIds(successIds)
            .failures(failures)
            .build();
    }

    @Override
    public void updateAsset(ErpAssetSaveReqVO updateReqVO) {
        // 校验存在
        validateAssetExists(updateReqVO.getId());
        // 校验资产编码是否重复
        validateAssetNoUnique(updateReqVO.getId(), updateReqVO.getAssetNo());
        // 校验分类
        if (updateReqVO.getCategoryId() != null) {
            assetCategoryService.validateAssetCategory(updateReqVO.getCategoryId());
        }
        // 更新
        ErpAssetDO updateObj = BeanUtils.toBean(updateReqVO, ErpAssetDO.class);
        assetMapper.updateById(updateObj);
        
        // 处理附件文件ID，创建附件记录
        if (CollUtil.isNotEmpty(updateReqVO.getFileIds())) {
            createAssetAttachmentsByFileIds(updateReqVO.getId(), updateReqVO.getFileIds());
        }
    }

    @Override
    public void deleteAsset(Long id) {
        // 校验存在
        validateAssetExists(id);
        // 删除
        assetMapper.deleteById(id);
    }

    @Override
    public List<ErpAssetDO> validAssetList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<ErpAssetDO> list = assetMapper.selectBatchIds(ids);
        Map<Long, ErpAssetDO> assetMap = convertMap(list, ErpAssetDO::getId);
        for (Long id : ids) {
            ErpAssetDO asset = assetMap.get(id);
            if (asset == null) {
                throw exception(ASSET_NOT_EXISTS);
            }
        }
        return list;
    }

    private void validateAssetExists(Long id) {
        if (assetMapper.selectById(id) == null) {
            throw exception(ASSET_NOT_EXISTS);
        }
    }

    private void validateAssetNoUnique(Long id, String assetNo) {
        ErpAssetDO asset = assetMapper.selectByAssetNo(assetNo);
        if (asset == null) {
            return;
        }
        if (id == null || !id.equals(asset.getId())) {
            throw exception(ASSET_NO_DUPLICATE, assetNo);
        }
    }

    @Override
    public ErpAssetDO getAsset(Long id) {
        return assetMapper.selectById(id);
    }

    @Override
    public List<ErpAssetRespVO> getAssetVOListByStatus(Integer status) {
        List<ErpAssetDO> list = assetMapper.selectListByStatus(status);
        return BeanUtils.toBean(list, ErpAssetRespVO.class);
    }

    @Override
    public List<ErpAssetRespVO> getAssetVOList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<ErpAssetDO> list = assetMapper.selectBatchIds(ids);
        return BeanUtils.toBean(list, ErpAssetRespVO.class);
    }

    @Override
    public PageResult<ErpAssetRespVO> getAssetVOPage(ErpAssetPageReqVO pageReqVO) {
        PageResult<ErpAssetDO> pageResult = assetMapper.selectPage(pageReqVO);
        return buildAssetVOPageResult(pageResult);
    }

    private PageResult<ErpAssetRespVO> buildAssetVOPageResult(PageResult<ErpAssetDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        
        // 1.1 获取分类信息
        Map<Long, ErpAssetCategoryDO> categoryMap = assetCategoryService.getAssetCategoryMap(
                convertSet(pageResult.getList(), ErpAssetDO::getCategoryId).stream()
                        .filter(java.util.Objects::nonNull).collect(Collectors.toSet()));
        
        // 1.2 获取部门信息
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(
                convertSet(pageResult.getList(), ErpAssetDO::getDeptId).stream()
                        .filter(java.util.Objects::nonNull).collect(Collectors.toSet()));
        
        // 1.3 获取供应商信息
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(
                convertSet(pageResult.getList(), ErpAssetDO::getSupplierId).stream()
                        .filter(java.util.Objects::nonNull).collect(Collectors.toSet()));
        
        // 2. 开始拼接
        return BeanUtils.toBean(pageResult, ErpAssetRespVO.class, asset -> {
            // 设置分类名称
            MapUtils.findAndThen(categoryMap, asset.getCategoryId(), category -> asset.setCategoryName(category.getName()));
            // 设置部门名称
            MapUtils.findAndThen(deptMap, asset.getDeptId(), dept -> asset.setDeptName(dept.getName()));
            // 设置供应商名称
            MapUtils.findAndThen(supplierMap, asset.getSupplierId(), supplier -> asset.setSupplierName(supplier.getName()));
        });
    }

    @Override
    public Long getAssetCountByCategoryId(Long categoryId) {
        return assetMapper.selectCountByCategoryId(categoryId);
    }

    @Override
    public Long getAssetCountByDeptId(Long deptId) {
        return assetMapper.selectCountByDeptId(deptId);
    }

    /**
     * 根据文件ID列表创建资产附件记录
     */
    private void createAssetAttachmentsByFileIds(Long assetId, List<String> fileIds) {
        if (CollUtil.isEmpty(fileIds)) {
            return;
        }
        
        log.info("开始为资产[{}]创建附件记录，文件ID数量: {}", assetId, fileIds.size());
        
        for (int i = 0; i < fileIds.size(); i++) {
            String fileId = fileIds.get(i); // 直接使用字符串格式的fileId
            try {
                // 根据fileId获取文件信息
                FileCreateReqDTO fileInfo = fileApi.getFileByFileId(fileId).getCheckedData();
                if (fileInfo == null) {
                    log.warn("文件ID[{}]对应的文件不存在，跳过创建附件", fileId);
                    continue;
                }
                
                // 创建附件记录
                ErpAssetAttachmentDO attachment = ErpAssetAttachmentDO.builder()
                        .assetId(assetId)
                        .fileName(fileInfo.getName() != null ? fileInfo.getName() : "未知文件名")
                        .filePath(fileId) // 使用fileId作为路径
                        .fileUrl("/admin-api/infra/file/download/" + fileId) // 构建下载URL
                        .fileSize(fileInfo.getSize() != null ? fileInfo.getSize().longValue() : 0L) // 从文件服务获取文件大小
                        .fileType(fileInfo.getType() != null ? fileInfo.getType() : "application/octet-stream")
                        .sort(i + 1) // 按顺序排序
                        .remark("资产录入时上传")
                        .build();
                
                assetAttachmentService.createAssetAttachment(BeanUtils.toBean(attachment, 
                        com.kyx.service.erp.controller.admin.asset.vo.attachment.ErpAssetAttachmentSaveReqVO.class));
                
                log.info("成功为资产[{}]创建附件记录: fileName={}, fileId={}", assetId, fileInfo.getName(), fileId);
            } catch (Exception e) {
                log.error("为资产[{}]创建附件记录失败: fileId={}, error={}", assetId, fileId, e.getMessage(), e);
                // 继续处理其他文件，不中断整个流程
            }
        }
        
        log.info("完成为资产[{}]创建附件记录", assetId);
    }

    @Override
    public List<ErpAssetLogRespVO> getAssetLogs(Long assetId) {
        List<ErpAssetLogRespVO> logs = new ArrayList<>();
        
        // 1. 添加资产录入记录
        ErpAssetDO asset = getAsset(assetId);
        if (asset != null) {
            ErpAssetLogRespVO createLog = new ErpAssetLogRespVO();
            createLog.setId(asset.getId());
            createLog.setAssetId(asset.getId());
            createLog.setAssetNo(asset.getAssetNo());
            createLog.setAssetName(asset.getName());
            createLog.setOperationType(1); // 录入
            createLog.setOperationTypeName("资产录入");
            createLog.setOperationTime(asset.getCreateTime() != null ? 
                asset.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime() : null);
            createLog.setOperatorUserId(asset.getCreator() != null ? Long.valueOf(asset.getCreator()) : null);
            createLog.setAfterStatus(asset.getStatus());
            createLog.setAfterStatusName(getStatusName(asset.getStatus()));
            createLog.setDescription("资产录入系统");
            createLog.setResult(1); // 成功
            logs.add(createLog);
        }
        
        // 2. 添加领用记录
        List<ErpAssetCheckoutDO> checkouts = assetCheckoutMapper.selectListByAssetId(assetId);
        for (ErpAssetCheckoutDO checkout : checkouts) {
            // 领用申请记录
            ErpAssetLogRespVO checkoutLog = new ErpAssetLogRespVO();
            checkoutLog.setId(checkout.getId());
            checkoutLog.setAssetId(checkout.getAssetId());
            checkoutLog.setAssetNo(asset.getAssetNo());
            checkoutLog.setAssetName(asset.getName());
            checkoutLog.setOperationType(2); // 领用申请
            checkoutLog.setOperationTypeName("领用申请");
            checkoutLog.setOperationTime(checkout.getCreateTime() != null ? 
                checkout.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime() : null);
            checkoutLog.setOperatorUserId(checkout.getCreator() != null ? Long.valueOf(checkout.getCreator()) : null);
            checkoutLog.setRelatedUserId(checkout.getCheckoutUserId());
            checkoutLog.setRelatedDeptId(checkout.getCheckoutDeptId());
            checkoutLog.setBusinessId(checkout.getId());
            checkoutLog.setDescription("申请领用资产：" + (checkout.getCheckoutReason() != null ? checkout.getCheckoutReason() : ""));
            checkoutLog.setResult(1);
            logs.add(checkoutLog);
            
            // 如果审批通过，添加领用确认记录
            if (checkout.getApprovalStatus() != null && checkout.getApprovalStatus() == 2) {
                ErpAssetLogRespVO approvalLog = new ErpAssetLogRespVO();
                approvalLog.setId(checkout.getId() + 10000); // 避免ID冲突
                approvalLog.setAssetId(checkout.getAssetId());
                approvalLog.setAssetNo(asset.getAssetNo());
                approvalLog.setAssetName(asset.getName());
                approvalLog.setOperationType(3); // 领用确认
                approvalLog.setOperationTypeName("领用确认");
                approvalLog.setOperationTime(checkout.getApprovalTime());
                approvalLog.setOperatorUserId(checkout.getApproverUserId());
                approvalLog.setRelatedUserId(checkout.getCheckoutUserId());
                approvalLog.setRelatedDeptId(checkout.getCheckoutDeptId());
                approvalLog.setBusinessId(checkout.getId());
                approvalLog.setDescription("审批通过，确认领用");
                approvalLog.setResult(1);
                logs.add(approvalLog);
            }
        }
        
        // 3. 添加归还记录
        List<ErpAssetReturnDO> returns = assetReturnMapper.selectListByAssetId(assetId);
        for (ErpAssetReturnDO returnRecord : returns) {
            // 归还申请记录
            ErpAssetLogRespVO returnLog = new ErpAssetLogRespVO();
            returnLog.setId(returnRecord.getId());
            returnLog.setAssetId(returnRecord.getAssetId());
            returnLog.setAssetNo(asset.getAssetNo());
            returnLog.setAssetName(asset.getName());
            returnLog.setOperationType(4); // 归还申请
            returnLog.setOperationTypeName("归还申请");
            returnLog.setOperationTime(returnRecord.getCreateTime() != null ? 
                returnRecord.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime() : null);
            returnLog.setOperatorUserId(returnRecord.getReturnUserId());
            returnLog.setRelatedUserId(returnRecord.getReturnUserId());
            returnLog.setRelatedDeptId(returnRecord.getReturnDeptId());
            returnLog.setBusinessId(returnRecord.getId());
            returnLog.setDescription("申请归还资产：" + (returnRecord.getReturnRemark() != null ? returnRecord.getReturnRemark() : ""));
            returnLog.setResult(1);
            logs.add(returnLog);
            
            // 如果已接收确认，添加归还确认记录
            if (returnRecord.getStatus() != null && returnRecord.getStatus() == 2) {
                ErpAssetLogRespVO confirmLog = new ErpAssetLogRespVO();
                confirmLog.setId(returnRecord.getId() + 20000); // 避免ID冲突
                confirmLog.setAssetId(returnRecord.getAssetId());
                confirmLog.setAssetNo(asset.getAssetNo());
                confirmLog.setAssetName(asset.getName());
                confirmLog.setOperationType(5); // 归还确认
                confirmLog.setOperationTypeName("归还确认");
                confirmLog.setOperationTime(returnRecord.getReceiverTime());
                confirmLog.setOperatorUserId(returnRecord.getReceiverUserId());
                confirmLog.setRelatedUserId(returnRecord.getReturnUserId());
                confirmLog.setRelatedDeptId(returnRecord.getReturnDeptId());
                confirmLog.setBusinessId(returnRecord.getId());
                confirmLog.setDescription("确认接收归还：" + (returnRecord.getReceiverRemark() != null ? returnRecord.getReceiverRemark() : ""));
                confirmLog.setResult(1);
                logs.add(confirmLog);
            }
        }
        
        // 4. 填充用户和部门信息
        fillUserAndDeptInfo(logs);
        
        // 5. 按时间倒序排列
        logs.sort((a, b) -> {
            if (a.getOperationTime() == null && b.getOperationTime() == null) return 0;
            if (a.getOperationTime() == null) return 1;
            if (b.getOperationTime() == null) return -1;
            return b.getOperationTime().compareTo(a.getOperationTime());
        });
        
        return logs;
    }
    
    /**
     * 填充用户和部门信息
     */
    private void fillUserAndDeptInfo(List<ErpAssetLogRespVO> logs) {
        // 收集用户ID
        Set<Long> userIds = logs.stream()
                .flatMap(log -> Stream.of(log.getOperatorUserId(), log.getRelatedUserId()))
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        
        // 批量查询用户信息
        Map<Long, AdminUserRespDTO> userMap = Collections.emptyMap();
        if (!userIds.isEmpty()) {
            try {
                userMap = adminUserApi.getUserMap(userIds);
            } catch (Exception e) {
                log.warn("查询用户信息失败", e);
            }
        }
        
        // 收集部门ID
        Set<Long> deptIds = logs.stream()
                .flatMap(log -> Stream.of(log.getOperatorDeptId(), log.getRelatedDeptId()))
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        
        // 批量查询部门信息
        Map<Long, DeptRespDTO> deptMap = Collections.emptyMap();
        if (!deptIds.isEmpty()) {
            try {
                deptMap = deptApi.getDeptMap(deptIds);
            } catch (Exception e) {
                log.warn("查询部门信息失败", e);
            }
        }
        
        // 填充信息
        for (ErpAssetLogRespVO log : logs) {
            // 填充操作人信息
            if (log.getOperatorUserId() != null) {
                AdminUserRespDTO user = userMap.get(log.getOperatorUserId());
                if (user != null) {
                    log.setOperatorUserName(user.getNickname());
                    log.setOperatorDeptId(user.getDeptId());
                    if (user.getDeptId() != null) {
                        DeptRespDTO dept = deptMap.get(user.getDeptId());
                        if (dept != null) {
                            log.setOperatorDeptName(dept.getName());
                        }
                    }
                }
            }
            
            // 填充相关用户信息
            if (log.getRelatedUserId() != null) {
                AdminUserRespDTO user = userMap.get(log.getRelatedUserId());
                if (user != null) {
                    log.setRelatedUserName(user.getNickname());
                }
            }
            
            // 填充相关部门信息
            if (log.getRelatedDeptId() != null) {
                DeptRespDTO dept = deptMap.get(log.getRelatedDeptId());
                if (dept != null) {
                    log.setRelatedDeptName(dept.getName());
                }
            }
        }
    }
    
    /**
     * 获取状态名称
     */
    private String getStatusName(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 1: return "正常";
            case 2: return "维修中";
            case 3: return "报废";
            case 4: return "闲置";
            default: return "未知";
        }
    }
} 