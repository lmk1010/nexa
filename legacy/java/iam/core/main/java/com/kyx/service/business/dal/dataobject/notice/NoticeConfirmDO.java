package com.kyx.service.business.dal.dataobject.notice;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 通知确认回执记录 DO
 */
@TableName("system_notice_confirm")
@KeySequence("system_notice_confirm_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class NoticeConfirmDO extends TenantBaseDO {

    @TableId
    private Long id;

    /**
     * 公告 ID
     */
    private Long noticeId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户类型
     */
    private Integer userType;

    /**
     * 确认时间
     */
    private LocalDateTime confirmTime;

    /**
     * 催办次数
     */
    private Integer remindCount;

    /**
     * 最近催办时间
     */
    private LocalDateTime lastRemindTime;

}
