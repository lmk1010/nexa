package com.kyx.service.hr.controller.admin.payroll.vo;

import com.kyx.foundation.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
public class SocialSecurityAccountPageReqVO extends PageParam {

    private Long profileId;

    private Set<Long> profileIds;

    private String profileName;

    private String profileMobile;

    private String socialMonth;

    private String city;

    private String status;

}
