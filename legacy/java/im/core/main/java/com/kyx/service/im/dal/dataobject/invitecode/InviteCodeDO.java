package com.kyx.service.im.dal.dataobject.invitecode;

import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.aop.TenantIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 邀请码 DO
 *
 * @author MK
 */
@TableName("im_invite_code")
@KeySequence("im_invite_code_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TenantIgnore
public class InviteCodeDO extends BaseDO {

    /**
     * 编号，数据库递增
     */
    @TableId
    private Long id;

    /**
     * 邀请码
     */
    private String code;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 邀请码类型
     * 
     * 枚举 {@link InviteCodeTypeEnum}
     */
    private Integer type;

    /**
     * 使用次数限制
     */
    private Integer usageLimit;

    /**
     * 已使用次数
     */
    private Integer usedCount;

    /**
     * 有效期开始时间
     */
    private LocalDateTime validStartTime;

    /**
     * 有效期结束时间
     */
    private LocalDateTime validEndTime;

    /**
     * 状态
     * 
     * 枚举 {@link InviteCodeStatusEnum}
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建者用户ID
     */
    private Long creatorId;

    /**
     * 创建者用户名
     */
    private String creatorName;

    /**
     * 租户名称（非数据库字段）
     */
    @TableField(exist = false)
    private String tenantName;

    /**
     * 租户状态（非数据库字段）
     */
    @TableField(exist = false)
    private Integer tenantStatus;
} 