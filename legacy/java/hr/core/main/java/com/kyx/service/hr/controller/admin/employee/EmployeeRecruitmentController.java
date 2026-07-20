package com.kyx.service.hr.controller.admin.employee;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeRecruitmentConvertEntryReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeRecruitmentPageReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeRecruitmentPublicLinkRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeRecruitmentPublicLinkSaveReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeRecruitmentRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeRecruitmentSaveReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeRecruitmentStatsRespVO;
import com.kyx.service.hr.service.employee.EmployeeRecruitmentService;
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

@Tag(name = "管理后台 - 员工招聘信息")
@RestController
@RequestMapping("/hr/employee/recruitment")
@Validated
public class EmployeeRecruitmentController {

    @Resource
    private EmployeeRecruitmentService employeeRecruitmentService;

    @GetMapping("/list")
    @Operation(summary = "获得员工招聘信息列表")
    @Parameter(name = "profileId", description = "员工档案ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:employee:query')")
    public CommonResult<List<EmployeeRecruitmentRespVO>> getRecruitmentList(@RequestParam("profileId") Long profileId) {
        return success(employeeRecruitmentService.getRecruitmentList(profileId));
    }

    @GetMapping("/page")
    @Operation(summary = "获得招聘工作台分页")
    @PreAuthorize("@ss.hasAnyPermissions('hr:recruitment:query,hr:employee:query')")
    public CommonResult<PageResult<EmployeeRecruitmentRespVO>> getRecruitmentPage(
            @Valid EmployeeRecruitmentPageReqVO pageReqVO) {
        return success(employeeRecruitmentService.getRecruitmentPage(pageReqVO));
    }

    @GetMapping("/stats")
    @Operation(summary = "获得招聘工作台统计")
    @PreAuthorize("@ss.hasAnyPermissions('hr:recruitment:query,hr:employee:query')")
    public CommonResult<EmployeeRecruitmentStatsRespVO> getRecruitmentStats(
            @Valid EmployeeRecruitmentPageReqVO pageReqVO) {
        return success(employeeRecruitmentService.getRecruitmentStats(pageReqVO));
    }

    @PostMapping("/create")
    @Operation(summary = "创建员工招聘信息")
    @PreAuthorize("@ss.hasAnyPermissions('hr:recruitment:manage,hr:employee:update')")
    public CommonResult<Long> createRecruitment(@Valid @RequestBody EmployeeRecruitmentSaveReqVO createReqVO) {
        return success(employeeRecruitmentService.createRecruitment(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新员工招聘信息")
    @PreAuthorize("@ss.hasAnyPermissions('hr:recruitment:manage,hr:employee:update')")
    public CommonResult<Boolean> updateRecruitment(@Valid @RequestBody EmployeeRecruitmentSaveReqVO updateReqVO) {
        employeeRecruitmentService.updateRecruitment(updateReqVO);
        return success(true);
    }

    @PostMapping("/demand/submit-approval")
    @Operation(summary = "提交招聘需求审批")
    @Parameter(name = "id", description = "招聘记录编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasAnyPermissions('hr:recruitment:manage,hr:employee:update')")
    public CommonResult<String> submitDemandApproval(@RequestParam("id") Long id) {
        return success(employeeRecruitmentService.submitDemandApproval(id));
    }

    @PostMapping("/offer/submit-approval")
    @Operation(summary = "提交 Offer 审批")
    @Parameter(name = "id", description = "招聘记录编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasAnyPermissions('hr:recruitment:manage,hr:employee:update')")
    public CommonResult<String> submitOfferApproval(@RequestParam("id") Long id) {
        return success(employeeRecruitmentService.submitOfferApproval(id));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除员工招聘信息")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasAnyPermissions('hr:recruitment:manage,hr:employee:update')")
    public CommonResult<Boolean> deleteRecruitment(@RequestParam("id") Long id) {
        employeeRecruitmentService.deleteRecruitment(id);
        return success(true);
    }

    @PostMapping("/parse-resume")
    @Operation(summary = "解析招聘候选人简历")
    @Parameter(name = "id", description = "招聘记录编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasAnyPermissions('hr:recruitment:manage,hr:employee:update')")
    public CommonResult<EmployeeRecruitmentRespVO> parseResume(@RequestParam("id") Long id) {
        return success(employeeRecruitmentService.parseResume(id));
    }

    @PostMapping("/convert-entry")
    @Operation(summary = "招聘候选人转入职")
    @PreAuthorize("@ss.hasAnyPermissions('hr:recruitment:manage,hr:employee-entry:create,hr:employee:update')")
    public CommonResult<Long> convertRecruitmentToEntry(
            @Valid @RequestBody EmployeeRecruitmentConvertEntryReqVO convertReqVO) {
        return success(employeeRecruitmentService.convertToEntry(convertReqVO));
    }

    @PostMapping("/public-link/create")
    @Operation(summary = "创建招聘公开投递链接")
    @PreAuthorize("@ss.hasAnyPermissions('hr:recruitment:manage,hr:employee:update')")
    public CommonResult<EmployeeRecruitmentPublicLinkRespVO> createPublicLink(
            @Valid @RequestBody EmployeeRecruitmentPublicLinkSaveReqVO createReqVO) {
        return success(employeeRecruitmentService.createPublicLink(createReqVO));
    }
}
