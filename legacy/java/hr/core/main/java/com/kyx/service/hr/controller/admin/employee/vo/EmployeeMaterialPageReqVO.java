package com.kyx.service.hr.controller.admin.employee.vo;

import com.kyx.foundation.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeMaterialPageReqVO extends PageParam {

    private Long id;

    private Long profileId;

    private Set<Long> profileIds;

    private Long userId;

    private String keyword;

    private String profileName;

    private String category;

    private String materialType;

    private String materialName;

    private String status;

    private LocalDate expireDateStart;

    private LocalDate expireDateEnd;

}
