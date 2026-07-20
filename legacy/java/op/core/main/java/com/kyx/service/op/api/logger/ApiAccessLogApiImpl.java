package com.kyx.service.op.api.logger;

import com.kyx.foundation.common.biz.infra.logger.ApiAccessLogCommonApi;
import com.kyx.foundation.common.biz.infra.logger.dto.ApiAccessLogCreateReqDTO;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.service.op.service.logger.ApiAccessLogService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

import static com.kyx.foundation.common.pojo.CommonResult.success;

@RestController // 提供 RESTful API 接口，给 Feign 调用
@Validated
public class ApiAccessLogApiImpl implements ApiAccessLogCommonApi {

    @Resource
    private ApiAccessLogService apiAccessLogService;

    @Override
    public CommonResult<Boolean> createApiAccessLog(ApiAccessLogCreateReqDTO createDTO) {
        apiAccessLogService.createApiAccessLog(createDTO);
        return success(true);
    }

}
