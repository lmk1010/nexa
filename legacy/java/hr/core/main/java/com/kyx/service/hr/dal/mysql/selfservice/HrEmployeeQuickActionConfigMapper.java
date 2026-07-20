package com.kyx.service.hr.dal.mysql.selfservice;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.dal.dataobject.selfservice.HrEmployeeQuickActionConfigDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface HrEmployeeQuickActionConfigMapper extends BaseMapperX<HrEmployeeQuickActionConfigDO> {

    default HrEmployeeQuickActionConfigDO selectByActionCode(String actionCode) {
        return selectOne(new LambdaQueryWrapperX<HrEmployeeQuickActionConfigDO>()
                .eq(HrEmployeeQuickActionConfigDO::getActionCode, actionCode)
                .last("LIMIT 1"));
    }

    default List<HrEmployeeQuickActionConfigDO> selectListOrdered(Boolean enabledOnly) {
        LambdaQueryWrapperX<HrEmployeeQuickActionConfigDO> wrapper = new LambdaQueryWrapperX<>();
        if (Boolean.TRUE.equals(enabledOnly)) {
            wrapper.eq(HrEmployeeQuickActionConfigDO::getStatus, 0);
        }
        return selectList(wrapper.orderByAsc(HrEmployeeQuickActionConfigDO::getSortOrder)
                .orderByAsc(HrEmployeeQuickActionConfigDO::getId));
    }

}
