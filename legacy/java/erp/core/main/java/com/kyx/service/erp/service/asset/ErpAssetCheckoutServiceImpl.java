package com.kyx.service.erp.service.asset;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.erp.controller.admin.asset.vo.checkout.ErpAssetCheckoutPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.checkout.ErpAssetCheckoutRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.checkout.ErpAssetCheckoutSaveReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.checkout.ErpAssetReturnReqVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetCheckoutDO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetDO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetOwnershipDO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetReturnDO;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetCheckoutMapper;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetMapper;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetOwnershipMapper;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetReturnMapper;
import com.kyx.service.business.api.dept.DeptApi;
import com.kyx.service.business.api.dept.dto.DeptRespDTO;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import com.kyx.service.bpm.api.task.BpmProcessInstanceApi;
import com.kyx.service.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.kyx.service.erp.enums.asset.ErpAssetCheckoutBmpStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.foundation.common.util.collection.CollectionUtils.convertList;
import static com.kyx.foundation.common.util.collection.CollectionUtils.convertMap;
import static com.kyx.foundation.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static com.kyx.service.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 资产领用记录 Service 实现类
 *
 * @author kyx
 */
@Slf4j
@Service
@Validated
public class ErpAssetCheckoutServiceImpl implements ErpAssetCheckoutService {

    /**
     * 资产领用对应的流程定义 KEY
     */
    public static final String PROCESS_KEY = "assets-get";

    @Resource
    private ErpAssetCheckoutMapper checkoutMapper;
    @Resource
    private ErpAssetMapper assetMapper;
    @Resource
    private ErpAssetOwnershipMapper ownershipMapper;
    @Resource
    private ErpAssetReturnMapper assetReturnMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCheckout(@Valid ErpAssetCheckoutSaveReqVO createReqVO) {
        // 1. 校验资产是否存在
        ErpAssetDO asset = validateAssetExists(createReqVO.getAssetId());
        
        // 2. 校验资产是否可以领用
        if (!canCheckout(createReqVO.getAssetId())) {
            throw exception(ASSET_CHECKOUT_NOT_AVAILABLE);
        }
        
        // 3. 校验用户和部门是否存在
        validateUserExists(createReqVO.getCheckoutUserId());
        validateDeptExists(createReqVO.getCheckoutDeptId());
        
        // 4. 创建领用记录
        ErpAssetCheckoutDO checkout = BeanUtils.toBean(createReqVO, ErpAssetCheckoutDO.class);
        checkout.setStatus(0); // 申请中
        checkout.setApprovalStatus(1); // 待审批
        checkoutMapper.insert(checkout);
        
        // 5. 注意：不在此处创建资产所有权关系，等审批通过后再创建
        // 资产表不再维护使用人信息，使用权关系通过ownership表管理
        
        return checkout.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCheckoutAndSubmit(Long userId, @Valid ErpAssetCheckoutSaveReqVO createReqVO) {
        // 1. 校验资产是否存在
        ErpAssetDO asset = validateAssetExists(createReqVO.getAssetId());
        
        // 2. 校验资产是否可以领用
        if (!canCheckout(createReqVO.getAssetId())) {
            throw exception(ASSET_CHECKOUT_NOT_AVAILABLE);
        }
        
        // 3. 校验用户和部门是否存在
        validateUserExists(createReqVO.getCheckoutUserId());
        validateDeptExists(createReqVO.getCheckoutDeptId());
        
        // 4. 创建领用记录 - 设置为流程中状态
        ErpAssetCheckoutDO checkout = BeanUtils.toBean(createReqVO, ErpAssetCheckoutDO.class);
        checkout.setStatus(0); // 申请中
        checkout.setApprovalStatus(1); // 待审批
        checkout.setBmpStatus(ErpAssetCheckoutBmpStatusEnum.IN_PROGRESS.getStatus()); // 流程中
        checkoutMapper.insert(checkout);
        
        // 5. 注意：不在此处创建资产所有权关系，等审批通过后再创建
        
        // 6. 发起 BPM 流程
        Map<String, Object> processInstanceVariables = new HashMap<>();
        processInstanceVariables.put("checkoutId", checkout.getId());
        processInstanceVariables.put("assetId", checkout.getAssetId());
        processInstanceVariables.put("assetNo", asset.getAssetNo());
        processInstanceVariables.put("assetName", asset.getName());
        processInstanceVariables.put("checkoutUserId", checkout.getCheckoutUserId());
        processInstanceVariables.put("checkoutDeptId", checkout.getCheckoutDeptId());
        processInstanceVariables.put("checkoutReason", checkout.getCheckoutReason());
        
        String processInstanceId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(PROCESS_KEY)
                        .setVariables(processInstanceVariables).setBusinessKey(String.valueOf(checkout.getId()))
                        .setStartUserSelectAssignees(createReqVO.getStartUserSelectAssignees())).getCheckedData();
        
        // 7. 将工作流的编号，更新到领用记录中
        ErpAssetCheckoutDO updateObj = new ErpAssetCheckoutDO();
        updateObj.setId(checkout.getId());
        updateObj.setProcessInstanceId(processInstanceId);
        checkoutMapper.updateById(updateObj);
        
        return checkout.getId();
    }

