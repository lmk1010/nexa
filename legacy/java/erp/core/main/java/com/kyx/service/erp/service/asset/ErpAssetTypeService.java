package com.kyx.service.erp.service.asset;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.erp.controller.admin.asset.vo.type.ErpAssetTypePageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.type.ErpAssetTypeRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.type.ErpAssetTypeSaveReqVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetTypeDO;
import javax.validation.Valid;

import java.util.Collection;
import java.util.List;

/**
 * ERP 资产类型 Service 接口
 *
 * @author kyx
 */
public interface ErpAssetTypeService {

    /**
     * 创建资产类型
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createAssetType(@Valid ErpAssetTypeSaveReqVO createReqVO);

    /**
     * 更新资产类型
     *
     * @param updateReqVO 更新信息
     */
    void updateAssetType(@Valid ErpAssetTypeSaveReqVO updateReqVO);

    /**
     * 删除资产类型
     *
     * @param id 编号
     */
    void deleteAssetType(Long id);

    /**
     * 获得资产类型
     *
     * @param id 编号
     * @return 资产类型
     */
    ErpAssetTypeDO getAssetType(Long id);

    /**
     * 校验资产类型
     *
     * @param id 编号
     * @return 资产类型
     */
    ErpAssetTypeDO validateAssetType(Long id);

    /**
     * 获得指定状态的资产类型列表
     *
     * @param status 状态
     * @return 资产类型列表
     */
    List<ErpAssetTypeDO> getAssetTypeListByStatus(Integer status);

    /**
     * 获得资产类型列表
     *
     * @param ids 编号数组
     * @return 资产类型列表
     */
    List<ErpAssetTypeDO> getAssetTypeList(Collection<Long> ids);

    /**
     * 获得资产类型 VO 分页
     *
     * @param pageReqVO 分页查询
     * @return 资产类型分页
     */
    PageResult<ErpAssetTypeRespVO> getAssetTypeVOPage(ErpAssetTypePageReqVO pageReqVO);

    /**
     * 获得资产类型 VO 分页（IPage真分页）
     *
     * @param pageReqVO 分页查询
     * @return 资产类型分页
     */
    IPage<ErpAssetTypeRespVO> getAssetTypeVOPageWithIPage(ErpAssetTypePageReqVO pageReqVO);

    /**
     * 获得资产类型 VO 列表
     *
     * @return 资产类型列表
     */
    List<ErpAssetTypeRespVO> getAssetTypeVOList();

} 