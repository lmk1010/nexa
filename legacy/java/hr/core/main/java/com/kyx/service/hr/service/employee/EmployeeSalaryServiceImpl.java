package com.kyx.service.hr.service.employee;

import com.kyx.foundation.common.exception.enums.GlobalErrorCodeConstants;
import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeSalaryRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeSalarySaveReqVO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeEntryDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeSalaryDO;
import com.kyx.service.hr.dal.mysql.employee.EmployeeEntryMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeProfileMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeSalaryMapper;
import com.kyx.service.hr.enums.ErrorCodeConstants;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@Validated
public class EmployeeSalaryServiceImpl implements EmployeeSalaryService {

    @Resource
    private EmployeeSalaryMapper employeeSalaryMapper;

    @Resource
    private EmployeeProfileMapper employeeProfileMapper;

    @Resource
    private EmployeeEntryMapper employeeEntryMapper;

    @Override
    public List<EmployeeSalaryRespVO> getSalaryList(Long profileId) {
        List<EmployeeSalaryDO> list = employeeSalaryMapper.selectListByProfileId(profileId);
        return BeanUtils.toBean(list, EmployeeSalaryRespVO.class);
    }

    @Override
    public Long createSalary(EmployeeSalarySaveReqVO createReqVO) {
        validateProfileExists(createReqVO.getProfileId());
        validateDirectSalaryMutationAllowed(createReqVO.getProfileId());
        validateSalaryPayload(createReqVO);
        EmployeeSalaryDO salary = toSalaryDO(createReqVO);
        employeeSalaryMapper.insert(salary);
        return salary.getId();
    }

    @Override
    public void updateSalary(EmployeeSalarySaveReqVO updateReqVO) {
        EmployeeSalaryDO existing = validateSalaryExists(updateReqVO.getId());
        if (!Objects.equals(existing.getProfileId(), updateReqVO.getProfileId())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "薪酬记录不能变更所属员工");
        }
        validateDirectSalaryMutationAllowed(existing.getProfileId());
        validateSalaryPayload(updateReqVO);
        EmployeeSalaryDO salary = toSalaryDO(updateReqVO);
        employeeSalaryMapper.updateById(salary);
    }

    @Override
    public void deleteSalary(Long id) {
        EmployeeSalaryDO existing = validateSalaryExists(id);
        validateDirectSalaryMutationAllowed(existing.getProfileId());
        employeeSalaryMapper.deleteById(id);
    }

    private EmployeeSalaryDO validateSalaryExists(Long id) {
        if (id == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_PROFILE_NOT_EXISTS);
        }
        EmployeeSalaryDO salary = employeeSalaryMapper.selectById(id);
        if (salary == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_PROFILE_NOT_EXISTS);
        }
        return salary;
    }

    private void validateProfileExists(Long profileId) {
        if (profileId == null || employeeProfileMapper.selectById(profileId) == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_PROFILE_NOT_EXISTS);
        }
    }

    private void validateDirectSalaryMutationAllowed(Long profileId) {
        boolean hasActiveEmployment = employeeEntryMapper.selectListByProfileId(profileId).stream()
                .map(EmployeeEntryDO::getWorkStatus)
                .anyMatch(status -> Objects.equals(status, 2) || Objects.equals(status, 3));
        if (hasActiveEmployment) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST,
                    "在职员工薪酬变更请通过生命周期调薪审批");
        }
    }

    private void validateSalaryPayload(EmployeeSalarySaveReqVO reqVO) {
        if (reqVO.getAmount() == null || reqVO.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "薪酬金额必须大于 0");
        }
        if (reqVO.getEffectiveDate() == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "薪酬生效日期不能为空");
        }
    }

    private EmployeeSalaryDO toSalaryDO(EmployeeSalarySaveReqVO reqVO) {
        EmployeeSalaryDO salary = BeanUtils.toBean(reqVO, EmployeeSalaryDO.class);
        if (!StringUtils.hasText(salary.getSalaryType())) {
            salary.setSalaryType("月薪");
        }
        if (!StringUtils.hasText(salary.getCurrency())) {
            salary.setCurrency("CNY");
        }
        return salary;
    }
}
