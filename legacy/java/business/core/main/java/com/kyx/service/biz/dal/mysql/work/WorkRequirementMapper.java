package com.kyx.service.biz.dal.mysql.work;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.biz.controller.admin.work.vo.WorkRequirementPageReqVO;
import com.kyx.service.biz.dal.dataobject.work.WorkRequirementDO;
import com.kyx.service.biz.enums.WorkRequirementStatusEnum;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Mapper
public interface WorkRequirementMapper extends BaseMapperX<WorkRequirementDO> {

    default PageResult<WorkRequirementDO> selectPage(WorkRequirementPageReqVO reqVO) {
        return selectPage(reqVO, buildListPageWrapper(reqVO, null));
    }

    default PageResult<WorkRequirementDO> selectPage(WorkRequirementPageReqVO reqVO, Collection<Long> tenantIds) {
        return selectPage(reqVO, buildListPageWrapper(reqVO, tenantIds));
    }

    default List<WorkRequirementDO> selectChildrenByParentId(Long parentId) {
        return selectChildrenByParentId(parentId, null);
    }

    default List<WorkRequirementDO> selectChildrenByParentId(Long parentId, WorkRequirementPageReqVO reqVO) {
        LambdaQueryWrapperX<WorkRequirementDO> wrapper = new LambdaQueryWrapperX<WorkRequirementDO>()
                .eq(WorkRequirementDO::getParentId, parentId);
        if (reqVO != null) {
            appendDirectFilterConditions(wrapper, reqVO);
        }
        return selectList(wrapper
                .orderByDesc(WorkRequirementDO::getUpdateTime)
                .orderByDesc(WorkRequirementDO::getId));
    }

    default List<WorkRequirementDO> selectSubtreeByPath(Long requirementId, String path) {
        LambdaQueryWrapperX<WorkRequirementDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.and(query -> {
            query.eq(WorkRequirementDO::getId, requirementId)
                    .or()
                    .eq(WorkRequirementDO::getParentId, requirementId);
            if (StrUtil.isNotBlank(path)) {
                query.or().likeRight(WorkRequirementDO::getPath, path);
            }
        });
        wrapper.orderByDesc(WorkRequirementDO::getLevel)
                .orderByDesc(WorkRequirementDO::getUpdateTime)
                .orderByDesc(WorkRequirementDO::getId);
        return selectList(wrapper);
    }

    default int deleteByRequirementIds(Collection<Long> requirementIds) {
        return deleteBatch(WorkRequirementDO::getId, requirementIds);
    }

    default Long countByParentId(Long parentId) {
        return selectCount(new LambdaQueryWrapperX<WorkRequirementDO>()
                .eq(WorkRequirementDO::getParentId, parentId));
    }

    default Long countOpenChildren(Long parentId) {
        return selectCount(new LambdaQueryWrapperX<WorkRequirementDO>()
                .eq(WorkRequirementDO::getParentId, parentId)
                .notIn(WorkRequirementDO::getStatus,
                        WorkRequirementStatusEnum.DONE.getStatus(),
                        WorkRequirementStatusEnum.CANCELED.getStatus()));
    }

    @Select({
            "SELECT COUNT(1)",
            "FROM business_work_requirement target",
            "JOIN business_work_requirement child_req ON child_req.deleted = b'0'",
            "  AND child_req.tenant_id = target.tenant_id",
            "  AND child_req.id <> target.id",
            "  AND (child_req.parent_id = target.id",
            "    OR (target.tree_path IS NOT NULL AND child_req.tree_path LIKE CONCAT(target.tree_path, '_%'))",
            "    OR (target.parent_id IS NULL AND child_req.root_id = target.id))",
            "WHERE target.deleted = b'0'",
            "AND target.id = #{requirementId}",
            "AND child_req.status NOT IN (5, 6)"
    })
    Long countOpenDescendants(@Param("requirementId") Long requirementId);

    default boolean existsTreeParticipant(Long requirementId, Long userId) {
        if (requirementId == null || userId == null) {
            return false;
        }
        Long count = countTreeParticipant(requirementId, userId);
        return count != null && count > 0;
    }

