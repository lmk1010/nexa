package com.kyx.service.biz.service.executive;

import com.kyx.service.biz.controller.admin.executive.vo.ExecutiveCockpitChatReqVO;
import com.kyx.service.biz.controller.admin.executive.vo.ExecutiveCockpitChatRespVO;
import com.kyx.service.biz.controller.admin.executive.vo.ExecutiveCockpitOverviewRespVO;

public interface ExecutiveCockpitService {

    ExecutiveCockpitOverviewRespVO getOverview(Integer days, Long loginUserId);

    ExecutiveCockpitChatRespVO chat(ExecutiveCockpitChatReqVO reqVO, Long loginUserId);

}
