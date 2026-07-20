package com.kyx.service.biz.controller.admin.hotel.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class HotelWorkOrderStatusReqVO {
    @NotNull
    private Long id;
    @NotNull
    private Integer status;
    private String remark;
}
