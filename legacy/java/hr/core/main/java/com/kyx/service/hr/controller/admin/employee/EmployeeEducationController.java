package com.kyx.service.hr.controller.admin.employee;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeEducationRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeEducationSaveReqVO;
import com.kyx.service.hr.service.employee.EmployeeEducationService;
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

@Tag(name = "管理后台 - 员工学历")
@RestController
@RequestMapping("/hr/employee/education")
@Validated
public class EmployeeEducationController {

    @Resource
    private EmployeeEducationService employeeEducationService;

    @GetMapping("/list")
    @Operation(summary = "获得员工学历列表")
    @Parameter(name = "profileId", description = "员工档案ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:employee:query')")
    public CommonResult<List<EmployeeEducationRespVO>> getEducationList(@RequestParam("profileId") Long profileId) {
        return success(employeeEducationService.getEducationList(profileId));
    }

    @PostMapping("/create")
    @Operation(summary = "创建员工学历")
    @PreAuthorize("@ss.hasPermission('hr:employee:update')")
    public CommonResult<Long> createEducation(@Valid @RequestBody EmployeeEducationSaveReqVO createReqVO) {
        return success(employeeEducationService.createEducation(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新员工学历")
    @PreAuthorize("@ss.hasPermission('hr:employee:update')")
    public CommonResult<Boolean> updateEducation(@Valid @RequestBody EmployeeEducationSaveReqVO updateReqVO) {
        employeeEducationService.updateEducation(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除员工学历")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:employee:update')")
    public CommonResult<Boolean> deleteEducation(@RequestParam("id") Long id) {
        employeeEducationService.deleteEducation(id);
        return success(true);
    }
}
