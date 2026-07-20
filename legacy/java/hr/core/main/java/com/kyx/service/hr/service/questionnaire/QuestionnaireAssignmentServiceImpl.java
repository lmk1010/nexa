package com.kyx.service.hr.service.questionnaire;

import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireAssignmentPageReqVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireAssignmentRespVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireAssignmentSaveReqVO;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnaireAssignmentDO;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnaireAnswerDO;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnaireDO;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnaireItemDO;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnairePublishDO;
import com.kyx.service.hr.dal.mysql.questionnaire.QuestionnaireAnswerMapper;
import com.kyx.service.hr.dal.mysql.questionnaire.QuestionnaireAssignmentMapper;
import com.kyx.service.hr.dal.mysql.questionnaire.QuestionnaireItemMapper;
import com.kyx.service.hr.dal.mysql.questionnaire.QuestionnaireMapper;
import com.kyx.service.hr.dal.mysql.questionnaire.QuestionnairePublishMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.kyx.service.hr.enums.ErrorCodeConstants.QUESTIONNAIRE_ASSIGNMENT_NOT_EXISTS;
import static com.kyx.service.hr.enums.ErrorCodeConstants.QUESTIONNAIRE_NOT_EXISTS;
import static com.kyx.service.hr.enums.ErrorCodeConstants.QUESTIONNAIRE_PUBLISH_NOT_EXISTS;

/**
 * HR 问卷分配 Service 实现类
 *
 * @author MK
 */
@Service
@Validated
@Slf4j
public class QuestionnaireAssignmentServiceImpl implements QuestionnaireAssignmentService {

    @Resource
    private QuestionnaireAssignmentMapper assignmentMapper;
    @Resource
    private QuestionnairePublishMapper publishMapper;
    @Resource
    private QuestionnaireMapper questionnaireMapper;
    @Resource
    private QuestionnaireAnswerMapper answerMapper;
    @Resource
    private QuestionnaireItemMapper itemMapper;
    @Resource
    private AdminUserApi adminUserApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAssignment(QuestionnaireAssignmentSaveReqVO createReqVO) {
        QuestionnaireAssignmentDO assignment = buildValidatedAssignment(createReqVO, null);
        assignmentMapper.insert(assignment);
        return assignment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAssignment(QuestionnaireAssignmentSaveReqVO updateReqVO) {
        QuestionnaireAssignmentDO existing = validateAssignmentExists(updateReqVO.getId());
        if (isSubmitted(existing)) {
            throw ServiceExceptionUtil.invalidParamException("已提交的问卷分配不能编辑");
        }
        QuestionnaireAssignmentDO updateObj = buildValidatedAssignment(updateReqVO, existing);
        assignmentMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAssignment(Long id) {
        QuestionnaireAssignmentDO existing = validateAssignmentExists(id);
        if (isSubmitted(existing)) {
            throw ServiceExceptionUtil.invalidParamException("已提交的问卷分配不能删除");
        }
        answerMapper.deleteByAssignmentId(id);
        assignmentMapper.deleteById(id);
    }

    @Override
    public QuestionnaireAssignmentRespVO getAssignment(Long id) {
        QuestionnaireAssignmentDO assignment = validateAssignmentExists(id);
        QuestionnaireAssignmentRespVO respVO = BeanUtils.toBean(assignment, QuestionnaireAssignmentRespVO.class);
        fillQuestionnaireMetadata(java.util.Collections.singletonList(respVO),
                assignment.getId() == null
                        ? java.util.Collections.emptyMap()
                        : java.util.Collections.singletonMap(assignment.getId(), assignment));
        List<QuestionnaireAnswerDO> answerList = answerMapper.selectListByAssignmentId(id);
        if (answerList == null || answerList.isEmpty()) {
            answerList = TenantUtils.executeIgnore(() -> answerMapper.selectListByAssignmentId(id));
        }
        Map<Long, String> itemTypeMap = buildItemTypeMap(answerList);
        fillSubmitStats(respVO, assignment, answerList, itemTypeMap);
        return respVO;
    }

    @Override
    public PageResult<QuestionnaireAssignmentRespVO> getAssignmentPage(QuestionnaireAssignmentPageReqVO pageReqVO) {
        boolean myVisibleOnly = Boolean.TRUE.equals(pageReqVO.getVisibleOnly());
        if (!myVisibleOnly) {
            cleanupInvalidAssignments(pageReqVO);
        }
        PageResult<QuestionnaireAssignmentDO> pageResult = myVisibleOnly
                ? TenantUtils.executeIgnore(() -> assignmentMapper.selectPage(pageReqVO))
                : assignmentMapper.selectPage(pageReqVO);
        if (!myVisibleOnly
                && (pageResult == null || pageResult.getList() == null || pageResult.getList().isEmpty())
                && pageReqVO.getEvaluatorId() != null) {
            pageResult = TenantUtils.executeIgnore(() -> assignmentMapper.selectPage(pageReqVO));
        }
        PageResult<QuestionnaireAssignmentRespVO> resp = BeanUtils.toBean(pageResult, QuestionnaireAssignmentRespVO.class);
        if (resp == null || resp.getList() == null || resp.getList().isEmpty()) {
            return resp;
        }

        Map<Long, QuestionnaireAssignmentDO> assignmentMap = pageResult.getList().stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(QuestionnaireAssignmentDO::getId, item -> item, (left, right) -> left));
        List<Long> assignmentIds = assignmentMap.keySet().stream().collect(Collectors.toList());
        List<QuestionnaireAnswerDO> answerList = answerMapper.selectListByAssignmentIds(assignmentIds);
        if ((answerList == null || answerList.isEmpty()) && !assignmentIds.isEmpty()) {
            List<Long> finalAssignmentIds = assignmentIds;
            answerList = TenantUtils.executeIgnore(() -> answerMapper.selectListByAssignmentIds(finalAssignmentIds));
        }
        Map<Long, String> itemTypeMap = buildItemTypeMap(answerList);
        Map<Long, List<QuestionnaireAnswerDO>> answerMap = answerList.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getAssignmentId() != null)
                .collect(Collectors.groupingBy(QuestionnaireAnswerDO::getAssignmentId));

        for (QuestionnaireAssignmentRespVO item : resp.getList()) {
            if (item == null || item.getId() == null) {
                continue;
            }
            fillSubmitStats(item, assignmentMap.get(item.getId()), answerMap.get(item.getId()), itemTypeMap);
        }
        fillQuestionnaireMetadata(resp.getList(), assignmentMap);
        return resp;
    }

