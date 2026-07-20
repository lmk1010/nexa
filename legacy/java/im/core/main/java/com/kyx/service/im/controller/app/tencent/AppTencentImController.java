package com.kyx.service.im.controller.app.tencent;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.service.im.controller.app.tencent.vo.TencentImLoginRespVO;
import com.kyx.service.im.controller.app.tencent.vo.TencentImUserIdRespVO;
import com.kyx.service.im.service.tencent.TencentImService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

import static com.kyx.foundation.common.pojo.CommonResult.success;

@Tag(name = "Mobile - Tencent IM")
@RestController
@RequestMapping("/im/tencent")
@Validated
@Slf4j
public class AppTencentImController {

    @Resource
    private TencentImService tencentImService;

    @GetMapping("/session")
    @Operation(summary = "Get Tencent IM login ticket")
    public CommonResult<TencentImLoginRespVO> getLoginTicket() {
        return success(tencentImService.getLoginTicket());
    }

    @GetMapping("/user-id")
    @Operation(summary = "Resolve OA user id to Tencent IM user id")
    @Parameter(name = "oaUserId", description = "OA user id", required = true)
    public CommonResult<TencentImUserIdRespVO> getUserId(@RequestParam("oaUserId") Long oaUserId) {
        return success(tencentImService.getUserId(oaUserId));
    }
}
