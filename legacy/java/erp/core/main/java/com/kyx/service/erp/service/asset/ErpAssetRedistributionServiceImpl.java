package com.kyx.service.erp.service.asset;

import cn.hutool.core.collection.CollUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.bpm.api.task.BpmProcessInstanceApi;
import com.kyx.service.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.kyx.service.business.api.dept.DeptApi;
import com.kyx.service.business.api.dept.dto.DeptRespDTO;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import com.kyx.service.erp.controller.admin.asset.vo.redistribution.ErpAssetRedistributionPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.redistribution.ErpAssetRedistributionRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.redistribution.ErpAssetRedistributionSaveReqVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetDO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetOwnershipDO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetRedistributionDO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetRedistributionItemDO;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetMapper;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetOwnershipMapper;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetRedistributionItemMapper;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetRedistributionMapper;
import com.kyx.service.erp.dal.redis.no.ErpNoRedisDAO;
import com.kyx.service.erp.enums.asset.ErpAssetRedistributionApprovalStatusEnum;
import com.kyx.service.erp.enums.asset.ErpAssetRedistributionBmpStatusEnum;
import com.kyx.service.erp.enums.asset.ErpAssetRedistributionStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.service.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 资产调拨记录 Service 实现类
 *
 * @author kyx
 */
@Slf4j
@Service
@Validated
public class ErpAssetRedistributionServiceImpl implements ErpAssetRedistributionService {

    /**
     * 资产调拨对应的流程定义 KEY
     */
    public static final String PROCESS_KEY = "assets-redistribution";

