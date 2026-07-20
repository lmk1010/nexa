package com.kyx.service.erp.dal.dataobject.asset;

import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ERP 盘点计划 DO
 *
 * @author kyx
 */
@TableName("erp_inventory_plan")
@KeySequence("erp_inventory_plan_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpInventoryPlanDO extends BaseDO {

    /**
     * 盘点计划编号
     */
    @TableId
    private Long id;
    
    /**
     * 计划编号（自动生成）
     */
    private String planNo;
    
    /**
     * 计划名称
     */
    private String planName;
    
    /**
     * 盘点周期：weekly-周盘点，monthly-月盘点，quarterly-季度盘点，yearly-年终盘点，temporary-临时盘点
     */
    private String planType;
    
    /**
     * 盘点方式：full-全盘，sample-抽盘
     */
    private String method;
    
    /**
     * 计划开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 计划结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 计划描述
     */
    private String description;
    
    /**
     * 抽样比例（1-100），仅当method=sample时有效
     */
    private Integer sampleRate;
    
    /**
     * 抽样方式：random-随机抽样，category-按类别抽样，value-按价值抽样，frequency-按使用频率抽样
     */
    private String sampleMethod;
    
    /**
     * 选择的部门ID列表，JSON格式存储
     */
    private String departmentIds;
    
    /**
     * 选择的使用人ID列表，JSON格式存储
     */
    private String userIds;
    
    /**
     * 选择的资产位置ID列表，JSON格式存储
     */
    private String locationIds;
    
    /**
     * 盘点负责人用户ID
     */
    private Long responsiblePersonId;
    
    /**
     * 盘点负责人姓名
     */
    private String responsiblePersonName;
    
    /**
     * 扫码员用户ID列表，JSON格式存储
     */
    private String scannerIds;
    
    /**
     * 扫码员姓名列表，JSON格式存储
     */
    private String scannerNames;
    
    /**
     * 复核人员用户ID列表，JSON格式存储
     */
    private String reviewerIds;
    
    /**
     * 复核人员姓名列表，JSON格式存储
     */
    private String reviewerNames;
    
    /**
     * 是否锁定待盘库存：true-是，false-否
     */
    private Boolean lockInventory;
    
    /**
     * 是否自动导出盘点清单：true-是，false-否
     */
    private Boolean autoExportList;
    
    /**
     * 盘点计划状态：0-草稿，1-已发布，2-进行中，3-已完成，4-已提交，5-待审核，6-已审核/已关闭，7-已取消
     */
    private Integer status;
    
    /**
     * 总资产数量
     */
    private Integer totalAssetCount;
    
    /**
     * 已完成数量
     */
    private Integer completedAssetCount;
    
    /**
     * 实际开始时间
     */
    private LocalDateTime actualStartTime;
    
    /**
     * 实际结束时间
     */
    private LocalDateTime actualEndTime;
    
    /**
     * 审批人用户ID
     */
    private Long approvalUserId;
    
    /**
     * 审批人姓名
     */
    private String approvalUserName;
    
    /**
     * 审批时间
     */
    private LocalDateTime approvalTime;
    
    /**
     * 审批备注
     */
    private String approvalRemark;

    /**
     * BPM流程实例ID
     */
    private String processInstanceId;
    
    /**
     * 备注
     */
    private String remark;

}
