package com.kyx.service.hr.dal.mysql.employee;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeFamilyDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 员工家庭信息 Mapper
 *
 * @author MK
 */
@Mapper
public interface EmployeeFamilyMapper extends BaseMapperX<EmployeeFamilyDO> {

    default List<EmployeeFamilyDO> selectListByProfileId(Long profileId) {
        return selectList(EmployeeFamilyDO::getProfileId, profileId);
    }

    default List<EmployeeFamilyDO> selectListByRelation(String relation) {
        return selectList(EmployeeFamilyDO::getRelation, relation);
    }

} 