package com.kyx.service.hr.dal.mysql.todo;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.todo.vo.HrTodoPageReqVO;
import com.kyx.service.hr.dal.dataobject.todo.HrTodoTaskDO;
import lombok.Data;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * HR todo task Mapper.
 */
@Mapper
public interface HrTodoTaskMapper extends BaseMapperX<HrTodoTaskDO> {

    @Select({"<script>",
            "SELECT COALESCE(e.dept_id, 0) AS deptId,",
            "       COUNT(1) AS openTodoCount,",
            "       COUNT(CASE WHEN t.due_time IS NOT NULL AND t.due_time &lt; #{now} THEN 1 END) AS overdueTodoCount",
            "FROM hr_todo_task t",
            "LEFT JOIN (",
            "    SELECT ee.profile_id, ee.dept_id",
            "    FROM hr_employee_entry ee",
            "    INNER JOIN (",
            "        SELECT profile_id, MAX(id) AS id",
            "        FROM hr_employee_entry",
            "        WHERE deleted = 0",
            "          AND profile_id IS NOT NULL",
            "          AND work_status IN (#{probationStatus}, #{activeStatus})",
            "        GROUP BY profile_id",
            "    ) latest ON latest.id = ee.id",
            "    WHERE ee.deleted = 0",
            ") e ON e.profile_id = t.profile_id",
            "WHERE t.deleted = 0",
            "  AND t.status = #{status}",
            "GROUP BY COALESCE(e.dept_id, 0)",
            "</script>"})
    List<DeptTodoCount> selectDeptTodoCounts(@Param("status") String status,
                                             @Param("probationStatus") Integer probationStatus,
                                             @Param("activeStatus") Integer activeStatus,
                                             @Param("now") LocalDateTime now);

    default PageResult<HrTodoTaskDO> selectPage(HrTodoPageReqVO reqVO, boolean manage, Long loginUserId) {
        LambdaQueryWrapperX<HrTodoTaskDO> wrapper = new LambdaQueryWrapperX<HrTodoTaskDO>()
                .eqIfPresent(HrTodoTaskDO::getBusinessType, reqVO.getBusinessType())
                .eqIfPresent(HrTodoTaskDO::getStatus, reqVO.getStatus())
                .eqIfPresent(HrTodoTaskDO::getPriority, reqVO.getPriority())
                .eqIfPresent(HrTodoTaskDO::getProfileId, reqVO.getProfileId())
                .inIfPresent(HrTodoTaskDO::getProfileId, reqVO.getProfileIds())
                .geIfPresent(HrTodoTaskDO::getDueTime, reqVO.getDueTimeStart())
                .leIfPresent(HrTodoTaskDO::getDueTime, reqVO.getDueTimeEnd());
        if (StringUtils.hasText(reqVO.getKeyword())) {
            String keyword = reqVO.getKeyword().trim();
            wrapper.and(query -> query.like(HrTodoTaskDO::getTitle, keyword)
                    .or()
                    .like(HrTodoTaskDO::getContent, keyword));
        }
        if (!manage || Boolean.TRUE.equals(reqVO.getMine())) {
            wrapper.eq(HrTodoTaskDO::getAssigneeUserId, loginUserId == null ? -1L : loginUserId);
        }
        wrapper.orderByAsc(HrTodoTaskDO::getStatus)
                .orderByAsc(HrTodoTaskDO::getDueTime)
                .orderByDesc(HrTodoTaskDO::getId);
        return selectPage(reqVO, wrapper);
    }

    default HrTodoTaskDO selectByBusiness(String businessType, Long businessId, String taskType) {
        return selectOne(new LambdaQueryWrapperX<HrTodoTaskDO>()
                .eq(HrTodoTaskDO::getBusinessType, businessType)
                .eq(HrTodoTaskDO::getBusinessId, businessId)
                .eq(HrTodoTaskDO::getTaskType, taskType)
                .last("LIMIT 1"));
    }

    default List<HrTodoTaskDO> selectOpenGeneratedByBusinessType(String businessType, Integer limit) {
        LambdaQueryWrapperX<HrTodoTaskDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(HrTodoTaskDO::getGeneratedFlag, true)
                .eq(HrTodoTaskDO::getBusinessType, businessType)
                .eq(HrTodoTaskDO::getStatus, "OPEN")
                .orderByAsc(HrTodoTaskDO::getDueTime)
                .orderByDesc(HrTodoTaskDO::getId);
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        return selectList(wrapper);
    }

    default Long selectScopeCount(String status, Long assigneeUserId, LocalDateTime dueBefore,
                                  String priority, Boolean generatedFlag) {
        LambdaQueryWrapperX<HrTodoTaskDO> wrapper = new LambdaQueryWrapperX<HrTodoTaskDO>()
                .eqIfPresent(HrTodoTaskDO::getStatus, status)
                .eqIfPresent(HrTodoTaskDO::getAssigneeUserId, assigneeUserId)
                .ltIfPresent(HrTodoTaskDO::getDueTime, dueBefore)
                .eqIfPresent(HrTodoTaskDO::getPriority, priority)
                .eqIfPresent(HrTodoTaskDO::getGeneratedFlag, generatedFlag);
        return selectCount(wrapper);
    }

    default Long selectDueSoonCount(Long assigneeUserId, LocalDateTime now, LocalDateTime dueBefore) {
        return selectCount(new LambdaQueryWrapperX<HrTodoTaskDO>()
                .eq(HrTodoTaskDO::getStatus, "OPEN")
                .eqIfPresent(HrTodoTaskDO::getAssigneeUserId, assigneeUserId)
                .ge(HrTodoTaskDO::getDueTime, now)
                .le(HrTodoTaskDO::getDueTime, dueBefore));
    }

    @Data
    class DeptTodoCount {

        private Long deptId;

        private Long openTodoCount;

        private Long overdueTodoCount;
    }

}
