package com.kyx.service.biz.service.work;

import cn.hutool.core.util.StrUtil;
import com.kyx.foundation.common.util.date.DateUtils;
import com.kyx.service.biz.controller.admin.work.vo.WorkCalendarEventReqVO;
import com.kyx.service.biz.controller.admin.work.vo.WorkCalendarEventRespVO;
import com.kyx.service.biz.controller.admin.work.vo.WorkCalendarEventSaveReqVO;
import com.kyx.service.biz.dal.dataobject.todo.TodoDO;
import com.kyx.service.biz.dal.dataobject.work.WorkCalendarEventDO;
import com.kyx.service.biz.dal.dataobject.work.WorkCalendarLeaveDO;
import com.kyx.service.biz.dal.dataobject.work.WorkCalendarMeetingDO;
import com.kyx.service.biz.dal.dataobject.work.WorkCalendarTripDO;
import com.kyx.service.biz.dal.mysql.todo.TodoMapper;
import com.kyx.service.biz.dal.mysql.work.WorkCalendarEventMapper;
import com.kyx.service.biz.dal.mysql.work.WorkCalendarLeaveMapper;
import com.kyx.service.biz.dal.mysql.work.WorkCalendarMeetingMapper;
import com.kyx.service.biz.dal.mysql.work.WorkCalendarTripMapper;
import com.kyx.service.biz.enums.TodoStatusEnum;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.service.biz.enums.ErrorCodeConstants.WORK_CALENDAR_EVENT_FORBIDDEN;
import static com.kyx.service.biz.enums.ErrorCodeConstants.WORK_CALENDAR_EVENT_NOT_EXISTS;
import static com.kyx.service.biz.enums.ErrorCodeConstants.WORK_CALENDAR_EVENT_TIME_INVALID;

@Service
@Validated
public class WorkCalendarServiceImpl implements WorkCalendarService {

    private static final String TYPE_SCHEDULE = "schedule";
    private static final String TYPE_MEETING = "meeting";
    private static final String TYPE_TASK = "task";
    private static final String TYPE_TRIP = "trip";
    private static final String TYPE_LEAVE = "leave";

    @Resource
    private WorkCalendarEventMapper eventMapper;
    @Resource
    private WorkCalendarMeetingMapper meetingMapper;
    @Resource
    private WorkCalendarLeaveMapper leaveMapper;
    @Resource
    private WorkCalendarTripMapper tripMapper;
    @Resource
    private TodoMapper todoMapper;

