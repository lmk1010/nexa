package com.kyx.service.finance.controller.admin.init.vo.company;

import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.validation.InEnum;
import com.kyx.foundation.common.validation.group.SaveGroup;
import com.kyx.foundation.common.validation.group.UpdateGroup;
import com.kyx.foundation.operatelog.parse.CommonStatusParseFunction;
import com.kyx.service.finance.operatelog.AccountingSystemParseFunction;
import com.mzt.logapi.starter.annotation.DiffLogField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 账套保存请求 VO
 *
 * @author Trae AI
 */
@Data
@Schema(description = "账套保存请求")
public class FinanceCompanySaveReqVO {

    @Schema(description = "账套编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "28684")
    @NotNull(message = "账套编号不能为空", groups = UpdateGroup.class)
    private Long id;

    @Schema(description = "账套名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "账套名称不能为空", groups = {SaveGroup.class, UpdateGroup.class})
    @Size(min = 2, max = 50, message = "账套名称长度必须在2-50个字符之间", groups = {SaveGroup.class, UpdateGroup.class})
    @DiffLogField(name = "账套名称")
    private String companyName;

    @Schema(description = "账套编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "账套编码不能为空", groups = SaveGroup.class)
    @Pattern(regexp = "^[0-9]{4,10}$", message = "账套编码只能包含4-10位数字", groups = SaveGroup.class)
    @DiffLogField(name = "账套编码")
    private String companyCode;

    @Schema(description = "会计制度", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "会计制度不能为空", groups = SaveGroup.class)
    @DiffLogField(name = "会计制度", function = AccountingSystemParseFunction.NAME)
    private String accountingSystem;

    @Schema(description = "货币类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "货币类型不能为空", groups = SaveGroup.class)
    @DiffLogField(name = "货币类型")
    private String currency;

    @Schema(description = "启用期间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "启用期间不能为空", groups = SaveGroup.class)
    @DiffLogField(name = "启用期间")
    private String startPeriod;

    @Schema(description = "开启状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "开启状态不能为空", groups = {SaveGroup.class, UpdateGroup.class})
    @InEnum(value = CommonStatusEnum.class, groups = {SaveGroup.class, UpdateGroup.class})
    @DiffLogField(name = "开启状态", function = CommonStatusParseFunction.NAME)
    private Integer status;

    @Schema(description = "描述")
    @DiffLogField(name = "描述")
    private String description;

}
