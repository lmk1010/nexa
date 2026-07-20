package com.kyx.service.hr.api.dingtalk;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.service.hr.api.dingtalk.dto.DingTalkRequirementNoticeReqDTO;
import com.kyx.service.hr.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.Valid;

@FeignClient(name = ApiConstants.NAME)
@Tag(name = "RPC 服务 - 钉钉需求通知")
public interface DingTalkRequirementNoticeApi {

    String PREFIX = ApiConstants.PREFIX + "/dingtalk/requirement-notice";

    @PostMapping(PREFIX + "/approval-todo")
    @Operation(summary = "发送需求待审批钉钉通知")
    CommonResult<Boolean> sendApprovalTodo(@Valid @RequestBody DingTalkRequirementNoticeReqDTO reqDTO);

    @PostMapping(PREFIX + "/assigned-dev")
    @Operation(summary = "发送需求进入开发钉钉通知")
    CommonResult<Boolean> sendAssignedDev(@Valid @RequestBody DingTalkRequirementNoticeReqDTO reqDTO);

    @PostMapping(PREFIX + "/comment-remind")
    @Operation(summary = "Send requirement comment reminder DingTalk notice")
    CommonResult<Boolean> sendCommentRemind(@Valid @RequestBody DingTalkRequirementNoticeReqDTO reqDTO);

}
