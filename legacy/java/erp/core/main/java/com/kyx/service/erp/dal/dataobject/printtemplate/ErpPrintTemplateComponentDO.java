package com.kyx.service.erp.dal.dataobject.printtemplate;

import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 打印模版组件 DO
 *
 * @author kyx
 */
@TableName("erp_print_template_component")
@KeySequence("erp_print_template_component_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpPrintTemplateComponentDO extends BaseDO {

    /**
     * 组件编号
     */
    @TableId
    private Long id;
    /**
     * 模版编号
     */
    private Long templateId;
    /**
     * 组件类型：qrcode, barcode, text, label, image, line
     */
    private String type;
    /**
     * 组件名称
     */
    private String name;
    /**
     * X坐标(mm)
     */
    private BigDecimal x;
    /**
     * Y坐标(mm)
     */
    private BigDecimal y;
    /**
     * 宽度(mm)
     */
    private BigDecimal width;
    /**
     * 高度(mm)
     */
    private BigDecimal height;
    /**
     * 组件内容/数据源
     */
    private String content;
    /**
     * 样式配置JSON
     */
    private String styleJson;
    /**
     * 层级排序
     */
    private Integer sort;

} 