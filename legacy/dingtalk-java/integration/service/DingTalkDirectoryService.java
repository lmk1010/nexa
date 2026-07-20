package com.kyx.service.hr.integration.dingtalk.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.kyx.service.hr.config.DingTalkProperties;
import com.kyx.service.hr.integration.dingtalk.client.DingTalkOpenApiClient;
import com.kyx.service.hr.integration.dingtalk.client.DingTalkOpenApiClient.DingTalkApiException;
import com.kyx.service.hr.integration.dingtalk.model.DingTalkDeptSnapshot;
import com.kyx.service.hr.integration.dingtalk.model.DingTalkUserSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Reads department and user snapshots from DingTalk通讯录.
 */
@Service
@Slf4j
public class DingTalkDirectoryService {

    private static final long CACHE_SECONDS = 300;

    @Resource
    private DingTalkOpenApiClient dingTalkOpenApiClient;
    @Resource
    private DingTalkProperties dingTalkProperties;

    private volatile CachedUserIndex cachedUserIndex;

    public List<DingTalkUserSnapshot> listAllUsers() {
        DepartmentIndex departmentIndex = listAllDepartments();
        Map<String, DingTalkUserSnapshot> users = new LinkedHashMap<>();
        for (DeptMeta dept : departmentIndex.getDeptMap().values()) {
            Long deptId = dept.getId();
            int cursor = 0;
            while (true) {
                Map<String, Object> body = dingTalkOpenApiClient.body();
                body.put("dept_id", deptId);
                body.put("cursor", cursor);
                body.put("size", 100);
                JsonNode root;
                try {
                    root = dingTalkOpenApiClient.postTopApiWithRetry("/topapi/v2/user/list", body);
                } catch (DingTalkApiException ex) {
                    if (ex.getErrcode() == 60003) {
                        log.warn("Skip DingTalk department users because department no longer exists, deptId={}, deptName={}, errmsg={}",
                                deptId, dept.getName(), ex.getErrmsg());
                        break;
                    }
                    throw ex;
                }
                JsonNode result = root.path("result");
                JsonNode listNode = result.path("list");
                if (listNode.isArray()) {
                    for (JsonNode node : listNode) {
                        DingTalkUserSnapshot snapshot = toUserSnapshot(node, dept);
                        if (StringUtils.hasText(snapshot.getUserId())) {
                            users.put(snapshot.getUserId(), snapshot);
                        }
                    }
                }
                boolean hasMore = result.path("has_more").asBoolean(false);
                cursor = result.path("next_cursor").asInt(cursor + 100);
                if (!hasMore) {
                    break;
                }
            }
        }
        return enrichUsersWithDetail(new ArrayList<>(users.values()));
    }

    public Map<String, DingTalkUserSnapshot> getUserByMobileIndex() {
        CachedUserIndex local = cachedUserIndex;
        long now = Instant.now().getEpochSecond();
        if (local != null && local.expireAtEpochSeconds > now) {
            return local.byMobile;
        }
        synchronized (this) {
            local = cachedUserIndex;
            if (local != null && local.expireAtEpochSeconds > now) {
                return local.byMobile;
            }
            List<DingTalkUserSnapshot> users = listAllUsers();
            Map<String, DingTalkUserSnapshot> byMobile = new HashMap<>();
            for (DingTalkUserSnapshot user : users) {
                if (StringUtils.hasText(user.getMobile())) {
                    byMobile.put(user.getMobile().trim(), user);
                }
            }
            cachedUserIndex = new CachedUserIndex(byMobile, now + CACHE_SECONDS);
            return byMobile;
        }
    }

    public List<DingTalkDeptSnapshot> listAllDepartmentSnapshots() {
        DepartmentIndex departmentIndex = listAllDepartments();
        List<DingTalkDeptSnapshot> snapshots = new ArrayList<>();
        for (DeptMeta dept : departmentIndex.getDeptMap().values()) {
            DingTalkDeptSnapshot snapshot = new DingTalkDeptSnapshot();
            snapshot.setDeptId(dept.getId());
            snapshot.setName(dept.getName());
            snapshot.setParentId(dept.getParentId());
            snapshot.setTopDeptId(dept.getTopDeptId());
            snapshot.setTopDeptName(dept.getTopDeptName());
            snapshot.setOrder(dept.getOrder());
            snapshots.add(snapshot);
        }
        return snapshots;
    }

