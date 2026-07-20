package com.kyx.service.hr.dal.mysql.scope;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.dal.dataobject.scope.ScopeDeptDO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ScopeDeptMapper extends BaseMapperX<ScopeDeptDO> {

    default List<ScopeDeptDO> selectListByTenantIds(@Nullable Collection<Long> tenantIds, Integer status) {
        return selectList(new LambdaQueryWrapperX<ScopeDeptDO>()
                .inIfPresent(ScopeDeptDO::getTenantId, tenantIds)
                .eqIfPresent(ScopeDeptDO::getStatus, status)
                .orderByAsc(ScopeDeptDO::getTenantId, ScopeDeptDO::getParentId, ScopeDeptDO::getSort, ScopeDeptDO::getId));
    }
}
