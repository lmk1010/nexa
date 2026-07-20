package com.kyx.service.hr.controller.admin.todo;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.todo.vo.HrTodoCompleteReqVO;
import com.kyx.service.hr.controller.admin.todo.vo.HrTodoPageReqVO;
import com.kyx.service.hr.controller.admin.todo.vo.HrTodoRespVO;
import com.kyx.service.hr.controller.admin.todo.vo.HrTodoSummaryReqVO;
import com.kyx.service.hr.controller.admin.todo.vo.HrTodoSummaryRespVO;
import com.kyx.service.hr.service.todo.HrTodoTaskService;
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

@Tag(name = "管理后台 - HR 统一待办")
@RestController
@RequestMapping("/hr/todo")
@Validated
public class HrTodoController {

    @Resource
    private HrTodoTaskService hrTodoTaskService;

    @GetMapping("/page")
    @Operation(summary = "获得 HR 统一待办分页")
    @PreAuthorize("@ss.hasAnyPermissions('hr:todo:query,hr:todo:self')")
    public CommonResult<PageResult<HrTodoRespVO>> getPage(@Valid HrTodoPageReqVO pageReqVO) {
        return success(hrTodoTaskService.getPage(pageReqVO));
    }

    @GetMapping("/summary")
    @Operation(summary = "获得 HR 统一待办摘要")
    @PreAuthorize("@ss.hasAnyPermissions('hr:todo:query,hr:todo:self')")
    public CommonResult<HrTodoSummaryRespVO> getSummary(@Valid HrTodoSummaryReqVO reqVO) {
        return success(hrTodoTaskService.getSummary(reqVO));
    }

    @PutMapping("/complete")
    @Operation(summary = "完成手工待办")
    @PreAuthorize("@ss.hasAnyPermissions('hr:todo:update,hr:todo:self')")
    public CommonResult<Boolean> complete(@Valid @RequestBody HrTodoCompleteReqVO reqVO) {
        return success(hrTodoTaskService.complete(reqVO));
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新业务来源待办")
    @PreAuthorize("@ss.hasPermission('hr:todo:query')")
    public CommonResult<Integer> refreshGeneratedTasks() {
        return success(hrTodoTaskService.refreshGeneratedTasks());
    }

}
