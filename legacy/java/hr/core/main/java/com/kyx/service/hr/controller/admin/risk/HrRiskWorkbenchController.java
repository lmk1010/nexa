package com.kyx.service.hr.controller.admin.risk;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.risk.vo.HrRiskEventBatchHandleReqVO;
import com.kyx.service.hr.controller.admin.risk.vo.HrRiskEventCreateReqVO;
import com.kyx.service.hr.controller.admin.risk.vo.HrRiskEventHandleReqVO;
import com.kyx.service.hr.controller.admin.risk.vo.HrRiskEventPageReqVO;
import com.kyx.service.hr.controller.admin.risk.vo.HrRiskEventRespVO;
import com.kyx.service.hr.controller.admin.risk.vo.HrRiskWorkbenchRespVO;
import com.kyx.service.hr.service.risk.HrRiskWorkbenchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

import static com.kyx.foundation.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - HR 风险工作台")
@RestController
@RequestMapping("/hr/risk")
@Validated
public class HrRiskWorkbenchController {

    @Resource
    private HrRiskWorkbenchService hrRiskWorkbenchService;

    @GetMapping("/workbench")
    @Operation(summary = "获得 HR 风险工作台")
    @PreAuthorize("@ss.hasAnyPermissions('hr:risk:query,hr:employee-master:query')")
    public CommonResult<HrRiskWorkbenchRespVO> getWorkbench() {
        return success(hrRiskWorkbenchService.getWorkbench());
    }

    @GetMapping("/event/page")
    @Operation(summary = "获得风险事件分页")
    @PreAuthorize("@ss.hasPermission('hr:risk:query')")
    public CommonResult<PageResult<HrRiskEventRespVO>> getEventPage(@Valid HrRiskEventPageReqVO pageReqVO) {
        return success(hrRiskWorkbenchService.getEventPage(pageReqVO));
    }

    @PostMapping("/event/refresh")
    @Operation(summary = "刷新风险事件")
    @PreAuthorize("@ss.hasPermission('hr:risk:handle')")
    public CommonResult<Integer> refreshEvents() {
        return success(hrRiskWorkbenchService.refreshGeneratedEvents());
    }

    @PostMapping("/event/create")
    @Operation(summary = "人工登记风险事件")
    @PreAuthorize("@ss.hasPermission('hr:risk:handle')")
    public CommonResult<Long> createEvent(@Valid @RequestBody HrRiskEventCreateReqVO reqVO) {
        return success(hrRiskWorkbenchService.createEvent(reqVO));
    }

    @PutMapping("/event/handle")
    @Operation(summary = "处理风险事件")
    @PreAuthorize("@ss.hasPermission('hr:risk:handle')")
    public CommonResult<Boolean> handleEvent(@Valid @RequestBody HrRiskEventHandleReqVO reqVO) {
        hrRiskWorkbenchService.handleEvent(reqVO);
        return success(true);
    }

    @PutMapping("/event/batch-handle")
    @Operation(summary = "批量处理风险事件")
    @PreAuthorize("@ss.hasPermission('hr:risk:handle')")
    public CommonResult<Integer> batchHandleEvents(@Valid @RequestBody HrRiskEventBatchHandleReqVO reqVO) {
        return success(hrRiskWorkbenchService.batchHandleEvents(reqVO));
    }

}
