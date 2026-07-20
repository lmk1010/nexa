package com.kyx.service.business.service.tenant;

import com.kyx.service.business.controller.admin.tenant.vo.tenant.TenantFeatureConfigSaveReqVO;
import com.kyx.service.business.dal.dataobject.tenant.TenantFeatureConfigDO;

import java.util.List;

public interface TenantFeatureConfigService {

    List<TenantFeatureConfigDO> getTenantFeatureConfigs(Long tenantId);

    void saveTenantFeatureConfigs(Long tenantId, List<TenantFeatureConfigSaveReqVO> configs);

    boolean isCrossTenantEnabled(Long tenantId, String featureCode);

}
