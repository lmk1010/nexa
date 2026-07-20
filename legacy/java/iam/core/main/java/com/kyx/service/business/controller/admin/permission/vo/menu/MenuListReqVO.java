package com.kyx.service.business.controller.admin.permission.vo.menu;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - 菜单列表 Request VO")
@Data
@Accessors(chain = true)
public class MenuListReqVO {

    @Schema(description = "菜单名称，模糊匹配", example = "芋道")
    private String name;

    @Schema(description = "展示状态，参见 CommonStatusEnum 枚举类", example = "1")
    private Integer status;

}
