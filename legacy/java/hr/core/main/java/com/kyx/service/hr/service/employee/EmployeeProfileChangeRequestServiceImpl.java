package com.kyx.service.hr.service.employee;

import com.fasterxml.jackson.core.type.TypeReference;
import com.kyx.foundation.common.exception.enums.GlobalErrorCodeConstants;
import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.json.JsonUtils;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.foundation.security.core.service.SecurityFrameworkService;
import com.kyx.foundation.security.core.util.SecurityFrameworkUtils;
import com.kyx.service.bpm.api.task.BpmProcessInstanceApi;
import com.kyx.service.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.kyx.service.bpm.enums.task.BpmProcessInstanceStatusEnum;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeProfileChangeApplyReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeProfileChangeApproveReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeProfileChangePageReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeProfileChangeRespVO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeProfileChangeRequestDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeProfileDO;
import com.kyx.service.hr.dal.mysql.employee.EmployeeProfileChangeRequestMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeProfileMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Employee profile change request Service implementation.
 */
@Service
@Validated
@Slf4j
public class EmployeeProfileChangeRequestServiceImpl implements EmployeeProfileChangeRequestService {

    public static final String PROCESS_KEY = "hr_profile_change";

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_CANCELED = "CANCELED";
    private static final String DEFAULT_CHANGE_TYPE = "CONTACT";
    private static final String PERMISSION_APPROVE = "hr:profile-change:approve";

    private static final Set<String> ALLOWED_FIELDS = new HashSet<>(Arrays.asList(
            "mobile", "email", "address", "emergencyContact", "emergencyPhone",
            "emergencyRelation", "maritalStatus", "hometown"));

