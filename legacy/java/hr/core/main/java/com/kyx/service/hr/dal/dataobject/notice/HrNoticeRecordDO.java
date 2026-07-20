package com.kyx.service.hr.dal.dataobject.notice;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * HR notice sending record.
 */
@TableName("hr_notice_record")
@KeySequence("hr_notice_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class HrNoticeRecordDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String noticeKey;

    private String channel;

    private String businessType;

    private Long businessId;

    private Long receiverUserId;

    private String title;

    private String content;

    private String sendStatus;

    private LocalDateTime sendTime;

    private String errorMessage;

    private Integer retryCount;

    private String remark;

}
