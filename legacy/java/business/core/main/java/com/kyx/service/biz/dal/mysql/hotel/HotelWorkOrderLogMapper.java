package com.kyx.service.biz.dal.mysql.hotel;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.biz.dal.dataobject.hotel.HotelWorkOrderLogDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface HotelWorkOrderLogMapper extends BaseMapperX<HotelWorkOrderLogDO> {
    default List<HotelWorkOrderLogDO> selectListByOrderId(Long orderId) {
        return selectList(new LambdaQueryWrapperX<HotelWorkOrderLogDO>()
                .eq(HotelWorkOrderLogDO::getOrderId, orderId)
                .orderByAsc(HotelWorkOrderLogDO::getCreateTime));
    }
}
