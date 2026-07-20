package com.kyx.service.hr.dal.mysql.employee;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeSavedViewDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface EmployeeSavedViewMapper extends BaseMapperX<EmployeeSavedViewDO> {

    default List<EmployeeSavedViewDO> selectListByUserId(Long userId) {
        return selectList(new LambdaQueryWrapperX<EmployeeSavedViewDO>()
                .eq(EmployeeSavedViewDO::getUserId, userId)
                .orderByDesc(EmployeeSavedViewDO::getDefaultView)
                .orderByDesc(EmployeeSavedViewDO::getId));
    }

    default void clearDefaultByUserId(Long userId) {
        update(null, new LambdaUpdateWrapper<EmployeeSavedViewDO>()
                .eq(EmployeeSavedViewDO::getUserId, userId)
                .set(EmployeeSavedViewDO::getDefaultView, false));
    }

}
