package com.kyx.service.biz.controller.admin.work;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.biz.controller.admin.work.vo.WorkRequirementActionReqVO;
import com.kyx.service.biz.controller.admin.work.vo.WorkRequirementAssignReqVO;
import com.kyx.service.biz.controller.admin.work.vo.WorkRequirementCommentCreateReqVO;
import com.kyx.service.biz.controller.admin.work.vo.WorkRequirementCommentRespVO;
import com.kyx.service.biz.controller.admin.work.vo.WorkRequirementLogRespVO;
import com.kyx.service.biz.controller.admin.work.vo.WorkRequirementOverviewRespVO;
import com.kyx.service.biz.controller.admin.work.vo.WorkRequirementPageReqVO;
import com.kyx.service.biz.controller.admin.work.vo.WorkRequirementRateRespVO;
import com.kyx.service.biz.controller.admin.work.vo.WorkRequirementRateSaveReqVO;
import com.kyx.service.biz.controller.admin.work.vo.WorkRequirementRespVO;
import com.kyx.service.biz.controller.admin.work.vo.WorkRequirementSaveReqVO;
import com.kyx.service.biz.controller.admin.work.vo.WorkRequirementScopeOptionsRespVO;
import com.kyx.service.biz.dal.dataobject.work.WorkRequirementDO;
import com.kyx.service.biz.service.work.WorkRequirementService;
import com.kyx.service.biz.service.work.WorkRequirementTenantScopeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static com.kyx.foundation.common.pojo.CommonResult.success;
import static com.kyx.foundation.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "Admin - Work Requirement")
@RestController
@RequestMapping("/business/work/requirement")
@Validated
public class WorkRequirementController {

    private static final String REQUIREMENT_ADMIN_ROLE_CHECK =
            "@ss.hasAnyRoles('super_admin', 'tenant_admin', 'system_admin')";

    @Resource
    private WorkRequirementService workRequirementService;
    @Resource
    private WorkRequirementTenantScopeService workRequirementTenantScopeService;

    @PostMapping("/create")
    @Operation(summary = "Create requirement")
    public CommonResult<Long> createRequirement(@Valid @RequestBody WorkRequirementSaveReqVO createReqVO) {
        return success(workRequirementService.createRequirement(createReqVO, getLoginUserId()));
    }

