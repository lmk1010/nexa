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
import com.kyx.service.erp.api.asset.vo.lost.ErpAssetLostSaveReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.lost.ErpAssetLostPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.lost.ErpAssetLostRespVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetDO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetLostDO;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetMapper;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetLostMapper;
import com.kyx.service.erp.enums.asset.ErpAssetLostBmpStatusEnum;
import com.kyx.service.erp.enums.asset.ErpAssetLostStatusEnum;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetLostFileDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.kyx.service.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 资产挂失 Service 实现类
 *
 * @author kyx
 */
@Slf4j
@Service
@Validated
public class ErpAssetLostServiceImpl implements ErpAssetLostService {

    /**
     * 资产挂失审批流程 KEY
     */
    public static final String PROCESS_KEY = "assets-lost";

    @Resource
    private ErpAssetLostMapper lostMapper;
    @Resource
    private ErpAssetMapper assetMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private BpmProcessInstanceApi bmpProcessInstanceApi;
    @Resource
    private ErpAssetLostFileService lostFileService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createLost(@Valid ErpAssetLostSaveReqVO createReqVO) {
        // 1. 校验资产是否存在
        ErpAssetDO asset = validateAssetExists(createReqVO.getAssetId());
        
        // 2. 校验资产状态是否允许挂失
        validateAssetCanBeLost(asset);
        
        // 3. 校验用户和部门是否存在
        validateUserExists(createReqVO.getHandleUserId());
        validateDeptExists(createReqVO.getHandleDeptId());
        
        // 4. 生成挂失编号
        String lostNo = generateLostNo();
        
        // 5. 创建挂失记录
        ErpAssetLostDO lost = BeanUtils.toBean(createReqVO, ErpAssetLostDO.class);
        lost.setLostNo(lostNo);
        lost.setStatus(ErpAssetLostStatusEnum.PENDING.getStatus()); // 申请中
        lost.setApprovalStatus(1); // 待审批
        lostMapper.insert(lost);
        
        // 6. 保存文件关联
        if (createReqVO.getFileIds() != null && !createReqVO.getFileIds().isEmpty()) {
            lostFileService.saveLostFiles(lost.getId(), createReqVO.getFileIds());
        }
        
        return lost.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createLostAndSubmit(Long userId, @Valid ErpAssetLostSaveReqVO createReqVO) {
        // 1. 创建挂失记录
        Long lostId = createLost(createReqVO);
        
        // 2. 发起BPM流程
        ErpAssetLostDO lost = lostMapper.selectById(lostId);
        ErpAssetDO asset = assetMapper.selectById(lost.getAssetId());
        
        Map<String, Object> processVariables = new HashMap<>();
        processVariables.put("lostId", lostId);
        processVariables.put("assetNo", asset.getAssetNo());
        processVariables.put("assetName", asset.getName());
        processVariables.put("lostReason", lost.getLostReason());
        processVariables.put("estimatedValue", lost.getEstimatedValue());
        processVariables.put("handleUserId", lost.getHandleUserId());
        processVariables.put("handleDeptId", lost.getHandleDeptId());
        
        BpmProcessInstanceCreateReqDTO processReqDTO = new BpmProcessInstanceCreateReqDTO();
        processReqDTO.setProcessDefinitionKey(PROCESS_KEY);
        processReqDTO.setBusinessKey(String.valueOf(lostId));
        processReqDTO.setVariables(processVariables);
        
        String processInstanceId = bmpProcessInstanceApi.createProcessInstance(userId, processReqDTO).getData();
        
        // 3. 更新挂失记录状态
        lost.setStatus(ErpAssetLostStatusEnum.APPROVING.getStatus()); // 审批中
        lost.setBmpStatus(ErpAssetLostBmpStatusEnum.PROCESSING.getStatus()); // 流程中
        lost.setBmpProcessInstanceId(processInstanceId);
        lostMapper.updateById(lost);
        
        log.info("资产挂失申请已提交，挂失ID: {}, 流程实例ID: {}", lostId, processInstanceId);
        return lostId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLost(@Valid ErpAssetLostSaveReqVO updateReqVO) {
        // 1. 校验挂失记录是否存在
        ErpAssetLostDO existingLost = validateLostExists(updateReqVO.getId());
        
        // 2. 校验是否可以更新
        if (!ErpAssetLostStatusEnum.PENDING.getStatus().equals(existingLost.getStatus())) {
            throw ServiceExceptionUtil.exception(ASSET_LOST_NOT_PENDING);
        }
        
        // 3. 更新挂失记录
        ErpAssetLostDO updateLost = BeanUtils.toBean(updateReqVO, ErpAssetLostDO.class);
        lostMapper.updateById(updateLost);
        
        // 4. 更新文件关联
        lostFileService.deleteLostFiles(updateReqVO.getId());
        if (updateReqVO.getFileIds() != null && !updateReqVO.getFileIds().isEmpty()) {
            lostFileService.saveLostFiles(updateReqVO.getId(), updateReqVO.getFileIds());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLost(Long id) {
        // 1. 校验挂失记录是否存在
        ErpAssetLostDO lost = validateLostExists(id);
        
        // 2. 校验是否可以删除
        if (!ErpAssetLostStatusEnum.PENDING.getStatus().equals(lost.getStatus())) {
            throw ServiceExceptionUtil.exception(ASSET_LOST_NOT_PENDING);
        }
        
        // 3. 删除挂失记录和文件关联
        lostMapper.deleteById(id);
        lostFileService.deleteLostFiles(id);
    }

    @Override
    public ErpAssetLostDO getLost(Long id) {
        return lostMapper.selectById(id);
    }

    @Override
    public ErpAssetLostRespVO getLostDetail(Long id) {
        ErpAssetLostDO lost = lostMapper.selectById(id);
        if (lost == null) {
            return null;
        }
        
        // 使用现有的转换逻辑填充关联数据
        List<ErpAssetLostDO> singleList = java.util.Collections.singletonList(lost);
        List<ErpAssetLostRespVO> list = convertList(singleList);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public PageResult<ErpAssetLostRespVO> getLostPage(ErpAssetLostPageReqVO pageReqVO) {
        PageResult<ErpAssetLostDO> pageResult = lostMapper.selectPage(pageReqVO);
        return convertPage(pageResult);
    }

    @Override
    public List<ErpAssetLostRespVO> getLostList(ErpAssetLostPageReqVO exportReqVO) {
        List<ErpAssetLostDO> list = lostMapper.selectPage(exportReqVO).getList();
        return convertList(list);
    }

    @Override
    public ErpAssetLostDO getLostByBmpProcessInstanceId(String bmpProcessInstanceId) {
        return lostMapper.selectByBmpProcessInstanceId(bmpProcessInstanceId);
    }

    @Override
    public ErpAssetLostRespVO getLostDetailByBmpProcessInstanceId(String bmpProcessInstanceId) {
        ErpAssetLostDO lost = lostMapper.selectByBmpProcessInstanceId(bmpProcessInstanceId);
        if (lost == null) {
            return null;
        }
        
        // 使用现有的转换逻辑填充关联数据
        List<ErpAssetLostDO> singleList = java.util.Collections.singletonList(lost);
        List<ErpAssetLostRespVO> list = convertList(singleList);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleBmpStatusChange(String bmpProcessInstanceId, Integer bmpStatus) {
        ErpAssetLostDO lost = lostMapper.selectByBmpProcessInstanceId(bmpProcessInstanceId);
        if (lost == null) {
            log.warn("未找到对应的挂失记录，流程实例ID: {}", bmpProcessInstanceId);
            return;
        }

        lost.setBmpStatus(bmpStatus);
        
        // 根据BMP状态更新挂失状态
        if (ErpAssetLostBmpStatusEnum.COMPLETED.getStatus().equals(bmpStatus)) {
            lost.setStatus(ErpAssetLostStatusEnum.APPROVED.getStatus()); // 审批通过
            lost.setApprovalStatus(2); // 审批通过
            lost.setApprovalTime(LocalDateTime.now());
        } else if (ErpAssetLostBmpStatusEnum.CANCELLED.getStatus().equals(bmpStatus)) {
            lost.setStatus(ErpAssetLostStatusEnum.REJECTED.getStatus()); // 审批拒绝
            lost.setApprovalStatus(3); // 审批拒绝
            lost.setApprovalTime(LocalDateTime.now());
        }
        
        lostMapper.updateById(lost);
        log.info("处理挂失BMP状态变更完成，挂失ID: {}, BMP状态: {}", lost.getId(), bmpStatus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleFoundAsset(Long id, ErpAssetLostSaveReqVO saveReqVO) {
        ErpAssetLostDO lost = validateLostExists(id);
        
        // 只有审批通过的挂失才能处理找回
        if (!ErpAssetLostStatusEnum.APPROVED.getStatus().equals(lost.getStatus())) {
            throw ServiceExceptionUtil.exception(ASSET_LOST_NOT_APPROVED);
        }
        
        lost.setStatus(ErpAssetLostStatusEnum.FOUND.getStatus()); // 已找回
        lost.setFoundTime(saveReqVO.getFoundTime());
        lost.setFindLocation(saveReqVO.getFindLocation());
        lost.setFinderUserId(saveReqVO.getFinderUserId());
        lost.setFindDescription(saveReqVO.getFindDescription());
        
        lostMapper.updateById(lost);
        log.info("资产找回处理完成，挂失ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmLostAsset(Long id, String remark) {
        ErpAssetLostDO lost = validateLostExists(id);
        
        // 只有审批通过的挂失才能确认丢失
        if (!ErpAssetLostStatusEnum.APPROVED.getStatus().equals(lost.getStatus())) {
            throw ServiceExceptionUtil.exception(ASSET_LOST_NOT_APPROVED);
        }
        
        lost.setStatus(ErpAssetLostStatusEnum.CONFIRMED_LOST.getStatus()); // 确认丢失
        lost.setRemark(remark);
        
        lostMapper.updateById(lost);
        log.info("资产确认丢失处理完成，挂失ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLostBmpStatus(Long lostId, Integer approvalStatus, Integer bmpStatus) {
        // 1. 校验挂失记录是否存在
        ErpAssetLostDO lost = validateLostExists(lostId);
        
        // 2. 更新挂失记录状态
        ErpAssetLostDO updateLost = new ErpAssetLostDO();
        updateLost.setId(lostId);
        updateLost.setApprovalStatus(approvalStatus);
        updateLost.setBmpStatus(bmpStatus);
        
        // 根据审批状态更新挂失状态
        if (approvalStatus != null) {
            switch (approvalStatus) {
                case 2: // 审批通过
                    updateLost.setStatus(ErpAssetLostStatusEnum.APPROVED.getStatus());
                    updateLost.setApprovalTime(LocalDateTime.now());
                    break;
                case 3: // 审批拒绝
                    updateLost.setStatus(ErpAssetLostStatusEnum.REJECTED.getStatus());
                    updateLost.setApprovalTime(LocalDateTime.now());
                    break;
                default:
                    // 其他状态保持不变
                    break;
            }
        }
        
        lostMapper.updateById(updateLost);
        
        log.info("成功更新挂失记录BMP状态: lostId={}, approvalStatus={}, bmpStatus={}", 
                lostId, approvalStatus, bmpStatus);
    }

    // ========== 私有方法 ==========

    private ErpAssetDO validateAssetExists(Long assetId) {
        ErpAssetDO asset = assetMapper.selectById(assetId);
        if (asset == null) {
            throw ServiceExceptionUtil.exception(ASSET_NOT_EXISTS);
        }
        return asset;
    }

    private void validateAssetCanBeLost(ErpAssetDO asset) {
        // 校验资产状态是否允许挂失
        // 正常、维修中、闲置状态的资产可以挂失
        if (asset.getStatus() == 3) { // 已报废的资产不能挂失
            throw ServiceExceptionUtil.exception(ASSET_ALREADY_SCRAPPED);
        }
    }

    private void validateUserExists(Long userId) {
        AdminUserRespDTO user = adminUserApi.getUser(userId).getData();
        if (user == null) {
            throw ServiceExceptionUtil.exception(USER_NOT_EXISTS);
        }
    }

    private void validateDeptExists(Long deptId) {
        DeptRespDTO dept = deptApi.getDept(deptId).getData();
        if (dept == null) {
            throw ServiceExceptionUtil.exception(DEPT_NOT_EXISTS);
        }
    }

    private ErpAssetLostDO validateLostExists(Long id) {
        ErpAssetLostDO lost = lostMapper.selectById(id);
        if (lost == null) {
            throw ServiceExceptionUtil.exception(ASSET_LOST_NOT_EXISTS);
        }
        return lost;
    }

    private String generateLostNo() {
        // 生成挂失编号：LOST + YYYYMMDD + UUID前8位
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = UUID.randomUUID().toString(true).substring(0, 8).toUpperCase();
        return "LOST" + dateStr + uuid;
    }

    private PageResult<ErpAssetLostRespVO> convertPage(PageResult<ErpAssetLostDO> pageResult) {
        PageResult<ErpAssetLostRespVO> result = new PageResult<>();
        result.setList(convertList(pageResult.getList()));
        result.setTotal(pageResult.getTotal());
        return result;
    }

    private List<ErpAssetLostRespVO> convertList(List<ErpAssetLostDO> list) {
        return BeanUtils.toBean(list, ErpAssetLostRespVO.class, lost -> {
            // 填充资产信息
            if (lost.getAssetId() != null) {
                ErpAssetDO asset = assetMapper.selectById(lost.getAssetId());
                if (asset != null) {
                    lost.setAssetNo(asset.getAssetNo());
                    lost.setAssetName(asset.getName());
                    lost.setAssetType(asset.getType());
                    lost.setAssetSpecification(asset.getSpecification());
                    lost.setAssetBrand(asset.getBrand());
                    lost.setAssetModel(asset.getModel());
                    lost.setAssetLocation(asset.getLocation());
                }
            }

            // 填充用户信息
            if (lost.getHandleUserId() != null) {
                AdminUserRespDTO user = adminUserApi.getUser(lost.getHandleUserId()).getData();
                if (user != null) {
                    lost.setHandleUserName(user.getNickname());
                }
            }
            
            if (lost.getApproverUserId() != null) {
                AdminUserRespDTO approver = adminUserApi.getUser(lost.getApproverUserId()).getData();
                if (approver != null) {
                    lost.setApproverUserName(approver.getNickname());
                }
            }
            
            if (lost.getFinderUserId() != null) {
                AdminUserRespDTO finder = adminUserApi.getUser(lost.getFinderUserId()).getData();
                if (finder != null) {
                    lost.setFinderUserName(finder.getNickname());
                }
            }

            // 填充部门信息
            if (lost.getHandleDeptId() != null) {
                DeptRespDTO dept = deptApi.getDept(lost.getHandleDeptId()).getData();
                if (dept != null) {
                    lost.setHandleDeptName(dept.getName());
                }
            }

            // 填充状态名称
            for (ErpAssetLostStatusEnum statusEnum : ErpAssetLostStatusEnum.values()) {
                if (statusEnum.getStatus().equals(lost.getStatus())) {
                    lost.setStatusName(statusEnum.getName());
                    break;
                }
            }

            // 填充文件信息
            List<ErpAssetLostFileDO> files = lostFileService.getLostFiles(lost.getId());
            lost.setAttachments(BeanUtils.toBean(files, ErpAssetLostRespVO.FileInfo.class));
        });
    }
} 