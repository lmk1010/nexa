package com.kyx.service.erp.service.asset;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.erp.controller.admin.asset.vo.asset.ErpAssetPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.asset.ErpAssetRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.asset.ErpAssetSaveReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.asset.ErpAssetBatchSaveReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.asset.ErpAssetBatchSaveRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.asset.ErpAssetLogRespVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetDO;
import javax.validation.Valid;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.kyx.foundation.common.util.collection.CollectionUtils.convertMap;

/**
 * ERP 资产 Service 接口
 *
 * @author kyx
 */
public interface ErpAssetService {

    /**
     * 创建资产
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createAsset(@Valid ErpAssetSaveReqVO createReqVO);

    /**
     * 批量创建资产
     *
     * @param batchSaveReqVO 批量创建信息
     * @return 批量创建结果
     */
    ErpAssetBatchSaveRespVO batchCreateAssets(@Valid ErpAssetBatchSaveReqVO batchSaveReqVO);

    /**
     * 更新资产
     *
     * @param updateReqVO 更新信息
     */
    void updateAsset(@Valid ErpAssetSaveReqVO updateReqVO);

    /**
     * 删除资产
     *
     * @param id 编号
     */
    void deleteAsset(Long id);

    /**
     * 校验资产们的有效性
     *
     * @param ids 编号数组
     * @return 资产列表
     */
    List<ErpAssetDO> validAssetList(Collection<Long> ids);

    /**
     * 获得资产
     *
     * @param id 编号
     * @return 资产
     */
    ErpAssetDO getAsset(Long id);

    /**
     * 获得指定状态的资产 VO 列表
     *
     * @param status 状态
     * @return 资产 VO 列表
     */
    List<ErpAssetRespVO> getAssetVOListByStatus(Integer status);

    /**
     * 获得资产 VO 列表
     *
     * @param ids 编号数组
     * @return 资产 VO 列表
     */
    List<ErpAssetRespVO> getAssetVOList(Collection<Long> ids);

    /**
     * 获得资产 VO Map
     *
     * @param ids 编号数组
     * @return 资产 VO Map
     */
    default Map<Long, ErpAssetRespVO> getAssetVOMap(Collection<Long> ids) {
        return convertMap(getAssetVOList(ids), ErpAssetRespVO::getId);
    }

    /**
     * 获得资产 VO 分页
     *
     * @param pageReqVO 分页查询
     * @return 资产分页
     */
    PageResult<ErpAssetRespVO> getAssetVOPage(ErpAssetPageReqVO pageReqVO);

    /**
     * 基于资产分类编号，获得资产数量
     *
     * @param categoryId 资产分类编号
     * @return 资产数量
     */
    Long getAssetCountByCategoryId(Long categoryId);

    /**
     * 基于部门编号，获得资产数量
     *
     * @param deptId 部门编号
     * @return 资产数量
     */
    Long getAssetCountByDeptId(Long deptId);

    /**
     * 获取资产流转记录
     *
     * @param assetId 资产编号
     * @return 资产流转记录列表
     */
    List<ErpAssetLogRespVO> getAssetLogs(Long assetId);

} 