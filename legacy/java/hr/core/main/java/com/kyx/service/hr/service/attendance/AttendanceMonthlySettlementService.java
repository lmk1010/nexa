package com.kyx.service.hr.service.attendance;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceMonthlyConfirmActionReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceMonthlyConfirmDetailRespVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceMonthlyConfirmIssueReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceMonthlyConfirmPageReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceMonthlyConfirmResolveReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceMonthlyConfirmRespVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceMonthlySettlementGenerateReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceMonthlySettlementPageReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceMonthlySettlementRespVO;

import javax.validation.Valid;

/**
 * Monthly attendance settlement service.
 */
public interface AttendanceMonthlySettlementService {

    AttendanceMonthlySettlementRespVO generate(@Valid AttendanceMonthlySettlementGenerateReqVO reqVO);

    PageResult<AttendanceMonthlySettlementRespVO> getPage(AttendanceMonthlySettlementPageReqVO pageReqVO);

    Boolean lock(Long id);

    Boolean unlock(Long id);

    PageResult<AttendanceMonthlyConfirmRespVO> getConfirmPage(AttendanceMonthlyConfirmPageReqVO pageReqVO);

    PageResult<AttendanceMonthlyConfirmRespVO> getMyConfirmPage(AttendanceMonthlyConfirmPageReqVO pageReqVO);

    AttendanceMonthlyConfirmDetailRespVO getConfirmDetail(Long id);

    AttendanceMonthlyConfirmDetailRespVO getMyConfirmDetail(Long id);

    Boolean confirmMy(@Valid AttendanceMonthlyConfirmActionReqVO reqVO);

    Boolean issueMy(@Valid AttendanceMonthlyConfirmIssueReqVO reqVO);

    Boolean resolveConfirm(@Valid AttendanceMonthlyConfirmResolveReqVO reqVO);

}
