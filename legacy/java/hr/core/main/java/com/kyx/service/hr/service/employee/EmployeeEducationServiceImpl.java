package com.kyx.service.hr.service.employee;

import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeEducationRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeEducationSaveReqVO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeEducationDO;
import com.kyx.service.hr.dal.mysql.employee.EmployeeEducationMapper;
import com.kyx.service.hr.enums.ErrorCodeConstants;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

@Service
@Validated
public class EmployeeEducationServiceImpl implements EmployeeEducationService {

    @Resource
    private EmployeeEducationMapper employeeEducationMapper;

    @Override
    public List<EmployeeEducationRespVO> getEducationList(Long profileId) {
        List<EmployeeEducationDO> list = employeeEducationMapper.selectListByProfileId(profileId);
        return BeanUtils.toBean(list, EmployeeEducationRespVO.class);
    }

    @Override
    public Long createEducation(EmployeeEducationSaveReqVO createReqVO) {
        EmployeeEducationDO education = BeanUtils.toBean(createReqVO, EmployeeEducationDO.class);
        employeeEducationMapper.insert(education);
        return education.getId();
    }

    @Override
    public void updateEducation(EmployeeEducationSaveReqVO updateReqVO) {
        if (updateReqVO.getId() == null || employeeEducationMapper.selectById(updateReqVO.getId()) == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_PROFILE_NOT_EXISTS);
        }
        EmployeeEducationDO education = BeanUtils.toBean(updateReqVO, EmployeeEducationDO.class);
        employeeEducationMapper.updateById(education);
    }

    @Override
    public void deleteEducation(Long id) {
        if (employeeEducationMapper.selectById(id) == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_PROFILE_NOT_EXISTS);
        }
        employeeEducationMapper.deleteById(id);
    }
}
