package com.kyx.service.business.dal.mysql.scheduler;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.business.controller.admin.scheduler.vo.SchedulerLogPageReqVO;
import com.kyx.service.business.dal.dataobject.scheduler.SchedulerLogDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务执行日志 Mapper
 *
 * @author MK
 */
@Mapper
public interface SchedulerLogMapper extends BaseMapperX<SchedulerLogDO> {

    default PageResult<SchedulerLogDO> selectPage(SchedulerLogPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SchedulerLogDO>()
                .likeIfPresent(SchedulerLogDO::getJobName, reqVO.getJobName())
                .eqIfPresent(SchedulerLogDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(SchedulerLogDO::getStartTime, reqVO.getStartTime())
                .orderByDesc(SchedulerLogDO::getStartTime));
    }

    default List<SchedulerLogDO> selectRecentLogs(int limit) {
        return selectList(new LambdaQueryWrapperX<SchedulerLogDO>()
                .orderByDesc(SchedulerLogDO::getStartTime)
                .last("LIMIT " + limit));
    }

    default int countByStatus(Integer status) {
        return Math.toIntExact(selectCount(SchedulerLogDO::getStatus, status));
    }

    default int deleteByCreateTimeBefore(LocalDateTime beforeTime) {
        return delete(new LambdaQueryWrapperX<SchedulerLogDO>()
                .lt(SchedulerLogDO::getCreateTime, beforeTime));
    }

    default SchedulerLogDO selectLatestByTaskId(Long taskId) {
        return selectOne(new LambdaQueryWrapperX<SchedulerLogDO>()
                .eq(SchedulerLogDO::getTaskId, taskId)
                .orderByDesc(SchedulerLogDO::getStartTime)
                .last("LIMIT 1"));
    }

}