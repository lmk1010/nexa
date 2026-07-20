package com.kyx.service.hr.dal.mysql.employee;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.dal.dataobject.employee.EmployeePointsDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 员工积分记录 Mapper
 *
 * @author MK
 */
@Mapper
public interface EmployeePointsMapper extends BaseMapperX<EmployeePointsDO> {

    default List<EmployeePointsDO> selectListByProfileId(Long profileId) {
        return selectList(new LambdaQueryWrapperX<EmployeePointsDO>()
                .eq(EmployeePointsDO::getProfileId, profileId)
                .orderByDesc(EmployeePointsDO::getCreateTime)
                .orderByDesc(EmployeePointsDO::getId));
    }

}
