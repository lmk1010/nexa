package com.kyx.service.bpm.service.task;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.kyx.foundation.common.util.http.HttpUtils;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.number.NumberUtils;
import com.kyx.foundation.web.config.WebProperties;
import com.kyx.service.bpm.api.task.dto.BpmCopyNoticeResendRespDTO;
import com.kyx.service.bpm.controller.admin.task.vo.instance.BpmProcessInstanceCopyPageReqVO;
import com.kyx.service.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO;
import com.kyx.service.bpm.dal.dataobject.task.BpmProcessInstanceCopyDO;
import com.kyx.service.bpm.dal.mysql.task.BpmProcessInstanceCopyMapper;
import com.kyx.service.bpm.enums.ErrorCodeConstants;
import com.kyx.service.bpm.framework.flowable.core.util.FlowableUtils;
import com.kyx.service.bpm.service.definition.BpmProcessDefinitionService;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import com.kyx.service.hr.api.dingtalk.DingTalkBpmNoticeApi;
import com.kyx.service.hr.api.dingtalk.dto.DingTalkBpmNoticeReqDTO;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.foundation.common.util.collection.CollectionUtils.convertList;
import static com.kyx.foundation.common.util.collection.CollectionUtils.convertSet;

/**
 * 流程抄送 Service 实现类
 *
 * @author kyle
 */
@Service
@Validated
@Slf4j
public class BpmProcessInstanceCopyServiceImpl implements BpmProcessInstanceCopyService {

    @Resource
    private BpmProcessInstanceCopyMapper processInstanceCopyMapper;

    @Resource
    @Lazy // 延迟加载，避免循环依赖
    private BpmTaskService taskService;

    @Resource
    @Lazy // 延迟加载，避免循环依赖
    private BpmProcessInstanceService processInstanceService;
    @Resource
    @Lazy // 延迟加载，避免循环依赖
    private BpmProcessDefinitionService processDefinitionService;
    @Resource
    private DingTalkBpmNoticeApi dingTalkBpmNoticeApi;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private WebProperties webProperties;

    @Override
    public void createProcessInstanceCopy(Collection<Long> userIds, String reason, String taskId) {
        Task task = taskService.getTask(taskId);
        if (ObjectUtil.isNull(task)) {
            throw exception(ErrorCodeConstants.TASK_NOT_EXISTS);
        }
        // 执行抄送
        createProcessInstanceCopy(userIds, reason,
                task.getProcessInstanceId(), task.getTaskDefinitionKey(), task.getId(), task.getName());
    }

    @Override
    public void createProcessInstanceCopy(Collection<Long> userIds, String reason, String processInstanceId,
                                          String activityId, String activityName, String taskId) {
        // 1.1 校验流程实例存在
        ProcessInstance processInstance = processInstanceService.getProcessInstance(processInstanceId);
        if (processInstance == null) {
            throw exception(ErrorCodeConstants.PROCESS_INSTANCE_NOT_EXISTS);
        }
        // 1.2 校验流程定义存在
        ProcessDefinition processDefinition = processDefinitionService.getProcessDefinition(
                processInstance.getProcessDefinitionId());
        if (processDefinition == null) {
            throw exception(ErrorCodeConstants.PROCESS_DEFINITION_NOT_EXISTS);
        }

        // 2. 创建抄送流程
        List<BpmProcessInstanceCopyDO> copyList = convertList(userIds, userId -> new BpmProcessInstanceCopyDO()
                .setUserId(userId).setReason(reason).setStartUserId(Long.valueOf(processInstance.getStartUserId()))
                .setProcessInstanceId(processInstanceId).setProcessInstanceName(processInstance.getName())
                .setCategory(processDefinition.getCategory()).setTaskId(taskId)
                .setActivityId(activityId).setActivityName(activityName)
                .setProcessDefinitionId(processInstance.getProcessDefinitionId()));
        processInstanceCopyMapper.insertBatch(copyList);
        sendCopyDingTalkNotice(userIds, reason, processInstance, activityId, activityName, taskId);
    }

    @Override
    public PageResult<BpmProcessInstanceCopyDO> getProcessInstanceCopyPage(Long userId,
                                                                           BpmProcessInstanceCopyPageReqVO pageReqVO) {
        PageResult<BpmProcessInstanceCopyDO> pageResult = processInstanceCopyMapper.selectPage(userId, pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return pageResult;
        }
        List<BpmProcessDefinitionInfoDO> visibleDefinitions = processDefinitionService.getVisibleProcessDefinitionList();
        if (CollUtil.isEmpty(visibleDefinitions)) {
            return PageResult.empty();
        }
        Set<String> visibleProcessDefinitionIds = convertSet(visibleDefinitions,
                BpmProcessDefinitionInfoDO::getProcessDefinitionId);
        List<BpmProcessInstanceCopyDO> copyList = new ArrayList<>(pageResult.getList());
        copyList.removeIf(copy -> !visibleProcessDefinitionIds.contains(copy.getProcessDefinitionId()));
        return new PageResult<>(copyList, (long) copyList.size());
    }

