package com.kyx.service.hr.service.employee;

import com.kyx.service.hr.controller.admin.employee.vo.master.EmployeeChangeLogRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.master.EmployeeCustomFieldRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.master.EmployeeCustomFieldSaveReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.master.EmployeeCustomFieldValueRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.master.EmployeeCustomFieldValuesSaveReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.master.EmployeeDataQualityRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.master.EmployeeMasterWorkbenchRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.master.EmployeeSavedViewRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.master.EmployeeSavedViewSaveReqVO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeProfileDO;

import javax.validation.Valid;
import java.util.List;

public interface EmployeeMasterDataService {

    EmployeeMasterWorkbenchRespVO getWorkbench();

    EmployeeDataQualityRespVO getDataQuality();

    List<EmployeeCustomFieldRespVO> getCustomFieldList(Integer status);

    Long createCustomField(@Valid EmployeeCustomFieldSaveReqVO reqVO);

    void updateCustomField(@Valid EmployeeCustomFieldSaveReqVO reqVO);

    void deleteCustomField(Long id);

    List<EmployeeCustomFieldValueRespVO> getCustomFieldValues(Long profileId);

    void saveCustomFieldValues(@Valid EmployeeCustomFieldValuesSaveReqVO reqVO);

    List<EmployeeSavedViewRespVO> getMySavedViews();

    Long saveMySavedView(@Valid EmployeeSavedViewSaveReqVO reqVO);

    void deleteMySavedView(Long id);

    void setMyDefaultSavedView(Long id);

    List<EmployeeChangeLogRespVO> getChangeLogs(Long profileId);

    void recordProfileChanges(EmployeeProfileDO beforeProfile, EmployeeProfileDO afterProfile,
                              String sourceType, Long sourceId);

}
