package com.kyx.service.business.controller.admin.mail;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.business.controller.admin.mail.vo.log.MailLogPageReqVO;
import com.kyx.service.business.controller.admin.mail.vo.log.MailLogRespVO;
import com.kyx.service.business.dal.dataobject.mail.MailLogDO;
import com.kyx.service.business.service.mail.MailLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

import static com.kyx.foundation.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 邮件日志")
@RestController
@RequestMapping("/system/mail-log")
public class MailLogController {

    @Resource
    private MailLogService mailLogService;

    @GetMapping("/page")
    @Operation(summary = "获得邮箱日志分页")
    @PreAuthorize("@ss.hasPermission('system:mail-log:query')")
    public CommonResult<PageResult<MailLogRespVO>> getMailLogPage(@Valid MailLogPageReqVO pageVO) {
        PageResult<MailLogDO> pageResult = mailLogService.getMailLogPage(pageVO);
        return success(BeanUtils.toBean(pageResult, MailLogRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获得邮箱日志")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:mail-log:query')")
    public CommonResult<MailLogRespVO> getMailTemplate(@RequestParam("id") Long id) {
        MailLogDO log = mailLogService.getMailLog(id);
        return success(BeanUtils.toBean(log, MailLogRespVO.class));
    }

}
