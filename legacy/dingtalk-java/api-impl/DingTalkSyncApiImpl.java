package com.kyx.service.hr.api.dingtalk;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.service.hr.integration.dingtalk.job.DingTalkSyncScheduleJob;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

import static com.kyx.foundation.common.pojo.CommonResult.success;

@RestController
@Validated
public class DingTalkSyncApiImpl implements DingTalkSyncApi {

    @Resource
    private DingTalkSyncScheduleJob dingTalkSyncScheduleJob;

    @Override
    public CommonResult<String> executeDailySync() {
        return success(dingTalkSyncScheduleJob.executeDailySync());
    }

    @Override
    public CommonResult<String> executeEmployeeSync() {
        return success(dingTalkSyncScheduleJob.executeEmployeeSync());
    }

    @Override
    public CommonResult<String> executeAttendanceSync() {
        return success(dingTalkSyncScheduleJob.executeAttendanceSync());
    }

    @Override
    public CommonResult<String> executeLeaveSync() {
        return success(dingTalkSyncScheduleJob.executeLeaveSync());
    }
}
