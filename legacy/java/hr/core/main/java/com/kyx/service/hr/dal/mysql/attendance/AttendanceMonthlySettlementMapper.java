package com.kyx.service.hr.dal.mysql.attendance;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceMonthlySettlementPageReqVO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceMonthlySettlementDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 考勤月度结算 Mapper
 */
@Mapper
public interface AttendanceMonthlySettlementMapper extends BaseMapperX<AttendanceMonthlySettlementDO> {

    default AttendanceMonthlySettlementDO selectByMonthAndDept(String settlementMonth, Long deptId) {
        return selectOne(new LambdaQueryWrapperX<AttendanceMonthlySettlementDO>()
                .eq(AttendanceMonthlySettlementDO::getSettlementMonth, settlementMonth)
                .eq(AttendanceMonthlySettlementDO::getDeptId, deptId)
                .last("LIMIT 1"));
    }

    default List<AttendanceMonthlySettlementDO> selectListByMonth(String settlementMonth) {
        return selectList(new LambdaQueryWrapperX<AttendanceMonthlySettlementDO>()
                .eq(AttendanceMonthlySettlementDO::getSettlementMonth, settlementMonth)
                .orderByDesc(AttendanceMonthlySettlementDO::getId));
    }

    default List<AttendanceMonthlySettlementDO> selectLockedListByMonth(String settlementMonth) {
        return selectList(new LambdaQueryWrapperX<AttendanceMonthlySettlementDO>()
                .eq(AttendanceMonthlySettlementDO::getSettlementMonth, settlementMonth)
                .eq(AttendanceMonthlySettlementDO::getStatus, "LOCKED")
                .orderByDesc(AttendanceMonthlySettlementDO::getId));
    }

    default PageResult<AttendanceMonthlySettlementDO> selectPage(AttendanceMonthlySettlementPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AttendanceMonthlySettlementDO>()
                .eqIfPresent(AttendanceMonthlySettlementDO::getSettlementMonth, reqVO.getSettlementMonth())
                .eqIfPresent(AttendanceMonthlySettlementDO::getStatus, reqVO.getStatus())
                .orderByDesc(AttendanceMonthlySettlementDO::getSettlementMonth)
                .orderByDesc(AttendanceMonthlySettlementDO::getId));
    }

}
