package com.kyx.service.hr.service.employee;

import com.kyx.service.hr.controller.admin.employee.vo.EmployeeOperationLogRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeOperationLogSaveReqVO;

import javax.validation.Valid;
import java.util.List;

/**
 * 员工操作日志 Service 接口
 *
 * @author MK
 */
public interface EmployeeOperationLogService {

    /**
     * 创建员工操作日志
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createEmployeeOperationLog(@Valid EmployeeOperationLogSaveReqVO createReqVO);

    /**
     * 获得员工操作日志列表
     *
     * @param profileId 员工档案ID
     * @return 操作日志列表
     */
    List<EmployeeOperationLogRespVO> getEmployeeOperationLogList(Long profileId);

}
