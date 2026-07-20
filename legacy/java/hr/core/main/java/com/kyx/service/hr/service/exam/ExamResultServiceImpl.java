package com.kyx.service.hr.service.exam;

import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.common.exception.enums.GlobalErrorCodeConstants;
import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.ai.api.exam.AiExamGradeApi;
import com.kyx.service.ai.api.exam.dto.AiExamGradeReqDTO;
import com.kyx.service.ai.api.exam.dto.AiExamGradeRespDTO;
import com.kyx.foundation.security.core.service.SecurityFrameworkService;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import com.kyx.service.business.enums.permission.RoleCodeEnum;
import com.kyx.service.hr.controller.admin.exam.vo.ExamResultRespVO;
import com.kyx.service.hr.dal.dataobject.exam.ExamAnswerDO;
import com.kyx.service.hr.dal.dataobject.exam.ExamAttemptDO;
import com.kyx.service.hr.dal.dataobject.exam.ExamDO;
import com.kyx.service.hr.dal.dataobject.exam.ExamPaperItemDO;
import com.kyx.service.hr.dal.mysql.exam.ExamAnswerMapper;
import com.kyx.service.hr.dal.mysql.exam.ExamAttemptMapper;
import com.kyx.service.hr.dal.mysql.exam.ExamMapper;
import com.kyx.service.hr.dal.mysql.exam.ExamPaperItemMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.kyx.foundation.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * HR 考试结果 Service 实现
 *
 * @author MK
 */
@Service
@Slf4j
public class ExamResultServiceImpl implements ExamResultService {

    private static final String GRADE_MODE_AI = "AI";
    private static final String GRADE_MODE_MANUAL = "MANUAL";
    private static final Integer GRADE_STATUS_PENDING = 0;
    private static final Integer GRADE_STATUS_DONE = 1;
    private static final Integer GRADE_STATUS_FAILED = 2;

    @Resource
    private ExamAttemptMapper attemptMapper;
    @Resource
    private ExamMapper examMapper;
    @Resource
    private ExamAnswerMapper answerMapper;
    @Resource
    private ExamPaperItemMapper itemMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private AiExamGradeApi aiExamGradeApi;
    @Resource
    private SecurityFrameworkService securityFrameworkService;
    @Resource
    private ExamViewScopeSupport examViewScopeSupport;
    @Resource(name = "applicationTaskExecutor")
    private Executor applicationTaskExecutor;

    @Override
    public List<ExamResultRespVO> getResultList(Long examId) {
        return getResultList(examId, null);
    }

