package com.kyx.service.op.service.apprelease;

import cn.hutool.core.util.StrUtil;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.op.controller.admin.apprelease.vo.AppReleasePublishReqVO;
import com.kyx.service.op.controller.admin.apprelease.vo.AppReleaseRespVO;
import com.kyx.service.op.controller.app.apprelease.vo.AppReleaseCheckRespVO;
import com.kyx.service.op.dal.dataobject.apprelease.AppReleaseDO;
import com.kyx.service.op.dal.mysql.apprelease.AppReleaseMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
@Validated
public class AppReleaseServiceImpl implements AppReleaseService {

    private static final String DEFAULT_PLATFORM = "android";
    private static final String DEFAULT_CHANNEL = "prod";

    @Resource
    private AppReleaseMapper appReleaseMapper;

    @Override
    public Long publish(AppReleasePublishReqVO reqVO) {
        String platform = normalize(reqVO.getPlatform(), DEFAULT_PLATFORM);
        String channel = normalize(reqVO.getChannel(), DEFAULT_CHANNEL);
        String downloadUrl = normalizeDownloadUrl(reqVO.getDownloadUrl(), reqVO.getFileId());
        if (StrUtil.isBlank(downloadUrl)) {
            throw invalidParamException("下载地址不能为空");
        }

        AppReleaseDO release = BeanUtils.toBean(reqVO, AppReleaseDO.class)
                .setPlatform(platform)
                .setChannel(channel)
                .setDownloadUrl(downloadUrl)
                .setSha256(reqVO.getSha256().trim().toLowerCase())
                .setForceUpdate(Boolean.TRUE.equals(reqVO.getForceUpdate()))
                .setEnabled(reqVO.getEnabled() == null || Boolean.TRUE.equals(reqVO.getEnabled()));

        AppReleaseDO exists = appReleaseMapper.selectByVersion(platform, channel, reqVO.getVersionCode());
        if (exists == null) {
            appReleaseMapper.insert(release);
            return release.getId();
        }

        release.setId(exists.getId());
        appReleaseMapper.updateById(release);
        return exists.getId();
    }

    @Override
    public AppReleaseRespVO getLatest(String platform, String channel) {
        AppReleaseDO latest = appReleaseMapper.selectLatest(
                normalize(platform, DEFAULT_PLATFORM),
                normalize(channel, DEFAULT_CHANNEL));
        return latest == null ? null : BeanUtils.toBean(latest, AppReleaseRespVO.class);
    }

    @Override
    public AppReleaseCheckRespVO check(String platform, String channel, Integer currentVersionCode) {
        AppReleaseDO latest = appReleaseMapper.selectLatestEnabled(
                normalize(platform, DEFAULT_PLATFORM),
                normalize(channel, DEFAULT_CHANNEL),
                currentVersionCode);
        if (latest == null) {
            return AppReleaseCheckRespVO.noUpdate();
        }
        return new AppReleaseCheckRespVO()
                .setHasUpdate(true)
                .setLatestVersionName(latest.getVersionName())
                .setLatestVersionCode(latest.getVersionCode())
                .setDownloadUrl(latest.getDownloadUrl())
                .setSha256(latest.getSha256())
                .setFileSize(latest.getFileSize())
                .setForceUpdate(Boolean.TRUE.equals(latest.getForceUpdate()))
                .setReleaseNotes(latest.getReleaseNotes());
    }

    private String normalize(String value, String defaultValue) {
        String normalized = StrUtil.blankToDefault(value, defaultValue);
        return normalized.trim().toLowerCase();
    }

    private String normalizeDownloadUrl(String downloadUrl, String fileId) {
        if (StrUtil.isNotBlank(downloadUrl)) {
            return downloadUrl.trim();
        }
        if (StrUtil.isBlank(fileId)) {
            return null;
        }
        return "/admin-api/infra/file/download/" + fileId.trim();
    }

}
