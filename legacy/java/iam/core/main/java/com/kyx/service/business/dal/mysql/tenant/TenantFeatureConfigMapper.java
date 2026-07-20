package com.kyx.service.business.dal.mysql.tenant;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.business.dal.dataobject.tenant.TenantFeatureConfigDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TenantFeatureConfigMapper extends BaseMapperX<TenantFeatureConfigDO> {

    default List<TenantFeatureConfigDO> selectListByTenantId(Long tenantId) {
        return selectList(new LambdaQueryWrapperX<TenantFeatureConfigDO>()
                .eq(TenantFeatureConfigDO::getTenantId, tenantId)
                .orderByAsc(TenantFeatureConfigDO::getId));
    }

    default TenantFeatureConfigDO selectByTenantIdAndFeatureCode(Long tenantId, String featureCode) {
        return selectOne(new LambdaQueryWrapperX<TenantFeatureConfigDO>()
                .eq(TenantFeatureConfigDO::getTenantId, tenantId)
                .eq(TenantFeatureConfigDO::getFeatureCode, featureCode));
    }

    default void deleteByTenantIdAndFeatureCode(Long tenantId, String featureCode) {
        delete(new LambdaQueryWrapperX<TenantFeatureConfigDO>()
                .eq(TenantFeatureConfigDO::getTenantId, tenantId)
                .eq(TenantFeatureConfigDO::getFeatureCode, featureCode));
    }

}
