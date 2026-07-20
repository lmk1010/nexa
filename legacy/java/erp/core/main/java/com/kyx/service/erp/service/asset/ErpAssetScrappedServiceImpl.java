package com.kyx.service.erp.service.asset;

import cn.hutool.core.lang.UUID;
import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.bpm.api.task.BpmProcessInstanceApi;
import com.kyx.service.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.kyx.service.business.api.dept.DeptApi;
import com.kyx.service.business.api.dept.dto.DeptRespDTO;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import com.kyx.service.erp.api.asset.vo.scrapped.ErpAssetScrappedSaveReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.scrapped.ErpAssetScrappedPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.scrapped.ErpAssetScrappedRespVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetDO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetScrappedDO;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetMapper;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetScrappedMapper;
import com.kyx.service.erp.enums.asset.ErpAssetScrappedBmpStatusEnum;
import com.kyx.service.erp.enums.asset.ErpAssetScrappedStatusEnum;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetScrappedFileDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.kyx.service.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 资产报废 Service 实现类
 *
 * @author kyx
 */
@Slf4j
@Service
@Validated
public class ErpAssetScrappedServiceImpl implements ErpAssetScrappedService {

    /**
     * 资产报废审批流程 KEY
     */
    public static final String PROCESS_KEY = "asset-scrapped";

