package com.kyx.service.hr.dal.mysql.employee;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeCustomFieldDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface EmployeeCustomFieldMapper extends BaseMapperX<EmployeeCustomFieldDO> {

    default EmployeeCustomFieldDO selectByFieldKey(String fieldKey) {
        return selectOne(new LambdaQueryWrapperX<EmployeeCustomFieldDO>()
                .eq(EmployeeCustomFieldDO::getFieldKey, fieldKey)
                .last("LIMIT 1"));
    }

    default List<EmployeeCustomFieldDO> selectListByStatus(Integer status) {
        return selectList(new LambdaQueryWrapperX<EmployeeCustomFieldDO>()
                .eqIfPresent(EmployeeCustomFieldDO::getStatus, status)
                .orderByAsc(EmployeeCustomFieldDO::getFieldGroup)
                .orderByAsc(EmployeeCustomFieldDO::getSortOrder)
                .orderByAsc(EmployeeCustomFieldDO::getId));
    }

}
