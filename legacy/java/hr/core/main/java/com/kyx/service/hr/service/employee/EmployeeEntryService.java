package com.kyx.service.hr.service.employee;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeEntryPageReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeEntryRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeEntrySaveReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeEntryUpdateReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeResignationReqVO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeEntryDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 员工入职记录 Service 接口
 *
 * @author MK
 */
public interface EmployeeEntryService {

    /**
     * 创建员工入职记录
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createEmployeeEntry(@Valid EmployeeEntrySaveReqVO createReqVO);

    /**
     * 更新员工入职记录
     *
     * @param updateReqVO 更新信息
     */
    void updateEmployeeEntry(@Valid EmployeeEntrySaveReqVO updateReqVO);

    /**
     * 局部更新员工入职记录
     *
     * @param updateReqVO 更新信息
     */
    void updateEmployeeEntryPartial(@Valid EmployeeEntryUpdateReqVO updateReqVO);

    /**
     * 删除员工入职记录
     *
     * @param id 编号
     */
    void deleteEmployeeEntry(Long id);

    /**
     * 获得员工入职记录
     *
     * @param id 编号
     * @return 员工入职记录
     */
    EmployeeEntryDO getEmployeeEntry(Long id);

    /**
     * 获得员工入职记录详情（包含教育信息、家庭信息等）
     *
     * @param id 编号
     * @return 员工入职记录详情
     */
    EmployeeEntryRespVO getEmployeeEntryDetail(Long id);

    /**
     * 获得员工入职记录分页
     *
     * @param pageReqVO 分页查询
     * @return 员工入职记录分页
     */
    PageResult<EmployeeEntryRespVO> getEmployeeEntryPage(EmployeeEntryPageReqVO pageReqVO);

    /**
     * 获得员工入职记录列表
     *
     * @param ids 编号数组
     * @return 员工入职记录列表
     */
    List<EmployeeEntryDO> getEmployeeEntryList(List<Long> ids);

    /**
     * 通过档案ID查询入职记录列表
     *
     * @param profileId 档案ID
     * @return 入职记录列表
     */
    List<EmployeeEntryDO> getEmployeeEntryListByProfileId(Long profileId);

    /**
     * 通过员工编号查询入职记录
     *
     * @param employeeNo 员工编号
     * @return 入职记录
     */
    EmployeeEntryDO getEmployeeEntryByEmployeeNo(String employeeNo);

    /**
     * 通过入职编号查询入职记录
     *
     * @param entryNo 入职编号
     * @return 入职记录
     */
    EmployeeEntryDO getEmployeeEntryByEntryNo(String entryNo);

    /**
     * 生成入职编号
     *
     * @return 入职编号
     */
    String generateEntryNo();

    /**
     * 生成员工编号
     *
     * @return 员工编号
     */
    String generateEmployeeNo();

    /**
     * 调整员工入职日期
     *
     * @param id 入职记录ID
     * @param newEntryDate 新的入职日期
     */
    void adjustEntryDate(Long id, String newEntryDate);

    /**
     * 办理员工离职
     *
     * @param reqVO 离职信息
     */
    void processResignation(@Valid EmployeeResignationReqVO reqVO);

} 
