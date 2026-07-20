package com.kyx.service.erp.dal.mysql.asset;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.erp.controller.admin.asset.vo.transfer.ErpAssetTransferPageReqVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetTransferDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 资产转移记录 Mapper
 *
 * @author kyx
 */
@Mapper
public interface ErpAssetTransferMapper extends BaseMapperX<ErpAssetTransferDO> {

    default PageResult<ErpAssetTransferDO> selectPage(ErpAssetTransferPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpAssetTransferDO>()
                .likeIfPresent(ErpAssetTransferDO::getTransferNo, reqVO.getTransferNo())
                .eqIfPresent(ErpAssetTransferDO::getAssetId, reqVO.getAssetId())
                .eqIfPresent(ErpAssetTransferDO::getFromUserId, reqVO.getFromUserId())
                .eqIfPresent(ErpAssetTransferDO::getToUserId, reqVO.getToUserId())
                .eqIfPresent(ErpAssetTransferDO::getFromDeptId, reqVO.getFromDeptId())
                .eqIfPresent(ErpAssetTransferDO::getToDeptId, reqVO.getToDeptId())
                .eqIfPresent(ErpAssetTransferDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpAssetTransferDO::getApprovalStatus, reqVO.getApprovalStatus())
                .betweenIfPresent(ErpAssetTransferDO::getTransferDate, reqVO.getTransferDateStart(), reqVO.getTransferDateEnd())
                .betweenIfPresent(ErpAssetTransferDO::getCreateTime, reqVO.getCreateTimeStart(), reqVO.getCreateTimeEnd())
                .orderByDesc(ErpAssetTransferDO::getId));
    }

    default ErpAssetTransferDO selectByTransferNo(String transferNo) {
        return selectOne(ErpAssetTransferDO::getTransferNo, transferNo);
    }

    default List<ErpAssetTransferDO> selectListByAssetId(Long assetId) {
        return selectList(ErpAssetTransferDO::getAssetId, assetId);
    }

    default List<ErpAssetTransferDO> selectListByFromUserId(Long fromUserId) {
        return selectList(ErpAssetTransferDO::getFromUserId, fromUserId);
    }

    default List<ErpAssetTransferDO> selectListByToUserId(Long toUserId) {
        return selectList(ErpAssetTransferDO::getToUserId, toUserId);
    }

    default List<ErpAssetTransferDO> selectListByStatus(Integer status) {
        return selectList(ErpAssetTransferDO::getStatus, status);
    }

    default List<ErpAssetTransferDO> selectListByApprovalStatus(Integer approvalStatus) {
        return selectList(ErpAssetTransferDO::getApprovalStatus, approvalStatus);
    }

} 