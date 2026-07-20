package com.kyx.service.erp.dal.dataobject.asset;

import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ERP 资产归还记录 DO
 *
 * @author kyx
 */
@TableName("erp_asset_return")
@KeySequence("erp_asset_return_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpAssetReturnDO extends BaseDO {

    /**
     * 归还记录编号
     */
    @TableId
    private Long id;
    
    /**
     * 领用记录编号
     */
    private Long checkoutId;
    
    /**
     * 资产编号
     */
    private Long assetId;
    
    /**
     * 归还人编号
     */
    private Long returnUserId;
    
    /**
     * 归还部门编号
     */
    private Long returnDeptId;
    
    /**
     * 归还日期
     */
    private LocalDate returnDate;
    
    /**
     * 归还状态：1-完好，2-轻微损坏，3-严重损坏，4-丢失
     */
    private Integer returnCondition;
    
    /**
     * 归还备注
     */
    private String returnRemark;
    
    /**
     * 接收人编号（资产管理员）
     */
    private Long receiverUserId;
    
    /**
     * 接收时间
     */
    private LocalDateTime receiverTime;
    
    /**
     * 接收备注
     */
    private String receiverRemark;
    
    /**
     * 归还状态：1-已归还，2-已接收确认
     */
    private Integer status;
    
    /**
     * BPM流程实例ID
     */
    private String processInstanceId;
    
    /**
     * BMP流程状态：1-流程中，2-已完成，3-已拒绝，4-已撤销
     */
    private Integer bmpStatus;
    
    /**
     * 备注
     */
    private String remark;

} 