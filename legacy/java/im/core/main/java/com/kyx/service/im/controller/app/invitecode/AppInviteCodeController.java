package com.kyx.service.im.controller.app.invitecode;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.service.im.controller.admin.invitecode.vo.InviteCodeValidateRespVO;
import com.kyx.service.im.service.invitecode.InviteCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.PermitAll;
import javax.annotation.Resource;

import static com.kyx.foundation.common.pojo.CommonResult.success;

@Tag(name = "移动端 - 邀请码")
@RestController
@RequestMapping("/im/invite-code")
@Validated
@Slf4j
public class AppInviteCodeController {

    @Resource
    private InviteCodeService inviteCodeService;

    @GetMapping("/validate")
    @PermitAll
    @Operation(summary = "验证邀请码")
    @Parameter(name = "code", description = "邀请码", required = true)
    public CommonResult<InviteCodeValidateRespVO> validateInviteCode(@RequestParam("code") String code) {
        InviteCodeValidateRespVO result = inviteCodeService.validateInviteCodeWithDetails(code);
        return success(result);
    }

    @PostMapping("/use")
    @PermitAll
    @Operation(summary = "使用邀请码")
    @Parameter(name = "code", description = "邀请码", required = true)
    public CommonResult<Boolean> useInviteCode(@RequestParam("code") String code) {
        inviteCodeService.useInviteCode(code);
        return success(true);
    }
} 