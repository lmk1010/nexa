package com.kyx.service.erp.service.asset;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.bpm.api.task.BpmProcessInstanceApi;
import com.kyx.service.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.kyx.service.business.api.dept.DeptApi;
import com.kyx.service.business.api.dept.dto.DeptRespDTO;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import com.kyx.service.erp.controller.admin.asset.vo.transfer.ErpAssetTransferPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.transfer.ErpAssetTransferRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.transfer.ErpAssetTransferSaveReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.transfer.ErpAssetTransferUserSearchReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.transfer.ErpAssetTransferUserSearchRespVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetDO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetOwnershipDO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetTransferDO;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetMapper;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetOwnershipMapper;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetTransferMapper;
import com.kyx.service.erp.dal.redis.no.ErpNoRedisDAO;
import com.kyx.service.erp.enums.ErrorCodeConstants;
import com.kyx.service.erp.enums.asset.ErpAssetTransferApprovalStatusEnum;
import com.kyx.service.erp.enums.asset.ErpAssetTransferBmpStatusEnum;
import com.kyx.service.erp.enums.asset.ErpAssetTransferStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.service.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 资产转移记录 Service 实现类
 *
 * @author kyx
 */
@Slf4j
@Service
@Validated
public class ErpAssetTransferServiceImpl implements ErpAssetTransferService {

    /**
     * 资产转移对应的流程定义 KEY
     */
    public static final String PROCESS_KEY = "assets-transfer";

