package com.kyx.service.hr.service.employee;

import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeInventoryRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeInventorySaveReqVO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeInventoryDO;
import com.kyx.service.hr.dal.mysql.employee.EmployeeInventoryMapper;
import com.kyx.service.hr.enums.ErrorCodeConstants;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

@Service
@Validated
public class EmployeeInventoryServiceImpl implements EmployeeInventoryService {

    @Resource
    private EmployeeInventoryMapper employeeInventoryMapper;

    @Override
    public List<EmployeeInventoryRespVO> getInventoryList(Long profileId) {
        List<EmployeeInventoryDO> list = employeeInventoryMapper.selectListByProfileId(profileId);
        return BeanUtils.toBean(list, EmployeeInventoryRespVO.class);
    }

    @Override
    public Long createInventory(EmployeeInventorySaveReqVO createReqVO) {
        EmployeeInventoryDO inventory = BeanUtils.toBean(createReqVO, EmployeeInventoryDO.class);
        employeeInventoryMapper.insert(inventory);
        return inventory.getId();
    }

    @Override
    public void updateInventory(EmployeeInventorySaveReqVO updateReqVO) {
        if (updateReqVO.getId() == null || employeeInventoryMapper.selectById(updateReqVO.getId()) == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_PROFILE_NOT_EXISTS);
        }
        EmployeeInventoryDO inventory = BeanUtils.toBean(updateReqVO, EmployeeInventoryDO.class);
        employeeInventoryMapper.updateById(inventory);
    }

    @Override
    public void deleteInventory(Long id) {
        if (employeeInventoryMapper.selectById(id) == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_PROFILE_NOT_EXISTS);
        }
        employeeInventoryMapper.deleteById(id);
    }
}
