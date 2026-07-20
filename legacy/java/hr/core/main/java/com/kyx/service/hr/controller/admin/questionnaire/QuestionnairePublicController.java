package com.kyx.service.hr.controller.admin.questionnaire;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnairePublicSubmitReqVO;
import com.kyx.service.hr.dal.dataobject.questionnaire.*;
import com.kyx.service.hr.dal.mysql.questionnaire.*;
import com.kyx.service.hr.service.questionnaire.QuestionnairePublicLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.kyx.foundation.common.pojo.CommonResult.success;
import static com.kyx.service.hr.enums.ErrorCodeConstants.*;

/**
 * 公开问卷填写 Controller（无需登录）
 */
@Tag(name = "公开问卷填写")
@RestController
@RequestMapping("/hr/public/questionnaire")
@Validated
@Slf4j
public class QuestionnairePublicController {

    @Resource
    private QuestionnairePublicLinkService publicLinkService;
    @Resource
    private QuestionnairePublicLinkMapper publicLinkMapper;
    @Resource
    private QuestionnairePublicAnswerMapper publicAnswerMapper;
    @Resource
    private QuestionnaireMapper questionnaireMapper;
    @Resource
    private QuestionnairePublishMapper publishMapper;
    @Resource
    private QuestionnaireItemMapper itemMapper;
    @Resource
    private QuestionnaireOptionMapper optionMapper;
    @Resource
    private ObjectMapper objectMapper;

    @GetMapping("/info")
    @Operation(summary = "获取公开问卷信息")
    @Parameter(name = "token", description = "访问令牌", required = true)
    public CommonResult<PublicQuestionnaireInfoVO> getPublicQuestionnaireInfo(@RequestParam("token") String token) {
        // 先忽略租户查询 link
        QuestionnairePublicLinkDO link = TenantUtils.executeIgnore(() -> validateLink(token));

        // 使用 link 的租户执行后续操作
        return TenantUtils.execute(link.getTenantId(), () -> {
            validatePublishAvailable(link.getPublishId());
            // 获取问卷信息
            QuestionnaireDO questionnaire = questionnaireMapper.selectById(link.getQuestionnaireId());
            if (questionnaire == null) {
                throw ServiceExceptionUtil.exception(QUESTIONNAIRE_NOT_EXISTS);
            }

            PublicQuestionnaireInfoVO vo = new PublicQuestionnaireInfoVO();
            vo.setQuestionnaireId(questionnaire.getId());
            vo.setName(questionnaire.getName());
            vo.setDescription(null);
            vo.setTitle(link.getTitle());
            vo.setCollectInfo(link.getCollectInfo());
            vo.setNeedPassword(link.getPassword() != null && !link.getPassword().isEmpty());

            // 获取题目列表
            List<QuestionnaireItemDO> items = itemMapper.selectList(
                QuestionnaireItemDO::getQuestionnaireId, questionnaire.getId());
            vo.setItems(items.stream().map(item -> {
                PublicQuestionnaireInfoVO.ItemVO itemVO = BeanUtils.toBean(item, PublicQuestionnaireInfoVO.ItemVO.class);
                List<QuestionnaireOptionDO> options = optionMapper.selectList(
                    QuestionnaireOptionDO::getItemId, item.getId());
                itemVO.setOptions(BeanUtils.toBean(options, PublicQuestionnaireInfoVO.OptionVO.class));
                return itemVO;
            }).collect(java.util.stream.Collectors.toList()));

            return success(vo);
        });
    }

    @PostMapping("/verify-password")
    @Operation(summary = "验证访问密码")
    public CommonResult<Boolean> verifyPassword(@RequestParam("token") String token,
                                                 @RequestParam("password") String password) {
        // 先忽略租户查询 link
        QuestionnairePublicLinkDO link = TenantUtils.executeIgnore(() -> publicLinkMapper.selectByToken(token));
        if (link == null) {
            throw ServiceExceptionUtil.exception(QUESTIONNAIRE_PUBLIC_LINK_NOT_EXISTS);
        }

        // 使用 link 的租户验证密码
        return TenantUtils.execute(link.getTenantId(), () -> {
            boolean valid = publicLinkService.verifyPassword(token, password);
            if (!valid) {
                throw ServiceExceptionUtil.exception(QUESTIONNAIRE_PUBLIC_LINK_PASSWORD_ERROR);
            }
            return success(true);
        });
    }

