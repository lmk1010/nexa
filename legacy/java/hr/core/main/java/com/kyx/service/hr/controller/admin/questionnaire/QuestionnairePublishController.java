package com.kyx.service.hr.controller.admin.questionnaire;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnairePublishAddAssigneesReqVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnairePublishAddAssigneesRespVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnairePublishBatchRespVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnairePublishPageReqVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnairePublishRespVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnairePublishSaveReqVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnairePublishScopePreviewRespVO;
import com.kyx.service.hr.service.questionnaire.QuestionnairePublishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static com.kyx.foundation.common.pojo.CommonResult.success;
import static com.kyx.foundation.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * HR 问卷发布 Controller
 *
 * @author MK
 */
@Tag(name = "管理后台 - HR 问卷发布")
@RestController
@RequestMapping("/hr/questionnaire-publish")
@Validated
public class QuestionnairePublishController {

    @Resource
    private QuestionnairePublishService publishService;

    @PostMapping("/create")
    @Operation(summary = "创建发布")
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:publish:create')")
    public CommonResult<Long> createPublish(@Valid @RequestBody QuestionnairePublishSaveReqVO createReqVO) {
        return success(publishService.createPublish(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新发布")
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:publish:update')")
    public CommonResult<Boolean> updatePublish(@Valid @RequestBody QuestionnairePublishSaveReqVO updateReqVO) {
        publishService.updatePublish(updateReqVO);
        return success(true);
    }

    @PostMapping("/add-assignees")
    @Operation(summary = "追加填写人")
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:publish:update')")
    public CommonResult<QuestionnairePublishAddAssigneesRespVO> addAssignees(
            @Valid @RequestBody QuestionnairePublishAddAssigneesReqVO reqVO) {
        return success(publishService.addAssignees(reqVO));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除发布")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:publish:delete')")
    public CommonResult<Boolean> deletePublish(@RequestParam("id") Long id) {
        publishService.deletePublish(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得发布详情")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:publish:query')")
    public CommonResult<QuestionnairePublishRespVO> getPublish(@RequestParam("id") Long id) {
        return success(publishService.getPublish(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得发布分页")
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:publish:query')")
    public CommonResult<PageResult<QuestionnairePublishRespVO>> getPublishPage(@Valid QuestionnairePublishPageReqVO pageVO) {
        return success(publishService.getPublishPage(pageVO));
    }

    @GetMapping("/list")
    @Operation(summary = "获得发布列表")
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:publish:query')")
    public CommonResult<java.util.List<QuestionnairePublishRespVO>> getPublishList(
            @RequestParam(value = "status", required = false) Integer status) {
        return success(publishService.getPublishList(status));
    }

    @GetMapping("/my-list")
    @Operation(summary = "获得我的发布列表")
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:publish:query')")
    public CommonResult<java.util.List<QuestionnairePublishRespVO>> getMyPublishList(
            @RequestParam(value = "status", required = false) Integer status) {
        return success(publishService.getMyPublishList(getLoginUserId(), status));
    }

    @GetMapping("/batch-list")
    @Operation(summary = "获得发布批次列表")
    @Parameter(name = "publishId", description = "发布ID", required = true)
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:publish:query')")
    public CommonResult<java.util.List<QuestionnairePublishBatchRespVO>> getPublishBatchList(
            @RequestParam("publishId") Long publishId) {
        return success(publishService.getPublishBatchList(publishId));
    }

    @PutMapping("/finish-batch")
    @Operation(summary = "手动结束批次")
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:publish:update')")
    public CommonResult<Boolean> finishBatch(
            @RequestParam("publishId") Long publishId,
            @RequestParam(value = "batchNo", required = false) Integer batchNo) {
        publishService.finishBatch(publishId, batchNo);
        return success(true);
    }

    @PutMapping("/enable-batch")
    @Operation(summary = "手动启用批次")
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:publish:update')")
    public CommonResult<Boolean> enableBatch(
            @RequestParam("publishId") Long publishId,
            @RequestParam(value = "batchNo", required = false) Integer batchNo) {
        publishService.enableBatch(publishId, batchNo);
        return success(true);
    }

    @PostMapping("/preview-scope-count")
    @Operation(summary = "预览发布范围人数")
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:publish:query')")
    public CommonResult<Integer> previewScopeCount(@RequestBody java.util.Map<String, String> body) {
        String scopeJson = body.get("publishScopeJson");
        return success(publishService.previewScopeUserCount(scopeJson));
    }

    @PostMapping("/preview-scope-users")
    @Operation(summary = "预览发布范围人员明细")
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:publish:query')")
    public CommonResult<QuestionnairePublishScopePreviewRespVO> previewScopeUsers(
            @RequestBody java.util.Map<String, String> body) {
        String scopeJson = body.get("publishScopeJson");
        return success(publishService.previewScopeUsers(scopeJson));
    }

}
