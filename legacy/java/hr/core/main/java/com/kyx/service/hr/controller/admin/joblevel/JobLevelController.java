package com.kyx.service.hr.controller.admin.joblevel;

import com.kyx.foundation.apilog.core.annotation.ApiAccessLog;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.excel.core.util.ExcelUtils;
import com.kyx.service.hr.controller.admin.joblevel.vo.JobLevelOptionVO;
import com.kyx.service.hr.controller.admin.joblevel.vo.JobLevelPageReqVO;
import com.kyx.service.hr.controller.admin.joblevel.vo.JobLevelRespVO;
import com.kyx.service.hr.controller.admin.joblevel.vo.JobLevelSaveReqVO;
import com.kyx.service.hr.dal.dataobject.joblevel.JobLevelDO;
import com.kyx.service.hr.service.joblevel.JobLevelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

import static com.kyx.foundation.apilog.core.enums.OperateTypeEnum.EXPORT;
import static com.kyx.foundation.common.pojo.CommonResult.success;

/**
 * 职级管理 Controller
 *
 * @author MK
 */
@Tag(name = "管理后台 - 职级管理")
@RestController
@RequestMapping("/hr/job-level")
@Validated
public class JobLevelController {

    @Resource
    private JobLevelService jobLevelService;

    @PostMapping("/create")
    @Operation(summary = "创建职级")
    @PreAuthorize("@ss.hasPermission('hr:job-level:create')")
    public CommonResult<Long> createJobLevel(@Valid @RequestBody JobLevelSaveReqVO createReqVO) {
        return success(jobLevelService.createJobLevel(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新职级")
    @PreAuthorize("@ss.hasPermission('hr:job-level:update')")
    public CommonResult<Boolean> updateJobLevel(@Valid @RequestBody JobLevelSaveReqVO updateReqVO) {
        jobLevelService.updateJobLevel(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除职级")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('hr:job-level:delete')")
    public CommonResult<Boolean> deleteJobLevel(@RequestParam("id") Long id) {
        jobLevelService.deleteJobLevel(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得职级")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:job-level:query')")
    public CommonResult<JobLevelRespVO> getJobLevel(@RequestParam("id") Long id) {
        JobLevelDO jobLevel = jobLevelService.getJobLevel(id);
        return success(BeanUtils.toBean(jobLevel, JobLevelRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得职级分页")
    @PreAuthorize("@ss.hasPermission('hr:job-level:query')")
    public CommonResult<PageResult<JobLevelRespVO>> getJobLevelPage(@Valid JobLevelPageReqVO pageReqVO) {
        PageResult<JobLevelRespVO> pageResult = jobLevelService.getJobLevelPage(pageReqVO);
        return success(pageResult);
    }

    @GetMapping("/list")
    @Operation(summary = "获得职级列表")
    @PreAuthorize("@ss.hasPermission('hr:job-level:query')")
    public CommonResult<List<JobLevelRespVO>> getJobLevelList() {
        List<JobLevelRespVO> list = jobLevelService.getJobLevelList();
        return success(list);
    }

    @GetMapping("/options")
    @Operation(summary = "获得职级选项列表")
    public CommonResult<List<JobLevelOptionVO>> getJobLevelOptions() {
        List<JobLevelOptionVO> list = jobLevelService.getJobLevelOptions();
        return success(list);
    }

    @GetMapping("/by-sequence")
    @Operation(summary = "根据序列ID获得职级列表")
    @Parameter(name = "sequenceId", description = "序列ID", required = true)
    public CommonResult<List<JobLevelRespVO>> getJobLevelListBySequenceId(@RequestParam("sequenceId") Long sequenceId) {
        List<JobLevelRespVO> list = jobLevelService.getJobLevelListBySequenceId(sequenceId);
        return success(list);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出职级 Excel")
    @PreAuthorize("@ss.hasPermission('hr:job-level:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportJobLevelExcel(@Valid JobLevelPageReqVO pageReqVO,
                                   HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(10000);
        List<JobLevelRespVO> list = jobLevelService.getJobLevelPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "职级管理.xls", "数据", JobLevelRespVO.class, list);
    }

}