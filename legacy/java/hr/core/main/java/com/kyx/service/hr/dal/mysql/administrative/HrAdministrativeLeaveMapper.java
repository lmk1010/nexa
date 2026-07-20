package com.kyx.service.hr.dal.mysql.administrative;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.administrative.leave.vo.HrLeavePageReqVO;
import com.kyx.service.hr.dal.dataobject.administrative.HrAdministrativeLeaveDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 请假申请 Mapper
 *
 * @author MK
 */
@Mapper
public interface HrAdministrativeLeaveMapper extends BaseMapperX<HrAdministrativeLeaveDO> {

    default PageResult<HrAdministrativeLeaveDO> selectPage(HrLeavePageReqVO reqVO) {
        LambdaQueryWrapperX<HrAdministrativeLeaveDO> wrapper = new LambdaQueryWrapperX<HrAdministrativeLeaveDO>()
                .eqIfPresent(HrAdministrativeLeaveDO::getUserId, reqVO.getUserId())
                .eqIfPresent(HrAdministrativeLeaveDO::getLeaveType, reqVO.getLeaveType())
                .eqIfPresent(HrAdministrativeLeaveDO::getLeaveCategory, reqVO.getLeaveCategory())
                .eqIfPresent(HrAdministrativeLeaveDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(HrAdministrativeLeaveDO::getCreateTime, reqVO.getCreateTime());
        LocalDateTime[] leaveTime = reqVO.getLeaveTime();
        if (leaveTime != null && leaveTime.length >= 2) {
            wrapper.leIfPresent(HrAdministrativeLeaveDO::getStartTime, leaveTime[1])
                    .geIfPresent(HrAdministrativeLeaveDO::getEndTime, leaveTime[0]);
        }
        return selectPage(reqVO, wrapper.orderByDesc(HrAdministrativeLeaveDO::getId));
    }

    default HrAdministrativeLeaveDO selectByProcessInstanceId(String processInstanceId) {
        return selectOne(new LambdaQueryWrapperX<HrAdministrativeLeaveDO>()
                .eq(HrAdministrativeLeaveDO::getProcessInstanceId, processInstanceId)
                .orderByDesc(HrAdministrativeLeaveDO::getId)
                .last("LIMIT 1"));
    }

    default Long selectCountByUserIdAndStatus(Long userId, Integer status) {
        return selectCount(new LambdaQueryWrapperX<HrAdministrativeLeaveDO>()
                .eq(HrAdministrativeLeaveDO::getUserId, userId)
                .eqIfPresent(HrAdministrativeLeaveDO::getStatus, status));
    }

    default List<HrAdministrativeLeaveDO> selectListByUserIdAndTimeRange(Long userId,
                                                                         LocalDateTime startTime,
                                                                         LocalDateTime endTime) {
        return selectList(new LambdaQueryWrapperX<HrAdministrativeLeaveDO>()
                .eq(HrAdministrativeLeaveDO::getUserId, userId)
                .in(HrAdministrativeLeaveDO::getStatus, 1, 2)
                .le(HrAdministrativeLeaveDO::getStartTime, endTime)
                .ge(HrAdministrativeLeaveDO::getEndTime, startTime)
                .orderByDesc(HrAdministrativeLeaveDO::getId));
    }

}
