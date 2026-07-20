package com.kyx.service.erp.service.asset;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.common.util.json.JsonUtils;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.erp.controller.admin.asset.vo.inventory.ErpInventoryPlanPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.inventory.ErpInventoryPlanSaveReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.inventory.ErpAssetInventoryReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.inventory.ErpAssetInventoryRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.inventory.ErpValidateScanReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.inventory.ErpValidateScanRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.inventory.ErpConfirmInventoryReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.inventory.ErpInventoryPlanProgressRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.inventory.ErpInventoryRecordPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.inventory.ErpInventoryRecordRespVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetDO;
import com.kyx.service.erp.dal.dataobject.asset.ErpInventoryPlanDO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetInventoryRecordDO;
import com.kyx.service.erp.dal.dataobject.asset.ErpInventoryPlanChangeLogDO;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetMapper;
import com.kyx.service.erp.dal.mysql.asset.ErpInventoryPlanMapper;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetInventoryRecordMapper;
import com.kyx.service.erp.dal.mysql.asset.ErpInventoryPlanChangeLogMapper;
import com.kyx.service.erp.enums.ErrorCodeConstants;
import com.kyx.service.bpm.api.task.BpmProcessInstanceApi;
import com.kyx.service.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.foundation.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * ERP 盘点计划 Service 实现类
 *
 * @author kyx
 */
@Service
@Slf4j
public class ErpInventoryPlanServiceImpl implements ErpInventoryPlanService {

    /**
     * 盘点计划提交审核对应的流程定义KEY
     */
    public static final String PROCESS_KEY = "assets-plan-submit";

    @Resource
    private ErpInventoryPlanMapper inventoryPlanMapper;

    @Resource
    private ErpAssetMapper assetMapper;

