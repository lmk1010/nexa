package com.kyx.service.hr.service.lifecycle;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.lifecycle.vo.HrLifecycleEventPageReqVO;
import com.kyx.service.hr.controller.admin.lifecycle.vo.HrLifecycleEventRespVO;
import com.kyx.service.hr.controller.admin.lifecycle.vo.HrLifecycleCalendarEventRespVO;
import com.kyx.service.hr.controller.admin.lifecycle.vo.HrLifecycleRegularizationCreateReqVO;
import com.kyx.service.hr.controller.admin.lifecycle.vo.HrLifecycleReminderRespVO;
import com.kyx.service.hr.controller.admin.lifecycle.vo.HrLifecycleResignationCreateReqVO;
import com.kyx.service.hr.controller.admin.lifecycle.vo.HrLifecycleSalaryAdjustCreateReqVO;
import com.kyx.service.hr.controller.admin.lifecycle.vo.HrLifecycleTaskCompleteReqVO;
import com.kyx.service.hr.controller.admin.lifecycle.vo.HrLifecycleTaskRespVO;
import com.kyx.service.hr.controller.admin.lifecycle.vo.HrLifecycleTransferCreateReqVO;
import com.kyx.service.hr.controller.admin.lifecycle.vo.HrLifecycleWorkbenchRespVO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeEntryDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeProfileDO;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

public interface HrLifecycleService {

    HrLifecycleWorkbenchRespVO getWorkbench();

    PageResult<HrLifecycleEventRespVO> getEventPage(HrLifecycleEventPageReqVO pageReqVO);

    HrLifecycleEventRespVO getEvent(Long id);

    List<HrLifecycleEventRespVO> getTimeline(Long profileId);

    List<HrLifecycleTaskRespVO> getTaskList(Long eventId);

    List<HrLifecycleReminderRespVO> getReminderList();

    List<HrLifecycleCalendarEventRespVO> getCalendarEvents(LocalDate startDate, LocalDate endDate);

    Long createResignation(@Valid HrLifecycleResignationCreateReqVO reqVO);

    Long createRegularization(@Valid HrLifecycleRegularizationCreateReqVO reqVO);

    Long createTransfer(@Valid HrLifecycleTransferCreateReqVO reqVO);

    Long createSalaryAdjust(@Valid HrLifecycleSalaryAdjustCreateReqVO reqVO);

    void completeTask(@Valid HrLifecycleTaskCompleteReqVO reqVO);

    void effectiveEvent(Long id);

    Integer effectiveDueEvents();

    void cancelEvent(Long id, String reason);

    void updateApprovalStatusByBpmEvent(Long eventId, String processInstanceId, Integer bpmStatus, Long operatorUserId);

    Integer backfillBaselineEvents();

    void recordOnboardingConfirmed(EmployeeEntryDO employeeEntry, EmployeeProfileDO profile);

    void recordResignationEffective(EmployeeEntryDO beforeEntry, EmployeeEntryDO afterEntry,
                                    EmployeeProfileDO profile, String leaveReason);

    void recordTransferEffective(EmployeeEntryDO beforeEntry, EmployeeEntryDO afterEntry,
                                 EmployeeProfileDO profile, String reason);

}
