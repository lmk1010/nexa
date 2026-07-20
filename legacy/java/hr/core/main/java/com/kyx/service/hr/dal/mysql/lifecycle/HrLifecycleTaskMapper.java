package com.kyx.service.hr.dal.mysql.lifecycle;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.dal.dataobject.lifecycle.HrLifecycleTaskDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface HrLifecycleTaskMapper extends BaseMapperX<HrLifecycleTaskDO> {

    default List<HrLifecycleTaskDO> selectListByEventId(Long eventId) {
        return selectList(new LambdaQueryWrapperX<HrLifecycleTaskDO>()
                .eq(HrLifecycleTaskDO::getEventId, eventId)
                .orderByAsc(HrLifecycleTaskDO::getSortOrder)
                .orderByAsc(HrLifecycleTaskDO::getId));
    }

    default Long selectOpenRequiredCountByEventId(Long eventId) {
        return selectCount(new LambdaQueryWrapperX<HrLifecycleTaskDO>()
                .eq(HrLifecycleTaskDO::getEventId, eventId)
                .eq(HrLifecycleTaskDO::getRequiredFlag, true)
                .ne(HrLifecycleTaskDO::getTaskStatus, "DONE"));
    }

    default Long selectCompletedCountByEventId(Long eventId) {
        return selectCount(new LambdaQueryWrapperX<HrLifecycleTaskDO>()
                .eq(HrLifecycleTaskDO::getEventId, eventId)
                .eq(HrLifecycleTaskDO::getTaskStatus, "DONE"));
    }

    default Long selectPendingCount() {
        return selectCount(new LambdaQueryWrapperX<HrLifecycleTaskDO>()
                .ne(HrLifecycleTaskDO::getTaskStatus, "DONE"));
    }

    default List<HrLifecycleTaskDO> selectPendingListByProfileOrAssignee(Long profileId, Long assigneeUserId, Integer limit) {
        LambdaQueryWrapperX<HrLifecycleTaskDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.ne(HrLifecycleTaskDO::getTaskStatus, "DONE");
        if (profileId != null && assigneeUserId != null) {
            wrapper.and(query -> query.eq(HrLifecycleTaskDO::getProfileId, profileId)
                    .or()
                    .eq(HrLifecycleTaskDO::getAssigneeUserId, assigneeUserId));
        } else if (profileId != null) {
            wrapper.eq(HrLifecycleTaskDO::getProfileId, profileId);
        } else if (assigneeUserId != null) {
            wrapper.eq(HrLifecycleTaskDO::getAssigneeUserId, assigneeUserId);
        } else {
            wrapper.eq(HrLifecycleTaskDO::getId, -1L);
        }
        wrapper.orderByAsc(HrLifecycleTaskDO::getDueDate)
                .orderByAsc(HrLifecycleTaskDO::getId);
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        return selectList(wrapper);
    }

    default Long selectOverdueCount(LocalDate today) {
        return selectCount(new LambdaQueryWrapperX<HrLifecycleTaskDO>()
                .ne(HrLifecycleTaskDO::getTaskStatus, "DONE")
                .lt(HrLifecycleTaskDO::getDueDate, today));
    }

    default List<HrLifecycleTaskDO> selectOverdueList(LocalDate today, Integer limit) {
        return selectList(new LambdaQueryWrapperX<HrLifecycleTaskDO>()
                .ne(HrLifecycleTaskDO::getTaskStatus, "DONE")
                .lt(HrLifecycleTaskDO::getDueDate, today)
                .orderByAsc(HrLifecycleTaskDO::getDueDate)
                .orderByDesc(HrLifecycleTaskDO::getId)
                .last("LIMIT " + limit));
    }

}
