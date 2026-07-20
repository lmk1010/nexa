package com.kyx.service.hr.integration.dingtalk.service;

import com.kyx.foundation.common.biz.system.tenant.TenantCommonApi;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.business.api.dept.DeptApi;
import com.kyx.service.business.api.dept.dto.DeptRespDTO;
import com.kyx.service.business.api.dept.dto.DeptUpsertReqDTO;
import com.kyx.service.business.api.dept.dto.DeptUpsertRespDTO;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import com.kyx.service.business.api.user.dto.UserFunctionSyncReqDTO;
import com.kyx.service.business.api.user.dto.UserFunctionSyncRespDTO;
import com.kyx.service.hr.config.DingTalkProperties;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeCreateReqVO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeCustomFieldDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeCustomFieldValueDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeEducationDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeEntryDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeFamilyDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeProfileDO;
import com.kyx.service.hr.dal.mysql.employee.EmployeeCustomFieldMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeCustomFieldValueMapper;
import com.kyx.service.hr.dal.dataobject.integration.DingTalkUserBindingDO;
import com.kyx.service.hr.dal.mysql.employee.EmployeeEducationMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeEntryMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeFamilyMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeProfileMapper;
import com.kyx.service.hr.dal.mysql.integration.DingTalkUserBindingMapper;
import com.kyx.service.hr.integration.dingtalk.model.DingTalkDeptSnapshot;
import com.kyx.service.hr.integration.dingtalk.model.DingTalkRosterSnapshot;
import com.kyx.service.hr.integration.dingtalk.model.DingTalkUserSnapshot;
import com.kyx.service.hr.service.employee.EmployeeService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Employee sync from DingTalk -> OA profile table.
 *
 * First phase only updates existing profiles by mobile and unique name fallback.
 */
@Service
@Slf4j
public class DingTalkEmployeeSyncService {

    private static final String MATCH_TYPE_MOBILE = "MOBILE";
    private static final String MATCH_TYPE_NAME = "NAME";
    private static final String MATCH_TYPE_AUTO_CREATE = "AUTO_CREATE";
    private static final String SOURCE_TYPE_AUTO = "AUTO_SYNC";
    private static final String ACTION_SKIP_CROSS_TENANT_DUP = "SKIPPED_CROSS_TENANT_DUPLICATE";
    private static final long DEFAULT_DEPT_ROOT_ID = 1L;
    private static final long DEFAULT_DINGTALK_MASTER_TENANT_ID = 171L;
    private static final int USER_SYNC_BATCH_SIZE = 200;
    private static final int PROFILE_STATUS_ENABLED = 1;
    private static final int PROFILE_STATUS_DISABLED = 0;
    private static final int WORK_STATUS_ACTIVE = 3;
    private static final int WORK_STATUS_RESIGNED = 4;
    private static final int DEPT_STATUS_ENABLED = 0;
    private static final int ADMIN_USER_STATUS_ENABLED = 0;
    private static final int ADMIN_USER_STATUS_DISABLED = 1;
    private static final String ROLE_CODE_MEMBER = "biz_member";
    private static final String ROLE_NAME_MEMBER = "普通员工";
    private static final String ROLE_CODE_LEADER = "biz_leader";
    private static final String ROLE_NAME_LEADER = "队长";
    private static final String ROLE_CODE_BOSS = "biz_boss";
    private static final String ROLE_NAME_BOSS = "老板";
    private static final String ROLE_CODE_TENANT_ADMIN = "tenant_admin";
    private static final String ROLE_NAME_TENANT_ADMIN = "租户管理员";
    private static final String ROLE_CODE_HR = "HROwner";
    private static final String ROLE_NAME_HR = "人事";
    private static final String ROLE_CODE_FINANCE = "finance";
    private static final String ROLE_NAME_FINANCE = "财务";
    private static final String FUNCTION_SYNC_SOURCE = "DINGTALK";
    private static final String CUSTOM_FIELD_ID_CARD_NAME = "id_card_name";
    private static final String CUSTOM_FIELD_CERTIFICATE_EXPIRE_DATE = "certificate_expire_date";
    private static final String CUSTOM_FIELD_RESIDENCE_TYPE = "residence_type";
    private static final String CUSTOM_FIELD_FIRST_WORK_TIME = "first_work_time";
    private static final String CUSTOM_FIELD_PERSONAL_SI = "personal_social_security_account";
    private static final String CUSTOM_FIELD_PERSONAL_HF = "personal_housing_fund_account";
    private static final String CUSTOM_FIELD_HIGHEST_EDU = "highest_edu";
    private static final String CUSTOM_FIELD_GRADUATE_SCHOOL = "graduate_school";
    private static final String CUSTOM_FIELD_GRADUATION_TIME = "graduation_time";
    private static final String CUSTOM_FIELD_MAJOR = "major";
    private static final String CUSTOM_FIELD_CONTRACT_COMPANY = "contract_company_name";
    private static final String CUSTOM_FIELD_HAVE_CHILD = "have_child";
    private static final String CUSTOM_FIELD_CHILD_NAME = "child_name";
    private static final String CUSTOM_FIELD_CHILD_SEX = "child_sex";
    private static final String CUSTOM_FIELD_CHILD_BIRTH_DATE = "child_birth_date";
    private static final String CUSTOM_FIELD_TYPE_TEXT = "TEXT";
    private static final String CUSTOM_FIELD_TYPE_DATE = "DATE";
    private static final String CUSTOM_FIELD_GROUP_DINGTALK_ROSTER = "钉钉花名册";

    @Resource
    private DingTalkDirectoryService dingTalkDirectoryService;
    @Resource
    private DingTalkRosterService dingTalkRosterService;
    @Resource
    private EmployeeProfileMapper employeeProfileMapper;
    @Resource
    private EmployeeEntryMapper employeeEntryMapper;
    @Resource
    private EmployeeEducationMapper employeeEducationMapper;
    @Resource
    private EmployeeFamilyMapper employeeFamilyMapper;
    @Resource
    private EmployeeCustomFieldMapper employeeCustomFieldMapper;
    @Resource
    private EmployeeCustomFieldValueMapper employeeCustomFieldValueMapper;
    @Resource
    private EmployeeService employeeService;
    @Resource
    private DingTalkProperties dingTalkProperties;
    @Resource
    private DingTalkUserBindingMapper dingTalkUserBindingMapper;
    @Resource
    private DingTalkUserBindingService dingTalkUserBindingService;
    @Resource
    private DingTalkSyncSnapshotService dingTalkSyncSnapshotService;
    @Resource
    private DeptApi deptApi;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private TenantCommonApi tenantCommonApi;

    public EmployeeSyncReport syncExistingProfiles() {
        return syncExistingProfiles(null, null);
    }

    public EmployeeSyncPreview previewSync(Long autoCreateDeptId, Boolean autoCreateMissing) {
        List<DingTalkUserSnapshot> users = dingTalkDirectoryService.listAllUsers();
        EmployeeSyncPreview preview = new EmployeeSyncPreview();
        preview.setTotalFromDingTalk(users.size());
        boolean createMissing = resolveAutoCreateEnabled(autoCreateMissing);
        Long targetDeptId = resolveAutoCreateDeptId(autoCreateDeptId);
        DeptRouteContext deptRouteContext = buildDeptRouteContext(targetDeptId);
        preview.setAutoCreateEnabled(createMissing);
        preview.setAutoCreateDeptId(createMissing && deptRouteContext != null ? deptRouteContext.getDefaultDeptId() : null);
        Map<String, DingTalkRosterSnapshot> rosterByUserId = safeLoadRosterSnapshots(users, null);
        preview.setRosterEnabled(isRosterSyncEnabled());
        preview.setRosterPulledUsers(rosterByUserId.size());

        for (DingTalkUserSnapshot user : users) {
            String mobile = normalizeMobile(user.getMobile());
            String name = normalizeName(user.getName());
            if (StringUtils.hasText(mobile)) {
                preview.increaseUsersWithMobile();
            }
            if (StringUtils.hasText(name)) {
                preview.increaseUsersWithName();
            }
            if (!StringUtils.hasText(mobile) && !StringUtils.hasText(name)) {
                preview.increaseUsersWithNeitherIdentity();
            }
        }

        MatchContext matchContext = buildMatchContext();
        Map<String, EmployeeProfileDO> profileByMobile = matchContext.profileByMobile;
        Map<String, EmployeeProfileDO> profileByName = matchContext.profileByName;
        Set<Long> usedOaUserIds = new HashSet<>();
        Set<String> plannedCreateMobile = new HashSet<>();
        Set<String> plannedCreateName = new HashSet<>();

        for (DingTalkUserSnapshot user : users) {
            String dingUserId = normalizeDingUserId(user.getUserId());
            DingTalkRosterSnapshot roster = rosterByUserId.get(dingUserId);
            String mobile = normalizeMobile(user.getMobile());
            String name = normalizeName(user.getName());
            MatchResult matchResult = resolveProfile(user, profileByMobile, profileByName);
            EmployeeProfileDO profile = matchResult.getProfile();
            if (profile != null) {
                if (profile.getUserId() != null && !usedOaUserIds.add(profile.getUserId())) {
                    preview.increaseSkippedDuplicateMapping();
                    preview.addSyncDetail(dingUserId, trim(user.getName()), mobile, trim(user.getEmail()),
                            "NONE", "SKIPPED_DUPLICATE_MAPPING", "oaUserId=" + profile.getUserId());
                    continue;
                }
                String matchType = matchResult.isMatchedByName() ? MATCH_TYPE_NAME : MATCH_TYPE_MOBILE;
                if (matchResult.isMatchedByName()) {
                    preview.increaseMatchedByName();
                } else {
                    preview.increaseMatchedByMobile();
                }

                List<String> changedFields = collectChangedFields(user, profile, roster);
                if (changedFields.isEmpty()) {
                    preview.increaseUnchanged();
                    preview.addSyncDetail(dingUserId, trim(user.getName()), mobile, trim(user.getEmail()),
                            matchType, "UNCHANGED", null);
                } else {
                    preview.increaseUpdated();
                    preview.addSyncDetail(dingUserId, trim(user.getName()), mobile, trim(user.getEmail()),
                            matchType, "UPDATED", String.join(",", changedFields));
                }
                continue;
            }

            if (!StringUtils.hasText(mobile) && !StringUtils.hasText(name)) {
                preview.increaseSkippedNoMobile();
                preview.addSyncDetail(dingUserId, trim(user.getName()), mobile, trim(user.getEmail()),
                        "NONE", "SKIPPED_NO_IDENTITY", null);
                continue;
            }

            if (!createMissing) {
                preview.increaseUnmatched();
                preview.addSyncDetail(dingUserId, trim(user.getName()), mobile, trim(user.getEmail()),
                        "NONE", "UNMATCHED", null);
                continue;
            }

            CreateIdentity createIdentity = buildCreateIdentity(user);
            ResolvedDept resolvedDept = resolveDeptForCreate(user, deptRouteContext);
            String createMobile = normalizeMobile(createIdentity.getMobile());
            String createName = normalizeName(createIdentity.getName());
            if (!StringUtils.hasText(createMobile) || !StringUtils.hasText(createName)) {
                preview.increaseSkippedCreateInvalid();
                preview.addSyncDetail(dingUserId, createIdentity.getName(), createMobile, trim(user.getEmail()),
                        "NONE", "SKIPPED_CREATE_INVALID", "identity fallback failed");
                continue;
            }
            if (!isResolvedCreateDept(resolvedDept)) {
                preview.increaseSkippedCreateInvalid();
                preview.addSyncDetail(dingUserId, createIdentity.getName(), createMobile, trim(user.getEmail()),
                        "NONE", "SKIPPED_DEPT_UNMATCHED", buildDeptUnmatchedReason(user, resolvedDept));
                continue;
            }

            if (existsIdentityInCurrentTenant(createMobile, createName, matchContext)) {
                preview.increaseSkippedDuplicateMapping();
                preview.addSyncDetail(dingUserId, createIdentity.getName(), createMobile, trim(user.getEmail()),
                        "NONE", "SKIPPED_DUPLICATE_MAPPING", "mobile/name already exists");
                continue;
            }

            if (existsIdentityInCrossTenants(createMobile, createName, matchContext)) {
                preview.increaseSkippedDuplicateMapping();
                preview.addSyncDetail(dingUserId, createIdentity.getName(), createMobile, trim(user.getEmail()),
                        "NONE", ACTION_SKIP_CROSS_TENANT_DUP, "duplicate in accessible tenants");
                continue;
            }

            if (plannedCreateMobile.contains(createMobile) || plannedCreateName.contains(createName)) {
                preview.increaseSkippedDuplicateMapping();
                preview.addSyncDetail(dingUserId, createIdentity.getName(), createMobile, trim(user.getEmail()),
                        "NONE", "SKIPPED_DUPLICATE_IN_BATCH", "duplicate mobile or name in DingTalk batch");
                continue;
            }
            plannedCreateMobile.add(createMobile);
            plannedCreateName.add(createName);
            preview.increaseToCreate();
            preview.addSyncDetail(dingUserId, createIdentity.getName(), createMobile, trim(user.getEmail()),
                    MATCH_TYPE_AUTO_CREATE, "TO_CREATE", buildCreatePreviewHint(createIdentity, resolvedDept));
        }
        previewSyncCleanups(users, preview);
        return preview;
    }

    public EmployeeSyncReport syncExistingProfiles(Long autoCreateDeptId, Boolean autoCreateMissing) {
        EmployeeSyncReport report = new EmployeeSyncReport();
        report.setSnapshotBatchId(UUID.randomUUID().toString());
        syncDingTalkDepartments(report);
        List<DingTalkUserSnapshot> users = dingTalkDirectoryService.listAllUsers();
        report.setTotalFromDingTalk(users.size());
        report.setRosterEnabled(isRosterSyncEnabled());
        Map<String, DingTalkRosterSnapshot> rosterByUserId = safeLoadRosterSnapshots(users, report);
        boolean createMissing = resolveAutoCreateEnabled(autoCreateMissing);
        Long targetDeptId = resolveAutoCreateDeptId(autoCreateDeptId);
        DeptRouteContext deptRouteContext = createMissing ? buildDeptRouteContext(targetDeptId) : null;
        report.setAutoCreateEnabled(createMissing);
        report.setAutoCreateDeptId(createMissing && deptRouteContext != null ? deptRouteContext.getDefaultDeptId() : null);

        for (DingTalkUserSnapshot user : users) {
            String mobile = normalizeMobile(user.getMobile());
            String name = normalizeName(user.getName());
            if (StringUtils.hasText(mobile)) {
                report.increaseUsersWithMobile();
            }
            if (StringUtils.hasText(name)) {
                report.increaseUsersWithName();
            }
            if (!StringUtils.hasText(mobile) && !StringUtils.hasText(name)) {
                report.increaseUsersWithNeitherIdentity();
            }
        }

        MatchContext matchContext = buildMatchContext();
        Map<String, EmployeeProfileDO> profileByMobile = matchContext.profileByMobile;
        Map<String, EmployeeProfileDO> profileByName = matchContext.profileByName;
        Map<String, BindingCandidate> bindingCandidates = new HashMap<>();
        Set<Long> usedOaUserIds = new HashSet<>();

        for (DingTalkUserSnapshot user : users) {
            String dingUserId = normalizeDingUserId(user.getUserId());
            DingTalkRosterSnapshot roster = rosterByUserId.get(dingUserId);
            MatchResult matchResult = resolveProfile(user, profileByMobile, profileByName);
            EmployeeProfileDO profile = matchResult.getProfile();
            if (profile == null) {
                String mobile = normalizeMobile(user.getMobile());
                String name = normalizeName(user.getName());
                if (!StringUtils.hasText(mobile) && !StringUtils.hasText(name)) {
                    report.increaseSkippedNoMobile();
                    report.addSyncDetail(dingUserId, trim(user.getName()), mobile, trim(user.getEmail()),
                            "NONE", "SKIPPED_NO_IDENTITY", null);
                    continue;
                }

                if (!createMissing) {
                    report.increaseUnmatched();
                    report.addUnmatchedIdentity(dingUserId, mobile, name, trim(user.getEmail()), Boolean.TRUE.equals(user.getActive()));
                    report.addSyncDetail(dingUserId, trim(user.getName()), mobile, trim(user.getEmail()),
                            "NONE", "UNMATCHED", null);
                    continue;
                }

                CreateIdentity createIdentity = buildCreateIdentity(user);
                ResolvedDept resolvedDept = resolveDeptForCreate(user, deptRouteContext);
                String createMobile = normalizeMobile(createIdentity.getMobile());
                String createName = normalizeName(createIdentity.getName());
                if (!isResolvedCreateDept(resolvedDept)) {
                    report.increaseSkippedCreateInvalid();
                    report.addUnmatchedIdentity(dingUserId, createMobile, createName,
                            trim(user.getEmail()), Boolean.TRUE.equals(user.getActive()));
                    report.addSyncDetail(dingUserId, createIdentity.getName(), createMobile, trim(user.getEmail()),
                            "NONE", "SKIPPED_DEPT_UNMATCHED", buildDeptUnmatchedReason(user, resolvedDept));
                    continue;
                }
                if (existsIdentityInCurrentTenant(createMobile, createName, matchContext)) {
                    report.increaseSkippedDuplicateMapping();
                    report.addSyncDetail(dingUserId, createIdentity.getName(), createMobile, trim(user.getEmail()),
                            "NONE", "SKIPPED_DUPLICATE_MAPPING", "mobile/name already exists");
                    continue;
                }
                if (existsIdentityInCrossTenants(createMobile, createName, matchContext)) {
                    report.increaseSkippedDuplicateMapping();
                    report.addSyncDetail(dingUserId, createIdentity.getName(), createMobile, trim(user.getEmail()),
                            "NONE", ACTION_SKIP_CROSS_TENANT_DUP, "duplicate in accessible tenants");
                    continue;
                }
                AutoCreateResult autoCreateResult = createMissingEmployee(user, createIdentity,
                        resolvedDept.getDeptId(), resolvedDept.getTenantId(),
                        profileByMobile, profileByName);
                if (autoCreateResult.getProfile() != null) {
                    profile = autoCreateResult.getProfile();
                    if (profile.getUserId() != null && !usedOaUserIds.add(profile.getUserId())) {
                        report.increaseSkippedDuplicateMapping();
                        report.addSyncDetail(dingUserId, createIdentity.getName(), createIdentity.getMobile(), trim(user.getEmail()),
                                "NONE", "SKIPPED_DUPLICATE_MAPPING", "oaUserId=" + profile.getUserId());
                        continue;
                    }
                    if (autoCreateResult.isCreated()) {
                        report.increaseCreated();
                        report.addSyncDetail(dingUserId, createIdentity.getName(), createIdentity.getMobile(), trim(user.getEmail()),
                                MATCH_TYPE_AUTO_CREATE, "CREATED", buildCreatePreviewHint(createIdentity, resolvedDept));
                        registerCreatedIdentity(createMobile, createName, matchContext);
                    } else {
                        report.increaseUnchanged();
                        report.addSyncDetail(dingUserId, createIdentity.getName(), createIdentity.getMobile(), trim(user.getEmail()),
                                MATCH_TYPE_AUTO_CREATE, "REUSED_EXISTING", null);
                    }
                    if (StringUtils.hasText(dingUserId) && profile.getUserId() != null) {
                        bindingCandidates.put(dingUserId, BindingCandidate.of(user, profile,
                                MATCH_TYPE_AUTO_CREATE, SOURCE_TYPE_AUTO, resolveTargetTenantId(profile.getTenantId())));
                    }
                    List<String> rosterChangedFields = syncRosterFieldsForProfile(profile, roster, report);
                    if (!rosterChangedFields.isEmpty()) {
                        report.addSyncDetail(dingUserId, createIdentity.getName(), createIdentity.getMobile(), trim(user.getEmail()),
                                MATCH_TYPE_AUTO_CREATE, "ROSTER_SYNCED", String.join(",", rosterChangedFields));
                    }
                    syncMatchedEmployeeOrganization(user, profile, deptRouteContext, report);
                    continue;
                }

                if (autoCreateResult.isInvalid()) {
                    report.increaseSkippedCreateInvalid();
                    report.addSyncDetail(dingUserId, createIdentity.getName(), createIdentity.getMobile(), trim(user.getEmail()),
                            "NONE", "SKIPPED_CREATE_INVALID", autoCreateResult.getReason());
                    continue;
                }

                report.increaseCreateFailed();
                report.increaseUnmatched();
                report.addUnmatchedIdentity(dingUserId, createIdentity.getMobile(), createIdentity.getName(),
                        trim(user.getEmail()), Boolean.TRUE.equals(user.getActive()));
                report.addSyncDetail(dingUserId, createIdentity.getName(), createIdentity.getMobile(), trim(user.getEmail()),
                        "NONE", "CREATE_FAILED", autoCreateResult.getReason());
                continue;
            }
            if (profile.getUserId() != null && !usedOaUserIds.add(profile.getUserId())) {
                report.increaseSkippedDuplicateMapping();
                report.addSyncDetail(dingUserId, trim(user.getName()), normalizeMobile(user.getMobile()), trim(user.getEmail()),
                        "NONE", "SKIPPED_DUPLICATE_MAPPING", "oaUserId=" + profile.getUserId());
                continue;
            }
            String matchType = matchResult.isMatchedByName() ? MATCH_TYPE_NAME : MATCH_TYPE_MOBILE;
            if (matchResult.isMatchedByName()) {
                report.increaseMatchedByName();
            } else {
                report.increaseMatchedByMobile();
            }

            EmployeeProfileDO update = new EmployeeProfileDO();
            update.setId(profile.getId());
            boolean changed = false;
            List<String> changedFields = new ArrayList<>();

            String oldName = trim(profile.getName());
            String newName = trim(user.getName());
            if (StringUtils.hasText(newName) && !newName.equals(oldName)) {
                update.setName(newName);
                changed = true;
                changedFields.add(formatChangedField("name", oldName, newName));
            }
            String oldMobile = normalizeMobile(profile.getMobile());
            String newMobile = normalizeMobile(user.getMobile());
            if (StringUtils.hasText(newMobile) && !newMobile.equals(oldMobile)) {
                update.setMobile(newMobile);
                changed = true;
                changedFields.add(formatChangedField("mobile", oldMobile, newMobile));
            }
            String oldEmail = trim(profile.getEmail());
            String newEmail = trim(user.getEmail());
            if (StringUtils.hasText(newEmail) && !newEmail.equals(oldEmail)) {
                update.setEmail(newEmail);
                changed = true;
                changedFields.add(formatChangedField("email", oldEmail, newEmail));
            }

            Integer newStatus = Boolean.TRUE.equals(user.getActive()) ? PROFILE_STATUS_ENABLED : PROFILE_STATUS_DISABLED;
            if (profile.getStatus() == null || !profile.getStatus().equals(newStatus)) {
                update.setStatus(newStatus);
                changed = true;
                changedFields.add(formatChangedField("status",
                        toWorkStatusText(profile.getStatus()), toWorkStatusText(newStatus)));
            }

            if (changed) {
                recordEmployeeSnapshot(report, "PROFILE_UPDATED", String.join(",", changedFields),
                        profile, null, null, null);
                TenantUtils.executeIgnore(() -> employeeProfileMapper.updateById(update));
                profile.setStatus(newStatus);
                report.increaseUpdated();
                report.addSyncDetail(dingUserId, trim(user.getName()), newMobile, trim(user.getEmail()),
                        matchType, "UPDATED", String.join(",", changedFields));
            } else {
                report.increaseUnchanged();
                report.addSyncDetail(dingUserId, trim(user.getName()), newMobile, trim(user.getEmail()),
                        matchType, "UNCHANGED", null);
            }
            List<String> rosterChangedFields = syncRosterFieldsForProfile(profile, roster, report);
            if (!rosterChangedFields.isEmpty()) {
                report.addSyncDetail(dingUserId, trim(user.getName()), newMobile, trim(user.getEmail()),
                        matchType, "ROSTER_SYNCED", String.join(",", rosterChangedFields));
            }
            syncMatchedEmployeeOrganization(user, profile, deptRouteContext, report);
            if (StringUtils.hasText(dingUserId) && profile.getUserId() != null) {
                bindingCandidates.put(dingUserId, BindingCandidate.of(user, profile,
                        matchType, SOURCE_TYPE_AUTO, resolveTargetTenantId(profile.getTenantId())));
            }
        }

        int syncedBindings = persistLocalBindings(bindingCandidates);
        report.setSyncedBindings(syncedBindings);
        report.setSyncedUserSyncRows(safeMarkUserSyncSynced(bindingCandidates.keySet()));
        // 人员绑定完成后回写部门主管（依赖 OA userId）
        syncDeptLeadersFromUsers(users, bindingCandidates, report);
        // 再回写直属主管（entry.directSupervisorId 存的是主管 profileId）
        syncDirectSupervisorsFromUsers(users, bindingCandidates, report);
        SyncCleanupResult cleanupResult = applySyncCleanups(users, bindingCandidates, report);
        if (cleanupResult != null) {
            report.setSyncedEntryStatuses(cleanupResult.getSyncedEntryStatuses());
            report.setEnabledAdminUsers(cleanupResult.getEnabledAdminUsers());
            report.setDisabledAdminUsers(cleanupResult.getDisabledAdminUsers());
            report.setCleanedProfiles(cleanupResult.getCleanedProfiles());
            report.setCleanedBindings(cleanupResult.getCleanedBindings());
            report.setCleanedUserSyncRows(cleanupResult.getCleanedUserSyncRows());
        }
        if (syncedBindings > 0 || (cleanupResult != null && cleanupResult.getCleanedBindings() > 0)) {
            dingTalkUserBindingService.refreshCacheFromLocalBindings();
        }
        log.info("DingTalk employee sync finished, total={}, matchedMobile={}, matchedName={}, created={}, updated={}, "
                        + "failed={}, unmatched={}, deptTreePulled={}, deptTreeSynced={}, deptTreeCreated={}, "
                        + "deptTreeUpdated={}, deptTreeFailed={}, bindings={}, deptUpdates={}, jobTitleUpdates={}, "
                        + "jobNumberUpdates={}, deptLeaderUpdates={}, directSupervisorUpdates={}, postSyncs={}, "
                        + "roleCreates={}, roleAssigns={}, roleRemoves={}, rosterPulled={}, rosterSynced={}, "
                        + "rosterProfileUpdates={}, rosterCustomFields={}, rosterFailed={}, disabledUsers={}, cleanedProfiles={}, "
                        + "cleanedBindings={}, snapshots={}, syncDetails={}",
                report.getTotalFromDingTalk(), report.getMatchedByMobile(), report.getMatchedByName(),
                report.getCreated(), report.getUpdated(), report.getCreateFailed(), report.getUnmatched(),
                report.getDeptTreePulled(), report.getDeptTreeSynced(), report.getDeptTreeCreated(),
                report.getDeptTreeUpdated(), report.getDeptTreeFailed(), report.getSyncedBindings(),
                report.getSyncedDeptIds(), report.getSyncedJobTitles(), report.getSyncedJobNumbers(),
                report.getSyncedDeptLeaders(), report.getSyncedDirectSupervisors(),
                report.getSyncedPosts(), report.getCreatedRoles(), report.getAssignedRoles(),
                report.getRemovedRoles(), report.getRosterPulledUsers(), report.getRosterSyncedProfiles(),
                report.getRosterUpdatedProfiles(), report.getRosterCustomFields(), report.getRosterSyncFailed(),
                report.getDisabledAdminUsers(), report.getCleanedProfiles(),
                report.getCleanedBindings(), report.getSnapshotRows(),
                report.getSyncDetails() == null ? 0 : report.getSyncDetails().size());
        return report;
    }

