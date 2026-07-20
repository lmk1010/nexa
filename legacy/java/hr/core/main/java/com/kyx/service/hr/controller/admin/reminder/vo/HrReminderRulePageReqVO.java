package com.kyx.service.hr.controller.admin.reminder.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "Admin - HR reminder rule page request")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HrReminderRulePageReqVO extends PageParam {

    @Schema(description = "Keyword")
    private String keyword;

    @Schema(description = "Business type")
    private String businessType;

    @Schema(description = "Enabled")
    private Boolean enabled;

}
