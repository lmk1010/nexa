package com.kyx.service.hr.service.training;

import com.kyx.foundation.common.exception.enums.GlobalErrorCodeConstants;
import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.foundation.security.core.util.SecurityFrameworkUtils;
import com.kyx.service.business.api.dept.DeptApi;
import com.kyx.service.business.api.dept.dto.DeptRespDTO;
import com.kyx.service.hr.controller.admin.training.vo.TrainingAssignmentPageReqVO;
import com.kyx.service.hr.controller.admin.training.vo.TrainingAssignmentRespVO;
import com.kyx.service.hr.controller.admin.training.vo.TrainingAssignmentUpdateReqVO;
import com.kyx.service.hr.controller.admin.training.vo.TrainingCoursePageReqVO;
import com.kyx.service.hr.controller.admin.training.vo.TrainingCourseRespVO;
import com.kyx.service.hr.controller.admin.training.vo.TrainingCourseSaveReqVO;
import com.kyx.service.hr.controller.admin.training.vo.TrainingPlanPageReqVO;
import com.kyx.service.hr.controller.admin.training.vo.TrainingPlanRespVO;
import com.kyx.service.hr.controller.admin.training.vo.TrainingPlanSaveReqVO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeEntryDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeMaterialDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeProfileDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeTrainingDO;
import com.kyx.service.hr.dal.dataobject.training.TrainingAssignmentDO;
import com.kyx.service.hr.dal.dataobject.training.TrainingCourseDO;
import com.kyx.service.hr.dal.dataobject.training.TrainingPlanDO;
import com.kyx.service.hr.dal.mysql.employee.EmployeeEntryMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeMaterialMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeProfileMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeTrainingMapper;
import com.kyx.service.hr.dal.mysql.training.TrainingAssignmentMapper;
import com.kyx.service.hr.dal.mysql.training.TrainingCourseMapper;
import com.kyx.service.hr.dal.mysql.training.TrainingPlanMapper;
import com.kyx.service.hr.service.todo.HrTodoTaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Validated
public class TrainingLearningServiceImpl implements TrainingLearningService {

    private static final String PLAN_DRAFT = "DRAFT";
    private static final String PLAN_PUBLISHED = "PUBLISHED";
    private static final String PLAN_CLOSED = "CLOSED";

    private static final String ASSIGNMENT_NOT_STARTED = "NOT_STARTED";
    private static final String ASSIGNMENT_IN_PROGRESS = "IN_PROGRESS";
    private static final String ASSIGNMENT_COMPLETED = "COMPLETED";
    private static final String ASSIGNMENT_CANCELED = "CANCELED";
    private static final int COURSE_STATUS_ENABLED = 0;

    private static final String SOURCE_TRAINING_ASSIGNMENT = "TRAINING_ASSIGNMENT";
    private static final String SOURCE_TRAINING_RECORD = "TRAINING_RECORD";
    private static final String MATERIAL_CATEGORY_TRAINING = "TRAINING";
    private static final String MATERIAL_TYPE_CERTIFICATE = "CERTIFICATE";
    private static final String MATERIAL_TYPE_MATERIAL = "MATERIAL";
    private static final String MATERIAL_STATUS_ACTIVE = "ACTIVE";
    private static final int DEFAULT_RETRAIN_REMINDER_DAYS = 30;

    @Resource
    private TrainingCourseMapper trainingCourseMapper;
    @Resource
    private TrainingPlanMapper trainingPlanMapper;
    @Resource
    private TrainingAssignmentMapper trainingAssignmentMapper;
    @Resource
    private EmployeeProfileMapper employeeProfileMapper;
    @Resource
    private EmployeeEntryMapper employeeEntryMapper;
    @Resource
    private EmployeeTrainingMapper employeeTrainingMapper;
    @Resource
    private EmployeeMaterialMapper employeeMaterialMapper;
    @Resource
    private DeptApi deptApi;
    @Resource
    private HrTodoTaskService hrTodoTaskService;

    @Override
    public PageResult<TrainingCourseRespVO> getCoursePage(TrainingCoursePageReqVO pageReqVO) {
        PageResult<TrainingCourseDO> pageResult = trainingCourseMapper.selectPage(pageReqVO);
        return new PageResult<>(BeanUtils.toBean(pageResult.getList(), TrainingCourseRespVO.class), pageResult.getTotal());
    }

    @Override
    public TrainingCourseRespVO getCourse(Long id) {
        return BeanUtils.toBean(validateCourseExists(id), TrainingCourseRespVO.class);
    }

    @Override
    public Long createCourse(TrainingCourseSaveReqVO createReqVO) {
        TrainingCourseDO course = BeanUtils.toBean(createReqVO, TrainingCourseDO.class);
        if (course.getStatus() == null) {
            course.setStatus(0);
        }
        trainingCourseMapper.insert(course);
        return course.getId();
    }

    @Override
    public void updateCourse(TrainingCourseSaveReqVO updateReqVO) {
        validateCourseExists(updateReqVO.getId());
        TrainingCourseDO course = BeanUtils.toBean(updateReqVO, TrainingCourseDO.class);
        trainingCourseMapper.updateById(course);
    }

