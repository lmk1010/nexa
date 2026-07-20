package com.kyx.service.erp.dal.mysql.asset;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.erp.controller.admin.asset.vo.assetinput.ErpAssetInputPageReqVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetInputDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 资产录入申请 Mapper
 *
 * @author kyx
 */
@Mapper
public interface ErpAssetInputMapper extends BaseMapperX<ErpAssetInputDO> {

    default PageResult<ErpAssetInputDO> selectPage(ErpAssetInputPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpAssetInputDO>()
                .likeIfPresent(ErpAssetInputDO::getInputNo, reqVO.getInputNo())
                .likeIfPresent(ErpAssetInputDO::getAssetNo, reqVO.getAssetNo())
                .likeIfPresent(ErpAssetInputDO::getName, reqVO.getName())
                .eqIfPresent(ErpAssetInputDO::getType, reqVO.getType())
                .eqIfPresent(ErpAssetInputDO::getCategoryId, reqVO.getCategoryId())
                .eqIfPresent(ErpAssetInputDO::getDeptId, reqVO.getDeptId())
                .eqIfPresent(ErpAssetInputDO::getSupplierId, reqVO.getSupplierId())
                .eqIfPresent(ErpAssetInputDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpAssetInputDO::getApprovalStatus, reqVO.getApprovalStatus())
                .eqIfPresent(ErpAssetInputDO::getBmpStatus, reqVO.getBmpStatus())
                .betweenIfPresent(ErpAssetInputDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ErpAssetInputDO::getId));
    }

    default ErpAssetInputDO selectByInputNo(String inputNo) {
        return selectOne(ErpAssetInputDO::getInputNo, inputNo);
    }

    default ErpAssetInputDO selectByAssetNo(String assetNo) {
        return selectOne(ErpAssetInputDO::getAssetNo, assetNo);
    }

    default ErpAssetInputDO selectByBmpProcessInstanceId(String bmpProcessInstanceId) {
        return selectOne(ErpAssetInputDO::getBmpProcessInstanceId, bmpProcessInstanceId);
    }

} 