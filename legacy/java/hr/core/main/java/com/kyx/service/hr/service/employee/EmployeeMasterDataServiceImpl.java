package com.kyx.service.hr.service.employee;

import com.kyx.foundation.common.exception.enums.GlobalErrorCodeConstants;
import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.foundation.security.core.service.SecurityFrameworkService;
import com.kyx.foundation.security.core.util.SecurityFrameworkUtils;
import com.kyx.service.hr.controller.admin.employee.vo.master.EmployeeChangeLogRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.master.EmployeeCustomFieldRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.master.EmployeeCustomFieldSaveReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.master.EmployeeCustomFieldValueRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.master.EmployeeCustomFieldValueSaveReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.master.EmployeeCustomFieldValuesSaveReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.master.EmployeeDataQualityIssueRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.master.EmployeeDataQualityRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.master.EmployeeMasterWorkbenchRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.master.EmployeeSavedViewRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.master.EmployeeSavedViewSaveReqVO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeChangeLogDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeCustomFieldDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeCustomFieldValueDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeEntryDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeProfileDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeSavedViewDO;
import com.kyx.service.hr.dal.dataobject.integration.DingTalkUserBindingDO;
import com.kyx.service.hr.dal.mysql.employee.EmployeeChangeLogMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeCustomFieldMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeCustomFieldValueMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeEntryMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeProfileMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeSavedViewMapper;
import com.kyx.service.hr.dal.mysql.integration.DingTalkUserBindingMapper;
import com.kyx.service.hr.enums.ErrorCodeConstants;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Validated
public class EmployeeMasterDataServiceImpl implements EmployeeMasterDataService {

    private static final int ENABLED = 0;
    private static final String PERMISSION_SENSITIVE_VIEW = "hr:employee:sensitive-view";
    private static final String MASKED_VALUE = "******";
    private static final Pattern FIELD_KEY_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{1,63}$");
    private static final Set<String> SUPPORTED_FIELD_TYPES = new HashSet<>(Arrays.asList(
            "TEXT", "NUMBER", "DATE", "SELECT", "MULTI_SELECT", "BOOLEAN"));
    private static final String DEFAULT_CUSTOM_FIELD_GROUP = "基础信息";
    private static final Set<String> PROFILE_SENSITIVE_FIELD_KEYS = new HashSet<>(Arrays.asList(
            "idNumber", "birthDate", "address", "emergencyContact", "emergencyPhone",
            "fatherBirthday", "motherBirthday"));

    @Resource
    private EmployeeProfileMapper employeeProfileMapper;

    @Resource
    private EmployeeEntryMapper employeeEntryMapper;

    @Resource
    private DingTalkUserBindingMapper dingTalkUserBindingMapper;

    @Resource
    private EmployeeCustomFieldMapper employeeCustomFieldMapper;

    @Resource
    private EmployeeCustomFieldValueMapper employeeCustomFieldValueMapper;

    @Resource
    private EmployeeSavedViewMapper employeeSavedViewMapper;

    @Resource
    private EmployeeChangeLogMapper employeeChangeLogMapper;

    @Resource
    private SecurityFrameworkService securityFrameworkService;

    @Override
    public EmployeeMasterWorkbenchRespVO getWorkbench() {
        QualityContext context = buildQualityContext();
        EmployeeDataQualityRespVO quality = buildDataQuality(context);

        EmployeeMasterWorkbenchRespVO respVO = new EmployeeMasterWorkbenchRespVO();
        respVO.setTotalProfiles(context.getProfiles().size());
        respVO.setActiveProfiles((int) context.getProfiles().stream()
                .filter(this::isEnabledProfile)
                .count());
        respVO.setActiveEntries((int) context.getEntryByProfileId().values().stream()
                .filter(this::isEffectiveEntry)
                .count());
        respVO.setDataQualityScore(quality.getScore());
        respVO.setMissingMobileCount(countIssues(quality, "MISSING_MOBILE"));
        respVO.setMissingUserCount(countIssues(quality, "MISSING_USER"));
        respVO.setMissingDeptCount(countIssues(quality, "MISSING_DEPT"));
        respVO.setMissingDingTalkCount(countIssues(quality, "MISSING_DINGTALK"));
        respVO.setMissingCustomFieldCount(countIssues(quality, "MISSING_REQUIRED_CUSTOM_FIELD"));
        respVO.setDuplicateMobileCount(countIssues(quality, "DUPLICATE_MOBILE"));
        respVO.setDuplicateIdNumberCount(countIssues(quality, "DUPLICATE_ID_NUMBER"));
        respVO.setContractExpiringCount(countIssues(quality, "CONTRACT_EXPIRING"));
        respVO.setProbationDueCount(countIssues(quality, "PROBATION_DUE"));
        respVO.setTopIssues(quality.getIssues().stream().limit(8).collect(Collectors.toList()));
        return respVO;
    }

