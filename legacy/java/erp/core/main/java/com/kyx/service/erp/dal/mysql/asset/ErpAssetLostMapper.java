package com.kyx.service.erp.dal.mysql.asset;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.erp.controller.admin.asset.vo.lost.ErpAssetLostPageReqVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetLostDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 资产挂失 Mapper
 *
 * @author kyx
 */
@Mapper
public interface ErpAssetLostMapper extends BaseMapperX<ErpAssetLostDO> {

    default PageResult<ErpAssetLostDO> selectPage(ErpAssetLostPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpAssetLostDO>()
                .likeIfPresent(ErpAssetLostDO::getLostNo, reqVO.getLostNo())
                .eqIfPresent(ErpAssetLostDO::getLostReason, reqVO.getLostReason())
                .likeIfPresent(ErpAssetLostDO::getLostLocation, reqVO.getLostLocation())
                .eqIfPresent(ErpAssetLostDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpAssetLostDO::getApprovalStatus, reqVO.getApprovalStatus())
                .eqIfPresent(ErpAssetLostDO::getHandleUserId, reqVO.getHandleUserId())
                .eqIfPresent(ErpAssetLostDO::getHandleDeptId, reqVO.getHandleDeptId())
                .betweenIfPresent(ErpAssetLostDO::getLostDate, reqVO.getStartDate(), reqVO.getEndDate())
                .orderByDesc(ErpAssetLostDO::getId));
    }

    default ErpAssetLostDO selectByBmpProcessInstanceId(String bmpProcessInstanceId) {
        return selectOne(ErpAssetLostDO::getBmpProcessInstanceId, bmpProcessInstanceId);
    }
} 