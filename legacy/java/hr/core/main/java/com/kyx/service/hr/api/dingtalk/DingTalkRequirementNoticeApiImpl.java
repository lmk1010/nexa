package com.kyx.service.hr.api.dingtalk;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.service.hr.api.dingtalk.dto.DingTalkRequirementNoticeReqDTO;
import com.kyx.service.hr.integration.dingtalk.service.DingTalkRequirementNoticeService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

import static com.kyx.foundation.common.pojo.CommonResult.success;

@RestController
@Validated
public class DingTalkRequirementNoticeApiImpl implements DingTalkRequirementNoticeApi {

    @Resource
    private DingTalkRequirementNoticeService dingTalkRequirementNoticeService;

    @Override
    public CommonResult<Boolean> sendApprovalTodo(@Valid DingTalkRequirementNoticeReqDTO reqDTO) {
        dingTalkRequirementNoticeService.sendApprovalTodo(reqDTO);
        return success(true);
    }

    @Override
    public CommonResult<Boolean> sendAssignedDev(@Valid DingTalkRequirementNoticeReqDTO reqDTO) {
        dingTalkRequirementNoticeService.sendAssignedDev(reqDTO);
        return success(true);
    }

    @Override
    public CommonResult<Boolean> sendCommentRemind(@Valid DingTalkRequirementNoticeReqDTO reqDTO) {
        dingTalkRequirementNoticeService.sendCommentRemind(reqDTO);
        return success(true);
    }
}
