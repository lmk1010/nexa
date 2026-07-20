package com.kyx.service.op.controller.app.apprelease;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.tenant.core.aop.TenantIgnore;
import com.kyx.service.op.controller.app.apprelease.vo.AppReleaseCheckRespVO;
import com.kyx.service.op.service.apprelease.AppReleaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;

import static com.kyx.foundation.common.pojo.CommonResult.success;

@Tag(name = "用户 App - App 版本")
@RestController
@RequestMapping("/infra/app-release")
@Validated
public class AppAppReleaseController {

    @Resource
    private AppReleaseService appReleaseService;

    @GetMapping("/check")
    @PermitAll
    @TenantIgnore
    @Operation(summary = "检测 App 更新")
    @Parameters({
            @Parameter(name = "platform", description = "平台", example = "android"),
            @Parameter(name = "channel", description = "渠道", example = "prod"),
            @Parameter(name = "versionCode", description = "当前版本号", example = "1")
    })
    public CommonResult<AppReleaseCheckRespVO> check(@RequestParam(value = "platform", defaultValue = "android") String platform,
                                                     @RequestParam(value = "channel", defaultValue = "prod") String channel,
                                                     @RequestParam(value = "versionCode", defaultValue = "0") Integer versionCode) {
        return success(appReleaseService.check(platform, channel, versionCode));
    }

}