    @Override
    public EmployeeDataQualityRespVO getDataQuality() {
        return buildDataQuality(buildQualityContext());
    }

    @Override
    public List<EmployeeCustomFieldRespVO> getCustomFieldList(Integer status) {
        return employeeCustomFieldMapper.selectListByStatus(status).stream()
                .map(field -> BeanUtils.toBean(field, EmployeeCustomFieldRespVO.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCustomField(EmployeeCustomFieldSaveReqVO reqVO) {
        EmployeeCustomFieldDO field = BeanUtils.toBean(reqVO, EmployeeCustomFieldDO.class);
        normalizeCustomField(field);
        validateCustomFieldKey(field.getFieldKey());
        validateCustomFieldType(field.getFieldType());
        validateCustomFieldKeyUnique(null, field.getFieldKey());
        employeeCustomFieldMapper.insert(field);
        return field.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCustomField(EmployeeCustomFieldSaveReqVO reqVO) {
        validateCustomFieldExists(reqVO.getId());
        EmployeeCustomFieldDO field = BeanUtils.toBean(reqVO, EmployeeCustomFieldDO.class);
        normalizeCustomField(field);
        validateCustomFieldKey(field.getFieldKey());
        validateCustomFieldType(field.getFieldType());
        validateCustomFieldKeyUnique(reqVO.getId(), field.getFieldKey());
        employeeCustomFieldMapper.updateById(field);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCustomField(Long id) {
        validateCustomFieldExists(id);
        employeeCustomFieldMapper.deleteById(id);
    }

    @Override
    public List<EmployeeCustomFieldValueRespVO> getCustomFieldValues(Long profileId) {
        validateProfileExists(profileId);
        boolean canViewSensitive = canViewSensitiveFields();
        List<EmployeeCustomFieldDO> fields = employeeCustomFieldMapper.selectListByStatus(ENABLED);
        Map<Long, EmployeeCustomFieldValueDO> valueMap = employeeCustomFieldValueMapper.selectListByProfileId(profileId)
                .stream()
                .collect(Collectors.toMap(EmployeeCustomFieldValueDO::getFieldId, Function.identity(), (a, b) -> a));
        List<EmployeeCustomFieldValueRespVO> result = new ArrayList<>();
        for (EmployeeCustomFieldDO field : fields) {
            EmployeeCustomFieldValueDO value = valueMap.get(field.getId());
            EmployeeCustomFieldValueRespVO respVO = new EmployeeCustomFieldValueRespVO();
            respVO.setId(value == null ? null : value.getId());
            respVO.setProfileId(profileId);
            respVO.setFieldId(field.getId());
            respVO.setFieldKey(field.getFieldKey());
            respVO.setFieldName(field.getFieldName());
            respVO.setFieldType(field.getFieldType());
            respVO.setFieldGroup(field.getFieldGroup());
            respVO.setOptionsJson(field.getOptionsJson());
            respVO.setRequiredFlag(Boolean.TRUE.equals(field.getRequiredFlag()));
            respVO.setSensitiveFlag(Boolean.TRUE.equals(field.getSensitiveFlag()));
            respVO.setSortOrder(field.getSortOrder());
            boolean masked = Boolean.TRUE.equals(field.getSensitiveFlag()) && !canViewSensitive
                    && value != null && hasCustomFieldValue(value.getFieldValue(), value.getValueJson());
            respVO.setMaskedFlag(masked);
            respVO.setFieldValue(masked ? MASKED_VALUE : value == null ? null : value.getFieldValue());
            respVO.setValueJson(masked ? null : value == null ? null : value.getValueJson());
            result.add(respVO);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveCustomFieldValues(EmployeeCustomFieldValuesSaveReqVO reqVO) {
        validateProfileExists(reqVO.getProfileId());
        List<EmployeeCustomFieldValueSaveReqVO> values = reqVO.getValues() == null
                ? new ArrayList<>() : reqVO.getValues();
        List<EmployeeCustomFieldDO> enabledFields = employeeCustomFieldMapper.selectListByStatus(ENABLED);
        Map<Long, EmployeeCustomFieldDO> fieldMap = enabledFields.stream()
                .filter(field -> field.getId() != null)
                .collect(Collectors.toMap(EmployeeCustomFieldDO::getId, Function.identity(), (a, b) -> a));
        Map<Long, EmployeeCustomFieldValueSaveReqVO> valueReqMap = values.stream()
                .filter(value -> value.getFieldId() != null)
                .collect(Collectors.toMap(EmployeeCustomFieldValueSaveReqVO::getFieldId, Function.identity(), (a, b) -> b));
        Map<Long, EmployeeCustomFieldValueDO> existingValueMap = employeeCustomFieldValueMapper
                .selectListByProfileId(reqVO.getProfileId())
                .stream()
                .collect(Collectors.toMap(EmployeeCustomFieldValueDO::getFieldId, Function.identity(), (a, b) -> a));
        validateRequiredCustomFieldValues(enabledFields, valueReqMap, existingValueMap);
        if (valueReqMap.isEmpty()) {
            return;
        }
        boolean canViewSensitive = canViewSensitiveFields();
        for (EmployeeCustomFieldValueSaveReqVO valueReqVO : valueReqMap.values()) {
            EmployeeCustomFieldDO field = fieldMap.get(valueReqVO.getFieldId());
            if (field == null) {
                field = validateCustomFieldExists(valueReqVO.getFieldId());
            }
            validateSensitiveFieldPermission(field, canViewSensitive);
            validateCustomFieldValue(field, valueReqVO);
            EmployeeCustomFieldValueDO existing = existingValueMap.get(valueReqVO.getFieldId());
            boolean emptyValue = !StringUtils.hasText(valueReqVO.getFieldValue())
                    && !StringUtils.hasText(valueReqVO.getValueJson());
            if (emptyValue) {
                if (existing != null) {
                    employeeCustomFieldValueMapper.deleteById(existing.getId());
                    insertChangeLog(reqVO.getProfileId(), "custom_field", field.getFieldKey(), field.getFieldName(),
                            displayCustomFieldValue(existing.getFieldValue(), existing.getValueJson()),
                            null, "CUSTOM_FIELD", existing.getId());
                }
                continue;
            }

            if (existing == null) {
                EmployeeCustomFieldValueDO value = new EmployeeCustomFieldValueDO();
                value.setProfileId(reqVO.getProfileId());
                value.setFieldId(field.getId());
                value.setFieldKey(field.getFieldKey());
                value.setFieldValue(valueReqVO.getFieldValue());
                value.setValueJson(valueReqVO.getValueJson());
                employeeCustomFieldValueMapper.insert(value);
                insertChangeLog(reqVO.getProfileId(), "custom_field", field.getFieldKey(), field.getFieldName(),
                        null, displayCustomFieldValue(valueReqVO.getFieldValue(), valueReqVO.getValueJson()),
                        "CUSTOM_FIELD", value.getId());
            } else {
                String beforeValue = displayCustomFieldValue(existing.getFieldValue(), existing.getValueJson());
                String afterValue = displayCustomFieldValue(valueReqVO.getFieldValue(), valueReqVO.getValueJson());
                existing.setFieldValue(valueReqVO.getFieldValue());
                existing.setValueJson(valueReqVO.getValueJson());
                employeeCustomFieldValueMapper.updateById(existing);
                if (!Objects.equals(beforeValue, afterValue)) {
                    insertChangeLog(reqVO.getProfileId(), "custom_field", field.getFieldKey(), field.getFieldName(),
                            beforeValue, afterValue, "CUSTOM_FIELD", existing.getId());
                }
            }
        }
    }

    @Override
    public List<EmployeeSavedViewRespVO> getMySavedViews() {
        Long userId = getLoginUserId();
        return employeeSavedViewMapper.selectListByUserId(userId).stream()
                .map(view -> BeanUtils.toBean(view, EmployeeSavedViewRespVO.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveMySavedView(EmployeeSavedViewSaveReqVO reqVO) {
        Long userId = getLoginUserId();
        if (Boolean.TRUE.equals(reqVO.getDefaultView())) {
            employeeSavedViewMapper.clearDefaultByUserId(userId);
        }

        EmployeeSavedViewDO view;
        if (reqVO.getId() == null) {
            view = BeanUtils.toBean(reqVO, EmployeeSavedViewDO.class);
            view.setUserId(userId);
            view.setDefaultView(Boolean.TRUE.equals(reqVO.getDefaultView()));
            employeeSavedViewMapper.insert(view);
            return view.getId();
        }

        view = validateSavedViewOwner(reqVO.getId(), userId);
        view.setViewName(reqVO.getViewName());
        view.setFilterJson(reqVO.getFilterJson());
        view.setColumnsJson(reqVO.getColumnsJson());
        view.setSortJson(reqVO.getSortJson());
        view.setDefaultView(Boolean.TRUE.equals(reqVO.getDefaultView()));
        employeeSavedViewMapper.updateById(view);
        return view.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMySavedView(Long id) {
        EmployeeSavedViewDO view = validateSavedViewOwner(id, getLoginUserId());
        employeeSavedViewMapper.deleteById(view.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setMyDefaultSavedView(Long id) {
        Long userId = getLoginUserId();
        EmployeeSavedViewDO view = validateSavedViewOwner(id, userId);
        employeeSavedViewMapper.clearDefaultByUserId(userId);
        view.setDefaultView(true);
        employeeSavedViewMapper.updateById(view);
    }

    @Override
    public List<EmployeeChangeLogRespVO> getChangeLogs(Long profileId) {
        validateProfileExists(profileId);
        boolean canViewSensitive = canViewSensitiveFields();
        Set<String> sensitiveFieldKeys = loadSensitiveFieldKeys();
        return employeeChangeLogMapper.selectListByProfileId(profileId).stream()
                .map(log -> {
                    EmployeeChangeLogRespVO respVO = BeanUtils.toBean(log, EmployeeChangeLogRespVO.class);
                    if (!canViewSensitive && sensitiveFieldKeys.contains(log.getFieldKey())) {
                        respVO.setBeforeValue(maskLogValue(respVO.getBeforeValue()));
                        respVO.setAfterValue(maskLogValue(respVO.getAfterValue()));
                    }
                    return respVO;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordProfileChanges(EmployeeProfileDO beforeProfile, EmployeeProfileDO afterProfile,
                                     String sourceType, Long sourceId) {
        if (beforeProfile == null || afterProfile == null || beforeProfile.getId() == null) {
            return;
        }
        List<FieldSpec> specs = buildProfileFieldSpecs();
        for (FieldSpec spec : specs) {
            Object beforeValue = spec.getGetter().apply(beforeProfile);
            Object afterValue = spec.getGetter().apply(afterProfile);
            if (!Objects.equals(beforeValue, afterValue)) {
                insertChangeLog(beforeProfile.getId(), "profile", spec.getFieldKey(), spec.getFieldName(),
                        stringify(beforeValue), stringify(afterValue), sourceType, sourceId);
            }
        }
    }

    private EmployeeDataQualityRespVO buildDataQuality(QualityContext context) {
        List<EmployeeDataQualityIssueRespVO> issues = new ArrayList<>();
        List<EmployeeProfileDO> qualityProfiles = context.getProfiles().stream()
                .filter(profile -> isEnabledProfile(profile)
                        && isEffectiveEntry(context.getEntryByProfileId().get(profile.getId())))
                .collect(Collectors.toList());
        Map<String, Long> mobileCounts = countBy(qualityProfiles, EmployeeProfileDO::getMobile);
        Map<String, Long> idNumberCounts = countBy(qualityProfiles, EmployeeProfileDO::getIdNumber);
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(30);

        for (EmployeeProfileDO profile : qualityProfiles) {
            EmployeeEntryDO entry = context.getEntryByProfileId().get(profile.getId());
            Long resolvedUserId = resolveUserId(profile, entry);
            boolean hasDingTalk = context.getDingTalkProfileIds().contains(profile.getId())
                    || (resolvedUserId != null && context.getDingTalkUserIds().contains(resolvedUserId));

            if (!StringUtils.hasText(profile.getMobile())) {
                issues.add(issue("MISSING_MOBILE", "缺手机号", "HIGH", profile, entry,
                        "员工档案缺少手机号，无法稳定匹配钉钉、账号和通知。", "补齐手机号"));
            }
            if (resolvedUserId == null) {
                issues.add(issue("MISSING_USER", "缺账号绑定", "HIGH", profile, entry,
                        "员工档案未绑定系统账号，员工自助和权限无法闭环。", "绑定或创建系统账号"));
            }
            if (entry == null || entry.getDeptId() == null) {
                issues.add(issue("MISSING_DEPT", "缺部门", "HIGH", profile, entry,
                        "员工没有有效任职部门，数据权限、统计和审批链会失真。", "维护任职部门"));
            }
            if (!hasDingTalk) {
                issues.add(issue("MISSING_DINGTALK", "缺钉钉绑定", "MEDIUM", profile, entry,
                        "员工未绑定钉钉身份，钉钉考勤和消息无法稳定触达。", "执行钉钉同步或人工绑定"));
            }
            if (StringUtils.hasText(profile.getMobile()) && mobileCounts.getOrDefault(profile.getMobile().trim(), 0L) > 1) {
                issues.add(issue("DUPLICATE_MOBILE", "手机号重复", "HIGH", profile, entry,
                        "同一手机号绑定了多个员工档案，可能导致账号或钉钉同步错配。", "合并或修正重复档案"));
            }
            if (StringUtils.hasText(profile.getIdNumber()) && idNumberCounts.getOrDefault(profile.getIdNumber().trim(), 0L) > 1) {
                issues.add(issue("DUPLICATE_ID_NUMBER", "身份证重复", "HIGH", profile, entry,
                        "同一身份证号绑定了多个员工档案，影响用工合规和薪酬归档。", "核对身份证并合并档案"));
            }
            if (profile.getOnboardDate() == null && (entry == null || entry.getEntryDate() == null)) {
                issues.add(issue("MISSING_ONBOARD_DATE", "缺入职日期", "MEDIUM", profile, entry,
                        "员工缺少入职日期，司龄、转正和年假规则无法准确计算。", "补齐入职日期"));
            }
            if (entry != null && entry.getContractEndDate() != null
                    && !entry.getContractEndDate().isBefore(today)
                    && !entry.getContractEndDate().isAfter(deadline)) {
                issues.add(issue("CONTRACT_EXPIRING", "合同即将到期", "MEDIUM", profile, entry,
                        "员工合同将在 30 天内到期，需要 HR 提前处理续签或终止。", "发起合同续签提醒"));
            }
            if (isProbationDue(profile, entry, today, deadline)) {
                issues.add(issue("PROBATION_DUE", "即将转正", "LOW", profile, entry,
                        "员工 30 天内需要转正确认。", "发起转正流程"));
            }
            for (EmployeeCustomFieldDO field : context.getRequiredCustomFields()) {
                EmployeeCustomFieldValueDO value = context.getCustomValueMap()
                        .get(customFieldValueKey(profile.getId(), field.getId()));
                if (!hasCustomFieldValue(value == null ? null : value.getFieldValue(),
                        value == null ? null : value.getValueJson())) {
                    issues.add(issue("MISSING_REQUIRED_CUSTOM_FIELD", "扩展字段缺失", "MEDIUM", profile, entry,
                            "员工档案缺少必填扩展字段「" + field.getFieldName() + "」。",
                            "补齐扩展字段：" + field.getFieldName()));
                }
            }
        }

        issues.sort(Comparator
                .comparingInt((EmployeeDataQualityIssueRespVO issue) -> severityRank(issue.getSeverity()))
                .thenComparing(EmployeeDataQualityIssueRespVO::getIssueType));

        EmployeeDataQualityRespVO respVO = new EmployeeDataQualityRespVO();
        respVO.setIssues(issues);
        respVO.setTotalIssueCount(issues.size());
        respVO.setHighRiskCount((int) issues.stream().filter(issue -> "HIGH".equals(issue.getSeverity())).count());
        respVO.setMediumRiskCount((int) issues.stream().filter(issue -> "MEDIUM".equals(issue.getSeverity())).count());
        respVO.setLowRiskCount((int) issues.stream().filter(issue -> "LOW".equals(issue.getSeverity())).count());
        respVO.setScore(calculateQualityScore(qualityProfiles.size(), respVO.getHighRiskCount(),
                respVO.getMediumRiskCount(), respVO.getLowRiskCount()));
        return respVO;
    }

    private QualityContext buildQualityContext() {
        List<EmployeeProfileDO> profiles = employeeProfileMapper.selectList();
        List<EmployeeEntryDO> entries = employeeEntryMapper.selectList();
        List<DingTalkUserBindingDO> bindings = dingTalkUserBindingMapper.selectListAll();

        Map<Long, EmployeeEntryDO> entryByProfileId = entries.stream()
                .filter(entry -> entry.getProfileId() != null)
                .sorted(Comparator.comparing(EmployeeEntryDO::getId, Comparator.nullsLast(Long::compareTo)).reversed())
                .collect(Collectors.toMap(EmployeeEntryDO::getProfileId, Function.identity(), (a, b) -> a));
        Set<Long> dingTalkProfileIds = bindings.stream()
                .map(DingTalkUserBindingDO::getProfileId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> dingTalkUserIds = bindings.stream()
                .map(DingTalkUserBindingDO::getOaUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<EmployeeCustomFieldDO> requiredCustomFields = employeeCustomFieldMapper.selectListByStatus(ENABLED)
                .stream()
                .filter(field -> Boolean.TRUE.equals(field.getRequiredFlag()))
                .collect(Collectors.toList());
        Map<String, EmployeeCustomFieldValueDO> customValueMap = loadRequiredCustomValueMap(requiredCustomFields);
        return new QualityContext(profiles, entryByProfileId, dingTalkProfileIds, dingTalkUserIds,
                requiredCustomFields, customValueMap);
    }

    private Map<String, EmployeeCustomFieldValueDO> loadRequiredCustomValueMap(
            List<EmployeeCustomFieldDO> requiredCustomFields) {
        Map<String, EmployeeCustomFieldValueDO> result = new HashMap<>();
        if (requiredCustomFields == null || requiredCustomFields.isEmpty()) {
            return result;
        }
        Set<Long> fieldIds = requiredCustomFields.stream()
                .map(EmployeeCustomFieldDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (fieldIds.isEmpty()) {
            return result;
        }
        List<EmployeeCustomFieldValueDO> values = employeeCustomFieldValueMapper.selectList(
                new LambdaQueryWrapperX<EmployeeCustomFieldValueDO>()
                        .in(EmployeeCustomFieldValueDO::getFieldId, fieldIds));
        for (EmployeeCustomFieldValueDO value : values) {
            if (value.getProfileId() != null && value.getFieldId() != null) {
                result.put(customFieldValueKey(value.getProfileId(), value.getFieldId()), value);
            }
        }
        return result;
    }

    private Map<String, Long> countBy(List<EmployeeProfileDO> profiles, Function<EmployeeProfileDO, String> getter) {
        Map<String, Long> result = new HashMap<>();
        for (EmployeeProfileDO profile : profiles) {
            String value = getter.apply(profile);
            if (!StringUtils.hasText(value)) {
                continue;
            }
            result.merge(value.trim(), 1L, Long::sum);
        }
        return result;
    }

    private EmployeeDataQualityIssueRespVO issue(String issueType, String issueName, String severity,
                                                EmployeeProfileDO profile, EmployeeEntryDO entry,
                                                String description, String action) {
        EmployeeDataQualityIssueRespVO issue = new EmployeeDataQualityIssueRespVO();
        issue.setIssueType(issueType);
        issue.setIssueName(issueName);
        issue.setSeverity(severity);
        issue.setProfileId(profile.getId());
        issue.setEmployeeName(profile.getName());
        issue.setMobile(profile.getMobile());
        issue.setDeptId(entry == null ? null : entry.getDeptId());
        issue.setDescription(description);
        issue.setAction(action);
        return issue;
    }

    private boolean isProbationDue(EmployeeProfileDO profile, EmployeeEntryDO entry, LocalDate today, LocalDate deadline) {
        if (profile.getConfirmationDate() != null) {
            return !profile.getConfirmationDate().isBefore(today) && !profile.getConfirmationDate().isAfter(deadline);
        }
        if (entry == null || entry.getEntryDate() == null || entry.getProbationMonths() == null) {
            return false;
        }
        LocalDate estimatedDate = entry.getEntryDate().plusMonths(entry.getProbationMonths());
        return !estimatedDate.isBefore(today) && !estimatedDate.isAfter(deadline);
    }

    private int countIssues(EmployeeDataQualityRespVO quality, String issueType) {
        return (int) quality.getIssues().stream()
                .filter(issue -> issueType.equals(issue.getIssueType()))
                .count();
    }

    private int calculateQualityScore(int profileCount, int highRiskCount, int mediumRiskCount, int lowRiskCount) {
        if (profileCount <= 0) {
            return 100;
        }
        int weighted = highRiskCount * 5 + mediumRiskCount * 3 + lowRiskCount;
        int maxWeighted = Math.max(profileCount * 8, 1);
        int penalty = (int) Math.round(weighted * 100.0 / maxWeighted);
        return Math.max(0, Math.min(100, 100 - penalty));
    }

    private int severityRank(String severity) {
        if ("HIGH".equals(severity)) {
            return 0;
        }
        if ("MEDIUM".equals(severity)) {
            return 1;
        }
        return 2;
    }

    private Long resolveUserId(EmployeeProfileDO profile, EmployeeEntryDO entry) {
        if (profile.getUserId() != null) {
            return profile.getUserId();
        }
        return entry == null ? null : entry.getUserId();
    }

    private boolean isEnabledProfile(EmployeeProfileDO profile) {
        return profile.getStatus() == null || Objects.equals(profile.getStatus(), 1);
    }

    private boolean isEffectiveEntry(EmployeeEntryDO entry) {
        return entry == null || entry.getWorkStatus() == null || !Objects.equals(entry.getWorkStatus(), 4);
    }

    private EmployeeProfileDO validateProfileExists(Long profileId) {
        EmployeeProfileDO profile = employeeProfileMapper.selectById(profileId);
        if (profile == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_PROFILE_NOT_EXISTS);
        }
        return profile;
    }

    private EmployeeCustomFieldDO validateCustomFieldExists(Long id) {
        EmployeeCustomFieldDO field = employeeCustomFieldMapper.selectById(id);
        if (field == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_CUSTOM_FIELD_NOT_EXISTS);
        }
        return field;
    }

    private void validateCustomFieldKeyUnique(Long id, String fieldKey) {
        EmployeeCustomFieldDO field = employeeCustomFieldMapper.selectByFieldKey(fieldKey);
        if (field == null) {
            return;
        }
        if (id == null || !field.getId().equals(id)) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_CUSTOM_FIELD_KEY_DUPLICATE);
        }
    }

    private void validateCustomFieldKey(String fieldKey) {
        if (!StringUtils.hasText(fieldKey) || !FIELD_KEY_PATTERN.matcher(fieldKey).matches()) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_CUSTOM_FIELD_KEY_INVALID);
        }
    }

    private void validateCustomFieldType(String fieldType) {
        if (!StringUtils.hasText(fieldType) || !SUPPORTED_FIELD_TYPES.contains(fieldType)) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_CUSTOM_FIELD_TYPE_INVALID);
        }
    }

    private void validateRequiredCustomFieldValues(List<EmployeeCustomFieldDO> fields,
                                                   Map<Long, EmployeeCustomFieldValueSaveReqVO> valueReqMap,
                                                   Map<Long, EmployeeCustomFieldValueDO> existingValueMap) {
        for (EmployeeCustomFieldDO field : fields) {
            if (!Boolean.TRUE.equals(field.getRequiredFlag())) {
                continue;
            }
            EmployeeCustomFieldValueSaveReqVO reqValue = valueReqMap.get(field.getId());
            if (reqValue != null) {
                if (!hasCustomFieldValue(reqValue.getFieldValue(), reqValue.getValueJson())) {
                    throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST,
                            field.getFieldName() + " 为必填扩展字段");
                }
                continue;
            }
            EmployeeCustomFieldValueDO existing = existingValueMap.get(field.getId());
            if (!hasCustomFieldValue(existing == null ? null : existing.getFieldValue(),
                    existing == null ? null : existing.getValueJson())) {
                throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST,
                        field.getFieldName() + " 为必填扩展字段");
            }
        }
    }

    private void validateCustomFieldValue(EmployeeCustomFieldDO field, EmployeeCustomFieldValueSaveReqVO reqVO) {
        boolean emptyValue = !hasCustomFieldValue(reqVO.getFieldValue(), reqVO.getValueJson());
        if (Boolean.TRUE.equals(field.getRequiredFlag()) && emptyValue) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST,
                    field.getFieldName() + " 为必填扩展字段");
        }
        if (emptyValue) {
            return;
        }
        String fieldValue = reqVO.getFieldValue() == null ? null : reqVO.getFieldValue().trim();
        if ("NUMBER".equals(field.getFieldType()) && StringUtils.hasText(fieldValue)) {
            try {
                new BigDecimal(fieldValue);
            } catch (NumberFormatException ex) {
                throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST,
                        field.getFieldName() + " 必须是数字");
            }
        }
        if ("DATE".equals(field.getFieldType()) && StringUtils.hasText(fieldValue)) {
            try {
                LocalDate.parse(fieldValue);
            } catch (Exception ex) {
                throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST,
                        field.getFieldName() + " 日期格式应为 yyyy-MM-dd");
            }
        }
        if ("BOOLEAN".equals(field.getFieldType()) && StringUtils.hasText(fieldValue)
                && !isBooleanCustomFieldValue(fieldValue)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST,
                    field.getFieldName() + " 必须是布尔值");
        }
        if ("MULTI_SELECT".equals(field.getFieldType()) && !StringUtils.hasText(reqVO.getValueJson())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST,
                    field.getFieldName() + " 多选值格式不正确");
        }
    }

    private boolean isBooleanCustomFieldValue(String value) {
        return Arrays.asList("true", "false", "1", "0", "是", "否").contains(value);
    }

    private void normalizeCustomField(EmployeeCustomFieldDO field) {
        field.setFieldKey(field.getFieldKey() == null ? null : field.getFieldKey().trim());
        field.setFieldName(field.getFieldName() == null ? null : field.getFieldName().trim());
        field.setFieldType(field.getFieldType() == null ? null : field.getFieldType().trim().toUpperCase());
        field.setFieldGroup(StringUtils.hasText(field.getFieldGroup())
                ? field.getFieldGroup().trim() : DEFAULT_CUSTOM_FIELD_GROUP);
        field.setRequiredFlag(Boolean.TRUE.equals(field.getRequiredFlag()));
        field.setSensitiveFlag(Boolean.TRUE.equals(field.getSensitiveFlag()));
        field.setSortOrder(field.getSortOrder() == null ? 0 : field.getSortOrder());
        field.setStatus(field.getStatus() == null ? ENABLED : field.getStatus());
    }

    private void validateSensitiveFieldPermission(EmployeeCustomFieldDO field, boolean canViewSensitive) {
        if (Boolean.TRUE.equals(field.getSensitiveFlag()) && !canViewSensitive) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.FORBIDDEN, "无权维护敏感员工字段");
        }
    }

    private boolean canViewSensitiveFields() {
        try {
            return securityFrameworkService.hasPermission(PERMISSION_SENSITIVE_VIEW);
        } catch (Exception ex) {
            return false;
        }
    }

    private Set<String> loadSensitiveFieldKeys() {
        Set<String> fieldKeys = employeeCustomFieldMapper.selectListByStatus(null).stream()
                .filter(field -> Boolean.TRUE.equals(field.getSensitiveFlag()))
                .map(EmployeeCustomFieldDO::getFieldKey)
                .collect(Collectors.toSet());
        fieldKeys.addAll(PROFILE_SENSITIVE_FIELD_KEYS);
        return fieldKeys;
    }

    private String maskLogValue(String value) {
        return StringUtils.hasText(value) ? MASKED_VALUE : value;
    }

    private boolean hasCustomFieldValue(String fieldValue, String valueJson) {
        return StringUtils.hasText(fieldValue) || StringUtils.hasText(valueJson);
    }

    private String displayCustomFieldValue(String fieldValue, String valueJson) {
        return StringUtils.hasText(fieldValue) ? fieldValue : valueJson;
    }

    private String customFieldValueKey(Long profileId, Long fieldId) {
        return String.valueOf(profileId) + ":" + String.valueOf(fieldId);
    }

    private EmployeeSavedViewDO validateSavedViewOwner(Long id, Long userId) {
        EmployeeSavedViewDO view = employeeSavedViewMapper.selectById(id);
        if (view == null || !Objects.equals(view.getUserId(), userId)) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_SAVED_VIEW_NOT_EXISTS);
        }
        return view;
    }

    private Long getLoginUserId() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_MASTER_LOGIN_USER_REQUIRED);
        }
        return userId;
    }

    private void insertChangeLog(Long profileId, String module, String fieldKey, String fieldName,
                                 String beforeValue, String afterValue, String sourceType, Long sourceId) {
        EmployeeChangeLogDO log = new EmployeeChangeLogDO();
        log.setProfileId(profileId);
        log.setModule(module);
        log.setFieldKey(fieldKey);
        log.setFieldName(fieldName);
        log.setBeforeValue(beforeValue);
        log.setAfterValue(afterValue);
        log.setSourceType(sourceType);
        log.setSourceId(sourceId);
        log.setOperatorId(SecurityFrameworkUtils.getLoginUserId());
        log.setOperatorName(SecurityFrameworkUtils.getLoginUserNickname());
        log.setOperationTime(LocalDateTime.now());
        employeeChangeLogMapper.insert(log);
    }

    private List<FieldSpec> buildProfileFieldSpecs() {
        List<FieldSpec> specs = new ArrayList<>();
        specs.add(new FieldSpec("name", "姓名", EmployeeProfileDO::getName));
        specs.add(new FieldSpec("mobile", "手机号", EmployeeProfileDO::getMobile));
        specs.add(new FieldSpec("email", "邮箱", EmployeeProfileDO::getEmail));
        specs.add(new FieldSpec("idNumber", "身份证号", EmployeeProfileDO::getIdNumber));
        specs.add(new FieldSpec("gender", "性别", EmployeeProfileDO::getGender));
        specs.add(new FieldSpec("birthDate", "出生日期", EmployeeProfileDO::getBirthDate));
        specs.add(new FieldSpec("nationality", "国籍", EmployeeProfileDO::getNationality));
        specs.add(new FieldSpec("ethnicity", "民族", EmployeeProfileDO::getEthnicity));
        specs.add(new FieldSpec("politicalStatus", "政治面貌", EmployeeProfileDO::getPoliticalStatus));
        specs.add(new FieldSpec("maritalStatus", "婚姻状况", EmployeeProfileDO::getMaritalStatus));
        specs.add(new FieldSpec("hometown", "籍贯", EmployeeProfileDO::getHometown));
        specs.add(new FieldSpec("address", "现住址", EmployeeProfileDO::getAddress));
        specs.add(new FieldSpec("emergencyContact", "紧急联系人", EmployeeProfileDO::getEmergencyContact));
        specs.add(new FieldSpec("emergencyPhone", "紧急联系电话", EmployeeProfileDO::getEmergencyPhone));
        specs.add(new FieldSpec("emergencyRelation", "紧急联系人关系", EmployeeProfileDO::getEmergencyRelation));
        specs.add(new FieldSpec("onboardDate", "入职日期", EmployeeProfileDO::getOnboardDate));
        specs.add(new FieldSpec("confirmationDate", "转正日期", EmployeeProfileDO::getConfirmationDate));
        specs.add(new FieldSpec("jobLevel", "岗位职级", EmployeeProfileDO::getJobLevel));
        specs.add(new FieldSpec("fatherBirthday", "父亲生日", EmployeeProfileDO::getFatherBirthday));
        specs.add(new FieldSpec("motherBirthday", "母亲生日", EmployeeProfileDO::getMotherBirthday));
        specs.add(new FieldSpec("status", "档案状态", EmployeeProfileDO::getStatus));
        return specs;
    }

    private String stringify(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    @Data
    @AllArgsConstructor
    private static class QualityContext {
        private List<EmployeeProfileDO> profiles;
        private Map<Long, EmployeeEntryDO> entryByProfileId;
        private Set<Long> dingTalkProfileIds;
        private Set<Long> dingTalkUserIds;
        private List<EmployeeCustomFieldDO> requiredCustomFields;
        private Map<String, EmployeeCustomFieldValueDO> customValueMap;
    }

    @Data
    @AllArgsConstructor
    private static class FieldSpec {
        private String fieldKey;
        private String fieldName;
        private Function<EmployeeProfileDO, Object> getter;
    }

}
