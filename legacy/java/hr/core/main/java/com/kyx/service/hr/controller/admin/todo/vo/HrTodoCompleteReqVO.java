package com.kyx.service.hr.controller.admin.todo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "Admin - HR todo complete Request VO")
@Data
public class HrTodoCompleteReqVO {

    @Schema(description = "Todo id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "待办ID不能为空")
    private Long id;

    @Schema(description = "Complete remark")
    private String remark;

}
