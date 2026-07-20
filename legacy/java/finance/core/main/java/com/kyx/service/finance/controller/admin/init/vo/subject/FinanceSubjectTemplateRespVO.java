package com.kyx.service.finance.controller.admin.init.vo.subject;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.kyx.foundation.excel.core.annotations.DictFormat;
import com.kyx.foundation.excel.core.convert.DictConvert;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

import static com.kyx.service.business.enums.DictTypeConstants.COMMON_STATUS;
import static com.kyx.service.finance.enums.DictTypeConstants.ACCOUNTING_SYSTEM;

/**
 * 科目模板响应 VO
 *
 * @author xyang
 */
@Data
@Schema(description = "科目模板响应")
@ExcelIgnoreUnannotated
@Accessors(chain = true)
public class FinanceSubjectTemplateRespVO {

    @Schema(description = "模板编号")
    @ExcelProperty("模板编号")
    private Long id;

    @Schema(description = "会计制度")
    @ExcelProperty(value = "会计制度", converter = DictConvert.class)
    @DictFormat(ACCOUNTING_SYSTEM)
    private String accountingSystem;

    private Long customTenantId;

    @Schema(description = "科目编码")
    @ExcelProperty("科目编码")
    private String subjectCode;

    @Schema(description = "科目名称")
    @ExcelProperty("科目名称")
    private String subjectName;

    @Schema(description = "科目类型")
    @ExcelProperty("科目类型")
    private String subjectType;

    @Schema(description = "父级科目编码")
    @ExcelProperty("父级科目编码")
    private String parentCode;

    @Schema(description = "科目层级")
    @ExcelProperty("科目层级")
    private Integer level;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "费用性质（多选，逗号分隔）")
    private String feeType;

    @Schema(description = "往来管理开关（0-否，1-是）")
    private Boolean manageSwitch;

    @Schema(description = "经营属性：VARIABLE-变动，FIXED-固定")
    private String bizType;

    @Schema(description = "状态：0-启用，1-停用")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat(COMMON_STATUS)
    private Integer status;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;
}