    @Resource
    private ErpInventoryPlanChangeLogMapper inventoryPlanChangeLogMapper;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private ErpAssetInventoryRecordMapper assetInventoryRecordMapper;

    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createInventoryPlan(@Valid ErpInventoryPlanSaveReqVO createReqVO) {
        // 校验计划名称唯一性
        validatePlanNameUnique(null, createReqVO.getPlanName());

        // 校验时间范围
        validateTimeRange(createReqVO.getStartTime(), createReqVO.getEndTime());

        // 获取负责人信息
        AdminUserRespDTO responsibleUser = adminUserApi.getUser(createReqVO.getResponsiblePersonId()).getData();
        if (responsibleUser == null) {
            throw exception(ErrorCodeConstants.USER_NOT_EXISTS);
        }

        // 创建盘点计划
        ErpInventoryPlanDO inventoryPlan = BeanUtils.toBean(createReqVO, ErpInventoryPlanDO.class);
        
        // 转换日期字段（Date -> LocalDateTime）
        inventoryPlan.setStartTime(convertToLocalDateTime(createReqVO.getStartTime()));
        inventoryPlan.setEndTime(convertToLocalDateTime(createReqVO.getEndTime()));
        
        // 生成计划编号
        inventoryPlan.setPlanNo(generatePlanNo());
        
        // 设置状态为草稿
        inventoryPlan.setStatus(0);
        
        // 设置负责人信息
        inventoryPlan.setResponsiblePersonName(responsibleUser.getNickname());
        
        // 处理JSON字段
        inventoryPlan.setDepartmentIds(JsonUtils.toJsonString(createReqVO.getDepartmentIds()));
        inventoryPlan.setUserIds(JsonUtils.toJsonString(createReqVO.getUserIds()));
        inventoryPlan.setLocationIds(JsonUtils.toJsonString(createReqVO.getLocationIds()));
        
        // 处理人员信息
        fillUserInfo(inventoryPlan, createReqVO.getScannerIds(), createReqVO.getReviewerIds());

        // 插入数据库
        inventoryPlanMapper.insert(inventoryPlan);
        return inventoryPlan.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateInventoryPlan(@Valid ErpInventoryPlanSaveReqVO updateReqVO) {
        // 校验盘点计划存在
        ErpInventoryPlanDO existingPlan = validateInventoryPlanExists(updateReqVO.getId());
        
        // 只有草稿状态才能修改
        if (!existingPlan.getStatus().equals(0)) {
            throw exception(ErrorCodeConstants.INVENTORY_PLAN_STATUS_NOT_DRAFT);
        }

        // 校验计划名称唯一性
        validatePlanNameUnique(updateReqVO.getId(), updateReqVO.getPlanName());

        // 校验时间范围
        validateTimeRange(updateReqVO.getStartTime(), updateReqVO.getEndTime());

        // 获取负责人信息
        AdminUserRespDTO responsibleUser = adminUserApi.getUser(updateReqVO.getResponsiblePersonId()).getData();
        if (responsibleUser == null) {
            throw exception(ErrorCodeConstants.USER_NOT_EXISTS);
        }

        // 更新盘点计划
        ErpInventoryPlanDO updateObj = BeanUtils.toBean(updateReqVO, ErpInventoryPlanDO.class);
        
        // 转换日期字段（Date -> LocalDateTime）
        updateObj.setStartTime(convertToLocalDateTime(updateReqVO.getStartTime()));
        updateObj.setEndTime(convertToLocalDateTime(updateReqVO.getEndTime()));
        
        // 设置负责人信息
        updateObj.setResponsiblePersonName(responsibleUser.getNickname());
        
        // 处理JSON字段
        updateObj.setDepartmentIds(JsonUtils.toJsonString(updateReqVO.getDepartmentIds()));
        updateObj.setUserIds(JsonUtils.toJsonString(updateReqVO.getUserIds()));
        updateObj.setLocationIds(JsonUtils.toJsonString(updateReqVO.getLocationIds()));
        
        // 处理人员信息
        fillUserInfo(updateObj, updateReqVO.getScannerIds(), updateReqVO.getReviewerIds());

        inventoryPlanMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteInventoryPlan(Long id) {
        // 校验盘点计划存在
        ErpInventoryPlanDO existingPlan = validateInventoryPlanExists(id);
        
        // 只有草稿状态才能删除
        if (!existingPlan.getStatus().equals(0)) {
            throw exception(ErrorCodeConstants.INVENTORY_PLAN_STATUS_NOT_DRAFT);
        }

        // 删除盘点计划
        inventoryPlanMapper.deleteById(id);
    }

    @Override
    public ErpInventoryPlanDO getInventoryPlan(Long id) {
        return inventoryPlanMapper.selectById(id);
    }

    @Override
    public PageResult<ErpInventoryPlanDO> getInventoryPlanPage(ErpInventoryPlanPageReqVO pageReqVO) {
        return inventoryPlanMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpInventoryPlanDO> getInventoryPlanList(ErpInventoryPlanPageReqVO pageReqVO) {
        return inventoryPlanMapper.selectList(pageReqVO);
    }

    @Override
    public List<ErpInventoryPlanDO> getInventoryPlanListByStatus(Integer status) {
        return inventoryPlanMapper.selectListByStatus(status);
    }

    @Override
    public List<ErpInventoryPlanDO> getInventoryPlanListByResponsiblePerson(Long responsiblePersonId) {
        return inventoryPlanMapper.selectListByResponsiblePersonId(responsiblePersonId);
    }

    @Override
    public ErpInventoryPlanDO getInventoryPlanByPlanNo(String planNo) {
        return inventoryPlanMapper.selectByPlanNo(planNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitApproval(Long id) {
        ErpInventoryPlanDO plan = validateInventoryPlanExists(id);
        
        // 只有草稿状态才能提交发布（为了向后兼容，这个方法现在等同于发布）
        if (!plan.getStatus().equals(0)) {
            throw exception(ErrorCodeConstants.INVENTORY_PLAN_STATUS_NOT_DRAFT);
        }

        // 生成待盘点资产列表并计算总数
        List<Long> assetIds = generateInventoryAssetList(id);
        
        plan.setStatus(1); // 已发布（新状态体系下）
        plan.setTotalAssetCount(assetIds.size());
        plan.setCompletedAssetCount(0);
        
        inventoryPlanMapper.updateById(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveInventoryPlan(Long id, Boolean passed, String remark) {
        ErpInventoryPlanDO plan = validateInventoryPlanExists(id);
        
        // 只有已发布状态才能进行此审批（可选的发布后审批流程）
        if (!plan.getStatus().equals(1)) {
            throw exception(ErrorCodeConstants.INVENTORY_PLAN_STATUS_NOT_PENDING_APPROVAL);
        }

        if (passed) {
            plan.setStatus(2); // 通过：进行中
            plan.setActualStartTime(LocalDateTime.now());
        } else {
            plan.setStatus(0); // 拒绝：回到草稿状态
        }
        
        plan.setApprovalTime(LocalDateTime.now());
        plan.setApprovalRemark(remark);
        
        inventoryPlanMapper.updateById(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startInventoryPlan(Long id) {
        ErpInventoryPlanDO plan = validateInventoryPlanExists(id);
        
        // 已发布状态自动进入进行中状态（通过扫码触发）
        // 这个方法保留用于手动开始盘点的场景
        if (!plan.getStatus().equals(1)) {
            throw exception(ErrorCodeConstants.INVENTORY_PLAN_STATUS_NOT_APPROVED);
        }

        plan.setStatus(2); // 进行中
        plan.setActualStartTime(LocalDateTime.now());
        
        inventoryPlanMapper.updateById(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeInventoryPlan(Long id) {
        ErpInventoryPlanDO plan = validateInventoryPlanExists(id);
        
        // 只有进行中状态才能完成
        if (!plan.getStatus().equals(2)) {
            throw exception(ErrorCodeConstants.INVENTORY_PLAN_STATUS_NOT_EXECUTING);
        }

        plan.setStatus(3); // 已完成
        plan.setActualEndTime(LocalDateTime.now());
        
        inventoryPlanMapper.updateById(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelInventoryPlan(Long id) {
        ErpInventoryPlanDO plan = validateInventoryPlanExists(id);
        
        // 已审核/已关闭状态不能取消
        if (plan.getStatus().equals(6)) {
            throw exception(ErrorCodeConstants.INVENTORY_PLAN_STATUS_COMPLETED);
        }

        // 注意：在新的状态体系中，我们可能需要一个新的"已取消"状态值
        // 这里暂时保留原逻辑，实际应该定义一个新的状态值或重新设计取消逻辑
        plan.setStatus(7); // 已取消（需要在状态定义中添加这个新状态）
        inventoryPlanMapper.updateById(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateInventoryProgress(Long id, Integer completedAssetCount) {
        ErpInventoryPlanDO plan = validateInventoryPlanExists(id);
        
        // 只有进行中状态才能更新进度
        if (!plan.getStatus().equals(2)) {
            throw exception(ErrorCodeConstants.INVENTORY_PLAN_STATUS_NOT_EXECUTING);
        }

        plan.setCompletedAssetCount(completedAssetCount);
        
        // 检查是否所有资产都已盘点完成，如果是则自动完成
        if (plan.getTotalAssetCount() != null && 
            completedAssetCount.equals(plan.getTotalAssetCount()) && 
            completedAssetCount > 0) {
            plan.setStatus(3); // 已完成
            plan.setActualEndTime(LocalDateTime.now());
        }
        
        inventoryPlanMapper.updateById(plan);
    }

    @Override
    public List<ErpInventoryPlanDO> getActiveExecutionPlans() {
        return inventoryPlanMapper.selectActiveExecutionPlans();
    }

    @Override
    public List<Long> generateInventoryAssetList(Long planId) {
        ErpInventoryPlanDO plan = validateInventoryPlanExists(planId);
        
        // 根据选择条件查询资产
        List<ErpAssetDO> allAssets = queryAssetsByPlan(plan);
        
        // 如果是抽样盘点，进行抽样
        if ("sample".equals(plan.getMethod()) && plan.getSampleRate() != null) {
            return sampleAssets(allAssets, plan.getSampleRate(), plan.getSampleMethod());
        }
        
        // 全盘点返回所有资产ID
        return allAssets.stream().map(ErpAssetDO::getId).collect(Collectors.toList());
    }

    @Override
    public ErpInventoryPlanDO validateInventoryPlanExists(Long id) {
        ErpInventoryPlanDO plan = inventoryPlanMapper.selectById(id);
        if (plan == null) {
            throw exception(ErrorCodeConstants.INVENTORY_PLAN_NOT_EXISTS);
        }
        return plan;
    }

    // ========== 私有方法 ==========

    private void validatePlanNameUnique(Long id, String planName) {
        ErpInventoryPlanDO plan = inventoryPlanMapper.selectOne(ErpInventoryPlanDO::getPlanName, planName);
        if (plan != null && !plan.getId().equals(id)) {
            throw exception(ErrorCodeConstants.INVENTORY_PLAN_NAME_EXISTS);
        }
    }

    private void validateTimeRange(Date startTime, Date endTime) {
        if (startTime.after(endTime)) {
            throw exception(ErrorCodeConstants.INVENTORY_PLAN_TIME_RANGE_INVALID);
        }
    }

    private LocalDateTime convertToLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
    }

    private String generatePlanNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Long count = inventoryPlanMapper.selectCount();
        return String.format("INV-%s-%04d", dateStr, count + 1);
    }

    private void fillUserInfo(ErpInventoryPlanDO plan, List<Long> scannerIds, List<Long> reviewerIds) {
        // 处理扫码员信息
        if (scannerIds != null && !scannerIds.isEmpty()) {
            List<AdminUserRespDTO> scanners = adminUserApi.getUserList(scannerIds).getData();
            plan.setScannerIds(JsonUtils.toJsonString(scannerIds));
            plan.setScannerNames(JsonUtils.toJsonString(
                scanners.stream().map(AdminUserRespDTO::getNickname).collect(Collectors.toList())
            ));
        }
        
        // 处理复核人员信息
        if (reviewerIds != null && !reviewerIds.isEmpty()) {
            List<AdminUserRespDTO> reviewers = adminUserApi.getUserList(reviewerIds).getData();
            plan.setReviewerIds(JsonUtils.toJsonString(reviewerIds));
            plan.setReviewerNames(JsonUtils.toJsonString(
                reviewers.stream().map(AdminUserRespDTO::getNickname).collect(Collectors.toList())
            ));
        }
    }

    private List<ErpAssetDO> queryAssetsByPlan(ErpInventoryPlanDO plan) {
        // 这里简化处理，实际应该根据仓库、区域、类别等条件查询
        // 可以调用 assetMapper 的相关方法进行条件查询
        return assetMapper.selectList();
    }

    private List<Long> sampleAssets(List<ErpAssetDO> assets, Integer sampleRate, String sampleMethod) {
        int sampleSize = Math.max(1, assets.size() * sampleRate / 100);
        
        switch (sampleMethod) {
            case "random":
                // 随机抽样
                Collections.shuffle(assets);
                break;
            case "value":
                // 按价值抽样（价值高的优先）
                assets.sort((a, b) -> b.getCurrentValue().compareTo(a.getCurrentValue()));
                break;
            case "category":
                // 按类别抽样（均匀分布）
                // 这里简化处理，实际应该按类别分组后均匀抽取
                break;
            default:
                Collections.shuffle(assets);
        }
        
        return assets.stream()
                .limit(sampleSize)
                .map(ErpAssetDO::getId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpAssetInventoryRespVO scanAssetInventory(@Valid ErpAssetInventoryReqVO reqVO) {
        // 1. 验证盘点计划是否存在且可执行
        ErpInventoryPlanDO plan = validateInventoryPlanExists(reqVO.getPlanId());
        
        // 如果是已发布状态，第一次扫码自动转为进行中状态
        if (plan.getStatus().equals(1)) {
            plan.setStatus(2); // 进行中
            plan.setActualStartTime(LocalDateTime.now());
            inventoryPlanMapper.updateById(plan);
        }
        
        // 只有进行中状态才能进行扫码盘点
        if (!plan.getStatus().equals(2)) { // 2-进行中
            throw exception(ErrorCodeConstants.INVENTORY_PLAN_STATUS_NOT_EXECUTING);
        }

        // 2. 根据扫描内容查找资产
        ErpAssetDO asset = findAssetByScanContent(reqVO.getScanContent());
        if (asset == null) {
            throw exception(ErrorCodeConstants.ASSET_NOT_EXISTS);
        }

        // 3. 检查是否已经盘点过
        ErpAssetInventoryRecordDO existingRecord = assetInventoryRecordMapper
                .selectByAssetIdAndPlanId(asset.getId(), reqVO.getPlanId());

        ErpAssetInventoryRecordDO record;
        if (existingRecord != null) {
            // 更新已有记录
            record = updateInventoryRecord(existingRecord, asset, reqVO);
        } else {
            // 创建新的盘点记录
            record = createInventoryRecord(asset, reqVO);
        }

        // 4. 更新盘点计划进度
        updateInventoryProgress(plan);

        // 5. 转换并返回结果
        return convertToRespVO(record, asset);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpValidateScanRespVO validateScanCode(@Valid ErpValidateScanReqVO reqVO) {
        // 1. 验证盘点计划是否存在且可执行
        ErpInventoryPlanDO plan = validateInventoryPlanExists(reqVO.getPlanId());
        
        // 如果是已发布状态，第一次扫码自动转为进行中状态
        if (plan.getStatus().equals(1)) {
            plan.setStatus(2); // 进行中
            plan.setActualStartTime(LocalDateTime.now());
            inventoryPlanMapper.updateById(plan);
        }
        
        // 只有进行中状态才能进行扫码盘点
        if (!plan.getStatus().equals(2)) { // 2-进行中
            throw exception(ErrorCodeConstants.INVENTORY_PLAN_STATUS_NOT_EXECUTING);
        }

        // 2. 根据扫描内容查找资产
        ErpAssetDO asset = findAssetByScanContent(reqVO.getScanContent());
        if (asset == null) {
            throw exception(ErrorCodeConstants.ASSET_NOT_EXISTS);
        }

        // 3. 检查是否已经盘点过
        ErpAssetInventoryRecordDO existingRecord = assetInventoryRecordMapper
                .selectByAssetIdAndPlanId(asset.getId(), reqVO.getPlanId());

        ErpAssetInventoryRecordDO record;
        boolean alreadyInventoried = false;
        
        if (existingRecord != null && "completed".equals(existingRecord.getActionStatus())) {
            // 如果已经完成盘点，标记为已盘点
            alreadyInventoried = true;
            record = existingRecord;
        } else if (existingRecord != null) {
            // 更新已有的pending记录
            record = existingRecord;
            record.setScanContent(reqVO.getScanContent());
            record.setScanMethod(reqVO.getScanMethod());
            record.setInventoryTime(LocalDateTime.now());
            assetInventoryRecordMapper.updateById(record);
        } else {
            // 创建新的pending状态记录
            record = createPendingInventoryRecord(asset, reqVO);
        }

        // 4. 转换并返回验证结果
        return convertToValidateRespVO(record, asset, alreadyInventoried);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpAssetInventoryRespVO confirmInventory(@Valid ErpConfirmInventoryReqVO reqVO) {
        // 1. 获取盘点记录
        ErpAssetInventoryRecordDO record = assetInventoryRecordMapper.selectById(reqVO.getRecordId());
        if (record == null) {
            throw exception(ErrorCodeConstants.ASSET_INVENTORY_RECORD_NOT_EXISTS);
        }

        // 2. 获取资产信息
        ErpAssetDO asset = assetMapper.selectById(record.getAssetId());
        if (asset == null) {
            throw exception(ErrorCodeConstants.ASSET_NOT_EXISTS);
        }

        // 3. 更新盘点记录为已完成状态
        updateInventoryRecordToCompleted(record, reqVO);

        // 4. 更新盘点计划进度
        ErpInventoryPlanDO plan = validateInventoryPlanExists(record.getPlanId());
        updateInventoryProgress(plan);

        // 5. 转换并返回结果
        return convertToRespVO(record, asset);
    }

    /**
     * 根据扫描内容查找资产
     */
    private ErpAssetDO findAssetByScanContent(String scanContent) {
        // 尝试解析为JSON格式
        try {
            if (scanContent.trim().startsWith("{") && scanContent.trim().endsWith("}")) {
                Map<String, Object> jsonData = JsonUtils.parseObject(scanContent, Map.class);
                
                // 优先使用JSON中的assetNo字段查找
                String assetNo = (String) jsonData.get("assetNo");
                if (assetNo != null && !assetNo.trim().isEmpty()) {
                    ErpAssetDO asset = assetMapper.selectOne("asset_no", assetNo);
                    if (asset != null) {
                        return asset;
                    }
                }
                
                // 如果assetNo找不到，尝试使用id字段
                Object idObj = jsonData.get("id");
                if (idObj != null) {
                    String idStr = idObj.toString();
                    ErpAssetDO asset = assetMapper.selectById(Long.parseLong(idStr));
                    if (asset != null) {
                        return asset;
                    }
                }
                
                // 如果id也找不到，尝试使用name字段
                String name = (String) jsonData.get("name");
                if (name != null && !name.trim().isEmpty()) {
                    ErpAssetDO asset = assetMapper.selectOne("name", name);
                    if (asset != null) {
                        return asset;
                    }
                }
            }
        } catch (Exception e) {
            // JSON解析失败，继续使用原有逻辑
        }
        
        // 原有逻辑作为后备方案：首先尝试按资产编码查找
        ErpAssetDO asset = assetMapper.selectOne("asset_no", scanContent);
        if (asset != null) {
            return asset;
        }

        // 如果找不到，尝试按序列号查找
        asset = assetMapper.selectOne("serial_number", scanContent);
        if (asset != null) {
            return asset;
        }

        // 如果还找不到，按名称模糊查找
        return assetMapper.selectOne("name", scanContent);
    }

    /**
     * 将资产状态Integer转换为String
     */
    private String convertAssetStatusToString(Integer status) {
        if (status == null) {
            return "unknown";
        }
        switch (status) {
            case 1: return "normal";
            case 2: return "maintenance";
            case 3: return "scrapped";
            case 4: return "idle";
            default: return "unknown";
        }
    }

    /**
     * 创建新的盘点记录
     */
    private ErpAssetInventoryRecordDO createInventoryRecord(ErpAssetDO asset, ErpAssetInventoryReqVO reqVO) {
        ErpAssetInventoryRecordDO record = new ErpAssetInventoryRecordDO();
        
        // 基本信息
        record.setPlanId(reqVO.getPlanId());
        record.setAssetId(asset.getId());
        record.setAssetCode(asset.getAssetNo());
        record.setAssetName(asset.getName());
        record.setCategoryId(asset.getCategoryId());
        // 可以通过分类ID查询分类名称，这里简化处理
        record.setCategoryName(""); 

        // 预期信息（从资产表获取）
        record.setExpectedStatus(convertAssetStatusToString(asset.getStatus()));
        record.setExpectedLocationId(null); // 资产表中没有locationId，使用location字段
        record.setExpectedLocationName(asset.getLocation());
        record.setExpectedUserId(null); // 资产表中没有直接的userId字段
        record.setExpectedUserName("");

        // 实际信息（从请求获取）
        String actualStatus = reqVO.getActualStatus() != null ? 
            reqVO.getActualStatus() : convertAssetStatusToString(asset.getStatus());
        record.setActualStatus(actualStatus);
        record.setActualLocationId(reqVO.getActualLocationId());
        record.setActualLocationName(reqVO.getActualLocationName());
        record.setActualUserId(reqVO.getActualUserId());
        record.setActualUserName(reqVO.getActualUserName());

        // 盘点信息
        record.setInventoryTime(LocalDateTime.now());
        // TODO: 获取当前登录用户信息
        record.setInventoryUserId(1L); // 临时写死，实际应该从SecurityContext获取
        record.setInventoryUserName("盘点员"); // 临时写死
        record.setScanMethod(reqVO.getScanMethod());
        record.setScanContent(reqVO.getScanContent());

        // 资产价值信息
        record.setOriginalValue(asset.getPurchasePrice());
        record.setCurrentValue(reqVO.getCurrentValue() != null ? reqVO.getCurrentValue() : asset.getCurrentValue());

        // 差异分析
        String inventoryResult = analyzeInventoryResult(record);
        record.setInventoryResult(inventoryResult);
        record.setDiffDescription(reqVO.getDiffDescription());
        record.setRemark(reqVO.getRemark());
        record.setPhotoUrl(reqVO.getPhotoUrl());

        // 处理状态
        record.setNeedsAction(!"normal".equals(inventoryResult));
        record.setActionStatus("pending");

        assetInventoryRecordMapper.insert(record);
        return record;
    }

    /**
     * 创建新的盘点记录（pending状态）
     */
    private ErpAssetInventoryRecordDO createPendingInventoryRecord(ErpAssetDO asset, ErpValidateScanReqVO reqVO) {
        ErpAssetInventoryRecordDO record = new ErpAssetInventoryRecordDO();
        record.setPlanId(reqVO.getPlanId());
        record.setAssetId(asset.getId());
        record.setAssetCode(asset.getAssetNo());
        record.setAssetName(asset.getName());
        record.setCategoryId(asset.getCategoryId());
        record.setCategoryName("");
        record.setExpectedStatus(convertAssetStatusToString(asset.getStatus()));
        record.setExpectedLocationId(null);
        record.setExpectedLocationName(asset.getLocation());
        record.setExpectedUserId(null);
        record.setExpectedUserName("");
        record.setActualStatus(convertAssetStatusToString(asset.getStatus()));
        record.setActualLocationId(null);
        record.setActualLocationName(asset.getLocation());
        record.setActualUserId(null);
        record.setActualUserName("");
        record.setInventoryTime(LocalDateTime.now());
        record.setInventoryUserId(1L); // 临时写死
        record.setInventoryUserName("盘点员"); // 临时写死
        record.setScanMethod(reqVO.getScanMethod());
        record.setScanContent(reqVO.getScanContent());
        record.setOriginalValue(asset.getPurchasePrice());
        record.setCurrentValue(asset.getCurrentValue());
        record.setInventoryResult("normal");
        record.setDiffDescription("");
        record.setRemark("");
        record.setPhotoUrl("");
        record.setNeedsAction(false);
        record.setActionStatus("pending");
        assetInventoryRecordMapper.insert(record);
        return record;
    }

    /**
     * 更新已有盘点记录
     */
    private ErpAssetInventoryRecordDO updateInventoryRecord(ErpAssetInventoryRecordDO existingRecord, 
                                                            ErpAssetDO asset, ErpAssetInventoryReqVO reqVO) {
        // 更新实际信息
        String actualStatus = reqVO.getActualStatus() != null ? 
            reqVO.getActualStatus() : convertAssetStatusToString(asset.getStatus());
        existingRecord.setActualStatus(actualStatus);
        existingRecord.setActualLocationId(reqVO.getActualLocationId());
        existingRecord.setActualLocationName(reqVO.getActualLocationName());
        existingRecord.setActualUserId(reqVO.getActualUserId());
        existingRecord.setActualUserName(reqVO.getActualUserName());

        // 更新盘点信息
        existingRecord.setInventoryTime(LocalDateTime.now());
        existingRecord.setScanMethod(reqVO.getScanMethod());
        existingRecord.setScanContent(reqVO.getScanContent());
        existingRecord.setCurrentValue(reqVO.getCurrentValue());
        existingRecord.setDiffDescription(reqVO.getDiffDescription());
        existingRecord.setRemark(reqVO.getRemark());
        existingRecord.setPhotoUrl(reqVO.getPhotoUrl());

        // 重新分析差异
        String inventoryResult = analyzeInventoryResult(existingRecord);
        existingRecord.setInventoryResult(inventoryResult);
        existingRecord.setNeedsAction(!"normal".equals(inventoryResult));

        assetInventoryRecordMapper.updateById(existingRecord);
        return existingRecord;
    }

    /**
     * 更新盘点记录为已完成状态
     */
    private void updateInventoryRecordToCompleted(ErpAssetInventoryRecordDO record, ErpConfirmInventoryReqVO reqVO) {
        // 根据差异类型设置实际状态
        if ("status_mismatch".equals(reqVO.getDifferenceType())) {
            record.setActualStatus(reqVO.getActualStatus());
            record.setInventoryResult("status_diff");
            record.setNeedsAction(true);
        } else if ("location_mismatch".equals(reqVO.getDifferenceType())) {
            record.setActualLocationId(reqVO.getActualLocationId());
            record.setActualLocationName(reqVO.getActualLocationName());
            record.setInventoryResult("location_diff");
            record.setNeedsAction(true);
        } else if ("information_mismatch".equals(reqVO.getDifferenceType())) {
            record.setActualUserId(reqVO.getActualUserId());
            record.setActualUserName(reqVO.getActualUserName());
            record.setInventoryResult("user_diff");
            record.setNeedsAction(true);
        } else {
            // 正常情况，实际状态与预期状态一致
            record.setInventoryResult("normal");
            record.setNeedsAction(false);
        }
        
        // 更新通用字段
        if (reqVO.getCurrentValue() != null) {
            record.setCurrentValue(reqVO.getCurrentValue());
        }
        record.setPhotoUrl(reqVO.getPhotoUrl() != null ? reqVO.getPhotoUrl() : "");
        record.setDiffDescription(reqVO.getDiffDescription() != null ? reqVO.getDiffDescription() : "");
        record.setRemark(reqVO.getRemark() != null ? reqVO.getRemark() : "");
        record.setInventoryTime(LocalDateTime.now());
        record.setActionStatus("completed");
        record.setActionTime(LocalDateTime.now());
        record.setActionUserId(1L); // 临时写死，实际应该从登录用户获取
        record.setActionUserName("盘点员"); // 临时写死
        record.setActionRemark(reqVO.getRemark());
        
        assetInventoryRecordMapper.updateById(record);
    }

    /**
     * 分析盘点结果
     */
    private String analyzeInventoryResult(ErpAssetInventoryRecordDO record) {
        // 状态差异
        if (!Objects.equals(record.getExpectedStatus(), record.getActualStatus())) {
            return "status_diff";
        }
        
        // 位置差异
        if (!Objects.equals(record.getExpectedLocationId(), record.getActualLocationId())) {
            return "location_diff";
        }
        
        // 使用人差异
        if (!Objects.equals(record.getExpectedUserId(), record.getActualUserId())) {
            return "user_diff";
        }
        
        // 正常
        return "normal";
    }

    /**
     * 更新盘点计划进度
     */
    private void updateInventoryProgress(ErpInventoryPlanDO plan) {
        Long completedCount = assetInventoryRecordMapper.countByPlanId(plan.getId());
        plan.setCompletedAssetCount(completedCount.intValue());
        
        // 检查是否所有资产都已盘点完成，如果是则自动完成
        if (plan.getTotalAssetCount() != null && 
            completedCount.equals(plan.getTotalAssetCount().longValue()) && 
            completedCount > 0 && 
            plan.getStatus().equals(2)) { // 只有进行中状态才能自动完成
            plan.setStatus(3); // 已完成
            plan.setActualEndTime(LocalDateTime.now());
        }
        
        inventoryPlanMapper.updateById(plan);
    }

    /**
     * 转换为响应VO
     */
    private ErpAssetInventoryRespVO convertToRespVO(ErpAssetInventoryRecordDO record, ErpAssetDO asset) {
        ErpAssetInventoryRespVO respVO = BeanUtils.toBean(record, ErpAssetInventoryRespVO.class);
        
        // BeanUtils会自动处理时间字段的转换，这里不需要手动设置
        // 如果需要特定格式，应该在VO中使用@JsonFormat注解
        
        return respVO;
    }

    /**
     * 转换为验证响应VO
     */
    private ErpValidateScanRespVO convertToValidateRespVO(ErpAssetInventoryRecordDO record, ErpAssetDO asset, boolean alreadyInventoried) {
        ErpValidateScanRespVO respVO = new ErpValidateScanRespVO();
        respVO.setRecordId(record.getId());
        respVO.setAssetId(asset.getId());
        respVO.setAssetCode(asset.getAssetNo());
        respVO.setAssetName(asset.getName());
        respVO.setCategoryId(asset.getCategoryId());
        respVO.setCategoryName(record.getCategoryName()); // 从记录中获取分类名称
        respVO.setExpectedStatus(record.getExpectedStatus());
        respVO.setExpectedLocationId(record.getExpectedLocationId());
        respVO.setExpectedLocationName(record.getExpectedLocationName());
        respVO.setExpectedUserId(record.getExpectedUserId());
        respVO.setExpectedUserName(record.getExpectedUserName());
        respVO.setActualStatus(record.getActualStatus());
        respVO.setActualLocationId(record.getActualLocationId());
        respVO.setActualLocationName(record.getActualLocationName());
        respVO.setActualUserId(record.getActualUserId());
        respVO.setActualUserName(record.getActualUserName());
        respVO.setInventoryTime(record.getInventoryTime());
        respVO.setInventoryUserId(record.getInventoryUserId());
        respVO.setInventoryUserName(record.getInventoryUserName());
        respVO.setScanMethod(record.getScanMethod());
        respVO.setScanContent(record.getScanContent());
        respVO.setOriginalValue(record.getOriginalValue());
        respVO.setCurrentValue(record.getCurrentValue());
        respVO.setInventoryResult(record.getInventoryResult());
        respVO.setDiffDescription(record.getDiffDescription());
        respVO.setRemark(record.getRemark());
        respVO.setPhotoUrl(record.getPhotoUrl());
        respVO.setNeedsAction(record.getNeedsAction());
        respVO.setActionStatus(record.getActionStatus());
        respVO.setAlreadyInventoried(alreadyInventoried);
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishInventoryPlan(Long id) {
        ErpInventoryPlanDO plan = validateInventoryPlanExists(id);
        
        // 只有草稿状态才能发布
        if (!plan.getStatus().equals(0)) {
            throw exception(ErrorCodeConstants.INVENTORY_PLAN_STATUS_NOT_DRAFT);
        }

        // 生成待盘点资产列表并计算总数
        List<Long> assetIds = generateInventoryAssetList(id);
        
        plan.setStatus(1); // 已发布
        plan.setTotalAssetCount(assetIds.size());
        plan.setCompletedAssetCount(0);
        
        inventoryPlanMapper.updateById(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitInventoryPlan(Long id, String reason) {
        ErpInventoryPlanDO plan = validateInventoryPlanExists(id);
        
        // 已完成可提交；进行中允许提前结束并提交
        boolean earlySubmit = plan.getStatus().equals(2);
        if (!earlySubmit && !plan.getStatus().equals(3)) {
            throw exception(ErrorCodeConstants.INVENTORY_PLAN_STATUS_NOT_COMPLETED);
        }

        Map<String, Object> processInstanceVariables = new HashMap<>();
        processInstanceVariables.put("planId", plan.getId());
        processInstanceVariables.put("planNo", plan.getPlanNo());
        processInstanceVariables.put("planName", plan.getPlanName());
        processInstanceVariables.put("planType", plan.getPlanType());
        processInstanceVariables.put("method", plan.getMethod());
        processInstanceVariables.put("responsiblePersonId", plan.getResponsiblePersonId());
        processInstanceVariables.put("responsiblePersonName", plan.getResponsiblePersonName());
        processInstanceVariables.put("totalAssetCount", plan.getTotalAssetCount());
        processInstanceVariables.put("completedAssetCount", plan.getCompletedAssetCount());
        processInstanceVariables.put("reason", reason);
        processInstanceVariables.put("earlySubmit", earlySubmit);

        String processInstanceId = processInstanceApi.createProcessInstance(getLoginUserId(),
                new BpmProcessInstanceCreateReqDTO()
                        .setProcessDefinitionKey(PROCESS_KEY)
                        .setVariables(processInstanceVariables)
                        .setBusinessKey(String.valueOf(plan.getId()))).getCheckedData();

        if (earlySubmit) {
            plan.setActualEndTime(LocalDateTime.now());
        }
        plan.setStatus(5); // 待审核（提交后自动进入待审核状态）
        plan.setRemark(reason); // 将提交原因保存到备注字段
        plan.setProcessInstanceId(processInstanceId);
        
        inventoryPlanMapper.updateById(plan);

        log.info("盘点计划提交BPM审核成功: planId={}, processInstanceId={}", plan.getId(), processInstanceId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditInventoryPlan(Long id, Boolean passed, String remark) {
        ErpInventoryPlanDO plan = validateInventoryPlanExists(id);
        
        // 只有待审核状态才能审核
        if (!plan.getStatus().equals(5)) {
            throw exception(ErrorCodeConstants.INVENTORY_PLAN_STATUS_NOT_PENDING_AUDIT);
        }

        if (passed) {
            plan.setStatus(6); // 已审核/已关闭
        } else {
            // 驳回回到已完成状态，可以重新提交
            plan.setStatus(3); // 已完成
        }
        
        plan.setApprovalTime(LocalDateTime.now());
        plan.setApprovalRemark(remark);
        
        inventoryPlanMapper.updateById(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateInventoryPlanBpmStatus(Long id, Integer bpmStatus, Long approvalUserId) {
        ErpInventoryPlanDO plan = validateInventoryPlanExists(id);
        if (!plan.getStatus().equals(5)) {
            log.warn("盘点计划当前状态不是待审核，忽略BPM状态回调: planId={}, status={}, bpmStatus={}",
                    id, plan.getStatus(), bpmStatus);
            return;
        }

        String approvalRemark;
        switch (bpmStatus) {
            case 2:
                plan.setStatus(6); // 已审核/已关闭
                approvalRemark = "BPM审批通过";
                break;
            case 3:
                plan.setStatus(3); // 驳回后回到已完成，可重新提交
                approvalRemark = "BPM审批驳回";
                break;
            case 4:
                plan.setStatus(3); // 流程取消后回到已完成，可重新提交
                approvalRemark = "BPM审批取消";
                break;
            default:
                log.debug("盘点计划BPM流程仍在进行中: planId={}, bpmStatus={}", id, bpmStatus);
                return;
        }

        if (approvalUserId != null) {
            plan.setApprovalUserId(approvalUserId);
            try {
                AdminUserRespDTO approvalUser = adminUserApi.getUser(approvalUserId).getCheckedData();
                if (approvalUser != null) {
                    plan.setApprovalUserName(approvalUser.getNickname());
                }
            } catch (Exception e) {
                log.warn("获取盘点计划BPM审批人失败: planId={}, approvalUserId={}", id, approvalUserId, e);
            }
        }
        plan.setApprovalTime(LocalDateTime.now());
        plan.setApprovalRemark(approvalRemark);

        inventoryPlanMapper.updateById(plan);
        log.info("盘点计划BPM审核状态已更新: planId={}, bpmStatus={}, status={}", id, bpmStatus, plan.getStatus());
    }

    @Override
    public ErpInventoryPlanProgressRespVO getInventoryPlanProgress(Long planId) {
        // 获取盘点计划信息
        ErpInventoryPlanDO plan = validateInventoryPlanExists(planId);
        
        // 构建响应对象
        ErpInventoryPlanProgressRespVO respVO = new ErpInventoryPlanProgressRespVO();
        respVO.setPlanId(plan.getId());
        respVO.setPlanNo(plan.getPlanNo());
        respVO.setPlanName(plan.getPlanName());
        respVO.setStatus(plan.getStatus());
        respVO.setStatusName(getStatusName(plan.getStatus()));
        
        // 构建进度信息
        ErpInventoryPlanProgressRespVO.ProgressInfo progressInfo = new ErpInventoryPlanProgressRespVO.ProgressInfo();
        progressInfo.setTotalAssetCount(plan.getTotalAssetCount() != null ? plan.getTotalAssetCount() : 0);
        progressInfo.setCompletedAssetCount(plan.getCompletedAssetCount() != null ? plan.getCompletedAssetCount() : 0);
        
        // 计算完成百分比
        if (progressInfo.getTotalAssetCount() > 0) {
            BigDecimal percentage = BigDecimal.valueOf(progressInfo.getCompletedAssetCount())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(progressInfo.getTotalAssetCount()), 2, RoundingMode.HALF_UP);
            progressInfo.setCompletionPercentage(percentage);
        } else {
            progressInfo.setCompletionPercentage(BigDecimal.ZERO);
        }
        
        progressInfo.setPendingAssetCount(progressInfo.getTotalAssetCount() - progressInfo.getCompletedAssetCount());
        
        // 统计异常数量（需要处理的记录）
        List<ErpAssetInventoryRecordDO> abnormalRecords = assetInventoryRecordMapper.selectList(
                new LambdaQueryWrapperX<ErpAssetInventoryRecordDO>()
                        .eq(ErpAssetInventoryRecordDO::getPlanId, planId)
                        .eq(ErpAssetInventoryRecordDO::getNeedsAction, true));
        progressInfo.setAbnormalAssetCount(abnormalRecords.size());
        
        respVO.setProgressInfo(progressInfo);
        
        // 构建时间线信息
        List<ErpInventoryPlanChangeLogDO> changeLogs = inventoryPlanChangeLogMapper.selectListByPlanId(planId);
        List<ErpInventoryPlanProgressRespVO.TimelineItem> timeline = changeLogs.stream()
                .map(this::convertToTimelineItem)
                .collect(Collectors.toList());
        respVO.setTimeline(timeline);
        
        return respVO;
    }

    @Override
    public PageResult<ErpInventoryRecordRespVO> getInventoryRecordPage(ErpInventoryRecordPageReqVO pageReqVO) {
        PageResult<ErpAssetInventoryRecordDO> pageResult = assetInventoryRecordMapper.selectPage(pageReqVO, 
                new LambdaQueryWrapperX<ErpAssetInventoryRecordDO>()
                        .eqIfPresent(ErpAssetInventoryRecordDO::getPlanId, pageReqVO.getPlanId())
                        .likeIfPresent(ErpAssetInventoryRecordDO::getAssetCode, pageReqVO.getAssetCode())
                        .likeIfPresent(ErpAssetInventoryRecordDO::getAssetName, pageReqVO.getAssetName())
                        .eqIfPresent(ErpAssetInventoryRecordDO::getInventoryResult, pageReqVO.getInventoryResult())
                        .eqIfPresent(ErpAssetInventoryRecordDO::getNeedsAction, pageReqVO.getNeedsAction())
                        .eqIfPresent(ErpAssetInventoryRecordDO::getActionStatus, pageReqVO.getActionStatus())
                        .orderByDesc(ErpAssetInventoryRecordDO::getInventoryTime));
        
        List<ErpInventoryRecordRespVO> records = pageResult.getList().stream()
                .map(this::convertToRecordRespVO)
                .collect(Collectors.toList());
        
        return new PageResult<>(records, pageResult.getTotal());
    }

    @Override
    public List<ErpInventoryRecordRespVO> getInventoryRecordList(Long planId) {
        List<ErpAssetInventoryRecordDO> records = assetInventoryRecordMapper.selectListByPlanId(planId);
        return records.stream()
                .map(this::convertToRecordRespVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ErpInventoryRecordRespVO> getAbnormalInventoryRecordList(Long planId) {
        List<ErpAssetInventoryRecordDO> records = assetInventoryRecordMapper.selectList(
                new LambdaQueryWrapperX<ErpAssetInventoryRecordDO>()
                        .eq(ErpAssetInventoryRecordDO::getPlanId, planId)
                        .eq(ErpAssetInventoryRecordDO::getNeedsAction, true)
                        .orderByDesc(ErpAssetInventoryRecordDO::getInventoryTime));
        return records.stream()
                .map(this::convertToRecordRespVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(Integer status) {
        switch (status) {
            case 0: return "草稿";
            case 1: return "已发布";
            case 2: return "进行中";
            case 3: return "已完成";
            case 4: return "已提交";
            case 5: return "待审核";
            case 6: return "已审核/已关闭";
            case 7: return "已取消";
            default: return "未知";
        }
    }

    /**
     * 转换变更日志为时间线项目
     */
    private ErpInventoryPlanProgressRespVO.TimelineItem convertToTimelineItem(ErpInventoryPlanChangeLogDO changeLog) {
        ErpInventoryPlanProgressRespVO.TimelineItem item = new ErpInventoryPlanProgressRespVO.TimelineItem();
        item.setId(changeLog.getId());
        item.setChangeType(changeLog.getChangeType());
        item.setChangeTypeName(getChangeTypeName(changeLog.getChangeType()));
        item.setOldStatus(changeLog.getOldStatus());
        item.setNewStatus(changeLog.getNewStatus());
        item.setOldStatusName(changeLog.getOldStatusName());
        item.setNewStatusName(changeLog.getNewStatusName());
        item.setChangeReason(changeLog.getChangeReason());
        item.setOperationUserId(changeLog.getOperationUserId());
        item.setOperationUserName(changeLog.getOperationUserName());
        item.setOperationTime(changeLog.getOperationTime());
        item.setRemark(changeLog.getRemark());
        return item;
    }

    /**
     * 获取变更类型名称
     */
    private String getChangeTypeName(String changeType) {
        switch (changeType) {
            case "create": return "创建";
            case "update": return "更新";
            case "status_change": return "状态变更";
            case "delete": return "删除";
            default: return "其他";
        }
    }

    /**
     * 转换盘点记录为响应VO
     */
    private ErpInventoryRecordRespVO convertToRecordRespVO(ErpAssetInventoryRecordDO record) {
        ErpInventoryRecordRespVO respVO = BeanUtils.toBean(record, ErpInventoryRecordRespVO.class);
        respVO.setInventoryResultName(getInventoryResultName(record.getInventoryResult()));
        respVO.setActionStatusName(getActionStatusName(record.getActionStatus()));
        return respVO;
    }

    /**
     * 获取盘点结果名称
     */
    private String getInventoryResultName(String inventoryResult) {
        if (inventoryResult == null) return "未盘点";
        switch (inventoryResult) {
            case "normal": return "正常";
            case "status_diff": return "状态差异";
            case "location_diff": return "位置差异";
            case "user_diff": return "使用人差异";
            case "not_found": return "未找到";
            default: return "其他";
        }
    }

    /**
     * 获取处理状态名称
     */
    private String getActionStatusName(String actionStatus) {
        if (actionStatus == null) return "无需处理";
        switch (actionStatus) {
            case "pending": return "待处理";
            case "processing": return "处理中";
            case "completed": return "已处理";
            case "ignored": return "已忽略";
            default: return "其他";
        }
    }



}
