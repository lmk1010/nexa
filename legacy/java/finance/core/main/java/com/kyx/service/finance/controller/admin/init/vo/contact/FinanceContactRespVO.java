package com.kyx.service.finance.controller.admin.init.vo.contact;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.kyx.foundation.excel.core.annotations.DictFormat;
import com.kyx.foundation.excel.core.convert.DictConvert;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

import static com.kyx.service.business.enums.DictTypeConstants.COMMON_STATUS;

/**
 * 往来信息响应 VO
 *
 * @author xyang
 */
@Data
@Schema(description = "往来信息")
@ExcelIgnoreUnannotated
@Accessors(chain = true)
public class FinanceContactRespVO {

    @Schema(description = "往来ID")
    @ExcelProperty("往来ID")
    private Long id;

    @Schema(description = "分组ID")
    @ExcelProperty("分组ID")
    private Long groupId;

    @Schema(description = "分组名称")
    @ExcelProperty("分组名称")
    private String groupName;

    @Schema(description = "往来名称")
    @ExcelProperty("往来名称")
    private String contactName;

    @Schema(description = "地址")
    @ExcelProperty("地址")
    private String address;

    @Schema(description = "账户类型")
    @ExcelProperty("账户类型")
    private String accountType;

    @Schema(description = "账户名称")
    @ExcelProperty("账户名称")
    private String accountName;

    @Schema(description = "账号")
    @ExcelProperty("账号")
    private String accountNo;

    @Schema(description = "姓名")
    @ExcelProperty("姓名")
    private String ownerName;

    @Schema(description = "联系方式")
    @ExcelProperty("联系方式")
    private String phone;

    @Schema(description = "状态：0 启用，1 停用")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat(COMMON_STATUS)
    private Integer status;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;
}