    @Override
    public List<WorkCalendarEventRespVO> getCalendarEvents(WorkCalendarEventReqVO reqVO, Long userId) {
        if (reqVO.getStartDate().isAfter(reqVO.getEndDate())) {
            return Collections.emptyList();
        }
        LocalDateTime rangeStart = reqVO.getStartDate().atStartOfDay();
        LocalDateTime rangeEnd = reqVO.getEndDate().atTime(LocalTime.of(23, 59, 59));
        Set<String> typeFilter = normalizeTypes(reqVO.getTypes());

        List<WorkCalendarEventRespVO> events = new ArrayList<>();
        if (isTypeEnabled(typeFilter, TYPE_SCHEDULE)) {
            List<WorkCalendarEventDO> scheduleEvents = eventMapper.selectCalendarList(userId, rangeStart, rangeEnd);
            scheduleEvents.forEach(event -> events.add(buildScheduleEvent(event)));
        }
        if (isTypeEnabled(typeFilter, TYPE_MEETING)) {
            List<WorkCalendarMeetingDO> meetings = meetingMapper.selectCalendarList(userId, rangeStart, rangeEnd);
            meetings.forEach(meeting -> events.add(buildMeetingEvent(meeting)));
        }
        if (isTypeEnabled(typeFilter, TYPE_LEAVE)) {
            List<WorkCalendarLeaveDO> leaves = leaveMapper.selectCalendarList(userId, rangeStart, rangeEnd);
            leaves.forEach(leave -> events.add(buildLeaveEvent(leave)));
        }
        if (isTypeEnabled(typeFilter, TYPE_TRIP)) {
            List<WorkCalendarTripDO> trips = tripMapper.selectCalendarList(userId, rangeStart, rangeEnd);
            trips.forEach(trip -> events.add(buildTripEvent(trip)));
        }
        if (isTypeEnabled(typeFilter, TYPE_TASK)) {
            List<TodoDO> todos = todoMapper.selectCalendarList(userId,
                    DateUtils.of(rangeStart), DateUtils.of(rangeEnd), TodoStatusEnum.PROCESS.getStatus());
            todos.stream()
                    .filter(todo -> todo.getDueDate() != null)
                    .forEach(todo -> events.add(buildTodoEvent(todo)));
        }

        events.sort(Comparator.comparing(WorkCalendarEventRespVO::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())));
        return events;
    }

    @Override
    public Long createCalendarEvent(WorkCalendarEventSaveReqVO createReqVO, Long userId) {
        validateTimeRange(createReqVO.getStartTime(), createReqVO.getEndTime());
        WorkCalendarEventDO event = new WorkCalendarEventDO();
        fillCalendarEvent(event, createReqVO);
        event.setUserId(userId);
        event.setType(TYPE_SCHEDULE);
        eventMapper.insert(event);
        return event.getId();
    }

    @Override
    public void updateCalendarEvent(WorkCalendarEventSaveReqVO updateReqVO, Long userId) {
        WorkCalendarEventDO existing = validateCalendarEvent(updateReqVO.getId(), userId);
        validateTimeRange(updateReqVO.getStartTime(), updateReqVO.getEndTime());
        WorkCalendarEventDO updateObj = new WorkCalendarEventDO();
        updateObj.setId(existing.getId());
        fillCalendarEvent(updateObj, updateReqVO);
        updateObj.setType(TYPE_SCHEDULE);
        eventMapper.updateById(updateObj);
    }

    @Override
    public void deleteCalendarEvent(Long id, Long userId) {
        validateCalendarEvent(id, userId);
        eventMapper.deleteById(id);
    }

    private static boolean isTypeEnabled(Set<String> typeFilter, String type) {
        return typeFilter.isEmpty() || typeFilter.contains(type);
    }

    private static Set<String> normalizeTypes(List<String> types) {
        if (types == null || types.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> normalized = new HashSet<>();
        for (String type : types) {
            if (StrUtil.isBlank(type)) {
                continue;
            }
            normalized.add(type.trim().toLowerCase(Locale.ROOT));
        }
        return normalized;
    }

    private static void fillCalendarEvent(WorkCalendarEventDO event, WorkCalendarEventSaveReqVO reqVO) {
        event.setTitle(StrUtil.trim(reqVO.getTitle()));
        event.setStartTime(reqVO.getStartTime());
        event.setEndTime(reqVO.getEndTime());
        event.setLocation(blankToNull(reqVO.getLocation()));
        event.setRemark(blankToNull(reqVO.getRemark()));
    }

    private static String blankToNull(String value) {
        String trimmed = StrUtil.trim(value);
        return StrUtil.isBlank(trimmed) ? null : trimmed;
    }

    private WorkCalendarEventDO validateCalendarEvent(Long id, Long userId) {
        if (id == null) {
            throw exception(WORK_CALENDAR_EVENT_NOT_EXISTS);
        }
        WorkCalendarEventDO event = eventMapper.selectById(id);
        if (event == null) {
            throw exception(WORK_CALENDAR_EVENT_NOT_EXISTS);
        }
        if (!Objects.equals(event.getUserId(), userId)) {
            throw exception(WORK_CALENDAR_EVENT_FORBIDDEN);
        }
        return event;
    }

    private static void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw exception(WORK_CALENDAR_EVENT_TIME_INVALID);
        }
    }

    private static WorkCalendarEventRespVO buildScheduleEvent(WorkCalendarEventDO source) {
        WorkCalendarEventRespVO event = new WorkCalendarEventRespVO();
        event.setId(TYPE_SCHEDULE + "-" + source.getId());
        event.setType(TYPE_SCHEDULE);
        event.setTitle(source.getTitle());
        event.setStartTime(source.getStartTime());
        event.setEndTime(source.getEndTime());
        event.setLocation(source.getLocation());
        event.setRemark(source.getRemark());
        event.setEditable(true);
        return event;
    }

    private static WorkCalendarEventRespVO buildMeetingEvent(WorkCalendarMeetingDO meeting) {
        WorkCalendarEventRespVO event = new WorkCalendarEventRespVO();
        event.setId(TYPE_MEETING + "-" + meeting.getId());
        event.setType(TYPE_MEETING);
        event.setTitle(meeting.getMeetingTitle());
        event.setStartTime(meeting.getStartTime());
        event.setEndTime(meeting.getEndTime());
        if (StrUtil.isNotBlank(meeting.getRoomName())) {
            event.setLocation(meeting.getRoomName());
        } else if (StrUtil.isNotBlank(meeting.getRoomId())) {
            event.setLocation(meeting.getRoomId());
        }
        event.setOwner(meeting.getOrganizer());
        return event;
    }

    private static WorkCalendarEventRespVO buildLeaveEvent(WorkCalendarLeaveDO leave) {
        WorkCalendarEventRespVO event = new WorkCalendarEventRespVO();
        event.setId(TYPE_LEAVE + "-" + leave.getId());
        event.setType(TYPE_LEAVE);
        event.setTitle(buildLeaveTitle(leave));
        event.setStartTime(leave.getStartTime());
        event.setEndTime(leave.getEndTime());
        return event;
    }

    private static String buildLeaveTitle(WorkCalendarLeaveDO leave) {
        boolean hasCategory = StrUtil.isNotBlank(leave.getLeaveCategory());
        boolean hasType = StrUtil.isNotBlank(leave.getLeaveType());
        if (hasCategory && hasType) {
            return leave.getLeaveCategory() + " / " + leave.getLeaveType();
        }
        if (hasCategory) {
            return leave.getLeaveCategory();
        }
        if (hasType) {
            return leave.getLeaveType();
        }
        return "Leave";
    }

    private static WorkCalendarEventRespVO buildTripEvent(WorkCalendarTripDO trip) {
        WorkCalendarEventRespVO event = new WorkCalendarEventRespVO();
        event.setId(TYPE_TRIP + "-" + trip.getId());
        event.setType(TYPE_TRIP);
        event.setTitle(buildTripTitle(trip));
        event.setStartTime(trip.getStartTime());
        event.setEndTime(trip.getEndTime());
        event.setLocation(buildTripLocation(trip));
        return event;
    }

    private static String buildTripTitle(WorkCalendarTripDO trip) {
        if (StrUtil.isNotBlank(trip.getDestinationCity())) {
            return "Trip - " + trip.getDestinationCity();
        }
        if (StrUtil.isNotBlank(trip.getPurpose())) {
            return trip.getPurpose();
        }
        return "Trip";
    }

    private static String buildTripLocation(WorkCalendarTripDO trip) {
        if (StrUtil.isNotBlank(trip.getDestinationAddress())) {
            return trip.getDestinationAddress();
        }
        if (StrUtil.isNotBlank(trip.getDestinationCity())) {
            return trip.getDestinationCity();
        }
        return null;
    }

    private static WorkCalendarEventRespVO buildTodoEvent(TodoDO todo) {
        WorkCalendarEventRespVO event = new WorkCalendarEventRespVO();
        event.setId(TYPE_TASK + "-" + todo.getId());
        event.setType(TYPE_TASK);
        event.setTitle(todo.getTitle());
        event.setStartTime(DateUtils.of(todo.getDueDate()));
        event.setEndTime(DateUtils.of(todo.getDueDate()));
        event.setRoutePath(todo.getRoutePath());
        return event;
    }
}
