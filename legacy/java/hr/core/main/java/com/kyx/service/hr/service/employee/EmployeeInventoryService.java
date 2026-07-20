package com.kyx.service.hr.service.employee;

import com.kyx.service.hr.controller.admin.employee.vo.EmployeeInventoryRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeInventorySaveReqVO;

import javax.validation.Valid;
import java.util.List;

public interface EmployeeInventoryService {

    List<EmployeeInventoryRespVO> getInventoryList(Long profileId);

    Long createInventory(@Valid EmployeeInventorySaveReqVO createReqVO);

    void updateInventory(@Valid EmployeeInventorySaveReqVO updateReqVO);

    void deleteInventory(Long id);
}
