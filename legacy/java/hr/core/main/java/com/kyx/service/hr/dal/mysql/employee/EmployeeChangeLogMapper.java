package com.kyx.service.hr.dal.mysql.employee;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeChangeLogDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface EmployeeChangeLogMapper extends BaseMapperX<EmployeeChangeLogDO> {

    default List<EmployeeChangeLogDO> selectListByProfileId(Long profileId) {
        return selectList(new LambdaQueryWrapperX<EmployeeChangeLogDO>()
                .eq(EmployeeChangeLogDO::getProfileId, profileId)
                .orderByDesc(EmployeeChangeLogDO::getOperationTime)
                .orderByDesc(EmployeeChangeLogDO::getId));
    }

}
