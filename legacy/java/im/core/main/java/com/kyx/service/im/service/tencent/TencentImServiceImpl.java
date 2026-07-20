package com.kyx.service.im.service.tencent;

import cn.hutool.core.util.StrUtil;
import com.kyx.foundation.security.core.LoginUser;
import com.kyx.foundation.security.core.util.SecurityFrameworkUtils;
import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.service.im.config.tencent.TencentImProperties;
import com.kyx.service.im.config.tencent.TencentImUserSigGenerator;
import com.kyx.service.im.controller.app.tencent.vo.TencentImLoginRespVO;
import com.kyx.service.im.controller.app.tencent.vo.TencentImUserIdRespVO;
import com.kyx.service.im.dal.dataobject.tencent.TencentImUserMappingDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

import static com.kyx.foundation.common.exception.enums.GlobalErrorCodeConstants.UNAUTHORIZED;
import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.service.im.enums.ErrorCodeConstants.TENCENT_IM_CONFIG_INVALID;
import static com.kyx.service.im.enums.ErrorCodeConstants.TENCENT_IM_MAPPING_NOT_EXISTS;

@Service
@Validated
@Slf4j
public class TencentImServiceImpl implements TencentImService {

    private static final long DEFAULT_TENANT_ID = 0L;
    private static final int STATUS_ENABLED = 0;

    @Resource
    private TencentImProperties properties;
    @Resource
    private TencentImUserSigGenerator userSigGenerator;
    @Resource
    private TencentImUserMappingService userMappingService;

    @Override
    public TencentImLoginRespVO getLoginTicket() {
        validateConfig();

        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null || loginUser.getId() == null) {
            throw exception(UNAUTHORIZED);
        }

        Long tenantId = resolveTenantId(loginUser);

        TencentImUserMappingDO mapping = userMappingService.getActiveByOaUser(tenantId, loginUser.getId());
        if (mapping == null) {
            if (!properties.isAutoCreateMapping()) {
                throw exception(TENCENT_IM_MAPPING_NOT_EXISTS);
            }
            mapping = userMappingService.createDefaultMapping(
                    tenantId,
                    loginUser.getId(),
                    SecurityFrameworkUtils.getLoginUserNickname());
        }

        long now = System.currentTimeMillis() / 1000;
        String userSig = userSigGenerator.genUserSig(mapping.getImUserId());
        return new TencentImLoginRespVO()
                .setSdkAppId(properties.getSdkAppId())
                .setUserID(mapping.getImUserId())
                .setUserSig(userSig)
                .setExpire(now + properties.getExpireSeconds())
                .setOaUserId(loginUser.getId())
                .setTenantId(tenantId);
    }

    @Override
    public TencentImUserIdRespVO getUserId(Long oaUserId) {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null || loginUser.getId() == null) {
            throw exception(UNAUTHORIZED);
        }

        Long tenantId = resolveTenantId(loginUser);
        TencentImUserMappingDO mapping = userMappingService.getActiveByOaUser(tenantId, oaUserId);
        if (mapping == null) {
            throw exception(TENCENT_IM_MAPPING_NOT_EXISTS);
        }
        return new TencentImUserIdRespVO()
                .setOaUserId(oaUserId)
                .setTenantId(mapping.getTenantId())
                .setUserID(mapping.getImUserId());
    }

    private void validateConfig() {
        if (properties.getSdkAppId() <= 0 || StrUtil.isBlank(properties.getSecretKey())) {
            throw exception(TENCENT_IM_CONFIG_INVALID);
        }
    }

    private Long resolveTenantId(LoginUser loginUser) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            tenantId = loginUser.getTenantId();
        }
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }
}
