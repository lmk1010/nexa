package com.kyx.service.business.service.auth;

import cn.hutool.core.util.StrUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.collection.CollectionUtils;
import com.kyx.service.business.controller.admin.auth.vo.OnlineUserPageReqVO;
import com.kyx.service.business.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import com.kyx.service.business.dal.mysql.oauth2.OAuth2AccessTokenMapper;
import com.kyx.service.business.service.oauth2.OAuth2TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.service.business.enums.ErrorCodeConstants.USER_NOT_EXISTS;

/**
 * 在线状态检测 Service 实现类
 *
 * @author MK
 */
@Service
@Slf4j
public class OnlineStatusServiceImpl implements OnlineStatusService {

    @Resource
    private OAuth2TokenService oauth2TokenService;

    @Resource
    private OAuth2AccessTokenMapper oauth2AccessTokenMapper;

    @Override
    public void updateUserActiveTime(String accessToken) {
        if (StrUtil.isBlank(accessToken)) {
            return;
        }

        OAuth2AccessTokenDO accessTokenDO = oauth2TokenService.getAccessToken(accessToken);
        if (accessTokenDO != null && !isExpired(accessTokenDO.getExpiresTime())) {
            // 更新最后活跃时间
            accessTokenDO.setLastActiveTime(LocalDateTime.now());
            accessTokenDO.setOnlineStatus(1); // 设置为在线
            oauth2AccessTokenMapper.updateById(accessTokenDO);
        }
    }

    @Override
    public List<OAuth2AccessTokenDO> getUserOnlineDevices(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        // 查询用户的所有有效访问令牌
        return oauth2AccessTokenMapper.selectListByUserIdAndStatus(userId, 1);
    }

    @Override
    public void forceLogoutDevice(Long userId, String deviceId) {
        if (userId == null || StrUtil.isBlank(deviceId)) {
            return;
        }

        // 查找指定设备的访问令牌
        OAuth2AccessTokenDO accessTokenDO = oauth2AccessTokenMapper.selectByUserIdAndDeviceId(userId, deviceId);
        if (accessTokenDO != null) {
            // 强制下线
            oauth2TokenService.removeAccessToken(accessTokenDO.getAccessToken());
        }
    }

    @Override
    public void forceLogoutOtherDevices(Long userId, String currentDeviceId) {
        if (userId == null) {
            return;
        }

        // 获取用户所有在线设备
        List<OAuth2AccessTokenDO> onlineDevices = getUserOnlineDevices(userId);
        for (OAuth2AccessTokenDO device : onlineDevices) {
            // 如果不是当前设备，则强制下线
            if (!StrUtil.equals(device.getDeviceId(), currentDeviceId)) {
                oauth2TokenService.removeAccessToken(device.getAccessToken());
            }
        }
    }

    @Override
    public PageResult<OAuth2AccessTokenDO> getOnlineUserPage(OnlineUserPageReqVO reqVO) {
        return oauth2AccessTokenMapper.selectOnlineUserPage(reqVO);
    }

    /**
     * 检查令牌是否过期
     */
    private boolean isExpired(LocalDateTime expiresTime) {
        return expiresTime == null || LocalDateTime.now().isAfter(expiresTime);
    }
} 