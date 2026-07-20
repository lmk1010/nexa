package com.kyx.service.business.api.logger;

import com.kyx.foundation.common.biz.system.logger.OperateLogCommonApi;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.business.api.logger.dto.OperateLogPageReqDTO;
import com.kyx.service.business.api.logger.dto.OperateLogRespDTO;
import com.kyx.service.business.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;

import javax.validation.Valid;

@FeignClient(name = ApiConstants.NAME) // TODO ：fallbackFactory =
@Tag(name = "RPC 服务 - 操作日志")
public interface OperateLogApi extends OperateLogCommonApi {

    String PREFIX = ApiConstants.PREFIX + "/operate-log";

    @GetMapping(PREFIX + "/page")
    @Operation(summary = "获取指定模块的指定数据的操作日志分页")
    CommonResult<PageResult<OperateLogRespDTO>> getOperateLogPage(@SpringQueryMap OperateLogPageReqDTO pageReqDTO);

}
