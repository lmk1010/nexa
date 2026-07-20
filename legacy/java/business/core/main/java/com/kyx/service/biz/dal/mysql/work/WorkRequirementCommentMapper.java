package com.kyx.service.biz.dal.mysql.work;

import cn.hutool.core.collection.CollUtil;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.biz.controller.admin.work.vo.WorkRequirementPageReqVO;
import com.kyx.service.biz.dal.dataobject.work.WorkRequirementCommentCountDO;
import com.kyx.service.biz.dal.dataobject.work.WorkRequirementCommentDO;
import com.kyx.service.biz.dal.dataobject.work.WorkRequirementCommentReadDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface WorkRequirementCommentMapper extends BaseMapperX<WorkRequirementCommentDO> {

    default List<WorkRequirementCommentDO> selectListByRequirementId(Long requirementId) {
        return selectList(new LambdaQueryWrapperX<WorkRequirementCommentDO>()
                .eq(WorkRequirementCommentDO::getRequirementId, requirementId)
                .orderByAsc(WorkRequirementCommentDO::getCreateTime)
                .orderByAsc(WorkRequirementCommentDO::getId));
    }

    default List<WorkRequirementCommentDO> selectListByRequirementIds(Collection<Long> requirementIds) {
        return selectList(new LambdaQueryWrapperX<WorkRequirementCommentDO>()
                .inIfPresent(WorkRequirementCommentDO::getRequirementId, requirementIds)
                .orderByDesc(WorkRequirementCommentDO::getCreateTime)
                .orderByDesc(WorkRequirementCommentDO::getId));
    }

    @Select({
            "<script>",
            "SELECT DISTINCT c.requirement_id",
            "FROM business_work_requirement_comment c",
            "JOIN business_work_requirement r ON r.id = c.requirement_id AND r.deleted = b'0'",
            "LEFT JOIN business_work_requirement_comment_read cr ON cr.comment_id = c.id",
            "  AND cr.user_id = #{userId} AND cr.deleted = b'0'",
            "WHERE c.deleted = b'0'",
            "AND c.from_user_id != #{userId}",
            "AND cr.id IS NULL",
            "AND (c.target_user_id = #{userId}",
            "  OR (c.target_user_id IS NULL AND (r.proposer_user_id = #{userId}",
            "    OR r.assignee_user_id = #{userId}",
            "    OR EXISTS (SELECT 1 FROM business_work_requirement_developer d",
            "      WHERE d.requirement_id = c.requirement_id AND d.user_id = #{userId} AND d.deleted = b'0'))))",
            "<if test='tenantIds != null and tenantIds.size > 0'>",
            "AND c.tenant_id IN",
            "<foreach collection='tenantIds' item='tenantId' open='(' separator=',' close=')'>",
            "#{tenantId}",
            "</foreach>",
            "</if>",
            "</script>"
    })
    List<Long> selectUnreadRequirementIds(@Param("userId") Long userId,
                                          @Param("tenantIds") Collection<Long> tenantIds);

    default List<WorkRequirementCommentCountDO> selectCommentCountsByRequirementIds(Collection<Long> requirementIds) {
        if (CollUtil.isEmpty(requirementIds)) {
            return java.util.Collections.emptyList();
        }
        return selectCommentCountsByRequirementIdsInternal(requirementIds);
    }

    @Select({
            "<script>",
            "SELECT c.requirement_id AS requirementId, COUNT(1) AS commentCount",
            "FROM business_work_requirement_comment c",
            "WHERE c.deleted = b'0'",
            "AND c.requirement_id IN",
            "<foreach collection='requirementIds' item='requirementId' open='(' separator=',' close=')'>",
            "#{requirementId}",
            "</foreach>",
            "GROUP BY c.requirement_id",
            "</script>"
    })
    List<WorkRequirementCommentCountDO> selectCommentCountsByRequirementIdsInternal(
            @Param("requirementIds") Collection<Long> requirementIds);

    default Long countUnreadCommentsByRequirementIds(Long userId, Collection<Long> requirementIds) {
        if (userId == null || CollUtil.isEmpty(requirementIds)) {
            return 0L;
        }
        Long count = countUnreadCommentsByRequirementIdsInternal(userId, requirementIds);
        return count == null ? 0L : count;
    }

    @Select({
            "<script>",
            "SELECT COUNT(1)",
            "FROM business_work_requirement_comment c",
            "JOIN business_work_requirement r ON r.id = c.requirement_id AND r.deleted = b'0'",
            "LEFT JOIN business_work_requirement_comment_read cr ON cr.comment_id = c.id",
            "  AND cr.user_id = #{userId} AND cr.deleted = b'0'",
            "WHERE c.deleted = b'0'",
            "AND c.from_user_id != #{userId}",
            "AND cr.id IS NULL",
            "AND c.requirement_id IN",
            "<foreach collection='requirementIds' item='requirementId' open='(' separator=',' close=')'>",
            "#{requirementId}",
            "</foreach>",
            "AND (c.target_user_id = #{userId}",
            "  OR (c.target_user_id IS NULL AND (r.proposer_user_id = #{userId}",
            "    OR r.assignee_user_id = #{userId}",
            "    OR EXISTS (SELECT 1 FROM business_work_requirement_developer d",
            "      WHERE d.requirement_id = c.requirement_id AND d.user_id = #{userId} AND d.deleted = b'0'))))",
            "</script>"
    })
    Long countUnreadCommentsByRequirementIdsInternal(@Param("userId") Long userId,
                                                     @Param("requirementIds") Collection<Long> requirementIds);

    default List<WorkRequirementCommentCountDO> selectUnreadCommentCountsByRequirementIds(
            Long userId, Collection<Long> requirementIds) {
        if (userId == null || CollUtil.isEmpty(requirementIds)) {
            return java.util.Collections.emptyList();
        }
        return selectUnreadCommentCountsByRequirementIdsInternal(userId, requirementIds);
    }

    @Select({
            "<script>",
            "SELECT c.requirement_id AS requirementId, COUNT(1) AS commentCount",
            "FROM business_work_requirement_comment c",
            "JOIN business_work_requirement r ON r.id = c.requirement_id AND r.deleted = b'0'",
            "LEFT JOIN business_work_requirement_comment_read cr ON cr.comment_id = c.id",
            "  AND cr.user_id = #{userId} AND cr.deleted = b'0'",
            "WHERE c.deleted = b'0'",
            "AND c.from_user_id != #{userId}",
            "AND cr.id IS NULL",
            "AND c.requirement_id IN",
            "<foreach collection='requirementIds' item='requirementId' open='(' separator=',' close=')'>",
            "#{requirementId}",
            "</foreach>",
            "AND (c.target_user_id = #{userId}",
            "  OR (c.target_user_id IS NULL AND (r.proposer_user_id = #{userId}",
            "    OR r.assignee_user_id = #{userId}",
            "    OR EXISTS (SELECT 1 FROM business_work_requirement_developer d",
            "      WHERE d.requirement_id = c.requirement_id AND d.user_id = #{userId} AND d.deleted = b'0'))))",
            "GROUP BY c.requirement_id",
            "</script>"
    })
    List<WorkRequirementCommentCountDO> selectUnreadCommentCountsByRequirementIdsInternal(
            @Param("userId") Long userId,
            @Param("requirementIds") Collection<Long> requirementIds);

    @Select({
            "<script>",
            "SELECT DISTINCT c.id AS comment_id, c.tenant_id AS tenant_id",
            "FROM business_work_requirement_comment c",
            "JOIN business_work_requirement r ON r.id = c.requirement_id AND r.deleted = b'0'",
            "LEFT JOIN business_work_requirement_comment_read cr ON cr.comment_id = c.id",
            "  AND cr.user_id = #{userId} AND cr.deleted = b'0'",
            "WHERE c.deleted = b'0'",
            "AND c.from_user_id != #{userId}",
            "AND cr.id IS NULL",
            "AND (c.target_user_id = #{userId}",
            "  OR (c.target_user_id IS NULL AND (r.proposer_user_id = #{userId}",
            "    OR r.assignee_user_id = #{userId}",
            "    OR EXISTS (SELECT 1 FROM business_work_requirement_developer d",
            "      WHERE d.requirement_id = c.requirement_id AND d.user_id = #{userId} AND d.deleted = b'0'))))",
            "<if test='tenantIds != null and tenantIds.size > 0'>",
            "AND c.tenant_id IN",
            "<foreach collection='tenantIds' item='tenantId' open='(' separator=',' close=')'>",
            "#{tenantId}",
            "</foreach>",
            "</if>",
            "</script>"
    })
    List<WorkRequirementCommentReadDO> selectUnreadCommentReadRecords(@Param("userId") Long userId,
                                                                      @Param("tenantIds") Collection<Long> tenantIds);

    default Long countUnreadCommentsByRequirementFilter(Long userId,
                                                        Collection<Long> tenantIds,
                                                        WorkRequirementPageReqVO reqVO) {
        if (userId == null) {
            return 0L;
        }
        Long count = countUnreadCommentsByRequirementFilterInternal(userId, tenantIds, reqVO);
        return count == null ? 0L : count;
    }

    @Select({
            "<script>",
            "SELECT COUNT(1)",
            "FROM business_work_requirement_comment c",
            "JOIN business_work_requirement r ON r.id = c.requirement_id AND r.deleted = b'0'",
            "LEFT JOIN business_work_requirement_comment_read cr ON cr.comment_id = c.id",
            "  AND cr.user_id = #{userId} AND cr.deleted = b'0'",
            "WHERE c.deleted = b'0'",
            "AND c.tenant_id = r.tenant_id",
            "AND c.from_user_id != #{userId}",
            "AND cr.id IS NULL",
            "AND (c.target_user_id = #{userId}",
            "  OR (c.target_user_id IS NULL AND (r.proposer_user_id = #{userId}",
            "    OR r.assignee_user_id = #{userId}",
            "    OR EXISTS (SELECT 1 FROM business_work_requirement_developer d",
            "      WHERE d.requirement_id = c.requirement_id AND d.user_id = #{userId} AND d.deleted = b'0'))))",
            "<if test='tenantIds != null and tenantIds.size > 0'>",
            "AND r.tenant_id IN",
            "<foreach collection='tenantIds' item='tenantId' open='(' separator=',' close=')'>",
            "#{tenantId}",
            "</foreach>",
            "</if>",
            "<if test='reqVO.status != null'>AND r.status = #{reqVO.status}</if>",
            "<if test='reqVO.priority != null'>AND r.priority = #{reqVO.priority}</if>",
            "<if test='reqVO.approvalStatus != null'>AND r.approval_status = #{reqVO.approvalStatus}</if>",
            "<if test='reqVO.proposerUserId != null'>AND r.proposer_user_id = #{reqVO.proposerUserId}</if>",
            "<if test='reqVO.proposerName != null and reqVO.proposerName != \"\"'>",
            "AND r.proposer_name LIKE CONCAT('%', #{reqVO.proposerName}, '%')",
            "</if>",
            "<if test='reqVO.assigneeName != null and reqVO.assigneeName != \"\"'>",
            "AND r.assignee_name LIKE CONCAT('%', #{reqVO.assigneeName}, '%')",
            "</if>",
            "<if test='reqVO.assigneeUserId != null'>",
            "AND (r.assignee_user_id = #{reqVO.assigneeUserId}",
            "  OR EXISTS (SELECT 1 FROM business_work_requirement_developer fd",
            "    WHERE fd.requirement_id = r.id AND fd.user_id = #{reqVO.assigneeUserId} AND fd.deleted = b'0'))",
            "</if>",
            "<if test='reqVO.userId != null'>",
            "AND (r.proposer_user_id = #{reqVO.userId} OR r.assignee_user_id = #{reqVO.userId}",
            "  OR EXISTS (SELECT 1 FROM business_work_requirement_developer fd",
            "    WHERE fd.requirement_id = r.id AND fd.user_id = #{reqVO.userId} AND fd.deleted = b'0'))",
            "</if>",
            "<if test='reqVO.processInstanceIdList != null and reqVO.processInstanceIdList.size > 0'>",
            "AND r.process_instance_id IN",
            "<foreach collection='reqVO.processInstanceIdList' item='processInstanceId' open='(' separator=',' close=')'>",
            "#{processInstanceId}",
            "</foreach>",
            "</if>",
            "<if test='reqVO.trimmedKeyword != null and reqVO.trimmedKeyword != \"\"'>",
            "AND (r.title LIKE CONCAT('%', #{reqVO.trimmedKeyword}, '%')",
            "  OR r.description LIKE CONCAT('%', #{reqVO.trimmedKeyword}, '%')",
            "  OR r.proposer_name LIKE CONCAT('%', #{reqVO.trimmedKeyword}, '%')",
            "  OR r.target_dept LIKE CONCAT('%', #{reqVO.trimmedKeyword}, '%')",
            "  OR r.assignee_name LIKE CONCAT('%', #{reqVO.trimmedKeyword}, '%')",
            "  OR r.last_reject_reason LIKE CONCAT('%', #{reqVO.trimmedKeyword}, '%')",
            "  <if test='reqVO.keywordId != null'>OR r.id = #{reqVO.keywordId}</if>",
            ")",
            "</if>",
            "</script>"
    })
    Long countUnreadCommentsByRequirementFilterInternal(@Param("userId") Long userId,
                                                        @Param("tenantIds") Collection<Long> tenantIds,
                                                        @Param("reqVO") WorkRequirementPageReqVO reqVO);

    default void deleteByRequirementId(Long requirementId) {
        delete(new LambdaQueryWrapperX<WorkRequirementCommentDO>()
                .eq(WorkRequirementCommentDO::getRequirementId, requirementId));
    }

    default void deleteByRequirementIds(Collection<Long> requirementIds) {
        deleteBatch(WorkRequirementCommentDO::getRequirementId, requirementIds);
    }

}
