package com.kyx.service.op.controller.admin.apprelease;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.service.op.controller.admin.apprelease.vo.AppReleasePublishReqVO;
import com.kyx.service.op.controller.admin.apprelease.vo.AppReleaseRespVO;
import com.kyx.service.op.service.apprelease.AppReleaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static com.kyx.foundation.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - App 版本")
@RestController
@RequestMapping("/infra/app-release")
@Validated
public class AppReleaseController {

    @Resource
    private AppReleaseService appReleaseService;

    @PostMapping("/publish")
    @Operation(summary = "发布 App 版本")
    public CommonResult<Long> publish(@Valid @RequestBody AppReleasePublishReqVO reqVO) {
        return success(appReleaseService.publish(reqVO));
    }

    @GetMapping("/latest")
    @Operation(summary = "获取最新 App 版本")
    @Parameters({
            @Parameter(name = "platform", description = "平台", example = "android"),
            @Parameter(name = "channel", description = "渠道", example = "prod")
    })
    public CommonResult<AppReleaseRespVO> getLatest(@RequestParam(value = "platform", defaultValue = "android") String platform,
                                                    @RequestParam(value = "channel", defaultValue = "prod") String channel) {
        return success(appReleaseService.getLatest(platform, channel));
    }

}
