package com.kyx.service.hr.service.exam;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.enums.UserTypeEnum;
import com.kyx.foundation.common.exception.enums.GlobalErrorCodeConstants;
import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.collection.CollectionUtils;
import com.kyx.foundation.common.util.json.JsonUtils;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.security.core.service.SecurityFrameworkService;
import com.kyx.service.business.api.notice.NoticeApi;
import com.kyx.service.business.api.notice.dto.NoticeCreateReqDTO;
import com.kyx.service.business.enums.notice.NoticeTypeEnum;
import com.kyx.service.business.enums.permission.RoleCodeEnum;
import com.kyx.service.op.api.websocket.WebSocketSenderApi;
import com.kyx.service.hr.controller.admin.exam.vo.ExamPageReqVO;
import com.kyx.service.hr.controller.admin.exam.vo.ExamRespVO;
import com.kyx.service.hr.controller.admin.exam.vo.ExamSaveReqVO;
import com.kyx.service.hr.dal.dataobject.exam.ExamDO;
import com.kyx.service.hr.dal.dataobject.exam.ExamPaperDO;
import com.kyx.service.hr.dal.dataobject.exam.ExamPaperItemDO;
import com.kyx.service.hr.dal.dataobject.tenant.TenantDO;
import com.kyx.service.hr.dal.mysql.exam.ExamMapper;
import com.kyx.service.hr.dal.mysql.exam.ExamPaperItemMapper;
import com.kyx.service.hr.dal.mysql.exam.ExamPaperMapper;
import com.kyx.service.hr.dal.mysql.tenant.TenantMapper;
import com.kyx.service.hr.service.notice.HrNoticeRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.kyx.foundation.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static com.kyx.service.hr.enums.ErrorCodeConstants.EXAM_CODE_DUPLICATE;
import static com.kyx.service.hr.enums.ErrorCodeConstants.EXAM_NOT_EXISTS;

/**
 * HR 考试管理 Service 实现类
 *
 * @author MK
 */
@Service
@Validated
@Slf4j
public class ExamServiceImpl implements ExamService {

    private static final ObjectMapper EXAM_JSON_MAPPER = new ObjectMapper();
    private static final String BUSINESS_TYPE_EXAM_PUBLISH = "EXAM_PUBLISH";

