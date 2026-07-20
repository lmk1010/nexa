package com.kyx.service.hr.controller.admin.risk.vo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class HrRiskEventHandleReqVO {

    @NotNull(message = "风险事件不能为空")
    private Long id;

    @NotBlank(message = "处理状态不能为空")
    private String status;

    private Long ownerUserId;

    private String handleResult;

    private String remark;

}
