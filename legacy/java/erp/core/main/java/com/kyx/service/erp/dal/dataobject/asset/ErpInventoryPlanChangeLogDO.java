package com.kyx.service.erp.dal.dataobject.asset;

import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ERP 盘点计划变更日志 DO
 *
 * @author kyx
 */
@TableName("erp_inventory_plan_change_log")
@KeySequence("erp_inventory_plan_change_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpInventoryPlanChangeLogDO extends BaseDO {

    /**
     * 变更日志编号
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
     * 计划编号
     */
    private String planNo;
    
    /**
     * 变更类型
     * 
     * 枚举值：create-创建，update-更新，status_change-状态变更，delete-删除
     */
    private String changeType;
    
    /**
     * 原状态
     */
    private Integer oldStatus;
    
    /**
     * 新状态
     */
    private Integer newStatus;
    
    /**
     * 原状态名称
     */
    private String oldStatusName;
    
    /**
     * 新状态名称
     */
    private String newStatusName;
    
    /**
     * 变更字段详情，JSON格式存储
     */
    private String changeFields;
    
    /**
     * 变更原因
     */
    private String changeReason;
    
    /**
     * 操作人用户ID
     */
    private Long operationUserId;
    
    /**
     * 操作人姓名
     */
    private String operationUserName;
    
    /**
     * 操作时间
     */
    private LocalDateTime operationTime;
    
    /**
     * 备注
     */
    private String remark;

} 