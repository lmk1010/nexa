package com.kyx.service.hr.dal.mysql.administrative;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.administrative.meeting.vo.HrMeetingPageReqVO;
import com.kyx.service.hr.dal.dataobject.administrative.HrAdministrativeMeetingDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会议室预约 Mapper
 *
 * @author MK
 */
@Mapper
public interface HrAdministrativeMeetingMapper extends BaseMapperX<HrAdministrativeMeetingDO> {

    default PageResult<HrAdministrativeMeetingDO> selectPage(HrMeetingPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HrAdministrativeMeetingDO>()
                .eqIfPresent(HrAdministrativeMeetingDO::getRoomId, reqVO.getRoomId())
                .eqIfPresent(HrAdministrativeMeetingDO::getMeetingType, reqVO.getMeetingType())
                .eqIfPresent(HrAdministrativeMeetingDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(HrAdministrativeMeetingDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(HrAdministrativeMeetingDO::getId));
    }
}
