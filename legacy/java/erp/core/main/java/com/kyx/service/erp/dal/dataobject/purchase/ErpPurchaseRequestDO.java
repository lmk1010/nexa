package com.kyx.service.erp.dal.dataobject.purchase;

import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

/**
 * ERP 采购申请 DO
 *
 * @author MK
 */
@TableName("erp_purchase_request")
@KeySequence("erp_purchase_request_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpPurchaseRequestDO extends BaseDO {

    /**
     * 申请编号
     */
    @TableId
    private Long id;
    /**
     * 申请单号
     */
    private String requestNo;
    /**
     * 申请标题
     */
    private String title;
    /**
     * 申请人
     */
    private String applicant;
    /**
     * 申请部门
     */
    private String department;
    /**
     * 联系电话
     */
    private String contactPhone;
    /**
     * 预算科目
     */
    private String budgetAccount;
    /**
     * 申请日期
     */
    private Date applyDate;
    /**
     * 需求日期
     */
    private Date requiredDate;
    /**
     * 紧急程度
     *
     * 枚举 1-低，2-一般，3-高，4-紧急
     */
    private Integer urgentLevel;
    /**
     * 申请原因
     */
    private String reason;
    /**
     * 状态
     *
     * 枚举 0-草稿，1-待审核，2-已通过，3-已拒绝，4-已取消
     */
    private Integer status;
    /**
     * 总金额
     */
    private BigDecimal totalAmount;
    /**
     * 备注
     */
    private String remark;
    /**
     * BPM流程实例ID
     */
    private String processInstanceId;
    /**
     * BMP流程状态
     *
     * 枚举 1-流程中，2-已完成，3-已拒绝，4-已撤销
     */
    private Integer bmpStatus;
    /**
     * 审核意见
     */
    private String auditReason;

} 