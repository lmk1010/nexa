<script lang="ts" setup>
import type { DingTalkSyncApi } from '#/api/hr/integration/dingtalk';
import type { SystemDeptApi } from '#/api/system/dept';
import type { SystemRoleApi } from '#/api/system/role';

import { computed, onMounted, onUnmounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  Alert,
  Button,
  Input,
  InputNumber,
  message,
  Modal,
  Progress,
  Select,
  Switch,
  Table,
  Tabs,
  Tag,
  TreeSelect,
} from 'ant-design-vue';

import {
  getDingTalkRequirementNoticeConfig,
  getDingTalkSyncConfig,
  getDingTalkSyncHistoryPage,
  getSystemUpdateNoticeAvailableUsers,
  previewDingTalkAttendance,
  previewDingTalkEmployees,
  previewRunningBpmCopyNotice,
  previewSystemUpdateNotice,
  resendRunningBpmCopyNotice,
  runDingTalkFullSync,
  sendSystemUpdateNotice,
  syncDingTalkAttendance,
  syncDingTalkEmployees,
  syncDingTalkLeave,
  syncDingTalkRoster,
  updateDingTalkAttendanceSyncConfig,
  updateDingTalkLeaveSyncConfig,
  updateDingTalkRequirementNoticeConfig,
  updateDingTalkRequirementNoticeSceneConfig,
} from '#/api/hr/integration/dingtalk';
import { getSimpleDeptList } from '#/api/system/dept';
import { getSimpleRoleList } from '#/api/system/role';

const lookbackMinutes = ref(1440);
const leaveLookbackDays = ref(1);
const leaveForwardDays = ref(0);
const autoCreateDeptId = ref<number | undefined>(1);
const autoCreateMissing = ref<number>(1);
const deptLoading = ref(false);
const deptTreeData = ref<any[]>([]);
const roleLoading = ref(false);
const roleList = ref<SystemRoleApi.Role[]>([]);

const employeePreviewing = ref(false);
const employeeSyncing = ref(false);
const rosterSyncing = ref(false);
const previewing = ref(false);
const syncing = ref(false);
const leaveSyncing = ref(false);
const fullSyncing = ref(false);
const historyLoading = ref(false);
const syncConfigLoading = ref(false);
const attendanceSwitchSaving = ref(false);
const leaveSwitchSaving = ref(false);
const requirementNoticeLoading = ref(false);
const requirementNoticeSaving = ref(false);
const bpmCopyNoticeResending = ref(false);
const systemUpdatePreviewing = ref(false);
const systemUpdateSending = ref(false);
const systemUpdateAvailableLoading = ref(false);
const requirementNoticeEnabled = ref(false);
const requirementNoticeScenes = ref<DingTalkSyncApi.RequirementNoticeScene[]>(
  [],
);
const requirementNoticeStats = ref<DingTalkSyncApi.RequirementNoticeStats>();
const requirementNoticeSceneSaving = reactive<Record<string, boolean>>({});
const systemUpdateNotice = reactive<DingTalkSyncApi.SystemUpdateNoticeReq>({
  title: '连途系统更新通知',
  content: '连途系统正在更新中。',
  includeLeaders: false,
  includeTechDept: false,
  detailUrl: '',
});
const attendanceSyncEnabled = ref(true);
const leaveSyncEnabled = ref(true);

const employeePreview = ref<DingTalkSyncApi.EmployeeSyncPreview>();
const employeeReport = ref<DingTalkSyncApi.EmployeeSyncReport>();
const previewData = ref<DingTalkSyncApi.AttendanceSyncPreview>();
const leaveReport = ref<DingTalkSyncApi.LeaveSyncReport>();
const systemUpdatePreview = ref<DingTalkSyncApi.SystemUpdateNoticePreview>();
const systemUpdateReport = ref<DingTalkSyncApi.SystemUpdateNoticeSendReport>();
const systemUpdateAvailableUsers = ref<
  DingTalkSyncApi.SystemUpdateNoticeRecipient[]
>([]);
const systemUpdateManualUserIds = ref<number[]>([]);
const systemUpdateExcludedUserIds = ref<number[]>([]);
const systemUpdateRoleIds = ref<number[]>([]);
const systemUpdateDeptIds = ref<number[]>([]);
const historyList = ref<DingTalkSyncApi.SyncHistoryRecord[]>([]);
const historyTypeFilter = ref('ALL');
const activeSyncTitle = ref('');
const activeSyncStatus = ref('');
const activeSyncPercent = ref(0);
const activeSyncElapsedSeconds = ref(0);
let syncProgressTimer: ReturnType<typeof setInterval> | undefined;

function toDeptTreeData(list: SystemDeptApi.Dept[] = []): any[] {
  return list
    .filter((item) => !!item.id)
    .map((item) => ({
      title: item.name,
      value: item.id,
      children: toDeptTreeData(item.children || []),
    }));
}

async function loadDepts() {
  if (deptTreeData.value.length > 0) return;
  deptLoading.value = true;
  try {
    const list = await getSimpleDeptList();
    deptTreeData.value = toDeptTreeData(Array.isArray(list) ? list : []);
  } catch (error: any) {
    message.error(error?.message || '加载部门失败');
  } finally {
    deptLoading.value = false;
  }
}

async function loadRoles() {
  if (roleList.value.length > 0) return;
  roleLoading.value = true;
  try {
    const list = await getSimpleRoleList();
    roleList.value = Array.isArray(list) ? list : [];
  } catch (error: any) {
    message.error(error?.message || '加载角色失败');
  } finally {
    roleLoading.value = false;
  }
}

const historyPagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
});

const historyTypeOptions = [
  { label: '全部类型', value: 'ALL' },
  { label: '请假同步', value: 'LEAVE_SYNC' },
  { label: '考勤同步', value: 'ATTENDANCE_SYNC' },
  { label: '考勤预览', value: 'ATTENDANCE_PREVIEW' },
  { label: '用户同步', value: 'EMPLOYEE_SYNC' },
  { label: '用户预览', value: 'EMPLOYEE_PREVIEW' },
  { label: '花名册同步', value: 'ROSTER_SYNC' },
  { label: '全量同步', value: 'FULL_SYNC' },
];

const detailFilter = ref('DIFF');
const detailKeyword = ref('');

const detailFilterOptions = [
  { label: '仅差异', value: 'DIFF' },
  { label: '待创建', value: 'TO_CREATE' },
  { label: '已创建', value: 'CREATED' },
  { label: '已更新', value: 'UPDATED' },
  { label: '花名册已同步', value: 'ROSTER_SYNCED' },
  { label: '未匹配', value: 'UNMATCHED' },
  { label: '去重跳过', value: 'DUP' },
  { label: '姓名匹配', value: 'NAME_MATCH' },
  { label: '全部', value: 'ALL' },
];

const detailColumns = [
  { title: '钉钉账号', dataIndex: 'dingUserId', width: 220 },
  { title: '姓名', dataIndex: 'name', width: 120 },
  { title: '手机号', dataIndex: 'mobile', width: 140 },
  { title: '邮箱', dataIndex: 'email', width: 220 },
  { title: '匹配类型', dataIndex: 'matchType', width: 120 },
  { title: '处理结果', dataIndex: 'action', width: 140 },
  { title: '变更字段', dataIndex: 'changedFields', width: 180 },
];

const attendanceColumns = [
  { title: '日期', dataIndex: 'attendanceDate', width: 120 },
  { title: '时间', dataIndex: 'clockTime', width: 180 },
  { title: '打卡类型', dataIndex: 'clockType', width: 110 },
  { title: '状态', dataIndex: 'clockStatus', width: 110 },
  { title: '地点', dataIndex: 'locationName', ellipsis: true },
];

const historyColumns = [
  { title: '同步时间', dataIndex: 'syncEndTime', width: 180 },
  { title: '同步类型', dataIndex: 'syncType', width: 160 },
  { title: '范围', dataIndex: 'syncScope', width: 120 },
  { title: '触发方式', dataIndex: 'triggerMode', width: 110 },
  { title: '租户', dataIndex: 'targetTenantId', width: 90 },
  { title: '总数', dataIndex: 'totalCount', width: 90 },
  { title: '同步数', dataIndex: 'syncedCount', width: 90 },
  { title: '新建', dataIndex: 'createdCount', width: 80 },
  { title: '更新', dataIndex: 'updatedCount', width: 80 },
  { title: '失败', dataIndex: 'failedCount', width: 80 },
  { title: '跳过', dataIndex: 'skippedCount', width: 80 },
  { title: '耗时(ms)', dataIndex: 'durationMs', width: 100 },
  { title: '摘要', dataIndex: 'summary', ellipsis: true },
];

