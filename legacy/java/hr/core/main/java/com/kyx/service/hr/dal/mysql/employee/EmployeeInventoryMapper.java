package com.kyx.service.hr.dal.mysql.employee;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeInventoryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface EmployeeInventoryMapper extends BaseMapperX<EmployeeInventoryDO> {

    default List<EmployeeInventoryDO> selectListByProfileId(Long profileId) {
        return selectList(EmployeeInventoryDO::getProfileId, profileId);
    }
}
