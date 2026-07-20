package com.kyx.service.hr.controller.admin.employee;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeInventoryRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeInventorySaveReqVO;
import com.kyx.service.hr.service.employee.EmployeeInventoryService;
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

@Tag(name = "管理后台 - 员工盘点信息")
@RestController
@RequestMapping("/hr/employee/inventory")
@Validated
public class EmployeeInventoryController {

    @Resource
    private EmployeeInventoryService employeeInventoryService;

    @GetMapping("/list")
    @Operation(summary = "获得员工盘点信息列表")
    @Parameter(name = "profileId", description = "员工档案ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:employee:query')")
    public CommonResult<List<EmployeeInventoryRespVO>> getInventoryList(@RequestParam("profileId") Long profileId) {
        return success(employeeInventoryService.getInventoryList(profileId));
    }

    @PostMapping("/create")
    @Operation(summary = "创建员工盘点信息")
    @PreAuthorize("@ss.hasPermission('hr:employee:update')")
    public CommonResult<Long> createInventory(@Valid @RequestBody EmployeeInventorySaveReqVO createReqVO) {
        return success(employeeInventoryService.createInventory(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新员工盘点信息")
    @PreAuthorize("@ss.hasPermission('hr:employee:update')")
    public CommonResult<Boolean> updateInventory(@Valid @RequestBody EmployeeInventorySaveReqVO updateReqVO) {
        employeeInventoryService.updateInventory(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除员工盘点信息")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:employee:update')")
    public CommonResult<Boolean> deleteInventory(@RequestParam("id") Long id) {
        employeeInventoryService.deleteInventory(id);
        return success(true);
    }
}
