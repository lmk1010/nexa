package com.kyx.service.hr.service.employee;

import com.kyx.foundation.common.exception.enums.GlobalErrorCodeConstants;
import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.foundation.security.core.util.SecurityFrameworkUtils;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeTrainingPageReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeTrainingRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeTrainingSaveReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeTrainingStatsRespVO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeMaterialDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeProfileDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeTrainingDO;
import com.kyx.service.hr.dal.dataobject.exam.ExamAttemptDO;
import com.kyx.service.hr.dal.dataobject.exam.ExamDO;
import com.kyx.service.hr.dal.mysql.employee.EmployeeMaterialMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeProfileMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeTrainingMapper;
import com.kyx.service.hr.dal.mysql.exam.ExamAttemptMapper;
import com.kyx.service.hr.dal.mysql.exam.ExamMapper;
import com.kyx.service.hr.enums.ErrorCodeConstants;
import com.kyx.service.hr.service.todo.HrTodoTaskService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Validated
public class EmployeeTrainingServiceImpl implements EmployeeTrainingService {

    private static final String SOURCE_TRAINING_RECORD = "TRAINING_RECORD";
    private static final String MATERIAL_CATEGORY_TRAINING = "TRAINING";
    private static final String MATERIAL_TYPE_CERTIFICATE = "CERTIFICATE";
    private static final String MATERIAL_TYPE_MATERIAL = "MATERIAL";
    private static final String MATERIAL_STATUS_ACTIVE = "ACTIVE";
    private static final int DEFAULT_RETRAIN_REMINDER_DAYS = 30;

    @Resource
    private EmployeeTrainingMapper employeeTrainingMapper;
    @Resource
    private EmployeeProfileMapper employeeProfileMapper;
    @Resource
    private EmployeeMaterialMapper employeeMaterialMapper;
    @Resource
    private HrTodoTaskService hrTodoTaskService;
    @Resource
    private ExamAttemptMapper examAttemptMapper;
    @Resource
    private ExamMapper examMapper;

    @Override
    public List<EmployeeTrainingRespVO> getTrainingList(Long profileId) {
        List<EmployeeTrainingDO> list = employeeTrainingMapper.selectListByProfileId(profileId);
        List<EmployeeTrainingRespVO> respList = BeanUtils.toBean(list, EmployeeTrainingRespVO.class);
        fillProfileInfo(respList);
        return respList;
    }

