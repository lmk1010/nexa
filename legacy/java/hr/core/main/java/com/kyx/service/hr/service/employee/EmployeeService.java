package com.kyx.service.hr.service.employee;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeCurrentRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeOverviewRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePageReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeCreateReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeStatisticsRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeStatisticsTrendRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeUpdateReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeWorkspaceOverviewRespVO;

import javax.validation.Valid;

/**
 * 员工花名册 Service 接口
 *
 * @author MK
 */
public interface EmployeeService {

    /**
     * 获得员工花名册分页
     *
     * @param pageReqVO 分页查询
     * @return 员工花名册分页
     */
    PageResult<EmployeeRespVO> getEmployeePage(@Valid EmployeePageReqVO pageReqVO);

    /**
     * 获得员工详情
     *
     * @param id 员工ID
     * @return 员工详情
     */
    EmployeeRespVO getEmployee(Long id);

    /**
     * 获得当前登录用户绑定的员工档案
     *
     * @return 当前员工档案
     */
    EmployeeCurrentRespVO getCurrentEmployee();

    /**
     * 获得员工统计数据
     *
     * @return 员工统计数据
     */
    EmployeeStatisticsRespVO getEmployeeStatistics();

    /**
     * 获得员工统计趋势数据
     *
     * @param months 统计月份数
     * @return 趋势数据
     */
    EmployeeStatisticsTrendRespVO getEmployeeStatisticsTrend(Integer months);

    /**
     * 获得员工总览数据
     *
     * @return 员工总览
     */
    EmployeeOverviewRespVO getEmployeeOverview();

    /**
     * 获得工作台员工聚合概览
     *
     * @param months 趋势月份数
     * @return 工作台员工聚合概览
     */
    EmployeeWorkspaceOverviewRespVO getEmployeeWorkspaceOverview(Integer months);

    /**
     * 更新员工信息
     *
     * @param updateReqVO 更新信息
     */
    void updateEmployee(@Valid EmployeeUpdateReqVO updateReqVO);

    /**
     * 创建员工信息
     *
     * @param createReqVO 创建信息
     * @return 员工ID
     */
    Long createEmployee(@Valid EmployeeCreateReqVO createReqVO);

    /**
     * 删除员工信息
     *
     * @param id 员工ID
     */
    void deleteEmployee(Long id);
}
