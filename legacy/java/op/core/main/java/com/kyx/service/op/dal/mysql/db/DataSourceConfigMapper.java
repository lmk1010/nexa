package com.kyx.service.op.dal.mysql.db;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.service.op.dal.dataobject.db.DataSourceConfigDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据源配置 Mapper
 *
 * @author MK
 */
@Mapper
public interface DataSourceConfigMapper extends BaseMapperX<DataSourceConfigDO> {
}
