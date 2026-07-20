package com.kyx.service.erp.dal.mysql.asset;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.erp.controller.admin.asset.vo.barcode.ErpAssetBarcodePrintPageReqVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetBarcodePrintDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 资产条码打印 Mapper
 *
 * @author kyx
 */
@Mapper
public interface ErpAssetBarcodePrintMapper extends BaseMapperX<ErpAssetBarcodePrintDO> {

    default PageResult<ErpAssetBarcodePrintDO> selectPage(ErpAssetBarcodePrintPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpAssetBarcodePrintDO>()
                .likeIfPresent(ErpAssetBarcodePrintDO::getAssetNo, reqVO.getAssetNo())
                .likeIfPresent(ErpAssetBarcodePrintDO::getBarcodeNo, reqVO.getBarcodeNo())
                .likeIfPresent(ErpAssetBarcodePrintDO::getPrintSerialNo, reqVO.getPrintSerialNo())
                .eqIfPresent(ErpAssetBarcodePrintDO::getBarcodeType, reqVO.getBarcodeType())
                .eqIfPresent(ErpAssetBarcodePrintDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(ErpAssetBarcodePrintDO::getIssueDate, reqVO.getIssueDate())
                .betweenIfPresent(ErpAssetBarcodePrintDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ErpAssetBarcodePrintDO::getId));
    }

    default List<ErpAssetBarcodePrintDO> selectListByAssetId(Long assetId) {
        return selectList(ErpAssetBarcodePrintDO::getAssetId, assetId);
    }

    default List<ErpAssetBarcodePrintDO> selectListByAssetIds(List<Long> assetIds) {
        return selectList(ErpAssetBarcodePrintDO::getAssetId, assetIds);
    }

    default ErpAssetBarcodePrintDO selectByBarcodeNo(String barcodeNo) {
        return selectOne(ErpAssetBarcodePrintDO::getBarcodeNo, barcodeNo);
    }

    default List<ErpAssetBarcodePrintDO> selectListByStatus(Integer status) {
        return selectList(ErpAssetBarcodePrintDO::getStatus, status);
    }

    default Long selectCountByAssetId(Long assetId) {
        return selectCount(ErpAssetBarcodePrintDO::getAssetId, assetId);
    }

} 