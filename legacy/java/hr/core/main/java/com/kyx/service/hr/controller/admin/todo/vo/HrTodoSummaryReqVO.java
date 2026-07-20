package com.kyx.service.hr.controller.admin.todo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Admin - HR todo summary Request VO")
@Data
public class HrTodoSummaryReqVO {

    @Schema(description = "Only my assignee tasks")
    private Boolean mine;

    @Schema(description = "Employee profile id")
    private Long profileId;

    @Schema(description = "Department id")
    private Long deptId;

    @Schema(description = "Include child departments")
    private Boolean includeChildren;

}
