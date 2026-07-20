package com.kyx.service.hr.controller.admin.todo.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "Admin - HR todo page Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class HrTodoPageReqVO extends PageParam {

    @Schema(description = "Only my assignee tasks")
    private Boolean mine;

    @Schema(description = "Business type")
    private String businessType;

    @Schema(description = "Task status: OPEN/DONE/CANCELED")
    private String status;

    @Schema(description = "Priority: HIGH/MEDIUM/LOW")
    private String priority;

    @Schema(description = "Keyword for title/content")
    private String keyword;

    @Schema(description = "Employee profile id")
    private Long profileId;

    @Schema(description = "Department id")
    private Long deptId;

    @Schema(description = "Include child departments")
    private Boolean includeChildren;

    @Schema(description = "Resolved profile ids")
    private List<Long> profileIds;

    @Schema(description = "Due time start")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime dueTimeStart;

    @Schema(description = "Due time end")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime dueTimeEnd;

}
