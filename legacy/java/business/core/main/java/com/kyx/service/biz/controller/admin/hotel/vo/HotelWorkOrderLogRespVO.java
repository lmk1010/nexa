package com.kyx.service.biz.controller.admin.hotel.vo;

import lombok.Data;

import java.util.Date;

@Data
public class HotelWorkOrderLogRespVO {
    private Long id;
    private Long orderId;
    private Integer fromStatus;
    private Integer toStatus;
    private Long operatorUserId;
    private String operatorName;
    private String content;
    private Date createTime;
}