    @Select({
            "SELECT COUNT(1)",
            "FROM business_work_requirement target",
            "JOIN business_work_requirement related_req ON related_req.deleted = b'0'",
            "  AND related_req.tenant_id = target.tenant_id",
            "  AND (related_req.id = COALESCE(target.root_id, target.id)",
            "    OR related_req.root_id = COALESCE(target.root_id, target.id))",
            "WHERE target.deleted = b'0'",
            "AND target.id = #{requirementId}",
            "AND (related_req.proposer_user_id = #{userId}",
            "  OR related_req.assignee_user_id = #{userId}",
            "  OR EXISTS (SELECT 1 FROM business_work_requirement_developer d",
            "    WHERE d.requirement_id = related_req.id",
            "    AND d.user_id = #{userId}",
            "    AND d.deleted = b'0'))"
    })
    Long countTreeParticipant(@Param("requirementId") Long requirementId,
                              @Param("userId") Long userId);

    default int incrementChildCount(Long id) {
        return update(null, new LambdaUpdateWrapper<WorkRequirementDO>()
                .setSql("child_count = child_count + 1")
                .eq(WorkRequirementDO::getId, id));
    }

    default int decrementChildCount(Long id) {
        return update(null, new LambdaUpdateWrapper<WorkRequirementDO>()
                .setSql("child_count = GREATEST(child_count - 1, 0)")
                .eq(WorkRequirementDO::getId, id));
    }

    default int updateEstimatedUserCount(Long id, Integer estimatedUserCount) {
        return update(null, new LambdaUpdateWrapper<WorkRequirementDO>()
                .set(WorkRequirementDO::getEstimatedUserCount, estimatedUserCount)
                .eq(WorkRequirementDO::getId, id));
    }

    default List<WorkRequirementDO> selectOverviewList(WorkRequirementPageReqVO reqVO, Collection<Long> tenantIds) {
        LambdaQueryWrapperX<WorkRequirementDO> wrapper = buildFilterWrapper(reqVO, tenantIds);
        wrapper.select(WorkRequirementDO::getId,
                WorkRequirementDO::getStatus,
                WorkRequirementDO::getAssigneeUserId,
                WorkRequirementDO::getExpectedFinishDate,
                WorkRequirementDO::getCreateTime,
                WorkRequirementDO::getAcceptedTime,
                WorkRequirementDO::getCloseTime);
        return selectList(wrapper);
    }

    default Map<String, Object> selectOverviewCountMap(WorkRequirementPageReqVO reqVO,
                                                       Collection<Long> tenantIds,
                                                       Long userId) {
        QueryWrapper<WorkRequirementDO> wrapper = buildFilterQueryWrapper(reqVO, tenantIds);
        wrapper.select("COUNT(1) AS totalCount",
                "COALESCE(SUM(CASE WHEN status IN (" + statusSql(
                        WorkRequirementStatusEnum.PENDING_ASSIGN.getStatus(),
                        WorkRequirementStatusEnum.PENDING_DEVELOP.getStatus(),
                        WorkRequirementStatusEnum.TESTING.getStatus(),
                        WorkRequirementStatusEnum.PENDING_ACCEPT.getStatus()) + ") THEN 1 ELSE 0 END), 0) AS pendingCount",
                "COALESCE(SUM(CASE WHEN status = " + WorkRequirementStatusEnum.DEVELOPING.getStatus()
                        + " THEN 1 ELSE 0 END), 0) AS developingCount",
                "COALESCE(SUM(CASE WHEN status = " + WorkRequirementStatusEnum.DONE.getStatus()
                        + " THEN 1 ELSE 0 END), 0) AS completedCount",
                buildMyTodoCountSql(userId),
                "COALESCE(SUM(CASE WHEN expected_finish_date IS NOT NULL AND status NOT IN (" + statusSql(
                        WorkRequirementStatusEnum.DONE.getStatus(),
                        WorkRequirementStatusEnum.CANCELED.getStatus(),
                        WorkRequirementStatusEnum.SUSPENDED.getStatus()) + ") AND expected_finish_date < CURDATE()"
                        + " THEN 1 ELSE 0 END), 0) AS overdueCount");
        List<Map<String, Object>> rows = selectMaps(wrapper);
        return CollUtil.isEmpty(rows) ? Collections.emptyMap() : rows.get(0);
    }