    @Override
    public void updateCheckout(@Valid ErpAssetCheckoutSaveReqVO updateReqVO) {
        // 校验存在
        validateCheckoutExists(updateReqVO.getId());
        
        // 校验资产是否存在
        validateAssetExists(updateReqVO.getAssetId());
        
        // 校验用户和部门是否存在
        validateUserExists(updateReqVO.getCheckoutUserId());
        validateDeptExists(updateReqVO.getCheckoutDeptId());
        
        // 更新
        ErpAssetCheckoutDO updateObj = BeanUtils.toBean(updateReqVO, ErpAssetCheckoutDO.class);
        checkoutMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCheckout(Long id) {
        // 校验存在
        ErpAssetCheckoutDO checkout = validateCheckoutExists(id);
        
        // 只有待审批且申请中状态的记录才能删除
        if (!checkout.getApprovalStatus().equals(1) || !checkout.getStatus().equals(0)) {
            throw exception(ASSET_CHECKOUT_DELETE_FAIL_STATUS_ERROR);
        }
        
        // 删除领用记录
        checkoutMapper.deleteById(id);
        
        // 删除对应的所有权关系
        ErpAssetOwnershipDO ownership = ownershipMapper.selectCurrentOwnership(checkout.getAssetId());
        if (ownership != null && ownership.getCheckoutId().equals(id)) {
            ownershipMapper.deleteById(ownership.getId());
        }
    }

    private ErpAssetCheckoutDO validateCheckoutExists(Long id) {
        ErpAssetCheckoutDO checkout = checkoutMapper.selectById(id);
        if (checkout == null) {
            throw exception(ASSET_CHECKOUT_NOT_EXISTS);
        }
        return checkout;
    }

    @Override
    public ErpAssetCheckoutDO getCheckout(Long id) {
        return checkoutMapper.selectById(id);
    }

    @Override
    public PageResult<ErpAssetCheckoutRespVO> getCheckoutPage(ErpAssetCheckoutPageReqVO pageReqVO) {
        PageResult<ErpAssetCheckoutDO> pageResult = checkoutMapper.selectPage(pageReqVO);
        return buildCheckoutVOPageResult(pageResult);
    }

    @Override
    public List<ErpAssetCheckoutRespVO> getCheckoutListByAssetId(Long assetId) {
        List<ErpAssetCheckoutDO> list = checkoutMapper.selectListByAssetId(assetId);
        return buildCheckoutVOList(list);
    }

    @Override
    public List<ErpAssetCheckoutRespVO> getCheckoutListByUserId(Long userId) {
        List<ErpAssetCheckoutDO> list = checkoutMapper.selectListByUserId(userId);
        return buildCheckoutVOList(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnAsset(@Valid ErpAssetReturnReqVO returnReqVO) {
        // 1. 校验领用记录存在
        ErpAssetCheckoutDO checkout = validateCheckoutExists(returnReqVO.getCheckoutId());
        
        // 2. 校验状态（只有领用中的才能归还）
        if (!checkout.getStatus().equals(1)) {
            throw exception(ASSET_RETURN_FAIL_STATUS_ERROR);
        }
        
        // 3. 更新领用记录
        checkout.setActualReturnDate(returnReqVO.getActualReturnDate());
        checkout.setReturnCondition(returnReqVO.getReturnCondition());
        checkout.setReturnRemark(returnReqVO.getReturnRemark());
        checkout.setStatus(2); // 已归还
        checkoutMapper.updateById(checkout);
        
        // 4. 创建归还记录（重要：让用户可以在"我归还的资产"中看到记录）
        ErpAssetReturnDO assetReturn = new ErpAssetReturnDO();
        assetReturn.setCheckoutId(returnReqVO.getCheckoutId());
        assetReturn.setAssetId(checkout.getAssetId());
        assetReturn.setReturnUserId(checkout.getCheckoutUserId()); // 归还人就是领用人
        assetReturn.setReturnDeptId(checkout.getCheckoutDeptId()); // 归还部门就是领用部门
        assetReturn.setReturnDate(returnReqVO.getActualReturnDate());
        assetReturn.setReturnCondition(returnReqVO.getReturnCondition());
        assetReturn.setReturnRemark(returnReqVO.getReturnRemark());
        assetReturn.setStatus(2); // 设置为已接收确认状态（直接归还，无需管理员确认）
        assetReturn.setReceiverUserId(getLoginUserId()); // 接收人为当前操作人（可能是用户自己或管理员）
        assetReturn.setReceiverTime(LocalDateTime.now());
        assetReturn.setReceiverRemark("系统自动接收");
        assetReturnMapper.insert(assetReturn);
        
        // 5. 删除所有权关系记录（归还后资产不再归属任何人）
        ErpAssetOwnershipDO ownership = ownershipMapper.selectCurrentOwnership(checkout.getAssetId());
        if (ownership != null) {
            ownershipMapper.deleteById(ownership.getId());
            log.info("资产归还成功，删除所有权记录: checkoutId={}, assetId={}, ownershipId={}, returnId={}", 
                    returnReqVO.getCheckoutId(), checkout.getAssetId(), ownership.getId(), assetReturn.getId());
        }
        
        // 6. 资产归还后状态清理完成，历史信息保留在领用记录和归还记录中
        log.info("资产归还成功: checkoutId={}, assetId={}, returnId={}, returnUserId={}", 
                returnReqVO.getCheckoutId(), checkout.getAssetId(), assetReturn.getId(), checkout.getCheckoutUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveCheckout(Long checkoutId, Integer approvalStatus, String approvalRemark) {
        // 1. 校验领用记录存在
        ErpAssetCheckoutDO checkout = validateCheckoutExists(checkoutId);
        
        // 2. 校验状态（只有待审批的才能审批）
        if (!checkout.getApprovalStatus().equals(1)) {
            throw exception(ASSET_CHECKOUT_APPROVE_FAIL_STATUS_ERROR);
        }
        
        // 3. 更新审批信息
        checkout.setApproverUserId(getLoginUserId());
        checkout.setApprovalTime(LocalDateTime.now());
        checkout.setApprovalStatus(approvalStatus);
        checkout.setApprovalRemark(approvalRemark);
        
        // 4. 根据审批结果更新状态并处理相关业务
        if (approvalStatus.equals(2)) { // 审批通过
            checkout.setStatus(1); // 设置为"领用中"
            createAssetOwnership(checkout);
        } else if (approvalStatus.equals(3)) { // 审批拒绝
            checkout.setStatus(6); // 设置为"申请拒绝"
        }
        
        // 注意：审批拒绝时无需删除ownership记录，因为开始时就没有创建
        
        checkoutMapper.updateById(checkout);
    }

    @Override
    public List<ErpAssetCheckoutRespVO> getOverdueCheckoutList() {
        LocalDate currentDate = LocalDate.now();
        List<ErpAssetCheckoutDO> list = checkoutMapper.selectOverdueList(currentDate);
        return buildCheckoutVOList(list);
    }

    @Override
    public boolean canCheckout(Long assetId) {
        // 1. 检查资产是否存在
        ErpAssetDO asset = assetMapper.selectById(assetId);
        if (asset == null) {
            return false;
        }
        
        // 2. 检查资产状态（只有正常和闲置状态的可以领用）
        if (!asset.getStatus().equals(1) && !asset.getStatus().equals(4)) {
            return false;
        }
        
        // 3. 检查是否已有申请中或领用中的记录
        ErpAssetCheckoutDO pendingCheckout = checkoutMapper.selectByAssetIdAndStatus(assetId, 0); // 申请中
        ErpAssetCheckoutDO activeCheckout = checkoutMapper.selectByAssetIdAndStatus(assetId, 1);  // 领用中
        if (pendingCheckout != null || activeCheckout != null) {
            return false;
        }
        
        return true;
    }

    private void createAssetOwnership(ErpAssetCheckoutDO checkout) {
        ErpAssetOwnershipDO ownership = ErpAssetOwnershipDO.builder()
                .assetId(checkout.getAssetId())
                .currentUserId(checkout.getCheckoutUserId())
                .currentDeptId(checkout.getCheckoutDeptId())
                .checkoutId(checkout.getId())
                .startTime(LocalDateTime.now())
                .status(1) // 使用中
                .build();
        ownershipMapper.insert(ownership);
    }

    private PageResult<ErpAssetCheckoutRespVO> buildCheckoutVOPageResult(PageResult<ErpAssetCheckoutDO> pageResult) {
        List<ErpAssetCheckoutRespVO> list = buildCheckoutVOList(pageResult.getList());
        return new PageResult<>(list, pageResult.getTotal());
    }

    private List<ErpAssetCheckoutRespVO> buildCheckoutVOList(List<ErpAssetCheckoutDO> list) {
        if (list.isEmpty()) {
            return convertList(list, checkout -> BeanUtils.toBean(checkout, ErpAssetCheckoutRespVO.class));
        }
        
        // 获取资产信息
        Map<Long, ErpAssetDO> assetMap = convertMap(
                assetMapper.selectBatchIds(convertList(list, ErpAssetCheckoutDO::getAssetId)), 
                ErpAssetDO::getId);
        
        // 获取用户信息
        Set<Long> userIds = list.stream()
                .flatMap(checkout -> Stream.of(checkout.getCheckoutUserId(), checkout.getApproverUserId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        
        // 获取部门信息  
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(
                convertList(list, ErpAssetCheckoutDO::getCheckoutDeptId));
        
        // 构建VO
        return convertList(list, checkout -> {
            ErpAssetCheckoutRespVO vo = BeanUtils.toBean(checkout, ErpAssetCheckoutRespVO.class);
            
            // 设置资产信息
            ErpAssetDO asset = assetMap.get(checkout.getAssetId());
            if (asset != null) {
                vo.setAssetNo(asset.getAssetNo());
                vo.setAssetName(asset.getName());
            }
            
            // 设置领用人信息
            AdminUserRespDTO checkoutUser = userMap.get(checkout.getCheckoutUserId());
            if (checkoutUser != null) {
                vo.setCheckoutUserName(checkoutUser.getNickname());
            }
            
            // 设置审批人信息
            if (checkout.getApproverUserId() != null) {
                AdminUserRespDTO approverUser = userMap.get(checkout.getApproverUserId());
                if (approverUser != null) {
                    vo.setApproverUserName(approverUser.getNickname());
                }
            }
            
            // 设置部门信息
            DeptRespDTO dept = deptMap.get(checkout.getCheckoutDeptId());
            if (dept != null) {
                vo.setCheckoutDeptName(dept.getName());
            }
            
            return vo;
        });
    }

    private ErpAssetDO validateAssetExists(Long assetId) {
        ErpAssetDO asset = assetMapper.selectById(assetId);
        if (asset == null) {
            throw exception(ASSET_NOT_EXISTS);
        }
        return asset;
    }

    private void validateUserExists(Long userId) {
        AdminUserRespDTO user = adminUserApi.getUser(userId).getCheckedData();
        if (user == null) {
            throw exception(USER_NOT_EXISTS);
        }
    }

    private void validateDeptExists(Long deptId) {
        DeptRespDTO dept = deptApi.getDept(deptId).getCheckedData();
        if (dept == null) {
            throw exception(DEPT_NOT_EXISTS);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCheckoutBmpStatus(Long id, Integer approvalStatus, Integer bmpStatus) {
        ErpAssetCheckoutDO checkout = validateCheckoutExists(id);
        
        // 更新审批状态和BMP状态
        ErpAssetCheckoutDO updateObj = new ErpAssetCheckoutDO();
        updateObj.setId(id);
        updateObj.setApprovalStatus(approvalStatus);
        updateObj.setBmpStatus(bmpStatus);
        updateObj.setApprovalTime(LocalDateTime.now());
        
        // 根据审批结果更新领用状态
        if (approvalStatus.equals(2)) { // 审批通过
            // 审批通过后，设置为"领用中"状态，用户开始实际使用资产
            updateObj.setStatus(1); // 设置状态为"领用中"
            checkoutMapper.updateById(updateObj);
            
            // 创建资产所有权关系
            handleAssetOwnershipOnApproval(checkout);
            
            log.info("资产领用审批通过，创建所有权关系: checkoutId={}, assetId={}, userId={}", 
                    id, checkout.getAssetId(), checkout.getCheckoutUserId());
        } else if (approvalStatus.equals(3)) { // 审批拒绝
            // 审批拒绝后，设置为"申请拒绝"状态
            updateObj.setStatus(6); // 设置状态为"申请拒绝"
            checkoutMapper.updateById(updateObj);
            
            log.info("资产领用审批拒绝: checkoutId={}, assetId={}", id, checkout.getAssetId());
        } else {
            checkoutMapper.updateById(updateObj);
        }
    }
    
    /**
     * 处理审批通过时的资产所有权关系
     */
    private void handleAssetOwnershipOnApproval(ErpAssetCheckoutDO checkout) {
        // 检查是否已存在当前资产的所有权记录
        ErpAssetOwnershipDO existingOwnership = ownershipMapper.selectCurrentOwnership(checkout.getAssetId());
        
        if (existingOwnership != null) {
            // 如果已存在所有权记录，且是当前领用记录创建的，则无需重复创建
            if (existingOwnership.getCheckoutId().equals(checkout.getId())) {
                return;
            }
            
            // 如果是其他领用记录创建的，先删除旧的所有权记录（异常情况，一般不应该发生）
            ownershipMapper.deleteById(existingOwnership.getId());
            log.warn("发现重复的资产所有权记录，已删除: assetId={}, oldCheckoutId={}, newCheckoutId={}", 
                    checkout.getAssetId(), existingOwnership.getCheckoutId(), checkout.getId());
        }
        
        // 创建新的资产所有权关系
        createAssetOwnership(checkout);
    }

} 