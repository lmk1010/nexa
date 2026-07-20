package com.kyx.service.biz.service.work;

import com.kyx.service.biz.controller.admin.work.vo.WorkCalendarEventReqVO;
import com.kyx.service.biz.controller.admin.work.vo.WorkCalendarEventRespVO;
import com.kyx.service.biz.controller.admin.work.vo.WorkCalendarEventSaveReqVO;

import java.util.List;

public interface WorkCalendarService {

    List<WorkCalendarEventRespVO> getCalendarEvents(WorkCalendarEventReqVO reqVO, Long userId);

    Long createCalendarEvent(WorkCalendarEventSaveReqVO createReqVO, Long userId);

    void updateCalendarEvent(WorkCalendarEventSaveReqVO updateReqVO, Long userId);

    void deleteCalendarEvent(Long id, Long userId);
}
