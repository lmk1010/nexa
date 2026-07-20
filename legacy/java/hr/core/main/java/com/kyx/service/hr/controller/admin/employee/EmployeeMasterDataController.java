package com.kyx.service.hr.controller.admin.employee;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.service.hr.controller.admin.employee.vo.master.EmployeeChangeLogRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.master.EmployeeCustomFieldRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.master.EmployeeCustomFieldSaveReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.master.EmployeeCustomFieldValueRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.master.EmployeeCustomFieldValuesSaveReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.master.EmployeeDataQualityRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.master.EmployeeMasterWorkbenchRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.master.EmployeeSavedViewRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.master.EmployeeSavedViewSaveReqVO;
import com.kyx.service.hr.service.employee.EmployeeMasterDataService;
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
import java.util.List;

import static com.kyx.foundation.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 员工主数据中心")
@RestController
@RequestMapping("/hr/employee/master")
@Validated
public class EmployeeMasterDataController {

    @Resource
    private EmployeeMasterDataService employeeMasterDataService;

    @GetMapping("/workbench")
    @Operation(summary = "获得员工主数据工作台")
    @PreAuthorize("@ss.hasPermission('hr:employee-master:query')")
    public CommonResult<EmployeeMasterWorkbenchRespVO> getWorkbench() {
        return success(employeeMasterDataService.getWorkbench());
    }

    @GetMapping("/data-quality")
    @Operation(summary = "获得员工主数据质量报告")
    @PreAuthorize("@ss.hasPermission('hr:employee-master:query')")
    public CommonResult<EmployeeDataQualityRespVO> getDataQuality() {
        return success(employeeMasterDataService.getDataQuality());
    }

    @GetMapping("/custom-field/list")
    @Operation(summary = "获得员工自定义字段列表")
    @PreAuthorize("@ss.hasPermission('hr:employee-master:custom-field')")
    public CommonResult<List<EmployeeCustomFieldRespVO>> getCustomFieldList(
            @RequestParam(value = "status", required = false) Integer status) {
        return success(employeeMasterDataService.getCustomFieldList(status));
    }

    @PostMapping("/custom-field/create")
    @Operation(summary = "创建员工自定义字段")
    @PreAuthorize("@ss.hasPermission('hr:employee-master:custom-field')")
    public CommonResult<Long> createCustomField(@Valid @RequestBody EmployeeCustomFieldSaveReqVO reqVO) {
        return success(employeeMasterDataService.createCustomField(reqVO));
    }

    @PutMapping("/custom-field/update")
    @Operation(summary = "更新员工自定义字段")
    @PreAuthorize("@ss.hasPermission('hr:employee-master:custom-field')")
    public CommonResult<Boolean> updateCustomField(@Valid @RequestBody EmployeeCustomFieldSaveReqVO reqVO) {
        employeeMasterDataService.updateCustomField(reqVO);
        return success(true);
    }

    @DeleteMapping("/custom-field/delete")
    @Operation(summary = "删除员工自定义字段")
    @Parameter(name = "id", description = "字段 ID", required = true)
    @PreAuthorize("@ss.hasPermission('hr:employee-master:custom-field')")
    public CommonResult<Boolean> deleteCustomField(@RequestParam("id") Long id) {
        employeeMasterDataService.deleteCustomField(id);
        return success(true);
    }

    @GetMapping("/custom-field/value/list")
    @Operation(summary = "获得员工自定义字段值")
    @Parameter(name = "profileId", description = "员工档案 ID", required = true)
    @PreAuthorize("@ss.hasPermission('hr:employee:query')")
    public CommonResult<List<EmployeeCustomFieldValueRespVO>> getCustomFieldValues(@RequestParam("profileId") Long profileId) {
        return success(employeeMasterDataService.getCustomFieldValues(profileId));
    }

    @PostMapping("/custom-field/value/save")
    @Operation(summary = "保存员工自定义字段值")
    @PreAuthorize("@ss.hasPermission('hr:employee:update')")
    public CommonResult<Boolean> saveCustomFieldValues(@Valid @RequestBody EmployeeCustomFieldValuesSaveReqVO reqVO) {
        employeeMasterDataService.saveCustomFieldValues(reqVO);
        return success(true);
    }

    @GetMapping("/saved-view/list")
    @Operation(summary = "获得我的花名册保存视图")
    @PreAuthorize("@ss.hasPermission('hr:employee:query')")
    public CommonResult<List<EmployeeSavedViewRespVO>> getMySavedViews() {
        return success(employeeMasterDataService.getMySavedViews());
    }

    @PostMapping("/saved-view/save")
    @Operation(summary = "保存我的花名册视图")
    @PreAuthorize("@ss.hasPermission('hr:employee:query')")
    public CommonResult<Long> saveMySavedView(@Valid @RequestBody EmployeeSavedViewSaveReqVO reqVO) {
        return success(employeeMasterDataService.saveMySavedView(reqVO));
    }

    @DeleteMapping("/saved-view/delete")
    @Operation(summary = "删除我的花名册视图")
    @Parameter(name = "id", description = "视图 ID", required = true)
    @PreAuthorize("@ss.hasPermission('hr:employee:query')")
    public CommonResult<Boolean> deleteMySavedView(@RequestParam("id") Long id) {
        employeeMasterDataService.deleteMySavedView(id);
        return success(true);
    }

    @PutMapping("/saved-view/default")
    @Operation(summary = "设置我的默认花名册视图")
    @Parameter(name = "id", description = "视图 ID", required = true)
    @PreAuthorize("@ss.hasPermission('hr:employee:query')")
    public CommonResult<Boolean> setMyDefaultSavedView(@RequestParam("id") Long id) {
        employeeMasterDataService.setMyDefaultSavedView(id);
        return success(true);
    }

    @GetMapping("/change-log/list")
    @Operation(summary = "获得员工字段变更日志")
    @Parameter(name = "profileId", description = "员工档案 ID", required = true)
    @PreAuthorize("@ss.hasPermission('hr:employee:query')")
    public CommonResult<List<EmployeeChangeLogRespVO>> getChangeLogs(@RequestParam("profileId") Long profileId) {
        return success(employeeMasterDataService.getChangeLogs(profileId));
    }

}
