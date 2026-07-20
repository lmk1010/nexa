package com.kyx.service.hr.service.employee;

import com.kyx.service.hr.controller.admin.employee.vo.EmployeeGrowthLogRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeGrowthLogSaveReqVO;

import javax.validation.Valid;
import java.util.List;

/**
 * 员工成长记录 Service 接口
 *
 * @author MK
 */
public interface EmployeeGrowthLogService {

    /**
     * 创建员工成长记录
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createEmployeeGrowthLog(@Valid EmployeeGrowthLogSaveReqVO createReqVO);

    /**
     * 更新员工成长记录
     *
     * @param updateReqVO 更新信息
     */
    void updateEmployeeGrowthLog(@Valid EmployeeGrowthLogSaveReqVO updateReqVO);

    /**
     * 删除员工成长记录
     *
     * @param id 编号
     */
    void deleteEmployeeGrowthLog(Long id);

    /**
     * 获得员工成长记录列表
     *
     * @param profileId 员工档案ID
     * @return 成长记录列表
     */
    List<EmployeeGrowthLogRespVO> getEmployeeGrowthLogList(Long profileId);

}
