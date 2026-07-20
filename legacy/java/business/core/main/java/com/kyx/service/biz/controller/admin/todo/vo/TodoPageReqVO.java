package com.kyx.service.biz.controller.admin.todo.vo;

import com.kyx.foundation.common.pojo.PageParam;
import com.kyx.foundation.common.validation.InEnum;
import com.kyx.service.biz.enums.TodoPriorityEnum;
import com.kyx.service.biz.enums.TodoStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Admin - Todo page Request VO")
@Data
public class TodoPageReqVO extends PageParam {

    @Schema(description = "Keyword", example = "budget")
    private String keyword;

    @Schema(description = "Status", example = "0")
    @InEnum(TodoStatusEnum.class)
    private Integer status;

    @Schema(description = "Priority", example = "2")
    @InEnum(TodoPriorityEnum.class)
    private Integer priority;

}
