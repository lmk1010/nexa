package com.kyx.service.biz.controller.admin.todo.vo;

import com.kyx.foundation.common.validation.InEnum;
import com.kyx.service.biz.enums.TodoPriorityEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

@Schema(description = "Admin - Todo create/update Request VO")
@Data
public class TodoSaveReqVO {

    @Schema(description = "Todo ID", example = "1024")
    private Long id;

    @Schema(description = "Title", requiredMode = Schema.RequiredMode.REQUIRED, example = "Monthly budget review")
    @NotBlank(message = "Title is required")
    private String title;

    @Schema(description = "Description", example = "Notes and action items")
    private String description;

    @Schema(description = "Due date", example = "2025-01-30")
    private Date dueDate;

    @Schema(description = "Priority", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "Priority is required")
    @InEnum(TodoPriorityEnum.class)
    private Integer priority;

    @Schema(description = "Tags", example = "[\"approval\",\"hr\"]")
    private List<String> tags;

}
