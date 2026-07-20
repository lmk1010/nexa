package com.kyx.service.business.dal.dataobject.notice;

import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.kyx.foundation.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 通知公告已读记录 DO
 *
 * @author MK
 */
@TableName("system_notice_read")
@KeySequence("system_notice_read_seq")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class NoticeReadDO extends BaseDO {

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
     * 阅读时间
     */
    private LocalDateTime readTime;

}
