package com.kyx.service.hr.integration.dingtalk.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.kyx.foundation.common.util.json.JsonUtils;
import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeEntryDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeProfileDO;
import com.kyx.service.hr.dal.dataobject.integration.DingTalkSyncSnapshotDO;
import com.kyx.service.hr.dal.dataobject.integration.DingTalkUserBindingDO;
import com.kyx.service.hr.dal.mysql.employee.EmployeeEntryMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeProfileMapper;
import com.kyx.service.hr.dal.mysql.integration.DingTalkSyncSnapshotMapper;
import com.kyx.service.hr.dal.mysql.integration.DingTalkUserBindingMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class DingTalkSyncSnapshotService {

    private static final String TYPE_EMPLOYEE_SYNC = "EMPLOYEE_SYNC";

    @Resource
    private DingTalkSyncSnapshotMapper dingTalkSyncSnapshotMapper;
    @Resource
    private EmployeeProfileMapper employeeProfileMapper;
    @Resource
    private EmployeeEntryMapper employeeEntryMapper;
    @Resource
    private DingTalkUserBindingMapper dingTalkUserBindingMapper;
    @Resource
    private AdminUserApi adminUserApi;

    public boolean recordBeforeChange(String snapshotBatchId,
                                      String syncType,
                                      Long targetTenantId,
                                      String action,
                                      String reason,
                                      EmployeeProfileDO profile,
                                      EmployeeEntryDO entry,
                                      AdminUserRespDTO adminUser,
                                      DingTalkUserBindingDO binding) {
        if (!StringUtils.hasText(snapshotBatchId)
                || (profile == null && entry == null && adminUser == null && binding == null)) {
            return false;
        }
        String snapshotKey = buildSnapshotKey(profile, entry, adminUser, binding);
        if (!StringUtils.hasText(snapshotKey)) {
            return false;
        }
        if (dingTalkSyncSnapshotMapper.selectByBatchIdAndKey(snapshotBatchId, snapshotKey) != null) {
            return false;
        }

        DingTalkSyncSnapshotDO entity = new DingTalkSyncSnapshotDO();
        entity.setSnapshotBatchId(snapshotBatchId.trim());
        entity.setSnapshotKey(snapshotKey);
        entity.setSyncType(StringUtils.hasText(syncType) ? syncType.trim() : TYPE_EMPLOYEE_SYNC);
        entity.setTargetTenantId(resolveTargetTenantId(targetTenantId, profile, binding));
        entity.setAction(trim(action));
        entity.setReason(trim(reason));

        entity.setProfileId(resolveProfileId(profile, binding));
        entity.setUserId(resolveUserId(profile, entry, adminUser, binding));
        entity.setEntryId(entry == null ? null : entry.getId());
        entity.setBindingId(binding == null ? null : binding.getId());

        entity.setProfileSnapshot(profile != null);
        entity.setEntrySnapshot(entry != null);
        entity.setAdminUserSnapshot(adminUser != null);
        entity.setBindingSnapshot(binding != null);

        if (profile != null) {
            entity.setBeforeProfileName(profile.getName());
            entity.setBeforeProfileMobile(profile.getMobile());
            entity.setBeforeProfileEmail(profile.getEmail());
            entity.setBeforeProfileStatus(profile.getStatus());
        }
        if (entry != null) {
            entity.setBeforeEntryWorkStatus(entry.getWorkStatus());
            entity.setBeforeEntryDeptId(entry.getDeptId());
            entity.setBeforeEntryLeaveDate(entry.getLeaveDate());
            entity.setBeforeEntryLeaveReason(entry.getLeaveReason());
        }
        if (adminUser != null) {
            entity.setBeforeAdminUserStatus(adminUser.getStatus());
            entity.setBeforeAdminUserDeptId(adminUser.getDeptId());
        }
        if (binding != null) {
            entity.setBeforeDingActive(binding.getDingActive());
        }
        entity.setDetailJson(buildDetailJson(profile, entry, adminUser, binding));
        dingTalkSyncSnapshotMapper.insert(entity);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public RollbackReport rollbackEmployeeBatch(String snapshotBatchId) {
        RollbackReport report = new RollbackReport();
        report.setSnapshotBatchId(snapshotBatchId);
        if (!StringUtils.hasText(snapshotBatchId)) {
            return report;
        }

        List<DingTalkSyncSnapshotDO> snapshots = dingTalkSyncSnapshotMapper.selectListByBatchId(snapshotBatchId);
        report.setScannedSnapshots(snapshots.size());
        for (DingTalkSyncSnapshotDO snapshot : snapshots) {
            restoreProfile(snapshot, report);
            restoreEntry(snapshot, report);
            restoreBinding(snapshot, report);
            restoreAdminUser(snapshot, report);
        }
        return report;
    }

    private void restoreProfile(DingTalkSyncSnapshotDO snapshot, RollbackReport report) {
        if (!Boolean.TRUE.equals(snapshot.getProfileSnapshot()) || snapshot.getProfileId() == null) {
            return;
        }
        int updated = employeeProfileMapper.update(null, new LambdaUpdateWrapper<EmployeeProfileDO>()
                .eq(EmployeeProfileDO::getId, snapshot.getProfileId())
                .set(EmployeeProfileDO::getName, snapshot.getBeforeProfileName())
                .set(EmployeeProfileDO::getMobile, snapshot.getBeforeProfileMobile())
                .set(EmployeeProfileDO::getEmail, snapshot.getBeforeProfileEmail())
                .set(EmployeeProfileDO::getStatus, snapshot.getBeforeProfileStatus()));
        if (updated > 0) {
            report.increaseRestoredProfiles();
        }
    }

    private void restoreEntry(DingTalkSyncSnapshotDO snapshot, RollbackReport report) {
        if (!Boolean.TRUE.equals(snapshot.getEntrySnapshot()) || snapshot.getEntryId() == null) {
            return;
        }
        Map<String, Object> detail = parseDetailJson(snapshot.getDetailJson());
        LambdaUpdateWrapper<EmployeeEntryDO> updateWrapper = new LambdaUpdateWrapper<EmployeeEntryDO>()
                .eq(EmployeeEntryDO::getId, snapshot.getEntryId())
                .set(EmployeeEntryDO::getWorkStatus, snapshot.getBeforeEntryWorkStatus())
                .set(EmployeeEntryDO::getDeptId, snapshot.getBeforeEntryDeptId())
                .set(EmployeeEntryDO::getLeaveDate, snapshot.getBeforeEntryLeaveDate())
                .set(EmployeeEntryDO::getLeaveReason, snapshot.getBeforeEntryLeaveReason());
        if (detail != null && (Boolean.TRUE.equals(detail.get("jobTitleCaptured")) || detail.containsKey("jobTitle"))) {
            Object jobTitle = detail.get("jobTitle");
            updateWrapper.set(EmployeeEntryDO::getJobTitle, jobTitle == null ? null : String.valueOf(jobTitle));
        }
        int updated = employeeEntryMapper.update(null, updateWrapper);
        if (updated > 0) {
            report.increaseRestoredEntries();
        }
    }

    private void restoreBinding(DingTalkSyncSnapshotDO snapshot, RollbackReport report) {
        if (!Boolean.TRUE.equals(snapshot.getBindingSnapshot()) || snapshot.getBindingId() == null) {
            return;
        }
        int updated = dingTalkUserBindingMapper.update(null, new LambdaUpdateWrapper<DingTalkUserBindingDO>()
                .eq(DingTalkUserBindingDO::getId, snapshot.getBindingId())
                .set(DingTalkUserBindingDO::getDingActive, snapshot.getBeforeDingActive())
                .set(DingTalkUserBindingDO::getSyncTime, LocalDateTime.now()));
        if (updated > 0) {
            report.increaseRestoredBindings();
        }
    }

    private void restoreAdminUser(DingTalkSyncSnapshotDO snapshot, RollbackReport report) {
        if (!Boolean.TRUE.equals(snapshot.getAdminUserSnapshot()) || snapshot.getUserId() == null) {
            return;
        }
        try {
            if (snapshot.getBeforeAdminUserStatus() != null
                    && Boolean.TRUE.equals(adminUserApi.updateUserStatus(snapshot.getUserId(),
                    snapshot.getBeforeAdminUserStatus()).getCheckedData())) {
                report.increaseRestoredAdminUsers();
            }
            if (snapshot.getBeforeAdminUserDeptId() != null
                    && Boolean.TRUE.equals(adminUserApi.updateUserDept(snapshot.getUserId(),
                    snapshot.getBeforeAdminUserDeptId()).getCheckedData())) {
                report.increaseRestoredAdminUserDepts();
            }
        } catch (Exception ex) {
            report.increaseFailedAdminUsers();
            log.warn("Rollback DingTalk employee sync admin user status failed, batchId={}, userId={}, reason={}",
                    snapshot.getSnapshotBatchId(), snapshot.getUserId(), resolveErrorMessage(ex));
        }
    }

    private String buildSnapshotKey(EmployeeProfileDO profile,
                                    EmployeeEntryDO entry,
                                    AdminUserRespDTO adminUser,
                                    DingTalkUserBindingDO binding) {
        List<String> parts = new ArrayList<>();
        if (profile != null && profile.getId() != null) {
            parts.add("profile:" + profile.getId());
        }
        if (entry != null && entry.getId() != null) {
            parts.add("entry:" + entry.getId());
        }
        if (adminUser != null && adminUser.getId() != null) {
            parts.add("adminUser:" + adminUser.getId());
        }
        if (binding != null && binding.getId() != null) {
            parts.add("binding:" + binding.getId());
        }
        return String.join("|", parts);
    }

    private Long resolveTargetTenantId(Long targetTenantId, EmployeeProfileDO profile, DingTalkUserBindingDO binding) {
        if (targetTenantId != null && targetTenantId > 0) {
            return targetTenantId;
        }
        if (profile != null && profile.getTenantId() != null) {
            return profile.getTenantId();
        }
        if (binding != null && binding.getTenantId() != null) {
            return binding.getTenantId();
        }
        return TenantContextHolder.getTenantId();
    }

    private Long resolveProfileId(EmployeeProfileDO profile, DingTalkUserBindingDO binding) {
        if (profile != null) {
            return profile.getId();
        }
        return binding == null ? null : binding.getProfileId();
    }

    private Long resolveUserId(EmployeeProfileDO profile,
                               EmployeeEntryDO entry,
                               AdminUserRespDTO adminUser,
                               DingTalkUserBindingDO binding) {
        if (profile != null && profile.getUserId() != null) {
            return profile.getUserId();
        }
        if (entry != null && entry.getUserId() != null) {
            return entry.getUserId();
        }
        if (adminUser != null && adminUser.getId() != null) {
            return adminUser.getId();
        }
        return binding == null ? null : binding.getOaUserId();
    }

    private String buildDetailJson(EmployeeProfileDO profile,
                                   EmployeeEntryDO entry,
                                   AdminUserRespDTO adminUser,
                                   DingTalkUserBindingDO binding) {
        Map<String, Object> detail = new LinkedHashMap<>();
        if (profile != null) {
            detail.put("profileNo", profile.getProfileNo());
            detail.put("profileName", profile.getName());
            detail.put("profileMobile", profile.getMobile());
            detail.put("profileEmail", profile.getEmail());
        }
        if (entry != null) {
            detail.put("employeeNo", entry.getEmployeeNo());
            detail.put("entryNo", entry.getEntryNo());
            detail.put("jobTitleCaptured", true);
            detail.put("jobTitle", entry.getJobTitle());
            detail.put("leaveDate", entry.getLeaveDate());
            detail.put("leaveReason", entry.getLeaveReason());
        }
        if (adminUser != null) {
            detail.put("adminUsername", adminUser.getUsername());
            detail.put("adminNickname", adminUser.getNickname());
            detail.put("adminMobile", adminUser.getMobile());
        }
        if (binding != null) {
            detail.put("dingUserId", binding.getDingUserId());
            detail.put("dingUserName", binding.getDingUserName());
            detail.put("dingMobile", binding.getDingMobile());
        }
        return JsonUtils.toJsonString(detail);
    }

    private Map<String, Object> parseDetailJson(String detailJson) {
        if (!StringUtils.hasText(detailJson)) {
            return null;
        }
        return JsonUtils.parseObjectQuietly(detailJson, new TypeReference<Map<String, Object>>() {
        });
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String resolveErrorMessage(Exception ex) {
        if (ex == null) {
            return null;
        }
        Throwable cause = ex;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        if (!StringUtils.hasText(message)) {
            message = ex.getMessage();
        }
        return StringUtils.hasText(message) ? message.trim() : ex.getClass().getSimpleName();
    }

    @Data
    public static class RollbackReport {
        private String snapshotBatchId;
        private int scannedSnapshots;
        private int restoredProfiles;
        private int restoredEntries;
        private int restoredBindings;
        private int restoredAdminUsers;
        private int restoredAdminUserDepts;
        private int failedAdminUsers;

        void increaseRestoredProfiles() {
            this.restoredProfiles++;
        }

        void increaseRestoredEntries() {
            this.restoredEntries++;
        }

        void increaseRestoredBindings() {
            this.restoredBindings++;
        }

        void increaseRestoredAdminUsers() {
            this.restoredAdminUsers++;
        }

        void increaseRestoredAdminUserDepts() {
            this.restoredAdminUserDepts++;
        }

        void increaseFailedAdminUsers() {
            this.failedAdminUsers++;
        }
    }
}
