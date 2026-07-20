package com.kyx.service.hr.controller.admin.notice.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "Admin - HR notice record page request")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HrNoticeRecordPageReqVO extends PageParam {

    @Schema(description = "Keyword")
    private String keyword;

    @Schema(description = "Channel")
    private String channel;

    @Schema(description = "Business type")
    private String businessType;

    @Schema(description = "Send status")
    private String sendStatus;

}
