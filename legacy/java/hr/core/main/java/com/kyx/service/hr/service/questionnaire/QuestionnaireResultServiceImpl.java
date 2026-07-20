package com.kyx.service.hr.service.questionnaire;

import com.kyx.foundation.common.util.json.JsonUtils;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireItemStatRespVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireResultExportRespVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireResultRespVO;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnaireAnswerDO;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnaireAssignmentDO;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnaireDO;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnaireItemDO;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnaireOptionDO;
import com.kyx.service.hr.dal.mysql.questionnaire.QuestionnaireAnswerMapper;
import com.kyx.service.hr.dal.mysql.questionnaire.QuestionnaireAssignmentMapper;
import com.kyx.service.hr.dal.mysql.questionnaire.QuestionnaireItemMapper;
import com.kyx.service.hr.dal.mysql.questionnaire.QuestionnaireMapper;
import com.kyx.service.hr.dal.mysql.questionnaire.QuestionnaireOptionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * HR 问卷结果 Service 实现
 *
 * @author MK
 */
@Service
@Slf4j
public class QuestionnaireResultServiceImpl implements QuestionnaireResultService {

    private static final int ASSIGNMENT_STATUS_NOT_REQUIRED = 2;

    @Resource
    private QuestionnaireAssignmentMapper assignmentMapper;
    @Resource
    private QuestionnaireAnswerMapper answerMapper;
    @Resource
    private QuestionnaireItemMapper itemMapper;
    @Resource
    private QuestionnaireOptionMapper optionMapper;
    @Resource
    private QuestionnaireMapper questionnaireMapper;

