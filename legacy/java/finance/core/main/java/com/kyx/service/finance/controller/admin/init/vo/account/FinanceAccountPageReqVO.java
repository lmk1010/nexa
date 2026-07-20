package com.kyx.service.finance.controller.admin.init.vo.account;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 账户分页查询请求 VO
 *
 * @author xyang
 */
@Data
@Schema(description = "账户分页查询请求（租户级共享主数据）")
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FinanceAccountPageReqVO extends PageParam {

    @Schema(description = "账户别名")
    private String accountAlias;

    @Schema(description = "银行名称")
    private String bankName;

    @Schema(description = "账户类型")
    private String accountType;

    @Schema(description = "账户标签")
    private String accountTagText;

    @Schema(description = "状态：0 启用，1 停用")
    private Integer status;
}
