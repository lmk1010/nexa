package com.kyx.service.hr.dal.mysql.payroll;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.payroll.vo.PayrollBatchPageReqVO;
import com.kyx.service.hr.dal.dataobject.payroll.PayrollBatchDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * Payroll batch mapper.
 */
@Mapper
public interface PayrollBatchMapper extends BaseMapperX<PayrollBatchDO> {

    default PayrollBatchDO selectByMonth(String payrollMonth) {
        return selectOne(new LambdaQueryWrapperX<PayrollBatchDO>()
                .eq(PayrollBatchDO::getPayrollMonth, payrollMonth)
                .last("LIMIT 1"));
    }

    default PageResult<PayrollBatchDO> selectPage(PayrollBatchPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<PayrollBatchDO>()
                .eqIfPresent(PayrollBatchDO::getPayrollMonth, reqVO.getPayrollMonth())
                .eqIfPresent(PayrollBatchDO::getStatus, reqVO.getStatus())
                .orderByDesc(PayrollBatchDO::getPayrollMonth)
                .orderByDesc(PayrollBatchDO::getId));
    }

}