    @PostMapping("/submit")
    @Operation(summary = "提交公开问卷")
    public CommonResult<Boolean> submitPublicQuestionnaire(
            @Valid @RequestBody QuestionnairePublicSubmitReqVO submitReqVO,
            HttpServletRequest request) {
        // 先忽略租户查询 link
        QuestionnairePublicLinkDO link = TenantUtils.executeIgnore(() -> validateLink(submitReqVO.getToken()));

        // 使用 link 的租户执行提交操作
        return TenantUtils.execute(link.getTenantId(), () -> {
            validatePublishAvailable(link.getPublishId());
            // 验证密码
            if (link.getPassword() != null && !link.getPassword().isEmpty()) {
                if (!link.getPassword().equals(submitReqVO.getPassword())) {
                    throw ServiceExceptionUtil.exception(QUESTIONNAIRE_PUBLIC_LINK_PASSWORD_ERROR);
                }
            }

            // 检查提交数限制（maxSubmit > 0 才限制，0或null表示不限）
            if (link.getMaxSubmit() != null && link.getMaxSubmit() > 0) {
                int currentCount = link.getSubmitCount() != null ? link.getSubmitCount() : 0;
                if (currentCount >= link.getMaxSubmit()) {
                    throw ServiceExceptionUtil.exception(QUESTIONNAIRE_PUBLIC_LINK_MAX_SUBMIT);
                }
            }

            // 保存答案
            try {
                QuestionnairePublicAnswerDO answer = new QuestionnairePublicAnswerDO();
                answer.setLinkId(link.getId());
                answer.setQuestionnaireId(link.getQuestionnaireId());
                answer.setPublishId(link.getPublishId());
                answer.setRespondentName(submitReqVO.getRespondentName());
                answer.setRespondentPhone(submitReqVO.getRespondentPhone());
                answer.setRespondentEmail(submitReqVO.getRespondentEmail());
                answer.setIpAddress(getClientIp(request));
                answer.setUserAgent(request.getHeader("User-Agent"));
                answer.setSubmitTime(LocalDateTime.now());
                answer.setAnswersJson(objectMapper.writeValueAsString(submitReqVO.getAnswers()));

                // 计算总分
                BigDecimal totalScore = calculateTotalScore(submitReqVO.getAnswers());
                answer.setTotalScore(totalScore);

                publicAnswerMapper.insert(answer);

                // 增加提交计数
                publicLinkService.incrementSubmitCount(link.getId());

                log.info("公开问卷提交成功: linkId={}, questionnaireId={}", link.getId(), link.getQuestionnaireId());
            } catch (Exception e) {
                log.error("公开问卷提交失败", e);
                throw new RuntimeException("提交失败");
            }

            return success(true);
        });
    }

    private QuestionnairePublicLinkDO validateLink(String token) {
        QuestionnairePublicLinkDO link = publicLinkMapper.selectByToken(token);
        if (link == null) {
            throw ServiceExceptionUtil.exception(QUESTIONNAIRE_PUBLIC_LINK_NOT_EXISTS);
        }
        if (link.getEnabled() == null || link.getEnabled() != 1) {
            throw ServiceExceptionUtil.exception(QUESTIONNAIRE_PUBLIC_LINK_DISABLED);
        }
        if (link.getExpireTime() != null && link.getExpireTime().isBefore(LocalDateTime.now())) {
            throw ServiceExceptionUtil.exception(QUESTIONNAIRE_PUBLIC_LINK_EXPIRED);
        }
        return link;
    }

    private void validatePublishAvailable(Long publishId) {
        if (publishId == null) {
            return;
        }
        QuestionnairePublishDO publish = publishMapper.selectById(publishId);
        if (publish == null) {
            publish = TenantUtils.executeIgnore(() -> publishMapper.selectById(publishId));
        }
        if (publish == null) {
            throw ServiceExceptionUtil.exception(QUESTIONNAIRE_PUBLISH_NOT_EXISTS);
        }
        LocalDateTime now = LocalDateTime.now();
        if (publish.getStatus() != null && publish.getStatus() == 2) {
            throw ServiceExceptionUtil.exception(QUESTIONNAIRE_PUBLISH_ENDED);
        }
        if (publish.getDeadlineAt() != null && !now.isBefore(publish.getDeadlineAt())) {
            throw ServiceExceptionUtil.exception(QUESTIONNAIRE_PUBLISH_ENDED);
        }
    }

    private BigDecimal calculateTotalScore(List<QuestionnairePublicSubmitReqVO.AnswerItem> answers) {
        if (answers == null) return BigDecimal.ZERO;
        return answers.stream()
            .filter(a -> a.getAnswerScore() != null)
            .map(a -> BigDecimal.valueOf(a.getAnswerScore()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 公开问卷信息 VO
     */
    @Data
    public static class PublicQuestionnaireInfoVO {
        private Long questionnaireId;
        private String name;
        private String description;
        private String title;
        private Integer collectInfo;
        private Boolean needPassword;
        private List<ItemVO> items;

        @Data
        public static class ItemVO {
            private Long id;
            private String title;
            private String itemType;
            private Integer required;
            private Integer sortOrder;
            private Integer maxScore;
            private List<OptionVO> options;
        }

        @Data
        public static class OptionVO {
            private Long id;
            private String optionText;
            private Integer optionScore;
            private Integer sortOrder;
        }
    }

}
