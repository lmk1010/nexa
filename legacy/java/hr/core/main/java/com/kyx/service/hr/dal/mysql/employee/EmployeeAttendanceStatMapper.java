package com.kyx.service.hr.dal.mysql.employee;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeAttendanceStatDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 员工考勤月度统计 Mapper
 *
 * @author MK
 */
@Mapper
public interface EmployeeAttendanceStatMapper extends BaseMapperX<EmployeeAttendanceStatDO> {

    default EmployeeAttendanceStatDO selectByProfileIdAndMonth(Long profileId, Integer statYear, Integer statMonth) {
        return selectOne(new LambdaQueryWrapperX<EmployeeAttendanceStatDO>()
                .eq(EmployeeAttendanceStatDO::getProfileId, profileId)
                .eq(EmployeeAttendanceStatDO::getStatYear, statYear)
                .eq(EmployeeAttendanceStatDO::getStatMonth, statMonth));
    }

}
