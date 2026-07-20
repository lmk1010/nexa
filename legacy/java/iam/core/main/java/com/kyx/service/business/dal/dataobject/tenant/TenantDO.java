package com.kyx.service.business.dal.dataobject.tenant;

import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.kyx.foundation.tenant.core.aop.TenantIgnore;
import com.kyx.service.business.dal.dataobject.user.AdminUserDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 租户 DO
 *
 * @author MK
 */
@TableName(value = "system_tenant", autoResultMap = true)
@KeySequence("system_tenant_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TenantIgnore
@Accessors(chain = true)
public class TenantDO extends BaseDO {

    /**
     * 套餐编号 - 系统
     */
    public static final Long PACKAGE_ID_SYSTEM = 0L;

    /**
     * 租户编号，自增
     */
    private Long id;
    /**
     * 租户名，唯一
     */
    private String name;
    /**
     * 联系人的用户编号
     *
     * 关联 {@link AdminUserDO#getId()}
     */
    private Long contactUserId;
    /**
     * 联系人
     */
    private String contactName;
    /**
     * 联系手机
     */
    private String contactMobile;
    /**
     * 租户状态
     *
     * 枚举 {@link CommonStatusEnum}
     */
    private Integer status;
    /**
     * 绑定域名
     */
    private String website;
    /**
     * 租户套餐编号
     *
     * 关联 {@link TenantPackageDO#getId()}
     * 特殊逻辑：系统内置租户，不使用套餐，暂时使用 {@link #PACKAGE_ID_SYSTEM} 标识
     */
    private Long packageId;
    /**
     * 过期时间
     */
    private LocalDateTime expireTime;
    /**
     * 账号数量
     */
    private Integer accountCount;

    /**
     * 全局视图
     * 0-关闭，1-开启
     * 开启后可查看所有租户数据，不受租户隔离限制
     */
    private Integer globalView;

    /**
     * 父租户编号
     */
    private Long parentId;

    /**
     * 集团根租户编号
     */
    private Long rootId;

    /**
     * 层级路径
     */
    private String path;

    /**
     * 租户类型（GROUP/COMPANY）
     */
    private String tenantType;

    /**
     * 数据视角（SELF/GROUP/ALL）
     */
    private String viewScope;

}
