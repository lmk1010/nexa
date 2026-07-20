package com.kyx.service.erp.dal.mysql.asset;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.erp.controller.admin.asset.vo.checkout.ErpAssetCheckoutPageReqVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetCheckoutDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

/**
 * ERP 资产领用记录 Mapper
 *
 * @author kyx
 */
@Mapper
public interface ErpAssetCheckoutMapper extends BaseMapperX<ErpAssetCheckoutDO> {

    default PageResult<ErpAssetCheckoutDO> selectPage(ErpAssetCheckoutPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpAssetCheckoutDO>()
                .eqIfPresent(ErpAssetCheckoutDO::getAssetId, reqVO.getAssetId())
                .eqIfPresent(ErpAssetCheckoutDO::getCheckoutUserId, reqVO.getCheckoutUserId())
                .eqIfPresent(ErpAssetCheckoutDO::getCheckoutDeptId, reqVO.getCheckoutDeptId())
                .eqIfPresent(ErpAssetCheckoutDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpAssetCheckoutDO::getApprovalStatus, reqVO.getApprovalStatus())
                .eqIfPresent(ErpAssetCheckoutDO::getBmpStatus, reqVO.getBmpStatus())
                .betweenIfPresent(ErpAssetCheckoutDO::getCheckoutDate, reqVO.getCheckoutDate())
                .betweenIfPresent(ErpAssetCheckoutDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ErpAssetCheckoutDO::getId));
    }

    default List<ErpAssetCheckoutDO> selectListByAssetId(Long assetId) {
        return selectList(ErpAssetCheckoutDO::getAssetId, assetId);
    }

    default List<ErpAssetCheckoutDO> selectListByUserId(Long userId) {
        return selectList(ErpAssetCheckoutDO::getCheckoutUserId, userId);
    }

    default List<ErpAssetCheckoutDO> selectListByDeptId(Long deptId) {
        return selectList(ErpAssetCheckoutDO::getCheckoutDeptId, deptId);
    }

    default List<ErpAssetCheckoutDO> selectListByStatus(Integer status) {
        return selectList(ErpAssetCheckoutDO::getStatus, status);
    }

    default List<ErpAssetCheckoutDO> selectListByApprovalStatus(Integer approvalStatus) {
        return selectList(ErpAssetCheckoutDO::getApprovalStatus, approvalStatus);
    }

    default Long selectCountByAssetId(Long assetId) {
        return selectCount(ErpAssetCheckoutDO::getAssetId, assetId);
    }

    default Long selectCountByUserId(Long userId) {
        return selectCount(ErpAssetCheckoutDO::getCheckoutUserId, userId);
    }

    default ErpAssetCheckoutDO selectByAssetIdAndStatus(Long assetId, Integer status) {
        return selectOne(new LambdaQueryWrapperX<ErpAssetCheckoutDO>()
                .eq(ErpAssetCheckoutDO::getAssetId, assetId)
                .eq(ErpAssetCheckoutDO::getStatus, status));
    }

    default List<ErpAssetCheckoutDO> selectOverdueList(LocalDate currentDate) {
        return selectList(new LambdaQueryWrapperX<ErpAssetCheckoutDO>()
                .eq(ErpAssetCheckoutDO::getStatus, 1) // 领用中
                .isNotNull(ErpAssetCheckoutDO::getExpectedReturnDate)
                .lt(ErpAssetCheckoutDO::getExpectedReturnDate, currentDate));
    }

} 