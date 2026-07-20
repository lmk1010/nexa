package com.kyx.service.erp.dal.mysql.asset;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetOwnershipDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 资产所有权关系 Mapper
 *
 * @author kyx
 */
@Mapper
public interface ErpAssetOwnershipMapper extends BaseMapperX<ErpAssetOwnershipDO> {

    default List<ErpAssetOwnershipDO> selectListByAssetId(Long assetId) {
        return selectList(ErpAssetOwnershipDO::getAssetId, assetId);
    }

    default List<ErpAssetOwnershipDO> selectListByUserId(Long userId) {
        return selectList(ErpAssetOwnershipDO::getCurrentUserId, userId);
    }

    default List<ErpAssetOwnershipDO> selectListByDeptId(Long deptId) {
        return selectList(ErpAssetOwnershipDO::getCurrentDeptId, deptId);
    }

    default List<ErpAssetOwnershipDO> selectListByStatus(Integer status) {
        return selectList(ErpAssetOwnershipDO::getStatus, status);
    }

    default ErpAssetOwnershipDO selectCurrentOwnership(Long assetId) {
        // 简化查询：由于归还时直接删除记录，所以存在记录就表示正在使用中
        return selectOne(new LambdaQueryWrapperX<ErpAssetOwnershipDO>()
                .eq(ErpAssetOwnershipDO::getAssetId, assetId));
    }

    default List<ErpAssetOwnershipDO> selectActiveOwnershipsByUserId(Long userId) {
        // 简化查询：由于归还时直接删除记录，所以存在记录就表示正在使用中
        return selectList(new LambdaQueryWrapperX<ErpAssetOwnershipDO>()
                .eq(ErpAssetOwnershipDO::getCurrentUserId, userId));
    }

    default List<ErpAssetOwnershipDO> selectActiveOwnershipsByDeptId(Long deptId) {
        // 简化查询：由于归还时直接删除记录，所以存在记录就表示正在使用中
        return selectList(new LambdaQueryWrapperX<ErpAssetOwnershipDO>()
                .eq(ErpAssetOwnershipDO::getCurrentDeptId, deptId));
    }

    default Long selectCountByUserId(Long userId) {
        // 简化查询：由于归还时直接删除记录，所以存在记录就表示正在使用中
        return selectCount(new LambdaQueryWrapperX<ErpAssetOwnershipDO>()
                .eq(ErpAssetOwnershipDO::getCurrentUserId, userId));
    }

    default Long selectCountByDeptId(Long deptId) {
        // 简化查询：由于归还时直接删除记录，所以存在记录就表示正在使用中
        return selectCount(new LambdaQueryWrapperX<ErpAssetOwnershipDO>()
                .eq(ErpAssetOwnershipDO::getCurrentDeptId, deptId));
    }

} 