package com.kyx.service.im.controller.admin.tencent;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.security.core.LoginUser;
import com.kyx.foundation.security.core.util.SecurityFrameworkUtils;
import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.service.im.controller.admin.tencent.vo.TencentImUserMappingRespVO;
import com.kyx.service.im.controller.app.tencent.vo.TencentImLoginRespVO;
import com.kyx.service.im.controller.app.tencent.vo.TencentImUserIdRespVO;
import com.kyx.service.im.dal.dataobject.tencent.TencentImUserMappingDO;
import com.kyx.service.im.service.tencent.TencentImService;
import com.kyx.service.im.service.tencent.TencentImUserMappingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

import static com.kyx.foundation.common.exception.enums.GlobalErrorCodeConstants.UNAUTHORIZED;
import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.foundation.common.pojo.CommonResult.success;

@Tag(name = "Admin - Tencent IM")
@RestController
@RequestMapping("/im/tencent")
@Validated
public class AdminTencentImController {

    @Resource
    private TencentImService tencentImService;
    @Resource
    private TencentImUserMappingService userMappingService;

    @GetMapping("/session")
    @Operation(summary = "Get Tencent IM login ticket")
    public CommonResult<TencentImLoginRespVO> getLoginTicket() {
        return success(tencentImService.getLoginTicket());
    }

    @GetMapping("/user-id")
    @Operation(summary = "Resolve OA user id to Tencent IM user id")
    @Parameter(name = "oaUserId", description = "OA user id", required = true)
    public CommonResult<TencentImUserIdRespVO> getUserId(@RequestParam("oaUserId") Long oaUserId) {
        return success(tencentImService.getUserId(oaUserId));
    }

    @GetMapping("/contacts")
    @Operation(summary = "List Tencent IM contacts from OA mapping")
    public CommonResult<List<TencentImUserMappingRespVO>> getContacts(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "limit", required = false) Integer limit) {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null || loginUser.getId() == null) {
            throw exception(UNAUTHORIZED);
        }

        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            tenantId = loginUser.getTenantId();
        }

        List<TencentImUserMappingDO> contacts = userMappingService.getActiveContactList(
                tenantId, loginUser.getId(), keyword, limit);
        return success(BeanUtils.toBean(contacts, TencentImUserMappingRespVO.class));
    }
}