    public EmployeeSyncReport syncRosterByDingUserIds(Collection<String> dingUserIds) {
        EmployeeSyncReport report = new EmployeeSyncReport();
        report.setSnapshotBatchId(UUID.randomUUID().toString());
        report.setRosterEnabled(isRosterSyncEnabled());

        Set<String> normalizedUserIds = normalizeDingUserIds(dingUserIds);
        report.setTotalFromDingTalk(normalizedUserIds.size());
        if (normalizedUserIds.isEmpty()) {
            return report;
        }
        if (!isRosterSyncEnabled()) {
            for (String dingUserId : normalizedUserIds) {
                report.addSyncDetail(dingUserId, null, null, null,
                        "ROSTER_STREAM", "ROSTER_STREAM_DISABLED", "dingtalk.sync.roster-enabled=false");
            }
            return report;
        }

        Map<String, DingTalkRosterSnapshot> rosterByUserId;
        try {
            rosterByUserId = dingTalkRosterService.listRosterByUserIds(new ArrayList<>(normalizedUserIds));
            report.setRosterPulledUsers(rosterByUserId.size());
        } catch (Exception ex) {
            report.increaseRosterSyncFailed();
            log.warn("Load DingTalk roster by stream failed, users={}, reason={}",
                    normalizedUserIds.size(), resolveErrorMessage(ex));
            for (String dingUserId : normalizedUserIds) {
                report.addSyncDetail(dingUserId, null, null, null,
                        "ROSTER_STREAM", "ROSTER_STREAM_PULL_FAILED", resolveErrorMessage(ex));
            }
            return report;
        }

        Map<String, List<DingTalkUserBindingDO>> bindingsByUserId =
                groupBindingsByDingUserId(dingTalkUserBindingMapper.selectListByDingUserIds(normalizedUserIds));
        Set<String> processedProfiles = new HashSet<>();
        for (String dingUserId : normalizedUserIds) {
            DingTalkRosterSnapshot roster = rosterByUserId.get(dingUserId);
            if (roster == null) {
                report.increaseUnmatched();
                report.addSyncDetail(dingUserId, null, null, null,
                        "ROSTER_STREAM", "ROSTER_STREAM_ROSTER_MISSING", "DingTalk roster API returned no row");
                continue;
            }

            List<DingTalkUserBindingDO> bindings = bindingsByUserId.get(dingUserId);
            if (bindings == null || bindings.isEmpty()) {
                report.increaseUnmatched();
                report.addSyncDetail(dingUserId, null, null, null,
                        "ROSTER_STREAM", "ROSTER_STREAM_UNBOUND", "no OA binding in current tenant");
                continue;
            }

            for (DingTalkUserBindingDO binding : bindings) {
                if (binding == null) {
                    continue;
                }
                if (Boolean.FALSE.equals(binding.getDingActive())) {
                    report.increaseUnmatched();
                    report.addSyncDetail(dingUserId, trim(binding.getDingUserName()),
                            normalizeMobile(binding.getDingMobile()), trim(binding.getDingEmail()),
                            "ROSTER_STREAM", "ROSTER_STREAM_INACTIVE_BINDING", "dingActive=false");
                    continue;
                }
                String profileKey = buildBindingProfileKey(binding);
                if (StringUtils.hasText(profileKey) && !processedProfiles.add(profileKey)) {
                    continue;
                }
                try {
                    syncRosterForBindingInCurrentTenant(dingUserId, binding, roster, report);
                } catch (Exception ex) {
                    report.increaseRosterSyncFailed();
                    report.addSyncDetail(dingUserId, trim(binding.getDingUserName()),
                            normalizeMobile(binding.getDingMobile()), trim(binding.getDingEmail()),
                            "ROSTER_STREAM", "ROSTER_STREAM_SYNC_FAILED", resolveErrorMessage(ex));
                    log.warn("Sync DingTalk roster stream user failed, dingUserId={}, profileId={}, oaUserId={}, reason={}",
                            dingUserId, binding.getProfileId(), binding.getOaUserId(), resolveErrorMessage(ex));
                }
            }
        }
        log.info("DingTalk roster stream sync finished, users={}, rosterPulled={}, rosterSynced={}, "
                        + "rosterProfileUpdates={}, rosterCustomFields={}, failed={}, unmatched={}, unchanged={}",
                report.getTotalFromDingTalk(), report.getRosterPulledUsers(), report.getRosterSyncedProfiles(),
                report.getRosterUpdatedProfiles(), report.getRosterCustomFields(), report.getRosterSyncFailed(),
                report.getUnmatched(), report.getUnchanged());
        return report;
    }

    public EmployeeSyncReport syncRosterForActiveBindings() {
        List<DingTalkUserBindingDO> bindings = dingTalkUserBindingMapper.selectListActive();
        Set<String> dingUserIds = bindings.stream()
                .map(DingTalkUserBindingDO::getDingUserId)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return syncRosterByDingUserIds(dingUserIds);
    }

    public EmployeeImportReport importUnmatchedProfiles(Long deptId) {
        List<DingTalkUserSnapshot> users = dingTalkDirectoryService.listAllUsers();
        MatchContext matchContext = buildMatchContext();
        Map<String, EmployeeProfileDO> profileByMobile = matchContext.profileByMobile;
        Map<String, EmployeeProfileDO> profileByName = matchContext.profileByName;
        Map<String, BindingCandidate> bindingCandidates = new HashMap<>();

        EmployeeImportReport report = new EmployeeImportReport();
        report.setTotalFromDingTalk(users.size());
        Long targetDeptId = resolveAutoCreateDeptId(deptId);
        DeptRouteContext deptRouteContext = buildDeptRouteContext(targetDeptId);
        report.setTargetDeptId(deptRouteContext.getDefaultDeptId());

        for (DingTalkUserSnapshot user : users) {
            String dingUserId = normalizeDingUserId(user.getUserId());
            MatchResult matchResult = resolveProfile(user, profileByMobile, profileByName);
            if (matchResult.getProfile() != null) {
                continue;
            }

            CreateIdentity createIdentity = buildCreateIdentity(user);
            ResolvedDept resolvedDept = resolveDeptForCreate(user, deptRouteContext);
            String createMobile = normalizeMobile(createIdentity.getMobile());
            String createName = normalizeName(createIdentity.getName());
            if (existsIdentityInCurrentTenant(createMobile, createName, matchContext)) {
                report.increaseSkippedExists();
                report.addSkippedUser(dingUserId, createIdentity.getMobile(), createIdentity.getName(), "already exists");
                continue;
            }
            if (existsIdentityInCrossTenants(createMobile, createName, matchContext)) {
                report.increaseSkippedExists();
                report.addSkippedUser(dingUserId, createIdentity.getMobile(), createIdentity.getName(),
                        "duplicate in accessible tenants");
                continue;
            }
            AutoCreateResult autoCreateResult = createMissingEmployee(user, createIdentity,
                    resolvedDept.getDeptId(), resolvedDept.getTenantId(),
                    profileByMobile, profileByName);
            if (autoCreateResult.getProfile() != null) {
                EmployeeProfileDO profile = autoCreateResult.getProfile();
                if (autoCreateResult.isCreated()) {
                    report.increaseImported();
                    report.addImportedUser(dingUserId, createIdentity.getMobile(), createIdentity.getName(),
                            profile.getId(), profile.getUserId());
                    registerCreatedIdentity(createMobile, createName, matchContext);
                } else {
                    report.increaseSkippedExists();
                    report.addSkippedUser(dingUserId, createIdentity.getMobile(), createIdentity.getName(), "already exists");
                }
                if (StringUtils.hasText(dingUserId) && profile.getUserId() != null) {
                    bindingCandidates.put(dingUserId, BindingCandidate.of(user, profile,
                            MATCH_TYPE_AUTO_CREATE, SOURCE_TYPE_AUTO, resolveTargetTenantId(profile.getTenantId())));
                }
                syncMatchedEmployeeOrganization(user, profile, deptRouteContext, null);
                continue;
            }

            if (autoCreateResult.isInvalid()) {
                report.increaseSkippedInvalid();
                report.addSkippedUser(dingUserId, createIdentity.getMobile(), createIdentity.getName(), autoCreateResult.getReason());
            } else {
                report.increaseFailed();
                report.addFailedUser(dingUserId, createIdentity.getMobile(), createIdentity.getName(), autoCreateResult.getReason());
            }
        }
        int syncedBindings = persistLocalBindings(bindingCandidates);
        if (syncedBindings > 0) {
            dingTalkUserBindingService.refreshCacheFromLocalBindings();
        }
        log.info("DingTalk unmatched employee import finished: {}", report);
        return report;
    }

    private MatchResult resolveProfile(DingTalkUserSnapshot user, Map<String, EmployeeProfileDO> profileByMobile,
                                       Map<String, EmployeeProfileDO> profileByName) {
        EmployeeProfileDO profile = profileByMobile.get(normalizeMobile(user.getMobile()));
        if (profile != null) {
            return new MatchResult(profile, false);
        }
        profile = profileByName.get(normalizeName(user.getName()));
        return new MatchResult(profile, profile != null);
    }

    private MatchContext buildMatchContext() {
        List<EmployeeProfileDO> profiles = TenantUtils.executeIgnore(
                () -> employeeProfileMapper.selectList(new LambdaQueryWrapperX<>()));
        MatchContext context = new MatchContext();
        context.profileByMobile = buildUniqueKeyMap(profiles, profile -> normalizeMobile(profile.getMobile()));
        context.profileByName = buildUniqueKeyMap(profiles, profile -> normalizeName(profile.getName()));
        context.existingMobileSet = buildIdentitySet(profiles, profile -> normalizeMobile(profile.getMobile()));
        context.existingNameSet = buildIdentitySet(profiles, profile -> normalizeName(profile.getName()));
        context.crossTenantMobileSet = Collections.emptySet();
        context.crossTenantNameSet = Collections.emptySet();
        return context;
    }

    private Map<String, EmployeeProfileDO> buildUniqueKeyMap(List<EmployeeProfileDO> profiles,
                                                              Function<EmployeeProfileDO, String> keyExtractor) {
        Map<String, EmployeeProfileDO> uniqueMap = new HashMap<>();
        Set<String> duplicatedKeys = new HashSet<>();
        for (EmployeeProfileDO profile : profiles) {
            String key = keyExtractor.apply(profile);
            if (!StringUtils.hasText(key)) {
                continue;
            }
            EmployeeProfileDO existing = uniqueMap.putIfAbsent(key, profile);
            if (existing != null && !existing.getId().equals(profile.getId())) {
                duplicatedKeys.add(key);
            }
        }
        duplicatedKeys.forEach(uniqueMap::remove);
        return uniqueMap;
    }

    private Set<String> buildIdentitySet(List<EmployeeProfileDO> profiles, Function<EmployeeProfileDO, String> keyExtractor) {
        Set<String> identitySet = new HashSet<>();
        if (profiles == null || profiles.isEmpty()) {
            return identitySet;
        }
        for (EmployeeProfileDO profile : profiles) {
            String key = keyExtractor.apply(profile);
            if (StringUtils.hasText(key)) {
                identitySet.add(key);
            }
        }
        return identitySet;
    }

    private Map<String, DingTalkRosterSnapshot> safeLoadRosterSnapshots(List<DingTalkUserSnapshot> users,
                                                                        EmployeeSyncReport report) {
        if (!isRosterSyncEnabled() || users == null || users.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> userIds = new ArrayList<>();
        for (DingTalkUserSnapshot user : users) {
            String userId = normalizeDingUserId(user == null ? null : user.getUserId());
            if (StringUtils.hasText(userId)) {
                userIds.add(userId);
            }
        }
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            Map<String, DingTalkRosterSnapshot> rosterByUserId = dingTalkRosterService.listRosterByUserIds(userIds);
            if (report != null) {
                report.setRosterPulledUsers(rosterByUserId.size());
            }
            return rosterByUserId;
        } catch (Exception ex) {
            if (report != null) {
                report.increaseRosterSyncFailed();
            }
            log.warn("Load DingTalk roster fields failed, users={}, reason={}", userIds.size(), resolveErrorMessage(ex));
            return Collections.emptyMap();
        }
    }

    private Set<String> normalizeDingUserIds(Collection<String> dingUserIds) {
        if (dingUserIds == null || dingUserIds.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String dingUserId : dingUserIds) {
            String normalized = normalizeDingUserId(dingUserId);
            if (StringUtils.hasText(normalized)) {
                result.add(normalized);
            }
        }
        return result;
    }

