package com.kyx.service.hr.api.dingtalk;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.service.hr.api.dingtalk.dto.DingTalkBpmNoticeReqDTO;
import com.kyx.service.hr.integration.dingtalk.service.DingTalkRequirementNoticeService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

import static com.kyx.foundation.common.pojo.CommonResult.success;

@RestController
@Validated
public class DingTalkBpmNoticeApiImpl implements DingTalkBpmNoticeApi {

    @Resource
    private DingTalkRequirementNoticeService dingTalkRequirementNoticeService;

    @Override
    public CommonResult<Boolean> sendTaskTodo(@Valid DingTalkBpmNoticeReqDTO reqDTO) {
        dingTalkRequirementNoticeService.sendBpmTaskTodo(reqDTO);
        return success(true);
    }

    @Override
    public CommonResult<Boolean> sendCopy(@Valid DingTalkBpmNoticeReqDTO reqDTO) {
        dingTalkRequirementNoticeService.sendBpmCopy(reqDTO);
        return success(true);
    }
}
