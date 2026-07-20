package com.kyx.service.hr.service.exam;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.enums.UserTypeEnum;
import com.kyx.foundation.common.exception.enums.GlobalErrorCodeConstants;
import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.security.core.service.SecurityFrameworkService;
import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.service.business.api.notice.NoticeApi;
import com.kyx.service.business.api.notice.dto.NoticeCreateReqDTO;
import com.kyx.service.business.enums.permission.RoleCodeEnum;
import com.kyx.service.business.enums.notice.NoticeTypeEnum;
import com.kyx.service.op.api.websocket.WebSocketSenderApi;
import com.kyx.service.hr.controller.admin.exam.vo.ExamPublishPageReqVO;
import com.kyx.service.hr.controller.admin.exam.vo.ExamPublishRespVO;
import com.kyx.service.hr.controller.admin.exam.vo.ExamPublishSaveReqVO;
import com.kyx.service.hr.dal.dataobject.exam.ExamAttemptDO;
import com.kyx.service.hr.dal.dataobject.exam.ExamAnswerDO;
import com.kyx.service.hr.dal.dataobject.exam.ExamDO;
import com.kyx.service.hr.dal.dataobject.exam.ExamPublishDO;
import com.kyx.service.hr.dal.dataobject.tenant.TenantDO;
import com.kyx.service.hr.dal.mysql.exam.ExamAttemptMapper;
import com.kyx.service.hr.dal.mysql.exam.ExamAnswerMapper;
import com.kyx.service.hr.dal.mysql.exam.ExamMapper;
import com.kyx.service.hr.dal.mysql.exam.ExamPublishMapper;
import com.kyx.service.hr.dal.mysql.tenant.TenantMapper;
import com.kyx.service.hr.service.notice.HrNoticeRecordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.time.temporal.TemporalAdjusters;
import java.util.stream.Collectors;

import static com.kyx.foundation.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static com.kyx.service.hr.enums.ErrorCodeConstants.*;

/**
 * HR 考试发布 Service 实现
 *
 * @author MK
 */
@Service
@Validated
@Slf4j
public class ExamPublishServiceImpl implements ExamPublishService {

    private static final String BUSINESS_TYPE_EXAM_PUBLISH = "EXAM_PUBLISH";

