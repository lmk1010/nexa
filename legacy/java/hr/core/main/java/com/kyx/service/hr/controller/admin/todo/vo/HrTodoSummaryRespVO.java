package com.kyx.service.hr.controller.admin.todo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Admin - HR todo summary Response VO")
@Data
public class HrTodoSummaryRespVO {

    private Integer openCount;

    private Integer overdueCount;

    private Integer dueSoonCount;

    private Integer highPriorityCount;

    private Integer generatedOpenCount;

    private Integer doneCount;

}
