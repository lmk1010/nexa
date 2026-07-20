package com.kyx.service.erp.service.asset;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.erp.controller.admin.asset.vo.asset.ErpAssetSaveReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.assetinput.ErpAssetInputPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.assetinput.ErpAssetInputRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.assetinput.ErpAssetInputSaveReqVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetInputDO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetCategoryDO;
import com.kyx.service.erp.dal.dataobject.purchase.ErpSupplierDO;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetInputMapper;
import com.kyx.service.erp.enums.asset.ErpAssetInputStatusEnum;
import com.kyx.service.erp.enums.asset.ErpAssetInputBmpStatusEnum;
import com.kyx.service.erp.service.purchase.ErpSupplierService;
import com.kyx.service.business.api.dept.DeptApi;
import com.kyx.service.business.api.dept.dto.DeptRespDTO;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import com.kyx.service.bpm.api.task.BpmProcessInstanceApi;
import com.kyx.service.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
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
 * ERP 资产录入申请 Service 实现类
 *
 * @author kyx
 */
@Slf4j
@Service
@Validated
public class ErpAssetInputServiceImpl implements ErpAssetInputService {

    /**
     * 资产录入审批流程 KEY
     */
    public static final String ASSET_INPUT_PROCESS_KEY = "assets-input";

    @Resource
    private ErpAssetInputMapper assetInputMapper;
    @Resource
    private ErpAssetService assetService;
    @Resource
    private ErpAssetCategoryService assetCategoryService;
    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private BpmProcessInstanceApi bpmProcessInstanceApi;

    @Override
    @Transactional
    public Long createAssetInput(ErpAssetInputSaveReqVO createReqVO) {
        // 校验资产编码是否重复
        validateAssetNoUnique(null, createReqVO.getAssetNo());
        // 校验分类
        if (createReqVO.getCategoryId() != null) {
            assetCategoryService.validateAssetCategory(createReqVO.getCategoryId());
        }
        
        // 生成录入申请编号
        String inputNo = generateInputNo();
        
        // 创建申请记录
        ErpAssetInputDO assetInput = BeanUtils.toBean(createReqVO, ErpAssetInputDO.class);
        assetInput.setInputNo(inputNo);
        assetInput.setStatus(ErpAssetInputStatusEnum.PENDING.getStatus());
        assetInput.setApprovalStatus(ErpAssetInputStatusEnum.PENDING.getStatus());
        assetInputMapper.insert(assetInput);
        
        return assetInput.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAssetInputAndSubmit(Long userId, ErpAssetInputSaveReqVO createReqVO) {
        // 1. 创建申请记录
        Long assetInputId = createAssetInput(createReqVO);
        ErpAssetInputDO assetInput = assetInputMapper.selectById(assetInputId);
        
        // 2. 发起 BPM 流程
        Map<String, Object> processInstanceVariables = new HashMap<>();
        processInstanceVariables.put("assetInputId", assetInput.getId());
        processInstanceVariables.put("inputNo", assetInput.getInputNo());
        processInstanceVariables.put("assetNo", assetInput.getAssetNo());
        processInstanceVariables.put("assetName", assetInput.getName());
        processInstanceVariables.put("purchasePrice", assetInput.getPurchasePrice()); // 关键字段
        processInstanceVariables.put("categoryId", assetInput.getCategoryId());
        processInstanceVariables.put("supplierId", assetInput.getSupplierId());
        processInstanceVariables.put("deptId", assetInput.getDeptId());
        
        String processInstanceId = bpmProcessInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(ASSET_INPUT_PROCESS_KEY)
                        .setVariables(processInstanceVariables).setBusinessKey(String.valueOf(assetInput.getId()))).getCheckedData();
        
        // 3. 更新流程实例ID和状态
        ErpAssetInputDO updateObj = new ErpAssetInputDO();
        updateObj.setId(assetInputId);
        updateObj.setBmpProcessInstanceId(processInstanceId);
        updateObj.setStatus(ErpAssetInputStatusEnum.APPROVING.getStatus());
        updateObj.setApprovalStatus(ErpAssetInputStatusEnum.APPROVING.getStatus());
        updateObj.setBmpStatus(ErpAssetInputBmpStatusEnum.IN_PROGRESS.getStatus());
        assetInputMapper.updateById(updateObj);
        
        log.info("资产录入审批流程已启动，申请ID: {}, 流程实例ID: {}", assetInputId, processInstanceId);
        
        return assetInputId;
    }

