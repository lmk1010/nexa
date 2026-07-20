package com.kyx.service.erp.dal.mysql.asset;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.erp.controller.admin.asset.vo.scrapped.ErpAssetScrappedPageReqVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetScrappedDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 资产报废 Mapper
 *
 * @author kyx
 */
@Mapper
public interface ErpAssetScrappedMapper extends BaseMapperX<ErpAssetScrappedDO> {

    default PageResult<ErpAssetScrappedDO> selectPage(ErpAssetScrappedPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpAssetScrappedDO>()
                .likeIfPresent(ErpAssetScrappedDO::getScrappedNo, reqVO.getScrappedNo())
                .eqIfPresent(ErpAssetScrappedDO::getScrappedReason, reqVO.getScrappedReason())
                .eqIfPresent(ErpAssetScrappedDO::getScrappedType, reqVO.getScrappedType())
                .eqIfPresent(ErpAssetScrappedDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpAssetScrappedDO::getApprovalStatus, reqVO.getApprovalStatus())
                .eqIfPresent(ErpAssetScrappedDO::getHandleUserId, reqVO.getHandleUserId())
                .eqIfPresent(ErpAssetScrappedDO::getHandleDeptId, reqVO.getHandleDeptId())
                .betweenIfPresent(ErpAssetScrappedDO::getScrappedDate, reqVO.getStartDate(), reqVO.getEndDate())
                .orderByDesc(ErpAssetScrappedDO::getId));
    }

    default ErpAssetScrappedDO selectByBmpProcessInstanceId(String bmpProcessInstanceId) {
        return selectOne(ErpAssetScrappedDO::getBmpProcessInstanceId, bmpProcessInstanceId);
    }
} 