    private Map<String, List<DingTalkUserBindingDO>> groupBindingsByDingUserId(List<DingTalkUserBindingDO> bindings) {
        if (bindings == null || bindings.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<DingTalkUserBindingDO>> result = new LinkedHashMap<>();
        for (DingTalkUserBindingDO binding : bindings) {
            String dingUserId = normalizeDingUserId(binding == null ? null : binding.getDingUserId());
            if (!StringUtils.hasText(dingUserId)) {
                continue;
            }
            result.computeIfAbsent(dingUserId, key -> new ArrayList<>()).add(binding);
        }
        return result;
    }

    private void syncRosterForBindingInCurrentTenant(String dingUserId,
                                                     DingTalkUserBindingDO binding,
                                                     DingTalkRosterSnapshot roster,
                                                     EmployeeSyncReport report) {
        EmployeeProfileDO profile = selectProfileByBinding(binding);
        if (profile == null || profile.getId() == null) {
            report.increaseUnmatched();
            report.addSyncDetail(dingUserId, trim(binding.getDingUserName()),
                    normalizeMobile(binding.getDingMobile()), trim(binding.getDingEmail()),
                    "ROSTER_STREAM", "ROSTER_STREAM_PROFILE_MISSING",
                    binding.getOaUserId() == null ? null : "oaUserId=" + binding.getOaUserId());
            return;
        }

        List<String> changedFields = syncRosterFieldsForProfile(profile, roster, report);
        if (changedFields.isEmpty()) {
            report.increaseUnchanged();
            report.addSyncDetail(dingUserId, resolveProfileName(profile, binding),
                    normalizeMobile(resolveProfileMobile(profile, binding)), resolveProfileEmail(profile, binding),
                    "ROSTER_STREAM", "ROSTER_STREAM_UNCHANGED", "profileId=" + profile.getId());
            return;
        }

        report.increaseUpdated();
        report.addSyncDetail(dingUserId, resolveProfileName(profile, binding),
                normalizeMobile(resolveProfileMobile(profile, binding)), resolveProfileEmail(profile, binding),
                "ROSTER_STREAM", "ROSTER_STREAM_SYNCED", String.join(",", changedFields));
    }

    private EmployeeProfileDO selectProfileByBinding(DingTalkUserBindingDO binding) {
        if (binding == null) {
            return null;
        }
        if (binding.getProfileId() != null) {
            return employeeProfileMapper.selectById(binding.getProfileId());
        }
        if (binding.getOaUserId() != null) {
            return employeeProfileMapper.selectByUserId(binding.getOaUserId());
        }
        return null;
    }

    private String buildBindingProfileKey(DingTalkUserBindingDO binding) {
        if (binding == null) {
            return null;
        }
        if (binding.getProfileId() != null) {
            return "profile:" + binding.getProfileId();
        }
        if (binding.getOaUserId() != null) {
            return "user:" + binding.getOaUserId();
        }
        return null;
    }

    private String resolveProfileName(EmployeeProfileDO profile, DingTalkUserBindingDO binding) {
        String name = trim(profile == null ? null : profile.getName());
        return StringUtils.hasText(name) ? name : trim(binding == null ? null : binding.getDingUserName());
    }

    private String resolveProfileMobile(EmployeeProfileDO profile, DingTalkUserBindingDO binding) {
        String mobile = normalizeMobile(profile == null ? null : profile.getMobile());
        return StringUtils.hasText(mobile) ? mobile : normalizeMobile(binding == null ? null : binding.getDingMobile());
    }

    private String resolveProfileEmail(EmployeeProfileDO profile, DingTalkUserBindingDO binding) {
        String email = trim(profile == null ? null : profile.getEmail());
        return StringUtils.hasText(email) ? email : trim(binding == null ? null : binding.getDingEmail());
    }

    private boolean isRosterSyncEnabled() {
        return dingTalkProperties.getSync() != null
                && Boolean.TRUE.equals(dingTalkProperties.getSync().getRosterEnabled());
    }

    private List<String> collectChangedFields(DingTalkUserSnapshot user, EmployeeProfileDO profile) {
        return collectChangedFields(user, profile, null);
    }

    private List<String> collectChangedFields(DingTalkUserSnapshot user, EmployeeProfileDO profile,
                                              DingTalkRosterSnapshot roster) {
        List<String> changedFields = new ArrayList<>();
        String oldName = trim(profile.getName());
        String newName = trim(user.getName());
        if (StringUtils.hasText(newName) && !newName.equals(oldName)) {
            changedFields.add(formatChangedField("name", oldName, newName));
        }
        String oldMobile = normalizeMobile(profile.getMobile());
        String newMobile = normalizeMobile(user.getMobile());
        if (StringUtils.hasText(newMobile) && !newMobile.equals(oldMobile)) {
            changedFields.add(formatChangedField("mobile", oldMobile, newMobile));
        }
        String oldEmail = trim(profile.getEmail());
        String newEmail = trim(user.getEmail());
        if (StringUtils.hasText(newEmail) && !newEmail.equals(oldEmail)) {
            changedFields.add(formatChangedField("email", oldEmail, newEmail));
        }
        Integer newStatus = Boolean.TRUE.equals(user.getActive()) ? PROFILE_STATUS_ENABLED : PROFILE_STATUS_DISABLED;
        if (profile.getStatus() == null || !profile.getStatus().equals(newStatus)) {
            changedFields.add(formatChangedField("status",
                    toWorkStatusText(profile.getStatus()), toWorkStatusText(newStatus)));
        }
        changedFields.addAll(collectRosterProfileChangedFields(profile, roster));
        changedFields.addAll(collectRosterCustomChangedFields(profile, roster));
        return changedFields;
    }

    private List<String> syncRosterFieldsForProfile(EmployeeProfileDO profile,
                                                    DingTalkRosterSnapshot roster,
                                                    EmployeeSyncReport report) {
        List<String> changedFields = new ArrayList<>();
        if (!isRosterSyncEnabled() || profile == null || profile.getId() == null || roster == null) {
            return changedFields;
        }

        EmployeeProfileDO update = new EmployeeProfileDO();
        update.setId(profile.getId());
        boolean profileChanged = appendRosterProfileUpdates(update, profile, roster, changedFields);
        List<String> entryChangedFields = syncRosterEntryFields(profile, roster);
        changedFields.addAll(entryChangedFields);
        List<String> relationChangedFields = syncRosterRelationTables(profile, roster);
        changedFields.addAll(relationChangedFields);
        List<String> customChangedFields = collectRosterCustomChangedFields(profile, roster);
        changedFields.addAll(customChangedFields);
        if (!profileChanged && entryChangedFields.isEmpty()
                && relationChangedFields.isEmpty() && customChangedFields.isEmpty()) {
            return changedFields;
        }

        recordEmployeeSnapshot(report, "PROFILE_ROSTER_UPDATED", String.join(",", changedFields),
                profile, null, null, null);
        if (profileChanged) {
            TenantUtils.executeIgnore(() -> employeeProfileMapper.updateById(update));
            applyRosterProfileValues(profile, update);
            if (report != null) {
                report.increaseRosterUpdatedProfiles();
            }
        }
        int customFieldCount = customChangedFields.isEmpty() ? 0 : syncRosterCustomFields(profile, roster);
        if (report != null) {
            report.increaseRosterSyncedProfiles();
            report.increaseRosterCustomFields(customFieldCount);
        }
        return changedFields;
    }

    private List<String> collectRosterProfileChangedFields(EmployeeProfileDO profile, DingTalkRosterSnapshot roster) {
        List<String> changedFields = new ArrayList<>();
        if (profile == null || roster == null) {
            return changedFields;
        }
        appendRosterProfileUpdates(new EmployeeProfileDO(), profile, roster, changedFields);
        return changedFields;
    }

    private boolean appendRosterProfileUpdates(EmployeeProfileDO update,
                                               EmployeeProfileDO profile,
                                               DingTalkRosterSnapshot roster,
                                               List<String> changedFields) {
        boolean changed = false;

        String newIdNumber = trim(roster.getCertNo());
        String oldIdNumber = trim(profile.getIdNumber());
        if (StringUtils.hasText(newIdNumber) && !Objects.equals(oldIdNumber, newIdNumber)
                && shouldApplyRosterTextField(oldIdNumber)) {
            update.setIdNumber(newIdNumber);
            changedFields.add(formatSensitiveChangedField("idNumber", oldIdNumber, newIdNumber));
            changed = true;
        }

        Integer newGender = parseGender(roster.getSexType());
        if (newGender != null && !Objects.equals(profile.getGender(), newGender)
                && shouldApplyRosterValueField(profile.getGender())) {
            update.setGender(newGender);
            changedFields.add(formatChangedField("gender", toGenderText(profile.getGender()), toGenderText(newGender)));
            changed = true;
        }

        LocalDate newBirthDate = parseDate(roster.getBirthTime());
        boolean birthDateChanged = false;
        if (newBirthDate != null && !Objects.equals(profile.getBirthDate(), newBirthDate)
                && shouldApplyRosterValueField(profile.getBirthDate())) {
            update.setBirthDate(newBirthDate);
            changedFields.add(formatChangedField("birthDate", formatDate(profile.getBirthDate()), formatDate(newBirthDate)));
            birthDateChanged = true;
            changed = true;
        }
        Integer newAge = newBirthDate == null ? null : calculateAge(newBirthDate);
        if (newAge != null && !Objects.equals(profile.getAge(), newAge)
                && shouldApplyRosterAgeField(profile, newBirthDate, birthDateChanged)) {
            update.setAge(newAge);
            changedFields.add(formatChangedField("age", toString(profile.getAge()), toString(newAge)));
            changed = true;
        }

        String newEthnicity = trim(roster.getNationType());
        if (StringUtils.hasText(newEthnicity) && !Objects.equals(trim(profile.getEthnicity()), newEthnicity)
                && shouldApplyRosterTextField(profile.getEthnicity())) {
            update.setEthnicity(newEthnicity);
            changedFields.add(formatChangedField("ethnicity", trim(profile.getEthnicity()), newEthnicity));
            changed = true;
        }

        String newPoliticalStatus = trim(roster.getPoliticalStatus());
        if (StringUtils.hasText(newPoliticalStatus)
                && !Objects.equals(trim(profile.getPoliticalStatus()), newPoliticalStatus)
                && shouldApplyRosterTextField(profile.getPoliticalStatus())) {
            update.setPoliticalStatus(newPoliticalStatus);
            changedFields.add(formatChangedField("politicalStatus", trim(profile.getPoliticalStatus()), newPoliticalStatus));
            changed = true;
        }

        Integer newMaritalStatus = parseMaritalStatus(roster.getMarriage());
        if (newMaritalStatus != null && !Objects.equals(profile.getMaritalStatus(), newMaritalStatus)
                && shouldApplyRosterValueField(profile.getMaritalStatus())) {
            update.setMaritalStatus(newMaritalStatus);
            changedFields.add(formatChangedField("maritalStatus",
                    toMaritalStatusText(profile.getMaritalStatus()), toMaritalStatusText(newMaritalStatus)));
            changed = true;
        }

        String newHometown = trim(roster.getCertAddress());
        if (StringUtils.hasText(newHometown) && !Objects.equals(trim(profile.getHometown()), newHometown)
                && shouldApplyRosterTextField(profile.getHometown())) {
            update.setHometown(newHometown);
            changedFields.add(formatChangedField("hometown", trim(profile.getHometown()), newHometown));
            changed = true;
        }

        String newAddress = trim(roster.getAddress());
        if (StringUtils.hasText(newAddress) && !Objects.equals(trim(profile.getAddress()), newAddress)
                && shouldApplyRosterTextField(profile.getAddress())) {
            update.setAddress(newAddress);
            changedFields.add(formatChangedField("address", trim(profile.getAddress()), newAddress));
            changed = true;
        }

        // 紧急联系人（花名册 sys06）
        String newEmergencyContact = trim(roster.getUrgentContactsName());
        if (StringUtils.hasText(newEmergencyContact)
                && !Objects.equals(trim(profile.getEmergencyContact()), newEmergencyContact)
                && shouldApplyRosterTextField(profile.getEmergencyContact())) {
            update.setEmergencyContact(newEmergencyContact);
            changedFields.add(formatChangedField("emergencyContact",
                    trim(profile.getEmergencyContact()), newEmergencyContact));
            changed = true;
        }
        String newEmergencyPhone = trim(roster.getUrgentContactsPhone());
        if (StringUtils.hasText(newEmergencyPhone)
                && !Objects.equals(trim(profile.getEmergencyPhone()), newEmergencyPhone)
                && shouldApplyRosterTextField(profile.getEmergencyPhone())) {
            update.setEmergencyPhone(newEmergencyPhone);
            changedFields.add(formatChangedField("emergencyPhone",
                    trim(profile.getEmergencyPhone()), newEmergencyPhone));
            changed = true;
        }
        String newEmergencyRelation = trim(roster.getUrgentContactsRelation());
        if (StringUtils.hasText(newEmergencyRelation)
                && !Objects.equals(trim(profile.getEmergencyRelation()), newEmergencyRelation)
                && shouldApplyRosterTextField(profile.getEmergencyRelation())) {
            update.setEmergencyRelation(newEmergencyRelation);
            changedFields.add(formatChangedField("emergencyRelation",
                    trim(profile.getEmergencyRelation()), newEmergencyRelation));
            changed = true;
        }

        // 邮箱（花名册 sys00-email）
        String newEmail = trim(roster.getEmail());
        if (StringUtils.hasText(newEmail) && !Objects.equals(trim(profile.getEmail()), newEmail)
                && shouldApplyRosterTextField(profile.getEmail())) {
            update.setEmail(newEmail);
            changedFields.add(formatChangedField("email", trim(profile.getEmail()), newEmail));
            changed = true;
        }

        LocalDate newOnboardDate = parseDate(roster.getConfirmJoinTime());
        boolean onboardDateChanged = false;
        if (newOnboardDate != null && !Objects.equals(profile.getOnboardDate(), newOnboardDate)
                && shouldApplyRosterValueField(profile.getOnboardDate())) {
            update.setOnboardDate(newOnboardDate);
            changedFields.add(formatChangedField("onboardDate", formatDate(profile.getOnboardDate()), formatDate(newOnboardDate)));
            onboardDateChanged = true;
            changed = true;
        }
        // 转正日期
        LocalDate newConfirmationDate = parseDate(roster.getRegularTime());
        if (newConfirmationDate != null && !Objects.equals(profile.getConfirmationDate(), newConfirmationDate)
                && shouldApplyRosterValueField(profile.getConfirmationDate())) {
            update.setConfirmationDate(newConfirmationDate);
            changedFields.add(formatChangedField("confirmationDate",
                    formatDate(profile.getConfirmationDate()), formatDate(newConfirmationDate)));
            changed = true;
        }
        BigDecimal newCompanyYears = newOnboardDate == null ? null : calculateYears(newOnboardDate);
        if (newCompanyYears != null && !sameBigDecimal(profile.getCompanyYears(), newCompanyYears)
                && shouldApplyRosterCompanyYearsField(profile, newOnboardDate, onboardDateChanged)) {
            update.setCompanyYears(newCompanyYears);
            changedFields.add(formatChangedField("companyYears",
                    formatBigDecimal(profile.getCompanyYears()), formatBigDecimal(newCompanyYears)));
            changed = true;
        }
        return changed;
    }

    /**
     * 花名册字段回写 entry：职位/工号/银行卡/合同/办公地点/备注。
     */
    private List<String> syncRosterEntryFields(EmployeeProfileDO profile, DingTalkRosterSnapshot roster) {
        List<String> changedFields = new ArrayList<>();
        if (profile == null || profile.getId() == null || roster == null) {
            return changedFields;
        }
        Long targetTenantId = resolveTargetTenantId(profile.getTenantId());
        return TenantUtils.execute(targetTenantId, () -> {
            EmployeeEntryDO entry = profile.getUserId() == null
                    ? firstEntryByProfileId(profile.getId())
                    : employeeEntryMapper.selectByUserId(profile.getUserId());
            if (entry == null || entry.getId() == null) {
                return changedFields;
            }
            EmployeeEntryDO updateEntry = new EmployeeEntryDO();
            updateEntry.setId(entry.getId());
            boolean changed = false;

            String jobTitle = firstNonBlank(roster.getPosition(), null);
            if (StringUtils.hasText(jobTitle) && !Objects.equals(trim(entry.getJobTitle()), jobTitle)
                    && shouldApplyRosterTextField(entry.getJobTitle())) {
                updateEntry.setJobTitle(jobTitle);
                changedFields.add(formatChangedField("jobTitle", trim(entry.getJobTitle()), jobTitle));
                changed = true;
            }

            String employeeNo = firstNonBlank(roster.getJobNumber(), null);
            if (StringUtils.hasText(employeeNo) && !Objects.equals(trim(entry.getEmployeeNo()), employeeNo)
                    && shouldApplyRosterTextField(entry.getEmployeeNo())) {
                updateEntry.setEmployeeNo(employeeNo);
                changedFields.add(formatChangedField("employeeNo", trim(entry.getEmployeeNo()), employeeNo));
                changed = true;
            }

            String officeLocation = firstNonBlank(roster.getWorkPlace(), null);
            if (StringUtils.hasText(officeLocation) && !Objects.equals(trim(entry.getOfficeLocation()), officeLocation)
                    && shouldApplyRosterTextField(entry.getOfficeLocation())) {
                updateEntry.setOfficeLocation(officeLocation);
                changedFields.add(formatChangedField("officeLocation", trim(entry.getOfficeLocation()), officeLocation));
                changed = true;
            }

            String remark = firstNonBlank(roster.getRemark(), null);
            if (StringUtils.hasText(remark) && !Objects.equals(trim(entry.getRemark()), remark)
                    && shouldApplyRosterTextField(entry.getRemark())) {
                updateEntry.setRemark(remark);
                changedFields.add(formatChangedField("remark", trim(entry.getRemark()), remark));
                changed = true;
            }

            // 银行卡
            String bankAccount = firstNonBlank(roster.getBankAccountNo(), null);
            if (StringUtils.hasText(bankAccount) && !Objects.equals(trim(entry.getBankAccount()), bankAccount)
                    && shouldApplyRosterTextField(entry.getBankAccount())) {
                updateEntry.setBankAccount(bankAccount);
                changedFields.add(formatSensitiveChangedField("bankAccount", trim(entry.getBankAccount()), bankAccount));
                changed = true;
            }
            String bankName = firstNonBlank(roster.getAccountBank(), null);
            if (StringUtils.hasText(bankName) && !Objects.equals(trim(entry.getBankName()), bankName)
                    && shouldApplyRosterTextField(entry.getBankName())) {
                updateEntry.setBankName(bankName);
                changedFields.add(formatChangedField("bankName", trim(entry.getBankName()), bankName));
                changed = true;
            }
            if (StringUtils.hasText(bankAccount) && !StringUtils.hasText(entry.getAccountName())
                    && StringUtils.hasText(profile.getName())) {
                updateEntry.setAccountName(profile.getName());
                changedFields.add(formatChangedField("accountName", trim(entry.getAccountName()), profile.getName()));
                changed = true;
            }

            // 合同
            Integer contractType = parseContractType(roster.getContractType());
            if (contractType != null && !Objects.equals(entry.getContractType(), contractType)
                    && shouldApplyRosterValueField(entry.getContractType())) {
                updateEntry.setContractType(contractType);
                changedFields.add(formatChangedField("contractType",
                        toString(entry.getContractType()), toString(contractType)));
                changed = true;
            }
            LocalDate contractStart = parseDate(firstNonBlank(roster.getNowContractStartTime(),
                    roster.getFirstContractStartTime()));
            if (contractStart != null && !Objects.equals(entry.getContractStartDate(), contractStart)
                    && shouldApplyRosterValueField(entry.getContractStartDate())) {
                updateEntry.setContractStartDate(contractStart);
                changedFields.add(formatChangedField("contractStartDate",
                        formatDate(entry.getContractStartDate()), formatDate(contractStart)));
                changed = true;
            }
            LocalDate contractEnd = parseDate(firstNonBlank(roster.getNowContractEndTime(),
                    roster.getFirstContractEndTime()));
            if (contractEnd != null && !Objects.equals(entry.getContractEndDate(), contractEnd)
                    && shouldApplyRosterValueField(entry.getContractEndDate())) {
                updateEntry.setContractEndDate(contractEnd);
                changedFields.add(formatChangedField("contractEndDate",
                        formatDate(entry.getContractEndDate()), formatDate(contractEnd)));
                changed = true;
            }

            // 用工类型
            Integer employmentType = parseEmploymentType(roster.getEmployeeType());
            if (employmentType != null && !Objects.equals(entry.getEmploymentType(), employmentType)
                    && shouldApplyRosterValueField(entry.getEmploymentType())) {
                updateEntry.setEmploymentType(employmentType);
                changedFields.add(formatChangedField("employmentType",
                        toString(entry.getEmploymentType()), toString(employmentType)));
                changed = true;
            }

            if (changed) {
                employeeEntryMapper.updateById(updateEntry);
            }
            return changedFields;
        });
    }

    /**
     * 花名册关联表回写：最高学历、子女家庭信息。
     * 策略：仅在本地无对应记录时补写，避免覆盖人工维护数据。
     */
    private List<String> syncRosterRelationTables(EmployeeProfileDO profile, DingTalkRosterSnapshot roster) {
        List<String> changedFields = new ArrayList<>();
        if (profile == null || profile.getId() == null || roster == null) {
            return changedFields;
        }
        Long targetTenantId = resolveTargetTenantId(profile.getTenantId());
        return TenantUtils.execute(targetTenantId, () -> {
            // 学历
            String education = trim(roster.getHighestEdu());
            String school = trim(roster.getGraduateSchool());
            String major = trim(roster.getMajor());
            LocalDate graduationDate = parseDate(roster.getGraduationTime());
            if (StringUtils.hasText(education) || StringUtils.hasText(school)
                    || StringUtils.hasText(major) || graduationDate != null) {
                List<EmployeeEducationDO> existing = employeeEducationMapper.selectListByProfileId(profile.getId());
                boolean hasAny = existing != null && !existing.isEmpty();
                if (!hasAny) {
                    EmployeeEducationDO edu = new EmployeeEducationDO();
                    edu.setProfileId(profile.getId());
                    edu.setEducation(education);
                    edu.setSchoolName(school);
                    edu.setMajor(major);
                    edu.setGraduationDate(graduationDate);
                    edu.setIsHighest(Boolean.TRUE);
                    employeeEducationMapper.insert(edu);
                    changedFields.add("education:created");
                }
            }

            // 子女家庭信息
            String childName = trim(roster.getChildName());
            LocalDate childBirth = parseDate(roster.getChildBirthDate());
            Integer childGender = parseGender(roster.getChildSex());
            String haveChild = trim(roster.getHaveChild());
            boolean hasChildSignal = StringUtils.hasText(childName) || childBirth != null || childGender != null
                    || (StringUtils.hasText(haveChild) && (haveChild.contains("是") || "1".equals(haveChild)
                    || "true".equalsIgnoreCase(haveChild) || haveChild.contains("有")));
            if (hasChildSignal) {
                List<EmployeeFamilyDO> families = employeeFamilyMapper.selectListByProfileId(profile.getId());
                boolean hasChildRow = false;
                if (families != null) {
                    for (EmployeeFamilyDO family : families) {
                        String relation = trim(family.getRelation());
                        if (relation != null && (relation.contains("子女") || relation.contains("子")
                                || relation.contains("女") || relation.contains("孩子"))) {
                            hasChildRow = true;
                            break;
                        }
                    }
                }
                if (!hasChildRow && (StringUtils.hasText(childName) || childBirth != null || childGender != null)) {
                    EmployeeFamilyDO family = new EmployeeFamilyDO();
                    family.setProfileId(profile.getId());
                    family.setRelation("子女");
                    family.setName(StringUtils.hasText(childName) ? childName : "未填写");
                    family.setGender(childGender);
                    family.setBirthDate(childBirth);
                    employeeFamilyMapper.insert(family);
                    changedFields.add("familyChild:created");
                }
            }
            return changedFields;
        });
    }

    private Integer parseContractType(String value) {
        String text = trim(value);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        if (text.contains("劳务")) {
            return 2;
        }
        if (text.contains("实习")) {
            return 3;
        }
        if (text.contains("劳动") || "1".equals(text)) {
            return 1;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Integer parseEmploymentType(String value) {
        String text = trim(value);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        // 钉钉员工类型常见：1全职 2兼职 3实习 4劳务 等，尽量兼容中文
        if (text.contains("全职") || "1".equals(text)) {
            return 1;
        }
        if (text.contains("兼职") || "2".equals(text)) {
            return 2;
        }
        if (text.contains("劳务") || "4".equals(text) || "3".equals(text) && text.contains("外包")) {
            return 3;
        }
        if (text.contains("实习")) {
            return 4;
        }
        try {
            int num = Integer.parseInt(text);
            if (num >= 1 && num <= 4) {
                return num;
            }
        } catch (NumberFormatException ignored) {
            // ignore
        }
        return null;
    }

    private String firstNonBlank(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first.trim();
        }
        if (StringUtils.hasText(second)) {
            return second.trim();
        }
        return null;
    }

    private void applyRosterProfileValues(EmployeeProfileDO profile, EmployeeProfileDO update) {
        if (profile == null || update == null) {
            return;
        }
        if (update.getIdNumber() != null) {
            profile.setIdNumber(update.getIdNumber());
        }
        if (update.getGender() != null) {
            profile.setGender(update.getGender());
        }
        if (update.getBirthDate() != null) {
            profile.setBirthDate(update.getBirthDate());
        }
        if (update.getAge() != null) {
            profile.setAge(update.getAge());
        }
        if (update.getEthnicity() != null) {
            profile.setEthnicity(update.getEthnicity());
        }
        if (update.getPoliticalStatus() != null) {
            profile.setPoliticalStatus(update.getPoliticalStatus());
        }
        if (update.getMaritalStatus() != null) {
            profile.setMaritalStatus(update.getMaritalStatus());
        }
        if (update.getHometown() != null) {
            profile.setHometown(update.getHometown());
        }
        if (update.getAddress() != null) {
            profile.setAddress(update.getAddress());
        }
        if (update.getEmergencyContact() != null) {
            profile.setEmergencyContact(update.getEmergencyContact());
        }
        if (update.getEmergencyPhone() != null) {
            profile.setEmergencyPhone(update.getEmergencyPhone());
        }
        if (update.getEmergencyRelation() != null) {
            profile.setEmergencyRelation(update.getEmergencyRelation());
        }
        if (update.getEmail() != null) {
            profile.setEmail(update.getEmail());
        }
        if (update.getOnboardDate() != null) {
            profile.setOnboardDate(update.getOnboardDate());
        }
        if (update.getConfirmationDate() != null) {
            profile.setConfirmationDate(update.getConfirmationDate());
        }
        if (update.getCompanyYears() != null) {
            profile.setCompanyYears(update.getCompanyYears());
        }
    }

    private boolean shouldApplyRosterTextField(String oldValue) {
        if (isRosterProfileOverwriteEnabled()) {
            return true;
        }
        return isRosterProfileBackfillEmptyOnlyEnabled() && !StringUtils.hasText(trim(oldValue));
    }

    private boolean shouldApplyRosterValueField(Object oldValue) {
        if (isRosterProfileOverwriteEnabled()) {
            return true;
        }
        return isRosterProfileBackfillEmptyOnlyEnabled() && oldValue == null;
    }

    private boolean shouldApplyRosterAgeField(EmployeeProfileDO profile, LocalDate newBirthDate,
                                              boolean birthDateChanged) {
        if (isRosterProfileOverwriteEnabled()) {
            return true;
        }
        if (!isRosterProfileBackfillEmptyOnlyEnabled() || profile == null || profile.getAge() != null) {
            return false;
        }
        LocalDate oldBirthDate = profile.getBirthDate();
        return oldBirthDate == null || Objects.equals(oldBirthDate, newBirthDate) || birthDateChanged;
    }

    private boolean shouldApplyRosterCompanyYearsField(EmployeeProfileDO profile, LocalDate newOnboardDate,
                                                       boolean onboardDateChanged) {
        if (isRosterProfileOverwriteEnabled()) {
            return true;
        }
        if (!isRosterProfileBackfillEmptyOnlyEnabled() || profile == null || profile.getCompanyYears() != null) {
            return false;
        }
        LocalDate oldOnboardDate = profile.getOnboardDate();
        return oldOnboardDate == null || Objects.equals(oldOnboardDate, newOnboardDate) || onboardDateChanged;
    }

    private boolean isRosterProfileOverwriteEnabled() {
        return dingTalkProperties.getSync() != null
                && Boolean.TRUE.equals(dingTalkProperties.getSync().getRosterOverwriteProfileEnabled());
    }

    private boolean isRosterProfileBackfillEmptyOnlyEnabled() {
        return dingTalkProperties.getSync() == null
                || !Boolean.FALSE.equals(dingTalkProperties.getSync().getRosterBackfillProfileEmptyOnly());
    }

    private List<String> collectRosterCustomChangedFields(EmployeeProfileDO profile, DingTalkRosterSnapshot roster) {
        Long targetTenantId = resolveTargetTenantId(profile == null ? null : profile.getTenantId());
        return TenantUtils.execute(targetTenantId, () -> collectRosterCustomChangedFieldsInCurrentTenant(profile, roster));
    }

    private List<String> collectRosterCustomChangedFieldsInCurrentTenant(EmployeeProfileDO profile,
                                                                         DingTalkRosterSnapshot roster) {
        List<String> changedFields = new ArrayList<>();
        if (profile == null || profile.getId() == null || roster == null) {
            return changedFields;
        }
        for (RosterCustomFieldSpec spec : buildRosterCustomFieldSpecs(roster)) {
            String value = trim(spec.getValue());
            if (!StringUtils.hasText(value)) {
                continue;
            }
            EmployeeCustomFieldDO field = employeeCustomFieldMapper.selectByFieldKey(spec.getFieldKey());
            String oldValue = null;
            if (field != null && field.getId() != null) {
                EmployeeCustomFieldValueDO valueDO =
                        employeeCustomFieldValueMapper.selectByProfileAndField(profile.getId(), field.getId());
                oldValue = valueDO == null ? null : trim(valueDO.getFieldValue());
            }
            if (!Objects.equals(oldValue, value)) {
                changedFields.add(spec.isSensitive()
                        ? formatSensitiveChangedField(spec.getFieldKey(), oldValue, value)
                        : formatChangedField(spec.getFieldKey(), oldValue, value));
            }
        }
        return changedFields;
    }

    private int syncRosterCustomFields(EmployeeProfileDO profile, DingTalkRosterSnapshot roster) {
        Long targetTenantId = resolveTargetTenantId(profile == null ? null : profile.getTenantId());
        return TenantUtils.execute(targetTenantId, () -> syncRosterCustomFieldsInCurrentTenant(profile, roster));
    }

    private int syncRosterCustomFieldsInCurrentTenant(EmployeeProfileDO profile, DingTalkRosterSnapshot roster) {
        if (profile == null || profile.getId() == null || roster == null) {
            return 0;
        }
        int changed = 0;
        for (RosterCustomFieldSpec spec : buildRosterCustomFieldSpecs(roster)) {
            String value = trim(spec.getValue());
            if (!StringUtils.hasText(value)) {
                continue;
            }
            EmployeeCustomFieldDO field = ensureRosterCustomField(spec);
            if (field == null || field.getId() == null) {
                continue;
            }
            EmployeeCustomFieldValueDO existed =
                    employeeCustomFieldValueMapper.selectByProfileAndField(profile.getId(), field.getId());
            if (existed == null) {
                EmployeeCustomFieldValueDO create = new EmployeeCustomFieldValueDO();
                create.setProfileId(profile.getId());
                create.setFieldId(field.getId());
                create.setFieldKey(field.getFieldKey());
                create.setFieldValue(value);
                employeeCustomFieldValueMapper.insert(create);
                changed++;
                continue;
            }
            String oldValue = trim(existed.getFieldValue());
            if (!Objects.equals(oldValue, value)) {
                EmployeeCustomFieldValueDO update = new EmployeeCustomFieldValueDO();
                update.setId(existed.getId());
                update.setFieldValue(value);
                employeeCustomFieldValueMapper.updateById(update);
                changed++;
            }
        }
        return changed;
    }

    private EmployeeCustomFieldDO ensureRosterCustomField(RosterCustomFieldSpec spec) {
        EmployeeCustomFieldDO field = employeeCustomFieldMapper.selectByFieldKey(spec.getFieldKey());
        if (field != null) {
            return field;
        }
        EmployeeCustomFieldDO create = new EmployeeCustomFieldDO();
        create.setFieldKey(spec.getFieldKey());
        create.setFieldName(spec.getFieldName());
        create.setFieldType(spec.getFieldType());
        create.setFieldGroup(CUSTOM_FIELD_GROUP_DINGTALK_ROSTER);
        create.setRequiredFlag(false);
        create.setSensitiveFlag(spec.isSensitive());
        create.setVisibleRoles("HROwner,super_admin,tenant_admin");
        create.setSortOrder(spec.getSortOrder());
        create.setStatus(0);
        employeeCustomFieldMapper.insert(create);
        return create;
    }

    private List<RosterCustomFieldSpec> buildRosterCustomFieldSpecs(DingTalkRosterSnapshot roster) {
        List<RosterCustomFieldSpec> specs = new ArrayList<>();
        specs.add(new RosterCustomFieldSpec(CUSTOM_FIELD_ID_CARD_NAME, "身份证姓名",
                CUSTOM_FIELD_TYPE_TEXT, trim(roster.getRealName()), true, 10));
        specs.add(new RosterCustomFieldSpec(CUSTOM_FIELD_CERTIFICATE_EXPIRE_DATE, "证件到期日",
                CUSTOM_FIELD_TYPE_DATE, formatDateOrText(roster.getCertEndTime()), true, 20));
        specs.add(new RosterCustomFieldSpec(CUSTOM_FIELD_RESIDENCE_TYPE, "户籍类型",
                CUSTOM_FIELD_TYPE_TEXT, trim(roster.getResidenceType()), false, 30));
        specs.add(new RosterCustomFieldSpec(CUSTOM_FIELD_FIRST_WORK_TIME, "首次参加工作时间",
                CUSTOM_FIELD_TYPE_DATE, formatDateOrText(roster.getJoinWorkingTime()), false, 40));
        specs.add(new RosterCustomFieldSpec(CUSTOM_FIELD_PERSONAL_SI, "个人社保账号",
                CUSTOM_FIELD_TYPE_TEXT, trim(roster.getPersonalSi()), true, 50));
        specs.add(new RosterCustomFieldSpec(CUSTOM_FIELD_PERSONAL_HF, "个人公积金账号",
                CUSTOM_FIELD_TYPE_TEXT, trim(roster.getPersonalHf()), true, 60));
        specs.add(new RosterCustomFieldSpec(CUSTOM_FIELD_HIGHEST_EDU, "最高学历",
                CUSTOM_FIELD_TYPE_TEXT, trim(roster.getHighestEdu()), false, 70));
        specs.add(new RosterCustomFieldSpec(CUSTOM_FIELD_GRADUATE_SCHOOL, "毕业院校",
                CUSTOM_FIELD_TYPE_TEXT, trim(roster.getGraduateSchool()), false, 80));
        specs.add(new RosterCustomFieldSpec(CUSTOM_FIELD_GRADUATION_TIME, "毕业时间",
                CUSTOM_FIELD_TYPE_DATE, formatDateOrText(roster.getGraduationTime()), false, 90));
        specs.add(new RosterCustomFieldSpec(CUSTOM_FIELD_MAJOR, "所学专业",
                CUSTOM_FIELD_TYPE_TEXT, trim(roster.getMajor()), false, 100));
        specs.add(new RosterCustomFieldSpec(CUSTOM_FIELD_CONTRACT_COMPANY, "合同公司",
                CUSTOM_FIELD_TYPE_TEXT, trim(roster.getContractCompanyName()), false, 110));
        specs.add(new RosterCustomFieldSpec(CUSTOM_FIELD_HAVE_CHILD, "是否有子女",
                CUSTOM_FIELD_TYPE_TEXT, trim(roster.getHaveChild()), false, 120));
        specs.add(new RosterCustomFieldSpec(CUSTOM_FIELD_CHILD_NAME, "子女姓名",
                CUSTOM_FIELD_TYPE_TEXT, trim(roster.getChildName()), false, 130));
        specs.add(new RosterCustomFieldSpec(CUSTOM_FIELD_CHILD_SEX, "子女性别",
                CUSTOM_FIELD_TYPE_TEXT, trim(roster.getChildSex()), false, 140));
        specs.add(new RosterCustomFieldSpec(CUSTOM_FIELD_CHILD_BIRTH_DATE, "子女出生日期",
                CUSTOM_FIELD_TYPE_DATE, formatDateOrText(roster.getChildBirthDate()), false, 150));
        return specs;
    }

    private String formatChangedField(String field, String oldValue, String newValue) {
        return field + ":" + formatDiffValue(oldValue) + "->" + formatDiffValue(newValue);
    }

    private String formatSensitiveChangedField(String field, String oldValue, String newValue) {
        return field + ":" + maskDiffValue(oldValue) + "->" + maskDiffValue(newValue);
    }

    private String formatDiffValue(String value) {
        return StringUtils.hasText(value) ? value : "无";
    }

    private String maskDiffValue(String value) {
        String text = trim(value);
        if (!StringUtils.hasText(text)) {
            return "无";
        }
        if (text.length() <= 4) {
            return "****";
        }
        if (text.length() <= 8) {
            return text.substring(0, 1) + "****" + text.substring(text.length() - 1);
        }
        return text.substring(0, 3) + "****" + text.substring(text.length() - 4);
    }

    private Integer parseGender(String value) {
        String text = normalizeName(value);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        if ("1".equals(text) || "男".equals(text) || "男性".equals(text) || "male".equalsIgnoreCase(text)) {
            return 1;
        }
        if ("2".equals(text) || "女".equals(text) || "女性".equals(text) || "female".equalsIgnoreCase(text)) {
            return 2;
        }
        return null;
    }

    private String toGenderText(Integer gender) {
        if (gender == null) {
            return null;
        }
        if (Objects.equals(gender, 1)) {
            return "男";
        }
        if (Objects.equals(gender, 2)) {
            return "女";
        }
        return String.valueOf(gender);
    }

    private Integer parseMaritalStatus(String value) {
        String text = normalizeName(value);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        if ("1".equals(text) || "未婚".equals(text) || "单身".equals(text)) {
            return 1;
        }
        if ("2".equals(text) || "已婚".equals(text)) {
            return 2;
        }
        if ("3".equals(text) || "离异".equals(text) || "离婚".equals(text)) {
            return 3;
        }
        if ("4".equals(text) || "丧偶".equals(text)) {
            return 4;
        }
        return null;
    }

    private String toMaritalStatusText(Integer maritalStatus) {
        if (maritalStatus == null) {
            return null;
        }
        switch (maritalStatus) {
            case 1:
                return "未婚";
            case 2:
                return "已婚";
            case 3:
                return "离异";
            case 4:
                return "丧偶";
            default:
                return String.valueOf(maritalStatus);
        }
    }

    private LocalDate parseDate(String value) {
        String text = trim(value);
        if (!StringUtils.hasText(text) || "长期".equals(text)) {
            return null;
        }
        if (text.matches("^\\d{13}$")) {
            return java.time.Instant.ofEpochMilli(Long.parseLong(text))
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        }
        if (text.matches("^\\d{10}$")) {
            return java.time.Instant.ofEpochSecond(Long.parseLong(text))
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        }
        String normalized = text;
        int spaceIndex = normalized.indexOf(' ');
        if (spaceIndex > 0) {
            normalized = normalized.substring(0, spaceIndex);
        }
        int timeIndex = normalized.indexOf('T');
        if (timeIndex > 0) {
            normalized = normalized.substring(0, timeIndex);
        }
        List<DateTimeFormatter> formatters = java.util.Arrays.asList(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("yyyy/M/d"),
                DateTimeFormatter.ofPattern("yyyy.M.d"),
                DateTimeFormatter.ofPattern("yyyy年M月d日"),
                DateTimeFormatter.ofPattern("yyyyMMdd")
        );
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }
        return null;
    }

    private String formatDateOrText(String value) {
        String text = trim(value);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        LocalDate date = parseDate(text);
        return date == null ? text : date.toString();
    }

    private String formatDate(LocalDate value) {
        return value == null ? null : value.toString();
    }

    private Integer calculateAge(LocalDate birthDate) {
        if (birthDate == null || birthDate.isAfter(LocalDate.now())) {
            return null;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    private BigDecimal calculateYears(LocalDate startDate) {
        if (startDate == null || startDate.isAfter(LocalDate.now())) {
            return null;
        }
        long days = ChronoUnit.DAYS.between(startDate, LocalDate.now());
        return BigDecimal.valueOf(days)
                .divide(BigDecimal.valueOf(365.25D), 2, RoundingMode.HALF_UP);
    }

    private boolean sameBigDecimal(BigDecimal left, BigDecimal right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.compareTo(right) == 0;
    }

    private String formatBigDecimal(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private String toString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String normalizeJobTitle(String value) {
        return trim(value);
    }

    private String toWorkStatusText(Integer status) {
        if (status == null) {
            return null;
        }
        return Objects.equals(status, 1) ? "在职" : "离职";
    }

    private boolean resolveAutoCreateEnabled(Boolean requestValue) {
        if (requestValue != null) {
            return requestValue;
        }
        return dingTalkProperties.getSync() != null
                && Boolean.TRUE.equals(dingTalkProperties.getSync().getEmployeeAutoCreateEnabled());
    }

    private Long resolveAutoCreateDeptId(Long requestValue) {
        if (requestValue != null && requestValue > 0) {
            return requestValue;
        }
        Long configured = dingTalkProperties.getSync() == null ? null : dingTalkProperties.getSync().getEmployeeAutoCreateDeptId();
        if (configured != null && configured > 0) {
            return configured;
        }
        Integer rootDeptId = dingTalkProperties.getSync() == null ? null : dingTalkProperties.getSync().getRootDeptId();
        if (rootDeptId != null && rootDeptId > 0) {
            return rootDeptId.longValue();
        }
        return 1L;
    }

    private String buildCreatePreviewHint(CreateIdentity identity, ResolvedDept resolvedDept) {
        StringBuilder hint = new StringBuilder();
        if (resolvedDept.getTenantId() != null) {
            hint.append("tenantId=").append(resolvedDept.getTenantId()).append(",");
        }
        hint.append("deptId=").append(resolvedDept.getDeptId());
        if (StringUtils.hasText(resolvedDept.getMatchType())) {
            hint.append(",deptMatch=").append(resolvedDept.getMatchType());
        }
        if (StringUtils.hasText(identity.getAutoFillFields())) {
            hint.append(",autoFill=").append(identity.getAutoFillFields());
        }
        return hint.toString();
    }

    private boolean existsIdentityInCurrentTenant(String mobile, String name, MatchContext context) {
        if (context == null) {
            return false;
        }
        return (StringUtils.hasText(mobile) && context.existingMobileSet.contains(mobile))
                || (StringUtils.hasText(name) && context.existingNameSet.contains(name));
    }

    private boolean existsIdentityInCrossTenants(String mobile, String name, MatchContext context) {
        if (context == null) {
            return false;
        }
        return (StringUtils.hasText(mobile) && context.crossTenantMobileSet.contains(mobile))
                || (StringUtils.hasText(name) && context.crossTenantNameSet.contains(name));
    }

    private void registerCreatedIdentity(String mobile, String name, MatchContext context) {
        if (context == null) {
            return;
        }
        if (StringUtils.hasText(mobile)) {
            context.existingMobileSet.add(mobile);
        }
        if (StringUtils.hasText(name)) {
            context.existingNameSet.add(name);
        }
    }

    private void syncMatchedEmployeeOrganization(DingTalkUserSnapshot user,
                                                 EmployeeProfileDO profile,
                                                 DeptRouteContext routeContext,
                                                 EmployeeSyncReport report) {
        if (user == null || profile == null) {
            return;
        }
        Long targetTenantId = resolveTargetTenantId(profile.getTenantId());
        TenantUtils.execute(targetTenantId, () -> {
            EmployeeEntryDO entry = profile.getUserId() == null
                    ? firstEntryByProfileId(profile.getId())
                    : employeeEntryMapper.selectByUserId(profile.getUserId());
            AdminUserRespDTO adminUser = profile.getUserId() == null ? null
                    : safeGetAdminUserMap(Collections.singleton(profile.getUserId())).get(profile.getUserId());
            syncMatchedEmployeeJobTitleInCurrentTenant(user, entry, report);
            syncMatchedEmployeeJobNumberInCurrentTenant(user, profile, entry, report);
            syncMatchedAdminUserMobileInCurrentTenant(user, profile, adminUser, report);
            if (routeContext != null) {
                ResolvedDept resolvedDept = resolveDeptForExisting(user, routeContext, profile.getTenantId());
                syncMatchedEmployeeDeptInCurrentTenant(profile, entry, resolvedDept, adminUser, report);
            }
            syncMatchedEmployeeFunctionsInCurrentTenant(user, profile, report);
            return null;
        });
    }

    private void syncMatchedEmployeeDeptInCurrentTenant(EmployeeProfileDO profile,
                                                        EmployeeEntryDO entry,
                                                        ResolvedDept resolvedDept,
                                                        AdminUserRespDTO adminUser,
                                                        EmployeeSyncReport report) {
        Long deptId = resolvedDept == null ? null : resolvedDept.getDeptId();
        if (profile == null || !isResolvedCreateDept(resolvedDept) || deptId == null || deptId <= 0) {
            return;
        }
        if (entry != null && !Objects.equals(entry.getDeptId(), deptId)) {
            recordEmployeeSnapshot(report, "ENTRY_DEPT_SYNC",
                    buildDeptSyncReason(resolvedDept), null, entry, null, null);
            EmployeeEntryDO updateEntry = new EmployeeEntryDO();
            updateEntry.setId(entry.getId());
            updateEntry.setDeptId(deptId);
            employeeEntryMapper.updateById(updateEntry);
            if (report != null) {
                report.increaseSyncedDeptIds();
            }
        }

        if (profile.getUserId() == null) {
            return;
        }
        if (adminUser != null && !Objects.equals(adminUser.getDeptId(), deptId)
                && recordAndUpdateAdminUserDept(report, profile.getUserId(), deptId,
                "ADMIN_USER_DEPT_SYNC", buildDeptSyncReason(resolvedDept), adminUser)) {
            if (report != null) {
                report.increaseSyncedDeptIds();
            }
        }
    }

    private void syncMatchedAdminUserMobileInCurrentTenant(DingTalkUserSnapshot user,
                                                           EmployeeProfileDO profile,
                                                           AdminUserRespDTO adminUser,
                                                           EmployeeSyncReport report) {
        if (user == null || profile == null || profile.getUserId() == null || adminUser == null) {
            return;
        }
        String newMobile = normalizeMobile(user.getMobile());
        String oldMobile = normalizeMobile(adminUser.getMobile());
        if (!StringUtils.hasText(newMobile) || Objects.equals(oldMobile, newMobile)) {
            return;
        }
        recordEmployeeSnapshot(report, "ADMIN_USER_MOBILE_SYNC",
                formatChangedField("mobile", oldMobile, newMobile), null, null, adminUser, null);
        safeUpdateAdminUserMobile(profile.getUserId(), newMobile);
    }

    private void syncMatchedEmployeeJobTitleInCurrentTenant(DingTalkUserSnapshot user,
                                                            EmployeeEntryDO entry,
                                                            EmployeeSyncReport report) {
        String jobTitle = normalizeJobTitle(user == null ? null : user.getJobTitle());
        if (!StringUtils.hasText(jobTitle) || entry == null || entry.getId() == null) {
            return;
        }
        String oldJobTitle = normalizeJobTitle(entry.getJobTitle());
        if (Objects.equals(oldJobTitle, jobTitle)) {
            return;
        }
        recordEmployeeSnapshot(report, "ENTRY_JOB_TITLE_SYNC",
                formatChangedField("jobTitle", oldJobTitle, jobTitle), null, entry, null, null);
        EmployeeEntryDO updateEntry = new EmployeeEntryDO();
        updateEntry.setId(entry.getId());
        updateEntry.setJobTitle(jobTitle);
        employeeEntryMapper.updateById(updateEntry);
        if (report != null) {
            report.increaseSyncedJobTitles();
        }
    }

    /**
     * 同步钉钉工号：优先写 entry.employeeNo，并尽量把 profile.profileNo 对齐为同一工号。
     * 列表页工号列当前映射 profile_no，因此两边一起更新才能在成员管理/花名册显示正确。
     */
    private void syncMatchedEmployeeJobNumberInCurrentTenant(DingTalkUserSnapshot user,
                                                             EmployeeProfileDO profile,
                                                             EmployeeEntryDO entry,
                                                             EmployeeSyncReport report) {
        String jobNumber = normalizeJobNumber(user == null ? null : user.getJobNumber());
        if (!StringUtils.hasText(jobNumber)) {
            return;
        }

        boolean entryChanged = false;
        boolean profileChanged = false;
        String oldEmployeeNo = entry == null ? null : trim(entry.getEmployeeNo());
        String oldProfileNo = profile == null ? null : trim(profile.getProfileNo());

        if (entry != null && entry.getId() != null && !Objects.equals(oldEmployeeNo, jobNumber)) {
            EmployeeEntryDO updateEntry = new EmployeeEntryDO();
            updateEntry.setId(entry.getId());
            updateEntry.setEmployeeNo(jobNumber);
            employeeEntryMapper.updateById(updateEntry);
            entry.setEmployeeNo(jobNumber);
            entryChanged = true;
        }
        if (profile != null && profile.getId() != null && !Objects.equals(oldProfileNo, jobNumber)) {
            EmployeeProfileDO updateProfile = new EmployeeProfileDO();
            updateProfile.setId(profile.getId());
            updateProfile.setProfileNo(jobNumber);
            employeeProfileMapper.updateById(updateProfile);
            profile.setProfileNo(jobNumber);
            profileChanged = true;
        }
        if (!entryChanged && !profileChanged) {
            return;
        }

        List<String> changedFields = new ArrayList<>();
        if (entryChanged) {
            changedFields.add(formatChangedField("employeeNo", oldEmployeeNo, jobNumber));
        }
        if (profileChanged) {
            changedFields.add(formatChangedField("profileNo", oldProfileNo, jobNumber));
        }
        recordEmployeeSnapshot(report, "EMPLOYEE_NO_SYNC", String.join(",", changedFields),
                profile, entry, null, null);
        if (report != null) {
            report.increaseSyncedJobNumbers();
        }
    }

    private String normalizeJobNumber(String jobNumber) {
        String value = trim(jobNumber);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        // 钉钉偶发返回占位符
        if ("-".equals(value) || "无".equals(value) || "null".equalsIgnoreCase(value)) {
            return null;
        }
        return value;
    }

    private void syncMatchedEmployeeFunctionsInCurrentTenant(DingTalkUserSnapshot user,
                                                             EmployeeProfileDO profile,
                                                             EmployeeSyncReport report) {
        if (user == null || profile == null || profile.getUserId() == null) {
            return;
        }
        UserFunctionSyncReqDTO reqDTO = new UserFunctionSyncReqDTO();
        reqDTO.setUserId(profile.getUserId());
        reqDTO.setPostName(normalizeJobTitle(user.getJobTitle()));
        reqDTO.setRoles(buildFunctionSyncRoles(user));
        reqDTO.setManagedRoleCodes(buildFunctionManagedRoleCodes());
        reqDTO.setRemoveDingTalkGeneratedRoles(true);
        reqDTO.setSource(FUNCTION_SYNC_SOURCE);

        try {
            List<UserFunctionSyncRespDTO> respList = adminUserApi
                    .syncUserFunctionsBatch(Collections.singletonList(reqDTO))
                    .getCheckedData();
            UserFunctionSyncRespDTO respDTO = respList == null || respList.isEmpty() ? null : respList.get(0);
            applyFunctionSyncReport(user, report, respDTO);
        } catch (Exception ex) {
            if (report != null) {
                report.increaseFunctionSyncFailed();
                report.addSyncDetail(normalizeDingUserId(user.getUserId()), trim(user.getName()),
                        normalizeMobile(user.getMobile()), trim(user.getEmail()),
                        "FUNCTION", "FUNCTION_SYNC_FAILED", resolveErrorMessage(ex));
            }
            log.warn("Sync DingTalk user functions failed, dingUserId={}, userId={}, reason={}",
                    normalizeDingUserId(user.getUserId()), profile.getUserId(), resolveErrorMessage(ex));
        }
    }

    private List<UserFunctionSyncReqDTO.RoleItem> buildFunctionSyncRoles(DingTalkUserSnapshot user) {
        List<UserFunctionSyncReqDTO.RoleItem> roles = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        addFunctionRole(roles, seen, ROLE_CODE_MEMBER, ROLE_NAME_MEMBER, true);
        if (Boolean.TRUE.equals(user.getLeader())) {
            addFunctionRole(roles, seen, ROLE_CODE_LEADER, ROLE_NAME_LEADER, true);
        }
        if (Boolean.TRUE.equals(user.getBoss())) {
            addFunctionRole(roles, seen, ROLE_CODE_BOSS, ROLE_NAME_BOSS, true);
        }

        boolean dingTalkAdmin = Boolean.TRUE.equals(user.getAdmin());
        List<String> roleNames = user.getRoleNames();
        if (roleNames != null) {
            for (String roleName : roleNames) {
                String normalizedRoleName = normalizeRoleName(roleName);
                if (!StringUtils.hasText(normalizedRoleName)) {
                    continue;
                }
                if (isDingTalkAdminRoleName(normalizedRoleName)) {
                    dingTalkAdmin = true;
                    continue;
                }
                if (addMappedFunctionRole(roles, seen, normalizedRoleName)) {
                    continue;
                }
                addFunctionRole(roles, seen, null, normalizedRoleName, true);
            }
        }
        addTitleFunctionRoles(roles, seen, normalizeRoleName(user.getJobTitle()));
        addTitleFunctionRoles(roles, seen, normalizeRoleName(user.getName()));
        if (dingTalkAdmin) {
            addFunctionRole(roles, seen, ROLE_CODE_TENANT_ADMIN, ROLE_NAME_TENANT_ADMIN, true);
        }
        return roles;
    }

    private boolean addMappedFunctionRole(List<UserFunctionSyncReqDTO.RoleItem> roles,
                                          Set<String> seen,
                                          String roleName) {
        String lowerRoleName = roleName.toLowerCase();
        if (roleName.contains("财务") || roleName.contains("会计") || roleName.contains("出纳")) {
            addFunctionRole(roles, seen, ROLE_CODE_FINANCE, ROLE_NAME_FINANCE, true);
            return true;
        }
        if (roleName.contains("人事") || "hr".equals(lowerRoleName)
                || lowerRoleName.contains("humanresource")) {
            addFunctionRole(roles, seen, ROLE_CODE_HR, ROLE_NAME_HR, true);
            return true;
        }
        if (roleName.contains("老板") || roleName.contains("总经理") || roleName.contains("董事长")) {
            addFunctionRole(roles, seen, ROLE_CODE_BOSS, ROLE_NAME_BOSS, true);
            return true;
        }
        if (roleName.contains("队长") || roleName.contains("部门领导")
                || roleName.contains("负责人") || roleName.contains("主管")) {
            addFunctionRole(roles, seen, ROLE_CODE_LEADER, ROLE_NAME_LEADER, true);
            return true;
        }
        if ("普通员工".equals(roleName) || "员工".equals(roleName)) {
            addFunctionRole(roles, seen, ROLE_CODE_MEMBER, ROLE_NAME_MEMBER, true);
            return true;
        }
        return false;
    }

    private void addTitleFunctionRoles(List<UserFunctionSyncReqDTO.RoleItem> roles,
                                       Set<String> seen,
                                       String titleText) {
        if (!StringUtils.hasText(titleText)) {
            return;
        }
        if (titleText.contains("财务") || titleText.contains("会计") || titleText.contains("出纳")) {
            addFunctionRole(roles, seen, ROLE_CODE_FINANCE, ROLE_NAME_FINANCE, true);
        }
        if (titleText.contains("人事") || titleText.toLowerCase().contains("hr")
                || titleText.toLowerCase().contains("humanresource")) {
            addFunctionRole(roles, seen, ROLE_CODE_HR, ROLE_NAME_HR, true);
        }
    }

    private void addFunctionRole(List<UserFunctionSyncReqDTO.RoleItem> roles,
                                 Set<String> seen,
                                 String code,
                                 String name,
                                 boolean createIfMissing) {
        String roleCode = trim(code);
        String roleName = trim(name);
        if (!StringUtils.hasText(roleCode) && !StringUtils.hasText(roleName)) {
            return;
        }
        String key = StringUtils.hasText(roleCode) ? "code:" + roleCode : "name:" + roleName;
        if (!seen.add(key)) {
            return;
        }
        UserFunctionSyncReqDTO.RoleItem roleItem = new UserFunctionSyncReqDTO.RoleItem();
        roleItem.setCode(roleCode);
        roleItem.setName(roleName);
        roleItem.setCreateIfMissing(createIfMissing);
        roles.add(roleItem);
    }

    private Set<String> buildFunctionManagedRoleCodes() {
        Set<String> codes = new LinkedHashSet<>();
        codes.add(ROLE_CODE_MEMBER);
        codes.add(ROLE_CODE_LEADER);
        codes.add(ROLE_CODE_BOSS);
        codes.add(ROLE_CODE_TENANT_ADMIN);
        codes.add(ROLE_CODE_HR);
        codes.add(ROLE_CODE_FINANCE);
        return codes;
    }

    private boolean isDingTalkAdminRoleName(String roleName) {
        if (!StringUtils.hasText(roleName)) {
            return false;
        }
        String lowerRoleName = roleName.toLowerCase();
        return roleName.contains("管理员") || lowerRoleName.contains("admin");
    }

    private String normalizeRoleName(String roleName) {
        return roleName == null ? null : roleName.trim().replaceAll("\\s+", "");
    }

    private void applyFunctionSyncReport(DingTalkUserSnapshot user,
                                         EmployeeSyncReport report,
                                         UserFunctionSyncRespDTO respDTO) {
        if (report == null || respDTO == null) {
            return;
        }
        if (respDTO.getPostId() != null) {
            report.increaseSyncedPosts();
        }
        if (Boolean.TRUE.equals(respDTO.getPostCreated())) {
            report.increaseCreatedPosts();
        }
        report.increaseCreatedRoles(safeCount(respDTO.getCreatedRoleCount()));
        report.increaseAssignedRoles(safeCount(respDTO.getAssignedRoleCount()));
        report.increaseRemovedRoles(safeCount(respDTO.getRemovedRoleCount()));

        int changed = safeCount(respDTO.getCreatedRoleCount()) + safeCount(respDTO.getAssignedRoleCount())
                + safeCount(respDTO.getRemovedRoleCount()) + (Boolean.TRUE.equals(respDTO.getPostCreated()) ? 1 : 0);
        if (changed > 0) {
            report.addSyncDetail(normalizeDingUserId(user.getUserId()), trim(user.getName()),
                    normalizeMobile(user.getMobile()), trim(user.getEmail()),
                    "FUNCTION", "FUNCTION_SYNCED", buildFunctionSyncDetail(user, respDTO));
        }
    }

    private String buildFunctionSyncDetail(DingTalkUserSnapshot user, UserFunctionSyncRespDTO respDTO) {
        List<String> details = new ArrayList<>();
        if (respDTO.getPostId() != null) {
            details.add("postId=" + respDTO.getPostId());
        }
        if (Boolean.TRUE.equals(respDTO.getPostCreated())) {
            details.add("postCreated=true");
        }
        if (safeCount(respDTO.getCreatedRoleCount()) > 0) {
            details.add("createdRoles=" + respDTO.getCreatedRoleCount());
        }
        if (safeCount(respDTO.getAssignedRoleCount()) > 0) {
            details.add("assignedRoles=" + respDTO.getAssignedRoleCount());
        }
        if (safeCount(respDTO.getRemovedRoleCount()) > 0) {
            details.add("removedRoles=" + respDTO.getRemovedRoleCount());
        }
        if (user != null && user.getRoleNames() != null && !user.getRoleNames().isEmpty()) {
            details.add("dingRoles=" + String.join("|", user.getRoleNames()));
        }
        return String.join(",", details);
    }

    private int safeCount(Integer value) {
        return value == null ? 0 : Math.max(value, 0);
    }

    private EmployeeEntryDO firstEntryByProfileId(Long profileId) {
        if (profileId == null) {
            return null;
        }
        List<EmployeeEntryDO> entries = employeeEntryMapper.selectListByProfileId(profileId);
        return entries == null || entries.isEmpty() ? null : entries.get(0);
    }

    private AutoCreateResult createMissingEmployee(DingTalkUserSnapshot user, CreateIdentity identity,
                                                   Long deptId, Long tenantId,
                                                   Map<String, EmployeeProfileDO> profileByMobile,
                                                   Map<String, EmployeeProfileDO> profileByName) {
        String mobile = normalizeMobile(identity == null ? null : identity.getMobile());
        String name = trim(identity == null ? null : identity.getName());
        if (!StringUtils.hasText(mobile) || !StringUtils.hasText(name)) {
            return AutoCreateResult.invalid("missing mobile or name");
        }
        if (deptId == null || deptId <= 0) {
            return AutoCreateResult.invalid("invalid deptId");
        }

        String mobileKey = normalizeMobile(mobile);
        String nameKey = normalizeName(name);
        EmployeeProfileDO existed = profileByMobile.get(mobileKey);
        if (existed == null) {
            existed = profileByName.get(nameKey);
        }
        if (existed != null) {
            return AutoCreateResult.reused(existed);
        }

        EmployeeCreateReqVO reqVO = new EmployeeCreateReqVO();
        reqVO.setName(name);
        reqVO.setMobile(mobile);
        reqVO.setEmail(trim(user.getEmail()));
        reqVO.setDeptId(deptId);
        reqVO.setJobTitle(normalizeJobTitle(user.getJobTitle()));
        reqVO.setEmploymentType(1);
        reqVO.setWorkStatus(Boolean.TRUE.equals(user.getActive()) ? WORK_STATUS_ACTIVE : WORK_STATUS_RESIGNED);
        reqVO.setStatus(Boolean.TRUE.equals(user.getActive()) ? PROFILE_STATUS_ENABLED : PROFILE_STATUS_DISABLED);

        try {
            Long targetTenantId = resolveTargetTenantId(tenantId);
            EmployeeProfileDO createdProfile = TenantUtils.execute(targetTenantId, () -> {
                Long profileId = employeeService.createEmployee(reqVO);
                return employeeProfileMapper.selectById(profileId);
            });
            if (createdProfile == null || createdProfile.getUserId() == null) {
                return AutoCreateResult.failed("profile created but user initialization missing");
            }
            profileByMobile.put(mobileKey, createdProfile);
            profileByName.put(nameKey, createdProfile);
            return AutoCreateResult.created(createdProfile);
        } catch (Exception ex) {
            log.warn("Create employee from DingTalk failed, dingUserId={}, tenantId={}, deptId={}, deptName={}, topDeptName={}, reason={}",
                    normalizeDingUserId(user == null ? null : user.getUserId()),
                    resolveTargetTenantId(tenantId), deptId, trim(user == null ? null : user.getDeptName()),
                    trim(user == null ? null : user.getTopDeptName()), resolveErrorMessage(ex));
            return AutoCreateResult.failed(resolveErrorMessage(ex));
        }
    }

    private CreateIdentity buildCreateIdentity(DingTalkUserSnapshot user) {
        CreateIdentity identity = new CreateIdentity();
        String mobile = normalizeMobile(user == null ? null : user.getMobile());
        String name = trim(user == null ? null : user.getName());
        boolean autoFillMobile = !StringUtils.hasText(mobile);
        boolean autoFillName = !StringUtils.hasText(name);
        identity.setMobile(mobile);
        identity.setName(name);
        if (autoFillMobile && autoFillName) {
            identity.setAutoFillFields("missing-mobile,missing-name");
        } else if (autoFillMobile) {
            identity.setAutoFillFields("missing-mobile");
        } else if (autoFillName) {
            identity.setAutoFillFields("missing-name");
        }
        return identity;
    }

    private void syncDingTalkDepartments(EmployeeSyncReport report) {
        List<DingTalkDeptSnapshot> departments = dingTalkDirectoryService.listAllDepartmentSnapshots();
        if (departments == null || departments.isEmpty()) {
            return;
        }
        Long tenantId = resolveTargetTenantId(null);
        for (DingTalkDeptSnapshot department : departments) {
            if (!isSyncableDingTalkDept(department)) {
                continue;
            }
            if (report != null) {
                report.increaseDeptTreePulled();
            }
            upsertDingTalkDepartment(department, tenantId, report);
        }
    }

    private boolean isSyncableDingTalkDept(DingTalkDeptSnapshot department) {
        Long deptId = department == null ? null : department.getDeptId();
        return deptId != null && deptId > 0 && !Objects.equals(deptId, DEFAULT_DEPT_ROOT_ID);
    }

    private void upsertDingTalkDepartment(DingTalkDeptSnapshot department, Long tenantId, EmployeeSyncReport report) {
        Long deptId = department.getDeptId();
        String deptName = trim(department.getName());
        if (!StringUtils.hasText(deptName)) {
            if (report != null) {
                report.increaseDeptTreeFailed();
            }
            throw new IllegalStateException("DingTalk department name is empty, deptId=" + deptId);
        }

        DeptUpsertReqDTO reqDTO = new DeptUpsertReqDTO();
        reqDTO.setId(deptId);
        reqDTO.setName(deptName);
        reqDTO.setParentId(resolveDingTalkDepartmentParentId(department));
        // 钉钉 order 可能很大，截断到 int 范围；null 时不覆盖本地 sort
        Long dingOrder = department.getOrder();
        if (dingOrder != null) {
            long sortValue = dingOrder;
            if (sortValue > Integer.MAX_VALUE) {
                sortValue = Integer.MAX_VALUE;
            } else if (sortValue < Integer.MIN_VALUE) {
                sortValue = Integer.MIN_VALUE;
            }
            reqDTO.setSort((int) sortValue);
        }
        reqDTO.setStatus(DEPT_STATUS_ENABLED);
        // 主管在人员同步后单独回写，此处不传 leaderUserId，避免清空本地主管

        try {
            DeptUpsertRespDTO respDTO = deptApi.upsertDept(reqDTO).getCheckedData();
            if (report != null) {
                report.increaseDeptTreeSynced();
                if (respDTO != null && Boolean.TRUE.equals(respDTO.getCreated())) {
                    report.increaseDeptTreeCreated();
                }
                if (respDTO != null && Boolean.TRUE.equals(respDTO.getUpdated())) {
                    report.increaseDeptTreeUpdated();
                }
            }
        } catch (Exception ex) {
            if (report != null) {
                report.increaseDeptTreeFailed();
            }
            log.warn("Sync DingTalk department failed, tenantId={}, deptId={}, deptName={}, parentId={}, reason={}",
                    tenantId, deptId, deptName, reqDTO.getParentId(), resolveErrorMessage(ex));
            throw new IllegalStateException("Sync DingTalk department failed, deptId=" + deptId
                    + ", deptName=" + deptName + ", reason=" + resolveErrorMessage(ex), ex);
        }
    }

    /**
     * 根据钉钉 isLeader 标记，将 OA 用户写回 system_dept.leader_user_id。
     * 同一部门多个主管时取第一个已绑定 OA 账号的用户；未解析到则跳过，不覆盖本地。
     */
    private void syncDeptLeadersFromUsers(List<DingTalkUserSnapshot> users,
                                          Map<String, BindingCandidate> bindingCandidates,
                                          EmployeeSyncReport report) {
        if (users == null || users.isEmpty() || bindingCandidates == null || bindingCandidates.isEmpty()) {
            return;
        }
        Map<Long, Long> leaderByDeptId = new LinkedHashMap<>();
        for (DingTalkUserSnapshot user : users) {
            if (user == null || !Boolean.TRUE.equals(user.getLeader())) {
                continue;
            }
            Long dingDeptId = user.getDeptId();
            if (dingDeptId == null || dingDeptId <= 0) {
                continue;
            }
            if (leaderByDeptId.containsKey(dingDeptId)) {
                continue;
            }
            String dingUserId = normalizeDingUserId(user.getUserId());
            if (!StringUtils.hasText(dingUserId)) {
                continue;
            }
            BindingCandidate candidate = bindingCandidates.get(dingUserId);
            Long oaUserId = candidate == null || candidate.profile == null
                    ? null : candidate.profile.getUserId();
            if (oaUserId == null || oaUserId <= 0) {
                continue;
            }
            leaderByDeptId.put(dingDeptId, oaUserId);
        }
        if (leaderByDeptId.isEmpty()) {
            return;
        }
        for (Map.Entry<Long, Long> entry : leaderByDeptId.entrySet()) {
            Long deptId = entry.getKey();
            Long leaderUserId = entry.getValue();
            try {
                DeptUpsertReqDTO reqDTO = new DeptUpsertReqDTO();
                reqDTO.setId(deptId);
                // name 必填：先读现有部门名；读不到则跳过
                DeptRespDTO existing = null;
                try {
                    existing = deptApi.getDept(deptId).getCheckedData();
                } catch (Exception ignored) {
                    // ignore
                }
                if (existing == null || !StringUtils.hasText(existing.getName())) {
                    continue;
                }
                if (Objects.equals(existing.getLeaderUserId(), leaderUserId)) {
                    continue;
                }
                reqDTO.setName(existing.getName());
                reqDTO.setLeaderUserId(leaderUserId);
                DeptUpsertRespDTO respDTO = deptApi.upsertDept(reqDTO).getCheckedData();
                if (report != null && respDTO != null && Boolean.TRUE.equals(respDTO.getUpdated())) {
                    report.increaseSyncedDeptLeaders();
                } else if (report != null && respDTO != null && Boolean.TRUE.equals(respDTO.getCreated())) {
                    report.increaseSyncedDeptLeaders();
                }
            } catch (Exception ex) {
                log.warn("Sync DingTalk dept leader failed, deptId={}, leaderUserId={}, reason={}",
                        deptId, leaderUserId, resolveErrorMessage(ex));
            }
        }
    }

    /**
     * 根据钉钉 manager_userid 回写 entry.directSupervisorId。
     * 注意：系统里 directSupervisorId 关联的是主管的 profileId（不是 userId）。
     */
    private void syncDirectSupervisorsFromUsers(List<DingTalkUserSnapshot> users,
                                                Map<String, BindingCandidate> bindingCandidates,
                                                EmployeeSyncReport report) {
        if (users == null || users.isEmpty() || bindingCandidates == null || bindingCandidates.isEmpty()) {
            return;
        }
        // dingUserId -> supervisor profileId
        Map<String, Long> supervisorProfileByDingUserId = new HashMap<>();
        for (BindingCandidate candidate : bindingCandidates.values()) {
            if (candidate == null || candidate.user == null || candidate.profile == null
                    || candidate.profile.getId() == null) {
                continue;
            }
            String dingUserId = normalizeDingUserId(candidate.user.getUserId());
            if (StringUtils.hasText(dingUserId)) {
                supervisorProfileByDingUserId.put(dingUserId, candidate.profile.getId());
            }
        }
        if (supervisorProfileByDingUserId.isEmpty()) {
            return;
        }

        for (DingTalkUserSnapshot user : users) {
            if (user == null) {
                continue;
            }
            String dingUserId = normalizeDingUserId(user.getUserId());
            if (!StringUtils.hasText(dingUserId)) {
                continue;
            }
            BindingCandidate candidate = bindingCandidates.get(dingUserId);
            if (candidate == null || candidate.profile == null || candidate.profile.getId() == null) {
                continue;
            }
            String managerDingUserId = normalizeDingUserId(user.getManagerUserId());
            if (!StringUtils.hasText(managerDingUserId)) {
                // 无主管：不强制清空本地已设主管，避免误伤
                continue;
            }
            Long supervisorProfileId = supervisorProfileByDingUserId.get(managerDingUserId);
            if (supervisorProfileId == null || Objects.equals(supervisorProfileId, candidate.profile.getId())) {
                continue;
            }
            try {
                Long targetTenantId = resolveTargetTenantId(candidate.profile.getTenantId());
                TenantUtils.execute(targetTenantId, () -> {
                    EmployeeEntryDO entry = candidate.profile.getUserId() == null
                            ? firstEntryByProfileId(candidate.profile.getId())
                            : employeeEntryMapper.selectByUserId(candidate.profile.getUserId());
                    if (entry == null || entry.getId() == null) {
                        return null;
                    }
                    if (Objects.equals(entry.getDirectSupervisorId(), supervisorProfileId)) {
                        return null;
                    }
                    EmployeeEntryDO updateEntry = new EmployeeEntryDO();
                    updateEntry.setId(entry.getId());
                    updateEntry.setDirectSupervisorId(supervisorProfileId);
                    employeeEntryMapper.updateById(updateEntry);
                    if (report != null) {
                        report.increaseSyncedDirectSupervisors();
                    }
                    return null;
                });
            } catch (Exception ex) {
                log.warn("Sync DingTalk direct supervisor failed, dingUserId={}, managerDingUserId={}, reason={}",
                        dingUserId, managerDingUserId, resolveErrorMessage(ex));
            }
        }
    }

    private Long resolveDingTalkDepartmentParentId(DingTalkDeptSnapshot department) {
        Long deptId = department == null ? null : department.getDeptId();
        Long parentId = department == null ? null : department.getParentId();
        if (parentId == null || parentId <= 0 || Objects.equals(parentId, deptId)) {
            return DEFAULT_DEPT_ROOT_ID;
        }
        return parentId;
    }

    private DeptRouteContext buildDeptRouteContext(Long requestDeptId) {
        DeptRouteContext routeContext = new DeptRouteContext();
        routeContext.setRequestedDeptId(requestDeptId);
        Long defaultTenantId = resolveTargetTenantId(null);
        routeContext.setDefaultTenantId(defaultTenantId);

        Set<Long> routeTenantIds = resolveRouteTenantIds(defaultTenantId);
        routeContext.setRouteTenantIds(routeTenantIds);
        routeContext.setTopDeptTenantMap(buildTopDeptTenantMap(routeTenantIds));

        DeptMatchContext defaultContext = getOrBuildDeptMatchContext(routeContext, defaultTenantId);
        Long defaultDeptId = defaultContext == null ? null : defaultContext.getDefaultDeptId();
        routeContext.setDefaultDeptId(defaultDeptId != null ? defaultDeptId : DEFAULT_DEPT_ROOT_ID);
        return routeContext;
    }

    private DeptMatchContext buildDeptMatchContext(Long tenantId, Long requestDeptId) {
        Long targetTenantId = resolveTargetTenantId(tenantId);
        return TenantUtils.execute(targetTenantId, () -> {
            DeptMatchContext context = new DeptMatchContext();
            List<DeptRespDTO> deptList = listAllDeptCandidates();
            context.setDeptCandidates(deptList == null ? new ArrayList<>() : deptList);

            Map<String, Long> exactMap = new HashMap<>();
            for (DeptRespDTO dept : context.getDeptCandidates()) {
                String key = normalizeDeptName(dept == null ? null : dept.getName());
                if (StringUtils.hasText(key) && dept != null && dept.getId() != null) {
                    exactMap.putIfAbsent(key, dept.getId());
                }
            }
            context.setExactNameDeptMap(exactMap);

            Long defaultDeptId = requestDeptId;
            if (!isDeptValid(defaultDeptId) || Objects.equals(defaultDeptId, DEFAULT_DEPT_ROOT_ID)) {
                defaultDeptId = pickConfiguredFallbackDeptId(context);
            }
            if (!isDeptValid(defaultDeptId)) {
                defaultDeptId = pickFallbackDeptId(context.getDeptCandidates());
            }
            context.setDefaultDeptId(defaultDeptId);
            return context;
        });
    }

    private List<DeptRespDTO> listAllDeptCandidates() {
        List<DeptRespDTO> result = new ArrayList<>();
        try {
            DeptRespDTO root = deptApi.getDept(DEFAULT_DEPT_ROOT_ID).getCheckedData();
            if (root != null && root.getId() != null) {
                result.add(root);
            }
        } catch (Exception ignored) {
            // ignore
        }
        try {
            List<DeptRespDTO> children = deptApi.getChildDeptList(DEFAULT_DEPT_ROOT_ID).getCheckedData();
            if (children != null) {
                for (DeptRespDTO child : children) {
                    if (child != null && child.getId() != null) {
                        result.add(child);
                    }
                }
            }
        } catch (Exception ignored) {
            // ignore
        }
        return dedupeDeptById(result);
    }

    private DeptMatchContext getOrBuildDeptMatchContext(DeptRouteContext routeContext, Long tenantId) {
        if (routeContext == null) {
            return null;
        }
        Long targetTenantId = resolveTargetTenantId(tenantId);
        DeptMatchContext existed = routeContext.getMatchContextByTenant().get(targetTenantId);
        if (existed != null) {
            return existed;
        }
        DeptMatchContext created = buildDeptMatchContext(targetTenantId, routeContext.getRequestedDeptId());
        routeContext.getMatchContextByTenant().put(targetTenantId, created);
        return created;
    }

    private Set<Long> resolveRouteTenantIds(Long defaultTenantId) {
        LinkedHashSet<Long> tenantIds = new LinkedHashSet<>();
        if (defaultTenantId != null && defaultTenantId > 0) {
            tenantIds.add(defaultTenantId);
        }
        Set<Long> contextTenantIds = TenantContextHolder.getTenantIdList();
        if (contextTenantIds != null && !contextTenantIds.isEmpty()) {
            for (Long tenantId : contextTenantIds) {
                if (tenantId != null && tenantId > 0) {
                    tenantIds.add(tenantId);
                }
            }
        }
        if (defaultTenantId != null && defaultTenantId > 0) {
            try {
                List<Long> allowedTenantIds = tenantCommonApi.getAllowedTenantIds(defaultTenantId).getCheckedData();
                if (allowedTenantIds != null && !allowedTenantIds.isEmpty()) {
                    for (Long tenantId : allowedTenantIds) {
                        if (tenantId != null && tenantId > 0) {
                            tenantIds.add(tenantId);
                        }
                    }
                }
            } catch (Exception ex) {
                log.warn("Resolve allowed tenant ids failed, tenantId={}, reason={}",
                        defaultTenantId, resolveErrorMessage(ex));
            }
        }
        if (tenantIds.isEmpty()) {
            tenantIds.add(resolveTargetTenantId(null));
        }
        return tenantIds;
    }

    private Map<String, Long> buildTopDeptTenantMap(Set<Long> tenantIds) {
        Map<String, Long> topDeptTenantMap = new HashMap<>();
        if (tenantIds == null || tenantIds.isEmpty()) {
            return topDeptTenantMap;
        }
        String tenantIdsParam = joinTenantIds(tenantIds);
        if (!StringUtils.hasText(tenantIdsParam)) {
            return topDeptTenantMap;
        }
        List<DeptRespDTO> deptList;
        try {
            deptList = deptApi.getDeptListByTenants(tenantIdsParam).getCheckedData();
        } catch (Exception ex) {
            log.warn("List dept by tenants failed, tenantIds={}, reason={}", tenantIdsParam, resolveErrorMessage(ex));
            return topDeptTenantMap;
        }
        if (deptList == null || deptList.isEmpty()) {
            return topDeptTenantMap;
        }

        Map<Long, List<DeptRespDTO>> deptByTenant = new HashMap<>();
        for (DeptRespDTO dept : deptList) {
            if (dept == null || dept.getId() == null || dept.getTenantId() == null) {
                continue;
            }
            if (!tenantIds.contains(dept.getTenantId())) {
                continue;
            }
            deptByTenant.computeIfAbsent(dept.getTenantId(), key -> new ArrayList<>()).add(dept);
        }

        for (Map.Entry<Long, List<DeptRespDTO>> entry : deptByTenant.entrySet()) {
            Long tenantId = entry.getKey();
            List<DeptRespDTO> tenantDeptList = entry.getValue();
            Map<Long, DeptRespDTO> deptMap = new HashMap<>();
            for (DeptRespDTO dept : tenantDeptList) {
                if (dept != null && dept.getId() != null) {
                    deptMap.put(dept.getId(), dept);
                }
            }
            for (DeptRespDTO dept : tenantDeptList) {
                String topDeptName = resolveTopDeptName(dept, deptMap);
                String topDeptKey = normalizeDeptName(topDeptName);
                if (!StringUtils.hasText(topDeptKey)) {
                    continue;
                }
                topDeptTenantMap.putIfAbsent(topDeptKey, tenantId);
            }
        }
        return topDeptTenantMap;
    }

    private String resolveTopDeptName(DeptRespDTO dept, Map<Long, DeptRespDTO> deptMap) {
        if (dept == null) {
            return null;
        }
        DeptRespDTO cursor = dept;
        Set<Long> visited = new HashSet<>();
        while (cursor != null && cursor.getParentId() != null && cursor.getParentId() > 0) {
            if (cursor.getId() != null && !visited.add(cursor.getId())) {
                break;
            }
            DeptRespDTO parent = deptMap.get(cursor.getParentId());
            if (parent == null) {
                break;
            }
            cursor = parent;
        }
        return cursor == null ? null : cursor.getName();
    }

    private String joinTenantIds(Set<Long> tenantIds) {
        if (tenantIds == null || tenantIds.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (Long tenantId : tenantIds) {
            if (tenantId == null || tenantId <= 0) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(tenantId);
        }
        return builder.length() == 0 ? null : builder.toString();
    }

    private Long resolveCreateTenantId(DingTalkUserSnapshot user, DeptRouteContext routeContext) {
        if (routeContext == null) {
            return resolveTargetTenantId(null);
        }
        String topDeptName = normalizeDeptName(user == null ? null : user.getTopDeptName());
        if (!StringUtils.hasText(topDeptName)) {
            topDeptName = normalizeDeptName(user == null ? null : user.getDeptName());
        }
        if (StringUtils.hasText(topDeptName)) {
            Long matchedTenantId = routeContext.getTopDeptTenantMap().get(topDeptName);
            if (matchedTenantId != null) {
                return resolveTargetTenantId(matchedTenantId);
            }
        }
        return resolveTargetTenantId(routeContext.getDefaultTenantId());
    }

    private Long resolveTargetTenantId(Long tenantId) {
        if (tenantId != null && tenantId > 0) {
            return tenantId;
        }
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (currentTenantId != null && currentTenantId > 0) {
            return currentTenantId;
        }
        Set<Long> tenantIdList = TenantContextHolder.getTenantIdList();
        if (tenantIdList != null && !tenantIdList.isEmpty()) {
            if (tenantIdList.contains(DEFAULT_DINGTALK_MASTER_TENANT_ID)) {
                return DEFAULT_DINGTALK_MASTER_TENANT_ID;
            }
            for (Long id : tenantIdList) {
                if (id != null && id > 0) {
                    return id;
                }
            }
        }
        return DEFAULT_DINGTALK_MASTER_TENANT_ID;
    }

    private List<DeptRespDTO> dedupeDeptById(List<DeptRespDTO> input) {
        Map<Long, DeptRespDTO> map = new HashMap<>();
        for (DeptRespDTO dept : input) {
            if (dept == null || dept.getId() == null) {
                continue;
            }
            map.putIfAbsent(dept.getId(), dept);
        }
        return new ArrayList<>(map.values());
    }

    private boolean isDeptValid(Long deptId) {
        if (deptId == null || deptId <= 0) {
            return false;
        }
        try {
            Boolean valid = deptApi.validateDeptList(Collections.singleton(deptId)).getCheckedData();
            return Boolean.TRUE.equals(valid);
        } catch (Exception ex) {
            return false;
        }
    }

    private Long pickFallbackDeptId(List<DeptRespDTO> deptList) {
        if (deptList != null && !deptList.isEmpty()) {
            for (DeptRespDTO dept : deptList) {
                if (dept != null && dept.getId() != null && dept.getId() > 0
                        && !Objects.equals(dept.getId(), DEFAULT_DEPT_ROOT_ID)) {
                    return dept.getId();
                }
            }
        }
        return DEFAULT_DEPT_ROOT_ID;
    }

    private Long pickConfiguredFallbackDeptId(DeptMatchContext context) {
        if (context == null) {
            return null;
        }
        String fallbackName = trim(dingTalkProperties.getSync() == null
                ? null : dingTalkProperties.getSync().getEmployeeFallbackDeptName());
        if (!StringUtils.hasText(fallbackName)) {
            return null;
        }
        String normalizedFallbackName = normalizeDeptName(fallbackName);
        Long exactFallback = context.getExactNameDeptMap().get(normalizedFallbackName);
        if (exactFallback != null && isDeptValid(exactFallback)) {
            return exactFallback;
        }
        Long fuzzyFallback = findFuzzyDeptId(normalizedFallbackName, context.getDeptCandidates());
        if (fuzzyFallback != null && isDeptValid(fuzzyFallback)) {
            return fuzzyFallback;
        }
        DeptRespDTO existed = findDeptByName(fallbackName);
        if (existed != null && existed.getId() != null && isDeptValid(existed.getId())) {
            registerDeptCandidate(context, existed.getId(), existed.getName(), TenantContextHolder.getTenantId());
            return existed.getId();
        }
        return null;
    }

    private ResolvedDept resolveDeptForCreate(DingTalkUserSnapshot user, DeptRouteContext routeContext) {
        Long targetTenantId = resolveCreateTenantId(user, routeContext);
        if (routeContext == null) {
            return buildResolvedDept(null, targetTenantId, "UNMATCHED_NO_CONTEXT");
        }

        DeptMatchContext tenantContext = getOrBuildDeptMatchContext(routeContext, targetTenantId);
        if (tenantContext == null) {
            return buildResolvedDept(null, targetTenantId, "UNMATCHED_NO_CONTEXT");
        }
        return TenantUtils.execute(targetTenantId,
                () -> resolveDeptForCreateInCurrentTenant(user, tenantContext, targetTenantId));
    }

    private ResolvedDept resolveDeptForExisting(DingTalkUserSnapshot user,
                                                DeptRouteContext routeContext,
                                                Long profileTenantId) {
        Long targetTenantId = resolveTargetTenantId(profileTenantId);
        if (routeContext == null) {
            return buildResolvedDept(null, targetTenantId, "UNMATCHED_NO_CONTEXT");
        }
        DeptMatchContext tenantContext = getOrBuildDeptMatchContext(routeContext, targetTenantId);
        if (tenantContext == null) {
            return buildResolvedDept(null, targetTenantId, "UNMATCHED_NO_CONTEXT");
        }
        return TenantUtils.execute(targetTenantId,
                () -> resolveDeptForCreateInCurrentTenant(user, tenantContext, targetTenantId));
    }

    private ResolvedDept resolveDeptForCreateInCurrentTenant(DingTalkUserSnapshot user,
                                                             DeptMatchContext context,
                                                             Long targetTenantId) {
        String sourceDeptName = trim(user == null ? null : user.getDeptName());
        Long dingDeptId = user == null ? null : user.getDeptId();
        if (dingDeptId != null && dingDeptId > 0 && isDeptValid(dingDeptId)) {
            registerDeptCandidate(context, dingDeptId, sourceDeptName, targetTenantId);
            return buildResolvedDept(dingDeptId, targetTenantId, "DING_DEPT_ID");
        }

        String dingDeptName = normalizeDeptName(sourceDeptName);
        if (!StringUtils.hasText(dingDeptName)) {
            return buildResolvedDept(context.getDefaultDeptId(), targetTenantId, "FALLBACK_NO_DING_DEPT");
        }

        Long exactMatch = context.getExactNameDeptMap().get(dingDeptName);
        if (exactMatch != null) {
            return buildResolvedDept(exactMatch, targetTenantId, "EXACT");
        }

        Long fuzzyMatch = findFuzzyDeptId(dingDeptName, context.getDeptCandidates());
        if (fuzzyMatch != null) {
            return buildResolvedDept(fuzzyMatch, targetTenantId, "FUZZY");
        }

        ResolvedDept resolvedByApi = findExistingDeptByName(sourceDeptName, dingDeptName, context, targetTenantId);
        if (resolvedByApi != null) {
            return resolvedByApi;
        }

        return buildResolvedDept(context.getDefaultDeptId(), targetTenantId, "FALLBACK_UNMATCHED_DEPT");
    }

    private ResolvedDept findExistingDeptByName(String sourceDeptName, String normalizedDeptName,
                                                DeptMatchContext context, Long targetTenantId) {
        DeptRespDTO existed = findDeptByName(sourceDeptName);
        if ((existed == null || existed.getId() == null)
                && StringUtils.hasText(normalizedDeptName)
                && !Objects.equals(trim(sourceDeptName), normalizedDeptName)) {
            existed = findDeptByName(normalizedDeptName);
        }
        if (existed != null && existed.getId() != null) {
            registerDeptCandidate(context, existed.getId(), existed.getName(), targetTenantId);
            return buildResolvedDept(existed.getId(), targetTenantId, "EXACT_BY_NAME");
        }
        return null;
    }

    private DeptRespDTO findDeptByName(String deptName) {
        String normalized = trim(deptName);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        try {
            return deptApi.getDeptByName(normalized).getCheckedData();
        } catch (Exception ex) {
            log.warn("Query dept by name failed, deptName={}, reason={}", normalized, resolveErrorMessage(ex));
            return null;
        }
    }

    private void registerDeptCandidate(DeptMatchContext context, Long deptId, String deptName, Long tenantId) {
        if (context == null || deptId == null || deptId <= 0) {
            return;
        }
        List<DeptRespDTO> candidates = context.getDeptCandidates();
        if (candidates == null) {
            candidates = new ArrayList<>();
            context.setDeptCandidates(candidates);
        }
        boolean existed = false;
        for (DeptRespDTO candidate : candidates) {
            if (candidate != null && Objects.equals(candidate.getId(), deptId)) {
                existed = true;
                break;
            }
        }
        if (!existed) {
            DeptRespDTO dept = new DeptRespDTO();
            dept.setId(deptId);
            dept.setName(deptName);
            dept.setTenantId(tenantId);
            candidates.add(dept);
        }
        String key = normalizeDeptName(deptName);
        if (StringUtils.hasText(key)) {
            context.getExactNameDeptMap().putIfAbsent(key, deptId);
        }
    }

    private ResolvedDept buildResolvedDept(Long deptId, Long tenantId, String matchType) {
        ResolvedDept resolved = new ResolvedDept();
        resolved.setDeptId(deptId);
        resolved.setTenantId(resolveTargetTenantId(tenantId));
        resolved.setMatchType(matchType);
        return resolved;
    }

    private boolean isResolvedCreateDept(ResolvedDept resolvedDept) {
        return resolvedDept != null && isDeptValid(resolvedDept.getDeptId());
    }

    private String buildDeptUnmatchedReason(DingTalkUserSnapshot user, ResolvedDept resolvedDept) {
        String sourceDeptName = trim(user == null ? null : user.getDeptName());
        String topDeptName = trim(user == null ? null : user.getTopDeptName());
        String matchType = resolvedDept == null ? null : resolvedDept.getMatchType();
        List<String> reasons = new ArrayList<>();
        if (StringUtils.hasText(sourceDeptName)) {
            reasons.add("dingDept=" + sourceDeptName);
        }
        if (StringUtils.hasText(topDeptName) && !Objects.equals(topDeptName, sourceDeptName)) {
            reasons.add("topDept=" + topDeptName);
        }
        reasons.add("deptMatch=" + (StringUtils.hasText(matchType) ? matchType : "UNMATCHED"));
        reasons.add("skip create; dept auto-create disabled");
        return String.join(",", reasons);
    }

    private String buildDeptSyncReason(ResolvedDept resolvedDept) {
        if (resolvedDept == null) {
            return null;
        }
        List<String> reasons = new ArrayList<>();
        if (resolvedDept.getDeptId() != null) {
            reasons.add("deptId=" + resolvedDept.getDeptId());
        }
        if (StringUtils.hasText(resolvedDept.getMatchType())) {
            reasons.add("deptMatch=" + resolvedDept.getMatchType());
        }
        return String.join(",", reasons);
    }

    private Long findFuzzyDeptId(String dingDeptName, List<DeptRespDTO> deptList) {
        if (!StringUtils.hasText(dingDeptName) || deptList == null || deptList.isEmpty()) {
            return null;
        }
        Long bestId = null;
        int bestScore = 0;
        for (DeptRespDTO dept : deptList) {
            if (dept == null || dept.getId() == null) {
                continue;
            }
            String oaDeptName = normalizeDeptName(dept.getName());
            if (!StringUtils.hasText(oaDeptName)) {
                continue;
            }
            int score = calculateDeptMatchScore(dingDeptName, oaDeptName);
            if (score > bestScore) {
                bestScore = score;
                bestId = dept.getId();
            }
        }
        return bestId;
    }

    private int calculateDeptMatchScore(String dingDeptName, String oaDeptName) {
        if (!StringUtils.hasText(dingDeptName) || !StringUtils.hasText(oaDeptName)) {
            return 0;
        }
        if (Objects.equals(dingDeptName, oaDeptName)) {
            return 2000;
        }
        if (dingDeptName.contains(oaDeptName) || oaDeptName.contains(dingDeptName)) {
            int score = Math.min(dingDeptName.length(), oaDeptName.length());
            return score >= 2 ? 1000 + score : 0;
        }
        int maxLength = Math.max(dingDeptName.length(), oaDeptName.length());
        if (maxLength < 3) {
            return 0;
        }
        int commonLength = longestCommonSubsequenceLength(dingDeptName, oaDeptName);
        int ratio = commonLength * 100 / maxLength;
        return commonLength >= 2 && ratio >= 70 ? ratio : 0;
    }

    private int longestCommonSubsequenceLength(String left, String right) {
        if (!StringUtils.hasText(left) || !StringUtils.hasText(right)) {
            return 0;
        }
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int i = 1; i <= left.length(); i++) {
            for (int j = 1; j <= right.length(); j++) {
                if (left.charAt(i - 1) == right.charAt(j - 1)) {
                    current[j] = previous[j - 1] + 1;
                } else {
                    current[j] = Math.max(previous[j], current[j - 1]);
                }
            }
            int[] tmp = previous;
            previous = current;
            current = tmp;
        }
        return previous[right.length()];
    }

    private int persistLocalBindings(Map<String, BindingCandidate> bindingCandidates) {
        if (bindingCandidates == null || bindingCandidates.isEmpty()) {
            return 0;
        }
        Map<Long, Map<String, BindingCandidate>> groupedByTenant = new HashMap<>();
        for (Map.Entry<String, BindingCandidate> entry : bindingCandidates.entrySet()) {
            String dingUserId = normalizeDingUserId(entry.getKey());
            BindingCandidate candidate = entry.getValue();
            if (!StringUtils.hasText(dingUserId) || candidate == null) {
                continue;
            }
            Long tenantId = resolveTargetTenantId(candidate.tenantId);
            groupedByTenant.computeIfAbsent(tenantId, key -> new HashMap<>()).put(dingUserId, candidate);
        }
        int total = 0;
        for (Map.Entry<Long, Map<String, BindingCandidate>> entry : groupedByTenant.entrySet()) {
            Long tenantId = entry.getKey();
            Map<String, BindingCandidate> tenantCandidates = entry.getValue();
            if (tenantCandidates == null || tenantCandidates.isEmpty()) {
                continue;
            }
            total += TenantUtils.execute(tenantId, () -> persistLocalBindingsInCurrentTenant(tenantCandidates));
        }
        return total;
    }

    private int persistLocalBindingsInCurrentTenant(Map<String, BindingCandidate> bindingCandidates) {
        if (bindingCandidates == null || bindingCandidates.isEmpty()) {
            return 0;
        }
        List<DingTalkUserBindingDO> existedRows = dingTalkUserBindingMapper.selectListByDingUserIds(bindingCandidates.keySet());
        Map<String, DingTalkUserBindingDO> existedByDingUserId = new HashMap<>();
        for (DingTalkUserBindingDO row : existedRows) {
            String dingUserId = normalizeDingUserId(row == null ? null : row.getDingUserId());
            if (StringUtils.hasText(dingUserId)) {
                existedByDingUserId.put(dingUserId, row);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        List<DingTalkUserBindingDO> inserts = new ArrayList<>();
        List<DingTalkUserBindingDO> updates = new ArrayList<>();
        for (Map.Entry<String, BindingCandidate> entry : bindingCandidates.entrySet()) {
            String dingUserId = normalizeDingUserId(entry.getKey());
            BindingCandidate candidate = entry.getValue();
            if (!StringUtils.hasText(dingUserId) || candidate == null || candidate.profile == null
                    || candidate.profile.getUserId() == null) {
                continue;
            }
            DingTalkUserBindingDO entity = existedByDingUserId.get(dingUserId);
            if (entity == null) {
                entity = new DingTalkUserBindingDO();
                entity.setDingUserId(dingUserId);
            }

            entity.setOaUserId(candidate.profile.getUserId());
            entity.setProfileId(candidate.profile.getId());
            entity.setDingUserName(trim(candidate.user == null ? null : candidate.user.getName()));
            entity.setDingMobile(normalizeMobile(candidate.user == null ? null : candidate.user.getMobile()));
            entity.setDingEmail(trim(candidate.user == null ? null : candidate.user.getEmail()));
            entity.setDingDeptId(candidate.user == null ? null : candidate.user.getDeptId());
            entity.setDingActive(candidate.user == null ? null : candidate.user.getActive());
            entity.setMatchType(candidate.matchType);
            entity.setSourceType(candidate.sourceType);
            entity.setLastSeenTime(now);
            entity.setSyncTime(now);
            entity.setRawPayload(candidate.user == null ? null : candidate.user.getRawPayload());

            if (entity.getId() == null) {
                inserts.add(entity);
            } else {
                updates.add(entity);
            }
        }

        if (!inserts.isEmpty()) {
            dingTalkUserBindingMapper.insertBatch(inserts);
        }
        if (!updates.isEmpty()) {
            dingTalkUserBindingMapper.updateBatch(updates);
        }
        return inserts.size() + updates.size();
    }

    private void previewSyncCleanups(List<DingTalkUserSnapshot> users, EmployeeSyncPreview preview) {
        if (users == null || users.isEmpty() || preview == null) {
            return;
        }
        Set<String> currentDingUserIds = new HashSet<>();
        Set<String> currentMobiles = new HashSet<>();
        Set<String> currentNames = new HashSet<>();
        for (DingTalkUserSnapshot user : users) {
            String dingUserId = normalizeDingUserId(user == null ? null : user.getUserId());
            if (StringUtils.hasText(dingUserId)) {
                currentDingUserIds.add(dingUserId);
            }
            String mobile = normalizeMobile(user == null ? null : user.getMobile());
            if (StringUtils.hasText(mobile)) {
                currentMobiles.add(mobile);
            }
            String name = normalizeName(user == null ? null : user.getName());
            if (StringUtils.hasText(name)) {
                currentNames.add(name);
            }
        }

        PreviewCleanupContext context = new PreviewCleanupContext(preview);
        if (!currentDingUserIds.isEmpty()) {
            Long defaultTenantId = resolveTargetTenantId(null);
            Set<Long> tenantIds = new LinkedHashSet<>(resolveRouteTenantIds(defaultTenantId));
            for (Long tenantId : tenantIds) {
                if (tenantId == null || tenantId <= 0) {
                    continue;
                }
                TenantUtils.execute(tenantId, () -> {
                    previewCleanupMissingBindingsInCurrentTenant(currentDingUserIds, context);
                    return null;
                });
            }
        }

        if (currentMobiles.isEmpty() && currentNames.isEmpty()) {
            return;
        }
        Long tenantId = resolveTargetTenantId(null);
        if (tenantId != null && tenantId > 0) {
            TenantUtils.execute(tenantId, () -> {
                previewCleanupUnboundMissingProfilesInCurrentTenant(currentMobiles, currentNames, context);
                return null;
            });
        }
    }

    private void previewCleanupMissingBindingsInCurrentTenant(Set<String> currentDingUserIds,
                                                              PreviewCleanupContext context) {
        if (currentDingUserIds == null || currentDingUserIds.isEmpty() || context == null) {
            return;
        }
        List<DingTalkUserBindingDO> existedRows = dingTalkUserBindingMapper.selectListAll();
        if (existedRows == null || existedRows.isEmpty()) {
            return;
        }

        Map<Long, DingTalkUserBindingDO> staleBindingByUserId = new HashMap<>();
        for (DingTalkUserBindingDO row : existedRows) {
            String dingUserId = normalizeDingUserId(row == null ? null : row.getDingUserId());
            if (!StringUtils.hasText(dingUserId) || currentDingUserIds.contains(dingUserId)) {
                continue;
            }
            if (row.getId() != null && !Boolean.FALSE.equals(row.getDingActive())
                    && context.countedBindingIds.add(row.getId())) {
                context.preview.increaseToMarkBindingsInactive();
            }
            if (row.getOaUserId() != null) {
                staleBindingByUserId.putIfAbsent(row.getOaUserId(), row);
            }
            context.preview.addSyncDetail(dingUserId, trim(row.getDingUserName()), normalizeMobile(row.getDingMobile()),
                    trim(row.getDingEmail()), "BINDING", "TO_MARK_BINDING_MISSING_INACTIVE",
                    row.getOaUserId() == null ? null : "oaUserId=" + row.getOaUserId());
        }

        if (staleBindingByUserId.isEmpty()) {
            return;
        }
        previewAdminUserDisables(staleBindingByUserId.keySet(), context);
        for (Map.Entry<Long, DingTalkUserBindingDO> entry : staleBindingByUserId.entrySet()) {
            Long userId = entry.getKey();
            DingTalkUserBindingDO binding = entry.getValue();
            if (userId == null || binding == null) {
                continue;
            }
            EmployeeProfileDO profile = binding.getProfileId() == null
                    ? employeeProfileMapper.selectByUserId(userId)
                    : employeeProfileMapper.selectById(binding.getProfileId());
            if (profile != null && !Objects.equals(profile.getStatus(), PROFILE_STATUS_DISABLED)
                    && profile.getId() != null && context.countedProfileIds.add(profile.getId())) {
                context.preview.increaseToDisableProfiles();
            }
            previewEntryResignations(userId, context);
        }
    }

    private void previewCleanupUnboundMissingProfilesInCurrentTenant(Set<String> currentMobiles,
                                                                     Set<String> currentNames,
                                                                     PreviewCleanupContext context) {
        if (context == null) {
            return;
        }
        List<EmployeeProfileDO> activeProfiles = employeeProfileMapper.selectList(new LambdaQueryWrapperX<EmployeeProfileDO>()
                .eq(EmployeeProfileDO::getStatus, PROFILE_STATUS_ENABLED));
        if (activeProfiles == null || activeProfiles.isEmpty()) {
            return;
        }

        List<DingTalkUserBindingDO> bindingRows = dingTalkUserBindingMapper.selectListAll();
        Set<Long> boundProfileIds = new HashSet<>();
        Set<Long> boundUserIds = new HashSet<>();
        if (bindingRows != null) {
            for (DingTalkUserBindingDO binding : bindingRows) {
                if (binding == null) {
                    continue;
                }
                if (binding.getProfileId() != null) {
                    boundProfileIds.add(binding.getProfileId());
                }
                if (binding.getOaUserId() != null) {
                    boundUserIds.add(binding.getOaUserId());
                }
            }
        }

        Set<Long> userIdsToDisable = new HashSet<>();
        for (EmployeeProfileDO profile : activeProfiles) {
            if (profile == null || profile.getId() == null) {
                continue;
            }
            if (boundProfileIds.contains(profile.getId())
                    || (profile.getUserId() != null && boundUserIds.contains(profile.getUserId()))) {
                continue;
            }
            String mobile = normalizeMobile(profile.getMobile());
            String name = normalizeName(profile.getName());
            if (StringUtils.hasText(mobile) && currentMobiles.contains(mobile)) {
                continue;
            }
            if (StringUtils.hasText(name) && currentNames.contains(name)) {
                continue;
            }
            if (!StringUtils.hasText(mobile) && !StringUtils.hasText(name)) {
                continue;
            }

            if (context.countedProfileIds.add(profile.getId())) {
                context.preview.increaseToDisableProfiles();
            }
            if (profile.getUserId() != null) {
                userIdsToDisable.add(profile.getUserId());
                previewEntryResignations(profile.getUserId(), context);
            }
            context.preview.addSyncDetail(null, trim(profile.getName()), mobile, trim(profile.getEmail()),
                    "LOCAL_PROFILE", "TO_MARK_UNBOUND_MISSING_INACTIVE",
                    "profileId=" + profile.getId()
                            + (profile.getUserId() == null ? "" : ",oaUserId=" + profile.getUserId()));
        }

        previewAdminUserDisables(userIdsToDisable, context);
    }

    private void previewEntryResignations(Long userId, PreviewCleanupContext context) {
        if (userId == null || context == null) {
            return;
        }
        List<EmployeeEntryDO> entries = employeeEntryMapper.selectListByUserId(userId);
        if (entries == null || entries.isEmpty()) {
            return;
        }
        for (EmployeeEntryDO entry : entries) {
            if (needsEntryWorkStatusSync(entry, WORK_STATUS_RESIGNED)
                    && entry.getId() != null && context.countedEntryIds.add(entry.getId())) {
                context.preview.increaseToMarkEntriesResigned();
            }
        }
    }

    private void previewAdminUserDisables(Set<Long> userIds, PreviewCleanupContext context) {
        if (userIds == null || userIds.isEmpty() || context == null) {
            return;
        }
        Map<Long, AdminUserRespDTO> adminUserMap = safeGetAdminUserMap(userIds);
        for (Long userId : userIds) {
            AdminUserRespDTO adminUser = adminUserMap.get(userId);
            if (adminUser != null && !Objects.equals(adminUser.getStatus(), ADMIN_USER_STATUS_DISABLED)
                    && context.countedAdminUserIds.add(userId)) {
                context.preview.increaseToDisableAdminUsers();
            }
        }
    }

    private SyncCleanupResult applySyncCleanups(List<DingTalkUserSnapshot> users,
                                                Map<String, BindingCandidate> bindingCandidates,
                                                EmployeeSyncReport report) {
        SyncCleanupResult result = new SyncCleanupResult();
        result.merge(syncBindingCandidateStates(bindingCandidates, report));
        result.merge(cleanupMissingBindings(users, bindingCandidates, report));
        result.merge(cleanupUnboundMissingProfiles(users, report));
        return result;
    }

    private SyncCleanupResult syncBindingCandidateStates(Map<String, BindingCandidate> bindingCandidates,
                                                        EmployeeSyncReport report) {
        SyncCleanupResult result = new SyncCleanupResult();
        if (bindingCandidates == null || bindingCandidates.isEmpty()) {
            return result;
        }
        Map<Long, List<BindingCandidate>> groupedByTenant = new HashMap<>();
        for (BindingCandidate candidate : bindingCandidates.values()) {
            if (candidate == null || candidate.profile == null || candidate.profile.getUserId() == null
                    || candidate.user == null) {
                continue;
            }
            Long tenantId = resolveTargetTenantId(candidate.tenantId);
            groupedByTenant.computeIfAbsent(tenantId, key -> new ArrayList<>()).add(candidate);
        }
        for (Map.Entry<Long, List<BindingCandidate>> entry : groupedByTenant.entrySet()) {
            Long tenantId = entry.getKey();
            List<BindingCandidate> tenantCandidates = entry.getValue();
            if (tenantId == null || tenantCandidates == null || tenantCandidates.isEmpty()) {
                continue;
            }
            result.merge(TenantUtils.execute(tenantId,
                    () -> syncBindingCandidateStatesInCurrentTenant(tenantCandidates, report)));
        }
        return result;
    }

    private SyncCleanupResult syncBindingCandidateStatesInCurrentTenant(List<BindingCandidate> bindingCandidates,
                                                                       EmployeeSyncReport report) {
        SyncCleanupResult result = new SyncCleanupResult();
        if (bindingCandidates == null || bindingCandidates.isEmpty()) {
            return result;
        }
        Set<Long> userIds = new HashSet<>();
        for (BindingCandidate candidate : bindingCandidates) {
            if (candidate != null && candidate.profile != null && candidate.profile.getUserId() != null) {
                userIds.add(candidate.profile.getUserId());
            }
        }
        Map<Long, AdminUserRespDTO> adminUserMap = safeGetAdminUserMap(userIds);
        for (BindingCandidate candidate : bindingCandidates) {
            if (candidate == null || candidate.profile == null || candidate.profile.getUserId() == null
                    || candidate.user == null) {
                continue;
            }
            Long userId = candidate.profile.getUserId();
            boolean active = Boolean.TRUE.equals(candidate.user.getActive());

            Integer targetWorkStatus = active ? WORK_STATUS_ACTIVE : WORK_STATUS_RESIGNED;
            int syncedEntries = syncEmployeeEntryWorkStatus(report, userId, targetWorkStatus,
                    "ENTRY_STATUS_SYNC", "active=" + active);
            for (int i = 0; i < syncedEntries; i++) {
                result.increaseSyncedEntryStatuses();
            }

            AdminUserRespDTO adminUser = adminUserMap.get(userId);
            Integer targetAdminStatus = active ? ADMIN_USER_STATUS_ENABLED : ADMIN_USER_STATUS_DISABLED;
            if (adminUser != null && !Objects.equals(adminUser.getStatus(), targetAdminStatus)
                    && recordAndUpdateAdminUserStatus(report, userId, targetAdminStatus,
                    "ADMIN_USER_STATUS_SYNC", "active=" + active, adminUser)) {
                if (Objects.equals(targetAdminStatus, ADMIN_USER_STATUS_ENABLED)) {
                    result.increaseEnabledAdminUsers();
                } else {
                    result.increaseDisabledAdminUsers();
                }
            }
        }
        return result;
    }

    private SyncCleanupResult cleanupUnboundMissingProfiles(List<DingTalkUserSnapshot> users,
                                                            EmployeeSyncReport report) {
        SyncCleanupResult result = new SyncCleanupResult();
        if (users == null || users.isEmpty()) {
            return result;
        }
        Set<String> currentMobiles = new HashSet<>();
        Set<String> currentNames = new HashSet<>();
        for (DingTalkUserSnapshot user : users) {
            String mobile = normalizeMobile(user == null ? null : user.getMobile());
            if (StringUtils.hasText(mobile)) {
                currentMobiles.add(mobile);
            }
            String name = normalizeName(user == null ? null : user.getName());
            if (StringUtils.hasText(name)) {
                currentNames.add(name);
            }
        }
        if (currentMobiles.isEmpty() && currentNames.isEmpty()) {
            return result;
        }

        Long tenantId = resolveTargetTenantId(null);
        if (tenantId != null && tenantId > 0) {
            result.merge(TenantUtils.execute(tenantId,
                    () -> cleanupUnboundMissingProfilesInCurrentTenant(currentMobiles, currentNames, report)));
        }
        return result;
    }

    private SyncCleanupResult cleanupUnboundMissingProfilesInCurrentTenant(Set<String> currentMobiles,
                                                                           Set<String> currentNames,
                                                                           EmployeeSyncReport report) {
        SyncCleanupResult result = new SyncCleanupResult();
        List<EmployeeProfileDO> activeProfiles = employeeProfileMapper.selectList(new LambdaQueryWrapperX<EmployeeProfileDO>()
                .eq(EmployeeProfileDO::getStatus, PROFILE_STATUS_ENABLED));
        if (activeProfiles == null || activeProfiles.isEmpty()) {
            return result;
        }

        List<DingTalkUserBindingDO> bindingRows = dingTalkUserBindingMapper.selectListAll();
        Set<Long> boundProfileIds = new HashSet<>();
        Set<Long> boundUserIds = new HashSet<>();
        if (bindingRows != null) {
            for (DingTalkUserBindingDO binding : bindingRows) {
                if (binding == null) {
                    continue;
                }
                if (binding.getProfileId() != null) {
                    boundProfileIds.add(binding.getProfileId());
                }
                if (binding.getOaUserId() != null) {
                    boundUserIds.add(binding.getOaUserId());
                }
            }
        }

        Set<Long> userIdsToDisable = new HashSet<>();
        for (EmployeeProfileDO profile : activeProfiles) {
            if (profile == null || profile.getId() == null) {
                continue;
            }
            if (boundProfileIds.contains(profile.getId())
                    || (profile.getUserId() != null && boundUserIds.contains(profile.getUserId()))) {
                continue;
            }
            String mobile = normalizeMobile(profile.getMobile());
            String name = normalizeName(profile.getName());
            if (StringUtils.hasText(mobile) && currentMobiles.contains(mobile)) {
                continue;
            }
            if (StringUtils.hasText(name) && currentNames.contains(name)) {
                continue;
            }
            if (!StringUtils.hasText(mobile) && !StringUtils.hasText(name)) {
                continue;
            }

            recordEmployeeSnapshot(report, "UNBOUND_PROFILE_MISSING_IN_DINGTALK",
                    "profile missing in current DingTalk users", profile, null, null, null);
            EmployeeProfileDO updateProfile = new EmployeeProfileDO();
            updateProfile.setId(profile.getId());
            updateProfile.setStatus(PROFILE_STATUS_DISABLED);
            employeeProfileMapper.updateById(updateProfile);
            result.increaseCleanedProfiles();

            if (profile.getUserId() != null) {
                userIdsToDisable.add(profile.getUserId());
                int syncedEntries = syncEmployeeEntryWorkStatus(report, profile.getUserId(), WORK_STATUS_RESIGNED,
                        "UNBOUND_ENTRY_MARK_RESIGNED", "profile missing in current DingTalk users");
                for (int i = 0; i < syncedEntries; i++) {
                    result.increaseSyncedEntryStatuses();
                }
            }

            if (report != null) {
                report.addSyncDetail(null, trim(profile.getName()), mobile, trim(profile.getEmail()),
                        "LOCAL_PROFILE", "MARKED_UNBOUND_MISSING_INACTIVE",
                        "profileId=" + profile.getId()
                                + (profile.getUserId() == null ? "" : ",oaUserId=" + profile.getUserId()));
            }
        }

        if (!userIdsToDisable.isEmpty()) {
            Map<Long, AdminUserRespDTO> adminUserMap = safeGetAdminUserMap(userIdsToDisable);
            for (Long userId : userIdsToDisable) {
                AdminUserRespDTO adminUser = adminUserMap.get(userId);
                if (adminUser != null && !Objects.equals(adminUser.getStatus(), ADMIN_USER_STATUS_DISABLED)
                        && recordAndUpdateAdminUserStatus(report, userId, ADMIN_USER_STATUS_DISABLED,
                        "UNBOUND_ADMIN_USER_DISABLE", "profile missing in current DingTalk users", adminUser)) {
                    result.increaseDisabledAdminUsers();
                }
            }
        }
        return result;
    }

    private SyncCleanupResult cleanupMissingBindings(List<DingTalkUserSnapshot> users,
                                                     Map<String, BindingCandidate> bindingCandidates,
                                                     EmployeeSyncReport report) {
        SyncCleanupResult result = new SyncCleanupResult();
        if (users == null || users.isEmpty()) {
            return result;
        }
        Set<String> currentDingUserIds = new HashSet<>();
        for (DingTalkUserSnapshot user : users) {
            String dingUserId = normalizeDingUserId(user == null ? null : user.getUserId());
            if (StringUtils.hasText(dingUserId)) {
                currentDingUserIds.add(dingUserId);
            }
        }
        if (currentDingUserIds.isEmpty()) {
            return result;
        }

        Long defaultTenantId = resolveTargetTenantId(null);
        Set<Long> tenantIds = new LinkedHashSet<>(resolveRouteTenantIds(defaultTenantId));
        if (bindingCandidates != null) {
            for (BindingCandidate candidate : bindingCandidates.values()) {
                if (candidate == null) {
                    continue;
                }
                tenantIds.add(resolveTargetTenantId(candidate.tenantId));
            }
        }
        for (Long tenantId : tenantIds) {
            if (tenantId == null || tenantId <= 0) {
                continue;
            }
            result.merge(TenantUtils.execute(tenantId,
                    () -> cleanupMissingBindingsInCurrentTenant(currentDingUserIds, report)));
        }
        return result;
    }

    private SyncCleanupResult cleanupMissingBindingsInCurrentTenant(Set<String> currentDingUserIds,
                                                                    EmployeeSyncReport report) {
        SyncCleanupResult result = new SyncCleanupResult();
        if (currentDingUserIds == null || currentDingUserIds.isEmpty()) {
            return result;
        }
        List<DingTalkUserBindingDO> existedRows = dingTalkUserBindingMapper.selectListAll();
        if (existedRows == null || existedRows.isEmpty()) {
            return result;
        }

        LocalDateTime now = LocalDateTime.now();
        List<DingTalkUserBindingDO> bindingUpdates = new ArrayList<>();
        Set<String> staleExternalUserIds = new LinkedHashSet<>();
        Map<Long, DingTalkUserBindingDO> staleBindingByUserId = new HashMap<>();

        for (DingTalkUserBindingDO row : existedRows) {
            String dingUserId = normalizeDingUserId(row == null ? null : row.getDingUserId());
            if (!StringUtils.hasText(dingUserId) || currentDingUserIds.contains(dingUserId)) {
                continue;
            }
            staleExternalUserIds.add(dingUserId);
            if (row.getOaUserId() != null) {
                staleBindingByUserId.putIfAbsent(row.getOaUserId(), row);
            }
            if (!Boolean.FALSE.equals(row.getDingActive())) {
                recordEmployeeSnapshot(report, "BINDING_MISSING_IN_DINGTALK",
                        "binding missing in current DingTalk users", null, null, null, row);
                DingTalkUserBindingDO updateObj = new DingTalkUserBindingDO();
                updateObj.setId(row.getId());
                updateObj.setDingActive(Boolean.FALSE);
                updateObj.setSyncTime(now);
                bindingUpdates.add(updateObj);
                result.increaseCleanedBindings();
            }
            if (report != null) {
                report.addSyncDetail(dingUserId, trim(row.getDingUserName()), normalizeMobile(row.getDingMobile()),
                        trim(row.getDingEmail()), "BINDING", "MARKED_MISSING_INACTIVE",
                        row.getOaUserId() == null ? null : "oaUserId=" + row.getOaUserId());
            }
        }

        if (!bindingUpdates.isEmpty()) {
            dingTalkUserBindingMapper.updateBatch(bindingUpdates);
        }
        result.increaseCleanedUserSyncRows(safeMarkUserSyncInactive(staleExternalUserIds));

        if (staleBindingByUserId.isEmpty()) {
            return result;
        }
        Map<Long, AdminUserRespDTO> adminUserMap = safeGetAdminUserMap(staleBindingByUserId.keySet());
        for (Map.Entry<Long, DingTalkUserBindingDO> entry : staleBindingByUserId.entrySet()) {
            Long userId = entry.getKey();
            DingTalkUserBindingDO binding = entry.getValue();
            if (userId == null || binding == null) {
                continue;
            }
            EmployeeProfileDO profile = binding.getProfileId() == null
                    ? employeeProfileMapper.selectByUserId(userId)
                    : employeeProfileMapper.selectById(binding.getProfileId());
            if (profile != null && !Objects.equals(profile.getStatus(), PROFILE_STATUS_DISABLED)) {
                recordEmployeeSnapshot(report, "BOUND_PROFILE_MISSING_IN_DINGTALK",
                        "binding missing in current DingTalk users", profile, null, null, null);
                EmployeeProfileDO updateProfile = new EmployeeProfileDO();
                updateProfile.setId(profile.getId());
                updateProfile.setStatus(PROFILE_STATUS_DISABLED);
                employeeProfileMapper.updateById(updateProfile);
                result.increaseCleanedProfiles();
            }

            int syncedEntries = syncEmployeeEntryWorkStatus(report, userId, WORK_STATUS_RESIGNED,
                    "BOUND_ENTRY_MARK_RESIGNED", "binding missing in current DingTalk users");
            for (int i = 0; i < syncedEntries; i++) {
                result.increaseSyncedEntryStatuses();
            }

            AdminUserRespDTO adminUser = adminUserMap.get(userId);
            if (adminUser != null && !Objects.equals(adminUser.getStatus(), ADMIN_USER_STATUS_DISABLED)
                    && recordAndUpdateAdminUserStatus(report, userId, ADMIN_USER_STATUS_DISABLED,
                    "BOUND_ADMIN_USER_DISABLE", "binding missing in current DingTalk users", adminUser)) {
                result.increaseDisabledAdminUsers();
            }
        }
        return result;
    }

    private int syncEmployeeEntryWorkStatus(EmployeeSyncReport report,
                                            Long userId,
                                            Integer targetWorkStatus,
                                            String snapshotAction,
                                            String reason) {
        if (userId == null || targetWorkStatus == null) {
            return 0;
        }
        List<EmployeeEntryDO> entries = employeeEntryMapper.selectListByUserId(userId);
        if (entries == null || entries.isEmpty()) {
            return 0;
        }
        int updated = 0;
        for (EmployeeEntryDO entry : entries) {
            if (!needsEntryWorkStatusSync(entry, targetWorkStatus)) {
                continue;
            }
            recordEmployeeSnapshot(report, snapshotAction, reason, null, entry, null, null);
            EmployeeEntryDO updateEntry = new EmployeeEntryDO();
            updateEntry.setId(entry.getId());
            if (!Objects.equals(entry.getWorkStatus(), targetWorkStatus)) {
                updateEntry.setWorkStatus(targetWorkStatus);
            }
            if (Objects.equals(targetWorkStatus, WORK_STATUS_RESIGNED)) {
                if (entry.getLeaveDate() == null) {
                    updateEntry.setLeaveDate(LocalDate.now());
                }
                if (!StringUtils.hasText(entry.getLeaveReason())) {
                    updateEntry.setLeaveReason(resolveAutoLeaveReason(reason));
                }
            }
            employeeEntryMapper.updateById(updateEntry);
            updated++;
        }
        return updated;
    }

    private boolean needsEntryWorkStatusSync(EmployeeEntryDO entry, Integer targetWorkStatus) {
        if (entry == null || entry.getId() == null || targetWorkStatus == null) {
            return false;
        }
        if (!Objects.equals(entry.getWorkStatus(), targetWorkStatus)) {
            return true;
        }
        return Objects.equals(targetWorkStatus, WORK_STATUS_RESIGNED)
                && (entry.getLeaveDate() == null || !StringUtils.hasText(entry.getLeaveReason()));
    }

    private String resolveAutoLeaveReason(String reason) {
        String normalized = trim(reason);
        if (StringUtils.hasText(normalized) && normalized.contains("active=false")) {
            return "钉钉通讯录状态停用，自动标记离职";
        }
        if (StringUtils.hasText(normalized)
                && (normalized.contains("current DingTalk users") || normalized.contains("missing in"))) {
            return "钉钉通讯录缺失，自动标记离职";
        }
        return StringUtils.hasText(normalized) ? normalized : "钉钉同步自动标记离职";
    }

    private boolean recordAndUpdateAdminUserStatus(EmployeeSyncReport report,
                                                   Long userId,
                                                   Integer status,
                                                   String action,
                                                   String reason,
                                                   AdminUserRespDTO adminUser) {
        recordEmployeeSnapshot(report, action, reason, null, null, adminUser, null);
        return safeUpdateAdminUserStatus(userId, status);
    }

    private boolean recordAndUpdateAdminUserDept(EmployeeSyncReport report,
                                                 Long userId,
                                                 Long deptId,
                                                 String action,
                                                 String reason,
                                                 AdminUserRespDTO adminUser) {
        recordEmployeeSnapshot(report, action, reason, null, null, adminUser, null);
        return safeUpdateAdminUserDept(userId, deptId);
    }

    private void recordEmployeeSnapshot(EmployeeSyncReport report,
                                        String action,
                                        String reason,
                                        EmployeeProfileDO profile,
                                        EmployeeEntryDO entry,
                                        AdminUserRespDTO adminUser,
                                        DingTalkUserBindingDO binding) {
        if (report == null || !StringUtils.hasText(report.getSnapshotBatchId())) {
            return;
        }
        boolean recorded = dingTalkSyncSnapshotService.recordBeforeChange(report.getSnapshotBatchId(),
                "EMPLOYEE_SYNC", TenantContextHolder.getTenantId(), action, reason,
                profile, entry, adminUser, binding);
        if (recorded) {
            report.increaseSnapshotRows();
        }
    }

    private Map<Long, AdminUserRespDTO> safeGetAdminUserMap(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return adminUserApi.getUserMap(userIds);
        } catch (Exception ex) {
            log.warn("Load admin users for DingTalk cleanup failed, userIds={}, reason={}",
                    userIds, resolveErrorMessage(ex));
            return Collections.emptyMap();
        }
    }

    private boolean safeUpdateAdminUserStatus(Long userId, Integer status) {
        if (userId == null || status == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(adminUserApi.updateUserStatus(userId, status).getCheckedData());
        } catch (Exception ex) {
            log.warn("Update admin user status by DingTalk sync failed, userId={}, status={}, reason={}",
                    userId, status, resolveErrorMessage(ex));
            return false;
        }
    }

    private boolean safeUpdateAdminUserDept(Long userId, Long deptId) {
        if (userId == null || deptId == null || deptId <= 0) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(adminUserApi.updateUserDept(userId, deptId).getCheckedData());
        } catch (Exception ex) {
            log.warn("Update admin user dept by DingTalk sync failed, userId={}, deptId={}, reason={}",
                    userId, deptId, resolveErrorMessage(ex));
            return false;
        }
    }

    private boolean safeUpdateAdminUserMobile(Long userId, String mobile) {
        if (userId == null || !StringUtils.hasText(mobile)) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(adminUserApi.updateUserMobile(userId, mobile).getCheckedData());
        } catch (Exception ex) {
            log.warn("Update admin user mobile by DingTalk sync failed, userId={}, reason={}",
                    userId, resolveErrorMessage(ex));
            return false;
        }
    }

    private int safeMarkUserSyncInactive(Set<String> externalUserIds) {
        if (externalUserIds == null || externalUserIds.isEmpty()) {
            return 0;
        }
        try {
            Integer affected = adminUserApi.markUserSyncInactive(new ArrayList<>(externalUserIds)).getCheckedData();
            return affected == null ? 0 : affected;
        } catch (Exception ex) {
            log.warn("Mark user sync inactive by DingTalk cleanup failed, externalUserIds={}, reason={}",
                    externalUserIds, resolveErrorMessage(ex));
            return 0;
        }
    }

    private int safeMarkUserSyncSynced(Set<String> externalUserIds) {
        if (externalUserIds == null || externalUserIds.isEmpty()) {
            return 0;
        }
        try {
            Integer affected = adminUserApi.markUserSyncSynced(new ArrayList<>(externalUserIds)).getCheckedData();
            return affected == null ? 0 : affected;
        } catch (Exception ex) {
            log.warn("Mark user sync success by DingTalk sync failed, externalUserIds={}, reason={}",
                    externalUserIds, resolveErrorMessage(ex));
            return 0;
        }
    }

    private String normalizeDingUserId(String dingUserId) {
        return dingUserId == null ? null : dingUserId.trim();
    }

    private String resolveErrorMessage(Exception ex) {
        if (ex == null) {
            return null;
        }
        Throwable cause = ex;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = trim(cause.getMessage());
        if (!StringUtils.hasText(message)) {
            message = trim(ex.getMessage());
        }
        return StringUtils.hasText(message) ? message : ex.getClass().getSimpleName();
    }

    private String normalizeMobile(String mobile) {
        if (mobile == null) {
            return null;
        }
        String normalized = mobile.trim()
                .replaceAll("[\\s-]", "")
                .replace("(", "")
                .replace(")", "");
        if (normalized.startsWith("+86")) {
            normalized = normalized.substring(3);
        } else if (normalized.startsWith("86") && normalized.length() > 11) {
            normalized = normalized.substring(2);
        }
        return normalized;
    }

    private String normalizeName(String name) {
        return name == null ? null : name.trim().replaceAll("\\s+", "");
    }

    private String normalizeDeptName(String name) {
        String normalized = normalizeName(name);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if (normalized.endsWith("部门")) {
            normalized = normalized.substring(0, normalized.length() - 2);
        } else if (normalized.endsWith("中心") || normalized.endsWith("小组")
                || normalized.endsWith("团队")) {
            normalized = normalized.substring(0, normalized.length() - 2);
        } else if (normalized.endsWith("部")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    @Data
    private static class MatchResult {
        private final EmployeeProfileDO profile;
        private final boolean matchedByName;
    }

    @Data
    private static class AutoCreateResult {
        private final EmployeeProfileDO profile;
        private final boolean created;
        private final boolean invalid;
        private final String reason;

        static AutoCreateResult created(EmployeeProfileDO profile) {
            return new AutoCreateResult(profile, true, false, null);
        }

        static AutoCreateResult reused(EmployeeProfileDO profile) {
            return new AutoCreateResult(profile, false, false, null);
        }

        static AutoCreateResult invalid(String reason) {
            return new AutoCreateResult(null, false, true, reason);
        }

        static AutoCreateResult failed(String reason) {
            return new AutoCreateResult(null, false, false, reason);
        }
    }

    @Data
    private static class CreateIdentity {
        private String mobile;
        private String name;
        private String autoFillFields;
    }

    @Data
    private static class DeptMatchContext {
        private Long defaultDeptId;
        private Map<String, Long> exactNameDeptMap = new HashMap<>();
        private List<DeptRespDTO> deptCandidates = new ArrayList<>();
    }

    @Data
    private static class DeptRouteContext {
        private Long requestedDeptId;
        private Long defaultTenantId;
        private Long defaultDeptId;
        private Set<Long> routeTenantIds = new LinkedHashSet<>();
        private Map<String, Long> topDeptTenantMap = new HashMap<>();
        private Map<Long, DeptMatchContext> matchContextByTenant = new HashMap<>();
    }

    @Data
    private static class ResolvedDept {
        private Long deptId;
        private Long tenantId;
        private String matchType;
    }

    private static class BindingCandidate {
        private DingTalkUserSnapshot user;
        private EmployeeProfileDO profile;
        private String matchType;
        private String sourceType;
        private Long tenantId;

        static BindingCandidate of(DingTalkUserSnapshot user, EmployeeProfileDO profile,
                                   String matchType, String sourceType, Long tenantId) {
            BindingCandidate candidate = new BindingCandidate();
            candidate.user = user;
            candidate.profile = profile;
            candidate.matchType = matchType;
            candidate.sourceType = sourceType;
            candidate.tenantId = tenantId;
            return candidate;
        }
    }

    private static class PreviewCleanupContext {
        private final EmployeeSyncPreview preview;
        private final Set<Long> countedBindingIds = new HashSet<>();
        private final Set<Long> countedProfileIds = new HashSet<>();
        private final Set<Long> countedEntryIds = new HashSet<>();
        private final Set<Long> countedAdminUserIds = new HashSet<>();

        private PreviewCleanupContext(EmployeeSyncPreview preview) {
            this.preview = preview;
        }
    }

    @Data
    private static class SyncCleanupResult {
        private int syncedEntryStatuses;
        private int enabledAdminUsers;
        private int disabledAdminUsers;
        private int syncedDeptIds;
        private int cleanedProfiles;
        private int cleanedBindings;
        private int cleanedUserSyncRows;

        void increaseSyncedEntryStatuses() {
            this.syncedEntryStatuses++;
        }

        void increaseEnabledAdminUsers() {
            this.enabledAdminUsers++;
        }

        void increaseDisabledAdminUsers() {
            this.disabledAdminUsers++;
        }

        void increaseCleanedProfiles() {
            this.cleanedProfiles++;
        }

        void increaseCleanedBindings() {
            this.cleanedBindings++;
        }

        void increaseCleanedUserSyncRows(int count) {
            this.cleanedUserSyncRows += Math.max(count, 0);
        }

        void merge(SyncCleanupResult other) {
            if (other == null) {
                return;
            }
            this.syncedEntryStatuses += other.syncedEntryStatuses;
            this.enabledAdminUsers += other.enabledAdminUsers;
            this.disabledAdminUsers += other.disabledAdminUsers;
            this.cleanedProfiles += other.cleanedProfiles;
            this.cleanedBindings += other.cleanedBindings;
            this.cleanedUserSyncRows += other.cleanedUserSyncRows;
        }
    }

    @Data
    private static class RosterCustomFieldSpec {
        private final String fieldKey;
        private final String fieldName;
        private final String fieldType;
        private final String value;
        private final boolean sensitive;
        private final int sortOrder;
    }

    @Data
    public static class EmployeeSyncReport {

        private int totalFromDingTalk;
        private int matchedByMobile;
        private int matchedByName;
        private int updated;
        private int unchanged;
        private int created;
        private int createFailed;
        private int skippedCreateInvalid;
        private int skippedDuplicateMapping;
        private int unmatched;
        private int skippedNoMobile;
        private int usersWithMobile;
        private int usersWithName;
        private int usersWithNeitherIdentity;
        private int deptTreePulled;
        private int deptTreeSynced;
        private int deptTreeCreated;
        private int deptTreeUpdated;
        private int deptTreeFailed;
        private boolean autoCreateEnabled;
        private Long autoCreateDeptId;
        private int stagedToUserSync;
        private int skippedNoExternalUserId;
        private int syncedUserSyncRows;
        private int syncedBindings;
        private int syncedEntryStatuses;
        private int syncedDeptIds;
        private int syncedJobTitles;
        private int syncedJobNumbers;
        private int syncedDeptLeaders;
        private int syncedDirectSupervisors;
        private int syncedPosts;
        private int createdPosts;
        private int createdRoles;
        private int assignedRoles;
        private int removedRoles;
        private int functionSyncFailed;
        private boolean rosterEnabled;
        private int rosterPulledUsers;
        private int rosterSyncedProfiles;
        private int rosterUpdatedProfiles;
        private int rosterCustomFields;
        private int rosterSyncFailed;
        private int enabledAdminUsers;
        private int disabledAdminUsers;
        private int cleanedProfiles;
        private int cleanedBindings;
        private int cleanedUserSyncRows;
        private String snapshotBatchId;
        private int snapshotRows;
        private List<String> unmatchedMobiles = new ArrayList<>();
        private List<String> unmatchedIdentities = new ArrayList<>();
        private List<UnmatchedUser> unmatchedUsers = new ArrayList<>();
        private List<EmployeeSyncDetail> syncDetails = new ArrayList<>();

        void increaseMatchedByMobile() {
            this.matchedByMobile++;
        }

        void increaseMatchedByName() {
            this.matchedByName++;
        }

        void increaseUpdated() {
            this.updated++;
        }

        void increaseUnchanged() {
            this.unchanged++;
        }

        void increaseCreated() {
            this.created++;
        }

        void increaseCreateFailed() {
            this.createFailed++;
        }

        void increaseSkippedCreateInvalid() {
            this.skippedCreateInvalid++;
        }

        void increaseSkippedDuplicateMapping() {
            this.skippedDuplicateMapping++;
        }

        void increaseUnmatched() {
            this.unmatched++;
        }

        void increaseSkippedNoMobile() {
            this.skippedNoMobile++;
        }

        void increaseSnapshotRows() {
            this.snapshotRows++;
        }

        void increaseSyncedDeptIds() {
            this.syncedDeptIds++;
        }

        void increaseSyncedJobTitles() {
            this.syncedJobTitles++;
        }

        void increaseSyncedJobNumbers() {
            this.syncedJobNumbers++;
        }

        void increaseSyncedDeptLeaders() {
            this.syncedDeptLeaders++;
        }

        void increaseSyncedDirectSupervisors() {
            this.syncedDirectSupervisors++;
        }

        void increaseSyncedPosts() {
            this.syncedPosts++;
        }

        void increaseCreatedPosts() {
            this.createdPosts++;
        }

        void increaseCreatedRoles(int count) {
            this.createdRoles += Math.max(count, 0);
        }

        void increaseAssignedRoles(int count) {
            this.assignedRoles += Math.max(count, 0);
        }

        void increaseRemovedRoles(int count) {
            this.removedRoles += Math.max(count, 0);
        }

        void increaseFunctionSyncFailed() {
            this.functionSyncFailed++;
        }

        void increaseRosterSyncedProfiles() {
            this.rosterSyncedProfiles++;
        }

        void increaseRosterUpdatedProfiles() {
            this.rosterUpdatedProfiles++;
        }

        void increaseRosterCustomFields(int count) {
            this.rosterCustomFields += Math.max(count, 0);
        }

        void increaseRosterSyncFailed() {
            this.rosterSyncFailed++;
        }

        void increaseUsersWithMobile() {
            this.usersWithMobile++;
        }

        void increaseUsersWithName() {
            this.usersWithName++;
        }

        void increaseUsersWithNeitherIdentity() {
            this.usersWithNeitherIdentity++;
        }

        void increaseDeptTreePulled() {
            this.deptTreePulled++;
        }

        void increaseDeptTreeSynced() {
            this.deptTreeSynced++;
        }

        void increaseDeptTreeCreated() {
            this.deptTreeCreated++;
        }

        void increaseDeptTreeUpdated() {
            this.deptTreeUpdated++;
        }

        void increaseDeptTreeFailed() {
            this.deptTreeFailed++;
        }

        void addUnmatchedMobile(String mobile) {
            if (unmatchedMobiles.size() < 20) {
                unmatchedMobiles.add(mobile);
            }
        }

        void addUnmatchedIdentity(String dingUserId, String mobile, String name, String email, boolean active) {
            if (StringUtils.hasText(mobile)) {
                addUnmatchedMobile(mobile);
            }
            if (unmatchedIdentities.size() < 20) {
                StringBuilder value = new StringBuilder();
                if (StringUtils.hasText(dingUserId)) {
                    value.append("dingUserId:").append(dingUserId).append(", ");
                }
                if (StringUtils.hasText(mobile)) {
                    value.append("mobile:").append(mobile);
                } else {
                    value.append("name:").append(name == null ? "" : name);
                }
                unmatchedIdentities.add(value.toString());
            }
            if (unmatchedUsers.size() < 200) {
                UnmatchedUser user = new UnmatchedUser();
                user.setDingUserId(dingUserId);
                user.setName(name);
                user.setMobile(mobile);
                user.setEmail(email);
                user.setActive(active);
                unmatchedUsers.add(user);
            }
        }

        void addSyncDetail(String dingUserId, String name, String mobile, String email,
                           String matchType, String action, String changedFields) {
            if (syncDetails.size() >= 500) {
                return;
            }
            EmployeeSyncDetail detail = new EmployeeSyncDetail();
            detail.setDingUserId(dingUserId);
            detail.setName(name);
            detail.setMobile(mobile);
            detail.setEmail(email);
            detail.setMatchType(matchType);
            detail.setAction(action);
            detail.setChangedFields(changedFields);
            syncDetails.add(detail);
        }

    }

    @Data
    public static class UnmatchedUser {
        private String dingUserId;
        private String name;
        private String mobile;
        private String email;
        private boolean active;
    }

    @Data
    public static class EmployeeSyncDetail {
        private String dingUserId;
        private String name;
        private String mobile;
        private String email;
        private String matchType;
        private String action;
        private String changedFields;
    }

    @Data
    public static class EmployeeSyncPreview {
        private int totalFromDingTalk;
        private int matchedByMobile;
        private int matchedByName;
        private int updated;
        private int unchanged;
        private int toCreate;
        private int unmatched;
        private int skippedNoMobile;
        private int skippedCreateInvalid;
        private int skippedDuplicateMapping;
        private int usersWithMobile;
        private int usersWithName;
        private int usersWithNeitherIdentity;
        private int toMarkBindingsInactive;
        private int toDisableProfiles;
        private int toMarkEntriesResigned;
        private int toDisableAdminUsers;
        private boolean rosterEnabled;
        private int rosterPulledUsers;
        private boolean autoCreateEnabled;
        private Long autoCreateDeptId;
        private List<EmployeeSyncDetail> syncDetails = new ArrayList<>();

        void increaseMatchedByMobile() {
            this.matchedByMobile++;
        }

        void increaseMatchedByName() {
            this.matchedByName++;
        }

        void increaseUpdated() {
            this.updated++;
        }

        void increaseUnchanged() {
            this.unchanged++;
        }

        void increaseToCreate() {
            this.toCreate++;
        }

        void increaseUnmatched() {
            this.unmatched++;
        }

        void increaseSkippedNoMobile() {
            this.skippedNoMobile++;
        }

        void increaseSkippedCreateInvalid() {
            this.skippedCreateInvalid++;
        }

        void increaseSkippedDuplicateMapping() {
            this.skippedDuplicateMapping++;
        }

        void increaseUsersWithMobile() {
            this.usersWithMobile++;
        }

        void increaseUsersWithName() {
            this.usersWithName++;
        }

        void increaseUsersWithNeitherIdentity() {
            this.usersWithNeitherIdentity++;
        }

        void increaseToMarkBindingsInactive() {
            this.toMarkBindingsInactive++;
        }

        void increaseToDisableProfiles() {
            this.toDisableProfiles++;
        }

        void increaseToMarkEntriesResigned() {
            this.toMarkEntriesResigned++;
        }

        void increaseToDisableAdminUsers() {
            this.toDisableAdminUsers++;
        }

        void addSyncDetail(String dingUserId, String name, String mobile, String email,
                           String matchType, String action, String changedFields) {
            if (syncDetails.size() >= 500) {
                return;
            }
            EmployeeSyncDetail detail = new EmployeeSyncDetail();
            detail.setDingUserId(dingUserId);
            detail.setName(name);
            detail.setMobile(mobile);
            detail.setEmail(email);
            detail.setMatchType(matchType);
            detail.setAction(action);
            detail.setChangedFields(changedFields);
            syncDetails.add(detail);
        }
    }

    @Data
    public static class EmployeeImportReport {
        private int totalFromDingTalk;
        private Long targetDeptId;
        private int imported;
        private int failed;
        private int skippedExists;
        private int skippedInvalid;
        private List<String> importedUsers = new ArrayList<>();
        private List<String> failedUsers = new ArrayList<>();
        private List<String> skippedUsers = new ArrayList<>();
        private List<EmployeeImportDetail> importDetails = new ArrayList<>();

        void increaseImported() {
            this.imported++;
        }

        void increaseFailed() {
            this.failed++;
        }

        void increaseSkippedExists() {
            this.skippedExists++;
        }

        void increaseSkippedInvalid() {
            this.skippedInvalid++;
        }

        void addImportedUser(String dingUserId, String mobile, String name, Long profileId, Long userId) {
            if (importedUsers.size() < 50) {
                importedUsers.add(buildLabel(dingUserId, mobile, name, null));
            }
            addImportDetail("IMPORTED", dingUserId, mobile, name, profileId, userId, null);
        }

        void addFailedUser(String dingUserId, String mobile, String name, String reason) {
            if (failedUsers.size() < 50) {
                failedUsers.add(buildLabel(dingUserId, mobile, name, reason));
            }
            addImportDetail("FAILED", dingUserId, mobile, name, null, null, reason);
        }

        void addSkippedUser(String dingUserId, String mobile, String name, String reason) {
            if (skippedUsers.size() < 50) {
                skippedUsers.add(buildLabel(dingUserId, mobile, name, reason));
            }
            String status = "already exists".equals(reason) ? "SKIPPED_EXISTS" : "SKIPPED_INVALID";
            addImportDetail(status, dingUserId, mobile, name, null, null, reason);
        }

        private void addImportDetail(String status, String dingUserId, String mobile, String name,
                                     Long profileId, Long userId, String reason) {
            if (importDetails.size() >= 500) {
                return;
            }
            EmployeeImportDetail detail = new EmployeeImportDetail();
            detail.setStatus(status);
            detail.setDingUserId(dingUserId);
            detail.setMobile(mobile);
            detail.setName(name);
            detail.setProfileId(profileId);
            detail.setUserId(userId);
            detail.setReason(reason);
            importDetails.add(detail);
        }

        private String buildLabel(String dingUserId, String mobile, String name, String reason) {
            StringBuilder sb = new StringBuilder();
            if (StringUtils.hasText(dingUserId)) {
                sb.append("dingUserId:").append(dingUserId).append(", ");
            }
            if (StringUtils.hasText(mobile)) {
                sb.append("mobile:").append(mobile).append(", ");
            }
            sb.append("name:").append(name == null ? "" : name);
            if (StringUtils.hasText(reason)) {
                sb.append(", reason:").append(reason);
            }
            return sb.toString();
        }
    }

    @Data
    public static class EmployeeImportDetail {
        private String status;
        private String dingUserId;
        private String name;
        private String mobile;
        private Long profileId;
        private Long userId;
        private String reason;
    }

    private static class MatchContext {
        private Map<String, EmployeeProfileDO> profileByMobile;
        private Map<String, EmployeeProfileDO> profileByName;
        private Set<String> existingMobileSet = new HashSet<>();
        private Set<String> existingNameSet = new HashSet<>();
        private Set<String> crossTenantMobileSet = Collections.emptySet();
        private Set<String> crossTenantNameSet = Collections.emptySet();
    }

}
