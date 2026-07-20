package com.kyx.service.biz.dal.dataobject.work;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@TableName("hr_administrative_trip")
@KeySequence("hr_administrative_trip_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkCalendarTripDO extends BaseDO {

    @TableId
    private Long id;

    private Long userId;

    private String tripType;

    private String destinationCity;

    private String destinationAddress;

    private String purpose;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer status;
}
