package com.kyx.service.biz.dal.mysql.executive;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Mapper
public interface ExecutiveCockpitMapper {

    @Select({
            "<script>",
            "SELECT COUNT(1) AS totalCount,",
            "COALESCE(SUM(CASE WHEN status NOT IN (5, 6, 7) THEN 1 ELSE 0 END), 0) AS openCount,",
            "COALESCE(SUM(CASE WHEN status = 5 THEN 1 ELSE 0 END), 0) AS completedCount,",
            "COALESCE(SUM(CASE WHEN status NOT IN (5, 6, 7)",
            "  AND expected_finish_date IS NOT NULL AND expected_finish_date &lt; CURDATE() THEN 1 ELSE 0 END), 0) AS overdueCount,",
            "COALESCE(SUM(CASE WHEN status NOT IN (5, 6, 7) AND priority &gt;= 3 THEN 1 ELSE 0 END), 0) AS highPriorityCount,",
            "COALESCE(SUM(CASE WHEN create_time &gt;= #{startTime} THEN 1 ELSE 0 END), 0) AS createdRangeCount,",
            "COALESCE(SUM(CASE WHEN status = 5 AND COALESCE(accepted_time, close_time) &gt;= #{startTime} THEN 1 ELSE 0 END), 0) AS doneRangeCount",
            "FROM business_work_requirement",
            "WHERE deleted = b'0'",
            "AND tenant_id IN",
            "<foreach collection='tenantIds' item='tenantId' open='(' separator=',' close=')'>#{tenantId}</foreach>",
            "</script>"
    })
    Map<String, Object> selectRequirementSummary(@Param("tenantIds") List<Long> tenantIds,
                                                 @Param("startTime") Date startTime);

    @Select({
            "<script>",
            "SELECT status AS status, COUNT(1) AS count",
            "FROM business_work_requirement",
            "WHERE deleted = b'0'",
            "AND tenant_id IN",
            "<foreach collection='tenantIds' item='tenantId' open='(' separator=',' close=')'>#{tenantId}</foreach>",
            "GROUP BY status",
            "ORDER BY status",
            "</script>"
    })
    List<Map<String, Object>> selectRequirementStatusCounts(@Param("tenantIds") List<Long> tenantIds);

    @Select({
            "<script>",
            "SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS trendDate, COUNT(1) AS count",
            "FROM business_work_requirement",
            "WHERE deleted = b'0'",
            "AND tenant_id IN",
            "<foreach collection='tenantIds' item='tenantId' open='(' separator=',' close=')'>#{tenantId}</foreach>",
            "AND create_time &gt;= #{startTime}",
            "AND create_time &lt; #{endTime}",
            "GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d')",
            "</script>"
    })
    List<Map<String, Object>> selectRequirementCreatedTrend(@Param("tenantIds") List<Long> tenantIds,
                                                            @Param("startTime") Date startTime,
                                                            @Param("endTime") Date endTime);

    @Select({
            "<script>",
            "SELECT DATE_FORMAT(COALESCE(accepted_time, close_time), '%Y-%m-%d') AS trendDate, COUNT(1) AS count",
            "FROM business_work_requirement",
            "WHERE deleted = b'0'",
            "AND tenant_id IN",
            "<foreach collection='tenantIds' item='tenantId' open='(' separator=',' close=')'>#{tenantId}</foreach>",
            "AND status = 5",
            "AND COALESCE(accepted_time, close_time) &gt;= #{startTime}",
            "AND COALESCE(accepted_time, close_time) &lt; #{endTime}",
            "GROUP BY DATE_FORMAT(COALESCE(accepted_time, close_time), '%Y-%m-%d')",
            "</script>"
    })
    List<Map<String, Object>> selectRequirementFinishedTrend(@Param("tenantIds") List<Long> tenantIds,
                                                             @Param("startTime") Date startTime,
                                                             @Param("endTime") Date endTime);

    @Select({
            "<script>",
            "SELECT assignee_user_id AS assigneeUserId,",
            "COALESCE(NULLIF(TRIM(assignee_name), ''), '未分派') AS assigneeName,",
            "COUNT(1) AS totalCount,",
            "COALESCE(SUM(CASE WHEN status NOT IN (5, 6, 7) THEN 1 ELSE 0 END), 0) AS openCount,",
            "COALESCE(SUM(CASE WHEN status NOT IN (5, 6, 7)",
            "  AND expected_finish_date IS NOT NULL AND expected_finish_date &lt; CURDATE() THEN 1 ELSE 0 END), 0) AS overdueCount",
            "FROM business_work_requirement",
            "WHERE deleted = b'0'",
            "AND tenant_id IN",
            "<foreach collection='tenantIds' item='tenantId' open='(' separator=',' close=')'>#{tenantId}</foreach>",
            "GROUP BY assignee_user_id, assignee_name",
            "ORDER BY openCount DESC, overdueCount DESC, totalCount DESC",
            "LIMIT #{limit}",
            "</script>"
    })
    List<Map<String, Object>> selectRequirementWorkload(@Param("tenantIds") List<Long> tenantIds,
                                                        @Param("limit") Integer limit);

    @Select({
            "<script>",
            "SELECT id AS id, title AS title, status AS status, priority AS priority,",
            "COALESCE(NULLIF(TRIM(assignee_name), ''), '未分派') AS assigneeName,",
            "expected_finish_date AS expectedFinishDate, update_time AS updateTime",
            "FROM business_work_requirement",
            "WHERE deleted = b'0'",
            "AND tenant_id IN",
            "<foreach collection='tenantIds' item='tenantId' open='(' separator=',' close=')'>#{tenantId}</foreach>",
            "AND status NOT IN (5, 6, 7)",
            "AND (priority &gt;= 3 OR (expected_finish_date IS NOT NULL AND expected_finish_date &lt; CURDATE()))",
            "ORDER BY CASE WHEN expected_finish_date IS NOT NULL AND expected_finish_date &lt; CURDATE() THEN 0 ELSE 1 END,",
            "priority DESC, expected_finish_date ASC, update_time DESC",
            "LIMIT #{limit}",
            "</script>"
    })
    List<Map<String, Object>> selectRequirementRisks(@Param("tenantIds") List<Long> tenantIds,
                                                     @Param("limit") Integer limit);

}
