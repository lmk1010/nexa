package com.kyx.service.business.controller.admin.tenant.vo.tenant;

import com.kyx.foundation.excel.core.annotations.DictFormat;
import com.kyx.foundation.excel.core.convert.DictConvert;
import com.kyx.service.business.enums.DictTypeConstants;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 租户 Response VO")
@Data
@ExcelIgnoreUnannotated
@Accessors(chain = true)
public class TenantRespVO {

    @Schema(description = "租户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("租户编号")
    private Long id;

    @Schema(description = "租户名", requiredMode = Schema.RequiredMode.REQUIRED, example = "芋道")
    @ExcelProperty("租户名")
    private String name;

    @Schema(description = "联系人", requiredMode = Schema.RequiredMode.REQUIRED, example = "")
    @ExcelProperty("联系人")
    private String contactName;

    @Schema(description = "联系手机", example = "15601691300")
    @ExcelProperty("联系手机")
    private String contactMobile;

    @Schema(description = "租户状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.COMMON_STATUS)
    private Integer status;

    @Schema(description = "绑定域名", example = "https://www.iocoder.cn")
    private String website;

    @Schema(description = "租户套餐编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long packageId;

    @Schema(description = "过期时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime expireTime;

    @Schema(description = "账号数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Integer accountCount;

    @Schema(description = "全局视图（0关闭 1开启）", example = "0")
    private Integer globalView;

    @Schema(description = "父租户编号", example = "0")
    private Long parentId;

    @Schema(description = "集团根租户编号", example = "1")
    private Long rootId;

    @Schema(description = "层级路径", example = "/1/2/")
    private String path;

    @Schema(description = "租户类型（GROUP/COMPANY）", example = "COMPANY")
    private String tenantType;

    @Schema(description = "数据视角（SELF/GROUP/ALL）", example = "SELF")
    private String viewScope;

    @Schema(description = "租户功能配置")
    private List<TenantFeatureConfigRespVO> featureConfigs;


    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