    @Override
    public List<EmployeeTrainingRespVO> getMyTrainingList() {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            return new ArrayList<>();
        }
        EmployeeProfileDO profile = employeeProfileMapper.selectByUserId(loginUserId);
        if (profile == null || profile.getId() == null) {
            return new ArrayList<>();
        }
        return getTrainingList(profile.getId());
    }

    @Override
    public PageResult<EmployeeTrainingRespVO> getTrainingPage(EmployeeTrainingPageReqVO pageReqVO) {
        if (!prepareProfileFilter(pageReqVO)) {
            return new PageResult<>(new ArrayList<>(), 0L);
        }
        PageResult<EmployeeTrainingDO> pageResult = employeeTrainingMapper.selectPage(pageReqVO);
        List<EmployeeTrainingRespVO> respList = BeanUtils.toBean(pageResult.getList(), EmployeeTrainingRespVO.class);
        fillProfileInfo(respList);
        return new PageResult<>(respList, pageResult.getTotal());
    }

    @Override
    public PageResult<EmployeeTrainingRespVO> getRetrainReminderPage(EmployeeTrainingPageReqVO pageReqVO) {
        pageReqVO.setRetrainDueOnly(true);
        return getTrainingPage(pageReqVO);
    }

    @Override
    public EmployeeTrainingStatsRespVO getTrainingStats(EmployeeTrainingPageReqVO pageReqVO) {
        EmployeeTrainingStatsRespVO stats = new EmployeeTrainingStatsRespVO();
        if (!prepareProfileFilter(pageReqVO)) {
            return stats;
        }
        List<EmployeeTrainingDO> list = employeeTrainingMapper.selectListByReq(pageReqVO, 5000);
        stats.setTotalCount(list.size());
        stats.setCompletedCount((int) list.stream().filter(this::isCompleted).count());
        stats.setInProgressCount((int) list.stream().filter(this::isInProgress).count());
        stats.setUpcomingCount((int) list.stream().filter(this::isUpcoming).count());
        stats.setOverdueCount((int) list.stream().filter(this::isOverdue).count());
        stats.setRetrainDueCount((int) list.stream().filter(this::isRetrainDue).count());
        stats.setCertificateExpiringCount((int) list.stream().filter(this::isCertificateExpiring).count());
        stats.setCertificateLinkedCount((int) list.stream().filter(this::hasCertificate).count());
        stats.setCompletionRate(percent(stats.getCompletedCount(), stats.getTotalCount()));
        stats.setOverdueRate(percent(stats.getOverdueCount(), stats.getTotalCount()));
        stats.setCertificateCoverageRate(percent(stats.getCertificateLinkedCount(), stats.getTotalCount()));
        List<Integer> scores = list.stream()
                .map(EmployeeTrainingDO::getEvaluationScore)
                .filter(score -> score != null)
                .collect(Collectors.toList());
        stats.setEvaluatedCount(scores.size());
        stats.setAverageEvaluationScore(average(scores));
        stats.setSatisfactionRate(percent((int) scores.stream().filter(score -> score >= 4).count(), scores.size()));
        stats.setTotalHours(list.stream()
                .map(EmployeeTrainingDO::getHours)
                .filter(hours -> hours != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        stats.setAverageHoursPerRecord(stats.getTotalCount() <= 0 ? BigDecimal.ZERO
                : stats.getTotalHours().divide(BigDecimal.valueOf(stats.getTotalCount()), 2, RoundingMode.HALF_UP));
        fillExamStats(stats, list);
        stats.setProviderStats(groupStats(list, EmployeeTrainingDO::getProvider));
        stats.setResultStats(groupStats(list, EmployeeTrainingDO::getResult));
        return stats;
    }

    @Override
    public Long createTraining(EmployeeTrainingSaveReqVO createReqVO) {
        validateProfileExists(createReqVO.getProfileId());
        validateEvaluationScore(createReqVO.getEvaluationScore());
        EmployeeTrainingDO training = BeanUtils.toBean(createReqVO, EmployeeTrainingDO.class);
        normalizeTraining(training);
        employeeTrainingMapper.insert(training);
        if (training.getSourceId() == null) {
            EmployeeTrainingDO sourceUpdate = new EmployeeTrainingDO();
            sourceUpdate.setId(training.getId());
            sourceUpdate.setSourceId(training.getId());
            employeeTrainingMapper.updateById(sourceUpdate);
            training.setSourceId(training.getId());
        }
        syncTrainingMaterial(training);
        refreshTodoTasksQuietly();
        return training.getId();
    }

    @Override
    public void updateTraining(EmployeeTrainingSaveReqVO updateReqVO) {
        if (updateReqVO.getId() == null || employeeTrainingMapper.selectById(updateReqVO.getId()) == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_PROFILE_NOT_EXISTS);
        }
        validateProfileExists(updateReqVO.getProfileId());
        validateEvaluationScore(updateReqVO.getEvaluationScore());
        EmployeeTrainingDO training = BeanUtils.toBean(updateReqVO, EmployeeTrainingDO.class);
        normalizeTraining(training);
        employeeTrainingMapper.updateById(training);
        syncTrainingMaterial(training);
        refreshTodoTasksQuietly();
    }

    @Override
    public void deleteTraining(Long id) {
        if (employeeTrainingMapper.selectById(id) == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_PROFILE_NOT_EXISTS);
        }
        EmployeeMaterialDO material = employeeMaterialMapper.selectBySource(SOURCE_TRAINING_RECORD, id);
        if (material != null) {
            employeeMaterialMapper.deleteById(material.getId());
        }
        employeeTrainingMapper.deleteById(id);
        refreshTodoTasksQuietly();
    }

    private void validateProfileExists(Long profileId) {
        if (profileId == null || employeeProfileMapper.selectById(profileId) == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_PROFILE_NOT_EXISTS);
        }
    }

    private void validateEvaluationScore(Integer evaluationScore) {
        if (evaluationScore == null) {
            return;
        }
        if (evaluationScore < 1 || evaluationScore > 5) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "培训评价分数只能为 1-5 分");
        }
    }

    private void normalizeTraining(EmployeeTrainingDO training) {
        if (training.getRetrainReminderDays() == null && training.getRetrainDate() != null) {
            training.setRetrainReminderDays(DEFAULT_RETRAIN_REMINDER_DAYS);
        }
        if (!StringUtils.hasText(training.getSourceType())) {
            training.setSourceType(SOURCE_TRAINING_RECORD);
        }
        if (training.getSourceId() == null && training.getId() != null) {
            training.setSourceId(training.getId());
        }
    }

    private void syncTrainingMaterial(EmployeeTrainingDO training) {
        if (training.getId() == null) {
            return;
        }
        String fileUrl = firstText(training.getCertificateUrl(), training.getMaterialUrl());
        if (!StringUtils.hasText(fileUrl)) {
            return;
        }
        EmployeeMaterialDO existing = employeeMaterialMapper.selectBySource(SOURCE_TRAINING_RECORD, training.getId());
        EmployeeMaterialDO material = existing == null ? new EmployeeMaterialDO() : existing;
        if (existing != null) {
            material.setId(existing.getId());
        }
        material.setProfileId(training.getProfileId());
        material.setCategory(MATERIAL_CATEGORY_TRAINING);
        material.setMaterialType(StringUtils.hasText(training.getCertificateUrl())
                ? MATERIAL_TYPE_CERTIFICATE : MATERIAL_TYPE_MATERIAL);
        material.setMaterialName(firstText(training.getCertificateName(), training.getMaterialName(),
                training.getTrainingName(), "培训材料"));
        material.setFileUrl(fileUrl);
        material.setFileName(material.getMaterialName());
        material.setIssueDate(training.getEndDate());
        material.setExpireDate(firstDate(training.getCertificateExpireDate(), training.getRetrainDate()));
        material.setStatus(MATERIAL_STATUS_ACTIVE);
        material.setSourceType(SOURCE_TRAINING_RECORD);
        material.setSourceId(training.getId());
        material.setRemark(firstText(training.getRemark(), "培训台账关联材料"));
        if (material.getId() == null) {
            employeeMaterialMapper.insert(material);
        } else {
            employeeMaterialMapper.updateById(material);
        }
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private LocalDate firstDate(LocalDate... values) {
        for (LocalDate value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private boolean prepareProfileFilter(EmployeeTrainingPageReqVO reqVO) {
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

    private void fillProfileInfo(List<EmployeeTrainingRespVO> respList) {
        if (respList == null || respList.isEmpty()) {
            return;
        }
        Set<Long> profileIds = respList.stream()
                .map(EmployeeTrainingRespVO::getProfileId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(HashSet::new));
        if (profileIds.isEmpty()) {
            return;
        }
        Map<Long, EmployeeProfileDO> profileMap = employeeProfileMapper.selectBatchIds(profileIds).stream()
                .collect(Collectors.toMap(EmployeeProfileDO::getId, item -> item, (left, right) -> left));
        for (EmployeeTrainingRespVO item : respList) {
            EmployeeProfileDO profile = profileMap.get(item.getProfileId());
            if (profile == null) {
                continue;
            }
            item.setProfileName(profile.getName());
            item.setProfileMobile(profile.getMobile());
        }
    }

    private List<EmployeeTrainingStatsRespVO.StatItem> groupStats(
            List<EmployeeTrainingDO> list,
            java.util.function.Function<EmployeeTrainingDO, String> classifier) {
        Map<String, Integer> counts = new HashMap<>();
        for (EmployeeTrainingDO item : list) {
            String name = classifier.apply(item);
            if (!StringUtils.hasText(name)) {
                name = "未填写";
            }
            counts.put(name, counts.getOrDefault(name, 0) + 1);
        }
        return counts.entrySet().stream()
                .map(entry -> new EmployeeTrainingStatsRespVO.StatItem(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(EmployeeTrainingStatsRespVO.StatItem::getCount).reversed())
                .collect(Collectors.toList());
    }

    private void fillExamStats(EmployeeTrainingStatsRespVO stats, List<EmployeeTrainingDO> list) {
        Set<Long> examIds = list.stream()
                .map(EmployeeTrainingDO::getExamId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(HashSet::new));
        Set<Long> profileIds = list.stream()
                .map(EmployeeTrainingDO::getProfileId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(HashSet::new));
        if (examIds.isEmpty() || profileIds.isEmpty()) {
            return;
        }
        Map<Long, EmployeeProfileDO> profileMap = employeeProfileMapper.selectBatchIds(profileIds).stream()
                .collect(Collectors.toMap(EmployeeProfileDO::getId, item -> item, (left, right) -> left));
        Set<Long> userIds = profileMap.values().stream()
                .map(EmployeeProfileDO::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(HashSet::new));
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, ExamDO> examMap = examMapper.selectBatchIds(examIds).stream()
                .collect(Collectors.toMap(ExamDO::getId, item -> item, (left, right) -> left));
        Map<String, ExamAttemptDO> latestAttemptMap = latestSubmittedAttempts(examIds, userIds);
        int linked = 0;
        int submitted = 0;
        int passed = 0;
        BigDecimal scoreTotal = BigDecimal.ZERO;
        for (EmployeeTrainingDO item : list) {
            if (item.getExamId() == null || item.getProfileId() == null) {
                continue;
            }
            linked++;
            EmployeeProfileDO profile = profileMap.get(item.getProfileId());
            Long userId = profile == null ? null : profile.getUserId();
            ExamAttemptDO attempt = userId == null ? null : latestAttemptMap.get(examAttemptKey(item.getExamId(), userId));
            if (attempt == null || attempt.getTotalScore() == null) {
                continue;
            }
            submitted++;
            scoreTotal = scoreTotal.add(BigDecimal.valueOf(attempt.getTotalScore()));
            ExamDO exam = examMap.get(item.getExamId());
            int passScore = exam == null || exam.getPassScore() == null ? 60 : exam.getPassScore();
            if (attempt.getTotalScore() >= passScore) {
                passed++;
            }
        }
        stats.setExamLinkedCount(linked);
        stats.setExamSubmittedCount(submitted);
        stats.setExamPassedCount(passed);
        stats.setExamAverageScore(submitted <= 0 ? BigDecimal.ZERO
                : scoreTotal.divide(BigDecimal.valueOf(submitted), 2, RoundingMode.HALF_UP));
        stats.setExamPassRate(percent(passed, submitted));
    }

    private Map<String, ExamAttemptDO> latestSubmittedAttempts(Set<Long> examIds, Set<Long> userIds) {
        List<ExamAttemptDO> attempts = examAttemptMapper.selectList(new LambdaQueryWrapperX<ExamAttemptDO>()
                .in(ExamAttemptDO::getExamId, examIds)
                .in(ExamAttemptDO::getUserId, userIds)
                .eq(ExamAttemptDO::getStatus, 1)
                .isNotNull(ExamAttemptDO::getTotalScore)
                .orderByDesc(ExamAttemptDO::getSubmitAt)
                .orderByDesc(ExamAttemptDO::getId)
                .last("LIMIT " + Math.min(examIds.size() * Math.max(userIds.size(), 1) * 3, 5000)));
        Map<String, ExamAttemptDO> result = new HashMap<>();
        for (ExamAttemptDO attempt : attempts) {
            if (attempt.getExamId() == null || attempt.getUserId() == null) {
                continue;
            }
            result.putIfAbsent(examAttemptKey(attempt.getExamId(), attempt.getUserId()), attempt);
        }
        return result;
    }

    private String examAttemptKey(Long examId, Long userId) {
        return examId + ":" + userId;
    }

    private BigDecimal percent(int numerator, int denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal average(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        int total = values.stream().mapToInt(Integer::intValue).sum();
        return BigDecimal.valueOf(total)
                .divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private boolean isCompleted(EmployeeTrainingDO item) {
        if (isNegativeResult(item.getResult())) {
            return false;
        }
        return containsAny(item.getResult(), "通过", "完成", "已完成", "合格", "PASS", "pass", "Pass")
                || (item.getEndDate() != null && !item.getEndDate().isAfter(LocalDate.now())
                && StringUtils.hasText(item.getResult()) && !isNegativeResult(item.getResult()));
    }

    private boolean isInProgress(EmployeeTrainingDO item) {
        LocalDate today = LocalDate.now();
        return item.getStartDate() != null && item.getEndDate() != null
                && !item.getStartDate().isAfter(today)
                && !item.getEndDate().isBefore(today)
                && !isCompleted(item);
    }

    private boolean isUpcoming(EmployeeTrainingDO item) {
        return item.getStartDate() != null && item.getStartDate().isAfter(LocalDate.now());
    }

    private boolean isOverdue(EmployeeTrainingDO item) {
        return item.getEndDate() != null && item.getEndDate().isBefore(LocalDate.now()) && !isCompleted(item);
    }

    private boolean isRetrainDue(EmployeeTrainingDO item) {
        if (item.getRetrainDate() == null) {
            return false;
        }
        int reminderDays = item.getRetrainReminderDays() == null
                ? DEFAULT_RETRAIN_REMINDER_DAYS : Math.max(item.getRetrainReminderDays(), 0);
        return !item.getRetrainDate().isAfter(LocalDate.now().plusDays(reminderDays));
    }

    private boolean isCertificateExpiring(EmployeeTrainingDO item) {
        return item.getCertificateExpireDate() != null
                && !item.getCertificateExpireDate().isAfter(LocalDate.now().plusDays(DEFAULT_RETRAIN_REMINDER_DAYS));
    }

    private boolean hasCertificate(EmployeeTrainingDO item) {
        return StringUtils.hasText(item.getCertificateName())
                || StringUtils.hasText(item.getCertificateUrl());
    }

    private boolean isNegativeResult(String result) {
        return containsAny(result, "未通过", "不合格", "未完成", "待完成", "待补", "FAIL", "fail", "Fail");
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

    private void refreshTodoTasksQuietly() {
        try {
            hrTodoTaskService.refreshGeneratedTasks();
        } catch (Exception ex) {
            // 待办刷新失败不影响培训台账保存。
        }
    }
}
