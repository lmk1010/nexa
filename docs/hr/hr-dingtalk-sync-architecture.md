# HR x DingTalk Sync Architecture (Phase 1)

## 1. Goals
- Keep DingTalk as current attendance source, OA as business system.
- Build low-coupling integration layer for:
  - employee data sync
  - attendance sync
  - approval status notification to DingTalk
- Enable future migration to OA-first + self-owned attendance devices.

## 2. Bounded Context
- `HR Core` (existing): employee, attendance, onboarding, approval business.
- `DingTalk Integration` (new): pull/push adapter layer.

The integration layer is isolated under `com.kyx.service.hr.integration.dingtalk`.

## 3. Phase 1 Data Flow
### 3.1 Employee Sync (DingTalk -> OA)
1. Pull DingTalk departments and users.
2. Match OA employee profiles by mobile.
3. Update only safe profile fields:
   - `name`
   - `email`
   - `status` (active/inactive)
4. Unmatched users are reported for manual binding.

### 3.2 Attendance Sync (DingTalk -> OA)
1. Build DingTalk user -> OA user mapping by mobile + profile.userId.
2. Pull clock records from DingTalk in batches.
3. Convert records to `AttendanceSyncReqVO.Record`.
4. Reuse existing `AttendanceClockRecordService.syncDingTalk()` for upsert/idempotent write.

### 3.3 Approval Notification (OA -> DingTalk)
1. Listen to BPM status event topic.
2. Resolve target DingTalk user from local userId -> mobile -> DingTalk userId.
3. Send DingTalk app message (`asyncsend_v2`) with process/status summary.

## 4. Scheduling
- `DingTalkSyncScheduleJob` supports tenant loop and fixed-delay execution.
- Feature flags:
  - `dingtalk.sync.enabled`
  - `dingtalk.sync.employee-enabled`
  - `dingtalk.sync.attendance-enabled`

## 5. Config Keys
- Token:
  - `dingtalk.app.app-key`
  - `dingtalk.app.app-secret`
- Runtime:
  - `dingtalk.sync.fixed-delay-ms`
  - `dingtalk.sync.attendance-lookback-minutes`
  - `dingtalk.notify.enabled`

## 6. Decoupling Decisions
- HR domain services are not aware of DingTalk API details.
- DingTalk-specific protocol mapping is contained in adapter services.
- Existing attendance write path is reused; no duplicated persistence logic.
- BPM listener bridge is additive, not invasive.
- User identity binding is abstracted by `DingTalkUserBindingService`.
- Phase-1 binding strategy is mobile-based, can be replaced by table-based binding without changing sync/notify flows.

## 7. Phase 2 (Recommended Next)
- Add explicit `oa_user_id <-> ding_user_id` mapping table.
- Add outbox + retry for approval notifications.
- Add employee create/update/delete bidirectional sync (OA master mode).
- Add reconciliation jobs (daily employee and attendance consistency report).
- Add attendance event subscription + pull compensation window.
