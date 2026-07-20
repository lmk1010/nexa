package com.kyx.service.business.dal.dataobject.tenant;

import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 用户-租户关联 DO
 *
 * @author MK
 */
@TableName(value = "system_user_tenant_relation", autoResultMap = true)
@KeySequence("system_user_tenant_relation_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class UserTenantRelationDO extends BaseDO {

    /**
     * 关联ID
     */
    @TableId
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 状态（1正常 0停用）
     */
    private Integer status;

    /**
     * 是否默认租户（1是 0否）
     */
    private Integer isDefault;

    /**
     * 加入时间
     */
    private LocalDateTime joinTime;

    /**
     * 在该租户下的部门ID
     */
    private Long deptId;

    /**
     * 在该租户下的岗位ID列表
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Set<Long> postIds;

    /**
     * 在该租户下的昵称
     */
    private String nickname;

    /**
     * 在该租户下的头像
     */
    private String avatar;

    /**
     * 备注
     */
    private String remark;

} 