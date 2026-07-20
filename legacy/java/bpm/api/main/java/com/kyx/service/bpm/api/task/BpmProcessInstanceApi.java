package com.kyx.service.bpm.api.task;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.service.bpm.api.task.dto.BpmCopyNoticeResendRespDTO;
import com.kyx.service.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.kyx.service.bpm.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;
import java.util.List;

@FeignClient(name = ApiConstants.NAME) // TODO ：fallbackFactory =
@Tag(name = "RPC 服务 - 流程实例")
public interface BpmProcessInstanceApi {

    String PREFIX = ApiConstants.PREFIX + "/process-instance";

    @PostMapping(PREFIX + "/create")
    @Operation(summary = "创建流程实例（提供给内部），返回实例编号")
    @Parameter(name = "userId", description = "用户编号", required = true, example = "1")
    CommonResult<String> createProcessInstance(@RequestParam("userId") Long userId,
                                               @Valid @RequestBody BpmProcessInstanceCreateReqDTO reqDTO);

    @GetMapping(PREFIX + "/has-running-task")
    @Operation(summary = "校验用户是否有指定流程的待办任务")
    CommonResult<Boolean> hasRunningTask(@RequestParam("userId") Long userId,
                                         @RequestParam("processInstanceId") String processInstanceId);

    @GetMapping(PREFIX + "/todo-process-instance-ids")
    @Operation(summary = "获取用户待办流程实例编号")
    CommonResult<List<String>> getTodoProcessInstanceIds(@RequestParam("userId") Long userId,
                                                         @RequestParam("pageSize") Integer pageSize);

    @GetMapping(PREFIX + "/todo-assignee-user-ids")
    @Operation(summary = "获取流程实例当前待办处理人")
    CommonResult<List<Long>> getTodoAssigneeUserIds(@RequestParam("processInstanceId") String processInstanceId);

    @GetMapping(PREFIX + "/copy-user-ids")
    @Operation(summary = "Get process instance copy user ids")
    CommonResult<List<Long>> getCopyUserIds(@RequestParam("processInstanceId") String processInstanceId);

    @PostMapping(PREFIX + "/copy-notice/resend-running")
    @Operation(summary = "补发运行中流程的 BPM 知会钉钉通知")
    CommonResult<BpmCopyNoticeResendRespDTO> resendRunningCopyNotices(
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "dryRun", required = false) Boolean dryRun);

}
