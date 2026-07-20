package com.kyx.service.hr.controller.admin.lifecycle.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - HR 生命周期事件分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HrLifecycleEventPageReqVO extends PageParam {

    @Schema(description = "员工档案 ID")
    private Long profileId;

    @Schema(description = "员工姓名")
    private String employeeName;

    @Schema(description = "事件类型")
    private String eventType;

    @Schema(description = "事件状态")
    private String eventStatus;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "生效日期开始")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate effectiveDateStart;

    @Schema(description = "生效日期结束")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate effectiveDateEnd;

}
