package com.kyx.service.hr.controller.admin.exam;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.security.core.service.SecurityFrameworkService;
import com.kyx.service.business.enums.permission.RoleCodeEnum;
import com.kyx.service.hr.controller.admin.exam.vo.ExamPublishPageReqVO;
import com.kyx.service.hr.controller.admin.exam.vo.ExamPublishRespVO;
import com.kyx.service.hr.controller.admin.exam.vo.ExamPublishSaveReqVO;
import com.kyx.service.hr.controller.admin.exam.vo.scope.ExamPublishScopeOptionsRespVO;
import com.kyx.service.hr.service.exam.ExamPublishScopeService;
import com.kyx.service.hr.service.exam.ExamPublishService;
import com.kyx.service.hr.service.exam.ExamViewScopeSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

import static com.kyx.foundation.common.pojo.CommonResult.success;
import static com.kyx.foundation.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * HR 考试发布 Controller
 *
 * @author MK
 */
@Tag(name = "管理后台 - HR 考试发布")
@RestController
@RequestMapping("/hr/exam-publish")
@Validated
public class ExamPublishController {

    @Resource
    private ExamPublishService publishService;
    @Resource
    private SecurityFrameworkService securityFrameworkService;
    @Resource
    private ExamViewScopeSupport examViewScopeSupport;
    @Resource
    private ExamPublishScopeService examPublishScopeService;

    @PostMapping("/create")
    @Operation(summary = "创建考试发布")
    @PreAuthorize("@ss.hasPermission('hr:exam:create')")
    public CommonResult<Long> createPublish(@Valid @RequestBody ExamPublishSaveReqVO reqVO) {
        return success(publishService.createPublish(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新考试发布")
    @PreAuthorize("@ss.hasPermission('hr:exam:update')")
    public CommonResult<Boolean> updatePublish(@Valid @RequestBody ExamPublishSaveReqVO reqVO) {
        publishService.updatePublish(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除考试发布")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('hr:exam:delete')")
    public CommonResult<Boolean> deletePublish(@RequestParam("id") Long id) {
        publishService.deletePublish(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得考试发布详情")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('hr:exam:query')")
    public CommonResult<ExamPublishRespVO> getPublish(@RequestParam("id") Long id) {
        return success(publishService.getPublish(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得考试发布分页")
    @PreAuthorize("@ss.hasAnyPermissions('hr:exam:query,hr:exam:attempt')")
    public CommonResult<PageResult<ExamPublishRespVO>> getPublishPage(@Valid ExamPublishPageReqVO pageVO) {
        boolean canQueryAll = securityFrameworkService.hasPermission("hr:exam:query");
        if (Boolean.TRUE.equals(pageVO.getMine()) || !canQueryAll) {
            pageVO.setMine(true);
            pageVO.setCurrentUserId(getLoginUserId());
            pageVO.setCreator(null);
        } else if (!canViewAllExamData()) {
            pageVO.setCreator(String.valueOf(getLoginUserId()));
        }
        return success(publishService.getPublishPage(pageVO));
    }

    @GetMapping("/scope-options")
    @Operation(summary = "获得考试发布范围选项")
    @PreAuthorize("@ss.hasAnyPermissions('hr:exam:create,hr:exam:update,hr:exam:query')")
    public CommonResult<ExamPublishScopeOptionsRespVO> getScopeOptions() {
        return success(examPublishScopeService.getScopeOptions());
    }

    @PutMapping("/pause")
    @Operation(summary = "暂停定期考核")
    @Parameter(name = "id", description = "发布ID", required = true)
    @PreAuthorize("@ss.hasPermission('hr:exam:update')")
    public CommonResult<Boolean> pauseRecurring(@RequestParam("id") Long id) {
        publishService.pauseRecurring(id);
        return success(true);
    }

    @PutMapping("/resume")
    @Operation(summary = "恢复定期考核")
    @Parameter(name = "id", description = "发布ID", required = true)
    @PreAuthorize("@ss.hasPermission('hr:exam:update')")
    public CommonResult<Boolean> resumeRecurring(@RequestParam("id") Long id) {
        publishService.resumeRecurring(id);
        return success(true);
    }

    @PutMapping("/close")
    @Operation(summary = "结束考试发布")
    @Parameter(name = "id", description = "发布ID", required = true)
    @PreAuthorize("@ss.hasPermission('hr:exam:update')")
    public CommonResult<Boolean> closePublish(@RequestParam("id") Long id) {
        publishService.closePublish(id);
        return success(true);
    }

    private boolean canViewAllExamData() {
        return securityFrameworkService.hasRole(RoleCodeEnum.SUPER_ADMIN.getCode())
                || securityFrameworkService.hasRole(RoleCodeEnum.TENANT_ADMIN.getCode())
                || examViewScopeSupport.canViewAllData();
    }
}
