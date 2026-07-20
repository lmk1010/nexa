package com.kyx.service.im.controller.admin.invitecode;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.im.controller.admin.invitecode.vo.InviteCodeCreateReqVO;
import com.kyx.service.im.controller.admin.invitecode.vo.InviteCodePageReqVO;
import com.kyx.service.im.controller.admin.invitecode.vo.InviteCodeRespVO;
import com.kyx.service.im.controller.admin.invitecode.vo.InviteCodeUpdateReqVO;
import com.kyx.service.im.controller.admin.invitecode.vo.InviteCodeValidateRespVO;
import com.kyx.service.im.dal.dataobject.invitecode.InviteCodeDO;
import com.kyx.service.im.dal.mysql.invitecode.TenantInviteCodeStatsVO;
import com.kyx.service.im.service.invitecode.InviteCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.PermitAll;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.sql.Timestamp;
import java.util.List;

import static com.kyx.foundation.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 邀请码")
@RestController
@RequestMapping("/im/invite-code")
@Validated
@Slf4j
public class InviteCodeController {

    @Resource
    private InviteCodeService inviteCodeService;

    @PostMapping("/create")
    @Operation(summary = "创建邀请码")
    @PreAuthorize("@ss.hasPermission('im:invite-code:create')")
    public CommonResult<Long> createInviteCode(@Valid @RequestBody InviteCodeCreateReqVO createReqVO) {
        return success(inviteCodeService.createInviteCode(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新邀请码")
    @PreAuthorize("@ss.hasPermission('im:invite-code:update')")
    public CommonResult<Boolean> updateInviteCode(@Valid @RequestBody InviteCodeUpdateReqVO updateReqVO) {
        inviteCodeService.updateInviteCode(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除邀请码")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('im:invite-code:delete')")
    public CommonResult<Boolean> deleteInviteCode(@RequestParam("id") Long id) {
        inviteCodeService.deleteInviteCode(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得邀请码")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('im:invite-code:query')")
    public CommonResult<InviteCodeRespVO> getInviteCode(@RequestParam("id") Long id) {
        InviteCodeDO inviteCode = inviteCodeService.getInviteCode(id);
        return success(new InviteCodeRespVO()
                .setId(inviteCode.getId())
                .setCode(inviteCode.getCode())
                .setTenantId(inviteCode.getTenantId())
                .setTenantName(inviteCode.getTenantName())
                .setTenantStatus(inviteCode.getTenantStatus())
                .setType(inviteCode.getType())
                .setUsageLimit(inviteCode.getUsageLimit())
                .setUsedCount(inviteCode.getUsedCount())
                .setValidStartTime(inviteCode.getValidStartTime() != null ? Timestamp.valueOf(inviteCode.getValidStartTime()) : null)
                .setValidEndTime(inviteCode.getValidEndTime() != null ? Timestamp.valueOf(inviteCode.getValidEndTime()) : null)
                .setStatus(inviteCode.getStatus())
                .setRemark(inviteCode.getRemark())
                .setCreatorId(inviteCode.getCreatorId())
                .setCreatorName(inviteCode.getCreatorName())
                .setCreateTime(inviteCode.getCreateTime() != null ? inviteCode.getCreateTime() : null));
    }

    @GetMapping("/page")
    @Operation(summary = "获得邀请码分页")
    @PreAuthorize("@ss.hasPermission('im:invite-code:query')")
    public CommonResult<PageResult<InviteCodeRespVO>> getInviteCodePage(@Valid InviteCodePageReqVO pageReqVO) {
        try {
            // 使用 MyBatis Plus IPage 分页机制查询数据
            PageResult<InviteCodeDO> pageResult = inviteCodeService.getInviteCodePage(pageReqVO);
            
            // 转换 PageResult 中的 DO 对象为 VO 对象
            List<InviteCodeRespVO> respList = pageResult.getList().stream()
                    .map(inviteCode -> new InviteCodeRespVO()
                            .setId(inviteCode.getId())
                            .setCode(inviteCode.getCode())
                            .setTenantId(inviteCode.getTenantId())
                            .setTenantName(inviteCode.getTenantName())
                            .setTenantStatus(inviteCode.getTenantStatus())
                            .setType(inviteCode.getType())
                            .setUsageLimit(inviteCode.getUsageLimit())
                            .setUsedCount(inviteCode.getUsedCount())
                            .setValidStartTime(inviteCode.getValidStartTime() != null ? Timestamp.valueOf(inviteCode.getValidStartTime()) : null)
                            .setValidEndTime(inviteCode.getValidEndTime() != null ? Timestamp.valueOf(inviteCode.getValidEndTime()) : null)
                            .setStatus(inviteCode.getStatus())
                            .setRemark(inviteCode.getRemark())
                            .setCreatorId(inviteCode.getCreatorId())
                            .setCreatorName(inviteCode.getCreatorName())
                            .setCreateTime(inviteCode.getCreateTime() != null ? inviteCode.getCreateTime() : null))
                    .collect(java.util.stream.Collectors.toList());
            
            // 返回分页结果，保持原有的总数信息
            return success(new PageResult<>(respList, pageResult.getTotal()));
        } catch (Exception e) {
            log.error("查询邀请码分页失败", e);
            return success(new PageResult<>(java.util.Collections.emptyList(), 0L));
        }
    }

    @GetMapping("/validate")
    @Operation(summary = "验证邀请码")
    @Parameter(name = "code", description = "邀请码", required = true)
    @PermitAll
    public CommonResult<InviteCodeValidateRespVO> validateInviteCode(@RequestParam("code") String code) {
        InviteCodeValidateRespVO respVO = inviteCodeService.validateInviteCodeWithDetails(code);
        return success(respVO);
    }

    @PostMapping("/use")
    @Operation(summary = "使用邀请码")
    @Parameter(name = "code", description = "邀请码", required = true)
    @PermitAll
    public CommonResult<Boolean> useInviteCode(@RequestParam("code") String code) {
        inviteCodeService.useInviteCode(code);
        return success(true);
    }

    @GetMapping("/get-by-code")
    @Operation(summary = "根据邀请码获取邀请码信息")
    @Parameter(name = "code", description = "邀请码", required = true)
    @PreAuthorize("@ss.hasPermission('im:invite-code:query')")
    public CommonResult<InviteCodeRespVO> getInviteCodeByCode(@RequestParam("code") String code) {
        InviteCodeDO inviteCode = inviteCodeService.getInviteCodeByCode(code);
        return success(new InviteCodeRespVO()
                .setId(inviteCode.getId())
                .setCode(inviteCode.getCode())
                .setTenantId(inviteCode.getTenantId())
                .setTenantName(inviteCode.getTenantName())
                .setTenantStatus(inviteCode.getTenantStatus())
                .setType(inviteCode.getType())
                .setUsageLimit(inviteCode.getUsageLimit())
                .setUsedCount(inviteCode.getUsedCount())
                .setValidStartTime(inviteCode.getValidStartTime() != null ? Timestamp.valueOf(inviteCode.getValidStartTime()) : null)
                .setValidEndTime(inviteCode.getValidEndTime() != null ? Timestamp.valueOf(inviteCode.getValidEndTime()) : null)
                .setStatus(inviteCode.getStatus())
                .setRemark(inviteCode.getRemark())
                .setCreatorId(inviteCode.getCreatorId())
                .setCreatorName(inviteCode.getCreatorName())
                .setCreateTime(inviteCode.getCreateTime() != null ? inviteCode.getCreateTime() : null));
    }

    @GetMapping("/tenant-stats")
    @Operation(summary = "获取租户邀请码统计")
    @PreAuthorize("@ss.hasPermission('im:invite-code:query')")
    public CommonResult<List<TenantInviteCodeStatsVO>> getTenantInviteCodeStats() {
        List<TenantInviteCodeStatsVO> stats = inviteCodeService.getTenantInviteCodeStats();
        return success(stats);
    }

    @PostMapping("/update-expired")
    @Operation(summary = "批量更新过期邀请码状态")
    @PreAuthorize("@ss.hasPermission('im:invite-code:update')")
    public CommonResult<Boolean> updateExpiredInviteCodes() {
        inviteCodeService.updateExpiredInviteCodes();
        return success(true);
    }

    @PostMapping("/update-used-up")
    @Operation(summary = "批量更新已用完邀请码状态")
    @PreAuthorize("@ss.hasPermission('im:invite-code:update')")
    public CommonResult<Boolean> updateUsedUpInviteCodes() {
        inviteCodeService.updateUsedUpInviteCodes();
        return success(true);
    }

} 