    default List<Map<String, Object>> selectOverviewStatusCountMaps(WorkRequirementPageReqVO reqVO,
                                                                    Collection<Long> tenantIds) {
        QueryWrapper<WorkRequirementDO> wrapper = buildFilterQueryWrapper(reqVO, tenantIds);
        wrapper.select("CASE WHEN status = " + WorkRequirementStatusEnum.TESTING.getStatus()
                        + " THEN " + WorkRequirementStatusEnum.PENDING_ACCEPT.getStatus()
                        + " ELSE status END AS status",
                "COUNT(1) AS count");
        wrapper.groupBy("CASE WHEN status = " + WorkRequirementStatusEnum.TESTING.getStatus()
                + " THEN " + WorkRequirementStatusEnum.PENDING_ACCEPT.getStatus()
                + " ELSE status END");
        return selectMaps(wrapper);
    }

    default List<Map<String, Object>> selectOverviewCreatedTrendCountMaps(WorkRequirementPageReqVO reqVO,
                                                                          Collection<Long> tenantIds,
                                                                          Date startTime,
                                                                          Date endTime) {
        QueryWrapper<WorkRequirementDO> wrapper = buildFilterQueryWrapper(reqVO, tenantIds);
        wrapper.select("DATE(create_time) AS trendDate", "COUNT(1) AS count")
                .ge("create_time", startTime)
                .lt("create_time", endTime)
                .groupBy("DATE(create_time)");
        return selectMaps(wrapper);
    }

    default List<Map<String, Object>> selectOverviewFinishedTrendCountMaps(WorkRequirementPageReqVO reqVO,
                                                                           Collection<Long> tenantIds,
                                                                           Date startTime,
                                                                           Date endTime) {
        QueryWrapper<WorkRequirementDO> wrapper = buildFilterQueryWrapper(reqVO, tenantIds);
        wrapper.select("DATE(COALESCE(accepted_time, close_time)) AS trendDate", "COUNT(1) AS count")
                .apply("COALESCE(accepted_time, close_time) >= {0}", startTime)
                .apply("COALESCE(accepted_time, close_time) < {0}", endTime)
                .groupBy("DATE(COALESCE(accepted_time, close_time))");
        return selectMaps(wrapper);
    }

    default LambdaQueryWrapperX<WorkRequirementDO> buildPageWrapper(WorkRequirementPageReqVO reqVO, Collection<Long> tenantIds) {
        LambdaQueryWrapperX<WorkRequirementDO> wrapper = buildFilterWrapper(reqVO, tenantIds);
        String sortField = reqVO == null ? null : reqVO.getSortField();
        boolean asc = reqVO != null && "asc".equalsIgnoreCase(reqVO.getSortOrder());
        if ("id".equals(sortField)) {
            wrapper.orderBy(true, asc, WorkRequirementDO::getId);
        } else if ("priority".equals(sortField)) {
            wrapper.orderBy(true, asc, WorkRequirementDO::getPriority);
        } else if ("status".equals(sortField)) {
            wrapper.orderBy(true, asc, WorkRequirementDO::getStatus);
        } else if ("approvalStatus".equals(sortField)) {
            wrapper.orderBy(true, asc, WorkRequirementDO::getApprovalStatus);
        } else if ("expectedFinishDate".equals(sortField)) {
            wrapper.orderBy(true, asc, WorkRequirementDO::getExpectedFinishDate);
        } else if ("acceptedTime".equals(sortField)) {
            wrapper.orderBy(true, asc, WorkRequirementDO::getAcceptedTime);
        } else if ("createTime".equals(sortField)) {
            wrapper.orderBy(true, asc, WorkRequirementDO::getCreateTime);
        } else {
            wrapper.orderBy(true, asc, WorkRequirementDO::getUpdateTime);
        }
        return wrapper.orderByDesc(WorkRequirementDO::getId);
    }

    default LambdaQueryWrapperX<WorkRequirementDO> buildListPageWrapper(WorkRequirementPageReqVO reqVO, Collection<Long> tenantIds) {
        LambdaQueryWrapperX<WorkRequirementDO> wrapper = buildPageWrapper(reqVO, tenantIds);
        wrapper.select(WorkRequirementDO::getId,
                WorkRequirementDO::getParentId,
                WorkRequirementDO::getRootId,
                WorkRequirementDO::getLevel,
                WorkRequirementDO::getPath,
                WorkRequirementDO::getChildCount,
                WorkRequirementDO::getTitle,
                WorkRequirementDO::getPriority,
                WorkRequirementDO::getStatus,
                WorkRequirementDO::getProcessInstanceId,
                WorkRequirementDO::getApprovalStatus,
                WorkRequirementDO::getProposerDept,
                WorkRequirementDO::getTargetDept,
                WorkRequirementDO::getProposerName,
                WorkRequirementDO::getProposerUserId,
                WorkRequirementDO::getAssigneeUserId,
                WorkRequirementDO::getAssigneeName,
                WorkRequirementDO::getExpectedFinishDate,
                WorkRequirementDO::getSubmitTestTime,
                WorkRequirementDO::getTestPassTime,
                WorkRequirementDO::getAcceptedTime,
                WorkRequirementDO::getCloseTime,
                WorkRequirementDO::getPreviousStatus,
                WorkRequirementDO::getLastRejectReason,
                WorkRequirementDO::getAttachmentUrls,
                WorkRequirementDO::getCreateTime,
                WorkRequirementDO::getUpdateTime,
                WorkRequirementDO::getTenantId);
        return wrapper;
    }