    @Resource
    private EmployeeProfileChangeRequestMapper profileChangeRequestMapper;
    @Resource
    private EmployeeProfileMapper employeeProfileMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private SecurityFrameworkService securityFrameworkService;
    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long apply(EmployeeProfileChangeApplyReqVO reqVO) {
        EmployeeProfileDO profile = resolveApplyProfile(reqVO.getProfileId(), reqVO.getUserId());
        Map<String, Object> afterMap = parseChangeJson(reqVO.getAfterJson());
        Map<String, Object> sanitizedAfter = sanitizeAfterMap(afterMap);
        if (sanitizedAfter.isEmpty()) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "没有可变更的资料字段");
        }
        Map<String, Object> beforeMap = buildBeforeMap(profile, sanitizedAfter.keySet());
        removeUnchangedFields(beforeMap, sanitizedAfter);
        if (sanitizedAfter.isEmpty()) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "资料内容没有变化");
        }
        EmployeeProfileChangeRequestDO request = new EmployeeProfileChangeRequestDO();
        request.setProfileId(profile.getId());
        request.setUserId(profile.getUserId());
        request.setChangeType(StringUtils.hasText(reqVO.getChangeType()) ? reqVO.getChangeType().trim().toUpperCase() : DEFAULT_CHANGE_TYPE);
        request.setBeforeJson(JsonUtils.toJsonString(beforeMap));
        request.setAfterJson(JsonUtils.toJsonString(sanitizedAfter));
        request.setChangeSummary(buildSummary(beforeMap, sanitizedAfter));
        request.setReason(reqVO.getReason());
        request.setStatus(STATUS_PENDING);
        profileChangeRequestMapper.insert(request);

        String processInstanceId = startProfileChangeApprovalProcess(request, profile);
        EmployeeProfileChangeRequestDO processUpdate = new EmployeeProfileChangeRequestDO();
        processUpdate.setId(request.getId());
        processUpdate.setProcessInstanceId(processInstanceId);
        profileChangeRequestMapper.updateById(processUpdate);
        return request.getId();
    }

    @Override
    public PageResult<EmployeeProfileChangeRespVO> getPage(EmployeeProfileChangePageReqVO pageReqVO) {
        if (!canManage()) {
            Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
            if (loginUserId == null) {
                return new PageResult<>(new ArrayList<>(), 0L);
            }
            pageReqVO.setUserId(loginUserId);
            pageReqVO.setProfileId(null);
        }
        normalizePageReq(pageReqVO);
        PageResult<EmployeeProfileChangeRequestDO> pageResult = profileChangeRequestMapper.selectPage(pageReqVO);
        List<EmployeeProfileChangeRequestDO> rows = pageResult.getList();
        if (rows == null || rows.isEmpty()) {
            return new PageResult<>(new ArrayList<>(), pageResult.getTotal());
        }
        List<EmployeeProfileChangeRespVO> respList = BeanUtils.toBean(rows, EmployeeProfileChangeRespVO.class);
        fillPeopleInfo(rows, respList);
        return new PageResult<>(respList, pageResult.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean approve(EmployeeProfileChangeApproveReqVO reqVO) {
        EmployeeProfileChangeRequestDO request = profileChangeRequestMapper.selectById(reqVO.getId());
        if (request == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND, "资料变更申请不存在");
        }
        if (!STATUS_PENDING.equals(request.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "只有待审批申请可以处理");
        }
        if (StringUtils.hasText(request.getProcessInstanceId())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "该申请已进入 BPM 流程，请在流程中心处理审批");
        }
        if (Boolean.TRUE.equals(reqVO.getApproved())) {
            applyProfileChanges(request);
        }
        EmployeeProfileChangeRequestDO updateDO = new EmployeeProfileChangeRequestDO();
        updateDO.setId(request.getId());
        updateDO.setStatus(Boolean.TRUE.equals(reqVO.getApproved()) ? STATUS_APPROVED : STATUS_REJECTED);
        updateDO.setApproverId(SecurityFrameworkUtils.getLoginUserId());
        updateDO.setApprovedTime(LocalDateTime.now());
        updateDO.setApproveRemark(reqVO.getApproveRemark());
        profileChangeRequestMapper.updateById(updateDO);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean cancel(Long id) {
        EmployeeProfileChangeRequestDO request = profileChangeRequestMapper.selectById(id);
        if (request == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND, "资料变更申请不存在");
        }
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (!canManage() && (loginUserId == null || !loginUserId.equals(request.getUserId()))) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.FORBIDDEN, "无权撤销该资料变更申请");
        }
        if (!STATUS_PENDING.equals(request.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "只有待审批申请可以撤销");
        }
        if (StringUtils.hasText(request.getProcessInstanceId())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "该申请已进入 BPM 流程，请在流程详情中撤销");
        }
        EmployeeProfileChangeRequestDO updateDO = new EmployeeProfileChangeRequestDO();
        updateDO.setId(request.getId());
        updateDO.setStatus(STATUS_CANCELED);
        profileChangeRequestMapper.updateById(updateDO);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateApprovalStatusByBpmEvent(Long id, String processInstanceId, Integer bpmStatus, Long operatorUserId) {
        EmployeeProfileChangeRequestDO request = profileChangeRequestMapper.selectById(id);
        if (request == null) {
            log.warn("资料变更 BPM 回调申请不存在，id={}, processInstanceId={}, bpmStatus={}",
                    id, processInstanceId, bpmStatus);
            return;
        }
        if (StringUtils.hasText(request.getProcessInstanceId())
                && StringUtils.hasText(processInstanceId)
                && !Objects.equals(request.getProcessInstanceId(), processInstanceId)) {
            log.warn("资料变更 BPM 回调流程实例不匹配，id={}, processInstanceId={}, currentProcessInstanceId={}",
                    id, processInstanceId, request.getProcessInstanceId());
            return;
        }
        if (Objects.equals(bpmStatus, BpmProcessInstanceStatusEnum.APPROVE.getStatus())) {
            approveByBpm(request, processInstanceId, operatorUserId);
            return;
        }
        if (Objects.equals(bpmStatus, BpmProcessInstanceStatusEnum.REJECT.getStatus())) {
            closeByBpm(request, STATUS_REJECTED, processInstanceId, operatorUserId, "BPM审批驳回");
            return;
        }
        if (Objects.equals(bpmStatus, BpmProcessInstanceStatusEnum.CANCEL.getStatus())) {
            closeByBpm(request, STATUS_CANCELED, processInstanceId, operatorUserId, "BPM流程撤销");
        }
    }

    private String startProfileChangeApprovalProcess(EmployeeProfileChangeRequestDO request, EmployeeProfileDO profile) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.UNAUTHORIZED, "资料变更发起人不能为空");
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("profileChangeId", request.getId());
        variables.put("profileId", request.getProfileId());
        variables.put("userId", request.getUserId());
        variables.put("employeeName", profile.getName());
        variables.put("changeType", request.getChangeType());
        variables.put("changeSummary", request.getChangeSummary());
        variables.put("beforeJson", request.getBeforeJson());
        variables.put("afterJson", request.getAfterJson());
        variables.put("reason", request.getReason());
        return processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO()
                        .setProcessDefinitionKey(PROCESS_KEY)
                        .setBusinessKey(String.valueOf(request.getId()))
                        .setVariables(variables))
                .getCheckedData();
    }

    private void approveByBpm(EmployeeProfileChangeRequestDO request, String processInstanceId, Long operatorUserId) {
        if (STATUS_APPROVED.equals(request.getStatus())) {
            return;
        }
        if (!STATUS_PENDING.equals(request.getStatus())) {
            log.warn("资料变更 BPM 通过回调忽略非待审批记录，id={}, status={}", request.getId(), request.getStatus());
            return;
        }
        applyProfileChanges(request);
        EmployeeProfileChangeRequestDO updateDO = new EmployeeProfileChangeRequestDO();
        updateDO.setId(request.getId());
        updateDO.setStatus(STATUS_APPROVED);
        updateDO.setProcessInstanceId(StringUtils.hasText(processInstanceId)
                ? processInstanceId : request.getProcessInstanceId());
        updateDO.setApproverId(operatorUserId);
        updateDO.setApprovedTime(LocalDateTime.now());
        updateDO.setApproveRemark("BPM审批通过");
        profileChangeRequestMapper.updateById(updateDO);
    }

    private void closeByBpm(EmployeeProfileChangeRequestDO request, String status,
                            String processInstanceId, Long operatorUserId, String remark) {
        if (status.equals(request.getStatus())) {
            return;
        }
        if (!STATUS_PENDING.equals(request.getStatus())) {
            log.warn("资料变更 BPM 关闭回调忽略非待审批记录，id={}, status={}, targetStatus={}",
                    request.getId(), request.getStatus(), status);
            return;
        }
        EmployeeProfileChangeRequestDO updateDO = new EmployeeProfileChangeRequestDO();
        updateDO.setId(request.getId());
        updateDO.setStatus(status);
        updateDO.setProcessInstanceId(StringUtils.hasText(processInstanceId)
                ? processInstanceId : request.getProcessInstanceId());
        updateDO.setApproverId(operatorUserId);
        updateDO.setApprovedTime(LocalDateTime.now());
        updateDO.setApproveRemark(remark);
        profileChangeRequestMapper.updateById(updateDO);
    }

    private void applyProfileChanges(EmployeeProfileChangeRequestDO request) {
        EmployeeProfileDO profile = employeeProfileMapper.selectById(request.getProfileId());
        if (profile == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND, "员工档案不存在");
        }
        Map<String, Object> afterMap = sanitizeAfterMap(parseChangeJson(request.getAfterJson()));
        EmployeeProfileDO updateDO = new EmployeeProfileDO();
        updateDO.setId(profile.getId());
        for (Map.Entry<String, Object> entry : afterMap.entrySet()) {
            applyField(updateDO, entry.getKey(), entry.getValue());
        }
        employeeProfileMapper.updateById(updateDO);
    }

    private EmployeeProfileDO resolveApplyProfile(Long profileId, Long userId) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.UNAUTHORIZED);
        }
        boolean manage = canManage();
        if ((profileId != null || userId != null) && !manage) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.FORBIDDEN, "无权替他人提交资料变更申请");
        }
        EmployeeProfileDO profile = profileId == null ? null : employeeProfileMapper.selectById(profileId);
        if (profile == null) {
            profile = employeeProfileMapper.selectByUserId(userId == null ? loginUserId : userId);
        }
        if (profile == null || profile.getUserId() == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "员工档案不存在或未绑定登录用户");
        }
        if (userId != null && !Objects.equals(userId, profile.getUserId())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "员工档案和用户不匹配");
        }
        return profile;
    }

    private Map<String, Object> parseChangeJson(String json) {
        Map<String, Object> map = JsonUtils.parseObjectQuietly(json, new TypeReference<Map<String, Object>>() {
        });
        if (map == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "资料变更内容不是合法JSON");
        }
        return map;
    }

    private Map<String, Object> sanitizeAfterMap(Map<String, Object> afterMap) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (afterMap == null) {
            return result;
        }
        for (Map.Entry<String, Object> entry : afterMap.entrySet()) {
            if (entry.getKey() == null || !ALLOWED_FIELDS.contains(entry.getKey())) {
                continue;
            }
            result.put(entry.getKey(), normalizeValue(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    private Object normalizeValue(String field, Object value) {
        if ("maritalStatus".equals(field)) {
            if (value == null || !StringUtils.hasText(String.valueOf(value))) {
                return null;
            }
            try {
                return Integer.valueOf(String.valueOf(value));
            } catch (NumberFormatException ex) {
                throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "婚姻状况格式不正确");
            }
        }
        return value == null ? null : String.valueOf(value).trim();
    }

    private void removeUnchangedFields(Map<String, Object> beforeMap, Map<String, Object> afterMap) {
        List<String> unchangedFields = new ArrayList<>();
        for (Map.Entry<String, Object> entry : afterMap.entrySet()) {
            Object before = beforeMap.get(entry.getKey());
            Object after = entry.getValue();
            if (Objects.equals(before == null ? null : String.valueOf(before), after == null ? null : String.valueOf(after))) {
                unchangedFields.add(entry.getKey());
            }
        }
        for (String field : unchangedFields) {
            beforeMap.remove(field);
            afterMap.remove(field);
        }
    }

    private Map<String, Object> buildBeforeMap(EmployeeProfileDO profile, Set<String> fields) {
        Map<String, Object> beforeMap = new LinkedHashMap<>();
        for (String field : fields) {
            beforeMap.put(field, getFieldValue(profile, field));
        }
        return beforeMap;
    }

    private Object getFieldValue(EmployeeProfileDO profile, String field) {
        if ("mobile".equals(field)) {
            return profile.getMobile();
        }
        if ("email".equals(field)) {
            return profile.getEmail();
        }
        if ("address".equals(field)) {
            return profile.getAddress();
        }
        if ("emergencyContact".equals(field)) {
            return profile.getEmergencyContact();
        }
        if ("emergencyPhone".equals(field)) {
            return profile.getEmergencyPhone();
        }
        if ("emergencyRelation".equals(field)) {
            return profile.getEmergencyRelation();
        }
        if ("maritalStatus".equals(field)) {
            return profile.getMaritalStatus();
        }
        if ("hometown".equals(field)) {
            return profile.getHometown();
        }
        return null;
    }

    private void applyField(EmployeeProfileDO updateDO, String field, Object value) {
        if ("mobile".equals(field)) {
            updateDO.setMobile(toStringValue(value));
        } else if ("email".equals(field)) {
            updateDO.setEmail(toStringValue(value));
        } else if ("address".equals(field)) {
            updateDO.setAddress(toStringValue(value));
        } else if ("emergencyContact".equals(field)) {
            updateDO.setEmergencyContact(toStringValue(value));
        } else if ("emergencyPhone".equals(field)) {
            updateDO.setEmergencyPhone(toStringValue(value));
        } else if ("emergencyRelation".equals(field)) {
            updateDO.setEmergencyRelation(toStringValue(value));
        } else if ("maritalStatus".equals(field)) {
            updateDO.setMaritalStatus(value == null ? null : Integer.valueOf(String.valueOf(value)));
        } else if ("hometown".equals(field)) {
            updateDO.setHometown(toStringValue(value));
        }
    }

    private String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String buildSummary(Map<String, Object> beforeMap, Map<String, Object> afterMap) {
        List<String> pieces = new ArrayList<>();
        for (String field : afterMap.keySet()) {
            Object before = beforeMap.get(field);
            Object after = afterMap.get(field);
            if (!Objects.equals(before == null ? null : String.valueOf(before), after == null ? null : String.valueOf(after))) {
                pieces.add(fieldLabel(field));
            }
        }
        return pieces.isEmpty() ? "资料无变化" : String.join("、", pieces);
    }

    private String fieldLabel(String field) {
        if ("mobile".equals(field)) {
            return "手机号";
        }
        if ("email".equals(field)) {
            return "邮箱";
        }
        if ("address".equals(field)) {
            return "现住址";
        }
        if ("emergencyContact".equals(field)) {
            return "紧急联系人";
        }
        if ("emergencyPhone".equals(field)) {
            return "紧急联系电话";
        }
        if ("emergencyRelation".equals(field)) {
            return "紧急联系人关系";
        }
        if ("maritalStatus".equals(field)) {
            return "婚姻状况";
        }
        if ("hometown".equals(field)) {
            return "户籍所在地";
        }
        return field;
    }

    private void normalizePageReq(EmployeeProfileChangePageReqVO pageReqVO) {
        if (StringUtils.hasText(pageReqVO.getChangeType())) {
            pageReqVO.setChangeType(pageReqVO.getChangeType().trim().toUpperCase());
        }
        if (StringUtils.hasText(pageReqVO.getStatus())) {
            pageReqVO.setStatus(pageReqVO.getStatus().trim().toUpperCase());
        }
    }

    private boolean canManage() {
        try {
            return securityFrameworkService.hasPermission(PERMISSION_APPROVE);
        } catch (Exception ex) {
            log.warn("check profile change approve permission failed: {}", ex.getMessage());
            return false;
        }
    }

    private void fillPeopleInfo(List<EmployeeProfileChangeRequestDO> rows, List<EmployeeProfileChangeRespVO> respList) {
        Set<Long> userIds = new HashSet<>();
        Set<Long> profileIds = new HashSet<>();
        for (EmployeeProfileChangeRequestDO row : rows) {
            if (row.getUserId() != null) {
                userIds.add(row.getUserId());
            }
            if (row.getApproverId() != null) {
                userIds.add(row.getApproverId());
            }
            if (row.getProfileId() != null) {
                profileIds.add(row.getProfileId());
            }
        }
        Map<Long, AdminUserRespDTO> userMap = loadUserMapSafe(userIds);
        Map<Long, EmployeeProfileDO> profileMap = loadProfileMapSafe(profileIds);
        for (EmployeeProfileChangeRespVO item : respList) {
            AdminUserRespDTO user = userMap.get(item.getUserId());
            if (user != null) {
                item.setUserNickname(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
            }
            AdminUserRespDTO approver = userMap.get(item.getApproverId());
            if (approver != null) {
                item.setApproverName(StringUtils.hasText(approver.getNickname()) ? approver.getNickname() : approver.getUsername());
            }
            EmployeeProfileDO profile = profileMap.get(item.getProfileId());
            if (profile != null) {
                item.setProfileName(profile.getName());
            }
        }
    }

    private Map<Long, AdminUserRespDTO> loadUserMapSafe(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return adminUserApi.getUserMap(userIds);
        } catch (Exception ex) {
            log.warn("Failed to load admin users for profile change page: {}", ex.getMessage());
            return new HashMap<>();
        }
    }

    private Map<Long, EmployeeProfileDO> loadProfileMapSafe(Set<Long> profileIds) {
        Map<Long, EmployeeProfileDO> profileMap = new HashMap<>();
        if (profileIds == null || profileIds.isEmpty()) {
            return profileMap;
        }
        List<EmployeeProfileDO> profiles = employeeProfileMapper.selectList(
                new LambdaQueryWrapperX<EmployeeProfileDO>().in(EmployeeProfileDO::getId, profileIds));
        if (profiles == null) {
            return profileMap;
        }
        for (EmployeeProfileDO profile : profiles) {
            if (profile.getId() != null) {
                profileMap.put(profile.getId(), profile);
            }
        }
        return profileMap;
    }

}
