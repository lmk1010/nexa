package com.kyx.service.hr.service.employee;

import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeOperationLogRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeOperationLogSaveReqVO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeOperationLogDO;
import com.kyx.service.hr.dal.mysql.employee.EmployeeOperationLogMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeProfileMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

import static com.kyx.service.hr.enums.ErrorCodeConstants.EMPLOYEE_PROFILE_NOT_EXISTS;

/**
 * 员工操作日志 Service 实现类
 *
 * @author MK
 */
@Service
@Validated
public class EmployeeOperationLogServiceImpl implements EmployeeOperationLogService {

    @Resource
    private EmployeeOperationLogMapper employeeOperationLogMapper;

    @Resource
    private EmployeeProfileMapper employeeProfileMapper;

    @Override
    public Long createEmployeeOperationLog(EmployeeOperationLogSaveReqVO createReqVO) {
        validateProfileExists(createReqVO.getProfileId());
        EmployeeOperationLogDO operationLog = BeanUtils.toBean(createReqVO, EmployeeOperationLogDO.class);
        if (operationLog.getOperationTime() == null) {
            operationLog.setOperationTime(LocalDateTime.now());
        }
        employeeOperationLogMapper.insert(operationLog);
        return operationLog.getId();
    }

    @Override
    public List<EmployeeOperationLogRespVO> getEmployeeOperationLogList(Long profileId) {
        List<EmployeeOperationLogDO> list = employeeOperationLogMapper.selectListByProfileId(profileId);
        return BeanUtils.toBean(list, EmployeeOperationLogRespVO.class);
    }

    private void validateProfileExists(Long profileId) {
        if (employeeProfileMapper.selectById(profileId) == null) {
            throw ServiceExceptionUtil.exception(EMPLOYEE_PROFILE_NOT_EXISTS);
        }
    }

}
