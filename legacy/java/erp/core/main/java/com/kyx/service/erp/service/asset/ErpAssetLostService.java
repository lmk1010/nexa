package com.kyx.service.erp.service.asset;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.erp.api.asset.vo.lost.ErpAssetLostSaveReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.lost.ErpAssetLostPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.lost.ErpAssetLostRespVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetLostDO;

import javax.validation.Valid;
import java.util.List;

/**
 * ERP 资产挂失 Service 接口
 *
 * @author kyx
 */
public interface ErpAssetLostService {

    /**
     * 创建资产挂失记录
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createLost(@Valid ErpAssetLostSaveReqVO createReqVO);

    /**
     * 创建并提交资产挂失记录（发起BPM流程）
     *
     * @param userId      用户编号
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createLostAndSubmit(Long userId, @Valid ErpAssetLostSaveReqVO createReqVO);

    /**
     * 更新资产挂失记录
     *
     * @param updateReqVO 更新信息
     */
    void updateLost(@Valid ErpAssetLostSaveReqVO updateReqVO);

    /**
     * 删除资产挂失记录
     *
     * @param id 编号
     */
    void deleteLost(Long id);

    /**
     * 获得资产挂失记录
     *
     * @param id 编号
     * @return 资产挂失记录
     */
    ErpAssetLostDO getLost(Long id);

    /**
     * 获得资产挂失记录详情（包含关联数据）
     *
     * @param id 编号
     * @return 资产挂失记录详情
     */
    ErpAssetLostRespVO getLostDetail(Long id);

    /**
     * 获得资产挂失记录分页
     *
     * @param pageReqVO 分页查询
     * @return 资产挂失记录分页
     */
    PageResult<ErpAssetLostRespVO> getLostPage(ErpAssetLostPageReqVO pageReqVO);

    /**
     * 获得资产挂失记录列表, 用于 Excel 导出
     *
     * @param exportReqVO 查询条件
     * @return 资产挂失记录列表
     */
    List<ErpAssetLostRespVO> getLostList(ErpAssetLostPageReqVO exportReqVO);

    /**
     * 通过 BMP 流程实例编号获取挂失记录
     *
     * @param bmpProcessInstanceId BMP流程实例编号
     * @return 挂失记录
     */
    ErpAssetLostDO getLostByBmpProcessInstanceId(String bmpProcessInstanceId);

    /**
     * 通过 BMP 流程实例编号获取挂失记录详情（包含关联数据）
     *
     * @param bmpProcessInstanceId BMP流程实例编号
     * @return 挂失记录详情
     */
    ErpAssetLostRespVO getLostDetailByBmpProcessInstanceId(String bmpProcessInstanceId);

    /**
     * 处理BMP状态变更
     *
     * @param bmpProcessInstanceId BMP流程实例编号
     * @param bmpStatus            新的BMP状态
     */
    void handleBmpStatusChange(String bmpProcessInstanceId, Integer bmpStatus);

    /**
     * 处理找回资产
     *
     * @param id 挂失记录编号
     * @param saveReqVO 找回信息
     */
    void handleFoundAsset(Long id, ErpAssetLostSaveReqVO saveReqVO);

    /**
     * 确认资产丢失
     *
     * @param id 挂失记录编号
     * @param remark 确认备注
     */
    void confirmLostAsset(Long id, String remark);

    /**
     * 更新挂失记录的BMP状态和审批状态
     *
     * @param lostId 挂失记录编号
     * @param approvalStatus 审批状态
     * @param bmpStatus BMP状态
     */
    void updateLostBmpStatus(Long lostId, Integer approvalStatus, Integer bmpStatus);
} 