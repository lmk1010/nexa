package com.kyx.service.hr.controller.admin.selfservice.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "Admin - Employee self-service application page Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class HrSelfServiceApplicationPageReqVO extends PageParam {

    @Schema(description = "Business type")
    private String businessType;

    @Schema(description = "Normalized status")
    private String status;

}
