package com.kyx.service.finance.controller.admin.init.vo.contact;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

/**
 * 往来信息分页请求 VO
 *
 * @author xyang
 */
@Data
@Schema(description = "往来信息分页请求（租户级共享主数据）")
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FinanceContactPageReqVO extends PageParam {

    @Schema(description = "往来名称")
    private String contactName;

    @Schema(description = "分组ID（查询当前分组及下级）")
    private Long groupId;

    @Schema(description = "状态：0 启用，1 停用")
    private Integer status;

    @Schema(description = "查询分组范围（服务层内部填充）")
    private List<Long> groupIds;
}
