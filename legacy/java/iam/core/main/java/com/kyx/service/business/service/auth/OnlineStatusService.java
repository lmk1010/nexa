package com.kyx.service.business.service.auth;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.business.controller.admin.auth.vo.OnlineUserPageReqVO;
import com.kyx.service.business.dal.dataobject.oauth2.OAuth2AccessTokenDO;

import java.util.List;

/**
 * 在线状态检测 Service 接口
 *
 * @author MK
 */
public interface OnlineStatusService {

    /**
     * 更新用户活跃时间
     *
     * @param accessToken 访问令牌
     */
    void updateUserActiveTime(String accessToken);

    /**
     * 获取用户在线设备列表
     *
     * @param userId 用户ID
     * @return 在线设备列表
     */
    List<OAuth2AccessTokenDO> getUserOnlineDevices(Long userId);

    /**
     * 强制下线指定设备
     *
     * @param userId 用户ID
     * @param deviceId 设备ID
     */
    void forceLogoutDevice(Long userId, String deviceId);

    /**
     * 强制下线除当前设备外的所有设备
     *
     * @param userId 用户ID
     * @param currentDeviceId 当前设备ID
     */
    void forceLogoutOtherDevices(Long userId, String currentDeviceId);

    /**
     * 获取在线用户分页列表
     *
     * @param reqVO 查询条件
     * @return 在线用户分页列表
     */
    PageResult<OAuth2AccessTokenDO> getOnlineUserPage(OnlineUserPageReqVO reqVO);

} 