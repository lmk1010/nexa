package com.kyx.service.erp.dal.mysql.asset;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetInventoryRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 资产盘点记录 Mapper
 *
 * @author kyx
 */
@Mapper
public interface ErpAssetInventoryRecordMapper extends BaseMapperX<ErpAssetInventoryRecordDO> {

    /**
     * 根据盘点计划ID获取盘点记录列表
     *
     * @param planId 盘点计划ID
     * @return 盘点记录列表
     */
    default List<ErpAssetInventoryRecordDO> selectListByPlanId(Long planId) {
        return selectList(new LambdaQueryWrapperX<ErpAssetInventoryRecordDO>()
                .eq(ErpAssetInventoryRecordDO::getPlanId, planId)
                .orderByDesc(ErpAssetInventoryRecordDO::getInventoryTime));
    }

    /**
     * 根据资产编码和盘点计划ID查询盘点记录
     *
     * @param assetCode 资产编码
     * @param planId    盘点计划ID
     * @return 盘点记录
     */
    default ErpAssetInventoryRecordDO selectByAssetCodeAndPlanId(String assetCode, Long planId) {
        return selectOne(new LambdaQueryWrapperX<ErpAssetInventoryRecordDO>()
                .eq(ErpAssetInventoryRecordDO::getAssetCode, assetCode)
                .eq(ErpAssetInventoryRecordDO::getPlanId, planId));
    }

    /**
     * 根据资产ID和盘点计划ID查询盘点记录
     *
     * @param assetId 资产ID
     * @param planId  盘点计划ID
     * @return 盘点记录
     */
    default ErpAssetInventoryRecordDO selectByAssetIdAndPlanId(Long assetId, Long planId) {
        return selectOne(new LambdaQueryWrapperX<ErpAssetInventoryRecordDO>()
                .eq(ErpAssetInventoryRecordDO::getAssetId, assetId)
                .eq(ErpAssetInventoryRecordDO::getPlanId, planId));
    }

    /**
     * 根据盘点计划ID统计已盘点数量
     *
     * @param planId 盘点计划ID
     * @return 已盘点数量
     */
    default Long countByPlanId(Long planId) {
        return selectCount(new LambdaQueryWrapperX<ErpAssetInventoryRecordDO>()
                .eq(ErpAssetInventoryRecordDO::getPlanId, planId)
                .isNotNull(ErpAssetInventoryRecordDO::getInventoryTime));
    }

    /**
     * 根据盘点计划ID和盘点结果统计数量
     *
     * @param planId          盘点计划ID
     * @param inventoryResult 盘点结果
     * @return 统计数量
     */
    default Long countByPlanIdAndResult(Long planId, String inventoryResult) {
        return selectCount(new LambdaQueryWrapperX<ErpAssetInventoryRecordDO>()
                .eq(ErpAssetInventoryRecordDO::getPlanId, planId)
                .eq(ErpAssetInventoryRecordDO::getInventoryResult, inventoryResult));
    }

} 