    @Override
    public List<QuestionnaireResultRespVO> getResultList(Long publishId, Integer batchNo) {
        if (publishId == null) {
            return Collections.emptyList();
        }
        List<QuestionnaireAssignmentDO> assignments = assignmentMapper.selectListByPublishIdAndBatchNo(publishId, batchNo);
        if (assignments == null || assignments.isEmpty()) {
            return Collections.emptyList();
        }
        assignments = assignments.stream()
                .filter(Objects::nonNull)
                .filter(assignment -> !Objects.equals(assignment.getStatus(), ASSIGNMENT_STATUS_NOT_REQUIRED))
                .collect(Collectors.toList());
        if (assignments.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> submittedAssignmentIds = assignments.stream()
                .filter(assignment -> Objects.equals(assignment.getStatus(), 1))
                .map(QuestionnaireAssignmentDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<QuestionnaireAnswerDO> answers = submittedAssignmentIds.isEmpty()
                ? Collections.emptyList()
                : answerMapper.selectListByAssignmentIds(submittedAssignmentIds);
        Map<Long, List<QuestionnaireAnswerDO>> answerMapByAssignment = answers.stream()
                .filter(Objects::nonNull)
                .filter(answer -> answer.getAssignmentId() != null)
                .collect(Collectors.groupingBy(QuestionnaireAnswerDO::getAssignmentId));

        Set<Long> itemIds = answers.stream()
                .map(QuestionnaireAnswerDO::getItemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> itemTypeMap = itemIds.isEmpty()
                ? Collections.emptyMap()
                : itemMapper.selectBatchIds(itemIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(QuestionnaireItemDO::getId, QuestionnaireItemDO::getItemType, (l, r) -> l));
        Map<Long, QuestionnaireDO> questionnaireMap = loadQuestionnaireMap(assignments);

        Map<Long, QuestionnaireResultRespVO> resultMap = new LinkedHashMap<>();
        for (QuestionnaireAssignmentDO assignment : assignments) {
            boolean peerQuestionnaire = isPeerQuestionnaire(questionnaireMap.get(assignment.getQuestionnaireId()));
            List<QuestionnaireAnswerDO> assignmentAnswers =
                    answerMapByAssignment.getOrDefault(assignment.getId(), Collections.emptyList());
            double total = assignmentAnswers.stream()
                    .filter(answer -> answer.getAnswerScore() != null)
                    .filter(answer -> {
                        String itemType = itemTypeMap.get(answer.getItemId());
                        return !"text".equals(itemType) && !"blank".equals(itemType);
                    })
                    .mapToDouble(answer -> answer.getAnswerScore().doubleValue())
                    .sum();
            Long subjectId = peerQuestionnaire ? assignment.getTargetId() : assignment.getEvaluatorId();
            String subjectName = peerQuestionnaire ? assignment.getTargetName() : assignment.getEvaluatorName();
            Long resultKey = subjectId != null ? subjectId : assignment.getId();
            QuestionnaireResultRespVO result = resultMap.computeIfAbsent(resultKey, id -> {
                QuestionnaireResultRespVO resp = new QuestionnaireResultRespVO();
                resp.setTargetId(subjectId);
                resp.setTargetName(subjectName);
                resp.setRole(assignment.getRole());
                resp.setTotalScore(0.0);
                resp.setAvgScore(0.0);
                resp.setAssignmentCount(0);
                resp.setTotalAssignmentCount(0);
                resp.setPendingCount(0);
                resp.setAvgScoreWithPendingAsZero(0.0);
                return resp;
            });
            result.setTotalAssignmentCount((result.getTotalAssignmentCount() == null ? 0 : result.getTotalAssignmentCount()) + 1);
            if (Objects.equals(assignment.getStatus(), 1)) {
                result.setTotalScore(result.getTotalScore() + total);
                result.setAssignmentCount((result.getAssignmentCount() == null ? 0 : result.getAssignmentCount()) + 1);
            }
        }
        resultMap.values().forEach(r -> {
            int submittedCount = r.getAssignmentCount() == null ? 0 : r.getAssignmentCount();
            int totalCount = r.getTotalAssignmentCount() == null ? 0 : r.getTotalAssignmentCount();
            r.setPendingCount(Math.max(totalCount - submittedCount, 0));
            if (submittedCount > 0) {
                r.setAvgScore(r.getTotalScore() / submittedCount);
            }
            if (totalCount > 0) {
                r.setAvgScoreWithPendingAsZero(r.getTotalScore() / totalCount);
            }
        });
        return resultMap.values().stream()
                .sorted(Comparator.comparing(QuestionnaireResultRespVO::getAvgScoreWithPendingAsZero,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(QuestionnaireResultRespVO::getTargetName,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    @Override
    public List<QuestionnaireResultExportRespVO> getResultExportList(Long publishId, Integer batchNo) {
        if (publishId == null) {
            return Collections.emptyList();
        }
        List<QuestionnaireAssignmentDO> assignments = assignmentMapper.selectListByPublishIdAndBatchNo(publishId, batchNo);
        if (assignments == null || assignments.isEmpty()) {
            return Collections.emptyList();
        }
        assignments.sort(Comparator.comparing((QuestionnaireAssignmentDO a) -> a.getBatchNo() == null ? Integer.MAX_VALUE : a.getBatchNo())
                .thenComparing(a -> a.getTargetName() == null ? "" : a.getTargetName())
                .thenComparing(a -> a.getEvaluatorName() == null ? "" : a.getEvaluatorName())
                .thenComparing(a -> a.getId() == null ? Long.MAX_VALUE : a.getId()));
        assignments = assignments.stream()
                .filter(Objects::nonNull)
                .filter(assignment -> !Objects.equals(assignment.getStatus(), ASSIGNMENT_STATUS_NOT_REQUIRED))
                .collect(Collectors.toList());
        List<Long> assignmentIds = assignments.stream().map(QuestionnaireAssignmentDO::getId).filter(Objects::nonNull).collect(Collectors.toList());
        if (assignmentIds.isEmpty()) return Collections.emptyList();

        List<QuestionnaireAnswerDO> allAnswers = answerMapper.selectListByAssignmentIds(assignmentIds);
        Map<Long, Map<Long, QuestionnaireAnswerDO>> answerByAssignmentAndItem = allAnswers.stream()
                .filter(Objects::nonNull).filter(a -> a.getAssignmentId() != null && a.getItemId() != null)
                .collect(Collectors.groupingBy(QuestionnaireAnswerDO::getAssignmentId,
                        Collectors.toMap(QuestionnaireAnswerDO::getItemId, a -> a, (l, r) -> l)));
        List<Long> questionnaireIds = assignments.stream().map(QuestionnaireAssignmentDO::getQuestionnaireId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<QuestionnaireItemDO> allItems = questionnaireIds.isEmpty() ? Collections.emptyList() : itemMapper.selectListByQuestionnaireIds(questionnaireIds);
        Map<Long, List<QuestionnaireItemDO>> itemsByQuestionnaireId = allItems.stream()
                .filter(Objects::nonNull).collect(Collectors.groupingBy(QuestionnaireItemDO::getQuestionnaireId));
        Map<Long, String> itemTypeMap = allItems.stream().filter(Objects::nonNull)
                .collect(Collectors.toMap(QuestionnaireItemDO::getId, QuestionnaireItemDO::getItemType, (l, r) -> l));
        Map<Long, Integer> totalScoreByAssignment = assignments.stream()
                .filter(a -> a != null && a.getId() != null)
                .collect(Collectors.toMap(QuestionnaireAssignmentDO::getId,
                        a -> calcAssignmentTotalScore(answerByAssignmentAndItem.get(a.getId()), itemTypeMap),
                        (l, r) -> l));
        Map<Long, QuestionnaireDO> questionnaireMap = loadQuestionnaireMap(assignments);

        List<QuestionnaireResultExportRespVO> result = new ArrayList<>();
        for (QuestionnaireAssignmentDO assignment : assignments) {
            if (assignment == null || assignment.getId() == null) continue;
            List<QuestionnaireItemDO> items = itemsByQuestionnaireId.getOrDefault(assignment.getQuestionnaireId(), Collections.emptyList());
            Map<Long, QuestionnaireAnswerDO> answerByItem = answerByAssignmentAndItem.getOrDefault(assignment.getId(), Collections.emptyMap());
            QuestionnaireDO questionnaire = questionnaireMap.get(assignment.getQuestionnaireId());
            if (items.isEmpty()) {
                result.add(buildExportRow(assignment, questionnaire, totalScoreByAssignment.getOrDefault(assignment.getId(), 0), 0, null, null));
                continue;
            }
            for (QuestionnaireItemDO item : items) {
                QuestionnaireAnswerDO answer = item == null ? null : answerByItem.get(item.getId());
                result.add(buildExportRow(assignment, questionnaire, totalScoreByAssignment.getOrDefault(assignment.getId(), 0), items.size(), item, answer));
            }
        }
        result.sort(Comparator.comparing((QuestionnaireResultExportRespVO r) -> r.getItemSortNo() == null ? Integer.MAX_VALUE : r.getItemSortNo())
                .thenComparing(r -> r.getItemId() == null ? Long.MAX_VALUE : r.getItemId())
                .thenComparing(r -> r.getBatchNo() == null ? Integer.MAX_VALUE : r.getBatchNo())
                .thenComparing(r -> r.getTargetName() == null ? "" : r.getTargetName())
                .thenComparing(r -> r.getEvaluatorName() == null ? "" : r.getEvaluatorName()));
        return result;
    }

    @Override
    public List<QuestionnaireItemStatRespVO> getItemStats(Long publishId, Long questionnaireId) {
        if (questionnaireId == null) {
            return Collections.emptyList();
        }
        List<QuestionnaireItemDO> items = itemMapper.selectListByQuestionnaireId(questionnaireId);
        if (items.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> itemIds = items.stream().map(QuestionnaireItemDO::getId).collect(Collectors.toList());
        List<QuestionnaireOptionDO> options = optionMapper.selectListByItemIds(itemIds);
        Map<Long, List<QuestionnaireOptionDO>> optionMap = options.stream()
                .collect(Collectors.groupingBy(QuestionnaireOptionDO::getItemId));
        // 按 publishId 过滤答案
        List<QuestionnaireAnswerDO> answers = publishId != null
                ? answerMapper.selectListByPublishId(publishId)
                : answerMapper.selectListByQuestionnaireId(questionnaireId);
        Map<Long, List<QuestionnaireAnswerDO>> answerMap = answers.stream()
                .collect(Collectors.groupingBy(QuestionnaireAnswerDO::getItemId));

        List<QuestionnaireItemStatRespVO> result = new ArrayList<>();
        for (QuestionnaireItemDO item : items) {
            QuestionnaireItemStatRespVO stat = new QuestionnaireItemStatRespVO();
            stat.setItemId(item.getId());
            stat.setTitle(item.getTitle());
            stat.setItemType(item.getItemType());

            List<QuestionnaireAnswerDO> itemAnswers = answerMap.getOrDefault(item.getId(), Collections.emptyList());

            if ("single".equals(item.getItemType()) || "multi".equals(item.getItemType())) {
                Map<String, Integer> countMap = new LinkedHashMap<>();
                List<QuestionnaireOptionDO> itemOptions = optionMap.getOrDefault(item.getId(), Collections.emptyList());
                for (QuestionnaireOptionDO opt : itemOptions) {
                    countMap.put(opt.getOptionText(), 0);
                }
                for (QuestionnaireAnswerDO answer : itemAnswers) {
                    List<String> selected = parseAnswerList(answer.getAnswerJson());
                    for (String sel : selected) {
                        countMap.compute(sel, (k, v) -> v == null ? 1 : v + 1);
                    }
                }
                List<QuestionnaireItemStatRespVO.OptionStat> optionStats = new ArrayList<>();
                for (Map.Entry<String, Integer> entry : countMap.entrySet()) {
                    QuestionnaireItemStatRespVO.OptionStat os = new QuestionnaireItemStatRespVO.OptionStat();
                    os.setOptionText(entry.getKey());
                    os.setCount(entry.getValue());
                    optionStats.add(os);
                }
                stat.setOptionStats(optionStats);
            }

            if ("score".equals(item.getItemType()) || "score_text".equals(item.getItemType())) {
                List<Integer> scores = itemAnswers.stream()
                        .map(QuestionnaireAnswerDO::getAnswerScore)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                if (!scores.isEmpty()) {
                    int sum = scores.stream().mapToInt(Integer::intValue).sum();
                    stat.setScoreCount(scores.size());
                    stat.setAvgScore(sum * 1.0 / scores.size());
                    stat.setMinScore(scores.stream().min(Integer::compareTo).orElse(null));
                    stat.setMaxScore(scores.stream().max(Integer::compareTo).orElse(null));
                } else {
                    stat.setScoreCount(0);
                }
            }

            if ("text".equals(item.getItemType()) || "score_text".equals(item.getItemType()) || "blank".equals(item.getItemType())) {
                List<String> texts = itemAnswers.stream()
                        .map(QuestionnaireAnswerDO::getAnswerText)
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
                stat.setTextCount(texts.size());
                if (!texts.isEmpty()) {
                    stat.setTextSamples(texts.subList(0, Math.min(20, texts.size())));
                }
            }

            result.add(stat);
        }
        return result;
    }

    private QuestionnaireResultExportRespVO buildExportRow(QuestionnaireAssignmentDO assignment, QuestionnaireDO questionnaire, Integer totalScore,
                                                           Integer totalItemCount, QuestionnaireItemDO item,
                                                           QuestionnaireAnswerDO answer) {
        boolean peerQuestionnaire = isPeerQuestionnaire(questionnaire);
        QuestionnaireResultExportRespVO row = new QuestionnaireResultExportRespVO();
        row.setBatchNo(assignment.getBatchNo());
        row.setBatchLabel(assignment.getBatchLabel());
        row.setQuestionnaireName(questionnaire != null ? questionnaire.getName() : null);
        row.setQuestionnaireType(questionnaire != null ? questionnaire.getType() : null);
        row.setAssignmentId(assignment.getId());
        row.setEvaluatorId(assignment.getEvaluatorId());
        row.setEvaluatorName(assignment.getEvaluatorName());
        row.setTargetId(peerQuestionnaire ? assignment.getTargetId() : null);
        row.setTargetName(peerQuestionnaire ? assignment.getTargetName() : null);
        row.setStatusText(assignment.getStatus() != null && assignment.getStatus() == 1 ? "已提交" : "未填写");
        row.setSubmitTime(assignment.getStatus() != null && assignment.getStatus() == 1 ? assignment.getUpdateTime() : null);
        row.setItemId(item != null ? item.getId() : null);
        row.setItemSortNo(item != null ? item.getSortNo() : null);
        row.setItemTitle(item != null ? item.getTitle() : null);
        row.setItemTypeText(resolveItemTypeText(item != null ? item.getItemType() : null));
        row.setAnswerContent(resolveAnswerContent(answer));
        row.setItemScore(answer != null ? answer.getAnswerScore() : null);
        row.setTotalScore(totalScore != null ? totalScore : 0);
        row.setTotalItemCount(totalItemCount != null ? totalItemCount : 0);
        return row;
    }

    private Map<Long, QuestionnaireDO> loadQuestionnaireMap(List<QuestionnaireAssignmentDO> assignments) {
        if (assignments == null || assignments.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> questionnaireIds = assignments.stream()
                .map(QuestionnaireAssignmentDO::getQuestionnaireId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (questionnaireIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return questionnaireMapper.selectBatchIds(questionnaireIds).stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(QuestionnaireDO::getId, item -> item, (left, right) -> left));
    }

    private boolean isPeerQuestionnaire(QuestionnaireDO questionnaire) {
        return questionnaire != null && "peer".equalsIgnoreCase(questionnaire.getType());
    }

    private int calcAssignmentTotalScore(Map<Long, QuestionnaireAnswerDO> answerByItem, Map<Long, String> itemTypeMap) {
        if (answerByItem == null || answerByItem.isEmpty()) {
            return 0;
        }
        return answerByItem.values().stream()
                .filter(Objects::nonNull)
                .filter(answer -> answer.getAnswerScore() != null)
                .filter(answer -> {
                    String itemType = itemTypeMap == null ? null : itemTypeMap.get(answer.getItemId());
                    return !"text".equals(itemType) && !"blank".equals(itemType);
                })
                .mapToInt(QuestionnaireAnswerDO::getAnswerScore)
                .sum();
    }

    private String resolveAnswerContent(QuestionnaireAnswerDO answer) {
        if (answer == null) {
            return "";
        }
        if (answer.getAnswerText() != null && !answer.getAnswerText().trim().isEmpty()) {
            return answer.getAnswerText().trim();
        }
        if (answer.getAnswerJson() != null && !answer.getAnswerJson().trim().isEmpty()) {
            List<String> list = parseAnswerList(answer.getAnswerJson());
            if (!list.isEmpty()) {
                return String.join(" / ", list);
            }
            return answer.getAnswerJson().trim();
        }
        if (answer.getAnswerScore() != null) {
            return String.valueOf(answer.getAnswerScore());
        }
        return "";
    }

    private String resolveItemTypeText(String itemType) {
        if (itemType == null) return "";
        if ("single".equals(itemType)) return "单选";
        if ("multi".equals(itemType)) return "多选";
        if ("score".equals(itemType)) return "打分";
        if ("text".equals(itemType)) return "文本";
        if ("score_text".equals(itemType)) return "打分+文本";
        if ("blank".equals(itemType)) return "填空";
        return itemType;
    }

    private List<String> parseAnswerList(String answerJson) {
        if (answerJson == null || answerJson.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String trimmed = answerJson.trim();
        if (JsonUtils.isJson(trimmed)) {
            try {
                Object obj = JsonUtils.parseObject(trimmed, Object.class);
                if (obj instanceof List) {
                    List<?> list = (List<?>) obj;
                    return list.stream().map(v -> v == null ? "" : String.valueOf(v)).collect(Collectors.toList());
                }
                if (obj != null) {
                    return Collections.singletonList(String.valueOf(obj));
                }
            } catch (Exception ignored) {
                // fallback
            }
        }
        return Collections.singletonList(trimmed);
    }

}
