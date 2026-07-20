package com.kyx.service.hr.service.selfservice;

import com.kyx.service.hr.controller.admin.selfservice.vo.HrQuickActionConfigRespVO;
import com.kyx.service.hr.controller.admin.selfservice.vo.HrQuickActionConfigSaveReqVO;
import com.kyx.service.hr.controller.admin.selfservice.vo.HrSelfServiceHomeRespVO;

import java.util.List;

public interface HrQuickActionConfigService {

    List<HrQuickActionConfigRespVO> getList(Boolean enabledOnly);

    Long save(HrQuickActionConfigSaveReqVO reqVO);

    Boolean delete(Long id);

    Integer resetDefault();

    List<HrSelfServiceHomeRespVO.QuickAction> getHomeActions();

}