    @Resource
    private ExamPublishMapper publishMapper;
    @Resource
    private ExamMapper examMapper;
    @Resource
    private ExamAttemptMapper attemptMapper;
    @Resource
    private ExamAnswerMapper answerMapper;
    @Resource
    private NoticeApi noticeApi;
    @Resource
    private WebSocketSenderApi webSocketSenderApi;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private ExamScopeSupport examScopeSupport;
    @Resource
    private SecurityFrameworkService securityFrameworkService;
    @Resource
    private ExamViewScopeSupport examViewScopeSupport;
    @Resource
    private ExamPublishScopeService examPublishScopeService;
    @Resource
    private TenantMapper tenantMapper;
    @Resource
    private HrNoticeRecordService noticeRecordService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPublish(ExamPublishSaveReqVO reqVO) {
        ExamPublishDO publish = BeanUtils.toBean(reqVO, ExamPublishDO.class);
        cleanJsonFields(publish);
        publish.setPublishScopeJson(examPublishScopeService.normalizeScopeJson(
                publish.getPublishScopeJson(), TenantContextHolder.getTenantId()));
        ExamDO exam = validateExamExists(publish.getExamId());
        validateExamAccess(exam);

        Integer publishType = publish.getPublishType() != null ? publish.getPublishType() : 0;
        ExamPublishDO existingRoot = publishMapper.selectLatestRootByExam(publish.getExamId(), publishType);
        if (existingRoot != null && (existingRoot.getStatus() == null || existingRoot.getStatus() != 2)) {
            validatePublishAccess(existingRoot);
            throw ServiceExceptionUtil.exception(EXAM_PUBLISH_ALREADY_EXISTS);
        }

        if (publishType == 0) {
            // 一次性发布
            Integer sendType = publish.getSendType() != null ? publish.getSendType() : 0;
            if (sendType == 0) {
                // 立即发布
                publish.setStatus(1);
            } else {
                // 定时发布，等定时任务触发
                publish.setStatus(0);
            }
        } else {
            // 定期考核：计算 nextPublishAt
            publish.setStatus(1);
            publish.setGeneratedCount(0);
            LocalDateTime nextTime = calculateNextPublishAt(publish.getRepeatRuleJson(), LocalDateTime.now());
            publish.setNextPublishAt(nextTime);
        }

        publishMapper.insert(publish);

        // 一次性+立即 → 发通知
        if (publishType == 0 && publish.getStatus() == 1) {
            sendPublishNotice(publish);
        }
        return publish.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePublish(ExamPublishSaveReqVO reqVO) {
        ExamPublishDO exist = validatePublishExists(reqVO.getId());
        validatePublishAccess(exist);
        ExamPublishDO updateObj = BeanUtils.toBean(reqVO, ExamPublishDO.class);
        cleanJsonFields(updateObj);
        updateObj.setPublishScopeJson(examPublishScopeService.normalizeScopeJson(
                updateObj.getPublishScopeJson(), exist.getTenantId()));
        publishMapper.updateById(updateObj);
    }

    @Override
    public void deletePublish(Long id) {
        ExamPublishDO publish = validatePublishExists(id);
        validatePublishAccess(publish);
        publishMapper.deleteById(id);
    }

    @Override
    public ExamPublishRespVO getPublish(Long id) {
        ExamPublishDO publish = validatePublishExists(id);
        validatePublishAccess(publish);
        ExamPublishRespVO resp = BeanUtils.toBean(publish, ExamPublishRespVO.class);
        fillExamName(resp);
        fillTenantName(resp);
        fillAttemptStats(resp);
        return resp;
    }

    @Override
    public PageResult<ExamPublishRespVO> getPublishPage(ExamPublishPageReqVO pageReqVO) {
        PageResult<ExamPublishDO> pageResult = publishMapper.selectPage(pageReqVO);
        if (Boolean.TRUE.equals(pageReqVO.getMine()) && pageReqVO.getCurrentUserId() != null) {
            List<ExamPublishDO> filteredList = pageResult.getList().stream()
                    .filter(item -> examScopeSupport.isUserInScope(
                            item.getPublishScopeJson(),
                            pageReqVO.getCurrentUserId(),
                            item.getTenantId()))
                    .collect(java.util.stream.Collectors.toList());
            pageResult = new PageResult<>(filteredList, (long) filteredList.size());
        }
        PageResult<ExamPublishRespVO> resp = BeanUtils.toBean(pageResult, ExamPublishRespVO.class);
        if (resp.getList() != null) {
            fillTenantNames(resp.getList());
            for (ExamPublishRespVO vo : resp.getList()) {
                fillExamName(vo);
                fillAttemptStats(vo);
            }
        }
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pauseRecurring(Long publishId) {
        ExamPublishDO publish = validatePublishExists(publishId);
        validatePublishAccess(publish);
        if (publish.getPublishType() == null || publish.getPublishType() != 1
                || publish.getParentPublishId() != null
                || publish.getStatus() == null || publish.getStatus() != 1) {
            throw ServiceExceptionUtil.exception(EXAM_PUBLISH_CANNOT_PAUSE);
        }
        ExamPublishDO update = new ExamPublishDO();
        update.setId(publishId);
        update.setStatus(3);
        publishMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resumeRecurring(Long publishId) {
        ExamPublishDO publish = validatePublishExists(publishId);
        validatePublishAccess(publish);
        if (publish.getPublishType() == null || publish.getPublishType() != 1
                || publish.getParentPublishId() != null
                || publish.getStatus() == null || publish.getStatus() != 3) {
            throw ServiceExceptionUtil.exception(EXAM_PUBLISH_CANNOT_RESUME);
        }
        LocalDateTime nextTime = calculateNextPublishAt(publish.getRepeatRuleJson(), LocalDateTime.now());
        ExamPublishDO update = new ExamPublishDO();
        update.setId(publishId);
        update.setStatus(1);
        update.setNextPublishAt(nextTime);
        publishMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closePublish(Long publishId) {
        ExamPublishDO publish = validatePublishExists(publishId);
        validatePublishAccess(publish);
        if (publish.getStatus() != null && publish.getStatus() == 2) {
            return;
        }
        closeSinglePublish(publish);
        if (publish.getPublishType() != null && publish.getPublishType() == 1
                && publish.getParentPublishId() == null) {
            for (ExamPublishDO batch : publishMapper.selectBatchesByParentId(publishId)) {
                if (batch.getStatus() != null && batch.getStatus() == 2) {
                    continue;
                }
                closeSinglePublish(batch);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processScheduledPublishes() {
        LocalDateTime now = LocalDateTime.now();
        List<ExamPublishDO> dueList = publishMapper.selectListByScheduleDue(now);
        for (ExamPublishDO publish : dueList) {
            ExamPublishDO update = new ExamPublishDO();
            update.setId(publish.getId());
            update.setStatus(1);
            if (publish.getStartAt() == null) {
                update.setStartAt(now);
                publish.setStartAt(now);
            }
            publishMapper.updateById(update);
            publish.setStatus(1);
            sendPublishNotice(publish);
            log.info("[processScheduledPublishes] 定时发布触发, publishId={}", publish.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processRecurringPublishes() {
        LocalDateTime now = LocalDateTime.now();
        List<ExamPublishDO> parentList = publishMapper.selectRecurringDue(now);
        for (ExamPublishDO parent : parentList) {
            int generatedCount = parent.getGeneratedCount() != null ? parent.getGeneratedCount() : 0;
            LocalDateTime nextPublishAt = parent.getNextPublishAt();
            if (nextPublishAt == null) {
                nextPublishAt = calculateNextPublishAt(parent.getRepeatRuleJson(), now);
            }
            RepeatRule rule = parseRepeatRule(parent.getRepeatRuleJson());
            boolean ended = false;
            int generatedInThisRound = 0;

            while (nextPublishAt != null && !nextPublishAt.isAfter(now) && generatedInThisRound < 24) {
                if (isRecurringEnded(parent, generatedCount, nextPublishAt)) {
                    ended = true;
                    break;
                }

                int newBatchNo = generatedCount + 1;
                LocalDateTime batchEndAt = calculateBatchEndAt(nextPublishAt, rule);

                ExamPublishDO batch = new ExamPublishDO();
                batch.setExamId(parent.getExamId());
                batch.setPublishType(parent.getPublishType());
                batch.setPublishScopeJson(parent.getPublishScopeJson());
                batch.setParentPublishId(parent.getId());
                batch.setBatchNo(newBatchNo);
                batch.setBatchLabel(nextPublishAt.format(DateTimeFormatter.ofPattern("yyyy年M月")));
                batch.setStartAt(nextPublishAt);
                batch.setEndAt(batchEndAt);
                batch.setDurationMin(parent.getDurationMin());
                batch.setMaxAttempts(parent.getMaxAttempts());
                batch.setGradeMode(parent.getGradeMode());
                batch.setRemindRuleJson(parent.getRemindRuleJson());
                batch.setStatus(1);
                batch.setCreator(parent.getCreator());
                publishMapper.insert(batch);

                sendPublishNotice(batch);
                generatedCount = newBatchNo;
                generatedInThisRound++;
                nextPublishAt = calculateNextPublishAt(parent.getRepeatRuleJson(), nextPublishAt);

                log.info("[processRecurringPublishes] 创建批次, parentId={}, batchNo={}, batchId={}",
                        parent.getId(), newBatchNo, batch.getId());
            }

            ExamPublishDO parentUpdate = new ExamPublishDO();
            parentUpdate.setId(parent.getId());
            parentUpdate.setGeneratedCount(generatedCount);
            parentUpdate.setNextPublishAt(nextPublishAt);
            if (ended) {
                parentUpdate.setStatus(2);
            } else if (parent.getStatus() == null || parent.getStatus() == 0) {
                parentUpdate.setStatus(1);
            }
            publishMapper.updateById(parentUpdate);

            if (ended) {
                log.info("[processRecurringPublishes] 定期考核结束, publishId={}", parent.getId());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeExpiredBatches() {
        LocalDateTime now = LocalDateTime.now();
        List<ExamPublishDO> expired = publishMapper.selectExpiredBatches(now);
        for (ExamPublishDO batch : expired) {
            closeSinglePublish(batch);
            log.info("[closeExpiredBatches] 关闭过期批次, batchId={}", batch.getId());
        }
        List<ExamPublishDO> oneTimeExpired = publishMapper.selectExpiredOneTimePublishes(now);
        for (ExamPublishDO publish : oneTimeExpired) {
            closeSinglePublish(publish);
            log.info("[closeExpiredBatches] 关闭过期一次性发布, publishId={}", publish.getId());
        }
    }

    // ========== 内部方法 ==========

    private ExamPublishDO validatePublishExists(Long id) {
        ExamPublishDO publish = publishMapper.selectById(id);
        if (publish == null) {
            throw ServiceExceptionUtil.exception(EXAM_PUBLISH_NOT_EXISTS);
        }
        return publish;
    }

    private ExamDO validateExamExists(Long examId) {
        ExamDO exam = examId == null ? null : examMapper.selectById(examId);
        if (exam == null) {
            throw ServiceExceptionUtil.exception(EXAM_NOT_EXISTS);
        }
        return exam;
    }

    private void validateExamAccess(ExamDO exam) {
        if (exam == null || isAdmin()) {
            return;
        }
        Long loginUserId = getLoginUserId();
        if (loginUserId == null || !String.valueOf(loginUserId).equals(exam.getCreator())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.FORBIDDEN, "无权操作该考试");
        }
    }

    private void validatePublishAccess(ExamPublishDO publish) {
        if (publish == null || isAdmin()) {
            return;
        }
        Long loginUserId = getLoginUserId();
        if (loginUserId == null || !String.valueOf(loginUserId).equals(publish.getCreator())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.FORBIDDEN, "无权操作该考试发布");
        }
    }

    private boolean isAdmin() {
        return securityFrameworkService.hasRole(RoleCodeEnum.SUPER_ADMIN.getCode())
                || securityFrameworkService.hasRole(RoleCodeEnum.TENANT_ADMIN.getCode())
                || examViewScopeSupport.canViewAllData();
    }

    private void cleanJsonFields(ExamPublishDO publish) {
        if (publish.getRepeatRuleJson() != null && publish.getRepeatRuleJson().trim().isEmpty()) {
            publish.setRepeatRuleJson(null);
        }
        if (publish.getPublishScopeJson() != null && publish.getPublishScopeJson().trim().isEmpty()) {
            publish.setPublishScopeJson(null);
        }
        if (publish.getRemindRuleJson() != null && publish.getRemindRuleJson().trim().isEmpty()) {
            publish.setRemindRuleJson(null);
        }
    }

    private void fillExamName(ExamPublishRespVO vo) {
        if (vo.getExamId() != null) {
            ExamDO exam = examMapper.selectById(vo.getExamId());
            if (exam != null) {
                vo.setExamName(exam.getName());
            }
        }
    }

    private void fillTenantName(ExamPublishRespVO vo) {
        if (vo == null || vo.getTenantId() == null) {
            return;
        }
        TenantDO tenant = tenantMapper.selectById(vo.getTenantId());
        if (tenant != null) {
            vo.setTenantName(tenant.getName());
        }
    }

    private void fillTenantNames(List<ExamPublishRespVO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> tenantIds = list.stream()
                .map(ExamPublishRespVO::getTenantId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (tenantIds.isEmpty()) {
            return;
        }
        Map<Long, String> tenantMap = tenantMapper.selectBatchIds(tenantIds).stream()
                .collect(Collectors.toMap(TenantDO::getId, TenantDO::getName, (left, right) -> left));
        for (ExamPublishRespVO vo : list) {
            if (vo.getTenantId() != null) {
                vo.setTenantName(tenantMap.get(vo.getTenantId()));
            }
        }
    }

    private void fillAttemptStats(ExamPublishRespVO vo) {
        if (vo.getId() == null) return;
        try {
            Long publishId = vo.getId();
            Set<Long> participantIds = examScopeSupport.resolveUserIds(vo.getPublishScopeJson(), vo.getTenantId());
            Long submitted = null;
            if (!participantIds.isEmpty()) {
                submitted = (long) attemptMapper.selectList(new LambdaQueryWrapper<ExamAttemptDO>()
                                .eq(ExamAttemptDO::getPublishId, publishId)
                                .eq(ExamAttemptDO::getStatus, 1)
                                .in(ExamAttemptDO::getUserId, participantIds))
                        .stream()
                        .map(ExamAttemptDO::getUserId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .count();
                vo.setParticipantCount(participantIds.size());
            } else {
                Long total = attemptMapper.selectCount(
                        new LambdaQueryWrapper<ExamAttemptDO>()
                                .eq(ExamAttemptDO::getPublishId, publishId));
                submitted = attemptMapper.selectCount(
                        new LambdaQueryWrapper<ExamAttemptDO>()
                                .eq(ExamAttemptDO::getPublishId, publishId)
                                .eq(ExamAttemptDO::getStatus, 1));
                vo.setParticipantCount(total != null ? total.intValue() : 0);
            }
            vo.setSubmittedCount(submitted != null ? submitted.intValue() : 0);

            // 通过率
            if (submitted != null && submitted > 0 && vo.getExamId() != null) {
                ExamDO exam = examMapper.selectById(vo.getExamId());
                if (exam != null && exam.getPassScore() != null) {
                    Long passed;
                    if (!participantIds.isEmpty()) {
                        passed = (long) attemptMapper.selectList(new LambdaQueryWrapper<ExamAttemptDO>()
                                        .eq(ExamAttemptDO::getPublishId, publishId)
                                        .eq(ExamAttemptDO::getStatus, 1)
                                        .ge(ExamAttemptDO::getTotalScore, exam.getPassScore())
                                        .in(ExamAttemptDO::getUserId, participantIds))
                                .stream()
                                .map(ExamAttemptDO::getUserId)
                                .filter(Objects::nonNull)
                                .distinct()
                                .count();
                    } else {
                        passed = attemptMapper.selectCount(
                                new LambdaQueryWrapper<ExamAttemptDO>()
                                        .eq(ExamAttemptDO::getPublishId, publishId)
                                        .eq(ExamAttemptDO::getStatus, 1)
                                        .ge(ExamAttemptDO::getTotalScore, exam.getPassScore()));
                    }
                    if (passed != null && submitted > 0) {
                        vo.setPassRate(Math.round(passed * 10000.0 / submitted) / 100.0);
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("fillAttemptStats failed, publishId={}", vo.getId(), ex);
        }
    }

    private void sendPublishNotice(ExamPublishDO publish) {
        ExamDO exam = examMapper.selectById(publish.getExamId());
        if (exam == null) return;
        String batchInfo = publish.getBatchLabel() != null ? " - " + publish.getBatchLabel() : "";
        String title = "考试通知：" + exam.getName() + batchInfo;
        StringBuilder content = new StringBuilder("考试：").append(exam.getName());
        if (publish.getEndAt() != null) {
            content.append("，截止：").append(publish.getEndAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        }
        content.append("。请按时完成。");
        String noticeKey = noticeRecordService.buildNoticeKey(HrNoticeRecordService.CHANNEL_IN_APP,
                BUSINESS_TYPE_EXAM_PUBLISH, publish.getId(), null);
        String remark = "publishId=" + publish.getId();
        try {
            NoticeCreateReqDTO reqDTO = new NoticeCreateReqDTO();
            reqDTO.setTitle(title);
            reqDTO.setType(NoticeTypeEnum.ANNOUNCEMENT.getType());
            reqDTO.setContent(content.toString());
            reqDTO.setStatus(CommonStatusEnum.ENABLE.getStatus());
            noticeApi.createNotice(reqDTO);
            Map<String, Object> payload = new HashMap<>();
            payload.put("title", reqDTO.getTitle());
            payload.put("content", reqDTO.getContent());
            pushNoticeToRecipients(publish, payload);
            recordNoticeSuccess(noticeKey, publish.getId(), title, content.toString(), remark);
        } catch (Exception ex) {
            recordNoticeFailure(noticeKey, publish.getId(), title, content.toString(), ex.getMessage(), remark);
            log.warn("sendPublishNotice failed, publishId={}", publish.getId(), ex);
        }
    }

    private void pushNoticeToRecipients(ExamPublishDO publish, Map<String, Object> payload) {
        Set<Long> userIds = examScopeSupport.resolveUserIds(
                publish.getPublishScopeJson(),
                publish.getTenantId());
        if (userIds.isEmpty()) {
            log.warn("pushNoticeToRecipients skipped, publishId={}, no recipients resolved", publish.getId());
            return;
        }
        for (Long userId : userIds) {
            if (userId == null) {
                continue;
            }
            webSocketSenderApi.sendObject(UserTypeEnum.ADMIN.getValue(), userId, "notice-push", payload);
        }
    }

    private void recordNoticeSuccess(String noticeKey, Long businessId, String title, String content, String remark) {
        try {
            noticeRecordService.recordSuccess(noticeKey, HrNoticeRecordService.CHANNEL_IN_APP,
                    BUSINESS_TYPE_EXAM_PUBLISH, businessId, null, title, content, remark);
        } catch (Exception ex) {
            log.warn("Persist exam publish notice success failed, businessId={}, reason={}",
                    businessId, ex.getMessage());
        }
    }

    private void recordNoticeFailure(String noticeKey, Long businessId, String title, String content,
                                     String errorMessage, String remark) {
        try {
            noticeRecordService.recordFailure(noticeKey, HrNoticeRecordService.CHANNEL_IN_APP,
                    BUSINESS_TYPE_EXAM_PUBLISH, businessId, null, title, content, errorMessage, remark);
        } catch (Exception ex) {
            log.warn("Persist exam publish notice failure failed, businessId={}, reason={}",
                    businessId, ex.getMessage());
        }
    }

    private void closeSinglePublish(ExamPublishDO publish) {
        ExamPublishDO update = new ExamPublishDO();
        update.setId(publish.getId());
        update.setStatus(2);
        if (publish.getEndAt() == null || publish.getEndAt().isAfter(LocalDateTime.now())) {
            update.setEndAt(LocalDateTime.now());
        }
        publishMapper.updateById(update);
        if (publish.getStatus() != null && publish.getStatus() == 1) {
            autoSubmitInProgressAttempts(publish.getId());
        }
    }

    private void autoSubmitInProgressAttempts(Long publishId) {
        List<ExamAttemptDO> inProgress = attemptMapper.selectList(
                new LambdaQueryWrapper<ExamAttemptDO>()
                        .eq(ExamAttemptDO::getPublishId, publishId)
                        .eq(ExamAttemptDO::getStatus, 0));
        if (inProgress.isEmpty()) return;
        for (ExamAttemptDO attempt : inProgress) {
            // 按已作答题目计分
            int totalScore = 0;
            List<ExamAnswerDO> answers = answerMapper.selectListByAttemptId(attempt.getId());
            if (answers != null && !answers.isEmpty()) {
                for (ExamAnswerDO answer : answers) {
                    if (answer.getAnswerScore() != null) {
                        totalScore += answer.getAnswerScore();
                    }
                }
            }
            attempt.setSubmitAt(LocalDateTime.now());
            attempt.setStatus(1);
            attempt.setTotalScore(totalScore);
            attemptMapper.updateById(attempt);
        }
        log.info("[autoSubmitInProgressAttempts] 自动提交 {} 份作答, publishId={}",
                inProgress.size(), publishId);
    }

    private boolean isRecurringEnded(ExamPublishDO parent, int generatedCount, LocalDateTime publishAt) {
        Integer endType = parent.getRepeatEndType();
        if (endType == null || endType == 0) return false;
        if (endType == 1 && parent.getRepeatEndAt() != null) {
            return publishAt.isAfter(parent.getRepeatEndAt());
        }
        if (endType == 2 && parent.getRepeatEndCount() != null) {
            return generatedCount >= parent.getRepeatEndCount();
        }
        return false;
    }

    private LocalDateTime calculateNextPublishAt(String repeatRuleJson, LocalDateTime from) {
        RepeatRule rule = parseRepeatRule(repeatRuleJson);
        if (rule == null) return from.plusMonths(1);

        int interval = (rule.getInterval() != null && rule.getInterval() > 0) ? rule.getInterval() : 1;
        String unit = rule.getUnit() != null ? rule.getUnit() : "month";

        switch (unit) {
            case "day":
                return from.plusDays(interval);
            case "week":
                LocalDateTime nextWeek = from.plusWeeks(interval);
                if (rule.getDayOfWeek() != null) {
                    int dayOfWeek = Math.max(1, Math.min(7, rule.getDayOfWeek()));
                    nextWeek = nextWeek.with(TemporalAdjusters.nextOrSame(DayOfWeek.of(dayOfWeek)))
                            .truncatedTo(ChronoUnit.DAYS);
                }
                return nextWeek;
            case "month":
                LocalDateTime next = from.plusMonths(interval);
                if (rule.getDayOfMonth() != null) {
                    next = next.withDayOfMonth(Math.min(rule.getDayOfMonth(), next.toLocalDate().lengthOfMonth()));
                    next = next.truncatedTo(ChronoUnit.DAYS);
                }
                return next;
            case "quarter":
                LocalDateTime nextQuarter = from.plusMonths((long) interval * 3);
                if (rule.getDayOfMonth() != null) {
                    nextQuarter = nextQuarter.withDayOfMonth(Math.min(rule.getDayOfMonth(), nextQuarter.toLocalDate().lengthOfMonth()))
                            .truncatedTo(ChronoUnit.DAYS);
                }
                return nextQuarter;
            case "year":
                LocalDateTime nextYear = from.plusYears(interval);
                if (rule.getDayOfMonth() != null) {
                    nextYear = nextYear.withDayOfMonth(Math.min(rule.getDayOfMonth(), nextYear.toLocalDate().lengthOfMonth()))
                            .truncatedTo(ChronoUnit.DAYS);
                }
                return nextYear;
            default:
                return from.plusMonths(1);
        }
    }

    private LocalDateTime calculateBatchEndAt(LocalDateTime startAt, RepeatRule rule) {
        if (rule == null) {
            return startAt.plusDays(7);
        }
        int duration = (rule.getBatchDuration() != null && rule.getBatchDuration() > 0) ? rule.getBatchDuration() : 7;
        String durationUnit = rule.getBatchDurationUnit() != null ? rule.getBatchDurationUnit() : "day";
        switch (durationUnit) {
            case "day":
                return startAt.plusDays(duration);
            case "week":
                return startAt.plusWeeks(duration);
            case "month":
                return startAt.plusMonths(duration);
            case "quarter":
                return startAt.plusMonths((long) duration * 3);
            case "year":
                return startAt.plusYears(duration);
            default:
                return startAt.plusDays(duration);
        }
    }

    private RepeatRule parseRepeatRule(String json) {
        if (json == null || json.trim().isEmpty()) return null;
        try {
            return objectMapper.readValue(json, RepeatRule.class);
        } catch (Exception ex) {
            log.warn("parseRepeatRule failed: {}", json, ex);
            return null;
        }
    }

    @Data
    private static class RepeatRule {
        private Integer interval;
        private String unit;
        private Integer dayOfMonth;
        private Integer dayOfWeek;
        private Integer batchDuration;
        private String batchDurationUnit;
    }

}
