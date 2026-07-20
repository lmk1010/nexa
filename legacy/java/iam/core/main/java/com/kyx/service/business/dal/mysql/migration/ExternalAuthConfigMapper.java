package com.kyx.service.business.dal.mysql.migration;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.service.business.dal.dataobject.migration.ExternalAuthConfigDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 外部系统认证配置 Mapper
 * 
 * @author MK
 */
@Mapper
public interface ExternalAuthConfigMapper extends BaseMapperX<ExternalAuthConfigDO> {

    /**
     * 根据系统名称查询
     */
    default ExternalAuthConfigDO selectBySystemName(String systemName) {
        return selectOne(ExternalAuthConfigDO::getSystemName, systemName);
    }

    /**
     * 查询启用的配置列表
     */
    default List<ExternalAuthConfigDO> selectEnabledList() {
        return selectList(ExternalAuthConfigDO::getStatus, 0);
    }
}