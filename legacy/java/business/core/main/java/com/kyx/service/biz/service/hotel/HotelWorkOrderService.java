package com.kyx.service.biz.service.hotel;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.biz.controller.admin.hotel.vo.HotelDashboardRespVO;
import com.kyx.service.biz.controller.admin.hotel.vo.HotelPermissionRespVO;
import com.kyx.service.biz.controller.admin.hotel.vo.HotelWorkOrderPageReqVO;
import com.kyx.service.biz.controller.admin.hotel.vo.HotelWorkOrderRespVO;
import com.kyx.service.biz.controller.admin.hotel.vo.HotelWorkOrderSaveReqVO;
import com.kyx.service.biz.controller.admin.hotel.vo.HotelWorkOrderStatusReqVO;

public interface HotelWorkOrderService {
    Long create(HotelWorkOrderSaveReqVO reqVO, Long userId);
    PageResult<HotelWorkOrderRespVO> page(HotelWorkOrderPageReqVO reqVO, Long userId, boolean mine);
    HotelWorkOrderRespVO get(Long id, Long userId);
    void update(HotelWorkOrderSaveReqVO reqVO, Long userId);
    void delete(Long id, Long userId);
    void updateStatus(HotelWorkOrderStatusReqVO reqVO, Long userId);
    void submitAcceptance(HotelWorkOrderStatusReqVO reqVO, Long userId);
    void acceptPass(HotelWorkOrderStatusReqVO reqVO, Long userId);
    void acceptReject(HotelWorkOrderStatusReqVO reqVO, Long userId);
    HotelDashboardRespVO dashboard(String store, Long userId);
    Integer badge(Long userId);
    HotelPermissionRespVO permission(Long userId);
}
