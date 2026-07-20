package com.kyx.service.erp.controller.admin.purchase.vo.prorganization;

import com.kyx.foundation.excel.core.annotations.DictFormat;
import com.kyx.foundation.excel.core.convert.DictConvert;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 采购组织 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpPrOrganizationRespVO {

    @Schema(description = "组织编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("组织编号")
    private Long id;

    @Schema(description = "组织编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "HQ_PURCHASE")
    @ExcelProperty("组织编码")
    private String code;

    @Schema(description = "组织名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "总部采购部")
    @ExcelProperty("组织名称")
    private String name;

    @Schema(description = "上级组织编号", example = "1")
    private Long parentId;

    @Schema(description = "上级组织名称", example = "总公司")
    @ExcelProperty("上级组织")
    private String parentName;

    @Schema(description = "负责人", example = "张经理")
    @ExcelProperty("负责人")
    private String manager;

    @Schema(description = "联系电话", example = "021-12345678")
    @ExcelProperty("联系电话")
    private String phone;

    @Schema(description = "电子邮箱", example = "purchase@company.com")
    @ExcelProperty("电子邮箱")
    private String email;

    @Schema(description = "地址", example = "上海市浦东新区科技路123号")
    @ExcelProperty("地址")
    private String address;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("排序")
    private Integer sort;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat("common_status")
    private Integer status;

    @Schema(description = "备注", example = "总部采购组织")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "创建者")
    private String creator;

    @Schema(description = "子组织列表")
    private List<ErpPrOrganizationRespVO> children;

} 