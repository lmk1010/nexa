package com.kyx.service.hr.dal.mysql.employee;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePerformanceSchemePageReqVO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeePerformanceSchemeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.time.LocalDate;

@Mapper
public interface EmployeePerformanceSchemeMapper extends BaseMapperX<EmployeePerformanceSchemeDO> {

    default PageResult<EmployeePerformanceSchemeDO> selectPage(EmployeePerformanceSchemePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<EmployeePerformanceSchemeDO>()
                .likeIfPresent(EmployeePerformanceSchemeDO::getSchemeName, reqVO.getSchemeName())
                .eqIfPresent(EmployeePerformanceSchemeDO::getSchemeCode, reqVO.getSchemeCode())
                .eqIfPresent(EmployeePerformanceSchemeDO::getSchemeType, reqVO.getSchemeType())
                .eqIfPresent(EmployeePerformanceSchemeDO::getCycleType, reqVO.getCycleType())
                .eqIfPresent(EmployeePerformanceSchemeDO::getStatus, reqVO.getStatus())
                .eqIfPresent(EmployeePerformanceSchemeDO::getDefaultFlag, reqVO.getDefaultFlag())
                .orderByDesc(EmployeePerformanceSchemeDO::getDefaultFlag)
                .orderByDesc(EmployeePerformanceSchemeDO::getEffectiveDate)
                .orderByDesc(EmployeePerformanceSchemeDO::getId));
    }

    default List<EmployeePerformanceSchemeDO> selectActiveList() {
        return selectList(new LambdaQueryWrapperX<EmployeePerformanceSchemeDO>()
                .eq(EmployeePerformanceSchemeDO::getStatus, "ACTIVE")
                .orderByDesc(EmployeePerformanceSchemeDO::getDefaultFlag)
                .orderByDesc(EmployeePerformanceSchemeDO::getEffectiveDate)
                .orderByDesc(EmployeePerformanceSchemeDO::getId)
                .last("LIMIT 200"));
    }

    default EmployeePerformanceSchemeDO selectDefaultActive(LocalDate schemeDate) {
        LambdaQueryWrapperX<EmployeePerformanceSchemeDO> wrapper = new LambdaQueryWrapperX<EmployeePerformanceSchemeDO>()
                .eq(EmployeePerformanceSchemeDO::getStatus, "ACTIVE")
                .eq(EmployeePerformanceSchemeDO::getDefaultFlag, true);
        if (schemeDate != null) {
            wrapper.le(EmployeePerformanceSchemeDO::getEffectiveDate, schemeDate)
                    .and(query -> query.isNull(EmployeePerformanceSchemeDO::getExpireDate)
                            .or()
                            .ge(EmployeePerformanceSchemeDO::getExpireDate, schemeDate));
        }
        wrapper.orderByDesc(EmployeePerformanceSchemeDO::getEffectiveDate)
                .orderByDesc(EmployeePerformanceSchemeDO::getId)
                .last("LIMIT 1");
        return selectOne(wrapper);
    }
}
