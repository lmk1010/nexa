package com.kyx.service.hr.service.employee;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePerformanceAdvanceReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePerformanceApprovalReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePerformancePageReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePerformanceSchemePageReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePerformanceSchemeRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePerformanceSchemeSaveReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePerformanceRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePerformanceSaveReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePerformanceStatsRespVO;

import javax.validation.Valid;
import java.util.List;

public interface EmployeePerformanceService {

    List<EmployeePerformanceRespVO> getPerformanceList(Long profileId);

    PageResult<EmployeePerformanceRespVO> getPerformancePage(EmployeePerformancePageReqVO pageReqVO);

    EmployeePerformanceStatsRespVO getPerformanceStats(EmployeePerformancePageReqVO pageReqVO);

    Long createPerformance(@Valid EmployeePerformanceSaveReqVO createReqVO);

    void updatePerformance(@Valid EmployeePerformanceSaveReqVO updateReqVO);

    void advancePerformance(@Valid EmployeePerformanceAdvanceReqVO advanceReqVO);

    void submitPerformance(@Valid EmployeePerformanceApprovalReqVO approvalReqVO);

    void approvePerformance(@Valid EmployeePerformanceApprovalReqVO approvalReqVO);

    void rejectPerformance(@Valid EmployeePerformanceApprovalReqVO approvalReqVO);

    void updateApprovalStatusByBpmEvent(Long id, String processInstanceId, Integer bpmStatus, Long operatorUserId);

    void deletePerformance(Long id);

    PageResult<EmployeePerformanceSchemeRespVO> getSchemePage(EmployeePerformanceSchemePageReqVO pageReqVO);

    List<EmployeePerformanceSchemeRespVO> getActiveSchemeList();

    Long saveScheme(@Valid EmployeePerformanceSchemeSaveReqVO reqVO);

    Boolean enableScheme(Long id);

    void deleteScheme(Long id);
}