    private DepartmentIndex listAllDepartments() {
        int rootDeptId = safeRootDeptId();
        Map<Long, DeptMeta> all = new LinkedHashMap<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        queue.add((long) rootDeptId);
        all.put((long) rootDeptId, new DeptMeta((long) rootDeptId, null,
                null, null, null, null));

        while (!queue.isEmpty()) {
            Long deptId = queue.poll();
            DeptMeta parentMeta = all.get(deptId);
            Map<String, Object> body = dingTalkOpenApiClient.body();
            body.put("dept_id", deptId);
            JsonNode root = dingTalkOpenApiClient.postTopApiWithRetry("/topapi/v2/department/listsub", body);
            JsonNode result = root.path("result");
            JsonNode list = result.path("result");
            if (!list.isArray()) {
                list = result;
            }
            if (!list.isArray()) {
                continue;
            }
            for (JsonNode node : list) {
                long childId = node.path("dept_id").asLong(0L);
                if (childId <= 0) {
                    childId = node.path("deptId").asLong(0L);
                }
                if (childId > 0 && !all.containsKey(childId)) {
                    String childName = text(node, "name");
                    Long order = longValue(node, "order", "dept_order", "deptOrder", "sort");
                    Long topDeptId;
                    String topDeptName;
                    if (Objects.equals(deptId, (long) rootDeptId)) {
                        topDeptId = childId;
                        topDeptName = childName;
                    } else {
                        topDeptId = parentMeta == null ? childId : parentMeta.getTopDeptId();
                        topDeptName = parentMeta == null ? childName : parentMeta.getTopDeptName();
                    }
                    all.put(childId, new DeptMeta(childId, childName, deptId, topDeptId, topDeptName, order));
                    queue.add(childId);
                }
            }
        }
        return new DepartmentIndex(all);
    }

    private DingTalkUserSnapshot toUserSnapshot(JsonNode node, DeptMeta deptMeta) {
        DingTalkUserSnapshot snapshot = new DingTalkUserSnapshot();
        snapshot.setUserId(text(node, "userid", "user_id", "userId"));
        snapshot.setName(text(node, "name"));
        snapshot.setMobile(text(node, "mobile"));
        snapshot.setEmail(text(node, "email"));
        snapshot.setJobTitle(text(node, "title", "position", "jobTitle", "job_title"));
        snapshot.setJobNumber(text(node, "job_number", "jobNumber", "jobnumber", "employeeNo", "employee_no"));
        snapshot.setAdmin(bool(node, "admin", "isAdmin", "is_admin"));
        snapshot.setBoss(bool(node, "boss", "isBoss", "is_boss"));
        snapshot.setLeader(bool(node, "leader", "isLeader", "is_leader"));
        snapshot.setManagerUserId(text(node, "manager_userid", "managerUserid", "managerUserId", "manager_user_id"));
        snapshot.setRoleNames(roleNames(node));
        snapshot.setDeptId(deptMeta == null ? null : deptMeta.getId());
        snapshot.setDeptName(deptMeta == null ? null : deptMeta.getName());
        snapshot.setTopDeptId(deptMeta == null ? null : deptMeta.getTopDeptId());
        snapshot.setTopDeptName(deptMeta == null ? null : deptMeta.getTopDeptName());
        snapshot.setActive(node.path("active").asBoolean(true));
        snapshot.setRawPayload(node.toString());
        return snapshot;
    }

