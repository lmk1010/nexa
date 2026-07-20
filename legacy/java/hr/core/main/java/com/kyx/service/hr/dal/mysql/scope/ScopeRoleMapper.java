package com.kyx.service.hr.dal.mysql.scope;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.dal.dataobject.scope.ScopeRoleDO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ScopeRoleMapper extends BaseMapperX<ScopeRoleDO> {

    default List<ScopeRoleDO> selectListByTenantIds(@Nullable Collection<Long> tenantIds, Integer status) {
        return selectList(new LambdaQueryWrapperX<ScopeRoleDO>()
                .inIfPresent(ScopeRoleDO::getTenantId, tenantIds)
                .eqIfPresent(ScopeRoleDO::getStatus, status)
                .orderByAsc(ScopeRoleDO::getTenantId, ScopeRoleDO::getSort, ScopeRoleDO::getId));
    }
}
