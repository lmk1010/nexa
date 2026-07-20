package com.kyx.service.hr.dal.mysql.scope;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.dal.dataobject.scope.ScopeUserRoleDO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ScopeUserRoleMapper extends BaseMapperX<ScopeUserRoleDO> {

    default List<ScopeUserRoleDO> selectListByRoleIds(@Nullable Collection<Long> roleIds) {
        return selectList(new LambdaQueryWrapperX<ScopeUserRoleDO>()
                .inIfPresent(ScopeUserRoleDO::getRoleId, roleIds));
    }
}
