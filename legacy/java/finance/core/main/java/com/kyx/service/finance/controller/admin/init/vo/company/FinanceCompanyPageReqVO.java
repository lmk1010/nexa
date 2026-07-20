package com.kyx.service.finance.controller.admin.init.vo.company;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 账套分页查询请求 VO
 *
 * @author Trae AI
 */
@Data
@Schema(description = "账套分页查询请求")
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FinanceCompanyPageReqVO extends PageParam {

    @Schema(description = "账套名称")
    private String companyName;

    @Schema(description = "账套编码")
    private String companyCode;

    @Schema(description = "状态：0(启用), 1(停用)")
    private Integer status;

}
