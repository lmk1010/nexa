package com.kyx.service.hr.service.employee;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.kyx.foundation.common.exception.enums.GlobalErrorCodeConstants;
import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.foundation.security.core.util.SecurityFrameworkUtils;
import com.kyx.service.bpm.api.task.BpmProcessInstanceApi;
import com.kyx.service.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.kyx.service.bpm.enums.task.BpmProcessInstanceStatusEnum;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePerformanceAdvanceReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePerformanceApprovalReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePerformancePageReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePerformanceRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePerformanceSchemePageReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePerformanceSchemeRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePerformanceSchemeSaveReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePerformanceSaveReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePerformanceStatsRespVO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeProfileDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeePerformanceDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeePerformanceSchemeDO;
import com.kyx.service.hr.dal.mysql.employee.EmployeeProfileMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeePerformanceMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeePerformanceSchemeMapper;
import com.kyx.service.hr.enums.ErrorCodeConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Validated
@Slf4j
public class EmployeePerformanceServiceImpl implements EmployeePerformanceService {

    public static final String PROCESS_KEY = "hr_performance_approval";

    private static final String STATUS_UNSET = "UNSET";
    private static final String STATUS_GOAL_SETTING = "GOAL_SETTING";
    private static final String STATUS_SELF_REVIEW = "SELF_REVIEW";
    private static final String STATUS_MANAGER_REVIEW = "MANAGER_REVIEW";
    private static final String STATUS_CALIBRATION = "CALIBRATION";
    private static final String STATUS_INTERVIEW = "INTERVIEW";
    private static final String STATUS_FOLLOW_UP = "FOLLOW_UP";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final String APPLICATION_STATUS_PENDING = "PENDING";
    private static final String APPLICATION_STATUS_APPLIED = "APPLIED";
    private static final String APPLICATION_STATUS_SKIPPED = "SKIPPED";
    private static final String APPROVAL_STATUS_DRAFT = "DRAFT";
    private static final String APPROVAL_STATUS_SUBMITTED = "SUBMITTED";
    private static final String APPROVAL_STATUS_APPROVED = "APPROVED";
    private static final String APPROVAL_STATUS_REJECTED = "REJECTED";
    private static final String SCHEME_STATUS_ACTIVE = "ACTIVE";
    private static final String SCHEME_STATUS_DISABLED = "DISABLED";
    private static final String SCHEME_TYPE_KPI = "KPI";
    private static final String SCHEME_TYPE_OKR = "OKR";
    private static final String SCHEME_TYPE_MBO = "MBO";
    private static final String SCHEME_TYPE_360 = "360";
    private static final String SCHEME_TYPE_MIXED = "MIXED";
    private static final String SCHEME_CYCLE_MONTHLY = "MONTHLY";
    private static final String SCHEME_CYCLE_QUARTERLY = "QUARTERLY";
    private static final String SCHEME_CYCLE_HALF_YEAR = "HALF_YEAR";
    private static final String SCHEME_CYCLE_YEARLY = "YEARLY";
    private static final String SCHEME_CYCLE_PROJECT = "PROJECT";
    private static final String[] STATUS_ORDER = new String[] {
            STATUS_GOAL_SETTING,
            STATUS_SELF_REVIEW,
            STATUS_MANAGER_REVIEW,
            STATUS_CALIBRATION,
            STATUS_INTERVIEW,
            STATUS_FOLLOW_UP,
            STATUS_CLOSED
    };

    @Resource
    private EmployeePerformanceMapper employeePerformanceMapper;
    @Resource
    private EmployeeProfileMapper employeeProfileMapper;
    @Resource
    private EmployeePerformanceSchemeMapper employeePerformanceSchemeMapper;
    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    @Override
    public List<EmployeePerformanceRespVO> getPerformanceList(Long profileId) {
        List<EmployeePerformanceDO> list = employeePerformanceMapper.selectListByProfileId(profileId);
        List<EmployeePerformanceRespVO> respList = BeanUtils.toBean(list, EmployeePerformanceRespVO.class);
        fillProfileInfo(respList);
        fillSchemeInfo(respList);
        return respList;
    }

    @Override
    public PageResult<EmployeePerformanceRespVO> getPerformancePage(EmployeePerformancePageReqVO pageReqVO) {
        if (!prepareProfileFilter(pageReqVO)) {
            return new PageResult<>(new ArrayList<>(), 0L);
        }
        PageResult<EmployeePerformanceDO> pageResult = employeePerformanceMapper.selectPage(pageReqVO);
        List<EmployeePerformanceRespVO> respList = BeanUtils.toBean(pageResult.getList(), EmployeePerformanceRespVO.class);
        fillProfileInfo(respList);
        fillSchemeInfo(respList);
        return new PageResult<>(respList, pageResult.getTotal());
    }

    @Override
    public EmployeePerformanceStatsRespVO getPerformanceStats(EmployeePerformancePageReqVO pageReqVO) {
        EmployeePerformanceStatsRespVO stats = new EmployeePerformanceStatsRespVO();
        if (!prepareProfileFilter(pageReqVO)) {
            return stats;
        }
        List<EmployeePerformanceDO> list = employeePerformanceMapper.selectListByReq(pageReqVO, 5000);
        stats.setTotalCount(list.size());
        stats.setAvgScore(calculateAvgScore(list));
        stats.setExcellentCount((int) list.stream().filter(this::isExcellent).count());
        stats.setWarningCount((int) list.stream().filter(this::needsAttention).count());
        stats.setRecentCount((int) list.stream().filter(this::isRecent).count());
        stats.setPendingCount((int) list.stream().filter(item -> !isCycleClosed(item)).count());
        stats.setClosedCount((int) list.stream().filter(this::isCycleClosed).count());
        stats.setInterviewScheduledCount((int) list.stream().filter(item -> item.getInterviewTime() != null).count());
        stats.setFollowUpCount((int) list.stream().filter(item -> item.getNextFollowTime() != null).count());
        stats.setApplicationPendingCount((int) list.stream().filter(this::isApplicationPending).count());
        stats.setApplicationAppliedCount((int) list.stream().filter(this::isApplicationApplied).count());
        stats.setApprovalSubmittedCount((int) list.stream().filter(this::isApprovalSubmitted).count());
        stats.setApprovalApprovedCount((int) list.stream().filter(this::isApprovalApproved).count());
        stats.setApprovalRejectedCount((int) list.stream().filter(this::isApprovalRejected).count());
        stats.setStatusStats(groupStatusStats(list));
        stats.setGradeStats(groupStats(list, EmployeePerformanceDO::getGrade));
        stats.setResultStats(groupStats(list, EmployeePerformanceDO::getResult));
        stats.setApplicationStatusStats(groupStats(list, this::resolveApplicationStatus));
        stats.setApplicationTypeStats(groupStats(list, EmployeePerformanceDO::getApplicationType));
        stats.setApprovalStatusStats(groupStats(list, this::resolveApprovalStatus));
        return stats;
    }

