package com.kyx.service.hr.controller.admin.employee.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "Admin - Employee profile change page Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeProfileChangePageReqVO extends PageParam {

    private Long profileId;

    private Long userId;

    private String keyword;

    private String changeType;

    private String status;

}