    @Override
    @Transactional
    public void updateAssetInput(ErpAssetInputSaveReqVO updateReqVO) {
        // 校验存在
        validateAssetInputExists(updateReqVO.getId());
        // 校验资产编码是否重复
        validateAssetNoUnique(updateReqVO.getId(), updateReqVO.getAssetNo());
        // 校验分类
        if (updateReqVO.getCategoryId() != null) {
            assetCategoryService.validateAssetCategory(updateReqVO.getCategoryId());
        }
        
        // 更新
        ErpAssetInputDO updateObj = BeanUtils.toBean(updateReqVO, ErpAssetInputDO.class);
        assetInputMapper.updateById(updateObj);
    }

    @Override
    @Transactional
    public void deleteAssetInput(Long id) {
        // 校验存在
        ErpAssetInputDO assetInput = validateAssetInputExists(id);
        
        // 只有待审批状态的记录才能删除
        if (!assetInput.getStatus().equals(ErpAssetInputStatusEnum.PENDING.getStatus())) {
            throw exception(ASSET_INPUT_DELETE_FAIL_STATUS_ERROR);
        }
        
        // 删除
        assetInputMapper.deleteById(id);
    }

    @Override
    public ErpAssetInputDO getAssetInput(Long id) {
        return assetInputMapper.selectById(id);
    }

    @Override
    public PageResult<ErpAssetInputRespVO> getAssetInputVOPage(ErpAssetInputPageReqVO pageReqVO) {
        PageResult<ErpAssetInputDO> pageResult = assetInputMapper.selectPage(pageReqVO);
        
        // 1.1 获取分类列表
        Map<Long, ErpAssetCategoryDO> categoryMap = assetCategoryService.getAssetCategoryMap(
                pageResult.getList().stream().map(ErpAssetInputDO::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet()));
        
        // 1.2 获取供应商列表
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(
                pageResult.getList().stream().map(ErpAssetInputDO::getSupplierId).filter(Objects::nonNull).collect(Collectors.toSet()));
        
        // 1.3 获取部门列表
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(
                pageResult.getList().stream().map(ErpAssetInputDO::getDeptId).filter(Objects::nonNull).collect(Collectors.toSet()));
        
        // 1.4 获取用户列表（审批人）
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(
                pageResult.getList().stream().map(ErpAssetInputDO::getApproverUserId).filter(Objects::nonNull).collect(Collectors.toSet()));
        
        // 2. 拼接数据
        List<ErpAssetInputRespVO> resultList = pageResult.getList().stream().map(assetInput -> {
            ErpAssetInputRespVO respVO = BeanUtils.toBean(assetInput, ErpAssetInputRespVO.class);
            
            // 设置分类名称
            if (assetInput.getCategoryId() != null && categoryMap.containsKey(assetInput.getCategoryId())) {
                respVO.setCategoryName(categoryMap.get(assetInput.getCategoryId()).getName());
            }
            
            // 设置供应商名称
            if (assetInput.getSupplierId() != null && supplierMap.containsKey(assetInput.getSupplierId())) {
                respVO.setSupplierName(supplierMap.get(assetInput.getSupplierId()).getName());
            }
            
            // 设置部门名称
            if (assetInput.getDeptId() != null && deptMap.containsKey(assetInput.getDeptId())) {
                respVO.setDeptName(deptMap.get(assetInput.getDeptId()).getName());
            }
            
            // 设置状态名称
            respVO.setStatusName(getStatusName(assetInput.getStatus()));
            respVO.setApprovalStatusName(getStatusName(assetInput.getApprovalStatus()));
            if (assetInput.getBmpStatus() != null) {
                respVO.setBmpStatusName(getBmpStatusName(assetInput.getBmpStatus()));
            }
            
            return respVO;
        }).collect(Collectors.toList());
        
        return new PageResult<>(resultList, pageResult.getTotal());
    }

