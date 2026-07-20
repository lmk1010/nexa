package com.kyx.service.hr.dal.mysql.employee;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeProfileDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 员工档案 Mapper
 *
 * @author MK
 */
@Mapper
public interface EmployeeProfileMapper extends BaseMapperX<EmployeeProfileDO> {

    default EmployeeProfileDO selectByProfileNo(String profileNo) {
        return selectFirstOne(EmployeeProfileDO::getProfileNo, profileNo);
    }

    default EmployeeProfileDO selectByIdNumber(String idNumber) {
        return selectFirstOne(EmployeeProfileDO::getIdNumber, idNumber);
    }

    default EmployeeProfileDO selectByUserId(Long userId) {
        return selectFirstOne(EmployeeProfileDO::getUserId, userId);
    }

    default List<EmployeeProfileDO> selectListByName(String name) {
        return selectList(new LambdaQueryWrapperX<EmployeeProfileDO>()
                .likeIfPresent(EmployeeProfileDO::getName, name));
    }

    default List<EmployeeProfileDO> selectListByMobile(String mobile) {
        return selectList(new LambdaQueryWrapperX<EmployeeProfileDO>()
                .likeIfPresent(EmployeeProfileDO::getMobile, mobile));
    }

    default List<EmployeeProfileDO> selectListByEmail(String email) {
        return selectList(new LambdaQueryWrapperX<EmployeeProfileDO>()
                .likeIfPresent(EmployeeProfileDO::getEmail, email));
    }

    default List<EmployeeProfileDO> selectListByStatus(Integer status) {
        return selectList(EmployeeProfileDO::getStatus, status);
    }

    /**
     * 查询当天最大档案编号
     *
     * @param dateStr 日期字符串，格式：yyyyMMdd
     * @return 最大档案编号
     */
    default String selectMaxProfileNoByDate(String dateStr) {
        List<EmployeeProfileDO> results = selectList(new LambdaQueryWrapperX<EmployeeProfileDO>()
                .likeRight(EmployeeProfileDO::getProfileNo, "PROF" + dateStr)
                .orderByDesc(EmployeeProfileDO::getProfileNo)
                .last("LIMIT 1"));
        return results.isEmpty() ? null : results.get(0).getProfileNo();
    }

} 
