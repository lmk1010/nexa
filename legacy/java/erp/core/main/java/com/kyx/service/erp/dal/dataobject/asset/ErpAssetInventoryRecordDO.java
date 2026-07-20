package com.kyx.service.erp.dal.dataobject.asset;

import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ERP 资产盘点记录 DO
 *
 * @author kyx
 */
@TableName("erp_asset_inventory_record")
@KeySequence("erp_asset_inventory_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpAssetInventoryRecordDO extends BaseDO {

    /**
     * 盘点记录编号
     */
    @TableId
    private Long id;
    
    /**
     * 盘点计划编号
     * 
     * 关联 {@link ErpInventoryPlanDO#getId()}
     */
    private Long planId;
    
    /**
     * 资产编号
     * 
     * 关联 {@link ErpAssetDO#getId()}
     */
    private Long assetId;
    
    /**
     * 资产编码
     */
    private String assetCode;
    
    /**
     * 资产名称
     */
    private String assetName;
    
    /**
     * 资产分类编号
     */
    private Long categoryId;
    
    /**
     * 资产分类名称
     */
    private String categoryName;
    
    /**
     * 预期状态（账面状态）
     * 
     * 枚举值：normal-正常，in_use-在用，idle-闲置，maintenance-维修中，scrapped-报废，lost-丢失
     */
    private String expectedStatus;
    
    /**
     * 实际状态（盘点发现的状态）
     * 
     * 枚举值：normal-正常，in_use-在用，idle-闲置，maintenance-维修中，scrapped-报废，lost-丢失，not_found-未找到
     */
    private String actualStatus;
    
    /**
     * 预期位置编号
     */
    private Long expectedLocationId;
    
    /**
     * 预期位置名称
     */
    private String expectedLocationName;
    
    /**
     * 实际位置编号
     */
    private Long actualLocationId;
    
    /**
     * 实际位置名称
     */
    private String actualLocationName;
    
    /**
     * 预期使用人编号
     */
    private Long expectedUserId;
    
    /**
     * 预期使用人姓名
     */
    private String expectedUserName;
    
    /**
     * 实际使用人编号
     */
    private Long actualUserId;
    
    /**
     * 实际使用人姓名
     */
    private String actualUserName;
    
    /**
     * 盘点结果
     * 
     * 枚举值：normal-正常，status_diff-状态差异，location_diff-位置差异，user_diff-使用人差异，not_found-未找到
     */
    private String inventoryResult;
    
    /**
     * 盘点时间
     */
    private LocalDateTime inventoryTime;
    
    /**
     * 盘点人员编号
     */
    private Long inventoryUserId;
    
    /**
     * 盘点人员姓名
     */
    private String inventoryUserName;
    
    /**
     * 扫描方式
     * 
     * 枚举值：qr_code-二维码扫描，barcode-条形码扫描，manual-手动录入
     */
    private String scanMethod;
    
    /**
     * 扫描内容（二维码或条形码的原始内容）
     */
    private String scanContent;
    
    /**
     * 资产原值
     */
    private BigDecimal originalValue;
    
    /**
     * 资产现值
     */
    private BigDecimal currentValue;
    
    /**
     * 差异说明
     */
    private String diffDescription;
    
    /**
     * 盘点备注
     */
    private String remark;
    
    /**
     * 照片URL（盘点时拍摄的照片）
     */
    private String photoUrl;
    
    /**
     * 是否需要处理
     * 
     * true-需要处理（有差异），false-无需处理（正常）
     */
    private Boolean needsAction;
    
    /**
     * 处理状态
     * 
     * 枚举值：pending-待处理，processing-处理中，completed-已处理，ignored-已忽略
     */
    private String actionStatus;
    
    /**
     * 处理人员编号
     */
    private Long actionUserId;
    
    /**
     * 处理人员姓名
     */
    private String actionUserName;
    
    /**
     * 处理时间
     */
    private LocalDateTime actionTime;
    
    /**
     * 处理备注
     */
    private String actionRemark;

} 