package com.kyx.service.erp.dal.mysql.asset;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetBarcodePrintLogDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 资产条码打印日志 Mapper
 *
 * @author kyx
 */
@Mapper
public interface ErpAssetBarcodePrintLogMapper extends BaseMapperX<ErpAssetBarcodePrintLogDO> {

    default List<ErpAssetBarcodePrintLogDO> selectListByBarcodePrintId(Long barcodePrintId) {
        return selectList(ErpAssetBarcodePrintLogDO::getBarcodePrintId, barcodePrintId);
    }

    default List<ErpAssetBarcodePrintLogDO> selectListByAssetId(Long assetId) {
        return selectList(ErpAssetBarcodePrintLogDO::getAssetId, assetId);
    }

    default List<ErpAssetBarcodePrintLogDO> selectListByBarcodeNo(String barcodeNo) {
        return selectList(ErpAssetBarcodePrintLogDO::getBarcodeNo, barcodeNo);
    }

    default List<ErpAssetBarcodePrintLogDO> selectListByOperationType(Integer operationType) {
        return selectList(ErpAssetBarcodePrintLogDO::getOperationType, operationType);
    }

    default List<ErpAssetBarcodePrintLogDO> selectListByPrintUserId(Long printUserId) {
        return selectList(ErpAssetBarcodePrintLogDO::getPrintUserId, printUserId);
    }

} 