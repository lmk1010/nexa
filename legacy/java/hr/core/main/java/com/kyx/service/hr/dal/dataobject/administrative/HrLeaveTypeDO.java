package com.kyx.service.hr.dal.dataobject.administrative;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 假期类型 DO
 */
@TableName("hr_leave_type")
@KeySequence("hr_leave_type_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class HrLeaveTypeDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String typeName;

    private String typeCode;

    private Boolean balanceEnabled;

    private String minUnit;

    private Boolean paid;

    private Boolean attachmentRequired;

    private BigDecimal annualDefaultAmount;

    private Integer status;

    private String remark;

}
