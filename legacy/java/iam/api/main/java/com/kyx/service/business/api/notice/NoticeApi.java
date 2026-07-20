package com.kyx.service.business.api.notice;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.service.business.api.notice.dto.NoticeCreateReqDTO;
import com.kyx.service.business.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.Valid;

import static com.kyx.service.business.api.notice.NoticeApi.PREFIX;

@FeignClient(name = ApiConstants.NAME, contextId = "noticeApi")
@Tag(name = "RPC 服务 - 通知公告")
public interface NoticeApi {

    String PREFIX = ApiConstants.PREFIX + "/notice";

    @PostMapping(PREFIX + "/create")
    @Operation(summary = "创建通知公告")
    CommonResult<Long> createNotice(@Valid @RequestBody NoticeCreateReqDTO reqDTO);

}