    default LambdaQueryWrapperX<WorkRequirementDO> buildFilterWrapper(WorkRequirementPageReqVO reqVO, Collection<Long> tenantIds) {
        LambdaQueryWrapperX<WorkRequirementDO> wrapper = new LambdaQueryWrapperX<WorkRequirementDO>()
                .inIfPresent(WorkRequirementDO::getTenantId, tenantIds);
        if (shouldIncludeChildMatches(reqVO)) {
            wrapper.isNull(WorkRequirementDO::getParentId);
            if (hasDirectFilter(reqVO)) {
                wrapper.and(query -> {
                    appendDirectFilterConditions(query, reqVO);
                    query.or().exists(buildChildMatchExistsSql(reqVO));
                });
            }
            return wrapper;
        }
        appendDirectFilterConditions(wrapper, reqVO);
        if (reqVO.getParentId() != null) {
            wrapper.eq(WorkRequirementDO::getParentId, reqVO.getParentId());
        } else if (Boolean.TRUE.equals(reqVO.getOnlyRoot())) {
            wrapper.isNull(WorkRequirementDO::getParentId);
        }
        return wrapper;
    }

    static boolean shouldIncludeChildMatches(WorkRequirementPageReqVO reqVO) {
        return Boolean.TRUE.equals(reqVO.getOnlyRoot())
                && Boolean.TRUE.equals(reqVO.getIncludeChildMatches())
                && reqVO.getStatus() == null
                && reqVO.getParentId() == null;
    }

    static boolean hasDirectFilter(WorkRequirementPageReqVO reqVO) {
        return reqVO.getStatus() != null
                || reqVO.getPriority() != null
                || reqVO.getApprovalStatus() != null
                || reqVO.getRootId() != null
                || reqVO.getProposerUserId() != null
                || StrUtil.isNotBlank(reqVO.getProposerName())
                || StrUtil.isNotBlank(reqVO.getAssigneeName())
                || Boolean.TRUE.equals(reqVO.getAssigneeUnassignedOnly())
                || reqVO.getAssigneeUserId() != null
                || reqVO.getUserId() != null
                || StrUtil.isNotBlank(reqVO.getProcessInstanceIds())
                || StrUtil.isNotBlank(reqVO.getTrimmedKeyword())
                || (Boolean.TRUE.equals(reqVO.getCommentUnreadOnly()) && reqVO.getUnreadCommentUserId() != null)
                || reqVO.getCreateTimeRange() != null
                || reqVO.getUpdateTimeRange() != null;
    }

