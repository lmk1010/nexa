package com.kyx.service.hr.controller.admin.manager;

import com.kyx.foundation.apilog.core.annotation.ApiAccessLog;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.excel.core.util.ExcelUtils;
import com.kyx.service.hr.controller.admin.manager.vo.HrManagerSelfServiceRespVO;
import com.kyx.service.hr.controller.admin.manager.vo.HrManagerTeamExportRespVO;
import com.kyx.service.hr.service.manager.HrManagerSelfServiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import static com.kyx.foundation.apilog.core.enums.OperateTypeEnum.EXPORT;
import static com.kyx.foundation.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 经理自助")
@RestController
@RequestMapping("/hr/manager-self-service")
@Validated
public class HrManagerSelfServiceController {

    @Resource
    private HrManagerSelfServiceService hrManagerSelfServiceService;

    @GetMapping("/home")
    @Operation(summary = "获得经理自助首页")
    @PreAuthorize("@ss.hasAnyPermissions('hr:manager:self,hr:employee:query')")
    public CommonResult<HrManagerSelfServiceRespVO> getHome() {
        return success(hrManagerSelfServiceService.getHome());
    }

    @GetMapping("/team/export-excel")
    @Operation(summary = "导出经理团队成员 Excel")
    @PreAuthorize("@ss.hasAnyPermissions('hr:manager:self,hr:employee:query')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportTeamExcel(HttpServletResponse response) throws IOException {
        List<HrManagerTeamExportRespVO> list = hrManagerSelfServiceService.getTeamExportList();
        ExcelUtils.write(response, "团队成员.xls", "数据", HrManagerTeamExportRespVO.class, list);
    }

}
