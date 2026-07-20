package com.kyx.service.hr.service.risk;

import com.kyx.foundation.common.exception.enums.GlobalErrorCodeConstants;
import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.foundation.security.core.util.SecurityFrameworkUtils;
import com.kyx.service.business.api.dept.DeptApi;
import com.kyx.service.business.api.dept.dto.DeptRespDTO;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import com.kyx.service.hr.controller.admin.employee.vo.master.EmployeeDataQualityIssueRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.master.EmployeeDataQualityRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.master.EmployeeMasterWorkbenchRespVO;
import com.kyx.service.hr.controller.admin.risk.vo.HrRiskEventBatchHandleReqVO;
import com.kyx.service.hr.controller.admin.risk.vo.HrRiskEventCreateReqVO;
import com.kyx.service.hr.controller.admin.risk.vo.HrRiskEventHandleReqVO;
import com.kyx.service.hr.controller.admin.risk.vo.HrRiskEventPageReqVO;
import com.kyx.service.hr.controller.admin.risk.vo.HrRiskEventRespVO;
import com.kyx.service.hr.controller.admin.risk.vo.HrRiskWorkbenchRespVO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeEntryDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeePerformanceDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeProfileDO;
import com.kyx.service.hr.dal.dataobject.payroll.SocialSecurityAccountDO;
import com.kyx.service.hr.dal.dataobject.risk.HrRiskEventDO;
import com.kyx.service.hr.dal.dataobject.todo.HrTodoTaskDO;
import com.kyx.service.hr.dal.mysql.employee.EmployeeEntryMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeePerformanceMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeProfileMapper;
import com.kyx.service.hr.dal.mysql.payroll.SocialSecurityAccountMapper;
import com.kyx.service.hr.dal.mysql.risk.HrRiskEventMapper;
import com.kyx.service.hr.dal.mysql.todo.HrTodoTaskMapper;
import com.kyx.service.hr.service.employee.EmployeeMasterDataService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Validated
public class HrRiskWorkbenchServiceImpl implements HrRiskWorkbenchService {

    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_RESOLVED = "RESOLVED";
    private static final String STATUS_IGNORED = "IGNORED";

    private static final String SEVERITY_HIGH = "HIGH";
    private static final String SEVERITY_MEDIUM = "MEDIUM";
    private static final String SEVERITY_LOW = "LOW";

    private static final String SOURCE_DATA_QUALITY = "DATA_QUALITY";
    private static final String SOURCE_MANUAL = "MANUAL";
    private static final String SOURCE_PERFORMANCE = "PERFORMANCE";
    private static final String SOURCE_SOCIAL_SECURITY = "SOCIAL_SECURITY";
    private static final String SOURCE_TODO = "TODO";
    private static final String BUSINESS_RISK_EVENT = "RISK_EVENT";
    private static final String TODO_STATUS_OPEN = "OPEN";
    private static final String TODO_TASK_DEFAULT = "DEFAULT";
    private static final String SOCIAL_STATUS_ENROLLED = "ENROLLED";

    private static final String ISSUE_CONTRACT_EXPIRING = "CONTRACT_EXPIRING";
    private static final String ISSUE_PROBATION_DUE = "PROBATION_DUE";
    private static final String ISSUE_PERFORMANCE_WARNING = "PERFORMANCE_WARNING";
    private static final String ISSUE_SOCIAL_SECURITY_MISSING = "SOCIAL_SECURITY_MISSING";
    private static final String ISSUE_TODO_OPEN = "TODO_OPEN";

    private static final int WORKBENCH_EVENT_LIMIT = 1000;
    private static final int STALE_EVENT_LIMIT = 5000;
    private static final int SOCIAL_SECURITY_ENTRY_LIMIT = 5000;
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Resource
    private EmployeeMasterDataService employeeMasterDataService;
    @Resource
    private EmployeePerformanceMapper employeePerformanceMapper;
    @Resource
    private EmployeeEntryMapper employeeEntryMapper;
    @Resource
    private EmployeeMapper employeeMapper;
    @Resource
    private EmployeeProfileMapper employeeProfileMapper;
    @Resource
    private HrTodoTaskMapper hrTodoTaskMapper;
    @Resource
    private SocialSecurityAccountMapper socialSecurityAccountMapper;
    @Resource
    private HrRiskEventMapper hrRiskEventMapper;
    @Resource
    private DeptApi deptApi;
    @Resource
    private AdminUserApi adminUserApi;

    @Override
    @Cacheable(cacheNames = "hr:risk:workbench#15s", key = "'current'", sync = true)
    public HrRiskWorkbenchRespVO getWorkbench() {
        RiskSnapshot snapshot = buildSnapshot();
        refreshGeneratedEvents(snapshot);

        List<HrRiskEventDO> activeEvents = hrRiskEventMapper.selectActiveList(WORKBENCH_EVENT_LIMIT);
        HrRiskWorkbenchRespVO respVO = new HrRiskWorkbenchRespVO();
        respVO.setSummary(buildSummary(snapshot, activeEvents));
        respVO.setCategories(buildCategories(activeEvents));
        respVO.setPriorityItems(buildPriorityItems(activeEvents));
        respVO.setQuickActions(buildQuickActions());
        return respVO;
    }

