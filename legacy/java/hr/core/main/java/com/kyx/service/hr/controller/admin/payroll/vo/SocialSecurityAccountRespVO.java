package com.kyx.service.hr.controller.admin.payroll.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class SocialSecurityAccountRespVO {

    private Long id;

    private Long profileId;

    private String profileName;

    private String profileMobile;

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

    private BigDecimal personalTotal;

    private BigDecimal companyTotal;

    private String status;

    private String remark;

    private Date createTime;

}
