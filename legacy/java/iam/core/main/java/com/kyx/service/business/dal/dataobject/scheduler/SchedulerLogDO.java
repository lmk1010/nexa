package com.kyx.service.business.dal.dataobject.scheduler;

import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 定时任务执行日志表
 *
 * @author MK
 */
@TableName("system_scheduler_log")
@KeySequence("system_scheduler_log_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
public class SchedulerLogDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 任务名称
     */
    private String jobName;

    /**
     * 任务类名
     */
    private String jobClass;

    /**
     * 任务方法名
     */
    private String jobMethod;

    /**
     * 任务参数
     */
    private String jobParams;

    /**
     * Cron表达式
     */
    private String cronExpression;

    /**
     * 执行状态（0成功 1失败）
     */
    private Integer status;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 执行耗时（毫秒）
     */
    private Long duration;

    /**
     * 执行结果消息
     */
    private String resultMessage;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 任务ID（关联到具体的任务）
     */
    private Long taskId;

    /**
     * 执行进度（0-100）
     */
    private Integer progress;

    /**
     * 进度描述信息
     */
    private String progressMessage;

    /**
     * 是否为手动执行
     */
    private Boolean isManual;

    /**
     * 租户编号
     */
    private Long tenantId;

    /**
     * 新增记录数
     */
    private Integer insertCount;

    /**
     * 更新记录数
     */
    private Integer updateCount;

    /**
     * 失败记录数
     */
    private Integer failureCount;

    /**
     * 跳过记录数
     */
    private Integer skipCount;

    /**
     * 执行状态枚举
     */
    public enum Status {
        RUNNING(2, "执行中"),
        SUCCESS(0, "成功"),
        FAILED(1, "失败");

        private final Integer value;
        private final String desc;

        Status(Integer value, String desc) {
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