package com.kyx.service.erp.service.asset;

import cn.hutool.core.util.IdUtil;
import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.bpm.api.task.BpmProcessInstanceApi;
import com.kyx.service.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.kyx.service.business.api.dept.DeptApi;
import com.kyx.service.business.api.dept.dto.DeptRespDTO;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import com.kyx.service.erp.api.asset.vo.borrow.ErpAssetBorrowSaveReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.borrow.ErpAssetBorrowPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.borrow.ErpAssetBorrowRespVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetBorrowDO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetDO;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetBorrowMapper;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetMapper;
import com.kyx.service.erp.enums.asset.ErpAssetBorrowStatusEnum;
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
import java.util.stream.Collectors;

import static com.kyx.service.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 资产借用记录 Service 实现类
 *
 * @author kyx
 */
@Slf4j
@Service
@Validated
public class ErpAssetBorrowServiceImpl implements ErpAssetBorrowService {

    /**
     * 资产借用审批流程 KEY
     */
    public static final String PROCESS_KEY = "assets-borrow";

    @Resource
    private ErpAssetBorrowMapper borrowMapper;
    @Resource
    private ErpAssetMapper assetMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private BpmProcessInstanceApi bpmProcessInstanceApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBorrow(@Valid ErpAssetBorrowSaveReqVO createReqVO) {
        // 1. 校验资产是否存在
        ErpAssetDO asset = validateAssetExists(createReqVO.getAssetId());
        
        // 2. 校验资产是否可以借用
        if (!canBorrow(createReqVO.getAssetId())) {
            throw ServiceExceptionUtil.exception(ASSET_BORROW_NOT_AVAILABLE);
        }
        
        // 3. 校验用户和部门是否存在
        validateUserExists(createReqVO.getBorrowUserId());
        validateDeptExists(createReqVO.getBorrowDeptId());
        
        // 4. 生成借用编号
        String borrowNo = generateBorrowNo();
        
        // 5. 创建借用记录
        ErpAssetBorrowDO borrow = BeanUtils.toBean(createReqVO, ErpAssetBorrowDO.class);
        borrow.setBorrowNo(borrowNo);
        borrow.setStatus(ErpAssetBorrowStatusEnum.PENDING.getStatus()); // 申请中
        borrow.setApprovalStatus(1); // 待审批
        borrowMapper.insert(borrow);
        
        return borrow.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBorrowAndSubmit(Long userId, @Valid ErpAssetBorrowSaveReqVO createReqVO) {
        // 1. 校验资产是否存在
        ErpAssetDO asset = validateAssetExists(createReqVO.getAssetId());
        
        // 2. 校验资产是否可以借用
        if (!canBorrow(createReqVO.getAssetId())) {
            throw ServiceExceptionUtil.exception(ASSET_BORROW_NOT_AVAILABLE);
        }
        
        // 3. 校验用户和部门是否存在
        validateUserExists(createReqVO.getBorrowUserId());
        validateDeptExists(createReqVO.getBorrowDeptId());
        
        // 4. 生成借用编号
        String borrowNo = generateBorrowNo();
        
        // 5. 创建借用记录 - 设置为流程中状态
        ErpAssetBorrowDO borrow = BeanUtils.toBean(createReqVO, ErpAssetBorrowDO.class);
        borrow.setBorrowNo(borrowNo);
        borrow.setStatus(ErpAssetBorrowStatusEnum.PENDING.getStatus()); // 申请中
        borrow.setApprovalStatus(1); // 待审批
        borrow.setBmpStatus(1); // 流程中
        borrowMapper.insert(borrow);
        
        // 6. 发起 BPM 流程
        Map<String, Object> processInstanceVariables = new HashMap<>();
        processInstanceVariables.put("borrowId", borrow.getId());
        processInstanceVariables.put("borrowNo", borrowNo);
        processInstanceVariables.put("assetId", borrow.getAssetId());
        processInstanceVariables.put("assetNo", asset.getAssetNo());
        processInstanceVariables.put("assetName", asset.getName());
        processInstanceVariables.put("borrowUserId", borrow.getBorrowUserId());
        processInstanceVariables.put("borrowDeptId", borrow.getBorrowDeptId());
        processInstanceVariables.put("borrowReason", borrow.getBorrowReason());
        processInstanceVariables.put("borrowDate", borrow.getBorrowDate());
        processInstanceVariables.put("expectedReturnDate", borrow.getExpectedReturnDate());
        
        String processInstanceId = bpmProcessInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(PROCESS_KEY)
                        .setVariables(processInstanceVariables).setBusinessKey(String.valueOf(borrow.getId()))).getCheckedData();
        
        // 7. 更新流程实例ID
        ErpAssetBorrowDO updateObj = new ErpAssetBorrowDO();
        updateObj.setId(borrow.getId());
        updateObj.setBmpProcessInstanceId(processInstanceId);
        borrowMapper.updateById(updateObj);
        
        return borrow.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBorrow(@Valid ErpAssetBorrowSaveReqVO updateReqVO) {
        // 校验存在
        validateBorrowExists(updateReqVO.getId());
        
        // 校验资产是否存在
        validateAssetExists(updateReqVO.getAssetId());
        
        // 校验用户和部门是否存在
        validateUserExists(updateReqVO.getBorrowUserId());
        validateDeptExists(updateReqVO.getBorrowDeptId());
        
        // 更新
        ErpAssetBorrowDO updateObj = BeanUtils.toBean(updateReqVO, ErpAssetBorrowDO.class);
        borrowMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBorrow(Long id) {
        // 校验存在
        ErpAssetBorrowDO borrow = validateBorrowExists(id);
        
        // 只有申请中状态的记录才能删除
        if (!borrow.getStatus().equals(ErpAssetBorrowStatusEnum.PENDING.getStatus())) {
            throw ServiceExceptionUtil.exception(ASSET_BORROW_DELETE_FAIL_STATUS_ERROR);
        }
        
        // 删除
        borrowMapper.deleteById(id);
    }

    @Override
    public ErpAssetBorrowDO getBorrow(Long id) {
        return borrowMapper.selectById(id);
    }

    @Override
    public PageResult<ErpAssetBorrowRespVO> getBorrowPage(ErpAssetBorrowPageReqVO pageReqVO) {
        PageResult<ErpAssetBorrowDO> pageResult = borrowMapper.selectPage(pageReqVO);
        return buildBorrowVOPageResult(pageResult);
    }

    @Override
    public List<ErpAssetBorrowRespVO> getBorrowList(ErpAssetBorrowPageReqVO exportReqVO) {
        List<ErpAssetBorrowDO> list = borrowMapper.selectList(new com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX<ErpAssetBorrowDO>()
                .likeIfPresent(ErpAssetBorrowDO::getBorrowNo, exportReqVO.getBorrowNo())
                .eqIfPresent(ErpAssetBorrowDO::getAssetId, exportReqVO.getAssetId())
                .eqIfPresent(ErpAssetBorrowDO::getBorrowUserId, exportReqVO.getBorrowUserId())
                .eqIfPresent(ErpAssetBorrowDO::getBorrowDeptId, exportReqVO.getBorrowDeptId())
                .betweenIfPresent(ErpAssetBorrowDO::getBorrowDate, exportReqVO.getBorrowDate())
                .betweenIfPresent(ErpAssetBorrowDO::getExpectedReturnDate, exportReqVO.getExpectedReturnDate())
                .eqIfPresent(ErpAssetBorrowDO::getStatus, exportReqVO.getStatus())
                .eqIfPresent(ErpAssetBorrowDO::getApprovalStatus, exportReqVO.getApprovalStatus())
                .orderByDesc(ErpAssetBorrowDO::getId));
        return buildBorrowVOList(list);
    }

    @Override
    public boolean canBorrow(Long assetId) {
        // 1. 检查资产是否存在
        ErpAssetDO asset = assetMapper.selectById(assetId);
        if (asset == null) {
            return false;
        }
        
        // 2. 检查资产状态（只有正常和闲置状态的可以借用）
        if (!asset.getStatus().equals(1) && !asset.getStatus().equals(4)) {
            return false;
        }
        
        // 3. 检查是否已有申请中或借用中的记录
        ErpAssetBorrowDO pendingBorrow = borrowMapper.selectByAssetIdAndStatus(assetId, ErpAssetBorrowStatusEnum.PENDING.getStatus()); // 申请中
        ErpAssetBorrowDO activeBorrow = borrowMapper.selectByAssetIdAndStatus(assetId, ErpAssetBorrowStatusEnum.BORROWED.getStatus());  // 借用中
        if (pendingBorrow != null || activeBorrow != null) {
            return false;
        }
        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnAsset(Long id, Integer returnCondition, String returnRemark) {
        ErpAssetBorrowDO borrow = validateBorrowExists(id);
        
        // 只有借用中状态的记录才能归还
        if (!borrow.getStatus().equals(ErpAssetBorrowStatusEnum.BORROWED.getStatus())) {
            throw ServiceExceptionUtil.exception(ASSET_BORROW_RETURN_FAIL_STATUS_ERROR);
        }
        
        // 更新借用记录
        ErpAssetBorrowDO updateObj = new ErpAssetBorrowDO();
        updateObj.setId(id);
        updateObj.setStatus(ErpAssetBorrowStatusEnum.RETURNED.getStatus()); // 已归还
        updateObj.setActualReturnDate(LocalDate.now());
        updateObj.setReturnCondition(returnCondition);
        updateObj.setReturnRemark(returnRemark);
        borrowMapper.updateById(updateObj);
        
        log.info("资产归还成功: borrowId={}, assetId={}, returnCondition={}", 
                id, borrow.getAssetId(), returnCondition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBorrowBmpStatus(Long id, Integer approvalStatus, Integer bmpStatus) {
        ErpAssetBorrowDO borrow = validateBorrowExists(id);
        
        // 更新审批状态和BMP状态
        ErpAssetBorrowDO updateObj = new ErpAssetBorrowDO();
        updateObj.setId(id);
        updateObj.setApprovalStatus(approvalStatus);
        updateObj.setBmpStatus(bmpStatus);
        updateObj.setApprovalTime(LocalDateTime.now());
        
        // 根据审批结果更新借用状态
        if (approvalStatus.equals(2)) { // 审批通过
            // 审批通过后，设置为"借用中"状态
            updateObj.setStatus(ErpAssetBorrowStatusEnum.BORROWED.getStatus());
            borrowMapper.updateById(updateObj);
            
            log.info("资产借用审批通过: borrowId={}, assetId={}, userId={}", 
                    id, borrow.getAssetId(), borrow.getBorrowUserId());
        } else if (approvalStatus.equals(3)) { // 审批拒绝
            // 审批拒绝后，设置为"申请拒绝"状态
            updateObj.setStatus(ErpAssetBorrowStatusEnum.REJECTED.getStatus());
            borrowMapper.updateById(updateObj);
            
            log.info("资产借用审批拒绝: borrowId={}, assetId={}", id, borrow.getAssetId());
        } else {
            borrowMapper.updateById(updateObj);
        }
    }

    @Override
    public List<ErpAssetBorrowDO> getOverdueBorrows() {
        return borrowMapper.selectOverdueReturns();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOverdueStatus(Long id) {
        ErpAssetBorrowDO updateObj = new ErpAssetBorrowDO();
        updateObj.setId(id);
        updateObj.setStatus(ErpAssetBorrowStatusEnum.OVERDUE.getStatus()); // 逾期未还
        borrowMapper.updateById(updateObj);
    }

    // 私有方法

    private String generateBorrowNo() {
        // 生成借用编号，格式：BOR + 年月日 + 6位随机数
        return "BOR" + java.time.LocalDate.now().toString().replace("-", "") + IdUtil.fastSimpleUUID().substring(0, 6);
    }

    private ErpAssetDO validateAssetExists(Long assetId) {
        ErpAssetDO asset = assetMapper.selectById(assetId);
        if (asset == null) {
            throw ServiceExceptionUtil.exception(ASSET_NOT_EXISTS);
        }
        return asset;
    }

    private ErpAssetBorrowDO validateBorrowExists(Long id) {
        ErpAssetBorrowDO borrow = borrowMapper.selectById(id);
        if (borrow == null) {
            throw ServiceExceptionUtil.exception(ASSET_BORROW_NOT_EXISTS);
        }
        return borrow;
    }

    private void validateUserExists(Long userId) {
        AdminUserRespDTO user = adminUserApi.getUser(userId).getCheckedData();
        if (user == null) {
            throw ServiceExceptionUtil.exception(USER_NOT_EXISTS);
        }
    }

    private void validateDeptExists(Long deptId) {
        DeptRespDTO dept = deptApi.getDept(deptId).getCheckedData();
        if (dept == null) {
            throw ServiceExceptionUtil.exception(DEPT_NOT_EXISTS);
        }
    }

    private PageResult<ErpAssetBorrowRespVO> buildBorrowVOPageResult(PageResult<ErpAssetBorrowDO> pageResult) {
        List<ErpAssetBorrowRespVO> list = buildBorrowVOList(pageResult.getList());
        return new PageResult<>(list, pageResult.getTotal());
    }

    private List<ErpAssetBorrowRespVO> buildBorrowVOList(List<ErpAssetBorrowDO> list) {
        if (list.isEmpty()) {
            return list.stream().map(item -> BeanUtils.toBean(item, ErpAssetBorrowRespVO.class))
                    .collect(Collectors.toList());
        }

        // 获取资产信息
        Map<Long, ErpAssetDO> assetMap = assetMapper.selectBatchIds(
                list.stream().map(ErpAssetBorrowDO::getAssetId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(ErpAssetDO::getId, item -> item));

        // 获取用户信息
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(
                list.stream().map(ErpAssetBorrowDO::getBorrowUserId).collect(Collectors.toSet()));

        // 获取部门信息
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(
                list.stream().map(ErpAssetBorrowDO::getBorrowDeptId).collect(Collectors.toSet()));

        return list.stream().map(item -> {
            ErpAssetBorrowRespVO respVO = BeanUtils.toBean(item, ErpAssetBorrowRespVO.class);
            
            // 设置资产信息
            ErpAssetDO asset = assetMap.get(item.getAssetId());
            if (asset != null) {
                respVO.setAssetNo(asset.getAssetNo());
                respVO.setAssetName(asset.getName());
            }
            
            // 设置借用人信息
            AdminUserRespDTO user = userMap.get(item.getBorrowUserId());
            if (user != null) {
                respVO.setBorrowUserName(user.getNickname());
            }
            
            // 设置部门信息
            DeptRespDTO dept = deptMap.get(item.getBorrowDeptId());
            if (dept != null) {
                respVO.setBorrowDeptName(dept.getName());
            }
            
            // 设置审批人信息
            if (item.getApproverUserId() != null) {
                AdminUserRespDTO approver = userMap.get(item.getApproverUserId());
                if (approver != null) {
                    respVO.setApproverUserName(approver.getNickname());
                }
            }
            
            return respVO;
        }).collect(Collectors.toList());
    }

    @Override
    public ErpAssetBorrowDO getBorrowByBmpProcessInstanceId(String bmpProcessInstanceId) {
        return borrowMapper.selectByBmpProcessInstanceId(bmpProcessInstanceId);
    }
} 