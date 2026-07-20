package com.kyx.service.erp.dal.mysql.asset;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetRedistributionItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 资产调拨项 Mapper
 *
 * @author kyx
 */
@Mapper
public interface ErpAssetRedistributionItemMapper extends BaseMapperX<ErpAssetRedistributionItemDO> {

    default List<ErpAssetRedistributionItemDO> selectListByRedistributionId(Long redistributionId) {
        return selectList(ErpAssetRedistributionItemDO::getRedistributionId, redistributionId);
    }

    default void deleteByRedistributionId(Long redistributionId) {
        delete(ErpAssetRedistributionItemDO::getRedistributionId, redistributionId);
    }

    default List<ErpAssetRedistributionItemDO> selectListByAssetId(Long assetId) {
        return selectList(ErpAssetRedistributionItemDO::getAssetId, assetId);
    }
} 