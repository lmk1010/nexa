package com.kyx.service.biz.controller.admin.todo.vo;

import com.kyx.foundation.common.validation.InEnum;
import com.kyx.service.biz.enums.TodoStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "Admin - Todo status update Request VO")
@Data
public class TodoUpdateStatusReqVO {

    @Schema(description = "Todo ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "Todo ID is required")
    private Long id;

    @Schema(description = "Status", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "Status is required")
    @InEnum(TodoStatusEnum.class)
    private Integer status;

}
