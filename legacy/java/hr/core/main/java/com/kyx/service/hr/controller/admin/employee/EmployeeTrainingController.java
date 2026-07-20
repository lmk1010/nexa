package com.kyx.service.hr.controller.admin.employee;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeTrainingPageReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeTrainingRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeTrainingSaveReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeTrainingStatsRespVO;
import com.kyx.service.hr.service.employee.EmployeeTrainingService;
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

@Tag(name = "管理后台 - 员工培训信息")
@RestController
@RequestMapping("/hr/employee/training")
@Validated
public class EmployeeTrainingController {

    @Resource
    private EmployeeTrainingService employeeTrainingService;

    @GetMapping("/list")
    @Operation(summary = "获得员工培训信息列表")
    @Parameter(name = "profileId", description = "员工档案ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:employee:query')")
    public CommonResult<List<EmployeeTrainingRespVO>> getTrainingList(@RequestParam("profileId") Long profileId) {
        return success(employeeTrainingService.getTrainingList(profileId));
    }

    @GetMapping("/my-list")
    @Operation(summary = "获取我的培训档案")
    @PreAuthorize("@ss.hasAnyPermissions('hr:learning:self,hr:training:query')")
    public CommonResult<List<EmployeeTrainingRespVO>> getMyTrainingList() {
        return success(employeeTrainingService.getMyTrainingList());
    }

    @GetMapping("/page")
    @Operation(summary = "获得培训工作台分页")
    @PreAuthorize("@ss.hasAnyPermissions('hr:training:query,hr:employee:query')")
    public CommonResult<PageResult<EmployeeTrainingRespVO>> getTrainingPage(
            @Valid EmployeeTrainingPageReqVO pageReqVO) {
        return success(employeeTrainingService.getTrainingPage(pageReqVO));
    }

    @GetMapping("/retrain-page")
    @Operation(summary = "获得到期复训提醒分页")
    @PreAuthorize("@ss.hasAnyPermissions('hr:training:query,hr:employee:query')")
    public CommonResult<PageResult<EmployeeTrainingRespVO>> getRetrainReminderPage(
            @Valid EmployeeTrainingPageReqVO pageReqVO) {
        return success(employeeTrainingService.getRetrainReminderPage(pageReqVO));
    }

    @GetMapping("/stats")
    @Operation(summary = "获得培训工作台统计")
    @PreAuthorize("@ss.hasAnyPermissions('hr:training:query,hr:employee:query')")
    public CommonResult<EmployeeTrainingStatsRespVO> getTrainingStats(
            @Valid EmployeeTrainingPageReqVO pageReqVO) {
        return success(employeeTrainingService.getTrainingStats(pageReqVO));
    }

    @PostMapping("/create")
    @Operation(summary = "创建员工培训信息")
    @PreAuthorize("@ss.hasAnyPermissions('hr:training:manage,hr:employee:update')")
    public CommonResult<Long> createTraining(@Valid @RequestBody EmployeeTrainingSaveReqVO createReqVO) {
        return success(employeeTrainingService.createTraining(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新员工培训信息")
    @PreAuthorize("@ss.hasAnyPermissions('hr:training:manage,hr:employee:update')")
    public CommonResult<Boolean> updateTraining(@Valid @RequestBody EmployeeTrainingSaveReqVO updateReqVO) {
        employeeTrainingService.updateTraining(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除员工培训信息")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasAnyPermissions('hr:training:manage,hr:employee:update')")
    public CommonResult<Boolean> deleteTraining(@RequestParam("id") Long id) {
        employeeTrainingService.deleteTraining(id);
        return success(true);
    }
}
