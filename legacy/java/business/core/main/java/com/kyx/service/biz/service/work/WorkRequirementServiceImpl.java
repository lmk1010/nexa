package com.kyx.service.biz.service.work;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.servlet.ServletUtils;
import com.kyx.foundation.security.core.service.SecurityFrameworkService;
import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.foundation.web.config.WebProperties;
import com.kyx.service.bpm.api.task.BpmProcessInstanceApi;
import com.kyx.service.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.kyx.service.bpm.enums.task.BpmTaskStatusEnum;
import com.kyx.service.business.enums.permission.RoleCodeEnum;
import com.kyx.service.biz.controller.admin.todo.vo.TodoSaveReqVO;
import com.kyx.service.biz.controller.admin.work.vo.WorkRequirementActionReqVO;
import com.kyx.service.biz.controller.admin.work.vo.WorkRequirementAssignReqVO;
import com.kyx.service.biz.controller.admin.work.vo.WorkRequirementCommentCreateReqVO;
import com.kyx.service.biz.controller.admin.work.vo.WorkRequirementOverviewRespVO;
import com.kyx.service.biz.controller.admin.work.vo.WorkRequirementPageReqVO;
import com.kyx.service.biz.controller.admin.work.vo.WorkRequirementRateSaveReqVO;
import com.kyx.service.biz.controller.admin.work.vo.WorkRequirementSaveReqVO;
import com.kyx.service.biz.dal.dataobject.work.WorkRequirementCommentCountDO;
import com.kyx.service.biz.dal.dataobject.work.WorkRequirementCommentDO;
import com.kyx.service.biz.dal.dataobject.work.WorkRequirementCommentReadDO;
import com.kyx.service.biz.dal.dataobject.work.WorkRequirementDeveloperDO;
import com.kyx.service.biz.dal.dataobject.work.WorkRequirementDO;
import com.kyx.service.biz.dal.dataobject.work.WorkRequirementLogDO;
import com.kyx.service.biz.dal.dataobject.work.WorkRequirementRateDO;
import com.kyx.service.biz.dal.mysql.work.WorkRequirementCommentMapper;
import com.kyx.service.biz.dal.mysql.work.WorkRequirementCommentReadMapper;
import com.kyx.service.biz.dal.mysql.work.WorkRequirementDeveloperMapper;
import com.kyx.service.biz.dal.mysql.work.WorkRequirementLogMapper;
import com.kyx.service.biz.dal.mysql.work.WorkRequirementMapper;
import com.kyx.service.biz.dal.mysql.work.WorkRequirementRateMapper;
import com.kyx.service.biz.enums.WorkRequirementApprovalStatusEnum;
import com.kyx.service.biz.enums.WorkRequirementStatusEnum;
import com.kyx.service.biz.service.todo.TodoService;
import com.kyx.service.business.api.dept.DeptApi;
import com.kyx.service.business.api.dept.dto.DeptRespDTO;
import com.kyx.service.business.api.notify.NotifyMessageSendApi;
import com.kyx.service.business.api.notify.dto.NotifySendSingleToUserReqDTO;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import com.kyx.service.hr.api.dingtalk.DingTalkRequirementNoticeApi;
import com.kyx.service.hr.api.dingtalk.dto.DingTalkRequirementNoticeReqDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.service.biz.enums.ErrorCodeConstants.WORK_REQUIREMENT_ASSIGNEE_REQUIRED;
import static com.kyx.service.biz.enums.ErrorCodeConstants.WORK_REQUIREMENT_ACCEPT_ATTACHMENT_REQUIRED;
import static com.kyx.service.biz.enums.ErrorCodeConstants.WORK_REQUIREMENT_APPROVAL_ALREADY_APPROVED;
import static com.kyx.service.biz.enums.ErrorCodeConstants.WORK_REQUIREMENT_APPROVAL_PENDING;
import static com.kyx.service.biz.enums.ErrorCodeConstants.WORK_REQUIREMENT_COMMENT_CONTENT_REQUIRED;
import static com.kyx.service.biz.enums.ErrorCodeConstants.WORK_REQUIREMENT_FORBIDDEN;
import static com.kyx.service.biz.enums.ErrorCodeConstants.WORK_REQUIREMENT_HAS_OPEN_CHILDREN;
import static com.kyx.service.biz.enums.ErrorCodeConstants.WORK_REQUIREMENT_NOT_EXISTS;
import static com.kyx.service.biz.enums.ErrorCodeConstants.WORK_REQUIREMENT_REJECT_REASON_REQUIRED;
import static com.kyx.service.biz.enums.ErrorCodeConstants.WORK_REQUIREMENT_STATUS_INVALID;
import static com.kyx.service.biz.enums.ErrorCodeConstants.WORK_REQUIREMENT_TREE_DEPTH_EXCEEDED;

@Service
@Validated
@Slf4j
public class WorkRequirementServiceImpl implements WorkRequirementService {

    private static final String ACTION_CREATE = "CREATE";
    private static final String ACTION_CREATE_CHILD = "CREATE_CHILD";
    private static final String ACTION_UPDATE = "UPDATE";
    private static final String ACTION_ASSIGN = "ASSIGN";
    private static final String ACTION_DEV_REJECT = "DEV_REJECT";
    private static final String ACTION_START_DEV = "START_DEV";
    private static final String ACTION_SUBMIT_TEST = "SUBMIT_TEST";
    private static final String ACTION_TEST_PASS = "TEST_PASS";
    private static final String ACTION_TEST_REJECT = "TEST_REJECT";
    private static final String ACTION_ACCEPT_PASS = "ACCEPT_PASS";
    private static final String ACTION_ACCEPT_REJECT = "ACCEPT_REJECT";
    private static final String ACTION_CANCEL = "CANCEL";
    private static final String ACTION_SUSPEND = "SUSPEND";
    private static final String ACTION_REOPEN = "REOPEN";
    private static final String ACTION_BPM_SUBMIT = "BPM_SUBMIT";
    private static final String ACTION_BPM_APPROVE = "BPM_APPROVE";
    private static final String ACTION_BPM_REJECT = "BPM_REJECT";
    private static final String ACTION_BPM_CANCEL = "BPM_CANCEL";
    private static final String ACTION_COMMENT = "COMMENT";
    private static final String ACTION_QUESTION = "QUESTION";

    private static final String COMMENT_TYPE_COMMENT = "COMMENT";
    private static final String COMMENT_TYPE_QUESTION = "QUESTION";
    private static final String DEVELOPER_ROLE_MAIN = "MAIN";
    private static final String DEVELOPER_ROLE_COLLABORATOR = "COLLABORATOR";
    private static final String NOTIFY_TEMPLATE_ASSIGN = "work_requirement_assign";
    private static final String NOTIFY_TEMPLATE_STATUS = "work_requirement_status_change";
    private static final String NOTIFY_TEMPLATE_COMMENT = "work_requirement_comment";
    private static final String PERMISSION_ASSIGN_REQUIREMENT = "work:requirement:assign";
    private static final String PERMISSION_UPDATE_REQUIREMENT = "work:requirement:update";
    private static final String ROLE_SYSTEM_ADMIN = "system_admin";
    private static final int TODO_TITLE_MAX_LENGTH = 255;
    private static final int TODO_DESCRIPTION_MAX_LENGTH = 1000;
    private static final String TODO_TEXT_TRUNCATED_SUFFIX = "...";
    private static final String TODO_BUSINESS_TYPE_REQUIREMENT = "WORK_REQUIREMENT";
    private static final String TODO_TASK_TYPE_APPROVAL = "APPROVAL";
    private static final String TODO_TASK_TYPE_DEVELOP = "DEVELOP";
    private static final int BPM_CALLBACK_LOOKUP_MAX_RETRY = 5;
    private static final long BPM_CALLBACK_LOOKUP_RETRY_INTERVAL_MS = 300L;
    private static final int REQUIREMENT_PAGE_SIZE_MAX = 100;
    private static final int OVERVIEW_TREND_DAYS = 14;
    private static final String OVERVIEW_DATE_PATTERN = "yyyy-MM-dd";
    public static final String PROCESS_KEY = "work-requirement-approval";

    @Resource
    private WorkRequirementMapper workRequirementMapper;
    @Resource
    private WorkRequirementLogMapper workRequirementLogMapper;
    @Resource
    private WorkRequirementCommentMapper workRequirementCommentMapper;
    @Resource
    private WorkRequirementCommentReadMapper workRequirementCommentReadMapper;
    @Resource
    private WorkRequirementDeveloperMapper workRequirementDeveloperMapper;
    @Resource
    private WorkRequirementRateMapper workRequirementRateMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private TodoService todoService;
    @Resource
    private NotifyMessageSendApi notifyMessageSendApi;
    @Resource
    private WebProperties webProperties;
    @Resource
    private SecurityFrameworkService securityFrameworkService;
    @Resource
    private BpmProcessInstanceApi processInstanceApi;
    @Resource
    private WorkRequirementTenantScopeService workRequirementTenantScopeService;
    @Resource
    private DingTalkRequirementNoticeApi dingTalkRequirementNoticeApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRequirement(WorkRequirementSaveReqVO createReqVO, Long userId) {
        validateCreateRequirementPermission(userId);
        WorkRequirementDO parentRequirement = validateParentForCreate(createReqVO.getParentId(), userId);
        return executeInTenant(parentRequirement == null ? null : parentRequirement.getTenantId(),
                () -> doCreateRequirement(createReqVO, userId, parentRequirement));
    }

    private Long doCreateRequirement(WorkRequirementSaveReqVO createReqVO, Long userId, WorkRequirementDO parentRequirement) {
        OperatorSnapshot operator = getOperatorSnapshot(userId);
        String proposerDept = StrUtil.blankToDefault(normalizeBlank(StrUtil.trim(createReqVO.getProposerDept())),
                StrUtil.blankToDefault(operator.getDeptName(), ""));
        String proposerName = resolveOperatorName(operator.getUserName(), createReqVO.getProposerName(), userId);
        WorkRequirementDO requirement = new WorkRequirementDO();
        applyRequirementHierarchyBeforeInsert(requirement, parentRequirement);
        requirement.setTitle(createReqVO.getTitle());
        requirement.setDescription(createReqVO.getDescription());
        requirement.setPriority(createReqVO.getPriority());
        requirement.setStatus(WorkRequirementStatusEnum.PENDING_ASSIGN.getStatus());
        requirement.setProposerDept(proposerDept);
        requirement.setTargetDept(normalizeBlank(StrUtil.trim(createReqVO.getTargetDept())));
        requirement.setProposerName(proposerName);
        requirement.setProposerUserId(userId);
        requirement.setExpectedFinishDate(createReqVO.getExpectedFinishDate());
        requirement.setEstimatedUserCount(createReqVO.getEstimatedUserCount());
        requirement.setIntegral(createReqVO.getIntegral());
        requirement.setUseType(normalizeBlank(StrUtil.trim(createReqVO.getUseType())));
        requirement.setSourceIp(ServletUtils.getClientIP());
        requirement.setAttachmentUrls(normalizeAttachmentUrls(createReqVO.getAttachmentUrls()));
        Long assigneeTenantId = null;
        String assigneeName = null;
        if (createReqVO.getAssigneeUserId() == null && parentRequirement != null && parentRequirement.getAssigneeUserId() != null) {
            createReqVO.setAssigneeUserId(parentRequirement.getAssigneeUserId());
            createReqVO.setAssigneeName(parentRequirement.getAssigneeName());
        }
        if (createReqVO.getAssigneeUserId() != null) {
            AdminUserRespDTO assignee = adminUserApi.getUser(createReqVO.getAssigneeUserId()).getCheckedData();
            assigneeTenantId = workRequirementTenantScopeService.resolveAssigneeTenantId(
                    createReqVO.getAssigneeUserId(), createReqVO.getAssigneeTenantId());
            assigneeName = getUserDisplayName(assignee, createReqVO.getAssigneeUserId(), createReqVO.getAssigneeName());
            requirement.setAssigneeUserId(createReqVO.getAssigneeUserId());
            requirement.setAssigneeName(assigneeName);
            requirement.setStatus(WorkRequirementStatusEnum.PENDING_DEVELOP.getStatus());
        }
        workRequirementMapper.insert(requirement);
        applyRequirementHierarchyAfterInsert(requirement, parentRequirement);
        syncRequirementDevelopers(requirement, createReqVO.getAssigneeUserId(), assigneeName, assigneeTenantId,
                createReqVO.getCollaboratorUserIds());

        // 先落库为审批中，避免流程秒级结束回调先到后，被下面的写操作覆盖为审批中。
        WorkRequirementDO approvalRunningObj = new WorkRequirementDO();
        approvalRunningObj.setId(requirement.getId());
        approvalRunningObj.setApprovalStatus(WorkRequirementApprovalStatusEnum.RUNNING.getStatus());
        workRequirementMapper.updateById(approvalRunningObj);

        String processInstanceId = startRequirementApprovalProcess(requirement, userId, createReqVO.getStartUserSelectAssignees());

        WorkRequirementDO processInstanceUpdateObj = new WorkRequirementDO();
        processInstanceUpdateObj.setId(requirement.getId());
        processInstanceUpdateObj.setProcessInstanceId(processInstanceId);
        workRequirementMapper.updateById(processInstanceUpdateObj);

        requirement.setProcessInstanceId(processInstanceId);
        requirement.setApprovalStatus(WorkRequirementApprovalStatusEnum.RUNNING.getStatus());
        syncApprovalTodos(requirement, processInstanceId);
        String createRemark = normalizeBlank(StrUtil.trim(createReqVO.getRemark()));
        createLog(requirement.getId(), ACTION_CREATE, null, WorkRequirementStatusEnum.PENDING_ASSIGN.getStatus(), createRemark, userId, proposerName);
        if (parentRequirement != null) {
            createLog(parentRequirement.getId(), ACTION_CREATE_CHILD, parentRequirement.getStatus(), parentRequirement.getStatus(),
                    StrUtil.format("新增子需求 #{}：{}", requirement.getId(), StrUtil.blankToDefault(requirement.getTitle(), "-")),
                    userId, proposerName);
        }
        if (requirement.getAssigneeUserId() != null) {
            createLog(requirement.getId(), ACTION_ASSIGN, WorkRequirementStatusEnum.PENDING_ASSIGN.getStatus(),
                    requirement.getStatus(), requirement.getAssigneeName(), userId, proposerName);
        }
        return requirement.getId();
    }

