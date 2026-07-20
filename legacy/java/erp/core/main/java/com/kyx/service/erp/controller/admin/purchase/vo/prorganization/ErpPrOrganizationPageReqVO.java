package com.kyx.service.erp.controller.admin.purchase.vo.prorganization;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 采购组织分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpPrOrganizationPageReqVO extends PageParam {

    @Schema(description = "组织名称", example = "总部采购部")
    private String name;

    @Schema(description = "组织编码", example = "HQ_PURCHASE")
    private String code;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "上级组织编号", example = "1")
    private Long parentId;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] createTime;

} 