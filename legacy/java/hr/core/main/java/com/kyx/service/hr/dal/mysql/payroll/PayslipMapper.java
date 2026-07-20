package com.kyx.service.hr.dal.mysql.payroll;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.payroll.vo.PayslipPageReqVO;
import com.kyx.service.hr.dal.dataobject.payroll.PayslipDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Payslip mapper.
 */
@Mapper
public interface PayslipMapper extends BaseMapperX<PayslipDO> {

    default PayslipDO selectByBatchIdAndUserId(Long batchId, Long userId) {
        return selectOne(new LambdaQueryWrapperX<PayslipDO>()
                .eq(PayslipDO::getBatchId, batchId)
                .eq(PayslipDO::getUserId, userId)
                .last("LIMIT 1"));
    }

    default PageResult<PayslipDO> selectPage(PayslipPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO)
                .orderByAsc(PayslipDO::getStatus)
                .orderByAsc(PayslipDO::getProfileId)
                .orderByDesc(PayslipDO::getId));
    }

    default List<PayslipDO> selectExportList(PayslipPageReqVO reqVO, Integer limit) {
        LambdaQueryWrapperX<PayslipDO> wrapper = buildQuery(reqVO);
        wrapper.orderByAsc(PayslipDO::getPayrollMonth)
                .orderByAsc(PayslipDO::getStatus)
                .orderByAsc(PayslipDO::getProfileId)
                .orderByDesc(PayslipDO::getId);
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        return selectList(wrapper);
    }

    default PageResult<PayslipDO> selectEmployeePage(PayslipPageReqVO reqVO) {
        LambdaQueryWrapperX<PayslipDO> wrapper = buildQuery(reqVO);
        wrapper.ne(PayslipDO::getStatus, "DRAFT")
                .orderByDesc(PayslipDO::getPayrollMonth)
                .orderByDesc(PayslipDO::getId);
        return selectPage(reqVO, wrapper);
    }

    default List<PayslipDO> selectEmployeeYearList(Long userId, String startMonth, String endMonth) {
        return selectList(new LambdaQueryWrapperX<PayslipDO>()
                .eq(PayslipDO::getUserId, userId)
                .between(PayslipDO::getPayrollMonth, startMonth, endMonth)
                .ne(PayslipDO::getStatus, "DRAFT")
                .orderByAsc(PayslipDO::getPayrollMonth)
                .orderByDesc(PayslipDO::getId));
    }

    default List<PayslipDO> selectListByBatchId(Long batchId) {
        return selectList(new LambdaQueryWrapperX<PayslipDO>()
                .eq(PayslipDO::getBatchId, batchId)
                .orderByAsc(PayslipDO::getProfileId)
                .orderByDesc(PayslipDO::getId));
    }

    default List<PayslipDO> selectOpenList(Integer limit) {
        LambdaQueryWrapperX<PayslipDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.in(PayslipDO::getStatus, "PUBLISHED", "ISSUE")
                .orderByAsc(PayslipDO::getPayrollMonth)
                .orderByDesc(PayslipDO::getId);
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        return selectList(wrapper);
    }

    default LambdaQueryWrapperX<PayslipDO> buildQuery(PayslipPageReqVO reqVO) {
        return new LambdaQueryWrapperX<PayslipDO>()
                .eqIfPresent(PayslipDO::getId, reqVO.getId())
                .eqIfPresent(PayslipDO::getBatchId, reqVO.getBatchId())
                .eqIfPresent(PayslipDO::getPayrollMonth, reqVO.getPayrollMonth())
                .eqIfPresent(PayslipDO::getStatus, reqVO.getStatus())
                .eqIfPresent(PayslipDO::getProfileId, reqVO.getProfileId())
                .eqIfPresent(PayslipDO::getUserId, reqVO.getUserId());
    }

}
