package com.kyx.service.hr.service.manager;

import com.kyx.service.hr.controller.admin.manager.vo.HrManagerSelfServiceRespVO;
import com.kyx.service.hr.controller.admin.manager.vo.HrManagerTeamExportRespVO;

import java.util.List;

public interface HrManagerSelfServiceService {

    HrManagerSelfServiceRespVO getHome();

    List<HrManagerTeamExportRespVO> getTeamExportList();

}
