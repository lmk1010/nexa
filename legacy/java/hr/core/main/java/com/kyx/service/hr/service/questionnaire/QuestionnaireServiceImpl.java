package com.kyx.service.hr.service.questionnaire;

import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.util.json.JsonUtils;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.collection.CollectionUtils;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnairePageReqVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireRespVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireSaveReqVO;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnaireAssignmentDO;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnaireDO;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnaireItemDO;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnaireOptionDO;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnairePublishDO;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnairePublicLinkDO;
import com.kyx.service.hr.dal.dataobject.tenant.TenantDO;
import com.kyx.service.hr.dal.mysql.questionnaire.QuestionnaireAnswerMapper;
import com.kyx.service.hr.dal.mysql.questionnaire.QuestionnaireAssignmentMapper;
import com.kyx.service.hr.dal.mysql.questionnaire.QuestionnaireItemMapper;
import com.kyx.service.hr.dal.mysql.questionnaire.QuestionnaireMapper;
import com.kyx.service.hr.dal.mysql.questionnaire.QuestionnaireOptionMapper;
import com.kyx.service.hr.dal.mysql.questionnaire.QuestionnairePublishMapper;
import com.kyx.service.hr.dal.mysql.questionnaire.QuestionnairePublicAnswerMapper;
import com.kyx.service.hr.dal.mysql.questionnaire.QuestionnairePublicLinkMapper;
import com.kyx.service.hr.dal.mysql.tenant.TenantMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.kyx.service.hr.enums.ErrorCodeConstants.QUESTIONNAIRE_CODE_DUPLICATE;
import static com.kyx.service.hr.enums.ErrorCodeConstants.QUESTIONNAIRE_NOT_EXISTS;

/**
 * HR 问卷管理 Service 实现类
 *
 * @author MK
 */
@Service
@Validated
@Slf4j
public class QuestionnaireServiceImpl implements QuestionnaireService {

