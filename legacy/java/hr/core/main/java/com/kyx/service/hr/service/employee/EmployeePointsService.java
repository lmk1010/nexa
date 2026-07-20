package com.kyx.service.hr.service.employee;

import com.kyx.service.hr.controller.admin.employee.vo.EmployeePointsAccountRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePointsAddReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePointsDeductReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePointsRespVO;

import javax.validation.Valid;
import java.util.List;

/**
 * 员工积分 Service 接口
 *
 * @author MK
 */
public interface EmployeePointsService {

    /**
     * 获得员工积分记录列表
     *
     * @param profileId 员工档案ID
     * @return 积分记录列表
     */
    List<EmployeePointsRespVO> getEmployeePointsList(Long profileId);

    /**
     * 获得员工积分账户
     *
     * @param profileId 员工档案ID
     * @return 积分账户
     */
    EmployeePointsAccountRespVO getEmployeePointsAccount(Long profileId);

    /**
     * 增加积分
     *
     * @param operatorId   操作人ID
     * @param operatorName 操作人姓名
     * @param reqVO        积分信息
     * @return 编号
     */
    Long addEmployeePoints(Long operatorId, String operatorName, @Valid EmployeePointsAddReqVO reqVO);

    /**
     * 扣减积分
     *
     * @param operatorId   操作人ID
     * @param operatorName 操作人姓名
     * @param reqVO        积分信息
     * @return 编号
     */
    Long deductEmployeePoints(Long operatorId, String operatorName, @Valid EmployeePointsDeductReqVO reqVO);

}
