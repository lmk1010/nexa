package com.kyx.service.hr.service.questionnaire;

import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.biz.system.tenant.TenantCommonApi;
import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.business.api.notice.NoticeApi;
import com.kyx.service.business.api.notice.dto.NoticeCreateReqDTO;
import com.kyx.service.business.api.notify.NotifyMessageSendApi;
import com.kyx.service.business.api.notify.dto.NotifySendSingleToUserReqDTO;
import com.kyx.service.business.enums.notice.NoticeTypeEnum;
import com.kyx.service.op.api.websocket.WebSocketSenderApi;
import com.kyx.foundation.common.enums.UserTypeEnum;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnairePublishPageReqVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnairePublishAddAssigneesReqVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnairePublishAddAssigneesRespVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnairePublishBatchRespVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnairePublishRespVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnairePublishSaveReqVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnairePublishScopePreviewRespVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnairePublishScopeUserRespVO;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnairePublishDO;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnaireDO;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnairePublicLinkDO;
import com.kyx.service.hr.dal.mysql.questionnaire.QuestionnairePublishMapper;
import com.kyx.service.hr.dal.mysql.questionnaire.QuestionnaireMapper;
import com.kyx.service.hr.dal.mysql.questionnaire.QuestionnaireAssignmentMapper;
import com.kyx.service.hr.dal.mysql.questionnaire.QuestionnaireAnswerMapper;
import com.kyx.service.hr.dal.mysql.questionnaire.QuestionnairePublicAnswerMapper;
import com.kyx.service.hr.dal.mysql.questionnaire.QuestionnairePublicLinkMapper;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnaireAssignmentDO;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnaireAnswerDO;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnairePublicAnswerDO;
import com.kyx.service.business.api.dept.DeptApi;
import com.kyx.service.business.api.dept.dto.DeptRespDTO;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.hr.service.notice.HrNoticeRecordService;
import com.kyx.service.hr.service.todo.HrTodoTaskService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.kyx.service.hr.enums.ErrorCodeConstants.QUESTIONNAIRE_NOT_EXISTS;
import static com.kyx.service.hr.enums.ErrorCodeConstants.QUESTIONNAIRE_PUBLISH_BATCH_CANNOT_ENABLE;
import static com.kyx.service.hr.enums.ErrorCodeConstants.QUESTIONNAIRE_PUBLISH_NOT_EXISTS;

/**
 * HR 问卷发布 Service 实现类
 *
 * @author MK
 */
@Service
@Validated
@Slf4j
public class QuestionnairePublishServiceImpl implements QuestionnairePublishService {

    private static final int ASSIGNMENT_STATUS_PENDING = 0;
    private static final int ASSIGNMENT_STATUS_SUBMITTED = 1;
    private static final int ASSIGNMENT_STATUS_NOT_REQUIRED = 2;
    private static final String BUSINESS_TYPE_QUESTIONNAIRE_PUBLISH = "QUESTIONNAIRE_PUBLISH";
    private static final String BUSINESS_TYPE_QUESTIONNAIRE_REMINDER = "QUESTIONNAIRE_REMINDER";
    private static final String NOTIFY_TEMPLATE_QUESTIONNAIRE_PUBLISH = "hr_questionnaire_publish_notice";
    private static final String NOTIFY_TEMPLATE_QUESTIONNAIRE_REMINDER = "hr_questionnaire_reminder_notice";
    private static final String QUESTIONNAIRE_SELF_ROUTE = "/hr/learning-center?tab=questionnaire";

    @Resource
    private QuestionnairePublishMapper publishMapper;
    @Resource
    private QuestionnaireMapper questionnaireMapper;
    @Resource
    private QuestionnaireAssignmentMapper assignmentMapper;
    @Resource
    private QuestionnaireAnswerMapper answerMapper;
    @Resource
    private QuestionnairePublicAnswerMapper publicAnswerMapper;
    @Resource
    private QuestionnairePublicLinkMapper publicLinkMapper;
    @Resource
    private NoticeApi noticeApi;
    @Resource
    private NotifyMessageSendApi notifyMessageSendApi;
    @Resource
    private WebSocketSenderApi webSocketSenderApi;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private TenantCommonApi tenantCommonApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private HrNoticeRecordService noticeRecordService;
    @Resource
    private HrTodoTaskService hrTodoTaskService;

