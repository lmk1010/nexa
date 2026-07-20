package com.kyx.service.hr.service.administrative.meeting;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.administrative.meeting.vo.HrMeetingPageReqVO;
import com.kyx.service.hr.controller.admin.administrative.meeting.vo.HrMeetingSaveReqVO;
import com.kyx.service.hr.dal.dataobject.administrative.HrAdministrativeMeetingDO;

/**
 * 会议室预约 Service 接口
 *
 * @author MK
 */
public interface HrAdministrativeMeetingService {

    /**
     * 创建会议预约
     *
     * @param userId 申请人用户ID
     * @param createReqVO 创建信息
     * @return 会议预约编号
     */
    Long createMeeting(Long userId, HrMeetingSaveReqVO createReqVO);

    /**
     * 更新会议预约
     *
     * @param updateReqVO 更新信息
     */
    void updateMeeting(HrMeetingSaveReqVO updateReqVO);

    /**
     * 更新会议预约状态
     *
     * @param id 会议预约编号
     * @param status 状态
     */
    void updateMeetingStatus(Long id, Integer status);

    /**
     * 获得会议预约
     *
     * @param id 会议预约编号
     * @return 会议预约
     */
    HrAdministrativeMeetingDO getMeeting(Long id);

    /**
     * 获得会议预约分页
     *
     * @param pageReqVO 分页查询
     * @return 会议预约分页
     */
    PageResult<HrAdministrativeMeetingDO> getMeetingPage(HrMeetingPageReqVO pageReqVO);
}
