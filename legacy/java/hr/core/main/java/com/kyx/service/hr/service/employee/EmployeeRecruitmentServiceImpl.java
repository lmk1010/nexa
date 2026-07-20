package com.kyx.service.hr.service.employee;

import cn.hutool.core.util.IdUtil;
import com.kyx.foundation.common.exception.enums.GlobalErrorCodeConstants;
import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.foundation.security.core.util.SecurityFrameworkUtils;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.bpm.api.task.BpmProcessInstanceApi;
import com.kyx.service.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.kyx.service.bpm.enums.task.BpmProcessInstanceStatusEnum;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeRecruitmentConvertEntryReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeRecruitmentPageReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeRecruitmentPublicInfoRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeRecruitmentPublicLinkRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeRecruitmentPublicLinkSaveReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeRecruitmentPublicSubmitReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeRecruitmentRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeRecruitmentSaveReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeRecruitmentStatsRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeEntrySaveReqVO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeProfileDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeEntryDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeRecruitmentDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeRecruitmentPublicLinkDO;
import com.kyx.service.hr.dal.mysql.employee.EmployeeProfileMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeRecruitmentMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeRecruitmentPublicLinkMapper;
import com.kyx.service.hr.enums.ErrorCodeConstants;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Validated
@Slf4j
public class EmployeeRecruitmentServiceImpl implements EmployeeRecruitmentService {

    public static final String PROCESS_KEY_RECRUITMENT_DEMAND = "hr_recruitment_demand";
    public static final String PROCESS_KEY_RECRUITMENT_OFFER = "hr_recruitment_offer";

    private static final String TALENT_STATUS_BLACKLISTED = "BLACKLISTED";
    private static final String TALENT_STATUS_HIRED = "HIRED";
    private static final String TALENT_STATUS_OFFERING = "OFFERING";
    private static final String TALENT_STATUS_POOL = "POOL";
    private static final String DEMAND_STATUS_APPROVED = "APPROVED";
    private static final String DEMAND_STATUS_APPROVING = "APPROVING";
    private static final String DEMAND_STATUS_CLOSED = "CLOSED";
    private static final String DEMAND_STATUS_DRAFT = "DRAFT";
    private static final String DEMAND_STATUS_PAUSED = "PAUSED";
    private static final String DEMAND_STATUS_REJECTED = "REJECTED";
    private static final String INTERVIEW_DECISION_PASS = "PASS";
    private static final String INTERVIEW_DECISION_PENDING = "PENDING";
    private static final String INTERVIEW_DECISION_REJECT = "REJECT";
    private static final String OFFER_STATUS_APPROVING = "审批中";
    private static final String OFFER_STATUS_PENDING_ACCEPT = "待接受";
    private static final String OFFER_STATUS_REJECTED = "已拒绝";
    private static final String TOUCH_STATUS_CONTACTED = "CONTACTED";
    private static final String TOUCH_STATUS_RESPONDED = "RESPONDED";
    private static final String RESUME_PARSE_STATUS_PARSED = "PARSED";
    private static final String RESUME_PARSE_STATUS_FAILED = "FAILED";
    private static final Pattern WORK_YEARS_PATTERN = Pattern.compile("(\\d{1,2}(?:\\.\\d)?)\\s*(?:年|年以上|年经验|years?|Y)", Pattern.CASE_INSENSITIVE);

    @Resource
    private EmployeeRecruitmentMapper employeeRecruitmentMapper;
    @Resource
    private EmployeeRecruitmentPublicLinkMapper employeeRecruitmentPublicLinkMapper;
    @Resource
    private EmployeeProfileMapper employeeProfileMapper;
    @Resource
    private EmployeeEntryService employeeEntryService;
    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    @Override
    public List<EmployeeRecruitmentRespVO> getRecruitmentList(Long profileId) {
        List<EmployeeRecruitmentDO> list = employeeRecruitmentMapper.selectListByProfileId(profileId);
        List<EmployeeRecruitmentRespVO> respList = BeanUtils.toBean(list, EmployeeRecruitmentRespVO.class);
        fillProfileInfo(respList);
        return respList;
    }

    @Override
    public PageResult<EmployeeRecruitmentRespVO> getRecruitmentPage(EmployeeRecruitmentPageReqVO pageReqVO) {
        if (!prepareProfileFilter(pageReqVO)) {
            return new PageResult<>(new ArrayList<>(), 0L);
        }
        PageResult<EmployeeRecruitmentDO> pageResult = employeeRecruitmentMapper.selectPage(pageReqVO);
        List<EmployeeRecruitmentRespVO> respList = BeanUtils.toBean(pageResult.getList(), EmployeeRecruitmentRespVO.class);
        fillProfileInfo(respList);
        return new PageResult<>(respList, pageResult.getTotal());
    }

    @Override
    public EmployeeRecruitmentStatsRespVO getRecruitmentStats(EmployeeRecruitmentPageReqVO pageReqVO) {
        EmployeeRecruitmentStatsRespVO stats = new EmployeeRecruitmentStatsRespVO();
        if (!prepareProfileFilter(pageReqVO)) {
            return stats;
        }
        List<EmployeeRecruitmentDO> list = employeeRecruitmentMapper.selectListByReq(pageReqVO, 5000);
        stats.setTotalCount(list.size());
        stats.setOfferCount((int) list.stream().filter(this::hasOffer).count());
        stats.setInterviewCount((int) list.stream().filter(this::hasInterview).count());
        stats.setOfferAcceptedCount((int) list.stream().filter(this::hasAcceptedOffer).count());
        stats.setEntryCount((int) list.stream().filter(this::hasEntry).count());
        stats.setPendingEntryCount((int) list.stream().filter(this::isPendingEntry).count());
        stats.setOverdueFollowCount((int) list.stream().filter(this::isOverdueFollow).count());
        stats.setHighPriorityCount((int) list.stream().filter(this::isHighPriority).count());
        stats.setPoolCount((int) list.stream().filter(this::isInTalentPool).count());
        stats.setBlacklistCount((int) list.stream().filter(this::isBlacklisted).count());
        stats.setResumeCount((int) list.stream().filter(this::hasResume).count());
        stats.setResumeParsedCount((int) list.stream().filter(this::hasResumeParsed).count());
        stats.setDemandOpenCount((int) list.stream().filter(this::isDemandOpen).count());
        stats.setDemandApprovedCount((int) list.stream().filter(this::isDemandApproved).count());
        stats.setInterviewEvaluatedCount((int) list.stream().filter(this::hasInterviewEvaluation).count());
        stats.setInterviewPassCount((int) list.stream().filter(this::isInterviewPass).count());
        stats.setAvgInterviewScore(calculateAvgInterviewScore(list));
        stats.setReferralCount((int) list.stream().filter(this::hasReferral).count());
        stats.setTouchedCount((int) list.stream().filter(this::hasTouched).count());
        stats.setResponseCount((int) list.stream().filter(this::hasResponse).count());
        stats.setChannelCostTotal(calculateChannelCostTotal(list));
        stats.setResponseRate(percent(stats.getResponseCount(), stats.getTotalCount()));
        stats.setInterviewRate(percent(stats.getInterviewCount(), stats.getTotalCount()));
        stats.setOfferRate(percent(stats.getOfferCount(), stats.getTotalCount()));
        stats.setEntryRate(percent(stats.getEntryCount(), stats.getTotalCount()));
        stats.setCostPerCandidate(average(stats.getChannelCostTotal(), stats.getTotalCount()));
        stats.setCostPerEntry(average(stats.getChannelCostTotal(), stats.getEntryCount()));
        stats.setChannelStats(groupStats(list, EmployeeRecruitmentDO::getChannel));
        stats.setChannelEffectStats(buildChannelEffectStats(list));
        stats.setStatusStats(groupStats(list, EmployeeRecruitmentDO::getStatus));
        stats.setStageStats(groupStats(list, EmployeeRecruitmentDO::getCandidateStage));
        stats.setTalentStatusStats(groupStats(list, this::resolveTalentStatus));
        stats.setDemandStatusStats(groupStats(list, EmployeeRecruitmentDO::getDemandStatus));
        stats.setInterviewDecisionStats(groupStats(list, EmployeeRecruitmentDO::getInterviewDecision));
        stats.setCampaignStats(groupStats(list, EmployeeRecruitmentDO::getCampaignName));
        stats.setTouchStatusStats(groupStats(list, EmployeeRecruitmentDO::getTouchStatus));
        stats.setResumeParseStatusStats(groupStats(list, EmployeeRecruitmentDO::getResumeParseStatus));
        return stats;
    }

