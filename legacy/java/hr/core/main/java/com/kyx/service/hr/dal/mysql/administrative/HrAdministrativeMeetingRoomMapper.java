package com.kyx.service.hr.dal.mysql.administrative;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.administrative.meetingroom.vo.HrMeetingRoomPageReqVO;
import com.kyx.service.hr.dal.dataobject.administrative.HrAdministrativeMeetingRoomDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会议室管理 Mapper
 *
 * @author MK
 */
@Mapper
public interface HrAdministrativeMeetingRoomMapper extends BaseMapperX<HrAdministrativeMeetingRoomDO> {

    default PageResult<HrAdministrativeMeetingRoomDO> selectPage(HrMeetingRoomPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HrAdministrativeMeetingRoomDO>()
                .likeIfPresent(HrAdministrativeMeetingRoomDO::getRoomCode, reqVO.getRoomCode())
                .likeIfPresent(HrAdministrativeMeetingRoomDO::getRoomName, reqVO.getRoomName())
                .eqIfPresent(HrAdministrativeMeetingRoomDO::getFloor, reqVO.getFloor())
                .eqIfPresent(HrAdministrativeMeetingRoomDO::getStatus, reqVO.getStatus())
                .orderByDesc(HrAdministrativeMeetingRoomDO::getId));
    }

    default HrAdministrativeMeetingRoomDO selectByRoomCode(String roomCode) {
        return selectOne(HrAdministrativeMeetingRoomDO::getRoomCode, roomCode);
    }
}
