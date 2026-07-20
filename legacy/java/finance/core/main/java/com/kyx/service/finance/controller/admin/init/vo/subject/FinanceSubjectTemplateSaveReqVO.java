package com.kyx.service.finance.controller.admin.init.vo.subject;

import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.util.json.JsonUtils;
import com.kyx.foundation.common.validation.InEnum;
import com.kyx.foundation.common.validation.group.SaveGroup;
import com.kyx.foundation.common.validation.group.UpdateGroup;
import com.kyx.foundation.operatelog.parse.CommonStatusParseFunction;
import com.kyx.service.finance.operatelog.AccountingSystemParseFunction;
import com.kyx.service.finance.operatelog.SubjectTypeParseFunction;
import com.mzt.logapi.starter.annotation.DiffLogField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * 科目模板保存请求 VO
 * <p>
 * 费用性质（feeType）：多选，逗号分隔，如 "SALES,MANAGEMENT"
 * 经营属性（bizType）：单选，如 "VARIABLE" / "FIXED"
 *
 * @author xyang
 */
@Schema(description = "管理后台 - 科目模板创建/修改 Request VO")
@Data
@Accessors(chain = true)
public class FinanceSubjectTemplateSaveReqVO {

    @Schema(description = "科目模板编号")
    @NotNull(message = "模板ID不能为空", groups = {UpdateGroup.class})
    private Long id;

    @Schema(description = "会计制度", example = "02")
    @NotEmpty(message = "会计制度不能为空", groups = {SaveGroup.class})
    @DiffLogField(name = "会计制度", function = AccountingSystemParseFunction.NAME)
    private String accountingSystem;

    private Long customTenantId;

    @Schema(description = "科目编码", example = "1001")
    @NotEmpty(message = "科目编码不能为空", groups = {SaveGroup.class})
    @Pattern(regexp = "^[0-9]{4,12}$", message = "科目编码只能为4-12位数字", groups = {SaveGroup.class})
    @DiffLogField(name = "科目编码")
    private String subjectCode;

    @Schema(description = "科目名称", example = "库存现金")
    @NotEmpty(message = "科目名称不能为空", groups = {SaveGroup.class, UpdateGroup.class})
    @DiffLogField(name = "科目名称")
    private String subjectName;

    @Schema(description = "科目类型：ASSET/LIABILITY/EQUITY/COST/INCOME/EXPENSE")
    @NotEmpty(message = "科目类型不能为空", groups = {SaveGroup.class})
    @DiffLogField(name = "科目类型", function = SubjectTypeParseFunction.NAME)
    private String subjectType;

    @Schema(description = "科目层级", example = "1")
    @NotNull(message = "科目层级不能为空", groups = {SaveGroup.class})
    @DiffLogField(name = "科目层级")
    private Integer level;

    @Schema(description = "父级科目编码", example = "0")
    @DiffLogField(name = "父级科目编码")
    private String parentCode;

    @Schema(description = "排序号")
    @DiffLogField(name = "排序号")
    private Integer sort;

    @Schema(description = "备注")
    @DiffLogField(name = "备注")
    private String remark;

    @Schema(description = "费用性质（多选，逗号分隔，如 SALES,MANAGEMENT）")
    @DiffLogField(name = "费用性质")
    private String feeType;

    @Schema(description = "往来管理开关（0-否，1-是）")
    @DiffLogField(name = "往来管理开关")
    private Boolean manageSwitch;

    @Schema(description = "经营属性：VARIABLE-变动成本费用，FIXED-固定成本费用")
    @DiffLogField(name = "经营属性")
    private String bizType;

    @Schema(description = "状态：0-启用，1-停用")
    @InEnum(value = CommonStatusEnum.class, groups = {SaveGroup.class, UpdateGroup.class})
    @DiffLogField(name = "状态", function = CommonStatusParseFunction.NAME)
    private Integer status;

    @Override
    public String toString() {
        return JsonUtils.toJsonString(this);
    }
}
