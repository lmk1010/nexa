package com.kyx.service.hr.service.administrative.meeting;

import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.bpm.api.task.BpmProcessInstanceApi;
import com.kyx.service.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.kyx.service.hr.controller.admin.administrative.meeting.vo.HrMeetingPageReqVO;
import com.kyx.service.hr.controller.admin.administrative.meeting.vo.HrMeetingSaveReqVO;
import com.kyx.service.hr.dal.dataobject.administrative.HrAdministrativeMeetingDO;
import com.kyx.service.hr.dal.mysql.administrative.HrAdministrativeMeetingMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static com.kyx.service.hr.enums.ErrorCodeConstants.HR_MEETING_NOT_EXISTS;

/**
 * 会议室预约 Service 实现
 *
 * @author MK
 */
@Service
@Validated
@Slf4j
public class HrAdministrativeMeetingServiceImpl implements HrAdministrativeMeetingService {

    /**
     * HR 会议室预约流程定义 KEY
     */
    public static final String PROCESS_KEY = "hr_administrative_meeting";

    private static final Integer STATUS_RUNNING = 1;
    private static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);

    @Resource
    private HrAdministrativeMeetingMapper meetingMapper;
    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createMeeting(Long userId, HrMeetingSaveReqVO createReqVO) {
        HrAdministrativeMeetingDO meeting = BeanUtils.toBean(createReqVO, HrAdministrativeMeetingDO.class)
                .setUserId(userId)
                .setStatus(STATUS_RUNNING);
        fillDefaultDuration(meeting);
        meetingMapper.insert(meeting);

        Map<String, Object> variables = new HashMap<>();
        variables.put("meetingTitle", meeting.getMeetingTitle());
        variables.put("meetingType", meeting.getMeetingType());
        variables.put("roomId", meeting.getRoomId());
        variables.put("roomName", meeting.getRoomName());
        variables.put("attendees", meeting.getAttendees());
        variables.put("startTime", meeting.getStartTime());
        variables.put("endTime", meeting.getEndTime());
        variables.put("duration", meeting.getDuration());

        String processInstanceId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO()
                        .setProcessDefinitionKey(PROCESS_KEY)
                        .setBusinessKey(String.valueOf(meeting.getId()))
                        .setVariables(variables)
                        .setStartUserSelectAssignees(createReqVO.getStartUserSelectAssignees()))
                .getCheckedData();

        meetingMapper.updateById(new HrAdministrativeMeetingDO()
                .setId(meeting.getId())
                .setProcessInstanceId(processInstanceId));
        return meeting.getId();
    }

    @Override
    public void updateMeeting(HrMeetingSaveReqVO updateReqVO) {
        if (updateReqVO.getId() == null) {
            throw ServiceExceptionUtil.exception(HR_MEETING_NOT_EXISTS);
        }
        validateMeetingExists(updateReqVO.getId());
        HrAdministrativeMeetingDO updateObj = BeanUtils.toBean(updateReqVO, HrAdministrativeMeetingDO.class);
        fillDefaultDuration(updateObj);
        meetingMapper.updateById(updateObj);
    }

    @Override
    public void updateMeetingStatus(Long id, Integer status) {
        validateMeetingExists(id);
        meetingMapper.updateById(new HrAdministrativeMeetingDO().setId(id).setStatus(status));
    }

    @Override
    public HrAdministrativeMeetingDO getMeeting(Long id) {
        return meetingMapper.selectById(id);
    }

    @Override
    public PageResult<HrAdministrativeMeetingDO> getMeetingPage(HrMeetingPageReqVO pageReqVO) {
        return meetingMapper.selectPage(pageReqVO);
    }

    private void validateMeetingExists(Long id) {
        if (meetingMapper.selectById(id) == null) {
            throw ServiceExceptionUtil.exception(HR_MEETING_NOT_EXISTS);
        }
    }

    private void fillDefaultDuration(HrAdministrativeMeetingDO meeting) {
        if (meeting.getDuration() != null || meeting.getStartTime() == null || meeting.getEndTime() == null) {
            return;
        }
        long minutes = Duration.between(meeting.getStartTime(), meeting.getEndTime()).toMinutes();
        if (minutes <= 0) {
            return;
        }
        BigDecimal hours = BigDecimal.valueOf(minutes)
                .divide(MINUTES_PER_HOUR, 2, RoundingMode.HALF_UP);
        meeting.setDuration(hours);
    }
}
