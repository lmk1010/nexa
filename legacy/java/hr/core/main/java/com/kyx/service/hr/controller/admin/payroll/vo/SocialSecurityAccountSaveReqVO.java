package com.kyx.service.hr.controller.admin.payroll.vo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class SocialSecurityAccountSaveReqVO {

    private Long id;

    @NotNull(message = "员工不能为空")
    private Long profileId;

    @NotBlank(message = "社保月份不能为空")
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