    @Override
    public PageResult<HrRiskEventRespVO> getEventPage(HrRiskEventPageReqVO pageReqVO) {
        applyDeptScope(pageReqVO);
        refreshGeneratedEvents(buildSnapshot());
        PageResult<HrRiskEventDO> pageResult = hrRiskEventMapper.selectPage(pageReqVO);
        List<HrRiskEventRespVO> respList = BeanUtils.toBean(pageResult.getList(), HrRiskEventRespVO.class);
        fillEventPeopleInfo(pageResult.getList(), respList);
        return new PageResult<>(respList, pageResult.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEvent(HrRiskEventCreateReqVO reqVO) {
        if (!isAllowedSeverity(reqVO.getSeverity())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "不支持的风险等级");
        }
        if (reqVO.getProfileId() != null && employeeProfileMapper.selectById(reqVO.getProfileId()) == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND, "员工档案不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        HrRiskEventDO event = new HrRiskEventDO();
        event.setSourceType(SOURCE_MANUAL);
        event.setSourceKey(SOURCE_MANUAL + ":" + UUID.randomUUID());
        event.setIssueType(defaultText(reqVO.getIssueType(), SOURCE_MANUAL));
        event.setSeverity(normalizeSeverity(reqVO.getSeverity()));
        event.setTitle(reqVO.getTitle().trim());
        event.setDescription(reqVO.getDescription());
        event.setAction(reqVO.getAction());
        event.setProfileId(reqVO.getProfileId());
        event.setOwnerUserId(reqVO.getOwnerUserId() == null
                ? resolveRiskOwnerUserId(reqVO.getProfileId()) : reqVO.getOwnerUserId());
        event.setRoutePath(StringUtils.hasText(reqVO.getRoutePath())
                ? reqVO.getRoutePath().trim() : employeeRoute(reqVO.getProfileId(), "basic"));
        event.setDueTime(reqVO.getDueTime() == null ? defaultDueTime(reqVO.getSeverity()) : reqVO.getDueTime());
        event.setStatus(STATUS_OPEN);
        event.setGeneratedFlag(false);
        event.setFirstSeenTime(now);
        event.setLastSeenTime(now);
        event.setRemark(reqVO.getRemark());
        hrRiskEventMapper.insert(event);
        return event.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleEvent(HrRiskEventHandleReqVO reqVO) {
        handleEventInternal(reqVO.getId(), reqVO.getStatus(), reqVO.getOwnerUserId(),
                reqVO.getHandleResult(), reqVO.getRemark());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer batchHandleEvents(HrRiskEventBatchHandleReqVO reqVO) {
        if (!isAllowedStatus(reqVO.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "不支持的风险处理状态");
        }
        int changed = 0;
        for (Long id : reqVO.getIds()) {
            if (id == null) {
                continue;
            }
            handleEventInternal(id, reqVO.getStatus(), reqVO.getOwnerUserId(),
                    reqVO.getHandleResult(), reqVO.getRemark());
            changed++;
        }
        return changed;
    }

    private void handleEventInternal(Long id, String status, Long ownerUserId, String handleResult, String remark) {
        HrRiskEventDO existing = hrRiskEventMapper.selectById(id);
        if (existing == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND, "风险事件不存在");
        }
        if (!isAllowedStatus(status)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "不支持的风险处理状态");
        }
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        HrRiskEventDO updateDO = new HrRiskEventDO();
        updateDO.setId(id);
        updateDO.setStatus(status);
        updateDO.setOwnerUserId(ownerUserId);
        updateDO.setHandleResult(handleResult);
        updateDO.setRemark(remark);
        if (STATUS_PROCESSING.equals(status) && ownerUserId == null && existing.getOwnerUserId() == null) {
            updateDO.setOwnerUserId(loginUserId);
        }
        if (!STATUS_OPEN.equals(status) || StringUtils.hasText(handleResult)) {
            updateDO.setHandledBy(loginUserId);
            updateDO.setHandledTime(LocalDateTime.now());
        }
        hrRiskEventMapper.updateById(updateDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer refreshGeneratedEvents() {
        return refreshGeneratedEvents(buildSnapshot());
    }

    private RiskSnapshot buildSnapshot() {
        EmployeeMasterWorkbenchRespVO master = employeeMasterDataService.getWorkbench();
        EmployeeDataQualityRespVO quality = employeeMasterDataService.getDataQuality();
        List<EmployeePerformanceDO> latestPerformances = loadLatestPerformances();
        List<EmployeePerformanceDO> performanceWarnings = latestPerformances.stream()
                .filter(this::isPerformanceWarning)
                .collect(Collectors.toList());
        List<HrTodoTaskDO> openTodos = loadOpenTodos();
        Map<Long, EmployeeProfileDO> profileMap = loadProfiles(quality, performanceWarnings, openTodos);
        List<HrRiskWorkbenchRespVO.RiskItem> sourceItems = buildSourceItems(quality, performanceWarnings, openTodos, profileMap);
        sourceItems.addAll(buildSocialSecurityRiskItems());
        return new RiskSnapshot(master, quality, latestPerformances, performanceWarnings, openTodos, sourceItems);
    }

    private Integer refreshGeneratedEvents(RiskSnapshot snapshot) {
        Set<String> currentKeys = new HashSet<>();
        int changed = 0;
        for (HrRiskWorkbenchRespVO.RiskItem item : snapshot.getSourceItems()) {
            if (!StringUtils.hasText(item.getSourceType()) || !StringUtils.hasText(item.getSourceKey())) {
                continue;
            }
            currentKeys.add(eventKey(item.getSourceType(), item.getSourceKey()));
            changed += upsertGeneratedEvent(item);
        }
        changed += closeStaleGeneratedEvents(currentKeys);
        return changed;
    }

    private int upsertGeneratedEvent(HrRiskWorkbenchRespVO.RiskItem item) {
        HrRiskEventDO existing = hrRiskEventMapper.selectBySource(item.getSourceType(), item.getSourceKey());
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            HrRiskEventDO insertDO = new HrRiskEventDO();
            applyItem(insertDO, item);
            insertDO.setOwnerUserId(resolveRiskOwnerUserId(item.getProfileId()));
            insertDO.setStatus(STATUS_OPEN);
            insertDO.setGeneratedFlag(true);
            insertDO.setFirstSeenTime(now);
            insertDO.setLastSeenTime(now);
            hrRiskEventMapper.insert(insertDO);
            return 1;
        }
        HrRiskEventDO updateDO = new HrRiskEventDO();
        updateDO.setId(existing.getId());
        applyItem(updateDO, item);
        if (existing.getOwnerUserId() == null) {
            updateDO.setOwnerUserId(resolveRiskOwnerUserId(item.getProfileId()));
        }
        updateDO.setGeneratedFlag(true);
        updateDO.setLastSeenTime(now);
        if (!StringUtils.hasText(existing.getStatus())) {
            updateDO.setStatus(STATUS_OPEN);
        }
        hrRiskEventMapper.updateById(updateDO);
        return 1;
    }

    private void applyItem(HrRiskEventDO target, HrRiskWorkbenchRespVO.RiskItem item) {
        target.setSourceType(item.getSourceType());
        target.setSourceKey(item.getSourceKey());
        target.setIssueType(item.getIssueType());
        target.setSeverity(normalizeSeverity(item.getSeverity()));
        target.setTitle(item.getTitle());
        target.setDescription(item.getDescription());
        target.setAction(item.getAction());
        target.setProfileId(item.getProfileId());
        target.setRoutePath(item.getRoutePath());
        target.setDueTime(item.getDueTime());
    }

    private int closeStaleGeneratedEvents(Set<String> currentKeys) {
        List<HrRiskEventDO> openEvents = hrRiskEventMapper.selectActiveGeneratedList(STALE_EVENT_LIMIT);
        int changed = 0;
        LocalDateTime now = LocalDateTime.now();
        for (HrRiskEventDO event : openEvents) {
            if (currentKeys.contains(eventKey(event.getSourceType(), event.getSourceKey()))) {
                continue;
            }
            HrRiskEventDO updateDO = new HrRiskEventDO();
            updateDO.setId(event.getId());
            updateDO.setStatus(STATUS_RESOLVED);
            updateDO.setHandledTime(now);
            updateDO.setHandleResult("来源风险已消失，系统自动关闭");
            hrRiskEventMapper.updateById(updateDO);
            changed++;
        }
        return changed;
    }

    private List<EmployeePerformanceDO> loadLatestPerformances() {
        List<EmployeePerformanceDO> list = employeePerformanceMapper.selectList(new LambdaQueryWrapperX<EmployeePerformanceDO>()
                .orderByDesc(EmployeePerformanceDO::getEvaluatedDate)
                .orderByDesc(EmployeePerformanceDO::getId)
                .last("LIMIT 5000"));
        Map<Long, EmployeePerformanceDO> latestMap = new LinkedHashMap<>();
        for (EmployeePerformanceDO item : list) {
            if (item.getProfileId() != null) {
                latestMap.putIfAbsent(item.getProfileId(), item);
            }
        }
        return new ArrayList<>(latestMap.values());
    }

    private List<HrTodoTaskDO> loadOpenTodos() {
        return hrTodoTaskMapper.selectList(new LambdaQueryWrapperX<HrTodoTaskDO>()
                .eq(HrTodoTaskDO::getStatus, TODO_STATUS_OPEN)
                .and(query -> query.isNull(HrTodoTaskDO::getBusinessType)
                        .or()
                        .ne(HrTodoTaskDO::getBusinessType, BUSINESS_RISK_EVENT))
                .orderByAsc(HrTodoTaskDO::getDueTime)
                .orderByDesc(HrTodoTaskDO::getId)
                .last("LIMIT 1000"));
    }

    private Map<Long, EmployeeProfileDO> loadProfiles(EmployeeDataQualityRespVO quality,
                                                      List<EmployeePerformanceDO> performanceWarnings,
                                                      List<HrTodoTaskDO> openTodos) {
        Set<Long> profileIds = new HashSet<>();
        if (quality != null && quality.getIssues() != null) {
            quality.getIssues().stream()
                    .map(EmployeeDataQualityIssueRespVO::getProfileId)
                    .filter(Objects::nonNull)
                    .forEach(profileIds::add);
        }
        performanceWarnings.stream()
                .map(EmployeePerformanceDO::getProfileId)
                .filter(Objects::nonNull)
                .forEach(profileIds::add);
        openTodos.stream()
                .map(HrTodoTaskDO::getProfileId)
                .filter(Objects::nonNull)
                .forEach(profileIds::add);
        return loadProfileMap(profileIds);
    }

    private List<HrRiskWorkbenchRespVO.RiskItem> buildSourceItems(EmployeeDataQualityRespVO quality,
                                                                  List<EmployeePerformanceDO> performanceWarnings,
                                                                  List<HrTodoTaskDO> openTodos,
                                                                  Map<Long, EmployeeProfileDO> profileMap) {
        List<HrRiskWorkbenchRespVO.RiskItem> items = new ArrayList<>();
        if (quality != null && quality.getIssues() != null) {
            quality.getIssues().stream()
                    .sorted(Comparator.comparingInt(issue -> severityRank(issue.getSeverity())))
                    .forEach(issue -> items.add(fromQualityIssue(issue)));
        }
        performanceWarnings.forEach(item -> items.add(fromPerformanceWarning(item, profileMap.get(item.getProfileId()))));
        openTodos.stream()
                .filter(item -> isHighPriority(item.getPriority()) || isOverdue(item))
                .forEach(item -> items.add(fromTodo(item, profileMap.get(item.getProfileId()))));
        return items;
    }

    private HrRiskWorkbenchRespVO.Summary buildSummary(RiskSnapshot snapshot, List<HrRiskEventDO> activeEvents) {
        HrRiskWorkbenchRespVO.Summary summary = new HrRiskWorkbenchRespVO.Summary();
        EmployeeMasterWorkbenchRespVO master = snapshot.getMaster();
        summary.setTotalProfiles(orZero(master.getTotalProfiles()));
        summary.setDataQualityScore(orZero(master.getDataQualityScore()));
        summary.setHighRiskCount(countSeverity(activeEvents, SEVERITY_HIGH));
        summary.setMediumRiskCount(countSeverity(activeEvents, SEVERITY_MEDIUM));
        summary.setLowRiskCount(countSeverity(activeEvents, SEVERITY_LOW));
        summary.setTotalRiskCount(summary.getHighRiskCount() + summary.getMediumRiskCount() + summary.getLowRiskCount());
        summary.setContractExpiringCount(orZero(master.getContractExpiringCount()));
        summary.setProbationDueCount(orZero(master.getProbationDueCount()));
        summary.setPerformanceWarningCount(snapshot.getPerformanceWarnings().size());
        summary.setOpenTodoCount(snapshot.getOpenTodos().size());
        summary.setOverdueTodoCount(countOverdueTodos(snapshot.getOpenTodos()));
        summary.setHighPriorityTodoCount(countHighPriorityTodos(snapshot.getOpenTodos()));
        summary.setAvgPerformanceScore(avgScore(snapshot.getLatestPerformances()));
        summary.setOpenEventCount(countStatus(STATUS_OPEN));
        summary.setProcessingEventCount(countStatus(STATUS_PROCESSING));
        summary.setResolvedEventCount(countStatus(STATUS_RESOLVED));
        summary.setIgnoredEventCount(countStatus(STATUS_IGNORED));
        return summary;
    }

    private List<HrRiskWorkbenchRespVO.RiskCategory> buildCategories(List<HrRiskEventDO> activeEvents) {
        List<HrRiskWorkbenchRespVO.RiskCategory> categories = new ArrayList<>();
        categories.add(category("DATA_QUALITY", "主数据质量", highestSeverity(activeEvents, "DATA_QUALITY"),
                countCategory(activeEvents, "DATA_QUALITY"), "档案、账号、部门、钉钉绑定和重复证件问题",
                "lucide:database", "/hr/employee/master-data"));
        categories.add(category("CONTRACT", "合同合规", highestSeverity(activeEvents, "CONTRACT"),
                countCategory(activeEvents, "CONTRACT"), "30 天内合同到期，需要续签或终止处理",
                "lucide:file-warning", "/hr/employee/master-data"));
        categories.add(category("PROBATION", "试用转正", highestSeverity(activeEvents, "PROBATION"),
                countCategory(activeEvents, "PROBATION"), "30 天内需要转正确认的员工",
                "lucide:badge-check", "/hr/lifecycle/workbench"));
        categories.add(category("PERFORMANCE", "绩效预警", highestSeverity(activeEvents, "PERFORMANCE"),
                countCategory(activeEvents, "PERFORMANCE"), "最新绩效低分或结果不合格员工",
                "lucide:triangle-alert", "/hr/performance"));
        categories.add(category("TODO", "待办积压", highestSeverity(activeEvents, "TODO"),
                countCategory(activeEvents, "TODO"), "开放、逾期或高优先级 HR 待办",
                "lucide:list-checks", "/hr/todo"));
        categories.add(category("SOCIAL_SECURITY", "社保合规", highestSeverity(activeEvents, "SOCIAL_SECURITY"),
                countCategory(activeEvents, "SOCIAL_SECURITY"), "在职员工当月社保公积金台账缺失或未参保",
                "lucide:landmark", "/hr/payroll?tab=social"));
        categories.add(category("MANUAL", "人工登记", highestSeverity(activeEvents, "MANUAL"),
                countCategory(activeEvents, "MANUAL"), "HR 主动登记的合规、用工或员工服务风险",
                "lucide:pen-line", "/hr/risk?sourceType=MANUAL"));
        return categories;
    }

    private List<HrRiskWorkbenchRespVO.RiskItem> buildPriorityItems(List<HrRiskEventDO> activeEvents) {
        Map<Long, EmployeeProfileDO> profileMap = loadProfileMap(activeEvents.stream()
                .map(HrRiskEventDO::getProfileId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        return activeEvents.stream()
                .sorted(Comparator
                        .comparingInt((HrRiskEventDO item) -> severityRank(item.getSeverity()))
                        .thenComparing(item -> item.getDueTime() == null ? LocalDateTime.MAX : item.getDueTime()))
                .limit(24)
                .map(item -> fromEvent(item, profileMap.get(item.getProfileId())))
                .collect(Collectors.toList());
    }

    private HrRiskWorkbenchRespVO.RiskItem fromEvent(HrRiskEventDO event, EmployeeProfileDO profile) {
        HrRiskWorkbenchRespVO.RiskItem item = new HrRiskWorkbenchRespVO.RiskItem();
        item.setId(event.getId());
        item.setSourceType(event.getSourceType());
        item.setSourceKey(event.getSourceKey());
        item.setSeverity(event.getSeverity());
        item.setIssueType(event.getIssueType());
        item.setTitle(event.getTitle());
        item.setDescription(event.getDescription());
        item.setAction(event.getAction());
        item.setProfileId(event.getProfileId());
        item.setEmployeeName(profile == null ? null : profile.getName());
        item.setMobile(profile == null ? null : profile.getMobile());
        item.setRoutePath(event.getRoutePath());
        item.setDueTime(event.getDueTime());
        item.setStatus(event.getStatus());
        return item;
    }

    private HrRiskWorkbenchRespVO.RiskItem fromQualityIssue(EmployeeDataQualityIssueRespVO issue) {
        HrRiskWorkbenchRespVO.RiskItem item = new HrRiskWorkbenchRespVO.RiskItem();
        item.setSourceType(SOURCE_DATA_QUALITY);
        item.setSourceKey(issueSourceKey(issue));
        item.setSeverity(issue.getSeverity());
        item.setIssueType(issue.getIssueType());
        item.setTitle(issue.getIssueName());
        item.setDescription(issue.getDescription());
        item.setAction(issue.getAction());
        item.setProfileId(issue.getProfileId());
        item.setEmployeeName(issue.getEmployeeName());
        item.setMobile(issue.getMobile());
        item.setRoutePath(employeeRoute(issue.getProfileId(), "basic"));
        item.setDueTime(defaultDueTime(issue.getSeverity()));
        return item;
    }

    private HrRiskWorkbenchRespVO.RiskItem fromPerformanceWarning(EmployeePerformanceDO performance,
                                                                  EmployeeProfileDO profile) {
        HrRiskWorkbenchRespVO.RiskItem item = new HrRiskWorkbenchRespVO.RiskItem();
        item.setSourceType(SOURCE_PERFORMANCE);
        item.setSourceKey("PERFORMANCE:" + defaultText(performance.getId(), performance.getProfileId()));
        item.setSeverity(SEVERITY_HIGH);
        item.setIssueType(ISSUE_PERFORMANCE_WARNING);
        item.setTitle("绩效需要关注");
        item.setDescription("最新绩效：" + empty(performance.getGrade()) + " "
                + empty(performance.getResult()) + "，得分 " + formatScore(performance.getScore()));
        item.setAction("进入员工绩效页复盘目标、辅导计划和改进记录");
        item.setProfileId(performance.getProfileId());
        item.setEmployeeName(profile == null ? null : profile.getName());
        item.setMobile(profile == null ? null : profile.getMobile());
        item.setRoutePath(employeeRoute(performance.getProfileId(), "performance"));
        item.setDueTime(performanceDueTime(performance));
        return item;
    }

    private HrRiskWorkbenchRespVO.RiskItem fromTodo(HrTodoTaskDO todo, EmployeeProfileDO profile) {
        HrRiskWorkbenchRespVO.RiskItem item = new HrRiskWorkbenchRespVO.RiskItem();
        item.setSourceType(SOURCE_TODO);
        item.setSourceKey("TODO:" + todo.getId());
        item.setSeverity(isOverdue(todo) || isHighPriority(todo.getPriority()) ? SEVERITY_HIGH : SEVERITY_MEDIUM);
        item.setIssueType(ISSUE_TODO_OPEN);
        item.setTitle(StringUtils.hasText(todo.getTitle()) ? todo.getTitle() : "待处理 HR 待办");
        item.setDescription(todo.getContent());
        item.setAction("进入待办中心处理");
        item.setProfileId(todo.getProfileId());
        item.setEmployeeName(profile == null ? null : profile.getName());
        item.setMobile(profile == null ? null : profile.getMobile());
        item.setRoutePath(StringUtils.hasText(todo.getRoutePath()) ? todo.getRoutePath() : "/hr/todo");
        item.setDueTime(todo.getDueTime());
        return item;
    }

    private List<HrRiskWorkbenchRespVO.RiskItem> buildSocialSecurityRiskItems() {
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
        String socialMonth = monthStart.format(MONTH_FORMATTER);
        if (!hasSocialSecurityLedger(socialMonth)) {
            return Collections.emptyList();
        }
        List<EmployeeEntryDO> entries = loadSocialSecurityCheckEntries(monthStart, monthEnd);
        Map<Long, EmployeeEntryDO> entryMap = new LinkedHashMap<>();
        for (EmployeeEntryDO entry : emptyIfNull(entries)) {
            if (!shouldCheckSocialSecurity(entry, monthStart, monthEnd)) {
                continue;
            }
            entryMap.putIfAbsent(entry.getProfileId(), entry);
        }
        if (entryMap.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> profileIds = new HashSet<>(entryMap.keySet());
        Map<Long, EmployeeProfileDO> profileMap = loadProfileMap(profileIds);
        Map<Long, SocialSecurityAccountDO> socialSecurityMap = loadSocialSecurityMap(profileIds, socialMonth);
        List<HrRiskWorkbenchRespVO.RiskItem> items = new ArrayList<>();
        for (EmployeeEntryDO entry : entryMap.values()) {
            SocialSecurityAccountDO account = socialSecurityMap.get(entry.getProfileId());
            if (isSocialEnrolled(account)) {
                continue;
            }
            items.add(fromSocialSecurityMissing(entry, account, profileMap.get(entry.getProfileId()), socialMonth));
        }
        return items;
    }

    private boolean hasSocialSecurityLedger(String socialMonth) {
        if (!StringUtils.hasText(socialMonth)) {
            return false;
        }
        return socialSecurityAccountMapper.selectCount(new LambdaQueryWrapperX<SocialSecurityAccountDO>()
                .eq(SocialSecurityAccountDO::getSocialMonth, socialMonth)) > 0;
    }

    private List<EmployeeEntryDO> loadSocialSecurityCheckEntries(LocalDate monthStart, LocalDate monthEnd) {
        return employeeEntryMapper.selectList(new LambdaQueryWrapperX<EmployeeEntryDO>()
                .in(EmployeeEntryDO::getWorkStatus, 2, 3)
                .isNotNull(EmployeeEntryDO::getProfileId)
                .and(query -> query.isNull(EmployeeEntryDO::getEntryDate)
                        .or()
                        .le(EmployeeEntryDO::getEntryDate, monthEnd))
                .and(query -> query.isNull(EmployeeEntryDO::getLeaveDate)
                        .or()
                        .ge(EmployeeEntryDO::getLeaveDate, monthStart))
                .orderByDesc(EmployeeEntryDO::getEntryDate)
                .orderByDesc(EmployeeEntryDO::getId)
                .last("LIMIT " + SOCIAL_SECURITY_ENTRY_LIMIT));
    }

    private boolean shouldCheckSocialSecurity(EmployeeEntryDO entry, LocalDate monthStart, LocalDate monthEnd) {
        if (entry == null || entry.getProfileId() == null || !Arrays.asList(2, 3).contains(entry.getWorkStatus())) {
            return false;
        }
        if (entry.getEmploymentType() != null && entry.getEmploymentType() != 1) {
            return false;
        }
        if (entry.getEntryDate() != null && entry.getEntryDate().isAfter(monthEnd)) {
            return false;
        }
        return entry.getLeaveDate() == null || !entry.getLeaveDate().isBefore(monthStart);
    }

    private Map<Long, SocialSecurityAccountDO> loadSocialSecurityMap(Set<Long> profileIds, String socialMonth) {
        Map<Long, SocialSecurityAccountDO> result = new HashMap<>();
        if (profileIds == null || profileIds.isEmpty() || !StringUtils.hasText(socialMonth)) {
            return result;
        }
        List<SocialSecurityAccountDO> accounts = socialSecurityAccountMapper.selectListByMonthAndProfileIds(socialMonth, profileIds);
        for (SocialSecurityAccountDO account : emptyIfNull(accounts)) {
            if (account.getProfileId() != null && !result.containsKey(account.getProfileId())) {
                result.put(account.getProfileId(), account);
            }
        }
        return result;
    }

    private boolean isSocialEnrolled(SocialSecurityAccountDO account) {
        return account != null && SOCIAL_STATUS_ENROLLED.equalsIgnoreCase(empty(account.getStatus()));
    }

    private HrRiskWorkbenchRespVO.RiskItem fromSocialSecurityMissing(EmployeeEntryDO entry,
                                                                     SocialSecurityAccountDO account,
                                                                     EmployeeProfileDO profile,
                                                                     String socialMonth) {
        HrRiskWorkbenchRespVO.RiskItem item = new HrRiskWorkbenchRespVO.RiskItem();
        item.setSourceType(SOURCE_SOCIAL_SECURITY);
        item.setSourceKey("SOCIAL_SECURITY:" + socialMonth + ":" + entry.getProfileId());
        item.setSeverity(SEVERITY_HIGH);
        item.setIssueType(ISSUE_SOCIAL_SECURITY_MISSING);
        item.setTitle("社保公积金参保异常");
        item.setDescription(socialSecurityRiskDescription(entry, account, profile, socialMonth));
        item.setAction("维护社保/公积金台账，并确认状态为参保");
        item.setProfileId(entry.getProfileId());
        item.setEmployeeName(profile == null ? null : profile.getName());
        item.setMobile(profile == null ? null : profile.getMobile());
        item.setRoutePath(socialSecurityRoute(entry.getProfileId(), socialMonth));
        item.setDueTime(socialSecurityDueTime());
        return item;
    }

    private String socialSecurityRiskDescription(EmployeeEntryDO entry, SocialSecurityAccountDO account,
                                                 EmployeeProfileDO profile, String socialMonth) {
        List<String> pieces = new ArrayList<>();
        pieces.add("员工：" + profileDisplayName(profile));
        pieces.add("社保月份：" + socialMonth);
        if (entry.getEntryDate() != null) {
            pieces.add("入职日期：" + entry.getEntryDate());
        }
        if (account == null) {
            pieces.add("未找到当月社保公积金台账");
        } else {
            pieces.add("当前状态：" + defaultText(account.getStatus(), "未维护"));
        }
        return String.join("；", pieces);
    }

    private LocalDateTime socialSecurityDueTime() {
        LocalDate dueDate = LocalDate.now().withDayOfMonth(1).plusDays(14);
        if (dueDate.isBefore(LocalDate.now())) {
            dueDate = LocalDate.now().plusDays(1);
        }
        return LocalDateTime.of(dueDate, LocalTime.of(18, 0));
    }

    private String socialSecurityRoute(Long profileId, String socialMonth) {
        return "/hr/payroll?tab=social&socialProfileId=" + profileId + "&socialMonth=" + socialMonth;
    }

    private String profileDisplayName(EmployeeProfileDO profile) {
        return profile == null ? "未命名员工" : defaultText(profile.getName(), "未命名员工");
    }

    private List<HrRiskWorkbenchRespVO.QuickAction> buildQuickActions() {
        List<HrRiskWorkbenchRespVO.QuickAction> actions = new ArrayList<>();
        actions.add(action("风险台账", "lucide:clipboard-list", "/hr/risk"));
        actions.add(action("主数据中心", "lucide:database", "/hr/employee/master-data"));
        actions.add(action("花名册", "lucide:users-round", "/hr/employee"));
        actions.add(action("绩效工作台", "lucide:trophy", "/hr/performance"));
        actions.add(action("待办中心", "lucide:list-checks", "/hr/todo"));
        actions.add(action("入转调离", "lucide:workflow", "/hr/lifecycle/workbench"));
        return actions;
    }

    private void fillEventPeopleInfo(List<HrRiskEventDO> rows, List<HrRiskEventRespVO> respList) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Set<Long> profileIds = new HashSet<>();
        Set<Long> userIds = new HashSet<>();
        for (HrRiskEventDO row : rows) {
            if (row.getProfileId() != null) {
                profileIds.add(row.getProfileId());
            }
            if (row.getOwnerUserId() != null) {
                userIds.add(row.getOwnerUserId());
            }
            if (row.getHandledBy() != null) {
                userIds.add(row.getHandledBy());
            }
        }
        Map<Long, EmployeeProfileDO> profileMap = loadProfileMap(profileIds);
        Map<Long, AdminUserRespDTO> userMap = loadUserMapSafe(userIds);
        for (HrRiskEventRespVO item : respList) {
            EmployeeProfileDO profile = profileMap.get(item.getProfileId());
            if (profile != null) {
                item.setProfileName(profile.getName());
                item.setMobile(profile.getMobile());
            }
            item.setOwnerName(userName(userMap.get(item.getOwnerUserId())));
            item.setHandledByName(userName(userMap.get(item.getHandledBy())));
        }
    }

    private Map<Long, EmployeeProfileDO> loadProfileMap(Set<Long> profileIds) {
        Map<Long, EmployeeProfileDO> profileMap = new HashMap<>();
        if (profileIds == null || profileIds.isEmpty()) {
            return profileMap;
        }
        List<EmployeeProfileDO> profiles = employeeProfileMapper.selectList(
                new LambdaQueryWrapperX<EmployeeProfileDO>().in(EmployeeProfileDO::getId, profileIds));
        if (profiles == null) {
            return profileMap;
        }
        for (EmployeeProfileDO profile : profiles) {
            if (profile.getId() != null) {
                profileMap.put(profile.getId(), profile);
            }
        }
        return profileMap;
    }

    private Map<Long, AdminUserRespDTO> loadUserMapSafe(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return adminUserApi.getUserMap(userIds);
        } catch (Exception ex) {
            return new HashMap<>();
        }
    }

    private String userName(AdminUserRespDTO user) {
        if (user == null) {
            return null;
        }
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }

    private HrRiskWorkbenchRespVO.RiskCategory category(String code, String name, String severity, Integer count,
                                                        String description, String icon, String routePath) {
        HrRiskWorkbenchRespVO.RiskCategory category = new HrRiskWorkbenchRespVO.RiskCategory();
        category.setCode(code);
        category.setName(name);
        category.setSeverity(severity);
        category.setCount(count);
        category.setDescription(description);
        category.setIcon(icon);
        category.setRoutePath(routePath);
        return category;
    }

    private HrRiskWorkbenchRespVO.QuickAction action(String title, String icon, String path) {
        HrRiskWorkbenchRespVO.QuickAction action = new HrRiskWorkbenchRespVO.QuickAction();
        action.setTitle(title);
        action.setIcon(icon);
        action.setPath(path);
        return action;
    }

    private Integer countOverdueTodos(Collection<HrTodoTaskDO> todos) {
        return (int) todos.stream().filter(this::isOverdue).count();
    }

    private Integer countHighPriorityTodos(Collection<HrTodoTaskDO> todos) {
        return (int) todos.stream().filter(item -> isHighPriority(item.getPriority())).count();
    }

    private boolean isOverdue(HrTodoTaskDO todo) {
        return todo.getDueTime() != null && todo.getDueTime().isBefore(LocalDateTime.now());
    }

    private boolean isHighPriority(String priority) {
        String value = empty(priority).toUpperCase();
        return SEVERITY_HIGH.equals(value) || "URGENT".equals(value) || "高".equals(priority) || "紧急".equals(priority);
    }

    private boolean isPerformanceWarning(EmployeePerformanceDO performance) {
        if (performance.getScore() != null && performance.getScore().compareTo(BigDecimal.valueOf(60)) < 0) {
            return true;
        }
        String grade = empty(performance.getGrade()).toUpperCase();
        String result = empty(performance.getResult());
        return grade.contains("C") || grade.contains("D")
                || result.contains("待改进") || result.contains("不合格") || result.contains("较差");
    }

    private BigDecimal avgScore(List<EmployeePerformanceDO> performances) {
        List<BigDecimal> scores = performances.stream()
                .map(EmployeePerformanceDO::getScore)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (scores.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(scores.size()), 1, RoundingMode.HALF_UP);
    }

    private Integer countSeverity(List<HrRiskEventDO> events, String severity) {
        return (int) events.stream()
                .filter(item -> severity.equals(normalizeSeverity(item.getSeverity())))
                .count();
    }

    private Integer countStatus(String status) {
        Long count = hrRiskEventMapper.selectCount(new LambdaQueryWrapperX<HrRiskEventDO>()
                .eq(HrRiskEventDO::getStatus, status));
        return count == null ? 0 : count.intValue();
    }

    private Integer countCategory(List<HrRiskEventDO> events, String category) {
        return (int) events.stream()
                .filter(item -> category.equals(categoryOf(item)))
                .count();
    }

    private String highestSeverity(List<HrRiskEventDO> events, String category) {
        return events.stream()
                .filter(item -> category.equals(categoryOf(item)))
                .map(HrRiskEventDO::getSeverity)
                .min(Comparator.comparingInt(this::severityRank))
                .orElse(SEVERITY_LOW);
    }

    private String categoryOf(HrRiskEventDO event) {
        if (ISSUE_CONTRACT_EXPIRING.equals(event.getIssueType())) {
            return "CONTRACT";
        }
        if (ISSUE_PROBATION_DUE.equals(event.getIssueType())) {
            return "PROBATION";
        }
        if (SOURCE_PERFORMANCE.equals(event.getSourceType())) {
            return "PERFORMANCE";
        }
        if (SOURCE_TODO.equals(event.getSourceType())) {
            return "TODO";
        }
        if (SOURCE_SOCIAL_SECURITY.equals(event.getSourceType())) {
            return "SOCIAL_SECURITY";
        }
        if (SOURCE_MANUAL.equals(event.getSourceType())) {
            return "MANUAL";
        }
        return "DATA_QUALITY";
    }

    private int severityRank(String severity) {
        String value = normalizeSeverity(severity);
        if (SEVERITY_HIGH.equals(value)) {
            return 0;
        }
        if (SEVERITY_MEDIUM.equals(value)) {
            return 1;
        }
        return 2;
    }

    private String normalizeSeverity(String severity) {
        if (SEVERITY_HIGH.equals(severity) || SEVERITY_MEDIUM.equals(severity) || SEVERITY_LOW.equals(severity)) {
            return severity;
        }
        return SEVERITY_LOW;
    }

    private boolean isAllowedSeverity(String severity) {
        return Arrays.asList(SEVERITY_HIGH, SEVERITY_MEDIUM, SEVERITY_LOW).contains(severity);
    }

    private boolean isAllowedStatus(String status) {
        return Arrays.asList(STATUS_OPEN, STATUS_PROCESSING, STATUS_RESOLVED, STATUS_IGNORED).contains(status);
    }

    private String issueSourceKey(EmployeeDataQualityIssueRespVO issue) {
        String profileKey = issue.getProfileId() == null ? "GLOBAL" : String.valueOf(issue.getProfileId());
        return defaultText(issue.getIssueType(), "UNKNOWN") + ":" + profileKey;
    }

    private String eventKey(String sourceType, String sourceKey) {
        return sourceType + "::" + sourceKey;
    }

    private LocalDateTime defaultDueTime(String severity) {
        int days = SEVERITY_HIGH.equals(severity) ? 3 : SEVERITY_MEDIUM.equals(severity) ? 7 : 14;
        return LocalDateTime.now().plusDays(days).with(LocalTime.of(18, 0));
    }

    private LocalDateTime performanceDueTime(EmployeePerformanceDO performance) {
        LocalDate baseDate = performance.getEvaluatedDate() == null ? LocalDate.now() : performance.getEvaluatedDate();
        return LocalDateTime.of(baseDate.plusDays(7), LocalTime.of(18, 0));
    }

    private String employeeRoute(Long profileId, String tab) {
        if (profileId == null) {
            return "/hr/employee";
        }
        return "/hr/employee/detail?id=" + profileId + "&tab=" + tab;
    }

    private Long resolveRiskOwnerUserId(Long profileId) {
        EmployeeEntryDO entry = selectPrimaryEntry(profileId);
        if (entry == null || entry.getDirectSupervisorId() == null) {
            return null;
        }
        return resolveUserIdByProfileId(entry.getDirectSupervisorId());
    }

    private Long resolveUserIdByProfileId(Long profileId) {
        if (profileId == null) {
            return null;
        }
        EmployeeProfileDO profile = employeeProfileMapper.selectById(profileId);
        if (profile != null && profile.getUserId() != null) {
            return profile.getUserId();
        }
        EmployeeDO employee = employeeMapper.selectOne(new LambdaQueryWrapperX<EmployeeDO>()
                .eq(EmployeeDO::getProfileId, profileId)
                .isNotNull(EmployeeDO::getUserId)
                .orderByDesc(EmployeeDO::getId)
                .last("LIMIT 1"));
        if (employee != null && employee.getUserId() != null) {
            return employee.getUserId();
        }
        EmployeeEntryDO entry = selectPrimaryEntry(profileId);
        return entry == null ? null : entry.getUserId();
    }

    private EmployeeEntryDO selectPrimaryEntry(Long profileId) {
        if (profileId == null) {
            return null;
        }
        List<EmployeeEntryDO> entries = employeeEntryMapper.selectListByProfileId(profileId);
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        return entries.stream()
                .min(Comparator
                        .comparing((EmployeeEntryDO item) -> isActiveWorkStatus(item.getWorkStatus()) ? 0 : 1)
                        .thenComparing(item -> item.getEntryDate() == null ? LocalDate.MIN : item.getEntryDate(),
                                Comparator.reverseOrder())
                        .thenComparing(item -> item.getId() == null ? Long.MIN_VALUE : item.getId(),
                                Comparator.reverseOrder()))
                .orElse(null);
    }

    private boolean isActiveWorkStatus(Integer status) {
        return status != null && Arrays.asList(1, 2, 3).contains(status);
    }

    private void applyDeptScope(HrRiskEventPageReqVO pageReqVO) {
        if (pageReqVO == null || pageReqVO.getDeptId() == null || pageReqVO.getDeptId() <= 0) {
            return;
        }
        Set<Long> deptIds = resolveDeptIds(pageReqVO.getDeptId(), Boolean.TRUE.equals(pageReqVO.getIncludeChildren()));
        Set<Long> profileIds = loadDeptProfileIds(deptIds);
        pageReqVO.setProfileIds(profileIds.isEmpty() ? Collections.singletonList(-1L) : new ArrayList<>(profileIds));
    }

    private Set<Long> resolveDeptIds(Long deptId, boolean includeChildren) {
        Set<Long> deptIds = new HashSet<>();
        if (deptId == null || deptId <= 0) {
            return deptIds;
        }
        deptIds.add(deptId);
        if (!includeChildren) {
            return deptIds;
        }
        try {
            List<DeptRespDTO> children = deptApi.getChildDeptList(deptId).getCheckedData();
            if (children != null) {
                children.stream()
                        .map(DeptRespDTO::getId)
                        .filter(Objects::nonNull)
                        .forEach(deptIds::add);
            }
        } catch (Exception ignored) {
        }
        return deptIds;
    }

    private Set<Long> loadDeptProfileIds(Set<Long> deptIds) {
        Set<Long> profileIds = new HashSet<>();
        if (deptIds == null || deptIds.isEmpty()) {
            return profileIds;
        }
        List<EmployeeEntryDO> entries = employeeEntryMapper.selectList(new LambdaQueryWrapperX<EmployeeEntryDO>()
                .in(EmployeeEntryDO::getDeptId, deptIds)
                .in(EmployeeEntryDO::getWorkStatus, Arrays.asList(1, 2, 3)));
        if (entries == null || entries.isEmpty()) {
            return profileIds;
        }
        entries.stream()
                .map(EmployeeEntryDO::getProfileId)
                .filter(Objects::nonNull)
                .forEach(profileIds::add);
        return profileIds;
    }

    private Integer orZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String empty(String value) {
        return value == null ? "" : value;
    }

    private <T> List<T> emptyIfNull(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private String formatScore(BigDecimal score) {
        return score == null ? "-" : score.setScale(1, RoundingMode.HALF_UP).toPlainString();
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String defaultText(Long value, Long fallbackValue) {
        Long result = value == null ? fallbackValue : value;
        return result == null ? "UNKNOWN" : String.valueOf(result);
    }

    @SuppressWarnings("unused")
    private Date nowDate() {
        return new Date();
    }

    private static class RiskSnapshot {

        private final EmployeeMasterWorkbenchRespVO master;
        private final EmployeeDataQualityRespVO quality;
        private final List<EmployeePerformanceDO> latestPerformances;
        private final List<EmployeePerformanceDO> performanceWarnings;
        private final List<HrTodoTaskDO> openTodos;
        private final List<HrRiskWorkbenchRespVO.RiskItem> sourceItems;

        RiskSnapshot(EmployeeMasterWorkbenchRespVO master,
                     EmployeeDataQualityRespVO quality,
                     List<EmployeePerformanceDO> latestPerformances,
                     List<EmployeePerformanceDO> performanceWarnings,
                     List<HrTodoTaskDO> openTodos,
                     List<HrRiskWorkbenchRespVO.RiskItem> sourceItems) {
            this.master = master;
            this.quality = quality;
            this.latestPerformances = latestPerformances;
            this.performanceWarnings = performanceWarnings;
            this.openTodos = openTodos;
            this.sourceItems = sourceItems;
        }

        public EmployeeMasterWorkbenchRespVO getMaster() {
            return master;
        }

        public EmployeeDataQualityRespVO getQuality() {
            return quality;
        }

        public List<EmployeePerformanceDO> getLatestPerformances() {
            return latestPerformances;
        }

        public List<EmployeePerformanceDO> getPerformanceWarnings() {
            return performanceWarnings;
        }

        public List<HrTodoTaskDO> getOpenTodos() {
            return openTodos;
        }

        public List<HrRiskWorkbenchRespVO.RiskItem> getSourceItems() {
            return sourceItems;
        }
    }
}
