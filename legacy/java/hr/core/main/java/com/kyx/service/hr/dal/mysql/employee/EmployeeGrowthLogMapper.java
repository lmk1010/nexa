package com.kyx.service.hr.dal.mysql.employee;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeGrowthLogDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 员工成长记录 Mapper
 *
 * @author MK
 */
@Mapper
public interface EmployeeGrowthLogMapper extends BaseMapperX<EmployeeGrowthLogDO> {

    default List<EmployeeGrowthLogDO> selectListByProfileId(Long profileId) {
        return selectList(new LambdaQueryWrapperX<EmployeeGrowthLogDO>()
                .eq(EmployeeGrowthLogDO::getProfileId, profileId)
                .orderByDesc(EmployeeGrowthLogDO::getEventDate)
                .orderByDesc(EmployeeGrowthLogDO::getId));
    }

}
