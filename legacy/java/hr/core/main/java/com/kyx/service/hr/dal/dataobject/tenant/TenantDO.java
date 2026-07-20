package com.kyx.service.hr.dal.dataobject.tenant;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.kyx.foundation.tenant.core.aop.TenantIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租户 DO（发布范围简化查询）
 *
 * @author MK
 */
@TableName(value = "system_tenant")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
public class TenantDO extends BaseDO {

    private Long id;

    private String name;

    private Integer status;

    private Long parentId;

    private Long rootId;

    private Integer globalView;

    private String viewScope;

    private String tenantType;
}
