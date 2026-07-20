package com.kyx.service.hr.dal.mysql.employee;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeRecruitmentPublicLinkDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmployeeRecruitmentPublicLinkMapper extends BaseMapperX<EmployeeRecruitmentPublicLinkDO> {

    default EmployeeRecruitmentPublicLinkDO selectByToken(String token) {
        return selectOne(new LambdaQueryWrapperX<EmployeeRecruitmentPublicLinkDO>()
                .eq(EmployeeRecruitmentPublicLinkDO::getToken, token)
                .last("LIMIT 1"));
    }
}
