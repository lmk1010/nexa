package com.kyx.service.business.dal.mysql.tenant;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.business.controller.admin.tenant.vo.packages.TenantPackagePageReqVO;
import com.kyx.service.business.dal.dataobject.tenant.TenantPackageDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 租户套餐 Mapper
 *
 * @author MK
 */
@Mapper
public interface TenantPackageMapper extends BaseMapperX<TenantPackageDO> {

    default PageResult<TenantPackageDO> selectPage(TenantPackagePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TenantPackageDO>()
                .likeIfPresent(TenantPackageDO::getName, reqVO.getName())
                .eqIfPresent(TenantPackageDO::getStatus, reqVO.getStatus())
                .likeIfPresent(TenantPackageDO::getRemark, reqVO.getRemark())
                .betweenIfPresent(TenantPackageDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(TenantPackageDO::getId));
    }

    default List<TenantPackageDO> selectListByStatus(Integer status) {
        return selectList(TenantPackageDO::getStatus, status);
    }

    default TenantPackageDO selectByName(String name) {
        return selectOne(TenantPackageDO::getName, name);
    }
}
