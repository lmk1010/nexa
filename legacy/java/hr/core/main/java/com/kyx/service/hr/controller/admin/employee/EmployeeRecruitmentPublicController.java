package com.kyx.service.hr.controller.admin.employee;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeRecruitmentPublicInfoRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeRecruitmentPublicSubmitReqVO;
import com.kyx.service.hr.service.employee.EmployeeRecruitmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static com.kyx.foundation.common.pojo.CommonResult.success;

/**
 * 公开招聘投递 Controller（无需登录）
 */
@Tag(name = "公开招聘投递")
@RestController
@RequestMapping("/hr/public/recruitment")
@Validated
public class EmployeeRecruitmentPublicController {

    @Resource
    private EmployeeRecruitmentService employeeRecruitmentService;

    @GetMapping("/info")
    @Operation(summary = "获取公开招聘投递页信息")
    @Parameter(name = "token", description = "访问令牌", required = true)
    public CommonResult<EmployeeRecruitmentPublicInfoRespVO> getPublicInfo(@RequestParam("token") String token) {
        return success(employeeRecruitmentService.getPublicInfo(token));
    }

    @PostMapping("/submit")
    @Operation(summary = "提交公开招聘候选人")
    public CommonResult<Long> submitPublicCandidate(
            @Valid @RequestBody EmployeeRecruitmentPublicSubmitReqVO submitReqVO) {
        return success(employeeRecruitmentService.submitPublicCandidate(submitReqVO));
    }
}