    @Resource
    private ErpAssetRedistributionMapper redistributionMapper;
    @Resource
    private ErpAssetRedistributionItemMapper redistributionItemMapper;
    @Resource
    private ErpAssetMapper assetMapper;
    @Resource
    private ErpAssetOwnershipMapper assetOwnershipMapper;
    @Resource
    private ErpNoRedisDAO noRedisDAO;
    @Resource
    private BpmProcessInstanceApi processInstanceApi;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAssetRedistribution(ErpAssetRedistributionSaveReqVO createReqVO) {
        // 1. 校验资产和部门
        validateAssets(createReqVO.getAssetIds());
        validateDept(createReqVO.getToDeptId());

        // 2. 校验资产是否可以调拨
        if (!canRedistributeAssets(createReqVO.getAssetIds())) {
            throw exception(ASSET_REDISTRIBUTION_NOT_ALLOWED);
        }

        // 3. 生成调拨编号
        String redistributionNo = generateRedistributionNo();

        // 4. 创建调拨记录
        ErpAssetRedistributionDO redistribution = BeanUtils.toBean(createReqVO, ErpAssetRedistributionDO.class);
        redistribution.setRedistributionNo(redistributionNo);
        redistribution.setStatus(ErpAssetRedistributionStatusEnum.PENDING.getStatus());
        redistribution.setApprovalStatus(ErpAssetRedistributionApprovalStatusEnum.PENDING.getStatus());
        redistributionMapper.insert(redistribution);

        // 5. 创建调拨项
        createRedistributionItems(redistribution.getId(), createReqVO.getAssetIds());

        return redistribution.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAssetRedistributionAndSubmit(Long userId, ErpAssetRedistributionSaveReqVO createReqVO) {
        // 1. 校验资产和部门
        List<ErpAssetDO> assets = validateAssets(createReqVO.getAssetIds());
        DeptRespDTO toDept = validateDept(createReqVO.getToDeptId());

        // 2. 校验资产是否可以调拨
        if (!canRedistributeAssets(createReqVO.getAssetIds())) {
            throw exception(ASSET_REDISTRIBUTION_NOT_ALLOWED);
        }

        // 3. 生成调拨编号
        String redistributionNo = generateRedistributionNo();

        // 4.1 创建调拨记录 - 设置为流程中状态
        ErpAssetRedistributionDO redistribution = BeanUtils.toBean(createReqVO, ErpAssetRedistributionDO.class);
        redistribution.setRedistributionNo(redistributionNo);
        redistribution.setStatus(ErpAssetRedistributionStatusEnum.PENDING.getStatus());
        redistribution.setApprovalStatus(ErpAssetRedistributionApprovalStatusEnum.IN_PROGRESS.getStatus());
        redistribution.setBmpStatus(ErpAssetRedistributionBmpStatusEnum.IN_PROGRESS.getStatus());
        redistributionMapper.insert(redistribution);

        // 4.2 创建调拨项
        createRedistributionItems(redistribution.getId(), createReqVO.getAssetIds());

        // 4.3 发起 BPM 流程
        Map<String, Object> processInstanceVariables = new HashMap<>();
        processInstanceVariables.put("redistributionId", redistribution.getId());
        processInstanceVariables.put("redistributionNo", redistributionNo);
        processInstanceVariables.put("assetCount", createReqVO.getAssetIds().size());
        processInstanceVariables.put("fromDeptId", redistribution.getFromDeptId());
        processInstanceVariables.put("toDeptId", redistribution.getToDeptId());
        processInstanceVariables.put("toDeptName", toDept.getName());
        processInstanceVariables.put("toLocation", redistribution.getToLocation());
        processInstanceVariables.put("allocationReason", redistribution.getAllocationReason());
        processInstanceVariables.put("allocationDate", redistribution.getAllocationDate());

        // 添加资产信息到流程变量
        String assetInfo = assets.stream()
                .map(asset -> asset.getAssetNo() + "(" + asset.getName() + ")")
                .collect(Collectors.joining(", "));
        processInstanceVariables.put("assetInfo", assetInfo);

        String processInstanceId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(PROCESS_KEY)
                        .setVariables(processInstanceVariables).setBusinessKey(String.valueOf(redistribution.getId()))
                        .setStartUserSelectAssignees(createReqVO.getStartUserSelectAssignees())).getCheckedData();

        // 4.4 将工作流的编号，更新到调拨记录中
        ErpAssetRedistributionDO updateObj = new ErpAssetRedistributionDO();
        updateObj.setId(redistribution.getId());
        updateObj.setProcessInstanceId(processInstanceId);
        redistributionMapper.updateById(updateObj);

        return redistribution.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAssetRedistribution(ErpAssetRedistributionSaveReqVO updateReqVO) {
        // 校验存在
        validateAssetRedistributionExists(updateReqVO.getId());

        // 更新
        ErpAssetRedistributionDO updateObj = BeanUtils.toBean(updateReqVO, ErpAssetRedistributionDO.class);
        redistributionMapper.updateById(updateObj);

        // 更新调拨项
        updateRedistributionItems(updateReqVO.getId(), updateReqVO.getAssetIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAssetRedistribution(Long id) {
        // 校验存在
        validateAssetRedistributionExists(id);
        
        // 删除调拨项
        redistributionItemMapper.deleteByRedistributionId(id);
        
        // 删除调拨记录
        redistributionMapper.deleteById(id);
    }

    private ErpAssetRedistributionDO validateAssetRedistributionExists(Long id) {
        ErpAssetRedistributionDO redistribution = redistributionMapper.selectById(id);
        if (redistribution == null) {
            throw exception(ASSET_REDISTRIBUTION_NOT_EXISTS);
        }
        return redistribution;
    }

    @Override
    public ErpAssetRedistributionDO getAssetRedistribution(Long id) {
        return redistributionMapper.selectById(id);
    }

    @Override
    public PageResult<ErpAssetRedistributionRespVO> getAssetRedistributionPage(ErpAssetRedistributionPageReqVO pageReqVO) {
        PageResult<ErpAssetRedistributionDO> pageResult = redistributionMapper.selectPage(pageReqVO);
        List<ErpAssetRedistributionRespVO> respList = pageResult.getList().stream()
                .map(this::buildAssetRedistributionRespVO)
                .collect(Collectors.toList());
        return new PageResult<>(respList, pageResult.getTotal());
    }

    @Override
    public ErpAssetRedistributionRespVO getAssetRedistributionDetail(Long id) {
        ErpAssetRedistributionDO redistribution = redistributionMapper.selectById(id);
        if (redistribution == null) {
            return null;
        }
        return buildAssetRedistributionRespVOWithItems(redistribution);
    }

    @Override
    public List<ErpAssetRedistributionRespVO> getAssetRedistributionListByDeptId(Long deptId) {
        List<ErpAssetRedistributionDO> list = redistributionMapper.selectListByFromDeptId(deptId);
        list.addAll(redistributionMapper.selectListByToDeptId(deptId));
        return list.stream()
                .map(this::buildAssetRedistributionRespVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveAssetRedistribution(Long redistributionId, Integer approvalStatus, String approvalRemark) {
        ErpAssetRedistributionDO redistribution = validateAssetRedistributionExists(redistributionId);
        
        ErpAssetRedistributionDO updateObj = new ErpAssetRedistributionDO();
        updateObj.setId(redistributionId);
        updateObj.setApprovalStatus(approvalStatus);
        updateObj.setApprovalRemark(approvalRemark);
        updateObj.setApprovalTime(LocalDateTime.now());
        
        // 如果审批通过，需要更新资产归属
        if (ErpAssetRedistributionApprovalStatusEnum.APPROVED.getStatus().equals(approvalStatus)) {
            updateObj.setStatus(ErpAssetRedistributionStatusEnum.COMPLETED.getStatus());
            redistributionMapper.updateById(updateObj);
            
            // 更新资产归属和位置
            updateAssetsAfterApproval(redistributionId, redistribution);
        } else if (ErpAssetRedistributionApprovalStatusEnum.REJECTED.getStatus().equals(approvalStatus)) {
            updateObj.setStatus(ErpAssetRedistributionStatusEnum.REJECTED.getStatus());
            redistributionMapper.updateById(updateObj);
        } else {
            redistributionMapper.updateById(updateObj);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceiveAssetRedistribution(Long redistributionId, String confirmRemark) {
        ErpAssetRedistributionDO redistribution = validateAssetRedistributionExists(redistributionId);
        
        ErpAssetRedistributionDO updateObj = new ErpAssetRedistributionDO();
        updateObj.setId(redistributionId);
        updateObj.setConfirmTime(LocalDateTime.now());
        updateObj.setConfirmRemark(confirmRemark);
        updateObj.setStatus(ErpAssetRedistributionStatusEnum.COMPLETED.getStatus());
        
        redistributionMapper.updateById(updateObj);
    }

    @Override
    public boolean canRedistributeAssets(List<Long> assetIds) {
        if (CollUtil.isEmpty(assetIds)) {
            return false;
        }
        
        for (Long assetId : assetIds) {
            ErpAssetDO asset = assetMapper.selectById(assetId);
            if (asset == null) {
                return false;
            }
            
            // 检查资产状态：只有正常状态的资产可以调拨
            if (!asset.getStatus().equals(1)) {
                return false;
            }
            
            // 检查是否有进行中的调拨申请
            List<ErpAssetRedistributionItemDO> pendingItems = redistributionItemMapper.selectListByAssetId(assetId);
            for (ErpAssetRedistributionItemDO item : pendingItems) {
                ErpAssetRedistributionDO redistribution = redistributionMapper.selectById(item.getRedistributionId());
                if (redistribution != null && ErpAssetRedistributionStatusEnum.PENDING.getStatus().equals(redistribution.getStatus())) {
                    return false;
                }
            }
        }
        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAssetRedistributionBmpStatus(Long redistributionId, Integer approvalStatus, Integer bmpStatus, Long approverUserId) {
        ErpAssetRedistributionDO redistribution = validateAssetRedistributionExists(redistributionId);
        
        // 更新审批状态和BMP状态
        ErpAssetRedistributionDO updateObj = new ErpAssetRedistributionDO();
        updateObj.setId(redistributionId);
        updateObj.setApprovalStatus(approvalStatus);
        updateObj.setBmpStatus(bmpStatus);
        updateObj.setApprovalTime(LocalDateTime.now());
        updateObj.setApproverUserId(approverUserId); // 设置审批人用户ID
        
        // 根据审批结果更新调拨状态
        if (ErpAssetRedistributionApprovalStatusEnum.APPROVED.getStatus().equals(approvalStatus)) {
            // 审批通过后，设置为"已完成"状态，并更新资产归属
            updateObj.setStatus(ErpAssetRedistributionStatusEnum.COMPLETED.getStatus());
            redistributionMapper.updateById(updateObj);
            
            // 更新资产归属和位置
            updateAssetsAfterApproval(redistributionId, redistribution);
            
            log.info("资产调拨审批通过，更新资产归属: redistributionId={}, assetCount={}", 
                    redistributionId, redistribution);
        } else if (ErpAssetRedistributionApprovalStatusEnum.REJECTED.getStatus().equals(approvalStatus)) {
            // 审批拒绝后，设置为"已拒绝"状态
            updateObj.setStatus(ErpAssetRedistributionStatusEnum.REJECTED.getStatus());
            redistributionMapper.updateById(updateObj);
            
            log.info("资产调拨审批拒绝: redistributionId={}", redistributionId);
        } else {
            redistributionMapper.updateById(updateObj);
        }
    }

    /**
     * 构建资产调拨响应VO
     */
    private ErpAssetRedistributionRespVO buildAssetRedistributionRespVO(ErpAssetRedistributionDO redistribution) {
        ErpAssetRedistributionRespVO respVO = BeanUtils.toBean(redistribution, ErpAssetRedistributionRespVO.class);
        
        // 设置调拨前部门信息
        if (redistribution.getFromDeptId() != null) {
            DeptRespDTO fromDept = deptApi.getDept(redistribution.getFromDeptId()).getCheckedData();
            if (fromDept != null) {
                respVO.setFromDeptName(fromDept.getName());
            }
        }
        
        // 设置调拨到部门信息
        if (redistribution.getToDeptId() != null) {
            DeptRespDTO toDept = deptApi.getDept(redistribution.getToDeptId()).getCheckedData();
            if (toDept != null) {
                respVO.setToDeptName(toDept.getName());
            }
        }
        
        // 设置审批人信息
        if (redistribution.getApproverUserId() != null) {
            AdminUserRespDTO approver = adminUserApi.getUser(redistribution.getApproverUserId()).getCheckedData();
            if (approver != null) {
                respVO.setApproverUserName(approver.getNickname());
            }
        }
        
        return respVO;
    }

    /**
     * 构建带有调拨项的资产调拨响应VO
     */
    private ErpAssetRedistributionRespVO buildAssetRedistributionRespVOWithItems(ErpAssetRedistributionDO redistribution) {
        ErpAssetRedistributionRespVO respVO = buildAssetRedistributionRespVO(redistribution);
        
        // 设置调拨项列表
        List<ErpAssetRedistributionItemDO> items = redistributionItemMapper.selectListByRedistributionId(redistribution.getId());
        List<ErpAssetRedistributionRespVO.Item> itemVOs = items.stream()
                .map(this::buildRedistributionItemRespVO)
                .collect(Collectors.toList());
        respVO.setItems(itemVOs);
        
        return respVO;
    }

    /**
     * 构建调拨项响应VO
     */
    private ErpAssetRedistributionRespVO.Item buildRedistributionItemRespVO(ErpAssetRedistributionItemDO item) {
        ErpAssetRedistributionRespVO.Item itemVO = BeanUtils.toBean(item, ErpAssetRedistributionRespVO.Item.class);
        
        // 设置资产信息
        ErpAssetDO asset = assetMapper.selectById(item.getAssetId());
        if (asset != null) {
            itemVO.setAssetNo(asset.getAssetNo());
            itemVO.setAssetName(asset.getName());
        }
        
        // 设置原部门信息
        if (item.getOriginalDeptId() != null) {
            DeptRespDTO originalDept = deptApi.getDept(item.getOriginalDeptId()).getCheckedData();
            if (originalDept != null) {
                itemVO.setOriginalDeptName(originalDept.getName());
            }
        }
        
        // 设置原使用人信息
        if (item.getOriginalUserId() != null) {
            AdminUserRespDTO originalUser = adminUserApi.getUser(item.getOriginalUserId()).getCheckedData();
            if (originalUser != null) {
                itemVO.setOriginalUserName(originalUser.getNickname());
            }
        }
        
        return itemVO;
    }

    /**
     * 生成调拨编号
     */
    private String generateRedistributionNo() {
        String redistributionNo = noRedisDAO.generate(ErpNoRedisDAO.ASSET_REDISTRIBUTION_NO_PREFIX);
        while (redistributionMapper.selectByRedistributionNo(redistributionNo) != null) {
            redistributionNo = noRedisDAO.generate(ErpNoRedisDAO.ASSET_REDISTRIBUTION_NO_PREFIX);
        }
        return redistributionNo;
    }

    /**
     * 校验资产存在
     */
    private List<ErpAssetDO> validateAssets(List<Long> assetIds) {
        if (CollUtil.isEmpty(assetIds)) {
            throw exception(ASSET_REDISTRIBUTION_ASSETS_EMPTY);
        }
        
        List<ErpAssetDO> assets = assetMapper.selectBatchIds(assetIds);
        if (assets.size() != assetIds.size()) {
            throw exception(ASSET_NOT_EXISTS);
        }
        return assets;
    }

    /**
     * 校验部门存在
     */
    private DeptRespDTO validateDept(Long deptId) {
        DeptRespDTO dept = deptApi.getDept(deptId).getCheckedData();
        if (dept == null) {
            throw exception(ASSET_REDISTRIBUTION_NOT_ALLOWED);
        }
        return dept;
    }

    /**
     * 创建调拨项
     */
    private void createRedistributionItems(Long redistributionId, List<Long> assetIds) {
        for (Long assetId : assetIds) {
            ErpAssetDO asset = assetMapper.selectById(assetId);
            ErpAssetOwnershipDO ownership = assetOwnershipMapper.selectCurrentOwnership(assetId);
            
            ErpAssetRedistributionItemDO item = new ErpAssetRedistributionItemDO();
            item.setRedistributionId(redistributionId);
            item.setAssetId(assetId);
            item.setOriginalStatus(asset.getStatus());
            item.setOriginalLocation(asset.getLocation());
            item.setOriginalDeptId(asset.getDeptId());
            if (ownership != null) {
                item.setOriginalUserId(ownership.getCurrentUserId());
            }
            redistributionItemMapper.insert(item);
        }
    }

    /**
     * 更新调拨项
     */
    private void updateRedistributionItems(Long redistributionId, List<Long> assetIds) {
        // 删除原有的调拨项
        redistributionItemMapper.deleteByRedistributionId(redistributionId);
        
        // 重新创建调拨项
        createRedistributionItems(redistributionId, assetIds);
    }

    /**
     * 审批通过后更新资产
     */
    private void updateAssetsAfterApproval(Long redistributionId, ErpAssetRedistributionDO redistribution) {
        // 获取调拨项列表
        List<ErpAssetRedistributionItemDO> items = redistributionItemMapper.selectListByRedistributionId(redistributionId);
        
        for (ErpAssetRedistributionItemDO item : items) {
            // 更新资产部门和位置
            ErpAssetDO updateAsset = new ErpAssetDO();
            updateAsset.setId(item.getAssetId());
            updateAsset.setDeptId(redistribution.getToDeptId());
            updateAsset.setLocation(redistribution.getToLocation());
            assetMapper.updateById(updateAsset);
            
            // 清除原有的所有权关系（如果有的话）
            ErpAssetOwnershipDO currentOwnership = assetOwnershipMapper.selectCurrentOwnership(item.getAssetId());
            if (currentOwnership != null) {
                currentOwnership.setEndTime(LocalDateTime.now());
                currentOwnership.setStatus(2); // 已归还
                assetOwnershipMapper.updateById(currentOwnership);
                
                log.info("资产调拨清除原所有权关系: assetId={}, ownershipId={}, redistributionId={}", 
                        item.getAssetId(), currentOwnership.getId(), redistributionId);
            }
        }
        
        log.info("资产调拨完成，更新了 {} 个资产的归属和位置", items.size());
    }
} 