    @PutMapping("/update")
    @Operation(summary = "Update requirement")
    @PreAuthorize(REQUIREMENT_ADMIN_ROLE_CHECK + " || @ss.hasPermission('work:requirement:update')")
    public CommonResult<Boolean> updateRequirement(@Valid @RequestBody WorkRequirementSaveReqVO updateReqVO) {
        workRequirementService.updateRequirement(updateReqVO, getLoginUserId());
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "Delete requirement")
    @Parameter(name = "id", required = true, example = "1")
    public CommonResult<Boolean> deleteRequirement(@RequestParam("id") Long id) {
        workRequirementService.deleteRequirement(id, getLoginUserId());
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "Get requirement")
    @Parameter(name = "id", required = true, example = "1")
    public CommonResult<WorkRequirementRespVO> getRequirement(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(workRequirementService.getRequirement(id, getLoginUserId()), WorkRequirementRespVO.class));
    }

    @GetMapping("/children")
    @Operation(summary = "Get requirement children")
    @Parameter(name = "parentId", required = true, example = "1")
    public CommonResult<List<WorkRequirementRespVO>> getRequirementChildren(@RequestParam("parentId") Long parentId,
                                                                            @Valid WorkRequirementPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(workRequirementService.getRequirementChildren(parentId, pageReqVO, getLoginUserId()), WorkRequirementRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "Get requirement page")
    public CommonResult<PageResult<WorkRequirementRespVO>> getRequirementPage(@Valid WorkRequirementPageReqVO pageReqVO) {
        PageResult<WorkRequirementDO> pageResult = workRequirementService.getRequirementPage(pageReqVO, getLoginUserId());
        return success(BeanUtils.toBean(pageResult, WorkRequirementRespVO.class));
    }

    @GetMapping("/overview")
    @Operation(summary = "Get requirement overview")
    public CommonResult<WorkRequirementOverviewRespVO> getRequirementOverview(@Valid WorkRequirementPageReqVO pageReqVO) {
        return success(workRequirementService.getRequirementOverview(pageReqVO, getLoginUserId()));
    }

    @GetMapping("/todo-approval-process-instance-ids")
    @Operation(summary = "Get current user's requirement approval process instance ids")
    public CommonResult<List<String>> getTodoApprovalProcessInstanceIds() {
        return success(workRequirementService.getTodoApprovalProcessInstanceIds(getLoginUserId()));
    }

    @GetMapping("/scope-options")
    @Operation(summary = "Get requirement scope options")
    public CommonResult<WorkRequirementScopeOptionsRespVO> getScopeOptions() {
        return success(workRequirementTenantScopeService.getScopeOptions());
    }

    @GetMapping("/logs")
    @Operation(summary = "Get requirement logs")
    public CommonResult<List<WorkRequirementLogRespVO>> getRequirementLogs(@RequestParam("requirementId") Long requirementId) {
        return success(BeanUtils.toBean(workRequirementService.getRequirementLogs(requirementId, getLoginUserId()), WorkRequirementLogRespVO.class));
    }

    @GetMapping("/comment/list")
    @Operation(summary = "Get requirement comments")
    public CommonResult<List<WorkRequirementCommentRespVO>> getRequirementComments(@RequestParam("requirementId") Long requirementId) {
        return success(BeanUtils.toBean(workRequirementService.getRequirementComments(requirementId, getLoginUserId()), WorkRequirementCommentRespVO.class));
    }

    @PostMapping("/comment/create")
    @Operation(summary = "Create requirement comment")
    public CommonResult<Long> createRequirementComment(@Valid @RequestBody WorkRequirementCommentCreateReqVO reqVO) {
        return success(workRequirementService.createRequirementComment(reqVO, getLoginUserId()));
    }

    @PutMapping("/comment/read-all")
    @Operation(summary = "Read all requirement comments")
    public CommonResult<Boolean> readAllRequirementComments(@RequestParam("requirementId") Long requirementId) {
        workRequirementService.readAllRequirementComments(requirementId, getLoginUserId());
        return success(true);
    }

    @PutMapping("/comment/read-all-mine")
    @Operation(summary = "Read all current user's requirement comments")
    public CommonResult<Boolean> readAllMyRequirementComments() {
        workRequirementService.readAllMyRequirementComments(getLoginUserId());
        return success(true);
    }

    @GetMapping("/rate/list")
    @Operation(summary = "Get requirement rates")
    public CommonResult<List<WorkRequirementRateRespVO>> getRequirementRates(@RequestParam("requirementId") Long requirementId) {
        return success(BeanUtils.toBean(workRequirementService.getRequirementRates(requirementId, getLoginUserId()), WorkRequirementRateRespVO.class));
    }

    @PostMapping("/rate/save")
    @Operation(summary = "Save requirement rate")
    @PreAuthorize(REQUIREMENT_ADMIN_ROLE_CHECK + " || @ss.hasPermission('work:requirement:update')")
    public CommonResult<Long> saveRequirementRate(@Valid @RequestBody WorkRequirementRateSaveReqVO reqVO) {
        return success(workRequirementService.saveRequirementRate(reqVO, getLoginUserId()));
    }

    @PutMapping("/assign")
    @Operation(summary = "Assign requirement")
    @PreAuthorize(REQUIREMENT_ADMIN_ROLE_CHECK + " || @ss.hasPermission('work:requirement:assign') || @ss.hasPermission('bpm:task:update')")
    public CommonResult<Boolean> assignRequirement(@Valid @RequestBody WorkRequirementAssignReqVO reqVO) {
        workRequirementService.assignRequirement(reqVO, getLoginUserId());
        return success(true);
    }

    @PutMapping("/transfer-assign")
    @Operation(summary = "Transfer requirement assignee")
    @PreAuthorize(REQUIREMENT_ADMIN_ROLE_CHECK + " || @ss.hasPermission('work:requirement:startDev')")
    public CommonResult<Boolean> transferAssignRequirement(@Valid @RequestBody WorkRequirementAssignReqVO reqVO) {
        workRequirementService.transferAssignRequirement(reqVO, getLoginUserId());
        return success(true);
    }

    @PutMapping("/dev-reject")
    @Operation(summary = "Developer reject or ask")
    @PreAuthorize(REQUIREMENT_ADMIN_ROLE_CHECK + " || @ss.hasPermission('work:requirement:startDev')")
    public CommonResult<Boolean> devReject(@Valid @RequestBody WorkRequirementActionReqVO reqVO) {
        workRequirementService.devReject(reqVO, getLoginUserId());
        return success(true);
    }

    @PutMapping("/start-dev")
    @Operation(summary = "Start development")
    @PreAuthorize(REQUIREMENT_ADMIN_ROLE_CHECK + " || @ss.hasPermission('work:requirement:startDev')")
    public CommonResult<Boolean> startDev(@Valid @RequestBody WorkRequirementActionReqVO reqVO) {
        workRequirementService.startDev(reqVO.getId(), getLoginUserId());
        return success(true);
    }

    @PutMapping("/submit-test")
    @Operation(summary = "Submit test")
    @PreAuthorize(REQUIREMENT_ADMIN_ROLE_CHECK + " || @ss.hasPermission('work:requirement:submitTest')")
    public CommonResult<Boolean> submitTest(@Valid @RequestBody WorkRequirementActionReqVO reqVO) {
        workRequirementService.submitTest(reqVO.getId(), getLoginUserId());
        return success(true);
    }

    @PutMapping("/test-pass")
    @Operation(summary = "Test pass")
    @PreAuthorize(REQUIREMENT_ADMIN_ROLE_CHECK + " || @ss.hasPermission('work:requirement:testPass')")
    public CommonResult<Boolean> testPass(@Valid @RequestBody WorkRequirementActionReqVO reqVO) {
        workRequirementService.testPass(reqVO.getId(), getLoginUserId());
        return success(true);
    }

    @PutMapping("/test-reject")
    @Operation(summary = "Test reject")
    @PreAuthorize(REQUIREMENT_ADMIN_ROLE_CHECK + " || @ss.hasPermission('work:requirement:testReject')")
    public CommonResult<Boolean> testReject(@Valid @RequestBody WorkRequirementActionReqVO reqVO) {
        workRequirementService.testReject(reqVO, getLoginUserId());
        return success(true);
    }

    @PutMapping("/accept-pass")
    @Operation(summary = "Accept pass")
    @PreAuthorize(REQUIREMENT_ADMIN_ROLE_CHECK + " || @ss.hasPermission('work:requirement:acceptPass')")
    public CommonResult<Boolean> acceptPass(@Valid @RequestBody WorkRequirementActionReqVO reqVO) {
        workRequirementService.acceptPass(reqVO, getLoginUserId());
        return success(true);
    }

    @PutMapping("/accept-reject")
    @Operation(summary = "Accept reject")
    @PreAuthorize(REQUIREMENT_ADMIN_ROLE_CHECK + " || @ss.hasPermission('work:requirement:acceptReject')")
    public CommonResult<Boolean> acceptReject(@Valid @RequestBody WorkRequirementActionReqVO reqVO) {
        workRequirementService.acceptReject(reqVO, getLoginUserId());
        return success(true);
    }

    @PutMapping("/submit-approval")
    @Operation(summary = "Submit requirement approval")
    @PreAuthorize(REQUIREMENT_ADMIN_ROLE_CHECK + " || @ss.hasPermission('work:requirement:submitApproval')")
    public CommonResult<Boolean> submitRequirementApproval(@Valid @RequestBody WorkRequirementActionReqVO reqVO) {
        workRequirementService.submitRequirementApproval(reqVO, getLoginUserId());
        return success(true);
    }

    @PutMapping("/cancel")
    @Operation(summary = "Cancel requirement")
    @PreAuthorize(REQUIREMENT_ADMIN_ROLE_CHECK + " || @ss.hasPermission('work:requirement:update') || @ss.hasPermission('work:requirement:query')")
    public CommonResult<Boolean> cancelRequirement(@Valid @RequestBody WorkRequirementActionReqVO reqVO) {
        workRequirementService.cancelRequirement(reqVO, getLoginUserId());
        return success(true);
    }

    @PutMapping("/suspend")
    @Operation(summary = "Suspend requirement")
    @PreAuthorize(REQUIREMENT_ADMIN_ROLE_CHECK + " || @ss.hasPermission('work:requirement:update') || @ss.hasPermission('work:requirement:query')")
    public CommonResult<Boolean> suspendRequirement(@Valid @RequestBody WorkRequirementActionReqVO reqVO) {
        workRequirementService.suspendRequirement(reqVO, getLoginUserId());
        return success(true);
    }

    @PutMapping("/reopen")
    @Operation(summary = "Reopen requirement")
    @PreAuthorize(REQUIREMENT_ADMIN_ROLE_CHECK + " || @ss.hasPermission('work:requirement:update') || @ss.hasPermission('work:requirement:query')")
    public CommonResult<Boolean> reopenRequirement(@Valid @RequestBody WorkRequirementActionReqVO reqVO) {
        workRequirementService.reopenRequirement(reqVO, getLoginUserId());
        return success(true);
    }

}