    private List<DingTalkUserSnapshot> enrichUsersWithDetail(List<DingTalkUserSnapshot> users) {
        if (!Boolean.TRUE.equals(dingTalkProperties.getSync().getUserDetailEnabled()) || users.isEmpty()) {
            return users;
        }

        long intervalMs = safeUserDetailIntervalMs();
        long start = System.currentTimeMillis();
        int detailCalls = 0;
        for (DingTalkUserSnapshot user : users) {
            if (!StringUtils.hasText(user.getUserId())) {
                continue;
            }
            // The list API may omit job title, so enrich until identity and position are complete.
            if (hasCompleteUserDetail(user)) {
                continue;
            }
            try {
                Map<String, Object> body = dingTalkOpenApiClient.body();
                body.put("userid", user.getUserId());
                JsonNode root = dingTalkOpenApiClient.postTopApiWithRetry("/topapi/v2/user/get", body);
                JsonNode result = root.path("result");
                if (!result.isMissingNode() && !result.isNull()) {
                    mergeDetail(user, result);
                }
                detailCalls++;
            } catch (Exception ignored) {
                // continue on partial user detail failures
            }

            if (intervalMs > 0L) {
                try {
                    TimeUnit.MILLISECONDS.sleep(intervalMs);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        long cost = System.currentTimeMillis() - start;
        if (detailCalls > 0) {
            org.slf4j.LoggerFactory.getLogger(DingTalkDirectoryService.class).info(
                    "DingTalk user detail enrichment finished, users={}, detailCalls={}, costMs={}",
                    users.size(), detailCalls, cost);
        }
        return users;
    }

    private void mergeDetail(DingTalkUserSnapshot snapshot, JsonNode detail) {
        String name = text(detail, "name");
        String mobile = text(detail, "mobile");
        String email = text(detail, "email");
        String jobTitle = text(detail, "title", "position", "jobTitle", "job_title");
        String jobNumber = text(detail, "job_number", "jobNumber", "jobnumber", "employeeNo", "employee_no");
        Boolean admin = bool(detail, "admin", "isAdmin", "is_admin");
        Boolean boss = bool(detail, "boss", "isBoss", "is_boss");
        Boolean leader = bool(detail, "leader", "isLeader", "is_leader");
        String managerUserId = text(detail, "manager_userid", "managerUserid", "managerUserId", "manager_user_id");
        List<String> roleNames = roleNames(detail);
        if (StringUtils.hasText(name)) {
            snapshot.setName(name);
        }
        if (StringUtils.hasText(mobile)) {
            snapshot.setMobile(mobile);
        }
        if (StringUtils.hasText(email)) {
            snapshot.setEmail(email);
        }
        if (StringUtils.hasText(jobTitle)) {
            snapshot.setJobTitle(jobTitle);
        }
        if (StringUtils.hasText(jobNumber)) {
            snapshot.setJobNumber(jobNumber);
        }
        if (admin != null) {
            snapshot.setAdmin(admin);
        }
        if (boss != null) {
            snapshot.setBoss(boss);
        }
        if (leader != null) {
            snapshot.setLeader(leader);
        }
        // 详情接口已处理过主管字段：有值就写，无值也写空串，避免反复拉取详情
        if (managerUserId != null) {
            snapshot.setManagerUserId(managerUserId);
        } else if (snapshot.getManagerUserId() == null) {
            snapshot.setManagerUserId("");
        }
        if (roleNames != null) {
            snapshot.setRoleNames(roleNames);
        }
        JsonNode active = detail.path("active");
        if (!active.isMissingNode() && !active.isNull()) {
            snapshot.setActive(active.asBoolean(Boolean.TRUE.equals(snapshot.getActive())));
        }
        snapshot.setRawPayload(detail.toString());
    }

    private boolean hasCompleteUserDetail(DingTalkUserSnapshot user) {
        // job_number / manager_userid 在 list 接口常缺失，详情补全时尽量带上
        return StringUtils.hasText(user.getMobile())
                && StringUtils.hasText(user.getName())
                && StringUtils.hasText(user.getJobTitle())
                && StringUtils.hasText(user.getJobNumber())
                && user.getAdmin() != null
                && user.getBoss() != null
                && user.getLeader() != null
                && user.getRoleNames() != null
                // managerUserId 非 null 表示详情已处理（可能是空串=无主管）
                && user.getManagerUserId() != null;
    }

    private String text(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode v = node.path(name);
            if (!v.isMissingNode() && !v.isNull()) {
                String text = v.asText("");
                if (StringUtils.hasText(text)) {
                    return text.trim();
                }
            }
        }
        return null;
    }

    private Long longValue(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode v = node.path(name);
            if (v == null || v.isMissingNode() || v.isNull()) {
                continue;
            }
            if (v.isNumber()) {
                return v.asLong();
            }
            String text = v.asText(null);
            if (!StringUtils.hasText(text)) {
                continue;
            }
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                // try next field
            }
        }
        return null;
    }

