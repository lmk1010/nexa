import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace DingTalkSyncApi {
  export interface EmployeeSyncDetail {
    dingUserId?: string;
    name?: string;
    mobile?: string;
    email?: string;
    matchType?: string;
    action?: string;
    changedFields?: string;
  }

  export interface EmployeeSyncPreview {
    totalFromDingTalk?: number;
    matchedByMobile?: number;
    matchedByName?: number;
    updated?: number;
    unchanged?: number;
    toCreate?: number;
    unmatched?: number;
    skippedNoMobile?: number;
    skippedCreateInvalid?: number;
    skippedDuplicateMapping?: number;
    usersWithMobile?: number;
    usersWithName?: number;
    usersWithNeitherIdentity?: number;
    toMarkBindingsInactive?: number;
    toDisableProfiles?: number;
    toMarkEntriesResigned?: number;
    toDisableAdminUsers?: number;
    autoCreateEnabled?: boolean;
    autoCreateDeptId?: number;
    rosterEnabled?: boolean;
    rosterPulledUsers?: number;
    syncDetails?: EmployeeSyncDetail[];
  }

  export interface EmployeeSyncReport {
    totalFromDingTalk?: number;
    matchedByMobile?: number;
    matchedByName?: number;
    updated?: number;
    unchanged?: number;
    created?: number;
    createFailed?: number;
    skippedCreateInvalid?: number;
    skippedDuplicateMapping?: number;
    unmatched?: number;
    skippedNoMobile?: number;
    usersWithMobile?: number;
    usersWithName?: number;
    usersWithNeitherIdentity?: number;
    unmatchedMobiles?: string[];
    unmatchedIdentities?: string[];
    autoCreateEnabled?: boolean;
    autoCreateDeptId?: number;
    stagedToUserSync?: number;
    skippedNoExternalUserId?: number;
    syncedBindings?: number;
    syncedUserSyncRows?: number;
    syncedEntryStatuses?: number;
    syncedDeptIds?: number;
    syncedJobTitles?: number;
    rosterEnabled?: boolean;
    rosterPulledUsers?: number;
    rosterSyncedProfiles?: number;
    rosterUpdatedProfiles?: number;
    rosterCustomFields?: number;
    rosterSyncFailed?: number;
    enabledAdminUsers?: number;
    disabledAdminUsers?: number;
    cleanedProfiles?: number;
    cleanedBindings?: number;
    cleanedUserSyncRows?: number;
    snapshotBatchId?: string;
    snapshotRows?: number;
    unmatchedUsers?: Array<{
      dingUserId?: string;
      name?: string;
      mobile?: string;
      email?: string;
      active?: boolean;
    }>;
    syncDetails?: EmployeeSyncDetail[];
  }

  export interface EmployeeImportReport {
    totalFromDingTalk?: number;
    targetDeptId?: number;
    imported?: number;
    failed?: number;
    skippedExists?: number;
    skippedInvalid?: number;
    importedUsers?: string[];
    failedUsers?: string[];
    skippedUsers?: string[];
    importDetails?: Array<{
      status?: 'IMPORTED' | 'FAILED' | 'SKIPPED_EXISTS' | 'SKIPPED_INVALID';
      dingUserId?: string;
      name?: string;
      mobile?: string;
      profileId?: number;
      userId?: number;
      reason?: string;
    }>;
  }

  export interface AttendanceSyncReport {
    mappedUsers?: number;
    pulledRecords?: number;
    syncedRecords?: number;
    skippedRecords?: number;
  }
  export interface LeaveSyncReport {
    fromDate?: string;
    toDate?: string;
    mappedUsers?: number;
    checkedDays?: number;
    pulledLeaveDays?: number;
    syncedLeaves?: number;
    createdLeaves?: number;
    updatedLeaves?: number;
    cancelledLeaves?: number;
    skippedUsers?: number;
    failedCalls?: number;
    apiCalls?: number;
    syncMode?: string;
  }
  export interface AttendanceRecordPreview {
    userId?: number;
    attendanceDate?: string;
    clockTime?: string;
    clockType?: string;
    clockStatus?: string;
    sourceRecordId?: string;
    locationName?: string;
    locationAddress?: string;
    deviceInfo?: string;
  }
  export interface AttendanceSyncPreview {
    fromTime?: string;
    toTime?: string;
    sampleLimit?: number;
    mappedUsers?: number;
    pulledRecords?: number;
    skippedRecords?: number;
    sampleRecords?: AttendanceRecordPreview[];
  }

  export interface FullSyncReport {
    result?: string;
  }

  export interface SyncConfig {
    attendanceEnabled?: boolean;
    leaveEnabled?: boolean;
  }

  export interface RequirementNoticeConfig {
    enabled?: boolean;
    scenes?: RequirementNoticeScene[];
    stats?: RequirementNoticeStats;
  }

  export interface RequirementNoticeScene {
    scene?: string;
    name?: string;
    description?: string;
    enabled?: boolean;
  }

  export interface RequirementNoticeStats {
    totalSentCount?: number;
    todaySentCount?: number;
    currentMinuteSentCount?: number;
    perUserLimitPerMinute?: number;
    tenantLimitPerMinute?: number;
    dedupTtlMinutes?: number;
    sceneStats?: RequirementNoticeSceneStats[];
  }

  export interface RequirementNoticeSceneStats {
    scene?: string;
    totalSentCount?: number;
    todaySentCount?: number;
    currentMinuteSentCount?: number;
  }

  export interface BpmCopyNoticeResendReport {
    scannedCount?: number;
    runningCount?: number;
    receiverCount?: number;
    taskScannedCount?: number;
    taskReceiverCount?: number;
    copyScannedCount?: number;
    copyRunningCount?: number;
    copyReceiverCount?: number;
  }

  export interface SystemUpdateNoticeReq {
    title?: string;
    content?: string;
    includeLeaders?: boolean;
    includeTechDept?: boolean;
    roleIds?: number[];
    deptIds?: number[];
    detailUrl?: string;
    receiverUserIds?: number[];
    excludeUserIds?: number[];
  }

  export interface SystemUpdateNoticeRecipient {
    userId?: number;
    nickname?: string;
    username?: string;
    deptId?: number;
    deptName?: string;
    dingUserId?: string;
    sources?: string[];
  }

  export interface SystemUpdateNoticePreview {
    totalCount?: number;
    leaderCount?: number;
    techDeptCount?: number;
    roleCount?: number;
    deptCount?: number;
    manualCount?: number;
    recipients?: SystemUpdateNoticeRecipient[];
  }

  export interface SystemUpdateNoticeSendReport
    extends SystemUpdateNoticePreview {
    title?: string;
    content?: string;
    sentCount?: number;
    failedCount?: number;
    taskIds?: number[];
    failedRecipients?: Array<{
      userId?: number;
      nickname?: string;
      deptName?: string;
      reason?: string;
    }>;
  }

  export interface SyncHistoryRecord {
    id?: number;
    syncType?: string;
    syncScope?: string;
    triggerMode?: string;
    targetTenantId?: number;
    operatorUserId?: number;
    lookbackMinutes?: number;
    autoCreateEnabled?: boolean;
    autoCreateDeptId?: number;
    totalCount?: number;
    pulledCount?: number;
    syncedCount?: number;
    createdCount?: number;
    updatedCount?: number;
    failedCount?: number;
    skippedCount?: number;
    syncStartTime?: string;
    syncEndTime?: string;
    durationMs?: number;
    summary?: string;
    detailJson?: string;
    createTime?: string | number;
  }
}

