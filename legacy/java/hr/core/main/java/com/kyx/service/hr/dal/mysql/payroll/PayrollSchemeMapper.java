package com.kyx.service.hr.dal.mysql.payroll;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.payroll.vo.PayrollSchemePageReqVO;
import com.kyx.service.hr.dal.dataobject.payroll.PayrollSchemeDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;

@Mapper
public interface PayrollSchemeMapper extends BaseMapperX<PayrollSchemeDO> {

    default PageResult<PayrollSchemeDO> selectPage(PayrollSchemePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<PayrollSchemeDO>()
                .likeIfPresent(PayrollSchemeDO::getSchemeName, reqVO.getSchemeName())
                .eqIfPresent(PayrollSchemeDO::getSchemeCode, reqVO.getSchemeCode())
                .eqIfPresent(PayrollSchemeDO::getStatus, reqVO.getStatus())
                .eqIfPresent(PayrollSchemeDO::getDefaultFlag, reqVO.getDefaultFlag())
                .orderByDesc(PayrollSchemeDO::getDefaultFlag)
                .orderByDesc(PayrollSchemeDO::getEffectiveDate)
                .orderByDesc(PayrollSchemeDO::getId));
    }

    default PayrollSchemeDO selectDefaultActive(LocalDate payrollDate) {
        LambdaQueryWrapperX<PayrollSchemeDO> wrapper = new LambdaQueryWrapperX<PayrollSchemeDO>()
                .eq(PayrollSchemeDO::getStatus, "ACTIVE")
                .eq(PayrollSchemeDO::getDefaultFlag, true);
        if (payrollDate != null) {
            wrapper.le(PayrollSchemeDO::getEffectiveDate, payrollDate)
                    .and(w -> w.isNull(PayrollSchemeDO::getExpireDate)
                            .or()
                            .ge(PayrollSchemeDO::getExpireDate, payrollDate));
        }
        wrapper.orderByDesc(PayrollSchemeDO::getEffectiveDate)
                .orderByDesc(PayrollSchemeDO::getId)
                .last("LIMIT 1");
        return selectOne(wrapper);
    }

}
