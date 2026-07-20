package com.kyx.service.hr.dal.mysql.administrative;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.administrative.leave.vo.HrLeaveBalancePageReqVO;
import com.kyx.service.hr.dal.dataobject.administrative.HrLeaveBalanceDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 假期余额 Mapper
 */
@Mapper
public interface HrLeaveBalanceMapper extends BaseMapperX<HrLeaveBalanceDO> {

    default HrLeaveBalanceDO selectByUserYearType(Long userId, Integer year, String leaveTypeCode) {
        return selectOne(new LambdaQueryWrapperX<HrLeaveBalanceDO>()
                .eq(HrLeaveBalanceDO::getUserId, userId)
                .eq(HrLeaveBalanceDO::getYear, year)
                .eq(HrLeaveBalanceDO::getLeaveTypeCode, leaveTypeCode)
                .last("LIMIT 1"));
    }

    default List<HrLeaveBalanceDO> selectListByUserYear(Long userId, Integer year) {
        return selectList(new LambdaQueryWrapperX<HrLeaveBalanceDO>()
                .eq(HrLeaveBalanceDO::getUserId, userId)
                .eqIfPresent(HrLeaveBalanceDO::getYear, year)
                .orderByDesc(HrLeaveBalanceDO::getYear)
                .orderByAsc(HrLeaveBalanceDO::getLeaveTypeId));
    }

    default PageResult<HrLeaveBalanceDO> selectPage(HrLeaveBalancePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HrLeaveBalanceDO>()
                .eqIfPresent(HrLeaveBalanceDO::getProfileId, reqVO.getProfileId())
                .eqIfPresent(HrLeaveBalanceDO::getUserId, reqVO.getUserId())
                .eqIfPresent(HrLeaveBalanceDO::getLeaveTypeCode, reqVO.getLeaveTypeCode())
                .eqIfPresent(HrLeaveBalanceDO::getYear, reqVO.getYear())
                .orderByDesc(HrLeaveBalanceDO::getYear)
                .orderByAsc(HrLeaveBalanceDO::getProfileId)
                .orderByAsc(HrLeaveBalanceDO::getLeaveTypeId));
    }

}
