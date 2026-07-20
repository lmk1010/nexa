package com.kyx.service.hr.service.exam;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.kyx.foundation.common.exception.enums.GlobalErrorCodeConstants;
import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.util.json.JsonUtils;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.security.core.service.SecurityFrameworkService;
import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.ai.api.exam.AiExamGradeApi;
import com.kyx.service.ai.api.exam.dto.AiExamGradeReqDTO;
import com.kyx.service.ai.api.exam.dto.AiExamGradeRespDTO;
import com.kyx.service.business.enums.permission.RoleCodeEnum;
import com.kyx.service.hr.controller.admin.exam.vo.ExamAnswerSubmitReqVO;
import com.kyx.service.hr.controller.admin.exam.vo.ExamAttemptRespVO;
import com.kyx.service.hr.controller.admin.exam.vo.ExamPaperRespVO;
import com.kyx.service.hr.controller.admin.exam.vo.ExamReviewRespVO;
import com.kyx.service.hr.dal.dataobject.exam.ExamAttemptDO;
import com.kyx.service.hr.dal.dataobject.exam.ExamAnswerDO;
import com.kyx.service.hr.dal.dataobject.exam.ExamDO;
import com.kyx.service.hr.dal.dataobject.exam.ExamPaperDO;
import com.kyx.service.hr.dal.dataobject.exam.ExamPaperItemDO;
import com.kyx.service.hr.dal.dataobject.exam.ExamPublishDO;
import com.kyx.service.hr.dal.mysql.exam.ExamAnswerMapper;
import com.kyx.service.hr.dal.mysql.exam.ExamAttemptMapper;
import com.kyx.service.hr.dal.mysql.exam.ExamMapper;
import com.kyx.service.hr.dal.mysql.exam.ExamPaperItemMapper;
import com.kyx.service.hr.dal.mysql.exam.ExamPaperMapper;
import com.kyx.service.hr.dal.mysql.exam.ExamPublishMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.kyx.service.hr.enums.ErrorCodeConstants.EXAM_ATTEMPT_FORBIDDEN;
import static com.kyx.service.hr.enums.ErrorCodeConstants.EXAM_ATTEMPT_LIMIT;
import static com.kyx.service.hr.enums.ErrorCodeConstants.EXAM_ATTEMPT_NOT_EXISTS;
import static com.kyx.service.hr.enums.ErrorCodeConstants.EXAM_ENDED;
import static com.kyx.service.hr.enums.ErrorCodeConstants.EXAM_NOT_IN_SCOPE;
import static com.kyx.service.hr.enums.ErrorCodeConstants.EXAM_NOT_EXISTS;
import static com.kyx.service.hr.enums.ErrorCodeConstants.EXAM_NOT_STARTED;
import static com.kyx.service.hr.enums.ErrorCodeConstants.EXAM_PAPER_NOT_CONFIGURED;
import static com.kyx.service.hr.enums.ErrorCodeConstants.EXAM_PUBLISH_NOT_EXISTS;

/**
 * HR 考试作答 Service 实现
 */
@Service
@Validated
@Slf4j
public class ExamAttemptServiceImpl implements ExamAttemptService {

    private static final String GRADE_MODE_AI = "AI";
    private static final String GRADE_MODE_MANUAL = "MANUAL";
    private static final Integer GRADE_STATUS_PENDING = 0;
    private static final Integer GRADE_STATUS_DONE = 1;
    private static final Integer GRADE_STATUS_FAILED = 2;
    private static final Pattern BLANK_MARKER_PATTERN = Pattern.compile(
            "_{2,}|＿{2,}|\\(\\s*\\)|（\\s*）|\\[\\s*\\]|【\\s*】");
    private static final Pattern BLANK_ANSWER_SEPARATOR_PATTERN = Pattern.compile(
            "[\\r\\n,，;；、]+");
    private static final Set<String> JUDGE_TRUE_VALUES = new HashSet<>(Arrays.asList(
            "true", "1", "yes", "y", "right", "correct", "对", "是", "正确", "√", "✓"));
    private static final Set<String> JUDGE_FALSE_VALUES = new HashSet<>(Arrays.asList(
            "false", "0", "no", "n", "wrong", "incorrect", "错", "否", "错误", "×", "x"));