/** Sync DingTalk employees to OA profiles. */
export async function syncDingTalkEmployees(params?: {
  targetTenantId?: number;
  autoCreateDeptId?: number;
  autoCreateMissing?: boolean;
}) {
  return requestClient.post<DingTalkSyncApi.EmployeeSyncReport>(
    '/hr/integration/dingtalk/employees/sync',
    null,
    { params, timeout: 120_000 },
  );
}

/** Sync DingTalk roster fields to existing bound OA employee profiles. */
export async function syncDingTalkRoster(targetTenantId?: number) {
  return requestClient.post<DingTalkSyncApi.EmployeeSyncReport>(
    '/hr/integration/dingtalk/roster/sync',
    null,
    { params: { targetTenantId }, timeout: 120_000 },
  );
}

/** Preview DingTalk employees sync without persistence. */
export async function previewDingTalkEmployees(params?: {
  targetTenantId?: number;
  autoCreateDeptId?: number;
  autoCreateMissing?: boolean;
}) {
  return requestClient.post<DingTalkSyncApi.EmployeeSyncPreview>(
    '/hr/integration/dingtalk/employees/preview',
    null,
    { params, timeout: 120_000 },
  );
}

/** Import unmatched DingTalk users into OA employee roster. */
export async function importUnmatchedDingTalkEmployees(
  deptId: number,
  targetTenantId?: number,
) {
  return requestClient.post<DingTalkSyncApi.EmployeeImportReport>(
    '/hr/integration/dingtalk/employees/import-unmatched',
    null,
    { params: { deptId, targetTenantId }, timeout: 120_000 },
  );
}

/** Sync DingTalk attendance to OA. */
export async function syncDingTalkAttendance(
  lookbackMinutes = 30,
  targetTenantId?: number,
) {
  return requestClient.post<DingTalkSyncApi.AttendanceSyncReport>(
    '/hr/integration/dingtalk/attendance/sync',
    null,
    { params: { lookbackMinutes, targetTenantId } },
  );
}

/** Sync DingTalk approved leave to OA. */
export async function syncDingTalkLeave(
  lookbackDays = 1,
  forwardDays = 0,
  targetTenantId?: number,
) {
  return requestClient.post<DingTalkSyncApi.LeaveSyncReport>(
    '/hr/integration/dingtalk/leave/sync',
    null,
    { params: { forwardDays, lookbackDays, targetTenantId }, timeout: 120_000 },
  );
}

