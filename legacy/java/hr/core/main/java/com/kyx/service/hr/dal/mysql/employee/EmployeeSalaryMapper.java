package com.kyx.service.hr.dal.mysql.employee;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeSalaryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface EmployeeSalaryMapper extends BaseMapperX<EmployeeSalaryDO> {

    default List<EmployeeSalaryDO> selectListByProfileId(Long profileId) {
        return selectList(EmployeeSalaryDO::getProfileId, profileId);
    }
}