    @Resource
    private ErpAssetScrappedMapper scrappedMapper;
    @Resource
    private ErpAssetMapper assetMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private BpmProcessInstanceApi bmpProcessInstanceApi;
    @Resource
    private ErpAssetScrappedFileService scrappedFileService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createScrapped(@Valid ErpAssetScrappedSaveReqVO createReqVO) {
        // 1. 校验资产是否存在
        ErpAssetDO asset = validateAssetExists(createReqVO.getAssetId());
        
        // 2. 校验资产状态是否允许报废
        validateAssetCanBeScrapped(asset);
        
        // 3. 校验用户和部门是否存在
        validateUserExists(createReqVO.getHandleUserId());
        validateDeptExists(createReqVO.getHandleDeptId());
        
        // 4. 生成报废编号
        String scrappedNo = generateScrappedNo();
        
        // 5. 创建报废记录
        ErpAssetScrappedDO scrapped = BeanUtils.toBean(createReqVO, ErpAssetScrappedDO.class);
        scrapped.setScrappedNo(scrappedNo);
        scrapped.setStatus(ErpAssetScrappedStatusEnum.PENDING.getStatus()); // 申请中
        scrapped.setApprovalStatus(1); // 待审批
        scrappedMapper.insert(scrapped);
        
        // 6. 保存文件关联
        if (createReqVO.getFileIds() != null && !createReqVO.getFileIds().isEmpty()) {
            scrappedFileService.saveScrappedFiles(scrapped.getId(), createReqVO.getFileIds());
        }
        
        return scrapped.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createScrappedAndSubmit(Long userId, @Valid ErpAssetScrappedSaveReqVO createReqVO) {
        // 1. 校验资产是否存在
        ErpAssetDO asset = validateAssetExists(createReqVO.getAssetId());
        
        // 2. 校验资产状态是否允许报废
        validateAssetCanBeScrapped(asset);
        
        // 3. 校验用户和部门是否存在
        validateUserExists(createReqVO.getHandleUserId());
        validateDeptExists(createReqVO.getHandleDeptId());
        
        // 4. 生成报废编号
        String scrappedNo = generateScrappedNo();
        
        // 5. 创建报废记录 - 设置为流程中状态
        ErpAssetScrappedDO scrapped = BeanUtils.toBean(createReqVO, ErpAssetScrappedDO.class);
        scrapped.setScrappedNo(scrappedNo);
        scrapped.setStatus(ErpAssetScrappedStatusEnum.APPROVING.getStatus()); // 审批中
        scrapped.setApprovalStatus(1); // 待审批
        scrapped.setBmpStatus(ErpAssetScrappedBmpStatusEnum.PROCESSING.getStatus()); // 流程中
        scrappedMapper.insert(scrapped);
        
        // 6. 发起 BPM 流程
        Map<String, Object> processInstanceVariables = new HashMap<>();
        processInstanceVariables.put("scrappedId", scrapped.getId());
        processInstanceVariables.put("scrappedNo", scrappedNo);
        processInstanceVariables.put("assetId", scrapped.getAssetId());
        processInstanceVariables.put("assetNo", asset.getAssetNo());
        processInstanceVariables.put("assetName", asset.getName());
        processInstanceVariables.put("scrappedReason", scrapped.getScrappedReason());
        processInstanceVariables.put("scrappedType", scrapped.getScrappedType());
        processInstanceVariables.put("scrappedDate", scrapped.getScrappedDate());
        processInstanceVariables.put("handleUserId", scrapped.getHandleUserId());
        processInstanceVariables.put("handleDeptId", scrapped.getHandleDeptId());
        processInstanceVariables.put("estimatedValue", scrapped.getEstimatedValue());
        processInstanceVariables.put("actualValue", scrapped.getActualValue());
        processInstanceVariables.put("scrappedDescription", scrapped.getScrappedDescription());
        
        // 创建流程实例
        String processInstanceId = bmpProcessInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(PROCESS_KEY)
                        .setVariables(processInstanceVariables).setBusinessKey(String.valueOf(scrapped.getId()))).getCheckedData();
        
        // 7. 更新流程实例ID
        ErpAssetScrappedDO updateObj = new ErpAssetScrappedDO();
        updateObj.setId(scrapped.getId());
        updateObj.setBmpProcessInstanceId(processInstanceId);
        scrappedMapper.updateById(updateObj);
        
        // 8. 保存文件关联
        if (createReqVO.getFileIds() != null && !createReqVO.getFileIds().isEmpty()) {
            scrappedFileService.saveScrappedFiles(scrapped.getId(), createReqVO.getFileIds());
        }
        
        log.info("资产报废申请提交成功: scrappedId={}, assetId={}, processInstanceId={}", 
                scrapped.getId(), asset.getId(), processInstanceId);
        
        return scrapped.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateScrapped(@Valid ErpAssetScrappedSaveReqVO updateReqVO) {
        // 校验存在
        validateScrappedExists(updateReqVO.getId());
        
        // 校验资产是否存在
        validateAssetExists(updateReqVO.getAssetId());
        
        // 校验用户和部门是否存在
        validateUserExists(updateReqVO.getHandleUserId());
        validateDeptExists(updateReqVO.getHandleDeptId());
        
        // 更新
        ErpAssetScrappedDO updateObj = BeanUtils.toBean(updateReqVO, ErpAssetScrappedDO.class);
        scrappedMapper.updateById(updateObj);
        
        // 更新文件关联
        if (updateReqVO.getFileIds() != null) {
            scrappedFileService.updateScrappedFiles(updateReqVO.getId(), updateReqVO.getFileIds());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteScrapped(Long id) {
        // 校验存在
        validateScrappedExists(id);
        // 删除文件关联
        scrappedFileService.deleteScrappedFiles(id);
        // 删除
        scrappedMapper.deleteById(id);
    }

    @Override
    public ErpAssetScrappedDO getScrapped(Long id) {
        return scrappedMapper.selectById(id);
    }

    @Override
    public PageResult<ErpAssetScrappedRespVO> getScrappedPage(ErpAssetScrappedPageReqVO pageReqVO) {
        PageResult<ErpAssetScrappedDO> pageResult = scrappedMapper.selectPage(pageReqVO);
        return convertPage(pageResult);
    }

    @Override
    public List<ErpAssetScrappedRespVO> getScrappedList(ErpAssetScrappedPageReqVO exportReqVO) {
        exportReqVO.setPageSize(Integer.MAX_VALUE);
        PageResult<ErpAssetScrappedDO> pageResult = scrappedMapper.selectPage(exportReqVO);
        return convertList(pageResult.getList());
    }

    @Override
    public ErpAssetScrappedDO getScrappedByBmpProcessInstanceId(String bmpProcessInstanceId) {
        return scrappedMapper.selectByBmpProcessInstanceId(bmpProcessInstanceId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateScrappedBmpStatus(Long id, Integer approvalStatus, Integer bmpStatus) {
        ErpAssetScrappedDO scrapped = validateScrappedExists(id);
        
        // 更新审批状态和BMP状态
        ErpAssetScrappedDO updateObj = new ErpAssetScrappedDO();
        updateObj.setId(id);
        updateObj.setApprovalStatus(approvalStatus);
        updateObj.setBmpStatus(bmpStatus);
        updateObj.setApprovalTime(LocalDateTime.now());
        
        // 根据审批结果更新报废状态
        if (approvalStatus.equals(2)) { // 审批通过
            updateObj.setStatus(ErpAssetScrappedStatusEnum.APPROVED.getStatus());
            scrappedMapper.updateById(updateObj);
            
            // 更新资产状态为报废
            updateAssetStatusToScrapped(scrapped.getAssetId());
            
            log.info("资产报废审批通过: scrappedId={}, assetId={}", id, scrapped.getAssetId());
        } else if (approvalStatus.equals(3)) { // 审批拒绝
            updateObj.setStatus(ErpAssetScrappedStatusEnum.REJECTED.getStatus());
            scrappedMapper.updateById(updateObj);
            
            log.info("资产报废审批拒绝: scrappedId={}, assetId={}", id, scrapped.getAssetId());
        } else {
            scrappedMapper.updateById(updateObj);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeScrapped(Long id, String processingMethod, BigDecimal disposalRevenue, String remark) {
        ErpAssetScrappedDO scrapped = validateScrappedExists(id);
        
        // 校验状态是否允许完成
        if (!scrapped.getStatus().equals(ErpAssetScrappedStatusEnum.APPROVED.getStatus())) {
            throw ServiceExceptionUtil.exception(ASSET_SCRAPPED_NOT_APPROVED);
        }
        
        // 更新为已完成状态
        ErpAssetScrappedDO updateObj = new ErpAssetScrappedDO();
        updateObj.setId(id);
        updateObj.setStatus(ErpAssetScrappedStatusEnum.COMPLETED.getStatus());
        updateObj.setProcessingDate(LocalDate.now());
        updateObj.setProcessingMethod(processingMethod);
        updateObj.setDisposalRevenue(disposalRevenue);
        updateObj.setRemark(remark);
        scrappedMapper.updateById(updateObj);
        
        log.info("资产报废处理完成: scrappedId={}, assetId={}, method={}", 
                id, scrapped.getAssetId(), processingMethod);
    }

    // ==================== 私有方法 ====================

    private ErpAssetDO validateAssetExists(Long assetId) {
        if (assetId == null) {
            return null;
        }
        ErpAssetDO asset = assetMapper.selectById(assetId);
        if (asset == null) {
            throw ServiceExceptionUtil.exception(ASSET_NOT_EXISTS);
        }
        return asset;
    }

    private void validateAssetCanBeScrapped(ErpAssetDO asset) {
        // 检查资产状态，已经报废的资产不能再次报废
        if (asset.getStatus() != null && asset.getStatus().equals(3)) { // 3-报废
            throw ServiceExceptionUtil.exception(ASSET_ALREADY_SCRAPPED);
        }
    }

    private ErpAssetScrappedDO validateScrappedExists(Long id) {
        if (id == null) {
            return null;
        }
        ErpAssetScrappedDO scrapped = scrappedMapper.selectById(id);
        if (scrapped == null) {
            throw ServiceExceptionUtil.exception(ASSET_SCRAPPED_NOT_EXISTS);
        }
        return scrapped;
    }

    private void validateUserExists(Long userId) {
        if (userId == null) {
            return;
        }
        AdminUserRespDTO user = adminUserApi.getUser(userId).getCheckedData();
        if (user == null) {
            throw ServiceExceptionUtil.exception(USER_NOT_EXISTS);
        }
    }

    private void validateDeptExists(Long deptId) {
        if (deptId == null) {
            return;
        }
        DeptRespDTO dept = deptApi.getDept(deptId).getCheckedData();
        if (dept == null) {
            throw ServiceExceptionUtil.exception(DEPT_NOT_EXISTS);
        }
    }

    private String generateScrappedNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = UUID.randomUUID().toString(true).substring(0, 6).toUpperCase();
        return "SCRAP" + date + uuid;
    }

    private void updateAssetStatusToScrapped(Long assetId) {
        ErpAssetDO updateObj = new ErpAssetDO();
        updateObj.setId(assetId);
        updateObj.setStatus(3); // 3-报废
        assetMapper.updateById(updateObj);
    }

    private PageResult<ErpAssetScrappedRespVO> convertPage(PageResult<ErpAssetScrappedDO> pageResult) {
        PageResult<ErpAssetScrappedRespVO> result = new PageResult<>();
        result.setList(convertList(pageResult.getList()));
        result.setTotal(pageResult.getTotal());
        return result;
    }

    private List<ErpAssetScrappedRespVO> convertList(List<ErpAssetScrappedDO> list) {
        return BeanUtils.toBean(list, ErpAssetScrappedRespVO.class, scrapped -> {
            // 翻译资产信息
            if (scrapped.getAssetId() != null) {
                ErpAssetDO asset = assetMapper.selectById(scrapped.getAssetId());
                if (asset != null) {
                    scrapped.setAssetNo(asset.getAssetNo());
                    scrapped.setAssetName(asset.getName());
                    scrapped.setAssetType(asset.getType());
                    scrapped.setAssetSpecification(asset.getSpecification());
                    scrapped.setAssetBrand(asset.getBrand());
                    scrapped.setAssetModel(asset.getModel());
                    scrapped.setAssetLocation(asset.getLocation());
                }
            }
            
            // 翻译处理人信息
            if (scrapped.getHandleUserId() != null) {
                AdminUserRespDTO user = adminUserApi.getUser(scrapped.getHandleUserId()).getCheckedData();
                if (user != null) {
                    scrapped.setHandleUserName(user.getNickname());
                }
            }
            
            // 翻译处理部门信息
            if (scrapped.getHandleDeptId() != null) {
                DeptRespDTO dept = deptApi.getDept(scrapped.getHandleDeptId()).getCheckedData();
                if (dept != null) {
                    scrapped.setHandleDeptName(dept.getName());
                }
            }
            
            // 翻译审批人信息
            if (scrapped.getApproverUserId() != null) {
                AdminUserRespDTO approver = adminUserApi.getUser(scrapped.getApproverUserId()).getCheckedData();
                if (approver != null) {
                    scrapped.setApproverUserName(approver.getNickname());
                }
            }
            
            // 设置状态名称
            for (ErpAssetScrappedStatusEnum statusEnum : ErpAssetScrappedStatusEnum.values()) {
                if (statusEnum.getStatus().equals(scrapped.getStatus())) {
                    scrapped.setStatusName(statusEnum.getName());
                    break;
                }
            }
            
            // 设置审批状态名称
            if (scrapped.getApprovalStatus() != null) {
                switch (scrapped.getApprovalStatus()) {
                    case 1:
                        scrapped.setApprovalStatusName("待审批");
                        break;
                    case 2:
                        scrapped.setApprovalStatusName("审批通过");
                        break;
                    case 3:
                        scrapped.setApprovalStatusName("审批拒绝");
                        break;
                }
            }
            
            // 设置附件信息
            List<ErpAssetScrappedFileDO> files = scrappedFileService.getScrappedFiles(scrapped.getId());
            if (files != null && !files.isEmpty()) {
                scrapped.setAttachments(BeanUtils.toBean(files, ErpAssetScrappedRespVO.FileInfo.class));
            }
        });
    }
} 