    @Resource
    private ExamMapper examMapper;
    @Resource
    private ExamPaperMapper paperMapper;
    @Resource
    private ExamPaperItemMapper itemMapper;
    @Resource
    private ExamAttemptMapper attemptMapper;
    @Resource
    private ExamAnswerMapper answerMapper;
    @Resource
    private ExamPublishMapper publishMapper;
    @Resource
    private ExamScopeSupport examScopeSupport;
    @Resource
    private AiExamGradeApi aiExamGradeApi;
    @Resource
    private SecurityFrameworkService securityFrameworkService;
    @Resource(name = "applicationTaskExecutor")
    private Executor applicationTaskExecutor;
    @Resource
    private ExamViewScopeSupport examViewScopeSupport;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExamAttemptRespVO startExam(Long examId, Long publishId, Long userId) {
        ExamDO exam = examMapper.selectById(examId);
        if (exam == null) {
            throw ServiceExceptionUtil.exception(EXAM_NOT_EXISTS);
        }

        LocalDateTime now = LocalDateTime.now();
        Integer maxAttempts;
        LocalDateTime windowStart;
        LocalDateTime windowEnd;

        if (publishId != null) {
            // 按发布批次的时间窗口和限制
            ExamPublishDO publish = publishMapper.selectById(publishId);
            if (publish == null) {
                throw ServiceExceptionUtil.exception(EXAM_PUBLISH_NOT_EXISTS);
            }
            if (!examId.equals(publish.getExamId())) {
                throw ServiceExceptionUtil.exception(EXAM_ATTEMPT_FORBIDDEN);
            }
            if (!examScopeSupport.isUserInScope(
                    publish.getPublishScopeJson(),
                    userId,
                    publish.getTenantId())) {
                throw ServiceExceptionUtil.exception(EXAM_NOT_IN_SCOPE);
            }
            if (publish.getStatus() == null || publish.getStatus() != 1) {
                throw ServiceExceptionUtil.exception(EXAM_NOT_STARTED);
            }
            windowStart = publish.getStartAt();
            windowEnd = publish.getEndAt();
            maxAttempts = publish.getMaxAttempts() != null ? publish.getMaxAttempts() : exam.getMaxAttempts();
        } else {
            // 走原有逻辑
            if (exam.getStatus() != null && exam.getStatus() != 1) {
                throw ServiceExceptionUtil.exception(EXAM_NOT_STARTED);
            }
            windowStart = exam.getStartAt();
            windowEnd = exam.getEndAt();
            maxAttempts = exam.getMaxAttempts();
        }

        if (windowStart != null && now.isBefore(windowStart)) {
            throw ServiceExceptionUtil.exception(EXAM_NOT_STARTED);
        }
        if (windowEnd != null && now.isAfter(windowEnd)) {
            throw ServiceExceptionUtil.exception(EXAM_ENDED);
        }

        // 查询该用户的作答记录（按 publishId 过滤）
        LambdaQueryWrapper<ExamAttemptDO> qw = new LambdaQueryWrapper<ExamAttemptDO>()
                .eq(ExamAttemptDO::getExamId, examId)
                .eq(ExamAttemptDO::getUserId, userId)
                .orderByDesc(ExamAttemptDO::getId);
        if (publishId != null) {
            qw.eq(ExamAttemptDO::getPublishId, publishId);
        } else {
            qw.isNull(ExamAttemptDO::getPublishId);
        }
        List<ExamAttemptDO> attempts = attemptMapper.selectList(qw);
        long submittedCount = attempts.stream()
                .filter(attempt -> attempt.getStatus() != null && attempt.getStatus() == 1)
                .count();
        if (maxAttempts != null && submittedCount >= maxAttempts) {
            throw ServiceExceptionUtil.exception(EXAM_ATTEMPT_LIMIT);
        }

        if (!attempts.isEmpty()) {
            ExamAttemptDO latest = attempts.get(0);
            if (latest.getStatus() != null && latest.getStatus() == 0) {
                return BeanUtils.toBean(latest, ExamAttemptRespVO.class);
            }
        }
        if (maxAttempts != null && attempts.size() >= maxAttempts) {
            throw ServiceExceptionUtil.exception(EXAM_ATTEMPT_LIMIT);
        }

        List<ExamPaperDO> papers = paperMapper.selectListByExamId(examId);
        if (papers.isEmpty()) {
            throw ServiceExceptionUtil.exception(EXAM_PAPER_NOT_CONFIGURED);
        }
        ExamPaperDO paper = papers.get(0);

        ExamAttemptDO attempt = new ExamAttemptDO();
        attempt.setExamId(examId);
        attempt.setPublishId(publishId);
        attempt.setPaperId(paper.getId());
        attempt.setUserId(userId);
        attempt.setStartAt(LocalDateTime.now());
        attempt.setStatus(0);
        attemptMapper.insert(attempt);
        return BeanUtils.toBean(attempt, ExamAttemptRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExamAttemptRespVO restartExam(Long examId, Long publishId, Long userId) {
        LambdaQueryWrapper<ExamAttemptDO> wrapper = new LambdaQueryWrapper<ExamAttemptDO>()
                .eq(ExamAttemptDO::getExamId, examId)
                .eq(ExamAttemptDO::getUserId, userId)
                .eq(ExamAttemptDO::getStatus, 0);
        if (publishId != null) {
            wrapper.eq(ExamAttemptDO::getPublishId, publishId);
        } else {
            wrapper.isNull(ExamAttemptDO::getPublishId);
        }
        List<ExamAttemptDO> attempts = attemptMapper.selectList(wrapper);
        for (ExamAttemptDO attempt : attempts) {
            answerMapper.deleteByAttemptId(attempt.getId());
            attemptMapper.deleteById(attempt.getId());
        }
        return startExam(examId, publishId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer resetInProgressAttempts(Long examId, Long publishId, Long operatorUserId) {
        validateManageAccess(examId, publishId, operatorUserId);
        LambdaQueryWrapper<ExamAttemptDO> wrapper = new LambdaQueryWrapper<ExamAttemptDO>()
                .eq(ExamAttemptDO::getExamId, examId)
                .eq(ExamAttemptDO::getStatus, 0);
        if (publishId != null) {
            wrapper.eq(ExamAttemptDO::getPublishId, publishId);
        } else {
            wrapper.isNull(ExamAttemptDO::getPublishId);
        }
        List<ExamAttemptDO> attempts = attemptMapper.selectList(wrapper);
        for (ExamAttemptDO attempt : attempts) {
            answerMapper.deleteByAttemptId(attempt.getId());
            attemptMapper.deleteById(attempt.getId());
        }
        return attempts.size();
    }

    @Override
    public ExamPaperRespVO getPaper(Long attemptId, Long userId) {
        ExamAttemptDO attempt = attemptMapper.selectById(attemptId);
        if (attempt == null) {
            return null;
        }
        if (!attempt.getUserId().equals(userId)) {
            throw ServiceExceptionUtil.exception(EXAM_ATTEMPT_FORBIDDEN);
        }
        ExamPaperDO paper = paperMapper.selectByIdIncludeDeleted(attempt.getPaperId());
        if (paper == null) {
            return null;
        }
        List<ExamPaperItemDO> items = itemMapper.selectListByPaperIdsIncludeDeleted(Collections.singletonList(paper.getId()));
        Map<Long, ExamAnswerDO> savedAnswerMap = answerMapper.selectListByAttemptId(attemptId).stream()
                .filter(answer -> answer.getItemId() != null)
                .collect(Collectors.toMap(ExamAnswerDO::getItemId, answer -> answer, (left, right) -> left));
        List<ExamPaperRespVO.ExamPaperItemRespVO> itemList = new ArrayList<>();
        for (ExamPaperItemDO item : items) {
            ExamPaperRespVO.ExamPaperItemRespVO resp = new ExamPaperRespVO.ExamPaperItemRespVO();
            resp.setId(item.getId());
            resp.setTitle(item.getTitle());
            resp.setItemType(item.getItemType());
            resp.setOptionsJson(sanitizeOptionsJson(item.getOptionsJson(), item.getAnswerJson(), attemptId, item.getId()));
            resp.setScore(item.getScore());
            if ("blank".equals(item.getItemType())) {
                resp.setBlankCount(resolveBlankCount(item));
            }
            ExamAnswerDO savedAnswer = savedAnswerMap.get(item.getId());
            if (savedAnswer != null) {
                resp.setAnswerText(savedAnswer.getAnswerText());
                resp.setAnswerJson(savedAnswer.getAnswerJson());
            }
            itemList.add(resp);
        }
        ExamPaperRespVO respVO = new ExamPaperRespVO();
        respVO.setId(paper.getId());
        respVO.setName(paper.getName());
        respVO.setTotalScore(paper.getTotalScore());
        respVO.setItems(itemList);
        return respVO;
    }

    @Override
    public ExamReviewRespVO getReview(Long examId, Long publishId, Long attemptId, Long userId) {
        ExamReviewRespVO respVO = new ExamReviewRespVO();
        respVO.setCanReview(false);
        respVO.setItems(Collections.emptyList());
        ExamDO exam = examMapper.selectById(examId);
        if (exam == null) {
            throw ServiceExceptionUtil.exception(EXAM_NOT_EXISTS);
        }
        ExamPublishDO publish = publishMapper.selectById(publishId);
        if (publish == null) {
            throw ServiceExceptionUtil.exception(EXAM_PUBLISH_NOT_EXISTS);
        }
        if (!examId.equals(publish.getExamId())) {
            throw ServiceExceptionUtil.exception(EXAM_ATTEMPT_FORBIDDEN);
        }
        if (!examScopeSupport.isUserInScope(publish.getPublishScopeJson(), userId, publish.getTenantId())) {
            throw ServiceExceptionUtil.exception(EXAM_NOT_IN_SCOPE);
        }

        Set<Long> participantIds = examScopeSupport.resolveUserIds(publish.getPublishScopeJson(), publish.getTenantId());
        respVO.setParticipantCount(participantIds.size());
        if (participantIds.isEmpty()) {
            respVO.setMessage("本批次未解析到考试人员，暂不可查看正确答案");
            return respVO;
        }

        List<ExamAttemptDO> submittedAttempts = attemptMapper.selectList(new LambdaQueryWrapper<ExamAttemptDO>()
                .eq(ExamAttemptDO::getExamId, examId)
                .eq(ExamAttemptDO::getPublishId, publishId)
                .eq(ExamAttemptDO::getStatus, 1)
                .in(ExamAttemptDO::getUserId, participantIds));
        Map<Long, Long> submittedTimes = submittedAttempts.stream()
                .filter(attempt -> attempt.getUserId() != null)
                .collect(Collectors.groupingBy(ExamAttemptDO::getUserId, Collectors.counting()));
        respVO.setSubmittedCount(submittedTimes.size());

        ExamAttemptDO ownAttempt = resolveReviewAttempt(examId, publishId, attemptId, userId);
        if (ownAttempt == null) {
            respVO.setMessage("你尚未完成本次考试，提交后才可查看正确答案");
            return respVO;
        }
        fillReviewAttemptMeta(respVO, ownAttempt);

        if (submittedTimes.size() < participantIds.size()) {
            respVO.setMessage(String.format("全部人员完成后可查看正确答案（已完成 %d/%d）",
                    submittedTimes.size(), participantIds.size()));
            return respVO;
        }
        if (hasInProgressAttempt(examId, publishId, participantIds)) {
            respVO.setMessage("仍有人员正在作答，全部提交后可查看正确答案");
            return respVO;
        }
        if (!isRetryWindowClosed(exam, publish, submittedTimes, participantIds)) {
            respVO.setMessage("本批次仍可继续重考，考试窗口结束或全部次数完成后可查看正确答案");
            return respVO;
        }

        ExamPaperDO paper = paperMapper.selectByIdIncludeDeleted(ownAttempt.getPaperId());
        if (paper == null) {
            throw ServiceExceptionUtil.exception(EXAM_PAPER_NOT_CONFIGURED);
        }
        respVO.setPaperId(paper.getId());
        respVO.setPaperName(paper.getName());
        respVO.setTotalScore(paper.getTotalScore());
        respVO.setItems(buildReviewItems(ownAttempt, paper));
        respVO.setCanReview(true);
        respVO.setMessage("可以查看正确答案");
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDraft(ExamAnswerSubmitReqVO submitReqVO, Long userId) {
        ExamAttemptDO attempt = validateWritableAttempt(submitReqVO.getAttemptId(), userId);
        if (attempt.getStatus() != null && attempt.getStatus() == 1) {
            return;
        }
        List<ExamPaperItemDO> items = itemMapper.selectListByPaperIdsIncludeDeleted(Collections.singletonList(attempt.getPaperId()));
        Map<Long, ExamPaperItemDO> itemMap = items.stream()
                .collect(Collectors.toMap(ExamPaperItemDO::getId, item -> item));
        saveAnswers(submitReqVO.getAttemptId(), submitReqVO.getAnswers(), itemMap, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitExam(ExamAnswerSubmitReqVO submitReqVO, Long userId) {
        ExamAttemptDO attempt = validateWritableAttempt(submitReqVO.getAttemptId(), userId);
        if (attempt.getStatus() != null && attempt.getStatus() == 1) {
            return; // 已提交，忽略
        }
        List<ExamPaperItemDO> items = itemMapper.selectListByPaperIdsIncludeDeleted(Collections.singletonList(attempt.getPaperId()));
        Map<Long, ExamPaperItemDO> itemMap = items.stream()
                .collect(Collectors.toMap(ExamPaperItemDO::getId, it -> it));
        int totalScore = saveAnswers(submitReqVO.getAttemptId(), submitReqVO.getAnswers(), itemMap, true);

        attempt.setSubmitAt(LocalDateTime.now());
        attempt.setStatus(1);
        attempt.setTotalScore(totalScore);
        String gradeMode = resolveGradeMode(attempt.getPublishId());
        attempt.setGradeMode(gradeMode);
        attempt.setGradeStatus(GRADE_MODE_AI.equals(gradeMode) ? GRADE_STATUS_PENDING : GRADE_STATUS_DONE);
        attemptMapper.updateById(attempt);
        if (GRADE_MODE_AI.equals(gradeMode)) {
            scheduleAiGradeAfterCommit(attempt.getId(), TenantContextHolder.getTenantId());
        }
    }

    @Override
    public List<ExamAttemptRespVO> getMyAttempts(Long examId, Long publishId, Long userId) {
        LambdaQueryWrapper<ExamAttemptDO> wrapper = new LambdaQueryWrapper<ExamAttemptDO>()
                .eq(ExamAttemptDO::getExamId, examId)
                .eq(ExamAttemptDO::getUserId, userId)
                .orderByDesc(ExamAttemptDO::getId);
        if (publishId != null) {
            wrapper.eq(ExamAttemptDO::getPublishId, publishId);
        } else {
            wrapper.isNull(ExamAttemptDO::getPublishId);
        }
        List<ExamAttemptDO> attempts = attemptMapper.selectList(wrapper);
        return BeanUtils.toBean(attempts, ExamAttemptRespVO.class);
    }

    private ExamAttemptDO validateWritableAttempt(Long attemptId, Long userId) {
        ExamAttemptDO attempt = attemptMapper.selectById(attemptId);
        if (attempt == null) {
            throw ServiceExceptionUtil.exception(EXAM_ATTEMPT_NOT_EXISTS);
        }
        if (!attempt.getUserId().equals(userId)) {
            throw ServiceExceptionUtil.exception(EXAM_ATTEMPT_FORBIDDEN);
        }
        if (attempt.getStatus() != null && attempt.getStatus() == 1) {
            return attempt;
        }
        return attempt;
    }

    private int saveAnswers(Long attemptId,
                            List<ExamAnswerSubmitReqVO.Answer> answerReqList,
                            Map<Long, ExamPaperItemDO> itemMap,
                            boolean forSubmit) {
        answerMapper.deleteByAttemptId(attemptId);
        int totalScore = 0;
        if (answerReqList == null) {
            return totalScore;
        }
        for (ExamAnswerSubmitReqVO.Answer answerReq : answerReqList) {
            ExamPaperItemDO item = itemMap.get(answerReq.getItemId());
            if (item == null) {
                continue;
            }
            ExamAnswerDO answer = new ExamAnswerDO();
            answer.setAttemptId(attemptId);
            answer.setItemId(answerReq.getItemId());
            answer.setAnswerText(answerReq.getAnswerText());
            answer.setAnswerJson(answerReq.getAnswerJson());
            if (forSubmit) {
                Integer score = calcScore(item, answerReq.getAnswerJson(), answerReq.getAnswerText());
                answer.setAnswerScore(score);
                answer.setGradeStatus(needsAiGrade(item) ? GRADE_STATUS_PENDING : GRADE_STATUS_DONE);
                if (score != null) {
                    totalScore += score;
                }
            }
            answerMapper.insert(answer);
        }
        return totalScore;
    }

    private ExamAttemptDO resolveReviewAttempt(Long examId, Long publishId, Long attemptId, Long userId) {
        LambdaQueryWrapper<ExamAttemptDO> wrapper = new LambdaQueryWrapper<ExamAttemptDO>()
                .eq(ExamAttemptDO::getExamId, examId)
                .eq(ExamAttemptDO::getPublishId, publishId)
                .eq(ExamAttemptDO::getUserId, userId)
                .eq(ExamAttemptDO::getStatus, 1)
                .orderByDesc(ExamAttemptDO::getId);
        if (attemptId != null) {
            wrapper.eq(ExamAttemptDO::getId, attemptId);
        }
        List<ExamAttemptDO> attempts = attemptMapper.selectList(wrapper);
        return attempts.isEmpty() ? null : attempts.get(0);
    }

    private boolean hasInProgressAttempt(Long examId, Long publishId, Set<Long> participantIds) {
        Long count = attemptMapper.selectCount(new LambdaQueryWrapper<ExamAttemptDO>()
                .eq(ExamAttemptDO::getExamId, examId)
                .eq(ExamAttemptDO::getPublishId, publishId)
                .eq(ExamAttemptDO::getStatus, 0)
                .in(ExamAttemptDO::getUserId, participantIds));
        return count != null && count > 0;
    }

    private boolean isRetryWindowClosed(ExamDO exam,
                                        ExamPublishDO publish,
                                        Map<Long, Long> submittedTimes,
                                        Set<Long> participantIds) {
        if (publish.getStatus() != null && publish.getStatus() == 2) {
            return true;
        }
        if (publish.getEndAt() != null && !LocalDateTime.now().isBefore(publish.getEndAt())) {
            return true;
        }
        Integer maxAttempts = publish.getMaxAttempts() != null ? publish.getMaxAttempts() : exam.getMaxAttempts();
        if (maxAttempts == null || maxAttempts <= 0) {
            return false;
        }
        if (maxAttempts <= 1) {
            return true;
        }
        for (Long userId : participantIds) {
            if (submittedTimes.getOrDefault(userId, 0L) < maxAttempts) {
                return false;
            }
        }
        return true;
    }

    private void fillReviewAttemptMeta(ExamReviewRespVO respVO, ExamAttemptDO attempt) {
        respVO.setAttemptId(attempt.getId());
        respVO.setPaperId(attempt.getPaperId());
        respVO.setUserScore(attempt.getTotalScore());
        respVO.setSubmitAt(attempt.getSubmitAt());
    }

    private List<ExamReviewRespVO.ItemRespVO> buildReviewItems(ExamAttemptDO attempt, ExamPaperDO paper) {
        List<ExamPaperItemDO> items = itemMapper.selectListByPaperIdsIncludeDeleted(Collections.singletonList(paper.getId()));
        Map<Long, ExamAnswerDO> answerMap = answerMapper.selectListByAttemptId(attempt.getId()).stream()
                .collect(Collectors.toMap(ExamAnswerDO::getItemId, answer -> answer, (left, right) -> left));
        return items.stream()
                .sorted(Comparator.comparing(ExamPaperItemDO::getSortNo, Comparator.nullsLast(Integer::compareTo)))
                .map(item -> buildReviewItem(attempt, item, answerMap.get(item.getId())))
                .collect(Collectors.toList());
    }

    private ExamReviewRespVO.ItemRespVO buildReviewItem(ExamAttemptDO attempt, ExamPaperItemDO item, ExamAnswerDO answer) {
        ExamReviewRespVO.ItemRespVO respVO = new ExamReviewRespVO.ItemRespVO();
        respVO.setItemId(item.getId());
        respVO.setSortNo(item.getSortNo());
        respVO.setTitle(item.getTitle());
        respVO.setItemType(item.getItemType());
        respVO.setOptionsJson(sanitizeOptionsJson(item.getOptionsJson(), item.getAnswerJson(), attempt.getId(), item.getId()));
        respVO.setStandardAnswerJson(item.getAnswerJson());
        respVO.setMaxScore(item.getScore());
        if (answer != null) {
            respVO.setAnswerText(answer.getAnswerText());
            respVO.setAnswerJson(answer.getAnswerJson());
            respVO.setAnswerScore(answer.getAnswerScore());
            respVO.setAiComment(answer.getAiComment());
        }
        return respVO;
    }

    private Integer calcScore(ExamPaperItemDO item, String answerJson, String answerText) {
        if (item == null || item.getScore() == null) {
            return 0;
        }
        if (item.getAnswerJson() == null) {
            return 0;
        }
        String standard = item.getAnswerJson().trim();
        String userAnswer = answerJson != null ? answerJson.trim() : null;
        if ((userAnswer == null || userAnswer.isEmpty()) && answerText != null) {
            userAnswer = answerText.trim();
        }
        if (userAnswer == null || userAnswer.isEmpty()) {
            return 0;
        }

        List<String> standardList = toComparableAnswerList(item, standard);
        List<String> userList = toComparableAnswerList(item, userAnswer);
        if (standardList.isEmpty() || userList.isEmpty()) {
            return 0;
        }
        if (standardList.size() != userList.size()) {
            return 0;
        }
        List<String> stdSorted = new ArrayList<>(standardList);
        List<String> usrSorted = new ArrayList<>(userList);
        Collections.sort(stdSorted);
        Collections.sort(usrSorted);
        if (stdSorted.equals(usrSorted)) {
            return item.getScore();
        }
        return 0;
    }

    private List<String> toComparableAnswerList(ExamPaperItemDO item, String value) {
        List<String> values = toAnswerList(value, item.getItemType());
        if (values.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> optionTexts = parseOptionTexts(item.getOptionsJson());
        return values.stream()
                .map(v -> normalizeObjectiveAnswerToken(v, item.getItemType(), optionTexts))
                .filter(v -> !v.isEmpty())
                .collect(Collectors.toList());
    }

    private String normalizeObjectiveAnswerToken(String value, String itemType, List<String> optionTexts) {
        String normalized = normalizeLooseText(value);
        if (normalized.isEmpty()) {
            return "";
        }
        if ("judge".equals(itemType)) {
            String lower = normalized.toLowerCase(Locale.ROOT);
            if (JUDGE_TRUE_VALUES.contains(lower)) {
                return "正确";
            }
            if (JUDGE_FALSE_VALUES.contains(lower)) {
                return "错误";
            }
            return normalized;
        }
        if (("single".equals(itemType) || "multi".equals(itemType)) && normalized.length() == 1) {
            char ch = Character.toUpperCase(normalized.charAt(0));
            int index = ch - 'A';
            if (index >= 0 && index < optionTexts.size()) {
                return normalizeLooseText(optionTexts.get(index));
            }
            return String.valueOf(ch);
        }
        return normalized;
    }

    private String normalizeLooseText(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        while (normalized.length() >= 2
                && ((normalized.startsWith("\"") && normalized.endsWith("\""))
                || (normalized.startsWith("'") && normalized.endsWith("'")))) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        normalized = normalized.replaceAll("^[\\s\\p{Punct}，。；：、（）()【】\\[\\]《》<>]+", "")
                .replaceAll("[\\s\\p{Punct}，。；：、（）()【】\\[\\]《》<>]+$", "")
                .replaceAll("\\s+", " ")
                .trim();
        return normalized;
    }

    private List<String> parseOptionTexts(String optionsJson) {
        if (optionsJson == null || optionsJson.trim().isEmpty() || !JsonUtils.isJson(optionsJson.trim())) {
            return Collections.emptyList();
        }
        try {
            Object obj = JsonUtils.parseObject(optionsJson, Object.class);
            if (!(obj instanceof List)) {
                return Collections.emptyList();
            }
            List<?> list = (List<?>) obj;
            List<String> result = new ArrayList<>();
            for (Object option : list) {
                if (option instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) option;
                    Object text = map.get("text");
                    if (text == null) {
                        text = map.get("optionText");
                    }
                    if (text == null) {
                        text = map.get("label");
                    }
                    if (text == null) {
                        text = map.get("content");
                    }
                    if (text == null) {
                        text = map.get("value");
                    }
                    result.add(text == null ? "" : String.valueOf(text));
                } else {
                    result.add(option == null ? "" : String.valueOf(option));
                }
            }
            return result;
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
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

    private void scheduleAiGradeAfterCommit(Long attemptId, Long tenantId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            scheduleAiGrade(attemptId, tenantId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                scheduleAiGrade(attemptId, tenantId);
            }
        });
    }

    private void gradeAttemptByAi(Long attemptId) {
        ExamAttemptDO attempt = attemptMapper.selectById(attemptId);
        if (!isAiGradeActive(attempt)) {
            return;
        }
        List<ExamPaperItemDO> items = itemMapper.selectListByPaperIds(Collections.singletonList(attempt.getPaperId()));
        Map<Long, ExamPaperItemDO> itemMap = items.stream().collect(Collectors.toMap(ExamPaperItemDO::getId, item -> item));
        List<ExamAnswerDO> answers = answerMapper.selectListByAttemptId(attemptId);
        boolean failed = false;
        String error = null;
        for (ExamAnswerDO answer : answers) {
            ExamPaperItemDO item = itemMap.get(answer.getItemId());
            if (!needsAiGrade(item)) {
                continue;
            }
            if (!isAiGradeActive(attemptId)) {
                return;
            }
            if (!hasUserAnswer(item, answer)) {
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
        reqDTO.setStandardAnswer(String.join("、",
                toAnswerList(item.getAnswerJson(), item.getItemType())));
        reqDTO.setUserAnswer(String.join("、",
                toAnswerList(answer.getAnswerJson(), item.getItemType())));
        if (reqDTO.getUserAnswer() == null || reqDTO.getUserAnswer().isEmpty()) {
            reqDTO.setUserAnswer(answer.getAnswerText());
        }
        reqDTO.setMaxScore(item.getScore());
        CommonResult<AiExamGradeRespDTO> result = aiExamGradeApi.grade(reqDTO);
        return result.getCheckedData();
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
                .filter(score -> score != null)
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
        return isAiGradeActive(attemptMapper.selectById(attemptId));
    }

    private boolean isAiGradeActive(ExamAttemptDO attempt) {
        return attempt != null && GRADE_MODE_AI.equalsIgnoreCase(attempt.getGradeMode())
                && GRADE_STATUS_PENDING.equals(attempt.getGradeStatus());
    }

    private boolean hasUserAnswer(ExamPaperItemDO item, ExamAnswerDO answer) {
        if (answer == null) {
            return false;
        }
        if (!toAnswerList(answer.getAnswerJson(), item == null ? null : item.getItemType()).isEmpty()) {
            return true;
        }
        return answer.getAnswerText() != null && !answer.getAnswerText().trim().isEmpty();
    }

    private String resolveGradeMode(Long publishId) {
        if (publishId == null) {
            return GRADE_MODE_AI;
        }
        ExamPublishDO publish = publishMapper.selectById(publishId);
        return publish != null && GRADE_MODE_MANUAL.equalsIgnoreCase(publish.getGradeMode()) ? GRADE_MODE_MANUAL : GRADE_MODE_AI;
    }

    private int resolveBlankCount(ExamPaperItemDO item) {
        if (item == null) {
            return 1;
        }
        int answerCount = toAnswerList(item.getAnswerJson(), "blank").size();
        int titleCount = inferBlankCount(item.getTitle());
        return Math.max(1, Math.max(answerCount, titleCount));
    }

    private int inferBlankCount(String title) {
        if (title == null || title.trim().isEmpty()) {
            return 0;
        }
        int count = 0;
        Matcher matcher = BLANK_MARKER_PATTERN.matcher(title);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private List<String> toAnswerList(String value, String itemType) {
        if (value == null) {
            return Collections.emptyList();
        }
        Object actualValue = extractAnswerValue(value);
        if (actualValue == null) {
            return Collections.emptyList();
        }
        if (actualValue instanceof List) {
            List<?> list = (List<?>) actualValue;
            return list.stream()
                    .map(v -> v == null ? "" : String.valueOf(v).trim())
                    .flatMap(v -> splitChoiceAnswerText(v, itemType).stream())
                    .map(v -> normalizeAnswerToken(v, itemType))
                    .filter(v -> !v.isEmpty())
                    .collect(Collectors.toList());
        }
        String normalized = String.valueOf(actualValue).trim();
        if (normalized.isEmpty()) {
            return Collections.emptyList();
        }
        if ("blank".equals(itemType)) {
            return splitBlankAnswerText(normalized);
        }
        return splitChoiceAnswerText(normalized, itemType).stream()
                .map(v -> normalizeAnswerToken(v, itemType))
                .filter(v -> !v.isEmpty())
                .collect(Collectors.toList());
    }

    private String normalizeAnswerToken(String value, String itemType) {
        if (value == null) {
            return "";
        }
        String normalized = normalizeLooseText(value);
        if (!"judge".equals(itemType)) {
            return normalized;
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (JUDGE_TRUE_VALUES.contains(lower)) {
            return "正确";
        }
        if (JUDGE_FALSE_VALUES.contains(lower)) {
            return "错误";
        }
        return normalized;
    }

    private List<String> splitChoiceAnswerText(String value, String itemType) {
        if (value == null || value.trim().isEmpty()) {
            return Collections.emptyList();
        }
        if (!"multi".equals(itemType)) {
            return Collections.singletonList(value.trim());
        }
        String[] parts = BLANK_ANSWER_SEPARATOR_PATTERN.split(value);
        List<String> answers = new ArrayList<>();
        for (String part : parts) {
            String answer = part == null ? "" : part.trim();
            if (!answer.isEmpty()) {
                answers.add(answer);
            }
        }
        if (answers.size() == 1 && answers.get(0).matches("(?i)^[A-H]{2,}$")) {
            return answers.get(0).toUpperCase(Locale.ROOT).chars()
                    .mapToObj(ch -> String.valueOf((char) ch))
                    .collect(Collectors.toList());
        }
        return answers.isEmpty() ? Collections.singletonList(value.trim()) : answers;
    }

    private List<String> splitBlankAnswerText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String[] parts = BLANK_ANSWER_SEPARATOR_PATTERN.split(value);
        List<String> answers = new ArrayList<>();
        for (String part : parts) {
            String answer = part == null ? "" : part.trim();
            if (!answer.isEmpty()) {
                answers.add(answer);
            }
        }
        return answers.isEmpty() ? Collections.singletonList(value.trim()) : answers;
    }

    private Object extractAnswerValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (!shouldParseJsonValue(trimmed)) {
            return trimmed;
        }
        try {
            Object obj = JsonUtils.parseObject(trimmed, Object.class);
            if (obj instanceof Map) {
                Map<?, ?> payload = (Map<?, ?>) obj;
                Object actual = payload.get("value");
                if (actual == null) {
                    actual = payload.get("answer");
                }
                if (actual == null) {
                    actual = payload.get("answers");
                }
                if (actual == null) {
                    actual = payload.get("standardAnswer");
                }
                return actual;
            }
            return obj;
        } catch (Exception ignored) {
            return trimmed;
        }
    }

    private boolean shouldParseJsonValue(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        if (JsonUtils.isJson(trimmed)) {
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

    private boolean shouldShuffleOptions(String answerJson) {
        if (answerJson == null) {
            return false;
        }
        String trimmed = answerJson.trim();
        if (trimmed.isEmpty() || !JsonUtils.isJson(trimmed)) {
            return false;
        }
        try {
            Object obj = JsonUtils.parseObject(trimmed, Object.class);
            if (obj instanceof Map) {
                Map<?, ?> payload = (Map<?, ?>) obj;
                Object shuffle = payload.get("shuffleOptions");
                if (shuffle == null) {
                    shuffle = payload.get("shuffle");
                }
                if (shuffle instanceof Boolean) {
                    return (Boolean) shuffle;
                }
                return shuffle != null && Boolean.parseBoolean(String.valueOf(shuffle));
            }
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    private String sanitizeOptionsJson(String optionsJson, String answerJson, Long attemptId, Long itemId) {
        if (optionsJson == null || optionsJson.trim().isEmpty()) {
            return optionsJson;
        }
        if (!JsonUtils.isJson(optionsJson)) {
            return optionsJson;
        }
        try {
            Object obj = JsonUtils.parseObject(optionsJson, Object.class);
            if (obj instanceof List) {
                List<?> list = (List<?>) obj;
                List<Object> sanitized = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof Map) {
                        Map<?, ?> map = (Map<?, ?>) item;
                        Map<String, Object> cleaned = new LinkedHashMap<>();
                        for (Map.Entry<?, ?> entry : map.entrySet()) {
                            String key = String.valueOf(entry.getKey());
                            if ("isCorrect".equals(key) || "correct".equalsIgnoreCase(key)) {
                                continue;
                            }
                            cleaned.put(key, entry.getValue());
                        }
                        sanitized.add(cleaned);
                    } else {
                        sanitized.add(item);
                    }
                }
                if (shouldShuffleOptions(answerJson) && sanitized.size() > 1 && attemptId != null && itemId != null) {
                    long seed = attemptId * 31L + itemId;
                    Collections.shuffle(sanitized, new Random(seed));
                }
                return JsonUtils.toJsonString(sanitized);
            }
        } catch (Exception ignored) {
            return optionsJson;
        }
        return optionsJson;
    }

    private void validateManageAccess(Long examId, Long publishId, Long operatorUserId) {
        ExamDO exam = examMapper.selectById(examId);
        if (exam == null) {
            throw ServiceExceptionUtil.exception(EXAM_NOT_EXISTS);
        }
        ExamPublishDO publish = null;
        if (publishId != null) {
            publish = publishMapper.selectById(publishId);
            if (publish == null) {
                throw ServiceExceptionUtil.exception(EXAM_PUBLISH_NOT_EXISTS);
            }
            if (!examId.equals(publish.getExamId())) {
                throw ServiceExceptionUtil.exception(EXAM_ATTEMPT_FORBIDDEN);
            }
        }
        if (isAdmin()) {
            return;
        }
        String creator = publish != null ? publish.getCreator() : exam.getCreator();
        if (operatorUserId == null || !String.valueOf(operatorUserId).equals(creator)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.FORBIDDEN, "无权重置该考试作答");
        }
    }

    private boolean isAdmin() {
        return securityFrameworkService.hasRole(RoleCodeEnum.SUPER_ADMIN.getCode())
                || securityFrameworkService.hasRole(RoleCodeEnum.TENANT_ADMIN.getCode())
                || examViewScopeSupport.canViewAllData();
    }

}
