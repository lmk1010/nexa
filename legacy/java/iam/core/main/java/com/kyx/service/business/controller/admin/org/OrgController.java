package com.kyx.service.business.controller.admin.org;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.service.business.controller.admin.org.vo.OrgTreeNodeRespVO;
import com.kyx.service.business.service.org.OrgService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

import static com.kyx.foundation.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 组织架构")
@RestController
@RequestMapping("/system/org")
@Validated
public class OrgController {

    @Resource
    private OrgService orgService;

    @GetMapping("/tree")
    @Operation(summary = "获取组织架构树", description = "返回租户+部门合并的扁平列表，前端用 handleTree 构建树形结构")
    @PreAuthorize("@ss.hasAnyPermissions('system:dept:query','hr:employee:query')")
    public CommonResult<List<OrgTreeNodeRespVO>> getOrgTree() {
        return success(orgService.getOrgTree());
    }

}
