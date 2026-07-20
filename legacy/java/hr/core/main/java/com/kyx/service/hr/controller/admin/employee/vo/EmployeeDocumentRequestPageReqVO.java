package com.kyx.service.hr.controller.admin.employee.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "Admin - Employee document request page Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeDocumentRequestPageReqVO extends PageParam {

    private Long id;

    private Long profileId;

    private Long userId;

    private String keyword;

    private String requestType;

    private String status;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate expectedDateStart;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate expectedDateEnd;

}
