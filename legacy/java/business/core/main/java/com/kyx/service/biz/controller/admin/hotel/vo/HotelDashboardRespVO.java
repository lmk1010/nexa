package com.kyx.service.biz.controller.admin.hotel.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class HotelDashboardRespVO {
    private List<HotelWorkOrderRespVO> orders;
    private Integer myTodoCount;
    private Integer total;
    private Integer pending;
    private Integer doing;
    private Integer done;
    private Map<String, Integer> storeCounts;
    private Map<String, Integer> typeCounts;
    private Map<String, Integer> statusCounts;
    private Map<String, Integer> emotionCounts;
}
