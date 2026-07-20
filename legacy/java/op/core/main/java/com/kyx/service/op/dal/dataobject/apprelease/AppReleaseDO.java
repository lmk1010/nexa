package com.kyx.service.op.dal.dataobject.apprelease;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.kyx.foundation.tenant.core.aop.TenantIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@TableName("infra_app_release")
@KeySequence("infra_app_release_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TenantIgnore
public class AppReleaseDO extends BaseDO {

    @TableId
    private Long id;

    private String platform;

    private String channel;

    private String versionName;

    private Integer versionCode;

    private String fileId;

    private String downloadUrl;

    private String sha256;

    private Long fileSize;

    private Boolean forceUpdate;

    private Boolean enabled;

    private String releaseNotes;

}
