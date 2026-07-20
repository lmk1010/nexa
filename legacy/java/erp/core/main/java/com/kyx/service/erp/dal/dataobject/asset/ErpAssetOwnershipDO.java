package com.kyx.service.erp.dal.dataobject.asset;

import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ERP 资产所有权关系 DO
 * 用于记录资产当前归属关系，便于统计资产在谁名下
 *
 * @author kyx
 */
@TableName("erp_asset_ownership")
@KeySequence("erp_asset_ownership_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpAssetOwnershipDO extends BaseDO {

    /**
     * 关系编号
     */
    @TableId
    private Long id;
    
    /**
     * 资产编号
     */
    private Long assetId;
    
    /**
     * 当前使用人编号
     */
    private Long currentUserId;
    
    /**
     * 当前所属部门编号
     */
    private Long currentDeptId;
    
    /**
     * 领用记录编号（关联到具体的领用记录）
     */
    private Long checkoutId;
    
    /**
     * 关系开始时间（领用时间）
     */
    private LocalDateTime startTime;
    
    /**
     * 关系结束时间（归还时间，为空表示仍在使用）
     */
    private LocalDateTime endTime;
    
    /**
     * 状态：1-使用中，2-已归还，3-转移
     */
    private Integer status;
    
    /**
     * 备注
     */
    private String remark;

} 