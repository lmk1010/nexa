package com.kyx.service.business.controller.admin.logger;

import com.kyx.foundation.apilog.core.annotation.ApiAccessLog;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageParam;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.excel.core.util.ExcelUtils;
import com.kyx.foundation.translate.core.TranslateUtils;
import com.kyx.service.business.controller.admin.logger.vo.operatelog.OperateLogPageReqVO;
import com.kyx.service.business.controller.admin.logger.vo.operatelog.OperateLogRespVO;
import com.kyx.service.business.dal.dataobject.logger.OperateLogDO;
import com.kyx.service.business.dal.dataobject.tenant.TenantDO;
import com.kyx.service.business.service.logger.OperateLogService;
import com.kyx.service.business.service.tenant.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.kyx.foundation.apilog.core.enums.OperateTypeEnum.EXPORT;
import static com.kyx.foundation.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 操作日志")
@RestController
@RequestMapping("/system/operate-log")
@Validated
public class OperateLogController {

    @Resource
    private OperateLogService operateLogService;

    @Resource
    private TenantService tenantService;

    @GetMapping("/page")
    @Operation(summary = "查看操作日志分页列表")
    @PreAuthorize("@ss.hasPermission('system:operate-log:query')")
    public CommonResult<PageResult<OperateLogRespVO>> pageOperateLog(@Valid OperateLogPageReqVO pageReqVO) {
        PageResult<OperateLogDO> pageResult = operateLogService.getOperateLogPage(pageReqVO);
        PageResult<OperateLogRespVO> result = BeanUtils.toBean(pageResult, OperateLogRespVO.class);
        fillTenantNames(result.getList());
        return success(result);
    }


    private void fillTenantNames(List<OperateLogRespVO> logList) {
        if (logList == null || logList.isEmpty()) {
            return;
        }
        Map<Long, String> tenantMap = logList.stream()
                .map(OperateLogRespVO::getTenantId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> {
                            TenantDO tenant = tenantService.getTenant(id);
                            return tenant != null ? tenant.getName() : "";
                        }
                ));
        logList.forEach(log -> {
            if (log.getTenantId() != null) {
                log.setTenantName(tenantMap.get(log.getTenantId()));
            }
        });
    }

    @Operation(summary = "导出操作日志")
    @GetMapping("/export")
    @PreAuthorize("@ss.hasPermission('system:operate-log:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportOperateLog(HttpServletResponse response, @Valid OperateLogPageReqVO exportReqVO) throws IOException {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<OperateLogDO> list = operateLogService.getOperateLogPage(exportReqVO).getList();
        ExcelUtils.write(response, "操作日志.xls", "数据列表", OperateLogRespVO.class,
                TranslateUtils.translate(BeanUtils.toBean(list, OperateLogRespVO.class)));
    }

}
