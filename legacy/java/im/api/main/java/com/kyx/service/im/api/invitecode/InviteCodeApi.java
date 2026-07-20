package com.kyx.service.im.api.invitecode;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.im.api.invitecode.dto.InviteCodeCreateReqDTO;
import com.kyx.service.im.api.invitecode.dto.InviteCodePageReqDTO;
import com.kyx.service.im.api.invitecode.dto.InviteCodeRespDTO;
import com.kyx.service.im.api.invitecode.dto.InviteCodeUpdateReqDTO;
import com.kyx.service.im.api.invitecode.dto.InviteCodeValidateRespDTO;
import com.kyx.service.im.api.invitecode.dto.TenantInviteCodeStatsRespDTO;
import com.kyx.service.im.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 邀请码 API 接口
 *
 * @author MK
 */
@FeignClient(name = ApiConstants.NAME)
@Tag(name = "RPC 服务 - 邀请码")
public interface InviteCodeApi {

    String PREFIX = ApiConstants.PREFIX + "/invite-code";

    /**
     * 根据邀请码获取邀请码信息
     *
     * @param code 邀请码
     * @return 邀请码信息
     */
    @GetMapping(PREFIX + "/get-by-code")
    @Operation(summary = "根据邀请码获取邀请码信息")
    @Parameter(name = "code", description = "邀请码", required = true)
    InviteCodeRespDTO getInviteCodeByCode(@RequestParam("code") String code);

    /**
     * 验证邀请码
     *
     * @param code 邀请码
     * @return 邀请码验证结果
     */
    @GetMapping(PREFIX + "/validate")
    @Operation(summary = "验证邀请码")
    @Parameter(name = "code", description = "邀请码", required = true)
    InviteCodeValidateRespDTO validateInviteCode(@RequestParam("code") String code);

    /**
     * 使用邀请码
     *
     * @param code 邀请码
     */
    @PostMapping(PREFIX + "/use")
    @Operation(summary = "使用邀请码")
    @Parameter(name = "code", description = "邀请码", required = true)
    void useInviteCode(@RequestParam("code") String code);

} 