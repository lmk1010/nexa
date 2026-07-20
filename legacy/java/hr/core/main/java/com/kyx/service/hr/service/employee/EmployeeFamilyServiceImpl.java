package com.kyx.service.hr.service.employee;

import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeFamilyRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeFamilySaveReqVO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeFamilyDO;
import com.kyx.service.hr.dal.mysql.employee.EmployeeFamilyMapper;
import com.kyx.service.hr.enums.ErrorCodeConstants;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

@Service
@Validated
public class EmployeeFamilyServiceImpl implements EmployeeFamilyService {

    @Resource
    private EmployeeFamilyMapper employeeFamilyMapper;

    @Override
    public List<EmployeeFamilyRespVO> getFamilyList(Long profileId) {
        List<EmployeeFamilyDO> list = employeeFamilyMapper.selectListByProfileId(profileId);
        return BeanUtils.toBean(list, EmployeeFamilyRespVO.class);
    }

    @Override
    public Long createFamily(EmployeeFamilySaveReqVO createReqVO) {
        EmployeeFamilyDO family = BeanUtils.toBean(createReqVO, EmployeeFamilyDO.class);
        employeeFamilyMapper.insert(family);
        return family.getId();
    }

    @Override
    public void updateFamily(EmployeeFamilySaveReqVO updateReqVO) {
        if (updateReqVO.getId() == null || employeeFamilyMapper.selectById(updateReqVO.getId()) == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_PROFILE_NOT_EXISTS);
        }
        EmployeeFamilyDO family = BeanUtils.toBean(updateReqVO, EmployeeFamilyDO.class);
        employeeFamilyMapper.updateById(family);
    }

    @Override
    public void deleteFamily(Long id) {
        if (employeeFamilyMapper.selectById(id) == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_PROFILE_NOT_EXISTS);
        }
        employeeFamilyMapper.deleteById(id);
    }
}
