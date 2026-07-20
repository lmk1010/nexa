package com.kyx.service.biz.dal.dataobject.work;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@TableName("hr_administrative_leave")
@KeySequence("hr_administrative_leave_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkCalendarLeaveDO extends BaseDO {

    @TableId
    private Long id;

    private Long userId;

    private String leaveCategory;

    private String leaveType;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer status;
}
