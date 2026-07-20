package com.kyx.service.hr.service.administrative.meetingroom;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.administrative.meetingroom.vo.HrMeetingRoomPageReqVO;
import com.kyx.service.hr.controller.admin.administrative.meetingroom.vo.HrMeetingRoomRespVO;
import com.kyx.service.hr.controller.admin.administrative.meetingroom.vo.HrMeetingRoomSaveReqVO;
import com.kyx.service.hr.dal.dataobject.administrative.HrAdministrativeMeetingRoomDO;

/**
 * 会议室管理 Service 接口
 *
 * @author MK
 */
public interface HrAdministrativeMeetingRoomService {

    /**
     * 创建会议室
     *
     * @param createReqVO 创建信息
     * @return 会议室编号
     */
    Long createMeetingRoom(HrMeetingRoomSaveReqVO createReqVO);

    /**
     * 更新会议室
     *
     * @param updateReqVO 更新信息
     */
    void updateMeetingRoom(HrMeetingRoomSaveReqVO updateReqVO);

    /**
     * 删除会议室
     *
     * @param id 会议室编号
     */
    void deleteMeetingRoom(Long id);

    /**
     * 获得会议室
     *
     * @param id 会议室编号
     * @return 会议室
     */
    HrAdministrativeMeetingRoomDO getMeetingRoom(Long id);

    /**
     * 获得会议室分页
     *
     * @param pageReqVO 分页查询
     * @return 会议室分页
     */
    PageResult<HrMeetingRoomRespVO> getMeetingRoomPage(HrMeetingRoomPageReqVO pageReqVO);
}
