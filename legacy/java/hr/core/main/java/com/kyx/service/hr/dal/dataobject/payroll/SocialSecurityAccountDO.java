package com.kyx.service.hr.dal.dataobject.payroll;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@TableName("hr_social_security_account")
@KeySequence("hr_social_security_account_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class SocialSecurityAccountDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long profileId;

    private String socialMonth;

    private String city;

    private BigDecimal insuranceBase;

    private BigDecimal fundBase;

    private BigDecimal pensionPersonal;

    private BigDecimal medicalPersonal;

    private BigDecimal unemploymentPersonal;

    private BigDecimal pensionCompany;

    private BigDecimal medicalCompany;

    private BigDecimal unemploymentCompany;

    private BigDecimal workInjuryCompany;

    private BigDecimal maternityCompany;

    private BigDecimal fundPersonal;

    private BigDecimal fundCompany;

    private String status;

    private String remark;

}
