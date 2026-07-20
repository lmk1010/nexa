package com.kyx.service.biz.dal.dataobject.hotel;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("business_hotel_work_order_log")
@KeySequence("business_hotel_work_order_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class HotelWorkOrderLogDO extends TenantBaseDO {
    @TableId
    private Long id;
    private Long orderId;
    private Integer fromStatus;
    private Integer toStatus;
    private Long operatorUserId;
    private String operatorName;
    private String content;
}
