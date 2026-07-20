package com.kyx.service.business.controller.admin.scheduler;

import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

import javax.validation.*;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import static com.kyx.foundation.common.pojo.CommonResult.success;

import com.kyx.foundation.apilog.core.annotation.ApiAccessLog;
import static com.kyx.foundation.apilog.core.enums.OperateTypeEnum.*;

import com.kyx.service.business.controller.admin.scheduler.vo.task.*;
import com.kyx.service.business.controller.admin.scheduler.vo.SchedulerExecutionStatusRespVO;
import com.kyx.service.business.dal.dataobject.scheduler.SchedulerTaskDO;
import com.kyx.service.business.dal.dataobject.tenant.TenantDO;
import com.kyx.service.business.service.scheduler.SchedulerTaskService;
import com.kyx.service.business.service.scheduler.SchedulerLogService;
import com.kyx.service.business.service.tenant.TenantService;
import com.kyx.service.business.dal.dataobject.scheduler.SchedulerLogDO;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "管理后台 - 定时任务管理")
@RestController
@RequestMapping("/system/scheduler-task")
@Validated
public class SchedulerTaskController {

    @Resource
    private SchedulerTaskService schedulerTaskService;

    @Resource
    private SchedulerLogService schedulerLogService;

    @Resource
    private TenantService tenantService;

    @PostMapping("/create")
    @Operation(summary = "创建定时任务管理")
    @PreAuthorize("@ss.hasPermission('system:scheduler-task:create')")
    public CommonResult<Long> createSchedulerTask(@Valid @RequestBody SchedulerTaskCreateReqVO createReqVO) {
        return success(schedulerTaskService.createSchedulerTask(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新定时任务管理")
    @PreAuthorize("@ss.hasPermission('system:scheduler-task:update')")
    public CommonResult<Boolean> updateSchedulerTask(@Valid @RequestBody SchedulerTaskUpdateReqVO updateReqVO) {
        schedulerTaskService.updateSchedulerTask(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除定时任务管理")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('system:scheduler-task:delete')")
    public CommonResult<Boolean> deleteSchedulerTask(@RequestParam("id") Long id) {
        schedulerTaskService.deleteSchedulerTask(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得定时任务管理")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:scheduler-task:query')")
    public CommonResult<SchedulerTaskRespVO> getSchedulerTask(@RequestParam("id") Long id) {
        SchedulerTaskDO schedulerTask = schedulerTaskService.getSchedulerTask(id);
        SchedulerTaskRespVO respVO = BeanUtils.toBean(schedulerTask, SchedulerTaskRespVO.class);
        if (respVO != null) {
            fillTenantNames(java.util.Collections.singletonList(respVO));
        }
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得定时任务管理分页")
    @PreAuthorize("@ss.hasPermission('system:scheduler-task:query')")
    public CommonResult<PageResult<SchedulerTaskRespVO>> getSchedulerTaskPage(@Valid SchedulerTaskPageReqVO pageReqVO) {
        PageResult<SchedulerTaskDO> pageResult = schedulerTaskService.getSchedulerTaskPage(pageReqVO);
        PageResult<SchedulerTaskRespVO> result = BeanUtils.toBean(pageResult, SchedulerTaskRespVO.class);
        fillTenantNames(result.getList());
        return success(result);
    }


    private void fillTenantNames(List<SchedulerTaskRespVO> taskList) {
        if (taskList == null || taskList.isEmpty()) {
            return;
        }
        Map<Long, String> tenantMap = taskList.stream()
                .map(SchedulerTaskRespVO::getTenantId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> {
                            TenantDO tenant = tenantService.getTenant(id);
                            return tenant != null ? tenant.getName() : "";
                        }
                ));
        taskList.forEach(task -> {
            if (task.getTenantId() != null) {
                task.setTenantName(tenantMap.get(task.getTenantId()));
            }
        });
    }

    @PostMapping("/execute")
    @Operation(summary = "异步执行定时任务")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:scheduler-task:execute')")
    @ApiAccessLog(operateType = OTHER)
    public CommonResult<Map<String, Object>> executeTask(@RequestParam("id") Long id) {
        // 执行任务并返回执行信息，包含taskId和logId用于轮询执行状态
        Long logId = schedulerTaskService.executeTaskAndReturnLogId(id);
        Map<String, Object> result = new HashMap<>();
        result.put("taskId", id);
        result.put("logId", logId);
        result.put("status", "started");
        result.put("message", "任务已开始执行");
        return success(result);
    }

    @PutMapping("/enable")
    @Operation(summary = "启用定时任务")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:scheduler-task:update')")
    public CommonResult<Boolean> enableTask(@RequestParam("id") Long id) {
        schedulerTaskService.enableTask(id);
        return success(true);
    }

    @PutMapping("/disable")
    @Operation(summary = "禁用定时任务")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:scheduler-task:update')")
    public CommonResult<Boolean> disableTask(@RequestParam("id") Long id) {
        schedulerTaskService.disableTask(id);
        return success(true);
    }

    @GetMapping("/execution-status")
    @Operation(summary = "获取任务执行状态")
    @Parameter(name = "id", description = "任务编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:scheduler-task:query')")
    public CommonResult<SchedulerLogDO> getTaskExecutionStatus(@RequestParam("id") Long id) {
        SchedulerLogDO latestLog = schedulerLogService.getLatestLogByTaskId(id);
        return success(latestLog);
    }

    @GetMapping("/execution-status-by-log")
    @Operation(summary = "根据日志ID获取任务执行状态")
    @Parameter(name = "logId", description = "执行日志ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:scheduler-task:query')")
    public CommonResult<SchedulerExecutionStatusRespVO> getTaskExecutionStatusByLogId(@RequestParam("logId") Long logId) {
        SchedulerLogDO log = schedulerLogService.getSchedulerLog(logId);
        if (log == null) {
            return success(null);
        }
        
        SchedulerExecutionStatusRespVO.ExecutionStats stats = SchedulerExecutionStatusRespVO.ExecutionStats.builder()
            .inserted(log.getInsertCount() != null ? log.getInsertCount() : 0)
            .updated(log.getUpdateCount() != null ? log.getUpdateCount() : 0)
            .failed(log.getFailureCount() != null ? log.getFailureCount() : 0)
            .skipped(log.getSkipCount() != null ? log.getSkipCount() : 0)
            .build();
        stats.setTotal(stats.getInserted() + stats.getUpdated() + stats.getFailed() + stats.getSkipped());
        
        SchedulerExecutionStatusRespVO statusVO = SchedulerExecutionStatusRespVO.builder()
            .logId(log.getId())
            .taskId(log.getTaskId())
            .jobName(log.getJobName())
            .status(log.getStatus())
            .progress(log.getProgress() != null ? log.getProgress() : 0)
            .progressMessage(log.getProgressMessage())
            .startTime(log.getStartTime())
            .endTime(log.getEndTime())
            .duration(log.getDuration())
            .resultMessage(log.getResultMessage())
            .errorMessage(log.getErrorMessage())
            .isManual(log.getIsManual())
            .insertCount(log.getInsertCount())
            .updateCount(log.getUpdateCount())
            .failureCount(log.getFailureCount())
            .skipCount(log.getSkipCount())
            .stats(stats)
            .build();
            
        return success(statusVO);
    }

}