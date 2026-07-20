package com.kyx.service.biz.controller.admin.work;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.service.biz.controller.admin.work.vo.WorkCalendarEventReqVO;
import com.kyx.service.biz.controller.admin.work.vo.WorkCalendarEventRespVO;
import com.kyx.service.biz.controller.admin.work.vo.WorkCalendarEventSaveReqVO;
import com.kyx.service.biz.service.work.WorkCalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import static com.kyx.foundation.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "Admin - Work Calendar")
@RestController
@RequestMapping("/business/work/calendar")
@Validated
public class WorkCalendarController {

    @Resource
    private WorkCalendarService workCalendarService;

    @GetMapping("/events")
    @Operation(summary = "Get work calendar events")
    public CommonResult<List<WorkCalendarEventRespVO>> getCalendarEvents(@Valid WorkCalendarEventReqVO reqVO) {
        return success(workCalendarService.getCalendarEvents(reqVO, getLoginUserId()));
    }

    @PostMapping("/event/create")
    @Operation(summary = "Create personal calendar event")
    public CommonResult<Long> createCalendarEvent(@Valid @RequestBody WorkCalendarEventSaveReqVO createReqVO) {
        return success(workCalendarService.createCalendarEvent(createReqVO, getLoginUserId()));
    }

    @PutMapping("/event/update")
    @Operation(summary = "Update personal calendar event")
    public CommonResult<Boolean> updateCalendarEvent(@Valid @RequestBody WorkCalendarEventSaveReqVO updateReqVO) {
        workCalendarService.updateCalendarEvent(updateReqVO, getLoginUserId());
        return success(true);
    }

    @DeleteMapping("/event/delete")
    @Operation(summary = "Delete personal calendar event")
    @Parameter(name = "id", required = true, example = "1024")
    public CommonResult<Boolean> deleteCalendarEvent(@RequestParam("id") Long id) {
        workCalendarService.deleteCalendarEvent(id, getLoginUserId());
        return success(true);
    }
}