    @Resource
    private QuestionnaireMapper questionnaireMapper;
    @Resource
    private QuestionnaireItemMapper questionnaireItemMapper;
    @Resource
    private QuestionnaireOptionMapper questionnaireOptionMapper;
    @Resource
    private QuestionnairePublishMapper questionnairePublishMapper;
    @Resource
    private QuestionnaireAssignmentMapper questionnaireAssignmentMapper;
    @Resource
    private QuestionnaireAnswerMapper questionnaireAnswerMapper;
    @Resource
    private QuestionnairePublicLinkMapper questionnairePublicLinkMapper;
    @Resource
    private QuestionnairePublicAnswerMapper questionnairePublicAnswerMapper;
    @Resource
    private TenantMapper tenantMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createQuestionnaire(QuestionnaireSaveReqVO createReqVO) {
        normalizeQuestionnaireSaveReq(createReqVO, null);
        validateQuestionnairePayload(createReqVO);
        validateQuestionnaireCodeUnique(null, createReqVO.getCode());

        QuestionnaireDO questionnaire = BeanUtils.toBean(createReqVO, QuestionnaireDO.class);
        normalizeEmptyJsonFields(questionnaire);
        questionnaireMapper.insert(questionnaire);
        saveItems(questionnaire.getId(), createReqVO.getItems());
        return questionnaire.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQuestionnaire(QuestionnaireSaveReqVO updateReqVO) {
        QuestionnaireDO exist = validateQuestionnaireExists(updateReqVO.getId());
        validateQuestionnaireEditable(updateReqVO.getId());
        normalizeQuestionnaireSaveReq(updateReqVO, exist);
        validateQuestionnairePayload(updateReqVO);
        validateQuestionnaireCodeUnique(exist.getId(), updateReqVO.getCode());

        QuestionnaireDO updateObj = BeanUtils.toBean(updateReqVO, QuestionnaireDO.class);
        normalizeEmptyJsonFields(updateObj);
        questionnaireMapper.updateById(updateObj);

        // 重建题目与选项
        List<QuestionnaireItemDO> items = questionnaireItemMapper.selectListByQuestionnaireId(updateReqVO.getId());
        List<Long> itemIds = CollectionUtils.convertList(items, QuestionnaireItemDO::getId);
        if (!itemIds.isEmpty()) {
            questionnaireOptionMapper.deleteByItemIds(itemIds);
        }
        questionnaireItemMapper.deleteByQuestionnaireId(updateReqVO.getId());
        saveItems(updateReqVO.getId(), updateReqVO.getItems());
    }

    @Override
    public void deleteQuestionnaire(Long id) {
        validateQuestionnaireExists(id);
        List<QuestionnairePublishDO> publishes = questionnairePublishMapper.selectListByQuestionnaireId(id);
        for (QuestionnairePublishDO publish : publishes) {
            clearPublishRelatedData(publish.getId());
        }
        questionnairePublishMapper.deleteByQuestionnaireId(id);

        List<QuestionnairePublicLinkDO> links = questionnairePublicLinkMapper.selectListByQuestionnaireId(id);
        if (!links.isEmpty()) {
            List<Long> linkIds = links.stream().map(QuestionnairePublicLinkDO::getId).collect(Collectors.toList());
            questionnairePublicAnswerMapper.deleteByLinkIds(linkIds);
        }
        questionnairePublicLinkMapper.deleteByQuestionnaireId(id);
        questionnairePublicAnswerMapper.deleteByQuestionnaireId(id);
        questionnaireAssignmentMapper.deleteByQuestionnaireId(id);
        questionnaireAnswerMapper.deleteByQuestionnaireId(id);

        List<QuestionnaireItemDO> items = questionnaireItemMapper.selectListByQuestionnaireId(id);
        List<Long> itemIds = CollectionUtils.convertList(items, QuestionnaireItemDO::getId);
        if (!itemIds.isEmpty()) {
            questionnaireOptionMapper.deleteByItemIds(itemIds);
        }
        questionnaireItemMapper.deleteByQuestionnaireId(id);
        questionnaireMapper.deleteById(id);
    }

    private QuestionnaireDO getQuestionnaireDO(Long id) {
        QuestionnaireDO questionnaire = questionnaireMapper.selectById(id);
        if (questionnaire == null) {
            questionnaire = TenantUtils.executeIgnore(() -> questionnaireMapper.selectById(id));
        }
        if (questionnaire == null) {
            throw ServiceExceptionUtil.exception(QUESTIONNAIRE_NOT_EXISTS);
        }
        return questionnaire;
    }

    private QuestionnaireRespVO buildQuestionnaireResp(QuestionnaireDO questionnaire) {
        QuestionnaireRespVO resp = BeanUtils.toBean(questionnaire, QuestionnaireRespVO.class);
        Long questionnaireId = questionnaire.getId();
        List<QuestionnaireItemDO> items = questionnaireItemMapper.selectListByQuestionnaireId(questionnaireId);
        if ((items == null || items.isEmpty()) && questionnaire.getTenantId() != null) {
            items = TenantUtils.executeIgnore(() -> questionnaireItemMapper.selectListByQuestionnaireId(questionnaireId));
        }
        List<Long> itemIds = CollectionUtils.convertList(items, QuestionnaireItemDO::getId);
        List<QuestionnaireOptionDO> options = itemIds.isEmpty()
                ? Collections.emptyList()
                : questionnaireOptionMapper.selectListByItemIds(itemIds);
        if (options.isEmpty() && !itemIds.isEmpty()) {
            options = TenantUtils.executeIgnore(() -> questionnaireOptionMapper.selectListByItemIds(itemIds));
        }

        Map<Long, List<QuestionnaireRespVO.Option>> optionMap = options.stream()
                .collect(Collectors.groupingBy(QuestionnaireOptionDO::getItemId,
                        Collectors.mapping(opt -> BeanUtils.toBean(opt, QuestionnaireRespVO.Option.class), Collectors.toList())));

        List<QuestionnaireRespVO.Item> itemRespList = items.stream().map(item -> {
            QuestionnaireRespVO.Item itemResp = BeanUtils.toBean(item, QuestionnaireRespVO.Item.class);
            itemResp.setOptions(optionMap.getOrDefault(item.getId(), new ArrayList<>()));
            return itemResp;
        }).collect(Collectors.toList());

        resp.setItems(itemRespList);
        return resp;
    }

    private boolean hasQuestionnaireAccess(Long questionnaireId, Long assignmentId, Long publishId, Long loginUserId) {
        if (questionnaireId == null || loginUserId == null) {
            return false;
        }
        if (assignmentId != null && assignmentId > 0) {
            QuestionnaireAssignmentDO assignment = questionnaireAssignmentMapper.selectById(assignmentId);
            if (assignment == null) {
                assignment = TenantUtils.executeIgnore(() -> questionnaireAssignmentMapper.selectById(assignmentId));
            }
            return assignment != null
                    && Objects.equals(assignment.getQuestionnaireId(), questionnaireId)
                    && Objects.equals(assignment.getEvaluatorId(), loginUserId);
        }
        if (publishId != null && publishId > 0) {
            Long count = questionnaireAssignmentMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<QuestionnaireAssignmentDO>()
                    .eq(QuestionnaireAssignmentDO::getQuestionnaireId, questionnaireId)
                    .eq(QuestionnaireAssignmentDO::getPublishId, publishId)
                    .eq(QuestionnaireAssignmentDO::getEvaluatorId, loginUserId));
            if (count == null || count <= 0) {
                count = TenantUtils.executeIgnore(() -> questionnaireAssignmentMapper.selectCount(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<QuestionnaireAssignmentDO>()
                                .eq(QuestionnaireAssignmentDO::getQuestionnaireId, questionnaireId)
                                .eq(QuestionnaireAssignmentDO::getPublishId, publishId)
                                .eq(QuestionnaireAssignmentDO::getEvaluatorId, loginUserId)));
            }
            return count != null && count > 0;
        }
        Long count = questionnaireAssignmentMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<QuestionnaireAssignmentDO>()
                .eq(QuestionnaireAssignmentDO::getQuestionnaireId, questionnaireId)
                .eq(QuestionnaireAssignmentDO::getEvaluatorId, loginUserId));
        if (count == null || count <= 0) {
            count = TenantUtils.executeIgnore(() -> questionnaireAssignmentMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<QuestionnaireAssignmentDO>()
                            .eq(QuestionnaireAssignmentDO::getQuestionnaireId, questionnaireId)
                            .eq(QuestionnaireAssignmentDO::getEvaluatorId, loginUserId)));
        }
        return count != null && count > 0;
    }

    private void clearPublishRelatedData(Long publishId) {
        questionnaireAssignmentMapper.deleteByPublishId(publishId);
        questionnaireAnswerMapper.deleteByPublishId(publishId);
        questionnairePublicAnswerMapper.deleteByPublishId(publishId);
        List<QuestionnairePublicLinkDO> links = questionnairePublicLinkMapper.selectListByPublishId(publishId);
        if (!links.isEmpty()) {
            List<Long> linkIds = links.stream().map(QuestionnairePublicLinkDO::getId).collect(Collectors.toList());
            questionnairePublicAnswerMapper.deleteByLinkIds(linkIds);
        }
        questionnairePublicLinkMapper.deleteByPublishId(publishId);
    }

    @Override
    public QuestionnaireRespVO getQuestionnaire(Long id) {
        QuestionnaireDO questionnaire = getQuestionnaireDO(id);
        return buildQuestionnaireResp(questionnaire);
    }

    @Override
    public QuestionnaireRespVO getAccessibleQuestionnaire(Long id, Long assignmentId, Long publishId) {
        Long loginUserId = com.kyx.foundation.security.core.util.SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null || !hasQuestionnaireAccess(id, assignmentId, publishId, loginUserId)) {
            throw ServiceExceptionUtil.exception(com.kyx.service.hr.enums.ErrorCodeConstants.QUESTIONNAIRE_ASSIGNMENT_FORBIDDEN);
        }
        QuestionnaireDO questionnaire = getQuestionnaireDO(id);
        return buildQuestionnaireResp(questionnaire);
    }

    @Override
    public PageResult<QuestionnaireRespVO> getQuestionnairePage(QuestionnairePageReqVO pageReqVO) {
        PageResult<QuestionnaireDO> pageResult = questionnaireMapper.selectPage(pageReqVO);
        List<QuestionnaireDO> list = pageResult.getList();
        if (list.isEmpty()) {
            return BeanUtils.toBean(pageResult, QuestionnaireRespVO.class);
        }

        List<Long> questionnaireIds = list.stream()
                .map(QuestionnaireDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<QuestionnaireItemDO> allItems = questionnaireIds.isEmpty()
                ? Collections.emptyList()
                : questionnaireItemMapper.selectListByQuestionnaireIds(questionnaireIds);
        Map<Long, List<QuestionnaireItemDO>> itemMap = allItems.stream()
                .collect(Collectors.groupingBy(QuestionnaireItemDO::getQuestionnaireId));

        List<Long> itemIds = allItems.stream()
                .map(QuestionnaireItemDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<QuestionnaireOptionDO> allOptions = itemIds.isEmpty()
                ? Collections.emptyList()
                : questionnaireOptionMapper.selectListByItemIds(itemIds);
        Map<Long, Integer> maxOptionScoreMap = new HashMap<>();
        Map<Long, Integer> sumOptionScoreMap = new HashMap<>();
        Map<Long, Integer> optionCountMap = new HashMap<>();
        for (QuestionnaireOptionDO option : allOptions) {
            if (option.getItemId() == null) {
                continue;
            }
            int score = option.getOptionScore() == null ? 0 : option.getOptionScore();
            maxOptionScoreMap.merge(option.getItemId(), score, Math::max);
            sumOptionScoreMap.merge(option.getItemId(), score, Integer::sum);
            optionCountMap.merge(option.getItemId(), 1, Integer::sum);
        }

        Map<Long, String> multiScoreModeMap = new HashMap<>();
        for (QuestionnaireDO questionnaire : list) {
            multiScoreModeMap.put(questionnaire.getId(), parseMultiScoreMode(questionnaire.getTargetRuleJson()));
        }

        // 批量获取租户名称
        List<Long> tenantIds = list.stream()
                .map(QuestionnaireDO::getTenantId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> tenantMap = new HashMap<>();
        if (!tenantIds.isEmpty()) {
            List<TenantDO> tenantList = tenantMapper.selectBatchIds(tenantIds);
            tenantMap = tenantList.stream()
                    .collect(Collectors.toMap(TenantDO::getId, TenantDO::getName));
        }

        final Map<Long, String> finalTenantMap = tenantMap;
        List<QuestionnaireRespVO> voList = BeanUtils.toBean(list, QuestionnaireRespVO.class, vo -> {
            if (vo.getTenantId() != null) {
                vo.setTenantName(finalTenantMap.get(vo.getTenantId()));
            }
            List<QuestionnaireItemDO> questionnaireItems = vo.getId() == null
                    ? Collections.emptyList()
                    : itemMap.getOrDefault(vo.getId(), Collections.emptyList());
            vo.setQuestionCount(questionnaireItems.size());
            int totalScore = 0;
            for (QuestionnaireItemDO item : questionnaireItems) {
                if ("score".equals(item.getItemType()) || "score_text".equals(item.getItemType())) {
                    totalScore += item.getMaxScore() == null ? 0 : item.getMaxScore();
                } else if ("single".equals(item.getItemType())) {
                    totalScore += maxOptionScoreMap.getOrDefault(item.getId(), 0);
                } else if ("multi".equals(item.getItemType())) {
                    String multiScoreMode = multiScoreModeMap.getOrDefault(vo.getId(), "none");
                    if ("max".equals(multiScoreMode)) {
                        totalScore += maxOptionScoreMap.getOrDefault(item.getId(), 0);
                    } else if ("sum".equals(multiScoreMode)) {
                        totalScore += sumOptionScoreMap.getOrDefault(item.getId(), 0);
                    } else if ("avg".equals(multiScoreMode)) {
                        int sumScore = sumOptionScoreMap.getOrDefault(item.getId(), 0);
                        int optionCount = optionCountMap.getOrDefault(item.getId(), 0);
                        totalScore += optionCount > 0 ? Math.round(sumScore * 1.0f / optionCount) : 0;
                    }
                }
            }
            vo.setTotalScore(totalScore);
        });
        return new PageResult<>(voList, pageResult.getTotal());
    }

    private QuestionnaireDO validateQuestionnaireExists(Long id) {
        QuestionnaireDO questionnaire = questionnaireMapper.selectById(id);
        if (questionnaire == null) {
            throw ServiceExceptionUtil.exception(QUESTIONNAIRE_NOT_EXISTS);
        }
        return questionnaire;
    }

    private void validateQuestionnaireEditable(Long id) {
        List<QuestionnairePublishDO> publishes = questionnairePublishMapper.selectListByQuestionnaireId(id);
        if (publishes == null || publishes.isEmpty()) {
            return;
        }
        boolean hasGeneratedPublish = publishes.stream()
                .filter(Objects::nonNull)
                .anyMatch(publish -> (publish.getStatus() != null && publish.getStatus() != 0)
                        || (publish.getGeneratedCount() != null && publish.getGeneratedCount() > 0)
                        || (publish.getCurrentBatchNo() != null && publish.getCurrentBatchNo() > 0));
        if (hasGeneratedPublish) {
            throw ServiceExceptionUtil.invalidParamException("问卷已发布或已有批次，不能直接修改题目；请复制问卷后重新发布，或在发布编辑中追加填写人");
        }
    }

    private void validateQuestionnaireCodeUnique(Long id, String code) {
        if (code == null || code.trim().isEmpty()) {
            return;
        }
        QuestionnaireDO questionnaire = questionnaireMapper.selectByCode(code);
        if (questionnaire == null) {
            return;
        }
        if (id == null) {
            throw ServiceExceptionUtil.exception(QUESTIONNAIRE_CODE_DUPLICATE);
        }
        if (!questionnaire.getId().equals(id)) {
            throw ServiceExceptionUtil.exception(QUESTIONNAIRE_CODE_DUPLICATE);
        }
    }

    private void saveItems(Long questionnaireId, List<com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireItemSaveReqVO> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireItemSaveReqVO itemReq : items) {
            QuestionnaireItemDO item = BeanUtils.toBean(itemReq, QuestionnaireItemDO.class);
            item.setQuestionnaireId(questionnaireId);
            questionnaireItemMapper.insert(item);
            if (itemReq.getOptions() == null || itemReq.getOptions().isEmpty()) {
                continue;
            }
            for (com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireOptionSaveReqVO optionReq : itemReq.getOptions()) {
                QuestionnaireOptionDO option = BeanUtils.toBean(optionReq, QuestionnaireOptionDO.class);
                option.setItemId(item.getId());
                questionnaireOptionMapper.insert(option);
            }
        }
    }

    private void normalizeQuestionnaireSaveReq(QuestionnaireSaveReqVO reqVO, QuestionnaireDO exist) {
        if (reqVO == null) {
            return;
        }
        reqVO.setName(trimToNull(reqVO.getName()));
        reqVO.setType(normalizeQuestionnaireType(reqVO.getType()));
        if (reqVO.getStatus() == null) {
            reqVO.setStatus(0);
        }
        reqVO.setRoleScope("both");
        reqVO.setPeriodStart(null);
        reqVO.setPeriodEnd(null);

        String code = trimToNull(reqVO.getCode());
        if (code == null && exist != null && exist.getCode() != null && !exist.getCode().trim().isEmpty()) {
            code = exist.getCode().trim();
        }
        if (code == null) {
            code = buildQuestionnaireCode(reqVO.getType(), reqVO.getName());
        }
        reqVO.setCode(code);

        List<com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireItemSaveReqVO> items = reqVO.getItems();
        if (items == null) {
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireItemSaveReqVO item = items.get(i);
            if (item == null) {
                continue;
            }
            item.setTitle(trimToNull(item.getTitle()));
            item.setItemType(normalizeItemType(item.getItemType()));
            item.setRequired(item.getRequired() == null || item.getRequired());
            item.setSortNo(i + 1);
            if (!isScoreInputItem(item.getItemType())) {
                item.setMaxScore(null);
            } else if (item.getMaxScore() == null || item.getMaxScore() < 0) {
                item.setMaxScore(0);
            }
            List<com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireOptionSaveReqVO> options = item.getOptions();
            if (options == null) {
                continue;
            }
            for (int j = 0; j < options.size(); j++) {
                com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireOptionSaveReqVO option = options.get(j);
                if (option == null) {
                    continue;
                }
                option.setOptionText(trimToNull(option.getOptionText()));
                option.setOptionScore(option.getOptionScore() == null || option.getOptionScore() < 0 ? 0 : option.getOptionScore());
                option.setSortNo(j + 1);
            }
        }
    }

    private void validateQuestionnairePayload(QuestionnaireSaveReqVO reqVO) {
        if (reqVO == null) {
            throw ServiceExceptionUtil.invalidParamException("问卷内容不能为空");
        }
        if (reqVO.getName() == null) {
            throw ServiceExceptionUtil.invalidParamException("请填写问卷名称");
        }
        if (reqVO.getItems() == null || reqVO.getItems().isEmpty()) {
            throw ServiceExceptionUtil.invalidParamException("请至少添加 1 道题目");
        }
        for (int i = 0; i < reqVO.getItems().size(); i++) {
            com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireItemSaveReqVO item = reqVO.getItems().get(i);
            if (item == null || item.getTitle() == null) {
                throw ServiceExceptionUtil.invalidParamException("第 " + (i + 1) + " 题标题不能为空");
            }
            if (isChoiceItem(item.getItemType())) {
                List<com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireOptionSaveReqVO> options = item.getOptions();
                if (options == null || options.size() < 2) {
                    throw ServiceExceptionUtil.invalidParamException("第 " + (i + 1) + " 题至少需要 2 个选项");
                }
                for (int j = 0; j < options.size(); j++) {
                    com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireOptionSaveReqVO option = options.get(j);
                    if (option == null || option.getOptionText() == null) {
                        throw ServiceExceptionUtil.invalidParamException("第 " + (i + 1) + " 题第 " + (j + 1) + " 个选项不能为空");
                    }
                }
            }
            if (isScoreInputItem(item.getItemType()) && (item.getMaxScore() == null || item.getMaxScore() <= 0)) {
                throw ServiceExceptionUtil.invalidParamException("第 " + (i + 1) + " 题最大分必须大于 0");
            }
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeQuestionnaireType(String type) {
        String value = trimToNull(type);
        if ("employee_impression".equals(value) || "exam".equals(value) || "peer".equals(value)) {
            return value;
        }
        return "peer";
    }

    private String normalizeItemType(String itemType) {
        String value = trimToNull(itemType);
        if ("single".equals(value) || "multi".equals(value) || "score".equals(value)
                || "text".equals(value) || "score_text".equals(value) || "blank".equals(value)) {
            return value;
        }
        return "single";
    }

    private boolean isChoiceItem(String itemType) {
        return "single".equals(itemType) || "multi".equals(itemType);
    }

    private boolean isScoreInputItem(String itemType) {
        return "score".equals(itemType) || "score_text".equals(itemType);
    }

    private String buildQuestionnaireCode(String type, String name) {
        String prefix = "peer";
        if ("employee_impression".equals(type)) {
            prefix = "survey";
        } else if ("exam".equals(type)) {
            prefix = "exam";
        }
        String rawName = name == null ? "questionnaire" : name.trim().toLowerCase(Locale.ROOT);
        String normalizedName = rawName.replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (normalizedName.isEmpty()) {
            normalizedName = "questionnaire";
        }
        if (normalizedName.length() > 24) {
            normalizedName = normalizedName.substring(0, 24);
        }
        String code = (prefix + "_" + normalizedName + "_" + System.currentTimeMillis()).toLowerCase(Locale.ROOT);
        if (code.length() > 64) {
            code = code.substring(0, 64);
        }
        int index = 1;
        String candidate = code;
        while (questionnaireMapper.selectByCode(candidate) != null) {
            String suffix = "_" + index++;
            int maxPrefixLength = Math.max(1, 64 - suffix.length());
            candidate = code.substring(0, Math.min(code.length(), maxPrefixLength)) + suffix;
        }
        return candidate;
    }

    private void normalizeEmptyJsonFields(QuestionnaireDO questionnaire) {
        if (questionnaire == null) {
            return;
        }
        questionnaire.setRoleScope("both");
        questionnaire.setPeriodStart(null);
        questionnaire.setPeriodEnd(null);
        String targetRuleJson = questionnaire.getTargetRuleJson();
        if (targetRuleJson == null || targetRuleJson.trim().isEmpty()) {
            questionnaire.setTargetRuleJson("{}");
        }
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

}