const systemUpdateRecipientColumns = [
  { title: '姓名', dataIndex: 'nickname', width: 120 },
  { title: '账号', dataIndex: 'username', width: 130 },
  { title: '部门', dataIndex: 'deptName', width: 130 },
  { title: '范围', dataIndex: 'sources', width: 180 },
  { title: '操作', dataIndex: 'action', width: 90 },
];

const employeeSummary = computed(
  () => employeePreview.value || employeeReport.value,
);

const systemUpdateRecipients = computed(
  () =>
    systemUpdateReport.value?.recipients ||
    systemUpdatePreview.value?.recipients ||
    [],
);

const systemUpdateUserOptions = computed(() =>
  systemUpdateAvailableUsers.value
    .filter((user) => user.userId)
    .map((user) => {
      const label = [
        user.nickname || user.username || '未命名用户',
        user.deptName,
        user.username,
      ]
        .filter(Boolean)
        .join(' · ');
      return {
        label,
        value: user.userId!,
      };
    }),
);

const systemUpdateRoleOptions = computed(() =>
  roleList.value
    .filter(
      (role) => role.id && (role.status === undefined || role.status === 0),
    )
    .map((role) => ({
      label: role.name,
      value: role.id!,
    })),
);

const systemUpdateRoleNameMap = computed(() => {
  const map = new Map<number, string>();
  for (const role of roleList.value) {
    if (role.id) {
      map.set(role.id, role.name || String(role.id));
    }
  }
  return map;
});

const systemUpdateDeptNameMap = computed(() => {
  const map = new Map<number, string>();
  const walk = (nodes: any[] = []) => {
    for (const node of nodes) {
      const id = Number(node?.value || 0);
      if (id > 0) {
        map.set(id, node.title || String(id));
      }
      if (Array.isArray(node?.children)) {
        walk(node.children);
      }
    }
  };
  walk(deptTreeData.value);
  return map;
});

const systemUpdateSelectedRoleNames = computed(() =>
  systemUpdateRoleIds.value.map(
    (id) => systemUpdateRoleNameMap.value.get(id) || String(id),
  ),
);

const systemUpdateSelectedDeptNames = computed(() =>
  systemUpdateDeptIds.value.map(
    (id) => systemUpdateDeptNameMap.value.get(id) || String(id),
  ),
);

function buildSelectionLabel(label: string, values: string[]) {
  if (values.length === 0) return '';
  if (values.length <= 3) {
    return `${label}：${values.join('、')}`;
  }
  return `${label}：${values.slice(0, 3).join('、')} 等 ${values.length} 项`;
}

const systemUpdateScopeLabel = computed(() => {
  const parts = [
    buildSelectionLabel('角色', systemUpdateSelectedRoleNames.value),
    buildSelectionLabel('部门', systemUpdateSelectedDeptNames.value),
    systemUpdateNotice.includeLeaders ? '队长' : '',
    systemUpdateNotice.includeTechDept ? '技术部' : '',
    systemUpdateManualUserIds.value.length > 0
      ? `手动 ${systemUpdateManualUserIds.value.length} 人`
      : '',
  ].filter(Boolean);
  return parts.join(' + ') || '-';
});

const systemUpdateHasTarget = computed(
  () =>
    systemUpdateRoleIds.value.length > 0 ||
    systemUpdateDeptIds.value.length > 0 ||
    Boolean(systemUpdateNotice.includeLeaders) ||
    Boolean(systemUpdateNotice.includeTechDept) ||
    systemUpdateManualUserIds.value.length > 0,
);

const isAnyManualSyncing = computed(
  () =>
    employeePreviewing.value ||
    employeeSyncing.value ||
    rosterSyncing.value ||
    previewing.value ||
    syncing.value ||
    leaveSyncing.value ||
    fullSyncing.value,
);

const filteredDetails = computed(() => {
  const source = employeeSummary.value?.syncDetails || [];
  const keyword = detailKeyword.value.trim().toLowerCase();
  return source.filter((item) => {
    const action = item.action || '';
    const matchType = item.matchType || '';
    let hitFilter = false;
    switch (detailFilter.value) {
      case 'ALL': {
        hitFilter = true;
        break;
      }
      case 'DIFF': {
        hitFilter = [
          'CREATE_FAILED',
          'SKIPPED_CREATE_INVALID',
          'SKIPPED_CROSS_TENANT_DUPLICATE',
          'SKIPPED_DUPLICATE_IN_BATCH',
          'SKIPPED_DUPLICATE_MAPPING',
          'SKIPPED_NO_IDENTITY',
          'TO_CREATE',
          'UNMATCHED',
          'UPDATED',
          'ROSTER_SYNCED',
          'ROSTER_STREAM_SYNCED',
          'ROSTER_STREAM_SYNC_FAILED',
        ].includes(action);
        break;
      }
      case 'DUP': {
        hitFilter = [
          'SKIPPED_CROSS_TENANT_DUPLICATE',
          'SKIPPED_DUPLICATE_IN_BATCH',
          'SKIPPED_DUPLICATE_MAPPING',
        ].includes(action);
        break;
      }
      case 'NAME_MATCH': {
        hitFilter = matchType === 'NAME';
        break;
      }
      case 'ROSTER_SYNCED': {
        hitFilter =
          action === 'ROSTER_SYNCED' || action === 'ROSTER_STREAM_SYNCED';
        break;
      }
      default: {
        hitFilter = action === detailFilter.value;
      }
    }
    if (!hitFilter) return false;
    if (!keyword) return true;
    const text =
      `${item.dingUserId || ''} ${item.name || ''} ${item.mobile || ''} ${item.email || ''}`.toLowerCase();
    return text.includes(keyword);
  });
});

