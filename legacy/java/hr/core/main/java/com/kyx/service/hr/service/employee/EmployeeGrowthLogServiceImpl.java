package com.kyx.service.hr.service.employee;

import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeGrowthLogRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeGrowthLogSaveReqVO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeGrowthLogDO;
import com.kyx.service.hr.dal.mysql.employee.EmployeeGrowthLogMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeProfileMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static com.kyx.service.hr.enums.ErrorCodeConstants.EMPLOYEE_GROWTH_LOG_NOT_EXISTS;
import static com.kyx.service.hr.enums.ErrorCodeConstants.EMPLOYEE_PROFILE_NOT_EXISTS;

/**
 * 员工成长记录 Service 实现类
 *
 * @author MK
 */
@Service
@Validated
public class EmployeeGrowthLogServiceImpl implements EmployeeGrowthLogService {

    @Resource
    private EmployeeGrowthLogMapper employeeGrowthLogMapper;

    @Resource
    private EmployeeProfileMapper employeeProfileMapper;

    @Override
    public Long createEmployeeGrowthLog(EmployeeGrowthLogSaveReqVO createReqVO) {
        validateProfileExists(createReqVO.getProfileId());
        EmployeeGrowthLogDO growthLog = BeanUtils.toBean(createReqVO, EmployeeGrowthLogDO.class);
        employeeGrowthLogMapper.insert(growthLog);
        return growthLog.getId();
    }

    @Override
    public void updateEmployeeGrowthLog(EmployeeGrowthLogSaveReqVO updateReqVO) {
        validateEmployeeGrowthLogExists(updateReqVO.getId());
        EmployeeGrowthLogDO updateObj = BeanUtils.toBean(updateReqVO, EmployeeGrowthLogDO.class);
        employeeGrowthLogMapper.updateById(updateObj);
    }

    @Override
    public void deleteEmployeeGrowthLog(Long id) {
        validateEmployeeGrowthLogExists(id);
        employeeGrowthLogMapper.deleteById(id);
    }

    @Override
    public List<EmployeeGrowthLogRespVO> getEmployeeGrowthLogList(Long profileId) {
        List<EmployeeGrowthLogDO> list = employeeGrowthLogMapper.selectListByProfileId(profileId);
        return BeanUtils.toBean(list, EmployeeGrowthLogRespVO.class);
    }

    private void validateProfileExists(Long profileId) {
        if (employeeProfileMapper.selectById(profileId) == null) {
            throw ServiceExceptionUtil.exception(EMPLOYEE_PROFILE_NOT_EXISTS);
        }
    }

    private void validateEmployeeGrowthLogExists(Long id) {
        if (employeeGrowthLogMapper.selectById(id) == null) {
            throw ServiceExceptionUtil.exception(EMPLOYEE_GROWTH_LOG_NOT_EXISTS);
        }
    }

}
