package com.kyx.service.hr.service.selfservice;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.selfservice.vo.HrSelfServiceApplicationPageReqVO;
import com.kyx.service.hr.controller.admin.selfservice.vo.HrSelfServiceApplicationRespVO;

public interface HrSelfServiceApplicationService {

    PageResult<HrSelfServiceApplicationRespVO> getApplicationPage(HrSelfServiceApplicationPageReqVO reqVO);

}
