package com.kyx.service.erp.dal.mysql.stock;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.service.erp.dal.dataobject.stock.ErpStockOutItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * ERP 其它出库单项 Mapper
 *
 * @author MK
 */
@Mapper
public interface ErpStockOutItemMapper extends BaseMapperX<ErpStockOutItemDO> {

    default List<ErpStockOutItemDO> selectListByOutId(Long outId) {
        return selectList(ErpStockOutItemDO::getOutId, outId);
    }

    default List<ErpStockOutItemDO> selectListByOutIds(Collection<Long> outIds) {
        return selectList(ErpStockOutItemDO::getOutId, outIds);
    }

    default int deleteByOutId(Long outId) {
        return delete(ErpStockOutItemDO::getOutId, outId);
    }

}