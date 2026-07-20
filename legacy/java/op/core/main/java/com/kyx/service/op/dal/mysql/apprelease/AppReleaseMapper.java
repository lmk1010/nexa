package com.kyx.service.op.dal.mysql.apprelease;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.op.dal.dataobject.apprelease.AppReleaseDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AppReleaseMapper extends BaseMapperX<AppReleaseDO> {

    default AppReleaseDO selectByVersion(String platform, String channel, Integer versionCode) {
        return selectOne(new LambdaQueryWrapperX<AppReleaseDO>()
                .eq(AppReleaseDO::getPlatform, platform)
                .eq(AppReleaseDO::getChannel, channel)
                .eq(AppReleaseDO::getVersionCode, versionCode));
    }

    default AppReleaseDO selectLatestEnabled(String platform, String channel, Integer currentVersionCode) {
        return selectOne(new LambdaQueryWrapperX<AppReleaseDO>()
                .eq(AppReleaseDO::getPlatform, platform)
                .eq(AppReleaseDO::getChannel, channel)
                .eq(AppReleaseDO::getEnabled, true)
                .gt(AppReleaseDO::getVersionCode, currentVersionCode == null ? 0 : currentVersionCode)
                .orderByDesc(AppReleaseDO::getVersionCode)
                .last("LIMIT 1"));
    }

    default AppReleaseDO selectLatest(String platform, String channel) {
        return selectOne(new LambdaQueryWrapperX<AppReleaseDO>()
                .eq(AppReleaseDO::getPlatform, platform)
                .eq(AppReleaseDO::getChannel, channel)
                .orderByDesc(AppReleaseDO::getVersionCode)
                .last("LIMIT 1"));
    }

}