    @Override
    public List<ExamResultRespVO> getResultList(Long examId, Long publishId) {
        if (examId == null) {
            return Collections.emptyList();
        }
        ExamDO exam = examMapper.selectById(examId);
        validateExamAccess(exam);

        List<ExamAttemptDO> attempts;
        if (publishId != null) {
            attempts = attemptMapper.selectList(
                    new LambdaQueryWrapper<ExamAttemptDO>()
                            .eq(ExamAttemptDO::getExamId, examId)
                            .eq(ExamAttemptDO::getPublishId, publishId));
        } else {
            attempts = attemptMapper.selectListByExamId(examId);
        }

        if (attempts.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> userIds = attempts.stream()
                .map(ExamAttemptDO::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        Set<Long> attemptIds = attempts.stream()
                .map(ExamAttemptDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, List<ExamAnswerDO>> answerMap = attemptIds.isEmpty() ? Collections.emptyMap()
                : answerMapper.selectListByAttemptIds(attemptIds).stream()
                .collect(Collectors.groupingBy(ExamAnswerDO::getAttemptId));
        Set<Long> paperIds = attempts.stream()
                .map(ExamAttemptDO::getPaperId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<ExamPaperItemDO> paperItems = paperIds.isEmpty() ? Collections.emptyList()
                : itemMapper.selectListByPaperIdsIncludeDeleted(paperIds);
        Map<Long, ExamPaperItemDO> itemMap = paperItems.stream()
                .collect(Collectors.toMap(ExamPaperItemDO::getId, Function.identity(), (a, b) -> a));
        Map<Long, List<ExamPaperItemDO>> itemMapByPaperId = paperItems.stream()
                .collect(Collectors.groupingBy(ExamPaperItemDO::getPaperId));
        List<ExamResultRespVO> list = new ArrayList<>();
        for (ExamAttemptDO attempt : attempts) {
            if (attempt.getStatus() == null || attempt.getStatus() != 1) {
                continue;
            }
            ExamResultRespVO resp = BeanUtils.toBean(attempt, ExamResultRespVO.class);
            resp.setAttemptId(attempt.getId());
            AdminUserRespDTO user = userMap.get(attempt.getUserId());
            resp.setUserName(user != null && user.getNickname() != null ? user.getNickname() : user != null ? user.getUsername() : null);
            if (exam != null && exam.getPassScore() != null && attempt.getTotalScore() != null) {
                resp.setPassFlag(attempt.getTotalScore() >= exam.getPassScore());
            } else {
                resp.setPassFlag(null);
            }
            resp.setAnswers(buildAnswers(
                    answerMap.getOrDefault(attempt.getId(), Collections.emptyList()),
                    itemMap,
                    itemMapByPaperId.getOrDefault(attempt.getPaperId(), Collections.emptyList())));
            list.add(resp);
        }
        return list;
    }

    private void validateExamAccess(ExamDO exam) {
        if (exam == null || isAdmin()) {
            return;
        }
        Long loginUserId = getLoginUserId();
        if (loginUserId == null || !String.valueOf(loginUserId).equals(exam.getCreator())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.FORBIDDEN, "无权查看该考试结果");
        }
    }

    private List<ExamResultRespVO.AnswerRespVO> buildAnswers(List<ExamAnswerDO> answers, Map<Long, ExamPaperItemDO> itemMap, List<ExamPaperItemDO> paperItems) {
        if (paperItems != null && !paperItems.isEmpty()) {
            Map<Long, ExamAnswerDO> answerMap = answers.stream()
                    .filter(answer -> answer.getItemId() != null)
                    .collect(Collectors.toMap(ExamAnswerDO::getItemId, Function.identity(), (a, b) -> a));
            return paperItems.stream()
                    .map(item -> buildAnswer(answerMap.get(item.getId()), item))
                    .sorted(Comparator.comparing(ExamResultRespVO.AnswerRespVO::getSortNo, Comparator.nullsLast(Integer::compareTo)))
                    .collect(Collectors.toList());
        }
        return answers.stream()
                .map(answer -> buildAnswer(answer, itemMap.get(answer.getItemId())))
                .sorted(Comparator.comparing(ExamResultRespVO.AnswerRespVO::getSortNo, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());
    }

    private ExamResultRespVO.AnswerRespVO buildAnswer(ExamAnswerDO answer, ExamPaperItemDO item) {
        ExamResultRespVO.AnswerRespVO resp = answer != null
                ? BeanUtils.toBean(answer, ExamResultRespVO.AnswerRespVO.class)
                : new ExamResultRespVO.AnswerRespVO();
        if (answer != null) {
            resp.setAnswerId(answer.getId());
        }
        if (item != null) {
            resp.setItemId(item.getId());
            resp.setSortNo(item.getSortNo());
            resp.setTitle(item.getTitle());
            resp.setItemType(item.getItemType());
            resp.setOptionsJson(item.getOptionsJson());
            resp.setStandardAnswerJson(item.getAnswerJson());
            resp.setMaxScore(item.getScore());
        }
        return resp;
    }

    @Override
    public void retryAiGrade(Long attemptId) {
        scheduleAiGrade(attemptId, TenantContextHolder.getTenantId());
    }

    @Override
    public void retryAiGradeBatch(List<Long> attemptIds) {
        if (attemptIds == null || attemptIds.isEmpty()) {
            return;
        }
        attemptIds.stream().filter(Objects::nonNull).forEach(this::retryAiGrade);
    }

    @Override
    public void pauseAiGrade(Long attemptId) {
        ExamAttemptDO attempt = attemptMapper.selectById(attemptId);
        if (attempt == null) {
            return;
        }
        ExamAttemptDO modeUpdate = new ExamAttemptDO();
        modeUpdate.setId(attemptId);
        modeUpdate.setGradeMode(GRADE_MODE_MANUAL);
        modeUpdate.setGradeError(null);
        attemptMapper.updateById(modeUpdate);
        for (ExamAnswerDO answer : answerMapper.selectListByAttemptId(attemptId)) {
            if (!GRADE_STATUS_PENDING.equals(answer.getGradeStatus())) {
                continue;
            }
            ExamAnswerDO update = new ExamAnswerDO();
            update.setId(answer.getId());
            update.setAnswerScore(answer.getAnswerScore() == null ? 0 : answer.getAnswerScore());
            update.setGradeStatus(GRADE_STATUS_DONE);
            update.setGradeError(null);
            answerMapper.updateById(update);
        }
        refreshAttemptScore(attemptId, GRADE_STATUS_DONE, null);
    }

    @Override
    public void pauseAiGradeBatch(List<Long> attemptIds) {
        if (attemptIds == null || attemptIds.isEmpty()) {
            return;
        }
        attemptIds.stream().filter(Objects::nonNull).forEach(this::pauseAiGrade);
    }

    @Override
    public void updateManualScore(Long answerId, Integer score) {
        ExamAnswerDO answer = answerMapper.selectById(answerId);
        if (answer == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND);
        }
        ExamAttemptDO attempt = attemptMapper.selectById(answer.getAttemptId());
        ExamDO exam = examMapper.selectById(attempt.getExamId());
        validateExamAccess(exam);
        ExamPaperItemDO item = itemMapper.selectByIdIncludeDeleted(answer.getItemId());
        Integer safeScore = normalizeAiScore(score, item != null ? item.getScore() : null);
        ExamAnswerDO update = new ExamAnswerDO();
        update.setId(answerId);
        update.setManualScore(safeScore);
        update.setAnswerScore(safeScore);
        update.setGradeStatus(GRADE_STATUS_DONE);
        answerMapper.updateById(update);
        refreshAttemptScore(answer.getAttemptId(), GRADE_STATUS_DONE, null);
    }

    private void scheduleAiGrade(Long attemptId, Long tenantId) {
        CompletableFuture.runAsync(() -> {
            try {
                if (tenantId != null) {
                    TenantUtils.execute(tenantId, () -> gradeAttemptByAi(attemptId));
                } else {
                    gradeAttemptByAi(attemptId);
                }
            } catch (Exception ex) {
                if (isAiGradeActive(attemptId)) {
                    markAttemptGradeFailed(attemptId, ex.getMessage());
                }
            }
        }, applicationTaskExecutor);
    }

    private void gradeAttemptByAi(Long attemptId) {
        ExamAttemptDO attempt = attemptMapper.selectById(attemptId);
        if (attempt == null) {
            return;
        }
        ExamAttemptDO pending = new ExamAttemptDO();
        pending.setId(attemptId);
        pending.setGradeMode(GRADE_MODE_AI);
        pending.setGradeStatus(GRADE_STATUS_PENDING);
        pending.setGradeError(null);
        attemptMapper.updateById(pending);
        List<ExamPaperItemDO> items = itemMapper.selectListByPaperIdsIncludeDeleted(Collections.singletonList(attempt.getPaperId()));
        Map<Long, ExamPaperItemDO> itemMap = items.stream().collect(Collectors.toMap(ExamPaperItemDO::getId, Function.identity(), (a, b) -> a));
        boolean failed = false;
        String error = null;
        for (ExamAnswerDO answer : answerMapper.selectListByAttemptId(attemptId)) {
            ExamPaperItemDO item = itemMap.get(answer.getItemId());
            if (!needsAiGrade(item)) {
                continue;
            }
            if (!isAiGradeActive(attemptId)) {
                return;
            }
            if (!hasUserAnswer(answer)) {
                ExamAnswerDO update = new ExamAnswerDO();
                update.setId(answer.getId());
                update.setAnswerScore(0);
                update.setAiComment(null);
                update.setGradeStatus(GRADE_STATUS_DONE);
                update.setGradeError(null);
                answerMapper.updateById(update);
                continue;
            }
            try {
                AiExamGradeRespDTO result = requestAiGrade(item, answer);
                if (!isAiGradeActive(attemptId)) {
                    return;
                }
                ExamAnswerDO update = new ExamAnswerDO();
                update.setId(answer.getId());
                update.setAnswerScore(normalizeAiScore(result.getScore(), item.getScore()));
                update.setAiComment(result.getComment());
                update.setGradeStatus(GRADE_STATUS_DONE);
                update.setGradeError(null);
                answerMapper.updateById(update);
            } catch (Exception ex) {
                if (!isAiGradeActive(attemptId)) {
                    return;
                }
                failed = true;
                error = ex.getMessage();
                ExamAnswerDO update = new ExamAnswerDO();
                update.setId(answer.getId());
                update.setGradeStatus(GRADE_STATUS_FAILED);
                update.setGradeError(error);
                answerMapper.updateById(update);
            }
        }
        if (!isAiGradeActive(attemptId)) {
            return;
        }
        refreshAttemptScore(attemptId, failed ? GRADE_STATUS_FAILED : GRADE_STATUS_DONE, error);
    }

    private AiExamGradeRespDTO requestAiGrade(ExamPaperItemDO item, ExamAnswerDO answer) {
        AiExamGradeReqDTO reqDTO = new AiExamGradeReqDTO();
        reqDTO.setQuestionTitle(item.getTitle());
        reqDTO.setItemType(item.getItemType());
        reqDTO.setStandardAnswer(String.valueOf(extractAnswerValue(item.getAnswerJson())));
        reqDTO.setUserAnswer(answer.getAnswerText() != null ? answer.getAnswerText() : String.valueOf(extractAnswerValue(answer.getAnswerJson())));
        reqDTO.setMaxScore(item.getScore());
        return aiExamGradeApi.grade(reqDTO).getCheckedData();
    }

    private Integer normalizeAiScore(Integer score, Integer maxScore) {
        if (score == null) {
            return 0;
        }
        int safeMaxScore = maxScore == null ? 0 : maxScore;
        return Math.max(0, Math.min(score, safeMaxScore));
    }

    private void refreshAttemptScore(Long attemptId, Integer gradeStatus, String gradeError) {
        List<ExamAnswerDO> answers = answerMapper.selectListByAttemptId(attemptId);
        int totalScore = answers.stream()
                .map(ExamAnswerDO::getAnswerScore)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        Integer finalStatus = gradeStatus;
        String finalError = gradeError;
        boolean hasPending = false;
        for (ExamAnswerDO answer : answers) {
            if (GRADE_STATUS_FAILED.equals(answer.getGradeStatus())) {
                finalStatus = GRADE_STATUS_FAILED;
                if (answer.getGradeError() != null && !answer.getGradeError().trim().isEmpty()) {
                    finalError = answer.getGradeError();
                }
                break;
            }
            if (GRADE_STATUS_PENDING.equals(answer.getGradeStatus())) {
                hasPending = true;
            }
        }
        if (!GRADE_STATUS_FAILED.equals(finalStatus)) {
            finalStatus = hasPending ? GRADE_STATUS_PENDING : GRADE_STATUS_DONE;
            finalError = null;
        }
        attemptMapper.update(null, new LambdaUpdateWrapper<ExamAttemptDO>()
                .eq(ExamAttemptDO::getId, attemptId)
                .set(ExamAttemptDO::getTotalScore, totalScore)
                .set(ExamAttemptDO::getGradeStatus, finalStatus)
                .set(ExamAttemptDO::getGradeError, finalError));
    }

    private void markAttemptGradeFailed(Long attemptId, String error) {
        ExamAttemptDO update = new ExamAttemptDO();
        update.setId(attemptId);
        update.setGradeStatus(GRADE_STATUS_FAILED);
        update.setGradeError(error);
        attemptMapper.updateById(update);
    }

    private boolean needsAiGrade(ExamPaperItemDO item) {
        return item != null && ("blank".equals(item.getItemType()) || "short".equals(item.getItemType()));
    }

    private boolean isAiGradeActive(Long attemptId) {
        ExamAttemptDO attempt = attemptMapper.selectById(attemptId);
        return attempt != null && GRADE_MODE_AI.equalsIgnoreCase(attempt.getGradeMode())
                && GRADE_STATUS_PENDING.equals(attempt.getGradeStatus());
    }

    private boolean hasUserAnswer(ExamAnswerDO answer) {
        if (answer == null) {
            return false;
        }
        if (answer.getAnswerText() != null && !answer.getAnswerText().trim().isEmpty()) {
            return true;
        }
        Object value = extractAnswerValue(answer.getAnswerJson());
        if (value instanceof java.util.Collection) {
            for (Object item : (java.util.Collection<?>) value) {
                if (item != null && !String.valueOf(item).trim().isEmpty()) {
                    return true;
                }
            }
            return false;
        }
        return value != null && !String.valueOf(value).trim().isEmpty();
    }

    private Object extractAnswerValue(String value) {
        if (value == null || !shouldParseJsonValue(value.trim())) {
            return value;
        }
        Object parsed = com.kyx.foundation.common.util.json.JsonUtils.parseObject(value.trim(), Object.class);
        if (parsed instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) parsed;
            Object actual = map.get("value");
            if (actual == null) {
                actual = map.get("answer");
            }
            if (actual == null) {
                actual = map.get("answers");
            }
            if (actual == null) {
                actual = map.get("standardAnswer");
            }
            return actual;
        }
        return parsed;
    }

    private boolean shouldParseJsonValue(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        if (com.kyx.foundation.common.util.json.JsonUtils.isJson(trimmed)) {
            return true;
        }
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return true;
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if ("true".equals(lower) || "false".equals(lower) || "null".equals(lower)) {
            return true;
        }
        return trimmed.matches("-?(0|[1-9]\\d*)(\\.\\d+)?([eE][+-]?\\d+)?");
    }

    private boolean isAdmin() {
        return securityFrameworkService.hasRole(RoleCodeEnum.SUPER_ADMIN.getCode())
                || securityFrameworkService.hasRole(RoleCodeEnum.TENANT_ADMIN.getCode())
                || examViewScopeSupport.canViewAllData();
    }
}
