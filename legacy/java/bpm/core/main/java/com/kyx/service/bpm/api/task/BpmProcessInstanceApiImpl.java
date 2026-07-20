package com.kyx.service.bpm.api.task;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.service.bpm.api.task.dto.BpmCopyNoticeResendRespDTO;
import com.kyx.service.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.kyx.service.bpm.controller.admin.task.vo.task.BpmTaskPageReqVO;
import com.kyx.service.bpm.service.task.BpmProcessInstanceCopyService;
import com.kyx.service.bpm.service.task.BpmProcessInstanceService;
import com.kyx.service.bpm.service.task.BpmTaskService;
import org.flowable.task.api.Task;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.kyx.foundation.common.pojo.CommonResult.success;

/**
 * Flowable 流程实例 Api 实现类
 *
 * @author MK
 * @author jason
 */
@RestController
@Validated
public class BpmProcessInstanceApiImpl implements BpmProcessInstanceApi {

    @Resource
    private BpmProcessInstanceService processInstanceService;
    @Resource
    private BpmProcessInstanceCopyService processInstanceCopyService;
    @Resource
    private BpmTaskService taskService;

    @Override
    public CommonResult<String> createProcessInstance(Long userId, @Valid BpmProcessInstanceCreateReqDTO reqDTO) {
        return success(processInstanceService.createProcessInstance(userId, reqDTO));
    }

    @Override
    public CommonResult<Boolean> hasRunningTask(Long userId, String processInstanceId) {
        return success(taskService.getTodoTask(userId, null, processInstanceId) != null);
    }

    @Override
    public CommonResult<List<String>> getTodoProcessInstanceIds(Long userId, Integer pageSize) {
        BpmTaskPageReqVO pageReqVO = new BpmTaskPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(pageSize == null ? 200 : pageSize);
        return success(taskService.getTaskTodoPage(userId, pageReqVO).getList().stream()
                .map(Task::getProcessInstanceId)
                .distinct()
                .collect(Collectors.toList()));
    }

    @Override
    public CommonResult<List<Long>> getTodoAssigneeUserIds(String processInstanceId) {
        if (processInstanceId == null || processInstanceId.trim().isEmpty()) {
            return success(Collections.emptyList());
        }
        return success(taskService.getTasksByProcessInstanceIds(Collections.singletonList(processInstanceId)).stream()
                .map(Task::getAssignee)
                .filter(assignee -> assignee != null && !assignee.trim().isEmpty())
                .map(Long::valueOf)
                .distinct()
                .collect(Collectors.toList()));
    }

    @Override
    public CommonResult<List<Long>> getCopyUserIds(String processInstanceId) {
        return success(processInstanceCopyService.getProcessInstanceCopyUserIds(processInstanceId));
    }

    @Override
    public CommonResult<BpmCopyNoticeResendRespDTO> resendRunningCopyNotices(Integer limit, Boolean dryRun) {
        BpmCopyNoticeResendRespDTO taskReport = taskService.resendRunningTaskDingTalkNotices(limit, dryRun);
        BpmCopyNoticeResendRespDTO copyReport = processInstanceCopyService.resendRunningCopyDingTalkNotices(limit, dryRun);
        return success(new BpmCopyNoticeResendRespDTO()
                .setScannedCount(safe(taskReport.getScannedCount()) + safe(copyReport.getScannedCount()))
                .setRunningCount(safe(taskReport.getRunningCount()) + safe(copyReport.getRunningCount()))
                .setReceiverCount(safe(taskReport.getReceiverCount()) + safe(copyReport.getReceiverCount()))
                .setTaskScannedCount(safe(taskReport.getTaskScannedCount()))
                .setTaskReceiverCount(safe(taskReport.getTaskReceiverCount()))
                .setCopyScannedCount(safe(copyReport.getCopyScannedCount()))
                .setCopyRunningCount(safe(copyReport.getCopyRunningCount()))
                .setCopyReceiverCount(safe(copyReport.getCopyReceiverCount())));
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }

}
