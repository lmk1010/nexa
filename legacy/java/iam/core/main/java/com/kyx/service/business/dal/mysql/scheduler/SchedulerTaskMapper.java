package com.kyx.service.business.dal.mysql.scheduler;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.business.controller.admin.scheduler.vo.task.SchedulerTaskPageReqVO;
import com.kyx.service.business.dal.dataobject.scheduler.SchedulerTaskDO;
import com.kyx.foundation.common.pojo.PageResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * 定时任务管理 Mapper
 *
 * @author MK
 */
@Mapper
public interface SchedulerTaskMapper extends BaseMapperX<SchedulerTaskDO> {

    default PageResult<SchedulerTaskDO> selectPage(SchedulerTaskPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SchedulerTaskDO>()
                .likeIfPresent(SchedulerTaskDO::getTaskName, reqVO.getTaskName())
                .eqIfPresent(SchedulerTaskDO::getTaskStatus, reqVO.getTaskStatus())
                .eqIfPresent(SchedulerTaskDO::getTaskType, reqVO.getTaskType())
                .betweenIfPresent(SchedulerTaskDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(SchedulerTaskDO::getId));
    }

}