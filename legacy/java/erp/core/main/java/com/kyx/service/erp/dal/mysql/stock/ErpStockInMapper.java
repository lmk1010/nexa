package com.kyx.service.erp.dal.mysql.stock;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.MPJLambdaWrapperX;
import com.kyx.service.erp.controller.admin.stock.vo.in.ErpStockInPageReqVO;
import com.kyx.service.erp.dal.dataobject.stock.ErpStockInDO;
import com.kyx.service.erp.dal.dataobject.stock.ErpStockInItemDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 其它入库单 Mapper
 *
 * @author MK
 */
@Mapper
public interface ErpStockInMapper extends BaseMapperX<ErpStockInDO> {

    default PageResult<ErpStockInDO> selectPage(ErpStockInPageReqVO reqVO) {
        MPJLambdaWrapperX<ErpStockInDO> query = new MPJLambdaWrapperX<ErpStockInDO>()
                .likeIfPresent(ErpStockInDO::getNo, reqVO.getNo())
                .eqIfPresent(ErpStockInDO::getSupplierId, reqVO.getSupplierId())
                .betweenIfPresent(ErpStockInDO::getInTime, reqVO.getInTime())
                .eqIfPresent(ErpStockInDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpStockInDO::getRemark, reqVO.getRemark())
                .eqIfPresent(ErpStockInDO::getCreator, reqVO.getCreator())
                .orderByDesc(ErpStockInDO::getId);
        if (reqVO.getWarehouseId() != null || reqVO.getProductId() != null) {
            query.leftJoin(ErpStockInItemDO.class, ErpStockInItemDO::getInId, ErpStockInDO::getId)
                    .eq(reqVO.getWarehouseId() != null, ErpStockInItemDO::getWarehouseId, reqVO.getWarehouseId())
                    .eq(reqVO.getProductId() != null, ErpStockInItemDO::getProductId, reqVO.getProductId())
                    .groupBy(ErpStockInDO::getId); // 避免 1 对多查询，产生相同的 1
        }
        return selectJoinPage(reqVO, ErpStockInDO.class, query);
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpStockInDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpStockInDO>()
                .eq(ErpStockInDO::getId, id).eq(ErpStockInDO::getStatus, status));
    }

    default ErpStockInDO selectByNo(String no) {
        return selectOne(ErpStockInDO::getNo, no);
    }

}