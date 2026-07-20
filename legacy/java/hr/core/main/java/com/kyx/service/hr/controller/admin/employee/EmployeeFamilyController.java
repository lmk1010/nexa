package com.kyx.service.hr.controller.admin.employee;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeFamilyRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeFamilySaveReqVO;
import com.kyx.service.hr.service.employee.EmployeeFamilyService;
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

@Tag(name = "管理后台 - 员工家庭信息")
@RestController
@RequestMapping("/hr/employee/family")
@Validated
public class EmployeeFamilyController {

    @Resource
    private EmployeeFamilyService employeeFamilyService;

    @GetMapping("/list")
    @Operation(summary = "获得员工家庭信息列表")
    @Parameter(name = "profileId", description = "员工档案ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:employee:query')")
    public CommonResult<List<EmployeeFamilyRespVO>> getFamilyList(@RequestParam("profileId") Long profileId) {
        return success(employeeFamilyService.getFamilyList(profileId));
    }

    @PostMapping("/create")
    @Operation(summary = "创建员工家庭信息")
    @PreAuthorize("@ss.hasPermission('hr:employee:update')")
    public CommonResult<Long> createFamily(@Valid @RequestBody EmployeeFamilySaveReqVO createReqVO) {
        return success(employeeFamilyService.createFamily(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新员工家庭信息")
    @PreAuthorize("@ss.hasPermission('hr:employee:update')")
    public CommonResult<Boolean> updateFamily(@Valid @RequestBody EmployeeFamilySaveReqVO updateReqVO) {
        employeeFamilyService.updateFamily(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除员工家庭信息")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:employee:update')")
    public CommonResult<Boolean> deleteFamily(@RequestParam("id") Long id) {
        employeeFamilyService.deleteFamily(id);
        return success(true);
    }
}
