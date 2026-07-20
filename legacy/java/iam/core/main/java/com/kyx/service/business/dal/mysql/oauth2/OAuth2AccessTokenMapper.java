package com.kyx.service.business.dal.mysql.oauth2;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.foundation.tenant.core.aop.TenantIgnore;
import com.kyx.service.business.controller.admin.auth.vo.OnlineUserPageReqVO;
import com.kyx.service.business.controller.admin.oauth2.vo.token.OAuth2AccessTokenPageReqVO;
import com.kyx.service.business.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import com.kyx.service.business.dal.dataobject.oauth2.OAuth2OnlineUserSummaryDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Mapper
public interface OAuth2AccessTokenMapper extends BaseMapperX<OAuth2AccessTokenDO> {

    @TenantIgnore // 获取 token 的时候，需要忽略租户编号。原因是：一些场景下，可能不会传递 tenant-id 请求头，例如说文件上传、积木报表等等
    default OAuth2AccessTokenDO selectByAccessToken(String accessToken) {
        return selectOne(OAuth2AccessTokenDO::getAccessToken, accessToken);
    }

    default List<OAuth2AccessTokenDO> selectListByRefreshToken(String refreshToken) {
        return selectList(OAuth2AccessTokenDO::getRefreshToken, refreshToken);
    }

    default PageResult<OAuth2AccessTokenDO> selectPage(OAuth2AccessTokenPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<OAuth2AccessTokenDO>()
                .eqIfPresent(OAuth2AccessTokenDO::getUserId, reqVO.getUserId())
                .eqIfPresent(OAuth2AccessTokenDO::getUserType, reqVO.getUserType())
                .likeIfPresent(OAuth2AccessTokenDO::getClientId, reqVO.getClientId())
                .gt(OAuth2AccessTokenDO::getExpiresTime, LocalDateTime.now())
                .orderByDesc(OAuth2AccessTokenDO::getId));
    }

    default List<OAuth2AccessTokenDO> selectListByUserIdAndStatus(Long userId, Integer onlineStatus) {
        return selectList(new LambdaQueryWrapperX<OAuth2AccessTokenDO>()
                .eq(OAuth2AccessTokenDO::getUserId, userId)
                .eq(OAuth2AccessTokenDO::getOnlineStatus, onlineStatus)
                .gt(OAuth2AccessTokenDO::getExpiresTime, LocalDateTime.now())
                .orderByDesc(OAuth2AccessTokenDO::getId));
    }

    default OAuth2AccessTokenDO selectByUserIdAndDeviceId(Long userId, String deviceId) {
        return selectOne(new LambdaQueryWrapperX<OAuth2AccessTokenDO>()
                .eq(OAuth2AccessTokenDO::getUserId, userId)
                .eq(OAuth2AccessTokenDO::getDeviceId, deviceId)
                .gt(OAuth2AccessTokenDO::getExpiresTime, LocalDateTime.now()));
    }

    default PageResult<OAuth2AccessTokenDO> selectOnlineUserPage(OnlineUserPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<OAuth2AccessTokenDO>()
                .eqIfPresent(OAuth2AccessTokenDO::getUserId, reqVO.getUserId())
                .likeIfPresent(OAuth2AccessTokenDO::getDeviceId, reqVO.getDeviceId())
                .eqIfPresent(OAuth2AccessTokenDO::getDeviceType, reqVO.getDeviceType())
                .eq(OAuth2AccessTokenDO::getOnlineStatus, 1)
                .gt(OAuth2AccessTokenDO::getExpiresTime, LocalDateTime.now())
                .orderByDesc(OAuth2AccessTokenDO::getId));
    }

    @Select({
            "<script>",
            "SELECT user_id AS userId,",
            "       COUNT(*) AS onlineSessionCount,",
            "       COUNT(DISTINCT CASE",
            "           WHEN device_id IS NULL OR device_id = '' THEN access_token",
            "           ELSE device_id",
            "       END) AS onlineDeviceCount,",
            "       MAX(last_active_time) AS lastActiveTime",
            "FROM system_oauth2_access_token",
            "WHERE deleted = b'0'",
            "AND online_status = 1",
            "AND expires_time &gt; NOW()",
            "AND user_type = #{userType}",
            "<if test='userIds != null and userIds.size > 0'>",
            "AND user_id IN",
            "<foreach collection='userIds' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</if>",
            "<if test='userIds == null or userIds.size == 0'>",
            "AND 1 = 0",
            "</if>",
            "GROUP BY user_id",
            "</script>"
    })
    List<OAuth2OnlineUserSummaryDO> selectOnlineSummariesByUserIds(@Param("userIds") Collection<Long> userIds,
                                                                   @Param("userType") Integer userType);

}
