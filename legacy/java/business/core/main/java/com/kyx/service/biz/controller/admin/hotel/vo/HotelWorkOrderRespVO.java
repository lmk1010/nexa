package com.kyx.service.biz.controller.admin.hotel.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class HotelWorkOrderRespVO {
    private Long id;
    private String store;
    private String roomNo;
    private String title;
    private String type;
    private String priority;
    private Integer status;
    private String content;
    private String source;
    private Long sourceRecordId;
    private String sourceRecordTitle;
    private String customerEmotion;
    private Long assigneeUserId;
    private String assigneeName;
    private String assigneeImUserId;
    private Long creatorUserId;
    private String creatorName;
    private Date createTime;
    private Date acceptedTime;
    private Long acceptedUserId;
    private String acceptedUserName;
    private Date finishTime;
    private Long finishUserId;
    private String finishUserName;
    private List<HotelWorkOrderLogRespVO> logs;
}
