package com.kyx.service.erp.dal.dataobject.asset;

import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * ERP 资产类型 DO
 *
 * @author kyx
 */
@TableName("erp_asset_type")
@KeySequence("erp_asset_type_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpAssetTypeDO extends BaseDO {

    /**
     * 类型编号
     */
    @TableId
    private Long id;
    /**
     * 父类型编号
     */
    private Long parentId;
    /**
     * 类型名称
     */
    private String name;
    /**
     * 类型编码
     */
    private String code;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 状态：1-启用，0-禁用
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

} 