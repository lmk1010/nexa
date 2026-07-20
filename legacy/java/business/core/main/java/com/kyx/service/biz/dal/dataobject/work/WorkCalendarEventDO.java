package com.kyx.service.biz.dal.dataobject.work;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@TableName("business_work_calendar_event")
@KeySequence("business_work_calendar_event_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkCalendarEventDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long userId;

    private String title;

    private String type;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String location;

    private String remark;
}
