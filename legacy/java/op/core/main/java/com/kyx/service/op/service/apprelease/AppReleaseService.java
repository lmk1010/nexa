package com.kyx.service.op.service.apprelease;

import com.kyx.service.op.controller.admin.apprelease.vo.AppReleasePublishReqVO;
import com.kyx.service.op.controller.admin.apprelease.vo.AppReleaseRespVO;
import com.kyx.service.op.controller.app.apprelease.vo.AppReleaseCheckRespVO;

public interface AppReleaseService {

    Long publish(AppReleasePublishReqVO reqVO);

    AppReleaseRespVO getLatest(String platform, String channel);

    AppReleaseCheckRespVO check(String platform, String channel, Integer currentVersionCode);

}
