package com.kyx.service.biz.service.executive;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.service.biz.controller.admin.executive.vo.ExecutiveCockpitChatReqVO;
import com.kyx.service.biz.controller.admin.executive.vo.ExecutiveCockpitChatRespVO;
import com.kyx.service.biz.controller.admin.executive.vo.ExecutiveCockpitOverviewRespVO;
import com.kyx.service.biz.dal.mysql.executive.ExecutiveCockpitMapper;
import com.kyx.service.biz.dal.mysql.executive.OrdersysCockpitMapper;
import com.kyx.service.biz.enums.WorkRequirementPriorityEnum;
import com.kyx.service.biz.enums.WorkRequirementStatusEnum;
import com.kyx.service.biz.service.work.WorkRequirementTenantScopeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ExecutiveCockpitServiceImpl implements ExecutiveCockpitService {

    private static final int DEFAULT_RANGE_DAYS = 30;
    private static final int MIN_RANGE_DAYS = 7;
    private static final int MAX_RANGE_DAYS = 90;
    private static final int WORKLOAD_LIMIT = 8;
    private static final int RISK_LIMIT = 8;
    private static final int RECENT_TASK_LIMIT = 8;
    private static final String DATE_PATTERN = "yyyy-MM-dd";

    @Resource
    private ExecutiveCockpitMapper executiveCockpitMapper;
    @Resource
    private OrdersysCockpitMapper ordersysCockpitMapper;
    @Resource
    private WorkRequirementTenantScopeService workRequirementTenantScopeService;

    @Override
    public ExecutiveCockpitOverviewRespVO getOverview(Integer days, Long loginUserId) {
        int rangeDays = normalizeRangeDays(days);
        Date todayBegin = DateUtil.beginOfDay(new Date());
        Date startTime = DateUtil.offsetDay(todayBegin, -(rangeDays - 1));
        Date endTime = DateUtil.offsetDay(todayBegin, 1);
        List<Long> tenantIds = resolveTenantIds();

        Map<String, Object> requirementSummary = executiveCockpitMapper.selectRequirementSummary(tenantIds, startTime);
        List<Map<String, Object>> statusRows = executiveCockpitMapper.selectRequirementStatusCounts(tenantIds);
        List<Map<String, Object>> createdTrendRows =
                executiveCockpitMapper.selectRequirementCreatedTrend(tenantIds, startTime, endTime);
        List<Map<String, Object>> finishedTrendRows =
                executiveCockpitMapper.selectRequirementFinishedTrend(tenantIds, startTime, endTime);
        List<Map<String, Object>> workloadRows = executiveCockpitMapper.selectRequirementWorkload(tenantIds, WORKLOAD_LIMIT);
        List<Map<String, Object>> riskRows = executiveCockpitMapper.selectRequirementRisks(tenantIds, RISK_LIMIT);

        ExecutiveCockpitOverviewRespVO respVO = new ExecutiveCockpitOverviewRespVO();
        respVO.setGeneratedAt(new Date());
        respVO.setRangeDays(rangeDays);
        respVO.setRequirementTrend(buildTrend(rangeDays, todayBegin, createdTrendRows, finishedTrendRows));
        respVO.setRequirementStatus(buildStatusCounts(statusRows));
        respVO.setWorkload(buildWorkload(workloadRows));
        respVO.setRisks(buildRequirementRisks(riskRows, todayBegin));

        Map<String, Object> ordersysSummary = Collections.emptyMap();
        respVO.setOrdersysAvailable(false);
        respVO.setOrdersysMessage("ordersys 需求看板查询将由 Agent 工具单独提供");
        respVO.setRecentOrdersysTasks(Collections.emptyList());
        respVO.setMetrics(buildMetrics(requirementSummary, ordersysSummary, rangeDays, Boolean.TRUE.equals(respVO.getOrdersysAvailable())));
        return respVO;
    }

    @Override
    public ExecutiveCockpitChatRespVO chat(ExecutiveCockpitChatReqVO reqVO, Long loginUserId) {
        ExecutiveCockpitOverviewRespVO snapshot = getOverview(reqVO.getRangeDays(), loginUserId);
        ExecutiveCockpitChatRespVO respVO = new ExecutiveCockpitChatRespVO();
        respVO.setConversationId(resolveConversationId(reqVO.getConversationId()));
        respVO.setSnapshot(snapshot);
        respVO.setReply(buildReply(reqVO.getMessage(), snapshot));
        respVO.setSuggestions(buildSuggestions(snapshot));
        respVO.setChartHints(buildChartHints(reqVO.getMessage()));
        return respVO;
    }

    private List<Long> resolveTenantIds() {
        List<Long> tenantIds = workRequirementTenantScopeService.getQueryAllTenantIds();
        if (CollUtil.isNotEmpty(tenantIds)) {
            return tenantIds;
        }
        return Collections.singletonList(TenantContextHolder.getRequiredTenantId());
    }

    private List<ExecutiveCockpitOverviewRespVO.MetricCard> buildMetrics(Map<String, Object> requirementSummary,
                                                                         Map<String, Object> ordersysSummary,
                                                                         int rangeDays,
                                                                         boolean ordersysAvailable) {
        Long openCount = readLong(requirementSummary, "openCount");
        Long completedCount = readLong(requirementSummary, "completedCount");
        Long overdueCount = readLong(requirementSummary, "overdueCount");
        Long highPriorityCount = readLong(requirementSummary, "highPriorityCount");
        Long createdRangeCount = readLong(requirementSummary, "createdRangeCount");
        Long doneRangeCount = readLong(requirementSummary, "doneRangeCount");
        Long closeRate = calculateRate(completedCount, openCount + completedCount);

        List<ExecutiveCockpitOverviewRespVO.MetricCard> metrics = new ArrayList<>();
        metrics.add(metric("requirement_open", "在办需求", openCount, "件",
                "高优先级 " + highPriorityCount + " 件", "近" + rangeDays + "天新增 " + createdRangeCount + " 件", "blue"));
        metrics.add(metric("requirement_done", "已完成需求", completedCount, "件",
                "近" + rangeDays + "天完成 " + doneRangeCount + " 件", "闭环率 " + closeRate + "%", "green"));
        metrics.add(metric("requirement_overdue", "逾期风险", overdueCount, "件",
                "未完成且超过期望完成日", overdueCount > 0 ? "需要关注" : "风险稳定", overdueCount > 0 ? "red" : "green"));
        metrics.add(metric("requirement_close_rate", "需求闭环率", closeRate, "%",
                "按当前完成与在办需求估算", "累计完成 " + completedCount + " 件", "indigo"));

        if (ordersysAvailable) {
            Long orderOpenCount = readLong(ordersysSummary, "openCount");
            Long orderOverdueCount = readLong(ordersysSummary, "overdueCount");
            Long orderCreated7dCount = readLong(ordersysSummary, "created7dCount");
            metrics.add(metric("ordersys_open", "ordersys 在办", orderOpenCount, "件",
                    "近7天新增 " + orderCreated7dCount + " 件", "来自 dev ordersys", "cyan"));
            metrics.add(metric("ordersys_overdue", "ordersys 逾期", orderOverdueCount, "件",
                    "按 sys_task.task_time 计算", orderOverdueCount > 0 ? "需要追踪" : "风险稳定",
                    orderOverdueCount > 0 ? "amber" : "green"));
        } else {
            metrics.add(metric("ordersys_open", "ordersys 在办", null, "",
                    "需求看板由 Agent 工具单独查询", "已隔离", "slate"));
        }
        return metrics;
    }

    private ExecutiveCockpitOverviewRespVO.MetricCard metric(String code,
                                                             String title,
                                                             Long numericValue,
                                                             String unit,
                                                             String description,
                                                             String trendLabel,
                                                             String tone) {
        ExecutiveCockpitOverviewRespVO.MetricCard metric = new ExecutiveCockpitOverviewRespVO.MetricCard();
        metric.setCode(code);
        metric.setTitle(title);
        metric.setNumericValue(numericValue);
        metric.setValue(numericValue == null ? "--" : String.valueOf(numericValue));
        metric.setUnit(unit);
        metric.setDescription(description);
        metric.setTrendLabel(trendLabel);
        metric.setTone(tone);
        return metric;
    }

    private List<ExecutiveCockpitOverviewRespVO.TrendPoint> buildTrend(int rangeDays,
                                                                       Date todayBegin,
                                                                       List<Map<String, Object>> createdRows,
                                                                       List<Map<String, Object>> finishedRows) {
        Map<String, ExecutiveCockpitOverviewRespVO.TrendPoint> trendMap = new LinkedHashMap<>();
        for (int index = rangeDays - 1; index >= 0; index--) {
            Date day = DateUtil.offsetDay(todayBegin, -index);
            String date = DateUtil.format(day, DATE_PATTERN);
            ExecutiveCockpitOverviewRespVO.TrendPoint point = new ExecutiveCockpitOverviewRespVO.TrendPoint();
            point.setDate(date);
            point.setCreatedCount(0L);
            point.setFinishedCount(0L);
            trendMap.put(date, point);
        }
        applyTrendRows(trendMap, createdRows, true);
        applyTrendRows(trendMap, finishedRows, false);
        return new ArrayList<>(trendMap.values());
    }

    private void applyTrendRows(Map<String, ExecutiveCockpitOverviewRespVO.TrendPoint> trendMap,
                                List<Map<String, Object>> rows,
                                boolean created) {
        for (Map<String, Object> row : rows) {
            String date = readString(row, "trendDate");
            ExecutiveCockpitOverviewRespVO.TrendPoint point = trendMap.get(date);
            if (point == null) {
                continue;
            }
            Long count = readLong(row, "count");
            if (created) {
                point.setCreatedCount(point.getCreatedCount() + count);
            } else {
                point.setFinishedCount(point.getFinishedCount() + count);
            }
        }
    }

    private List<ExecutiveCockpitOverviewRespVO.StatusCount> buildStatusCounts(List<Map<String, Object>> rows) {
        Map<Integer, Long> countMap = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Integer status = readInteger(row, "status");
            if (status != null) {
                countMap.merge(status, readLong(row, "count"), Long::sum);
            }
        }
        List<Integer> visibleStatuses = Arrays.asList(
                WorkRequirementStatusEnum.PENDING_ASSIGN.getStatus(),
                WorkRequirementStatusEnum.PENDING_DEVELOP.getStatus(),
                WorkRequirementStatusEnum.DEVELOPING.getStatus(),
                WorkRequirementStatusEnum.TESTING.getStatus(),
                WorkRequirementStatusEnum.PENDING_ACCEPT.getStatus(),
                WorkRequirementStatusEnum.DONE.getStatus(),
                WorkRequirementStatusEnum.CANCELED.getStatus(),
                WorkRequirementStatusEnum.SUSPENDED.getStatus());
        return visibleStatuses.stream().map(status -> {
            ExecutiveCockpitOverviewRespVO.StatusCount item = new ExecutiveCockpitOverviewRespVO.StatusCount();
            item.setStatus(status);
            item.setLabel(getStatusLabel(status));
            item.setCount(countMap.getOrDefault(status, 0L));
            item.setTone(getStatusTone(status));
            return item;
        }).collect(Collectors.toList());
    }

    private List<ExecutiveCockpitOverviewRespVO.AssigneeWorkload> buildWorkload(List<Map<String, Object>> rows) {
        List<ExecutiveCockpitOverviewRespVO.AssigneeWorkload> workload = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            ExecutiveCockpitOverviewRespVO.AssigneeWorkload item = new ExecutiveCockpitOverviewRespVO.AssigneeWorkload();
            item.setAssigneeUserId(readLongObject(row, "assigneeUserId"));
            item.setAssigneeName(StrUtil.blankToDefault(readString(row, "assigneeName"), "未分派"));
            item.setTotalCount(readLong(row, "totalCount"));
            item.setOpenCount(readLong(row, "openCount"));
            item.setOverdueCount(readLong(row, "overdueCount"));
            workload.add(item);
        }
        return workload;
    }

    private List<ExecutiveCockpitOverviewRespVO.RiskItem> buildRequirementRisks(List<Map<String, Object>> rows,
                                                                                Date todayBegin) {
        List<ExecutiveCockpitOverviewRespVO.RiskItem> risks = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Integer status = readInteger(row, "status");
            Integer priority = readInteger(row, "priority");
            Date expectedFinishDate = readDate(row, "expectedFinishDate");
            ExecutiveCockpitOverviewRespVO.RiskItem item = new ExecutiveCockpitOverviewRespVO.RiskItem();
            item.setSource("OA");
            item.setId(readLongObject(row, "id"));
            item.setTitle(StrUtil.blankToDefault(readString(row, "title"), "未命名需求"));
            item.setStatus(status);
            item.setStatusLabel(getStatusLabel(status));
            item.setPriority(priority);
            item.setPriorityLabel(getPriorityLabel(priority));
            item.setAssigneeName(StrUtil.blankToDefault(readString(row, "assigneeName"), "未分派"));
            item.setExpectedFinishDate(expectedFinishDate);
            item.setUpdateTime(readDate(row, "updateTime"));
            item.setRiskReason(buildRiskReason(expectedFinishDate, priority, todayBegin));
            risks.add(item);
        }
        return risks;
    }

    private List<ExecutiveCockpitOverviewRespVO.RecentTask> buildRecentOrdersysTasks(List<Map<String, Object>> rows) {
        List<ExecutiveCockpitOverviewRespVO.RecentTask> tasks = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String status = readString(row, "status");
            ExecutiveCockpitOverviewRespVO.RecentTask item = new ExecutiveCockpitOverviewRespVO.RecentTask();
            item.setSource("ordersys");
            item.setId(readLongObject(row, "id"));
            item.setTitle(StrUtil.blankToDefault(readString(row, "title"), "未命名工单"));
            item.setStatus(status);
            item.setStatusLabel(getOrdersysStatusLabel(status));
            item.setOperatorName(readString(row, "operatorName"));
            item.setAssigneeName(readString(row, "assigneeName"));
            item.setTaskTime(readDate(row, "taskTime"));
            item.setUpdateTime(readDate(row, "updateTime"));
            tasks.add(item);
        }
        return tasks;
    }

    private String buildReply(String message, ExecutiveCockpitOverviewRespVO snapshot) {
        String normalized = StrUtil.nullToEmpty(message).trim();
        Long requirementOpen = metricValue(snapshot, "requirement_open");
        Long requirementDone = metricValue(snapshot, "requirement_done");
        Long requirementOverdue = metricValue(snapshot, "requirement_overdue");
        Long ordersysOpen = metricValue(snapshot, "ordersys_open");

        StringBuilder reply = new StringBuilder();
        reply.append("老板，近").append(snapshot.getRangeDays()).append("天看下来，OA 需求当前在办 ")
                .append(requirementOpen).append(" 件，累计完成 ").append(requirementDone).append(" 件，逾期风险 ")
                .append(requirementOverdue).append(" 件。");
        if (Boolean.TRUE.equals(snapshot.getOrdersysAvailable()) && ordersysOpen != null) {
            reply.append("ordersys dev 库当前在办 ").append(ordersysOpen).append(" 件。");
        }

        if (StrUtil.containsAny(normalized, "逾期", "风险", "卡住")) {
            reply.append(" 逾期优先看负责人和期望完成日，列表里已经把最高风险排在前面。");
            if (CollUtil.isNotEmpty(snapshot.getRisks())) {
                ExecutiveCockpitOverviewRespVO.RiskItem firstRisk = snapshot.getRisks().get(0);
                reply.append(" 当前最靠前的是「").append(firstRisk.getTitle()).append("」，负责人 ")
                        .append(firstRisk.getAssigneeName()).append("。");
            }
        } else if (StrUtil.containsAny(normalized, "谁", "人", "负责人", "团队")) {
            if (CollUtil.isNotEmpty(snapshot.getWorkload())) {
                ExecutiveCockpitOverviewRespVO.AssigneeWorkload top = snapshot.getWorkload().get(0);
                reply.append(" 负责人负载最高的是 ").append(top.getAssigneeName())
                        .append("，在办 ").append(top.getOpenCount()).append(" 件，逾期 ")
                        .append(top.getOverdueCount()).append(" 件。");
            }
        } else if (StrUtil.containsAny(normalized, "ordersys", "订单", "工单")) {
            reply.append(Boolean.TRUE.equals(snapshot.getOrdersysAvailable())
                    ? " ordersys 的最近更新已经放在右侧，适合继续追到具体工单。"
                    : " ordersys 连接暂不可用，先看 OA 已同步需求，等 dev 数据源恢复后会自动补齐。");
        } else {
            reply.append(" 建议先看逾期风险、负责人负载，再看近几天新增和完成趋势是否背离。");
        }
        return reply.toString();
    }

    private List<String> buildSuggestions(ExecutiveCockpitOverviewRespVO snapshot) {
        List<String> suggestions = new ArrayList<>();
        suggestions.add("按负责人看谁的在办最多");
        suggestions.add("列出当前最危险的逾期需求");
        suggestions.add("分析近" + snapshot.getRangeDays() + "天新增和完成趋势");
        if (Boolean.TRUE.equals(snapshot.getOrdersysAvailable())) {
            suggestions.add("看看 ordersys 最近更新的工单");
        }
        return suggestions;
    }

    private List<ExecutiveCockpitChatRespVO.ChartHint> buildChartHints(String message) {
        String normalized = StrUtil.nullToEmpty(message);
        List<ExecutiveCockpitChatRespVO.ChartHint> hints = new ArrayList<>();
        hints.add(chartHint("line", "需求新增/完成趋势", "requirementTrend"));
        if (StrUtil.containsAny(normalized, "谁", "人", "负责人", "团队")) {
            hints.add(chartHint("bar", "负责人负载", "workload"));
        } else {
            hints.add(chartHint("bar", "需求状态分布", "requirementStatus"));
        }
        return hints;
    }

    private ExecutiveCockpitChatRespVO.ChartHint chartHint(String type, String title, String dataKey) {
        ExecutiveCockpitChatRespVO.ChartHint hint = new ExecutiveCockpitChatRespVO.ChartHint();
        hint.setType(type);
        hint.setTitle(title);
        hint.setDataKey(dataKey);
        return hint;
    }

    private String resolveConversationId(String conversationId) {
        if (StrUtil.isNotBlank(conversationId)) {
            return conversationId;
        }
        return "cockpit-" + UUID.randomUUID();
    }

    private int normalizeRangeDays(Integer rangeDays) {
        if (rangeDays == null) {
            return DEFAULT_RANGE_DAYS;
        }
        return Math.max(MIN_RANGE_DAYS, Math.min(MAX_RANGE_DAYS, rangeDays));
    }

    private Long metricValue(ExecutiveCockpitOverviewRespVO snapshot, String code) {
        if (snapshot == null || CollUtil.isEmpty(snapshot.getMetrics())) {
            return 0L;
        }
        for (ExecutiveCockpitOverviewRespVO.MetricCard metric : snapshot.getMetrics()) {
            if (Objects.equals(metric.getCode(), code)) {
                return metric.getNumericValue() == null ? 0L : metric.getNumericValue();
            }
        }
        return 0L;
    }

    private Long calculateRate(Long numerator, Long denominator) {
        if (denominator == null || denominator <= 0) {
            return 0L;
        }
        return Math.round(numerator * 100.0 / denominator);
    }

    private String buildRiskReason(Date expectedFinishDate, Integer priority, Date todayBegin) {
        if (expectedFinishDate != null && expectedFinishDate.before(todayBegin)) {
            return "已超过期望完成日";
        }
        if (priority != null && priority >= WorkRequirementPriorityEnum.URGENT.getPriority()) {
            return "紧急优先级";
        }
        if (priority != null && priority >= WorkRequirementPriorityEnum.HIGH.getPriority()) {
            return "高优先级";
        }
        return "需要关注";
    }

    private String getStatusLabel(Integer status) {
        if (status == null) {
            return "未知";
        }
        for (WorkRequirementStatusEnum item : WorkRequirementStatusEnum.values()) {
            if (Objects.equals(item.getStatus(), status)) {
                return item.getName();
            }
        }
        return "状态" + status;
    }

    private String getStatusTone(Integer status) {
        if (Objects.equals(status, WorkRequirementStatusEnum.DONE.getStatus())) {
            return "green";
        }
        if (Objects.equals(status, WorkRequirementStatusEnum.CANCELED.getStatus())
                || Objects.equals(status, WorkRequirementStatusEnum.SUSPENDED.getStatus())) {
            return "slate";
        }
        if (Objects.equals(status, WorkRequirementStatusEnum.DEVELOPING.getStatus())) {
            return "blue";
        }
        if (Objects.equals(status, WorkRequirementStatusEnum.TESTING.getStatus())
                || Objects.equals(status, WorkRequirementStatusEnum.PENDING_ACCEPT.getStatus())) {
            return "amber";
        }
        return "indigo";
    }

    private String getPriorityLabel(Integer priority) {
        if (priority == null) {
            return "未知";
        }
        for (WorkRequirementPriorityEnum item : WorkRequirementPriorityEnum.values()) {
            if (Objects.equals(item.getPriority(), priority)) {
                return item.getName();
            }
        }
        return "优先级" + priority;
    }

    private String getOrdersysStatusLabel(String status) {
        if (Objects.equals(status, "1")) {
            return "已完成";
        }
        if (Objects.equals(status, "-1")) {
            return "已取消";
        }
        if (Objects.equals(status, "2")) {
            return "待接单";
        }
        if (Objects.equals(status, "3")) {
            return "处理中";
        }
        if (Objects.equals(status, "0")) {
            return "待处理";
        }
        return StrUtil.blankToDefault(status, "未知");
    }

    private Long readLong(Map<String, Object> row, String key) {
        Long value = readLongObject(row, key);
        return value == null ? 0L : value;
    }

    private Long readLongObject(Map<String, Object> row, String key) {
        Object value = readValue(row, key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).longValue();
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Integer readInteger(Map<String, Object> row, String key) {
        Object value = readValue(row, key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String readString(Map<String, Object> row, String key) {
        Object value = readValue(row, key);
        return value == null ? null : value.toString();
    }

    private Date readDate(Map<String, Object> row, String key) {
        Object value = readValue(row, key);
        if (value instanceof Date) {
            return (Date) value;
        }
        if (value instanceof LocalDateTime) {
            return Date.from(((LocalDateTime) value).atZone(ZoneId.systemDefault()).toInstant());
        }
        if (value instanceof LocalDate) {
            return Date.from(((LocalDate) value).atStartOfDay(ZoneId.systemDefault()).toInstant());
        }
        if (value instanceof Number) {
            return new Date(((Number) value).longValue());
        }
        if (value != null) {
            try {
                return DateUtil.parse(value.toString());
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private Object readValue(Map<String, Object> row, String key) {
        if (row == null || key == null) {
            return null;
        }
        if (row.containsKey(key)) {
            return row.get(key);
        }
        String underscoreKey = camelToUnderscore(key);
        if (row.containsKey(underscoreKey)) {
            return row.get(underscoreKey);
        }
        String lowerKey = key.toLowerCase();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey() != null && entry.getKey().toLowerCase().equals(lowerKey)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String camelToUnderscore(String value) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            if (Character.isUpperCase(ch)) {
                builder.append('_').append(Character.toLowerCase(ch));
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

}
