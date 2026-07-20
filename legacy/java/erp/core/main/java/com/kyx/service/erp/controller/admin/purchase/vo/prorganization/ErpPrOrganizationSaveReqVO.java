package com.kyx.service.erp.controller.admin.purchase.vo.prorganization;

import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - ERP 采购组织新增/修改 Request VO")
@Data
public class ErpPrOrganizationSaveReqVO {

    @Schema(description = "组织编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "组织编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "HQ_PURCHASE")
    @NotEmpty(message = "组织编码不能为空")
    private String code;

    @Schema(description = "组织名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "总部采购部")
    @NotEmpty(message = "组织名称不能为空")
    private String name;

    @Schema(description = "上级组织编号", example = "1")
    private Long parentId;

    @Schema(description = "负责人", example = "张经理")
    private String manager;

    @Schema(description = "联系电话", example = "021-12345678")
    private String phone;

    @Schema(description = "电子邮箱", example = "purchase@company.com")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "地址", example = "上海市浦东新区科技路123号")
    private String address;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "排序不能为空")
    private Integer sort;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class)
    private Integer status;

    @Schema(description = "备注", example = "总部采购组织")
    private String remark;

} 