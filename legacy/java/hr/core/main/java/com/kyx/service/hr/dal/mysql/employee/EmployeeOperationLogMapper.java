package com.kyx.service.hr.dal.mysql.employee;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeOperationLogDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 员工操作日志 Mapper
 *
 * @author MK
 */
@Mapper
public interface EmployeeOperationLogMapper extends BaseMapperX<EmployeeOperationLogDO> {

    default List<EmployeeOperationLogDO> selectListByProfileId(Long profileId) {
        return selectList(new LambdaQueryWrapperX<EmployeeOperationLogDO>()
                .eq(EmployeeOperationLogDO::getProfileId, profileId)
                .orderByDesc(EmployeeOperationLogDO::getOperationTime)
                .orderByDesc(EmployeeOperationLogDO::getId));
    }

}
