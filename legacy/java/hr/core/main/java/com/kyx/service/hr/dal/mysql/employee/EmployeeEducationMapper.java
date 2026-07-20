package com.kyx.service.hr.dal.mysql.employee;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeEducationDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 员工教育信息 Mapper
 *
 * @author MK
 */
@Mapper
public interface EmployeeEducationMapper extends BaseMapperX<EmployeeEducationDO> {

    default List<EmployeeEducationDO> selectListByProfileId(Long profileId) {
        return selectList(EmployeeEducationDO::getProfileId, profileId);
    }

    default List<EmployeeEducationDO> selectListByEducation(String education) {
        return selectList(EmployeeEducationDO::getEducation, education);
    }

    default List<EmployeeEducationDO> selectListByIsHighest(Boolean isHighest) {
        return selectList(EmployeeEducationDO::getIsHighest, isHighest);
    }

} 