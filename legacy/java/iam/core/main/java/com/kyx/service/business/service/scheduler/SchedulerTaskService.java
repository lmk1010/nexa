package com.kyx.service.business.service.scheduler;

import javax.validation.*;
import com.kyx.service.business.controller.admin.scheduler.vo.task.*;
import com.kyx.service.business.dal.dataobject.scheduler.SchedulerTaskDO;
import com.kyx.foundation.common.pojo.PageResult;

/**
 * 定时任务管理 Service 接口
 *
 * @author MK
 */
public interface SchedulerTaskService {

    /**
     * 创建定时任务管理
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createSchedulerTask(@Valid SchedulerTaskCreateReqVO createReqVO);

    /**
     * 更新定时任务管理
     *
     * @param updateReqVO 更新信息
     */
    void updateSchedulerTask(@Valid SchedulerTaskUpdateReqVO updateReqVO);

    /**
     * 删除定时任务管理
     *
     * @param id 编号
     */
    void deleteSchedulerTask(Long id);

    /**
     * 获得定时任务管理
     *
     * @param id 编号
     * @return 定时任务管理
     */
    SchedulerTaskDO getSchedulerTask(Long id);

    /**
     * 获得定时任务管理分页
     *
     * @param pageReqVO 分页查询
     * @return 定时任务管理分页
     */
    PageResult<SchedulerTaskDO> getSchedulerTaskPage(SchedulerTaskPageReqVO pageReqVO);

    /**
     * 异步执行定时任务
     *
     * @param id 任务编号
     */
    void executeTask(Long id);

    /**
     * 异步执行定时任务并返回执行日志ID
     *
     * @param id 任务编号
     * @return 执行日志ID
     */
    Long executeTaskAndReturnLogId(Long id);

    /**
     * 启用定时任务
     *
     * @param id 任务编号
     */
    void enableTask(Long id);

    /**
     * 禁用定时任务
     *
     * @param id 任务编号
     */
    void disableTask(Long id);

    /**
     * 根据任务名称获取定时任务
     *
     * @param taskName 任务名称
     * @return 定时任务，如果不存在则返回null
     */
    SchedulerTaskDO getSchedulerTaskByName(String taskName);

}