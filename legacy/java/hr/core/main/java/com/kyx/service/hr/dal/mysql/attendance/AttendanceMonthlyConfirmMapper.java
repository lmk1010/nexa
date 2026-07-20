package com.kyx.service.hr.dal.mysql.attendance;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceMonthlyConfirmPageReqVO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceMonthlyConfirmDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * Monthly attendance confirmation mapper.
 */
@Mapper
public interface AttendanceMonthlyConfirmMapper extends BaseMapperX<AttendanceMonthlyConfirmDO> {

    default AttendanceMonthlyConfirmDO selectBySettlementIdAndUserId(Long settlementId, Long userId) {
        return selectOne(new LambdaQueryWrapperX<AttendanceMonthlyConfirmDO>()
                .eq(AttendanceMonthlyConfirmDO::getSettlementId, settlementId)
                .eq(AttendanceMonthlyConfirmDO::getUserId, userId)
                .last("LIMIT 1"));
    }

    default PageResult<AttendanceMonthlyConfirmDO> selectPage(AttendanceMonthlyConfirmPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO)
                .orderByAsc(AttendanceMonthlyConfirmDO::getStatus)
                .orderByAsc(AttendanceMonthlyConfirmDO::getUserId)
                .orderByDesc(AttendanceMonthlyConfirmDO::getId));
    }

    default List<AttendanceMonthlyConfirmDO> selectListBySettlementId(Long settlementId) {
        return selectList(new LambdaQueryWrapperX<AttendanceMonthlyConfirmDO>()
                .eq(AttendanceMonthlyConfirmDO::getSettlementId, settlementId)
                .orderByAsc(AttendanceMonthlyConfirmDO::getUserId)
                .orderByDesc(AttendanceMonthlyConfirmDO::getId));
    }

    default List<AttendanceMonthlyConfirmDO> selectListBySettlementIdsAndUserIds(Collection<Long> settlementIds,
                                                                                 Collection<Long> userIds) {
        return selectList(new LambdaQueryWrapperX<AttendanceMonthlyConfirmDO>()
                .in(AttendanceMonthlyConfirmDO::getSettlementId, settlementIds)
                .in(AttendanceMonthlyConfirmDO::getUserId, userIds)
                .orderByAsc(AttendanceMonthlyConfirmDO::getUserId)
                .orderByDesc(AttendanceMonthlyConfirmDO::getId));
    }

    default List<AttendanceMonthlyConfirmDO> selectOpenList(Integer limit) {
        LambdaQueryWrapperX<AttendanceMonthlyConfirmDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.in(AttendanceMonthlyConfirmDO::getStatus, "PENDING", "ISSUE")
                .orderByAsc(AttendanceMonthlyConfirmDO::getSettlementMonth)
                .orderByDesc(AttendanceMonthlyConfirmDO::getId);
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        return selectList(wrapper);
    }

    default Long selectCountBySettlementIdAndStatus(Long settlementId, String status) {
        return selectCount(new LambdaQueryWrapperX<AttendanceMonthlyConfirmDO>()
                .eq(AttendanceMonthlyConfirmDO::getSettlementId, settlementId)
                .eq(AttendanceMonthlyConfirmDO::getStatus, status));
    }

    default LambdaQueryWrapperX<AttendanceMonthlyConfirmDO> buildQuery(AttendanceMonthlyConfirmPageReqVO reqVO) {
        return new LambdaQueryWrapperX<AttendanceMonthlyConfirmDO>()
                .eqIfPresent(AttendanceMonthlyConfirmDO::getSettlementId, reqVO.getSettlementId())
                .eqIfPresent(AttendanceMonthlyConfirmDO::getSettlementMonth, reqVO.getSettlementMonth())
                .eqIfPresent(AttendanceMonthlyConfirmDO::getStatus, reqVO.getStatus())
                .eqIfPresent(AttendanceMonthlyConfirmDO::getProfileId, reqVO.getProfileId())
                .eqIfPresent(AttendanceMonthlyConfirmDO::getUserId, reqVO.getUserId());
    }

}