    @Override
    public void deleteProcessInstanceCopy(String processInstanceId) {
        processInstanceCopyMapper.deleteByProcessInstanceId(processInstanceId);
    }

    @Override
    public List<Long> getProcessInstanceCopyUserIds(String processInstanceId) {
        if (processInstanceId == null || processInstanceId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return processInstanceCopyMapper.selectListByProcessInstanceId(processInstanceId).stream()
                .map(BpmProcessInstanceCopyDO::getUserId)
                .filter(ObjectUtil::isNotNull)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public BpmCopyNoticeResendRespDTO resendRunningCopyDingTalkNotices(Integer limit, Boolean dryRun) {
        List<BpmProcessInstanceCopyDO> copyList = processInstanceCopyMapper.selectRecentList(limit);
        int runningCount = 0;
        int receiverCount = 0;
        for (BpmProcessInstanceCopyDO copy : copyList) {
            if (copy == null || copy.getUserId() == null || StrUtil.isBlank(copy.getProcessInstanceId())) {
                continue;
            }
            ProcessInstance processInstance = processInstanceService.getProcessInstance(copy.getProcessInstanceId());
            if (processInstance == null) {
                continue;
            }
            runningCount++;
            receiverCount++;
            if (!Boolean.TRUE.equals(dryRun)) {
                sendCopyDingTalkNotice(Collections.singletonList(copy.getUserId()), copy.getReason(), processInstance,
                        copy.getActivityId(), copy.getActivityName(), copy.getTaskId());
            }
        }
        return new BpmCopyNoticeResendRespDTO()
                .setScannedCount(copyList.size())
                .setRunningCount(runningCount)
                .setReceiverCount(receiverCount)
                .setCopyScannedCount(copyList.size())
                .setCopyRunningCount(runningCount)
                .setCopyReceiverCount(receiverCount);
    }

    private void sendCopyDingTalkNotice(Collection<Long> userIds, String reason, ProcessInstance processInstance,
                                        String activityId, String activityName, String taskId) {
        List<Long> receiverUserIds = userIds == null ? Collections.emptyList() : userIds.stream()
                .filter(ObjectUtil::isNotNull).distinct().collect(Collectors.toList());
        if (CollUtil.isEmpty(receiverUserIds)) {
            return;
        }
        Long startUserId = NumberUtils.parseLong(processInstance.getStartUserId());
        try {
            FlowableUtils.execute(processInstance.getTenantId(), () -> dingTalkBpmNoticeApi.sendCopy(new DingTalkBpmNoticeReqDTO()
                    .setReceiverUserIds(receiverUserIds)
                    .setProcessInstanceId(processInstance.getProcessInstanceId())
                    .setProcessInstanceName(processInstance.getName())
                    .setStartUserId(startUserId)
                    .setStartUserNickname(resolveStartUserNickname(startUserId))
                    .setTaskId(taskId)
                    .setActivityName(activityName)
                    .setReason(reason)
                    .setDetailUrl(getProcessInstanceDetailUrl(processInstance.getProcessInstanceId(), activityId))
                    .setDedupBizId("bpm-copy:" + processInstance.getProcessInstanceId() + ":"
                            + StrUtil.blankToDefault(activityName, StrUtil.blankToDefault(taskId, "-"))))
                    .checkError());
        } catch (Exception ex) {
            log.warn("[sendCopyDingTalkNotice][发送 BPM 知会钉钉通知失败][processInstanceId={}, activityName={}]",
                    processInstance.getProcessInstanceId(), activityName, ex);
        }
    }

    private String resolveStartUserNickname(Long startUserId) {
        if (startUserId == null) {
            return "System";
        }
        try {
            AdminUserRespDTO user = adminUserApi.getUser(startUserId).getCheckedData();
            return user == null || StrUtil.isBlank(user.getNickname()) ? "System" : user.getNickname();
        } catch (Exception ex) {
            log.warn("[resolveStartUserNickname][查询发起人失败][startUserId={}]", startUserId, ex);
            return "System";
        }
    }

    private String getProcessInstanceDetailUrl(String processInstanceId, String activityId) {
        StringBuilder url = new StringBuilder(webProperties.getAdminUi().getUrl())
                .append("/bpm/process-instance/detail?id=")
                .append(encodeQueryValue(processInstanceId));
        if (StrUtil.isNotBlank(activityId)) {
            url.append("&activityId=").append(encodeQueryValue(activityId));
        }
        return url.toString();
    }

    private String encodeQueryValue(String value) {
        return HttpUtils.encodeUtf8(value == null ? "" : value);
    }

}
