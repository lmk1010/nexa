package com.kyx.service.biz.controller.admin.executive;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.service.biz.controller.admin.executive.vo.ExecutiveCockpitChatReqVO;
import com.kyx.service.biz.controller.admin.executive.vo.ExecutiveCockpitChatRespVO;
import com.kyx.service.biz.controller.admin.executive.vo.ExecutiveCockpitOverviewRespVO;
import com.kyx.service.biz.service.executive.ExecutiveCockpitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

import static com.kyx.foundation.common.pojo.CommonResult.success;
import static com.kyx.foundation.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "Admin - Executive Cockpit")
@RestController
@RequestMapping("/business/executive-cockpit")
@Validated
public class ExecutiveCockpitController {

    private static final String EXECUTIVE_QUERY_CHECK =
            "@ss.hasAnyRoles('super_admin', 'tenant_admin', 'system_admin', 'biz_boss')"
                    + " || @ss.hasPermission('business:executive-cockpit:query')"
                    + " || @ss.hasPermission('work:requirement:query-all')";

    @Resource
    private ExecutiveCockpitService executiveCockpitService;

    @GetMapping("/overview")
    @Operation(summary = "Get executive cockpit overview")
    @PreAuthorize(EXECUTIVE_QUERY_CHECK)
    public CommonResult<ExecutiveCockpitOverviewRespVO> getOverview(
            @Parameter(description = "Range days", example = "30")
            @RequestParam(value = "days", required = false) Integer days) {
        return success(executiveCockpitService.getOverview(days, getLoginUserId()));
    }

    @PostMapping("/chat")
    @Operation(summary = "Chat with executive cockpit agent")
    @PreAuthorize(EXECUTIVE_QUERY_CHECK)
    public CommonResult<ExecutiveCockpitChatRespVO> chat(@Valid @RequestBody ExecutiveCockpitChatReqVO reqVO) {
        return success(executiveCockpitService.chat(reqVO, getLoginUserId()));
    }

}
