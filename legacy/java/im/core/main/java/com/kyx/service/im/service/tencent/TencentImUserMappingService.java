package com.kyx.service.im.service.tencent;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.im.controller.admin.tencent.vo.TencentImUserMappingCreateReqVO;
import com.kyx.service.im.controller.admin.tencent.vo.TencentImUserMappingPageReqVO;
import com.kyx.service.im.controller.admin.tencent.vo.TencentImUserMappingUpdateReqVO;
import com.kyx.service.im.dal.dataobject.tencent.TencentImUserMappingDO;

import java.util.List;

public interface TencentImUserMappingService {

    Long createUserMapping(TencentImUserMappingCreateReqVO createReqVO);

    void updateUserMapping(TencentImUserMappingUpdateReqVO updateReqVO);

    void deleteUserMapping(Long id);

    TencentImUserMappingDO getUserMapping(Long id);

    PageResult<TencentImUserMappingDO> getUserMappingPage(TencentImUserMappingPageReqVO pageReqVO);

    TencentImUserMappingDO getByOaUser(Long tenantId, Long oaUserId);

    TencentImUserMappingDO getActiveByOaUser(Long tenantId, Long oaUserId);

    TencentImUserMappingDO getActiveByOaUser(Long oaUserId);

    TencentImUserMappingDO createDefaultMapping(Long tenantId, Long oaUserId, String oaUsername);

    List<TencentImUserMappingDO> getActiveContactList(Long tenantId, Long excludeOaUserId, String keyword, Integer limit);

    String composeImUserId(String fixedPrefix, String ordersysUserPrefix, String ordersysUsername);
}
