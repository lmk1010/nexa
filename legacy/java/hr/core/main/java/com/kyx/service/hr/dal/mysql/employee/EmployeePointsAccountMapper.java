package com.kyx.service.hr.dal.mysql.employee;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.service.hr.dal.dataobject.employee.EmployeePointsAccountDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 员工积分账户 Mapper
 *
 * @author MK
 */
@Mapper
public interface EmployeePointsAccountMapper extends BaseMapperX<EmployeePointsAccountDO> {

    default EmployeePointsAccountDO selectByProfileId(Long profileId) {
        return selectFirstOne(EmployeePointsAccountDO::getProfileId, profileId);
    }

}