    @Resource
    private ErpAssetTransferMapper assetTransferMapper;
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
    public Long createAssetTransfer(ErpAssetTransferSaveReqVO createReqVO) {
        // 注意：此方法缺少当前用户ID参数，建议使用createAssetTransferAndSubmit方法
        // 1. 校验资产和用户
        ErpAssetDO asset = validateAssetExists(createReqVO.getAssetId());
        // 获取当前资产的拥有者作为转移人
        ErpAssetOwnershipDO currentOwnership = assetOwnershipMapper.selectCurrentOwnership(createReqVO.getAssetId());
        if (currentOwnership == null) {
            throw exception(ASSET_TRANSFER_NOT_ALLOWED);
        }
        AdminUserRespDTO fromUser = validateUserExists(currentOwnership.getCurrentUserId());
        AdminUserRespDTO toUser = validateUserExists(createReqVO.getToUserId());

        // 2. 校验资产是否可以转移
        if (!canTransferAsset(createReqVO.getAssetId())) {
            throw exception(ASSET_TRANSFER_NOT_ALLOWED);
        }

        // 3. 生成转移编号
        String transferNo = generateTransferNo();

        // 4. 创建转移记录
        ErpAssetTransferDO transfer = BeanUtils.toBean(createReqVO, ErpAssetTransferDO.class);
        transfer.setTransferNo(transferNo);
        transfer.setFromUserId(fromUser.getId()); // 这里需要从当前登录用户获取
        transfer.setFromDeptId(fromUser.getDeptId());
        transfer.setToDeptId(toUser.getDeptId());
        transfer.setStatus(ErpAssetTransferStatusEnum.PENDING.getStatus());
        transfer.setApprovalStatus(ErpAssetTransferApprovalStatusEnum.PENDING.getStatus());
        assetTransferMapper.insert(transfer);

        return transfer.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAssetTransferAndSubmit(Long userId, ErpAssetTransferSaveReqVO createReqVO) {
        // 1. 校验资产和用户
        ErpAssetDO asset = validateAssetExists(createReqVO.getAssetId());
        AdminUserRespDTO fromUser = validateUserExists(userId);
        AdminUserRespDTO toUser = validateUserExists(createReqVO.getToUserId());

        // 2. 校验资产是否可以转移
        if (!canTransferAsset(createReqVO.getAssetId())) {
            throw exception(ASSET_TRANSFER_NOT_ALLOWED);
        }

        // 3. 生成转移编号
        String transferNo = generateTransferNo();

        // 4.1 创建转移记录 - 设置为流程中状态
        ErpAssetTransferDO transfer = BeanUtils.toBean(createReqVO, ErpAssetTransferDO.class);
        transfer.setTransferNo(transferNo);
        transfer.setFromUserId(fromUser.getId());
        transfer.setFromDeptId(fromUser.getDeptId());
        transfer.setToDeptId(toUser.getDeptId());
        transfer.setStatus(ErpAssetTransferStatusEnum.PENDING.getStatus());
        transfer.setApprovalStatus(ErpAssetTransferApprovalStatusEnum.IN_PROGRESS.getStatus());
        transfer.setBmpStatus(ErpAssetTransferBmpStatusEnum.IN_PROGRESS.getStatus());
        assetTransferMapper.insert(transfer);

        // 4.2 发起 BPM 流程
        Map<String, Object> processInstanceVariables = new HashMap<>();
        processInstanceVariables.put("transferId", transfer.getId());
        processInstanceVariables.put("assetId", transfer.getAssetId());
        processInstanceVariables.put("assetNo", asset.getAssetNo());
        processInstanceVariables.put("assetName", asset.getName());
        processInstanceVariables.put("fromUserId", fromUser.getId());
        processInstanceVariables.put("fromUserName", fromUser.getNickname());
        processInstanceVariables.put("toUserId", toUser.getId());
        processInstanceVariables.put("toUserName", toUser.getNickname());
        processInstanceVariables.put("transferReason", transfer.getTransferReason());
        processInstanceVariables.put("transferDate", transfer.getTransferDate());

        String processInstanceId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(PROCESS_KEY)
                        .setVariables(processInstanceVariables).setBusinessKey(String.valueOf(transfer.getId()))
                        .setStartUserSelectAssignees(createReqVO.getStartUserSelectAssignees())).getCheckedData();

        // 4.3 将工作流的编号，更新到转移记录中
        ErpAssetTransferDO updateObj = new ErpAssetTransferDO();
        updateObj.setId(transfer.getId());
        updateObj.setProcessInstanceId(processInstanceId);
        assetTransferMapper.updateById(updateObj);

        return transfer.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAssetTransfer(ErpAssetTransferSaveReqVO updateReqVO) {
        // 校验存在
        validateAssetTransferExists(updateReqVO.getId());

        // 更新
        ErpAssetTransferDO updateObj = BeanUtils.toBean(updateReqVO, ErpAssetTransferDO.class);
        assetTransferMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAssetTransfer(Long id) {
        // 校验存在
        validateAssetTransferExists(id);
        // 删除
        assetTransferMapper.deleteById(id);
    }

    private ErpAssetTransferDO validateAssetTransferExists(Long id) {
        ErpAssetTransferDO transfer = assetTransferMapper.selectById(id);
        if (transfer == null) {
            throw exception(ASSET_TRANSFER_NOT_EXISTS);
        }
        return transfer;
    }

    @Override
    public ErpAssetTransferDO getAssetTransfer(Long id) {
        return assetTransferMapper.selectById(id);
    }

    @Override
    public PageResult<ErpAssetTransferRespVO> getAssetTransferPage(ErpAssetTransferPageReqVO pageReqVO) {
        PageResult<ErpAssetTransferDO> pageResult = assetTransferMapper.selectPage(pageReqVO);
        List<ErpAssetTransferRespVO> respList = pageResult.getList().stream()
                .map(this::buildAssetTransferRespVO)
                .collect(Collectors.toList());
        return new PageResult<>(respList, pageResult.getTotal());
    }

    @Override
    public List<ErpAssetTransferRespVO> getAssetTransferListByAssetId(Long assetId) {
        List<ErpAssetTransferDO> list = assetTransferMapper.selectListByAssetId(assetId);
        return list.stream()
                .map(this::buildAssetTransferRespVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ErpAssetTransferRespVO> getAssetTransferListByFromUserId(Long fromUserId) {
        List<ErpAssetTransferDO> list = assetTransferMapper.selectListByFromUserId(fromUserId);
        return list.stream()
                .map(this::buildAssetTransferRespVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ErpAssetTransferRespVO> getAssetTransferListByToUserId(Long toUserId) {
        List<ErpAssetTransferDO> list = assetTransferMapper.selectListByToUserId(toUserId);
        return list.stream()
                .map(this::buildAssetTransferRespVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveAssetTransfer(Long transferId, Integer approvalStatus, String approvalRemark) {
        ErpAssetTransferDO transfer = validateAssetTransferExists(transferId);
        
        ErpAssetTransferDO updateObj = new ErpAssetTransferDO();
        updateObj.setId(transferId);
        updateObj.setApprovalStatus(approvalStatus);
        updateObj.setApprovalRemark(approvalRemark);
        updateObj.setApprovalTime(LocalDateTime.now());
        
        // 如果审批通过，需要更新资产归属
        if (ErpAssetTransferApprovalStatusEnum.APPROVED.getStatus().equals(approvalStatus)) {
            updateObj.setStatus(ErpAssetTransferStatusEnum.COMPLETED.getStatus());
            updateAssetOwnership(transfer.getAssetId(), transfer.getFromUserId(), transfer.getToUserId());
        } else if (ErpAssetTransferApprovalStatusEnum.REJECTED.getStatus().equals(approvalStatus)) {
            updateObj.setStatus(ErpAssetTransferStatusEnum.REJECTED.getStatus());
        }
        
        assetTransferMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceiveAssetTransfer(Long transferId, String confirmRemark) {
        ErpAssetTransferDO transfer = validateAssetTransferExists(transferId);
        
        ErpAssetTransferDO updateObj = new ErpAssetTransferDO();
        updateObj.setId(transferId);
        updateObj.setConfirmTime(LocalDateTime.now());
        updateObj.setConfirmRemark(confirmRemark);
        updateObj.setStatus(ErpAssetTransferStatusEnum.COMPLETED.getStatus());
        
        assetTransferMapper.updateById(updateObj);
    }

    @Override
    public List<ErpAssetTransferUserSearchRespVO> searchUsers(ErpAssetTransferUserSearchReqVO searchReqVO) {
        // 调用用户API搜索用户 - 暂时返回空列表，需要实现具体的用户搜索逻辑
        List<AdminUserRespDTO> users = new ArrayList<>();
        
        // 过滤排除的用户ID
        if (searchReqVO.getExcludeUserId() != null) {
            users = users.stream()
                    .filter(user -> !user.getId().equals(searchReqVO.getExcludeUserId()))
                    .collect(Collectors.toList());
        }
        
        // 转换为响应VO
        return users.stream().map(user -> {
            ErpAssetTransferUserSearchRespVO respVO = new ErpAssetTransferUserSearchRespVO();
            respVO.setId(user.getId());
            respVO.setUsername(user.getUsername());
            respVO.setNickname(user.getNickname());
            respVO.setDeptId(user.getDeptId());
            respVO.setMobile(user.getMobile());
            
            // 获取部门名称
            if (user.getDeptId() != null) {
                DeptRespDTO dept = deptApi.getDept(user.getDeptId()).getCheckedData();
                if (dept != null) {
                    respVO.setDeptName(dept.getName());
                }
            }
            
            return respVO;
        }).collect(Collectors.toList());
    }

    @Override
    public boolean canTransferAsset(Long assetId) {
        ErpAssetDO asset = assetMapper.selectById(assetId);
        if (asset == null) {
            return false;
        }
        
        // 检查资产状态：只有正常状态的资产可以转移
        if (!asset.getStatus().equals(1)) {
            return false;
        }
        
        // 检查是否有进行中的转移申请
        List<ErpAssetTransferDO> pendingTransfers = assetTransferMapper.selectListByAssetId(assetId).stream()
                .filter(transfer -> ErpAssetTransferStatusEnum.PENDING.getStatus().equals(transfer.getStatus()))
                .collect(Collectors.toList());
        
        return pendingTransfers.isEmpty();
    }

    /**
     * 构建资产转移响应VO
     */
    private ErpAssetTransferRespVO buildAssetTransferRespVO(ErpAssetTransferDO transfer) {
        ErpAssetTransferRespVO respVO = BeanUtils.toBean(transfer, ErpAssetTransferRespVO.class);
        // 设置资产信息
        ErpAssetDO asset = assetMapper.selectById(transfer.getAssetId());
        if (asset != null) {
            respVO.setAssetNo(asset.getAssetNo());
            respVO.setAssetName(asset.getName());
        }
        
        // 设置转移人信息
        AdminUserRespDTO fromUser = adminUserApi.getUser(transfer.getFromUserId()).getCheckedData();
        if (fromUser != null) {
            respVO.setFromUserName(fromUser.getNickname());
        }
        
        // 设置接收人信息
        AdminUserRespDTO toUser = adminUserApi.getUser(transfer.getToUserId()).getCheckedData();
        if (toUser != null) {
            respVO.setToUserName(toUser.getNickname());
        }
        
        // 设置转移人部门信息
        if (transfer.getFromDeptId() != null) {
            DeptRespDTO fromDept = deptApi.getDept(transfer.getFromDeptId()).getCheckedData();
            if (fromDept != null) {
                respVO.setFromDeptName(fromDept.getName());
            }
        }
        
        // 设置接收人部门信息
        if (transfer.getToDeptId() != null) {
            DeptRespDTO toDept = deptApi.getDept(transfer.getToDeptId()).getCheckedData();
            if (toDept != null) {
                respVO.setToDeptName(toDept.getName());
            }
        }
        
        // 设置审批人信息
        if (transfer.getApproverUserId() != null) {
            AdminUserRespDTO approver = adminUserApi.getUser(transfer.getApproverUserId()).getCheckedData();
            if (approver != null) {
                respVO.setApproverUserName(approver.getNickname());
            }
        }
        
        return respVO;
    }

    /**
     * 生成转移编号
     */
    private String generateTransferNo() {
        String transferNo = noRedisDAO.generate(ErpNoRedisDAO.ASSET_TRANSFER_NO_PREFIX);
        while (assetTransferMapper.selectByTransferNo(transferNo) != null) {
            transferNo = noRedisDAO.generate(ErpNoRedisDAO.ASSET_TRANSFER_NO_PREFIX);
        }
        return transferNo;
    }

    /**
     * 校验资产存在
     */
    private ErpAssetDO validateAssetExists(Long assetId) {
        ErpAssetDO asset = assetMapper.selectById(assetId);
        if (asset == null) {
            throw exception(ASSET_NOT_EXISTS);
        }
        return asset;
    }

    /**
     * 校验用户存在
     */
    private AdminUserRespDTO validateUserExists(Long userId) {
        AdminUserRespDTO user = adminUserApi.getUser(userId).getCheckedData();
        if (user == null) {
            throw exception(USER_NOT_EXISTS);
        }
        return user;
    }

    /**
     * 更新资产归属
     */
    private void updateAssetOwnership(Long assetId, Long fromUserId, Long toUserId) {
        // 获取接收人的部门信息
        AdminUserRespDTO toUser = validateUserExists(toUserId);
        
        // 查找当前资产的所有权记录
        ErpAssetOwnershipDO currentOwnership = assetOwnershipMapper.selectCurrentOwnership(assetId);
        
        if (currentOwnership != null) {
            // 检查当前ownership记录是否属于转移人
            if (currentOwnership.getCurrentUserId().equals(fromUserId)) {
                // 正常情况：当前ownership记录属于转移人，直接删除
                assetOwnershipMapper.deleteById(currentOwnership.getId());
                log.info("资产转移删除旧的所有权记录: assetId={}, ownershipId={}, fromUserId={}, toUserId={}", 
                        assetId, currentOwnership.getId(), fromUserId, toUserId);
            } else {
                // 异常情况：当前ownership记录不属于转移人，记录警告并强制删除
                log.warn("资产转移发现数据不一致：当前所有权记录不属于转移人，强制删除。assetId={}, ownershipId={}, " +
                        "currentUserId={}, fromUserId={}, toUserId={}", 
                        assetId, currentOwnership.getId(), currentOwnership.getCurrentUserId(), fromUserId, toUserId);
                assetOwnershipMapper.deleteById(currentOwnership.getId());
            }
        } else {
            // 异常情况：资产没有所有权记录
            log.warn("资产转移时发现资产没有所有权记录: assetId={}, fromUserId={}, toUserId={}", 
                    assetId, fromUserId, toUserId);
        }
        
        // 创建新的归属记录
        ErpAssetOwnershipDO newOwnership = new ErpAssetOwnershipDO();
        newOwnership.setAssetId(assetId);
        newOwnership.setCurrentUserId(toUserId);
        newOwnership.setCurrentDeptId(toUser.getDeptId());
        newOwnership.setCheckoutId(null); // 转移获得的资产，不关联领用记录
        newOwnership.setStartTime(LocalDateTime.now());
        newOwnership.setStatus(1); // 使用中
        newOwnership.setRemark("资产转移");
        assetOwnershipMapper.insert(newOwnership);
        
        log.info("资产转移创建新的所有权记录: assetId={}, ownershipId={}, fromUserId={}, toUserId={}", 
                assetId, newOwnership.getId(), fromUserId, toUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAssetTransferBmpStatus(Long transferId, Integer approvalStatus, Integer bmpStatus) {
        ErpAssetTransferDO transfer = validateAssetTransferExists(transferId);
        
        // 更新审批状态和BMP状态
        ErpAssetTransferDO updateObj = new ErpAssetTransferDO();
        updateObj.setId(transferId);
        updateObj.setApprovalStatus(approvalStatus);
        updateObj.setBmpStatus(bmpStatus);
        updateObj.setApprovalTime(LocalDateTime.now());
        
        // 根据审批结果更新转移状态
        if (ErpAssetTransferApprovalStatusEnum.APPROVED.getStatus().equals(approvalStatus)) {
            // 审批通过后，设置为"已完成"状态，并更新资产归属
            updateObj.setStatus(ErpAssetTransferStatusEnum.COMPLETED.getStatus());
            assetTransferMapper.updateById(updateObj);
            
            // 更新资产所有权关系
            updateAssetOwnership(transfer.getAssetId(), transfer.getFromUserId(), transfer.getToUserId());
            
            log.info("资产转移审批通过，更新所有权关系: transferId={}, assetId={}, fromUserId={}, toUserId={}", 
                    transferId, transfer.getAssetId(), transfer.getFromUserId(), transfer.getToUserId());
        } else if (ErpAssetTransferApprovalStatusEnum.REJECTED.getStatus().equals(approvalStatus)) {
            // 审批拒绝后，设置为"已拒绝"状态
            updateObj.setStatus(ErpAssetTransferStatusEnum.REJECTED.getStatus());
            assetTransferMapper.updateById(updateObj);
            
            log.info("资产转移审批拒绝: transferId={}, assetId={}", transferId, transfer.getAssetId());
        } else {
            assetTransferMapper.updateById(updateObj);
        }
    }

} 