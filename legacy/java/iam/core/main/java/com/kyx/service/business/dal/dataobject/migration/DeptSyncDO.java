package com.kyx.service.business.dal.dataobject.migration;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.*;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 部门同步迁移数据对象
 * 
 * 用于存储从外部系统同步过来的部门数据
 * 支持统一认证和数据用户中心的部门数据迁移功能
 * 
 * @author MK
 */
@TableName(value = "system_dept_sync", autoResultMap = true)
@KeySequence("system_dept_sync_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class DeptSyncDO extends TenantBaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 外部系统部门ID
     */
    private String externalDeptId;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 父部门ID
     */
    private Long parentId;

    /**
     * 外部系统父部门ID
     */
    private String externalParentId;

    /**
     * 祖级列表
     */
    private String ancestors;

    /**
     * 显示顺序
     */
    private Integer orderNum;

    /**
     * 负责人ID
     */
    private Long leaderId;

    /**
     * 负责人姓名
     */
    private String leaderName;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 部门状态（0正常 1停用）
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
     * 对应的内部系统部门ID（同步成功后设置）
     */
    private Long mappedDeptId;

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