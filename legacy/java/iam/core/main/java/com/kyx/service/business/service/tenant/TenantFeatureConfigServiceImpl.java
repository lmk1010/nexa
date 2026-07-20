package com.kyx.service.business.service.tenant;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.kyx.service.business.controller.admin.tenant.vo.tenant.TenantFeatureConfigSaveReqVO;
import com.kyx.service.business.dal.dataobject.tenant.TenantFeatureConfigDO;
import com.kyx.service.business.dal.mysql.tenant.TenantFeatureConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Validated
public class TenantFeatureConfigServiceImpl implements TenantFeatureConfigService {

    @Resource
    private TenantFeatureConfigMapper tenantFeatureConfigMapper;

    @Override
    public List<TenantFeatureConfigDO> getTenantFeatureConfigs(Long tenantId) {
        if (tenantId == null) {
            return Collections.emptyList();
        }
        return tenantFeatureConfigMapper.selectListByTenantId(tenantId);
    }

    @Override
    public void saveTenantFeatureConfigs(Long tenantId, List<TenantFeatureConfigSaveReqVO> configs) {
        if (tenantId == null || configs == null) {
            return;
        }
        Map<String, TenantFeatureConfigSaveReqVO> normalizedConfigs = new LinkedHashMap<>();
        for (TenantFeatureConfigSaveReqVO config : CollUtil.emptyIfNull(configs)) {
            if (config == null || StrUtil.isBlank(config.getFeatureCode())) {
                continue;
            }
            normalizedConfigs.put(config.getFeatureCode().trim(), config);
        }
        normalizedConfigs.forEach((featureCode, config) -> saveOne(tenantId, featureCode, config));
    }

    private void saveOne(Long tenantId, String featureCode, TenantFeatureConfigSaveReqVO config) {
        TenantFeatureConfigDO existing = tenantFeatureConfigMapper.selectByTenantIdAndFeatureCode(tenantId, featureCode);
        int enabled = Boolean.TRUE.equals(config.getCrossTenantEnabled()) ? 1 : 0;
        if (existing == null) {
            tenantFeatureConfigMapper.insert(new TenantFeatureConfigDO()
                    .setTenantId(tenantId)
                    .setFeatureCode(featureCode)
                    .setCrossTenantEnabled(enabled));
            return;
        }
        tenantFeatureConfigMapper.updateById(new TenantFeatureConfigDO()
                .setId(existing.getId())
                .setCrossTenantEnabled(enabled));
    }

    @Override
    public boolean isCrossTenantEnabled(Long tenantId, String featureCode) {
        if (tenantId == null || StrUtil.isBlank(featureCode)) {
            return false;
        }
        TenantFeatureConfigDO config =
                tenantFeatureConfigMapper.selectByTenantIdAndFeatureCode(tenantId, featureCode.trim());
        return config != null && Integer.valueOf(1).equals(config.getCrossTenantEnabled());
    }

}
