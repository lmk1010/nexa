package com.kyx.service.business.job;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.service.hr.api.dingtalk.DingTalkSyncApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.function.Supplier;

/**
 * OA scheduler entry for DingTalk sync.
 */
@Component
@Slf4j
public class DingTalkSyncSchedulerJob {

    @Resource
    private DingTalkSyncApi dingTalkSyncApi;

    public String executeDailySync() {
        return executeSync("daily", dingTalkSyncApi::executeDailySync);
    }

    public String executeEmployeeSync() {
        return executeSync("employee", dingTalkSyncApi::executeEmployeeSync);
    }

    public String executeAttendanceSync() {
        return executeSync("attendance", dingTalkSyncApi::executeAttendanceSync);
    }

    public String executeLeaveSync() {
        return executeSync("leave", dingTalkSyncApi::executeLeaveSync);
    }

    private String executeSync(String scope, Supplier<CommonResult<String>> syncCall) {
        log.info("Start DingTalk {} sync by OA scheduler", scope);
        CommonResult<String> result = syncCall.get();
        String data = result.getCheckedData();
        String message = data == null || data.trim().isEmpty() ? "DingTalk sync done" : data;
        log.info("DingTalk {} sync finished by OA scheduler: {}", scope, message);
        return message;
    }
}
