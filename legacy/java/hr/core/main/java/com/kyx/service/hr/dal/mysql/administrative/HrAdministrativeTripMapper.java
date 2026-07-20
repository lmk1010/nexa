package com.kyx.service.hr.dal.mysql.administrative;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.administrative.trip.vo.HrTripPageReqVO;
import com.kyx.service.hr.dal.dataobject.administrative.HrAdministrativeTripDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 出差申请 Mapper
 *
 * @author MK
 */
@Mapper
public interface HrAdministrativeTripMapper extends BaseMapperX<HrAdministrativeTripDO> {

    default PageResult<HrAdministrativeTripDO> selectPage(HrTripPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HrAdministrativeTripDO>()
                .eqIfPresent(HrAdministrativeTripDO::getTripType, reqVO.getTripType())
                .eqIfPresent(HrAdministrativeTripDO::getDestinationCity, reqVO.getDestinationCity())
                .eqIfPresent(HrAdministrativeTripDO::getTransportType, reqVO.getTransportType())
                .eqIfPresent(HrAdministrativeTripDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(HrAdministrativeTripDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(HrAdministrativeTripDO::getId));
    }

    default Long selectCountByUserIdAndStatus(Long userId, Integer status) {
        return selectCount(new LambdaQueryWrapperX<HrAdministrativeTripDO>()
                .eq(HrAdministrativeTripDO::getUserId, userId)
                .eqIfPresent(HrAdministrativeTripDO::getStatus, status));
    }

    default List<HrAdministrativeTripDO> selectListByUserIdAndTimeRange(Long userId,
                                                                        LocalDateTime startTime,
                                                                        LocalDateTime endTime) {
        return selectList(new LambdaQueryWrapperX<HrAdministrativeTripDO>()
                .eq(HrAdministrativeTripDO::getUserId, userId)
                .in(HrAdministrativeTripDO::getStatus, 1, 2)
                .le(HrAdministrativeTripDO::getStartTime, endTime)
                .ge(HrAdministrativeTripDO::getEndTime, startTime)
                .orderByDesc(HrAdministrativeTripDO::getId));
    }
}