    @Override
    public Long createPerformance(EmployeePerformanceSaveReqVO createReqVO) {
        validateProfileExists(createReqVO.getProfileId());
        EmployeePerformanceDO performance = BeanUtils.toBean(createReqVO, EmployeePerformanceDO.class);
        applySchemeSnapshot(performance, true);
        resetApprovalState(performance);
        preparePerformance(performance);
        employeePerformanceMapper.insert(performance);
        return performance.getId();
    }

    @Override
    public void updatePerformance(EmployeePerformanceSaveReqVO updateReqVO) {
        EmployeePerformanceDO existing = updateReqVO.getId() == null ? null : employeePerformanceMapper.selectById(updateReqVO.getId());
        if (existing == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_PROFILE_NOT_EXISTS);
        }
        validateProfileExists(updateReqVO.getProfileId());
        EmployeePerformanceDO performance = BeanUtils.toBean(updateReqVO, EmployeePerformanceDO.class);
        applySchemeSnapshot(performance, false);
        if (performance.getSchemeId() == null && existing.getSchemeId() != null) {
            performance.setSchemeId(existing.getSchemeId());
            performance.setSchemeCode(existing.getSchemeCode());
            performance.setSchemeName(existing.getSchemeName());
            performance.setSchemeType(existing.getSchemeType());
            performance.setCycleType(existing.getCycleType());
        }
        validatePerformanceUpdateAllowed(existing, performance);
        preserveApprovalState(performance, existing);
        preparePerformance(performance);
        employeePerformanceMapper.updateById(performance);
    }

    @Override
    public void advancePerformance(EmployeePerformanceAdvanceReqVO advanceReqVO) {
        EmployeePerformanceDO performance = employeePerformanceMapper.selectById(advanceReqVO.getId());
        if (performance == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_PROFILE_NOT_EXISTS);
        }
        validateCanAdvancePerformance(performance);
        String nextStatus = nextCycleStatus(performance.getCycleStatus());
        validatePerformanceStepReady(performance, nextStatus);
        performance.setCycleStatus(nextStatus);
        if (STATUS_INTERVIEW.equals(nextStatus) && performance.getInterviewTime() == null) {
            performance.setInterviewTime(LocalDateTime.now());
        }
        if (STATUS_FOLLOW_UP.equals(nextStatus) && performance.getNextFollowTime() == null) {
            performance.setNextFollowTime(LocalDateTime.now().plusDays(7));
        }
        employeePerformanceMapper.updateById(performance);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitPerformance(EmployeePerformanceApprovalReqVO approvalReqVO) {
        EmployeePerformanceDO performance = getPerformance(approvalReqVO.getId());
        String approvalStatus = resolveApprovalStatus(performance);
        if (APPROVAL_STATUS_APPROVED.equals(approvalStatus)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "已审批通过的绩效记录不能重复提交");
        }
        if (APPROVAL_STATUS_SUBMITTED.equals(approvalStatus)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "绩效记录已在审批中，不能重复提交");
        }
        validatePerformanceReadyForApproval(performance);
        EmployeeProfileDO profile = getProfile(performance.getProfileId());
        String processInstanceId = startPerformanceApprovalProcess(performance, profile, approvalReqVO);
        employeePerformanceMapper.update(null, new LambdaUpdateWrapper<EmployeePerformanceDO>()
                .eq(EmployeePerformanceDO::getId, performance.getId())
                .set(EmployeePerformanceDO::getApprovalStatus, APPROVAL_STATUS_SUBMITTED)
                .set(EmployeePerformanceDO::getProcessInstanceId, processInstanceId)
                .set(EmployeePerformanceDO::getSubmittedTime,
                        performance.getSubmittedTime() == null ? LocalDateTime.now() : performance.getSubmittedTime())
                .set(EmployeePerformanceDO::getApprovedBy, null)
                .set(EmployeePerformanceDO::getApprovedTime, null)
                .set(EmployeePerformanceDO::getApprovalRemark, trimToNull(approvalReqVO.getApprovalRemark())));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approvePerformance(EmployeePerformanceApprovalReqVO approvalReqVO) {
        EmployeePerformanceDO performance = getPerformance(approvalReqVO.getId());
        validatePerformanceApprovalPending(performance, "只有待审批绩效记录可以通过");
        validateLocalApprovalAllowed(performance);
        performance.setApprovalStatus(APPROVAL_STATUS_APPROVED);
        if (performance.getSubmittedTime() == null) {
            performance.setSubmittedTime(LocalDateTime.now());
        }
        performance.setApprovedBy(SecurityFrameworkUtils.getLoginUserId());
        performance.setApprovedTime(LocalDateTime.now());
        performance.setApprovalRemark(trimToNull(approvalReqVO.getApprovalRemark()));
        performance.setCycleStatus(STATUS_CLOSED);
        employeePerformanceMapper.updateById(performance);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectPerformance(EmployeePerformanceApprovalReqVO approvalReqVO) {
        EmployeePerformanceDO performance = getPerformance(approvalReqVO.getId());
        validatePerformanceApprovalPending(performance, "只有待审批绩效记录可以驳回");
        validateLocalApprovalAllowed(performance);
        performance.setApprovalStatus(APPROVAL_STATUS_REJECTED);
        if (performance.getSubmittedTime() == null) {
            performance.setSubmittedTime(LocalDateTime.now());
        }
        performance.setApprovedBy(SecurityFrameworkUtils.getLoginUserId());
        performance.setApprovedTime(LocalDateTime.now());
        performance.setApprovalRemark(trimToNull(approvalReqVO.getApprovalRemark()));
        employeePerformanceMapper.updateById(performance);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateApprovalStatusByBpmEvent(Long id, String processInstanceId, Integer bpmStatus, Long operatorUserId) {
        EmployeePerformanceDO performance = employeePerformanceMapper.selectById(id);
        if (performance == null) {
            log.warn("绩效审批 BPM 回调记录不存在，id={}, processInstanceId={}, bpmStatus={}",
                    id, processInstanceId, bpmStatus);
            return;
        }
        if (StringUtils.hasText(performance.getProcessInstanceId())
                && StringUtils.hasText(processInstanceId)
                && !Objects.equals(performance.getProcessInstanceId(), processInstanceId)) {
            log.warn("绩效审批 BPM 回调流程实例不匹配，id={}, processInstanceId={}, currentProcessInstanceId={}",
                    id, processInstanceId, performance.getProcessInstanceId());
            return;
        }
        if (Objects.equals(bpmStatus, BpmProcessInstanceStatusEnum.APPROVE.getStatus())) {
            approveByBpm(performance, processInstanceId, operatorUserId);
            return;
        }
        if (Objects.equals(bpmStatus, BpmProcessInstanceStatusEnum.REJECT.getStatus())) {
            closeByBpm(performance, APPROVAL_STATUS_REJECTED, processInstanceId, operatorUserId, "BPM审批驳回");
            return;
        }
        if (Objects.equals(bpmStatus, BpmProcessInstanceStatusEnum.CANCEL.getStatus())) {
            closeByBpm(performance, APPROVAL_STATUS_DRAFT, processInstanceId, operatorUserId, "BPM流程撤销");
        }
    }

    private String startPerformanceApprovalProcess(EmployeePerformanceDO performance, EmployeeProfileDO profile,
                                                   EmployeePerformanceApprovalReqVO approvalReqVO) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.UNAUTHORIZED, "绩效审批发起人不能为空");
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("performanceId", performance.getId());
        variables.put("profileId", performance.getProfileId());
        variables.put("employeeName", profile.getName());
        variables.put("period", performance.getPeriod());
        variables.put("schemeId", performance.getSchemeId());
        variables.put("schemeName", performance.getSchemeName());
        variables.put("cycleStatus", currentCycleStatus(performance.getCycleStatus()));
        variables.put("score", performance.getScore());
        variables.put("grade", performance.getGrade());
        variables.put("result", performance.getResult());
        variables.put("evaluatedDate", performance.getEvaluatedDate());
        variables.put("applicationType", performance.getApplicationType());
        variables.put("applicationStatus", resolveApplicationStatus(performance));
        variables.put("approvalRemark", trimToNull(approvalReqVO.getApprovalRemark()));
        return processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO()
                        .setProcessDefinitionKey(PROCESS_KEY)
                        .setBusinessKey(String.valueOf(performance.getId()))
                        .setVariables(variables))
                .getCheckedData();
    }

    private void approveByBpm(EmployeePerformanceDO performance, String processInstanceId, Long operatorUserId) {
        if (APPROVAL_STATUS_APPROVED.equals(resolveApprovalStatus(performance))) {
            return;
        }
        if (!APPROVAL_STATUS_SUBMITTED.equals(resolveApprovalStatus(performance))) {
            log.warn("绩效审批 BPM 通过回调忽略非待审批记录，id={}, status={}",
                    performance.getId(), performance.getApprovalStatus());
            return;
        }
        validatePerformanceReadyForApproval(performance);
        EmployeePerformanceDO updateDO = new EmployeePerformanceDO();
        updateDO.setId(performance.getId());
        updateDO.setApprovalStatus(APPROVAL_STATUS_APPROVED);
        updateDO.setProcessInstanceId(StringUtils.hasText(processInstanceId)
                ? processInstanceId : performance.getProcessInstanceId());
        if (performance.getSubmittedTime() == null) {
            updateDO.setSubmittedTime(LocalDateTime.now());
        }
        updateDO.setApprovedBy(operatorUserId);
        updateDO.setApprovedTime(LocalDateTime.now());
        updateDO.setApprovalRemark("BPM审批通过");
        updateDO.setCycleStatus(STATUS_CLOSED);
        employeePerformanceMapper.updateById(updateDO);
    }

    private void closeByBpm(EmployeePerformanceDO performance, String approvalStatus,
                            String processInstanceId, Long operatorUserId, String remark) {
        if (approvalStatus.equals(resolveApprovalStatus(performance))) {
            return;
        }
        if (!APPROVAL_STATUS_SUBMITTED.equals(resolveApprovalStatus(performance))) {
            log.warn("绩效审批 BPM 关闭回调忽略非待审批记录，id={}, status={}, targetStatus={}",
                    performance.getId(), performance.getApprovalStatus(), approvalStatus);
            return;
        }
        EmployeePerformanceDO updateDO = new EmployeePerformanceDO();
        updateDO.setId(performance.getId());
        updateDO.setApprovalStatus(approvalStatus);
        updateDO.setProcessInstanceId(StringUtils.hasText(processInstanceId)
                ? processInstanceId : performance.getProcessInstanceId());
        if (APPROVAL_STATUS_DRAFT.equals(approvalStatus)) {
            employeePerformanceMapper.update(null, new LambdaUpdateWrapper<EmployeePerformanceDO>()
                    .eq(EmployeePerformanceDO::getId, performance.getId())
                    .set(EmployeePerformanceDO::getApprovalStatus, APPROVAL_STATUS_DRAFT)
                    .set(EmployeePerformanceDO::getProcessInstanceId, updateDO.getProcessInstanceId())
                    .set(EmployeePerformanceDO::getSubmittedTime, null)
                    .set(EmployeePerformanceDO::getApprovedBy, null)
                    .set(EmployeePerformanceDO::getApprovedTime, null)
                    .set(EmployeePerformanceDO::getApprovalRemark, remark));
            return;
        }
        if (performance.getSubmittedTime() == null) {
            updateDO.setSubmittedTime(LocalDateTime.now());
        }
        updateDO.setApprovedBy(operatorUserId);
        updateDO.setApprovedTime(LocalDateTime.now());
        updateDO.setApprovalRemark(remark);
        employeePerformanceMapper.updateById(updateDO);
    }

    @Override
    public void deletePerformance(Long id) {
        EmployeePerformanceDO performance = employeePerformanceMapper.selectById(id);
        if (performance == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_PROFILE_NOT_EXISTS);
        }
        if (APPROVAL_STATUS_SUBMITTED.equals(resolveApprovalStatus(performance))
                || APPROVAL_STATUS_APPROVED.equals(resolveApprovalStatus(performance))
                || STATUS_CLOSED.equals(currentCycleStatus(performance.getCycleStatus()))) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "已提交、已通过或已闭环绩效记录不能删除");
        }
        employeePerformanceMapper.deleteById(id);
    }

    @Override
    public PageResult<EmployeePerformanceSchemeRespVO> getSchemePage(EmployeePerformanceSchemePageReqVO pageReqVO) {
        normalizeSchemePageReq(pageReqVO);
        PageResult<EmployeePerformanceSchemeDO> pageResult = employeePerformanceSchemeMapper.selectPage(pageReqVO);
        List<EmployeePerformanceSchemeRespVO> respList = BeanUtils.toBean(pageResult.getList(),
                EmployeePerformanceSchemeRespVO.class);
        return new PageResult<>(respList, pageResult.getTotal());
    }

    @Override
    public List<EmployeePerformanceSchemeRespVO> getActiveSchemeList() {
        List<EmployeePerformanceSchemeDO> list = employeePerformanceSchemeMapper.selectActiveList();
        return BeanUtils.toBean(list, EmployeePerformanceSchemeRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveScheme(EmployeePerformanceSchemeSaveReqVO reqVO) {
        EmployeePerformanceSchemeDO scheme = BeanUtils.toBean(reqVO, EmployeePerformanceSchemeDO.class);
        normalizeScheme(scheme);
        if (Boolean.TRUE.equals(scheme.getDefaultFlag()) && SCHEME_STATUS_ACTIVE.equals(scheme.getStatus())) {
            clearOtherDefaultSchemes(scheme.getId());
        }
        if (scheme.getId() == null) {
            employeePerformanceSchemeMapper.insert(scheme);
        } else {
            if (employeePerformanceSchemeMapper.selectById(scheme.getId()) == null) {
                throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND, "绩效方案不存在");
            }
            employeePerformanceSchemeMapper.updateById(scheme);
        }
        return scheme.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean enableScheme(Long id) {
        EmployeePerformanceSchemeDO scheme = getScheme(id);
        EmployeePerformanceSchemeDO updateDO = new EmployeePerformanceSchemeDO();
        updateDO.setId(scheme.getId());
        updateDO.setStatus(SCHEME_STATUS_ACTIVE);
        updateDO.setDefaultFlag(true);
        clearOtherDefaultSchemes(scheme.getId());
        employeePerformanceSchemeMapper.updateById(updateDO);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteScheme(Long id) {
        EmployeePerformanceSchemeDO scheme = getScheme(id);
        if (SCHEME_STATUS_ACTIVE.equals(scheme.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "启用中的绩效方案不能删除，请先停用");
        }
        employeePerformanceSchemeMapper.deleteById(id);
    }

    private void validateProfileExists(Long profileId) {
        if (profileId == null || employeeProfileMapper.selectById(profileId) == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_PROFILE_NOT_EXISTS);
        }
    }

    private EmployeeProfileDO getProfile(Long profileId) {
        EmployeeProfileDO profile = profileId == null ? null : employeeProfileMapper.selectById(profileId);
        if (profile == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_PROFILE_NOT_EXISTS);
        }
        return profile;
    }

    private boolean prepareProfileFilter(EmployeePerformancePageReqVO reqVO) {
        if (!StringUtils.hasText(reqVO.getProfileName()) && !StringUtils.hasText(reqVO.getProfileMobile())) {
            return true;
        }
        List<EmployeeProfileDO> profiles = employeeProfileMapper.selectList(new LambdaQueryWrapperX<EmployeeProfileDO>()
                .likeIfPresent(EmployeeProfileDO::getName, reqVO.getProfileName())
                .likeIfPresent(EmployeeProfileDO::getMobile, reqVO.getProfileMobile())
                .last("LIMIT 1000"));
        Set<Long> profileIds = profiles.stream()
                .map(EmployeeProfileDO::getId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(HashSet::new));
        if (profileIds.isEmpty()) {
            return false;
        }
        reqVO.setProfileIds(profileIds);
        return true;
    }

    private void fillProfileInfo(List<EmployeePerformanceRespVO> respList) {
        if (respList == null || respList.isEmpty()) {
            return;
        }
        Set<Long> profileIds = respList.stream()
                .map(EmployeePerformanceRespVO::getProfileId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(HashSet::new));
        if (profileIds.isEmpty()) {
            return;
        }
        Map<Long, EmployeeProfileDO> profileMap = employeeProfileMapper.selectBatchIds(profileIds).stream()
                .collect(Collectors.toMap(EmployeeProfileDO::getId, item -> item, (left, right) -> left));
        for (EmployeePerformanceRespVO item : respList) {
            EmployeeProfileDO profile = profileMap.get(item.getProfileId());
            if (profile == null) {
                continue;
            }
            item.setProfileName(profile.getName());
            item.setProfileMobile(profile.getMobile());
        }
    }

    private void fillSchemeInfo(List<EmployeePerformanceRespVO> respList) {
        if (respList == null || respList.isEmpty()) {
            return;
        }
        Set<Long> schemeIds = respList.stream()
                .map(EmployeePerformanceRespVO::getSchemeId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(HashSet::new));
        if (schemeIds.isEmpty()) {
            return;
        }
        Map<Long, EmployeePerformanceSchemeDO> schemeMap = employeePerformanceSchemeMapper.selectBatchIds(schemeIds).stream()
                .collect(Collectors.toMap(EmployeePerformanceSchemeDO::getId, item -> item, (left, right) -> left));
        for (EmployeePerformanceRespVO item : respList) {
            Long schemeId = item.getSchemeId();
            if (schemeId == null) {
                continue;
            }
            EmployeePerformanceSchemeDO scheme = schemeMap.get(schemeId);
            if (scheme == null) {
                continue;
            }
            if (!StringUtils.hasText(item.getSchemeCode())) {
                item.setSchemeCode(scheme.getSchemeCode());
            }
            if (!StringUtils.hasText(item.getSchemeName())) {
                item.setSchemeName(scheme.getSchemeName());
            }
            if (!StringUtils.hasText(item.getSchemeType())) {
                item.setSchemeType(scheme.getSchemeType());
            }
            if (!StringUtils.hasText(item.getCycleType())) {
                item.setCycleType(scheme.getCycleType());
            }
        }
    }

    private BigDecimal calculateAvgScore(List<EmployeePerformanceDO> list) {
        List<BigDecimal> scores = list.stream()
                .map(EmployeePerformanceDO::getScore)
                .filter(score -> score != null)
                .collect(Collectors.toList());
        if (scores.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
    }

    private List<EmployeePerformanceStatsRespVO.StatItem> groupStatusStats(List<EmployeePerformanceDO> list) {
        Map<String, Integer> counts = new HashMap<>();
        for (EmployeePerformanceDO item : list) {
            String code = currentCycleStatus(item.getCycleStatus());
            counts.put(code, counts.getOrDefault(code, 0) + 1);
        }
        return counts.entrySet().stream()
                .map(entry -> new EmployeePerformanceStatsRespVO.StatItem(
                        entry.getKey(), cycleStatusLabel(entry.getKey()), entry.getValue()))
                .sorted(Comparator.comparingInt(item -> cycleStatusRank(item.getCode())))
                .collect(Collectors.toList());
    }

    private List<EmployeePerformanceStatsRespVO.StatItem> groupStats(
            List<EmployeePerformanceDO> list,
            java.util.function.Function<EmployeePerformanceDO, String> classifier) {
        Map<String, Integer> counts = new HashMap<>();
        for (EmployeePerformanceDO item : list) {
            String name = classifier.apply(item);
            if (!StringUtils.hasText(name)) {
                name = "未填写";
            }
            counts.put(name, counts.getOrDefault(name, 0) + 1);
        }
        return counts.entrySet().stream()
                .map(entry -> new EmployeePerformanceStatsRespVO.StatItem(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(EmployeePerformanceStatsRespVO.StatItem::getCount).reversed())
                .collect(Collectors.toList());
    }

    private boolean isExcellent(EmployeePerformanceDO item) {
        return scoreGte(item, 90)
                || containsAny(item.getGrade(), "A", "优秀", "卓越")
                || containsAny(item.getResult(), "优秀", "卓越");
    }

    private boolean needsAttention(EmployeePerformanceDO item) {
        return scoreLt(item, 60)
                || containsAny(item.getGrade(), "C", "D", "待改进", "不合格")
                || containsAny(item.getResult(), "待改进", "不合格", "较差");
    }

    private boolean isRecent(EmployeePerformanceDO item) {
        return item.getEvaluatedDate() != null && !item.getEvaluatedDate().isBefore(LocalDate.now().minusDays(30));
    }

    private boolean isCycleClosed(EmployeePerformanceDO item) {
        return STATUS_CLOSED.equals(currentCycleStatus(item.getCycleStatus()));
    }

    private boolean isApplicationPending(EmployeePerformanceDO item) {
        return APPLICATION_STATUS_PENDING.equals(resolveApplicationStatus(item));
    }

    private boolean isApplicationApplied(EmployeePerformanceDO item) {
        return APPLICATION_STATUS_APPLIED.equals(resolveApplicationStatus(item));
    }

    private boolean isApprovalSubmitted(EmployeePerformanceDO item) {
        return APPROVAL_STATUS_SUBMITTED.equals(resolveApprovalStatus(item));
    }

    private boolean isApprovalApproved(EmployeePerformanceDO item) {
        return APPROVAL_STATUS_APPROVED.equals(resolveApprovalStatus(item));
    }

    private boolean isApprovalRejected(EmployeePerformanceDO item) {
        return APPROVAL_STATUS_REJECTED.equals(resolveApprovalStatus(item));
    }

    private String resolveApplicationStatus(EmployeePerformanceDO item) {
        String status = trimToNull(item.getApplicationStatus());
        if (status != null) {
            return status;
        }
        return hasApplicationCandidate(item) ? APPLICATION_STATUS_PENDING : null;
    }

    private String resolveApprovalStatus(EmployeePerformanceDO item) {
        String status = trimToNull(item.getApprovalStatus());
        return status == null ? APPROVAL_STATUS_DRAFT : status;
    }

    private boolean hasApplicationCandidate(EmployeePerformanceDO item) {
        return StringUtils.hasText(item.getApplicationType())
                || StringUtils.hasText(item.getGrade())
                || StringUtils.hasText(item.getResult())
                || item.getScore() != null
                || isCycleClosed(item);
    }

    private boolean scoreGte(EmployeePerformanceDO item, int threshold) {
        return item.getScore() != null && item.getScore().compareTo(BigDecimal.valueOf(threshold)) >= 0;
    }

    private boolean scoreLt(EmployeePerformanceDO item, int threshold) {
        return item.getScore() != null && item.getScore().compareTo(BigDecimal.valueOf(threshold)) < 0;
    }

    private boolean containsAny(String value, String... keywords) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private void preparePerformance(EmployeePerformanceDO performance) {
        performance.setSchemeCode(trimToNull(performance.getSchemeCode()));
        performance.setSchemeName(trimToNull(performance.getSchemeName()));
        performance.setSchemeType(trimToNull(performance.getSchemeType()));
        performance.setCycleType(trimToNull(performance.getCycleType()));
        performance.setPeriod(trimToNull(performance.getPeriod()));
        performance.setGrade(trimToNull(performance.getGrade()));
        performance.setResult(trimToNull(performance.getResult()));
        performance.setCycleStatus(defaultCycleStatus(performance.getCycleStatus()));
        performance.setGoalContent(trimToNull(performance.getGoalContent()));
        performance.setSelfReview(trimToNull(performance.getSelfReview()));
        performance.setManagerReview(trimToNull(performance.getManagerReview()));
        performance.setCalibrationResult(trimToNull(performance.getCalibrationResult()));
        performance.setApplicationType(trimToNull(performance.getApplicationType()));
        performance.setApplicationStatus(defaultApplicationStatus(performance));
        performance.setApplicationRemark(trimToNull(performance.getApplicationRemark()));
        performance.setApprovalStatus(defaultApprovalStatus(performance.getApprovalStatus()));
        performance.setApprovalRemark(trimToNull(performance.getApprovalRemark()));
        if ((APPLICATION_STATUS_APPLIED.equals(performance.getApplicationStatus())
                || APPLICATION_STATUS_SKIPPED.equals(performance.getApplicationStatus()))
                && performance.getApplicationTime() == null) {
            performance.setApplicationTime(LocalDateTime.now());
        }
        performance.setRemark(trimToNull(performance.getRemark()));
    }

    private void preserveApprovalState(EmployeePerformanceDO performance, EmployeePerformanceDO existing) {
        performance.setApprovalStatus(existing.getApprovalStatus());
        performance.setProcessInstanceId(existing.getProcessInstanceId());
        performance.setSubmittedTime(existing.getSubmittedTime());
        performance.setApprovedBy(existing.getApprovedBy());
        performance.setApprovedTime(existing.getApprovedTime());
        performance.setApprovalRemark(existing.getApprovalRemark());
    }

    private void resetApprovalState(EmployeePerformanceDO performance) {
        performance.setApprovalStatus(APPROVAL_STATUS_DRAFT);
        performance.setProcessInstanceId(null);
        performance.setSubmittedTime(null);
        performance.setApprovedBy(null);
        performance.setApprovedTime(null);
        performance.setApprovalRemark(null);
    }

    private void validatePerformanceUpdateAllowed(EmployeePerformanceDO existing, EmployeePerformanceDO update) {
        String approvalStatus = resolveApprovalStatus(existing);
        if (APPROVAL_STATUS_SUBMITTED.equals(approvalStatus)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "审批中的绩效记录不能编辑，请先审批或驳回");
        }
        if (!APPROVAL_STATUS_APPROVED.equals(approvalStatus)) {
            if (APPROVAL_STATUS_REJECTED.equals(approvalStatus)
                    && APPLICATION_STATUS_APPLIED.equals(trimToNull(update.getApplicationStatus()))) {
                throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "已驳回绩效记录不能应用结果");
            }
            return;
        }
        if (hasProtectedPerformanceChange(existing, update)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "已审批通过的绩效记录只能更新结果应用信息");
        }
        if (!APPLICATION_STATUS_APPLIED.equals(trimToNull(update.getApplicationStatus()))
                && !Objects.equals(trimToNull(existing.getApplicationStatus()), trimToNull(update.getApplicationStatus()))) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "已审批通过的绩效记录只能标记结果应用完成");
        }
    }

    private boolean hasProtectedPerformanceChange(EmployeePerformanceDO existing, EmployeePerformanceDO update) {
        return !Objects.equals(existing.getProfileId(), update.getProfileId())
                || !Objects.equals(existing.getSchemeId(), update.getSchemeId())
                || !sameText(existing.getPeriod(), update.getPeriod())
                || !sameText(existing.getGrade(), update.getGrade())
                || !sameText(existing.getResult(), update.getResult())
                || !Objects.equals(existing.getScore(), update.getScore())
                || !Objects.equals(existing.getEvaluatedDate(), update.getEvaluatedDate())
                || !sameText(currentCycleStatus(existing.getCycleStatus()), currentCycleStatus(update.getCycleStatus()))
                || !sameText(existing.getGoalContent(), update.getGoalContent())
                || !sameText(existing.getSelfReview(), update.getSelfReview())
                || !sameText(existing.getManagerReview(), update.getManagerReview())
                || !sameText(existing.getCalibrationResult(), update.getCalibrationResult())
                || !Objects.equals(existing.getInterviewTime(), update.getInterviewTime())
                || !Objects.equals(existing.getNextFollowTime(), update.getNextFollowTime())
                || !sameText(existing.getRemark(), update.getRemark());
    }

    private boolean sameText(String left, String right) {
        return Objects.equals(trimToNull(left), trimToNull(right));
    }

    private void validateCanAdvancePerformance(EmployeePerformanceDO performance) {
        String approvalStatus = resolveApprovalStatus(performance);
        if (APPROVAL_STATUS_SUBMITTED.equals(approvalStatus)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "审批中的绩效记录不能推进周期");
        }
        if (APPROVAL_STATUS_APPROVED.equals(approvalStatus)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "已审批通过的绩效记录不能推进周期");
        }
        if (STATUS_CLOSED.equals(currentCycleStatus(performance.getCycleStatus()))) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "绩效周期已闭环，不能继续推进");
        }
    }

    private void validatePerformanceStepReady(EmployeePerformanceDO performance, String nextStatus) {
        if (STATUS_SELF_REVIEW.equals(nextStatus)) {
            requireText(performance.getGoalContent(), "请先填写绩效目标，再进入员工自评");
            return;
        }
        if (STATUS_MANAGER_REVIEW.equals(nextStatus)) {
            requireText(performance.getSelfReview(), "请先填写员工自评，再进入主管评价");
            return;
        }
        if (STATUS_CALIBRATION.equals(nextStatus)) {
            requireText(performance.getManagerReview(), "请先填写主管评价，再进入绩效校准");
            return;
        }
        if (STATUS_INTERVIEW.equals(nextStatus)) {
            requireScore(performance);
            requireText(performance.getResult(), "请先填写绩效结果，再进入面谈");
            return;
        }
        if (STATUS_FOLLOW_UP.equals(nextStatus)) {
            if (performance.getInterviewTime() == null) {
                throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "请先安排绩效面谈，再进入跟进");
            }
            return;
        }
        if (STATUS_CLOSED.equals(nextStatus)) {
            requireScore(performance);
            requireText(performance.getResult(), "请先填写绩效结果，再闭环绩效周期");
        }
    }

    private void validatePerformanceReadyForApproval(EmployeePerformanceDO performance) {
        requireScore(performance);
        requireText(performance.getResult(), "请先填写绩效结果，再提交审批");
        if (performance.getEvaluatedDate() == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "请先填写考核日期，再提交审批");
        }
    }

    private void validatePerformanceApprovalPending(EmployeePerformanceDO performance, String message) {
        if (!APPROVAL_STATUS_SUBMITTED.equals(resolveApprovalStatus(performance))) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, message);
        }
        validatePerformanceReadyForApproval(performance);
    }

    private void validateLocalApprovalAllowed(EmployeePerformanceDO performance) {
        if (StringUtils.hasText(performance.getProcessInstanceId())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "该绩效记录已进入 BPM 流程，请在流程中心处理审批");
        }
    }

    private void requireScore(EmployeePerformanceDO performance) {
        if (performance.getScore() == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "请先填写绩效得分");
        }
        if (performance.getScore().compareTo(BigDecimal.ZERO) < 0
                || performance.getScore().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "绩效得分只能在 0-100 之间");
        }
    }

    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, message);
        }
    }

    private void applySchemeSnapshot(EmployeePerformanceDO performance, boolean allowDefaultScheme) {
        EmployeePerformanceSchemeDO scheme;
        if (performance.getSchemeId() != null) {
            scheme = getScheme(performance.getSchemeId());
        } else if (allowDefaultScheme) {
            scheme = employeePerformanceSchemeMapper.selectDefaultActive(performance.getEvaluatedDate());
        } else {
            return;
        }
        if (scheme == null) {
            return;
        }
        performance.setSchemeId(scheme.getId());
        performance.setSchemeCode(scheme.getSchemeCode());
        performance.setSchemeName(scheme.getSchemeName());
        performance.setSchemeType(scheme.getSchemeType());
        performance.setCycleType(scheme.getCycleType());
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private void normalizeSchemePageReq(EmployeePerformanceSchemePageReqVO reqVO) {
        if (StringUtils.hasText(reqVO.getSchemeCode())) {
            reqVO.setSchemeCode(reqVO.getSchemeCode().trim());
        }
        if (StringUtils.hasText(reqVO.getSchemeName())) {
            reqVO.setSchemeName(reqVO.getSchemeName().trim());
        }
        if (StringUtils.hasText(reqVO.getSchemeType())) {
            reqVO.setSchemeType(reqVO.getSchemeType().trim().toUpperCase());
        }
        if (StringUtils.hasText(reqVO.getCycleType())) {
            reqVO.setCycleType(reqVO.getCycleType().trim().toUpperCase());
        }
        if (StringUtils.hasText(reqVO.getStatus())) {
            reqVO.setStatus(reqVO.getStatus().trim().toUpperCase());
        }
    }

    private void normalizeScheme(EmployeePerformanceSchemeDO scheme) {
        scheme.setSchemeName(trimToNull(scheme.getSchemeName()));
        if (!StringUtils.hasText(scheme.getSchemeName())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "绩效方案名称不能为空");
        }
        if (!StringUtils.hasText(scheme.getSchemeCode())) {
            scheme.setSchemeCode("PERF-SCHEME-" + System.currentTimeMillis());
        } else {
            scheme.setSchemeCode(scheme.getSchemeCode().trim());
        }
        scheme.setSchemeType(normalizeSchemeType(scheme.getSchemeType()));
        scheme.setCycleType(normalizeSchemeCycleType(scheme.getCycleType()));
        scheme.setStatus(normalizeSchemeStatus(scheme.getStatus()));
        scheme.setDefaultFlag(Boolean.TRUE.equals(scheme.getDefaultFlag()));
        scheme.setTemplateJson(defaultText(trimToNull(scheme.getTemplateJson()),
                defaultTemplateJson(scheme.getSchemeType(), scheme.getCycleType())));
        scheme.setReviewFlowJson(defaultText(trimToNull(scheme.getReviewFlowJson()),
                defaultReviewFlowJson(scheme.getSchemeType())));
        if (scheme.getEffectiveDate() == null) {
            scheme.setEffectiveDate(LocalDate.now());
        }
        if (scheme.getExpireDate() != null && scheme.getExpireDate().isBefore(scheme.getEffectiveDate())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "失效日期不能早于生效日期");
        }
        scheme.setRemark(trimToNull(scheme.getRemark()));
    }

    private String normalizeSchemeType(String value) {
        String type = trimToNull(value);
        if (type == null) {
            return SCHEME_TYPE_KPI;
        }
        type = type.toUpperCase();
        if (SCHEME_TYPE_KPI.equals(type)
                || SCHEME_TYPE_OKR.equals(type)
                || SCHEME_TYPE_MBO.equals(type)
                || SCHEME_TYPE_360.equals(type)
                || SCHEME_TYPE_MIXED.equals(type)) {
            return type;
        }
        throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "绩效方案类型不合法");
    }

    private String normalizeSchemeCycleType(String value) {
        String cycleType = trimToNull(value);
        if (cycleType == null) {
            return SCHEME_CYCLE_QUARTERLY;
        }
        cycleType = cycleType.toUpperCase();
        if (SCHEME_CYCLE_MONTHLY.equals(cycleType)
                || SCHEME_CYCLE_QUARTERLY.equals(cycleType)
                || SCHEME_CYCLE_HALF_YEAR.equals(cycleType)
                || SCHEME_CYCLE_YEARLY.equals(cycleType)
                || SCHEME_CYCLE_PROJECT.equals(cycleType)) {
            return cycleType;
        }
        throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "绩效周期类型不合法");
    }

    private String normalizeSchemeStatus(String value) {
        String status = trimToNull(value);
        if (status == null) {
            return SCHEME_STATUS_ACTIVE;
        }
        status = status.toUpperCase();
        if (SCHEME_STATUS_ACTIVE.equals(status) || SCHEME_STATUS_DISABLED.equals(status)) {
            return status;
        }
        throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "绩效方案状态不合法");
    }

    private String defaultTemplateJson(String schemeType, String cycleType) {
        if (SCHEME_TYPE_OKR.equals(schemeType)) {
            return "{\"schemeType\":\"OKR\",\"cycleType\":\"" + cycleType
                    + "\",\"dimensions\":[{\"name\":\"Objective\",\"weight\":40},{\"name\":\"Key Results\",\"weight\":60}]}";
        }
        if (SCHEME_TYPE_360.equals(schemeType)) {
            return "{\"schemeType\":\"360\",\"cycleType\":\"" + cycleType
                    + "\",\"participants\":[\"self\",\"manager\",\"peer\",\"subordinate\"]}";
        }
        if (SCHEME_TYPE_MBO.equals(schemeType)) {
            return "{\"schemeType\":\"MBO\",\"cycleType\":\"" + cycleType
                    + "\",\"dimensions\":[{\"name\":\"目标完成\",\"weight\":70},{\"name\":\"协同配合\",\"weight\":30}]}";
        }
        if (SCHEME_TYPE_MIXED.equals(schemeType)) {
            return "{\"schemeType\":\"MIXED\",\"cycleType\":\"" + cycleType
                    + "\",\"dimensions\":[{\"name\":\"定量\",\"weight\":60},{\"name\":\"定性\",\"weight\":40}]}";
        }
        return "{\"schemeType\":\"KPI\",\"cycleType\":\"" + cycleType
                + "\",\"dimensions\":[{\"name\":\"目标达成\",\"weight\":60},{\"name\":\"能力素质\",\"weight\":40}]}";
    }

    private String defaultReviewFlowJson(String schemeType) {
        if (SCHEME_TYPE_360.equals(schemeType)) {
            return "{\"steps\":[\"SELF_REVIEW\",\"PEER_REVIEW\",\"MANAGER_REVIEW\",\"CALIBRATION\",\"CLOSED\"]}";
        }
        return "{\"steps\":[\"GOAL_SETTING\",\"SELF_REVIEW\",\"MANAGER_REVIEW\",\"CALIBRATION\",\"INTERVIEW\",\"FOLLOW_UP\",\"CLOSED\"]}";
    }

    private void clearOtherDefaultSchemes(Long currentId) {
        LambdaUpdateWrapper<EmployeePerformanceSchemeDO> update = new LambdaUpdateWrapper<EmployeePerformanceSchemeDO>()
                .eq(EmployeePerformanceSchemeDO::getDefaultFlag, true)
                .set(EmployeePerformanceSchemeDO::getDefaultFlag, false);
        if (currentId != null) {
            update.ne(EmployeePerformanceSchemeDO::getId, currentId);
        }
        employeePerformanceSchemeMapper.update(null, update);
    }

    private EmployeePerformanceSchemeDO getScheme(Long id) {
        EmployeePerformanceSchemeDO scheme = employeePerformanceSchemeMapper.selectById(id);
        if (scheme == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND, "绩效方案不存在");
        }
        return scheme;
    }

    private EmployeePerformanceDO getPerformance(Long id) {
        EmployeePerformanceDO performance = id == null ? null : employeePerformanceMapper.selectById(id);
        if (performance == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_PROFILE_NOT_EXISTS);
        }
        return performance;
    }

    private String defaultCycleStatus(String status) {
        String value = trimToNull(status);
        return value == null ? STATUS_GOAL_SETTING : value;
    }

    private String defaultApplicationStatus(EmployeePerformanceDO performance) {
        String status = trimToNull(performance.getApplicationStatus());
        if (status != null) {
            return status;
        }
        return hasApplicationCandidate(performance) ? APPLICATION_STATUS_PENDING : null;
    }

    private String defaultApprovalStatus(String status) {
        String value = trimToNull(status);
        return value == null ? APPROVAL_STATUS_DRAFT : value;
    }

    private String currentCycleStatus(String status) {
        String value = trimToNull(status);
        return value == null ? STATUS_UNSET : value;
    }

    private String nextCycleStatus(String status) {
        String current = currentCycleStatus(status);
        if (STATUS_UNSET.equals(current)) {
            return STATUS_GOAL_SETTING;
        }
        if (STATUS_GOAL_SETTING.equals(current)) {
            return STATUS_SELF_REVIEW;
        }
        if (STATUS_SELF_REVIEW.equals(current)) {
            return STATUS_MANAGER_REVIEW;
        }
        if (STATUS_MANAGER_REVIEW.equals(current)) {
            return STATUS_CALIBRATION;
        }
        if (STATUS_CALIBRATION.equals(current)) {
            return STATUS_INTERVIEW;
        }
        if (STATUS_INTERVIEW.equals(current)) {
            return STATUS_FOLLOW_UP;
        }
        if (STATUS_FOLLOW_UP.equals(current)) {
            return STATUS_CLOSED;
        }
        return STATUS_CLOSED.equals(current) ? STATUS_CLOSED : STATUS_GOAL_SETTING;
    }

    private String cycleStatusLabel(String status) {
        if (STATUS_GOAL_SETTING.equals(status)) {
            return "目标设定";
        }
        if (STATUS_SELF_REVIEW.equals(status)) {
            return "员工自评";
        }
        if (STATUS_MANAGER_REVIEW.equals(status)) {
            return "主管评价";
        }
        if (STATUS_CALIBRATION.equals(status)) {
            return "校准中";
        }
        if (STATUS_INTERVIEW.equals(status)) {
            return "面谈中";
        }
        if (STATUS_FOLLOW_UP.equals(status)) {
            return "跟进中";
        }
        if (STATUS_CLOSED.equals(status)) {
            return "已闭环";
        }
        if (STATUS_UNSET.equals(status)) {
            return "未设置";
        }
        return status;
    }

    private int cycleStatusRank(String status) {
        if (STATUS_UNSET.equals(status)) {
            return -1;
        }
        for (int i = 0; i < STATUS_ORDER.length; i++) {
            if (STATUS_ORDER[i].equals(status)) {
                return i;
            }
        }
        return STATUS_ORDER.length;
    }
}