    private final java.util.concurrent.ConcurrentMap<Long, Long> userTenantIdCache = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public Long createPublish(QuestionnairePublishSaveReqVO createReqVO) {
        QuestionnairePublishDO publish = BeanUtils.toBean(createReqVO, QuestionnairePublishDO.class);
        fillTenantIdIfNeeded(publish);
        normalizeEmptyJsonFields(publish);
        applyCreateDefaults(publish);
        validatePublishConfig(publish);
        // 周期性定时：计算下次发布时间
        if (isRecurringSchedule(publish)) {
            publish.setNextPublishTime(calcNextPublishTime(publish, null));
            publish.setStatus(1); // 启用状态
            publish.setSendAt(null);
            publish.setDeadlineAt(null);
            publish.setLastRemindTime(null);
        }
        publishMapper.insert(publish);
        // 立即发送（非周期）
        if (publish.getStatus() != null && publish.getStatus() == 1
                && publish.getSendType() != null && publish.getSendType() == 0) {
            QuestionnaireDO questionnaire = questionnaireMapper.selectById(publish.getQuestionnaireId());
            if (questionnaire != null) {
                questionnaire.setStatus(1);
                questionnaireMapper.updateById(questionnaire);
            }
            BatchContext batchContext = buildNextBatchContext(publish, LocalDateTime.now());
            applyCurrentBatch(publish, batchContext);
            publishMapper.updateById(buildBatchUpdateDO(publish));
            createAssignments(publish, questionnaire, batchContext);
            if (isPublishNoticeEnabled(publish)) {
                sendPublishNotice(publish, questionnaire);
            }
        }
        return publish.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePublish(QuestionnairePublishSaveReqVO updateReqVO) {
        QuestionnairePublishDO exist = validatePublishExists(updateReqVO.getId());
        QuestionnairePublishDO updateObj = BeanUtils.toBean(updateReqVO, QuestionnairePublishDO.class);
        normalizeEmptyJsonFields(updateObj);
        boolean assignmentStructureTouched = isAssignmentStructureTouched(updateObj, exist);
        boolean onlyCurrentBatchEligibilityTouched = isOnlyCurrentBatchEligibilityTouched(updateObj, exist);
        if (shouldValidateConfigOnUpdate(updateObj, exist)) {
            validatePublishConfig(mergeForValidation(updateObj, exist));
        }

        if (isRecurringSchedule(updateObj)) {
            updateObj.setNextPublishTime(calcNextPublishTime(updateObj, null));
            updateObj.setSendAt(null);
            updateObj.setDeadlineAt(null);
        }
        if (!Objects.equals(exist.getRemindRuleJson(), updateObj.getRemindRuleJson())) {
            updateObj.setLastRemindTime(null);
        }

        publishMapper.updateById(updateObj);
        QuestionnairePublishDO merged = mergeForRuntime(updateObj, exist);
        clearInactiveScheduleFields(merged);
        Integer effectiveSendType = updateObj.getSendType() != null ? updateObj.getSendType() : exist.getSendType();
        boolean justEnabled = updateObj.getStatus() != null && updateObj.getStatus() == 1
                && (exist.getStatus() == null || exist.getStatus() != 1);
        if (justEnabled && Objects.equals(effectiveSendType, 0)) {
            QuestionnaireDO questionnaire = questionnaireMapper.selectById(updateObj.getQuestionnaireId());
            if (questionnaire == null) {
                questionnaire = TenantUtils.executeIgnore(() -> questionnaireMapper.selectById(updateObj.getQuestionnaireId()));
            }
            if (questionnaire != null) {
                questionnaire.setStatus(1);
                questionnaireMapper.updateById(questionnaire);
            }
            QuestionnairePublishDO batchSeed = new QuestionnairePublishDO();
            batchSeed.setGeneratedCount(exist.getGeneratedCount());
            batchSeed.setDeadlineHours(updateObj.getDeadlineHours() != null
                    ? updateObj.getDeadlineHours() : exist.getDeadlineHours());
            batchSeed.setDeadlineAt(updateObj.getDeadlineAt() != null
                    ? updateObj.getDeadlineAt() : exist.getDeadlineAt());
            BatchContext batchContext = buildNextBatchContext(batchSeed, LocalDateTime.now());
            applyCurrentBatch(updateObj, batchContext);
            publishMapper.updateById(buildBatchUpdateDO(updateObj));
            createAssignments(updateObj, questionnaire, batchContext);
            if (isPublishNoticeEnabled(updateObj)) {
                sendPublishNotice(updateObj, questionnaire);
            }
        }

        boolean activeAfterUpdate = merged.getStatus() != null && merged.getStatus() == 1;
        boolean deadlineChanged = updateObj.getDeadlineAt() != null
                && !Objects.equals(updateObj.getDeadlineAt(), exist.getDeadlineAt());
        if (activeAfterUpdate && !isRecurringSchedule(merged) && deadlineChanged) {
            merged.setDeadlineAt(updateObj.getDeadlineAt());
            merged.setCurrentBatchEndAt(updateObj.getDeadlineAt());
            syncCurrentBatchDeadline(merged, updateObj.getDeadlineAt());
        }
        boolean supportsNextBatch = supportsNextBatchEffective(merged);
        boolean immediateApply = !supportsNextBatch
                || (updateReqVO.getApplyMode() != null && updateReqVO.getApplyMode() == 1);
        if (assignmentStructureTouched && activeAfterUpdate && !justEnabled && immediateApply && !onlyCurrentBatchEligibilityTouched) {
            if (hasCurrentBatchAssignments(merged)) {
                throw ServiceExceptionUtil.invalidParamException("已发布批次不能重建填写关系；如需新增人员，请使用追加填写人");
            }
            rebuildCurrentBatchAssignments(merged);
        }
        if (activeAfterUpdate && !justEnabled && onlyCurrentBatchEligibilityTouched) {
            applyCurrentBatchPeerEvaluationRules(merged);
        }
        if (updateObj.getStatus() != null && updateObj.getStatus() == 0 && exist.getStatus() != null && exist.getStatus() == 1) {
            revokeAssignments(exist.getId(), Boolean.TRUE.equals(updateReqVO.getClearSubmittedOnRevoke()));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuestionnairePublishAddAssigneesRespVO addAssignees(QuestionnairePublishAddAssigneesReqVO reqVO) {
        if (reqVO == null) {
            throw ServiceExceptionUtil.invalidParamException("追加填写人不能为空");
        }
        QuestionnairePublishDO publish = validatePublishExists(reqVO.getPublishId());
        if (publish.getStatus() == null || publish.getStatus() != 1) {
            throw ServiceExceptionUtil.invalidParamException("只有进行中的发布可以追加填写人");
        }
        QuestionnaireDO questionnaire = questionnaireMapper.selectById(publish.getQuestionnaireId());
        if (questionnaire == null) {
            questionnaire = TenantUtils.executeIgnore(() -> questionnaireMapper.selectById(publish.getQuestionnaireId()));
        }
        if (questionnaire == null) {
            throw ServiceExceptionUtil.exception(QUESTIONNAIRE_NOT_EXISTS);
        }

        Integer batchNo = resolveAppendBatchNo(publish, reqVO.getBatchNo());
        BatchContext batchContext = buildExistingBatchContext(publish, batchNo);
        if (batchContext.getBatchEndAt() != null && !batchContext.getBatchEndAt().isAfter(LocalDateTime.now())) {
            throw ServiceExceptionUtil.invalidParamException("当前批次已截止，请重新启用批次后再追加填写人");
        }

        Set<Long> evaluatorIds = normalizeUserIdSet(reqVO.getEvaluatorUserIds());
        if (evaluatorIds.isEmpty()) {
            throw ServiceExceptionUtil.invalidParamException("请选择新增填写人");
        }
        boolean peerQuestionnaire = isPeerQuestionnaire(questionnaire);
        Set<Long> targetIds = peerQuestionnaire ? normalizeUserIdSet(reqVO.getTargetUserIds()) : new HashSet<>();
        if (peerQuestionnaire && targetIds.isEmpty()) {
            throw ServiceExceptionUtil.invalidParamException("互评问卷追加人员时，请同时选择被评人");
        }

        Set<Long> allUserIds = new HashSet<>(evaluatorIds);
        allUserIds.addAll(targetIds);
        java.util.Map<Long, AdminUserRespDTO> userMap = loadUserMap(allUserIds);
        int invalidUserCount = (int) allUserIds.stream()
                .filter(userId -> !userMap.containsKey(userId))
                .count();

        boolean excludeSelf = true;
        PublishScope publishScope = null;
        try {
            publishScope = parseScopeOrThrow(publish.getPublishScopeJson());
            excludeSelf = publishScope.getExcludeSelf() == null || Boolean.TRUE.equals(publishScope.getExcludeSelf());
        } catch (Exception ignored) {
            // 追加人员使用保守默认：互评不生成本人自评。
        }

        int createdCount = 0;
        int skippedDuplicateCount = 0;
        Set<Long> createdEvaluatorIds = new HashSet<>();
        for (Long evaluatorId : evaluatorIds) {
            AdminUserRespDTO evaluator = userMap.get(evaluatorId);
            if (evaluator == null) {
                continue;
            }
            if (!peerQuestionnaire) {
                boolean exists = TenantUtils.executeIgnore(
                        () -> assignmentMapper.existsByPublishIdAndPair(publish.getId(), evaluatorId, null, batchNo));
                if (exists) {
                    skippedDuplicateCount++;
                    continue;
                }
                QuestionnaireAssignmentDO assignment = buildAssignmentDO(
                        publish, questionnaire, batchContext, evaluator, evaluatorId, null, null,
                        resolveWriteTenantId(publish, questionnaire, evaluator));
                TenantUtils.executeIgnore(() -> {
                    assignmentMapper.insert(assignment);
                    return null;
                });
                createdCount++;
                createdEvaluatorIds.add(evaluatorId);
                continue;
            }

            for (Long targetId : targetIds) {
                AdminUserRespDTO target = userMap.get(targetId);
                if (target == null) {
                    continue;
                }
                if (excludeSelf && Objects.equals(evaluatorId, targetId)) {
                    skippedDuplicateCount++;
                    continue;
                }
                if (isPeerAssignmentIgnored(publishScope, evaluatorId, targetId)) {
                    skippedDuplicateCount++;
                    continue;
                }
                boolean exists = TenantUtils.executeIgnore(
                        () -> assignmentMapper.existsByPublishIdAndPair(publish.getId(), evaluatorId, targetId, batchNo));
                if (exists) {
                    skippedDuplicateCount++;
                    continue;
                }
                QuestionnaireAssignmentDO assignment = buildAssignmentDO(
                        publish, questionnaire, batchContext, evaluator, evaluatorId, target, targetId,
                        resolveWriteTenantId(publish, questionnaire, evaluator));
                TenantUtils.executeIgnore(() -> {
                    assignmentMapper.insert(assignment);
                    return null;
                });
                createdCount++;
                createdEvaluatorIds.add(evaluatorId);
            }
        }

        refreshGeneratedTodoTasks(createdCount, questionnaire.getId(), batchNo);
        if (createdCount > 0 && !Boolean.FALSE.equals(reqVO.getSendNotice())) {
            sendAppendNoticeToEvaluators(publish, questionnaire, createdEvaluatorIds);
        }

        QuestionnairePublishAddAssigneesRespVO respVO = new QuestionnairePublishAddAssigneesRespVO();
        respVO.setPublishId(publish.getId());
        respVO.setBatchNo(batchNo);
        respVO.setRequestedCount(evaluatorIds.size());
        respVO.setCreatedCount(createdCount);
        respVO.setSkippedDuplicateCount(skippedDuplicateCount);
        respVO.setInvalidUserCount(invalidUserCount);
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePublish(Long id) {
        validatePublishExists(id);
        clearPublishRelatedData(id);
        publishMapper.deleteById(id);
        TenantUtils.executeIgnore(() -> {
            publishMapper.deleteById(id);
            return null;
        });
    }

    private void clearPublishRelatedData(Long publishId) {
        assignmentMapper.deleteByPublishId(publishId);
        answerMapper.deleteByPublishId(publishId);
        publicAnswerMapper.deleteByPublishId(publishId);
        TenantUtils.executeIgnore(() -> {
            assignmentMapper.deleteByPublishId(publishId);
            answerMapper.deleteByPublishId(publishId);
            publicAnswerMapper.deleteByPublishId(publishId);
            return null;
        });

        List<QuestionnairePublicLinkDO> links = publicLinkMapper.selectListByPublishId(publishId);
        if (links == null) {
            links = new ArrayList<>();
        }
        List<QuestionnairePublicLinkDO> ignoreLinks = TenantUtils.executeIgnore(() -> publicLinkMapper.selectListByPublishId(publishId));
        if (ignoreLinks != null && !ignoreLinks.isEmpty()) {
            links.addAll(ignoreLinks);
        }

        if (!links.isEmpty()) {
            List<Long> linkIds = links.stream()
                    .map(QuestionnairePublicLinkDO::getId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            if (!linkIds.isEmpty()) {
                publicAnswerMapper.deleteByLinkIds(linkIds);
                List<Long> finalLinkIds = linkIds;
                TenantUtils.executeIgnore(() -> {
                    publicAnswerMapper.deleteByLinkIds(finalLinkIds);
                    return null;
                });
            }
        }

        publicLinkMapper.deleteByPublishId(publishId);
        TenantUtils.executeIgnore(() -> {
            publicLinkMapper.deleteByPublishId(publishId);
            return null;
        });
    }

    @Override
    public QuestionnairePublishRespVO getPublish(Long id) {
        QuestionnairePublishDO publish = getAccessiblePublish(id);
        if (publish == null) {
            throw ServiceExceptionUtil.exception(QUESTIONNAIRE_PUBLISH_NOT_EXISTS);
        }
        QuestionnairePublishRespVO vo = BeanUtils.toBean(publish, QuestionnairePublishRespVO.class);
        QuestionnaireDO questionnaire = questionnaireMapper.selectById(publish.getQuestionnaireId());
        if (questionnaire == null) {
            questionnaire = TenantUtils.executeIgnore(() -> questionnaireMapper.selectById(publish.getQuestionnaireId()));
        }
        if (questionnaire != null) {
            vo.setQuestionnaireName(questionnaire.getName());
            vo.setQuestionnaireType(questionnaire.getType());
        }
        fillAssignmentStats(vo);
        return vo;
    }

    private QuestionnairePublishDO getAccessiblePublish(Long id) {
        QuestionnairePublishDO publish = publishMapper.selectById(id);
        if (publish != null) {
            return publish;
        }
        Long loginUserId = com.kyx.foundation.security.core.util.SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null || !hasAssignmentAccessForPublish(id, loginUserId)) {
            return null;
        }
        return TenantUtils.executeIgnore(() -> publishMapper.selectById(id));
    }

    private boolean hasAssignmentAccessForPublish(Long publishId, Long userId) {
        if (publishId == null || userId == null) {
            return false;
        }
        Long count = TenantUtils.executeIgnore(() -> assignmentMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<QuestionnaireAssignmentDO>()
                .eq(QuestionnaireAssignmentDO::getPublishId, publishId)
                .eq(QuestionnaireAssignmentDO::getEvaluatorId, userId)));
        return count != null && count > 0;
    }

    @Override
    public PageResult<QuestionnairePublishRespVO> getPublishPage(QuestionnairePublishPageReqVO pageReqVO) {
        PageResult<QuestionnairePublishDO> pageResult = publishMapper.selectPage(pageReqVO);
        PageResult<QuestionnairePublishRespVO> resp = BeanUtils.toBean(pageResult, QuestionnairePublishRespVO.class);
        if (resp.getList() != null) {
            for (QuestionnairePublishRespVO vo : resp.getList()) {
                QuestionnaireDO questionnaire = questionnaireMapper.selectById(vo.getQuestionnaireId());
                if (questionnaire != null) {
                    vo.setQuestionnaireName(questionnaire.getName());
                    vo.setQuestionnaireType(questionnaire.getType());
                }
                // 统计填写人数
                fillAssignmentStats(vo);
            }
        }
        return resp;
    }

    @Override
    public void publishScheduled() {
        LocalDateTime now = LocalDateTime.now();
        List<QuestionnairePublishDO> dueList = TenantUtils.executeIgnore(() -> publishMapper.selectListByScheduleDue(now));
        for (QuestionnairePublishDO publish : dueList) {
            TenantUtils.executeIgnore(() -> {
                if (publish.getScheduleType() != null && publish.getScheduleType() > 0) {
                    return null;
                }
                BatchContext batchContext = buildNextBatchContext(publish,
                        publish.getSendAt() != null ? publish.getSendAt() : now);
                publish.setStatus(1);
                applyCurrentBatch(publish, batchContext);
                publishMapper.updateById(buildBatchUpdateDO(publish));
                QuestionnaireDO questionnaire = questionnaireMapper.selectById(publish.getQuestionnaireId());
                if (questionnaire != null && (questionnaire.getStatus() == null || questionnaire.getStatus() != 1)) {
                    questionnaire.setStatus(1);
                    questionnaireMapper.updateById(questionnaire);
                }
                createAssignments(publish, questionnaire, batchContext);
                if (isPublishNoticeEnabled(publish)) {
                    sendPublishNotice(publish, questionnaire);
                }
                return null;
            });
        }

        List<QuestionnairePublishDO> recurringList = TenantUtils.executeIgnore(() -> publishMapper.selectRecurringDue(now));
        for (QuestionnairePublishDO publish : recurringList) {
            TenantUtils.executeIgnore(() -> {
                QuestionnaireDO questionnaire = questionnaireMapper.selectById(publish.getQuestionnaireId());
                LocalDateTime batchStartTime = publish.getNextPublishTime() != null ? publish.getNextPublishTime() : now;
                BatchContext batchContext = buildNextBatchContext(publish, batchStartTime);
                applyCurrentBatch(publish, batchContext);
                createAssignments(publish, questionnaire, batchContext);
                if (isPublishNoticeEnabled(publish)) {
                    sendPublishNotice(publish, questionnaire);
                }
                publish.setLastPublishTime(batchStartTime);
                publish.setNextPublishTime(calcNextPublishTime(publish, batchStartTime));
                publishMapper.updateById(buildBatchUpdateDO(publish));
                log.info("周期发布执行: publishId={}, next={}", publish.getId(), publish.getNextPublishTime());
                return null;
            });
        }
    }

    @Override
    public void processDueDeadlines() {
        LocalDateTime now = LocalDateTime.now();
        List<QuestionnairePublishDO> dueList = TenantUtils.executeIgnore(() -> publishMapper.selectListByNeedClose(now));
        for (QuestionnairePublishDO publish : dueList) {
            TenantUtils.executeIgnore(() -> {
                publish.setStatus(2);
                publishMapper.updateById(publish);
                return null;
            });
        }
    }

    @Override
    public void sendDueReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<QuestionnairePublishDO> reminderList = TenantUtils.executeIgnore(() -> publishMapper.selectListByNeedRemind());
        for (QuestionnairePublishDO publish : reminderList) {
            TenantUtils.executeIgnore(() -> {
                RemindRule remindRule = parseRemindRule(publish.getRemindRuleJson());
                if (!Boolean.TRUE.equals(remindRule.getEnabled())) {
                    return null;
                }
                if (!containsNoticeChannel(remindRule)) {
                    return null;
                }
                if (!isRemindTimeReached(remindRule, now)) {
                    return null;
                }
                if (isSameDay(publish.getLastRemindTime(), now)) {
                    return null;
                }
                QuestionnaireDO questionnaire = questionnaireMapper.selectById(publish.getQuestionnaireId());
                sendReminderNotice(publish, questionnaire);
                QuestionnairePublishDO updateObj = new QuestionnairePublishDO();
                updateObj.setId(publish.getId());
                updateObj.setLastRemindTime(now);
                publishMapper.updateById(updateObj);
                return null;
            });
        }
    }

    @Override
    public java.util.List<QuestionnairePublishRespVO> getPublishList(Integer status) {
        List<QuestionnairePublishDO> list = publishMapper.selectListByStatus(status);
        List<QuestionnairePublishRespVO> resp = BeanUtils.toBean(list, QuestionnairePublishRespVO.class);
        if (resp != null) {
            for (QuestionnairePublishRespVO vo : resp) {
                QuestionnaireDO questionnaire = questionnaireMapper.selectById(vo.getQuestionnaireId());
                if (questionnaire != null) {
                    vo.setQuestionnaireName(questionnaire.getName());
                    vo.setQuestionnaireType(questionnaire.getType());
                }
            }
        }
        return resp;
    }

    @Override
    public java.util.List<QuestionnairePublishRespVO> getMyPublishList(Long userId, Integer status) {
        if (userId == null) {
            return java.util.Collections.emptyList();
        }
        Set<Long> publishIds = assignmentMapper.selectPublishIdsByEvaluatorId(userId, status);
        if (publishIds == null || publishIds.isEmpty()) {
            publishIds = TenantUtils.executeIgnore(() -> assignmentMapper.selectPublishIdsByEvaluatorId(userId, status));
        }
        if (publishIds == null || publishIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        List<QuestionnairePublishDO> list = publishMapper.selectListByIds(publishIds);
        if (list == null) {
            list = new ArrayList<>();
        }
        Set<Long> loadedIds = list.stream()
                .filter(Objects::nonNull)
                .map(QuestionnairePublishDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (loadedIds.size() < publishIds.size()) {
            Set<Long> missingIds = new HashSet<>(publishIds);
            missingIds.removeAll(loadedIds);
            for (Long publishId : missingIds) {
                QuestionnairePublishDO publish = TenantUtils.executeIgnore(() -> publishMapper.selectById(publishId));
                if (publish != null) {
                    list.add(publish);
                }
            }
        }
        if (list.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        list.sort((a, b) -> Long.compare(
                b != null && b.getId() != null ? b.getId() : 0L,
                a != null && a.getId() != null ? a.getId() : 0L));
        List<QuestionnairePublishRespVO> resp = BeanUtils.toBean(list, QuestionnairePublishRespVO.class);
        for (QuestionnairePublishRespVO vo : resp) {
            QuestionnaireDO questionnaire = questionnaireMapper.selectById(vo.getQuestionnaireId());
            if (questionnaire == null) {
                questionnaire = TenantUtils.executeIgnore(() -> questionnaireMapper.selectById(vo.getQuestionnaireId()));
            }
            if (questionnaire != null) {
                vo.setQuestionnaireName(questionnaire.getName());
                vo.setQuestionnaireType(questionnaire.getType());
            }
            fillAssignmentStats(vo);
        }
        return resp;
    }

    @Override
    public List<QuestionnairePublishBatchRespVO> getPublishBatchList(Long publishId) {
        QuestionnairePublishDO publish = validatePublishExists(publishId);
        List<QuestionnaireAssignmentDO> assignments = selectAssignmentsByPublishId(publishId);
        if (assignments == null || assignments.isEmpty()) {
            if (publish.getCurrentBatchNo() == null || publish.getCurrentBatchNo() <= 0) {
                return java.util.Collections.emptyList();
            }
            QuestionnairePublishBatchRespVO only = new QuestionnairePublishBatchRespVO();
            only.setPublishId(publishId);
            only.setQuestionnaireId(publish.getQuestionnaireId());
            only.setBatchNo(publish.getCurrentBatchNo());
            only.setBatchLabel(publish.getCurrentBatchLabel() != null
                    ? publish.getCurrentBatchLabel()
                    : "第" + publish.getCurrentBatchNo() + "期");
            only.setBatchStartAt(publish.getCurrentBatchStartAt());
            only.setBatchEndAt(publish.getCurrentBatchEndAt());
            only.setTotalCount(0);
            only.setSubmittedCount(0);
            only.setPendingCount(0);
            only.setStatus(2);
            return java.util.Collections.singletonList(only);
        }

        LinkedHashMap<Integer, List<QuestionnaireAssignmentDO>> batchMap = assignments.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(item -> item.getBatchNo() != null ? item.getBatchNo() : 1,
                        LinkedHashMap::new,
                        Collectors.toList()));

        LocalDateTime now = LocalDateTime.now();
        List<QuestionnairePublishBatchRespVO> result = new ArrayList<>();
        for (java.util.Map.Entry<Integer, List<QuestionnaireAssignmentDO>> entry : batchMap.entrySet()) {
            Integer batchNo = entry.getKey();
            List<QuestionnaireAssignmentDO> batchAssignments = entry.getValue();
            if (batchAssignments == null || batchAssignments.isEmpty()) {
                continue;
            }

            String batchLabel = null;
            LocalDateTime batchStartAt = null;
            LocalDateTime batchEndAt = null;
            for (QuestionnaireAssignmentDO assignment : batchAssignments) {
                if (batchLabel == null && assignment.getBatchLabel() != null && !assignment.getBatchLabel().trim().isEmpty()) {
                    batchLabel = assignment.getBatchLabel();
                }
                if (assignment.getBatchStartAt() != null && (batchStartAt == null || assignment.getBatchStartAt().isBefore(batchStartAt))) {
                    batchStartAt = assignment.getBatchStartAt();
                }
                if (assignment.getBatchEndAt() != null && (batchEndAt == null || assignment.getBatchEndAt().isAfter(batchEndAt))) {
                    batchEndAt = assignment.getBatchEndAt();
                }
            }

            if (batchLabel == null || batchLabel.trim().isEmpty()) {
                batchLabel = "第" + batchNo + "期";
            }
            if (batchStartAt == null && Objects.equals(batchNo, publish.getCurrentBatchNo())) {
                batchStartAt = publish.getCurrentBatchStartAt();
            }
            if (batchEndAt == null && Objects.equals(batchNo, publish.getCurrentBatchNo())) {
                batchEndAt = publish.getCurrentBatchEndAt();
            }

            AssignmentProgressStats progressStats = buildAssignmentProgressStats(
                    batchAssignments, extractExcludedEvaluatorIds(publish.getPublishScopeJson()));
            int totalCount = progressStats.getTotalCount();
            int submittedCount = progressStats.getSubmittedCount();
            int pendingCount = progressStats.getPendingCount();
            int status;
            if (pendingCount <= 0) {
                status = 2;
            } else if (batchEndAt != null && !now.isBefore(batchEndAt)) {
                status = 3;
            } else {
                status = 1;
            }

            QuestionnairePublishBatchRespVO batchVO = new QuestionnairePublishBatchRespVO();
            batchVO.setPublishId(publishId);
            batchVO.setQuestionnaireId(publish.getQuestionnaireId());
            batchVO.setBatchNo(batchNo);
            batchVO.setBatchLabel(batchLabel);
            batchVO.setBatchStartAt(batchStartAt);
            batchVO.setBatchEndAt(batchEndAt);
            batchVO.setTotalCount(totalCount);
            batchVO.setSubmittedCount(submittedCount);
            batchVO.setPendingCount(pendingCount);
            batchVO.setPendingUserNames(progressStats.getPendingUserNames());
            batchVO.setStatus(status);
            result.add(batchVO);
        }

        result.sort((left, right) -> Integer.compare(
                right != null && right.getBatchNo() != null ? right.getBatchNo() : 0,
                left != null && left.getBatchNo() != null ? left.getBatchNo() : 0));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finishBatch(Long publishId, Integer batchNo) {
        QuestionnairePublishDO publish = validatePublishExists(publishId);
        Integer targetBatchNo = batchNo;
        if (targetBatchNo == null || targetBatchNo <= 0) {
            targetBatchNo = publish.getCurrentBatchNo();
        }
        if (targetBatchNo == null || targetBatchNo <= 0) {
            targetBatchNo = assignmentMapper.selectLatestBatchNoByPublishId(publishId);
        }
        if (targetBatchNo == null || targetBatchNo <= 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<QuestionnaireAssignmentDO> assignments = selectAssignmentsByPublishIdAndBatchNo(publishId, targetBatchNo, shouldReadAssignmentsCrossTenant(publishId));
        if (assignments != null && !assignments.isEmpty()) {
            for (QuestionnaireAssignmentDO assignment : assignments) {
                if (assignment == null || assignment.getId() == null) {
                    continue;
                }
                if (assignment.getBatchEndAt() != null && !assignment.getBatchEndAt().isAfter(now)) {
                    continue;
                }
                QuestionnaireAssignmentDO updateAssignment = new QuestionnaireAssignmentDO();
                updateAssignment.setId(assignment.getId());
                updateAssignment.setBatchEndAt(now);
                assignmentMapper.updateById(updateAssignment);
            }
        }

        QuestionnairePublishDO updatePublish = new QuestionnairePublishDO();
        updatePublish.setId(publishId);
        if (Objects.equals(targetBatchNo, publish.getCurrentBatchNo())) {
            updatePublish.setCurrentBatchEndAt(now);
            updatePublish.setDeadlineAt(now);
        }
        if (publish.getScheduleType() == null || publish.getScheduleType() == 0) {
            updatePublish.setStatus(2);
        }
        publishMapper.updateById(updatePublish);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableBatch(Long publishId, Integer batchNo) {
        QuestionnairePublishDO publish = validatePublishExists(publishId);
        Integer targetBatchNo = batchNo;
        if (targetBatchNo == null || targetBatchNo <= 0) {
            targetBatchNo = publish.getCurrentBatchNo();
        }
        if (targetBatchNo == null || targetBatchNo <= 0) {
            targetBatchNo = assignmentMapper.selectLatestBatchNoByPublishId(publishId);
        }
        if (targetBatchNo == null || targetBatchNo <= 0) {
            throw ServiceExceptionUtil.exception(QUESTIONNAIRE_PUBLISH_BATCH_CANNOT_ENABLE);
        }

        List<QuestionnaireAssignmentDO> assignments = selectAssignmentsByPublishIdAndBatchNo(publishId, targetBatchNo, shouldReadAssignmentsCrossTenant(publishId));
        if (assignments == null || assignments.isEmpty()) {
            throw ServiceExceptionUtil.exception(QUESTIONNAIRE_PUBLISH_BATCH_CANNOT_ENABLE);
        }

        int pendingCount = (int) assignments.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getStatus() == null || item.getStatus() == 0)
                .count();
        if (pendingCount <= 0) {
            throw ServiceExceptionUtil.exception(QUESTIONNAIRE_PUBLISH_BATCH_CANNOT_ENABLE);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime batchStartAt = assignments.stream()
                .filter(Objects::nonNull)
                .map(QuestionnaireAssignmentDO::getBatchStartAt)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        if (batchStartAt == null && Objects.equals(targetBatchNo, publish.getCurrentBatchNo())) {
            batchStartAt = publish.getCurrentBatchStartAt();
        }

        LocalDateTime batchEndAt = null;
        if (publish.getDeadlineHours() != null && publish.getDeadlineHours() > 0) {
            LocalDateTime baseStart = batchStartAt != null ? batchStartAt : now;
            batchEndAt = baseStart.plusHours(publish.getDeadlineHours());
            if (!batchEndAt.isAfter(now)) {
                batchEndAt = now.plusHours(publish.getDeadlineHours());
            }
        } else if (Objects.equals(targetBatchNo, publish.getCurrentBatchNo())
                && publish.getCurrentBatchEndAt() != null
                && publish.getCurrentBatchEndAt().isAfter(now)) {
            batchEndAt = publish.getCurrentBatchEndAt();
        } else if (publish.getDeadlineAt() != null && publish.getDeadlineAt().isAfter(now)) {
            batchEndAt = publish.getDeadlineAt();
        }

        if (batchEndAt == null || !batchEndAt.isAfter(now)) {
            throw ServiceExceptionUtil.exception(QUESTIONNAIRE_PUBLISH_BATCH_CANNOT_ENABLE);
        }

        for (QuestionnaireAssignmentDO assignment : assignments) {
            if (assignment == null || assignment.getId() == null) {
                continue;
            }
            QuestionnaireAssignmentDO updateAssignment = new QuestionnaireAssignmentDO();
            updateAssignment.setId(assignment.getId());
            updateAssignment.setBatchEndAt(batchEndAt);
            assignmentMapper.updateById(updateAssignment);
        }

        QuestionnairePublishDO updatePublish = new QuestionnairePublishDO();
        updatePublish.setId(publishId);
        if (Objects.equals(targetBatchNo, publish.getCurrentBatchNo())) {
            updatePublish.setCurrentBatchEndAt(batchEndAt);
            updatePublish.setDeadlineAt(batchEndAt);
        }
        if (publish.getScheduleType() == null || publish.getScheduleType() == 0) {
            updatePublish.setStatus(1);
        }
        publishMapper.updateById(updatePublish);
    }

    private QuestionnairePublishDO validatePublishExists(Long id) {
        QuestionnairePublishDO publish = publishMapper.selectById(id);
        if (publish == null) {
            throw ServiceExceptionUtil.exception(QUESTIONNAIRE_PUBLISH_NOT_EXISTS);
        }
        return publish;
    }

    private void validateQuestionnaireExists(Long questionnaireId) {
        if (questionnaireId == null || questionnaireMapper.selectById(questionnaireId) == null) {
            throw ServiceExceptionUtil.exception(QUESTIONNAIRE_NOT_EXISTS);
        }
    }

    private void validatePublishConfig(QuestionnairePublishDO publish) {
        if (publish == null) {
            throw ServiceExceptionUtil.invalidParamException("发布配置不能为空");
        }
        validateQuestionnaireExists(publish.getQuestionnaireId());

        PublishScope scope = parseScopeOrThrow(publish.getPublishScopeJson());
        if (!hasScopeSelected(scope)) {
            throw ServiceExceptionUtil.invalidParamException("请至少选择一个发布范围（部门/角色/人员）");
        }
        if (!hasEffectiveEvaluatorSelected(scope)) {
            throw ServiceExceptionUtil.invalidParamException("当前发布范围排除后没有匹配到人员，请调整部门、角色或排除名单");
        }
        validateTargetScope(scope);

        Integer sendType = publish.getSendType() != null ? publish.getSendType() : 0;
        if (sendType != 0 && sendType != 1) {
            throw ServiceExceptionUtil.invalidParamException("发送方式无效");
        }
        Integer scheduleType = publish.getScheduleType() != null ? publish.getScheduleType() : 0;
        if (scheduleType < 0 || scheduleType > 3) {
            throw ServiceExceptionUtil.invalidParamException("发送周期无效");
        }

        if (sendType == 0) {
            if (publish.getDeadlineAt() == null) {
                throw ServiceExceptionUtil.invalidParamException("立即发送需设置截止时间");
            }
            return;
        }

        if (scheduleType == 0) {
            if (publish.getSendAt() == null) {
                throw ServiceExceptionUtil.invalidParamException("一次性定时需设置发送时间");
            }
            if (publish.getDeadlineAt() == null) {
                throw ServiceExceptionUtil.invalidParamException("一次性定时需设置截止时间");
            }
            if (!publish.getDeadlineAt().isAfter(publish.getSendAt())) {
                throw ServiceExceptionUtil.invalidParamException("截止时间必须晚于发送时间");
            }
            return;
        }

        if (!isValidScheduleTime(publish.getScheduleTime())) {
            throw ServiceExceptionUtil.invalidParamException("周期发送时刻格式错误，应为 HH:mm");
        }
        if (scheduleType == 2 && (publish.getScheduleDayOfWeek() == null
                || publish.getScheduleDayOfWeek() < 1
                || publish.getScheduleDayOfWeek() > 7)) {
            throw ServiceExceptionUtil.invalidParamException("每周发布需设置 1-7 的星期值");
        }
        if (scheduleType == 3 && (publish.getScheduleDayOfMonth() == null
                || publish.getScheduleDayOfMonth() < 1
                || publish.getScheduleDayOfMonth() > 31)) {
            throw ServiceExceptionUtil.invalidParamException("每月发布需设置 1-31 的日期");
        }
        if (publish.getDeadlineHours() == null || publish.getDeadlineHours() < 1 || publish.getDeadlineHours() > 720) {
            throw ServiceExceptionUtil.invalidParamException("周期发布截止时长需为 1-720 小时");
        }
    }

    private PublishScope parseScopeOrThrow(String publishScopeJson) {
        if (publishScopeJson == null || publishScopeJson.trim().isEmpty()) {
            throw ServiceExceptionUtil.invalidParamException("发布范围不能为空");
        }
        try {
            return objectMapper.readValue(publishScopeJson, PublishScope.class);
        } catch (Exception ex) {
            throw ServiceExceptionUtil.invalidParamException("发布范围格式错误");
        }
    }

    private boolean hasScopeSelected(PublishScope scope) {
        if (scope == null) {
            return false;
        }
        return hasAnyId(scope.getDeptIds()) || hasAnyId(scope.getRoleIds()) || hasAnyId(scope.getUserIds());
    }

    private boolean hasEffectiveEvaluatorSelected(PublishScope scope) {
        try {
            Set<Long> userIds = resolveUserIdsByScope(scope);
            removeExcludedUsers(userIds, scope);
            return userIds != null && !userIds.isEmpty();
        } catch (Exception ex) {
            log.warn("validate effective questionnaire publish scope failed", ex);
            return false;
        }
    }

    private void validateTargetScope(PublishScope scope) {
        if (scope == null || scope.getTargetScope() == null) {
            return;
        }
        TargetScope targetScope = scope.getTargetScope();
        if (targetScope.getScope() == null || targetScope.getScope().trim().isEmpty()) {
            return;
        }
        String targetScopeType = targetScope.getScope().trim();
        if (("dept".equals(targetScopeType) || "user".equals(targetScopeType)) && !hasAnyId(targetScope.getIds())) {
            throw ServiceExceptionUtil.invalidParamException("互评规则中被评人范围已指定对象，但未配置具体人员/部门");
        }
    }

    private boolean hasAnyId(List<Long> ids) {
        return ids != null && ids.stream().anyMatch(Objects::nonNull);
    }

    private boolean isValidScheduleTime(String scheduleTime) {
        if (scheduleTime == null || scheduleTime.trim().isEmpty()) {
            return false;
        }
        try {
            LocalTime.parse(scheduleTime.trim(), DateTimeFormatter.ofPattern("HH:mm"));
            return true;
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    private boolean shouldValidateConfigOnUpdate(QuestionnairePublishDO updateObj, QuestionnairePublishDO exist) {
        if (updateObj == null) {
            return false;
        }
        boolean enableNow = updateObj.getStatus() != null && updateObj.getStatus() == 1
                && (exist == null || exist.getStatus() == null || exist.getStatus() != 1);
        return isPublishConfigTouched(updateObj) || enableNow;
    }

    private boolean isPublishConfigTouched(QuestionnairePublishDO updateObj) {
        if (updateObj == null) {
            return false;
        }
        return updateObj.getQuestionnaireId() != null
                || updateObj.getPublishScopeJson() != null
                || updateObj.getSendType() != null
                || updateObj.getScheduleType() != null
                || updateObj.getScheduleDayOfWeek() != null
                || updateObj.getScheduleDayOfMonth() != null
                || updateObj.getScheduleTime() != null
                || updateObj.getDeadlineHours() != null
                || updateObj.getSendAt() != null
                || updateObj.getDeadlineAt() != null;
    }

    private boolean isAssignmentStructureTouched(QuestionnairePublishDO updateObj, QuestionnairePublishDO exist) {
        if (updateObj == null || exist == null) {
            return false;
        }
        return isChanged(updateObj.getQuestionnaireId(), exist.getQuestionnaireId())
                || isChanged(updateObj.getPublishScopeJson(), exist.getPublishScopeJson())
                || isChanged(updateObj.getSendType(), exist.getSendType())
                || isChanged(updateObj.getScheduleType(), exist.getScheduleType())
                || isChanged(updateObj.getScheduleDayOfWeek(), exist.getScheduleDayOfWeek())
                || isChanged(updateObj.getScheduleDayOfMonth(), exist.getScheduleDayOfMonth())
                || isChanged(updateObj.getScheduleTime(), exist.getScheduleTime())
                || isChanged(updateObj.getDeadlineHours(), exist.getDeadlineHours())
                || isChanged(updateObj.getSendAt(), exist.getSendAt());
    }

    private boolean isChanged(Object updateValue, Object existValue) {
        return updateValue != null && !Objects.equals(updateValue, existValue);
    }

    private boolean hasCurrentBatchAssignments(QuestionnairePublishDO publish) {
        if (publish == null || publish.getId() == null) {
            return false;
        }
        Integer resolvedBatchNo = publish.getCurrentBatchNo();
        if (resolvedBatchNo == null || resolvedBatchNo <= 0) {
            resolvedBatchNo = assignmentMapper.selectLatestBatchNoByPublishId(publish.getId());
        }
        if (resolvedBatchNo == null || resolvedBatchNo <= 0) {
            return false;
        }
        final Integer queryBatchNo = resolvedBatchNo;
        Long count = TenantUtils.executeIgnore(() -> assignmentMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<QuestionnaireAssignmentDO>()
                        .eq(QuestionnaireAssignmentDO::getPublishId, publish.getId())
                        .eq(QuestionnaireAssignmentDO::getBatchNo, queryBatchNo)
        ));
        return count != null && count > 0;
    }

    private boolean supportsNextBatchEffective(QuestionnairePublishDO publish) {
        return publish != null
                && publish.getSendType() != null
                && publish.getSendType() == 1
                && publish.getScheduleType() != null
                && publish.getScheduleType() > 0;
    }

    private QuestionnairePublishDO mergeForValidation(QuestionnairePublishDO updateObj, QuestionnairePublishDO exist) {
        QuestionnairePublishDO merged = new QuestionnairePublishDO();
        merged.setQuestionnaireId(updateObj.getQuestionnaireId() != null ? updateObj.getQuestionnaireId() : exist.getQuestionnaireId());
        merged.setPublishScopeJson(updateObj.getPublishScopeJson() != null ? updateObj.getPublishScopeJson() : exist.getPublishScopeJson());
        merged.setSendType(updateObj.getSendType() != null ? updateObj.getSendType() : exist.getSendType());
        merged.setScheduleType(updateObj.getScheduleType() != null ? updateObj.getScheduleType() : exist.getScheduleType());
        merged.setScheduleDayOfWeek(updateObj.getScheduleDayOfWeek() != null ? updateObj.getScheduleDayOfWeek() : exist.getScheduleDayOfWeek());
        merged.setScheduleDayOfMonth(updateObj.getScheduleDayOfMonth() != null ? updateObj.getScheduleDayOfMonth() : exist.getScheduleDayOfMonth());
        merged.setScheduleTime(updateObj.getScheduleTime() != null ? updateObj.getScheduleTime() : exist.getScheduleTime());
        merged.setDeadlineHours(updateObj.getDeadlineHours() != null ? updateObj.getDeadlineHours() : exist.getDeadlineHours());
        merged.setSendAt(updateObj.getSendAt() != null ? updateObj.getSendAt() : exist.getSendAt());
        merged.setDeadlineAt(updateObj.getDeadlineAt() != null ? updateObj.getDeadlineAt() : exist.getDeadlineAt());
        return merged;
    }

    private QuestionnairePublishDO mergeForRuntime(QuestionnairePublishDO updateObj, QuestionnairePublishDO exist) {
        QuestionnairePublishDO merged = new QuestionnairePublishDO();
        merged.setId(exist.getId());
        merged.setQuestionnaireId(updateObj.getQuestionnaireId() != null ? updateObj.getQuestionnaireId() : exist.getQuestionnaireId());
        merged.setPublishScopeJson(updateObj.getPublishScopeJson() != null ? updateObj.getPublishScopeJson() : exist.getPublishScopeJson());
        merged.setSendType(updateObj.getSendType() != null ? updateObj.getSendType() : exist.getSendType());
        merged.setScheduleType(updateObj.getScheduleType() != null ? updateObj.getScheduleType() : exist.getScheduleType());
        merged.setScheduleDayOfWeek(updateObj.getScheduleDayOfWeek() != null ? updateObj.getScheduleDayOfWeek() : exist.getScheduleDayOfWeek());
        merged.setScheduleDayOfMonth(updateObj.getScheduleDayOfMonth() != null ? updateObj.getScheduleDayOfMonth() : exist.getScheduleDayOfMonth());
        merged.setScheduleTime(updateObj.getScheduleTime() != null ? updateObj.getScheduleTime() : exist.getScheduleTime());
        merged.setDeadlineHours(updateObj.getDeadlineHours() != null ? updateObj.getDeadlineHours() : exist.getDeadlineHours());
        merged.setSendAt(updateObj.getSendAt() != null ? updateObj.getSendAt() : exist.getSendAt());
        merged.setDeadlineAt(updateObj.getDeadlineAt() != null ? updateObj.getDeadlineAt() : exist.getDeadlineAt());
        merged.setRemindRuleJson(updateObj.getRemindRuleJson() != null ? updateObj.getRemindRuleJson() : exist.getRemindRuleJson());
        merged.setStatus(updateObj.getStatus() != null ? updateObj.getStatus() : exist.getStatus());
        merged.setCurrentBatchNo(exist.getCurrentBatchNo());
        merged.setCurrentBatchLabel(exist.getCurrentBatchLabel());
        merged.setCurrentBatchStartAt(exist.getCurrentBatchStartAt());
        merged.setCurrentBatchEndAt(exist.getCurrentBatchEndAt());
        merged.setGeneratedCount(exist.getGeneratedCount());
        merged.setLastPublishTime(exist.getLastPublishTime());
        merged.setNextPublishTime(exist.getNextPublishTime());
        return merged;
    }

    private void clearInactiveScheduleFields(QuestionnairePublishDO publish) {
        if (publish == null || publish.getId() == null) {
            return;
        }
        Integer sendType = publish.getSendType() != null ? publish.getSendType() : 0;
        Integer scheduleType = publish.getScheduleType() != null ? publish.getScheduleType() : 0;
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<QuestionnairePublishDO> wrapper =
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<QuestionnairePublishDO>()
                        .eq(QuestionnairePublishDO::getId, publish.getId());
        if (sendType == 0) {
            wrapper.set(QuestionnairePublishDO::getSendAt, null)
                    .set(QuestionnairePublishDO::getDeadlineHours, null)
                    .set(QuestionnairePublishDO::getNextPublishTime, null);
        } else if (scheduleType == 0) {
            wrapper.set(QuestionnairePublishDO::getDeadlineHours, null)
                    .set(QuestionnairePublishDO::getNextPublishTime, null);
        } else {
            wrapper.set(QuestionnairePublishDO::getSendAt, null)
                    .set(QuestionnairePublishDO::getDeadlineAt, null);
        }
        publishMapper.update(null, wrapper);
    }

    private void rebuildCurrentBatchAssignments(QuestionnairePublishDO publish) {
        if (publish == null || publish.getId() == null || publish.getQuestionnaireId() == null) {
            return;
        }
        QuestionnaireDO questionnaire = questionnaireMapper.selectById(publish.getQuestionnaireId());
        if (questionnaire == null) {
            questionnaire = TenantUtils.executeIgnore(() -> questionnaireMapper.selectById(publish.getQuestionnaireId()));
        }
        if (questionnaire == null) {
            return;
        }

        Integer batchNo = publish.getCurrentBatchNo();
        if (batchNo == null || batchNo <= 0) {
            BatchContext nextBatch = buildNextBatchContext(publish, LocalDateTime.now());
            applyCurrentBatch(publish, nextBatch);
            publishMapper.updateById(buildBatchUpdateDO(publish));
            createAssignments(publish, questionnaire, nextBatch);
            return;
        }

        List<QuestionnaireAssignmentDO> currentAssignments =
                selectAssignmentsByPublishIdAndBatchNo(publish.getId(), batchNo, shouldReadAssignmentsCrossTenant(publish.getId()));
        if (currentAssignments != null && !currentAssignments.isEmpty()) {
            List<Long> assignmentIds = currentAssignments.stream()
                    .filter(Objects::nonNull)
                    .map(QuestionnaireAssignmentDO::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (!assignmentIds.isEmpty()) {
                TenantUtils.executeIgnore(() -> {
                    answerMapper.deleteByAssignmentIds(assignmentIds);
                    return null;
                });
            }
            TenantUtils.executeIgnore(() -> {
                assignmentMapper.deleteByPublishIdAndBatchNo(publish.getId(), batchNo);
                return null;
            });
        }

        BatchContext currentBatch = new BatchContext();
        currentBatch.setBatchNo(batchNo);
        currentBatch.setBatchLabel(publish.getCurrentBatchLabel() != null
                ? publish.getCurrentBatchLabel() : "第" + batchNo + "期");
        LocalDateTime batchStartAt = publish.getCurrentBatchStartAt() != null
                ? publish.getCurrentBatchStartAt() : LocalDateTime.now();
        currentBatch.setBatchStartAt(batchStartAt);
        LocalDateTime batchEndAt = publish.getCurrentBatchEndAt();
        if (batchEndAt == null && publish.getDeadlineHours() != null && publish.getDeadlineHours() > 0) {
            batchEndAt = batchStartAt.plusHours(publish.getDeadlineHours());
        }
        if (batchEndAt == null) {
            batchEndAt = publish.getDeadlineAt();
        }
        currentBatch.setBatchEndAt(batchEndAt);

        publish.setCurrentBatchNo(currentBatch.getBatchNo());
        publish.setCurrentBatchLabel(currentBatch.getBatchLabel());
        publish.setCurrentBatchStartAt(currentBatch.getBatchStartAt());
        publish.setCurrentBatchEndAt(currentBatch.getBatchEndAt());
        publish.setDeadlineAt(currentBatch.getBatchEndAt());
        publishMapper.updateById(buildBatchUpdateDO(publish));
        createAssignments(publish, questionnaire, currentBatch);
    }

    private void sendPublishNotice(QuestionnairePublishDO publish, QuestionnaireDO questionnaire) {
        sendAnnouncement(publish, questionnaire, "问卷通知：", "。请按时完成。", false,
                BUSINESS_TYPE_QUESTIONNAIRE_PUBLISH);
    }

    private void sendReminderNotice(QuestionnairePublishDO publish, QuestionnaireDO questionnaire) {
        sendAnnouncement(publish, questionnaire, "问卷提醒：", "。请尽快完成。", true,
                BUSINESS_TYPE_QUESTIONNAIRE_REMINDER);
    }

    private void sendAnnouncement(QuestionnairePublishDO publish, QuestionnaireDO questionnaire,
                                  String titlePrefix, String tailText, boolean pendingOnly, String businessType) {
        if (questionnaire == null) {
            return;
        }
        String title = titlePrefix + questionnaire.getName();
        StringBuilder content = new StringBuilder("问卷：").append(questionnaire.getName());
        if (publish.getDeadlineAt() != null) {
            content.append("，截止：").append(publish.getDeadlineAt());
        }
        content.append(tailText);
        String noticeKey = noticeRecordService.buildNoticeKey(HrNoticeRecordService.CHANNEL_IN_APP,
                businessType, publish.getId(), null);
        String remark = "publishId=" + publish.getId();
        try {
            NoticeCreateReqDTO reqDTO = new NoticeCreateReqDTO();
            reqDTO.setTitle(title);
            reqDTO.setType(NoticeTypeEnum.ANNOUNCEMENT.getType());
            reqDTO.setContent(content.toString());
            reqDTO.setStatus(CommonStatusEnum.ENABLE.getStatus());
            noticeApi.createNotice(reqDTO);
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("title", reqDTO.getTitle());
            payload.put("content", reqDTO.getContent());
            pushQuestionnaireNoticeToEvaluators(publish.getId(), payload, pendingOnly);
            sendStationNotifyToEvaluators(publish, questionnaire, pendingOnly, resolveNotifyTemplateCode(businessType));
            recordNoticeSuccess(noticeKey, businessType, publish.getId(), title, content.toString(), remark);
        } catch (Exception ex) {
            recordNoticeFailure(noticeKey, businessType, publish.getId(), title, content.toString(),
                    ex.getMessage(), remark);
            log.warn("sendPublishNotice failed, questionnaireId={}", publish.getQuestionnaireId(), ex);
        }
    }

    private String resolveNotifyTemplateCode(String businessType) {
        return BUSINESS_TYPE_QUESTIONNAIRE_REMINDER.equals(businessType)
                ? NOTIFY_TEMPLATE_QUESTIONNAIRE_REMINDER
                : NOTIFY_TEMPLATE_QUESTIONNAIRE_PUBLISH;
    }

    private void sendStationNotifyToEvaluators(QuestionnairePublishDO publish, QuestionnaireDO questionnaire,
                                               boolean pendingOnly, String templateCode) {
        if (publish == null || publish.getId() == null || questionnaire == null || templateCode == null) {
            return;
        }
        List<QuestionnaireAssignmentDO> assignments = pendingOnly
                ? assignmentMapper.selectPendingListByPublishId(publish.getId())
                : assignmentMapper.selectListByPublishId(publish.getId());
        if (assignments == null || assignments.isEmpty()) {
            return;
        }
        Set<Long> evaluatorIds = assignments.stream()
                .map(QuestionnaireAssignmentDO::getEvaluatorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        String deadlineText = publish.getDeadlineAt() == null
                ? ""
                : publish.getDeadlineAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String batchLabel = publish.getCurrentBatchLabel() == null ? "" : publish.getCurrentBatchLabel();
        for (Long evaluatorId : evaluatorIds) {
            try {
                NotifySendSingleToUserReqDTO reqDTO = new NotifySendSingleToUserReqDTO();
                reqDTO.setUserId(evaluatorId);
                reqDTO.setTemplateCode(templateCode);
                java.util.Map<String, Object> params = new java.util.HashMap<>();
                params.put("questionnaireName", questionnaire.getName());
                params.put("batchLabel", batchLabel);
                params.put("deadlineAt", deadlineText);
                params.put("deadlineText", deadlineText);
                params.put("detailUrl", QUESTIONNAIRE_SELF_ROUTE);
                reqDTO.setTemplateParams(params);
                CommonResult<Long> result = notifyMessageSendApi.sendSingleMessageToAdmin(reqDTO);
                if (result != null && result.isError()) {
                    log.warn("send questionnaire station notify failed, publishId={}, evaluatorId={}, code={}, msg={}",
                            publish.getId(), evaluatorId, result.getCode(), result.getMsg());
                }
            } catch (Exception ex) {
                log.warn("send questionnaire station notify failed, publishId={}, evaluatorId={}, templateCode={}, reason={}",
                        publish.getId(), evaluatorId, templateCode, ex.getMessage());
            }
        }
    }

    private void pushQuestionnaireNoticeToEvaluators(Long publishId, java.util.Map<String, Object> payload, boolean pendingOnly) {
        if (publishId == null) {
            return;
        }
        List<QuestionnaireAssignmentDO> assignments = pendingOnly
                ? assignmentMapper.selectPendingListByPublishId(publishId)
                : assignmentMapper.selectListByPublishId(publishId);
        if (assignments == null || assignments.isEmpty()) {
            log.warn("pushQuestionnaireNoticeToEvaluators skipped, publishId={}, pendingOnly={}, no assignments",
                    publishId, pendingOnly);
            return;
        }
        assignments.stream()
                .map(QuestionnaireAssignmentDO::getEvaluatorId)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(userId -> webSocketSenderApi.sendObject(UserTypeEnum.ADMIN.getValue(), userId, "notice-push", payload));
    }

    private void recordNoticeSuccess(String noticeKey, String businessType, Long businessId,
                                     String title, String content, String remark) {
        try {
            noticeRecordService.recordSuccess(noticeKey, HrNoticeRecordService.CHANNEL_IN_APP,
                    businessType, businessId, null, title, content, remark);
        } catch (Exception ex) {
            log.warn("Persist questionnaire notice success failed, businessType={}, businessId={}, reason={}",
                    businessType, businessId, ex.getMessage());
        }
    }

    private void recordNoticeFailure(String noticeKey, String businessType, Long businessId,
                                     String title, String content, String errorMessage, String remark) {
        try {
            noticeRecordService.recordFailure(noticeKey, HrNoticeRecordService.CHANNEL_IN_APP,
                    businessType, businessId, null, title, content, errorMessage, remark);
        } catch (Exception ex) {
            log.warn("Persist questionnaire notice failure failed, businessType={}, businessId={}, reason={}",
                    businessType, businessId, ex.getMessage());
        }
    }

    private void normalizeEmptyJsonFields(QuestionnairePublishDO publish) {
        if (publish.getRemindRuleJson() != null && publish.getRemindRuleJson().trim().isEmpty()) {
            publish.setRemindRuleJson(null);
        }
        if (publish.getPublishScopeJson() != null && publish.getPublishScopeJson().trim().isEmpty()) {
            publish.setPublishScopeJson(null);
        }
    }

    private void applyCreateDefaults(QuestionnairePublishDO publish) {
        if (publish == null) {
            return;
        }
        if (publish.getSendType() == null) {
            publish.setSendType(0);
        }
        if (publish.getScheduleType() == null) {
            publish.setScheduleType(0);
        }
        if (publish.getScheduleTime() == null || publish.getScheduleTime().trim().isEmpty()) {
            publish.setScheduleTime("09:00");
        }
        if (publish.getScheduleDayOfWeek() == null) {
            publish.setScheduleDayOfWeek(1);
        }
        if (publish.getScheduleDayOfMonth() == null) {
            publish.setScheduleDayOfMonth(1);
        }
        if (isRecurringSchedule(publish) && (publish.getDeadlineHours() == null || publish.getDeadlineHours() <= 0)) {
            publish.setDeadlineHours(168);
        }
        if (publish.getSendType() == 0 && publish.getDeadlineAt() == null) {
            publish.setDeadlineAt(LocalDateTime.now().plusDays(7));
        }
        if (publish.getStatus() == null) {
            publish.setStatus(publish.getSendType() == 0 || isRecurringSchedule(publish) ? 1 : 0);
        }
    }

    private boolean isRecurringSchedule(QuestionnairePublishDO publish) {
        return publish != null
                && publish.getSendType() != null && publish.getSendType() == 1
                && publish.getScheduleType() != null && publish.getScheduleType() > 0;
    }

    private boolean isPublishNoticeEnabled(QuestionnairePublishDO publish) {
        RemindRule remindRule = parseRemindRule(publish != null ? publish.getRemindRuleJson() : null);
        return remindRule.getSendNotice() == null || remindRule.getSendNotice();
    }

    private boolean containsNoticeChannel(RemindRule remindRule) {
        if (remindRule == null || remindRule.getChannels() == null || remindRule.getChannels().isEmpty()) {
            return true;
        }
        return remindRule.getChannels().contains("notice");
    }

    private boolean isRemindTimeReached(RemindRule remindRule, LocalDateTime now) {
        if (remindRule == null || now == null) {
            return false;
        }
        LocalTime remindTime = LocalTime.of(9, 0);
        if (remindRule.getTime() != null && !remindRule.getTime().trim().isEmpty()) {
            try {
                remindTime = LocalTime.parse(remindRule.getTime().trim(), DateTimeFormatter.ofPattern("HH:mm"));
            } catch (DateTimeParseException ex) {
                log.warn("invalid remind time: {}", remindRule.getTime());
            }
        }
        return !now.toLocalTime().isBefore(remindTime);
    }

    private RemindRule parseRemindRule(String remindRuleJson) {
        RemindRule remindRule = new RemindRule();
        remindRule.setSendNotice(true);
        remindRule.setEnabled(false);
        remindRule.setTime("09:00");
        remindRule.setChannels(new ArrayList<>());
        remindRule.getChannels().add("notice");

        if (remindRuleJson == null || remindRuleJson.trim().isEmpty()) {
            return remindRule;
        }
        try {
            RemindRule parsed = objectMapper.readValue(remindRuleJson, RemindRule.class);
            if (parsed.getSendNotice() != null) {
                remindRule.setSendNotice(parsed.getSendNotice());
            }
            if (parsed.getTime() != null && !parsed.getTime().trim().isEmpty()) {
                remindRule.setTime(parsed.getTime().trim());
            }
            if (parsed.getChannels() != null && !parsed.getChannels().isEmpty()) {
                remindRule.setChannels(parsed.getChannels());
            }
            boolean legacyEnabled = parsed.getTime() != null
                    || (parsed.getChannels() != null && !parsed.getChannels().isEmpty());
            remindRule.setEnabled(parsed.getEnabled() != null ? parsed.getEnabled() : legacyEnabled);
        } catch (Exception ex) {
            log.warn("parse remind rule failed: {}", remindRuleJson);
        }
        return remindRule;
    }

    /**
     * 根据发布范围创建分配记录
     */
    private void createAssignments(QuestionnairePublishDO publish, QuestionnaireDO questionnaire, BatchContext batchContext) {
        if (questionnaire == null || publish.getPublishScopeJson() == null) {
            return;
        }
        try {
            PublishScope publishScope = parseScopeOrThrow(publish.getPublishScopeJson());
            Set<Long> evaluatorIds = resolveUserIdsByScope(publishScope);
            if (evaluatorIds == null || evaluatorIds.isEmpty()) {
                log.warn("createAssignments skipped, evaluatorIds empty, questionnaireId={}", questionnaire.getId());
                return;
            }

            removeExcludedUsers(evaluatorIds, publishScope);
            if (evaluatorIds.isEmpty()) {
                log.warn("createAssignments skipped after exclusions, questionnaireId={}", questionnaire.getId());
                return;
            }

            java.util.Map<Long, AdminUserRespDTO> evaluatorUserMap = loadUserMap(evaluatorIds);
            alignPublishTenantWithEvaluators(publish, evaluatorUserMap);
            boolean peerQuestionnaire = isPeerQuestionnaire(questionnaire);
            java.util.Map<Long, Set<Long>> evaluatorTargetsMap = resolveEvaluatorTargetsMap(
                    publishScope, questionnaire, evaluatorIds, evaluatorUserMap);
            applyPeerEvaluationRules(evaluatorTargetsMap, publishScope);

            removeExcludedUsers(evaluatorTargetsMap.keySet(), publishScope);

            if (evaluatorTargetsMap.isEmpty()) {
                log.warn("createAssignments skipped, evaluatorTargets empty, questionnaireId={}", questionnaire.getId());
                return;
            }

            if (peerQuestionnaire && Boolean.TRUE.equals(publishScope.getExcludeTargetsFromEvaluators())) {
                Set<Long> targetUnionIds = evaluatorTargetsMap.values().stream()
                        .filter(Objects::nonNull)
                        .flatMap(Set::stream)
                        .collect(Collectors.toSet());
                evaluatorTargetsMap.keySet().removeIf(targetUnionIds::contains);
            }
            if (evaluatorTargetsMap.isEmpty()) {
                log.warn("createAssignments skipped, evaluatorTargets empty after exclusion, questionnaireId={}", questionnaire.getId());
                return;
            }

            Set<Long> allUserIds = new HashSet<>(evaluatorTargetsMap.keySet());
            evaluatorTargetsMap.values().forEach(targetIds -> {
                if (targetIds != null) {
                    allUserIds.addAll(targetIds);
                }
            });
            java.util.Map<Long, AdminUserRespDTO> userMap = loadUserMap(allUserIds);

            Integer batchNo = batchContext != null ? batchContext.getBatchNo() : null;
            int createdCount = 0;
            if (!peerQuestionnaire) {
                for (Long evaluatorId : evaluatorTargetsMap.keySet()) {
                    AdminUserRespDTO evaluator = userMap.get(evaluatorId);
                    if (evaluator == null) {
                        continue;
                    }
                    boolean exists = TenantUtils.executeIgnore(
                            () -> assignmentMapper.existsByPublishIdAndPair(publish.getId(), evaluatorId, null, batchNo));
                    if (exists) {
                        continue;
                    }
                    QuestionnaireAssignmentDO assignment = buildAssignmentDO(
                            publish, questionnaire, batchContext, evaluator, evaluatorId, null, null,
                            resolveWriteTenantId(publish, questionnaire, evaluator));
                    TenantUtils.executeIgnore(() -> {
                        assignmentMapper.insert(assignment);
                        return null;
                    });
                    createdCount++;
                }
                log.info("createAssignments success, questionnaireId={}, batchNo={}, evaluatorCount={}, targetCount={}, createdCount={}, relationMode={}",
                        questionnaire.getId(), batchNo, evaluatorTargetsMap.size(), 0, createdCount,
                        normalizeRelationMode(publishScope.getRelationMode()));
                refreshGeneratedTodoTasks(createdCount, questionnaire.getId(), batchNo);
                return;
            }
            for (java.util.Map.Entry<Long, Set<Long>> pair : evaluatorTargetsMap.entrySet()) {
                Long evaluatorId = pair.getKey();
                Set<Long> targetIds = pair.getValue();
                if (targetIds == null || targetIds.isEmpty()) {
                    continue;
                }
                AdminUserRespDTO evaluator = userMap.get(evaluatorId);
                if (evaluator == null) {
                    continue;
                }
                for (Long targetId : targetIds) {
                    AdminUserRespDTO target = userMap.get(targetId);
                    if (target == null) {
                        continue;
                    }
                    if (Boolean.TRUE.equals(publishScope.getExcludeSelf())
                            && Objects.equals(evaluatorId, targetId)) {
                        continue;
                    }
                    if (Boolean.TRUE.equals(publishScope.getSameDeptOnly())
                            && !isSameDept(evaluator, target)) {
                        continue;
                    }
                    boolean exists = TenantUtils.executeIgnore(
                            () -> assignmentMapper.existsByPublishIdAndPair(publish.getId(), evaluatorId, targetId, batchNo));
                    if (exists) {
                        continue;
                    }
                    QuestionnaireAssignmentDO assignment = buildAssignmentDO(
                            publish, questionnaire, batchContext, evaluator, evaluatorId, target, targetId,
                            resolveWriteTenantId(publish, questionnaire, evaluator));
                    TenantUtils.executeIgnore(() -> {
                        assignmentMapper.insert(assignment);
                        return null;
                    });
                    createdCount++;
                }
            }

            int targetCount = evaluatorTargetsMap.values().stream()
                    .filter(Objects::nonNull)
                    .mapToInt(Set::size)
                    .sum();
            log.info("createAssignments success, questionnaireId={}, batchNo={}, evaluatorCount={}, targetCount={}, createdCount={}, relationMode={}",
                    questionnaire.getId(), batchNo, evaluatorTargetsMap.size(), targetCount, createdCount,
                    normalizeRelationMode(publishScope.getRelationMode()));
            refreshGeneratedTodoTasks(createdCount, questionnaire.getId(), batchNo);
        } catch (Exception ex) {
            log.error("createAssignments failed, questionnaireId={}", publish.getQuestionnaireId(), ex);
        }
    }

    private void refreshGeneratedTodoTasks(int createdCount, Long questionnaireId, Integer batchNo) {
        if (createdCount <= 0) {
            return;
        }
        try {
            hrTodoTaskService.refreshGeneratedTasks();
        } catch (Exception ex) {
            log.warn("refresh questionnaire todo tasks failed, questionnaireId={}, batchNo={}, reason={}",
                    questionnaireId, batchNo, ex.getMessage());
        }
    }

    private void applyPeerEvaluationRules(java.util.Map<Long, Set<Long>> evaluatorTargetsMap, PublishScope publishScope) {
        if (evaluatorTargetsMap == null || evaluatorTargetsMap.isEmpty()
                || publishScope == null || publishScope.getPeerEvaluationRules() == null
                || publishScope.getPeerEvaluationRules().isEmpty()) {
            return;
        }
        java.util.Iterator<java.util.Map.Entry<Long, Set<Long>>> iterator = evaluatorTargetsMap.entrySet().iterator();
        while (iterator.hasNext()) {
            java.util.Map.Entry<Long, Set<Long>> entry = iterator.next();
            Set<Long> targetIds = applyPeerEvaluationRules(entry.getKey(), entry.getValue(), publishScope);
            if (targetIds.isEmpty()) {
                iterator.remove();
            } else {
                entry.setValue(targetIds);
            }
        }
    }

    private Set<Long> applyPeerEvaluationRules(Long evaluatorId, Set<Long> targetIds, PublishScope publishScope) {
        if (targetIds == null || targetIds.isEmpty()) {
            return new HashSet<>();
        }
        PeerEvaluationRule rule = findPeerEvaluationRule(publishScope, evaluatorId);
        if (rule == null) {
            return new HashSet<>(targetIds);
        }
        Set<Long> result = new HashSet<>(targetIds);
        Set<Long> includeTargetIds = normalizeUserIdSet(rule.getIncludeTargetIds());
        if (!includeTargetIds.isEmpty()) {
            result.retainAll(includeTargetIds);
        }
        Set<Long> excludeTargetIds = normalizeUserIdSet(rule.getExcludeTargetIds());
        if (!excludeTargetIds.isEmpty()) {
            result.removeAll(excludeTargetIds);
        }
        return result;
    }

    private boolean isPeerAssignmentIgnored(PublishScope publishScope, Long evaluatorId, Long targetId) {
        if (publishScope == null || evaluatorId == null || targetId == null) {
            return false;
        }
        PeerEvaluationRule rule = findPeerEvaluationRule(publishScope, evaluatorId);
        if (rule == null) {
            return false;
        }
        Set<Long> includeTargetIds = normalizeUserIdSet(rule.getIncludeTargetIds());
        if (!includeTargetIds.isEmpty() && !includeTargetIds.contains(targetId)) {
            return true;
        }
        Set<Long> excludeTargetIds = normalizeUserIdSet(rule.getExcludeTargetIds());
        return excludeTargetIds.contains(targetId);
    }

    private PeerEvaluationRule findPeerEvaluationRule(PublishScope publishScope, Long evaluatorId) {
        if (publishScope == null || evaluatorId == null || publishScope.getPeerEvaluationRules() == null) {
            return null;
        }
        Set<Long> includeTargetIds = new HashSet<>();
        Set<Long> excludeTargetIds = new HashSet<>();
        boolean matched = false;
        for (PeerEvaluationRule rule : publishScope.getPeerEvaluationRules()) {
            if (rule == null || !Objects.equals(rule.getEvaluatorId(), evaluatorId)) {
                continue;
            }
            matched = true;
            includeTargetIds.addAll(normalizeUserIdSet(rule.getIncludeTargetIds()));
            excludeTargetIds.addAll(normalizeUserIdSet(rule.getExcludeTargetIds()));
        }
        if (!matched) {
            return null;
        }
        PeerEvaluationRule merged = new PeerEvaluationRule();
        merged.setEvaluatorId(evaluatorId);
        merged.setIncludeTargetIds(new ArrayList<>(includeTargetIds));
        merged.setExcludeTargetIds(new ArrayList<>(excludeTargetIds));
        return merged;
    }

    private void applyCurrentBatchPeerEvaluationRules(QuestionnairePublishDO publish) {
        if (publish == null || publish.getId() == null || publish.getPublishScopeJson() == null) {
            return;
        }
        QuestionnaireDO questionnaire = questionnaireMapper.selectById(publish.getQuestionnaireId());
        if (questionnaire == null) {
            questionnaire = TenantUtils.executeIgnore(() -> questionnaireMapper.selectById(publish.getQuestionnaireId()));
        }
        boolean peerQuestionnaire = isPeerQuestionnaire(questionnaire);
        PublishScope publishScope;
        try {
            publishScope = parseScopeOrThrow(publish.getPublishScopeJson());
        } catch (Exception ex) {
            log.warn("apply questionnaire eligibility rules skipped, invalid scope, publishId={}", publish.getId(), ex);
            return;
        }
        Set<Long> excludedEvaluatorIds = normalizeUserIdSet(publishScope.getExcludeUserIds());
        if (!peerQuestionnaire && excludedEvaluatorIds.isEmpty()) {
            return;
        }
        Integer batchNo = publish.getCurrentBatchNo();
        if (batchNo == null || batchNo <= 0) {
            batchNo = selectLatestBatchNoByPublishId(publish.getId(), shouldReadAssignmentsCrossTenant(publish.getId()));
        }
        if (batchNo == null || batchNo <= 0) {
            return;
        }
        final Integer queryBatchNo = batchNo;
        List<QuestionnaireAssignmentDO> assignments = selectAssignmentsByPublishIdAndBatchNo(
                publish.getId(), queryBatchNo, shouldReadAssignmentsCrossTenant(publish.getId()));
        if (assignments == null || assignments.isEmpty()) {
            return;
        }
        int changedCount = 0;
        for (QuestionnaireAssignmentDO assignment : assignments) {
            if (assignment == null || assignment.getId() == null
                    || Objects.equals(assignment.getStatus(), ASSIGNMENT_STATUS_SUBMITTED)) {
                continue;
            }
            boolean ignored = excludedEvaluatorIds.contains(assignment.getEvaluatorId())
                    || (peerQuestionnaire
                            && isPeerAssignmentIgnored(publishScope, assignment.getEvaluatorId(), assignment.getTargetId()));
            Integer nextStatus = ignored ? ASSIGNMENT_STATUS_NOT_REQUIRED : ASSIGNMENT_STATUS_PENDING;
            if (Objects.equals(assignment.getStatus(), nextStatus)) {
                continue;
            }
            QuestionnaireAssignmentDO updateAssignment = new QuestionnaireAssignmentDO();
            updateAssignment.setId(assignment.getId());
            updateAssignment.setStatus(nextStatus);
            TenantUtils.executeIgnore(() -> {
                assignmentMapper.updateById(updateAssignment);
                return null;
            });
            changedCount++;
        }
        refreshGeneratedTodoTasks(changedCount, publish.getQuestionnaireId(), batchNo);
        log.info("apply peer evaluation rules success, publishId={}, batchNo={}, changedCount={}",
                publish.getId(), batchNo, changedCount);
    }

    private java.util.Map<Long, Set<Long>> resolveEvaluatorTargetsMap(PublishScope publishScope,
                                                                       QuestionnaireDO questionnaire,
                                                                       Set<Long> evaluatorIds,
                                                                       java.util.Map<Long, AdminUserRespDTO> evaluatorUserMap) {
        java.util.Map<Long, Set<Long>> result = new LinkedHashMap<>();
        if (evaluatorIds == null || evaluatorIds.isEmpty()) {
            return result;
        }
        if (!isPeerQuestionnaire(questionnaire)) {
            for (Long evaluatorId : evaluatorIds) {
                result.put(evaluatorId, java.util.Collections.emptySet());
            }
            return result;
        }

        String relationMode = normalizeRelationMode(publishScope != null ? publishScope.getRelationMode() : null);
        java.util.Map<Long, Set<Long>> relationModeTargets = resolveRelationTargetsByMode(
                relationMode, publishScope, evaluatorIds, evaluatorUserMap);
        if (!"custom".equals(relationMode)) {
            return relationModeTargets;
        }

        Set<Long> targetIds = resolveTargetUserIds(publishScope, questionnaire, evaluatorIds);
        if (targetIds == null || targetIds.isEmpty()) {
            return result;
        }
        for (Long evaluatorId : evaluatorIds) {
            result.put(evaluatorId, new HashSet<>(targetIds));
        }
        return result;
    }

    private java.util.Map<Long, Set<Long>> resolveRelationTargetsByMode(String relationMode,
                                                                         PublishScope publishScope,
                                                                         Set<Long> evaluatorIds,
                                                                         java.util.Map<Long, AdminUserRespDTO> evaluatorUserMap) {
        java.util.Map<Long, Set<Long>> result = new LinkedHashMap<>();
        if ("custom".equals(relationMode)) {
            return result;
        }

        Set<Long> targetLimitIds = resolveTargetLimitIds(publishScope, evaluatorIds);
        if ("same_level".equals(relationMode)) {
            java.util.Map<Long, Set<Long>> deptUsers = new LinkedHashMap<>();
            for (Long evaluatorId : evaluatorIds) {
                AdminUserRespDTO evaluator = evaluatorUserMap.get(evaluatorId);
                if (evaluator == null || evaluator.getDeptId() == null) {
                    continue;
                }
                deptUsers.computeIfAbsent(evaluator.getDeptId(), key -> new HashSet<>()).add(evaluatorId);
            }
            for (Long evaluatorId : evaluatorIds) {
                AdminUserRespDTO evaluator = evaluatorUserMap.get(evaluatorId);
                if (evaluator == null || evaluator.getDeptId() == null) {
                    continue;
                }
                Set<Long> targetIds = new HashSet<>(deptUsers.getOrDefault(evaluator.getDeptId(), new HashSet<>()));
                if (targetLimitIds != null) {
                    targetIds.retainAll(targetLimitIds);
                }
                if (!targetIds.isEmpty()) {
                    result.put(evaluatorId, targetIds);
                }
            }
            return result;
        }

        if ("dept_staff_to_leader".equals(relationMode)) {
            java.util.Map<Long, Long> deptLeaderCache = new LinkedHashMap<>();
            for (Long evaluatorId : evaluatorIds) {
                AdminUserRespDTO evaluator = evaluatorUserMap.get(evaluatorId);
                if (evaluator == null || evaluator.getDeptId() == null) {
                    continue;
                }
                Long leaderUserId = deptLeaderCache.get(evaluator.getDeptId());
                if (!deptLeaderCache.containsKey(evaluator.getDeptId())) {
                    leaderUserId = queryDeptLeaderUserId(evaluator.getDeptId());
                    deptLeaderCache.put(evaluator.getDeptId(), leaderUserId);
                }
                if (leaderUserId == null) {
                    continue;
                }
                if (targetLimitIds != null && !targetLimitIds.contains(leaderUserId)) {
                    continue;
                }
                result.put(evaluatorId, new HashSet<>(java.util.Collections.singleton(leaderUserId)));
            }
            return result;
        }

        if ("manager_to_staff".equals(relationMode)) {
            for (Long managerId : evaluatorIds) {
                Set<Long> subordinateIds = querySubordinateUserIds(managerId);
                if (subordinateIds.isEmpty()) {
                    continue;
                }
                if (targetLimitIds != null && !targetLimitIds.contains(managerId)) {
                    continue;
                }
                for (Long subordinateId : subordinateIds) {
                    if (subordinateId == null) {
                        continue;
                    }
                    result.computeIfAbsent(subordinateId, key -> new HashSet<>()).add(managerId);
                }
            }
            return result;
        }

        return result;
    }

    private Set<Long> resolveTargetLimitIds(PublishScope publishScope, Set<Long> evaluatorIds) {
        if (!hasPublishTargetScope(publishScope)) {
            return null;
        }
        TargetScope targetScope = publishScope.getTargetScope();
        if (targetScope == null || targetScope.getScope() == null) {
            return null;
        }
        String scope = targetScope.getScope().trim();
        if ("all".equals(scope)) {
            return null;
        }
        return resolveTargetUserIdsByTargetScope(targetScope, evaluatorIds);
    }

    private Long queryDeptLeaderUserId(Long deptId) {
        if (deptId == null) {
            return null;
        }
        try {
            return TenantUtils.executeIgnore(() -> {
                DeptRespDTO dept = deptApi.getDept(deptId).getCheckedData();
                return dept != null ? dept.getLeaderUserId() : null;
            });
        } catch (Exception ex) {
            log.warn("queryDeptLeaderUserId failed, deptId={}", deptId, ex);
            return null;
        }
    }

    private Set<Long> querySubordinateUserIds(Long evaluatorId) {
        if (evaluatorId == null) {
            return new HashSet<>();
        }
        try {
            List<AdminUserRespDTO> users = TenantUtils.executeIgnore(
                    () -> adminUserApi.getUserListBySubordinate(evaluatorId).getCheckedData());
            if (users == null || users.isEmpty()) {
                return new HashSet<>();
            }
            return users.stream()
                    .map(AdminUserRespDTO::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(HashSet::new));
        } catch (Exception ex) {
            log.warn("querySubordinateUserIds failed, evaluatorId={}", evaluatorId, ex);
            return new HashSet<>();
        }
    }

    private String normalizeRelationMode(String relationMode) {
        if (relationMode == null || relationMode.trim().isEmpty()) {
            return "custom";
        }
        String normalized = relationMode.trim();
        if ("dept_staff_to_leader".equals(normalized)
                || "same_level".equals(normalized)
                || "manager_to_staff".equals(normalized)) {
            return normalized;
        }
        return "custom";
    }

    private QuestionnaireAssignmentDO buildAssignmentDO(QuestionnairePublishDO publish,
                                                        QuestionnaireDO questionnaire,
                                                        BatchContext batchContext,
                                                        AdminUserRespDTO evaluator,
                                                        Long evaluatorId,
                                                        AdminUserRespDTO target,
                                                        Long targetId,
                                                        Long writeTenantId) {
        QuestionnaireAssignmentDO assignment = new QuestionnaireAssignmentDO();
        assignment.setQuestionnaireId(questionnaire.getId());
        assignment.setPublishId(publish.getId());
        assignment.setBatchNo(batchContext != null ? batchContext.getBatchNo() : null);
        assignment.setBatchLabel(batchContext != null ? batchContext.getBatchLabel() : null);
        assignment.setBatchStartAt(batchContext != null ? batchContext.getBatchStartAt() : null);
        assignment.setBatchEndAt(batchContext != null ? batchContext.getBatchEndAt() : null);
        assignment.setTenantId(writeTenantId);
        assignment.setEvaluatorId(evaluatorId);
        assignment.setEvaluatorName(evaluator != null ? evaluator.getNickname() : null);
        assignment.setTargetId(targetId);
        assignment.setTargetName(target != null ? target.getNickname() : null);
        assignment.setStatus(0);
        return assignment;
    }

    private boolean isPeerQuestionnaire(QuestionnaireDO questionnaire) {
        return questionnaire != null && "peer".equalsIgnoreCase(questionnaire.getType());
    }

    private boolean isSameDept(AdminUserRespDTO evaluator, AdminUserRespDTO target) {
        if (evaluator == null || target == null) {
            return false;
        }
        if (evaluator.getDeptId() == null || target.getDeptId() == null) {
            return false;
        }
        return Objects.equals(evaluator.getDeptId(), target.getDeptId());
    }

    private boolean hasConfiguredTargetRule(QuestionnaireDO questionnaire) {
        if (questionnaire == null || questionnaire.getTargetRuleJson() == null) {
            return false;
        }
        String targetRuleJson = questionnaire.getTargetRuleJson().trim();
        return !targetRuleJson.isEmpty() && !"{}".equals(targetRuleJson);
    }

    private boolean hasPublishTargetScope(PublishScope publishScope) {
        if (publishScope == null || publishScope.getTargetScope() == null) {
            return false;
        }
        TargetScope targetScope = publishScope.getTargetScope();
        return targetScope.getScope() != null && !targetScope.getScope().trim().isEmpty();
    }

    private Set<Long> resolveTargetUserIds(PublishScope publishScope,
                                           QuestionnaireDO questionnaire,
                                           Set<Long> fallbackUserIds) {
        if (hasPublishTargetScope(publishScope)) {
            Set<Long> targetIds = resolveTargetUserIdsByTargetScope(publishScope.getTargetScope(), fallbackUserIds);
            if (targetIds != null) {
                return targetIds;
            }
        }
        return new HashSet<>(fallbackUserIds);
    }

    private Set<Long> resolveTargetUserIdsByTargetScope(TargetScope targetScope, Set<Long> fallbackUserIds) {
        if (targetScope == null || targetScope.getScope() == null || targetScope.getScope().trim().isEmpty()) {
            return null;
        }
        String scope = targetScope.getScope().trim();
        if ("dept".equals(scope)) {
            if (!hasAnyId(targetScope.getIds())) {
                return new HashSet<>();
            }
            try {
                PublishScope deptScope = new PublishScope();
                deptScope.setDeptIds(targetScope.getIds());
                return resolveUserIdsByScope(deptScope);
            } catch (Exception ex) {
                log.warn("resolveTargetUserIdsByTargetScope dept failed, ids={}", targetScope.getIds(), ex);
                return new HashSet<>();
            }
        }
        if ("user".equals(scope)) {
            if (!hasAnyId(targetScope.getIds())) {
                return new HashSet<>();
            }
            return targetScope.getIds().stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(HashSet::new));
        }
        if ("all".equals(scope)) {
            return new HashSet<>(fallbackUserIds);
        }
        return null;
    }

    private Set<Long> resolveTargetUserIdsByTemplateRule(QuestionnaireDO questionnaire, Set<Long> fallbackUserIds) {
        try {
            TargetRule targetRule = objectMapper.readValue(questionnaire.getTargetRuleJson(), TargetRule.class);
            if (targetRule == null || targetRule.getScope() == null || targetRule.getScope().trim().isEmpty()) {
                return new HashSet<>(fallbackUserIds);
            }

            String scope = targetRule.getScope().trim();
            if ("dept".equals(scope)) {
                if (!hasAnyId(targetRule.getIds())) {
                    return new HashSet<>();
                }
                PublishScope deptScope = new PublishScope();
                deptScope.setDeptIds(targetRule.getIds());
                return resolveUserIdsByScope(deptScope);
            }
            if ("user".equals(scope)) {
                if (!hasAnyId(targetRule.getIds())) {
                    return new HashSet<>();
                }
                return targetRule.getIds().stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toCollection(HashSet::new));
            }
            return new HashSet<>(fallbackUserIds);
        } catch (Exception ex) {
            log.warn("resolveTargetUserIdsByTemplateRule failed, questionnaireId={}, targetRuleJson={}",
                    questionnaire != null ? questionnaire.getId() : null,
                    questionnaire != null ? questionnaire.getTargetRuleJson() : null,
                    ex);
            return new HashSet<>(fallbackUserIds);
        }
    }

    private java.util.Map<Long, AdminUserRespDTO> loadUserMap(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new LinkedHashMap<>();
        }
        try {
            return TenantUtils.executeIgnore(() -> {
                List<AdminUserRespDTO> users = adminUserApi.getUserList(userIds).getCheckedData();
                java.util.Map<Long, AdminUserRespDTO> userMap = new LinkedHashMap<>();
                if (users != null) {
                    for (AdminUserRespDTO user : users) {
                        if (user != null && user.getId() != null) {
                            userMap.put(user.getId(), user);
                        }
                    }
                }
                return userMap;
            });
        } catch (Exception ex) {
            log.warn("loadUserMap failed, userCount={}", userIds.size(), ex);
            return new LinkedHashMap<>();
        }
    }

    private BatchContext buildNextBatchContext(QuestionnairePublishDO publish, LocalDateTime startAt) {
        int generatedCount = publish != null && publish.getGeneratedCount() != null ? publish.getGeneratedCount() : 0;
        int batchNo = generatedCount + 1;
        LocalDateTime batchStartAt = startAt != null ? startAt : LocalDateTime.now();
        LocalDateTime batchEndAt = null;
        if (publish != null && publish.getDeadlineHours() != null && publish.getDeadlineHours() > 0) {
            batchEndAt = batchStartAt.plusHours(publish.getDeadlineHours());
        } else if (publish != null) {
            batchEndAt = publish.getDeadlineAt();
        }
        BatchContext batchContext = new BatchContext();
        batchContext.setBatchNo(batchNo);
        batchContext.setBatchLabel("第" + batchNo + "期");
        batchContext.setBatchStartAt(batchStartAt);
        batchContext.setBatchEndAt(batchEndAt);
        return batchContext;
    }

    private void applyCurrentBatch(QuestionnairePublishDO publish, BatchContext batchContext) {
        if (publish == null || batchContext == null) {
            return;
        }
        publish.setGeneratedCount(batchContext.getBatchNo());
        publish.setCurrentBatchNo(batchContext.getBatchNo());
        publish.setCurrentBatchLabel(batchContext.getBatchLabel());
        publish.setCurrentBatchStartAt(batchContext.getBatchStartAt());
        publish.setCurrentBatchEndAt(batchContext.getBatchEndAt());
        publish.setDeadlineAt(batchContext.getBatchEndAt());
    }

    private QuestionnairePublishDO buildBatchUpdateDO(QuestionnairePublishDO source) {
        QuestionnairePublishDO updateObj = new QuestionnairePublishDO();
        updateObj.setId(source.getId());
        updateObj.setStatus(source.getStatus());
        updateObj.setLastPublishTime(source.getLastPublishTime());
        updateObj.setNextPublishTime(source.getNextPublishTime());
        updateObj.setDeadlineAt(source.getDeadlineAt());
        updateObj.setGeneratedCount(source.getGeneratedCount());
        updateObj.setCurrentBatchNo(source.getCurrentBatchNo());
        updateObj.setCurrentBatchLabel(source.getCurrentBatchLabel());
        updateObj.setCurrentBatchStartAt(source.getCurrentBatchStartAt());
        updateObj.setCurrentBatchEndAt(source.getCurrentBatchEndAt());
        return updateObj;
    }

    private void fillTenantIdIfNeeded(QuestionnairePublishDO publish) {
        if (publish == null) {
            return;
        }
        if (publish.getTenantId() != null && publish.getTenantId() > 0) {
            return;
        }
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null && tenantId > 0) {
            publish.setTenantId(tenantId);
        }
    }

    private void syncCurrentBatchDeadline(QuestionnairePublishDO publish, LocalDateTime deadlineAt) {
        if (publish == null || publish.getId() == null || deadlineAt == null) {
            return;
        }
        QuestionnairePublishDO updatePublish = new QuestionnairePublishDO();
        updatePublish.setId(publish.getId());
        updatePublish.setDeadlineAt(deadlineAt);
        updatePublish.setCurrentBatchEndAt(deadlineAt);
        publishMapper.updateById(updatePublish);
        assignmentMapper.updateBatchEndAtByPublishId(
                publish.getId(),
                publish.getCurrentBatchNo(),
                deadlineAt
        );
    }

    private Integer resolveAppendBatchNo(QuestionnairePublishDO publish, Integer requestBatchNo) {
        Integer batchNo = requestBatchNo != null && requestBatchNo > 0
                ? requestBatchNo
                : publish != null ? publish.getCurrentBatchNo() : null;
        if ((batchNo == null || batchNo <= 0) && publish != null && publish.getId() != null) {
            batchNo = selectLatestBatchNoByPublishId(publish.getId(), shouldReadAssignmentsCrossTenant(publish.getId()));
        }
        if (batchNo == null || batchNo <= 0) {
            throw ServiceExceptionUtil.invalidParamException("当前发布还没有生成批次，不能追加填写人");
        }
        return batchNo;
    }

    private BatchContext buildExistingBatchContext(QuestionnairePublishDO publish, Integer batchNo) {
        BatchContext batchContext = new BatchContext();
        batchContext.setBatchNo(batchNo);
        batchContext.setBatchLabel("第" + batchNo + "期");

        List<QuestionnaireAssignmentDO> assignments = publish != null && publish.getId() != null
                ? selectAssignmentsByPublishIdAndBatchNo(publish.getId(), batchNo, shouldReadAssignmentsCrossTenant(publish.getId()))
                : new ArrayList<>();
        for (QuestionnaireAssignmentDO assignment : assignments) {
            if (assignment == null) {
                continue;
            }
            if (assignment.getBatchLabel() != null && !assignment.getBatchLabel().trim().isEmpty()) {
                batchContext.setBatchLabel(assignment.getBatchLabel());
            }
            if (assignment.getBatchStartAt() != null
                    && (batchContext.getBatchStartAt() == null
                    || assignment.getBatchStartAt().isBefore(batchContext.getBatchStartAt()))) {
                batchContext.setBatchStartAt(assignment.getBatchStartAt());
            }
            if (assignment.getBatchEndAt() != null
                    && (batchContext.getBatchEndAt() == null
                    || assignment.getBatchEndAt().isAfter(batchContext.getBatchEndAt()))) {
                batchContext.setBatchEndAt(assignment.getBatchEndAt());
            }
        }
        if (publish != null && Objects.equals(batchNo, publish.getCurrentBatchNo())) {
            if (publish.getCurrentBatchLabel() != null && !publish.getCurrentBatchLabel().trim().isEmpty()) {
                batchContext.setBatchLabel(publish.getCurrentBatchLabel());
            }
            if (batchContext.getBatchStartAt() == null) {
                batchContext.setBatchStartAt(publish.getCurrentBatchStartAt());
            }
            if (batchContext.getBatchEndAt() == null) {
                batchContext.setBatchEndAt(publish.getCurrentBatchEndAt() != null
                        ? publish.getCurrentBatchEndAt() : publish.getDeadlineAt());
            }
        }
        if (batchContext.getBatchStartAt() == null) {
            batchContext.setBatchStartAt(LocalDateTime.now());
        }
        return batchContext;
    }

    private Set<Long> normalizeUserIdSet(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new java.util.LinkedHashSet<>();
        }
        return userIds.stream()
                .filter(Objects::nonNull)
                .filter(userId -> userId > 0)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private void sendAppendNoticeToEvaluators(QuestionnairePublishDO publish,
                                              QuestionnaireDO questionnaire,
                                              Set<Long> evaluatorIds) {
        if (publish == null || questionnaire == null || evaluatorIds == null || evaluatorIds.isEmpty()) {
            return;
        }
        String deadlineText = publish.getDeadlineAt() == null
                ? ""
                : publish.getDeadlineAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String batchLabel = publish.getCurrentBatchLabel() == null ? "" : publish.getCurrentBatchLabel();
        java.util.Map<String, Object> socketPayload = new java.util.HashMap<>();
        socketPayload.put("title", "问卷通知：" + questionnaire.getName());
        socketPayload.put("content", "你已被追加到问卷：" + questionnaire.getName());
        for (Long evaluatorId : evaluatorIds) {
            try {
                NotifySendSingleToUserReqDTO reqDTO = new NotifySendSingleToUserReqDTO();
                reqDTO.setUserId(evaluatorId);
                reqDTO.setTemplateCode(NOTIFY_TEMPLATE_QUESTIONNAIRE_PUBLISH);
                java.util.Map<String, Object> params = new java.util.HashMap<>();
                params.put("questionnaireName", questionnaire.getName());
                params.put("batchLabel", batchLabel);
                params.put("deadlineAt", deadlineText);
                params.put("deadlineText", deadlineText);
                params.put("detailUrl", QUESTIONNAIRE_SELF_ROUTE);
                reqDTO.setTemplateParams(params);
                notifyMessageSendApi.sendSingleMessageToAdmin(reqDTO);
                webSocketSenderApi.sendObject(UserTypeEnum.ADMIN.getValue(), evaluatorId, "notice-push", socketPayload);
            } catch (Exception ex) {
                log.warn("send questionnaire append notify failed, publishId={}, evaluatorId={}, reason={}",
                        publish.getId(), evaluatorId, ex.getMessage());
            }
        }
    }


    private void alignPublishTenantWithEvaluators(QuestionnairePublishDO publish,
                                                  java.util.Map<Long, AdminUserRespDTO> evaluatorUserMap) {
        if (publish == null || publish.getId() == null || evaluatorUserMap == null || evaluatorUserMap.isEmpty()) {
            return;
        }
        Long resolvedTenantId = null;
        for (Long evaluatorId : evaluatorUserMap.keySet()) {
            resolvedTenantId = resolveUserTenantId(evaluatorId, publish != null ? publish.getTenantId() : null, null);
            if (resolvedTenantId != null && resolvedTenantId > 0) {
                break;
            }
        }
        if (resolvedTenantId == null || resolvedTenantId <= 0) {
            return;
        }
        if (Objects.equals(publish.getTenantId(), resolvedTenantId)) {
            return;
        }
        QuestionnairePublishDO tenantUpdate = new QuestionnairePublishDO();
        tenantUpdate.setId(publish.getId());
        tenantUpdate.setTenantId(resolvedTenantId);
        TenantUtils.executeIgnore(() -> {
            publishMapper.updateById(tenantUpdate);
            return null;
        });
        publish.setTenantId(resolvedTenantId);
    }

    private Long resolveWriteTenantId(QuestionnairePublishDO publish, QuestionnaireDO questionnaire, AdminUserRespDTO user) {
        Long userTenantId = resolveUserTenantId(
                user != null ? user.getId() : null,
                publish != null ? publish.getTenantId() : null,
                questionnaire != null ? questionnaire.getTenantId() : null
        );
        if (userTenantId != null && userTenantId > 0) {
            return userTenantId;
        }
        if (publish != null && publish.getTenantId() != null && publish.getTenantId() > 0) {
            return publish.getTenantId();
        }
        if (questionnaire != null && questionnaire.getTenantId() != null && questionnaire.getTenantId() > 0) {
            return questionnaire.getTenantId();
        }
        Long tenantId = TenantContextHolder.getTenantId();
        return tenantId != null && tenantId > 0 ? tenantId : null;
    }

    private Long resolveUserTenantId(Long userId, Long... preferredTenantIds) {
        if (userId == null || userId <= 0) {
            return null;
        }
        Long cachedTenantId = userTenantIdCache.get(userId);
        if (cachedTenantId != null && cachedTenantId > 0) {
            return cachedTenantId;
        }
        if (preferredTenantIds != null) {
            for (Long preferredTenantId : preferredTenantIds) {
                if (hasUserTenantAccess(userId, preferredTenantId)) {
                    userTenantIdCache.put(userId, preferredTenantId);
                    return preferredTenantId;
                }
            }
        }
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (hasUserTenantAccess(userId, currentTenantId)) {
            userTenantIdCache.put(userId, currentTenantId);
            return currentTenantId;
        }
        try {
            List<Long> tenantIds = TenantUtils.executeIgnore(() -> tenantCommonApi.getTenantIdList().getCheckedData());
            if (tenantIds != null) {
                for (Long tenantId : tenantIds) {
                    if (hasUserTenantAccess(userId, tenantId)) {
                        userTenantIdCache.put(userId, tenantId);
                        return tenantId;
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("resolveUserTenantId failed, userId={}", userId, ex);
        }
        return null;
    }

    private boolean hasUserTenantAccess(Long userId, Long tenantId) {
        if (userId == null || userId <= 0 || tenantId == null || tenantId <= 0) {
            return false;
        }
        try {
            Boolean allowed = TenantUtils.executeIgnore(
                    () -> tenantCommonApi.checkUserTenantAccess(userId, tenantId).getCheckedData());
            return Boolean.TRUE.equals(allowed);
        } catch (Exception ex) {
            log.warn("hasUserTenantAccess failed, userId={}, tenantId={}", userId, tenantId, ex);
            return false;
        }
    }

    /**
     * 撤回发布时清理分配记录（可选是否清理已提交答卷）
     */
    private void revokeAssignments(Long publishId, boolean clearSubmittedOnRevoke) {
        try {
            if (clearSubmittedOnRevoke) {
                answerMapper.deleteByPublishId(publishId);
                assignmentMapper.deleteByPublishId(publishId);
            } else {
                assignmentMapper.deleteUnsubmittedByPublishId(publishId);
            }
            log.info("revokeAssignments success, publishId={}", publishId);
        } catch (Exception ex) {
            log.error("revokeAssignments failed, publishId={}", publishId, ex);
        }
    }

    @Override
    public int previewScopeUserCount(String publishScopeJson) {
        if (publishScopeJson == null || publishScopeJson.trim().isEmpty()) {
            return 0;
        }
        try {
            PublishScope scope = objectMapper.readValue(publishScopeJson, PublishScope.class);
            Set<Long> userIds = resolveUserIdsByScope(scope);
            removeExcludedUsers(userIds, scope);
            return userIds.size();
        } catch (Exception ex) {
            log.warn("previewScopeUserCount failed", ex);
            return 0;
        }
    }

    @Override
    public QuestionnairePublishScopePreviewRespVO previewScopeUsers(String publishScopeJson) {
        QuestionnairePublishScopePreviewRespVO resp = new QuestionnairePublishScopePreviewRespVO();
        if (publishScopeJson == null || publishScopeJson.trim().isEmpty()) {
            return resp;
        }
        try {
            PublishScope scope = objectMapper.readValue(publishScopeJson, PublishScope.class);
            Set<Long> evaluatorIds = resolveUserIdsByScope(scope);
            if (evaluatorIds.isEmpty()) {
                return resp;
            }
            removeExcludedUsers(evaluatorIds, scope);
            if (evaluatorIds.isEmpty()) {
                return resp;
            }
            java.util.Map<Long, AdminUserRespDTO> evaluatorUserMap = loadUserMap(evaluatorIds);
            java.util.Map<Long, Set<Long>> evaluatorTargetsMap = new LinkedHashMap<>();
            String relationMode = normalizeRelationMode(scope.getRelationMode());
            if ("custom".equals(relationMode)) {
                Set<Long> targetIds = resolveTargetUserIdsByTargetScope(scope.getTargetScope(), evaluatorIds);
                if (targetIds == null) {
                    targetIds = new HashSet<>(evaluatorIds);
                }
                for (Long evaluatorId : evaluatorIds) {
                    evaluatorTargetsMap.put(evaluatorId, new HashSet<>(targetIds));
                }
            } else {
                evaluatorTargetsMap.putAll(resolveRelationTargetsByMode(
                        relationMode, scope, evaluatorIds, evaluatorUserMap));
            }
            applyPeerEvaluationRules(evaluatorTargetsMap, scope);
            if (evaluatorTargetsMap.isEmpty()) {
                return resp;
            }

            Set<Long> allUserIds = new HashSet<>(evaluatorTargetsMap.keySet());
            evaluatorTargetsMap.values().forEach(targetIds -> {
                if (targetIds != null) {
                    allUserIds.addAll(targetIds);
                }
            });
            java.util.Map<Long, AdminUserRespDTO> userMap = loadUserMap(allUserIds);

            java.util.Map<Long, Set<Long>> filteredMap = new LinkedHashMap<>();
            for (java.util.Map.Entry<Long, Set<Long>> pair : evaluatorTargetsMap.entrySet()) {
                Long evaluatorId = pair.getKey();
                Set<Long> targetIds = pair.getValue();
                if (targetIds == null || targetIds.isEmpty()) {
                    continue;
                }
                AdminUserRespDTO evaluator = userMap.get(evaluatorId);
                if (evaluator == null) {
                    continue;
                }
                Set<Long> filteredTargets = new HashSet<>();
                for (Long targetId : targetIds) {
                    AdminUserRespDTO target = userMap.get(targetId);
                    if (target == null) {
                        continue;
                    }
                    if (Boolean.TRUE.equals(scope.getExcludeSelf()) && Objects.equals(evaluatorId, targetId)) {
                        continue;
                    }
                    if (Boolean.TRUE.equals(scope.getSameDeptOnly()) && !isSameDept(evaluator, target)) {
                        continue;
                    }
                    filteredTargets.add(targetId);
                }
                if (!filteredTargets.isEmpty()) {
                    filteredMap.put(evaluatorId, filteredTargets);
                }
            }

            if (Boolean.TRUE.equals(scope.getExcludeTargetsFromEvaluators()) && !filteredMap.isEmpty()) {
                Set<Long> targetUnionIds = filteredMap.values().stream()
                        .filter(Objects::nonNull)
                        .flatMap(Set::stream)
                        .collect(Collectors.toSet());
                filteredMap.keySet().removeIf(targetUnionIds::contains);
            }
            if (filteredMap.isEmpty()) {
                return resp;
            }

            Set<Long> finalEvaluatorIds = new HashSet<>(filteredMap.keySet());
            Set<Long> finalTargetIds = filteredMap.values().stream()
                    .filter(Objects::nonNull)
                    .flatMap(Set::stream)
                    .collect(Collectors.toSet());
            int assignmentCount = hasPublishTargetScope(scope)
                    ? filteredMap.values().stream()
                    .filter(Objects::nonNull)
                    .mapToInt(Set::size)
                    .sum()
                    : finalEvaluatorIds.size();

            resp.setEvaluatorUsers(convertPreviewUsers(finalEvaluatorIds, userMap));
            resp.setTargetUsers(convertPreviewUsers(finalTargetIds, userMap));
            resp.setAssignmentCount(assignmentCount);
            return resp;
        } catch (Exception ex) {
            log.warn("previewScopeUsers failed", ex);
            return resp;
        }
    }

    private void removeExcludedUsers(Set<Long> userIds, PublishScope scope) {
        if (userIds == null || userIds.isEmpty()
                || scope == null || scope.getExcludeUserIds() == null || scope.getExcludeUserIds().isEmpty()) {
            return;
        }
        userIds.removeAll(scope.getExcludeUserIds().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
    }

    private boolean isOnlyCurrentBatchEligibilityTouched(QuestionnairePublishDO updateObj, QuestionnairePublishDO exist) {
        if (updateObj == null || exist == null || updateObj.getPublishScopeJson() == null
                || Objects.equals(updateObj.getPublishScopeJson(), exist.getPublishScopeJson())) {
            return false;
        }
        if (isChanged(updateObj.getQuestionnaireId(), exist.getQuestionnaireId())
                || isChanged(updateObj.getSendType(), exist.getSendType())
                || isChanged(updateObj.getScheduleType(), exist.getScheduleType())
                || isChanged(updateObj.getScheduleDayOfWeek(), exist.getScheduleDayOfWeek())
                || isChanged(updateObj.getScheduleDayOfMonth(), exist.getScheduleDayOfMonth())
                || isChanged(updateObj.getScheduleTime(), exist.getScheduleTime())
                || isChanged(updateObj.getDeadlineHours(), exist.getDeadlineHours())
                || isChanged(updateObj.getSendAt(), exist.getSendAt())) {
            return false;
        }
        try {
            PublishScope nextScope = parseScopeOrThrow(updateObj.getPublishScopeJson());
            PublishScope previousScope = parseScopeOrThrow(exist.getPublishScopeJson());
            boolean sameBaseScope = sameLongSet(nextScope.getDeptIds(), previousScope.getDeptIds())
                    && sameLongSet(nextScope.getRoleIds(), previousScope.getRoleIds())
                    && sameLongSet(nextScope.getUserIds(), previousScope.getUserIds())
                    && Objects.equals(Boolean.TRUE.equals(nextScope.getExcludeSelf()), Boolean.TRUE.equals(previousScope.getExcludeSelf()))
                    && Objects.equals(Boolean.TRUE.equals(nextScope.getExcludeTargetsFromEvaluators()), Boolean.TRUE.equals(previousScope.getExcludeTargetsFromEvaluators()))
                    && Objects.equals(Boolean.TRUE.equals(nextScope.getSameDeptOnly()), Boolean.TRUE.equals(previousScope.getSameDeptOnly()))
                    && Objects.equals(normalizeRelationMode(nextScope.getRelationMode()), normalizeRelationMode(previousScope.getRelationMode()))
                    && sameTargetScope(nextScope.getTargetScope(), previousScope.getTargetScope());
            if (!sameBaseScope) {
                return false;
            }
            boolean excludeUsersChanged = !sameLongSet(nextScope.getExcludeUserIds(), previousScope.getExcludeUserIds());
            boolean peerRulesChanged = !Objects.equals(
                    normalizePeerEvaluationRules(nextScope), normalizePeerEvaluationRules(previousScope));
            return excludeUsersChanged || peerRulesChanged;
        } catch (Exception ex) {
            log.warn("compare current batch eligibility rules failed, publishId={}", exist.getId(), ex);
            return false;
        }
    }

    private boolean sameTargetScope(TargetScope left, TargetScope right) {
        String leftScope = left != null && left.getScope() != null ? left.getScope().trim() : "";
        String rightScope = right != null && right.getScope() != null ? right.getScope().trim() : "";
        return Objects.equals(leftScope, rightScope)
                && sameLongSet(left != null ? left.getIds() : null, right != null ? right.getIds() : null);
    }

    private boolean sameLongSet(List<Long> left, List<Long> right) {
        return Objects.equals(normalizeUserIdSet(left), normalizeUserIdSet(right));
    }

    private List<String> normalizePeerEvaluationRules(PublishScope scope) {
        if (scope == null || scope.getPeerEvaluationRules() == null || scope.getPeerEvaluationRules().isEmpty()) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        for (PeerEvaluationRule rule : scope.getPeerEvaluationRules()) {
            if (rule == null || rule.getEvaluatorId() == null) {
                continue;
            }
            List<Long> includeIds = normalizeUserIdSet(rule.getIncludeTargetIds()).stream()
                    .sorted()
                    .collect(Collectors.toList());
            List<Long> excludeIds = normalizeUserIdSet(rule.getExcludeTargetIds()).stream()
                    .sorted()
                    .collect(Collectors.toList());
            if (includeIds.isEmpty() && excludeIds.isEmpty()) {
                continue;
            }
            result.add(rule.getEvaluatorId() + "|include=" + includeIds + "|exclude=" + excludeIds);
        }
        result.sort(String::compareTo);
        return result;
    }

    private List<QuestionnairePublishScopeUserRespVO> convertPreviewUsers(Set<Long> userIds,
                                                                          java.util.Map<Long, AdminUserRespDTO> userMap) {
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<QuestionnairePublishScopeUserRespVO> result = new ArrayList<>();
        for (Long userId : userIds) {
            if (userId == null) {
                continue;
            }
            AdminUserRespDTO user = userMap.get(userId);
            QuestionnairePublishScopeUserRespVO item = new QuestionnairePublishScopeUserRespVO();
            item.setId(userId);
            if (user != null) {
                item.setNickname(user.getNickname());
                item.setDeptId(user.getDeptId());
            }
            result.add(item);
        }
        result.sort(Comparator.comparing(
                item -> item.getNickname() != null ? item.getNickname() : "", String::compareTo));
        return result;
    }

    private Set<Long> resolveUserIdsByScope(PublishScope scope) throws Exception {
        Set<Long> userIds = new HashSet<>();
        if (scope == null) {
            return userIds;
        }

        return TenantUtils.executeIgnore(() -> {
            // 按部门（含子部门）
            if (scope.getDeptIds() != null && !scope.getDeptIds().isEmpty()) {
                Set<Long> allDeptIds = new HashSet<>(scope.getDeptIds());
                for (Long deptId : scope.getDeptIds()) {
                    try {
                        List<DeptRespDTO> children = deptApi.getChildDeptList(deptId).getCheckedData();
                        if (children != null) {
                            children.forEach(child -> allDeptIds.add(child.getId()));
                        }
                    } catch (Exception ex) {
                        log.warn("获取子部门失败，继续使用当前部门范围: deptId={}", deptId, ex);
                    }
                }
                List<AdminUserRespDTO> users = adminUserApi.getUserListByDeptIds(allDeptIds).getCheckedData();
                if (users != null) {
                    users.forEach(u -> userIds.add(u.getId()));
                }
                log.info("按部门获取用户(含子部门): deptIds={}, 展开后={}, userCount={}",
                        scope.getDeptIds(), allDeptIds, users != null ? users.size() : 0);
            }

            // 按角色
            if (scope.getRoleIds() != null && !scope.getRoleIds().isEmpty()) {
                List<AdminUserRespDTO> users = adminUserApi.getUserListByRoleIds(scope.getRoleIds()).getCheckedData();
                if (users != null) {
                    users.forEach(u -> userIds.add(u.getId()));
                }
                log.info("按角色获取用户: roleIds={}, userCount={}", scope.getRoleIds(), users != null ? users.size() : 0);
            }

            // 直接指定
            if (scope.getUserIds() != null && !scope.getUserIds().isEmpty()) {
                userIds.addAll(scope.getUserIds());
            }

            return userIds;
        });
    }

    /**
     * 计算下次发布时间
     */
    private LocalDateTime calcNextPublishTime(QuestionnairePublishDO publish, LocalDateTime from) {
        if (publish.getScheduleType() == null || publish.getScheduleType() == 0) {
            return null;
        }
        String timeStr = publish.getScheduleTime() != null ? publish.getScheduleTime() : "09:00";
        int hour = 9;
        int minute = 0;
        try {
            String[] parts = timeStr.split(":");
            hour = Integer.parseInt(parts[0]);
            minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        } catch (Exception ex) {
            log.warn("invalid scheduleTime: {}, fallback to 09:00", timeStr);
        }

        LocalDateTime base = from != null ? from : LocalDateTime.now();

        switch (publish.getScheduleType()) {
            case 1: // 每天
                LocalDateTime today = base.withHour(hour).withMinute(minute).withSecond(0).withNano(0);
                return today.isAfter(base) ? today : today.plusDays(1);
            case 2: // 每周
                int targetDow = publish.getScheduleDayOfWeek() != null ? publish.getScheduleDayOfWeek() : 1;
                if (targetDow < 1 || targetDow > 7) {
                    targetDow = 1;
                }
                java.time.DayOfWeek target = java.time.DayOfWeek.of(targetDow);
                LocalDateTime thisWeekOrNext = base.with(java.time.temporal.TemporalAdjusters.nextOrSame(target))
                        .withHour(hour).withMinute(minute).withSecond(0).withNano(0);
                return thisWeekOrNext.isAfter(base) ? thisWeekOrNext : thisWeekOrNext.plusWeeks(1);
            case 3: // 每月
                int targetDay = publish.getScheduleDayOfMonth() != null ? publish.getScheduleDayOfMonth() : 1;
                if (targetDay < 1) {
                    targetDay = 1;
                }
                int currentMonthDay = Math.min(targetDay, base.toLocalDate().lengthOfMonth());
                LocalDateTime thisMonthOrNext = base.withDayOfMonth(currentMonthDay)
                        .withHour(hour).withMinute(minute).withSecond(0).withNano(0);
                if (thisMonthOrNext.isAfter(base)) {
                    return thisMonthOrNext;
                }
                LocalDateTime nextMonth = base.plusMonths(1);
                int nextMonthDay = Math.min(targetDay, nextMonth.toLocalDate().lengthOfMonth());
                return nextMonth.withDayOfMonth(nextMonthDay)
                        .withHour(hour).withMinute(minute).withSecond(0).withNano(0);
            default:
                return null;
        }
    }

    private boolean isSameDay(LocalDateTime first, LocalDateTime second) {
        if (first == null || second == null) {
            return false;
        }
        return first.toLocalDate().equals(second.toLocalDate());
    }

    /**
     * 发布范围 DTO
     */
    @Data
    private static class PublishScope {
        private List<Long> deptIds;
        private List<Long> roleIds;
        private List<Long> userIds;
        private TargetScope targetScope;
        private List<Long> excludeUserIds;
        private List<PeerEvaluationRule> peerEvaluationRules;
        private Boolean excludeSelf;
        private Boolean excludeTargetsFromEvaluators;
        private Boolean sameDeptOnly;
        private String relationMode;
    }

    @Data
    private static class PeerEvaluationRule {
        private Long evaluatorId;
        private List<Long> includeTargetIds;
        private List<Long> excludeTargetIds;
    }

    @Data
    private static class TargetScope {
        private String scope;
        private List<Long> ids;
    }

    @Data
    private static class TargetRule {
        private String scope;
        private List<Long> ids;
    }

    @Data
    private static class BatchContext {
        private Integer batchNo;
        private String batchLabel;
        private LocalDateTime batchStartAt;
        private LocalDateTime batchEndAt;
    }

    @Data
    private static class AssignmentProgressStats {
        private int totalCount;
        private int submittedCount;
        private int pendingCount;
        private List<String> pendingUserNames = new ArrayList<>();
    }

    @Data
    private static class EvaluatorProgress {
        private Long evaluatorId;
        private String evaluatorName;
        private int requiredCount;
        private int submittedCount;
        private int pendingCount;
    }

    @Data
    private static class RemindRule {
        private Boolean sendNotice;
        private Boolean enabled;
        private String time;
        private List<String> channels;
    }

    /**
     * 填充分配统计信息（包含内部考核和公开问卷）
     */
    private void fillAssignmentStats(QuestionnairePublishRespVO vo) {
        if (vo.getId() == null) {
            return;
        }
        try {
            boolean crossTenantByPublish = shouldReadAssignmentsCrossTenant(vo.getId());
            Integer effectiveBatchNo = vo.getCurrentBatchNo();
            if (effectiveBatchNo == null && vo.getGeneratedCount() != null && vo.getGeneratedCount() > 0) {
                effectiveBatchNo = vo.getGeneratedCount();
            }
            if (effectiveBatchNo == null) {
                effectiveBatchNo = selectLatestBatchNoByPublishId(vo.getId(), crossTenantByPublish);
            }

            final Integer queryBatchNo = effectiveBatchNo;
            List<QuestionnaireAssignmentDO> assignments = selectAssignmentsByPublishIdAndBatchNo(vo.getId(), queryBatchNo, crossTenantByPublish);
            AssignmentProgressStats progressStats = buildAssignmentProgressStats(
                    assignments, extractExcludedEvaluatorIds(vo.getPublishScopeJson()));
            if (effectiveBatchNo != null && vo.getCurrentBatchNo() == null) {
                vo.setCurrentBatchNo(effectiveBatchNo);
                vo.setCurrentBatchLabel("第" + effectiveBatchNo + "期");
            }

            Long publicSubmitted = crossTenantByPublish
                    ? TenantUtils.executeIgnore(() -> publicAnswerMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<QuestionnairePublicAnswerDO>()
                            .eq(QuestionnairePublicAnswerDO::getPublishId, vo.getId())
            ))
                    : publicAnswerMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<QuestionnairePublicAnswerDO>()
                            .eq(QuestionnairePublicAnswerDO::getPublishId, vo.getId())
            );

            int assignmentTotal = progressStats.getTotalCount();
            if (assignmentTotal == 0 && (assignments == null || assignments.isEmpty())
                    && vo.getPublishScopeJson() != null && !vo.getPublishScopeJson().trim().isEmpty()) {
                assignmentTotal = previewScopeUserCount(vo.getPublishScopeJson());
            }
            vo.setTotalCount(assignmentTotal);

            int totalSubmitted = progressStats.getSubmittedCount() + (publicSubmitted != null ? publicSubmitted.intValue() : 0);
            vo.setSubmittedCount(totalSubmitted);
            vo.setPendingCount(Math.max(assignmentTotal - progressStats.getSubmittedCount(), 0));
            vo.setPendingUserNames(progressStats.getPendingUserNames());
            vo.setPublicSubmittedCount(publicSubmitted != null ? publicSubmitted.intValue() : 0);
        } catch (Exception ex) {
            log.warn("fillAssignmentStats failed, publishId={}", vo.getId(), ex);
            vo.setTotalCount(0);
            vo.setSubmittedCount(0);
            vo.setPendingCount(0);
            vo.setPendingUserNames(new ArrayList<>());
        }
    }

    private AssignmentProgressStats buildAssignmentProgressStats(List<QuestionnaireAssignmentDO> assignments,
                                                                 Set<Long> excludedEvaluatorIds) {
        Map<Long, EvaluatorProgress> evaluatorProgressMap = new LinkedHashMap<>();
        if (assignments != null) {
            for (QuestionnaireAssignmentDO assignment : assignments) {
                if (assignment == null) {
                    continue;
                }
                Long userKey = assignment.getEvaluatorId() != null ? assignment.getEvaluatorId() : assignment.getId();
                if (userKey == null) {
                    continue;
                }
                if (excludedEvaluatorIds != null && excludedEvaluatorIds.contains(userKey)) {
                    continue;
                }
                EvaluatorProgress progress = evaluatorProgressMap.computeIfAbsent(userKey, key -> {
                    EvaluatorProgress item = new EvaluatorProgress();
                    item.setEvaluatorId(key);
                    item.setEvaluatorName(resolveEvaluatorProgressName(assignment, key));
                    return item;
                });
                if ((progress.getEvaluatorName() == null || progress.getEvaluatorName().trim().isEmpty())
                        && assignment.getEvaluatorName() != null && !assignment.getEvaluatorName().trim().isEmpty()) {
                    progress.setEvaluatorName(assignment.getEvaluatorName().trim());
                }
                if (Objects.equals(assignment.getStatus(), ASSIGNMENT_STATUS_NOT_REQUIRED)) {
                    continue;
                }
                progress.setRequiredCount(progress.getRequiredCount() + 1);
                if (Objects.equals(assignment.getStatus(), ASSIGNMENT_STATUS_SUBMITTED)) {
                    progress.setSubmittedCount(progress.getSubmittedCount() + 1);
                } else {
                    progress.setPendingCount(progress.getPendingCount() + 1);
                }
            }
        }

        AssignmentProgressStats stats = new AssignmentProgressStats();
        List<String> pendingUserNames = new ArrayList<>();
        for (EvaluatorProgress progress : evaluatorProgressMap.values()) {
            if (progress.getRequiredCount() <= 0) {
                continue;
            }
            stats.setTotalCount(stats.getTotalCount() + 1);
            if (progress.getPendingCount() <= 0
                    && progress.getSubmittedCount() >= progress.getRequiredCount()) {
                stats.setSubmittedCount(stats.getSubmittedCount() + 1);
            } else {
                pendingUserNames.add(progress.getEvaluatorName());
            }
        }
        stats.setPendingCount(Math.max(stats.getTotalCount() - stats.getSubmittedCount(), 0));
        stats.setPendingUserNames(pendingUserNames);
        return stats;
    }

    private Set<Long> extractExcludedEvaluatorIds(String publishScopeJson) {
        if (publishScopeJson == null || publishScopeJson.trim().isEmpty()) {
            return new HashSet<>();
        }
        try {
            PublishScope scope = objectMapper.readValue(publishScopeJson, PublishScope.class);
            return normalizeUserIdSet(scope.getExcludeUserIds());
        } catch (Exception ex) {
            log.warn("extract excluded evaluator ids failed", ex);
            return new HashSet<>();
        }
    }

    private String resolveEvaluatorProgressName(QuestionnaireAssignmentDO assignment, Long userKey) {
        if (assignment != null && assignment.getEvaluatorName() != null && !assignment.getEvaluatorName().trim().isEmpty()) {
            return assignment.getEvaluatorName().trim();
        }
        return userKey != null ? "用户" + userKey : "-";
    }

    private boolean shouldReadAssignmentsCrossTenant(Long publishId) {
        if (publishId == null) {
            return false;
        }
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (currentTenantId == null || currentTenantId <= 0) {
            return false;
        }
        QuestionnairePublishDO publish = publishMapper.selectById(publishId);
        if (publish == null) {
            publish = TenantUtils.executeIgnore(() -> publishMapper.selectById(publishId));
        }
        return publish != null
                && publish.getTenantId() != null
                && publish.getTenantId() > 0
                && !Objects.equals(publish.getTenantId(), currentTenantId);
    }

    private List<QuestionnaireAssignmentDO> selectAssignmentsByPublishId(Long publishId) {
        List<QuestionnaireAssignmentDO> assignments = assignmentMapper.selectListByPublishId(publishId);
        if (assignments == null || assignments.isEmpty()) {
            assignments = TenantUtils.executeIgnore(() -> assignmentMapper.selectListByPublishId(publishId));
        }
        return assignments != null ? assignments : new ArrayList<>();
    }

    private List<QuestionnaireAssignmentDO> selectAssignmentsByPublishIdAndBatchNo(Long publishId,
                                                                                    Integer batchNo,
                                                                                    boolean crossTenant) {
        List<QuestionnaireAssignmentDO> assignments = crossTenant
                ? TenantUtils.executeIgnore(() -> assignmentMapper.selectListByPublishIdAndBatchNo(publishId, batchNo))
                : assignmentMapper.selectListByPublishIdAndBatchNo(publishId, batchNo);
        if ((assignments == null || assignments.isEmpty()) && !crossTenant) {
            assignments = TenantUtils.executeIgnore(() -> assignmentMapper.selectListByPublishIdAndBatchNo(publishId, batchNo));
        }
        return assignments != null ? assignments : new ArrayList<>();
    }

    private Integer selectLatestBatchNoByPublishId(Long publishId, boolean crossTenant) {
        Integer batchNo = crossTenant
                ? TenantUtils.executeIgnore(() -> assignmentMapper.selectLatestBatchNoByPublishId(publishId))
                : assignmentMapper.selectLatestBatchNoByPublishId(publishId);
        if (batchNo == null && !crossTenant) {
            batchNo = TenantUtils.executeIgnore(() -> assignmentMapper.selectLatestBatchNoByPublishId(publishId));
        }
        return batchNo;
    }

}
