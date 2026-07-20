package com.kyx.service.hr.service.administrative.leave;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.administrative.leave.vo.HrLeaveBalanceRecordPageReqVO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HrLeaveBalanceRecordMapper extends BaseMapperX<HrLeaveBalanceRecordDO> {

    default PageResult<HrLeaveBalanceRecordDO> selectPage(HrLeaveBalanceRecordPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HrLeaveBalanceRecordDO>()
                .eqIfPresent(HrLeaveBalanceRecordDO::getBalanceId, reqVO.getBalanceId())
                .eqIfPresent(HrLeaveBalanceRecordDO::getProfileId, reqVO.getProfileId())
                .eqIfPresent(HrLeaveBalanceRecordDO::getUserId, reqVO.getUserId())
                .eqIfPresent(HrLeaveBalanceRecordDO::getLeaveTypeCode, reqVO.getLeaveTypeCode())
                .eqIfPresent(HrLeaveBalanceRecordDO::getYear, reqVO.getYear())
                .eqIfPresent(HrLeaveBalanceRecordDO::getChangeType, reqVO.getChangeType())
                .betweenIfPresent(HrLeaveBalanceRecordDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(HrLeaveBalanceRecordDO::getId));
    }

}
