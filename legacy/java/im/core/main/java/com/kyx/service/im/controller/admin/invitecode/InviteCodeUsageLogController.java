package com.kyx.service.im.controller.admin.invitecode;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.im.controller.admin.invitecode.vo.InviteCodeUsageLogPageReqVO;
import com.kyx.service.im.controller.admin.invitecode.vo.InviteCodeUsageLogRespVO;
import com.kyx.service.im.dal.dataobject.invitecode.InviteCodeUsageLogDO;
import com.kyx.service.im.service.invitecode.InviteCodeUsageLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

import static com.kyx.foundation.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 邀请码使用记录")
@RestController
@RequestMapping("/im/invite-code-usage-log")
@Validated
@Slf4j
public class InviteCodeUsageLogController {

    @Resource
    private InviteCodeUsageLogService inviteCodeUsageLogService;

    @GetMapping("/get")
    @Operation(summary = "获得邀请码使用记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('im:invite-code:query')")
    public CommonResult<InviteCodeUsageLogRespVO> getUsageLog(@RequestParam("id") Long id) {
        InviteCodeUsageLogDO usageLog = inviteCodeUsageLogService.getUsageLog(id);
        return success(convertToRespVO(usageLog));
    }

    @GetMapping("/page")
    @Operation(summary = "获得邀请码使用记录分页")
    @PreAuthorize("@ss.hasPermission('im:invite-code:query')")
    public CommonResult<PageResult<InviteCodeUsageLogRespVO>> getUsageLogPage(@Valid InviteCodeUsageLogPageReqVO pageReqVO) {
        try {
            PageResult<InviteCodeUsageLogDO> pageResult = inviteCodeUsageLogService.getUsageLogPage(pageReqVO);
            
            // 转换DO对象为VO对象
            List<InviteCodeUsageLogRespVO> respList = pageResult.getList().stream()
                    .map(this::convertToRespVO)
                    .collect(Collectors.toList());
            
            return success(new PageResult<>(respList, pageResult.getTotal()));
        } catch (Exception e) {
            log.error("查询邀请码使用记录分页失败", e);
            return success(new PageResult<>(java.util.Collections.emptyList(), 0L));
        }
    }

    @GetMapping("/list-by-invite-code-id")
    @Operation(summary = "根据邀请码ID获取使用记录列表")
    @Parameter(name = "inviteCodeId", description = "邀请码ID", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('im:invite-code:query')")
    public CommonResult<List<InviteCodeUsageLogRespVO>> getUsageLogsByInviteCodeId(@RequestParam("inviteCodeId") Long inviteCodeId) {
        List<InviteCodeUsageLogDO> usageLogs = inviteCodeUsageLogService.getUsageLogsByInviteCodeId(inviteCodeId);
        List<InviteCodeUsageLogRespVO> respList = usageLogs.stream()
                .map(this::convertToRespVO)
                .collect(Collectors.toList());
        return success(respList);
    }

    @GetMapping("/list-by-invite-code")
    @Operation(summary = "根据邀请码获取使用记录列表")
    @Parameter(name = "inviteCode", description = "邀请码", required = true, example = "INV123456")
    @PreAuthorize("@ss.hasPermission('im:invite-code:query')")
    public CommonResult<List<InviteCodeUsageLogRespVO>> getUsageLogsByInviteCode(@RequestParam("inviteCode") String inviteCode) {
        List<InviteCodeUsageLogDO> usageLogs = inviteCodeUsageLogService.getUsageLogsByInviteCode(inviteCode);
        List<InviteCodeUsageLogRespVO> respList = usageLogs.stream()
                .map(this::convertToRespVO)
                .collect(Collectors.toList());
        return success(respList);
    }

    @GetMapping("/list-by-tenant-id")
    @Operation(summary = "根据租户ID获取使用记录列表")
    @Parameter(name = "tenantId", description = "租户ID", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('im:invite-code:query')")
    public CommonResult<List<InviteCodeUsageLogRespVO>> getUsageLogsByTenantId(@RequestParam("tenantId") Long tenantId) {
        List<InviteCodeUsageLogDO> usageLogs = inviteCodeUsageLogService.getUsageLogsByTenantId(tenantId);
        List<InviteCodeUsageLogRespVO> respList = usageLogs.stream()
                .map(this::convertToRespVO)
                .collect(Collectors.toList());
        return success(respList);
    }

    @GetMapping("/count/success")
    @Operation(summary = "获取邀请码成功使用次数")
    @Parameter(name = "inviteCodeId", description = "邀请码ID", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('im:invite-code:query')")
    public CommonResult<Long> getSuccessUsageCount(@RequestParam("inviteCodeId") Long inviteCodeId) {
        Long count = inviteCodeUsageLogService.getSuccessUsageCount(inviteCodeId);
        return success(count);
    }

    @GetMapping("/count/failure")
    @Operation(summary = "获取邀请码失败使用次数")
    @Parameter(name = "inviteCodeId", description = "邀请码ID", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('im:invite-code:query')")
    public CommonResult<Long> getFailureUsageCount(@RequestParam("inviteCodeId") Long inviteCodeId) {
        Long count = inviteCodeUsageLogService.getFailureUsageCount(inviteCodeId);
        return success(count);
    }

    /**
     * 转换DO对象为VO对象
     */
    private InviteCodeUsageLogRespVO convertToRespVO(InviteCodeUsageLogDO usageLog) {
        if (usageLog == null) {
            return null;
        }
        return new InviteCodeUsageLogRespVO()
                .setId(usageLog.getId())
                .setInviteCodeId(usageLog.getInviteCodeId())
                .setInviteCode(usageLog.getInviteCode())
                .setTenantId(usageLog.getTenantId())
                .setTenantName(usageLog.getTenantName())
                .setUserId(usageLog.getUserId())
                .setUserName(usageLog.getUserName())
                .setUserIp(usageLog.getUserIp())
                .setUserAgent(usageLog.getUserAgent())
                .setDeviceType(usageLog.getDeviceType())
                .setDeviceId(usageLog.getDeviceId())
                .setUsageTime(usageLog.getUsageTime())
                .setUsageResult(usageLog.getUsageResult())
                .setErrorMessage(usageLog.getErrorMessage())
                .setExtraData(usageLog.getExtraData())
                .setCreateTime(usageLog.getCreateTime());
    }
} 