package com.kyx.service.hr.dal.dataobject.integration;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * DingTalk sync history record.
 */
@TableName("hr_dingtalk_sync_history")
@KeySequence("hr_dingtalk_sync_history_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class DingTalkSyncHistoryDO extends TenantBaseDO {

    @TableId
    private Long id;

    /**
     * Sync type, for example EMPLOYEE_SYNC / ATTENDANCE_SYNC / FULL_SYNC.
     */
    private String syncType;

    /**
     * Sync scope, USER_PROFILE / ATTENDANCE / ALL.
     */
    private String syncScope;

    /**
     * Trigger mode, MANUAL / SCHEDULE / STREAM.
     */
    private String triggerMode;

    /**
     * Target tenant id.
     */
    private Long targetTenantId;

    /**
     * Operator user id.
     */
    private Long operatorUserId;

    /**
     * Attendance lookback minutes.
     */
    private Long lookbackMinutes;

    /**
     * Auto create switch.
     */
    private Boolean autoCreateEnabled;

    /**
     * Auto create department id.
     */
    private Long autoCreateDeptId;

    /**
     * Total records from source.
     */
    private Integer totalCount;

    /**
     * Pulled records count.
     */
    private Integer pulledCount;

    /**
     * Synced records count.
     */
    private Integer syncedCount;

    /**
     * Created records count.
     */
    private Integer createdCount;

    /**
     * Updated records count.
     */
    private Integer updatedCount;

    /**
     * Failed records count.
     */
    private Integer failedCount;

    /**
     * Skipped records count.
     */
    private Integer skippedCount;

    /**
     * Sync start time.
     */
    private LocalDateTime syncStartTime;

    /**
     * Sync end time.
     */
    private LocalDateTime syncEndTime;

    /**
     * Total duration in milliseconds.
     */
    private Long durationMs;

    /**
     * Summary text for quick scan.
     */
    private String summary;

    /**
     * Detail json payload.
     */
    private String detailJson;
}
