package com.kyx.service.erp.dal.mysql.asset;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.erp.controller.admin.asset.vo.assetreturn.ErpAssetReturnPageReqVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetReturnDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

/**
 * ERP 资产归还记录 Mapper
 *
 * @author kyx
 */
@Mapper
public interface ErpAssetReturnMapper extends BaseMapperX<ErpAssetReturnDO> {

    default PageResult<ErpAssetReturnDO> selectPage(ErpAssetReturnPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpAssetReturnDO>()
                .eqIfPresent(ErpAssetReturnDO::getCheckoutId, reqVO.getCheckoutId())
                .eqIfPresent(ErpAssetReturnDO::getAssetId, reqVO.getAssetId())
                .eqIfPresent(ErpAssetReturnDO::getReturnUserId, reqVO.getReturnUserId())
                .eqIfPresent(ErpAssetReturnDO::getReturnDeptId, reqVO.getReturnDeptId())
                .eqIfPresent(ErpAssetReturnDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpAssetReturnDO::getBmpStatus, reqVO.getBmpStatus())
                .betweenIfPresent(ErpAssetReturnDO::getReturnDate, reqVO.getReturnDate())
                .betweenIfPresent(ErpAssetReturnDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ErpAssetReturnDO::getId));
    }

    default List<ErpAssetReturnDO> selectListByAssetId(Long assetId) {
        return selectList(ErpAssetReturnDO::getAssetId, assetId);
    }

    default List<ErpAssetReturnDO> selectListByUserId(Long userId) {
        return selectList(ErpAssetReturnDO::getReturnUserId, userId);
    }

    default List<ErpAssetReturnDO> selectListByDeptId(Long deptId) {
        return selectList(ErpAssetReturnDO::getReturnDeptId, deptId);
    }

    default List<ErpAssetReturnDO> selectListByCheckoutId(Long checkoutId) {
        return selectList(ErpAssetReturnDO::getCheckoutId, checkoutId);
    }

    default List<ErpAssetReturnDO> selectListByStatus(Integer status) {
        return selectList(ErpAssetReturnDO::getStatus, status);
    }

    default Long selectCountByAssetId(Long assetId) {
        return selectCount(ErpAssetReturnDO::getAssetId, assetId);
    }

    default Long selectCountByUserId(Long userId) {
        return selectCount(ErpAssetReturnDO::getReturnUserId, userId);
    }

    default ErpAssetReturnDO selectByCheckoutId(Long checkoutId) {
        return selectOne(new LambdaQueryWrapperX<ErpAssetReturnDO>()
                .eq(ErpAssetReturnDO::getCheckoutId, checkoutId)
                .orderByDesc(ErpAssetReturnDO::getId)
                .last("LIMIT 1"));
    }

} 