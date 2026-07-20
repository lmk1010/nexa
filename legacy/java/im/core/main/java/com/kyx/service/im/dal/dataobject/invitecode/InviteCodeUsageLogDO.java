package com.kyx.service.im.dal.dataobject.invitecode;

import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 邀请码使用记录 DO
 *
 * @author MK
 */
@TableName("im_invite_code_usage_log")
@KeySequence("im_invite_code_usage_log_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class InviteCodeUsageLogDO extends BaseDO {

    /**
     * 编号，数据库递增
     */
    @TableId
    private Long id;

    /**
     * 邀请码ID
     */
    private Long inviteCodeId;

    /**
     * 邀请码
     */
    private String inviteCode;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 租户名称
     */
    private String tenantName;

    /**
     * 使用者用户ID
     */
    private Long userId;

    /**
     * 使用者用户名
     */
    private String userName;

    /**
     * 使用者IP
     */
    private String userIp;

    /**
     * 浏览器UA
     */
    private String userAgent;

    /**
     * 设备类型
     */
    private String deviceType;

    /**
     * 设备标识
     */
    private String deviceId;

    /**
     * 使用时间
     */
    private LocalDateTime usageTime;

    /**
     * 使用结果：1-成功，0-失败
     */
    private Integer usageResult;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 额外数据（JSON格式）
     */
    private String extraData;
} 