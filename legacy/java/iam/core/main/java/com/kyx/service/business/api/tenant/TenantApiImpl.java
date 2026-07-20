package com.kyx.service.business.api.tenant;

import com.kyx.foundation.common.biz.system.tenant.TenantCommonApi;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.tenant.core.aop.TenantIgnore;
import com.kyx.service.business.service.tenant.TenantFeatureConfigService;
import com.kyx.service.business.service.tenant.TenantService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

import static com.kyx.foundation.common.pojo.CommonResult.success;

@RestController // 提供 RESTful API 接口，给 Feign 调用
@Validated
public class TenantApiImpl implements TenantCommonApi {

    @Resource
    private TenantService tenantService;
    @Resource
    private TenantFeatureConfigService tenantFeatureConfigService;

    @Override
    @TenantIgnore // 防止递归。避免调用 /rpc-api/system/tenant/valid 接口时，又去触发 /rpc-api/system/tenant/valid 去校验
    public CommonResult<List<Long>> getTenantIdList() {
        return success(tenantService.getTenantIdList());
    }

    @Override
    @TenantIgnore // 获得租户列表的时候，无需传递租户编号
    public CommonResult<Boolean> validTenant(Long id) {
        tenantService.validTenant(id);
        return success(true);
    }

    @Override
    @TenantIgnore // 获取全局视图配置时，无需传递租户编号
    public CommonResult<Boolean> isGlobalView(Long id) {
        return success(tenantService.isGlobalView(id));
    }

    @Override
    @TenantIgnore
    public CommonResult<String> getViewScope(Long id) {
        return success(tenantService.getViewScope(id).getCode());
    }

    @Override
    @TenantIgnore
    public CommonResult<List<Long>> getAllowedTenantIds(Long id) {
        return success(tenantService.getAllowedTenantIds(id));
    }

    @Override
    @TenantIgnore
    public CommonResult<List<Long>> getCollaborationTenantIds(Long id) {
        return success(tenantService.getCollaborationTenantIds(id));
    }

    @Override
    @TenantIgnore // 检查用户租户关系时，无需传递租户编号
    public CommonResult<Boolean> checkUserTenantAccess(Long userId, Long tenantId) {
        return success(tenantService.checkUserTenantAccess(userId, tenantId));
    }

    @Override
    @TenantIgnore
    public CommonResult<Boolean> isFeatureCrossTenantEnabled(Long tenantId, String featureCode) {
        return success(tenantFeatureConfigService.isCrossTenantEnabled(tenantId, featureCode));
    }

}