function formatDateTime(value?: number | string) {
  if (value === undefined || value === null || value === '') return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  const pad = (num: number) => String(num).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

function matchTypeLabel(value?: string) {
  if (value === 'MOBILE') return '手机号匹配';
  if (value === 'NAME') return '姓名匹配';
  if (value === 'AUTO_CREATE') return '自动建档';
  if (value === 'ROSTER_STREAM') return '花名册绑定';
  return '无';
}

function actionLabel(value?: string) {
  if (value === 'TO_CREATE') return '待创建';
  if (value === 'CREATED') return '已创建';
  if (value === 'REUSED_EXISTING') return '复用已存在';
  if (value === 'UPDATED') return '已更新';
  if (value === 'ROSTER_SYNCED' || value === 'ROSTER_STREAM_SYNCED')
    return '花名册已同步';
  if (value === 'ROSTER_STREAM_ROSTER_MISSING') return '花名册无数据';
  if (value === 'ROSTER_STREAM_UNBOUND') return '未绑定 OA';
  if (value === 'ROSTER_STREAM_INACTIVE_BINDING') return '绑定已停用';
  if (value === 'ROSTER_STREAM_PROFILE_MISSING') return '员工档案不存在';
  if (value === 'ROSTER_STREAM_UNCHANGED') return '花名册无变更';
  if (value === 'ROSTER_STREAM_PULL_FAILED') return '花名册拉取失败';
  if (value === 'ROSTER_STREAM_SYNC_FAILED') return '花名册同步失败';
  if (value === 'ROSTER_STREAM_DISABLED') return '花名册同步已关闭';
  if (value === 'UNCHANGED') return '无变更';
  if (value === 'UNMATCHED') return '未匹配';
  if (value === 'CREATE_FAILED') return '创建失败';
  if (value === 'SKIPPED_CREATE_INVALID') return '缺少建档信息';
  if (value === 'SKIPPED_DUPLICATE_MAPPING') return '去重跳过';
  if (value === 'SKIPPED_DUPLICATE_IN_BATCH') return '批内重复跳过';
  if (value === 'SKIPPED_CROSS_TENANT_DUPLICATE') return '跨租户重复跳过';
  if (value === 'SKIPPED_NO_IDENTITY') return '无身份信息';
  return value || '-';
}

function actionColor(value?: string) {
  if (value === 'TO_CREATE') return 'cyan';
  if (
    value === 'CREATED' ||
    value === 'UPDATED' ||
    value === 'ROSTER_SYNCED' ||
    value === 'ROSTER_STREAM_SYNCED'
  )
    return 'green';
  if (value === 'UNCHANGED' || value === 'REUSED_EXISTING') return 'blue';
  if (value === 'UNMATCHED' || value === 'SKIPPED_CREATE_INVALID')
    return 'orange';
  if (
    value === 'SKIPPED_DUPLICATE_MAPPING' ||
    value === 'SKIPPED_DUPLICATE_IN_BATCH' ||
    value === 'SKIPPED_CROSS_TENANT_DUPLICATE'
  )
    return 'gold';
  if (
    value === 'CREATE_FAILED' ||
    value === 'SKIPPED_NO_IDENTITY' ||
    value === 'ROSTER_STREAM_PULL_FAILED' ||
    value === 'ROSTER_STREAM_SYNC_FAILED'
  )
    return 'red';
  return 'default';
}

function syncTypeLabel(value?: string) {
  const mapping: Record<string, string> = {
    EMPLOYEE_SYNC: '用户同步',
    EMPLOYEE_PREVIEW: '用户预览',
    EMPLOYEE_IMPORT: '用户导入',
    ROSTER_SYNC: '花名册同步',
    ATTENDANCE_SYNC: '考勤同步',
    ATTENDANCE_PREVIEW: '考勤预览',
    LEAVE_SYNC: '请假同步',
    FULL_SYNC: '全量同步',
  };
  return mapping[value || ''] || value || '-';
}

function syncScopeLabel(value?: string) {
  if (value === 'USER_PROFILE') return '资料类';
  if (value === 'ATTENDANCE') return '考勤类';
  if (value === 'LEAVE') return '请假类';
  if (value === 'ALL') return '全部';
  return value || '-';
}

function triggerModeLabel(value?: string) {
  if (value === 'MANUAL') return '手动';
  if (value === 'SCHEDULE') return '定时';
  if (value === 'STREAM') return '实时';
  return value || '-';
}

function syncModeLabel(value?: string) {
  if (value === 'BATCH_STATUS') return '批量状态接口';
  if (value === 'DURATION_FALLBACK') return '逐日兜底接口';
  return value || '-';
}

function safeInteger(value: unknown, defaultValue: number, minValue: number) {
  const numberValue = Number(value);
  if (!Number.isFinite(numberValue)) {
    return defaultValue;
  }
  return Math.max(Math.trunc(numberValue), minValue);
}

function beginSyncProgress(
  title: string,
  status: string,
  estimatedSeconds = 30,
) {
  if (syncProgressTimer) {
    clearInterval(syncProgressTimer);
  }
  const startedAt = Date.now();
  activeSyncTitle.value = title;
  activeSyncStatus.value = status;
  activeSyncPercent.value = 6;
  activeSyncElapsedSeconds.value = 0;
  syncProgressTimer = setInterval(() => {
    const elapsedSeconds = Math.max(
      Math.floor((Date.now() - startedAt) / 1000),
      0,
    );
    activeSyncElapsedSeconds.value = elapsedSeconds;
    const expectedPercent = Math.floor(
      6 + Math.min(elapsedSeconds / estimatedSeconds, 1) * 84,
    );
    activeSyncPercent.value = Math.min(
      90,
      Math.max(activeSyncPercent.value, expectedPercent),
    );
  }, 1000);
}

function finishSyncProgress(status: string) {
  if (syncProgressTimer) {
    clearInterval(syncProgressTimer);
    syncProgressTimer = undefined;
  }
  activeSyncStatus.value = status;
  activeSyncPercent.value = 100;
}

function failSyncProgress(status: string) {
  if (syncProgressTimer) {
    clearInterval(syncProgressTimer);
    syncProgressTimer = undefined;
  }
  activeSyncStatus.value = status;
  activeSyncPercent.value = 100;
}

function applyRequirementNoticeConfig(
  result?: DingTalkSyncApi.RequirementNoticeConfig,
) {
  if (!result) return;
  requirementNoticeEnabled.value = Boolean(result.enabled);
  requirementNoticeScenes.value = result.scenes || [];
  requirementNoticeStats.value = result.stats;
}

function noticeCount(value?: number) {
  return Number(value || 0);
}

function getRequirementNoticeSceneStats(scene?: string) {
  if (!scene) return undefined;
  return requirementNoticeStats.value?.sceneStats?.find(
    (item) => item.scene === scene,
  );
}

async function loadHistory(
  page = historyPagination.current,
  pageSize = historyPagination.pageSize,
) {
  if (historyLoading.value) return;
  historyLoading.value = true;
  try {
    const result = await getDingTalkSyncHistoryPage({
      pageNo: page,
      pageSize,
      syncType:
        historyTypeFilter.value === 'ALL' ? undefined : historyTypeFilter.value,
    });
    historyList.value = result.list || [];
    historyPagination.current = page;
    historyPagination.pageSize = pageSize;
    historyPagination.total = result.total || 0;
  } catch (error: any) {
    message.error(error?.message || '加载同步历史失败');
  } finally {
    historyLoading.value = false;
  }
}

async function loadRequirementNoticeConfig() {
  requirementNoticeLoading.value = true;
  try {
    const result = await getDingTalkRequirementNoticeConfig();
    applyRequirementNoticeConfig(result);
  } catch (error: any) {
    message.error(error?.message || '加载钉钉通知配置失败');
  } finally {
    requirementNoticeLoading.value = false;
  }
}

async function loadSystemUpdateAvailableUsers() {
  systemUpdateAvailableLoading.value = true;
  try {
    systemUpdateAvailableUsers.value =
      await getSystemUpdateNoticeAvailableUsers();
  } catch (error: any) {
    message.error(error?.message || '加载可通知人员失败');
  } finally {
    systemUpdateAvailableLoading.value = false;
  }
}

function applySyncConfig(result?: DingTalkSyncApi.SyncConfig) {
  if (!result) return;
  attendanceSyncEnabled.value = result.attendanceEnabled !== false;
  leaveSyncEnabled.value = result.leaveEnabled !== false;
}

async function loadSyncConfig() {
  syncConfigLoading.value = true;
  try {
    applySyncConfig(await getDingTalkSyncConfig());
  } catch (error: any) {
    message.error(error?.message || '加载同步开关失败');
  } finally {
    syncConfigLoading.value = false;
  }
}

async function handleAttendanceSyncToggle(checked: boolean) {
  if (attendanceSwitchSaving.value) return;
  const oldValue = attendanceSyncEnabled.value;
  attendanceSyncEnabled.value = checked;
  attendanceSwitchSaving.value = true;
  try {
    applySyncConfig(await updateDingTalkAttendanceSyncConfig(checked));
    message.success(checked ? '考勤同步已开启' : '考勤同步已关闭');
  } catch (error: any) {
    attendanceSyncEnabled.value = oldValue;
    message.error(error?.message || '更新考勤同步开关失败');
  } finally {
    attendanceSwitchSaving.value = false;
  }
}

async function handleLeaveSyncToggle(checked: boolean) {
  if (leaveSwitchSaving.value) return;
  const oldValue = leaveSyncEnabled.value;
  leaveSyncEnabled.value = checked;
  leaveSwitchSaving.value = true;
  try {
    applySyncConfig(await updateDingTalkLeaveSyncConfig(checked));
    message.success(checked ? '请假同步已开启' : '请假同步已关闭');
  } catch (error: any) {
    leaveSyncEnabled.value = oldValue;
    message.error(error?.message || '更新请假同步开关失败');
  } finally {
    leaveSwitchSaving.value = false;
  }
}

async function handleRequirementNoticeToggle(checked: boolean) {
  if (requirementNoticeSaving.value) return;
  const oldValue = requirementNoticeEnabled.value;
  requirementNoticeEnabled.value = checked;
  requirementNoticeSaving.value = true;
  try {
    const result = await updateDingTalkRequirementNoticeConfig(checked);
    applyRequirementNoticeConfig(result);
    message.success(
      requirementNoticeEnabled.value ? '钉钉通知已开启' : '钉钉通知已关闭',
    );
  } catch (error: any) {
    requirementNoticeEnabled.value = oldValue;
    message.error(error?.message || '更新钉钉通知配置失败');
  } finally {
    requirementNoticeSaving.value = false;
  }
}

async function handleRequirementNoticeSceneToggle(
  scene: string,
  checked: boolean,
) {
  if (!scene || requirementNoticeSceneSaving[scene]) return;
  const oldScenes = requirementNoticeScenes.value.map((item) => ({ ...item }));
  requirementNoticeScenes.value = requirementNoticeScenes.value.map((item) =>
    item.scene === scene ? { ...item, enabled: checked } : item,
  );
  requirementNoticeSceneSaving[scene] = true;
  try {
    const result = await updateDingTalkRequirementNoticeSceneConfig(
      scene,
      checked,
    );
    applyRequirementNoticeConfig(result);
    message.success(checked ? '场景推送已开启' : '场景推送已关闭');
  } catch (error: any) {
    requirementNoticeScenes.value = oldScenes;
    message.error(error?.message || '更新通知场景失败');
  } finally {
    requirementNoticeSceneSaving[scene] = false;
  }
}

async function handleResendRunningBpmCopyNotice() {
  if (bpmCopyNoticeResending.value) return;
  bpmCopyNoticeResending.value = true;
  try {
    const previewResult = await previewRunningBpmCopyNotice(200);
    const total = previewResult.receiverCount || 0;
    if (total <= 0) {
      message.info('当前没有可补发的 BPM 审批待办或知会通知');
      return;
    }
    Modal.confirm({
      title: '确认补发BPM通知？',
      content: `将通知审批待办 ${previewResult.taskReceiverCount || 0} 人，知会 ${previewResult.copyReceiverCount || 0} 人，总通知对象 ${total} 人。确认后立即发送。`,
      okText: '确认发送',
      cancelText: '取消',
      async onOk() {
        const result = await resendRunningBpmCopyNotice(200);
        message.success(
          `已补发：审批待办 ${result.taskReceiverCount || 0} 人，知会 ${result.copyReceiverCount || 0} 人，总通知对象 ${result.receiverCount || 0} 人`,
        );
        await loadRequirementNoticeConfig();
      },
    });
  } catch (error: any) {
    message.error(error?.message || '补发 BPM 通知失败');
  } finally {
    bpmCopyNoticeResending.value = false;
  }
}

function buildSystemUpdateNoticeReq(): DingTalkSyncApi.SystemUpdateNoticeReq {
  return {
    title: systemUpdateNotice.title?.trim(),
    content: systemUpdateNotice.content?.trim(),
    detailUrl: systemUpdateNotice.detailUrl?.trim(),
    includeLeaders: Boolean(systemUpdateNotice.includeLeaders),
    includeTechDept: Boolean(systemUpdateNotice.includeTechDept),
    roleIds: [...systemUpdateRoleIds.value],
    deptIds: [...systemUpdateDeptIds.value],
    receiverUserIds: [...systemUpdateManualUserIds.value],
    excludeUserIds: [...systemUpdateExcludedUserIds.value],
  };
}

function validateSystemUpdateNoticeReq() {
  if (!systemUpdateHasTarget.value) {
    message.warning('请选择至少一个接收范围或手动接收人');
    return false;
  }
  if (!systemUpdateNotice.content?.trim()) {
    message.warning('请输入通知内容');
    return false;
  }
  return true;
}

function filterSystemUpdateUserOption(input: string, option?: any) {
  return String(option?.label || '')
    .toLowerCase()
    .includes(input.toLowerCase());
}

function normalizeSystemUpdateIds(values: any) {
  return [
    ...new Set(
      (Array.isArray(values) ? values : []).map(Number).filter((id) => id > 0),
    ),
  ];
}

function resetSystemUpdatePreview() {
  systemUpdatePreview.value = undefined;
  systemUpdateReport.value = undefined;
}

function handleSystemUpdateRolesChange(values: any) {
  systemUpdateRoleIds.value = normalizeSystemUpdateIds(values);
  resetSystemUpdatePreview();
}

function handleSystemUpdateDeptsChange(values: any) {
  systemUpdateDeptIds.value = normalizeSystemUpdateIds(values);
  resetSystemUpdatePreview();
}

function handleSystemUpdateManualUsersChange(values: any) {
  const ids = normalizeSystemUpdateIds(values);
  systemUpdateManualUserIds.value = ids;
  systemUpdateExcludedUserIds.value = systemUpdateExcludedUserIds.value.filter(
    (id) => !ids.includes(id),
  );
  resetSystemUpdatePreview();
}

function countSystemUpdateSource(
  recipients: DingTalkSyncApi.SystemUpdateNoticeRecipient[],
  source: string,
) {
  return recipients.filter((item) => item.sources?.includes(source)).length;
}

function countSystemUpdateSourcePrefix(
  recipients: DingTalkSyncApi.SystemUpdateNoticeRecipient[],
  prefix: string,
) {
  return recipients.filter((item) =>
    item.sources?.some((source) => source.startsWith(prefix)),
  ).length;
}

function systemUpdateSourceColor(source?: string) {
  if (source === '手动选择') return 'purple';
  if (source === '技术部' || source?.startsWith('部门：')) return 'blue';
  if (source === '队长' || source?.startsWith('角色：')) return 'green';
  return 'green';
}

function getSystemUpdateRoleCount(
  summary?: DingTalkSyncApi.SystemUpdateNoticePreview,
) {
  return (summary?.roleCount || 0) + (summary?.leaderCount || 0);
}

function getSystemUpdateDeptCount(
  summary?: DingTalkSyncApi.SystemUpdateNoticePreview,
) {
  return (summary?.deptCount || 0) + (summary?.techDeptCount || 0);
}

function applySystemUpdateRecipientList(
  recipients: DingTalkSyncApi.SystemUpdateNoticeRecipient[],
) {
  const nextPreview = {
    ...systemUpdatePreview.value,
    recipients,
    totalCount: recipients.length,
    leaderCount: countSystemUpdateSource(recipients, '队长'),
    techDeptCount: countSystemUpdateSource(recipients, '技术部'),
    roleCount: countSystemUpdateSourcePrefix(recipients, '角色：'),
    deptCount: countSystemUpdateSourcePrefix(recipients, '部门：'),
    manualCount: countSystemUpdateSource(recipients, '手动选择'),
  };
  systemUpdatePreview.value = nextPreview;
  systemUpdateReport.value = undefined;
}

function handleRemoveSystemUpdateRecipient(
  record: DingTalkSyncApi.SystemUpdateNoticeRecipient,
) {
  const userId = Number(record.userId || 0);
  if (!userId) return;
  systemUpdateManualUserIds.value = systemUpdateManualUserIds.value.filter(
    (id) => id !== userId,
  );
  if (!systemUpdateExcludedUserIds.value.includes(userId)) {
    systemUpdateExcludedUserIds.value.push(userId);
  }
  applySystemUpdateRecipientList(
    systemUpdateRecipients.value.filter((item) => item.userId !== userId),
  );
}

function handleClearSystemUpdateExcludedUsers() {
  systemUpdateExcludedUserIds.value = [];
  systemUpdateReport.value = undefined;
  void handlePreviewSystemUpdateNotice();
}

async function handlePreviewSystemUpdateNotice() {
  if (systemUpdatePreviewing.value || !validateSystemUpdateNoticeReq()) return;
  systemUpdatePreviewing.value = true;
  try {
    systemUpdatePreview.value = await previewSystemUpdateNotice(
      buildSystemUpdateNoticeReq(),
    );
    systemUpdateReport.value = undefined;
    message.success(
      `接收人已更新：${systemUpdatePreview.value.totalCount || 0} 人`,
    );
  } catch (error: any) {
    message.error(error?.message || '预览系统更新通知失败');
  } finally {
    systemUpdatePreviewing.value = false;
  }
}

async function handleSendSystemUpdateNotice() {
  if (systemUpdateSending.value || !validateSystemUpdateNoticeReq()) return;
  systemUpdateSending.value = true;
  try {
    const req = buildSystemUpdateNoticeReq();
    const preview = await previewSystemUpdateNotice(req);
    systemUpdatePreview.value = preview;
    const total = preview.totalCount || 0;
    if (total <= 0) {
      message.info('当前没有可发送的系统更新通知接收人');
      return;
    }
    Modal.confirm({
      title: '确认发送系统更新卡片？',
      content: `将发送给 ${total} 人，其中角色匹配 ${getSystemUpdateRoleCount(preview)} 人，部门匹配 ${getSystemUpdateDeptCount(preview)} 人，手动选择 ${preview.manualCount || 0} 人。`,
      okText: '确认发送',
      cancelText: '取消',
      async onOk() {
        systemUpdateSending.value = true;
        try {
          const result = await sendSystemUpdateNotice(req);
          systemUpdateReport.value = result;
          systemUpdatePreview.value = result;
          if ((result.failedCount || 0) > 0) {
            message.warning(
              `已发送 ${result.sentCount || 0} 人，失败 ${result.failedCount || 0} 人`,
            );
          } else {
            message.success(`系统更新卡片已发送：${result.sentCount || 0} 人`);
          }
        } catch (error: any) {
          message.error(error?.message || '发送系统更新通知失败');
          throw error;
        } finally {
          systemUpdateSending.value = false;
        }
      },
    });
  } catch (error: any) {
    message.error(error?.message || '发送系统更新通知失败');
  } finally {
    systemUpdateSending.value = false;
  }
}

function handleHistoryTableChange(pagination: any) {
  const current = Number(pagination?.current || 1);
  const pageSize = Number(pagination?.pageSize || historyPagination.pageSize);
  loadHistory(current, pageSize);
}

async function handleSyncEmployees() {
  if (isAnyManualSyncing.value) return;
  employeePreviewing.value = true;
  historyTypeFilter.value = 'EMPLOYEE_PREVIEW';
  beginSyncProgress('用户同步预览', '正在拉取钉钉用户并匹配 OA 账号', 60);
  try {
    employeePreview.value = await previewDingTalkEmployees({
      autoCreateDeptId: Math.max(Number(autoCreateDeptId.value) || 1, 1),
      autoCreateMissing: autoCreateMissing.value === 1,
    });
    employeeReport.value = undefined;
    message.success(
      `预览完成：待创建 ${employeePreview.value.toCreate || 0}，待更新 ${employeePreview.value.updated || 0}，去重跳过 ${employeePreview.value.skippedDuplicateMapping || 0}`,
    );
    finishSyncProgress(
      `预览完成：待创建 ${employeePreview.value.toCreate || 0}，待更新 ${employeePreview.value.updated || 0}`,
    );
    await loadHistory(1, historyPagination.pageSize);
  } catch (error: any) {
    failSyncProgress(error?.message || '用户预览失败');
    message.error(error?.message || '用户预览失败');
  } finally {
    employeePreviewing.value = false;
  }
}

async function handleConfirmSyncEmployees() {
  if (isAnyManualSyncing.value) return;
  employeeSyncing.value = true;
  historyTypeFilter.value = 'EMPLOYEE_SYNC';
  beginSyncProgress('用户同步', '正在写入 OA 用户、HR 档案与钉钉绑定', 90);
  try {
    employeeReport.value = await syncDingTalkEmployees({
      autoCreateDeptId: Math.max(Number(autoCreateDeptId.value) || 1, 1),
      autoCreateMissing: autoCreateMissing.value === 1,
    });
    employeePreview.value = undefined;
    message.success(
      `用户同步完成：新建 ${employeeReport.value.created || 0}，更新 ${employeeReport.value.updated || 0}，去重跳过 ${employeeReport.value.skippedDuplicateMapping || 0}`,
    );
    finishSyncProgress(
      `用户同步完成：新建 ${employeeReport.value.created || 0}，更新 ${employeeReport.value.updated || 0}`,
    );
    await loadHistory(1, historyPagination.pageSize);
  } catch (error: any) {
    failSyncProgress(error?.message || '确认同步失败');
    message.error(error?.message || '确认同步失败');
  } finally {
    employeeSyncing.value = false;
  }
}

function handleSyncRoster() {
  if (isAnyManualSyncing.value) return;
  Modal.confirm({
    title: '确认同步钉钉花名册？',
    content:
      '只同步已有钉钉绑定员工的花名册字段，不创建账号或部门；默认仅补齐 OA 空字段。',
    okText: '确认同步',
    cancelText: '取消',
    async onOk() {
      rosterSyncing.value = true;
      historyTypeFilter.value = 'ROSTER_SYNC';
      beginSyncProgress(
        '花名册同步',
        '正在拉取钉钉智能人事花名册并更新已有员工档案',
        90,
      );
      try {
        employeeReport.value = await syncDingTalkRoster();
        employeePreview.value = undefined;
        const summary = `花名册同步完成：拉取 ${employeeReport.value.rosterPulledUsers || 0} 人，更新 ${employeeReport.value.rosterSyncedProfiles || 0} 人，失败 ${employeeReport.value.rosterSyncFailed || 0}`;
        message.success(summary);
        finishSyncProgress(summary);
        await loadHistory(1, historyPagination.pageSize);
      } catch (error: any) {
        failSyncProgress(error?.message || '花名册同步失败');
        message.error(error?.message || '花名册同步失败');
      } finally {
        rosterSyncing.value = false;
      }
    },
  });
}

async function handleFullSync() {
  if (isAnyManualSyncing.value) return;
  fullSyncing.value = true;
  historyTypeFilter.value = 'ALL';
  beginSyncProgress(
    '每日全量同步',
    '正在依次同步用户、考勤和请假，适合半夜补偿任务手动触发',
    120,
  );
  try {
    const result = await runDingTalkFullSync();
    message.success(result.result || '全量同步触发成功');
    finishSyncProgress(result.result || '每日全量同步完成');
    await loadHistory(1, historyPagination.pageSize);
  } catch (error: any) {
    failSyncProgress(error?.message || '全量同步失败');
    message.error(error?.message || '全量同步失败');
  } finally {
    fullSyncing.value = false;
  }
}

async function handlePreviewAttendance() {
  if (isAnyManualSyncing.value) return;
  previewing.value = true;
  historyTypeFilter.value = 'ATTENDANCE_PREVIEW';
  beginSyncProgress('考勤预览', '正在拉取钉钉考勤样本，不写入 OA', 30);
  try {
    previewData.value = await previewDingTalkAttendance(
      Math.max(Number(lookbackMinutes.value) || 30, 1),
      20,
    );
    message.success('考勤预览完成');
    finishSyncProgress(
      `考勤预览完成：拉取 ${previewData.value.pulledRecords || 0} 条`,
    );
    await loadHistory(1, historyPagination.pageSize);
  } catch (error: any) {
    failSyncProgress(error?.message || '考勤预览失败');
    message.error(error?.message || '考勤预览失败');
  } finally {
    previewing.value = false;
  }
}

async function handleConfirmAttendance() {
  if (isAnyManualSyncing.value) return;
  syncing.value = true;
  historyTypeFilter.value = 'ATTENDANCE_SYNC';
  beginSyncProgress('考勤同步', '正在同步钉钉考勤记录到 OA', 45);
  try {
    const result = await syncDingTalkAttendance(
      Math.max(Number(lookbackMinutes.value) || 30, 1),
    );
    message.success(`考勤同步完成：${result.syncedRecords || 0} 条`);
    finishSyncProgress(`考勤同步完成：${result.syncedRecords || 0} 条`);
    await loadHistory(1, historyPagination.pageSize);
  } catch (error: any) {
    failSyncProgress(error?.message || '考勤同步失败');
    message.error(error?.message || '考勤同步失败');
  } finally {
    syncing.value = false;
  }
}

async function handleConfirmLeave() {
  if (isAnyManualSyncing.value) return;
  leaveSyncing.value = true;
  historyTypeFilter.value = 'LEAVE_SYNC';
  const lookbackDays = safeInteger(leaveLookbackDays.value, 1, 1);
  const forwardDays = safeInteger(leaveForwardDays.value, 0, 0);
  leaveLookbackDays.value = lookbackDays;
  leaveForwardDays.value = forwardDays;
  beginSyncProgress(
    '请假同步',
    `只同步请假：${lookbackDays} 天回溯，未来 ${forwardDays} 天`,
    Math.max(20, Math.ceil((lookbackDays + forwardDays) * 1.5)),
  );
  try {
    leaveReport.value = await syncDingTalkLeave(lookbackDays, forwardDays);
    const summary = `请假同步完成：${leaveReport.value.syncedLeaves || 0} 条，接口 ${leaveReport.value.apiCalls || 0} 次`;
    message.success(summary);
    finishSyncProgress(summary);
    await loadHistory(1, historyPagination.pageSize);
  } catch (error: any) {
    failSyncProgress(error?.message || '请假同步失败');
    message.error(error?.message || '请假同步失败');
  } finally {
    leaveSyncing.value = false;
  }
}

onMounted(() => {
  loadDepts();
  loadRoles();
  loadSyncConfig();
  loadRequirementNoticeConfig();
  loadSystemUpdateAvailableUsers();
  loadHistory();
});

onUnmounted(() => {
  if (syncProgressTimer) {
    clearInterval(syncProgressTimer);
  }
});
</script>

<template>
  <Page
    auto-content-height
    class="!bg-white !p-0"
    content-class="!bg-white !p-0 !overflow-y-auto"
  >
    <div class="dingtalk-sync-shell">
      <Tabs default-active-key="sync">
        <Tabs.TabPane key="sync" tab="同步任务">
          <div class="mb-3 rounded border border-gray-200 p-3">
            <div class="mb-1 text-base font-semibold">
              1. 用户登录与账号映射
            </div>
            <div class="mb-3 text-gray-500">
              先预览，再确认。会先写入 system_user_sync，再更新 OA 用户和 HR
              档案。
            </div>
            <div class="flex flex-wrap items-center gap-2">
              <Button
                type="primary"
                :loading="employeePreviewing"
                :disabled="isAnyManualSyncing && !employeePreviewing"
                @click="handleSyncEmployees"
              >
                预览用户同步
              </Button>
              <Button
                type="primary"
                ghost
                :loading="employeeSyncing"
                :disabled="
                  !employeePreview || (isAnyManualSyncing && !employeeSyncing)
                "
                @click="handleConfirmSyncEmployees"
              >
                确认同步用户
              </Button>
            </div>
          </div>

          <div class="mb-3 rounded border border-gray-200 p-3">
            <div class="mb-1 text-base font-semibold">
              2. 资料类（用户档案）
            </div>
            <div class="mb-3 text-gray-500">
              自动补人会按钉钉部门匹配 OA
              现有部门，匹配不到会跳过，不再自动新建部门。
            </div>
            <div class="flex flex-wrap items-center gap-2">
              <span>建档部门</span>
              <TreeSelect
                v-model:value="autoCreateDeptId"
                :loading="deptLoading"
                :tree-data="deptTreeData"
                allow-clear
                placeholder="选择自动建档部门"
                show-search
                style="min-width: 220px"
                tree-node-filter-prop="title"
              />
              <span>自动建档</span>
              <Select
                v-model:value="autoCreateMissing"
                :options="[
                  { label: '开启', value: 1 },
                  { label: '关闭', value: 0 },
                ]"
                style="width: 90px"
              />
              <Button
                type="primary"
                ghost
                :disabled="isAnyManualSyncing && !fullSyncing"
                :loading="fullSyncing"
                @click="handleFullSync"
              >
                执行每日全量同步
              </Button>
              <Button
                type="primary"
                :loading="rosterSyncing"
                :disabled="isAnyManualSyncing && !rosterSyncing"
                @click="handleSyncRoster"
              >
                同步钉钉花名册
              </Button>
            </div>
          </div>

          <div class="mb-3 rounded border border-gray-200 p-3">
            <div class="mb-1 text-base font-semibold">3. 考勤类</div>
            <div class="mb-3 text-gray-500">
              考勤和请假都是 stream
              实时同步、定时任务半夜补偿；这里用于手动核对。
            </div>
            <div class="mb-3 flex flex-wrap items-center gap-4">
              <span>考勤同步</span>
              <Switch
                :checked="attendanceSyncEnabled"
                :disabled="syncConfigLoading"
                :loading="attendanceSwitchSaving"
                checked-children="开启"
                un-checked-children="关闭"
                @change="handleAttendanceSyncToggle"
              />
              <span>请假同步</span>
              <Switch
                :checked="leaveSyncEnabled"
                :disabled="syncConfigLoading"
                :loading="leaveSwitchSaving"
                checked-children="开启"
                un-checked-children="关闭"
                @change="handleLeaveSyncToggle"
              />
            </div>
            <div class="flex flex-wrap items-center gap-2">
              <span>考勤回溯分钟</span>
              <InputNumber
                v-model:value="lookbackMinutes"
                :min="1"
                :max="10080"
                :step="10"
              />
              <Button
                type="dashed"
                :disabled="
                  !attendanceSyncEnabled || (isAnyManualSyncing && !previewing)
                "
                :loading="previewing"
                @click="handlePreviewAttendance"
              >
                预览考勤
              </Button>
              <Button
                type="primary"
                :disabled="
                  !attendanceSyncEnabled || (isAnyManualSyncing && !syncing)
                "
                :loading="syncing"
                @click="handleConfirmAttendance"
              >
                确认同步考勤
              </Button>
              <span>请假回溯天数</span>
              <InputNumber
                v-model:value="leaveLookbackDays"
                :min="1"
                :max="366"
                :step="1"
              />
              <span>未来天数</span>
              <InputNumber
                v-model:value="leaveForwardDays"
                :min="0"
                :max="366"
                :step="1"
              />
              <Button
                type="primary"
                ghost
                :disabled="
                  !leaveSyncEnabled || (isAnyManualSyncing && !leaveSyncing)
                "
                :loading="leaveSyncing"
                @click="handleConfirmLeave"
              >
                确认同步请假
              </Button>
            </div>
          </div>

          <div
            v-if="activeSyncTitle"
            class="mb-3 rounded border border-blue-100 bg-blue-50 p-3"
          >
            <Alert
              show-icon
              type="info"
              :message="activeSyncTitle"
              :description="activeSyncStatus"
            />
            <div class="mt-3 flex flex-wrap items-center gap-3">
              <Progress
                class="max-w-xl flex-1"
                :percent="activeSyncPercent"
                :status="activeSyncPercent >= 100 ? 'success' : 'active'"
              />
              <Tag>耗时 {{ activeSyncElapsedSeconds }} 秒</Tag>
            </div>
          </div>

          <div v-if="employeeSummary" class="mb-3 flex flex-wrap gap-2">
            <Tag color="blue">
              总数 {{ employeeSummary.totalFromDingTalk || 0 }}
            </Tag>
            <Tag color="purple">
              自动建档
              {{ employeeSummary.autoCreateEnabled ? '开启' : '关闭' }}
            </Tag>
            <Tag v-if="employeeSummary.autoCreateDeptId" color="geekblue">
              已配置建档部门
            </Tag>
            <Tag color="green">
              手机号匹配 {{ employeeSummary.matchedByMobile || 0 }}
            </Tag>
            <Tag color="cyan">
              姓名匹配 {{ employeeSummary.matchedByName || 0 }}
            </Tag>
            <Tag v-if="employeePreview" color="cyan">
              待创建 {{ employeePreview.toCreate || 0 }}
            </Tag>
            <Tag v-else color="green">
              新建 {{ employeeReport?.created || 0 }}
            </Tag>
            <Tag v-if="employeeReport" color="red">
              创建失败 {{ employeeReport.createFailed || 0 }}
            </Tag>
            <Tag color="orange">
              建档信息缺失 {{ employeeSummary.skippedCreateInvalid || 0 }}
            </Tag>
            <Tag color="orange">
              未匹配 {{ employeeSummary.unmatched || 0 }}
            </Tag>
            <Tag color="gold">
              去重跳过 {{ employeeSummary.skippedDuplicateMapping || 0 }}
            </Tag>
            <Tag color="red">
              无身份信息 {{ employeeSummary.skippedNoMobile || 0 }}
            </Tag>
            <Tag color="purple">已更新 {{ employeeSummary.updated || 0 }}</Tag>
            <Tag color="cyan">
              花名册拉取 {{ employeeSummary.rosterPulledUsers || 0 }}
            </Tag>
            <Tag v-if="employeeReport" color="green">
              花名册同步 {{ employeeReport.rosterSyncedProfiles || 0 }}
            </Tag>
            <Tag v-if="employeeReport" color="geekblue">
              档案字段更新 {{ employeeReport.rosterUpdatedProfiles || 0 }}
            </Tag>
            <Tag v-if="employeeReport" color="blue">
              扩展字段更新 {{ employeeReport.rosterCustomFields || 0 }}
            </Tag>
            <Tag v-if="employeeReport?.rosterSyncFailed" color="red">
              花名册失败 {{ employeeReport.rosterSyncFailed || 0 }}
            </Tag>
            <Tag v-if="employeeReport" color="blue">
              同步表入库 {{ employeeReport.stagedToUserSync || 0 }}
            </Tag>
            <Tag v-if="employeeReport" color="gold">
              缺少钉钉ID {{ employeeReport.skippedNoExternalUserId || 0 }}
            </Tag>
            <Tag v-if="employeeReport" color="blue">
              绑定更新 {{ employeeReport.syncedBindings || 0 }}
            </Tag>
            <Tag v-if="employeeReport" color="geekblue">
              职位更新 {{ employeeReport.syncedJobTitles || 0 }}
            </Tag>
          </div>

          <div v-if="employeeSummary" class="mb-2 flex items-center gap-2">
            <span>明细筛选</span>
            <Select
              v-model:value="detailFilter"
              :options="detailFilterOptions"
              style="width: 140px"
            />
            <Input
              v-model:value="detailKeyword"
              placeholder="姓名/手机号/钉钉账号"
              style="width: 260px"
              allow-clear
            />
            <Tag>条数 {{ filteredDetails.length }}</Tag>
          </div>

          <Table
            v-if="employeeSummary"
            class="mb-4"
            row-key="dingUserId"
            :columns="detailColumns"
            :data-source="filteredDetails"
            :pagination="{ pageSize: 10 }"
            :scroll="{ x: 1200 }"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'matchType'">
                {{ matchTypeLabel(record.matchType) }}
              </template>
              <template v-else-if="column.dataIndex === 'action'">
                <Tag :color="actionColor(record.action)">
                  {{ actionLabel(record.action) }}
                </Tag>
              </template>
              <template v-else-if="column.dataIndex === 'changedFields'">
                {{ record.changedFields || '-' }}
              </template>
            </template>
          </Table>

          <div v-if="previewData" class="mb-2 flex flex-wrap gap-2">
            <Tag color="blue">映射用户 {{ previewData.mappedUsers || 0 }}</Tag>
            <Tag color="green">
              拉取记录 {{ previewData.pulledRecords || 0 }}
            </Tag>
            <Tag color="orange">
              跳过记录 {{ previewData.skippedRecords || 0 }}
            </Tag>
            <Tag>
              时间范围 {{ previewData.fromTime }} ~
              {{ previewData.toTime }}
            </Tag>
          </div>

          <div v-if="leaveReport" class="mb-2 flex flex-wrap gap-2">
            <Tag color="blue">映射用户 {{ leaveReport.mappedUsers || 0 }}</Tag>
            <Tag color="green">
              请假天数 {{ leaveReport.pulledLeaveDays || 0 }}
            </Tag>
            <Tag color="cyan">新建 {{ leaveReport.createdLeaves || 0 }}</Tag>
            <Tag color="purple">
              更新 {{ leaveReport.updatedLeaves || 0 }}
            </Tag>
            <Tag color="gold">
              取消 {{ leaveReport.cancelledLeaves || 0 }}
            </Tag>
            <Tag color="red">失败 {{ leaveReport.failedCalls || 0 }}</Tag>
            <Tag color="geekblue">
              模式 {{ syncModeLabel(leaveReport.syncMode) }}
            </Tag>
            <Tag color="blue">接口 {{ leaveReport.apiCalls || 0 }} 次</Tag>
            <Tag>
              日期 {{ leaveReport.fromDate }} ~ {{ leaveReport.toDate }}
            </Tag>
          </div>

          <Table
            v-if="previewData"
            class="mb-4"
            row-key="sourceRecordId"
            :columns="attendanceColumns"
            :data-source="previewData.sampleRecords || []"
            :pagination="false"
            :scroll="{ x: 900 }"
            size="small"
          />

          <div
            class="mb-2 mt-4 flex flex-wrap items-center justify-between gap-2"
          >
            <div class="text-base font-semibold">4. 同步历史</div>
            <div class="flex items-center gap-2">
              <span class="text-sm text-gray-500">类型</span>
              <Select
                v-model:value="historyTypeFilter"
                :options="historyTypeOptions"
                style="width: 150px"
                @change="() => loadHistory(1, historyPagination.pageSize)"
              />
            </div>
          </div>
          <Table
            row-key="id"
            :columns="historyColumns"
            :data-source="historyList"
            :loading="historyLoading"
            :pagination="{
              current: historyPagination.current,
              pageSize: historyPagination.pageSize,
              total: historyPagination.total,
              showSizeChanger: true,
              pageSizeOptions: ['10', '20', '50'],
            }"
            :scroll="{ x: 1700 }"
            size="small"
            @change="handleHistoryTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'syncType'">
                {{ syncTypeLabel(record.syncType) }}
              </template>
              <template v-else-if="column.dataIndex === 'syncScope'">
                {{ syncScopeLabel(record.syncScope) }}
              </template>
              <template v-else-if="column.dataIndex === 'triggerMode'">
                {{ triggerModeLabel(record.triggerMode) }}
              </template>
              <template v-else-if="column.dataIndex === 'syncEndTime'">
                {{ formatDateTime(record.syncEndTime || record.createTime) }}
              </template>
              <template v-else-if="column.dataIndex === 'summary'">
                {{ record.summary || '-' }}
              </template>
            </template>
          </Table>
        </Tabs.TabPane>

        <Tabs.TabPane key="notice" tab="通知场景">
          <div class="mb-3 rounded border border-blue-200 bg-blue-50 p-3">
            <div class="flex flex-wrap items-center justify-between gap-3">
              <div>
                <div class="mb-1 text-base font-semibold">钉钉通知总开关</div>
                <div class="text-gray-500">
                  总开关关闭后，下面所有场景都会停止发送；场景开关用于单独关闭某类通知。
                </div>
              </div>
              <Switch
                :checked="requirementNoticeEnabled"
                :disabled="requirementNoticeLoading"
                :loading="requirementNoticeSaving"
                checked-children="开启"
                un-checked-children="关闭"
                @change="handleRequirementNoticeToggle"
              />
            </div>
          </div>

          <div
            class="mb-3 flex flex-wrap items-center justify-between gap-3 rounded border border-orange-200 bg-orange-50 p-3"
          >
            <div>
              <div class="mb-1 text-base font-semibold">BPM知会补发</div>
              <div class="text-gray-500">
                用于上线后补发已存在的运行中流程审批待办和知会通知，最多各扫描最近
                200 条。
              </div>
            </div>
            <Button
              type="primary"
              :disabled="!requirementNoticeEnabled"
              :loading="bpmCopyNoticeResending"
              @click="handleResendRunningBpmCopyNotice"
            >
              补发BPM通知
            </Button>
          </div>

          <div class="mb-3 rounded border border-blue-200 bg-white p-3">
            <div class="mb-3 flex flex-wrap items-start justify-between gap-3">
              <div>
                <div
                  class="mb-1 flex items-center gap-2 text-base font-semibold"
                >
                  <span>系统更新通知</span>
                  <Tag color="blue">卡片推送</Tag>
                </div>
                <div class="text-gray-500">
                  可按角色、部门、人员组合通知，预览后再确认发送。
                </div>
              </div>
              <div class="flex flex-wrap items-center gap-2">
                <Tag color="blue">
                  接收人
                  {{
                    systemUpdateReport?.totalCount ||
                    systemUpdatePreview?.totalCount ||
                    0
                  }}
                </Tag>
                <Tag
                  v-if="
                    getSystemUpdateRoleCount(
                      systemUpdateReport || systemUpdatePreview,
                    )
                  "
                  color="green"
                >
                  角色
                  {{
                    getSystemUpdateRoleCount(
                      systemUpdateReport || systemUpdatePreview,
                    )
                  }}
                </Tag>
                <Tag
                  v-if="
                    getSystemUpdateDeptCount(
                      systemUpdateReport || systemUpdatePreview,
                    )
                  "
                  color="blue"
                >
                  部门
                  {{
                    getSystemUpdateDeptCount(
                      systemUpdateReport || systemUpdatePreview,
                    )
                  }}
                </Tag>
                <Tag
                  v-if="
                    systemUpdateReport?.manualCount ||
                    systemUpdatePreview?.manualCount
                  "
                  color="purple"
                >
                  手动
                  {{
                    systemUpdateReport?.manualCount ||
                    systemUpdatePreview?.manualCount ||
                    0
                  }}
                </Tag>
                <Tag v-if="systemUpdateReport" color="green">
                  已发送 {{ systemUpdateReport.sentCount || 0 }}
                </Tag>
                <Tag v-if="systemUpdateReport?.failedCount" color="red">
                  失败 {{ systemUpdateReport.failedCount || 0 }}
                </Tag>
              </div>
            </div>

            <div class="grid gap-3 lg:grid-cols-[minmax(0,1fr)_320px]">
              <div class="grid gap-3">
                <Input
                  v-model:value="systemUpdateNotice.title"
                  :maxlength="80"
                  placeholder="卡片标题"
                  show-count
                />
                <Input.TextArea
                  v-model:value="systemUpdateNotice.content"
                  :maxlength="500"
                  :rows="4"
                  placeholder="通知内容"
                  show-count
                />
                <Input
                  v-model:value="systemUpdateNotice.detailUrl"
                  :maxlength="500"
                  placeholder="跳转链接（可选，默认打开钉钉同步页）"
                  allow-clear
                />
                <div class="grid gap-3 md:grid-cols-2">
                  <div class="grid gap-1">
                    <div class="text-sm font-medium text-gray-700">
                      通知角色
                    </div>
                    <Select
                      v-model:value="systemUpdateRoleIds"
                      mode="multiple"
                      show-search
                      allow-clear
                      :filter-option="filterSystemUpdateUserOption"
                      :loading="roleLoading"
                      max-tag-count="responsive"
                      :options="systemUpdateRoleOptions"
                      placeholder="选择一个或多个角色"
                      @change="handleSystemUpdateRolesChange"
                    />
                  </div>
                  <div class="grid gap-1">
                    <div class="text-sm font-medium text-gray-700">
                      通知部门
                    </div>
                    <TreeSelect
                      v-model:value="systemUpdateDeptIds"
                      :loading="deptLoading"
                      :tree-data="deptTreeData"
                      max-tag-count="responsive"
                      allow-clear
                      multiple
                      placeholder="选择一个或多个部门"
                      show-search
                      tree-checkable
                      tree-node-filter-prop="title"
                      @change="handleSystemUpdateDeptsChange"
                    />
                  </div>
                </div>
                <div class="flex flex-wrap items-center gap-2">
                  <Tag
                    v-if="systemUpdateExcludedUserIds.length > 0"
                    color="orange"
                  >
                    已移除 {{ systemUpdateExcludedUserIds.length }} 人
                  </Tag>
                  <Button
                    v-if="systemUpdateExcludedUserIds.length > 0"
                    size="small"
                    @click="handleClearSystemUpdateExcludedUsers"
                  >
                    恢复自动范围
                  </Button>
                </div>
                <Select
                  v-model:value="systemUpdateManualUserIds"
                  mode="multiple"
                  show-search
                  allow-clear
                  :filter-option="filterSystemUpdateUserOption"
                  :loading="systemUpdateAvailableLoading"
                  :options="systemUpdateUserOptions"
                  placeholder="全局搜索接收人，手动添加到本次通知"
                  @change="handleSystemUpdateManualUsersChange"
                />
                <div class="flex flex-wrap items-center gap-3">
                  <Button
                    :loading="systemUpdatePreviewing"
                    :disabled="systemUpdateSending"
                    @click="handlePreviewSystemUpdateNotice"
                  >
                    预览接收人
                  </Button>
                  <Button
                    type="primary"
                    :loading="systemUpdateSending"
                    :disabled="systemUpdatePreviewing || !systemUpdateHasTarget"
                    @click="handleSendSystemUpdateNotice"
                  >
                    发送系统更新卡片
                  </Button>
                </div>
              </div>

              <div class="overflow-hidden rounded border border-gray-200">
                <div class="bg-blue-600 px-4 py-3 text-white">
                  <div class="text-sm opacity-90">系统更新</div>
                  <div class="mt-1 text-base font-semibold">
                    {{ systemUpdateNotice.title || '连途系统更新通知' }}
                  </div>
                </div>
                <div class="bg-gray-50 p-4">
                  <div class="mb-3 whitespace-pre-wrap text-sm text-gray-700">
                    {{ systemUpdateNotice.content || '连途系统正在更新中。' }}
                  </div>
                  <div class="grid gap-2 text-xs text-gray-600">
                    <div
                      class="flex justify-between rounded bg-white px-3 py-2"
                    >
                      <span>通知类型</span>
                      <span class="font-medium">系统更新</span>
                    </div>
                    <div
                      class="flex justify-between rounded bg-white px-3 py-2"
                    >
                      <span>接收范围</span>
                      <span class="font-medium">
                        {{ systemUpdateScopeLabel }}
                      </span>
                    </div>
                    <div
                      class="flex justify-between rounded bg-white px-3 py-2"
                    >
                      <span>接收人数</span>
                      <span class="font-medium">
                        {{
                          systemUpdateReport?.totalCount ||
                          systemUpdatePreview?.totalCount ||
                          0
                        }}
                        人
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <Table
              v-if="systemUpdateRecipients.length > 0"
              class="mt-3"
              row-key="userId"
              :columns="systemUpdateRecipientColumns"
              :data-source="systemUpdateRecipients"
              :pagination="{ pageSize: 8 }"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'sources'">
                  <Tag
                    v-for="source in record.sources || []"
                    :key="source"
                    :color="systemUpdateSourceColor(source)"
                  >
                    {{ source }}
                  </Tag>
                </template>
                <template v-else-if="column.dataIndex === 'action'">
                  <Button
                    type="link"
                    size="small"
                    danger
                    @click="handleRemoveSystemUpdateRecipient(record)"
                  >
                    移除
                  </Button>
                </template>
              </template>
            </Table>
          </div>

          <div class="notice-stat-grid mb-3 grid gap-3 md:grid-cols-4">
            <div
              class="notice-stat-cell rounded border border-gray-200 bg-white p-3"
            >
              <div class="text-xs text-gray-500">今日推送</div>
              <div class="mt-1 text-xl font-semibold text-blue-600">
                {{ noticeCount(requirementNoticeStats?.todaySentCount) }}
              </div>
            </div>
            <div
              class="notice-stat-cell rounded border border-gray-200 bg-white p-3"
            >
              <div class="text-xs text-gray-500">累计推送</div>
              <div class="mt-1 text-xl font-semibold">
                {{ noticeCount(requirementNoticeStats?.totalSentCount) }}
              </div>
            </div>
            <div
              class="notice-stat-cell rounded border border-gray-200 bg-white p-3"
            >
              <div class="text-xs text-gray-500">当前分钟</div>
              <div class="mt-1 text-xl font-semibold text-orange-500">
                {{
                  noticeCount(requirementNoticeStats?.currentMinuteSentCount)
                }}
              </div>
            </div>
            <div
              class="notice-stat-cell rounded border border-gray-200 bg-white p-3"
            >
              <div class="text-xs text-gray-500">限流配置</div>
              <div class="mt-1 text-sm font-medium text-gray-700">
                单人
                {{
                  noticeCount(requirementNoticeStats?.perUserLimitPerMinute)
                }}/分钟
              </div>
              <div class="mt-1 text-xs text-gray-500">
                租户
                {{
                  noticeCount(requirementNoticeStats?.tenantLimitPerMinute)
                }}/分钟，去重
                {{ noticeCount(requirementNoticeStats?.dedupTtlMinutes) }} 分钟
              </div>
            </div>
          </div>

          <div class="notice-scene-list grid gap-3">
            <div
              v-for="scene in requirementNoticeScenes"
              :key="scene.scene || scene.name"
              class="notice-scene-row rounded border border-gray-200 p-3"
            >
              <div class="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <div
                    class="mb-1 flex items-center gap-2 text-base font-semibold"
                  >
                    <span>{{ scene.name }}</span>
                    <Tag :color="scene.enabled ? 'green' : 'default'">
                      {{ scene.enabled ? '推送中' : '已关闭' }}
                    </Tag>
                  </div>
                  <div class="text-gray-500">{{ scene.description }}</div>
                </div>
                <Switch
                  :checked="Boolean(scene.enabled)"
                  :disabled="
                    requirementNoticeLoading || !requirementNoticeEnabled
                  "
                  :loading="
                    scene.scene
                      ? requirementNoticeSceneSaving[scene.scene]
                      : false
                  "
                  checked-children="开启"
                  un-checked-children="关闭"
                  @change="
                    (checked) =>
                      handleRequirementNoticeSceneToggle(
                        scene.scene || '',
                        Boolean(checked),
                      )
                  "
                />
              </div>
              <div class="mt-3 grid gap-2 text-sm md:grid-cols-3">
                <div class="rounded bg-gray-50 px-3 py-2">
                  <div class="text-xs text-gray-500">今日</div>
                  <div class="mt-1 font-semibold">
                    {{
                      noticeCount(
                        getRequirementNoticeSceneStats(scene.scene)
                          ?.todaySentCount,
                      )
                    }}
                  </div>
                </div>
                <div class="rounded bg-gray-50 px-3 py-2">
                  <div class="text-xs text-gray-500">累计</div>
                  <div class="mt-1 font-semibold">
                    {{
                      noticeCount(
                        getRequirementNoticeSceneStats(scene.scene)
                          ?.totalSentCount,
                      )
                    }}
                  </div>
                </div>
                <div class="rounded bg-gray-50 px-3 py-2">
                  <div class="text-xs text-gray-500">当前分钟</div>
                  <div class="mt-1 font-semibold">
                    {{
                      noticeCount(
                        getRequirementNoticeSceneStats(scene.scene)
                          ?.currentMinuteSentCount,
                      )
                    }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </Tabs.TabPane>
      </Tabs>
    </div>
  </Page>
</template>

<style scoped>
.dingtalk-sync-shell {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
  padding: 16px;
}
</style>
