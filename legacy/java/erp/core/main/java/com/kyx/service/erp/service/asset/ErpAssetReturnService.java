package com.kyx.service.erp.service.asset;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.erp.controller.admin.asset.vo.assetreturn.ErpAssetReturnPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.assetreturn.ErpAssetReturnRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.assetreturn.ErpAssetReturnSaveReqVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetReturnDO;

import javax.validation.Valid;
import java.util.List;

/**
 * ERP 资产归还记录 Service 接口
 *
 * @author kyx
 */
public interface ErpAssetReturnService {

    /**
     * 创建资产归还记录
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createReturn(@Valid ErpAssetReturnSaveReqVO createReqVO);

    /**
     * 更新资产归还记录
     *
     * @param updateReqVO 更新信息
     */
    void updateReturn(@Valid ErpAssetReturnSaveReqVO updateReqVO);

    /**
     * 删除资产归还记录
     *
     * @param id 编号
     */
    void deleteReturn(Long id);

    /**
     * 获得资产归还记录
     *
     * @param id 编号
     * @return 资产归还记录
     */
    ErpAssetReturnDO getReturn(Long id);

    /**
     * 获得资产归还记录分页
     *
     * @param pageReqVO 分页查询
     * @return 资产归还记录分页
     */
    PageResult<ErpAssetReturnRespVO> getReturnPage(ErpAssetReturnPageReqVO pageReqVO);

    /**
     * 获得资产归还记录列表
     *
     * @param assetId 资产编号
     * @return 资产归还记录列表
     */
    List<ErpAssetReturnRespVO> getReturnListByAssetId(Long assetId);

    /**
     * 获得用户归还记录列表
     *
     * @param userId 用户编号
     * @return 资产归还记录列表
     */
    List<ErpAssetReturnRespVO> getReturnListByUserId(Long userId);

    /**
     * 资产管理员接收确认归还
     *
     * @param returnId 归还记录编号
     * @param receiverRemark 接收备注
     */
    void receiveReturn(Long returnId, String receiverRemark);

} 