    private void fillQuestionnaireMetadata(List<QuestionnaireAssignmentRespVO> items,
                                           Map<Long, QuestionnaireAssignmentDO> assignmentMap) {
        if (items == null || items.isEmpty()) {
            return;
        }
        Set<Long> questionnaireIds = items.stream()
                .map(QuestionnaireAssignmentRespVO::getQuestionnaireId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> publishIds = items.stream()
                .map(QuestionnaireAssignmentRespVO::getPublishId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, QuestionnaireDO> questionnaireMap = selectQuestionnaireMap(questionnaireIds);
        Map<Long, QuestionnairePublishDO> publishMap = selectPublishMap(publishIds);

        for (QuestionnaireAssignmentRespVO item : items) {
            if (item == null) {
                continue;
            }
            QuestionnaireDO questionnaire = questionnaireMap.get(item.getQuestionnaireId());
            if (questionnaire != null) {
                item.setQuestionnaireName(questionnaire.getName());
                item.setQuestionnaireType(questionnaire.getType());
            }

            QuestionnaireAssignmentDO assignment = assignmentMap == null ? null : assignmentMap.get(item.getId());
            LocalDateTime deadlineAt = item.getBatchEndAt();
            if (deadlineAt == null && assignment != null) {
                deadlineAt = assignment.getBatchEndAt();
            }
            QuestionnairePublishDO publish = publishMap.get(item.getPublishId());
            if (deadlineAt == null && publish != null) {
                deadlineAt = publish.getCurrentBatchEndAt() != null
                        ? publish.getCurrentBatchEndAt()
                        : publish.getDeadlineAt();
            }
            item.setDeadlineAt(deadlineAt);
        }
    }

    private Map<Long, QuestionnaireDO> selectQuestionnaireMap(Set<Long> questionnaireIds) {
        if (questionnaireIds == null || questionnaireIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        List<QuestionnaireDO> list = questionnaireMapper.selectBatchIds(questionnaireIds);
        List<QuestionnaireDO> ignoreList = TenantUtils.executeIgnore(() -> questionnaireMapper.selectBatchIds(questionnaireIds));
        return java.util.stream.Stream.concat(
                        list == null ? java.util.stream.Stream.empty() : list.stream(),
                        ignoreList == null ? java.util.stream.Stream.empty() : ignoreList.stream())
                .filter(Objects::nonNull)
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(QuestionnaireDO::getId, item -> item, (left, right) -> left));
    }

    private Map<Long, QuestionnairePublishDO> selectPublishMap(Set<Long> publishIds) {
        if (publishIds == null || publishIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        List<QuestionnairePublishDO> list = publishMapper.selectBatchIds(publishIds);
        List<QuestionnairePublishDO> ignoreList = TenantUtils.executeIgnore(() -> publishMapper.selectBatchIds(publishIds));
        return java.util.stream.Stream.concat(
                        list == null ? java.util.stream.Stream.empty() : list.stream(),
                        ignoreList == null ? java.util.stream.Stream.empty() : ignoreList.stream())
                .filter(Objects::nonNull)
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(QuestionnairePublishDO::getId, item -> item, (left, right) -> left));
    }

    private Map<Long, String> buildItemTypeMap(List<QuestionnaireAnswerDO> answerList) {
        if (answerList == null || answerList.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        Set<Long> itemIds = answerList.stream()
                .map(QuestionnaireAnswerDO::getItemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (itemIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        return itemMapper.selectBatchIds(itemIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(QuestionnaireItemDO::getId, QuestionnaireItemDO::getItemType, (left, right) -> left));
    }

    private void fillSubmitStats(QuestionnaireAssignmentRespVO respVO,
                                 QuestionnaireAssignmentDO assignment,
                                 List<QuestionnaireAnswerDO> answerList,
                                 Map<Long, String> itemTypeMap) {
        if (respVO == null || assignment == null || assignment.getStatus() == null || assignment.getStatus() != 1) {
            return;
        }
        respVO.setSubmitTime(assignment.getUpdateTime());
        int totalScore = 0;
        if (answerList != null) {
            for (QuestionnaireAnswerDO answer : answerList) {
                if (answer != null && answer.getAnswerScore() != null) {
                    String itemType = itemTypeMap != null ? itemTypeMap.get(answer.getItemId()) : null;
                    if ("text".equals(itemType) || "blank".equals(itemType)) {
                        continue;
                    }
                    totalScore += answer.getAnswerScore();
                }
            }
        }
        respVO.setTotalScore(totalScore);
    }

    private void cleanupInvalidAssignments(QuestionnaireAssignmentPageReqVO pageReqVO) {
        List<QuestionnaireAssignmentDO> assignments = pageReqVO.getEvaluatorId() != null
                ? assignmentMapper.selectListByEvaluatorId(pageReqVO.getEvaluatorId(), pageReqVO.getStatus())
                : assignmentMapper.selectPage(pageReqVO).getList();
        if ((assignments == null || assignments.isEmpty()) && pageReqVO.getEvaluatorId() != null) {
            assignments = TenantUtils.executeIgnore(
                    () -> assignmentMapper.selectListByEvaluatorId(pageReqVO.getEvaluatorId(), pageReqVO.getStatus()));
        }
        if (assignments == null || assignments.isEmpty()) {
            return;
        }
        Set<Long> questionnaireIds = assignments.stream().map(QuestionnaireAssignmentDO::getQuestionnaireId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Set<Long> publishIds = assignments.stream().map(QuestionnaireAssignmentDO::getPublishId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());

        Set<Long> existingQuestionnaireIds = selectExistingQuestionnaireIds(questionnaireIds);
        Set<Long> existingPublishIds = selectExistingPublishIds(publishIds);

        assignments.stream().filter(item -> {
            if (item.getQuestionnaireId() == null || !existingQuestionnaireIds.contains(item.getQuestionnaireId())) {
                return true;
            }
            return item.getPublishId() != null && !existingPublishIds.contains(item.getPublishId());
        }).forEach(item -> {
            answerMapper.deleteByAssignmentId(item.getId());
            assignmentMapper.deleteById(item.getId());
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchCreateAssignments(java.util.List<QuestionnaireAssignmentSaveReqVO> assignments) {
        if (assignments == null || assignments.isEmpty()) {
            return;
        }
        List<QuestionnaireAssignmentDO> validatedAssignments = assignments.stream()
                .map(item -> buildValidatedAssignment(item, null))
                .collect(Collectors.toList());
        validateNoDuplicateAssignmentsInRequest(validatedAssignments);
        for (QuestionnaireAssignmentDO assignment : validatedAssignments) {
            assignmentMapper.insert(assignment);
        }
    }

    private QuestionnaireAssignmentDO buildValidatedAssignment(QuestionnaireAssignmentSaveReqVO reqVO,
                                                              QuestionnaireAssignmentDO existing) {
        if (reqVO == null) {
            throw ServiceExceptionUtil.invalidParamException("问卷分配不能为空");
        }
        QuestionnaireDO questionnaire = validateQuestionnaireExists(reqVO.getQuestionnaireId());
        QuestionnairePublishDO publish = reqVO.getPublishId() == null
                ? null : validatePublishExists(reqVO.getPublishId());
        if (publish != null && !Objects.equals(publish.getQuestionnaireId(), questionnaire.getId())) {
            throw ServiceExceptionUtil.invalidParamException("发布计划和问卷不匹配");
        }

        QuestionnaireAssignmentDO assignment = BeanUtils.toBean(reqVO, QuestionnaireAssignmentDO.class);
        assignment.setId(existing != null ? existing.getId() : reqVO.getId());
        assignment.setStatus(normalizeManualStatus(reqVO.getStatus()));
        if (publish != null) {
            assignment.setBatchNo(publish.getCurrentBatchNo());
            assignment.setBatchLabel(publish.getCurrentBatchLabel());
            assignment.setBatchStartAt(publish.getCurrentBatchStartAt());
            assignment.setBatchEndAt(publish.getCurrentBatchEndAt());
        }
        normalizeRole(assignment);
        if (!isPeerQuestionnaire(questionnaire)) {
            assignment.setTargetId(null);
            assignment.setTargetName(null);
        }
        fillUserNames(assignment, questionnaire);
        validateDuplicateAssignment(assignment, existing != null ? existing.getId() : null);
        return assignment;
    }

    private QuestionnaireDO validateQuestionnaireExists(Long questionnaireId) {
        QuestionnaireDO questionnaire = questionnaireMapper.selectById(questionnaireId);
        if (questionnaire == null) {
            questionnaire = TenantUtils.executeIgnore(() -> questionnaireMapper.selectById(questionnaireId));
        }
        if (questionnaire == null) {
            throw ServiceExceptionUtil.exception(QUESTIONNAIRE_NOT_EXISTS);
        }
        return questionnaire;
    }

    private QuestionnairePublishDO validatePublishExists(Long publishId) {
        QuestionnairePublishDO publish = publishMapper.selectById(publishId);
        if (publish == null) {
            publish = TenantUtils.executeIgnore(() -> publishMapper.selectById(publishId));
        }
        if (publish == null) {
            throw ServiceExceptionUtil.exception(QUESTIONNAIRE_PUBLISH_NOT_EXISTS);
        }
        return publish;
    }

    private Integer normalizeManualStatus(Integer status) {
        if (status == null || status == 0) {
            return 0;
        }
        throw ServiceExceptionUtil.invalidParamException("分配状态只能由答卷提交动作更新");
    }

    private void normalizeRole(QuestionnaireAssignmentDO assignment) {
        String role = assignment.getRole();
        if (!StringUtils.hasText(role)) {
            assignment.setRole(null);
            return;
        }
        String normalized = role.trim();
        if (!"manager".equals(normalized) && !"employee".equals(normalized) && !"both".equals(normalized)) {
            throw ServiceExceptionUtil.invalidParamException("问卷分配角色无效");
        }
        assignment.setRole(normalized);
    }

    private void fillUserNames(QuestionnaireAssignmentDO assignment, QuestionnaireDO questionnaire) {
        Set<Long> userIds = new HashSet<>();
        userIds.add(assignment.getEvaluatorId());
        if (isPeerQuestionnaire(questionnaire)) {
            if (assignment.getTargetId() == null) {
                throw ServiceExceptionUtil.invalidParamException("互评问卷被评人不能为空");
            }
            userIds.add(assignment.getTargetId());
        }
        Map<Long, AdminUserRespDTO> userMap = loadUserMap(userIds);
        AdminUserRespDTO evaluator = userMap.get(assignment.getEvaluatorId());
        if (evaluator == null) {
            throw ServiceExceptionUtil.invalidParamException("评价人不存在");
        }
        assignment.setEvaluatorName(displayName(evaluator));
        if (!isPeerQuestionnaire(questionnaire)) {
            assignment.setTargetName(null);
            return;
        }
        AdminUserRespDTO target = userMap.get(assignment.getTargetId());
        if (target == null) {
            throw ServiceExceptionUtil.invalidParamException("被评人不存在");
        }
        assignment.setTargetName(displayName(target));
    }

    private boolean isPeerQuestionnaire(QuestionnaireDO questionnaire) {
        return questionnaire != null && "peer".equalsIgnoreCase(questionnaire.getType());
    }

    private Map<Long, AdminUserRespDTO> loadUserMap(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        Set<Long> validUserIds = userIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (validUserIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        try {
            return TenantUtils.executeIgnore(() -> adminUserApi.getUserMap(validUserIds));
        } catch (Exception ex) {
            log.warn("load questionnaire assignment users failed, userIds={}", validUserIds, ex);
            return java.util.Collections.emptyMap();
        }
    }

    private String displayName(AdminUserRespDTO user) {
        if (user == null) {
            return null;
        }
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }

    private void validateDuplicateAssignment(QuestionnaireAssignmentDO assignment, Long excludeId) {
        boolean exists = assignmentMapper.existsByBusinessKey(
                assignment.getQuestionnaireId(),
                assignment.getPublishId(),
                assignment.getBatchNo(),
                assignment.getEvaluatorId(),
                assignment.getTargetId(),
                excludeId);
        if (!exists) {
            Boolean ignoreExists = TenantUtils.executeIgnore(() -> assignmentMapper.existsByBusinessKey(
                    assignment.getQuestionnaireId(),
                    assignment.getPublishId(),
                    assignment.getBatchNo(),
                    assignment.getEvaluatorId(),
                    assignment.getTargetId(),
                    excludeId));
            exists = Boolean.TRUE.equals(ignoreExists);
        }
        if (exists) {
            throw ServiceExceptionUtil.invalidParamException("同一问卷下已存在相同评价关系");
        }
    }

    private boolean isSubmitted(QuestionnaireAssignmentDO assignment) {
        return assignment != null && assignment.getStatus() != null && assignment.getStatus() == 1;
    }

    private void validateNoDuplicateAssignmentsInRequest(List<QuestionnaireAssignmentDO> assignments) {
        Set<String> businessKeys = new HashSet<>();
        for (QuestionnaireAssignmentDO assignment : assignments) {
            String businessKey = assignmentBusinessKey(assignment);
            if (!businessKeys.add(businessKey)) {
                throw ServiceExceptionUtil.invalidParamException("批量分配中存在重复评价关系");
            }
        }
    }

    private String assignmentBusinessKey(QuestionnaireAssignmentDO assignment) {
        return assignment.getQuestionnaireId()
                + "|" + assignment.getPublishId()
                + "|" + assignment.getBatchNo()
                + "|" + assignment.getEvaluatorId()
                + "|" + assignment.getTargetId();
    }

    private QuestionnaireAssignmentDO validateAssignmentExists(Long id) {
        QuestionnaireAssignmentDO assignment = assignmentMapper.selectById(id);
        if (assignment == null) {
            assignment = TenantUtils.executeIgnore(() -> assignmentMapper.selectById(id));
        }
        if (assignment == null) {
            throw ServiceExceptionUtil.exception(QUESTIONNAIRE_ASSIGNMENT_NOT_EXISTS);
        }
        return assignment;
    }

    private Set<Long> selectExistingQuestionnaireIds(Set<Long> questionnaireIds) {
        if (questionnaireIds == null || questionnaireIds.isEmpty()) {
            return java.util.Collections.emptySet();
        }
        Set<Long> result = questionnaireMapper.selectBatchIds(questionnaireIds).stream()
                .filter(Objects::nonNull)
                .map(QuestionnaireDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> ignoreResult = TenantUtils.executeIgnore(() -> questionnaireMapper.selectBatchIds(questionnaireIds)
                .stream()
                .filter(Objects::nonNull)
                .map(QuestionnaireDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        if (ignoreResult != null && !ignoreResult.isEmpty()) {
            result.addAll(ignoreResult);
        }
        return result;
    }

    private Set<Long> selectExistingPublishIds(Set<Long> publishIds) {
        if (publishIds == null || publishIds.isEmpty()) {
            return java.util.Collections.emptySet();
        }
        Set<Long> result = publishMapper.selectBatchIds(publishIds).stream()
                .filter(Objects::nonNull)
                .map(QuestionnairePublishDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> ignoreResult = TenantUtils.executeIgnore(() -> publishMapper.selectBatchIds(publishIds)
                .stream()
                .filter(Objects::nonNull)
                .map(QuestionnairePublishDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        if (ignoreResult != null && !ignoreResult.isEmpty()) {
            result.addAll(ignoreResult);
        }
        return result;
    }

}