    static void appendDirectFilterConditions(LambdaQueryWrapper<WorkRequirementDO> wrapper,
                                             WorkRequirementPageReqVO reqVO) {
        wrapper.eq(reqVO.getStatus() != null, WorkRequirementDO::getStatus, reqVO.getStatus())
                .eq(reqVO.getPriority() != null, WorkRequirementDO::getPriority, reqVO.getPriority())
                .eq(reqVO.getApprovalStatus() != null, WorkRequirementDO::getApprovalStatus, reqVO.getApprovalStatus())
                .eq(reqVO.getRootId() != null, WorkRequirementDO::getRootId, reqVO.getRootId())
                .eq(reqVO.getProposerUserId() != null, WorkRequirementDO::getProposerUserId, reqVO.getProposerUserId())
                .like(StrUtil.isNotBlank(reqVO.getProposerName()), WorkRequirementDO::getProposerName, reqVO.getProposerName())
                .like(StrUtil.isNotBlank(reqVO.getAssigneeName()), WorkRequirementDO::getAssigneeName, reqVO.getAssigneeName());
        if (Boolean.TRUE.equals(reqVO.getAssigneeUnassignedOnly())) {
            wrapper.isNull(WorkRequirementDO::getAssigneeUserId);
        }
        if (reqVO.getAssigneeUserId() != null) {
            wrapper.and(query -> query.eq(WorkRequirementDO::getAssigneeUserId, reqVO.getAssigneeUserId())
                    .or().exists(buildDeveloperExistsSql(reqVO.getAssigneeUserId())));
        }
        if (reqVO.getUserId() != null) {
            wrapper.and(query -> query.eq(WorkRequirementDO::getProposerUserId, reqVO.getUserId())
                    .or().eq(WorkRequirementDO::getAssigneeUserId, reqVO.getUserId())
                    .or().exists(buildDeveloperExistsSql(reqVO.getUserId())));
        }
        if (StrUtil.isNotBlank(reqVO.getProcessInstanceIds())) {
            wrapper.in(WorkRequirementDO::getProcessInstanceId,
                    StrUtil.splitTrim(reqVO.getProcessInstanceIds(), ','));
        }
        if (StrUtil.isNotBlank(reqVO.getTrimmedKeyword())) {
            String keyword = reqVO.getTrimmedKeyword();
            Long keywordId = parseKeywordId(keyword);
            wrapper.and(query -> {
                query.like(WorkRequirementDO::getTitle, keyword)
                        .or().like(WorkRequirementDO::getDescription, keyword)
                        .or().like(WorkRequirementDO::getProposerName, keyword)
                        .or().like(WorkRequirementDO::getTargetDept, keyword)
                        .or().like(WorkRequirementDO::getAssigneeName, keyword)
                        .or().like(WorkRequirementDO::getLastRejectReason, keyword);
                if (keywordId != null) {
                    query.or().eq(WorkRequirementDO::getId, keywordId);
                }
            });
        }
        if (Boolean.TRUE.equals(reqVO.getCommentUnreadOnly()) && reqVO.getUnreadCommentUserId() != null) {
            wrapper.exists(buildUnreadCommentExistsSql(reqVO.getUnreadCommentUserId()));
        }
        java.time.LocalDateTime[] createRange = reqVO.getCreateTimeRange();
        if (createRange != null) {
            wrapper.between(WorkRequirementDO::getCreateTime, createRange[0], createRange[1]);
        }
        java.time.LocalDateTime[] updateRange = reqVO.getUpdateTimeRange();
        if (updateRange != null) {
            wrapper.between(WorkRequirementDO::getUpdateTime, updateRange[0], updateRange[1]);
        }
    }

    default QueryWrapper<WorkRequirementDO> buildFilterQueryWrapper(WorkRequirementPageReqVO reqVO,
                                                                    Collection<Long> tenantIds) {
        QueryWrapper<WorkRequirementDO> wrapper = new QueryWrapper<>();
        if (CollUtil.isNotEmpty(tenantIds)) {
            wrapper.in("tenant_id", tenantIds);
        }
        wrapper.eq(reqVO.getStatus() != null, "status", reqVO.getStatus())
                .eq(reqVO.getPriority() != null, "priority", reqVO.getPriority())
                .eq(reqVO.getApprovalStatus() != null, "approval_status", reqVO.getApprovalStatus())
                .eq(reqVO.getRootId() != null, "root_id", reqVO.getRootId())
                .eq(reqVO.getProposerUserId() != null, "proposer_user_id", reqVO.getProposerUserId())
                .like(StrUtil.isNotBlank(reqVO.getProposerName()), "proposer_name", reqVO.getProposerName())
                .like(StrUtil.isNotBlank(reqVO.getAssigneeName()), "assignee_name", reqVO.getAssigneeName());
        if (Boolean.TRUE.equals(reqVO.getAssigneeUnassignedOnly())) {
            wrapper.isNull("assignee_user_id");
        }
        if (reqVO.getParentId() != null) {
            wrapper.eq("parent_id", reqVO.getParentId());
        } else if (Boolean.TRUE.equals(reqVO.getOnlyRoot())) {
            wrapper.isNull("parent_id");
        }
        if (reqVO.getAssigneeUserId() != null) {
            wrapper.and(query -> query.eq("assignee_user_id", reqVO.getAssigneeUserId())
                    .or().exists(buildDeveloperExistsSql(reqVO.getAssigneeUserId())));
        }
        if (reqVO.getUserId() != null) {
            wrapper.and(query -> query.eq("proposer_user_id", reqVO.getUserId())
                    .or().eq("assignee_user_id", reqVO.getUserId())
                    .or().exists(buildDeveloperExistsSql(reqVO.getUserId())));
        }
        if (CollUtil.isNotEmpty(reqVO.getProcessInstanceIdList())) {
            wrapper.in("process_instance_id", reqVO.getProcessInstanceIdList());
        }
        if (StrUtil.isNotBlank(reqVO.getTrimmedKeyword())) {
            String keyword = reqVO.getTrimmedKeyword();
            Long keywordId = parseKeywordId(keyword);
            wrapper.and(query -> {
                query.like("title", keyword)
                        .or().like("description", keyword)
                        .or().like("proposer_name", keyword)
                        .or().like("target_dept", keyword)
                        .or().like("assignee_name", keyword)
                        .or().like("last_reject_reason", keyword);
                if (keywordId != null) {
                    query.or().eq("id", keywordId);
                }
            });
        }
        if (Boolean.TRUE.equals(reqVO.getCommentUnreadOnly()) && reqVO.getUnreadCommentUserId() != null) {
            wrapper.exists(buildUnreadCommentExistsSql(reqVO.getUnreadCommentUserId()));
        }
        java.time.LocalDateTime[] createRange2 = reqVO.getCreateTimeRange();
        if (createRange2 != null) {
            wrapper.between("create_time", createRange2[0], createRange2[1]);
        }
        java.time.LocalDateTime[] updateRange2 = reqVO.getUpdateTimeRange();
        if (updateRange2 != null) {
            wrapper.between("update_time", updateRange2[0], updateRange2[1]);
        }
        return wrapper;
    }

