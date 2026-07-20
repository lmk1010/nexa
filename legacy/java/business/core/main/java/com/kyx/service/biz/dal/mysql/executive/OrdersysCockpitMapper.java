package com.kyx.service.biz.dal.mysql.executive;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@DS("ordersys")
@Mapper
public interface OrdersysCockpitMapper {

    @Select({
            "SELECT COUNT(1) AS totalCount,",
            "COALESCE(SUM(CASE WHEN status NOT IN ('1', '-1') THEN 1 ELSE 0 END), 0) AS openCount,",
            "COALESCE(SUM(CASE WHEN status = '1' THEN 1 ELSE 0 END), 0) AS completedCount,",
            "COALESCE(SUM(CASE WHEN status NOT IN ('1', '-1')",
            "  AND task_time IS NOT NULL AND task_time < CURDATE() THEN 1 ELSE 0 END), 0) AS overdueCount,",
            "COALESCE(SUM(CASE WHEN create_time >= DATE_SUB(NOW(), INTERVAL 7 DAY) THEN 1 ELSE 0 END), 0) AS created7dCount,",
            "COALESCE(SUM(CASE WHEN level IN ('3', '4', '紧急') AND status NOT IN ('1', '-1') THEN 1 ELSE 0 END), 0) AS highPriorityCount",
            "FROM sys_task",
            "WHERE COALESCE(deleted, 0) = 0"
    })
    Map<String, Object> selectTaskSummary();

    @Select({
            "SELECT status AS status, COUNT(1) AS count",
            "FROM sys_task",
            "WHERE COALESCE(deleted, 0) = 0",
            "GROUP BY status",
            "ORDER BY status"
    })
    List<Map<String, Object>> selectTaskStatusCounts();

    @Select({
            "SELECT id AS id, title AS title, status AS status,",
            "operator AS operatorName, task_name AS assigneeName,",
            "task_time AS taskTime, update_time AS updateTime",
            "FROM sys_task",
            "WHERE COALESCE(deleted, 0) = 0",
            "ORDER BY update_time DESC, id DESC",
            "LIMIT #{limit}"
    })
    List<Map<String, Object>> selectRecentTasks(@Param("limit") Integer limit);

}
