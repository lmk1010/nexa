package com.kyx.service.erp.dal.dataobject.asset;

import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ERP 资产 DO
 *
 * @author kyx
 */
@TableName("erp_asset")
@KeySequence("erp_asset_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpAssetDO extends BaseDO {

    /**
     * 资产编号
     */
    @TableId
    private Long id;
    /**
     * 资产编码
     */
    private String assetNo;
    /**
     * 资产名称
     */
    private String name;
    /**
     * 资产类型
     */
    private String type;
    /**
     * 资产分类编号
     */
    private Long categoryId;
    /**
     * 规格型号
     */
    private String specification;
    /**
     * 品牌
     */
    private String brand;
    /**
     * 型号
     */
    private String model;
    /**
     * 序列号
     */
    private String serialNumber;
    /**
     * 购置日期
     */
    private LocalDate purchaseDate;
    /**
     * 购置价格，单位：元
     */
    private BigDecimal purchasePrice;
    /**
     * 当前价值，单位：元
     */
    private BigDecimal currentValue;
    /**
     * 折旧率，百分比
     */
    private BigDecimal depreciationRate;
    /**
     * 使用年限（年）
     */
    private Integer usefulLife;
    /**
     * 保修到期日期
     */
    private LocalDate warrantyDate;
    /**
     * 存放位置
     */
    private String location;
    /**
     * 管理部门
     */
    private Long deptId;
    /**
     * 供应商编号
     */
    private Long supplierId;
    /**
     * 资产状态：1-正常，2-维修中，3-报废，4-闲置
     */
    private Integer status;
    /**
     * 资产状况：1-良好，2-一般，3-较差
     */
    private Integer conditionStatus;
    /**
     * 备注
     */
    private String remark;
    /**
     * 附件URL
     */
    private String attachmentUrl;

} 