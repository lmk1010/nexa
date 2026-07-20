package com.kyx.service.hr.service.risk;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.risk.vo.HrRiskEventBatchHandleReqVO;
import com.kyx.service.hr.controller.admin.risk.vo.HrRiskEventCreateReqVO;
import com.kyx.service.hr.controller.admin.risk.vo.HrRiskEventHandleReqVO;
import com.kyx.service.hr.controller.admin.risk.vo.HrRiskEventPageReqVO;
import com.kyx.service.hr.controller.admin.risk.vo.HrRiskEventRespVO;
import com.kyx.service.hr.controller.admin.risk.vo.HrRiskWorkbenchRespVO;

public interface HrRiskWorkbenchService {

    HrRiskWorkbenchRespVO getWorkbench();

    PageResult<HrRiskEventRespVO> getEventPage(HrRiskEventPageReqVO pageReqVO);

    Long createEvent(HrRiskEventCreateReqVO reqVO);

    void handleEvent(HrRiskEventHandleReqVO reqVO);

    Integer batchHandleEvents(HrRiskEventBatchHandleReqVO reqVO);

    Integer refreshGeneratedEvents();

}
