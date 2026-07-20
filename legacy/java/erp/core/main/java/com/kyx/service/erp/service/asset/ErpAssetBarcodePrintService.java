package com.kyx.service.erp.service.asset;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.erp.controller.admin.asset.vo.barcode.*;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetBarcodePrintDO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

/**
 * ERP 资产条码打印 Service 接口
 *
 * @author kyx
 */
public interface ErpAssetBarcodePrintService {

    /**
     * 创建资产条码打印记录
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createAssetBarcodePrint(@Valid ErpAssetBarcodePrintSaveReqVO createReqVO);

    /**
     * 更新资产条码打印记录
     *
     * @param updateReqVO 更新信息
     */
    void updateAssetBarcodePrint(@Valid ErpAssetBarcodePrintSaveReqVO updateReqVO);

    /**
     * 删除资产条码打印记录
     *
     * @param id 编号
     */
    void deleteAssetBarcodePrint(Long id);

    /**
     * 获得资产条码打印记录
     *
     * @param id 编号
     * @return 资产条码打印记录
     */
    ErpAssetBarcodePrintDO getAssetBarcodePrint(Long id);

    /**
     * 获得资产条码打印记录列表
     *
     * @param ids 编号数组
     * @return 资产条码打印记录列表
     */
    List<ErpAssetBarcodePrintDO> getAssetBarcodePrintList(Collection<Long> ids);

    /**
     * 获得资产条码打印记录分页
     *
     * @param pageReqVO 分页查询
     * @return 资产条码打印记录分页
     */
    PageResult<ErpAssetBarcodePrintRespVO> getAssetBarcodePrintPage(ErpAssetBarcodePrintPageReqVO pageReqVO);

    /**
     * 批量生成资产条码
     *
     * @param generateReqVO 生成请求
     * @return 生成的条码记录数量
     */
    int generateAssetBarcodes(@Valid ErpAssetBarcodeGenerateReqVO generateReqVO);

    /**
     * 批量打印资产条码
     *
     * @param printReqVO 打印请求
     * @return 打印结果
     */
    boolean printAssetBarcodes(@Valid ErpAssetBarcodePrintReqVO printReqVO);

    /**
     * 根据资产编号获取条码打印记录
     *
     * @param assetId 资产编号
     * @return 条码打印记录列表
     */
    List<ErpAssetBarcodePrintDO> getAssetBarcodePrintListByAssetId(Long assetId);

    /**
     * 根据条码编号获取条码打印记录
     *
     * @param barcodeNo 条码编号
     * @return 条码打印记录
     */
    ErpAssetBarcodePrintDO getAssetBarcodePrintByBarcodeNo(String barcodeNo);

    /**
     * 获得资产条码打印记录 VO 分页
     *
     * @param pageReqVO 分页查询
     * @return 资产条码打印记录分页
     */
    PageResult<ErpAssetBarcodePrintRespVO> getAssetBarcodePrintVOPage(ErpAssetBarcodePrintPageReqVO pageReqVO);

} 