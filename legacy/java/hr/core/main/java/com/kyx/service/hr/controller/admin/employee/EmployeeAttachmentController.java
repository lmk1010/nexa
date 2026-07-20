package com.kyx.service.hr.controller.admin.employee;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeAttachmentRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeAttachmentSaveReqVO;
import com.kyx.service.hr.service.employee.EmployeeAttachmentService;
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

@Tag(name = "管理后台 - 员工附件")
@RestController
@RequestMapping("/hr/employee/attachment")
@Validated
public class EmployeeAttachmentController {

    @Resource
    private EmployeeAttachmentService employeeAttachmentService;

    @GetMapping("/list")
    @Operation(summary = "获得员工附件列表")
    @Parameter(name = "profileId", description = "员工档案ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:employee:query')")
    public CommonResult<List<EmployeeAttachmentRespVO>> getAttachmentList(@RequestParam("profileId") Long profileId) {
        return success(employeeAttachmentService.getAttachmentList(profileId));
    }

    @PostMapping("/create")
    @Operation(summary = "创建员工附件")
    @PreAuthorize("@ss.hasPermission('hr:employee:update')")
    public CommonResult<Long> createAttachment(@Valid @RequestBody EmployeeAttachmentSaveReqVO createReqVO) {
        return success(employeeAttachmentService.createAttachment(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新员工附件")
    @PreAuthorize("@ss.hasPermission('hr:employee:update')")
    public CommonResult<Boolean> updateAttachment(@Valid @RequestBody EmployeeAttachmentSaveReqVO updateReqVO) {
        employeeAttachmentService.updateAttachment(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除员工附件")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:employee:update')")
    public CommonResult<Boolean> deleteAttachment(@RequestParam("id") Long id) {
        employeeAttachmentService.deleteAttachment(id);
        return success(true);
    }
}
