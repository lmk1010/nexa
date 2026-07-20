package com.kyx.service.hr.service.administrative.meetingroom;

import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.hr.controller.admin.administrative.meetingroom.vo.HrMeetingRoomPageReqVO;
import com.kyx.service.hr.controller.admin.administrative.meetingroom.vo.HrMeetingRoomRespVO;
import com.kyx.service.hr.controller.admin.administrative.meetingroom.vo.HrMeetingRoomSaveReqVO;
import com.kyx.service.hr.dal.dataobject.administrative.HrAdministrativeMeetingRoomDO;
import com.kyx.service.hr.dal.mysql.administrative.HrAdministrativeMeetingRoomMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

import static com.kyx.service.hr.enums.ErrorCodeConstants.HR_MEETING_ROOM_CODE_DUPLICATE;
import static com.kyx.service.hr.enums.ErrorCodeConstants.HR_MEETING_ROOM_NOT_EXISTS;

/**
 * 会议室管理 Service 实现类
 *
 * @author MK
 */
@Service
@Validated
@Slf4j
public class HrAdministrativeMeetingRoomServiceImpl implements HrAdministrativeMeetingRoomService {

    @Resource
    private HrAdministrativeMeetingRoomMapper meetingRoomMapper;

    @Override
    public Long createMeetingRoom(HrMeetingRoomSaveReqVO createReqVO) {
        validateRoomCodeUnique(null, createReqVO.getRoomCode());
        HrAdministrativeMeetingRoomDO meetingRoom = BeanUtils.toBean(createReqVO, HrAdministrativeMeetingRoomDO.class);
        meetingRoomMapper.insert(meetingRoom);
        return meetingRoom.getId();
    }

    @Override
    public void updateMeetingRoom(HrMeetingRoomSaveReqVO updateReqVO) {
        validateMeetingRoomExists(updateReqVO.getId());
        validateRoomCodeUnique(updateReqVO.getId(), updateReqVO.getRoomCode());
        HrAdministrativeMeetingRoomDO updateObj = BeanUtils.toBean(updateReqVO, HrAdministrativeMeetingRoomDO.class);
        meetingRoomMapper.updateById(updateObj);
    }

    @Override
    public void deleteMeetingRoom(Long id) {
        validateMeetingRoomExists(id);
        meetingRoomMapper.deleteById(id);
    }

    @Override
    public HrAdministrativeMeetingRoomDO getMeetingRoom(Long id) {
        return meetingRoomMapper.selectById(id);
    }

    @Override
    public PageResult<HrMeetingRoomRespVO> getMeetingRoomPage(HrMeetingRoomPageReqVO pageReqVO) {
        PageResult<HrAdministrativeMeetingRoomDO> pageResult = meetingRoomMapper.selectPage(pageReqVO);
        return BeanUtils.toBean(pageResult, HrMeetingRoomRespVO.class);
    }

    private void validateMeetingRoomExists(Long id) {
        if (meetingRoomMapper.selectById(id) == null) {
            throw ServiceExceptionUtil.exception(HR_MEETING_ROOM_NOT_EXISTS);
        }
    }

    private void validateRoomCodeUnique(Long id, String roomCode) {
        HrAdministrativeMeetingRoomDO room = meetingRoomMapper.selectByRoomCode(roomCode);
        if (room == null) {
            return;
        }
        if (id == null) {
            throw ServiceExceptionUtil.exception(HR_MEETING_ROOM_CODE_DUPLICATE);
        }
        if (!room.getId().equals(id)) {
            throw ServiceExceptionUtil.exception(HR_MEETING_ROOM_CODE_DUPLICATE);
        }
    }
}
