package com.kyx.service.erp.dal.mysql.asset;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.erp.controller.admin.asset.vo.redistribution.ErpAssetRedistributionPageReqVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetRedistributionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 资产调拨记录 Mapper
 *
 * @author kyx
 */
@Mapper
public interface ErpAssetRedistributionMapper extends BaseMapperX<ErpAssetRedistributionDO> {

    default PageResult<ErpAssetRedistributionDO> selectPage(ErpAssetRedistributionPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpAssetRedistributionDO>()
                .likeIfPresent(ErpAssetRedistributionDO::getRedistributionNo, reqVO.getRedistributionNo())
                .eqIfPresent(ErpAssetRedistributionDO::getFromDeptId, reqVO.getFromDeptId())
                .eqIfPresent(ErpAssetRedistributionDO::getToDeptId, reqVO.getToDeptId())
                .eqIfPresent(ErpAssetRedistributionDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpAssetRedistributionDO::getApprovalStatus, reqVO.getApprovalStatus())
                .betweenIfPresent(ErpAssetRedistributionDO::getAllocationDate, reqVO.getAllocationDateStart(), reqVO.getAllocationDateEnd())
                .betweenIfPresent(ErpAssetRedistributionDO::getCreateTime, reqVO.getCreateTimeStart(), reqVO.getCreateTimeEnd())
                .orderByDesc(ErpAssetRedistributionDO::getId));
    }

    default ErpAssetRedistributionDO selectByRedistributionNo(String redistributionNo) {
        return selectOne(ErpAssetRedistributionDO::getRedistributionNo, redistributionNo);
    }

    default List<ErpAssetRedistributionDO> selectListByFromDeptId(Long fromDeptId) {
        return selectList(ErpAssetRedistributionDO::getFromDeptId, fromDeptId);
    }

    default List<ErpAssetRedistributionDO> selectListByToDeptId(Long toDeptId) {
        return selectList(ErpAssetRedistributionDO::getToDeptId, toDeptId);
    }
} 