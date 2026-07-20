package com.kyx.service.erp.controller.admin.asset.vo.asset;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 资产分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpAssetPageReqVO extends PageParam {

    @Schema(description = "资产名称", example = "联想电脑")
    private String name;

    @Schema(description = "资产编码", example = "ASSET001")
    private String assetNo;

    @Schema(description = "资产类型", example = "办公设备")
    private String type;

    @Schema(description = "资产分类编号", example = "11161")
    private Long categoryId;

    @Schema(description = "管理部门", example = "1")
    private Long deptId;

    @Schema(description = "资产状态", example = "1")
    private Integer status;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

    @Schema(description = "是否排除已领用的资产", example = "true")
    private Boolean excludeCheckedOut;

} 