/** Preview DingTalk attendance without persistence. */
export async function previewDingTalkAttendance(
  lookbackMinutes = 30,
  sampleLimit = 20,
  targetTenantId?: number,
) {
  return requestClient.post<DingTalkSyncApi.AttendanceSyncPreview>(
    '/hr/integration/dingtalk/attendance/preview',
    null,
    { params: { lookbackMinutes, sampleLimit, targetTenantId } },
  );
}

/** Run the same all-in-one logic as daily scheduled sync. */
export async function runDingTalkFullSync(targetTenantId?: number) {
  return requestClient.post<DingTalkSyncApi.FullSyncReport>(
    '/hr/integration/dingtalk/full/sync',
    null,
    { params: { targetTenantId } },
  );
}

/** Get DingTalk sync switches. */
export async function getDingTalkSyncConfig() {
  return requestClient.get<DingTalkSyncApi.SyncConfig>(
    '/hr/integration/dingtalk/sync/config',
  );
}

/** Update DingTalk attendance sync switch. */
export async function updateDingTalkAttendanceSyncConfig(enabled: boolean) {
  return requestClient.post<DingTalkSyncApi.SyncConfig>(
    '/hr/integration/dingtalk/sync/config/attendance',
    null,
    { params: { enabled } },
  );
}

/** Update DingTalk leave sync switch. */
export async function updateDingTalkLeaveSyncConfig(enabled: boolean) {
  return requestClient.post<DingTalkSyncApi.SyncConfig>(
    '/hr/integration/dingtalk/sync/config/leave',
    null,
    { params: { enabled } },
  );
}

/** Get DingTalk requirement notification switch. */
export async function getDingTalkRequirementNoticeConfig() {
  return requestClient.get<DingTalkSyncApi.RequirementNoticeConfig>(
    '/hr/integration/dingtalk/requirement-notice/config',
  );
}

/** Update DingTalk requirement notification switch. */
export async function updateDingTalkRequirementNoticeConfig(enabled: boolean) {
  return requestClient.post<DingTalkSyncApi.RequirementNoticeConfig>(
    '/hr/integration/dingtalk/requirement-notice/config',
    null,
    { params: { enabled } },
  );
}

/** Update one DingTalk requirement notification scene switch. */
export async function updateDingTalkRequirementNoticeSceneConfig(
  scene: string,
  enabled: boolean,
) {
  return requestClient.post<DingTalkSyncApi.RequirementNoticeConfig>(
    '/hr/integration/dingtalk/requirement-notice/scene-config',
    null,
    { params: { scene, enabled } },
  );
}

/** Resend DingTalk notice to current running BPM copy users. */
export async function resendRunningBpmCopyNotice(limit = 200, dryRun = false) {
  return requestClient.post<DingTalkSyncApi.BpmCopyNoticeResendReport>(
    '/hr/integration/dingtalk/requirement-notice/bpm-copy/resend-running',
    null,
    { params: { dryRun, limit } },
  );
}

/** Preview current running BPM notice receivers without sending. */
export async function previewRunningBpmCopyNotice(limit = 200) {
  return resendRunningBpmCopyNotice(limit, true);
}

/** Get users who can receive system update DingTalk cards. */
export async function getSystemUpdateNoticeAvailableUsers(
  targetTenantId?: number,
) {
  return requestClient.get<DingTalkSyncApi.SystemUpdateNoticeRecipient[]>(
    '/hr/integration/dingtalk/system-update-notice/available-users',
    { params: { targetTenantId } },
  );
}

/** Preview system update DingTalk card recipients. */
export async function previewSystemUpdateNotice(
  data: DingTalkSyncApi.SystemUpdateNoticeReq,
  targetTenantId?: number,
) {
  return requestClient.post<DingTalkSyncApi.SystemUpdateNoticePreview>(
    '/hr/integration/dingtalk/system-update-notice/preview',
    data,
    { params: { targetTenantId } },
  );
}

/** Send a system update DingTalk card. */
export async function sendSystemUpdateNotice(
  data: DingTalkSyncApi.SystemUpdateNoticeReq,
  targetTenantId?: number,
) {
  return requestClient.post<DingTalkSyncApi.SystemUpdateNoticeSendReport>(
    '/hr/integration/dingtalk/system-update-notice/send',
    data,
    { params: { targetTenantId }, timeout: 120_000 },
  );
}

/** Query DingTalk sync history page. */
export async function getDingTalkSyncHistoryPage(
  params: PageParam & {
    syncType?: string;
    syncScope?: string;
    triggerMode?: string;
    targetTenantId?: number;
    syncEndTime?: string[];
  },
) {
  return requestClient.get<PageResult<DingTalkSyncApi.SyncHistoryRecord>>(
    '/hr/integration/dingtalk/history/page',
    { params },
  );
}
