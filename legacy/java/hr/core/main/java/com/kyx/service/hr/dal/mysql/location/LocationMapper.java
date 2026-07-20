package com.kyx.service.hr.dal.mysql.location;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.location.vo.LocationPageReqVO;
import com.kyx.service.hr.dal.dataobject.location.LocationDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 公司地点管理 Mapper
 *
 * @author MK
 */
@Mapper
public interface LocationMapper extends BaseMapperX<LocationDO> {

    default PageResult<LocationDO> selectPage(LocationPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<LocationDO>()
                .likeIfPresent(LocationDO::getLocationName, reqVO.getLocationName())
                .orderByDesc(LocationDO::getId));
    }

    default LocationDO selectByLocationName(String locationName) {
        return selectOne(LocationDO::getLocationName, locationName);
    }

} 