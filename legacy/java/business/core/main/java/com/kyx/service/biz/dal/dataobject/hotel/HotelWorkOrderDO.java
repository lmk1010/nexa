package com.kyx.service.biz.dal.dataobject.hotel;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@TableName("business_hotel_work_order")
@KeySequence("business_hotel_work_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class HotelWorkOrderDO extends TenantBaseDO {

    @TableId
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
    private Date acceptedTime;
    private Long acceptedUserId;
    private String acceptedUserName;
    private Date finishTime;
    private Long finishUserId;
    private String finishUserName;
}
