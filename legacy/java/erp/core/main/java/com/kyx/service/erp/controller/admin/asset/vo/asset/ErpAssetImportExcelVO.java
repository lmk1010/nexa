package com.kyx.service.erp.controller.admin.asset.vo.asset;

import com.kyx.foundation.excel.core.annotations.DictFormat;
import com.kyx.foundation.excel.core.convert.DictConvert;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ERP 资产 Excel 导入 VO
 *
 * @author kyx
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = false) // 设置 chain = false，避免导入有问题
public class ErpAssetImportExcelVO {

    @ExcelProperty("资产编码")
    private String assetNo;

    @ExcelProperty("资产名称")
    private String name;

    @ExcelProperty("资产类型")
    private String type;

    @ExcelProperty("资产分类编号")
    private Long categoryId;

    @ExcelProperty("规格型号")
    private String specification;

    @ExcelProperty("品牌")
    private String brand;

    @ExcelProperty("型号")
    private String model;

    @ExcelProperty("序列号")
    private String serialNumber;

    @ExcelProperty("购置日期")
    private LocalDate purchaseDate;

    @ExcelProperty("购置价格")
    private BigDecimal purchasePrice;

    @ExcelProperty("当前价值")
    private BigDecimal currentValue;

    @ExcelProperty("折旧率")
    private BigDecimal depreciationRate;

    @ExcelProperty("使用年限")
    private Integer usefulLife;

    @ExcelProperty("保修到期日期")
    private LocalDate warrantyDate;

    @ExcelProperty("存放位置")
    private String location;

    @ExcelProperty("管理部门编号")
    private Long deptId;

    @ExcelProperty("供应商编号")
    private Long supplierId;

    @ExcelProperty(value = "资产状态", converter = DictConvert.class)
    @DictFormat("erp_asset_status")
    private Integer status;

    @ExcelProperty(value = "资产状况", converter = DictConvert.class)
    @DictFormat("erp_asset_condition_status")
    private Integer conditionStatus;

    @ExcelProperty("备注")
    private String remark;

} 