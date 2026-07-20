package com.kyx.service.hr.service.employee;

import com.kyx.service.hr.controller.admin.employee.vo.EmployeeAttendanceStatRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeAttendanceStatSaveReqVO;

import javax.validation.Valid;

/**
 * 员工考勤统计 Service 接口
 *
 * @author MK
 */
public interface EmployeeAttendanceStatService {

    /**
     * 获得员工月度考勤统计
     *
     * @param profileId 员工档案ID
     * @param year      统计年份
     * @param month     统计月份
     * @return 考勤统计
     */
    EmployeeAttendanceStatRespVO getEmployeeAttendanceStat(Long profileId, Integer year, Integer month);

    /**
     * 创建员工月度考勤统计
     *
     * @param createReqVO 创建信息
     * @return ID
     */
    Long createEmployeeAttendanceStat(@Valid EmployeeAttendanceStatSaveReqVO createReqVO);

    /**
     * 更新员工月度考勤统计
     *
     * @param updateReqVO 更新信息
     */
    void updateEmployeeAttendanceStat(@Valid EmployeeAttendanceStatSaveReqVO updateReqVO);

}