    @Override
    public void updateRequirement(WorkRequirementSaveReqVO updateReqVO, Long userId) {
        WorkRequirementDO requirement = validateRequirement(updateReqVO.getId());
        validateParticipantOrAdmin(requirement, userId);
        if (Arrays.asList(WorkRequirementStatusEnum.DONE.getStatus(), WorkRequirementStatusEnum.CANCELED.getStatus())
                .contains(requirement.getStatus()) && !isRequirementAdmin(userId)) {
            throw exception(WORK_REQUIREMENT_STATUS_INVALID);
        }
        if (Objects.equals(requirement.getApprovalStatus(), WorkRequirementApprovalStatusEnum.RUNNING.getStatus())) {
            throw exception(WORK_REQUIREMENT_APPROVAL_PENDING);
        }
        runInRequirementTenant(requirement, () -> {
            WorkRequirementDO currentRequirement = requireCurrentRequirement(updateReqVO.getId());
            WorkRequirementDO updateObj = new WorkRequirementDO();
            updateObj.setId(updateReqVO.getId());
            updateObj.setTitle(updateReqVO.getTitle());
            updateObj.setDescription(updateReqVO.getDescription());
            updateObj.setPriority(updateReqVO.getPriority());
            updateObj.setExpectedFinishDate(updateReqVO.getExpectedFinishDate());
            updateObj.setIntegral(updateReqVO.getIntegral());
            updateObj.setUseType(normalizeBlank(StrUtil.trim(updateReqVO.getUseType())));
            updateObj.setAttachmentUrls(normalizeAttachmentUrls(updateReqVO.getAttachmentUrls()));
            String proposerDept = normalizeBlank(StrUtil.trim(updateReqVO.getProposerDept()));
            if (proposerDept != null) {
                updateObj.setProposerDept(proposerDept);
            }
            String targetDept = normalizeBlank(StrUtil.trim(updateReqVO.getTargetDept()));
            if (targetDept != null) {
                updateObj.setTargetDept(targetDept);
            }

            Long assigneeTenantId = null;
            String assigneeName = null;
            boolean hasAssigneeInRequest = updateReqVO.getAssigneeUserId() != null;
            if (hasAssigneeInRequest) {
                AdminUserRespDTO assignee = adminUserApi.getUser(updateReqVO.getAssigneeUserId()).getCheckedData();
                assigneeTenantId = workRequirementTenantScopeService.resolveAssigneeTenantId(
                        updateReqVO.getAssigneeUserId(), updateReqVO.getAssigneeTenantId());
                assigneeName = getUserDisplayName(assignee, updateReqVO.getAssigneeUserId(), updateReqVO.getAssigneeName());
                updateObj.setAssigneeUserId(updateReqVO.getAssigneeUserId());
                updateObj.setAssigneeName(assigneeName);
            }

            workRequirementMapper.updateById(updateObj);
            workRequirementMapper.updateEstimatedUserCount(updateReqVO.getId(), updateReqVO.getEstimatedUserCount());

            currentRequirement.setTitle(updateReqVO.getTitle());
            currentRequirement.setDescription(updateReqVO.getDescription());
            currentRequirement.setPriority(updateReqVO.getPriority());
            currentRequirement.setExpectedFinishDate(updateReqVO.getExpectedFinishDate());
            currentRequirement.setIntegral(updateReqVO.getIntegral());
            currentRequirement.setUseType(updateObj.getUseType());
            currentRequirement.setAttachmentUrls(updateObj.getAttachmentUrls());
            if (proposerDept != null) {
                currentRequirement.setProposerDept(proposerDept);
            }
            if (targetDept != null) {
                currentRequirement.setTargetDept(targetDept);
            }

            if (hasAssigneeInRequest) {
                closeDeveloperTodos(currentRequirement);
                currentRequirement.setAssigneeUserId(updateReqVO.getAssigneeUserId());
                currentRequirement.setAssigneeName(assigneeName);
                syncRequirementDevelopers(currentRequirement, updateReqVO.getAssigneeUserId(), assigneeName, assigneeTenantId,
                        updateReqVO.getCollaboratorUserIds());
            } else {
                fillDeveloperMembers(Collections.singletonList(currentRequirement));
            }
            if (Objects.equals(currentRequirement.getStatus(), WorkRequirementStatusEnum.PENDING_DEVELOP.getStatus())
                    || Objects.equals(currentRequirement.getStatus(), WorkRequirementStatusEnum.DEVELOPING.getStatus())) {
                createDeveloperTodos(currentRequirement);
            }
            createLog(requirement.getId(), ACTION_UPDATE, requirement.getStatus(), requirement.getStatus(),
                    normalizeBlank(StrUtil.trim(updateReqVO.getRemark())),
                    userId, getOperatorSnapshot(userId).getUserName());
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRequirement(Long id, Long userId) {
        WorkRequirementDO requirement = validateRequirement(id);
        validateDeleteRequirementPermission(requirement, userId);
        runInRequirementTenant(requirement, () -> {
            WorkRequirementDO currentRequirement = requireCurrentRequirement(id);
            validateDeleteRequirementPermission(currentRequirement, userId);
            List<WorkRequirementDO> deleteRequirements = workRequirementMapper.selectSubtreeByPath(
                    currentRequirement.getId(), currentRequirement.getPath());
            if (CollUtil.isEmpty(deleteRequirements)) {
                throw exception(WORK_REQUIREMENT_NOT_EXISTS);
            }
            List<Long> requirementIds = deleteRequirements.stream()
                    .map(WorkRequirementDO::getId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            List<WorkRequirementCommentDO> comments = workRequirementCommentMapper.selectListByRequirementIds(requirementIds);
            List<Long> commentIds = comments.stream().map(WorkRequirementCommentDO::getId).collect(Collectors.toList());
            if (CollUtil.isNotEmpty(commentIds)) {
                workRequirementCommentReadMapper.deleteByCommentIds(commentIds);
            }
            workRequirementDeveloperMapper.deleteByRequirementIds(requirementIds);
            workRequirementCommentMapper.deleteByRequirementIds(requirementIds);
            workRequirementRateMapper.deleteByRequirementIds(requirementIds);
            workRequirementLogMapper.deleteByRequirementIds(requirementIds);
            deleteRequirements.forEach(item -> {
                closeApprovalTodos(item);
                closeDeveloperTodos(item);
            });
            workRequirementMapper.deleteByRequirementIds(requirementIds);
            if (currentRequirement.getParentId() != null) {
                workRequirementMapper.decrementChildCount(currentRequirement.getParentId());
            }
            log.info("[deleteRequirement][删除需求成功][requirementId={}, operatorId={}, cascadeCount={}, title={}]",
                    id, userId, requirementIds.size(), currentRequirement.getTitle());
        });
    }

    @Override
    public WorkRequirementDO getRequirement(Long id, Long userId) {
        WorkRequirementDO requirement = validateRequirement(id);
        validateViewParticipantOrAdmin(requirement, userId);
        return executeInRequirementTenant(requirement, () -> {
            WorkRequirementDO currentRequirement = requireCurrentRequirement(id);
            fillProposerDept(Collections.singletonList(currentRequirement));
            fillDeveloperMembers(Collections.singletonList(currentRequirement));
            fillParentTitles(Collections.singletonList(currentRequirement));
            return currentRequirement;
        });
    }

    @Override
    public List<WorkRequirementDO> getRequirementChildren(Long parentId, WorkRequirementPageReqVO reqVO, Long userId) {
        WorkRequirementDO parentRequirement = validateRequirement(parentId);
        validateViewParticipantOrAdmin(parentRequirement, userId);
        WorkRequirementPageReqVO filterReqVO = reqVO == null ? new WorkRequirementPageReqVO() : reqVO;
        filterReqVO.setUnreadCommentUserId(Boolean.TRUE.equals(filterReqVO.getCommentUnreadOnly()) ? userId : null);
        return executeInRequirementTenant(parentRequirement, () -> {
            List<WorkRequirementDO> children = workRequirementMapper.selectChildrenByParentId(parentId, filterReqVO);
            fillProposerDept(children);
            fillDeveloperMembers(children);
            fillCommentStats(children, userId);
            fillParentTitles(children);
            return children;
        });
    }

    @Override
    public PageResult<WorkRequirementDO> getRequirementPage(WorkRequirementPageReqVO pageReqVO, Long userId) {
        validatePageQueryPermission(pageReqVO, userId);
        limitRequirementPageSize(pageReqVO);
        List<Long> tenantIds = resolvePageTenantIds(pageReqVO, userId);
        if (Boolean.TRUE.equals(pageReqVO.getCommentUnreadOnly()) && userId == null) {
            return PageResult.empty();
        }
        pageReqVO.setUnreadCommentUserId(Boolean.TRUE.equals(pageReqVO.getCommentUnreadOnly()) ? userId : null);
        PageResult<WorkRequirementDO> pageResult = TenantUtils.executeIgnore(
                () -> workRequirementMapper.selectPage(pageReqVO, tenantIds));
        fillDeveloperMembers(pageResult.getList());
        fillCommentStats(pageResult.getList(), userId);
        fillParentTitles(pageResult.getList());
        return pageResult;
    }

    @Override
    public WorkRequirementOverviewRespVO getRequirementOverview(WorkRequirementPageReqVO pageReqVO, Long userId) {
        validatePageQueryPermission(pageReqVO, userId);
        List<Long> tenantIds = resolvePageTenantIds(pageReqVO, userId);
        if (Boolean.TRUE.equals(pageReqVO.getCommentUnreadOnly()) && userId == null) {
            return createEmptyRequirementOverview();
        }
        pageReqVO.setUnreadCommentUserId(Boolean.TRUE.equals(pageReqVO.getCommentUnreadOnly()) ? userId : null);
        return buildRequirementOverview(pageReqVO, tenantIds, userId);
    }

    @Override
    public List<String> getTodoApprovalProcessInstanceIds(Long userId) {
        if (userId == null) {
            throw exception(WORK_REQUIREMENT_FORBIDDEN);
        }
        try {
            return processInstanceApi.getTodoProcessInstanceIds(userId, 200).getCheckedData();
        } catch (Exception ex) {
            log.warn("[getTodoApprovalProcessInstanceIds][查询需求待审批流程失败][userId={}]", userId, ex);
            return Collections.emptyList();
        }
    }

    @Override
    public List<WorkRequirementLogDO> getRequirementLogs(Long requirementId, Long userId) {
        WorkRequirementDO requirement = validateRequirement(requirementId);
        validateViewParticipantOrAdmin(requirement, userId);
        return executeInRequirementTenant(requirement,
                () -> workRequirementLogMapper.selectListByRequirementId(requirementId));
    }

    @Override
    public List<WorkRequirementCommentDO> getRequirementComments(Long requirementId, Long userId) {
        WorkRequirementDO requirement = validateRequirement(requirementId);
        validateViewParticipantOrAdmin(requirement, userId);
        return executeInRequirementTenant(requirement, () -> {
            List<WorkRequirementCommentDO> comments = workRequirementCommentMapper.selectListByRequirementId(requirementId);
            applyCommentReadStatus(comments, userId, requirement);
            return comments;
        });
    }

    @Override
    public Long createRequirementComment(WorkRequirementCommentCreateReqVO reqVO, Long userId) {
        WorkRequirementDO requirement = validateRequirement(reqVO.getRequirementId());
        validateParticipantOrAdmin(requirement, userId);
        return executeInRequirementTenant(requirement, () -> {
            WorkRequirementDO currentRequirement = requireCurrentRequirement(requirement.getId());
            OperatorSnapshot operator = getOperatorSnapshot(userId);
            String commentType = normalizeCommentType(reqVO.getCommentType());
            Long targetUserId = resolveCommentTargetUserId(currentRequirement, reqVO.getTargetUserId(), userId);
            List<String> attachmentUrls = normalizeAttachmentUrls(reqVO.getAttachmentUrls());
            String content = normalizeCommentContent(reqVO.getContent(), attachmentUrls);
            WorkRequirementCommentDO comment = new WorkRequirementCommentDO();
            comment.setRequirementId(currentRequirement.getId());
            comment.setCommentType(commentType);
            comment.setContent(content);
            comment.setAttachmentUrls(attachmentUrls);
            comment.setFromUserId(userId);
            comment.setFromUserName(operator.getUserName());
            comment.setTargetUserId(targetUserId);
            comment.setTargetUserName(getRequirementUserDisplayName(currentRequirement, targetUserId));
            comment.setIp(ServletUtils.getClientIP());
            workRequirementCommentMapper.insert(comment);
            touchRequirementUpdateTime(currentRequirement.getId());
            createLog(currentRequirement.getId(), COMMENT_TYPE_QUESTION.equals(commentType) ? ACTION_QUESTION : ACTION_COMMENT,
                    currentRequirement.getStatus(), currentRequirement.getStatus(), content, userId, operator.getUserName());
            sendCommentNotify(currentRequirement, comment);
            return comment.getId();
        });
    }

    @Override
    public void readAllRequirementComments(Long requirementId, Long userId) {
        WorkRequirementDO requirement = validateRequirement(requirementId);
        validateViewParticipantOrAdmin(requirement, userId);
        runInRequirementTenant(requirement, () -> {
            List<WorkRequirementCommentDO> comments = workRequirementCommentMapper.selectListByRequirementId(requirementId);
            createCommentReadRecords(comments, userId, requirement);
        });
    }

    @Override
    public void readAllMyRequirementComments(Long userId) {
        if (userId == null) {
            return;
        }
        List<Long> tenantIds = canQueryAllRequirements(userId)
                ? workRequirementTenantScopeService.getQueryAllTenantIds()
                : workRequirementTenantScopeService.getParticipantSearchTenantIds();
        List<WorkRequirementCommentReadDO> unreadReadRecords = TenantUtils.executeIgnore(
                () -> workRequirementCommentMapper.selectUnreadCommentReadRecords(userId, tenantIds));
        createCommentReadRecordsByPrototypes(unreadReadRecords, userId);
    }

    @Override
    public List<WorkRequirementRateDO> getRequirementRates(Long requirementId, Long userId) {
        WorkRequirementDO requirement = validateRequirement(requirementId);
        validateViewParticipantOrAdmin(requirement, userId);
        return executeInRequirementTenant(requirement,
                () -> workRequirementRateMapper.selectListByRequirementId(requirementId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveRequirementRate(WorkRequirementRateSaveReqVO reqVO, Long userId) {
        WorkRequirementDO requirement = validateRequirement(reqVO.getRequirementId());
        validateParticipantOrRequirementAdmin(requirement, userId);
        return executeInRequirementTenant(requirement, () -> {
            WorkRequirementDO currentRequirement = requireCurrentRequirement(reqVO.getRequirementId());
            Long targetUserId = resolveRateTargetUserId(currentRequirement, reqVO.getTargetUserId(), userId);
            WorkRequirementRateDO rate = workRequirementRateMapper.selectOneByUnique(currentRequirement.getId(), userId, targetUserId);
            if (rate == null) {
                rate = new WorkRequirementRateDO();
                rate.setRequirementId(currentRequirement.getId());
                rate.setRaterUserId(userId);
                rate.setRaterName(getOperatorSnapshot(userId).getUserName());
                rate.setTargetUserId(targetUserId);
                rate.setTargetUserName(getRequirementUserDisplayName(currentRequirement, targetUserId));
                rate.setScore(reqVO.getScore());
                rate.setContent(normalizeBlank(StrUtil.trim(reqVO.getContent())));
                workRequirementRateMapper.insert(rate);
                return rate.getId();
            }
            WorkRequirementRateDO updateObj = new WorkRequirementRateDO();
            updateObj.setId(rate.getId());
            updateObj.setRaterName(getOperatorSnapshot(userId).getUserName());
            updateObj.setTargetUserName(getRequirementUserDisplayName(currentRequirement, targetUserId));
            updateObj.setScore(reqVO.getScore());
            updateObj.setContent(normalizeBlank(StrUtil.trim(reqVO.getContent())));
            workRequirementRateMapper.updateById(updateObj);
            return rate.getId();
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRequirement(WorkRequirementAssignReqVO reqVO, Long userId) {
        WorkRequirementDO requirement = validateRequirement(reqVO.getId());
        validateApprovalPassed(requirement);
        validateStatus(requirement, WorkRequirementStatusEnum.PENDING_ASSIGN.getStatus(),
                WorkRequirementStatusEnum.PENDING_DEVELOP.getStatus(),
                WorkRequirementStatusEnum.DEVELOPING.getStatus());
        if (reqVO.getAssigneeUserId() == null) {
            throw exception(WORK_REQUIREMENT_ASSIGNEE_REQUIRED);
        }
        AdminUserRespDTO assignee = adminUserApi.getUser(reqVO.getAssigneeUserId()).getCheckedData();
        Long assigneeTenantId = workRequirementTenantScopeService.resolveAssigneeTenantId(
                reqVO.getAssigneeUserId(), reqVO.getAssigneeTenantId());
        String assigneeName = getUserDisplayName(assignee, reqVO.getAssigneeUserId(), reqVO.getAssigneeName());
        runInRequirementTenant(requirement, () -> {
            WorkRequirementDO updateObj = new WorkRequirementDO();
            updateObj.setId(requirement.getId());
            updateObj.setAssigneeUserId(reqVO.getAssigneeUserId());
            updateObj.setAssigneeName(assigneeName);
            updateObj.setStatus(WorkRequirementStatusEnum.PENDING_DEVELOP.getStatus());
            updateObj.setPreviousStatus(null);
            updateObj.setLastRejectReason(null);
            workRequirementMapper.updateById(updateObj);
            closeDeveloperTodos(requirement);
            syncRequirementDevelopers(requirement, reqVO.getAssigneeUserId(), assigneeName, assigneeTenantId,
                    reqVO.getCollaboratorUserIds());
            createLog(requirement.getId(), ACTION_ASSIGN, requirement.getStatus(),
                    WorkRequirementStatusEnum.PENDING_DEVELOP.getStatus(),
                    buildDeveloperNamesText(requirement.getDeveloperMembers()),
                    userId, getOperatorSnapshot(userId).getUserName());
            requirement.setAssigneeUserId(updateObj.getAssigneeUserId());
            requirement.setAssigneeName(updateObj.getAssigneeName());
            requirement.setStatus(updateObj.getStatus());
            createDeveloperTodos(requirement);
            sendAssignNotifyToDevelopers(requirement);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferAssignRequirement(WorkRequirementAssignReqVO reqVO, Long userId) {
        WorkRequirementDO requirement = validateRequirement(reqVO.getId());
        validateApprovalPassed(requirement);
        validateStatus(requirement, WorkRequirementStatusEnum.PENDING_DEVELOP.getStatus(),
                WorkRequirementStatusEnum.DEVELOPING.getStatus());
        validateDeveloperOrRequirementAdmin(requirement, userId);
        if (reqVO.getAssigneeUserId() == null) {
            throw exception(WORK_REQUIREMENT_ASSIGNEE_REQUIRED);
        }
        AdminUserRespDTO assignee = adminUserApi.getUser(reqVO.getAssigneeUserId()).getCheckedData();
        Long assigneeTenantId = workRequirementTenantScopeService.resolveAssigneeTenantId(
                reqVO.getAssigneeUserId(), reqVO.getAssigneeTenantId());
        String assigneeName = getUserDisplayName(assignee, reqVO.getAssigneeUserId(), reqVO.getAssigneeName());
        runInRequirementTenant(requirement, () -> {
            WorkRequirementDO updateObj = new WorkRequirementDO();
            updateObj.setId(requirement.getId());
            updateObj.setAssigneeUserId(reqVO.getAssigneeUserId());
            updateObj.setAssigneeName(assigneeName);
            updateObj.setStatus(WorkRequirementStatusEnum.PENDING_DEVELOP.getStatus());
            updateObj.setPreviousStatus(null);
            updateObj.setLastRejectReason(null);
            workRequirementMapper.updateById(updateObj);
            closeDeveloperTodos(requirement);
            syncRequirementDevelopers(requirement, reqVO.getAssigneeUserId(), assigneeName, assigneeTenantId,
                    reqVO.getCollaboratorUserIds());
            createLog(requirement.getId(), ACTION_ASSIGN, requirement.getStatus(),
                    WorkRequirementStatusEnum.PENDING_DEVELOP.getStatus(),
                    buildDeveloperNamesText(requirement.getDeveloperMembers()),
                    userId, getOperatorSnapshot(userId).getUserName());
            requirement.setAssigneeUserId(updateObj.getAssigneeUserId());
            requirement.setAssigneeName(updateObj.getAssigneeName());
            requirement.setStatus(updateObj.getStatus());
            createDeveloperTodos(requirement);
            sendAssignNotifyToDevelopers(requirement);
        });
    }

    @Override
    public void devReject(WorkRequirementActionReqVO reqVO, Long userId) {
        WorkRequirementDO requirement = validateRequirement(reqVO.getId());
        validateApprovalPassed(requirement);
        validateStatus(requirement, WorkRequirementStatusEnum.PENDING_DEVELOP.getStatus(),
                WorkRequirementStatusEnum.DEVELOPING.getStatus());
        validateDeveloperOrRequirementAdmin(requirement, userId);
        String rejectReason = StrUtil.trim(reqVO.getRemark());
        if (StrUtil.isBlank(rejectReason)) {
            throw exception(WORK_REQUIREMENT_REJECT_REASON_REQUIRED);
        }
        runInRequirementTenant(requirement, () -> {
            WorkRequirementDO updateObj = new WorkRequirementDO();
            updateObj.setId(requirement.getId());
            updateObj.setStatus(WorkRequirementStatusEnum.PENDING_ASSIGN.getStatus());
            updateObj.setAssigneeUserId(null);
            updateObj.setAssigneeName(null);
            updateObj.setPreviousStatus(null);
            updateObj.setLastRejectReason(rejectReason);
            workRequirementMapper.updateById(updateObj);
            workRequirementDeveloperMapper.deletePhysicallyByRequirementId(requirement.getId());
            closeDeveloperTodos(requirement);
            createLog(requirement.getId(), ACTION_DEV_REJECT, requirement.getStatus(),
                    WorkRequirementStatusEnum.PENDING_ASSIGN.getStatus(), rejectReason,
                    userId, getOperatorSnapshot(userId).getUserName());
            sendStatusNotify(requirement, requirement.getProposerUserId(), ACTION_DEV_REJECT,
                    WorkRequirementStatusEnum.PENDING_ASSIGN.getStatus(), rejectReason, userId);
        });
    }

    @Override
    public void startDev(Long id, Long userId) {
        WorkRequirementDO requirement = validateRequirement(id);
        validateApprovalPassed(requirement);
        validateStatus(requirement, WorkRequirementStatusEnum.PENDING_DEVELOP.getStatus());
        validateDeveloperOrRequirementAdmin(requirement, userId);
        runInRequirementTenant(requirement, () -> {
            updateStatus(requirement, WorkRequirementStatusEnum.DEVELOPING.getStatus(), ACTION_START_DEV, null, userId, false);
            closeDeveloperTodos(requirement);
            sendStatusNotify(requirement, requirement.getProposerUserId(), ACTION_START_DEV,
                    WorkRequirementStatusEnum.DEVELOPING.getStatus(), null, userId);
        });
    }

    @Override
    public void submitTest(Long id, Long userId) {
        WorkRequirementDO requirement = validateRequirement(id);
        validateApprovalPassed(requirement);
        validateStatus(requirement, WorkRequirementStatusEnum.DEVELOPING.getStatus());
        validateDeveloperOrRequirementAdmin(requirement, userId);
        runInRequirementTenant(requirement, () -> {
            WorkRequirementDO updateObj = new WorkRequirementDO();
            updateObj.setId(requirement.getId());
            updateObj.setStatus(WorkRequirementStatusEnum.PENDING_ACCEPT.getStatus());
            updateObj.setSubmitTestTime(new Date());
            workRequirementMapper.updateById(updateObj);
            closeDeveloperTodos(requirement);
            createLog(requirement.getId(), ACTION_SUBMIT_TEST, requirement.getStatus(),
                    WorkRequirementStatusEnum.PENDING_ACCEPT.getStatus(), null, userId, getOperatorSnapshot(userId).getUserName());
            sendStatusNotify(requirement, requirement.getProposerUserId(), ACTION_SUBMIT_TEST,
                    WorkRequirementStatusEnum.PENDING_ACCEPT.getStatus(), null, userId);
        });
    }

    @Override
    public void testPass(Long id, Long userId) {
        WorkRequirementDO requirement = validateRequirement(id);
        validateApprovalPassed(requirement);
        validateStatus(requirement, WorkRequirementStatusEnum.TESTING.getStatus());
        validateProposerOrRequirementAdmin(requirement, userId);
        Date now = new Date();
        runInRequirementTenant(requirement, () -> {
            WorkRequirementDO updateObj = new WorkRequirementDO();
            updateObj.setId(requirement.getId());
            updateObj.setStatus(WorkRequirementStatusEnum.PENDING_ACCEPT.getStatus());
            updateObj.setTestPassTime(now);
            updateObj.setPreviousStatus(null);
            updateObj.setLastRejectReason(null);
            workRequirementMapper.updateById(updateObj);
            createLog(requirement.getId(), ACTION_TEST_PASS, requirement.getStatus(),
                    WorkRequirementStatusEnum.PENDING_ACCEPT.getStatus(), null, userId, getOperatorSnapshot(userId).getUserName());
            sendStatusNotifyToDevelopers(requirement, ACTION_TEST_PASS,
                    WorkRequirementStatusEnum.PENDING_ACCEPT.getStatus(), null, userId);
        });
    }

    @Override
    public void testReject(WorkRequirementActionReqVO reqVO, Long userId) {
        WorkRequirementDO requirement = validateRequirement(reqVO.getId());
        validateApprovalPassed(requirement);
        validateStatus(requirement, WorkRequirementStatusEnum.TESTING.getStatus());
        validateProposerOrRequirementAdmin(requirement, userId);
        String rejectReason = StrUtil.trim(reqVO.getRemark());
        if (StrUtil.isBlank(rejectReason)) {
            throw exception(WORK_REQUIREMENT_REJECT_REASON_REQUIRED);
        }
        runInRequirementTenant(requirement, () -> {
            WorkRequirementDO updateObj = new WorkRequirementDO();
            updateObj.setId(requirement.getId());
            updateObj.setStatus(WorkRequirementStatusEnum.DEVELOPING.getStatus());
            updateObj.setPreviousStatus(null);
            updateObj.setLastRejectReason(rejectReason);
            workRequirementMapper.updateById(updateObj);
            createLog(requirement.getId(), ACTION_TEST_REJECT, requirement.getStatus(),
                    WorkRequirementStatusEnum.DEVELOPING.getStatus(), rejectReason,
                    userId, getOperatorSnapshot(userId).getUserName());
            createDeveloperTodos(requirement);
            sendStatusNotifyToDevelopers(requirement, ACTION_TEST_REJECT,
                    WorkRequirementStatusEnum.DEVELOPING.getStatus(), rejectReason, userId);
        });
    }

    @Override
    public void acceptPass(WorkRequirementActionReqVO reqVO, Long userId) {
        WorkRequirementDO requirement = validateRequirement(reqVO.getId());
        validateApprovalPassed(requirement);
        validateStatus(requirement, WorkRequirementStatusEnum.PENDING_ACCEPT.getStatus());
        validateProposerOrRequirementAdmin(requirement, userId);
        String acceptRemark = StrUtil.trim(reqVO.getRemark());
        List<String> acceptanceAttachmentUrls = normalizeAttachmentUrls(reqVO.getAttachmentUrls());
        if (StrUtil.isBlank(acceptRemark) && CollUtil.isEmpty(acceptanceAttachmentUrls)) {
            throw exception(WORK_REQUIREMENT_ACCEPT_ATTACHMENT_REQUIRED);
        }
        Date now = new Date();
        runInRequirementTenant(requirement, () -> {
            if (requirement.getParentId() == null) {
                Long openDescendantCount = workRequirementMapper.countOpenDescendants(requirement.getId());
                if (openDescendantCount != null && openDescendantCount > 0) {
                    throw exception(WORK_REQUIREMENT_HAS_OPEN_CHILDREN);
                }
            }
            OperatorSnapshot operator = getOperatorSnapshot(userId);
            WorkRequirementDO updateObj = new WorkRequirementDO();
            updateObj.setId(requirement.getId());
            updateObj.setStatus(WorkRequirementStatusEnum.DONE.getStatus());
            updateObj.setPreviousStatus(null);
            updateObj.setAcceptedTime(now);
            updateObj.setCloseTime(now);
            workRequirementMapper.updateById(updateObj);
            closeDeveloperTodos(requirement);
            createLog(requirement.getId(), ACTION_ACCEPT_PASS, requirement.getStatus(),
                    WorkRequirementStatusEnum.DONE.getStatus(), null, userId, operator.getUserName());
            createAcceptPassComment(requirement, userId, operator, acceptRemark, acceptanceAttachmentUrls);
            sendStatusNotifyToDevelopers(requirement, ACTION_ACCEPT_PASS,
                    WorkRequirementStatusEnum.DONE.getStatus(), null, userId);
        });
    }

    @Override
    public void acceptReject(WorkRequirementActionReqVO reqVO, Long userId) {
        WorkRequirementDO requirement = validateRequirement(reqVO.getId());
        validateApprovalPassed(requirement);
        validateStatus(requirement, WorkRequirementStatusEnum.PENDING_ACCEPT.getStatus());
        validateProposerOrRequirementAdmin(requirement, userId);
        String rejectReason = StrUtil.trim(reqVO.getRemark());
        if (StrUtil.isBlank(rejectReason)) {
            throw exception(WORK_REQUIREMENT_REJECT_REASON_REQUIRED);
        }
        runInRequirementTenant(requirement, () -> {
            WorkRequirementDO updateObj = new WorkRequirementDO();
            updateObj.setId(requirement.getId());
            updateObj.setStatus(WorkRequirementStatusEnum.DEVELOPING.getStatus());
            updateObj.setPreviousStatus(null);
            updateObj.setLastRejectReason(rejectReason);
            workRequirementMapper.updateById(updateObj);
            createLog(requirement.getId(), ACTION_ACCEPT_REJECT, requirement.getStatus(),
                    WorkRequirementStatusEnum.DEVELOPING.getStatus(), rejectReason,
                    userId, getOperatorSnapshot(userId).getUserName());
            createDeveloperTodos(requirement);
            sendStatusNotifyToDevelopers(requirement, ACTION_ACCEPT_REJECT,
                    WorkRequirementStatusEnum.DEVELOPING.getStatus(), rejectReason, userId);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitRequirementApproval(WorkRequirementActionReqVO reqVO, Long userId) {
        WorkRequirementDO requirement = validateRequirement(reqVO.getId());
        validateProposerOrRequirementAdmin(requirement, userId);
        validateStatus(requirement, WorkRequirementStatusEnum.PENDING_ASSIGN.getStatus(),
                WorkRequirementStatusEnum.PENDING_DEVELOP.getStatus(), WorkRequirementStatusEnum.DEVELOPING.getStatus(),
                WorkRequirementStatusEnum.TESTING.getStatus(), WorkRequirementStatusEnum.PENDING_ACCEPT.getStatus());
        if (Objects.equals(requirement.getApprovalStatus(), WorkRequirementApprovalStatusEnum.RUNNING.getStatus())) {
            throw exception(WORK_REQUIREMENT_APPROVAL_PENDING);
        }
        if (Objects.equals(requirement.getApprovalStatus(), WorkRequirementApprovalStatusEnum.APPROVE.getStatus())) {
            throw exception(WORK_REQUIREMENT_APPROVAL_ALREADY_APPROVED);
        }
        runInRequirementTenant(requirement, () -> {
            WorkRequirementDO currentRequirement = requireCurrentRequirement(reqVO.getId());
            // 重新提审前先切到审批中并清理旧流程实例，避免回调先到时被旧实例号拦截。
            WorkRequirementDO markRunningObj = new WorkRequirementDO();
            markRunningObj.setId(currentRequirement.getId());
            markRunningObj.setApprovalStatus(WorkRequirementApprovalStatusEnum.RUNNING.getStatus());
            markRunningObj.setProcessInstanceId(null);
            workRequirementMapper.updateById(markRunningObj);
            closeApprovalTodos(currentRequirement);
            closeDeveloperTodos(currentRequirement);

            String processInstanceId = startRequirementApprovalProcess(currentRequirement, userId, reqVO.getStartUserSelectAssignees());

            WorkRequirementDO processInstanceUpdateObj = new WorkRequirementDO();
            processInstanceUpdateObj.setId(currentRequirement.getId());
            processInstanceUpdateObj.setProcessInstanceId(processInstanceId);
            workRequirementMapper.updateById(processInstanceUpdateObj);

            currentRequirement.setProcessInstanceId(processInstanceId);
            syncApprovalTodos(currentRequirement, processInstanceId);
            createLog(currentRequirement.getId(), ACTION_BPM_SUBMIT, currentRequirement.getStatus(), currentRequirement.getStatus(),
                    normalizeBlank(StrUtil.trim(reqVO.getRemark())), userId, getOperatorSnapshot(userId).getUserName());
        });
    }

    @Override
    public void cancelRequirement(WorkRequirementActionReqVO reqVO, Long userId) {
        WorkRequirementDO requirement = validateRequirement(reqVO.getId());
        validateParticipantOrRequirementAdmin(requirement, userId);
        validateStatus(requirement, WorkRequirementStatusEnum.PENDING_ASSIGN.getStatus(),
                WorkRequirementStatusEnum.PENDING_DEVELOP.getStatus(), WorkRequirementStatusEnum.DEVELOPING.getStatus(),
                WorkRequirementStatusEnum.TESTING.getStatus(), WorkRequirementStatusEnum.PENDING_ACCEPT.getStatus());
        String remark = normalizeBlank(StrUtil.trim(reqVO.getRemark()));
        runInRequirementTenant(requirement, () -> {
            WorkRequirementDO updateObj = new WorkRequirementDO();
            updateObj.setId(requirement.getId());
            updateObj.setPreviousStatus(requirement.getStatus());
            updateObj.setStatus(WorkRequirementStatusEnum.CANCELED.getStatus());
            updateObj.setCloseTime(new Date());
            workRequirementMapper.updateById(updateObj);
            closeApprovalTodos(requirement);
            closeDeveloperTodos(requirement);
            createLog(requirement.getId(), ACTION_CANCEL, requirement.getStatus(),
                    WorkRequirementStatusEnum.CANCELED.getStatus(), remark,
                    userId, getOperatorSnapshot(userId).getUserName());
            sendStatusNotify(requirement, resolveCounterpartUserId(requirement, userId), ACTION_CANCEL,
                    WorkRequirementStatusEnum.CANCELED.getStatus(), remark, userId);
        });
    }

    @Override
    public void suspendRequirement(WorkRequirementActionReqVO reqVO, Long userId) {
        WorkRequirementDO requirement = validateRequirement(reqVO.getId());
        validateParticipantOrRequirementAdmin(requirement, userId);
        validateStatus(requirement, WorkRequirementStatusEnum.PENDING_ASSIGN.getStatus(),
                WorkRequirementStatusEnum.PENDING_DEVELOP.getStatus(), WorkRequirementStatusEnum.DEVELOPING.getStatus(),
                WorkRequirementStatusEnum.TESTING.getStatus(), WorkRequirementStatusEnum.PENDING_ACCEPT.getStatus());
        String remark = normalizeBlank(StrUtil.trim(reqVO.getRemark()));
        runInRequirementTenant(requirement, () -> {
            WorkRequirementDO updateObj = new WorkRequirementDO();
            updateObj.setId(requirement.getId());
            updateObj.setPreviousStatus(requirement.getStatus());
            updateObj.setStatus(WorkRequirementStatusEnum.SUSPENDED.getStatus());
            workRequirementMapper.updateById(updateObj);
            closeApprovalTodos(requirement);
            closeDeveloperTodos(requirement);
            createLog(requirement.getId(), ACTION_SUSPEND, requirement.getStatus(),
                    WorkRequirementStatusEnum.SUSPENDED.getStatus(), remark,
                    userId, getOperatorSnapshot(userId).getUserName());
            sendStatusNotify(requirement, resolveCounterpartUserId(requirement, userId), ACTION_SUSPEND,
                    WorkRequirementStatusEnum.SUSPENDED.getStatus(), remark, userId);
        });
    }

    @Override
    public void reopenRequirement(WorkRequirementActionReqVO reqVO, Long userId) {
        WorkRequirementDO requirement = validateRequirement(reqVO.getId());
        validateParticipantOrRequirementAdmin(requirement, userId);
        validateStatus(requirement, WorkRequirementStatusEnum.CANCELED.getStatus(),
                WorkRequirementStatusEnum.SUSPENDED.getStatus(), WorkRequirementStatusEnum.DONE.getStatus());
        Integer reopenStatus = resolveReopenStatus(requirement);
        String remark = normalizeBlank(StrUtil.trim(reqVO.getRemark()));
        runInRequirementTenant(requirement, () -> {
            WorkRequirementDO updateObj = new WorkRequirementDO();
            updateObj.setId(requirement.getId());
            updateObj.setStatus(reopenStatus);
            updateObj.setPreviousStatus(null);
            updateObj.setCloseTime(null);
            if (Objects.equals(requirement.getStatus(), WorkRequirementStatusEnum.DONE.getStatus())) {
                updateObj.setAcceptedTime(null);
            }
            workRequirementMapper.updateById(updateObj);
            syncDeveloperTodosByStatus(requirement, reopenStatus);
            createLog(requirement.getId(), ACTION_REOPEN, requirement.getStatus(), reopenStatus, remark,
                    userId, getOperatorSnapshot(userId).getUserName());
            sendStatusNotify(requirement, resolveCounterpartUserId(requirement, userId), ACTION_REOPEN,
                    reopenStatus, remark, userId);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateApprovalStatusByBpmEvent(Long requirementId, String processInstanceId, Integer bpmStatus, Long operatorUserId) {
        WorkRequirementDO requirement = workRequirementMapper.selectById(requirementId);
        if (requirement == null) {
            // 兼容流程结束回调先到、业务事务后提交的场景：短时重试后再跨租户定位
            WorkRequirementDO fallbackRequirement = findRequirementByIdIgnoreTenantWithRetry(requirementId, processInstanceId, bpmStatus);
            if (fallbackRequirement != null) {
                Long currentTenantId = TenantContextHolder.getTenantId();
                Long targetTenantId = fallbackRequirement.getTenantId();
                if (!Objects.equals(currentTenantId, targetTenantId)) {
                    log.warn("[updateApprovalStatusByBpmEvent][当前租户未找到需求，切换租户重试][requirementId={}, processInstanceId={}, bpmStatus={}, currentTenantId={}, targetTenantId={}]",
                            requirementId, processInstanceId, bpmStatus, currentTenantId, targetTenantId);
                    TenantUtils.execute(targetTenantId, () ->
                            updateApprovalStatusByBpmEvent(requirementId, processInstanceId, bpmStatus, operatorUserId));
                    return;
                }
                requirement = fallbackRequirement;
            }
        }
        if (requirement == null) {
            log.warn("[updateApprovalStatusByBpmEvent][需求不存在，忽略审批回调][requirementId={}, processInstanceId={}, bpmStatus={}]",
                    requirementId, processInstanceId, bpmStatus);
            return;
        }
        if (StrUtil.isNotBlank(requirement.getProcessInstanceId())
                && StrUtil.isNotBlank(processInstanceId)
                && !Objects.equals(requirement.getProcessInstanceId(), processInstanceId)) {
            log.warn("[updateApprovalStatusByBpmEvent][流程实例不匹配，忽略审批回调][requirementId={}, processInstanceId={}, currentProcessInstanceId={}]",
                    requirementId, processInstanceId, requirement.getProcessInstanceId());
            return;
        }
        Integer approvalStatus = mapApprovalStatusByBpmStatus(bpmStatus);
        if (approvalStatus == null) {
            return;
        }
        if (Objects.equals(approvalStatus, WorkRequirementApprovalStatusEnum.RUNNING.getStatus())) {
            syncApprovalTodos(requirement, processInstanceId);
        }
        if (Objects.equals(requirement.getApprovalStatus(), approvalStatus)) {
            return;
        }
        WorkRequirementDO updateObj = new WorkRequirementDO();
        updateObj.setId(requirementId);
        updateObj.setApprovalStatus(approvalStatus);
        if (StrUtil.isNotBlank(processInstanceId)) {
            updateObj.setProcessInstanceId(processInstanceId);
        }
        workRequirementMapper.updateById(updateObj);
        log.info("[updateApprovalStatusByBpmEvent][同步审批状态成功][requirementId={}, processInstanceId={}, oldStatus={}, newStatus={}, bpmStatus={}]",
                requirementId, processInstanceId, requirement.getApprovalStatus(), approvalStatus, bpmStatus);
        createLog(requirementId, mapApprovalAction(approvalStatus), requirement.getStatus(), requirement.getStatus(),
                mapApprovalRemark(approvalStatus), operatorUserId == null ? 0L : operatorUserId, resolveOperatorNameByUserId(operatorUserId));
        handleApprovalStatusSideEffects(requirement, approvalStatus, operatorUserId);
    }

    private WorkRequirementDO findRequirementByIdIgnoreTenantWithRetry(Long requirementId, String processInstanceId, Integer bpmStatus) {
        WorkRequirementDO requirement = TenantUtils.executeIgnore(() -> workRequirementMapper.selectById(requirementId));
        if (requirement != null) {
            return requirement;
        }
        for (int i = 1; i <= BPM_CALLBACK_LOOKUP_MAX_RETRY; i++) {
            try {
                Thread.sleep(BPM_CALLBACK_LOOKUP_RETRY_INTERVAL_MS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                log.warn("[updateApprovalStatusByBpmEvent][重试查询需求被中断][requirementId={}, processInstanceId={}, bpmStatus={}, attempt={}]",
                        requirementId, processInstanceId, bpmStatus, i);
                return null;
            }
            requirement = TenantUtils.executeIgnore(() -> workRequirementMapper.selectById(requirementId));
            if (requirement != null) {
                log.info("[updateApprovalStatusByBpmEvent][重试命中需求][requirementId={}, processInstanceId={}, bpmStatus={}, attempt={}]",
                        requirementId, processInstanceId, bpmStatus, i);
                return requirement;
            }
        }
        return null;
    }

    private void createAssigneeTodo(WorkRequirementDO requirement, Long assigneeUserId, Long assigneeTenantId) {
        if (assigneeUserId == null) {
            return;
        }
        TodoSaveReqVO todo = new TodoSaveReqVO();
        todo.setTitle(limitTodoText(StrUtil.format("需求待处理 #{}：{}",
                requirement.getId(), StrUtil.blankToDefault(requirement.getTitle(), "未命名需求")), TODO_TITLE_MAX_LENGTH));
        todo.setDescription(limitTodoText(buildTodoDescription(requirement), TODO_DESCRIPTION_MAX_LENGTH));
        todo.setDueDate(requirement.getExpectedFinishDate());
        todo.setPriority(requirement.getPriority());
        todo.setTags(Arrays.asList("需求管理", "开发负责人"));
        Long targetTenantId = assigneeTenantId == null ? requirement.getTenantId() : assigneeTenantId;
        runInTenant(targetTenantId, () -> todoService.upsertGeneratedTodo(todo, assigneeUserId,
                TODO_BUSINESS_TYPE_REQUIREMENT, requirement.getId(), TODO_TASK_TYPE_DEVELOP,
                buildRequirementRoutePath(requirement.getId())));
    }

    private void createApprovalTodo(WorkRequirementDO requirement, Long approverUserId) {
        if (approverUserId == null) {
            return;
        }
        TodoSaveReqVO todo = new TodoSaveReqVO();
        todo.setTitle(limitTodoText(StrUtil.format("需求待审批 #{}：{}",
                requirement.getId(), StrUtil.blankToDefault(requirement.getTitle(), "未命名需求")), TODO_TITLE_MAX_LENGTH));
        todo.setDescription(limitTodoText(buildTodoDescription(requirement), TODO_DESCRIPTION_MAX_LENGTH));
        todo.setDueDate(requirement.getExpectedFinishDate());
        todo.setPriority(requirement.getPriority());
        todo.setTags(Arrays.asList("需求管理", "审批"));
        runInRequirementTenant(requirement, () -> todoService.upsertGeneratedTodo(todo, approverUserId,
                TODO_BUSINESS_TYPE_REQUIREMENT, requirement.getId(), TODO_TASK_TYPE_APPROVAL,
                buildRequirementRoutePath(requirement.getId())));
    }

    private void syncApprovalTodos(WorkRequirementDO requirement, String processInstanceId) {
        if (requirement == null || requirement.getId() == null) {
            return;
        }
        String effectiveProcessInstanceId = StrUtil.blankToDefault(processInstanceId, requirement.getProcessInstanceId());
        closeApprovalTodos(requirement);
        List<Long> approverUserIds = resolveTodoAssigneeUserIds(effectiveProcessInstanceId);
        approverUserIds.forEach(userId -> createApprovalTodo(requirement, userId));
    }

    private int closeApprovalTodos(WorkRequirementDO requirement) {
        if (requirement == null || requirement.getId() == null) {
            return 0;
        }
        return TenantUtils.executeIgnore(() -> todoService.completeGeneratedTodos(
                TODO_BUSINESS_TYPE_REQUIREMENT, requirement.getId(), TODO_TASK_TYPE_APPROVAL));
    }

    private void createDeveloperTodos(WorkRequirementDO requirement) {
        closeDeveloperTodos(requirement);
        getRequirementDevelopers(requirement).forEach(member ->
                createAssigneeTodo(requirement, member.getUserId(), member.getUserTenantId()));
    }

    private int closeDeveloperTodos(WorkRequirementDO requirement) {
        if (requirement == null || requirement.getId() == null) {
            return 0;
        }
        Set<Long> tenantIds = new LinkedHashSet<>();
        tenantIds.add(requirement.getTenantId());
        getRequirementDevelopers(requirement).stream()
                .map(WorkRequirementDeveloperDO::getUserTenantId)
                .filter(Objects::nonNull)
                .forEach(tenantIds::add);
        int changed = 0;
        for (Long tenantId : tenantIds) {
            Integer count = TenantUtils.executeIgnore(() -> executeInTenant(tenantId,
                    () -> todoService.completeGeneratedTodos(TODO_BUSINESS_TYPE_REQUIREMENT,
                            requirement.getId(), TODO_TASK_TYPE_DEVELOP)));
            changed += count == null ? 0 : count;
        }
        return changed;
    }

    private void syncDeveloperTodosByStatus(WorkRequirementDO requirement, Integer status) {
        if (Objects.equals(status, WorkRequirementStatusEnum.PENDING_DEVELOP.getStatus())
                || Objects.equals(status, WorkRequirementStatusEnum.DEVELOPING.getStatus())) {
            createDeveloperTodos(requirement);
            return;
        }
        closeDeveloperTodos(requirement);
    }

    private String buildRequirementRoutePath(Long requirementId) {
        return "/work/requirement/detail?id=" + requirementId;
    }

    private String buildTodoDescription(WorkRequirementDO requirement) {
        return new StringBuilder()
                .append("提出人：").append(StrUtil.blankToDefault(requirement.getProposerName(), "-"))
                .append("\n提出部门：").append(StrUtil.blankToDefault(requirement.getProposerDept(), "-"))
                .append("\n期望完成：").append(requirement.getExpectedFinishDate() == null ? "-" : DateUtil.formatDateTime(requirement.getExpectedFinishDate()))
                .append("\n预计使用人数：").append(requirement.getEstimatedUserCount() == null ? "-" : requirement.getEstimatedUserCount() + "人")
                .append("\n需求描述：").append(StrUtil.blankToDefault(requirement.getDescription(), "-"))
                .toString();
    }

    private static String limitTodoText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        if (maxLength <= TODO_TEXT_TRUNCATED_SUFFIX.length()) {
            return StrUtil.sub(text, 0, maxLength);
        }
        return StrUtil.sub(text, 0, maxLength - TODO_TEXT_TRUNCATED_SUFFIX.length()) + TODO_TEXT_TRUNCATED_SUFFIX;
    }

    private void updateStatus(WorkRequirementDO requirement, Integer targetStatus, String actionType,
                              String remark, Long userId, boolean clearRejectReason) {
        WorkRequirementDO updateObj = new WorkRequirementDO();
        updateObj.setId(requirement.getId());
        updateObj.setStatus(targetStatus);
        updateObj.setPreviousStatus(null);
        if (clearRejectReason) {
            updateObj.setLastRejectReason(null);
        }
        workRequirementMapper.updateById(updateObj);
        createLog(requirement.getId(), actionType, requirement.getStatus(), targetStatus,
                remark, userId, getOperatorSnapshot(userId).getUserName());
    }

    private String startRequirementApprovalProcess(WorkRequirementDO requirement, Long userId,
                                                   Map<String, List<Long>> startUserSelectAssignees) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("requirementId", requirement.getId());
        variables.put("title", requirement.getTitle());
        variables.put("description", StrUtil.sub(requirement.getDescription(), 0, 500));
        variables.put("priority", requirement.getPriority());
        variables.put("proposerName", requirement.getProposerName());
        variables.put("proposerDept", requirement.getProposerDept());
        variables.put("expectedFinishDate", requirement.getExpectedFinishDate());
        variables.put("estimatedUserCount", requirement.getEstimatedUserCount());
        variables.put("parentRequirementId", requirement.getParentId());
        variables.put("rootRequirementId", requirement.getRootId());
        variables.put("requirementLevel", requirement.getLevel());
        return processInstanceApi.createProcessInstance(userId, new BpmProcessInstanceCreateReqDTO()
                .setProcessDefinitionKey(PROCESS_KEY)
                .setVariables(variables)
                .setBusinessKey(String.valueOf(requirement.getId()))
                .setStartUserSelectAssignees(startUserSelectAssignees))
                .getCheckedData();
    }

    private Integer mapApprovalStatusByBpmStatus(Integer bpmStatus) {
        if (Objects.equals(bpmStatus, BpmTaskStatusEnum.RUNNING.getStatus())) {
            return WorkRequirementApprovalStatusEnum.RUNNING.getStatus();
        }
        if (Objects.equals(bpmStatus, BpmTaskStatusEnum.APPROVE.getStatus())) {
            return WorkRequirementApprovalStatusEnum.APPROVE.getStatus();
        }
        if (Objects.equals(bpmStatus, BpmTaskStatusEnum.REJECT.getStatus())) {
            return WorkRequirementApprovalStatusEnum.REJECT.getStatus();
        }
        if (Objects.equals(bpmStatus, BpmTaskStatusEnum.CANCEL.getStatus())) {
            return WorkRequirementApprovalStatusEnum.CANCEL.getStatus();
        }
        return null;
    }

    private String mapApprovalAction(Integer approvalStatus) {
        if (Objects.equals(approvalStatus, WorkRequirementApprovalStatusEnum.APPROVE.getStatus())) {
            return ACTION_BPM_APPROVE;
        }
        if (Objects.equals(approvalStatus, WorkRequirementApprovalStatusEnum.REJECT.getStatus())) {
            return ACTION_BPM_REJECT;
        }
        if (Objects.equals(approvalStatus, WorkRequirementApprovalStatusEnum.CANCEL.getStatus())) {
            return ACTION_BPM_CANCEL;
        }
        return ACTION_UPDATE;
    }

    private String mapApprovalRemark(Integer approvalStatus) {
        for (WorkRequirementApprovalStatusEnum item : WorkRequirementApprovalStatusEnum.values()) {
            if (Objects.equals(item.getStatus(), approvalStatus)) {
                return "审批状态：" + item.getName();
            }
        }
        return "审批状态变更";
    }

    private String resolveOperatorNameByUserId(Long userId) {
        if (userId == null) {
            return "流程系统";
        }
        try {
            return getOperatorSnapshot(userId).getUserName();
        } catch (Exception ex) {
            return String.valueOf(userId);
        }
    }

    private void validateStatus(WorkRequirementDO requirement, Integer... allowedStatuses) {
        Integer currentStatus = requirement.getStatus();
        if (!Arrays.asList(allowedStatuses).contains(currentStatus)) {
            throw exception(WORK_REQUIREMENT_STATUS_INVALID);
        }
    }

    private void validateApprovalPassed(WorkRequirementDO requirement) {
        if (requirement.getApprovalStatus() == null
                || Objects.equals(requirement.getApprovalStatus(), WorkRequirementApprovalStatusEnum.APPROVE.getStatus())) {
            return;
        }
        throw exception(WORK_REQUIREMENT_APPROVAL_PENDING);
    }

    private void validateAssignee(WorkRequirementDO requirement, Long userId) {
        validateDeveloper(requirement, userId);
    }

    private void validateDeveloper(WorkRequirementDO requirement, Long userId) {
        if (!isRequirementDeveloper(requirement, userId)) {
            throw exception(WORK_REQUIREMENT_FORBIDDEN);
        }
    }

    private void validateDeveloperOrRequirementAdmin(WorkRequirementDO requirement, Long userId) {
        if (isRequirementAdmin(userId)) {
            return;
        }
        validateDeveloper(requirement, userId);
    }

    private void validateProposer(WorkRequirementDO requirement, Long userId) {
        if (!Objects.equals(requirement.getProposerUserId(), userId)) {
            throw exception(WORK_REQUIREMENT_FORBIDDEN);
        }
    }

    private void validateProposerOrRequirementAdmin(WorkRequirementDO requirement, Long userId) {
        if (isRequirementAdmin(userId)) {
            return;
        }
        if (Objects.equals(requirement.getProposerUserId(), userId)) {
            return;
        }
        if (requirement.getParentId() != null) {
            WorkRequirementDO parent = workRequirementMapper.selectById(requirement.getParentId());
            if (parent != null && Objects.equals(parent.getProposerUserId(), userId)) {
                return;
            }
        }
        throw exception(WORK_REQUIREMENT_FORBIDDEN);
    }

    private void validateParticipant(WorkRequirementDO requirement, Long userId) {
        if (!Objects.equals(requirement.getProposerUserId(), userId)
                && !isRequirementDeveloper(requirement, userId)) {
            throw exception(WORK_REQUIREMENT_FORBIDDEN);
        }
    }

    private void validateParticipantOrRequirementAdmin(WorkRequirementDO requirement, Long userId) {
        if (isRequirementAdmin(userId) || canAssignRequirement(userId)) {
            return;
        }
        validateParticipant(requirement, userId);
    }

    private void validateParticipantOrAdmin(WorkRequirementDO requirement, Long userId) {
        if (canQueryAllRequirements(userId)) {
            return;
        }
        if (isRunningApprovalOperator(requirement, userId)) {
            return;
        }
        validateParticipant(requirement, userId);
    }

    private void validateViewParticipantOrAdmin(WorkRequirementDO requirement, Long userId) {
        if (canQueryAllRequirements(userId)) {
            return;
        }
        if (isRunningApprovalOperator(requirement, userId)) {
            return;
        }
        if (isRequirementTreeParticipant(requirement, userId)) {
            return;
        }
        validateParticipant(requirement, userId);
    }

    private void validatePageQueryPermission(WorkRequirementPageReqVO pageReqVO, Long userId) {
        if (canQueryAllRequirements(userId)) {
            return;
        }
        if (userId == null) {
            throw exception(WORK_REQUIREMENT_FORBIDDEN);
        }
        Long proposerUserId = pageReqVO.getProposerUserId();
        Long assigneeUserId = pageReqVO.getAssigneeUserId();
        Long queryUserId = pageReqVO.getUserId();
        if (isRunningApprovalProcessIds(pageReqVO.getProcessInstanceIds(), userId)) {
            return;
        }
        if (proposerUserId == null && assigneeUserId == null && queryUserId == null) {
            throw exception(WORK_REQUIREMENT_FORBIDDEN);
        }
        if (queryUserId != null && !Objects.equals(queryUserId, userId)) {
            throw exception(WORK_REQUIREMENT_FORBIDDEN);
        }
        if (proposerUserId != null && !Objects.equals(proposerUserId, userId)) {
            throw exception(WORK_REQUIREMENT_FORBIDDEN);
        }
        if (assigneeUserId != null && !Objects.equals(assigneeUserId, userId)) {
            throw exception(WORK_REQUIREMENT_FORBIDDEN);
        }
    }

    private List<Long> resolvePageTenantIds(WorkRequirementPageReqVO pageReqVO, Long userId) {
        if (userId != null && (Objects.equals(pageReqVO.getProposerUserId(), userId)
                || Objects.equals(pageReqVO.getAssigneeUserId(), userId)
                || Objects.equals(pageReqVO.getUserId(), userId))) {
            return workRequirementTenantScopeService.getParticipantSearchTenantIds();
        }
        return workRequirementTenantScopeService.getQueryAllTenantIds();
    }

    private void limitRequirementPageSize(WorkRequirementPageReqVO pageReqVO) {
        if (pageReqVO.getPageSize() != null && pageReqVO.getPageSize() > REQUIREMENT_PAGE_SIZE_MAX) {
            pageReqVO.setPageSize(REQUIREMENT_PAGE_SIZE_MAX);
        }
    }

    private WorkRequirementOverviewRespVO createEmptyRequirementOverview() {
        WorkRequirementOverviewRespVO respVO = new WorkRequirementOverviewRespVO();
        respVO.setTotalCount(0L);
        respVO.setPendingCount(0L);
        respVO.setDevelopingCount(0L);
        respVO.setCompletedCount(0L);
        respVO.setMyTodoCount(0L);
        respVO.setOverdueCount(0L);
        respVO.setUnreadCount(0L);
        respVO.setStatusCounts(buildOverviewStatusCountsFromRows(Collections.emptyList()));
        respVO.setTrendDays(buildOverviewTrendDaysFromRows(Collections.emptyList(), Collections.emptyList(),
                DateUtil.beginOfDay(new Date())));
        return respVO;
    }

    private WorkRequirementOverviewRespVO buildRequirementOverview(WorkRequirementPageReqVO pageReqVO,
                                                                   List<Long> tenantIds,
                                                                   Long userId) {
        Map<String, Object> countMap = TenantUtils.executeIgnore(
                () -> workRequirementMapper.selectOverviewCountMap(pageReqVO, tenantIds, userId));
        Date todayBegin = DateUtil.beginOfDay(new Date());
        Date trendStart = DateUtil.offsetDay(todayBegin, -(OVERVIEW_TREND_DAYS - 1));
        Date trendEnd = DateUtil.offsetDay(todayBegin, 1);
        List<Map<String, Object>> statusRows = TenantUtils.executeIgnore(
                () -> workRequirementMapper.selectOverviewStatusCountMaps(pageReqVO, tenantIds));
        List<Map<String, Object>> createdTrendRows = TenantUtils.executeIgnore(
                () -> workRequirementMapper.selectOverviewCreatedTrendCountMaps(pageReqVO, tenantIds, trendStart, trendEnd));
        List<Map<String, Object>> finishedTrendRows = TenantUtils.executeIgnore(
                () -> workRequirementMapper.selectOverviewFinishedTrendCountMaps(pageReqVO, tenantIds, trendStart, trendEnd));

        WorkRequirementOverviewRespVO respVO = new WorkRequirementOverviewRespVO();
        respVO.setTotalCount(readLong(countMap, "totalCount"));
        respVO.setPendingCount(readLong(countMap, "pendingCount"));
        respVO.setDevelopingCount(readLong(countMap, "developingCount"));
        respVO.setCompletedCount(readLong(countMap, "completedCount"));
        respVO.setMyTodoCount(readLong(countMap, "myTodoCount"));
        respVO.setOverdueCount(readLong(countMap, "overdueCount"));
        respVO.setUnreadCount(userId == null ? 0L : TenantUtils.executeIgnore(
                () -> workRequirementCommentMapper.countUnreadCommentsByRequirementFilter(userId, tenantIds, pageReqVO)));
        respVO.setStatusCounts(buildOverviewStatusCountsFromRows(statusRows));
        respVO.setTrendDays(buildOverviewTrendDaysFromRows(createdTrendRows, finishedTrendRows, todayBegin));
        return respVO;
    }

    private List<WorkRequirementOverviewRespVO.StatusCount> buildOverviewStatusCountsFromRows(
            List<Map<String, Object>> rows) {
        Map<Integer, Long> countMap = new HashMap<>();
        rows.forEach(row -> {
            Integer status = normalizeOverviewStatus(readInteger(row, "status"));
            if (status != null) {
                countMap.merge(status, readLong(row, "count"), Long::sum);
            }
        });
        List<Integer> visibleStatuses = Arrays.asList(
                WorkRequirementStatusEnum.PENDING_ASSIGN.getStatus(),
                WorkRequirementStatusEnum.PENDING_DEVELOP.getStatus(),
                WorkRequirementStatusEnum.DEVELOPING.getStatus(),
                WorkRequirementStatusEnum.PENDING_ACCEPT.getStatus(),
                WorkRequirementStatusEnum.DONE.getStatus(),
                WorkRequirementStatusEnum.CANCELED.getStatus(),
                WorkRequirementStatusEnum.SUSPENDED.getStatus());
        return visibleStatuses.stream().map(status -> {
            WorkRequirementOverviewRespVO.StatusCount item = new WorkRequirementOverviewRespVO.StatusCount();
            item.setStatus(status);
            item.setCount(countMap.getOrDefault(status, 0L));
            return item;
        }).collect(Collectors.toList());
    }

    private Integer normalizeOverviewStatus(Integer status) {
        if (Objects.equals(status, WorkRequirementStatusEnum.TESTING.getStatus())) {
            return WorkRequirementStatusEnum.PENDING_ACCEPT.getStatus();
        }
        return status;
    }

    private List<WorkRequirementOverviewRespVO.DailyTrend> buildOverviewTrendDaysFromRows(
            List<Map<String, Object>> createdRows,
            List<Map<String, Object>> finishedRows,
            Date todayBegin) {
        Map<String, WorkRequirementOverviewRespVO.DailyTrend> trendMap = new LinkedHashMap<>();
        for (int index = OVERVIEW_TREND_DAYS - 1; index >= 0; index--) {
            Date day = DateUtil.offsetDay(todayBegin, -index);
            String date = DateUtil.format(day, OVERVIEW_DATE_PATTERN);
            WorkRequirementOverviewRespVO.DailyTrend trend = new WorkRequirementOverviewRespVO.DailyTrend();
            trend.setDate(date);
            trend.setCreatedCount(0L);
            trend.setFinishedCount(0L);
            trendMap.put(date, trend);
        }
        applyOverviewTrendRows(trendMap, createdRows, true);
        applyOverviewTrendRows(trendMap, finishedRows, false);
        return new ArrayList<>(trendMap.values());
    }

    private void applyOverviewTrendRows(Map<String, WorkRequirementOverviewRespVO.DailyTrend> trendMap,
                                        List<Map<String, Object>> rows,
                                        boolean created) {
        rows.forEach(row -> {
            String date = readDate(row, "trendDate");
            WorkRequirementOverviewRespVO.DailyTrend trend = trendMap.get(date);
            if (trend == null) {
                return;
            }
            Long count = readLong(row, "count");
            if (created) {
                trend.setCreatedCount(trend.getCreatedCount() + count);
            } else {
                trend.setFinishedCount(trend.getFinishedCount() + count);
            }
        });
    }

    private Long readLong(Map<String, Object> row, String key) {
        Object value = readValue(row, key);
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private Integer readInteger(Map<String, Object> row, String key) {
        Object value = readValue(row, key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String readDate(Map<String, Object> row, String key) {
        Object value = readValue(row, key);
        if (value instanceof Date) {
            return DateUtil.format((Date) value, OVERVIEW_DATE_PATTERN);
        }
        String text = value == null ? "" : String.valueOf(value);
        return text.length() >= OVERVIEW_DATE_PATTERN.length()
                ? text.substring(0, OVERVIEW_DATE_PATTERN.length())
                : text;
    }

    private Object readValue(Map<String, Object> row, String key) {
        if (row == null || key == null) {
            return null;
        }
        if (row.containsKey(key)) {
            return row.get(key);
        }
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (key.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void validateCreateRequirementPermission(Long userId) {
        if (userId != null) {
            return;
        }
        throw exception(WORK_REQUIREMENT_FORBIDDEN);
    }

    private void validateDeleteRequirementPermission(WorkRequirementDO requirement, Long userId) {
        if (isRequirementAdmin(userId)) {
            return;
        }
        if (Objects.equals(requirement.getProposerUserId(), userId)) {
            return;
        }
        if (hasRequirementUpdatePermission(userId)) {
            return;
        }
        throw exception(WORK_REQUIREMENT_FORBIDDEN);
    }

    private void validateAdminOperator(Long userId) {
        if (!isRequirementAdmin(userId)) {
            throw exception(WORK_REQUIREMENT_FORBIDDEN);
        }
    }

    private boolean isRequirementAdmin(Long userId) {
        return userId != null && securityFrameworkService.hasAnyRoles(
                RoleCodeEnum.SUPER_ADMIN.getCode(),
                RoleCodeEnum.TENANT_ADMIN.getCode(),
                ROLE_SYSTEM_ADMIN);
    }

    private boolean canQueryAllRequirements(Long userId) {
        return userId != null && workRequirementTenantScopeService.canQueryAllRequirements();
    }

    private boolean canAssignRequirement(Long userId) {
        return userId != null && securityFrameworkService.hasPermission(PERMISSION_ASSIGN_REQUIREMENT);
    }

    private boolean hasRequirementUpdatePermission(Long userId) {
        return userId != null && securityFrameworkService.hasPermission(PERMISSION_UPDATE_REQUIREMENT);
    }

    private boolean isRunningApprovalOperator(WorkRequirementDO requirement, Long userId) {
        if (userId == null || requirement == null || StrUtil.isBlank(requirement.getProcessInstanceId())) {
            return false;
        }
        if (!Objects.equals(requirement.getApprovalStatus(), WorkRequirementApprovalStatusEnum.RUNNING.getStatus())) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(processInstanceApi.hasRunningTask(userId,
                    requirement.getProcessInstanceId()).getCheckedData());
        } catch (Exception ex) {
            log.warn("[isRunningApprovalOperator][校验审批待办失败][requirementId={}, userId={}]",
                    requirement.getId(), userId, ex);
            return false;
        }
    }

    private boolean isRunningApprovalProcessIds(String processInstanceIds, Long userId) {
        if (userId == null || StrUtil.isBlank(processInstanceIds)) {
            return false;
        }
        List<String> ids = StrUtil.splitTrim(processInstanceIds, ',');
        if (CollUtil.isEmpty(ids)) {
            return false;
        }
        try {
            return ids.stream().allMatch(processInstanceId -> Boolean.TRUE.equals(
                    processInstanceApi.hasRunningTask(userId, processInstanceId).getCheckedData()));
        } catch (Exception ex) {
            log.warn("[isRunningApprovalProcessIds][校验审批待办列表失败][userId={}]", userId, ex);
            return false;
        }
    }

    private boolean isRequirementTreeParticipant(WorkRequirementDO requirement, Long userId) {
        if (requirement == null || requirement.getId() == null || userId == null) {
            return false;
        }
        try {
            return TenantUtils.executeIgnore(() ->
                    workRequirementMapper.existsTreeParticipant(requirement.getId(), userId));
        } catch (Exception ex) {
            log.warn("[isRequirementTreeParticipant][校验父子需求参与权限失败][requirementId={}, userId={}]",
                    requirement.getId(), userId, ex);
            return false;
        }
    }

    private WorkRequirementDO validateRequirement(Long id) {
        WorkRequirementDO requirement = TenantUtils.executeIgnore(() -> workRequirementMapper.selectById(id));
        if (requirement == null) {
            throw exception(WORK_REQUIREMENT_NOT_EXISTS);
        }
        return requirement;
    }

    private WorkRequirementDO requireCurrentRequirement(Long id) {
        WorkRequirementDO requirement = workRequirementMapper.selectById(id);
        if (requirement == null) {
            throw exception(WORK_REQUIREMENT_NOT_EXISTS);
        }
        return requirement;
    }

    private WorkRequirementDO validateParentForCreate(Long parentId, Long userId) {
        if (parentId == null) {
            return null;
        }
        WorkRequirementDO parentRequirement = validateRequirement(parentId);
        validateParticipantOrAdmin(parentRequirement, userId);
        if (isChildRequirement(parentRequirement)) {
            throw exception(WORK_REQUIREMENT_TREE_DEPTH_EXCEEDED);
        }
        return parentRequirement;
    }

    private boolean isChildRequirement(WorkRequirementDO requirement) {
        if (requirement == null) {
            return false;
        }
        Long parentId = requirement.getParentId();
        if (parentId != null && parentId > 0) {
            return true;
        }
        return requirement.getLevel() != null && requirement.getLevel() > 0;
    }

    private void applyRequirementHierarchyBeforeInsert(WorkRequirementDO requirement, WorkRequirementDO parentRequirement) {
        requirement.setChildCount(0);
        if (parentRequirement == null) {
            requirement.setLevel(0);
            return;
        }
        Integer parentLevel = parentRequirement.getLevel() == null ? 0 : parentRequirement.getLevel();
        requirement.setParentId(parentRequirement.getId());
        requirement.setRootId(parentRequirement.getRootId() == null ? parentRequirement.getId() : parentRequirement.getRootId());
        requirement.setLevel(parentLevel + 1);
    }

    private void applyRequirementHierarchyAfterInsert(WorkRequirementDO requirement, WorkRequirementDO parentRequirement) {
        Long rootId = parentRequirement == null
                ? requirement.getId()
                : (parentRequirement.getRootId() == null ? parentRequirement.getId() : parentRequirement.getRootId());
        String path = parentRequirement == null
                ? "/" + requirement.getId() + "/"
                : normalizeRequirementPath(parentRequirement) + requirement.getId() + "/";
        WorkRequirementDO updateObj = new WorkRequirementDO();
        updateObj.setId(requirement.getId());
        updateObj.setParentId(parentRequirement == null ? null : parentRequirement.getId());
        updateObj.setRootId(rootId);
        updateObj.setLevel(requirement.getLevel() == null ? 0 : requirement.getLevel());
        updateObj.setPath(path);
        updateObj.setChildCount(0);
        workRequirementMapper.updateById(updateObj);
        requirement.setParentId(updateObj.getParentId());
        requirement.setRootId(rootId);
        requirement.setPath(path);
        requirement.setChildCount(0);
        if (parentRequirement != null) {
            workRequirementMapper.incrementChildCount(parentRequirement.getId());
            parentRequirement.setChildCount((parentRequirement.getChildCount() == null ? 0 : parentRequirement.getChildCount()) + 1);
            requirement.setParentTitle(parentRequirement.getTitle());
        }
    }

    private String normalizeRequirementPath(WorkRequirementDO requirement) {
        String path = StrUtil.trim(requirement.getPath());
        if (StrUtil.isNotBlank(path)) {
            return path.endsWith("/") ? path : path + "/";
        }
        Long rootId = requirement.getRootId() == null ? requirement.getId() : requirement.getRootId();
        if (Objects.equals(rootId, requirement.getId())) {
            return "/" + requirement.getId() + "/";
        }
        return "/" + rootId + "/" + requirement.getId() + "/";
    }

    private void createLog(Long requirementId, String actionType, Integer fromStatus, Integer toStatus,
                           String remark, Long operatorUserId, String operatorName) {
        WorkRequirementLogDO logDO = new WorkRequirementLogDO();
        logDO.setRequirementId(requirementId);
        logDO.setActionType(actionType);
        logDO.setFromStatus(fromStatus);
        logDO.setToStatus(toStatus);
        logDO.setRemark(remark);
        logDO.setOperatorUserId(operatorUserId);
        logDO.setOperatorName(operatorName);
        workRequirementLogMapper.insert(logDO);
    }

    private void touchRequirementUpdateTime(Long requirementId) {
        WorkRequirementDO updateObj = new WorkRequirementDO();
        updateObj.setId(requirementId);
        updateObj.setUpdateTime(new Date());
        workRequirementMapper.updateById(updateObj);
    }

    private void fillCommentStats(List<WorkRequirementDO> requirements, Long userId) {
        if (CollUtil.isEmpty(requirements)) {
            return;
        }
        requirements.forEach(item -> {
            item.setCommentCount(0);
            item.setCommentUnreadCount(0);
        });
        List<Long> requirementIds = requirements.stream().map(WorkRequirementDO::getId).collect(Collectors.toList());
        Map<Long, Integer> commentCountMap = TenantUtils.executeIgnore(
                () -> workRequirementCommentMapper.selectCommentCountsByRequirementIds(requirementIds))
                .stream()
                .collect(Collectors.toMap(WorkRequirementCommentCountDO::getRequirementId,
                        item -> NumberUtil.parseInt(String.valueOf(item.getCommentCount())),
                        (first, second) -> first));
        Map<Long, Integer> unreadCountMap = userId == null ? Collections.emptyMap() : TenantUtils.executeIgnore(
                () -> workRequirementCommentMapper.selectUnreadCommentCountsByRequirementIds(userId, requirementIds))
                .stream()
                .collect(Collectors.toMap(WorkRequirementCommentCountDO::getRequirementId,
                        item -> NumberUtil.parseInt(String.valueOf(item.getCommentCount())),
                        (first, second) -> first));
        requirements.forEach(item -> {
            item.setCommentCount(commentCountMap.getOrDefault(item.getId(), 0));
            item.setCommentUnreadCount(unreadCountMap.getOrDefault(item.getId(), 0));
        });
    }

    private void fillParentTitles(List<WorkRequirementDO> requirements) {
        if (CollUtil.isEmpty(requirements)) {
            return;
        }
        Set<Long> parentIds = requirements.stream()
                .map(WorkRequirementDO::getParentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (CollUtil.isEmpty(parentIds)) {
            return;
        }
        List<WorkRequirementDO> parents = TenantUtils.executeIgnore(
                () -> workRequirementMapper.selectBatchIds(parentIds));
        Map<Long, String> parentTitleMap = parents.stream()
                .collect(Collectors.toMap(WorkRequirementDO::getId,
                        item -> StrUtil.blankToDefault(item.getTitle(), String.valueOf(item.getId())),
                        (first, second) -> first));
        Map<Long, Long> parentProposerMap = parents.stream()
                .filter(item -> item.getProposerUserId() != null)
                .collect(Collectors.toMap(WorkRequirementDO::getId,
                        WorkRequirementDO::getProposerUserId,
                        (first, second) -> first));
        requirements.forEach(item -> {
            if (item.getParentId() != null) {
                item.setParentTitle(parentTitleMap.get(item.getParentId()));
                item.setParentProposerUserId(parentProposerMap.get(item.getParentId()));
            }
        });
    }

    private void fillProposerDept(List<WorkRequirementDO> requirements) {
        if (CollUtil.isEmpty(requirements)) {
            return;
        }
        List<WorkRequirementDO> needResolveList = requirements.stream()
                .filter(item -> item.getProposerUserId() != null && needResolveProposerDept(item.getProposerDept()))
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(needResolveList)) {
            return;
        }
        try {
            Set<Long> userIds = needResolveList.stream()
                    .map(WorkRequirementDO::getProposerUserId)
                    .collect(Collectors.toSet());
            Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
            Set<Long> deptIds = userMap.values().stream()
                    .map(AdminUserRespDTO::getDeptId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Map<Long, DeptRespDTO> deptMap = CollUtil.isEmpty(deptIds)
                    ? Collections.emptyMap()
                    : deptApi.getDeptMap(deptIds);
            needResolveList.forEach(item -> {
                AdminUserRespDTO user = userMap.get(item.getProposerUserId());
                if (user == null || user.getDeptId() == null) {
                    return;
                }
                DeptRespDTO dept = deptMap.get(user.getDeptId());
                String deptName = dept == null ? null : normalizeBlank(StrUtil.trim(dept.getName()));
                if (deptName != null) {
                    item.setProposerDept(deptName);
                }
            });
        } catch (Exception ex) {
            log.warn("[fillProposerDept][回填提出部门失败]", ex);
        }
    }

    private boolean needResolveProposerDept(String proposerDept) {
        String normalizedDept = normalizeBlank(StrUtil.trim(proposerDept));
        if (normalizedDept == null) {
            return true;
        }
        return "未识别".equals(normalizedDept)
                || "未分配部门".equals(normalizedDept)
                || "-".equals(normalizedDept);
    }

    private void fillDeveloperMembers(List<WorkRequirementDO> requirements) {
        if (CollUtil.isEmpty(requirements)) {
            return;
        }
        List<Long> requirementIds = requirements.stream()
                .map(WorkRequirementDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, List<WorkRequirementDeveloperDO>> memberMap = CollUtil.isEmpty(requirementIds)
                ? Collections.emptyMap()
                : TenantUtils.executeIgnore(() -> workRequirementDeveloperMapper.selectListByRequirementIds(requirementIds))
                .stream()
                .collect(Collectors.groupingBy(WorkRequirementDeveloperDO::getRequirementId));
        requirements.forEach(requirement -> {
            List<WorkRequirementDeveloperDO> members = new ArrayList<>(
                    memberMap.getOrDefault(requirement.getId(), Collections.emptyList()));
            if (CollUtil.isEmpty(members) && requirement.getAssigneeUserId() != null) {
                WorkRequirementDeveloperDO fallbackMember = new WorkRequirementDeveloperDO();
                fallbackMember.setRequirementId(requirement.getId());
                fallbackMember.setUserId(requirement.getAssigneeUserId());
                fallbackMember.setUserName(requirement.getAssigneeName());
                fallbackMember.setUserTenantId(requirement.getTenantId());
                fallbackMember.setMemberRole(DEVELOPER_ROLE_MAIN);
                members.add(fallbackMember);
            }
            syncDeveloperDisplayFields(requirement, members);
        });
    }

    private void syncRequirementDevelopers(WorkRequirementDO requirement, Long mainUserId, String mainUserName,
                                           Long mainUserTenantId, List<Long> collaboratorUserIds) {
        workRequirementDeveloperMapper.deletePhysicallyByRequirementId(requirement.getId());
        if (mainUserId == null) {
            syncDeveloperDisplayFields(requirement, Collections.emptyList());
            return;
        }
        List<WorkRequirementDeveloperDO> members = new ArrayList<>();
        WorkRequirementDeveloperDO mainMember = new WorkRequirementDeveloperDO();
        mainMember.setRequirementId(requirement.getId());
        mainMember.setUserId(mainUserId);
        mainMember.setUserName(mainUserName);
        mainMember.setUserTenantId(mainUserTenantId == null ? requirement.getTenantId() : mainUserTenantId);
        mainMember.setMemberRole(DEVELOPER_ROLE_MAIN);
        members.add(mainMember);

        normalizeCollaboratorUserIds(collaboratorUserIds, mainUserId).forEach(userId -> {
            AdminUserRespDTO user = adminUserApi.getUser(userId).getCheckedData();
            Long userTenantId = workRequirementTenantScopeService.resolveAssigneeTenantId(userId, null);
            WorkRequirementDeveloperDO member = new WorkRequirementDeveloperDO();
            member.setRequirementId(requirement.getId());
            member.setUserId(userId);
            member.setUserName(getUserDisplayName(user, userId, null));
            member.setUserTenantId(userTenantId);
            member.setMemberRole(DEVELOPER_ROLE_COLLABORATOR);
            members.add(member);
        });
        workRequirementDeveloperMapper.insertBatch(members);
        syncDeveloperDisplayFields(requirement, members);
    }

    private List<Long> normalizeCollaboratorUserIds(List<Long> collaboratorUserIds, Long mainUserId) {
        if (CollUtil.isEmpty(collaboratorUserIds)) {
            return Collections.emptyList();
        }
        return collaboratorUserIds.stream()
                .filter(Objects::nonNull)
                .filter(userId -> !Objects.equals(userId, mainUserId))
                .distinct()
                .collect(Collectors.toList());
    }

    private void syncDeveloperDisplayFields(WorkRequirementDO requirement, List<WorkRequirementDeveloperDO> members) {
        List<WorkRequirementDeveloperDO> normalizedMembers = members == null
                ? Collections.emptyList()
                : members.stream()
                .filter(member -> member.getUserId() != null)
                .sorted((first, second) -> {
                    boolean firstMain = DEVELOPER_ROLE_MAIN.equals(first.getMemberRole());
                    boolean secondMain = DEVELOPER_ROLE_MAIN.equals(second.getMemberRole());
                    if (firstMain == secondMain) {
                        return Long.compare(first.getId() == null ? 0L : first.getId(), second.getId() == null ? 0L : second.getId());
                    }
                    return firstMain ? -1 : 1;
                })
                .collect(Collectors.toList());
        requirement.setDeveloperMembers(normalizedMembers);
        requirement.setCollaboratorUserIds(normalizedMembers.stream()
                .filter(member -> !DEVELOPER_ROLE_MAIN.equals(member.getMemberRole()))
                .map(WorkRequirementDeveloperDO::getUserId)
                .collect(Collectors.toList()));
        requirement.setCollaboratorNames(normalizedMembers.stream()
                .filter(member -> !DEVELOPER_ROLE_MAIN.equals(member.getMemberRole()))
                .map(member -> StrUtil.blankToDefault(member.getUserName(), String.valueOf(member.getUserId())))
                .collect(Collectors.joining("、")));
    }

    private List<WorkRequirementDeveloperDO> getRequirementDevelopers(WorkRequirementDO requirement) {
        if (requirement == null) {
            return Collections.emptyList();
        }
        if (requirement.getDeveloperMembers() == null) {
            fillDeveloperMembers(Collections.singletonList(requirement));
        }
        return requirement.getDeveloperMembers() == null
                ? Collections.emptyList()
                : requirement.getDeveloperMembers();
    }

    private Set<Long> getRequirementDeveloperUserIds(WorkRequirementDO requirement) {
        return getRequirementDevelopers(requirement).stream()
                .map(WorkRequirementDeveloperDO::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private boolean isRequirementDeveloper(WorkRequirementDO requirement, Long userId) {
        return userId != null && getRequirementDeveloperUserIds(requirement).contains(userId);
    }

    private String buildDeveloperNamesText(List<WorkRequirementDeveloperDO> members) {
        String names = (members == null ? Collections.<WorkRequirementDeveloperDO>emptyList() : members)
                .stream()
                .map(member -> StrUtil.blankToDefault(member.getUserName(), String.valueOf(member.getUserId())))
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.joining("、"));
        return StrUtil.blankToDefault(names, "未分派");
    }

    private void applyCommentReadStatus(List<WorkRequirementCommentDO> comments, Long userId,
                                        WorkRequirementDO requirement) {
        if (CollUtil.isEmpty(comments)) {
            return;
        }
        Set<Long> readCommentIds = getReadCommentIds(userId, comments);
        comments.forEach(comment -> comment.setReadStatus(
                !isUnreadCommentForUser(comment, userId, readCommentIds, requirement)));
    }

    private Set<Long> getReadCommentIds(Long userId, List<WorkRequirementCommentDO> comments) {
        if (userId == null || CollUtil.isEmpty(comments)) {
            return Collections.emptySet();
        }
        List<Long> commentIds = comments.stream().map(WorkRequirementCommentDO::getId).collect(Collectors.toList());
        return TenantUtils.executeIgnore(() -> workRequirementCommentReadMapper.selectListByUserIdAndCommentIds(userId, commentIds)).stream()
                .map(WorkRequirementCommentReadDO::getCommentId)
                .collect(Collectors.toSet());
    }

    private <T> T executeInRequirementTenant(WorkRequirementDO requirement, Supplier<T> supplier) {
        return executeInTenant(requirement == null ? null : requirement.getTenantId(), supplier);
    }

    private void runInRequirementTenant(WorkRequirementDO requirement, Runnable runnable) {
        runInTenant(requirement == null ? null : requirement.getTenantId(), runnable);
    }

    private <T> T executeInTenant(Long tenantId, Supplier<T> supplier) {
        if (tenantId == null) {
            return supplier.get();
        }
        return TenantUtils.execute(tenantId, supplier::get);
    }

    private void runInTenant(Long tenantId, Runnable runnable) {
        executeInTenant(tenantId, () -> {
            runnable.run();
            return null;
        });
    }

    private void createCommentReadRecords(List<WorkRequirementCommentDO> comments, Long userId,
                                          WorkRequirementDO requirement) {
        if (CollUtil.isEmpty(comments) || userId == null) {
            return;
        }
        Set<Long> readCommentIds = getReadCommentIds(userId, comments);
        List<Long> unreadCommentIds = comments.stream()
                .filter(comment -> isUnreadCommentForUser(comment, userId, readCommentIds, requirement))
                .map(WorkRequirementCommentDO::getId)
                .collect(Collectors.toList());
        createCommentReadRecords(unreadCommentIds, userId);
    }

    private void createCommentReadRecords(List<Long> commentIds, Long userId) {
        if (CollUtil.isEmpty(commentIds) || userId == null) {
            return;
        }
        Set<Long> readCommentIds = TenantUtils.executeIgnore(
                () -> workRequirementCommentReadMapper.selectListByUserIdAndCommentIds(userId, commentIds)).stream()
                .map(WorkRequirementCommentReadDO::getCommentId)
                .collect(Collectors.toSet());
        List<WorkRequirementCommentReadDO> readList = new ArrayList<>();
        Date now = new Date();
        for (Long commentId : commentIds) {
            if (commentId == null || readCommentIds.contains(commentId)) {
                continue;
            }
            WorkRequirementCommentReadDO readDO = new WorkRequirementCommentReadDO();
            readDO.setCommentId(commentId);
            readDO.setUserId(userId);
            readDO.setReadTime(now);
            readList.add(readDO);
        }
        if (CollUtil.isNotEmpty(readList)) {
            workRequirementCommentReadMapper.insertBatch(readList);
        }
    }

    private void createCommentReadRecordsByPrototypes(List<WorkRequirementCommentReadDO> unreadReadRecords,
                                                      Long userId) {
        if (CollUtil.isEmpty(unreadReadRecords) || userId == null) {
            return;
        }
        Map<Long, Long> unreadCommentTenantMap = unreadReadRecords.stream()
                .filter(item -> item.getCommentId() != null)
                .collect(Collectors.toMap(WorkRequirementCommentReadDO::getCommentId,
                        WorkRequirementCommentReadDO::getTenantId, (first, second) -> first));
        if (CollUtil.isEmpty(unreadCommentTenantMap)) {
            return;
        }
        Set<Long> readCommentIds = TenantUtils.executeIgnore(
                () -> workRequirementCommentReadMapper.selectListByUserIdAndCommentIds(
                        userId, unreadCommentTenantMap.keySet())).stream()
                .map(WorkRequirementCommentReadDO::getCommentId)
                .collect(Collectors.toSet());
        Date now = new Date();
        List<WorkRequirementCommentReadDO> readList = unreadCommentTenantMap.entrySet().stream()
                .filter(entry -> !readCommentIds.contains(entry.getKey()))
                .map(entry -> {
                    WorkRequirementCommentReadDO readDO = new WorkRequirementCommentReadDO();
                    readDO.setCommentId(entry.getKey());
                    readDO.setUserId(userId);
                    readDO.setReadTime(now);
                    readDO.setTenantId(entry.getValue());
                    return readDO;
                })
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(readList)) {
            TenantUtils.executeIgnore(() -> workRequirementCommentReadMapper.insertBatch(readList));
        }
    }

    private boolean isUnreadCommentForUser(WorkRequirementCommentDO comment, Long userId, Set<Long> readCommentIds,
                                           WorkRequirementDO requirement) {
        if (comment == null || userId == null || Objects.equals(comment.getFromUserId(), userId)
                || readCommentIds.contains(comment.getId())) {
            return false;
        }
        if (comment.getTargetUserId() != null) {
            return Objects.equals(comment.getTargetUserId(), userId);
        }
        return isRequirementParticipant(requirement, userId);
    }

    private boolean isRequirementParticipant(WorkRequirementDO requirement, Long userId) {
        return requirement != null && userId != null
                && (Objects.equals(requirement.getProposerUserId(), userId)
                || Objects.equals(requirement.getAssigneeUserId(), userId)
                || isRequirementDeveloper(requirement, userId));
    }

    private String normalizeBlank(String value) {
        return StrUtil.isBlank(value) ? null : value;
    }

    private List<String> normalizeAttachmentUrls(List<String> attachmentUrls) {
        if (CollUtil.isEmpty(attachmentUrls)) {
            return null;
        }
        List<String> urls = attachmentUrls.stream()
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        return CollUtil.isEmpty(urls) ? null : urls;
    }

    private String normalizeCommentContent(String content, List<String> attachmentUrls) {
        String normalizedContent = StrUtil.trim(content);
        if (StrUtil.isNotBlank(normalizedContent)) {
            return normalizedContent;
        }
        if (CollUtil.isEmpty(attachmentUrls)) {
            throw exception(WORK_REQUIREMENT_COMMENT_CONTENT_REQUIRED);
        }
        return "附件";
    }

    private String resolveOperatorName(String operatorName, String fallbackName, Long userId) {
        if (StrUtil.isNotBlank(operatorName) && !Objects.equals(operatorName, String.valueOf(userId))) {
            return operatorName;
        }
        return StrUtil.blankToDefault(StrUtil.trim(fallbackName), operatorName);
    }

    private String normalizeCommentType(String commentType) {
        return COMMENT_TYPE_QUESTION.equalsIgnoreCase(StrUtil.blankToDefault(commentType, COMMENT_TYPE_COMMENT))
                ? COMMENT_TYPE_QUESTION : COMMENT_TYPE_COMMENT;
    }


    private void createAcceptPassComment(WorkRequirementDO requirement, Long userId, OperatorSnapshot operator,
                                         String remark, List<String> attachmentUrls) {
        WorkRequirementCommentDO comment = new WorkRequirementCommentDO();
        comment.setRequirementId(requirement.getId());
        comment.setCommentType(COMMENT_TYPE_COMMENT);
        comment.setContent(StrUtil.blankToDefault(StrUtil.trim(remark), "验收通过截图"));
        comment.setAttachmentUrls(attachmentUrls);
        comment.setFromUserId(userId);
        comment.setFromUserName(operator.getUserName());
        Long targetUserId = resolveCounterpartUserId(requirement, userId);
        comment.setTargetUserId(targetUserId);
        comment.setTargetUserName(getRequirementUserDisplayName(requirement, targetUserId));
        comment.setIp(ServletUtils.getClientIP());
        workRequirementCommentMapper.insert(comment);
    }

    private Long resolveCommentTargetUserId(WorkRequirementDO requirement, Long targetUserId, Long userId) {
        if (targetUserId != null && !Objects.equals(targetUserId, userId) && isEnabledUser(targetUserId)) {
            return targetUserId;
        }
        return resolveCounterpartUserId(requirement, userId);
    }

    private boolean isEnabledUser(Long userId) {
        try {
            AdminUserRespDTO user = adminUserApi.getUser(userId).getCheckedData();
            return user != null && Objects.equals(user.getStatus(), CommonStatusEnum.ENABLE.getStatus());
        } catch (Exception ex) {
            log.warn("[isEnabledUser][查询用户失败][userId={}]", userId, ex);
            return false;
        }
    }

    private Long resolveRateTargetUserId(WorkRequirementDO requirement, Long targetUserId, Long userId) {
        Set<Long> developerUserIds = getRequirementDeveloperUserIds(requirement);
        if (targetUserId != null
                && !Objects.equals(targetUserId, userId)
                && (developerUserIds.contains(targetUserId)
                || Objects.equals(targetUserId, requirement.getProposerUserId()))) {
            return targetUserId;
        }
        Long counterpartUserId = resolveCounterpartUserId(requirement, userId);
        if (counterpartUserId != null) {
            return counterpartUserId;
        }
        if (requirement.getAssigneeUserId() != null) {
            return requirement.getAssigneeUserId();
        }
        if (requirement.getProposerUserId() != null) {
            return requirement.getProposerUserId();
        }
        return userId;
    }

    private Long resolveCounterpartUserId(WorkRequirementDO requirement, Long userId) {
        Set<Long> developerUserIds = getRequirementDeveloperUserIds(requirement);
        if (Objects.equals(requirement.getProposerUserId(), userId)) {
            return developerUserIds.stream()
                    .filter(developerUserId -> !Objects.equals(developerUserId, userId))
                    .findFirst()
                    .orElse(null);
        }
        if (developerUserIds.contains(userId) && requirement.getProposerUserId() != null
                && !Objects.equals(requirement.getProposerUserId(), userId)) {
            return requirement.getProposerUserId();
        }
        Long developerUserId = developerUserIds.stream()
                .filter(item -> !Objects.equals(item, userId))
                .findFirst()
                .orElse(null);
        if (developerUserId != null) return developerUserId;
        if (requirement.getProposerUserId() != null && !Objects.equals(requirement.getProposerUserId(), userId)) {
            return requirement.getProposerUserId();
        }
        return null;
    }

    private Integer resolveReopenStatus(WorkRequirementDO requirement) {
        Integer previousStatus = requirement.getPreviousStatus();
        if (previousStatus != null
                && !Arrays.asList(WorkRequirementStatusEnum.CANCELED.getStatus(), WorkRequirementStatusEnum.SUSPENDED.getStatus())
                .contains(previousStatus)) {
            return previousStatus;
        }
        return requirement.getAssigneeUserId() == null
                ? WorkRequirementStatusEnum.PENDING_ASSIGN.getStatus()
                : WorkRequirementStatusEnum.PENDING_DEVELOP.getStatus();
    }

    private void sendAssignNotify(WorkRequirementDO requirement, Long assigneeUserId) {
        if (assigneeUserId == null) {
            return;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("requirementId", requirement.getId());
        params.put("title", StrUtil.blankToDefault(requirement.getTitle(), "未命名需求"));
        params.put("proposerName", StrUtil.blankToDefault(requirement.getProposerName(), "-"));
        params.put("expectedFinishDate", requirement.getExpectedFinishDate() == null ? "-" : DateUtil.formatDateTime(requirement.getExpectedFinishDate()));
        params.put("detailUrl", buildRequirementDetailUrl(requirement.getId()));
        sendNotifyToAdmin(assigneeUserId, NOTIFY_TEMPLATE_ASSIGN, params, "assign", requirement.getId());
        sendAssignedDevDingTalkNotify(requirement, assigneeUserId);
    }

    private void sendAssignNotifyToDevelopers(WorkRequirementDO requirement) {
        getRequirementDeveloperUserIds(requirement).forEach(userId -> sendAssignNotify(requirement, userId));
    }

    private void sendAssignedDevDingTalkNotify(WorkRequirementDO requirement, Long assigneeUserId) {
        if (assigneeUserId == null) {
            return;
        }
        try {
            dingTalkRequirementNoticeApi.sendAssignedDev(
                    buildDingTalkNoticeReq(requirement, Collections.singletonList(assigneeUserId))).checkError();
        } catch (Exception ex) {
            log.warn("[sendAssignedDevDingTalkNotify][发送需求开发钉钉通知失败][requirementId={}, assigneeUserId={}]",
                    requirement.getId(), assigneeUserId, ex);
        }
    }

    private void sendCommentRemindDingTalkNotify(WorkRequirementDO requirement, WorkRequirementCommentDO comment) {
        if (comment.getTargetUserId() == null || Objects.equals(comment.getTargetUserId(), comment.getFromUserId())) {
            return;
        }
        try {
            DingTalkRequirementNoticeReqDTO reqDTO = buildDingTalkNoticeReq(requirement,
                    Collections.singletonList(comment.getTargetUserId()));
            reqDTO.setDedupBizId("comment:" + comment.getId());
            reqDTO.setOperatorName(StrUtil.blankToDefault(comment.getFromUserName(), String.valueOf(comment.getFromUserId())));
            reqDTO.setCommentTypeLabel(getCommentTypeLabel(comment.getCommentType()));
            reqDTO.setCommentContent(comment.getContent());
            reqDTO.setTargetUserName(comment.getTargetUserName());
            dingTalkRequirementNoticeApi.sendCommentRemind(reqDTO).checkError();
        } catch (Exception ex) {
            log.warn("[sendCommentRemindDingTalkNotify][发送需求沟通提醒钉钉通知失败][requirementId={}, commentId={}, targetUserId={}]",
                    requirement.getId(), comment.getId(), comment.getTargetUserId(), ex);
        }
    }

    private DingTalkRequirementNoticeReqDTO buildDingTalkNoticeReq(WorkRequirementDO requirement, List<Long> receiverUserIds) {
        DingTalkRequirementNoticeReqDTO reqDTO = new DingTalkRequirementNoticeReqDTO();
        reqDTO.setReceiverUserIds(receiverUserIds);
        reqDTO.setRequirementId(requirement.getId());
        reqDTO.setTitle(requirement.getTitle());
        reqDTO.setProposerName(requirement.getProposerName());
        reqDTO.setProposerDept(requirement.getProposerDept());
        reqDTO.setAssigneeName(requirement.getAssigneeName());
        reqDTO.setExpectedFinishDate(requirement.getExpectedFinishDate() == null ? "-" : DateUtil.formatDateTime(requirement.getExpectedFinishDate()));
        reqDTO.setDetailUrl(buildRequirementDetailUrl(requirement.getId()));
        return reqDTO;
    }

    private List<Long> resolveTodoAssigneeUserIds(String processInstanceId) {
        if (StrUtil.isBlank(processInstanceId)) {
            return Collections.emptyList();
        }
        for (int i = 1; i <= 3; i++) {
            try {
                List<Long> userIds = processInstanceApi.getTodoAssigneeUserIds(processInstanceId).getCheckedData();
                if (CollUtil.isNotEmpty(userIds)) {
                    return userIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
                }
                Thread.sleep(200L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return Collections.emptyList();
            } catch (Exception ex) {
                log.warn("[resolveTodoAssigneeUserIds][查询流程待办人失败][processInstanceId={}]", processInstanceId, ex);
                return Collections.emptyList();
            }
        }
        return Collections.emptyList();
    }

    private List<Long> resolveApprovalNoticeReceiverUserIds(String processInstanceId) {
        List<Long> receiverUserIds = new ArrayList<>();
        receiverUserIds.addAll(resolveTodoAssigneeUserIds(processInstanceId));
        receiverUserIds.addAll(resolveCopyUserIds(processInstanceId));
        return receiverUserIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }

    private List<Long> resolveCopyUserIds(String processInstanceId) {
        if (StrUtil.isBlank(processInstanceId)) {
            return Collections.emptyList();
        }
        try {
            List<Long> userIds = processInstanceApi.getCopyUserIds(processInstanceId).getCheckedData();
            return userIds == null ? Collections.emptyList()
                    : userIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        } catch (Exception ex) {
            log.warn("[resolveCopyUserIds][查询流程抄送人失败][processInstanceId={}]", processInstanceId, ex);
            return Collections.emptyList();
        }
    }

    private void sendStatusNotify(WorkRequirementDO requirement, Long receiverUserId, String actionType,
                                  Integer targetStatus, String remark, Long operatorUserId) {
        if (receiverUserId == null || Objects.equals(receiverUserId, operatorUserId)) {
            return;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("requirementId", requirement.getId());
        params.put("title", StrUtil.blankToDefault(requirement.getTitle(), "未命名需求"));
        params.put("actionLabel", getActionLabel(actionType));
        params.put("statusName", getStatusName(targetStatus));
        params.put("operatorName", resolveOperatorNameByUserId(operatorUserId));
        params.put("remark", StrUtil.blankToDefault(normalizeBlank(remark), "-"));
        params.put("detailUrl", buildRequirementDetailUrl(requirement.getId()));
        sendNotifyToAdmin(receiverUserId, NOTIFY_TEMPLATE_STATUS, params, actionType, requirement.getId());
    }

    private void sendStatusNotifyToDevelopers(WorkRequirementDO requirement, String actionType,
                                              Integer targetStatus, String remark, Long operatorUserId) {
        getRequirementDeveloperUserIds(requirement).forEach(receiverUserId ->
                sendStatusNotify(requirement, receiverUserId, actionType, targetStatus, remark, operatorUserId));
    }

    private void handleApprovalStatusSideEffects(WorkRequirementDO requirement, Integer approvalStatus, Long operatorUserId) {
        if (Objects.equals(approvalStatus, WorkRequirementApprovalStatusEnum.APPROVE.getStatus())) {
            closeApprovalTodos(requirement);
            if (requirement.getAssigneeUserId() != null) {
                createDeveloperTodos(requirement);
                sendAssignNotifyToDevelopers(requirement);
            } else {
                sendStatusNotify(requirement, requirement.getProposerUserId(), ACTION_BPM_APPROVE,
                        requirement.getStatus(), mapApprovalRemark(approvalStatus), operatorUserId);
            }
            return;
        }
        if (Objects.equals(approvalStatus, WorkRequirementApprovalStatusEnum.REJECT.getStatus())
                || Objects.equals(approvalStatus, WorkRequirementApprovalStatusEnum.CANCEL.getStatus())) {
            closeApprovalTodos(requirement);
            closeDeveloperTodos(requirement);
            sendStatusNotify(requirement, requirement.getProposerUserId(), mapApprovalAction(approvalStatus),
                    requirement.getStatus(), mapApprovalRemark(approvalStatus), operatorUserId);
        }
    }

    private void sendCommentNotify(WorkRequirementDO requirement, WorkRequirementCommentDO comment) {
        if (comment.getTargetUserId() == null || Objects.equals(comment.getTargetUserId(), comment.getFromUserId())) {
            return;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("requirementId", requirement.getId());
        params.put("title", StrUtil.blankToDefault(requirement.getTitle(), "未命名需求"));
        params.put("commentTypeLabel", getCommentTypeLabel(comment.getCommentType()));
        params.put("content", StrUtil.blankToDefault(comment.getContent(), "-"));
        params.put("operatorName", StrUtil.blankToDefault(comment.getFromUserName(), String.valueOf(comment.getFromUserId())));
        params.put("detailUrl", buildRequirementDetailUrl(requirement.getId()));
        sendNotifyToAdmin(comment.getTargetUserId(), NOTIFY_TEMPLATE_COMMENT, params, comment.getCommentType(), requirement.getId());
        sendCommentRemindDingTalkNotify(requirement, comment);
    }

    private void sendNotifyToAdmin(Long userId, String templateCode, Map<String, Object> templateParams,
                                   String scene, Long requirementId) {
        if (userId == null) {
            return;
        }
        try {
            NotifySendSingleToUserReqDTO reqDTO = new NotifySendSingleToUserReqDTO();
            reqDTO.setUserId(userId);
            reqDTO.setTemplateCode(templateCode);
            reqDTO.setTemplateParams(templateParams);
            notifyMessageSendApi.sendSingleMessageToAdmin(reqDTO).checkError();
        } catch (Exception ex) {
            log.warn("[sendNotifyToAdmin][发送需求站内信失败][scene={}, requirementId={}]", scene, requirementId, ex);
        }
    }

    private String buildRequirementDetailUrl(Long requirementId) {
        return webProperties.getAdminUi().getUrl() + "/work/requirement/detail?id=" + requirementId;
    }

    private String getActionLabel(String actionType) {
        switch (actionType) {
            case ACTION_CREATE_CHILD:
                return "新增子需求";
            case ACTION_ASSIGN:
                return "分派";
            case ACTION_DEV_REJECT:
                return "退回/提疑";
            case ACTION_START_DEV:
                return "开始开发";
            case ACTION_SUBMIT_TEST:
                return "提交测试";
            case ACTION_TEST_PASS:
                return "提交验收";
            case ACTION_TEST_REJECT:
                return "测试退回";
            case ACTION_ACCEPT_PASS:
                return "验收通过";
            case ACTION_ACCEPT_REJECT:
                return "验收退回";
            case ACTION_CANCEL:
                return "取消需求";
            case ACTION_SUSPEND:
                return "挂起需求";
            case ACTION_REOPEN:
                return "重新打开";
            case ACTION_BPM_SUBMIT:
                return "提交审批";
            case ACTION_BPM_APPROVE:
                return "审批通过";
            case ACTION_BPM_REJECT:
                return "审批拒绝";
            case ACTION_BPM_CANCEL:
                return "审批取消";
            default:
                return actionType;
        }
    }

    private String getCommentTypeLabel(String commentType) {
        return COMMENT_TYPE_QUESTION.equalsIgnoreCase(commentType) ? "追问" : "评论";
    }

    private String getStatusName(Integer status) {
        if (status == null) {
            return "-";
        }
        for (WorkRequirementStatusEnum item : WorkRequirementStatusEnum.values()) {
            if (Objects.equals(item.getStatus(), status)) {
                return item.getName();
            }
        }
        return String.valueOf(status);
    }

    private String getRequirementUserDisplayName(WorkRequirementDO requirement, Long userId) {
        if (userId == null) {
            return null;
        }
        if (requirement != null && Objects.equals(requirement.getAssigneeUserId(), userId)
                && isReliableUserDisplayName(requirement.getAssigneeName(), userId)) {
            return StrUtil.trim(requirement.getAssigneeName());
        }
        if (requirement != null && Objects.equals(requirement.getProposerUserId(), userId)
                && isReliableUserDisplayName(requirement.getProposerName(), userId)) {
            return StrUtil.trim(requirement.getProposerName());
        }
        return getUserDisplayName(userId);
    }

    private boolean isReliableUserDisplayName(String name, Long userId) {
        String normalizedName = normalizeBlank(StrUtil.trim(name));
        return StrUtil.isNotBlank(normalizedName)
                && !Objects.equals(normalizedName, String.valueOf(userId))
                && !NumberUtil.isNumber(normalizedName);
    }

    private String getUserDisplayName(Long userId) {
        if (userId == null) {
            return null;
        }
        AdminUserRespDTO user = adminUserApi.getUser(userId).getCheckedData();
        return getUserDisplayName(user, userId);
    }

    private String getUserDisplayName(AdminUserRespDTO user, Long userId) {
        return getUserDisplayName(user, userId, null);
    }

    private String getUserDisplayName(AdminUserRespDTO user, Long userId, String fallbackName) {
        String resolvedName = null;
        if (user != null) {
            resolvedName = StrUtil.blankToDefault(normalizeBlank(StrUtil.trim(user.getNickname())),
                    normalizeBlank(StrUtil.trim(user.getUsername())));
        }
        if (StrUtil.isNotBlank(resolvedName) && !Objects.equals(resolvedName, String.valueOf(userId))) {
            return resolvedName;
        }
        String normalizedFallbackName = normalizeBlank(StrUtil.trim(fallbackName));
        if (StrUtil.isNotBlank(normalizedFallbackName)) {
            return normalizedFallbackName;
        }
        if (StrUtil.isNotBlank(resolvedName)) {
            return resolvedName;
        }
        return userId == null ? null : String.valueOf(userId);
    }

    private OperatorSnapshot getOperatorSnapshot(Long userId) {
        AdminUserRespDTO user = adminUserApi.getUser(userId).getCheckedData();
        String deptName = "";
        if (user != null && user.getDeptId() != null) {
            DeptRespDTO dept = deptApi.getDept(user.getDeptId()).getCheckedData();
            deptName = dept == null ? "" : StrUtil.blankToDefault(dept.getName(), "");
        }
        String userName = user == null ? String.valueOf(userId) : StrUtil.blankToDefault(user.getNickname(), user.getUsername());
        return new OperatorSnapshot(userName, deptName);
    }

    private static class OperatorSnapshot {

        private final String userName;
        private final String deptName;

        private OperatorSnapshot(String userName, String deptName) {
            this.userName = userName;
            this.deptName = deptName;
        }

        public String getUserName() {
            return userName;
        }

        public String getDeptName() {
            return deptName;
        }
    }

}
