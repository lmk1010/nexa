package com.kyx.service.hr.controller.admin.notice;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.notice.vo.HrNoticeRecordPageReqVO;
import com.kyx.service.hr.controller.admin.notice.vo.HrNoticeRecordRespVO;
import com.kyx.service.hr.service.notice.HrNoticeRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

import static com.kyx.foundation.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - HR 通知记录")
@RestController
@RequestMapping("/hr/notice-record")
@Validated
public class HrNoticeRecordController {

    @Resource
    private HrNoticeRecordService noticeRecordService;

    @GetMapping("/page")
    @Operation(summary = "获得通知记录分页")
    @PreAuthorize("@ss.hasPermission('hr:notice-record:query')")
    public CommonResult<PageResult<HrNoticeRecordRespVO>> getPage(@Valid HrNoticeRecordPageReqVO pageReqVO) {
        return success(noticeRecordService.getPage(pageReqVO));
    }

    @PostMapping("/retry")
    @Operation(summary = "重试失败通知")
    @Parameter(name = "id", description = "记录 ID", required = true)
    @PreAuthorize("@ss.hasPermission('hr:notice-record:retry')")
    public CommonResult<Boolean> retry(@RequestParam("id") Long id) {
        noticeRecordService.retry(id);
        return success(true);
    }

}
