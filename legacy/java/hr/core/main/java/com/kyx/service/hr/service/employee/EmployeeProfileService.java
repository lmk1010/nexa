package com.kyx.service.hr.service.employee;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeProfileDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 员工档案 Service 接口
 *
 * @author MK
 */
public interface EmployeeProfileService {

    /**
     * 创建员工档案
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createEmployeeProfile(@Valid Object createReqVO);

    /**
     * 更新员工档案
     *
     * @param updateReqVO 更新信息
     */
    void updateEmployeeProfile(@Valid Object updateReqVO);

    /**
     * 删除员工档案
     *
     * @param id 编号
     */
    void deleteEmployeeProfile(Long id);

    /**
     * 获得员工档案
     *
     * @param id 编号
     * @return 员工档案
     */
    EmployeeProfileDO getEmployeeProfile(Long id);

    /**
     * 获得员工档案分页
     *
     * @param pageReqVO 分页查询
     * @return 员工档案分页
     */
    PageResult<EmployeeProfileDO> getEmployeeProfilePage(Object pageReqVO);

    /**
     * 获得员工档案列表
     *
     * @param ids 编号数组
     * @return 员工档案列表
     */
    List<EmployeeProfileDO> getEmployeeProfileList(List<Long> ids);

    /**
     * 通过身份证号查询员工档案
     *
     * @param idNumber 身份证号
     * @return 员工档案
     */
    EmployeeProfileDO getEmployeeProfileByIdNumber(String idNumber);

    /**
     * 通过档案编号查询员工档案
     *
     * @param profileNo 档案编号
     * @return 员工档案
     */
    EmployeeProfileDO getEmployeeProfileByProfileNo(String profileNo);

    /**
     * 生成档案编号
     *
     * @return 档案编号
     */
    String generateProfileNo();

} 