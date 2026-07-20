package com.kyx.service.hr.controller.admin.employee;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeSalaryRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeSalarySaveReqVO;
import com.kyx.service.hr.service.employee.EmployeeSalaryService;
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

@Tag(name = "管理后台 - 员工薪酬信息")
@RestController
@RequestMapping("/hr/employee/salary")
@Validated
public class EmployeeSalaryController {

    @Resource
    private EmployeeSalaryService employeeSalaryService;

    @GetMapping("/list")
    @Operation(summary = "获得员工薪酬信息列表")
    @Parameter(name = "profileId", description = "员工档案ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:employee:query')")
    public CommonResult<List<EmployeeSalaryRespVO>> getSalaryList(@RequestParam("profileId") Long profileId) {
        return success(employeeSalaryService.getSalaryList(profileId));
    }

    @PostMapping("/create")
    @Operation(summary = "创建员工薪酬信息")
    @PreAuthorize("@ss.hasPermission('hr:employee:update')")
    public CommonResult<Long> createSalary(@Valid @RequestBody EmployeeSalarySaveReqVO createReqVO) {
        return success(employeeSalaryService.createSalary(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新员工薪酬信息")
    @PreAuthorize("@ss.hasPermission('hr:employee:update')")
    public CommonResult<Boolean> updateSalary(@Valid @RequestBody EmployeeSalarySaveReqVO updateReqVO) {
        employeeSalaryService.updateSalary(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除员工薪酬信息")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:employee:update')")
    public CommonResult<Boolean> deleteSalary(@RequestParam("id") Long id) {
        employeeSalaryService.deleteSalary(id);
        return success(true);
    }
}