    @Override
    public ErpAssetInputDO getAssetInputByBmpProcessInstanceId(String bmpProcessInstanceId) {
        return assetInputMapper.selectByBmpProcessInstanceId(bmpProcessInstanceId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long approveAssetInput(Long assetInputId, Long approverUserId, String approverUserName, String approvalRemark) {
        // 1. 获取申请记录
        ErpAssetInputDO assetInput = validateAssetInputExists(assetInputId);
        
        // 2. 创建正式资产记录
        ErpAssetSaveReqVO assetSaveReqVO = BeanUtils.toBean(assetInput, ErpAssetSaveReqVO.class);
        assetSaveReqVO.setId(null); // 清空ID，创建新记录
        Long assetId = assetService.createAsset(assetSaveReqVO);
        
        // 3. 更新申请记录状态
        ErpAssetInputDO updateObj = new ErpAssetInputDO();
        updateObj.setId(assetInputId);
        updateObj.setStatus(ErpAssetInputStatusEnum.APPROVED.getStatus());
        updateObj.setApprovalStatus(ErpAssetInputStatusEnum.APPROVED.getStatus());
        updateObj.setBmpStatus(ErpAssetInputBmpStatusEnum.COMPLETED.getStatus());
        updateObj.setAssetId(assetId);
        updateObj.setApproverUserId(approverUserId);
        updateObj.setApproverUserName(approverUserName);
        updateObj.setApprovalTime(LocalDateTime.now());
        updateObj.setApprovalRemark(approvalRemark);
        assetInputMapper.updateById(updateObj);
        
        log.info("资产录入申请审批通过，申请ID: {}, 创建资产ID: {}, 审批人: {}", assetInputId, assetId, approverUserName);
        
        return assetId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectAssetInput(Long assetInputId, Long approverUserId, String approverUserName, String rejectReason) {
        // 1. 校验申请记录存在
        validateAssetInputExists(assetInputId);
        
        // 2. 更新申请记录状态
        ErpAssetInputDO updateObj = new ErpAssetInputDO();
        updateObj.setId(assetInputId);
        updateObj.setStatus(ErpAssetInputStatusEnum.REJECTED.getStatus());
        updateObj.setApprovalStatus(ErpAssetInputStatusEnum.REJECTED.getStatus());
        updateObj.setBmpStatus(ErpAssetInputBmpStatusEnum.COMPLETED.getStatus());
        updateObj.setApproverUserId(approverUserId);
        updateObj.setApproverUserName(approverUserName);
        updateObj.setApprovalTime(LocalDateTime.now());
        updateObj.setRejectReason(rejectReason);
        assetInputMapper.updateById(updateObj);
        
        log.info("资产录入申请被拒绝，申请ID: {}, 审批人: {}, 拒绝原因: {}", assetInputId, approverUserName, rejectReason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBmpStatus(Long assetInputId, Integer bmpStatus) {
        // 1. 校验申请记录存在
        validateAssetInputExists(assetInputId);
        
        // 2. 更新BMP流程状态
        ErpAssetInputDO updateObj = new ErpAssetInputDO();
        updateObj.setId(assetInputId);
        updateObj.setBmpStatus(bmpStatus);
        assetInputMapper.updateById(updateObj);
        
        log.info("更新资产录入申请BMP状态: assetInputId={}, bmpStatus={}", assetInputId, bmpStatus);
    }

    // ========== 私有方法 ==========

    private ErpAssetInputDO validateAssetInputExists(Long id) {
        ErpAssetInputDO assetInput = assetInputMapper.selectById(id);
        if (assetInput == null) {
            throw exception(ASSET_INPUT_NOT_EXISTS);
        }
        return assetInput;
    }

    private void validateAssetNoUnique(Long id, String assetNo) {
        ErpAssetInputDO assetInput = assetInputMapper.selectByAssetNo(assetNo);
        if (assetInput != null && !assetInput.getId().equals(id)) {
            throw exception(ASSET_INPUT_ASSET_NO_DUPLICATE);
        }
        
        // 还需要检查正式资产表中是否有重复
        // 这里可以调用 assetService 的校验方法
        // 但需要注意异常处理，因为异常信息可能不同
    }

    private String generateInputNo() {
        // 生成格式：AI + 年月日 + 6位随机数
        // 例如：AI20250131123456
        String dateStr = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = String.format("%06d", (int) (Math.random() * 1000000));
        return "AI" + dateStr + randomStr;
    }

    private String getStatusName(Integer status) {
        if (status == null) return null;
        for (ErpAssetInputStatusEnum statusEnum : ErpAssetInputStatusEnum.values()) {
            if (statusEnum.getStatus().equals(status)) {
                return statusEnum.getName();
            }
        }
        return null;
    }

    private String getBmpStatusName(Integer bmpStatus) {
        if (bmpStatus == null) return null;
        for (ErpAssetInputBmpStatusEnum statusEnum : ErpAssetInputBmpStatusEnum.values()) {
            if (statusEnum.getStatus().equals(bmpStatus)) {
                return statusEnum.getName();
            }
        }
        return null;
    }

} 