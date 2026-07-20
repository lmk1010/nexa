package com.kyx.service.business.dal.dataobject.dept;

import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 岗位同步表
 *
 * @author MK
 */
@TableName("system_post_sync")
@KeySequence("system_post_sync_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
public class PostSyncDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 外部系统岗位ID
     */
    private Long externalPostId;

    /**
     * 外部系统岗位编码
     */
    private String externalPostCode;

    /**
     * 外部系统岗位名称
     */
    private String externalPostName;

    /**
     * 外部系统显示顺序
     */
    private Integer externalPostSort;

    /**
     * 外部系统状态（0正常 1停用）
     */
    private Integer externalStatus;

    /**
     * 外部系统备注
     */
    private String externalRemark;

    /**
     * 外部系统创建者
     */
    private String externalCreateBy;

    /**
     * 外部系统创建时间
     */
    private LocalDateTime externalCreateTime;

    /**
     * 外部系统更新者
     */
    private String externalUpdateBy;

    /**
     * 外部系统更新时间
     */
    private LocalDateTime externalUpdateTime;

    /**
     * 本地岗位ID（关联system_post表）
     */
    private Long localPostId;

    /**
     * 同步状态（0待同步 1已同步 2同步失败）
     */
    private Integer syncStatus;

    /**
     * 同步错误信息
     */
    private String syncErrorMsg;

    /**
     * 同步状态枚举
     */
    public enum SyncStatus {
        PENDING(0, "待同步"),
        SYNCED(1, "已同步"),
        FAILED(2, "同步失败");

        private final Integer value;
        private final String desc;

        SyncStatus(Integer value, String desc) {
            this.value = value;
            this.desc = desc;
        }

        public Integer getValue() {
            return value;
        }

        public String getDesc() {
            return desc;
        }
    }
}