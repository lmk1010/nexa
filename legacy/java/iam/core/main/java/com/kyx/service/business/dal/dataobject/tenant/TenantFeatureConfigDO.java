package com.kyx.service.business.dal.dataobject.tenant;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.kyx.foundation.tenant.core.aop.TenantIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

@TableName(value = "system_tenant_feature_config")
@KeySequence("system_tenant_feature_config_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TenantIgnore
@Accessors(chain = true)
public class TenantFeatureConfigDO extends BaseDO {

    private Long id;

    private Long tenantId;

    private String featureCode;

    private Integer crossTenantEnabled;

}
