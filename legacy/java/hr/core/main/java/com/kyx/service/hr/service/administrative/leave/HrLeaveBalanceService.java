package com.kyx.service.hr.service.administrative.leave;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.administrative.leave.vo.HrLeaveBalanceAdjustReqVO;
import com.kyx.service.hr.controller.admin.administrative.leave.vo.HrLeaveBalancePageReqVO;
import com.kyx.service.hr.controller.admin.administrative.leave.vo.HrLeaveBalanceRecordPageReqVO;
import com.kyx.service.hr.controller.admin.administrative.leave.vo.HrLeaveBalanceRecordRespVO;
import com.kyx.service.hr.controller.admin.administrative.leave.vo.HrLeaveBalanceRespVO;
import com.kyx.service.hr.controller.admin.administrative.leave.vo.HrLeaveTypeRespVO;
import com.kyx.service.hr.controller.admin.administrative.leave.vo.HrLeaveTypeSaveReqVO;
import com.kyx.service.hr.dal.dataobject.administrative.HrAdministrativeLeaveDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 假期类型与余额 Service
 */
public interface HrLeaveBalanceService {

    List<HrLeaveTypeRespVO> listLeaveTypes();

    Long saveLeaveType(@Valid HrLeaveTypeSaveReqVO reqVO);

    PageResult<HrLeaveBalanceRespVO> getBalancePage(HrLeaveBalancePageReqVO pageReqVO);

    PageResult<HrLeaveBalanceRecordRespVO> getBalanceRecordPage(HrLeaveBalanceRecordPageReqVO pageReqVO);

    List<HrLeaveBalanceRespVO> getMyBalances(Integer year);

    Boolean adjustBalance(@Valid HrLeaveBalanceAdjustReqVO reqVO);

    void handleLeaveStatusChange(HrAdministrativeLeaveDO leave, Integer oldStatus, Integer newStatus);

}
