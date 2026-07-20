package com.kyx.service.hr.service.employee;

import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeAttendanceStatRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeAttendanceStatSaveReqVO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeAttendanceStatDO;
import com.kyx.service.hr.dal.mysql.employee.EmployeeAttendanceStatMapper;
import com.kyx.service.hr.enums.ErrorCodeConstants;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

/**
 * 员工考勤统计 Service 实现类
 *
 * @author MK
 */
@Service
@Validated
public class EmployeeAttendanceStatServiceImpl implements EmployeeAttendanceStatService {

    @Resource
    private EmployeeAttendanceStatMapper employeeAttendanceStatMapper;

    @Override
    public EmployeeAttendanceStatRespVO getEmployeeAttendanceStat(Long profileId, Integer year, Integer month) {
        EmployeeAttendanceStatDO stat = employeeAttendanceStatMapper.selectByProfileIdAndMonth(profileId, year, month);
        return stat == null ? null : BeanUtils.toBean(stat, EmployeeAttendanceStatRespVO.class);
    }

    @Override
    public Long createEmployeeAttendanceStat(EmployeeAttendanceStatSaveReqVO createReqVO) {
        EmployeeAttendanceStatDO stat = BeanUtils.toBean(createReqVO, EmployeeAttendanceStatDO.class);
        employeeAttendanceStatMapper.insert(stat);
        return stat.getId();
    }

    @Override
    public void updateEmployeeAttendanceStat(EmployeeAttendanceStatSaveReqVO updateReqVO) {
        if (updateReqVO.getId() == null || employeeAttendanceStatMapper.selectById(updateReqVO.getId()) == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_PROFILE_NOT_EXISTS);
        }
        EmployeeAttendanceStatDO stat = BeanUtils.toBean(updateReqVO, EmployeeAttendanceStatDO.class);
        employeeAttendanceStatMapper.updateById(stat);
    }

}
