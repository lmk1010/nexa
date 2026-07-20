package com.kyx.service.hr.dal.mysql.tenant;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.dal.dataobject.tenant.TenantDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * 租户 Mapper（发布范围查询）
 *
 * @author MK
 */
@Mapper
public interface TenantMapper extends BaseMapperX<TenantDO> {

    default List<TenantDO> selectListByRootId(Long rootId) {
        return selectList(new LambdaQueryWrapperX<TenantDO>()
                .eq(TenantDO::getRootId, rootId)
                .orderByAsc(TenantDO::getId));
    }

    default List<TenantDO> selectListByIds(Collection<Long> ids) {
        return selectList(new LambdaQueryWrapperX<TenantDO>()
                .inIfPresent(TenantDO::getId, ids)
                .orderByAsc(TenantDO::getId));
    }
}
