package com.kyx.service.hr.dal.mysql.employee;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeCustomFieldValueDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface EmployeeCustomFieldValueMapper extends BaseMapperX<EmployeeCustomFieldValueDO> {

    default List<EmployeeCustomFieldValueDO> selectListByProfileId(Long profileId) {
        return selectList(new LambdaQueryWrapperX<EmployeeCustomFieldValueDO>()
                .eq(EmployeeCustomFieldValueDO::getProfileId, profileId)
                .orderByAsc(EmployeeCustomFieldValueDO::getFieldId));
    }

    default EmployeeCustomFieldValueDO selectByProfileAndField(Long profileId, Long fieldId) {
        return selectOne(new LambdaQueryWrapperX<EmployeeCustomFieldValueDO>()
                .eq(EmployeeCustomFieldValueDO::getProfileId, profileId)
                .eq(EmployeeCustomFieldValueDO::getFieldId, fieldId)
                .last("LIMIT 1"));
    }

}
