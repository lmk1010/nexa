package com.kyx.service.finance.controller.admin.init.vo.company;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 账套科目 新增/修改 请求 VO
 * <p>
 * 规则：
 * - 新增时：level >= 3（一级/二级科目由模板导入，不允许手动新增）
 * - 修改时：仅允许修改 subjectName / remark / status / sort / feeType / manageSwitch / bizType
 *
 * @author xyang
 */
@Schema(description = "账套科目 新增/修改 Request VO")
@Data
@Accessors(chain = true)
public class FinanceCompanySubjectSaveReqVO {

    @Schema(description = "科目ID（修改时必填）")
    @NotNull(message = "科目ID不能为空", groups = {com.kyx.foundation.common.validation.group.UpdateGroup.class})
    private Long id;

    @Schema(description = "账套ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "账套ID不能为空", groups = {com.kyx.foundation.common.validation.group.SaveGroup.class})
    private Long companyId;

    @Schema(description = "父级科目编码", example = "5602")
    @NotEmpty(message = "父级科目编码不能为空", groups = {com.kyx.foundation.common.validation.group.SaveGroup.class})
    private String parentCode;

    @Schema(description = "科目编码（4-12位数字）", example = "56020501")
    @NotEmpty(message = "科目编码不能为空", groups = {com.kyx.foundation.common.validation.group.SaveGroup.class})
    @Pattern(regexp = "^[0-9]{4,12}$", message = "科目编码只能为4-12位数字", groups = {com.kyx.foundation.common.validation.group.SaveGroup.class})
    private String subjectCode;

    @Schema(description = "科目名称", example = "差旅费")
    @NotEmpty(message = "科目名称不能为空",
            groups = {com.kyx.foundation.common.validation.group.SaveGroup.class,
                    com.kyx.foundation.common.validation.group.UpdateGroup.class})
    @Size(max = 30, message = "科目名称长度不能超过30个字符")
    private String subjectName;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "备注")
    @Size(max = 100, message = "备注长度不能超过100个字符")
    private String remark;

    @Schema(description = "状态：0-启用，1-停用")
    private Integer status;

    /**
     * 费用性质（多选，逗号分隔）
     * 可选值：SALES,MANAGEMENT
     */
    @Schema(description = "费用性质（多选，逗号分隔）")
    private String feeType;

    /**
     * 往来管理开关
     */
    @Schema(description = "往来管理开关（0-否，1-是）")
    private Boolean manageSwitch;

    /**
     * 经营属性（单选）
     */
    @Schema(description = "经营属性：VARIABLE-变动，FIXED-固定")
    private String bizType;
}
