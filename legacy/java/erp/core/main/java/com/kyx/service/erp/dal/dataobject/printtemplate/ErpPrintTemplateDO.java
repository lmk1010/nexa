package com.kyx.service.erp.dal.dataobject.printtemplate;

import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 打印模版 DO
 *
 * @author kyx
 */
@TableName("erp_print_template")
@KeySequence("erp_print_template_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpPrintTemplateDO extends BaseDO {

    /**
     * 模版编号
     */
    @TableId
    private Long id;
    /**
     * 模版名称
     */
    private String name;
    /**
     * 模版类型：asset-label, equipment-nameplate, inventory-label, maintenance-label
     */
    private String type;
    /**
     * 模版宽度(mm)
     */
    private BigDecimal width;
    /**
     * 模版高度(mm)
     */
    private BigDecimal height;
    /**
     * 模版描述
     */
    private String description;
    /**
     * 预览图片URL
     */
    private String previewImage;
    /**
     * 模版配置JSON
     */
    private String configJson;
    /**
     * 状态：1-启用，2-禁用
     */
    private Integer status;
    /**
     * 排序字段
     */
    private Integer sort;

} 