package com.kyx.service.hr.api.dingtalk;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.service.hr.api.dingtalk.dto.DingTalkBpmNoticeReqDTO;
import com.kyx.service.hr.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.Valid;

@FeignClient(name = ApiConstants.NAME)
@Tag(name = "RPC 服务 - 钉钉 BPM 通知")
public interface DingTalkBpmNoticeApi {

    String PREFIX = ApiConstants.PREFIX + "/dingtalk/bpm-notice";

    @PostMapping(PREFIX + "/task-todo")
    @Operation(summary = "发送 BPM 待办钉钉通知")
    CommonResult<Boolean> sendTaskTodo(@Valid @RequestBody DingTalkBpmNoticeReqDTO reqDTO);

    @PostMapping(PREFIX + "/copy")
    @Operation(summary = "发送 BPM 知会钉钉通知")
    CommonResult<Boolean> sendCopy(@Valid @RequestBody DingTalkBpmNoticeReqDTO reqDTO);

}