    @Override
    public Long createRecruitment(EmployeeRecruitmentSaveReqVO createReqVO) {
        validateProfileExists(createReqVO.getProfileId());
        EmployeeRecruitmentDO recruitment = BeanUtils.toBean(createReqVO, EmployeeRecruitmentDO.class);
        protectApprovalFieldsForCreate(recruitment);
        normalizeRecruitment(recruitment);
        validateRecruitmentFlow(recruitment);
        employeeRecruitmentMapper.insert(recruitment);
        return recruitment.getId();
    }

    @Override
    public void updateRecruitment(EmployeeRecruitmentSaveReqVO updateReqVO) {
        EmployeeRecruitmentDO existed = updateReqVO.getId() == null ? null : employeeRecruitmentMapper.selectById(updateReqVO.getId());
        if (existed == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_PROFILE_NOT_EXISTS);
        }
        validateProfileExists(updateReqVO.getProfileId());
        EmployeeRecruitmentDO recruitment = BeanUtils.toBean(updateReqVO, EmployeeRecruitmentDO.class);
        protectApprovalFieldsForUpdate(existed, recruitment);
        normalizeRecruitment(recruitment);
        validateRecruitmentFlow(recruitment);
        employeeRecruitmentMapper.updateById(recruitment);
    }

    @Override
    public void deleteRecruitment(Long id) {
        if (employeeRecruitmentMapper.selectById(id) == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_PROFILE_NOT_EXISTS);
        }
        employeeRecruitmentMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmployeeRecruitmentRespVO parseResume(Long id) {
        EmployeeRecruitmentDO recruitment = employeeRecruitmentMapper.selectById(id);
        if (recruitment == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_PROFILE_NOT_EXISTS);
        }
        ResumeParseResult parseResult = buildResumeParseResult(recruitment);
        EmployeeRecruitmentDO updateDO = new EmployeeRecruitmentDO();
        updateDO.setId(id);
        updateDO.setResumeParseStatus(parseResult.getStatus());
        updateDO.setResumeParseTime(LocalDateTime.now());
        updateDO.setResumeSummary(parseResult.getSummary());
        updateDO.setResumeSkills(parseResult.getSkills());
        updateDO.setResumeWorkYears(parseResult.getWorkYears());
        updateDO.setResumeEducation(parseResult.getEducation());
        updateDO.setResumeLastCompany(parseResult.getLastCompany());
        employeeRecruitmentMapper.updateById(updateDO);
        EmployeeRecruitmentRespVO respVO = BeanUtils.toBean(employeeRecruitmentMapper.selectById(id), EmployeeRecruitmentRespVO.class);
        List<EmployeeRecruitmentRespVO> respList = new ArrayList<>();
        respList.add(respVO);
        fillProfileInfo(respList);
        return respVO;
    }

    @Override
    public EmployeeRecruitmentPublicLinkRespVO createPublicLink(EmployeeRecruitmentPublicLinkSaveReqVO createReqVO) {
        EmployeeRecruitmentPublicLinkDO link = BeanUtils.toBean(createReqVO, EmployeeRecruitmentPublicLinkDO.class);
        normalizePublicLink(link);
        employeeRecruitmentPublicLinkMapper.insert(link);
        return BeanUtils.toBean(link, EmployeeRecruitmentPublicLinkRespVO.class);
    }

    @Override
    public EmployeeRecruitmentPublicInfoRespVO getPublicInfo(String token) {
        EmployeeRecruitmentPublicLinkDO link = TenantUtils.executeIgnore(() -> validatePublicLink(token));
        return TenantUtils.execute(link.getTenantId(), () ->
                BeanUtils.toBean(validatePublicLink(token), EmployeeRecruitmentPublicInfoRespVO.class));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long convertToEntry(EmployeeRecruitmentConvertEntryReqVO convertReqVO) {
        EmployeeRecruitmentDO recruitment = employeeRecruitmentMapper.selectById(convertReqVO.getId());
        if (recruitment == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_PROFILE_NOT_EXISTS);
        }
        validateRecruitmentCanConvert(recruitment);
        validateProfileExists(recruitment.getProfileId());

        List<EmployeeEntryDO> entryHistory = employeeEntryService.getEmployeeEntryListByProfileId(recruitment.getProfileId());
        LocalDate entryDate = convertReqVO.getEntryDate() != null
                ? convertReqVO.getEntryDate()
                : (recruitment.getEntryDate() != null ? recruitment.getEntryDate() : LocalDate.now());
        validateConvertEntryRequest(convertReqVO, entryDate);
        Long entryId = findReusableEntryId(entryHistory, entryDate);
        validateNoConflictingEntryHistory(entryHistory, entryId);
        if (entryId == null) {
            EmployeeEntrySaveReqVO entryReqVO = buildEntryReqVO(recruitment, convertReqVO, entryDate, entryHistory);
            entryId = employeeEntryService.createEmployeeEntry(entryReqVO);
        }
        updateRecruitmentToEntry(recruitment, entryDate, convertReqVO.getRemark());
        return entryId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitPublicCandidate(EmployeeRecruitmentPublicSubmitReqVO submitReqVO) {
        EmployeeRecruitmentPublicLinkDO publicLink = TenantUtils.executeIgnore(() -> validatePublicLink(submitReqVO.getToken()));
        return TenantUtils.execute(publicLink.getTenantId(), () -> submitPublicCandidateInTenant(submitReqVO));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String submitDemandApproval(Long id) {
        EmployeeRecruitmentDO recruitment = validateRecruitmentExists(id);
        if (isDemandClosed(recruitment)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "招聘需求已暂停或关闭，不能提交审批");
        }
        if (isDemandApproved(recruitment)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "招聘需求已审批通过");
        }
        if (DEMAND_STATUS_APPROVING.equals(recruitment.getDemandStatus())
                && StringUtils.hasText(recruitment.getDemandProcessInstanceId())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "招聘需求正在审批中");
        }
        if (!hasDemandBusinessInfo(recruitment)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "请先填写岗位、用人部门、HC、预算或招聘原因，再提交需求审批");
        }
        String processInstanceId = startRecruitmentApprovalProcess(recruitment, PROCESS_KEY_RECRUITMENT_DEMAND);
        EmployeeRecruitmentDO update = new EmployeeRecruitmentDO();
        update.setId(recruitment.getId());
        update.setDemandStatus(DEMAND_STATUS_APPROVING);
        update.setDemandProcessInstanceId(processInstanceId);
        employeeRecruitmentMapper.updateById(update);
        return processInstanceId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String submitOfferApproval(Long id) {
        EmployeeRecruitmentDO recruitment = validateRecruitmentExists(id);
        validateRecruitmentCanSubmitOfferApproval(recruitment);
        String processInstanceId = startRecruitmentApprovalProcess(recruitment, PROCESS_KEY_RECRUITMENT_OFFER);
        EmployeeRecruitmentDO update = new EmployeeRecruitmentDO();
        update.setId(recruitment.getId());
        update.setOfferStatus(OFFER_STATUS_APPROVING);
        update.setOfferProcessInstanceId(processInstanceId);
        update.setOfferDate(recruitment.getOfferDate() != null ? recruitment.getOfferDate() : LocalDate.now());
        update.setCandidateStage("Offer");
        update.setStatus("Offer");
        update.setTalentStatus(TALENT_STATUS_OFFERING);
        update.setLastContactTime(LocalDateTime.now());
        employeeRecruitmentMapper.updateById(update);
        return processInstanceId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateApprovalStatusByBpmEvent(Long id, String processDefinitionKey,
                                               String processInstanceId, Integer bpmStatus, Long operatorUserId) {
        if (id == null) {
            return;
        }
        EmployeeRecruitmentDO recruitment = employeeRecruitmentMapper.selectById(id);
        if (recruitment == null) {
            log.warn("招聘审批 BPM 回调记录不存在，id={}, processDefinitionKey={}, processInstanceId={}, bpmStatus={}",
                    id, processDefinitionKey, processInstanceId, bpmStatus);
            return;
        }
        if (PROCESS_KEY_RECRUITMENT_DEMAND.equals(processDefinitionKey)) {
            updateDemandApprovalStatusByBpmEvent(recruitment, processInstanceId, bpmStatus);
            return;
        }
        if (PROCESS_KEY_RECRUITMENT_OFFER.equals(processDefinitionKey)) {
            updateOfferApprovalStatusByBpmEvent(recruitment, processInstanceId, bpmStatus);
        }
    }

    private Long submitPublicCandidateInTenant(EmployeeRecruitmentPublicSubmitReqVO submitReqVO) {
        EmployeeRecruitmentPublicLinkDO link = validatePublicLink(submitReqVO.getToken());
        EmployeeProfileDO profile = getOrCreateCandidateProfile(submitReqVO);
        EmployeeRecruitmentDO recruitment = getOrCreatePublicRecruitment(profile.getId(), link);
        fillPublicRecruitment(recruitment, profile.getId(), link, submitReqVO);
        if (recruitment.getId() == null) {
            employeeRecruitmentMapper.insert(recruitment);
        } else {
            employeeRecruitmentMapper.updateById(recruitment);
        }
        incrementSubmitCount(link);
        return recruitment.getId();
    }

    private EmployeeRecruitmentDO validateRecruitmentExists(Long id) {
        EmployeeRecruitmentDO recruitment = id == null ? null : employeeRecruitmentMapper.selectById(id);
        if (recruitment == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_PROFILE_NOT_EXISTS);
        }
        return recruitment;
    }

    private void protectApprovalFieldsForCreate(EmployeeRecruitmentDO recruitment) {
        recruitment.setDemandProcessInstanceId(null);
        recruitment.setOfferProcessInstanceId(null);
        if (!StringUtils.hasText(recruitment.getDemandStatus()) && hasDemandBusinessInfo(recruitment)) {
            recruitment.setDemandStatus(DEMAND_STATUS_DRAFT);
        }
        if (DEMAND_STATUS_APPROVING.equals(recruitment.getDemandStatus())
                || DEMAND_STATUS_APPROVED.equals(recruitment.getDemandStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST,
                    "招聘需求审批状态不能手工设置，请保存后提交需求审批");
        }
        if (OFFER_STATUS_APPROVING.equals(recruitment.getOfferStatus())
                || OFFER_STATUS_PENDING_ACCEPT.equals(recruitment.getOfferStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST,
                    "Offer 审批状态不能手工设置，请通过 Offer 审批流程推进");
        }
    }

    private void protectApprovalFieldsForUpdate(EmployeeRecruitmentDO existed, EmployeeRecruitmentDO recruitment) {
        recruitment.setDemandProcessInstanceId(existed.getDemandProcessInstanceId());
        recruitment.setOfferProcessInstanceId(existed.getOfferProcessInstanceId());
        String demandStatus = trimText(recruitment.getDemandStatus());
        if (DEMAND_STATUS_APPROVING.equals(existed.getDemandStatus())
                && StringUtils.hasText(existed.getDemandProcessInstanceId())
                && !DEMAND_STATUS_APPROVING.equals(demandStatus)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST,
                    "招聘需求审批中，请在 BPM 流程中处理");
        }
        if (DEMAND_STATUS_APPROVING.equals(demandStatus) && !DEMAND_STATUS_APPROVING.equals(existed.getDemandStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST,
                    "招聘需求审批中状态不能手工设置，请使用提交需求审批");
        }
        if (DEMAND_STATUS_APPROVED.equals(demandStatus) && !isDemandApproved(existed)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST,
                    "招聘需求审批通过只能由 BPM 回写");
        }
        String offerStatus = trimText(recruitment.getOfferStatus());
        if (OFFER_STATUS_APPROVING.equals(existed.getOfferStatus())
                && StringUtils.hasText(existed.getOfferProcessInstanceId())
                && !OFFER_STATUS_APPROVING.equals(offerStatus)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST,
                    "Offer 审批中，请在 BPM 流程中处理");
        }
        if (OFFER_STATUS_APPROVING.equals(offerStatus) && !OFFER_STATUS_APPROVING.equals(existed.getOfferStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST,
                    "Offer 审批中状态不能手工设置，请使用提交 Offer 审批");
        }
        if (OFFER_STATUS_PENDING_ACCEPT.equals(offerStatus)
                && !OFFER_STATUS_PENDING_ACCEPT.equals(existed.getOfferStatus())
                && !StringUtils.hasText(existed.getOfferProcessInstanceId())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST,
                    "Offer 待接受状态必须先完成 Offer 审批");
        }
    }

    private void validateRecruitmentCanSubmitOfferApproval(EmployeeRecruitmentDO recruitment) {
        if (OFFER_STATUS_APPROVING.equals(recruitment.getOfferStatus())
                && StringUtils.hasText(recruitment.getOfferProcessInstanceId())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "Offer 正在审批中");
        }
        if (!isDemandApproved(recruitment)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "招聘需求未审批通过，不能提交 Offer 审批");
        }
        if (!isInterviewPass(recruitment)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "面试未通过或未评价，不能提交 Offer 审批");
        }
        if (hasAcceptedOfferStatus(recruitment)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "Offer 已接受，无需重复提交审批");
        }
        if (isDemandClosed(recruitment)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "招聘需求已暂停或关闭，不能提交 Offer 审批");
        }
    }

    private String startRecruitmentApprovalProcess(EmployeeRecruitmentDO recruitment, String processDefinitionKey) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.UNAUTHORIZED, "招聘审批发起人不能为空");
        }
        EmployeeProfileDO profile = recruitment.getProfileId() == null ? null
                : employeeProfileMapper.selectById(recruitment.getProfileId());
        Map<String, Object> variables = new HashMap<>();
        variables.put("recruitmentId", recruitment.getId());
        variables.put("profileId", recruitment.getProfileId());
        variables.put("candidateName", profile == null ? null : profile.getName());
        variables.put("candidateMobile", profile == null ? null : profile.getMobile());
        variables.put("position", recruitment.getPosition());
        variables.put("recruiter", recruitment.getRecruiter());
        variables.put("demandCode", recruitment.getDemandCode());
        variables.put("demandDeptName", recruitment.getDemandDeptName());
        variables.put("demandHeadcount", recruitment.getDemandHeadcount());
        variables.put("demandBudget", recruitment.getDemandBudget());
        variables.put("demandReason", recruitment.getDemandReason());
        variables.put("interviewDecision", recruitment.getInterviewDecision());
        variables.put("interviewScore", recruitment.getInterviewScore());
        variables.put("offerSalary", recruitment.getOfferSalary());
        variables.put("offerDate", recruitment.getOfferDate());

        return processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO()
                        .setProcessDefinitionKey(processDefinitionKey)
                        .setBusinessKey(String.valueOf(recruitment.getId()))
                        .setVariables(variables))
                .getCheckedData();
    }

    private void updateDemandApprovalStatusByBpmEvent(EmployeeRecruitmentDO recruitment,
                                                      String processInstanceId, Integer bpmStatus) {
        if (StringUtils.hasText(recruitment.getDemandProcessInstanceId())
                && StringUtils.hasText(processInstanceId)
                && !Objects.equals(recruitment.getDemandProcessInstanceId(), processInstanceId)) {
            log.warn("招聘需求 BPM 回调流程实例不匹配，id={}, processInstanceId={}, currentProcessInstanceId={}",
                    recruitment.getId(), processInstanceId, recruitment.getDemandProcessInstanceId());
            return;
        }
        EmployeeRecruitmentDO update = new EmployeeRecruitmentDO();
        update.setId(recruitment.getId());
        update.setDemandProcessInstanceId(StringUtils.hasText(processInstanceId)
                ? processInstanceId : recruitment.getDemandProcessInstanceId());
        if (Objects.equals(bpmStatus, BpmProcessInstanceStatusEnum.APPROVE.getStatus())) {
            update.setDemandStatus(DEMAND_STATUS_APPROVED);
        } else if (Objects.equals(bpmStatus, BpmProcessInstanceStatusEnum.REJECT.getStatus())) {
            update.setDemandStatus(DEMAND_STATUS_REJECTED);
        } else if (Objects.equals(bpmStatus, BpmProcessInstanceStatusEnum.CANCEL.getStatus())) {
            update.setDemandStatus(DEMAND_STATUS_DRAFT);
        } else {
            return;
        }
        employeeRecruitmentMapper.updateById(update);
    }

    private void updateOfferApprovalStatusByBpmEvent(EmployeeRecruitmentDO recruitment,
                                                     String processInstanceId, Integer bpmStatus) {
        if (StringUtils.hasText(recruitment.getOfferProcessInstanceId())
                && StringUtils.hasText(processInstanceId)
                && !Objects.equals(recruitment.getOfferProcessInstanceId(), processInstanceId)) {
            log.warn("Offer BPM 回调流程实例不匹配，id={}, processInstanceId={}, currentProcessInstanceId={}",
                    recruitment.getId(), processInstanceId, recruitment.getOfferProcessInstanceId());
            return;
        }
        EmployeeRecruitmentDO update = new EmployeeRecruitmentDO();
        update.setId(recruitment.getId());
        update.setOfferProcessInstanceId(StringUtils.hasText(processInstanceId)
                ? processInstanceId : recruitment.getOfferProcessInstanceId());
        if (Objects.equals(bpmStatus, BpmProcessInstanceStatusEnum.APPROVE.getStatus())) {
            update.setOfferStatus(OFFER_STATUS_PENDING_ACCEPT);
            update.setCandidateStage("Offer");
            update.setStatus("Offer");
            update.setTalentStatus(TALENT_STATUS_OFFERING);
            update.setOfferDate(recruitment.getOfferDate() != null ? recruitment.getOfferDate() : LocalDate.now());
        } else if (Objects.equals(bpmStatus, BpmProcessInstanceStatusEnum.REJECT.getStatus())) {
            update.setOfferStatus(OFFER_STATUS_REJECTED);
        } else if (Objects.equals(bpmStatus, BpmProcessInstanceStatusEnum.CANCEL.getStatus())) {
            update.setOfferStatus(null);
        } else {
            return;
        }
        employeeRecruitmentMapper.updateById(update);
    }

    private void validateProfileExists(Long profileId) {
        if (profileId == null || employeeProfileMapper.selectById(profileId) == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_PROFILE_NOT_EXISTS);
        }
    }

    private void normalizePublicLink(EmployeeRecruitmentPublicLinkDO link) {
        if (!StringUtils.hasText(link.getToken())) {
            link.setToken(IdUtil.fastSimpleUUID());
        }
        if (!StringUtils.hasText(link.getTitle())) {
            String title = StringUtils.hasText(link.getPosition()) ? link.getPosition() : "招聘投递";
            link.setTitle(title + "投递入口");
        }
        if (!StringUtils.hasText(link.getSource())) {
            link.setSource("公开投递");
        }
        if (!StringUtils.hasText(link.getChannel())) {
            link.setChannel("公开链接");
        }
        if (!StringUtils.hasText(link.getPriority())) {
            link.setPriority("MEDIUM");
        }
        if (link.getEnabled() == null) {
            link.setEnabled(1);
        }
        if (link.getMaxSubmit() == null) {
            link.setMaxSubmit(0);
        }
        link.setSubmitCount(0);
    }

    private EmployeeRecruitmentPublicLinkDO validatePublicLink(String token) {
        EmployeeRecruitmentPublicLinkDO link = employeeRecruitmentPublicLinkMapper.selectByToken(token);
        if (link == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.RECRUITMENT_PUBLIC_LINK_NOT_EXISTS);
        }
        if (link.getEnabled() == null || link.getEnabled() != 1) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.RECRUITMENT_PUBLIC_LINK_DISABLED);
        }
        if (link.getExpireTime() != null && link.getExpireTime().isBefore(LocalDateTime.now())) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.RECRUITMENT_PUBLIC_LINK_EXPIRED);
        }
        if (link.getMaxSubmit() != null
                && link.getMaxSubmit() > 0
                && link.getSubmitCount() != null
                && link.getSubmitCount() >= link.getMaxSubmit()) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.RECRUITMENT_PUBLIC_LINK_MAX_SUBMIT);
        }
        return link;
    }

    private EmployeeProfileDO getOrCreateCandidateProfile(EmployeeRecruitmentPublicSubmitReqVO submitReqVO) {
        EmployeeProfileDO profile = employeeProfileMapper.selectOne(new LambdaQueryWrapperX<EmployeeProfileDO>()
                .eq(EmployeeProfileDO::getMobile, submitReqVO.getMobile())
                .last("LIMIT 1"));
        if (profile != null) {
            return profile;
        }
        profile = new EmployeeProfileDO();
        profile.setProfileNo("CAND" + IdUtil.getSnowflakeNextIdStr());
        profile.setName(submitReqVO.getName());
        profile.setMobile(submitReqVO.getMobile());
        profile.setEmail(submitReqVO.getEmail());
        profile.setStatus(0);
        employeeProfileMapper.insert(profile);
        return profile;
    }

    private EmployeeRecruitmentDO getOrCreatePublicRecruitment(Long profileId, EmployeeRecruitmentPublicLinkDO link) {
        EmployeeRecruitmentDO existing = employeeRecruitmentMapper.selectOne(
                new LambdaQueryWrapperX<EmployeeRecruitmentDO>()
                        .eq(EmployeeRecruitmentDO::getProfileId, profileId)
                        .eqIfPresent(EmployeeRecruitmentDO::getCampaignName, link.getCampaignName())
                        .eqIfPresent(EmployeeRecruitmentDO::getDemandCode, link.getDemandCode())
                        .eqIfPresent(EmployeeRecruitmentDO::getPosition, link.getPosition())
                        .last("LIMIT 1"));
        return existing != null ? existing : new EmployeeRecruitmentDO();
    }

    private void fillPublicRecruitment(
            EmployeeRecruitmentDO recruitment,
            Long profileId,
            EmployeeRecruitmentPublicLinkDO link,
            EmployeeRecruitmentPublicSubmitReqVO submitReqVO) {
        recruitment.setProfileId(recruitment.getProfileId() != null ? recruitment.getProfileId() : profileId);
        recruitment.setChannel(link.getChannel());
        recruitment.setSource(StringUtils.hasText(submitReqVO.getSource()) ? submitReqVO.getSource() : link.getSource());
        recruitment.setCampaignName(link.getCampaignName());
        recruitment.setReferrerName(submitReqVO.getReferrerName());
        recruitment.setReferrerMobile(submitReqVO.getReferrerMobile());
        recruitment.setPosition(link.getPosition());
        recruitment.setRecruiter(link.getRecruiter());
        recruitment.setDemandCode(link.getDemandCode());
        recruitment.setDemandDeptName(link.getDemandDeptName());
        recruitment.setDemandHeadcount(link.getDemandHeadcount());
        recruitment.setDemandBudget(link.getDemandBudget());
        recruitment.setDemandReason(link.getDemandReason());
        recruitment.setDemandStatus(DEMAND_STATUS_DRAFT);
        recruitment.setCandidateStage("初筛");
        recruitment.setPriority(link.getPriority());
        recruitment.setExpectedSalary(submitReqVO.getExpectedSalary());
        recruitment.setLastContactTime(LocalDateTime.now());
        recruitment.setTouchStatus("PENDING");
        recruitment.setTalentStatus(TALENT_STATUS_POOL);
        recruitment.setTalentTags(submitReqVO.getTalentTags());
        recruitment.setResumeUrl(submitReqVO.getResumeUrl());
        recruitment.setStatus("初筛");
        recruitment.setRemark(buildPublicSubmitRemark(submitReqVO, link));
        normalizeRecruitment(recruitment);
    }

    private String buildPublicSubmitRemark(
            EmployeeRecruitmentPublicSubmitReqVO submitReqVO,
            EmployeeRecruitmentPublicLinkDO link) {
        List<String> parts = new ArrayList<>();
        parts.add("公开投递：" + LocalDate.now());
        if (StringUtils.hasText(submitReqVO.getRemark())) {
            parts.add(submitReqVO.getRemark().trim());
        }
        if (StringUtils.hasText(link.getRemark())) {
            parts.add("链接备注：" + link.getRemark().trim());
        }
        return parts.stream().filter(StringUtils::hasText).collect(Collectors.joining("；"));
    }

    private void incrementSubmitCount(EmployeeRecruitmentPublicLinkDO link) {
        EmployeeRecruitmentPublicLinkDO updateDO = new EmployeeRecruitmentPublicLinkDO();
        updateDO.setId(link.getId());
        updateDO.setSubmitCount((link.getSubmitCount() == null ? 0 : link.getSubmitCount()) + 1);
        employeeRecruitmentPublicLinkMapper.updateById(updateDO);
    }

    private Long findReusableEntryId(List<EmployeeEntryDO> entryHistory, LocalDate entryDate) {
        if (entryHistory == null || entryHistory.isEmpty() || entryDate == null) {
            return null;
        }
        return entryHistory.stream()
                .filter(entry -> entry.getEntryDate() != null && Objects.equals(entry.getEntryDate(), entryDate))
                .filter(entry -> Objects.equals(entry.getWorkStatus(), 1))
                .filter(entry -> !Objects.equals(entry.getOnboardingStatus(), 5))
                .map(EmployeeEntryDO::getId)
                .findFirst()
                .orElse(null);
    }

    private void validateConvertEntryRequest(EmployeeRecruitmentConvertEntryReqVO convertReqVO, LocalDate entryDate) {
        if (entryDate != null && entryDate.isBefore(LocalDate.now())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "入职日期不能早于今天");
        }
        if (convertReqVO.getWorkStatus() != null && !Objects.equals(convertReqVO.getWorkStatus(), 1)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "招聘转入职只能生成待入职记录");
        }
        if (convertReqVO.getOnboardingStatus() != null && !Objects.equals(convertReqVO.getOnboardingStatus(), 1)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "招聘转入职不能直接写入已审批入职状态");
        }
    }

    private void validateNoConflictingEntryHistory(List<EmployeeEntryDO> entryHistory, Long reusableEntryId) {
        if (entryHistory == null || entryHistory.isEmpty()) {
            return;
        }
        boolean hasOpenEntry = entryHistory.stream()
                .filter(entry -> reusableEntryId == null || !Objects.equals(entry.getId(), reusableEntryId))
                .anyMatch(entry -> !Objects.equals(entry.getWorkStatus(), 4)
                        && !Objects.equals(entry.getOnboardingStatus(), 5));
        if (hasOpenEntry) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST,
                    "候选人已有未关闭的任职或入职记录，不能重复转入职");
        }
    }

    private EmployeeEntrySaveReqVO buildEntryReqVO(
            EmployeeRecruitmentDO recruitment,
            EmployeeRecruitmentConvertEntryReqVO convertReqVO,
            LocalDate entryDate,
            List<EmployeeEntryDO> entryHistory) {
        EmployeeEntrySaveReqVO entryReqVO = new EmployeeEntrySaveReqVO();
        entryReqVO.setProfileId(recruitment.getProfileId());
        entryReqVO.setEntryType(entryHistory != null && !entryHistory.isEmpty() ? 2 : 1);
        entryReqVO.setProcessType(convertReqVO.getProcessType() == null ? 1 : convertReqVO.getProcessType());
        entryReqVO.setEntryDate(entryDate);
        entryReqVO.setJobTitle(recruitment.getPosition());
        entryReqVO.setEmploymentType(1);
        entryReqVO.setProbationMonths(3);
        entryReqVO.setContractType(1);
        entryReqVO.setContractStartDate(entryDate);
        entryReqVO.setWorkStatus(1);
        entryReqVO.setOnboardingStatus(1);
        entryReqVO.setRemark(buildEntryRemark(recruitment, convertReqVO.getRemark(), entryDate));
        return entryReqVO;
    }

    private String buildEntryRemark(EmployeeRecruitmentDO recruitment, String remark, LocalDate entryDate) {
        List<String> parts = new ArrayList<>();
        parts.add("由招聘记录转入职");
        if (entryDate != null) {
            parts.add("计划入职日期：" + entryDate);
        }
        if (StringUtils.hasText(recruitment.getPosition())) {
            parts.add("岗位：" + recruitment.getPosition());
        }
        if (StringUtils.hasText(recruitment.getCampaignName())) {
            parts.add("活动：" + recruitment.getCampaignName());
        }
        if (StringUtils.hasText(recruitment.getRemark())) {
            parts.add("招聘备注：" + recruitment.getRemark().trim());
        }
        if (StringUtils.hasText(remark)) {
            parts.add(remark.trim());
        }
        return parts.stream().filter(StringUtils::hasText).collect(Collectors.joining("；"));
    }

    private void updateRecruitmentToEntry(EmployeeRecruitmentDO recruitment, LocalDate entryDate, String remark) {
        recruitment.setCandidateStage("待入职");
        recruitment.setStatus("待入职");
        recruitment.setEntryDate(entryDate);
        recruitment.setOfferStatus(StringUtils.hasText(recruitment.getOfferStatus()) ? recruitment.getOfferStatus() : "已接受");
        recruitment.setTouchStatus(TOUCH_STATUS_RESPONDED);
        recruitment.setLastContactTime(LocalDateTime.now());
        recruitment.setTalentStatus(TALENT_STATUS_OFFERING);
        normalizeRecruitment(recruitment);
        employeeRecruitmentMapper.updateById(recruitment);
    }

    private boolean prepareProfileFilter(EmployeeRecruitmentPageReqVO reqVO) {
        if (!StringUtils.hasText(reqVO.getProfileName()) && !StringUtils.hasText(reqVO.getProfileMobile())) {
            return true;
        }
        List<EmployeeProfileDO> profiles = employeeProfileMapper.selectList(new LambdaQueryWrapperX<EmployeeProfileDO>()
                .likeIfPresent(EmployeeProfileDO::getName, reqVO.getProfileName())
                .likeIfPresent(EmployeeProfileDO::getMobile, reqVO.getProfileMobile())
                .last("LIMIT 1000"));
        Set<Long> profileIds = profiles.stream()
                .map(EmployeeProfileDO::getId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(HashSet::new));
        if (profileIds.isEmpty()) {
            return false;
        }
        reqVO.setProfileIds(profileIds);
        return true;
    }

    private void fillProfileInfo(List<EmployeeRecruitmentRespVO> respList) {
        if (respList == null || respList.isEmpty()) {
            return;
        }
        Set<Long> profileIds = respList.stream()
                .map(EmployeeRecruitmentRespVO::getProfileId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(HashSet::new));
        if (profileIds.isEmpty()) {
            return;
        }
        Map<Long, EmployeeProfileDO> profileMap = employeeProfileMapper.selectBatchIds(profileIds).stream()
                .collect(Collectors.toMap(EmployeeProfileDO::getId, item -> item, (left, right) -> left));
        for (EmployeeRecruitmentRespVO item : respList) {
            EmployeeProfileDO profile = profileMap.get(item.getProfileId());
            if (profile == null) {
                continue;
            }
            item.setProfileName(profile.getName());
            item.setProfileMobile(profile.getMobile());
        }
    }

    private List<EmployeeRecruitmentStatsRespVO.StatItem> groupStats(
            List<EmployeeRecruitmentDO> list,
            java.util.function.Function<EmployeeRecruitmentDO, String> classifier) {
        Map<String, Integer> counts = new HashMap<>();
        for (EmployeeRecruitmentDO item : list) {
            String name = classifier.apply(item);
            if (!StringUtils.hasText(name)) {
                name = "未填写";
            }
            counts.put(name, counts.getOrDefault(name, 0) + 1);
        }
        return counts.entrySet().stream()
                .map(entry -> new EmployeeRecruitmentStatsRespVO.StatItem(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(EmployeeRecruitmentStatsRespVO.StatItem::getCount).reversed())
                .collect(Collectors.toList());
    }

    private boolean hasOffer(EmployeeRecruitmentDO item) {
        return item.getOfferDate() != null
                || StringUtils.hasText(item.getOfferStatus())
                || containsAny(item, "OFFER", "Offer", "offer", "已发", "待接受");
    }

    private boolean hasInterview(EmployeeRecruitmentDO item) {
        return item.getInterviewTime() != null
                || containsValue(item.getCandidateStage(), "面试")
                || containsStatus(item, "面试");
    }

    private boolean hasAcceptedOffer(EmployeeRecruitmentDO item) {
        return containsValue(item.getOfferStatus(), "已接受")
                || containsValue(item.getOfferStatus(), "ACCEPT")
                || containsValue(item.getOfferStatus(), "accept")
                || containsStatus(item, "待入职");
    }

    private boolean hasEntry(EmployeeRecruitmentDO item) {
        return TALENT_STATUS_HIRED.equals(item.getTalentStatus())
                || containsValue(item.getCandidateStage(), "已入职")
                || containsStatus(item, "已入职");
    }

    private boolean isPendingEntry(EmployeeRecruitmentDO item) {
        return hasOffer(item) && !hasEntry(item);
    }

    private boolean isOverdueFollow(EmployeeRecruitmentDO item) {
        return item.getNextFollowTime() != null
                && item.getNextFollowTime().isBefore(LocalDateTime.now())
                && !hasEntry(item)
                && !containsAny(item, "淘汰", "流失", "关闭");
    }

    private boolean isHighPriority(EmployeeRecruitmentDO item) {
        return containsValue(item.getPriority(), "HIGH")
                || containsValue(item.getPriority(), "高")
                || containsValue(item.getPriority(), "紧急");
    }

    private boolean isInTalentPool(EmployeeRecruitmentDO item) {
        return TALENT_STATUS_POOL.equals(resolveTalentStatus(item))
                || "INTERVIEWING".equals(resolveTalentStatus(item))
                || TALENT_STATUS_OFFERING.equals(resolveTalentStatus(item));
    }

    private boolean isBlacklisted(EmployeeRecruitmentDO item) {
        return TALENT_STATUS_BLACKLISTED.equals(resolveTalentStatus(item));
    }

    private boolean hasResume(EmployeeRecruitmentDO item) {
        return StringUtils.hasText(item.getResumeUrl());
    }

    private boolean hasResumeParsed(EmployeeRecruitmentDO item) {
        return RESUME_PARSE_STATUS_PARSED.equals(item.getResumeParseStatus())
                || StringUtils.hasText(item.getResumeSummary())
                || StringUtils.hasText(item.getResumeSkills())
                || item.getResumeParseTime() != null;
    }

    private ResumeParseResult buildResumeParseResult(EmployeeRecruitmentDO recruitment) {
        EmployeeProfileDO profile = recruitment.getProfileId() == null ? null : employeeProfileMapper.selectById(recruitment.getProfileId());
        String material = buildResumeMaterial(recruitment, profile);
        boolean hasMaterial = StringUtils.hasText(recruitment.getResumeUrl())
                || StringUtils.hasText(recruitment.getTalentTags())
                || StringUtils.hasText(recruitment.getRemark())
                || StringUtils.hasText(recruitment.getDemandReason());

        ResumeParseResult result = new ResumeParseResult();
        result.setStatus(hasMaterial ? RESUME_PARSE_STATUS_PARSED : RESUME_PARSE_STATUS_FAILED);
        result.setSkills(joinKeywords(inferResumeSkills(material)));
        result.setWorkYears(inferWorkYears(material));
        result.setEducation(inferEducation(material));
        result.setLastCompany(inferLastCompany(material));
        result.setSummary(buildResumeSummary(recruitment, profile, result, hasMaterial));
        return result;
    }

    private String buildResumeMaterial(EmployeeRecruitmentDO recruitment, EmployeeProfileDO profile) {
        List<String> parts = new ArrayList<>();
        if (profile != null) {
            parts.add(profile.getName());
            parts.add(profile.getMobile());
            parts.add(profile.getEmail());
        }
        parts.add(recruitment.getPosition());
        parts.add(recruitment.getCandidateStage());
        parts.add(recruitment.getTalentTags());
        parts.add(recruitment.getResumeUrl());
        parts.add(recruitment.getDemandReason());
        parts.add(recruitment.getRemark());
        parts.add(recruitment.getSource());
        parts.add(recruitment.getChannel());
        return parts.stream().filter(StringUtils::hasText).collect(Collectors.joining(" "));
    }

    private Set<String> inferResumeSkills(String material) {
        Set<String> skills = new LinkedHashSet<>();
        if (!StringUtils.hasText(material)) {
            return skills;
        }
        String lower = material.toLowerCase();
        addSkillIfPresent(skills, lower, "java", "Java");
        addSkillIfPresent(skills, lower, "spring", "Spring");
        addSkillIfPresent(skills, lower, "mysql", "MySQL");
        addSkillIfPresent(skills, lower, "python", "Python");
        addSkillIfPresent(skills, lower, "vue", "Vue");
        addSkillIfPresent(skills, lower, "react", "React");
        addSkillIfPresent(skills, lower, "typescript", "TypeScript");
        addSkillIfPresent(skills, lower, "javascript", "JavaScript");
        addSkillIfPresent(skills, material, "产品", "产品");
        addSkillIfPresent(skills, material, "测试", "测试");
        addSkillIfPresent(skills, material, "运维", "运维");
        addSkillIfPresent(skills, material, "设计", "设计");
        addSkillIfPresent(skills, material, "销售", "销售");
        addSkillIfPresent(skills, material, "人事", "人事");
        addSkillIfPresent(skills, material, "招聘", "招聘");
        addSkillIfPresent(skills, material, "客服", "客服");
        for (String token : material.split("[,，;；\\s]+")) {
            String trimmed = token == null ? null : token.trim();
            if (StringUtils.hasText(trimmed) && trimmed.length() <= 30 && (trimmed.matches("[A-Za-z0-9+#.\\-]+") || trimmed.length() <= 8)) {
                skills.add(trimmed);
            }
            if (skills.size() >= 12) {
                break;
            }
        }
        return skills;
    }

    private void addSkillIfPresent(Set<String> skills, String material, String keyword, String skill) {
        if (StringUtils.hasText(material) && material.contains(keyword)) {
            skills.add(skill);
        }
    }

    private String joinKeywords(Set<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return null;
        }
        return keywords.stream().limit(12).collect(Collectors.joining(","));
    }

    private BigDecimal inferWorkYears(String material) {
        if (!StringUtils.hasText(material)) {
            return null;
        }
        Matcher matcher = WORK_YEARS_PATTERN.matcher(material);
        while (matcher.find()) {
            try {
                BigDecimal years = new BigDecimal(matcher.group(1));
                if (years.compareTo(BigDecimal.ZERO) >= 0 && years.compareTo(BigDecimal.valueOf(60)) <= 0) {
                    return years.setScale(1, RoundingMode.HALF_UP);
                }
            } catch (NumberFormatException ignored) {
                // Ignore unparseable year fragments.
            }
        }
        return null;
    }

    private String inferEducation(String material) {
        if (!StringUtils.hasText(material)) {
            return null;
        }
        String[] educations = {"博士", "硕士", "研究生", "本科", "大专", "专科", "高中", "中专"};
        for (String education : educations) {
            if (material.contains(education)) {
                return education;
            }
        }
        return null;
    }

    private String inferLastCompany(String material) {
        if (!StringUtils.hasText(material)) {
            return null;
        }
        Matcher matcher = Pattern.compile("(?:最近|上一家|前公司|公司)[:： ]*([\\u4e00-\\u9fa5A-Za-z0-9（）()\\-_.]{2,40})").matcher(material);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String buildResumeSummary(EmployeeRecruitmentDO recruitment, EmployeeProfileDO profile,
                                      ResumeParseResult result, boolean hasMaterial) {
        List<String> pieces = new ArrayList<>();
        String name = profile == null ? null : profile.getName();
        if (StringUtils.hasText(name)) {
            pieces.add("候选人：" + name);
        }
        if (StringUtils.hasText(recruitment.getPosition())) {
            pieces.add("应聘岗位：" + recruitment.getPosition());
        }
        if (StringUtils.hasText(recruitment.getSource()) || StringUtils.hasText(recruitment.getChannel())) {
            pieces.add("来源：" + defaultText(recruitment.getSource(), recruitment.getChannel()));
        }
        if (StringUtils.hasText(result.getSkills())) {
            pieces.add("技能/标签：" + result.getSkills());
        }
        if (result.getWorkYears() != null) {
            pieces.add("工作年限：" + result.getWorkYears() + "年");
        }
        if (StringUtils.hasText(result.getEducation())) {
            pieces.add("学历：" + result.getEducation());
        }
        if (StringUtils.hasText(recruitment.getResumeUrl())) {
            pieces.add("简历：" + recruitment.getResumeUrl());
        }
        if (!hasMaterial) {
            pieces.add("缺少简历材料，请先补充简历地址、人才标签或候选人备注");
        } else {
            pieces.add("解析来源为候选人资料、简历地址和备注，建议 HR 复核后保存");
        }
        return truncate(String.join("；", pieces), 1000);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    @Data
    private static class ResumeParseResult {

        private String status;

        private String summary;

        private String skills;

        private BigDecimal workYears;

        private String education;

        private String lastCompany;
    }

    private boolean isDemandOpen(EmployeeRecruitmentDO item) {
        return hasDemandInfo(item)
                && !containsValue(item.getDemandStatus(), "CLOSED")
                && !containsValue(item.getDemandStatus(), "PAUSED")
                && !containsValue(item.getDemandStatus(), "关闭")
                && !containsValue(item.getDemandStatus(), "暂停");
    }

    private boolean isDemandApproved(EmployeeRecruitmentDO item) {
        return containsValue(item.getDemandStatus(), DEMAND_STATUS_APPROVED)
                || containsValue(item.getDemandStatus(), "已通过")
                || containsValue(item.getDemandStatus(), "通过");
    }

    private boolean hasDemandInfo(EmployeeRecruitmentDO item) {
        return StringUtils.hasText(item.getDemandCode())
                || StringUtils.hasText(item.getDemandDeptName())
                || item.getDemandHeadcount() != null
                || item.getDemandBudget() != null
                || StringUtils.hasText(item.getDemandReason())
                || StringUtils.hasText(item.getDemandStatus())
                || StringUtils.hasText(item.getDemandApprover());
    }

    private boolean hasDemandBusinessInfo(EmployeeRecruitmentDO item) {
        return StringUtils.hasText(item.getPosition())
                || StringUtils.hasText(item.getDemandCode())
                || StringUtils.hasText(item.getDemandDeptName())
                || item.getDemandHeadcount() != null
                || item.getDemandBudget() != null
                || StringUtils.hasText(item.getDemandReason());
    }

    private boolean hasInterviewEvaluation(EmployeeRecruitmentDO item) {
        return StringUtils.hasText(item.getInterviewResult())
                || item.getInterviewScore() != null
                || StringUtils.hasText(item.getInterviewDecision())
                || StringUtils.hasText(item.getInterviewFeedback())
                || item.getInterviewEvaluationTime() != null;
    }

    private boolean isInterviewPass(EmployeeRecruitmentDO item) {
        return containsValue(item.getInterviewDecision(), INTERVIEW_DECISION_PASS)
                || containsValue(item.getInterviewResult(), "通过")
                || containsValue(item.getInterviewResult(), "PASS");
    }

    private BigDecimal calculateAvgInterviewScore(List<EmployeeRecruitmentDO> list) {
        List<BigDecimal> scores = list.stream()
                .map(EmployeeRecruitmentDO::getInterviewScore)
                .filter(score -> score != null)
                .collect(Collectors.toList());
        if (scores.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(scores.size()), 1, RoundingMode.HALF_UP);
    }

    private boolean hasReferral(EmployeeRecruitmentDO item) {
        return StringUtils.hasText(item.getReferrerName()) || StringUtils.hasText(item.getReferrerMobile());
    }

    private boolean hasTouched(EmployeeRecruitmentDO item) {
        return item.getTouchTime() != null
                || containsValue(item.getTouchStatus(), TOUCH_STATUS_CONTACTED)
                || containsValue(item.getTouchStatus(), TOUCH_STATUS_RESPONDED);
    }

    private boolean hasResponse(EmployeeRecruitmentDO item) {
        return containsValue(item.getTouchStatus(), TOUCH_STATUS_RESPONDED)
                || containsValue(item.getTouchStatus(), "已回复")
                || containsValue(item.getTouchStatus(), "回复");
    }

    private BigDecimal calculateChannelCostTotal(List<EmployeeRecruitmentDO> list) {
        return list.stream()
                .map(EmployeeRecruitmentDO::getChannelCost)
                .filter(cost -> cost != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<EmployeeRecruitmentStatsRespVO.ChannelEffectItem> buildChannelEffectStats(
            List<EmployeeRecruitmentDO> list) {
        Map<String, EmployeeRecruitmentStatsRespVO.ChannelEffectItem> effectMap = new HashMap<>();
        for (EmployeeRecruitmentDO item : list) {
            String name = StringUtils.hasText(item.getChannel()) ? item.getChannel().trim() : "未填写";
            EmployeeRecruitmentStatsRespVO.ChannelEffectItem effectItem = effectMap.computeIfAbsent(
                    name, EmployeeRecruitmentStatsRespVO.ChannelEffectItem::new);
            effectItem.setCandidateCount(effectItem.getCandidateCount() + 1);
            if (hasTouched(item)) {
                effectItem.setTouchedCount(effectItem.getTouchedCount() + 1);
            }
            if (hasResponse(item)) {
                effectItem.setResponseCount(effectItem.getResponseCount() + 1);
            }
            if (hasInterview(item)) {
                effectItem.setInterviewCount(effectItem.getInterviewCount() + 1);
            }
            if (hasOffer(item)) {
                effectItem.setOfferCount(effectItem.getOfferCount() + 1);
            }
            if (hasEntry(item)) {
                effectItem.setEntryCount(effectItem.getEntryCount() + 1);
            }
            if (item.getChannelCost() != null) {
                effectItem.setChannelCost(effectItem.getChannelCost().add(item.getChannelCost()));
            }
        }
        List<EmployeeRecruitmentStatsRespVO.ChannelEffectItem> effectList = new ArrayList<>(effectMap.values());
        for (EmployeeRecruitmentStatsRespVO.ChannelEffectItem item : effectList) {
            item.setResponseRate(percent(item.getResponseCount(), item.getCandidateCount()));
            item.setInterviewRate(percent(item.getInterviewCount(), item.getCandidateCount()));
            item.setOfferRate(percent(item.getOfferCount(), item.getCandidateCount()));
            item.setEntryRate(percent(item.getEntryCount(), item.getCandidateCount()));
            item.setCostPerCandidate(average(item.getChannelCost(), item.getCandidateCount()));
            item.setCostPerEntry(average(item.getChannelCost(), item.getEntryCount()));
        }
        return effectList.stream()
                .sorted(Comparator.comparing(
                                EmployeeRecruitmentStatsRespVO.ChannelEffectItem::getEntryCount,
                                Comparator.nullsFirst(Integer::compareTo))
                        .reversed()
                        .thenComparing(
                                Comparator.comparing(
                                        EmployeeRecruitmentStatsRespVO.ChannelEffectItem::getCandidateCount,
                                        Comparator.nullsFirst(Integer::compareTo))
                                        .reversed()))
                .collect(Collectors.toList());
    }

    private BigDecimal average(BigDecimal total, Integer count) {
        if (total == null || count == null || count <= 0) {
            return BigDecimal.ZERO;
        }
        return total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal percent(Integer numerator, Integer denominator) {
        if (numerator == null || denominator == null || denominator <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
    }

    private void normalizeRecruitment(EmployeeRecruitmentDO recruitment) {
        fillDefaultTouchStatus(recruitment);
        fillDefaultDemandStatus(recruitment);
        fillInterviewEvaluation(recruitment);
        fillDefaultTalentStatus(recruitment);
    }

    private void validateRecruitmentFlow(EmployeeRecruitmentDO recruitment) {
        if (isDemandClosed(recruitment) && hasRecruitmentProgress(recruitment)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "招聘需求已暂停或关闭，不能继续推进");
        }
        if (hasDemandInfo(recruitment) && hasRecruitmentProgress(recruitment) && !isDemandApproved(recruitment)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "招聘需求未通过，不能推进候选人流程");
        }
        if (hasOffer(recruitment) && !isInterviewPass(recruitment)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "面试未通过或未评价，不能推进 Offer");
        }
        if (hasEntry(recruitment) && !hasAcceptedOfferStatus(recruitment)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "Offer 未接受，不能转入职或标记入职");
        }
    }

    private void validateRecruitmentCanConvert(EmployeeRecruitmentDO recruitment) {
        if (!hasAcceptedOfferStatus(recruitment)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "Offer 未接受，不能转入职");
        }
        if (!isInterviewPass(recruitment)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "面试未通过或未评价，不能转入职");
        }
        if (hasDemandInfo(recruitment) && !isDemandApproved(recruitment)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "招聘需求未通过，不能转入职");
        }
        if (isDemandClosed(recruitment)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "招聘需求已暂停或关闭，不能转入职");
        }
    }

    private boolean hasRecruitmentProgress(EmployeeRecruitmentDO recruitment) {
        return hasInterview(recruitment)
                || hasOffer(recruitment)
                || hasEntry(recruitment)
                || containsValue(recruitment.getCandidateStage(), "待入职")
                || containsValue(recruitment.getStatus(), "待入职");
    }

    private boolean isDemandClosed(EmployeeRecruitmentDO recruitment) {
        return containsValue(recruitment.getDemandStatus(), DEMAND_STATUS_CLOSED)
                || containsValue(recruitment.getDemandStatus(), DEMAND_STATUS_PAUSED)
                || containsValue(recruitment.getDemandStatus(), "关闭")
                || containsValue(recruitment.getDemandStatus(), "暂停");
    }

    private boolean hasAcceptedOfferStatus(EmployeeRecruitmentDO item) {
        return containsValue(item.getOfferStatus(), "已接受")
                || containsValue(item.getOfferStatus(), "ACCEPT")
                || containsValue(item.getOfferStatus(), "accept");
    }

    private void fillDefaultTouchStatus(EmployeeRecruitmentDO recruitment) {
        if (StringUtils.hasText(recruitment.getTouchStatus())) {
            return;
        }
        if (recruitment.getTouchTime() != null || StringUtils.hasText(recruitment.getTouchRemark())) {
            recruitment.setTouchStatus(TOUCH_STATUS_CONTACTED);
        }
    }

    private void fillDefaultDemandStatus(EmployeeRecruitmentDO recruitment) {
        if (StringUtils.hasText(recruitment.getDemandStatus()) || !hasDemandInfo(recruitment)) {
            return;
        }
        recruitment.setDemandStatus(DEMAND_STATUS_DRAFT);
    }

    private void fillInterviewEvaluation(EmployeeRecruitmentDO recruitment) {
        if (StringUtils.hasText(recruitment.getInterviewDecision())
                && !StringUtils.hasText(recruitment.getInterviewResult())) {
            recruitment.setInterviewResult(interviewDecisionText(recruitment.getInterviewDecision()));
        }
        if (hasInterviewEvaluation(recruitment) && recruitment.getInterviewEvaluationTime() == null) {
            recruitment.setInterviewEvaluationTime(LocalDateTime.now());
        }
    }

    private String interviewDecisionText(String decision) {
        if (INTERVIEW_DECISION_PASS.equals(decision)) {
            return "通过";
        }
        if (INTERVIEW_DECISION_REJECT.equals(decision)) {
            return "淘汰";
        }
        if (INTERVIEW_DECISION_PENDING.equals(decision)) {
            return "待定";
        }
        return decision;
    }

    private void fillDefaultTalentStatus(EmployeeRecruitmentDO recruitment) {
        if (StringUtils.hasText(recruitment.getTalentStatus())) {
            return;
        }
        recruitment.setTalentStatus(resolveTalentStatus(recruitment));
    }

    private String resolveTalentStatus(EmployeeRecruitmentDO item) {
        if (StringUtils.hasText(item.getTalentStatus())) {
            return item.getTalentStatus().trim();
        }
        if (hasEntry(item)) {
            return TALENT_STATUS_HIRED;
        }
        if (containsAny(item, "淘汰", "流失", "关闭")) {
            return "ELIMINATED";
        }
        if (hasOffer(item)) {
            return TALENT_STATUS_OFFERING;
        }
        if (hasInterview(item)) {
            return "INTERVIEWING";
        }
        return TALENT_STATUS_POOL;
    }

    private boolean containsStatus(EmployeeRecruitmentDO item, String keyword) {
        return item.getStatus() != null && item.getStatus().contains(keyword);
    }

    private boolean containsAny(EmployeeRecruitmentDO item, String... keywords) {
        for (String keyword : keywords) {
            if (containsStatus(item, keyword)
                    || containsValue(item.getCandidateStage(), keyword)
                    || containsValue(item.getInterviewResult(), keyword)
                    || containsValue(item.getInterviewDecision(), keyword)
                    || containsValue(item.getInterviewFeedback(), keyword)
                    || containsValue(item.getOfferStatus(), keyword)
                    || containsValue(item.getLossReason(), keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsValue(String value, String keyword) {
        return value != null && value.contains(keyword);
    }

    private String trimText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
