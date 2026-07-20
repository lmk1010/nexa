package com.kyx.service.erp.service.asset;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.erp.controller.admin.asset.vo.inventory.ErpInventoryPlanPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.inventory.ErpInventoryPlanRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.inventory.ErpInventoryPlanSaveReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.inventory.ErpAssetInventoryReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.inventory.ErpAssetInventoryRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.inventory.ErpValidateScanReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.inventory.ErpValidateScanRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.inventory.ErpConfirmInventoryReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.inventory.ErpInventoryPlanProgressRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.inventory.ErpInventoryRecordPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.inventory.ErpInventoryRecordRespVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpInventoryPlanDO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetInventoryRecordDO;
import com.kyx.foundation.common.pojo.PageResult;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

/**
 * ERP 盘点计划 Service 接口
 *
 * @author kyx
 */
public interface ErpInventoryPlanService {

    /**
     * 创建盘点计划
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createInventoryPlan(@Valid ErpInventoryPlanSaveReqVO createReqVO);

    /**
     * 更新盘点计划
     *
     * @param updateReqVO 更新信息
     */
    void updateInventoryPlan(@Valid ErpInventoryPlanSaveReqVO updateReqVO);

    /**
     * 删除盘点计划
     *
     * @param id 编号
     */
    void deleteInventoryPlan(Long id);

    /**
     * 获得盘点计划
     *
     * @param id 编号
     * @return 盘点计划
     */
    ErpInventoryPlanDO getInventoryPlan(Long id);

    /**
     * 获得盘点计划分页
     *
     * @param pageReqVO 分页查询
     * @return 盘点计划分页
     */
    PageResult<ErpInventoryPlanDO> getInventoryPlanPage(ErpInventoryPlanPageReqVO pageReqVO);

    /**
     * 获得盘点计划列表，用于 Excel 导出
     *
     * @param pageReqVO 查询条件
     * @return 盘点计划列表
     */
    List<ErpInventoryPlanDO> getInventoryPlanList(ErpInventoryPlanPageReqVO pageReqVO);

    /**
     * 根据状态获取盘点计划列表
     *
     * @param status 状态
     * @return 盘点计划列表
     */
    List<ErpInventoryPlanDO> getInventoryPlanListByStatus(Integer status);

    /**
     * 根据负责人获取盘点计划列表
     *
     * @param responsiblePersonId 负责人ID
     * @return 盘点计划列表
     */
    List<ErpInventoryPlanDO> getInventoryPlanListByResponsiblePerson(Long responsiblePersonId);

    /**
     * 根据计划编号获取盘点计划
     *
     * @param planNo 计划编号
     * @return 盘点计划
     */
    ErpInventoryPlanDO getInventoryPlanByPlanNo(String planNo);

    /**
     * 提交审批
     *
     * @param id 编号
     */
    void submitApproval(Long id);

    /**
     * 审批盘点计划
     *
     * @param id     编号
     * @param passed 是否通过
     * @param remark 审批备注
     */
    void approveInventoryPlan(Long id, Boolean passed, String remark);

    /**
     * 开始执行盘点计划
     *
     * @param id 编号
     */
    void startInventoryPlan(Long id);

    /**
     * 完成盘点计划
     *
     * @param id 编号
     */
    void completeInventoryPlan(Long id);

    /**
     * 取消盘点计划
     *
     * @param id 编号
     */
    void cancelInventoryPlan(Long id);

    /**
     * 更新盘点进度
     *
     * @param id                  编号
     * @param completedAssetCount 已完成数量
     */
    void updateInventoryProgress(Long id, Integer completedAssetCount);

    /**
     * 获取正在执行的盘点计划列表
     *
     * @return 盘点计划列表
     */
    List<ErpInventoryPlanDO> getActiveExecutionPlans();

    /**
     * 根据抽样配置生成待盘点资产列表
     *
     * @param planId 计划ID
     * @return 资产ID列表
     */
    List<Long> generateInventoryAssetList(Long planId);

    /**
     * 校验盘点计划是否存在
     *
     * @param id 编号
     * @return 盘点计划
     */
    ErpInventoryPlanDO validateInventoryPlanExists(Long id);

    /**
     * 扫描资产进行盘点
     *
     * @param reqVO 盘点请求信息
     * @return 盘点结果
     */
    ErpAssetInventoryRespVO scanAssetInventory(@Valid ErpAssetInventoryReqVO reqVO);

    /**
     * 验证扫码内容
     *
     * @param reqVO 验证扫码请求信息
     * @return 验证结果（包含资产基础信息，状态为pending）
     */
    ErpValidateScanRespVO validateScanCode(@Valid ErpValidateScanReqVO reqVO);

    /**
     * 确认盘点结果
     *
     * @param reqVO 确认盘点请求信息
     * @return 盘点结果
     */
    ErpAssetInventoryRespVO confirmInventory(@Valid ErpConfirmInventoryReqVO reqVO);

    /**
     * 发布盘点计划
     *
     * @param id 编号
     */
    void publishInventoryPlan(Long id);

    /**
     * 提交盘点计划（走工作流）
     *
     * @param id     编号
     * @param reason 提交原因
     */
    void submitInventoryPlan(Long id, String reason);

    /**
     * 审核盘点计划
     *
     * @param id     编号
     * @param passed 是否通过
     * @param remark 审核备注
     */
    void auditInventoryPlan(Long id, Boolean passed, String remark);

    /**
     * 根据BPM流程状态更新盘点计划审核结果
     *
     * @param id             盘点计划ID
     * @param bpmStatus      BPM流程状态
     * @param approvalUserId 审批操作人ID
     */
    void updateInventoryPlanBpmStatus(Long id, Integer bpmStatus, Long approvalUserId);

    /**
     * 获取盘点执行进度
     *
     * @param planId 盘点计划ID
     * @return 盘点执行进度信息
     */
    ErpInventoryPlanProgressRespVO getInventoryPlanProgress(Long planId);

    /**
     * 获取盘点记录分页
     *
     * @param pageReqVO 分页查询条件
     * @return 盘点记录分页
     */
    PageResult<ErpInventoryRecordRespVO> getInventoryRecordPage(ErpInventoryRecordPageReqVO pageReqVO);

    /**
     * 获取盘点记录列表
     *
     * @param planId 盘点计划ID
     * @return 盘点记录列表
     */
    List<ErpInventoryRecordRespVO> getInventoryRecordList(Long planId);

    /**
     * 获取异常盘点记录列表
     *
     * @param planId 盘点计划ID
     * @return 异常盘点记录列表
     */
    List<ErpInventoryRecordRespVO> getAbnormalInventoryRecordList(Long planId);

}
