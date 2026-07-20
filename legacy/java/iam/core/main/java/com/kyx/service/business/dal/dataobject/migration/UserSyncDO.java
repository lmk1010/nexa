package com.kyx.service.business.dal.dataobject.migration;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.*;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 用户同步迁移数据对象
 * 
 * 用于存储从外部系统同步过来的用户数据
 * 支持统一认证和数据用户中心的数据迁移功能
 * 
 * @author MK
 */
@TableName(value = "system_user_sync", autoResultMap = true)
@KeySequence("system_user_sync_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class UserSyncDO extends TenantBaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 外部系统用户ID
     */
    private String externalUserId;

    /**
     * 用户账号
     */
    private String username;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户邮箱
     */
    private String email;

    /**
     * 手机号码
     */
    private String mobile;

    /**
     * 用户性别（0男 1女 2未知）
     */
    private Integer sex;

    /**
     * 头像地址
     */
    private String avatar;

    /**
     * 用户等级（1普通用户 2VIP用户 3超级用户）
     */
    private Integer userLevel;

    /**
     * 地区编码
     */
    private Integer region;

    /**
     * 地区路径数组（JSON格式）
     */
    private String regionPath;

    /**
     * 地区路径字符串数组（JSON格式）
     */
    private String regionPathStr;

    /**
     * 详细地址
     */
    private String address;

    /**
     * 登录IP
     */
    private String loginIp;

    /**
     * 登录时间
     */
    private LocalDateTime loginDate;

    /**
     * 用户类型（如：EMPLOYEE）
     */
    private String userType;

    /**
     * 联系人姓名
     */
    private String linkName;

    /**
     * 是否工作中（0否 1是）
     */
    private Integer worked;

    /**
     * PD权限
     */
    private Boolean permissionPd;

    /**
     * GD权限
     */
    private Boolean permissionGd;

    /**
     * 继承人用户ID
     */
    private Long heirUid;

    /**
     * 相同工作
     */
    private String sameWork;

    /**
     * 微信号
     */
    private String wechat;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 积分
     */
    private Double integral;

    /**
     * 点数
     */
    private Double point;

    /**
     * 工作权重
     */
    private Integer workWeight;

    /**
     * 平台
     */
    private String platform;

    /**
     * 上线时间
     */
    private LocalDateTime upTime;

    /**
     * 排序号
     */
    private Integer orderNum;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 岗位名称
     */
    private String postName;

    /**
     * 帐号状态（0正常 1停用）
     */
    private Integer status;

    /**
     * 同步状态（0待同步 1已同步 2同步失败）
     */
    private Integer syncStatus;

    /**
     * 同步时间
     */
    private LocalDateTime syncTime;

    /**
     * 同步错误信息
     */
    private String syncError;

    /**
     * 外部系统原始数据JSON
     */
    private String externalData;

    /**
     * 同步状态枚举
     */
    public enum SyncStatus {
        PENDING(0, "待同步"),
        SUCCESS(1, "已同步"),
        FAILED(2, "同步失败");

        private final Integer code;
        private final String description;

        SyncStatus(Integer code, String description) {
            this.code = code;
            this.description = description;
        }

        public Integer getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }
}