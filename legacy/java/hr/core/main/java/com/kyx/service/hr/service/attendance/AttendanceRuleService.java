package com.kyx.service.hr.service.attendance;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceGroupPageReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceGroupRespVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceGroupSaveReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceShiftRuleRespVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceShiftRuleSaveReqVO;

import javax.validation.Valid;
import java.util.List;

/**
 * 考勤规则 Service
 */
public interface AttendanceRuleService {

    Long saveShift(@Valid AttendanceShiftRuleSaveReqVO reqVO);

    List<AttendanceShiftRuleRespVO> getShiftList();

    Long saveGroup(@Valid AttendanceGroupSaveReqVO reqVO);

    PageResult<AttendanceGroupRespVO> getGroupPage(AttendanceGroupPageReqVO pageReqVO);

}
