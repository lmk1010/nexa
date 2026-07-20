package com.kyx.service.biz.dal.mysql.hotel;

import cn.hutool.core.util.StrUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.biz.controller.admin.hotel.vo.HotelWorkOrderPageReqVO;
import com.kyx.service.biz.dal.dataobject.hotel.HotelWorkOrderDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Date;

@Mapper
public interface HotelWorkOrderMapper extends BaseMapperX<HotelWorkOrderDO> {
    default PageResult<HotelWorkOrderDO> selectPage(HotelWorkOrderPageReqVO reqVO) {
        LambdaQueryWrapperX<HotelWorkOrderDO> wrapper = new LambdaQueryWrapperX<HotelWorkOrderDO>()
                .eqIfPresent(HotelWorkOrderDO::getStore, reqVO.getStore())
                .eqIfPresent(HotelWorkOrderDO::getStatus, reqVO.getStatus())
                .eqIfPresent(HotelWorkOrderDO::getAssigneeUserId, reqVO.getAssigneeUserId())
                .eqIfPresent(HotelWorkOrderDO::getCreatorUserId, reqVO.getCreatorUserId())
                .betweenIfPresent(HotelWorkOrderDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(HotelWorkOrderDO::getCreateTime);
        if (StrUtil.isNotBlank(reqVO.getType())) wrapper.eq(HotelWorkOrderDO::getType, reqVO.getType());
        if (StrUtil.isNotBlank(reqVO.getKeyword())) {
            String keyword = reqVO.getKeyword().trim();
            wrapper.and(query -> query.like(HotelWorkOrderDO::getTitle, keyword)
                    .or().like(HotelWorkOrderDO::getContent, keyword)
                    .or().like(HotelWorkOrderDO::getRoomNo, keyword)
                    .or().like(HotelWorkOrderDO::getType, keyword)
                    .or().like(HotelWorkOrderDO::getPriority, keyword)
                    .or().like(HotelWorkOrderDO::getCustomerEmotion, keyword)
                    .or().like(HotelWorkOrderDO::getAssigneeName, keyword)
                    .or().like(HotelWorkOrderDO::getCreatorName, keyword)
                    .or().like(HotelWorkOrderDO::getSourceRecordTitle, keyword));
        }
        return selectPage(reqVO, wrapper);
    }

    default Long countOpenByAssignee(Long userId) {
        LambdaQueryWrapperX<HotelWorkOrderDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(HotelWorkOrderDO::getAssigneeUserId, userId);
        wrapper.in(HotelWorkOrderDO::getStatus, 0, 1);
        return selectCount(wrapper);
    }

    default Long countOpenByStore(String store) {
        LambdaQueryWrapperX<HotelWorkOrderDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.in(HotelWorkOrderDO::getStatus, 0, 1);
        if (StrUtil.isNotBlank(store)) wrapper.eq(HotelWorkOrderDO::getStore, store);
        return selectCount(wrapper);
    }
}
