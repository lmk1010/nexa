package com.kyx.service.business.dal.mysql.tenant;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.business.controller.admin.tenant.vo.tenant.TenantPageReqVO;
import com.kyx.service.business.dal.dataobject.tenant.TenantDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 租户 Mapper
 *
 * @author MK
 */
@Mapper
public interface TenantMapper extends BaseMapperX<TenantDO> {

    default PageResult<TenantDO> selectPage(TenantPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TenantDO>()
                .likeIfPresent(TenantDO::getName, reqVO.getName())
                .likeIfPresent(TenantDO::getContactName, reqVO.getContactName())
                .likeIfPresent(TenantDO::getContactMobile, reqVO.getContactMobile())

                .eqIfPresent(TenantDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(TenantDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(TenantDO::getId));
    }

    default TenantDO selectByName(String name) {
        return selectOne(TenantDO::getName, name);
    }

    default TenantDO selectByWebsite(String website) {
        return selectOne(TenantDO::getWebsite, website);
    }

    default Long selectCountByPackageId(Long packageId) {
        return selectCount(TenantDO::getPackageId, packageId);
    }

    default List<TenantDO> selectListByPackageId(Long packageId) {
        return selectList(TenantDO::getPackageId, packageId);
    }

    default List<TenantDO> selectListByStatus(Integer status) {
        return selectList(TenantDO::getStatus, status);
    }

    default List<TenantDO> selectListByRootId(Long rootId) {
        return selectList(TenantDO::getRootId, rootId);
    }

    default List<TenantDO> selectListByIds(List<Long> ids) {
        return selectBatchIds(ids);
    }

    @Update("UPDATE system_tenant " +
            "SET root_id = #{rootId}, " +
            "path = CONCAT(#{newPathPrefix}, SUBSTRING(path, LENGTH(#{oldPathPrefix}) + 1)) " +
            "WHERE path LIKE CONCAT(#{oldPathPrefix}, '%')")
    void updatePathByPrefix(@Param("oldPathPrefix") String oldPathPrefix,
                            @Param("newPathPrefix") String newPathPrefix,
                            @Param("rootId") Long rootId);



}
