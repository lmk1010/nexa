package com.kyx.service.erp.dal.dataobject.asset;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import lombok.*;

/**
 * ERP 资产调拨项 DO
 *
 * @author kyx
 */
@TableName("erp_asset_redistribution_item")
@KeySequence("erp_asset_redistribution_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpAssetRedistributionItemDO extends BaseDO {

    /**
     * 调拨项ID
     */
    @TableId
    private Long id;
    
    /**
     * 调拨记录ID
     */
    private Long redistributionId;
    
    /**
     * 资产ID
     */
    private Long assetId;
    
    /**
     * 调拨前的资产状态
     */
    private Integer originalStatus;
    
    /**
     * 调拨前的资产位置
     */
    private String originalLocation;
    
    /**
     * 调拨前的所属部门ID
     */
    private Long originalDeptId;
    
    /**
     * 调拨前的使用人ID
     */
    private Long originalUserId;
    
    /**
     * 备注
     */
    private String remark;
} 