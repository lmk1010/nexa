package com.kyx.service.hr.dal.mysql.scope;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.dal.dataobject.scope.ScopeUserDO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ScopeUserMapper extends BaseMapperX<ScopeUserDO> {

    default List<ScopeUserDO> selectListByTenantIds(@Nullable Collection<Long> tenantIds, Integer status) {
        return selectList(new LambdaQueryWrapperX<ScopeUserDO>()
                .inIfPresent(ScopeUserDO::getTenantId, tenantIds)
                .eqIfPresent(ScopeUserDO::getStatus, status)
                .orderByAsc(ScopeUserDO::getTenantId, ScopeUserDO::getId));
    }

    default List<ScopeUserDO> selectListByDeptIds(@Nullable Collection<Long> deptIds, Integer status) {
        return selectList(new LambdaQueryWrapperX<ScopeUserDO>()
                .inIfPresent(ScopeUserDO::getDeptId, deptIds)
                .eqIfPresent(ScopeUserDO::getStatus, status)
                .orderByAsc(ScopeUserDO::getId));
    }
}
