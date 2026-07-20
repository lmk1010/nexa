package com.kyx.service.im.dal.dataobject.tencent;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@TableName("im_tencent_user_mapping")
@KeySequence("im_tencent_user_mapping_seq")
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class TencentImUserMappingDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;

    private Long oaUserId;

    private String oaUsername;

    private String ordersysUserPrefix;

    private String ordersysUsername;

    private String fixedPrefix;

    private String imUserId;

    private Integer status;

    private String remark;
}
