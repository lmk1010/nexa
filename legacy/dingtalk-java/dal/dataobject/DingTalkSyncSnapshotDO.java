package com.kyx.service.hr.dal.dataobject.integration;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * Restorable before-image for DingTalk employee sync changes.
 */
@TableName("hr_dingtalk_sync_snapshot")
@KeySequence("hr_dingtalk_sync_snapshot_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class DingTalkSyncSnapshotDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String snapshotBatchId;

    private String snapshotKey;

    private String syncType;

    private Long targetTenantId;

    private String action;

    private String reason;

    private Long profileId;

    private Long userId;

    private Long entryId;

    private Long bindingId;

    private Boolean profileSnapshot;

    private Boolean entrySnapshot;

    private Boolean adminUserSnapshot;

    private Boolean bindingSnapshot;

    private String beforeProfileName;

    private String beforeProfileMobile;

    private String beforeProfileEmail;

    private Integer beforeProfileStatus;

    private Integer beforeEntryWorkStatus;

    private Long beforeEntryDeptId;

    private LocalDate beforeEntryLeaveDate;

    private String beforeEntryLeaveReason;

    private Integer beforeAdminUserStatus;

    private Long beforeAdminUserDeptId;

    private Boolean beforeDingActive;

    private String detailJson;
}
