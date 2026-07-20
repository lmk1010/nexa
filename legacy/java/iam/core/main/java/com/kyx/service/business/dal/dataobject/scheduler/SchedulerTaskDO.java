package com.kyx.service.business.dal.dataobject.scheduler;

import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 定时任务管理表
 *
 * @author MK
 */
@TableName("system_scheduler_task")
@KeySequence("system_scheduler_task_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
public class SchedulerTaskDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务描述
     */
    private String taskDescription;

    /**
     * 任务类名
     */
    private String taskClass;

    /**
     * 任务方法名
     */
    private String taskMethod;

    /**
     * 任务参数
     */
    private String taskParams;

    /**
     * Cron表达式
     */
    private String cronExpression;

    /**
     * 任务状态（0禁用 1启用）
     */
    private Integer taskStatus;

    /**
     * 任务类型（SYNC同步任务 CLEAN清理任务 REPORT报表任务等）
     */
    private String taskType;

    /**
     * 最后执行时间
     */
    private LocalDateTime lastExecuteTime;

    /**
     * 下次执行时间
     */
    private LocalDateTime nextExecuteTime;

    /**
     * 执行次数
     */
    private Long executeCount;

    /**
     * 成功次数
     */
    private Long successCount;

    /**
     * 失败次数
     */
    private Long failCount;

    /**
     * 租户编号
     */
    private Long tenantId;

    /**
     * 任务状态枚举
     */
    public enum TaskStatus {
        DISABLED(0, "禁用"),
        ENABLED(1, "启用");

        private final Integer value;
        private final String label;

        TaskStatus(Integer value, String label) {
            this.value = value;
            this.label = label;
        }

        public Integer getValue() {
            return value;
        }

        public String getLabel() {
            return label;
        }
    }

    /**
     * 任务类型枚举
     */
    public enum TaskType {
        SYNC("SYNC", "同步任务"),
        CLEAN("CLEAN", "清理任务"),
        REPORT("REPORT", "报表任务"),
        BACKUP("BACKUP", "备份任务"),
        OTHER("OTHER", "其他任务");

        private final String value;
        private final String label;

        TaskType(String value, String label) {
            this.value = value;
            this.label = label;
        }

        public String getValue() {
            return value;
        }

        public String getLabel() {
            return label;
        }
    }
}