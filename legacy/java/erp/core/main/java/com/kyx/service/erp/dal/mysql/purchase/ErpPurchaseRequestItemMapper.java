package com.kyx.service.erp.dal.mysql.purchase;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.service.erp.dal.dataobject.purchase.ErpPurchaseRequestItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 采购申请明细 Mapper
 *
 * @author MK
 */
@Mapper
public interface ErpPurchaseRequestItemMapper extends BaseMapperX<ErpPurchaseRequestItemDO> {

    default List<ErpPurchaseRequestItemDO> selectListByRequestId(Long requestId) {
        return selectList(ErpPurchaseRequestItemDO::getRequestId, requestId);
    }

    default int deleteByRequestId(Long requestId) {
        return delete(ErpPurchaseRequestItemDO::getRequestId, requestId);
    }

} 