    private Boolean bool(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode v = node.path(name);
            Boolean value = bool(v);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Boolean bool(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isNumber()) {
            return node.asInt() != 0;
        }
        String text = node.asText(null);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String normalized = text.trim().toLowerCase();
        if ("true".equals(normalized) || "1".equals(normalized)
                || "yes".equals(normalized) || "y".equals(normalized)
                || "是".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized) || "0".equals(normalized)
                || "no".equals(normalized) || "n".equals(normalized)
                || "否".equals(normalized)) {
            return false;
        }
        return null;
    }

    private List<String> roleNames(JsonNode node) {
        JsonNode rolesNode = firstNode(node, "role_list", "roleList", "roles");
        if (rolesNode == null) {
            return null;
        }
        Set<String> names = new LinkedHashSet<>();
        collectRoleNames(rolesNode, names);
        return new ArrayList<>(names);
    }

    private void collectRoleNames(JsonNode node, Set<String> names) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                collectRoleNames(item, names);
            }
            return;
        }
        if (node.isObject()) {
            JsonNode nested = firstNode(node, "list", "role_list", "roleList", "roles");
            if (nested != null) {
                collectRoleNames(nested, names);
            }
            String name = text(node, "name", "role_name", "roleName", "role", "label");
            if (StringUtils.hasText(name)) {
                names.add(name);
            }
            return;
        }
        String text = node.asText(null);
        if (StringUtils.hasText(text)) {
            names.add(text.trim());
        }
    }

    private JsonNode firstNode(JsonNode node, String... names) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        for (String name : names) {
            JsonNode value = node.path(name);
            if (!value.isMissingNode() && !value.isNull()) {
                return value;
            }
        }
        return null;
    }

    private int safeRootDeptId() {
        Integer value = dingTalkProperties.getSync().getRootDeptId();
        return value == null || value <= 0 ? 1 : value;
    }

    private long safeUserDetailIntervalMs() {
        Long value = dingTalkProperties.getSync().getUserDetailIntervalMs();
        return value == null ? 0L : Math.max(value, 0L);
    }

    private static class CachedUserIndex {

        private final Map<String, DingTalkUserSnapshot> byMobile;

        private final long expireAtEpochSeconds;

        private CachedUserIndex(Map<String, DingTalkUserSnapshot> byMobile, long expireAtEpochSeconds) {
            this.byMobile = byMobile;
            this.expireAtEpochSeconds = expireAtEpochSeconds;
        }

    }

    private static class DepartmentIndex {

        private final Map<Long, DeptMeta> deptMap;

        private DepartmentIndex(Map<Long, DeptMeta> deptMap) {
            this.deptMap = deptMap;
        }

        private Map<Long, DeptMeta> getDeptMap() {
            return deptMap;
        }
    }

    private static class DeptMeta {

        private final Long id;

        private final String name;

        private final Long parentId;

        private final Long topDeptId;

        private final String topDeptName;

        private final Long order;

        private DeptMeta(Long id, String name, Long parentId, Long topDeptId, String topDeptName, Long order) {
            this.id = id;
            this.name = name;
            this.parentId = parentId;
            this.topDeptId = topDeptId;
            this.topDeptName = topDeptName;
            this.order = order;
        }

        private Long getId() {
            return id;
        }

        private String getName() {
            return name;
        }

        private Long getParentId() {
            return parentId;
        }

        private Long getTopDeptId() {
            return topDeptId;
        }

        private String getTopDeptName() {
            return topDeptName;
        }

        private Long getOrder() {
            return order;
        }
    }

}
