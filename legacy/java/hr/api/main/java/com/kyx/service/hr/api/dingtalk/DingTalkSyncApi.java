package com.kyx.service.hr.api.dingtalk;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.service.hr.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = ApiConstants.NAME)
@Tag(name = "RPC 服务 - 钉钉同步")
public interface DingTalkSyncApi {

    String PREFIX = ApiConstants.PREFIX + "/dingtalk/sync";

    @PostMapping(PREFIX + "/daily")
    @Operation(summary = "执行钉钉每日同步")
    CommonResult<String> executeDailySync();

    @PostMapping(PREFIX + "/employee")
    @Operation(summary = "执行钉钉员工同步")
    CommonResult<String> executeEmployeeSync();

    @PostMapping(PREFIX + "/attendance")
    @Operation(summary = "执行钉钉考勤同步")
    CommonResult<String> executeAttendanceSync();

    @PostMapping(PREFIX + "/leave")
    @Operation(summary = "执行钉钉请假同步")
    CommonResult<String> executeLeaveSync();

}
