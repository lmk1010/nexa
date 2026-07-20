package com.kyx.service.business.controller.admin.scheduler;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.business.controller.admin.scheduler.vo.SchedulerLogPageReqVO;
import com.kyx.service.business.controller.admin.scheduler.vo.SchedulerLogRespVO;
import com.kyx.service.business.controller.admin.scheduler.vo.SchedulerStatsRespVO;
import com.kyx.service.business.dal.dataobject.scheduler.SchedulerLogDO;
import com.kyx.service.business.dal.dataobject.tenant.TenantDO;
import com.kyx.service.business.service.scheduler.SchedulerLogService;
import com.kyx.service.business.service.tenant.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.kyx.foundation.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 定时任务执行日志")
@RestController
@RequestMapping("/system/scheduler-log")
@Validated
@Slf4j
public class SchedulerLogController {

    @Resource
    private SchedulerLogService schedulerLogService;

    @Resource
    private TenantService tenantService;

    @GetMapping("/page")
    @Operation(summary = "获得定时任务执行日志分页")
    @PreAuthorize("@ss.hasPermission('system:scheduler:query')")
    public CommonResult<PageResult<SchedulerLogRespVO>> getSchedulerLogPage(@Valid SchedulerLogPageReqVO pageReqVO) {
        PageResult<SchedulerLogDO> pageResult = schedulerLogService.getSchedulerLogPage(pageReqVO);
        PageResult<SchedulerLogRespVO> result = BeanUtils.toBean(pageResult, SchedulerLogRespVO.class);
        fillTenantNames(result.getList());

        // 设置状态描述
        result.getList().forEach(log -> {
            if (log.getStatus() != null) {
                if (log.getStatus().equals(SchedulerLogDO.Status.SUCCESS.getValue())) {
                    log.setStatusDesc(SchedulerLogDO.Status.SUCCESS.getDesc());
                } else if (log.getStatus().equals(SchedulerLogDO.Status.FAILED.getValue())) {
                    log.setStatusDesc(SchedulerLogDO.Status.FAILED.getDesc());
                }
            }
        });
        
        return success(result);
    }

    @GetMapping("/get")
    @Operation(summary = "获得定时任务执行日志详情")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:scheduler:query')")
    public CommonResult<SchedulerLogRespVO> getSchedulerLog(@RequestParam("id") Long id) {
        SchedulerLogDO schedulerLog = schedulerLogService.getSchedulerLog(id);
        SchedulerLogRespVO result = BeanUtils.toBean(schedulerLog, SchedulerLogRespVO.class);
        if (result != null) {
            fillTenantNames(java.util.Collections.singletonList(result));
        }

        // 设置状态描述
        if (result.getStatus() != null) {
            if (result.getStatus().equals(SchedulerLogDO.Status.SUCCESS.getValue())) {
                result.setStatusDesc(SchedulerLogDO.Status.SUCCESS.getDesc());
            } else if (result.getStatus().equals(SchedulerLogDO.Status.FAILED.getValue())) {
                result.setStatusDesc(SchedulerLogDO.Status.FAILED.getDesc());
            }
        }
        
        return success(result);
    }

    @GetMapping("/recent")
    @Operation(summary = "获得最近的执行日志")
    @Parameter(name = "limit", description = "数量限制", example = "10")
    @PreAuthorize("@ss.hasPermission('system:scheduler:query')")
    public CommonResult<List<SchedulerLogRespVO>> getRecentLogs(@RequestParam(value = "limit", defaultValue = "10") int limit) {
        List<SchedulerLogDO> logs = schedulerLogService.getRecentLogs(limit);
        List<SchedulerLogRespVO> result = BeanUtils.toBean(logs, SchedulerLogRespVO.class);
        fillTenantNames(result);

        // 设置状态描述
        result.forEach(log -> {
            if (log.getStatus() != null) {
                if (log.getStatus().equals(SchedulerLogDO.Status.SUCCESS.getValue())) {
                    log.setStatusDesc(SchedulerLogDO.Status.SUCCESS.getDesc());
                } else if (log.getStatus().equals(SchedulerLogDO.Status.FAILED.getValue())) {
                    log.setStatusDesc(SchedulerLogDO.Status.FAILED.getDesc());
                }
            }
        });
        
        return success(result);
    }


    private void fillTenantNames(List<SchedulerLogRespVO> logList) {
        if (logList == null || logList.isEmpty()) {
            return;
        }
        Map<Long, String> tenantMap = logList.stream()
                .map(SchedulerLogRespVO::getTenantId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> {
                            TenantDO tenant = tenantService.getTenant(id);
                            return tenant != null ? tenant.getName() : "";
                        }
                ));
        logList.forEach(log -> {
            if (log.getTenantId() != null) {
                log.setTenantName(tenantMap.get(log.getTenantId()));
            }
        });
    }

    @GetMapping("/stats")
    @Operation(summary = "获得定时任务执行统计")
    @PreAuthorize("@ss.hasPermission('system:scheduler:query')")
    public CommonResult<SchedulerStatsRespVO> getSchedulerStats() {
        SchedulerStatsRespVO stats = schedulerLogService.getSchedulerStats();
        return success(stats);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除定时任务执行日志")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:scheduler:delete')")
    public CommonResult<Boolean> deleteSchedulerLog(@RequestParam("id") Long id) {
        schedulerLogService.deleteSchedulerLog(id);
        return success(true);
    }

    @PostMapping("/cleanup")
    @Operation(summary = "清理过期的执行日志")
    @Parameter(name = "daysToKeep", description = "保留天数", example = "30")
    @PreAuthorize("@ss.hasPermission('system:scheduler:delete')")
    public CommonResult<Integer> cleanupExpiredLogs(@RequestParam(value = "daysToKeep", defaultValue = "30") int daysToKeep) {
        int cleanedCount = schedulerLogService.cleanupExpiredLogs(daysToKeep);
        return success(cleanedCount);
    }

}