    @Override
    public void deleteCourse(Long id) {
        validateCourseExists(id);
        Long planCount = trainingPlanMapper.selectCount(new LambdaQueryWrapperX<TrainingPlanDO>()
                .eq(TrainingPlanDO::getCourseId, id));
        if (planCount != null && planCount > 0) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "课程已被学习计划使用，不能删除");
        }
        trainingCourseMapper.deleteById(id);
    }

    @Override
    public PageResult<TrainingPlanRespVO> getPlanPage(TrainingPlanPageReqVO pageReqVO) {
        if (!preparePlanCourseFilter(pageReqVO)) {
            return new PageResult<>(new ArrayList<>(), 0L);
        }
        PageResult<TrainingPlanDO> pageResult = trainingPlanMapper.selectPage(pageReqVO);
        List<TrainingPlanRespVO> respList = BeanUtils.toBean(pageResult.getList(), TrainingPlanRespVO.class);
        fillPlanCourseInfo(respList);
        fillPlanAssignmentStats(respList);
        return new PageResult<>(respList, pageResult.getTotal());
    }

    @Override
    public TrainingPlanRespVO getPlan(Long id) {
        TrainingPlanRespVO respVO = BeanUtils.toBean(validatePlanExists(id), TrainingPlanRespVO.class);
        List<TrainingPlanRespVO> respList = new ArrayList<>();
        respList.add(respVO);
        fillPlanCourseInfo(respList);
        fillPlanAssignmentStats(respList);
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPlan(TrainingPlanSaveReqVO createReqVO) {
        TrainingCourseDO course = validateCourseExists(createReqVO.getCourseId());
        validateCourseEnabled(course);
        validatePlanDateRange(createReqVO.getStartDate(), createReqVO.getEndDate());
        Set<Long> targetProfileIds = resolveTargetProfileIds(createReqVO.getProfileIds(), createReqVO.getDeptIds());
        TrainingPlanDO plan = BeanUtils.toBean(createReqVO, TrainingPlanDO.class);
        applyPlanDefaults(plan, course);
        plan.setStatus(PLAN_DRAFT);
        if (!StringUtils.hasText(plan.getPlanCode())) {
            plan.setPlanCode(generatePlanCode());
        }
        if (!StringUtils.hasText(plan.getTargetSummary()) && hasAssignmentScope(createReqVO)) {
            plan.setTargetSummary(buildTargetSummary(createReqVO.getProfileIds(), createReqVO.getDeptIds(), targetProfileIds));
        }
        trainingPlanMapper.insert(plan);
        syncAssignments(plan, targetProfileIds);
        refreshTodoTasksQuietly();
        return plan.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePlan(TrainingPlanSaveReqVO updateReqVO) {
        TrainingPlanDO existing = validatePlanExists(updateReqVO.getId());
        validateDraftPlan(existing, "只有草稿学习计划可以编辑，请先关闭当前计划后新建调整版本");
        TrainingCourseDO course = validateCourseExists(updateReqVO.getCourseId());
        validateCourseEnabled(course);
        validatePlanDateRange(updateReqVO.getStartDate(), updateReqVO.getEndDate());
        Set<Long> targetProfileIds = hasAssignmentScope(updateReqVO)
                ? resolveTargetProfileIds(updateReqVO.getProfileIds(), updateReqVO.getDeptIds())
                : null;
        TrainingPlanDO plan = BeanUtils.toBean(updateReqVO, TrainingPlanDO.class);
        applyPlanDefaults(plan, course);
        plan.setStatus(existing.getStatus());
        if (!StringUtils.hasText(plan.getTargetSummary()) && hasAssignmentScope(updateReqVO)) {
            plan.setTargetSummary(buildTargetSummary(updateReqVO.getProfileIds(), updateReqVO.getDeptIds(), targetProfileIds));
        }
        trainingPlanMapper.updateById(plan);
        TrainingPlanDO latest = trainingPlanMapper.selectById(updateReqVO.getId());
        syncAssignments(latest, targetProfileIds);
        refreshTodoTasksQuietly();
    }

    @Override
    public void publishPlan(Long id) {
        TrainingPlanDO plan = validatePlanExists(id);
        validateDraftPlan(plan, "只有草稿学习计划可以发布");
        TrainingCourseDO course = validateCourseExists(plan.getCourseId());
        validateCourseEnabled(course);
        validatePlanDateRange(plan.getStartDate(), plan.getEndDate());
        Long assignmentCount = trainingAssignmentMapper.selectCountByPlanIdAndStatus(id, null);
        if (assignmentCount == null || assignmentCount == 0) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "学习计划至少需要分配一个员工");
        }
        TrainingPlanDO updateDO = new TrainingPlanDO();
        updateDO.setId(plan.getId());
        updateDO.setStatus(PLAN_PUBLISHED);
        trainingPlanMapper.updateById(updateDO);
        refreshTodoTasksQuietly();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closePlan(Long id) {
        TrainingPlanDO plan = validatePlanExists(id);
        if (!PLAN_PUBLISHED.equals(plan.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "只有已发布学习计划可以关闭");
        }
        TrainingPlanDO updateDO = new TrainingPlanDO();
        updateDO.setId(id);
        updateDO.setStatus(PLAN_CLOSED);
        trainingPlanMapper.updateById(updateDO);

        List<TrainingAssignmentDO> assignments = trainingAssignmentMapper.selectListByPlanId(id);
        for (TrainingAssignmentDO assignment : assignments) {
            if (ASSIGNMENT_COMPLETED.equals(assignment.getStatus()) || ASSIGNMENT_CANCELED.equals(assignment.getStatus())) {
                continue;
            }
            TrainingAssignmentDO assignmentUpdate = new TrainingAssignmentDO();
            assignmentUpdate.setId(assignment.getId());
            assignmentUpdate.setStatus(ASSIGNMENT_CANCELED);
            trainingAssignmentMapper.updateById(assignmentUpdate);
        }
        refreshTodoTasksQuietly();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePlan(Long id) {
        TrainingPlanDO plan = validatePlanExists(id);
        if (!PLAN_DRAFT.equals(plan.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "只有草稿学习计划可以删除，已发布或已关闭计划需保留业务记录");
        }
        trainingAssignmentMapper.delete(new LambdaQueryWrapperX<TrainingAssignmentDO>()
                .eq(TrainingAssignmentDO::getPlanId, id));
        trainingPlanMapper.deleteById(id);
        refreshTodoTasksQuietly();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long enrollPlan(Long planId) {
        TrainingPlanDO plan = validatePlanExists(planId);
        if (!PLAN_PUBLISHED.equals(plan.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "只有已发布学习计划可以报名");
        }
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.FORBIDDEN, "未登录，不能报名学习计划");
        }
        EmployeeProfileDO profile = employeeProfileMapper.selectByUserId(loginUserId);
        if (profile == null || profile.getId() == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND, "未找到当前用户员工档案");
        }
        TrainingAssignmentDO existing = trainingAssignmentMapper.selectOne(new LambdaQueryWrapperX<TrainingAssignmentDO>()
                .eq(TrainingAssignmentDO::getPlanId, planId)
                .eq(TrainingAssignmentDO::getProfileId, profile.getId())
                .last("LIMIT 1"));
        if (existing != null) {
            return existing.getId();
        }
        TrainingCourseDO course = plan.getCourseId() == null ? null : trainingCourseMapper.selectById(plan.getCourseId());
        TrainingAssignmentDO assignment = buildAssignment(plan, course, profile);
        trainingAssignmentMapper.insert(assignment);
        refreshTodoTasksQuietly();
        return assignment.getId();
    }

    @Override
    public PageResult<TrainingAssignmentRespVO> getAssignmentPage(TrainingAssignmentPageReqVO pageReqVO) {
        if (!prepareAssignmentProfileFilter(pageReqVO)) {
            return new PageResult<>(new ArrayList<>(), 0L);
        }
        PageResult<TrainingAssignmentDO> pageResult = trainingAssignmentMapper.selectPage(pageReqVO);
        List<TrainingAssignmentRespVO> respList = BeanUtils.toBean(pageResult.getList(), TrainingAssignmentRespVO.class);
        fillAssignmentInfo(respList);
        return new PageResult<>(respList, pageResult.getTotal());
    }

    @Override
    public void updateAssignment(TrainingAssignmentUpdateReqVO updateReqVO) {
        updateAssignmentInternal(updateReqVO, false);
    }

    @Override
    public void completeMyAssignment(TrainingAssignmentUpdateReqVO updateReqVO) {
        updateAssignmentInternal(updateReqVO, true);
    }

    private void updateAssignmentInternal(TrainingAssignmentUpdateReqVO updateReqVO, boolean mineOnly) {
        TrainingAssignmentDO assignment = validateAssignmentExists(updateReqVO.getId());
        if (mineOnly && !Objects.equals(assignment.getUserId(), SecurityFrameworkUtils.getLoginUserId())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.FORBIDDEN, "无权处理该学习任务");
        }
        validateEvaluationScore(updateReqVO.getEvaluationScore());
        validateAssignmentProgress(updateReqVO.getProgress());
        TrainingPlanDO plan = assignment.getPlanId() == null ? null : validatePlanExists(assignment.getPlanId());
        String targetStatus = normalizeAssignmentTargetStatus(assignment, updateReqVO);
        validateAssignmentWritable(plan, assignment, targetStatus, mineOnly);
        TrainingAssignmentDO updateDO = BeanUtils.toBean(updateReqVO, TrainingAssignmentDO.class);
        updateDO.setStatus(targetStatus);
        if (ASSIGNMENT_COMPLETED.equals(updateDO.getStatus())) {
            validateAssignmentCompletion(updateReqVO, assignment);
            updateDO.setProgress(100);
            updateDO.setCompletedTime(assignment.getCompletedTime() == null
                    ? LocalDateTime.now() : assignment.getCompletedTime());
            if (!StringUtils.hasText(updateDO.getResult())) {
                updateDO.setResult(firstText(assignment.getResult(), "已完成"));
            }
        }
        if (hasEvaluationPayload(updateReqVO)) {
            updateDO.setEvaluatedTime(LocalDateTime.now());
        }
        trainingAssignmentMapper.updateById(updateDO);
        TrainingAssignmentDO latest = trainingAssignmentMapper.selectById(assignment.getId());
        if (latest != null && ASSIGNMENT_COMPLETED.equals(latest.getStatus())) {
            syncEmployeeTrainingArchive(latest);
        }
        refreshTodoTasksQuietly();
    }

    private void validateEvaluationScore(Integer evaluationScore) {
        if (evaluationScore == null) {
            return;
        }
        if (evaluationScore < 1 || evaluationScore > 5) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "课程评价分数只能为 1-5 分");
        }
    }

    private boolean hasEvaluationPayload(TrainingAssignmentUpdateReqVO updateReqVO) {
        return updateReqVO.getEvaluationScore() != null || StringUtils.hasText(updateReqVO.getEvaluationFeedback());
    }

    private String resolveStatusByProgress(String currentStatus, Integer progress) {
        if (progress == null) {
            return currentStatus;
        }
        if (progress >= 100) {
            return ASSIGNMENT_COMPLETED;
        }
        if (progress > 0) {
            return ASSIGNMENT_IN_PROGRESS;
        }
        return ASSIGNMENT_NOT_STARTED;
    }

    private void syncEmployeeTrainingArchive(TrainingAssignmentDO assignment) {
        if (assignment.getProfileId() == null) {
            return;
        }
        TrainingPlanDO plan = assignment.getPlanId() == null ? null : trainingPlanMapper.selectById(assignment.getPlanId());
        TrainingCourseDO course = assignment.getCourseId() == null ? null : trainingCourseMapper.selectById(assignment.getCourseId());

        String trainingName = firstText(course == null ? null : course.getCourseName(),
                plan == null ? null : plan.getPlanName(), "学习任务-" + assignment.getId());
        String provider = firstText(course == null ? null : course.getProvider(), "内部学习");
        LocalDate startDate = plan == null ? null : plan.getStartDate();
        LocalDate endDate = assignment.getCompletedTime() == null
                ? (plan == null ? null : plan.getEndDate())
                : assignment.getCompletedTime().toLocalDate();

        EmployeeTrainingDO existing = employeeTrainingMapper.selectBySource(SOURCE_TRAINING_ASSIGNMENT, assignment.getId());
        if (existing == null) {
            existing = employeeTrainingMapper.selectArchive(
                    assignment.getProfileId(), trainingName, provider, startDate, endDate);
        }
        EmployeeTrainingDO archive = new EmployeeTrainingDO();
        if (existing != null) {
            archive.setId(existing.getId());
        }
        archive.setProfileId(assignment.getProfileId());
        archive.setCourseId(assignment.getCourseId());
        archive.setPlanId(assignment.getPlanId());
        archive.setAssignmentId(assignment.getId());
        archive.setTrainingName(trainingName);
        archive.setProvider(provider);
        archive.setStartDate(startDate);
        archive.setEndDate(endDate);
        archive.setHours(course == null ? null : course.getDurationHours());
        archive.setResult(firstText(assignment.getResult(), "已完成"));
        archive.setCertificateName(firstText(assignment.getCertificateName(), trainingName + "证书"));
        archive.setCertificateUrl(assignment.getCertificateUrl());
        archive.setMaterialName(firstText(assignment.getMaterialName(), course == null ? null : course.getMaterialName()));
        archive.setMaterialUrl(firstText(assignment.getMaterialUrl(), course == null ? null : course.getMaterialUrl()));
        archive.setExamId(firstLong(assignment.getExamId(), plan == null ? null : plan.getExamId(),
                course == null ? null : course.getExamId()));
        archive.setQuestionnaireId(firstLong(assignment.getQuestionnaireId(), plan == null ? null : plan.getQuestionnaireId(),
                course == null ? null : course.getQuestionnaireId()));
        LocalDate retrainDate = firstDate(assignment.getRetrainDate(), calculateRetrainDate(endDate, plan, course));
        Integer reminderDays = firstInteger(plan == null ? null : plan.getReminderDays(),
                course == null ? null : course.getDefaultReminderDays(), DEFAULT_RETRAIN_REMINDER_DAYS);
        archive.setCertificateExpireDate(retrainDate);
        archive.setRetrainDate(retrainDate);
        archive.setRetrainReminderDays(reminderDays);
        archive.setSourceType(SOURCE_TRAINING_ASSIGNMENT);
        archive.setSourceId(assignment.getId());
        archive.setRemark(buildTrainingArchiveRemark(plan, assignment));
        archive.setEvaluationScore(assignment.getEvaluationScore());
        archive.setEvaluationFeedback(assignment.getEvaluationFeedback());
        archive.setEvaluatedTime(assignment.getEvaluatedTime());
        if (archive.getId() == null) {
            employeeTrainingMapper.insert(archive);
        } else {
            employeeTrainingMapper.updateById(archive);
        }
        syncTrainingMaterial(archive);
    }

    private String buildTrainingArchiveRemark(TrainingPlanDO plan, TrainingAssignmentDO assignment) {
        StringBuilder remark = new StringBuilder("来源：学习计划");
        if (plan != null && StringUtils.hasText(plan.getPlanName())) {
            remark.append("【").append(plan.getPlanName().trim()).append("】");
        }
        if (StringUtils.hasText(assignment.getCertificateUrl())) {
            remark.append("；证书已归档");
        }
        if (StringUtils.hasText(assignment.getMaterialUrl())) {
            remark.append("；材料已归档");
        }
        if (assignment.getEvaluationScore() != null) {
            remark.append("；评价：").append(assignment.getEvaluationScore()).append("分");
        }
        if (assignment.getRetrainDate() != null) {
            remark.append("；复训日期：").append(assignment.getRetrainDate());
        }
        String evaluationFeedback = trimText(assignment.getEvaluationFeedback(), 120);
        if (StringUtils.hasText(evaluationFeedback)) {
            remark.append("；反馈：").append(evaluationFeedback);
        }
        if (StringUtils.hasText(assignment.getRemark())) {
            remark.append("；备注：").append(assignment.getRemark().trim());
        }
        return remark.toString();
    }

    private void syncTrainingMaterial(EmployeeTrainingDO archive) {
        if (archive.getId() == null) {
            return;
        }
        String fileUrl = firstText(archive.getCertificateUrl(), archive.getMaterialUrl());
        if (!StringUtils.hasText(fileUrl)) {
            return;
        }
        EmployeeMaterialDO existing = employeeMaterialMapper.selectBySource(SOURCE_TRAINING_RECORD, archive.getId());
        EmployeeMaterialDO material = existing == null ? new EmployeeMaterialDO() : existing;
        if (existing != null) {
            material.setId(existing.getId());
        }
        material.setProfileId(archive.getProfileId());
        material.setCategory(MATERIAL_CATEGORY_TRAINING);
        material.setMaterialType(StringUtils.hasText(archive.getCertificateUrl())
                ? MATERIAL_TYPE_CERTIFICATE : MATERIAL_TYPE_MATERIAL);
        material.setMaterialName(firstText(archive.getCertificateName(), archive.getMaterialName(), archive.getTrainingName(), "培训材料"));
        material.setFileUrl(fileUrl);
        material.setFileName(material.getMaterialName());
        material.setIssueDate(archive.getEndDate());
        material.setExpireDate(firstDate(archive.getCertificateExpireDate(), archive.getRetrainDate()));
        material.setStatus(MATERIAL_STATUS_ACTIVE);
        material.setSourceType(SOURCE_TRAINING_RECORD);
        material.setSourceId(archive.getId());
        material.setRemark(firstText(archive.getRemark(), "培训学习完成后自动沉淀"));
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

    private Long firstLong(Long... values) {
        for (Long value : values) {
            if (value != null) {
                return value;
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

    private Integer firstInteger(Integer... values) {
        for (Integer value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private LocalDate calculateRetrainDate(LocalDate baseDate, TrainingPlanDO plan, TrainingCourseDO course) {
        Integer months = firstInteger(plan == null ? null : plan.getRetrainCycleMonths(),
                course == null ? null : course.getRetrainCycleMonths());
        if (baseDate == null || months == null || months <= 0) {
            return null;
        }
        return baseDate.plusMonths(months);
    }

    private LocalDate calculateReminderDate(LocalDate retrainDate, TrainingPlanDO plan, TrainingCourseDO course) {
        if (retrainDate == null) {
            return null;
        }
        Integer days = firstInteger(plan == null ? null : plan.getReminderDays(),
                course == null ? null : course.getDefaultReminderDays(), DEFAULT_RETRAIN_REMINDER_DAYS);
        return retrainDate.minusDays(Math.max(days, 0));
    }

    private String trimText(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String text = value.trim();
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private TrainingCourseDO validateCourseExists(Long id) {
        if (id == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "课程不能为空");
        }
        TrainingCourseDO course = trainingCourseMapper.selectById(id);
        if (course == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND, "课程不存在");
        }
        return course;
    }

    private TrainingPlanDO validatePlanExists(Long id) {
        if (id == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "学习计划不能为空");
        }
        TrainingPlanDO plan = trainingPlanMapper.selectById(id);
        if (plan == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND, "学习计划不存在");
        }
        return plan;
    }

    private TrainingAssignmentDO validateAssignmentExists(Long id) {
        if (id == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "学习任务不能为空");
        }
        TrainingAssignmentDO assignment = trainingAssignmentMapper.selectById(id);
        if (assignment == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND, "学习任务不存在");
        }
        return assignment;
    }

    private boolean preparePlanCourseFilter(TrainingPlanPageReqVO reqVO) {
        if (!StringUtils.hasText(reqVO.getCourseName())) {
            return true;
        }
        List<TrainingCourseDO> courses = trainingCourseMapper.selectList(new LambdaQueryWrapperX<TrainingCourseDO>()
                .likeIfPresent(TrainingCourseDO::getCourseName, reqVO.getCourseName())
                .last("LIMIT 1000"));
        Set<Long> courseIds = courses.stream()
                .map(TrainingCourseDO::getId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(HashSet::new));
        if (courseIds.isEmpty()) {
            return false;
        }
        reqVO.setCourseIds(courseIds);
        return true;
    }

    private boolean prepareAssignmentProfileFilter(TrainingAssignmentPageReqVO reqVO) {
        if (Boolean.TRUE.equals(reqVO.getMine())) {
            Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
            if (loginUserId == null) {
                return false;
            }
            EmployeeProfileDO profile = employeeProfileMapper.selectByUserId(loginUserId);
            if (profile == null || (reqVO.getProfileId() != null && !Objects.equals(reqVO.getProfileId(), profile.getId()))) {
                return false;
            }
            reqVO.setProfileId(profile.getId());
            return true;
        }
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

    private boolean hasAssignmentScope(TrainingPlanSaveReqVO reqVO) {
        return reqVO.getProfileIds() != null || reqVO.getDeptIds() != null;
    }

    private Set<Long> resolveTargetProfileIds(Set<Long> profileIds, Set<Long> deptIds) {
        Set<Long> result = new LinkedHashSet<>();
        if (profileIds != null) {
            result.addAll(profileIds);
        }
        Set<Long> expandedDeptIds = expandDeptIds(deptIds);
        if (!expandedDeptIds.isEmpty()) {
            List<EmployeeEntryDO> entries = employeeEntryMapper.selectList(new LambdaQueryWrapperX<EmployeeEntryDO>()
                    .in(EmployeeEntryDO::getDeptId, expandedDeptIds)
                    .in(EmployeeEntryDO::getWorkStatus, Arrays.asList(2, 3)));
            entries.stream()
                    .map(EmployeeEntryDO::getProfileId)
                    .filter(id -> id != null)
                    .forEach(result::add);
        }
        return result;
    }

    private Set<Long> expandDeptIds(Set<Long> deptIds) {
        Set<Long> result = new LinkedHashSet<>();
        if (deptIds == null || deptIds.isEmpty()) {
            return result;
        }
        for (Long deptId : deptIds) {
            if (deptId == null || !result.add(deptId)) {
                continue;
            }
            try {
                List<DeptRespDTO> children = deptApi.getChildDeptList(deptId).getCheckedData();
                if (children != null) {
                    children.stream()
                            .map(DeptRespDTO::getId)
                            .filter(id -> id != null)
                            .forEach(result::add);
                }
            } catch (Exception ex) {
                // 部门 RPC 失败时保留当前部门，避免草稿保存被外部服务抖动阻断。
            }
        }
        return result;
    }

    private String buildTargetSummary(Set<Long> profileIds, Set<Long> deptIds, Set<Long> resolvedProfileIds) {
        List<String> parts = new ArrayList<>();
        if (deptIds != null && !deptIds.isEmpty()) {
            parts.add("部门 " + deptIds.size() + " 个");
        }
        if (profileIds != null && !profileIds.isEmpty()) {
            parts.add("员工 " + profileIds.size() + " 人");
        }
        parts.add("生成任务 " + (resolvedProfileIds == null ? 0 : resolvedProfileIds.size()) + " 人");
        return String.join("，", parts);
    }

    private void syncAssignments(TrainingPlanDO plan, Set<Long> profileIds) {
        if (profileIds == null) {
            return;
        }
        TrainingCourseDO course = plan.getCourseId() == null ? null : trainingCourseMapper.selectById(plan.getCourseId());
        Map<Long, EmployeeProfileDO> profileMap = loadProfileMap(profileIds);
        if (profileMap.size() != profileIds.size()) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "存在无效员工档案");
        }
        List<TrainingAssignmentDO> existingList = trainingAssignmentMapper.selectListByPlanId(plan.getId());
        Map<Long, TrainingAssignmentDO> existingMap = existingList.stream()
                .filter(item -> item.getProfileId() != null)
                .collect(Collectors.toMap(TrainingAssignmentDO::getProfileId, item -> item, (left, right) -> left));
        for (Long profileId : profileIds) {
            TrainingAssignmentDO existingAssignment = existingMap.get(profileId);
            if (existingAssignment != null) {
                refreshOpenAssignmentFromPlan(existingAssignment, plan, course);
                continue;
            }
            EmployeeProfileDO profile = profileMap.get(profileId);
            TrainingAssignmentDO assignment = buildAssignment(plan, course, profile);
            trainingAssignmentMapper.insert(assignment);
        }
        for (TrainingAssignmentDO existing : existingList) {
            if (profileIds.contains(existing.getProfileId())
                    || ASSIGNMENT_COMPLETED.equals(existing.getStatus())
                    || ASSIGNMENT_CANCELED.equals(existing.getStatus())) {
                continue;
            }
            TrainingAssignmentDO updateDO = new TrainingAssignmentDO();
            updateDO.setId(existing.getId());
            updateDO.setStatus(ASSIGNMENT_CANCELED);
            trainingAssignmentMapper.updateById(updateDO);
        }
    }

    private void applyPlanDefaults(TrainingPlanDO plan, TrainingCourseDO course) {
        if (plan == null || course == null) {
            return;
        }
        if (plan.getExamId() == null) {
            plan.setExamId(course.getExamId());
        }
        if (plan.getQuestionnaireId() == null) {
            plan.setQuestionnaireId(course.getQuestionnaireId());
        }
        if (plan.getRetrainCycleMonths() == null) {
            plan.setRetrainCycleMonths(course.getRetrainCycleMonths());
        }
        if (plan.getReminderDays() == null) {
            plan.setReminderDays(firstInteger(course.getDefaultReminderDays(), DEFAULT_RETRAIN_REMINDER_DAYS));
        }
        if (plan.getRequireEvaluation() == null) {
            plan.setRequireEvaluation(Boolean.TRUE);
        }
    }

    private TrainingAssignmentDO buildAssignment(TrainingPlanDO plan, TrainingCourseDO course, EmployeeProfileDO profile) {
        TrainingAssignmentDO assignment = new TrainingAssignmentDO();
        assignment.setPlanId(plan.getId());
        assignment.setCourseId(plan.getCourseId());
        assignment.setProfileId(profile.getId());
        assignment.setUserId(profile.getUserId());
        assignment.setStatus(ASSIGNMENT_NOT_STARTED);
        assignment.setProgress(0);
        applyAssignmentLearningFields(assignment, plan, course);
        return assignment;
    }

    private void validateCourseEnabled(TrainingCourseDO course) {
        if (course != null && course.getStatus() != null && course.getStatus() != COURSE_STATUS_ENABLED) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "课程已停用，不能用于学习计划");
        }
    }

    private void validatePlanDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "计划结束日期不能早于开始日期");
        }
    }

    private void validateDraftPlan(TrainingPlanDO plan, String message) {
        if (plan == null || !PLAN_DRAFT.equals(plan.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, message);
        }
    }

    private void validateAssignmentProgress(Integer progress) {
        if (progress == null) {
            return;
        }
        if (progress < 0 || progress > 100) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "学习进度只能在 0-100 之间");
        }
    }

    private String normalizeAssignmentTargetStatus(TrainingAssignmentDO assignment, TrainingAssignmentUpdateReqVO updateReqVO) {
        String status = trimText(updateReqVO.getStatus(), 32);
        if (!StringUtils.hasText(status)) {
            status = resolveStatusByProgress(assignment.getStatus(), updateReqVO.getProgress());
        }
        if (!ASSIGNMENT_NOT_STARTED.equals(status)
                && !ASSIGNMENT_IN_PROGRESS.equals(status)
                && !ASSIGNMENT_COMPLETED.equals(status)
                && !ASSIGNMENT_CANCELED.equals(status)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "学习任务状态不合法");
        }
        return status;
    }

    private void validateAssignmentWritable(TrainingPlanDO plan, TrainingAssignmentDO assignment, String targetStatus, boolean mineOnly) {
        if (ASSIGNMENT_CANCELED.equals(assignment.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "已取消学习任务不能继续更新");
        }
        if (plan != null && PLAN_CLOSED.equals(plan.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "学习计划已关闭，不能继续更新任务");
        }
        if (mineOnly && (plan == null || !PLAN_PUBLISHED.equals(plan.getStatus()))) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "学习计划尚未发布，不能更新学习进度");
        }
        if (mineOnly && ASSIGNMENT_CANCELED.equals(targetStatus)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "员工端不能取消学习任务");
        }
        if (ASSIGNMENT_COMPLETED.equals(assignment.getStatus()) && !ASSIGNMENT_COMPLETED.equals(targetStatus)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "已完成学习任务不能回退状态");
        }
        if (plan != null && PLAN_DRAFT.equals(plan.getStatus())
                && (ASSIGNMENT_IN_PROGRESS.equals(targetStatus) || ASSIGNMENT_COMPLETED.equals(targetStatus))) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "草稿学习计划不能更新学习进度");
        }
    }

    private void validateAssignmentCompletion(TrainingAssignmentUpdateReqVO updateReqVO, TrainingAssignmentDO assignment) {
        if (!StringUtils.hasText(updateReqVO.getResult()) && !StringUtils.hasText(assignment.getResult())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "完成学习任务时需要填写学习结果");
        }
    }

    private void refreshOpenAssignmentFromPlan(TrainingAssignmentDO existing, TrainingPlanDO plan, TrainingCourseDO course) {
        if (!isAssignmentOpen(existing)) {
            return;
        }
        TrainingAssignmentDO updateDO = new TrainingAssignmentDO();
        updateDO.setId(existing.getId());
        updateDO.setCourseId(plan.getCourseId());
        applyAssignmentLearningFields(updateDO, plan, course);
        trainingAssignmentMapper.updateById(updateDO);
    }

    private void applyAssignmentLearningFields(TrainingAssignmentDO assignment, TrainingPlanDO plan, TrainingCourseDO course) {
        assignment.setMaterialName(course == null ? null : course.getMaterialName());
        assignment.setMaterialUrl(course == null ? null : firstText(course.getMaterialUrl(), course.getContentUrl()));
        assignment.setExamId(firstLong(plan == null ? null : plan.getExamId(), course == null ? null : course.getExamId()));
        assignment.setQuestionnaireId(firstLong(plan == null ? null : plan.getQuestionnaireId(),
                course == null ? null : course.getQuestionnaireId()));
        LocalDate baseDate = plan == null ? null : plan.getEndDate();
        LocalDate retrainDate = calculateRetrainDate(baseDate, plan, course);
        assignment.setRetrainDate(retrainDate);
        assignment.setReminderDate(calculateReminderDate(retrainDate, plan, course));
    }

    private void fillPlanCourseInfo(List<TrainingPlanRespVO> respList) {
        if (respList == null || respList.isEmpty()) {
            return;
        }
        Set<Long> courseIds = respList.stream()
                .map(TrainingPlanRespVO::getCourseId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(HashSet::new));
        Map<Long, TrainingCourseDO> courseMap = loadCourseMap(courseIds);
        for (TrainingPlanRespVO item : respList) {
            TrainingCourseDO course = courseMap.get(item.getCourseId());
            if (course != null) {
                item.setCourseName(course.getCourseName());
            }
        }
    }

    private void fillPlanAssignmentStats(List<TrainingPlanRespVO> respList) {
        if (respList == null || respList.isEmpty()) {
            return;
        }
        Set<Long> planIds = respList.stream()
                .map(TrainingPlanRespVO::getId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(HashSet::new));
        List<TrainingAssignmentDO> assignments = trainingAssignmentMapper.selectListByPlanIds(planIds);
        Map<Long, List<TrainingAssignmentDO>> assignmentMap = assignments.stream()
                .collect(Collectors.groupingBy(TrainingAssignmentDO::getPlanId));
        LocalDate today = LocalDate.now();
        for (TrainingPlanRespVO item : respList) {
            List<TrainingAssignmentDO> planAssignments = assignmentMap.getOrDefault(item.getId(), new ArrayList<>());
            int assignmentCount = planAssignments.size();
            int completedCount = (int) planAssignments.stream().filter(this::isAssignmentCompleted).count();
            List<Integer> scores = planAssignments.stream()
                    .map(TrainingAssignmentDO::getEvaluationScore)
                    .filter(score -> score != null)
                    .collect(Collectors.toList());
            item.setAssignmentCount(planAssignments.size());
            item.setCompletedCount(completedCount);
            item.setInProgressCount((int) planAssignments.stream().filter(a -> ASSIGNMENT_IN_PROGRESS.equals(a.getStatus())).count());
            item.setNotStartedCount((int) planAssignments.stream().filter(a -> ASSIGNMENT_NOT_STARTED.equals(a.getStatus())).count());
            item.setOverdueCount((int) planAssignments.stream()
                    .filter(a -> isAssignmentOpen(a) && item.getEndDate() != null && item.getEndDate().isBefore(today))
                    .count());
            item.setCompletionRate(percent(completedCount, assignmentCount));
            item.setEvaluationCount(scores.size());
            item.setAverageEvaluationScore(average(scores));
            item.setSatisfactionRate(percent((int) scores.stream().filter(score -> score >= 4).count(), scores.size()));
        }
    }

    private void fillAssignmentInfo(List<TrainingAssignmentRespVO> respList) {
        if (respList == null || respList.isEmpty()) {
            return;
        }
        Set<Long> planIds = new HashSet<>();
        Set<Long> courseIds = new HashSet<>();
        Set<Long> profileIds = new HashSet<>();
        for (TrainingAssignmentRespVO item : respList) {
            if (item.getPlanId() != null) {
                planIds.add(item.getPlanId());
            }
            if (item.getCourseId() != null) {
                courseIds.add(item.getCourseId());
            }
            if (item.getProfileId() != null) {
                profileIds.add(item.getProfileId());
            }
        }
        Map<Long, TrainingPlanDO> planMap = loadPlanMap(planIds);
        Map<Long, TrainingCourseDO> courseMap = loadCourseMap(courseIds);
        Map<Long, EmployeeProfileDO> profileMap = loadProfileMap(profileIds);
        for (TrainingAssignmentRespVO item : respList) {
            TrainingPlanDO plan = planMap.get(item.getPlanId());
            if (plan != null) {
                item.setPlanName(plan.getPlanName());
                item.setStartDate(plan.getStartDate());
                item.setEndDate(plan.getEndDate());
                if (item.getExamId() == null) {
                    item.setExamId(plan.getExamId());
                }
                if (item.getQuestionnaireId() == null) {
                    item.setQuestionnaireId(plan.getQuestionnaireId());
                }
            }
            TrainingCourseDO course = courseMap.get(item.getCourseId());
            if (course != null) {
                item.setCourseName(course.getCourseName());
                item.setCourseType(course.getCourseType());
                item.setCategory(course.getCategory());
                item.setLecturer(course.getLecturer());
                item.setProvider(course.getProvider());
                item.setDurationHours(course.getDurationHours());
                item.setContentUrl(course.getContentUrl());
                if (!StringUtils.hasText(item.getMaterialName())) {
                    item.setMaterialName(course.getMaterialName());
                }
                if (!StringUtils.hasText(item.getMaterialUrl())) {
                    item.setMaterialUrl(firstText(course.getMaterialUrl(), course.getContentUrl()));
                }
                if (item.getExamId() == null) {
                    item.setExamId(course.getExamId());
                }
                if (item.getQuestionnaireId() == null) {
                    item.setQuestionnaireId(course.getQuestionnaireId());
                }
            }
            EmployeeProfileDO profile = profileMap.get(item.getProfileId());
            if (profile != null) {
                item.setProfileName(profile.getName());
                item.setProfileMobile(profile.getMobile());
            }
        }
    }

    private Map<Long, TrainingCourseDO> loadCourseMap(Set<Long> courseIds) {
        Map<Long, TrainingCourseDO> courseMap = new HashMap<>();
        if (courseIds == null || courseIds.isEmpty()) {
            return courseMap;
        }
        for (TrainingCourseDO course : trainingCourseMapper.selectBatchIds(courseIds)) {
            if (course.getId() != null) {
                courseMap.put(course.getId(), course);
            }
        }
        return courseMap;
    }

    private Map<Long, TrainingPlanDO> loadPlanMap(Set<Long> planIds) {
        Map<Long, TrainingPlanDO> planMap = new HashMap<>();
        if (planIds == null || planIds.isEmpty()) {
            return planMap;
        }
        for (TrainingPlanDO plan : trainingPlanMapper.selectBatchIds(planIds)) {
            if (plan.getId() != null) {
                planMap.put(plan.getId(), plan);
            }
        }
        return planMap;
    }

    private Map<Long, EmployeeProfileDO> loadProfileMap(Set<Long> profileIds) {
        Map<Long, EmployeeProfileDO> profileMap = new HashMap<>();
        if (profileIds == null || profileIds.isEmpty()) {
            return profileMap;
        }
        for (EmployeeProfileDO profile : employeeProfileMapper.selectBatchIds(profileIds)) {
            if (profile.getId() != null) {
                profileMap.put(profile.getId(), profile);
            }
        }
        return profileMap;
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

    private boolean isAssignmentOpen(TrainingAssignmentDO assignment) {
        return ASSIGNMENT_NOT_STARTED.equals(assignment.getStatus()) || ASSIGNMENT_IN_PROGRESS.equals(assignment.getStatus());
    }

    private void refreshTodoTasksQuietly() {
        try {
            hrTodoTaskService.refreshGeneratedTasks();
        } catch (Exception ex) {
            // 待办刷新失败不影响培训主流程。
        }
    }

    private boolean isAssignmentCompleted(TrainingAssignmentDO assignment) {
        return ASSIGNMENT_COMPLETED.equals(assignment.getStatus());
    }

    private String generatePlanCode() {
        return "TP" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

}
