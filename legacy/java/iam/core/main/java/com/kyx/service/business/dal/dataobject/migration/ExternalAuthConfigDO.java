package com.kyx.service.business.dal.dataobject.migration;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.*;
import lombok.experimental.Accessors;

/**
 * 外部系统认证配置数据对象
 * 
 * 用于配置外部系统的认证信息和接口地址
 * 支持多个外部系统的统一认证配置
 * 
 * @author MK
 */
@TableName(value = "system_external_auth_config", autoResultMap = true)
@KeySequence("system_external_auth_config_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ExternalAuthConfigDO extends TenantBaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 外部系统名称
     */
    private String systemName;

    /**
     * 外部系统基础URL
     */
    private String baseUrl;

    /**
     * 登录接口地址
     */
    private String loginUrl;

    /**
     * 用户列表接口地址
     */
    private String userListUrl;

    /**
     * 部门列表接口地址
     */
    private String deptListUrl;

    /**
     * 登录用户名
     */
    private String username;

    /**
     * 登录密码
     */
    private String password;

    /**
     * Token请求头名称
     */
    private String tokenHeader;

    /**
     * UUID请求头名称
     */
    private String uuidHeader;

    /**
     * UUID值
     */
    private String uuidValue;

    /**
     * 状态（0正常 1停用）
     */
    private Integer status;
}