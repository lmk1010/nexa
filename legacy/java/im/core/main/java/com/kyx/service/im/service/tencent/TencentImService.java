package com.kyx.service.im.service.tencent;

import com.kyx.service.im.controller.app.tencent.vo.TencentImLoginRespVO;
import com.kyx.service.im.controller.app.tencent.vo.TencentImUserIdRespVO;

public interface TencentImService {

    TencentImLoginRespVO getLoginTicket();

    TencentImUserIdRespVO getUserId(Long oaUserId);
}
