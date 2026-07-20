package com.kyx.service.hr.service.questionnaire;

import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.util.json.JsonUtils;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.security.core.util.SecurityFrameworkUtils;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireAnswerItemSaveReqVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireAnswerRespVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireAnswerSubmitReqVO;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnaireAnswerDO;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnaireAssignmentDO;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnaireDO;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnaireItemDO;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnaireOptionDO;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnairePublishDO;
import com.kyx.service.hr.dal.mysql.questionnaire.QuestionnaireAnswerMapper;
import com.kyx.service.hr.dal.mysql.questionnaire.QuestionnaireAssignmentMapper;
import com.kyx.service.hr.dal.mysql.questionnaire.QuestionnaireItemMapper;
import com.kyx.service.hr.dal.mysql.questionnaire.QuestionnaireMapper;
import com.kyx.service.hr.dal.mysql.questionnaire.QuestionnaireOptionMapper;
import com.kyx.service.hr.dal.mysql.questionnaire.QuestionnairePublishMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.kyx.service.hr.enums.ErrorCodeConstants.QUESTIONNAIRE_ASSIGNMENT_ALREADY_SUBMITTED;
import static com.kyx.service.hr.enums.ErrorCodeConstants.QUESTIONNAIRE_ASSIGNMENT_FORBIDDEN;
import static com.kyx.service.hr.enums.ErrorCodeConstants.QUESTIONNAIRE_ASSIGNMENT_NOT_EXISTS;
import static com.kyx.service.hr.enums.ErrorCodeConstants.QUESTIONNAIRE_PUBLISH_ENDED;

/**
 * HR 问卷答案 Service 实现类
 *
 * @author MK
 */
@Service
@Validated
@Slf4j
public class QuestionnaireAnswerServiceImpl implements QuestionnaireAnswerService {

    @Resource
    private QuestionnaireAnswerMapper answerMapper;
    @Resource
    private QuestionnaireAssignmentMapper assignmentMapper;
    @Resource
    private QuestionnaireMapper questionnaireMapper;
    @Resource
    private QuestionnaireItemMapper itemMapper;
    @Resource
    private QuestionnaireOptionMapper optionMapper;
    @Resource
    private QuestionnairePublishMapper publishMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitAnswers(QuestionnaireAnswerSubmitReqVO submitReqVO) {
        QuestionnaireAssignmentDO assignment = getAssignmentWithFallback(submitReqVO.getAssignmentId());
        validateSubmitPermission(assignment);
        if (assignment.getStatus() != null && assignment.getStatus() == 1) {
            throw ServiceExceptionUtil.exception(QUESTIONNAIRE_ASSIGNMENT_ALREADY_SUBMITTED);
        }
        if (assignment.getStatus() != null && assignment.getStatus() == 2) {
            throw ServiceExceptionUtil.invalidParamException("该评价关系已配置为无需评价，无需提交");
        }
        validateSubmitTime(assignment);
        List<QuestionnaireAnswerDO> answers = buildValidatedAnswers(submitReqVO, assignment);

        TenantUtils.executeIgnore(() -> {
            answerMapper.deleteByAssignmentId(submitReqVO.getAssignmentId());
            if (answers != null) {
                answers.forEach(answerMapper::insert);
            }
            QuestionnaireAssignmentDO updateObj = new QuestionnaireAssignmentDO();
            updateObj.setId(assignment.getId());
            updateObj.setStatus(1);
            assignmentMapper.updateById(updateObj);
            return null;
        });
    }