    static Long parseKeywordId(String keyword) {
        if (StrUtil.isBlank(keyword) || !keyword.matches("\\d{1,18}")) {
            return null;
        }
        try {
            return Long.parseLong(keyword);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    static String buildDeveloperExistsSql(Long userId) {
        String safeUserId = String.valueOf(userId);
        return "SELECT 1 FROM business_work_requirement_developer d"
                + " WHERE d.requirement_id = business_work_requirement.id"
                + " AND d.user_id = " + safeUserId
                + " AND d.deleted = b'0'";
    }

    static String buildUnreadCommentExistsSql(Long userId) {
        String safeUserId = String.valueOf(userId);
        return "SELECT 1 FROM business_work_requirement_comment c"
                + " LEFT JOIN business_work_requirement_comment_read cr"
                + " ON cr.comment_id = c.id AND cr.user_id = " + safeUserId
                + " AND cr.deleted = b'0'"
                + " WHERE c.deleted = b'0'"
                + " AND c.requirement_id = business_work_requirement.id"
                + " AND c.tenant_id = business_work_requirement.tenant_id"
                + " AND c.from_user_id <> " + safeUserId
                + " AND cr.id IS NULL"
                + " AND (c.target_user_id = " + safeUserId
                + " OR (c.target_user_id IS NULL AND (business_work_requirement.proposer_user_id = " + safeUserId
                + " OR business_work_requirement.assignee_user_id = " + safeUserId
                + " OR EXISTS (" + buildDeveloperExistsSql(userId).replace("business_work_requirement.id", "c.requirement_id")
                + "))))";
    }

    static String buildChildMatchExistsSql(WorkRequirementPageReqVO reqVO) {
        StringBuilder sql = new StringBuilder("SELECT 1 FROM business_work_requirement child_req"
                + " WHERE child_req.deleted = b'0'"
                + " AND child_req.id <> business_work_requirement.id"
                + " AND (child_req.parent_id = business_work_requirement.id"
                + " OR child_req.root_id = business_work_requirement.id)"
                + " AND child_req.tenant_id = business_work_requirement.tenant_id");
        appendChildEq(sql, "child_req.status", reqVO.getStatus());
        appendChildEq(sql, "child_req.priority", reqVO.getPriority());
        appendChildEq(sql, "child_req.approval_status", reqVO.getApprovalStatus());
        appendChildEq(sql, "child_req.root_id", reqVO.getRootId());
        appendChildEq(sql, "child_req.proposer_user_id", reqVO.getProposerUserId());
        appendChildLike(sql, "child_req.proposer_name", reqVO.getProposerName());
        appendChildLike(sql, "child_req.assignee_name", reqVO.getAssigneeName());
        if (Boolean.TRUE.equals(reqVO.getAssigneeUnassignedOnly())) {
            sql.append(" AND child_req.assignee_user_id IS NULL");
        }
        if (reqVO.getAssigneeUserId() != null) {
            String safeUserId = String.valueOf(reqVO.getAssigneeUserId());
            sql.append(" AND (child_req.assignee_user_id = ").append(safeUserId)
                    .append(" OR EXISTS (")
                    .append(buildDeveloperExistsSql(reqVO.getAssigneeUserId())
                            .replace("business_work_requirement.id", "child_req.id"))
                    .append("))");
        }
        if (reqVO.getUserId() != null) {
            String safeUserId = String.valueOf(reqVO.getUserId());
            sql.append(" AND (child_req.proposer_user_id = ").append(safeUserId)
                    .append(" OR child_req.assignee_user_id = ").append(safeUserId)
                    .append(" OR EXISTS (")
                    .append(buildDeveloperExistsSql(reqVO.getUserId())
                            .replace("business_work_requirement.id", "child_req.id"))
                    .append("))");
        }
        if (CollUtil.isNotEmpty(reqVO.getProcessInstanceIdList())) {
            String processInstanceIds = reqVO.getProcessInstanceIdList().stream()
                    .filter(StrUtil::isNotBlank)
                    .map(WorkRequirementMapper::sqlLiteral)
                    .collect(java.util.stream.Collectors.joining(","));
            if (StrUtil.isNotBlank(processInstanceIds)) {
                sql.append(" AND child_req.process_instance_id IN (").append(processInstanceIds).append(")");
            }
        }
        if (StrUtil.isNotBlank(reqVO.getTrimmedKeyword())) {
            String keyword = reqVO.getTrimmedKeyword();
            Long keywordId = parseKeywordId(keyword);
            sql.append(" AND (")
                    .append(buildLikeSql("child_req.title", keyword))
                    .append(" OR ").append(buildLikeSql("child_req.description", keyword))
                    .append(" OR ").append(buildLikeSql("child_req.proposer_name", keyword))
                    .append(" OR ").append(buildLikeSql("child_req.target_dept", keyword))
                    .append(" OR ").append(buildLikeSql("child_req.assignee_name", keyword))
                    .append(" OR ").append(buildLikeSql("child_req.last_reject_reason", keyword));
            if (keywordId != null) {
                sql.append(" OR child_req.id = ").append(keywordId);
            }
            sql.append(")");
        }
        if (Boolean.TRUE.equals(reqVO.getCommentUnreadOnly()) && reqVO.getUnreadCommentUserId() != null) {
            sql.append(" AND EXISTS (")
                    .append(buildUnreadCommentExistsSql(reqVO.getUnreadCommentUserId())
                            .replace("business_work_requirement.", "child_req."))
                    .append(")");
        }
        return sql.toString();
    }

    static void appendChildEq(StringBuilder sql, String column, Object value) {
        if (value != null) {
            sql.append(" AND ").append(column).append(" = ").append(value);
        }
    }

    static void appendChildLike(StringBuilder sql, String column, String value) {
        if (StrUtil.isNotBlank(value)) {
            sql.append(" AND ").append(buildLikeSql(column, value));
        }
    }

    static String buildLikeSql(String column, String value) {
        return column + " LIKE " + sqlLikeLiteral(value) + " ESCAPE '\\\\'";
    }

    static String sqlLikeLiteral(String value) {
        String escaped = StrUtil.trim(value)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_")
                .replace("'", "''");
        return "'%" + escaped + "%'";
    }

    static String sqlLiteral(String value) {
        return "'" + String.valueOf(value).replace("'", "''") + "'";
    }

    static String buildMyTodoCountSql(Long userId) {
        if (userId == null) {
            return "0 AS myTodoCount";
        }
        return "COALESCE(SUM(CASE WHEN assignee_user_id = " + userId + " AND status IN ("
                + statusSql(WorkRequirementStatusEnum.PENDING_DEVELOP.getStatus(),
                WorkRequirementStatusEnum.DEVELOPING.getStatus(),
                WorkRequirementStatusEnum.TESTING.getStatus(),
                WorkRequirementStatusEnum.PENDING_ACCEPT.getStatus())
                + ") THEN 1 ELSE 0 END), 0) AS myTodoCount";
    }

    static String statusSql(Integer... statuses) {
        return java.util.Arrays.stream(statuses)
                .map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(","));
    }

}