    @Resource
    private ExamMapper examMapper;
    @Resource
    private ExamPaperMapper paperMapper;
    @Resource
    private ExamPaperItemMapper itemMapper;
    @Resource
    private NoticeApi noticeApi;
    @Resource
    private WebSocketSenderApi webSocketSenderApi;
    @Resource
    private TenantMapper tenantMapper;
    @Resource
    private SecurityFrameworkService securityFrameworkService;
    @Resource
    private ExamViewScopeSupport examViewScopeSupport;
    @Resource
    private HrNoticeRecordService noticeRecordService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createExam(ExamSaveReqVO createReqVO) {
        validateExamCodeUnique(null, createReqVO.getCode());
        ExamDO exam = BeanUtils.toBean(createReqVO, ExamDO.class);
        examMapper.insert(exam);
        savePapers(exam.getId(), createReqVO.getPapers());
        if (exam.getStatus() != null && exam.getStatus() == 1) {
            sendExamPublishNotice(exam);
        }
        return exam.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateExam(ExamSaveReqVO updateReqVO) {
        ExamDO exist = validateExamExists(updateReqVO.getId());
        validateExamAccess(exist);
        validateExamCodeUnique(exist.getId(), updateReqVO.getCode());

        ExamDO updateObj = BeanUtils.toBean(updateReqVO, ExamDO.class);
        examMapper.updateById(updateObj);

        List<ExamPaperDO> papers = paperMapper.selectListByExamId(updateReqVO.getId());
        List<Long> paperIds = CollectionUtils.convertList(papers, ExamPaperDO::getId);
        if (!paperIds.isEmpty()) {
            itemMapper.deleteByPaperIds(paperIds);
        }
        paperMapper.deleteByExamId(updateReqVO.getId());
        savePapers(updateReqVO.getId(), updateReqVO.getPapers());
        if (updateObj.getStatus() != null && updateObj.getStatus() == 1 && (exist.getStatus() == null || exist.getStatus() != 1)) {
            sendExamPublishNotice(updateObj);
        }
    }

    @Override
    public void deleteExam(Long id) {
        ExamDO exam = validateExamExists(id);
        validateExamAccess(exam);
        List<ExamPaperDO> papers = paperMapper.selectListByExamId(id);
        List<Long> paperIds = CollectionUtils.convertList(papers, ExamPaperDO::getId);
        if (!paperIds.isEmpty()) {
            itemMapper.deleteByPaperIds(paperIds);
        }
        paperMapper.deleteByExamId(id);
        examMapper.deleteById(id);
    }

    @Override
    public ExamRespVO getExam(Long id) {
        ExamDO exam = validateExamExists(id);
        validateExamAccess(exam);
        ExamRespVO resp = BeanUtils.toBean(exam, ExamRespVO.class);

        List<ExamPaperDO> papers = paperMapper.selectListByExamId(id);
        List<Long> paperIds = CollectionUtils.convertList(papers, ExamPaperDO::getId);
        List<ExamPaperItemDO> items = paperIds.isEmpty()
                ? Collections.emptyList()
                : itemMapper.selectListByPaperIds(paperIds);

        Map<Long, List<ExamRespVO.Item>> itemMap = items.stream()
                .collect(Collectors.groupingBy(ExamPaperItemDO::getPaperId,
                        Collectors.mapping(it -> BeanUtils.toBean(it, ExamRespVO.Item.class), Collectors.toList())));

        List<ExamRespVO.Paper> paperRespList = papers.stream().map(paper -> {
            ExamRespVO.Paper paperResp = BeanUtils.toBean(paper, ExamRespVO.Paper.class);
            paperResp.setItems(itemMap.getOrDefault(paper.getId(), new ArrayList<>()));
            return paperResp;
        }).collect(Collectors.toList());

        resp.setPapers(paperRespList);
        return resp;
    }

    @Override
    public PageResult<ExamRespVO> getExamPage(ExamPageReqVO pageReqVO) {
        PageResult<ExamDO> pageResult = examMapper.selectPage(pageReqVO);
        List<ExamDO> list = pageResult.getList();
        if (list.isEmpty()) {
            return BeanUtils.toBean(pageResult, ExamRespVO.class);
        }

        // 批量获取租户名称
        List<Long> tenantIds = list.stream()
                .map(ExamDO::getTenantId)
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
        List<ExamRespVO> voList = BeanUtils.toBean(list, ExamRespVO.class, vo -> {
            if (vo.getTenantId() != null) {
                vo.setTenantName(finalTenantMap.get(vo.getTenantId()));
            }
        });
        return new PageResult<>(voList, pageResult.getTotal());
    }

    private ExamDO validateExamExists(Long id) {
        ExamDO exam = examMapper.selectById(id);
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
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.FORBIDDEN, "无权访问该考试");
        }
    }

    private boolean isAdmin() {
        return securityFrameworkService.hasRole(RoleCodeEnum.SUPER_ADMIN.getCode())
                || securityFrameworkService.hasRole(RoleCodeEnum.TENANT_ADMIN.getCode())
                || examViewScopeSupport.canViewAllData();
    }

    private void sendExamPublishNotice(ExamDO exam) {
        String title = "考试通知：" + exam.getName();
        StringBuilder content = new StringBuilder("考试：").append(exam.getName());
        if (exam.getStartAt() != null) {
            content.append("，开始：").append(exam.getStartAt());
        }
        if (exam.getEndAt() != null) {
            content.append("，结束：").append(exam.getEndAt());
        }
        content.append("。请按时参加。");
        String noticeKey = noticeRecordService.buildNoticeKey(HrNoticeRecordService.CHANNEL_IN_APP,
                BUSINESS_TYPE_EXAM_PUBLISH, exam.getId(), null);
        String remark = "examId=" + exam.getId();
        try {
            NoticeCreateReqDTO reqDTO = new NoticeCreateReqDTO();
            reqDTO.setTitle(title);
            reqDTO.setType(NoticeTypeEnum.ANNOUNCEMENT.getType());
            reqDTO.setContent(content.toString());
            reqDTO.setStatus(CommonStatusEnum.ENABLE.getStatus());
            noticeApi.createNotice(reqDTO);
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("title", reqDTO.getTitle());
            webSocketSenderApi.sendObject(UserTypeEnum.ADMIN.getValue(), "notice-push", payload);
            recordNoticeSuccess(noticeKey, exam.getId(), title, content.toString(), remark);
        } catch (Exception ex) {
            recordNoticeFailure(noticeKey, exam.getId(), title, content.toString(), ex.getMessage(), remark);
            log.warn("sendExamPublishNotice failed, examId={}", exam.getId(), ex);
        }
    }

    private void recordNoticeSuccess(String noticeKey, Long businessId, String title, String content, String remark) {
        try {
            noticeRecordService.recordSuccess(noticeKey, HrNoticeRecordService.CHANNEL_IN_APP,
                    BUSINESS_TYPE_EXAM_PUBLISH, businessId, null, title, content, remark);
        } catch (Exception ex) {
            log.warn("Persist exam notice success failed, businessId={}, reason={}", businessId, ex.getMessage());
        }
    }

    private void recordNoticeFailure(String noticeKey, Long businessId, String title, String content,
                                     String errorMessage, String remark) {
        try {
            noticeRecordService.recordFailure(noticeKey, HrNoticeRecordService.CHANNEL_IN_APP,
                    BUSINESS_TYPE_EXAM_PUBLISH, businessId, null, title, content, errorMessage, remark);
        } catch (Exception ex) {
            log.warn("Persist exam notice failure failed, businessId={}, reason={}", businessId, ex.getMessage());
        }
    }

    private void validateExamCodeUnique(Long id, String code) {
        ExamDO exam = examMapper.selectByCode(code);
        if (exam == null) {
            return;
        }
        if (id == null) {
            throw ServiceExceptionUtil.exception(EXAM_CODE_DUPLICATE);
        }
        if (!exam.getId().equals(id)) {
            throw ServiceExceptionUtil.exception(EXAM_CODE_DUPLICATE);
        }
    }

    private void savePapers(Long examId, List<com.kyx.service.hr.controller.admin.exam.vo.ExamPaperSaveReqVO> papers) {
        if (papers == null || papers.isEmpty()) {
            return;
        }
        for (com.kyx.service.hr.controller.admin.exam.vo.ExamPaperSaveReqVO paperReq : papers) {
            ExamPaperDO paper = BeanUtils.toBean(paperReq, ExamPaperDO.class);
            paper.setExamId(examId);
            normalizePaperJsonFields(paper);
            paperMapper.insert(paper);
            if (paperReq.getItems() == null || paperReq.getItems().isEmpty()) {
                continue;
            }
            for (com.kyx.service.hr.controller.admin.exam.vo.ExamPaperItemSaveReqVO itemReq : paperReq.getItems()) {
                ExamPaperItemDO item = BeanUtils.toBean(itemReq, ExamPaperItemDO.class);
                item.setPaperId(paper.getId());
                normalizeItemJsonFields(item);
                itemMapper.insert(item);
            }
        }
    }

    private void normalizePaperJsonFields(ExamPaperDO paper) {
        if (paper == null) {
            return;
        }
        String ruleJson = paper.getRuleJson();
        if (ruleJson == null || ruleJson.trim().isEmpty()) {
            paper.setRuleJson("{}");
            return;
        }
        paper.setRuleJson(ruleJson.trim());
    }

    private void normalizeItemJsonFields(ExamPaperItemDO item) {
        if (item == null) {
            return;
        }
        item.setOptionsJson(normalizeJsonColumn(item.getOptionsJson(), true));
        item.setAnswerJson(normalizeJsonColumn(item.getAnswerJson(), false));
    }

    private String normalizeJsonColumn(String value, boolean emptyArray) {
        if (value == null || value.trim().isEmpty()) {
            return emptyArray ? "[]" : null;
        }
        String trimmed = value.trim();
        return isValidJsonValue(trimmed) ? trimmed : JsonUtils.toJsonString(trimmed);
    }

    private boolean isValidJsonValue(String value) {
        try {
            EXAM_JSON_MAPPER.readTree(value);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

}
