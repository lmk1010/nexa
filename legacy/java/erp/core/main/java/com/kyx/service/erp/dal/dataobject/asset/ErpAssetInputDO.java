package com.kyx.service.erp.dal.dataobject.asset;

import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ERP 资产录入申请 DO
 *
 * @author kyx
 */
@TableName("erp_asset_input")
@KeySequence("erp_asset_input_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpAssetInputDO extends BaseDO {

    /**
     * 申请编号
     */
    @TableId
    private Long id;
    
    /**
     * 录入申请编号
     */
    private String inputNo;
    
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
     * 申请状态：1-待审批，2-审批中，3-审批通过，4-审批拒绝
     */
    private Integer status;
    
    /**
     * 资产状况：1-良好，2-一般，3-较差
     */
    private Integer conditionStatus;
    
    /**
     * 审批状态：1-待审批，2-审批中，3-审批通过，4-审批拒绝
     */
    private Integer approvalStatus;
    
    /**
     * BMP流程状态：1-流程中，2-已完成，3-已取消
     */
    private Integer bmpStatus;
    
    /**
     * BMP流程实例ID
     */
    private String bmpProcessInstanceId;
    
    /**
     * 审批通过后创建的资产ID
     */
    private Long assetId;
    
    /**
     * 审批人用户ID
     */
    private Long approverUserId;
    
    /**
     * 审批人姓名
     */
    private String approverUserName;
    
    /**
     * 审批时间
     */
    private LocalDateTime approvalTime;
    
    /**
     * 审批备注
     */
    private String approvalRemark;
    
    /**
     * 拒绝原因
     */
    private String rejectReason;
    
    /**
     * 备注
     */
    private String remark;

} 