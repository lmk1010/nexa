package com.kyx.service.business.controller.admin.logger;

import com.kyx.foundation.apilog.core.annotation.ApiAccessLog;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageParam;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.excel.core.util.ExcelUtils;
import com.kyx.service.business.controller.admin.logger.vo.loginlog.LoginLogPageReqVO;
import com.kyx.service.business.controller.admin.logger.vo.loginlog.LoginLogRespVO;
import com.kyx.service.business.dal.dataobject.logger.LoginLogDO;
import com.kyx.service.business.dal.dataobject.tenant.TenantDO;
import com.kyx.service.business.service.logger.LoginLogService;
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

@Tag(name = "管理后台 - 登录日志")
@RestController
@RequestMapping("/system/login-log")
@Validated
public class LoginLogController {

    @Resource
    private LoginLogService loginLogService;

    @Resource
    private TenantService tenantService;

    @GetMapping("/page")
    @Operation(summary = "获得登录日志分页列表")
    @PreAuthorize("@ss.hasPermission('system:login-log:query')")
    public CommonResult<PageResult<LoginLogRespVO>> getLoginLogPage(@Valid LoginLogPageReqVO pageReqVO) {
        PageResult<LoginLogDO> pageResult = loginLogService.getLoginLogPage(pageReqVO);
        PageResult<LoginLogRespVO> result = BeanUtils.toBean(pageResult, LoginLogRespVO.class);
        fillTenantNames(result.getList());
        return success(result);
    }


    private void fillTenantNames(List<LoginLogRespVO> logList) {
        if (logList == null || logList.isEmpty()) {
            return;
        }
        Map<Long, String> tenantMap = logList.stream()
                .map(LoginLogRespVO::getTenantId)
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

    @GetMapping("/export")
    @Operation(summary = "导出登录日志 Excel")
    @PreAuthorize("@ss.hasPermission('system:login-log:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportLoginLog(HttpServletResponse response, @Valid LoginLogPageReqVO exportReqVO) throws IOException {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<LoginLogDO> list = loginLogService.getLoginLogPage(exportReqVO).getList();
        // 输出
        ExcelUtils.write(response, "登录日志.xls", "数据列表", LoginLogRespVO.class,
                BeanUtils.toBean(list, LoginLogRespVO.class));
    }

}
