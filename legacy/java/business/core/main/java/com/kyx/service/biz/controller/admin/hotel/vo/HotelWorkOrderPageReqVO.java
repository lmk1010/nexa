package com.kyx.service.biz.controller.admin.hotel.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Schema(description = "Admin - Hotel work order page Request VO")
@Data
public class HotelWorkOrderPageReqVO extends PageParam {
    private String keyword;
    private String store;
    private String type;
    private Integer status;
    private Long assigneeUserId;
    private Long creatorUserId;
    private Date[] createTime;
}