    private List<QuestionnaireAnswerDO> buildValidatedAnswers(QuestionnaireAnswerSubmitReqVO submitReqVO,
                                                               QuestionnaireAssignmentDO assignment) {
        if (assignment == null || submitReqVO == null) {
            throw ServiceExceptionUtil.invalidParamException("问卷提交内容不能为空");
        }
        if (!Objects.equals(assignment.getQuestionnaireId(), submitReqVO.getQuestionnaireId())) {
            throw ServiceExceptionUtil.invalidParamException("问卷和分配记录不匹配");
        }

        QuestionnaireDO questionnaire = getQuestionnaireWithFallback(submitReqVO.getQuestionnaireId());
        List<QuestionnaireItemDO> items = getQuestionnaireItemsWithFallback(submitReqVO.getQuestionnaireId());
        if (items.isEmpty()) {
            throw ServiceExceptionUtil.invalidParamException("问卷没有题目，不能提交");
        }

        Map<Long, QuestionnaireItemDO> itemMap = items.stream()
                .filter(item -> item != null && item.getId() != null)
                .collect(Collectors.toMap(QuestionnaireItemDO::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<Long, List<QuestionnaireOptionDO>> optionMap = loadOptionMap(itemMap.keySet());
        Map<Long, QuestionnaireAnswerItemSaveReqVO> answerReqMap = buildAnswerReqMap(submitReqVO.getAnswers(), itemMap);
        String multiScoreMode = parseMultiScoreMode(questionnaire != null ? questionnaire.getTargetRuleJson() : null);

        List<QuestionnaireAnswerDO> answers = new ArrayList<>();
        for (QuestionnaireItemDO item : items) {
            if (item == null || item.getId() == null) {
                continue;
            }
            QuestionnaireAnswerItemSaveReqVO answerReq = answerReqMap.get(item.getId());
            SanitizedAnswer sanitized = sanitizeAnswer(item, optionMap.get(item.getId()), answerReq, multiScoreMode);
            if (!sanitized.isAnswered()) {
                if (Boolean.TRUE.equals(item.getRequired())) {
                    throw ServiceExceptionUtil.invalidParamException("题目「" + item.getTitle() + "」为必填");
                }
                continue;
            }

            QuestionnaireAnswerDO answer = new QuestionnaireAnswerDO();
            answer.setAssignmentId(submitReqVO.getAssignmentId());
            answer.setQuestionnaireId(submitReqVO.getQuestionnaireId());
            answer.setPublishId(assignment.getPublishId());
            answer.setTenantId(assignment.getTenantId());
            answer.setItemId(item.getId());
            answer.setAnswerText(sanitized.getAnswerText());
            answer.setAnswerJson(sanitized.getAnswerJson());
            answer.setAnswerScore(sanitized.getAnswerScore());
            answers.add(answer);
        }
        return answers;
    }

    private Map<Long, QuestionnaireAnswerItemSaveReqVO> buildAnswerReqMap(List<QuestionnaireAnswerItemSaveReqVO> answerReqs,
                                                                           Map<Long, QuestionnaireItemDO> itemMap) {
        Map<Long, QuestionnaireAnswerItemSaveReqVO> result = new LinkedHashMap<>();
        if (answerReqs == null) {
            return result;
        }
        for (QuestionnaireAnswerItemSaveReqVO answerReq : answerReqs) {
            if (answerReq == null || answerReq.getItemId() == null) {
                continue;
            }
            if (!itemMap.containsKey(answerReq.getItemId())) {
                throw ServiceExceptionUtil.invalidParamException("提交了不属于当前问卷的题目");
            }
            if (result.containsKey(answerReq.getItemId())) {
                throw ServiceExceptionUtil.invalidParamException("同一道题不能重复提交答案");
            }
            result.put(answerReq.getItemId(), answerReq);
        }
        return result;
    }

    private SanitizedAnswer sanitizeAnswer(QuestionnaireItemDO item,
                                           List<QuestionnaireOptionDO> options,
                                           QuestionnaireAnswerItemSaveReqVO answerReq,
                                           String multiScoreMode) {
        SanitizedAnswer sanitized = new SanitizedAnswer();
        if (answerReq == null) {
            return sanitized;
        }
        String itemType = item.getItemType();
        if ("single".equals(itemType)) {
            List<String> values = parseAnswerValues(answerReq.getAnswerJson(), false);
            if (values.isEmpty()) {
                return sanitized;
            }
            QuestionnaireOptionDO option = findOption(options, values.get(0));
            if (option == null) {
                throw ServiceExceptionUtil.invalidParamException("题目「" + item.getTitle() + "」包含无效选项");
            }
            sanitized.setAnswerJson(JsonUtils.toJsonString(option.getOptionText()));
            sanitized.setAnswerScore(toSafeScore(option.getOptionScore()));
            sanitized.setAnswered(true);
            return sanitized;
        }
        if ("multi".equals(itemType)) {
            List<String> values = parseAnswerValues(answerReq.getAnswerJson(), true);
            if (values.isEmpty()) {
                return sanitized;
            }
            List<String> selected = new ArrayList<>();
            List<Integer> scores = new ArrayList<>();
            for (String value : values) {
                QuestionnaireOptionDO option = findOption(options, value);
                if (option == null) {
                    throw ServiceExceptionUtil.invalidParamException("题目「" + item.getTitle() + "」包含无效选项");
                }
                if (!selected.contains(option.getOptionText())) {
                    selected.add(option.getOptionText());
                    scores.add(toSafeScore(option.getOptionScore()));
                }
            }
            sanitized.setAnswerJson(JsonUtils.toJsonString(selected));
            sanitized.setAnswerScore(calculateMultiScore(scores, multiScoreMode));
            sanitized.setAnswered(!selected.isEmpty());
            return sanitized;
        }
        if ("score".equals(itemType) || "score_text".equals(itemType)) {
            Integer score = answerReq.getAnswerScore();
            if (score == null) {
                return sanitized;
            }
            int maxScore = item.getMaxScore() == null || item.getMaxScore() <= 0 ? 10 : item.getMaxScore();
            if (score < 0 || score > maxScore) {
                throw ServiceExceptionUtil.invalidParamException("题目「" + item.getTitle() + "」评分超出范围");
            }
            sanitized.setAnswerScore(score);
            sanitized.setAnswerText(trimToNull(answerReq.getAnswerText()));
            sanitized.setAnswered(true);
            return sanitized;
        }

        String text = trimToNull(answerReq.getAnswerText());
        if (text == null) {
            return sanitized;
        }
        sanitized.setAnswerText(text);
        sanitized.setAnswered(true);
        return sanitized;
    }

    private QuestionnaireDO getQuestionnaireWithFallback(Long questionnaireId) {
        QuestionnaireDO questionnaire = questionnaireMapper.selectById(questionnaireId);
        if (questionnaire == null) {
            questionnaire = TenantUtils.executeIgnore(() -> questionnaireMapper.selectById(questionnaireId));
        }
        return questionnaire;
    }

    private List<QuestionnaireItemDO> getQuestionnaireItemsWithFallback(Long questionnaireId) {
        List<QuestionnaireItemDO> items = itemMapper.selectListByQuestionnaireId(questionnaireId);
        if (items == null || items.isEmpty()) {
            items = TenantUtils.executeIgnore(() -> itemMapper.selectListByQuestionnaireId(questionnaireId));
        }
        return items == null ? Collections.emptyList() : items;
    }

    private Map<Long, List<QuestionnaireOptionDO>> loadOptionMap(Set<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<QuestionnaireOptionDO> options = optionMapper.selectListByItemIds(itemIds);
        if (options == null || options.isEmpty()) {
            options = TenantUtils.executeIgnore(() -> optionMapper.selectListByItemIds(itemIds));
        }
        if (options == null || options.isEmpty()) {
            return Collections.emptyMap();
        }
        return options.stream()
                .filter(Objects::nonNull)
                .filter(option -> option.getItemId() != null)
                .collect(Collectors.groupingBy(QuestionnaireOptionDO::getItemId));
    }

    private QuestionnaireOptionDO findOption(List<QuestionnaireOptionDO> options, String value) {
        if (options == null || value == null) {
            return null;
        }
        String normalized = value.trim();
        for (QuestionnaireOptionDO option : options) {
            if (option != null && normalized.equals(trimToEmpty(option.getOptionText()))) {
                return option;
            }
        }
        return null;
    }

    private Integer calculateMultiScore(List<Integer> scores, String multiScoreMode) {
        if (scores == null || scores.isEmpty() || "none".equals(multiScoreMode)) {
            return null;
        }
        if ("sum".equals(multiScoreMode)) {
            return scores.stream().mapToInt(Integer::intValue).sum();
        }
        if ("avg".equals(multiScoreMode)) {
            return Math.round(scores.stream().mapToInt(Integer::intValue).sum() * 1.0f / scores.size());
        }
        return scores.stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    private List<String> parseAnswerValues(String answerJson, boolean allowMulti) {
        String value = trimToNull(answerJson);
        if (value == null) {
            return Collections.emptyList();
        }
        Object parsed = value;
        if (JsonUtils.isJson(value) || isJsonStringLiteral(value)) {
            try {
                parsed = JsonUtils.parseObject(value, Object.class);
            } catch (Exception ignored) {
                parsed = value;
            }
        }
        List<String> result = new ArrayList<>();
        if (parsed instanceof List) {
            if (!allowMulti) {
                throw ServiceExceptionUtil.invalidParamException("单选题不能提交多个选项");
            }
            for (Object item : (List<?>) parsed) {
                String text = trimToNull(item == null ? null : String.valueOf(item));
                if (text != null) {
                    result.add(text);
                }
            }
            return result;
        }
        String text = trimToNull(parsed == null ? null : String.valueOf(parsed));
        if (text != null) {
            result.add(text);
        }
        return result;
    }

    private boolean isJsonStringLiteral(String value) {
        return value != null && value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"");
    }

    private String parseMultiScoreMode(String targetRuleJson) {
        if (targetRuleJson == null || targetRuleJson.trim().isEmpty()) {
            return "none";
        }
        try {
            Map<String, Object> rule = JsonUtils.parseObject(targetRuleJson, Map.class);
            if (rule == null) {
                return "none";
            }
            Object scoreConfigObj = rule.get("scoreConfig");
            if (!(scoreConfigObj instanceof Map)) {
                return "none";
            }
            Object modeObj = ((Map<?, ?>) scoreConfigObj).get("multiScoreMode");
            String mode = modeObj == null ? null : String.valueOf(modeObj);
            if ("max".equals(mode) || "sum".equals(mode) || "avg".equals(mode)) {
                return mode;
            }
        } catch (Exception ignored) {
            // ignore invalid json
        }
        return "none";
    }

    private int toSafeScore(Integer value) {
        return value == null || value < 0 ? 0 : value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String trimToEmpty(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? "" : trimmed;
    }

    @Override
    public List<QuestionnaireAnswerRespVO> getAnswersByAssignment(Long assignmentId) {
        List<QuestionnaireAnswerDO> answerList;
        try {
            answerList = answerMapper.selectListByAssignmentId(assignmentId);
        } catch (Exception ex) {
            log.warn("getAnswersByAssignment direct query failed, fallback ignore tenant, assignmentId={}", assignmentId, ex);
            answerList = null;
        }
        if (answerList == null || answerList.isEmpty()) {
            answerList = TenantUtils.executeIgnore(() -> answerMapper.selectListByAssignmentId(assignmentId));
            if (answerList == null || answerList.isEmpty()) {
                return Collections.emptyList();
            }
        }

        Set<Long> itemIds = answerList.stream()
                .map(QuestionnaireAnswerDO::getItemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, QuestionnaireItemDO> itemMap = Collections.emptyMap();
        if (!itemIds.isEmpty()) {
            try {
                itemMap = itemMapper.selectBatchIds(itemIds).stream()
                        .collect(Collectors.toMap(QuestionnaireItemDO::getId, item -> item, (left, right) -> left));
            } catch (Exception ex) {
                log.warn("getAnswersByAssignment load items failed, fallback ignore tenant, assignmentId={}", assignmentId, ex);
                itemMap = Collections.emptyMap();
            }
            if (itemMap.isEmpty()) {
                Map<Long, QuestionnaireItemDO> ignoreItemMap = TenantUtils.executeIgnore(() -> itemMapper.selectBatchIds(itemIds)
                        .stream()
                        .collect(Collectors.toMap(QuestionnaireItemDO::getId, item -> item, (left, right) -> left)));
                if (ignoreItemMap != null && !ignoreItemMap.isEmpty()) {
                    itemMap = ignoreItemMap;
                }
            }
        }

        Map<Long, QuestionnaireItemDO> itemMapping = itemMap;
        answerList.sort(Comparator
                .comparingInt((QuestionnaireAnswerDO answer) -> {
                    QuestionnaireItemDO item = itemMapping.get(answer.getItemId());
                    return item != null && item.getSortNo() != null ? item.getSortNo() : Integer.MAX_VALUE;
                })
                .thenComparing(answer -> answer.getItemId() == null ? Long.MAX_VALUE : answer.getItemId()));

        return BeanUtils.toBean(answerList, QuestionnaireAnswerRespVO.class, answerResp -> {
            QuestionnaireItemDO item = itemMapping.get(answerResp.getItemId());
            if (item != null) {
                answerResp.setItemTitle(item.getTitle());
            }
        });
    }

    private QuestionnaireAssignmentDO getAssignmentWithFallback(Long assignmentId) {
        QuestionnaireAssignmentDO assignment;
        try {
            assignment = assignmentMapper.selectById(assignmentId);
        } catch (Exception ex) {
            log.warn("getAssignmentWithFallback direct query failed, fallback ignore tenant, assignmentId={}", assignmentId, ex);
            assignment = null;
        }
        if (assignment != null) {
            return assignment;
        }
        assignment = TenantUtils.executeIgnore(() -> assignmentMapper.selectById(assignmentId));
        if (assignment == null) {
            throw ServiceExceptionUtil.exception(QUESTIONNAIRE_ASSIGNMENT_NOT_EXISTS);
        }
        return assignment;
    }

    private void validateSubmitPermission(QuestionnaireAssignmentDO assignment) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null || assignment == null || !Objects.equals(loginUserId, assignment.getEvaluatorId())) {
            throw ServiceExceptionUtil.exception(QUESTIONNAIRE_ASSIGNMENT_FORBIDDEN);
        }
    }

    private void validateSubmitTime(QuestionnaireAssignmentDO assignment) {
        LocalDateTime now = LocalDateTime.now();
        if (assignment.getBatchStartAt() != null && now.isBefore(assignment.getBatchStartAt())) {
            throw ServiceExceptionUtil.exception(QUESTIONNAIRE_PUBLISH_ENDED);
        }
        if (assignment.getBatchEndAt() != null && !now.isBefore(assignment.getBatchEndAt())) {
            throw ServiceExceptionUtil.exception(QUESTIONNAIRE_PUBLISH_ENDED);
        }
        QuestionnairePublishDO publish = getPublishWithFallback(assignment.getPublishId());
        if (publish == null) {
            return;
        }
        if (publish.getStatus() != null && publish.getStatus() == 2) {
            throw ServiceExceptionUtil.exception(QUESTIONNAIRE_PUBLISH_ENDED);
        }
        if (publish.getDeadlineAt() != null && !now.isBefore(publish.getDeadlineAt())) {
            throw ServiceExceptionUtil.exception(QUESTIONNAIRE_PUBLISH_ENDED);
        }
    }

    private QuestionnairePublishDO getPublishWithFallback(Long publishId) {
        if (publishId == null) {
            return null;
        }
        QuestionnairePublishDO publish;
        try {
            publish = publishMapper.selectById(publishId);
        } catch (Exception ex) {
            log.warn("getPublishWithFallback direct query failed, fallback ignore tenant, publishId={}", publishId, ex);
            publish = null;
        }
        if (publish != null) {
            return publish;
        }
        return TenantUtils.executeIgnore(() -> publishMapper.selectById(publishId));
    }

    @Data
    private static class SanitizedAnswer {
        private boolean answered;
        private String answerText;
        private Integer answerScore;
        private String answerJson;
    }

}
