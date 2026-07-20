package com.kyx.service.business.dal.dataobject.notice;

import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.kyx.service.business.enums.notice.NoticeTypeEnum;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 通知公告表
 *
 * @author ruoyi
 */
@TableName("system_notice")
@KeySequence("system_notice_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
public class NoticeDO extends BaseDO {

    /**
     * 公告ID
     */
    private Long id;
    /**
     * 公告标题
     */
    private String title;
    /**
     * 公告类型
     *
     * 枚举 {@link NoticeTypeEnum}
     */
    private Integer type;
    /**
     * 公告内容
     */
    private String content;
    /**
     * 公告状态
     *
     * 枚举 {@link CommonStatusEnum}
     */
    private Integer status;
    /**
     * 接收范围
     */
    private String receiverType;
    /**
     * 接收用户编号，逗号分隔
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String receiverUserIds;
    /**
     * 是否需要确认
     */
    private Boolean needConfirm;
    /**
     * 确认截止时间
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime confirmDeadline;
    /**
     * 租户编号
     */
    private Long tenantId;

}
